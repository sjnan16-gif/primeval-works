package com.primevalworks.client.screen;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.inventory.AncientFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class AncientFurnaceScreen extends AbstractContainerScreen<AncientFurnaceMenu> {
    public static final int IMAGE_WIDTH = 427;
    public static final int IMAGE_HEIGHT = 240;
    private static final int TEXTURE_WIDTH = 427;
    private static final int TEXTURE_HEIGHT = 240;
    private static final int SLIDER_X = 171;
    private static final int SLIDER_Y = 87;
    private static final int SLIDER_WIDTH = 85;
    private static final int SLIDER_HEIGHT = 7;
    private static final int HANDLE_SIZE = 9;
    private static final int PROGRESS_X = 118;
    private static final int PROGRESS_Y = 112;
    private static final int PROGRESS_WIDTH = 6;
    private static final int PROGRESS_HEIGHT = 21;
    private static final int ENERGY_X = 148;
    private static final int ENERGY_Y = 130;
    private static final int READOUT_X = 176;
    private static final int READOUT_Y = 117;
    private static final int READOUT_WIDTH = 71;
    private static final int READOUT_HEIGHT = 12;
    private static final float OPEN_SECONDS = 1.20F;
    private static final Identifier TEXTURE = gui("ancient_furnace.png");
    private static final Identifier SLIDER_FILL = gui("ancient_furnace_energy_bar_fill.png");
    private static final Identifier SLIDER_HANDLE = gui("ancient_furnace_energy_bar_button.png");
    private static final Identifier ENERGY_ICON = gui("energy_above_block_icon.png");
    private static final Identifier FURNACE_FILL = gui("furnace_fill.png");

    private final Screen plannerParent;
    private long openedNanos;
    private long frameNanos;
    private float renderScale = 1.0F;
    private float renderOffsetY;
    private boolean draggingThrottle;
    private float dragThrottle = AncientFurnaceBlockEntityDefaults.DEFAULT_THROTTLE;
    private int lastSentThrottle = -1;
    private long lastHandleNanos;
    private float lastHandleMouseX = Float.NaN;
    private float lastHandleMouseY = Float.NaN;
    private float handleScale = 1.0F;
    private float handleStretch;
    private float handleLagX;
    private float handleLagY;
    private float handleVelocityX;
    private float handleVelocityY;

    public AncientFurnaceScreen(AncientFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        inventoryLabelX = -1000;
        inventoryLabelY = -1000;
        titleLabelX = -1000;
        titleLabelY = -1000;
        plannerParent = WorksitePlannerScreen.claimMachineMenuReturn();
    }

    @Override
    protected void init() {
        super.init();
        openedNanos = 0L;
        frameNanos = 0L;
        renderScale = 0.86F;
        renderOffsetY = 30.0F;
        dragThrottle = menu.throttle();
        lastSentThrottle = -1;
        lastHandleNanos = 0L;
        lastHandleMouseX = Float.NaN;
        lastHandleMouseY = Float.NaN;
        handleScale = 1.0F;
        handleStretch = 0.0F;
        handleLagX = 0.0F;
        handleLagY = 0.0F;
        handleVelocityX = 0.0F;
        handleVelocityY = 0.0F;
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
            drawTooltips(graphics, localMouseX, localMouseY);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateMotion();
        graphics.fill(0, 0, width, height, 0x98000000);
        graphics.pose().pushMatrix();
        applyMotion(graphics);
        try {
            graphics.blit(TEXTURE, leftPos, topPos - AncientFurnaceMenu.MACHINE_LIFT,
                    leftPos + TEXTURE_WIDTH, topPos - AncientFurnaceMenu.MACHINE_LIFT + TEXTURE_HEIGHT,
                    0.0F, 1.0F, 0.0F, 1.0F);
            drawPlayerInventory(graphics);
            drawThrottle(graphics, inverseX(mouseX), inverseY(mouseY));
            drawEnergy(graphics);
            drawProgress(graphics);
            drawReadout(graphics);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void drawProgress(GuiGraphicsExtractor graphics) {
        int duration = menu.processDuration();
        if (duration <= 0 || menu.processProgress() <= 0) return;
        float ratio = Mth.clamp(menu.processProgress() / (float)duration, 0.0F, 1.0F);
        int visible = Mth.clamp(Mth.ceil(PROGRESS_HEIGHT * ratio), 1, PROGRESS_HEIGHT);
        int sourceY = PROGRESS_HEIGHT - visible;
        int x = leftPos + PROGRESS_X;
        int y = topPos - AncientFurnaceMenu.MACHINE_LIFT + PROGRESS_Y + sourceY;
        graphics.blit(FURNACE_FILL, x, y, x + PROGRESS_WIDTH, y + visible,
                0.0F, 1.0F, sourceY / (float)PROGRESS_HEIGHT, 1.0F);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    private void drawPlayerInventory(GuiGraphicsExtractor graphics) {
        int panelX = leftPos + 126;
        int panelY = topPos + 108;
        graphics.fill(panelX, panelY, panelX + 176, panelY + 92, 0xFF15151F);
        graphics.fill(panelX + 2, panelY + 2, panelX + 174, panelY + 90, 0xFF292B3E);
        graphics.fill(panelX + 3, panelY + 3, panelX + 173, panelY + 89, 0xFF292839);
        graphics.text(font, Component.translatable("container.inventory").withStyle(Style.EMPTY.withBold(true)),
                leftPos + AncientFurnaceMenu.PLAYER_INVENTORY_X, topPos + 110, 0xFFB8B5C4, true);
        for (int index = AncientFurnaceMenu.MACHINE_SLOTS; index < menu.slots.size(); index++) {
            var slot = menu.getSlot(index);
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xFF15151F);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF202230);
            graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF292839);
        }
    }

    private void drawThrottle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float throttle = displayedThrottle();
        int x = leftPos + SLIDER_X;
        int y = topPos - AncientFurnaceMenu.MACHINE_LIFT + SLIDER_Y;
        int filled = Mth.clamp(Math.round(SLIDER_WIDTH * throttle), 0, SLIDER_WIDTH);
        if (filled > 0) {
            graphics.blit(SLIDER_FILL, x, y, x + filled, y + SLIDER_HEIGHT,
                    0.0F, filled / (float)SLIDER_WIDTH, 0.0F, 1.0F);
        }
        int handleX = x + Math.round((SLIDER_WIDTH - HANDLE_SIZE) * throttle);
        int handleY = y + 1;
        boolean hovered = inside(mouseX, mouseY, x - 5, y - 5, SLIDER_WIDTH + 10, SLIDER_HEIGHT + 10);
        updateHandlePhysics(mouseX, mouseY, hovered);
        float time = (frameNanos - openedNanos) / 1_000_000_000.0F;
        float activity = hovered || draggingThrottle ? 1.0F : 0.0F;
        float swayX = handleLagX + Mth.sin(time * 7.2F) * 0.24F * activity;
        float swayY = handleLagY + Mth.sin(time * 10.4F + 0.7F) * 0.18F * activity;
        float scaleX = handleScale * (1.0F + handleStretch);
        float scaleY = handleScale * (1.0F - handleStretch * 0.42F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(handleX + HANDLE_SIZE * 0.5F + swayX,
                handleY + HANDLE_SIZE * 0.5F + swayY);
        graphics.pose().scale(scaleX, scaleY);
        graphics.pose().translate(-(handleX + HANDLE_SIZE * 0.5F), -(handleY + HANDLE_SIZE * 0.5F));
        graphics.blit(SLIDER_HANDLE, handleX, handleY, handleX + HANDLE_SIZE, handleY + HANDLE_SIZE,
                0.0F, 1.0F, 0.0F, 1.0F);
        graphics.pose().popMatrix();
    }

    private void updateHandlePhysics(float mouseX, float mouseY, boolean hovered) {
        long now = frameNanos == 0L ? Util.getNanos() : frameNanos;
        float deltaSeconds = lastHandleNanos == 0L
                ? 1.0F / 60.0F
                : Mth.clamp((now - lastHandleNanos) / 1_000_000_000.0F, 0.001F, 0.05F);
        lastHandleNanos = now;
        float pointerDeltaX = Float.isNaN(lastHandleMouseX) ? 0.0F : mouseX - lastHandleMouseX;
        float pointerDeltaY = Float.isNaN(lastHandleMouseY) ? 0.0F : mouseY - lastHandleMouseY;
        lastHandleMouseX = mouseX;
        lastHandleMouseY = mouseY;

        float pointerSpeed = Mth.clamp((float)Math.hypot(pointerDeltaX, pointerDeltaY)
                / Math.max(0.001F, deltaSeconds), 0.0F, 1400.0F);
        float speedWeight = draggingThrottle ? pointerSpeed / 1400.0F : 0.0F;
        float targetScale = draggingThrottle ? 1.10F + speedWeight * 0.18F : hovered ? 1.055F : 1.0F;
        handleScale = follow(handleScale, targetScale, 13.0F, deltaSeconds);
        handleStretch = follow(handleStretch, speedWeight * 0.13F, 11.0F, deltaSeconds);

        float targetLagX = draggingThrottle ? Mth.clamp(-pointerDeltaX * 0.22F, -2.0F, 2.0F) : 0.0F;
        float targetLagY = draggingThrottle ? Mth.clamp(-pointerDeltaY * 0.16F, -1.25F, 1.25F) : 0.0F;
        handleVelocityX += (targetLagX - handleLagX) * 95.0F * deltaSeconds;
        handleVelocityY += (targetLagY - handleLagY) * 95.0F * deltaSeconds;
        float damping = (float)Math.exp(-13.0F * deltaSeconds);
        handleVelocityX *= damping;
        handleVelocityY *= damping;
        handleLagX += handleVelocityX * deltaSeconds;
        handleLagY += handleVelocityY * deltaSeconds;
    }

    private void drawEnergy(GuiGraphicsExtractor graphics) {
        int x = leftPos + ENERGY_X;
        int y = topPos - AncientFurnaceMenu.MACHINE_LIFT + ENERGY_Y;
        if (menu.hasEnergy()) {
            graphics.blit(ENERGY_ICON, x, y, x + 16, y + 16, 0.0F, 1.0F, 0.0F, 1.0F);
            return;
        }
        float wave = (Mth.sin((frameNanos - openedNanos) / 220_000_000.0F) + 1.0F) * 0.5F;
        int color = ((44 + Mth.floor(wave * 104.0F)) << 24) | 0x00FFFFFF;
        int[] starts = {6, 5, 5, 4, 4, 3, 3, 3, 6, 6, 5, 5, 4, 4};
        int[] widths = {7, 8, 7, 7, 10, 11, 11, 10, 6, 5, 5, 4, 4, 3};
        for (int row = 0; row < starts.length; row++) {
            graphics.fill(x + starts[row], y + row + 1,
                    x + starts[row] + widths[row], y + row + 2, color);
        }
    }

    private void drawReadout(GuiGraphicsExtractor graphics) {
        int x = leftPos + READOUT_X;
        int y = topPos - AncientFurnaceMenu.MACHINE_LIFT + READOUT_Y;
        String value = String.format(Locale.ROOT, "%.1f E/S   %.2f× SPEED",
                energyFor(displayedThrottle()), speedFor(displayedThrottle()));
        Component text = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        float xScale = Math.min(0.80F, (READOUT_WIDTH - 4.0F) / Math.max(1.0F, font.width(text)));
        float yScale = Math.min(0.90F, (READOUT_HEIGHT - 3.0F) / font.lineHeight);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + READOUT_WIDTH * 0.5F, y + READOUT_HEIGHT * 0.5F);
        graphics.pose().scale(xScale, yScale);
        int tx = -font.width(text) / 2;
        int ty = -font.lineHeight / 2;
        graphics.text(font, text, tx + 1, ty + 1, 0xA0070710, false);
        graphics.text(font, text, tx, ty, menu.hasEnergy() ? 0xFFF1CF62 : 0xFF9A94A3, false);
        graphics.pose().popMatrix();
    }

    private void drawTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int sliderX = leftPos + SLIDER_X;
        int sliderY = topPos - AncientFurnaceMenu.MACHINE_LIFT + SLIDER_Y;
        if (inside(mouseX, mouseY, sliderX - 5, sliderY - 5, SLIDER_WIDTH + 10, SLIDER_HEIGHT + 10)) {
            return;
        }
        int energyX = leftPos + ENERGY_X;
        int energyY = topPos - AncientFurnaceMenu.MACHINE_LIFT + ENERGY_Y;
        if (inside(mouseX, mouseY, energyX, energyY, 16, 16)) {
            graphics.setTooltipForNextFrame(Component.literal(menu.hasEnergy()
                    ? String.format(Locale.ROOT, "Powered at %.1f E/S.", menu.energyPerSecond())
                    : "No energy. Connect this furnace from the Energy Map."), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        MouseButtonEvent mapped = mapped(event);
        int sliderX = leftPos + SLIDER_X;
        int sliderY = topPos - AncientFurnaceMenu.MACHINE_LIFT + SLIDER_Y;
        if (event.button() == 0 && inside(mapped.x(), mapped.y(), sliderX - 5, sliderY - 5,
                SLIDER_WIDTH + 10, SLIDER_HEIGHT + 10)) {
            draggingThrottle = true;
            updateThrottle((float)mapped.x(), true);
            return true;
        }
        return super.mouseClicked(mapped, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent mapped = mapped(event);
        if (event.button() == 0 && draggingThrottle) {
            updateThrottle((float)mapped.x(), false);
            return true;
        }
        return super.mouseDragged(mapped, dragX / Math.max(0.001F, renderScale),
                dragY / Math.max(0.001F, renderScale));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        MouseButtonEvent mapped = mapped(event);
        if (event.button() == 0 && draggingThrottle) {
            updateThrottle((float)mapped.x(), true);
            draggingThrottle = false;
            return true;
        }
        return super.mouseReleased(mapped);
    }

    private void updateThrottle(float mouseX, boolean force) {
        dragThrottle = Mth.clamp((mouseX - leftPos - SLIDER_X - HANDLE_SIZE * 0.5F)
                / (SLIDER_WIDTH - HANDLE_SIZE), 0.0F, 1.0F);
        int encoded = Math.round(dragThrottle * 1000.0F);
        if ((force || Math.abs(encoded - lastSentThrottle) >= 5)
                && minecraft != null && minecraft.gameMode != null) {
            lastSentThrottle = encoded;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1000 + encoded);
        }
    }

    private float displayedThrottle() {
        return draggingThrottle ? dragThrottle : menu.throttle();
    }

    private static float energyFor(float throttle) {
        if (throttle <= 0.20F) return Mth.lerp(throttle / 0.20F, 2.5F, 3.0F);
        return Mth.lerp((throttle - 0.20F) / 0.80F, 3.0F, 10.5F);
    }

    private static float speedFor(float throttle) {
        if (throttle <= 0.20F) return Mth.lerp(throttle / 0.20F, 0.75F, 1.0F);
        return Mth.lerp((throttle - 0.20F) / 0.80F, 1.0F, 4.2F);
    }

    private void updateMotion() {
        long now = Util.getNanos();
        frameNanos = now;
        if (openedNanos == 0L) openedNanos = now;
        float elapsed = (now - openedNanos) / 1_000_000_000.0F;
        float settled = spring(Mth.clamp(elapsed / OPEN_SECONDS, 0.0F, 1.0F));
        renderScale = 0.86F + 0.14F * settled;
        renderOffsetY = 30.0F * (1.0F - settled);
    }

    private void applyMotion(GuiGraphicsExtractor graphics) {
        graphics.pose().translate(width * 0.5F, height * 0.5F + renderOffsetY);
        graphics.pose().scale(renderScale, renderScale);
        graphics.pose().translate(-width * 0.5F, -height * 0.5F);
    }

    private MouseButtonEvent mapped(MouseButtonEvent event) {
        return new MouseButtonEvent(inverseX(event.x()), inverseY(event.y()), event.buttonInfo());
    }

    private int inverseX(double x) {
        return Mth.floor(width * 0.5D + (x - width * 0.5D) / Math.max(0.001F, renderScale));
    }

    private int inverseY(double y) {
        return Mth.floor(height * 0.5D
                + (y - height * 0.5D - renderOffsetY) / Math.max(0.001F, renderScale));
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static float spring(float progress) {
        if (progress >= 1.0F) return 1.0F;
        double damping = 6.2D;
        double frequency = 11.4D;
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
    }

    private static float follow(float current, float target, float speed, float deltaSeconds) {
        return Mth.lerp(1.0F - (float)Math.exp(-speed * deltaSeconds), current, target);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (plannerParent != null && minecraft != null) minecraft.setScreen(plannerParent);
    }

    private static Identifier gui(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/gui/" + name);
    }

    private static final class AncientFurnaceBlockEntityDefaults {
        private static final float DEFAULT_THROTTLE = 0.20F;
    }
}
