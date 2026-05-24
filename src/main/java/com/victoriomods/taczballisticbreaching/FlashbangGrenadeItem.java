package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Consumer;

public class FlashbangGrenadeItem extends Item {
    public FlashbangGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BallisticBlockConfig.ENABLE_FLASHBANG.get()) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.FLASHBANG_THROW.get(), SoundSource.PLAYERS, 0.22F, 0.96F + level.random.nextFloat() * 0.08F);
        if (!level.isClientSide()) {
            FlashbangGrenadeEntity grenade = new FlashbangGrenadeEntity(level, player);
            grenade.shootFromPlayer(player, player.getXRot(), player.getYRot(), -3.2F, 0.82F, 0.72F);
            level.addFreshEntity(grenade);
        }

        int cooldown = BallisticBlockConfig.FLASHBANG_COOLDOWN_TICKS.get();
        if (cooldown > 0) {
            player.getCooldowns().addCooldown(this, cooldown);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return FlashbangGrenadeItemRenderer.instance();
            }
        }));
    }
}
