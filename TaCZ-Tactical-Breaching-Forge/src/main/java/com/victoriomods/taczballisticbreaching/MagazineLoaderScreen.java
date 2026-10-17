package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MagazineLoaderScreen extends AbstractContainerScreen<MagazineLoaderMenu> {
    public MagazineLoaderScreen(MagazineLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Load"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(this.leftPos + 120, this.topPos + 34, 48, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xE0101010);
        graphics.fill(x + 7, y + 7, x + this.imageWidth - 7, y + 68, 0xE0222428);
        graphics.fill(x + 7, y + 82, x + this.imageWidth - 7, y + this.imageHeight - 7, 0x90181818);
        drawSlot(graphics, x + 80, y + 35);
        drawInventorySlots(graphics, x, y);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, "Tactical Magazine", 10, 10, 0xF2F2F2, false);
        graphics.drawString(this.font, "Current Ammo: " + this.menu.getCurrentAmmo() + "/" + this.menu.getMaxAmmo(), 10, 24, 0xC9D1D9, false);
        graphics.drawString(this.font, "Ammo Type: " + this.menu.getAmmoTypeName(), 10, 58, 0xC9D1D9, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xC9D1D9, false);
    }

    private void drawInventorySlots(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + 8 + column * 18, y + 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + 8 + column * 18, y + 142);
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x, y, x + 16, y + 16, 0xFF111111);
        graphics.fill(x, y, x + 16, y + 1, 0xFF4E4E4E);
        graphics.fill(x, y, x + 1, y + 16, 0xFF4E4E4E);
        graphics.fill(x + 15, y, x + 16, y + 16, 0xFF050505);
        graphics.fill(x, y + 15, x + 16, y + 16, 0xFF050505);
    }
}
