package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientIndoorSmokeManager {
    private static final int CELL_SIZE = 4;
    private static final int MAX_CELLS = 96;
    private static final double MAX_VISIBLE_DISTANCE_SQR = 72.0D * 72.0D;
    private static final int MAX_SMOKE_PARTICLES_PER_SECOND = 95;
    private static final long SPREAD_INTERVAL_TICKS = 18L;
    private static final Map<Long, SmokeCell> CELLS = new HashMap<>();
    private static final ArrayDeque<Long> CELL_ORDER = new ArrayDeque<>();
    private static long particleLimiterSecond = -1L;
    private static int particlesThisSecond;

    private ClientIndoorSmokeManager() {
    }

    public static void addWeaponSmoke(MuzzleSmokePacket packet, Vec3 muzzle, Vec3 forward, int visibleSmokeCount, double heatSmokeMultiplier) {
        if (!BallisticBlockConfig.ENABLE_INDOOR_SMOKE.get() || visibleSmokeCount <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || player.distanceToSqr(muzzle) > MAX_VISIBLE_DISTANCE_SQR) {
            return;
        }

        double enclosure = enclosureScore(level, BlockPos.containing(muzzle));
        boolean indoor = enclosure >= 0.38D;
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        double kindMultiplier = switch (kind) {
            case PISTOL -> 0.55D;
            case RIFLE -> 1.0D;
            case SHOTGUN, SNIPER -> 1.65D;
        };
        double density = visibleSmokeCount * 0.012D * kindMultiplier * heatSmokeMultiplier;
        if (packet.automatic() && BallisticBlockConfig.DYNAMIC_SMOKE_BUILDUP.get()) {
            density *= 1.35D;
        }
        if (packet.highCaliber()) {
            density *= 1.25D;
        }
        if (packet.suppressed()) {
            density *= 0.45D;
        }
        density *= indoor ? BallisticBlockConfig.INDOOR_SMOKE_MULTIPLIER.get() * (0.65D + enclosure) : 0.22D;
        if (!level.isRainingAt(BlockPos.containing(muzzle.add(0.0D, 1.0D, 0.0D)))) {
            density *= indoor ? 1.08D : 1.0D;
        }

        addDensity(level, muzzle.add(forward.scale(0.35D)), density, indoor, enclosure);
    }

    public static void addExplosionSmoke(Vec3 position) {
        if (!BallisticBlockConfig.ENABLE_INDOOR_SMOKE.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || player.distanceToSqr(position) > MAX_VISIBLE_DISTANCE_SQR) {
            return;
        }
        double enclosure = enclosureScore(level, BlockPos.containing(position));
        addDensity(level, position, enclosure >= 0.35D ? 0.55D * BallisticBlockConfig.INDOOR_SMOKE_MULTIPLIER.get() : 0.18D, enclosure >= 0.35D, enclosure);
    }

    public static void illuminate(Vec3 position, double intensity, float red, float green, float blue) {
        if (!BallisticBlockConfig.ENABLE_INDOOR_SMOKE.get() || !BallisticBlockConfig.ENABLE_FLASHLIGHT_SMOKE_INTERACTION.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (SmokeCell cell : CELLS.values()) {
            if (cell.center.distanceToSqr(position) > 8.0D * 8.0D) {
                continue;
            }
            double distanceScale = 1.0D - Mth.clamp(cell.center.distanceTo(position) / 8.0D, 0.0D, 1.0D);
            cell.lightBoost = Math.max(cell.lightBoost, (float) (intensity * distanceScale));
            cell.lightRed = red;
            cell.lightGreen = green;
            cell.lightBlue = blue;
            cell.lastLightMillis = now;
        }
    }

    public static float densityAlong(Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double lengthSqr = direction.lengthSqr();
        if (lengthSqr < 0.001D) {
            return 0.0F;
        }
        float density = 0.0F;
        for (SmokeCell cell : CELLS.values()) {
            double t = Mth.clamp(cell.center.subtract(start).dot(direction) / lengthSqr, 0.0D, 1.0D);
            if (cell.center.distanceToSqr(start.add(direction.scale(t))) < 5.0D) {
                density = Math.max(density, (float) cell.density);
            }
        }
        return density;
    }

    public static void tick() {
        if (!BallisticBlockConfig.ENABLE_INDOOR_SMOKE.get() || CELLS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            clear();
            return;
        }

        long gameTime = level.getGameTime();
        List<SpreadRequest> spreadRequests = new ArrayList<>();
        Iterator<Map.Entry<Long, SmokeCell>> iterator = CELLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, SmokeCell> entry = iterator.next();
            SmokeCell cell = entry.getValue();
            double dissipation = cell.indoor
                    ? BallisticBlockConfig.SMOKE_DISSIPATION_SPEED_INDOOR.get()
                    : BallisticBlockConfig.SMOKE_DISSIPATION_SPEED_OUTDOOR.get();
            if (level.isRainingAt(BlockPos.containing(cell.center))) {
                dissipation *= 1.8D;
            }
            double openness = 1.0D - cell.enclosure;
            cell.density -= (0.0026D + openness * 0.0048D) * dissipation;
            cell.lightBoost *= 0.78F;
            if (cell.density <= 0.01D || player.distanceToSqr(cell.center) > MAX_VISIBLE_DISTANCE_SQR * 1.65D) {
                iterator.remove();
                CELL_ORDER.remove(entry.getKey());
                continue;
            }
            if (gameTime % SPREAD_INTERVAL_TICKS == cell.spreadOffset) {
                collectSpreadRequests(level, cell, spreadRequests);
            }
            spawnAmbientSmoke(level, player, cell);
        }
        for (SpreadRequest request : spreadRequests) {
            addDensity(level, request.position, request.amount, request.indoor, request.enclosure);
            request.source.density *= 0.992D;
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_INDOOR_SMOKE.get() || CELLS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (SmokeCell cell : CELLS.values()) {
            if (cell.center.distanceToSqr(cameraPos) > MAX_VISIBLE_DISTANCE_SQR) {
                continue;
            }
            if (!ClientEffectRenderSafety.canRenderAt(cameraPos, cell.center)) {
                continue;
            }
            renderHaze(buffer, matrix, cameraPos, cell);
        }

        BufferUploader.drawWithShader(buffer.end());
        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    public static void clear() {
        CELLS.clear();
        CELL_ORDER.clear();
        particleLimiterSecond = -1L;
        particlesThisSecond = 0;
    }

    private static void addDensity(ClientLevel level, Vec3 position, double amount, boolean indoor, double enclosure) {
        if (amount <= 0.0D) {
            return;
        }
        BlockPos origin = cellOrigin(position);
        long key = origin.asLong();
        SmokeCell cell = CELLS.get(key);
        if (cell == null) {
            cell = new SmokeCell(centerOf(origin), indoor, enclosure, level.random.nextInt((int) SPREAD_INTERVAL_TICKS));
            CELLS.put(key, cell);
            CELL_ORDER.addLast(key);
        }
        cell.indoor = cell.indoor || indoor;
        cell.enclosure = Math.max(cell.enclosure, enclosure);
        double maxDensity = BallisticBlockConfig.MAX_INDOOR_SMOKE_DENSITY.get();
        cell.density = Mth.clamp(cell.density + amount, 0.0D, maxDensity);
        trimCells();
        spawnBurst(level, cell, amount);
    }

    private static void spawnBurst(ClientLevel level, SmokeCell cell, double amount) {
        int count = reduceForGraphics(Minecraft.getInstance(), (int) Math.ceil(2.0D + amount * 5.0D));
        count = Math.min(count, reserveParticleBudget(count));
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            spawnSmokeParticle(level, random, cell, true);
        }
    }

    private static void spawnAmbientSmoke(ClientLevel level, Player player, SmokeCell cell) {
        if (player.distanceToSqr(cell.center) > MAX_VISIBLE_DISTANCE_SQR) {
            return;
        }
        RandomSource random = level.random;
        double chance = Mth.clamp(cell.density * (cell.indoor ? 0.36D : 0.12D), 0.0D, 0.72D);
        if (random.nextDouble() > chance) {
            return;
        }
        int count = reduceForGraphics(Minecraft.getInstance(), 1 + (cell.density > 0.68D ? 1 : 0));
        count = Math.min(count, reserveParticleBudget(count));
        for (int i = 0; i < count; i++) {
            spawnSmokeParticle(level, random, cell, false);
        }
    }

    private static void spawnSmokeParticle(ClientLevel level, RandomSource random, SmokeCell cell, boolean fresh) {
        Vec3 position = cell.center.add(randomSpread(random, fresh ? 1.2D : 2.4D)).add(0.0D, random.nextDouble() * 1.2D - 0.25D, 0.0D);
        Vec3 drift = new Vec3(
                Math.sin((level.getGameTime() + cell.spreadOffset) * 0.05D) * 0.006D,
                0.003D + random.nextDouble() * 0.008D,
                Math.cos((level.getGameTime() + cell.spreadOffset) * 0.047D) * 0.006D
        ).add(randomSpread(random, 0.006D));
        int lifetime = Mth.clamp((int) Math.round((fresh ? 18 : 26) * (cell.indoor ? 1.3D : 0.55D)), 8, 45);
        ClientMuzzleSmokeRenderer.spawnAmbient(position, drift, (float) (0.16D + cell.density * 0.22D), lifetime, random);
    }

    private static void collectSpreadRequests(ClientLevel level, SmokeCell cell, List<SpreadRequest> requests) {
        if (!cell.indoor || cell.density < 0.18D) {
            return;
        }
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction direction : directions) {
            BlockPos target = BlockPos.containing(cell.center).relative(direction, CELL_SIZE);
            double enclosure = enclosureScore(level, target);
            if (enclosure < 0.28D) {
                continue;
            }
            requests.add(new SpreadRequest(cell, Vec3.atCenterOf(target), cell.density * 0.035D, enclosure >= 0.38D, enclosure));
        }
    }

    private static void renderHaze(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, SmokeCell cell) {
        float density = (float) Mth.clamp(cell.density / Math.max(0.1D, BallisticBlockConfig.MAX_INDOOR_SMOKE_DENSITY.get()), 0.0D, 1.0D);
        if (density <= 0.035F) {
            return;
        }
        float light = Mth.clamp(cell.lightBoost, 0.0F, 1.6F);
        float alpha = Mth.clamp(0.018F + density * 0.045F + light * 0.012F, 0.0F, 0.09F);
        if (isReducedGraphics(Minecraft.getInstance())) {
            alpha *= 0.55F;
        }
        if (ClientGasMaskHandler.hasSmokeProtection()) {
            alpha *= 0.35F;
        }
        float gray = Mth.clamp(0.58F + light * 0.06F, 0.0F, 0.72F);
        Vec3 center = cell.center.subtract(cameraPos);
        float radius = (float) (1.25D + density * 1.35D);
        addQuad(buffer, matrix, center, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D), radius, gray, gray, gray, alpha);
        addQuad(buffer, matrix, center, new Vec3(0.0D, 0.0D, 1.0D), new Vec3(0.0D, 1.0D, 0.0D), radius * 0.85F, gray, gray, gray, alpha * 0.72F);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Vec3 tangent, Vec3 bitangent, float radius, float red, float green, float blue, float alpha) {
        Vec3 tx = tangent.scale(radius);
        Vec3 by = bitangent.scale(radius);
        vertex(buffer, matrix, center.subtract(tx).subtract(by), red, green, blue, 0.0F);
        vertex(buffer, matrix, center.add(tx).subtract(by), red, green, blue, 0.0F);
        vertex(buffer, matrix, center.add(tx.scale(0.18D)).add(by.scale(0.18D)), red, green, blue, alpha);
        vertex(buffer, matrix, center.subtract(tx).add(by), red, green, blue, 0.0F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }

    private static BlockPos cellOrigin(Vec3 position) {
        int x = Mth.floor(position.x / CELL_SIZE) * CELL_SIZE;
        int y = Mth.floor(position.y / CELL_SIZE) * CELL_SIZE;
        int z = Mth.floor(position.z / CELL_SIZE) * CELL_SIZE;
        return new BlockPos(x, y, z);
    }

    private static Vec3 centerOf(BlockPos origin) {
        return new Vec3(origin.getX() + CELL_SIZE * 0.5D, origin.getY() + CELL_SIZE * 0.5D, origin.getZ() + CELL_SIZE * 0.5D);
    }

    private static double enclosureScore(ClientLevel level, BlockPos pos) {
        int blocked = 0;
        int samples = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= 5; distance++) {
                mutable.set(pos).move(direction, distance);
                samples++;
                BlockState state = level.getBlockState(mutable);
                if (!state.isAir() && state.isSolidRender(level, mutable)) {
                    blocked++;
                    break;
                }
            }
        }
        double blockedScore = samples == 0 ? 0.0D : blocked / 6.0D;
        if (!level.canSeeSky(pos)) {
            blockedScore += 0.22D;
        }
        if (level.getMaxLocalRawBrightness(pos) < 12) {
            blockedScore += 0.12D;
        }
        return Mth.clamp(blockedScore, 0.0D, 1.0D);
    }

    private static int reserveParticleBudget(int requested) {
        if (requested <= 0) {
            return 0;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != particleLimiterSecond) {
            particleLimiterSecond = currentSecond;
            particlesThisSecond = 0;
        }
        int available = Math.max(0, MAX_SMOKE_PARTICLES_PER_SECOND - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static int reduceForGraphics(Minecraft minecraft, int amount) {
        if (amount <= 1) {
            return amount;
        }
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(java.util.Locale.ROOT);
        if (particles.contains("minimal")) {
            return Math.max(1, amount / 3);
        }
        if (particles.contains("decreased") || graphics.contains("fast")) {
            return Math.max(1, amount / 2);
        }
        return amount;
    }

    private static boolean isReducedGraphics(Minecraft minecraft) {
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(java.util.Locale.ROOT);
        return particles.contains("minimal") || particles.contains("decreased") || graphics.contains("fast");
    }

    private static void trimCells() {
        while (CELLS.size() > MAX_CELLS && !CELL_ORDER.isEmpty()) {
            Long oldest = CELL_ORDER.removeFirst();
            CELLS.remove(oldest);
        }
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private static final class SmokeCell {
        private final Vec3 center;
        private boolean indoor;
        private double enclosure;
        private double density;
        private final int spreadOffset;
        private float lightBoost;
        private float lightRed = 1.0F;
        private float lightGreen = 0.62F;
        private float lightBlue = 0.18F;
        private long lastLightMillis;

        private SmokeCell(Vec3 center, boolean indoor, double enclosure, int spreadOffset) {
            this.center = center;
            this.indoor = indoor;
            this.enclosure = enclosure;
            this.spreadOffset = spreadOffset;
        }
    }

    private record SpreadRequest(SmokeCell source, Vec3 position, double amount, boolean indoor, double enclosure) {
    }
}
