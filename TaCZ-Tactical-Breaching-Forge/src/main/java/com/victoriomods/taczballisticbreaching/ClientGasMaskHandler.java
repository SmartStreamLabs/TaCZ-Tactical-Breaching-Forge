package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGasMaskHandler {
    private static final int OVERLAY_TEXTURE_SIZE = 512;
    private static final ResourceLocation GAS_MASK_OVERLAY = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/gui/gas_mask_overlay.png");
    private static int breathCooldown;

    private ClientGasMaskHandler() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !hasWorkingMask(player)) {
            breathCooldown = 0;
            return;
        }
        if (!BallisticBlockConfig.ENABLE_BREATHING_SOUNDS.get()) {
            return;
        }
        if (breathCooldown > 0) {
            breathCooldown--;
            return;
        }
        breathCooldown = 64 + player.getRandom().nextInt(28);
        minecraft.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.HORSE_BREATHE, SoundSource.PLAYERS, 0.12F, 0.58F + player.getRandom().nextFloat() * 0.06F, false);
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.options.hideGui || player == null || !BallisticBlockConfig.ENABLE_GAS_MASKS.get()) {
            return;
        }
        boolean wearingMask = player.getInventory().armor.get(3).getItem() instanceof GasMaskItem;
        if (!wearingMask) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (BallisticBlockConfig.GAS_MASK_OVERLAY_ENABLED.get()) {
            renderOverlay(graphics, width, height);
        }
        if (BallisticBlockConfig.GAS_MASK_FILTER_TEXT_ENABLED.get()) {
            renderHud(graphics, minecraft.font, player, width, height);
        }
    }

    static boolean hasWorkingMask(LocalPlayer player) {
        return GasMaskHandler.hasWorkingGasMask(player);
    }

    static boolean hasSmokeProtection() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && BallisticBlockConfig.ENABLE_SMOKE_PROTECTION.get()
                && hasWorkingMask(minecraft.player);
    }

    private static void renderOverlay(GuiGraphics graphics, int width, int height) {
        float opacity = BallisticBlockConfig.GAS_MASK_OVERLAY_OPACITY.get().floatValue();
        if (opacity <= 0.0F) {
            return;
        }
        float scale = Math.max(width / (float) OVERLAY_TEXTURE_SIZE, height / (float) OVERLAY_TEXTURE_SIZE);
        int drawWidth = Math.round(OVERLAY_TEXTURE_SIZE * scale);
        int drawHeight = Math.round(OVERLAY_TEXTURE_SIZE * scale);
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        graphics.blit(GAS_MASK_OVERLAY, x, y, drawWidth, drawHeight, 0.0F, 0.0F, OVERLAY_TEXTURE_SIZE, OVERLAY_TEXTURE_SIZE, OVERLAY_TEXTURE_SIZE, OVERLAY_TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderHud(GuiGraphics graphics, Font font, LocalPlayer player, int width, int height) {
        int percent = GasMaskHandler.filterPercent(player);
        int color = percent <= 15 ? 0xFFFF5B4A : percent <= 35 ? 0xFFFFC857 : 0xFFB9F6CA;
        String text = "Filter: " + percent + "%";
        float scale = BallisticBlockConfig.GAS_MASK_FILTER_TEXT_SCALE.get().floatValue();
        int x = 16;
        int y = height - 52;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, Math.round(x / scale), Math.round(y / scale), color, true);
        graphics.pose().popPose();
    }
}
