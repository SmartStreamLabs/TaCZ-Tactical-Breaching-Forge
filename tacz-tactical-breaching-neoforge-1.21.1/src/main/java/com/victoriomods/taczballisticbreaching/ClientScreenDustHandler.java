package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientScreenDustHandler {
    private static long limiterSecond = -1L;
    private static int triggersThisSecond;

    private ClientScreenDustHandler() {
    }

    public static void trigger(ImpactFxPacket packet, Vec3 impact, Player player) {
        if (!BallisticBlockConfig.ENABLE_SCREEN_DUST.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || player == null) {
            return;
        }

        double radius = BallisticBlockConfig.SCREEN_DUST_RADIUS.get();
        double distance = player.getEyePosition().distanceTo(impact);
        if (distance > radius || !reserveTrigger()) {
            return;
        }

        RandomSource random = level.random;
        ImpactMaterial material = ImpactMaterial.byId(packet.materialId());
        double closeness = 1.0D - Mth.clamp(distance / radius, 0.0D, 1.0D);
        double amount = BallisticBlockConfig.SCREEN_DUST_INTENSITY.get() * (0.35D + closeness * 0.85D);
        if (packet.highCaliberImpact() || packet.finalBreak()) {
            amount *= 1.45D;
        }
        if (isIndoor(level, player)) {
            amount *= BallisticBlockConfig.SCREEN_DUST_INDOOR_MULTIPLIER.get();
        }
        if (isReducedGraphics(minecraft)) {
            amount *= 0.55D;
        }

        Vec3 screenSide = screenSide(player, impact);
        spawnCameraDebris(level, random, player, material, packet, closeness, screenSide, amount);
        if ((packet.highCaliberImpact() || packet.finalBreak()) && distance < radius * 0.45D) {
            ClientCameraShakeHandler.closeImpact(impact, packet.highCaliberImpact() || packet.finalBreak());
        }
    }

    public static void tick() {
    }

    public static void clear() {
        limiterSecond = -1L;
        triggersThisSecond = 0;
    }

    private static boolean reserveTrigger() {
        int maxPerSecond = BallisticBlockConfig.SCREEN_DUST_MAX_TRIGGERS_PER_SECOND.get();
        if (maxPerSecond <= 0) {
            return false;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            triggersThisSecond = 0;
        }
        if (triggersThisSecond >= maxPerSecond) {
            return false;
        }
        triggersThisSecond++;
        return true;
    }

    private static void spawnCameraDebris(ClientLevel level, RandomSource random, Player player, ImpactMaterial material, ImpactFxPacket packet, double closeness, Vec3 screenSide, double intensity) {
        int count = Math.max(1, (int) Math.ceil((2 + closeness * 5) * intensity * (packet.highCaliberImpact() || packet.finalBreak() ? 1.25D : 1.0D)));
        count = Math.min(count, 10);
        if (material == ImpactMaterial.METAL) {
            count = Math.max(1, count / 3);
        }
        if (material == ImpactMaterial.GLASS) {
            count = Math.max(1, count / 2);
        }
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(look).normalize();
        Vec3 edgeDirection = normalizeScreenSide(screenSide, random);
        Vec3 base = player.getEyePosition()
                .add(look.scale(0.34D + random.nextDouble() * 0.1D))
                .add(right.scale(edgeDirection.x * 0.34D))
                .add(up.scale(edgeDirection.y * 0.22D));
        for (int i = 0; i < count; i++) {
            Vec3 position = base
                    .add(right.scale((random.nextDouble() - 0.5D) * 0.18D))
                    .add(up.scale((random.nextDouble() - 0.5D) * 0.14D));
            Vec3 velocity = look.scale(-0.018D - random.nextDouble() * 0.02D)
                    .add(right.scale(-edgeDirection.x * (0.012D + random.nextDouble() * 0.02D)))
                    .add(up.scale(-edgeDirection.y * (0.008D + random.nextDouble() * 0.016D)))
                    .add(randomSpread(random, 0.01D));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(particleFor(material), position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.scale(particleScale(material, random));
                particle.setLifetime(particleLifetime(material, random));
                tintParticle(particle, material, random);
            }
        }
    }

    private static net.minecraft.core.particles.ParticleOptions particleFor(ImpactMaterial material) {
        return switch (material) {
            case GLASS -> new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_PANE));
            case METAL -> new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.18F), 0.42F);
            case WOOD -> new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.OAK_PLANKS));
            case STONE -> new DustParticleOptions(new Vector3f(0.43F, 0.42F, 0.39F), 0.62F);
            default -> new DustParticleOptions(new Vector3f(0.38F, 0.36F, 0.32F), 0.52F);
        };
    }

    private static void tintParticle(Particle particle, ImpactMaterial material, RandomSource random) {
        switch (material) {
            case WOOD -> particle.setColor(0.55F + random.nextFloat() * 0.16F, 0.36F + random.nextFloat() * 0.12F, 0.18F);
            case GLASS -> particle.setColor(0.78F, 0.92F, 1.0F);
            case METAL -> particle.setColor(1.0F, 0.72F + random.nextFloat() * 0.18F, 0.18F);
            case STONE -> {
                float grey = 0.38F + random.nextFloat() * 0.16F;
                particle.setColor(grey, grey, grey);
            }
            default -> {
            }
        }
    }

    private static Vec3 screenSide(Player player, Vec3 impact) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(look).normalize();
        Vec3 toImpact = impact.subtract(player.getEyePosition()).normalize();
        return new Vec3(Mth.clamp(toImpact.dot(right), -1.0D, 1.0D), Mth.clamp(toImpact.dot(up), -1.0D, 1.0D), 0.0D);
    }

    private static Vec3 normalizeScreenSide(Vec3 side, RandomSource random) {
        double x = side.x;
        double y = side.y;
        if (Math.abs(x) + Math.abs(y) < 0.18D) {
            x = random.nextBoolean() ? 1.0D : -1.0D;
            y = (random.nextDouble() - 0.5D) * 0.65D;
        }
        Vec3 normalized = new Vec3(x, y, 0.0D);
        return normalized.lengthSqr() < 0.001D ? new Vec3(1.0D, 0.0D, 0.0D) : normalized.normalize();
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private static float particleScale(ImpactMaterial material, RandomSource random) {
        return switch (material) {
            case GLASS -> 0.32F + random.nextFloat() * 0.28F;
            case METAL -> 0.26F + random.nextFloat() * 0.22F;
            case WOOD -> 0.42F + random.nextFloat() * 0.42F;
            case STONE -> 0.38F + random.nextFloat() * 0.36F;
            default -> 0.34F + random.nextFloat() * 0.32F;
        };
    }

    private static int particleLifetime(ImpactMaterial material, RandomSource random) {
        return switch (material) {
            case METAL -> 3 + random.nextInt(4);
            case GLASS -> 5 + random.nextInt(6);
            default -> 7 + random.nextInt(8);
        };
    }

    private static boolean isIndoor(ClientLevel level, Player player) {
        BlockPos pos = player.blockPosition();
        return !level.canSeeSky(pos) && level.getMaxLocalRawBrightness(pos) < 12;
    }

    private static boolean isReducedGraphics(Minecraft minecraft) {
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(java.util.Locale.ROOT);
        return particles.contains("minimal") || particles.contains("decreased") || graphics.contains("fast");
    }

}
