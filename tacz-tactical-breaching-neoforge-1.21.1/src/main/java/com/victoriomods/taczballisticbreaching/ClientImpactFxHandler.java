package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientImpactFxHandler {
    private static final double MAX_VISIBLE_DISTANCE_SQR = 64.0D * 64.0D;
    private static long limiterSecond = -1L;
    private static int particlesThisSecond;
    private static long ricochetLimiterSecond = -1L;
    private static int ricochetSoundsThisSecond;

    private ClientImpactFxHandler() {
    }

    public static void handle(ImpactFxPacket packet) {
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

        if (packet.finalBreak()) {
            BulletDecalManager.removeNear(impact, 0.8D);
        }
        BulletDecalManager.addFromImpact(packet);
        if (packet.penetrationExit() && !BallisticBlockConfig.ENABLE_PENETRATION_PARTICLES.get()) {
            return;
        }
        GlassShardDebrisManager.addFromImpact(packet);
        ClientScreenDustHandler.trigger(packet, impact, player);

        ImpactMaterial material = BallisticBlockConfig.MATERIAL_SPECIFIC_IMPACTS.get() ? ImpactMaterial.byId(packet.materialId()) : ImpactMaterial.GENERIC;
        ClientDynamicImpactSoundHandler.playBlockImpact(packet, material);
        PhysicalDebrisManager.addFromImpact(packet, material);
        BurnMarkManager.addFromImpact(packet, material);
        boolean metalImpact = material == ImpactMaterial.METAL;
        boolean hardImpact = material == ImpactMaterial.STONE;
        boolean woodImpact = material == ImpactMaterial.WOOD;
        if (packet.finalBreak()) {
            DynamicGunLightManager.addBreachingFlash(impact, material, packet.highCaliberImpact());
            ClientCameraShakeHandler.breaching(impact, packet.highCaliberImpact() || metalImpact);
        } else if (metalImpact) {
            DynamicGunLightManager.addSparkFlash(impact);
        }
        boolean smokeAllowed = BallisticBlockConfig.ENABLE_IMPACT_SMOKE.get()
                && (!BallisticBlockConfig.IMPACT_SMOKE_ONLY_ON_HARD_BLOCKS.get() || material == ImpactMaterial.STONE || material == ImpactMaterial.METAL);
        int smokeAmount = smokeAllowed ? BallisticBlockConfig.IMPACT_SMOKE_AMOUNT.get() : 0;
        int materialParticles = getMaterialParticleRequest(material, packet);
        int budget = reserveParticleBudget(smokeAmount + materialParticles);
        if (budget <= 0) {
            if (metalImpact || hardImpact) {
                playRicochet(level, level.random, player, impact, packet, material);
            }
            if (woodImpact) {
                playWoodImpact(level, level.random, impact, packet);
            }
            return;
        }

        RandomSource random = level.random;
        Vec3 normal = new Vec3(packet.normalX(), packet.normalY(), packet.normalZ());
        if (normal.lengthSqr() < 0.001D) {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }

        if (smokeAmount > 0) {
            int smokeToSpawn = Math.min(smokeAmount, budget);
            spawnSmoke(level, random, impact, normal, smokeToSpawn);
            budget -= smokeToSpawn;
        }
        if (metalImpact || hardImpact) {
            playRicochet(level, random, player, impact, packet, material);
        }
        if (woodImpact) {
            playWoodImpact(level, random, impact, packet);
        }
        if (budget > 0) {
            spawnMaterialParticles(level, random, material, impact, normal, budget, packet);
        }
    }

    private static int getMaterialParticleRequest(ImpactMaterial material, ImpactFxPacket packet) {
        if (material == ImpactMaterial.METAL && BallisticBlockConfig.ENABLE_METAL_SPARKS.get()) {
            return BallisticBlockConfig.METAL_SPARK_AMOUNT.get();
        }
        if (material == ImpactMaterial.WOOD && BallisticBlockConfig.ENABLE_WOOD_SPLINTERS.get()) {
            int amount = BallisticBlockConfig.WOOD_SPLINTER_AMOUNT.get();
            if (packet.shotgunImpact()) {
                amount = (int) Math.ceil(amount * 1.6D);
            }
            if (packet.highCaliberImpact()) {
                amount = (int) Math.ceil(amount * 1.35D);
            }
            if (packet.finalBreak()) {
                amount = (int) Math.ceil(amount * 2.2D);
            }
            return Math.min(amount, 96);
        }
        return material == ImpactMaterial.GLASS ? 5 : 4;
    }

    private static int reserveParticleBudget(int requested) {
        int maxPerSecond = BallisticBlockConfig.MAX_IMPACT_PARTICLES_PER_SECOND.get();
        if (maxPerSecond <= 0 || requested <= 0) {
            return 0;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            particlesThisSecond = 0;
        }
        int available = Math.max(0, maxPerSecond - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static void spawnSmoke(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count) {
        float size = BallisticBlockConfig.IMPACT_SMOKE_SIZE.get().floatValue();
        int lifetime = BallisticBlockConfig.IMPACT_SMOKE_LIFETIME_TICKS.get();
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomSpread(random, 0.045D).add(normal.scale(0.035D));
            Vec3 velocity = randomSpread(random, 0.018D).add(normal.scale(0.012D + random.nextDouble() * 0.018D));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x + offset.x, impact.y + offset.y, impact.z + offset.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.42F + random.nextFloat() * 0.16F;
                particle.setColor(grey, grey, grey);
                particle.scale(size * (0.75F + random.nextFloat() * 0.35F));
                particle.setLifetime(Math.max(2, lifetime + random.nextInt(5) - 2));
            }
        }
    }

    private static void spawnMaterialParticles(ClientLevel level, RandomSource random, ImpactMaterial material, Vec3 impact, Vec3 normal, int budget, ImpactFxPacket packet) {
        switch (material) {
            case WOOD -> spawnWoodSplinters(level, random, impact, normal, budget, packet);
            case GLASS -> spawnItemBurst(level, random, impact, normal, budget, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.WHITE_STAINED_GLASS_PANE)), 0.032D);
            case METAL -> {
                int sparks = BallisticBlockConfig.ENABLE_METAL_SPARKS.get() ? Math.min(BallisticBlockConfig.METAL_SPARK_AMOUNT.get(), budget) : 0;
                spawnMetalSparks(level, random, impact, normal, sparks);
                if (budget > sparks) {
                    spawnDust(level, random, impact, normal, budget - sparks, new Vector3f(0.36F, 0.36F, 0.34F), 0.55F);
                }
            }
            case STONE -> spawnDust(level, random, impact, normal, budget, new Vector3f(0.42F, 0.42F, 0.40F), 0.68F);
            case DIRT -> spawnDust(level, random, impact, normal, budget, new Vector3f(0.46F, 0.36F, 0.24F), 0.72F);
            default -> spawnDust(level, random, impact, normal, budget, new Vector3f(0.38F, 0.36F, 0.33F), 0.5F);
        }
    }

    private static void spawnWoodSplinters(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count, ImpactFxPacket packet) {
        if (!BallisticBlockConfig.ENABLE_WOOD_SPLINTERS.get()) {
            spawnDust(level, random, impact, normal, count, new Vector3f(0.55F, 0.36F, 0.18F), 0.7F);
            return;
        }

        int lifetime = BallisticBlockConfig.WOOD_SPLINTER_LIFETIME_TICKS.get();
        double velocityBoost = 1.0D;
        if (packet.shotgunImpact()) {
            velocityBoost += 0.25D;
        }
        if (packet.highCaliberImpact()) {
            velocityBoost += 0.22D;
        }
        if (packet.finalBreak()) {
            velocityBoost += 0.45D;
        }

        int dustCount = Math.max(2, count / 4);
        spawnDust(level, random, impact, normal, dustCount, new Vector3f(0.58F, 0.39F, 0.20F), 0.72F);

        ItemParticleOption splinter = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.OAK_PLANKS));
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomSpread(random, 0.045D).add(normal.scale(0.035D));
            Vec3 velocity = randomSpread(random, 0.06D * velocityBoost).add(normal.scale((0.055D + random.nextDouble() * 0.115D) * velocityBoost));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(splinter, impact.x + offset.x, impact.y + offset.y, impact.z + offset.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.scale(0.55F + random.nextFloat() * 0.5F);
                particle.setLifetime(Math.max(2, lifetime + random.nextInt(7) - 3));
            }
        }
    }

    private static void spawnMetalSparks(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count) {
        int lifetime = BallisticBlockConfig.METAL_SPARK_LIFETIME_TICKS.get();
        for (int i = 0; i < count; i++) {
            Vector3f color = random.nextBoolean() ? new Vector3f(1.0F, 0.66F, 0.08F) : new Vector3f(1.0F, 0.9F, 0.24F);
            DustParticleOptions spark = new DustParticleOptions(color, 0.55F + random.nextFloat() * 0.35F);
            Vec3 offset = randomSpread(random, 0.025D).add(normal.scale(0.035D));
            Vec3 velocity = randomSpread(random, 0.065D).add(normal.scale(0.09D + random.nextDouble() * 0.12D));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(spark, impact.x + offset.x, impact.y + offset.y, impact.z + offset.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setLifetime(Math.max(1, lifetime + random.nextInt(3) - 1));
            }
        }
    }

    private static void playRicochet(ClientLevel level, RandomSource random, Player player, Vec3 impact, ImpactFxPacket packet, ImpactMaterial material) {
        if (ClientDynamicImpactSoundHandler.enabled()) {
            return;
        }
        if (!BallisticBlockConfig.ENABLE_RICOCHET_SOUNDS.get()) {
            return;
        }
        if (material == ImpactMaterial.METAL && !BallisticBlockConfig.ENABLE_METAL_RICOCHET_SOUND.get()) {
            return;
        }
        if (material != ImpactMaterial.METAL && material != ImpactMaterial.STONE) {
            return;
        }

        double maxDistance = BallisticBlockConfig.RICOCHET_MAX_DISTANCE.get();
        if (player.distanceToSqr(impact) > maxDistance * maxDistance) {
            return;
        }

        double chance = material == ImpactMaterial.METAL
                ? BallisticBlockConfig.RICOCHET_SOUND_CHANCE_METAL.get()
                : BallisticBlockConfig.RICOCHET_SOUND_CHANCE_HARD_BLOCK.get();
        if (chance <= 0.0D || random.nextDouble() > chance) {
            return;
        }
        if (!reserveRicochetSound()) {
            return;
        }

        SoundEvent sound = chooseRicochetSound(random, material);
        float pitch = getRicochetPitch(random, packet, material);
        float volume = BallisticBlockConfig.RICOCHET_VOLUME.get().floatValue() * (0.82F + random.nextFloat() * 0.32F);
        if (packet.highCaliberImpact()) {
            volume *= 1.25F;
        }
        if (material == ImpactMaterial.STONE) {
            volume *= 0.72F;
        }
        level.playLocalSound(impact.x, impact.y, impact.z, sound, SoundSource.BLOCKS, Math.min(volume, 4.0F), pitch, false);
    }

    private static boolean reserveRicochetSound() {
        int maxPerSecond = BallisticBlockConfig.RICOCHET_MAX_SOUNDS_PER_SECOND.get();
        if (maxPerSecond <= 0) {
            return false;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != ricochetLimiterSecond) {
            ricochetLimiterSecond = currentSecond;
            ricochetSoundsThisSecond = 0;
        }
        if (ricochetSoundsThisSecond >= maxPerSecond) {
            return false;
        }
        ricochetSoundsThisSecond++;
        return true;
    }

    private static SoundEvent chooseRicochetSound(RandomSource random, ImpactMaterial material) {
        if (material == ImpactMaterial.METAL) {
            int variant = random.nextInt(3);
            if (variant == 0) {
                return SoundEvents.CHAIN_HIT;
            }
            if (variant == 1) {
                return SoundEvents.ANVIL_LAND;
            }
            return SoundType.METAL.getHitSound();
        }
        return random.nextBoolean() ? SoundType.STONE.getHitSound() : SoundType.DEEPSLATE.getHitSound();
    }

    private static float getRicochetPitch(RandomSource random, ImpactFxPacket packet, ImpactMaterial material) {
        float pitch = 0.85F + random.nextFloat() * 0.4F;
        if (packet.highCaliberImpact()) {
            pitch -= 0.12F + random.nextFloat() * 0.1F;
        } else {
            pitch += 0.08F + random.nextFloat() * 0.08F;
        }
        if (material == ImpactMaterial.STONE) {
            pitch -= 0.05F;
        }
        return Math.max(0.85F, Math.min(1.25F, pitch));
    }

    private static void playWoodImpact(ClientLevel level, RandomSource random, Vec3 impact, ImpactFxPacket packet) {
        if (ClientDynamicImpactSoundHandler.enabled()) {
            return;
        }
        if (!BallisticBlockConfig.ENABLE_WOOD_IMPACT_SOUND.get()) {
            return;
        }
        float volume = packet.finalBreak() ? 0.75F : 0.38F;
        if (packet.shotgunImpact()) {
            volume += 0.12F;
        }
        if (packet.highCaliberImpact()) {
            volume += 0.1F;
        }
        float pitch = 0.9F + random.nextFloat() * 0.28F;
        level.playLocalSound(impact.x, impact.y, impact.z, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, Math.min(volume, 1.0F), pitch, false);
    }

    private static void spawnDust(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count, Vector3f color, float scale) {
        DustParticleOptions dust = new DustParticleOptions(color, scale);
        spawnSimple(level, random, impact, normal, count, dust, 0.042D);
    }

    private static void spawnItemBurst(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count, ParticleOptions options, double speed) {
        spawnSimple(level, random, impact, normal, count, options, speed);
    }

    private static void spawnSimple(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count, ParticleOptions options, double speed) {
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomSpread(random, 0.035D).add(normal.scale(0.025D));
            Vec3 velocity = randomSpread(random, speed).add(normal.scale(speed * (0.45D + random.nextDouble() * 0.65D)));
            level.addParticle(options, impact.x + offset.x, impact.y + offset.y, impact.z + offset.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }
}
