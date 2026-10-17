package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ClientWeaponJamHandler {
    private static final KeyMapping CLEAR_MALFUNCTION = new KeyMapping(
            "key.tacz_tactical_breaching.clear_malfunction",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.tacz_tactical_breaching"
    );
    private static boolean lastPressed;
    private static boolean jammed;
    private static float condition = 100.0F;
    private static int clearProgressTicks;
    private static int clearTotalTicks = 40;
    private static String activeGunKey = "";

    private ClientWeaponJamHandler() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CLEAR_MALFUNCTION);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clear();
            return;
        }

        ItemStack gunStack = currentGun(player);
        if (gunStack.isEmpty()) {
            releaseClearKey();
            resetHudState();
            return;
        }

        String gunKey = currentGunKey(player, gunStack);
        if (!gunKey.equals(activeGunKey)) {
            activeGunKey = gunKey;
            jammed = WeaponJamData.isJammed(gunStack);
            condition = (float) WeaponJamData.getCondition(gunStack);
            clearProgressTicks = 0;
            clearTotalTicks = 40;
        } else if (!jammed) {
            condition = (float) WeaponJamData.getCondition(gunStack);
        }

        boolean pressed = CLEAR_MALFUNCTION.isDown();
        if (pressed != lastPressed) {
            lastPressed = pressed;
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new WeaponJamClearInputPacket(pressed));
        }
    }

    public static void handleState(WeaponJamStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || currentGun(minecraft.player).isEmpty()) {
            resetHudState();
            return;
        }
        boolean newlyJammed = packet.jammed() && !jammed;
        jammed = packet.jammed();
        condition = Mth.clamp(packet.condition(), 0.0F, 100.0F);
        clearProgressTicks = packet.clearProgressTicks();
        clearTotalTicks = Math.max(1, packet.clearTotalTicks());

        if (newlyJammed) {
            if (minecraft.player != null) {
                Vec3 source = minecraft.player.getEyePosition();
                ClientCameraShakeHandler.closeImpact(source, false);
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || !BallisticBlockConfig.SHOW_JAM_HUD.get()) {
            return;
        }
        if (currentGun(minecraft.player).isEmpty()) {
            return;
        }
        if (!jammed && condition > 85.0F) {
            return;
        }

        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int y = graphics.guiHeight() - 72;
        if (jammed) {
            String title = "WEAPON JAMMED";
            String detail = "Hold J to clear malfunction";
            int progressWidth = 72;
            int progress = Math.round(progressWidth * Mth.clamp(clearProgressTicks / (float) clearTotalTicks, 0.0F, 1.0F));
            drawCentered(graphics, font, title, width / 2, y, 0xFFDD3B2F);
            drawCentered(graphics, font, detail, width / 2, y + 11, 0xFFE8E1D1);
            int barX = width / 2 - progressWidth / 2;
            graphics.fill(barX, y + 23, barX + progressWidth, y + 27, 0xAA181818);
            graphics.fill(barX, y + 23, barX + progress, y + 27, 0xFFE0C15A);
        } else {
            if (!BallisticBlockConfig.SHOW_WEAPON_CONDITION_HUD.get()) {
                return;
            }
            String text = "Weapon Condition: " + Math.round(condition) + "%";
            int color = condition <= 40.0F ? 0xFFE5A13A : 0xFFD5D5D5;
            drawCentered(graphics, font, text, width / 2, y + 14, color);
        }
    }

    public static void clear() {
        if (lastPressed) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new WeaponJamClearInputPacket(false));
        }
        lastPressed = false;
        jammed = false;
        condition = 100.0F;
        clearProgressTicks = 0;
        clearTotalTicks = 40;
        activeGunKey = "";
    }

    private static void drawCentered(GuiGraphics graphics, Font font, String text, int centerX, int y, int color) {
        graphics.drawString(font, Component.literal(text), centerX - font.width(text) / 2, y, color, true);
    }

    private static void releaseClearKey() {
        if (lastPressed) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new WeaponJamClearInputPacket(false));
            lastPressed = false;
        }
    }

    private static void resetHudState() {
        jammed = false;
        condition = 100.0F;
        clearProgressTicks = 0;
        clearTotalTicks = 40;
        activeGunKey = "";
    }

    private static ItemStack currentGun(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (WeaponJamData.isGun(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        return WeaponJamData.isGun(offHand) ? offHand : ItemStack.EMPTY;
    }

    private static String currentGunKey(LocalPlayer player, ItemStack stack) {
        String hand = stack == player.getMainHandItem() ? "main:" + player.getInventory().selected : "offhand";
        IGun gun = IGun.getIGunOrNull(stack);
        String gunId = gun == null ? stack.getItem().toString() : gun.getGunId(stack).toString();
        return hand + ":" + gunId;
    }
}
