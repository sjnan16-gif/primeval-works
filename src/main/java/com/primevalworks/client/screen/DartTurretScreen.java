package com.primevalworks.client.screen;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.inventory.DartTurretMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

public final class DartTurretScreen extends AbstractContainerScreen<DartTurretMenu> {
    private static final int IMAGE_WIDTH = 427;
    private static final int IMAGE_HEIGHT = 240;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/gui/dart_turret.png");
    private static final int FOOD_BOX_EDGE = 0xFF6D4E3B;
    private static final int FOOD_BOX_PAPER_DARK = 0xFFB99472;
    private static final int FOOD_BOX_PAPER_LIGHT = 0xFFE7C9AA;
    private final Screen plannerParent;
    private long openedNanos;
    private float renderScale = 1.0F;
    private float renderOffsetY;

    public DartTurretScreen(DartTurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        titleLabelX = -1000;
        inventoryLabelX = -1000;
        plannerParent = WorksitePlannerScreen.claimMachineMenuReturn();
    }

    @Override
    protected void init() {
        super.init();
        openedNanos = 0L;
        renderScale = 0.82F;
        renderOffsetY = 26.0F;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateMotion();
        int localMouseX = inverseX(mouseX);
        int localMouseY = inverseY(mouseY);
        graphics.pose().pushMatrix();
        applyMotion(graphics);
        try {
            super.extractRenderState(graphics, localMouseX, localMouseY, partialTick);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateMotion();
        graphics.fill(0, 0, width, height, 0xA308050D);
        graphics.pose().pushMatrix();
        applyMotion(graphics);
        try {
            graphics.blit(TEXTURE, leftPos, topPos - DartTurretMenu.MACHINE_LIFT,
                    leftPos + IMAGE_WIDTH, topPos - DartTurretMenu.MACHINE_LIFT + IMAGE_HEIGHT,
                    0.0F, 1.0F, 0.0F, 1.0F);
            drawPlayerInventory(graphics);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    private void drawPlayerInventory(GuiGraphicsExtractor graphics) {
        int panelX = leftPos + 126;
        int panelY = topPos + 108;
        graphics.fill(panelX, panelY, panelX + 176, panelY + 92, FOOD_BOX_EDGE);
        graphics.fill(panelX + 2, panelY + 2, panelX + 174, panelY + 90, FOOD_BOX_PAPER_DARK);
        graphics.fill(panelX + 3, panelY + 3, panelX + 173, panelY + 89, FOOD_BOX_PAPER_LIGHT);
        for (int index = DartTurretMenu.MACHINE_SLOTS; index < menu.slots.size(); index++) {
            var slot = menu.getSlot(index);
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, FOOD_BOX_EDGE);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, FOOD_BOX_PAPER_DARK);
            graphics.fill(x + 2, y + 2, x + 16, y + 16, FOOD_BOX_PAPER_LIGHT);
        }
    }

    private void updateMotion() {
        long now = Util.getNanos();
        if (openedNanos == 0L) openedNanos = now;
        float progress = Mth.clamp((now - openedNanos) / 900_000_000.0F, 0.0F, 1.0F);
        float settled = spring(progress);
        renderScale = 0.82F + 0.18F * settled;
        renderOffsetY = 26.0F * (1.0F - settled);
    }

    private void applyMotion(GuiGraphicsExtractor graphics) {
        graphics.pose().translate(width * 0.5F, height * 0.5F + renderOffsetY);
        graphics.pose().scale(renderScale, renderScale);
        graphics.pose().translate(-width * 0.5F, -height * 0.5F);
    }

    private int inverseX(double x) {
        return Mth.floor(width * 0.5D + (x - width * 0.5D) / Math.max(0.001F, renderScale));
    }

    private int inverseY(double y) {
        return Mth.floor(height * 0.5D
                + (y - height * 0.5D - renderOffsetY) / Math.max(0.001F, renderScale));
    }

    private MouseButtonEvent mapped(MouseButtonEvent event) {
        return new MouseButtonEvent(inverseX(event.x()), inverseY(event.y()), event.buttonInfo());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(mapped(event), doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(mapped(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(mapped(event), dragX / renderScale, dragY / renderScale);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (plannerParent != null && minecraft != null) minecraft.setScreen(plannerParent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float spring(float value) {
        if (value >= 1.0F) return 1.0F;
        double damping = 6.2D;
        double frequency = 11.4D;
        double wave = Math.cos(frequency * value)
                + damping / frequency * Math.sin(frequency * value);
        return 1.0F - (float)(Math.exp(-damping * value) * wave);
    }
}
