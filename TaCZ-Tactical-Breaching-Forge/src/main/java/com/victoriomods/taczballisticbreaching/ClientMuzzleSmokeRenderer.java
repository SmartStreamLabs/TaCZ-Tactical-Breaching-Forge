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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
public final class ClientMuzzleSmokeRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/effect/muzzle_smoke_atlas.png");
    private static final ArrayDeque<Wisp> WISPS = new ArrayDeque<>();
    private static final double MAX_DISTANCE_SQR = 64.0D * 64.0D;
    private static boolean loggedSpawn;
    private static boolean loggedRender;

    private ClientMuzzleSmokeRenderer() {
    }

    public static void spawn(Vec3 muzzle, Vec3 forward, int count, MuzzleSmokePacket packet, boolean indoor, RandomSource random) {
        int capacity = Math.min(BallisticBlockConfig.MAX_ACTIVE_MUZZLE_SMOKE.get(), 160);
        if (capacity == 0) return;
        int lifetime = Mth.clamp((int) (BallisticBlockConfig.MUZZLE_SMOKE_LIFETIME_TICKS.get() * (indoor ? 1.3D : 0.85D)), 5, 32);
        float scale = BallisticBlockConfig.MUZZLE_SMOKE_SCALE.get().floatValue();
        for (int i = 0; i < count; i++) {
            Vec3 origin = muzzle.add(forward.scale(0.11D + random.nextDouble() * 0.11D))
                    .add((random.nextDouble() - 0.5D) * 0.045D, (random.nextDouble() - 0.5D) * 0.045D, (random.nextDouble() - 0.5D) * 0.045D);
            double speed = 0.035D + random.nextDouble() * 0.025D;
            Vec3 velocity = forward.scale(speed).add(0.0D, 0.005D + random.nextDouble() * 0.008D, 0.0D);
            float kindScale = switch (MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind())) {
                case PISTOL -> 0.65F;
                case SHOTGUN -> 1.3F;
                case SNIPER -> 0.9F;
                case RIFLE -> 1.0F;
            };
            if (packet.suppressed()) kindScale *= 0.55F;
            WISPS.addLast(new Wisp(origin, velocity, scale * kindScale * (1.15F + random.nextFloat() * 0.45F),
                    random.nextFloat() * 6.28318F, (random.nextFloat() - 0.5F) * 0.065F,
                    random.nextInt(4), lifetime + random.nextInt(5), indoor));
        }
        while (WISPS.size() > capacity) WISPS.removeFirst();
        if (!loggedSpawn && count > 0) {
            loggedSpawn = true;
            TaczTacticalBreaching.LOGGER.info("Custom muzzle smoke received shot and created {} wisps", count);
        }
    }

    public static void spawnAmbient(Vec3 position, Vec3 velocity, float size, int life, RandomSource random) {
        int capacity = Math.min(BallisticBlockConfig.MAX_ACTIVE_MUZZLE_SMOKE.get(), 160);
        if (capacity == 0) return;
        WISPS.addLast(new Wisp(position, velocity, size, random.nextFloat() * 6.28318F,
                (random.nextFloat() - 0.5F) * 0.035F, random.nextInt(4), life, true));
        while (WISPS.size() > capacity) WISPS.removeFirst();
    }

    public static void tick() {
        Iterator<Wisp> iterator = WISPS.iterator();
        while (iterator.hasNext()) {
            Wisp wisp = iterator.next();
            if (++wisp.age >= wisp.life) {
                iterator.remove();
                continue;
            }
            double turbulence = BallisticBlockConfig.SMOKE_TURBULENCE.get();
            double phase = (wisp.age + wisp.rotation * 8.0D) * 0.28D;
            wisp.position = wisp.position.add(wisp.velocity)
                    .add(Math.sin(phase) * 0.0015D * turbulence, 0.002D, Math.cos(phase) * 0.0015D * turbulence);
            wisp.velocity = wisp.velocity.scale(wisp.indoor ? 0.88D : 0.83D);
            wisp.rotation += wisp.spin;
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (WISPS.isEmpty() || !BallisticBlockConfig.ENABLE_MUZZLE_SMOKE.get()) return;
        Vec3 cameraPos = camera.getPosition();
        Vector3f right = camera.rotation().transform(new Vector3f(1, 0, 0));
        Vector3f up = camera.rotation().transform(new Vector3f(0, 1, 0));
        Matrix4f matrix = poseStack.last().pose();
        if (!loggedRender) {
            loggedRender = true;
            TaczTacticalBreaching.LOGGER.info("Custom muzzle smoke rendering {} active wisps", WISPS.size());
        }
        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (Wisp wisp : WISPS) {
            double distance = wisp.position.distanceToSqr(cameraPos);
            if (distance > MAX_DISTANCE_SQR) continue;
            float progress = (float) wisp.age / wisp.life;
            float fade = Math.min(1.0F, progress * 10.0F) * (1.0F - progress);
            float alpha = Math.min(0.7F, fade * BallisticBlockConfig.SMOKE_OPACITY.get().floatValue() * 2.3F);
            if (distance < 2.25D) alpha *= BallisticBlockConfig.FIRST_PERSON_SMOKE_INTENSITY.get().floatValue();
            if (alpha < 0.003F) continue;
            float size = wisp.size * (0.75F + progress * 1.7F * BallisticBlockConfig.SMOKE_EXPANSION.get().floatValue());
            float c = Mth.cos(wisp.rotation) * size;
            float s = Mth.sin(wisp.rotation) * size;
            float x = (float) (wisp.position.x - cameraPos.x);
            float y = (float) (wisp.position.y - cameraPos.y);
            float z = (float) (wisp.position.z - cameraPos.z);
            float u0 = wisp.frame * 0.25F;
            float u1 = u0 + 0.25F;
            vertex(buffer, matrix, x, y, z, right, up, -c + s, -s - c, u0, 1, alpha);
            vertex(buffer, matrix, x, y, z, right, up, -c - s, -s + c, u0, 0, alpha);
            vertex(buffer, matrix, x, y, z, right, up, c - s, s + c, u1, 0, alpha);
            vertex(buffer, matrix, x, y, z, right, up, c + s, s - c, u1, 1, alpha);
        }
        BufferUploader.drawWithShader(buffer.end());
        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                               Vector3f right, Vector3f up, float a, float b, float u, float v, float alpha) {
        buffer.vertex(matrix, x + right.x * a + up.x * b, y + right.y * a + up.y * b, z + right.z * a + up.z * b)
                .color(0.48F, 0.48F, 0.47F, alpha).uv(u, v).endVertex();
    }

    public static void clear() {
        WISPS.clear();
    }

    private static final class Wisp {
        private Vec3 position;
        private Vec3 velocity;
        private final float size;
        private float rotation;
        private final float spin;
        private final int frame;
        private final int life;
        private final boolean indoor;
        private int age;

        private Wisp(Vec3 position, Vec3 velocity, float size, float rotation, float spin, int frame, int life, boolean indoor) {
            this.position = position;
            this.velocity = velocity;
            this.size = size;
            this.rotation = rotation;
            this.spin = spin;
            this.frame = frame;
            this.life = life;
            this.indoor = indoor;
        }
    }
}
