package com.primevalworks.client.screen;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.inventory.FoodBoxMenu;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FoodBoxScreen extends AbstractContainerScreen<FoodBoxMenu> {
    private static final int IMAGE_WIDTH = 208;
    private static final int IMAGE_HEIGHT = 124;
    private static final int TITLE_WIDTH = 72;
    private static final int TITLE_X = (IMAGE_WIDTH - TITLE_WIDTH) / 2;
    private static final int TITLE_Y = 0;
    private static final int TITLE_HEIGHT = 14;
    private static final int FOOD_ROW_X = 4;
    private static final int FOOD_ROW_Y = 14;
    private static final int INVENTORY_X = 14;
    private static final int INVENTORY_Y = 39;
    private static final Identifier SPACE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID,
            "textures/gui/space.png"
    );
    private static final int EDGE_DARK = 0xFF6D4E3B;
    private static final int EDGE = 0xFF88664F;
    private static final int PAPER_DARK = 0xFFB99472;
    private static final int PAPER = 0xFFD7B392;
    private static final int PAPER_LIGHT = 0xFFE7C9AA;
    private static final int TITLE_COLOR = 0xFFC74F43;
    private static final float POPUP_DURATION_TICKS = 24.0F;

    private long openedNanos;
    private long renderNowNanos;
    private float renderScale = 1.0F;
    private float renderOffsetY;
    private final Screen plannerParent;

    public FoodBoxScreen(FoodBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        inventoryLabelX = 0;
        inventoryLabelY = 0;
        plannerParent = WorksitePlannerScreen.claimMachineMenuReturn();
    }

    @Override
    protected void init() {
        super.init();
        PrimevalUiSounds.open(this);
        openedNanos = 0L;
        renderNowNanos = 0L;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateMotion(mouseX, mouseY);
        int uiMouseX = inverseX(mouseX);
        int uiMouseY = inverseY(mouseY);
        graphics.pose().pushMatrix();
        applyUiMotion(graphics);
        try {
            super.extractRenderState(graphics, uiMouseX, uiMouseY, partialTick);
            drawInventoryFoodFocus(graphics, uiMouseX, uiMouseY);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateMotion(mouseX, mouseY);
        int uiMouseX = inverseX(mouseX);
        int uiMouseY = inverseY(mouseY);
        graphics.fill(0, 0, width, height, 0xA308050D);
        graphics.pose().pushMatrix();
        applyUiMotion(graphics);
        try {
            drawPaperPanel(graphics, local(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT));
            drawBubble(graphics, local(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT));

            for (int index = 0; index < FoodBoxMenu.FOOD_SLOTS; index++) {
                Rect rect = local(FOOD_ROW_X + index * 20, FOOD_ROW_Y, 20, 20);
                drawInventorySlot(graphics, rect, true, rect.contains(uiMouseX, uiMouseY), index);
            }

            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    int slotIndex = FoodBoxMenu.FOOD_SLOTS + row * 9 + column;
                    Slot slot = menu.getSlot(slotIndex);
                    Rect rect = local(INVENTORY_X + column * 20, INVENTORY_Y + row * 20, 20, 20);
                    drawInventorySlot(graphics, rect, FoodBoxMenu.isDinosaurFood(slot.getItem()),
                            rect.contains(uiMouseX, uiMouseY), slotIndex);
                }
            }

            for (int column = 0; column < 9; column++) {
                int slotIndex = FoodBoxMenu.FOOD_SLOTS + 27 + column;
                Slot slot = menu.getSlot(slotIndex);
                Rect rect = local(INVENTORY_X + column * 20, INVENTORY_Y + 65, 20, 20);
                drawInventorySlot(graphics, rect, FoodBoxMenu.isDinosaurFood(slot.getItem()),
                        rect.contains(uiMouseX, uiMouseY), slotIndex);
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("container.primevalworks.food_box")
                .withStyle(Style.EMPTY.withBold(true));
        int measured = font.width(title);
        float scale = Math.min(0.88F, (TITLE_WIDTH - 8.0F) / Math.max(1.0F, measured));
        graphics.pose().pushMatrix();
        graphics.pose().translate(TITLE_X + TITLE_WIDTH * 0.5F,
                TITLE_Y + (TITLE_HEIGHT - font.lineHeight * scale) * 0.5F);
        graphics.pose().scale(scale, scale);
        graphics.text(font, title, Math.round(-measured * 0.5F), 0, TITLE_COLOR, true);
        graphics.pose().popMatrix();
    }

    private void drawInventoryFoodFocus(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int index = FoodBoxMenu.FOOD_SLOTS; index < menu.slots.size(); index++) {
            Slot slot = menu.getSlot(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (!FoodBoxMenu.isDinosaurFood(stack)) {
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xB0000000);
            } else if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.fill(x, y, x + 16, y + 16, 0x25FFFFFF);
            }
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }

        for (int index = 0; index < FoodBoxMenu.FOOD_SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    private void drawInventorySlot(
            GuiGraphicsExtractor graphics,
            Rect rect,
            boolean edible,
            boolean hovered,
            int index
    ) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), EDGE_DARK);
        graphics.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, PAPER_DARK);
        graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, PAPER_LIGHT);
        if (hovered) {
            outline(graphics, new Rect(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2), 0xB8FFF1C7);
        }
    }

    private void updateMotion(int mouseX, int mouseY) {
        renderNowNanos = Util.getNanos();
        if (openedNanos == 0L) {
            openedNanos = renderNowNanos;
        }
        float time = (renderNowNanos - openedNanos) / 50_000_000.0F;
        float progress = Mth.clamp(time / POPUP_DURATION_TICKS, 0.0F, 1.0F);
        float settled = spring(progress, 6.2F, 11.4F);
        renderScale = 0.74F + 0.26F * settled;
        renderOffsetY = 18.0F * (1.0F - settled);
    }

    private void applyUiMotion(GuiGraphicsExtractor graphics) {
        graphics.pose().translate(width * 0.5F, height * 0.5F + renderOffsetY);
        graphics.pose().scale(renderScale, renderScale);
        graphics.pose().translate(-width * 0.5F, -height * 0.5F);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) PrimevalUiSounds.click();
        return super.mouseClicked(mapped(event), doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(mapped(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(mapped(event), dragX / Math.max(0.001F, renderScale),
                dragY / Math.max(0.001F, renderScale));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        PrimevalUiSounds.close(this);
        super.onClose();
        if (plannerParent != null && minecraft != null) {
            minecraft.setScreen(plannerParent);
        }
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

    private void drawPaperPanel(GuiGraphicsExtractor graphics, Rect panel) {
        graphics.fill(panel.x + 4, panel.y + 5, panel.right() + 4, panel.bottom() + 5, 0x46000000);
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), EDGE_DARK);
        graphics.fill(panel.x + 2, panel.y + 2, panel.right() - 2, panel.bottom() - 2, EDGE);
        graphics.fill(panel.x + 4, panel.y + 4, panel.right() - 4, panel.bottom() - 4, PAPER);
        for (int y = panel.y + 7; y < panel.bottom() - 5; y += 7) {
            graphics.fill(panel.x + 5, y, panel.right() - 5, y + 1, 0x12FFFFFF);
        }
        graphics.fill(panel.x + 2, panel.y + 2, panel.x + 10, panel.y + 4, PAPER_LIGHT);
        graphics.fill(panel.right() - 10, panel.y + 2, panel.right() - 2, panel.y + 4, PAPER_LIGHT);
        graphics.fill(panel.x + 2, panel.bottom() - 4, panel.x + 10, panel.bottom() - 2, PAPER_DARK);
        graphics.fill(panel.right() - 10, panel.bottom() - 4, panel.right() - 2, panel.bottom() - 2, PAPER_DARK);
    }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect) {
        int border = 2;
        blitRegion(graphics, rect.x, rect.y, border, border, 0, 0, 2, 2);
        blitRegion(graphics, rect.x + border, rect.y, rect.width - 4, border, 2, 0, 82, 2);
        blitRegion(graphics, rect.right() - border, rect.y, border, border, 84, 0, 2, 2);
        blitRegion(graphics, rect.x, rect.y + border, border, rect.height - 4, 0, 2, 2, 10);
        blitRegion(graphics, rect.x + border, rect.y + border, rect.width - 4, rect.height - 4, 2, 2, 82, 10);
        blitRegion(graphics, rect.right() - border, rect.y + border, border, rect.height - 4, 84, 2, 2, 10);
        blitRegion(graphics, rect.x, rect.bottom() - border, border, border, 0, 12, 2, 2);
        blitRegion(graphics, rect.x + border, rect.bottom() - border, rect.width - 4, border, 2, 12, 82, 2);
        blitRegion(graphics, rect.right() - border, rect.bottom() - border, border, border, 84, 12, 2, 2);
    }

    private void blitRegion(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.blit(SPACE, x, y, x + width, y + height,
                sourceX / 86.0F, (sourceX + sourceWidth) / 86.0F,
                sourceY / 14.0F, (sourceY + sourceHeight) / 14.0F);
    }

    private Rect local(int x, int y, int width, int height) {
        return new Rect(leftPos + x, topPos + y, width, height);
    }

    private static void outline(GuiGraphicsExtractor graphics, Rect rect, int color) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.y + 1, color);
        graphics.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x, rect.y + 1, rect.x + 1, rect.bottom() - 1, color);
        graphics.fill(rect.right() - 1, rect.y + 1, rect.right(), rect.bottom() - 1, color);
    }

    private static float spring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) {
            return 1.0F;
        }
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
    }

    private record Rect(int x, int y, int width, int height) {
        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
