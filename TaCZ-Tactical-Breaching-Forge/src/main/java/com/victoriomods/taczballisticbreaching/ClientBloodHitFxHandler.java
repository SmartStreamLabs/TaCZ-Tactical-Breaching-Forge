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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientBloodHitFxHandler {
    private static final ResourceLocation BLOOD_DECAL_TEXTURE = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/effect/blood_decal_atlas.png");
    private static final double MAX_VISIBLE_DISTANCE_SQR = 64.0D * 64.0D;
    private static final double MAX_DECAL_RENDER_DISTANCE_SQR = 72.0D * 72.0D;
    private static final double SURFACE_OFFSET = 0.0035D;
    private static final int MAX_MIST_PARTICLES_PER_SECOND = 180;
    private static final int MAX_ACTIVE_DROPLETS = 192;
    private static final double DROPLET_GRAVITY = 0.032D;
    private static final double DROPLET_DRAG = 0.86D;
    private static final Map<ChunkPos, ArrayDeque<BloodDecal>> DECALS = new HashMap<>();
    private static final ArrayDeque<BloodDroplet> DROPLETS = new ArrayDeque<>();
    private static long particleLimiterSecond = -1L;
    private static int particlesThisSecond;
    private static long lastTrailScanTick = -1L;

    private ClientBloodHitFxHandler() {
    }

    public static void handle(BloodHitFxPacket packet) {
        if (!BallisticBlockConfig.BLOOD_EFFECTS_ENABLED.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        Vec3 impact = new Vec3(packet.x(), packet.y(), packet.z());
        if (player.distanceToSqr(impact) > MAX_VISIBLE_DISTANCE_SQR) {
            return;
        }

        Vec3 direction = new Vec3(packet.dirX(), packet.dirY(), packet.dirZ());
        direction = direction.lengthSqr() < 0.001D ? player.getLookAngle() : direction.normalize();
        RandomSource random = level.random;
        double bloodScale = bloodAmountScale(level, impact, packet);
        if (bloodScale <= 0.01D) {
            return;
        }
        if (BallisticBlockConfig.ENABLE_BLOOD_MIST.get()) {
            spawnMist(level, random, impact, direction, packet, bloodScale);
            if (BallisticBlockConfig.BLOOD_PHYSICS_ENABLED.get()) {
                spawnPhysicsDroplets(level, random, impact, direction, packet, bloodScale);
            } else {
                spawnFallingDroplets(level, random, impact, direction, packet, bloodScale);
            }
        }
        if (BallisticBlockConfig.ENABLE_WALL_SPLATTER.get() && random.nextDouble() < splatterChance(level, impact, packet)) {
            addWallSplatter(level, random, impact, direction, packet);
        }
        if (BallisticBlockConfig.ENABLE_WALL_SPLATTER.get()) {
            addGroundSplatter(level, random, impact, direction, packet);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.BLOOD_EFFECTS_ENABLED.get() || (DECALS.isEmpty() && DROPLETS.isEmpty())) {
            return;
        }

        long now = System.currentTimeMillis();
        cleanup(Minecraft.getInstance().level, now);
        Vec3 cameraPos = camera.getPosition();

        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, BLOOD_DECAL_TEXTURE);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        if (BallisticBlockConfig.ENABLE_WALL_SPLATTER.get()) {
            for (ArrayDeque<BloodDecal> chunkDecals : DECALS.values()) {
                for (BloodDecal decal : chunkDecals) {
                    if (decal.position.distanceToSqr(cameraPos) > MAX_DECAL_RENDER_DISTANCE_SQR) {
                        continue;
                    }
                    if (!ClientEffectRenderSafety.canRenderAt(cameraPos, decal.position)) {
                        continue;
                    }
                    renderDecal(buffer, matrix, cameraPos, decal);
                }
            }
        }
        if (BallisticBlockConfig.BLOOD_PHYSICS_ENABLED.get()) {
            for (BloodDroplet droplet : DROPLETS) {
                if (droplet.position.distanceToSqr(cameraPos) > MAX_VISIBLE_DISTANCE_SQR) {
                    continue;
                }
                if (!ClientEffectRenderSafety.canRenderAt(cameraPos, droplet.position)) {
                    continue;
                }
                renderDroplet(buffer, matrix, cameraPos, droplet);
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    public static void clear() {
        DECALS.clear();
        DROPLETS.clear();
        particleLimiterSecond = -1L;
        particlesThisSecond = 0;
        lastTrailScanTick = -1L;
    }

    public static void tick() {
        if (!BallisticBlockConfig.BLOOD_EFFECTS_ENABLED.get()) {
            clear();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            clear();
            return;
        }
        if (BallisticBlockConfig.BLOOD_PHYSICS_ENABLED.get()) {
            tickDroplets(level, player);
        } else {
            DROPLETS.clear();
        }
        if (BallisticBlockConfig.BLOOD_TRAILS_ENABLED.get() && BallisticBlockConfig.BLOOD_PHYSICS_ENABLED.get()) {
            tickBloodTrails(level, player);
        }
    }

    private static void spawnMist(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet, double intensity) {
        if (intensity <= 0.01D) {
            return;
        }
        int requested = (int) Math.ceil(28.0D * intensity);
        int cap = BallisticBlockConfig.MAX_BLOOD_PARTICLES_PER_IMPACT.get();
        int count = reserveParticles(Math.min(Math.min(Math.max(8, requested), packet.shotgun() ? 42 : 30), cap));
        if (count <= 0) {
            return;
        }

        DustParticleOptions bloodDust = new DustParticleOptions(new Vector3f(0.62F, 0.018F, 0.014F), 0.92F);
        for (int i = 0; i < count; i++) {
            Vec3 spread = randomSpread(random, packet.shotgun() ? 0.16D : 0.085D);
            Vec3 velocity = direction.scale(0.055D + random.nextDouble() * 0.105D).add(spread).add(0.0D, 0.005D + random.nextDouble() * 0.018D, 0.0D);
            Vec3 position = impact.add(randomSpread(random, 0.105D));
            if (i % 4 == 0) {
                Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
                if (particle != null) {
                    particle.setColor(0.52F + random.nextFloat() * 0.16F, 0.018F, 0.014F);
                    particle.scale(0.48F + random.nextFloat() * 0.32F);
                    particle.setLifetime(8 + random.nextInt(10));
                }
            } else {
                level.addParticle(bloodDust, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }

    private static void spawnPhysicsDroplets(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet, double intensity) {
        int cap = Math.min(BallisticBlockConfig.MAX_BLOOD_PARTICLES_PER_HIT.get(), BallisticBlockConfig.MAX_BLOOD_PARTICLES_PER_IMPACT.get());
        int requested = (int) Math.ceil((packet.shotgun() ? 14.0D : 9.0D) * intensity);
        int count = reserveParticles(Math.min(Math.max(1, requested), Math.max(0, cap)));
        if (count <= 0) {
            return;
        }

        double speedBoost = packet.highCaliber() ? 1.22D : 1.0D;
        if (packet.shotgun()) {
            speedBoost *= 1.18D;
        }
        for (int i = 0; i < count; i++) {
            Vec3 lateral = randomSpread(random, packet.shotgun() ? 0.105D : 0.06D);
            Vec3 velocity = direction.scale((0.08D + random.nextDouble() * 0.13D) * speedBoost)
                    .add(lateral)
                    .add(0.0D, 0.035D + random.nextDouble() * 0.08D, 0.0D);
            Vec3 position = impact.add(randomSpread(random, 0.095D));
            double size = (packet.highCaliber() ? 0.055D : 0.038D) + random.nextDouble() * 0.035D;
            if (packet.shotgun()) {
                size *= 0.82D;
            }
            addDroplet(new BloodDroplet(position, velocity, size, 0, BallisticBlockConfig.BLOOD_LIFETIME_TICKS.get(), packet.highCaliber(), packet.shotgun()));
        }
    }

    private static void spawnFallingDroplets(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet, double intensity) {
        int cap = Math.max(0, BallisticBlockConfig.MAX_BLOOD_PARTICLES_PER_IMPACT.get() / 2);
        int count = reserveParticles(Math.min(Math.min(Math.max(5, (int) Math.ceil(18.0D * intensity)), packet.shotgun() ? 28 : 18), cap));
        if (count <= 0) {
            return;
        }

        DustParticleOptions droplet = new DustParticleOptions(new Vector3f(0.52F, 0.0F, 0.0F), 1.05F);
        for (int i = 0; i < count; i++) {
            Vec3 outward = direction.scale(0.018D + random.nextDouble() * 0.05D);
            Vec3 lateral = randomSpread(random, packet.shotgun() ? 0.10D : 0.055D);
            Vec3 velocity = outward.add(lateral).add(0.0D, -0.085D - random.nextDouble() * 0.12D, 0.0D);
            Vec3 position = impact.add(randomSpread(random, 0.12D)).add(0.0D, random.nextDouble() * 0.12D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(droplet, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setColor(0.48F + random.nextFloat() * 0.18F, 0.0F, 0.0F);
                particle.scale(0.72F + random.nextFloat() * 0.42F);
                particle.setLifetime(16 + random.nextInt(12));
            }
        }
    }

    private static void addWallSplatter(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet) {
        int maxPerChunk = BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get();
        if (maxPerChunk <= 0 || packet.armorReduction() < 0.18F) {
            return;
        }

        Vec3 start = impact.add(direction.scale(0.20D));
        Vec3 end = impact.add(direction.scale(packet.highCaliber() || packet.shotgun() ? 4.6D : 3.2D));
        BlockHitResult result = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (result.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();
        Vec3 position = result.getLocation().add(normal.scale(SURFACE_OFFSET));
        Basis basis = Basis.fromNormal(normal);
        double size = 0.18D + random.nextDouble() * 0.12D;
        if (packet.highCaliber()) {
            size *= 1.3D;
        }
        if (packet.shotgun()) {
            size *= 1.18D;
        }
        size *= Math.max(0.45D, packet.armorReduction());

        add(new BloodDecal(
                position.add(randomSurfaceOffset(random, basis, 0.045D)),
                backingBlockPos(position, normal),
                normal,
                size,
                random.nextFloat() * ((float) Math.PI * 2.0F),
                System.currentTimeMillis(),
                2 + random.nextInt(packet.shotgun() ? 4 : 3),
                packet.highCaliber(),
                packet.shotgun()
        ));
    }

    private static void addGroundSplatter(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet) {
        int maxPerChunk = BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get();
        if (maxPerChunk <= 0 || packet.armorReduction() < 0.16F) {
            return;
        }

        Vec3 start = impact.add(direction.scale(0.18D)).add(0.0D, 0.12D, 0.0D);
        Vec3 end = start.add(direction.scale(packet.shotgun() ? 0.55D : 0.32D)).add(0.0D, -3.2D, 0.0D);
        BlockHitResult result = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (result.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();
        if (normal.y < 0.45D) {
            return;
        }
        Vec3 position = result.getLocation().add(normal.scale(SURFACE_OFFSET));
        Basis basis = Basis.fromNormal(normal);
        double size = 0.22D + random.nextDouble() * 0.18D;
        if (packet.highCaliber()) {
            size *= 1.25D;
        }
        if (packet.shotgun()) {
            size *= 1.45D;
        }
        size *= Math.max(0.42D, packet.armorReduction());

        add(new BloodDecal(
                position.add(randomSurfaceOffset(random, basis, packet.shotgun() ? 0.16D : 0.09D)),
                backingBlockPos(position, normal),
                normal,
                size,
                random.nextFloat() * ((float) Math.PI * 2.0F),
                System.currentTimeMillis(),
                5 + random.nextInt(packet.shotgun() ? 8 : 5),
                packet.highCaliber(),
                packet.shotgun()
        ));
    }

    private static double splatterChance(ClientLevel level, Vec3 impact, BloodHitFxPacket packet) {
        double chance = BallisticBlockConfig.BLOOD_SPLATTER_CHANCE.get() * packet.armorReduction();
        if (packet.highCaliber()) {
            chance *= 1.35D;
        }
        if (packet.shotgun()) {
            chance *= 1.45D;
        }
        if (packet.armorPiercing() && packet.armored()) {
            chance *= 0.72D;
        }
        if (isIndoor(level, impact)) {
            chance *= 1.12D;
        }
        return Math.max(0.0D, Math.min(0.85D, chance));
    }

    private static void add(BloodDecal decal) {
        ChunkPos chunkPos = new ChunkPos((int) Math.floor(decal.position.x) >> 4, (int) Math.floor(decal.position.z) >> 4);
        ArrayDeque<BloodDecal> chunkDecals = DECALS.computeIfAbsent(chunkPos, ignored -> new ArrayDeque<>());
        chunkDecals.addLast(decal);
        int maxPerChunk = BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get();
        while (chunkDecals.size() > maxPerChunk) {
            chunkDecals.removeFirst();
        }
    }

    private static void addDroplet(BloodDroplet droplet) {
        if (droplet.lifetimeTicks <= 0 || droplet.size <= 0.0D) {
            return;
        }
        DROPLETS.addLast(droplet);
        while (DROPLETS.size() > MAX_ACTIVE_DROPLETS) {
            DROPLETS.removeFirst();
        }
    }

    private static void tickDroplets(ClientLevel level, Player player) {
        if (DROPLETS.isEmpty()) {
            return;
        }
        Iterator<BloodDroplet> iterator = DROPLETS.iterator();
        while (iterator.hasNext()) {
            BloodDroplet droplet = iterator.next();
            if (++droplet.ageTicks > droplet.lifetimeTicks || player.distanceToSqr(droplet.position) > MAX_VISIBLE_DISTANCE_SQR) {
                iterator.remove();
                continue;
            }
            BlockPos pos = BlockPos.containing(droplet.position);
            if (!level.hasChunkAt(pos)) {
                iterator.remove();
                continue;
            }

            Vec3 previous = droplet.position;
            Vec3 next = previous.add(droplet.velocity);
            BlockHitResult hit = level.clip(new ClipContext(previous, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                addDropletImpactDecal(level, hit, droplet);
                iterator.remove();
                continue;
            }
            droplet.position = next;
            droplet.velocity = droplet.velocity.scale(DROPLET_DRAG).add(0.0D, -DROPLET_GRAVITY, 0.0D);
        }
    }

    private static void addDropletImpactDecal(ClientLevel level, BlockHitResult hit, BloodDroplet droplet) {
        if (!BallisticBlockConfig.ENABLE_WALL_SPLATTER.get() || BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get() <= 0) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal()).normalize();
        Vec3 position = hit.getLocation().add(normal.scale(SURFACE_OFFSET));
        Basis basis = Basis.fromNormal(normal);
        RandomSource random = level.random;
        double size = Mth.clamp(droplet.size * (1.15D + random.nextDouble() * 1.35D), 0.035D, droplet.highCaliber ? 0.22D : 0.16D);
        add(new BloodDecal(
                position.add(randomSurfaceOffset(random, basis, 0.025D)),
                backingBlockPos(position, normal),
                normal,
                size,
                random.nextFloat() * ((float) Math.PI * 2.0F),
                System.currentTimeMillis(),
                droplet.shotgun ? 2 : random.nextInt(2),
                droplet.highCaliber,
                droplet.shotgun
        ));
    }

    private static void tickBloodTrails(ClientLevel level, Player player) {
        long gameTime = level.getGameTime();
        if (gameTime - lastTrailScanTick < 10L) {
            return;
        }
        lastTrailScanTick = gameTime;
        int checked = 0;
        int emitted = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(18.0D), entity -> entity.isAlive() && entity.getMaxHealth() > 0.0F)) {
            if (checked++ >= 8 || emitted >= 2) {
                break;
            }
            if (entity.getHealth() / entity.getMaxHealth() > 0.32F) {
                continue;
            }
            Vec3 movement = entity.getDeltaMovement();
            if (movement.lengthSqr() < 0.0004D || level.random.nextFloat() > 0.35F) {
                continue;
            }
            Vec3 position = entity.position().add(0.0D, 0.18D, 0.0D);
            Vec3 velocity = movement.scale(0.035D).add(randomSpread(level.random, 0.018D)).add(0.0D, -0.025D, 0.0D);
            addDroplet(new BloodDroplet(position, velocity, 0.032D + level.random.nextDouble() * 0.018D, 0, Math.min(BallisticBlockConfig.BLOOD_LIFETIME_TICKS.get(), 240), false, false));
            emitted++;
        }
    }

    private static void cleanup(ClientLevel level, long now) {
        long lifetimeMillis = bloodLifetimeMillis();
        Iterator<Map.Entry<ChunkPos, ArrayDeque<BloodDecal>>> mapIterator = DECALS.entrySet().iterator();
        while (mapIterator.hasNext()) {
            ArrayDeque<BloodDecal> chunkDecals = mapIterator.next().getValue();
            while (!chunkDecals.isEmpty() && now - chunkDecals.peekFirst().createdMillis > lifetimeMillis) {
                chunkDecals.removeFirst();
            }
            chunkDecals.removeIf(decal -> !hasBackingBlock(level, decal));
            if (chunkDecals.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static long bloodLifetimeMillis() {
        long tickLifetime = Math.max(1L, BallisticBlockConfig.BLOOD_LIFETIME_TICKS.get()) * 50L;
        long legacyLifetime = Math.max(1L, BallisticBlockConfig.BLOOD_DECAL_LIFETIME_SECONDS.get()) * 1000L;
        return Math.min(tickLifetime, legacyLifetime);
    }

    private static boolean hasBackingBlock(ClientLevel level, BloodDecal decal) {
        if (level == null || !level.hasChunkAt(decal.backingPos)) {
            return false;
        }
        return !level.getBlockState(decal.backingPos).isAir();
    }

    private static void renderDecal(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BloodDecal decal) {
        float alpha = fadeAlpha(decal);
        if (alpha <= 0.01F) {
            return;
        }
        Basis basis = Basis.fromNormal(decal.normal).rotated(decal.rotation);
        float r = decal.highCaliber ? 0.34F : 0.28F;
        float g = 0.006F;
        float b = 0.004F;
        addTexturedQuad(buffer, matrix, cameraPos, decal.position, basis, decal.size, decal.size * (0.72D + decal.droplets * 0.04D), r, g, b, 0.82F * alpha, 0.0F, 0.0F, 0.5F, 1.0F);
        for (int i = 0; i < decal.droplets; i++) {
            double angle = decal.rotation + i * 1.77D;
            double distance = decal.size * (0.42D + (i % 3) * 0.18D);
            Vec3 offset = basis.tangent.scale(Math.cos(angle) * distance).add(basis.bitangent.scale(Math.sin(angle) * distance));
            double dropletSize = decal.size * (decal.shotgun ? 0.28D : 0.23D) * (0.75D + (i % 2) * 0.24D);
            addTexturedQuad(buffer, matrix, cameraPos, decal.position.add(offset), basis.rotated((float) angle), dropletSize, dropletSize * 0.78D, r * 0.95F, g, b, 0.62F * alpha, 0.5F, 0.0F, 1.0F, 1.0F);
        }
    }

    private static void renderDroplet(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BloodDroplet droplet) {
        double lifetime = Math.max(1.0D, droplet.lifetimeTicks);
        float alpha = (float) Mth.clamp(1.0D - droplet.ageTicks / lifetime, 0.0D, 1.0D);
        if (alpha <= 0.02F) {
            return;
        }
        Vec3 toCamera = cameraPos.subtract(droplet.position);
        if (toCamera.lengthSqr() < 1.0E-6D) {
            toCamera = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            toCamera = toCamera.normalize();
        }
        Vec3 up = Math.abs(toCamera.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 tangent = up.cross(toCamera).normalize();
        Vec3 bitangent = toCamera.cross(tangent).normalize();
        double size = droplet.size * (1.0D + Math.min(0.9D, droplet.velocity.length() * 3.0D));
        float r = droplet.highCaliber ? 0.42F : 0.34F;
        addTexturedQuad(buffer, matrix, cameraPos, droplet.position, new Basis(tangent, bitangent), size * 2.0D, size * 1.45D, r, 0.0F, 0.0F, 0.76F * alpha, 0.5F, 0.0F, 1.0F, 1.0F);
    }

    private static float fadeAlpha(BloodDecal decal) {
        double lifetime = Math.max(50.0D, bloodLifetimeMillis());
        double age = System.currentTimeMillis() - decal.createdMillis;
        double normalizedAge = Math.max(0.0D, Math.min(1.0D, age / lifetime));
        if (normalizedAge <= 0.12D) {
            return 1.0F;
        }
        double fadeProgress = (normalizedAge - 0.12D) / 0.88D;
        return (float) Math.max(0.0D, Math.pow(1.0D - fadeProgress, 1.18D));
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 center, Basis basis, double width, double height, float r, float g, float b, float a) {
        addTexturedQuad(buffer, matrix, cameraPos, center, basis, width, height, r, g, b, a, 0.0F, 0.0F, 0.5F, 1.0F);
    }

    private static void addTexturedQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 center, Basis basis, double width, double height, float r, float g, float b, float a, float u0, float v0, float u1, float v1) {
        Vec3 c = center.subtract(cameraPos);
        Vec3 tx = basis.tangent.scale(width * 0.5D);
        Vec3 by = basis.bitangent.scale(height * 0.5D);
        vertex(buffer, matrix, c.subtract(tx).subtract(by), r, g, b, a, u0, v1);
        vertex(buffer, matrix, c.add(tx).subtract(by), r, g, b, a, u1, v1);
        vertex(buffer, matrix, c.add(tx).add(by), r, g, b, a, u1, v0);
        vertex(buffer, matrix, c.subtract(tx).add(by), r, g, b, a, u0, v0);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        vertex(buffer, matrix, position, r, g, b, a, 0.0F, 0.0F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a, float u, float v) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(r, g, b, a).uv(u, v).endVertex();
    }

    private static double bloodAmountScale(ClientLevel level, Vec3 impact, BloodHitFxPacket packet) {
        double scale = BallisticBlockConfig.BLOOD_MIST_INTENSITY.get() * BallisticBlockConfig.BLOOD_DENSITY.get() * packet.armorReduction();
        if (packet.damageAmount() > 0.0F) {
            scale *= Mth.clamp(packet.damageAmount() / 6.0D, 0.45D, 2.35D);
        }
        if (isIndoor(level, impact)) {
            scale *= 1.14D;
        }
        if (packet.highCaliber()) {
            scale *= 1.35D;
        }
        if (packet.shotgun()) {
            scale *= 1.45D;
        }
        if (packet.helmetHit()) {
            scale *= 0.82D;
        }
        if (packet.armored() && !packet.armorPiercing()) {
            scale *= 0.72D;
        }
        return Math.max(0.0D, scale);
    }

    private static int reserveParticles(int requested) {
        if (requested <= 0) {
            return 0;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != particleLimiterSecond) {
            particleLimiterSecond = currentSecond;
            particlesThisSecond = 0;
        }
        int available = Math.max(0, MAX_MIST_PARTICLES_PER_SECOND - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static boolean isIndoor(ClientLevel level, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        return level.hasChunkAt(pos) && !level.canSeeSky(pos);
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private static Vec3 randomSurfaceOffset(RandomSource random, Basis basis, double amount) {
        return basis.tangent.scale((random.nextDouble() - 0.5D) * amount)
                .add(basis.bitangent.scale((random.nextDouble() - 0.5D) * amount));
    }

    private static BlockPos backingBlockPos(Vec3 position, Vec3 normal) {
        return BlockPos.containing(position.subtract(normal.scale(0.03D)));
    }

    private record BloodDecal(Vec3 position, BlockPos backingPos, Vec3 normal, double size, float rotation, long createdMillis, int droplets, boolean highCaliber, boolean shotgun) {
    }

    private static final class BloodDroplet {
        private Vec3 position;
        private Vec3 velocity;
        private final double size;
        private int ageTicks;
        private final int lifetimeTicks;
        private final boolean highCaliber;
        private final boolean shotgun;

        private BloodDroplet(Vec3 position, Vec3 velocity, double size, int ageTicks, int lifetimeTicks, boolean highCaliber, boolean shotgun) {
            this.position = position;
            this.velocity = velocity;
            this.size = size;
            this.ageTicks = ageTicks;
            this.lifetimeTicks = lifetimeTicks;
            this.highCaliber = highCaliber;
            this.shotgun = shotgun;
        }
    }

    private record Basis(Vec3 tangent, Vec3 bitangent) {
        static Basis fromNormal(Vec3 normal) {
            Vec3 up = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 tangent = up.cross(normal).normalize();
            Vec3 bitangent = normal.cross(tangent).normalize();
            return new Basis(tangent, bitangent);
        }

        Basis rotated(float angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 rotatedTangent = tangent.scale(cos).add(bitangent.scale(sin));
            Vec3 rotatedBitangent = bitangent.scale(cos).subtract(tangent.scale(sin));
            return new Basis(rotatedTangent, rotatedBitangent);
        }
    }
}
