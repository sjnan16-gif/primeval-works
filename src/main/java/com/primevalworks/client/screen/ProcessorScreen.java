package com.primevalworks.client.screen;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.inventory.ProcessorMenu;
import com.primevalworks.world.processor.ProcessorRecipe;
import com.primevalworks.world.processor.ProcessorRecipes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ProcessorScreen extends AbstractContainerScreen<ProcessorMenu> {
    public static final int IMAGE_WIDTH = 427;
    public static final int IMAGE_HEIGHT = 240;
    private static final int TEXTURE_WIDTH = 427;
    private static final int TEXTURE_HEIGHT = 240;
    private static final int FIRE_SOURCE_X = 235;
    private static final int FIRE_SOURCE_Y = 61;
    private static final int FIRE_DESTINATION_X = 192;
    private static final int FIRE_DESTINATION_Y = 88;
    private static final int FIRE_WIDTH = 44;
    private static final int FIRE_HEIGHT = 17;
    private static final int ENERGY_ICON_X = 114;
    private static final int ENERGY_ICON_Y = 87;
    private static final float OPEN_SECONDS = 1.20F;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/gui/processor_ui.png"
    );
    private static final Identifier ENERGY_ICON = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/gui/energy_above_block_icon.png"
    );

    private long openedNanos;
    private long frameNanos;
    private float renderScale = 1.0F;
    private float renderOffsetY;
    private final Screen plannerParent;

    public ProcessorScreen(ProcessorMenu menu, Inventory inventory, Component title) {
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
        PrimevalUiSounds.open(this);
        openedNanos = 0L;
        frameNanos = 0L;
        renderScale = 0.86F;
        renderOffsetY = 30.0F;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateMotion();
        int localMouseX = inverseX(mouseX);
        int localMouseY = inverseY(mouseY);
        graphics.pose().pushMatrix();
        applyMotion(graphics);
        try {
            drawGhostRecipe(graphics);
            super.extractRenderState(graphics, localMouseX, localMouseY, partialTick);
            drawEnergyTooltip(graphics, localMouseX, localMouseY);
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
            drawProcessorArt(graphics);
            drawProcessLava(graphics);
            drawEnergyState(graphics);
            drawPlayerInventory(graphics);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    private void drawProcessorArt(GuiGraphicsExtractor graphics) {
        blitRegion(graphics, 0, 0, TEXTURE_WIDTH, FIRE_SOURCE_Y);
        blitRegion(graphics, 0, FIRE_SOURCE_Y, FIRE_SOURCE_X, FIRE_HEIGHT);
        blitRegion(graphics, FIRE_SOURCE_X + FIRE_WIDTH, FIRE_SOURCE_Y,
                TEXTURE_WIDTH - FIRE_SOURCE_X - FIRE_WIDTH, FIRE_HEIGHT);
        blitRegion(graphics, 0, FIRE_SOURCE_Y + FIRE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT - FIRE_SOURCE_Y - FIRE_HEIGHT);
    }

    private void drawProcessLava(GuiGraphicsExtractor graphics) {
        if (menu.processDuration() <= 0 || menu.processProgress() <= 0) return;
        float remaining = 1.0F - Mth.clamp(
                menu.processProgress() / (float)menu.processDuration(), 0.0F, 1.0F
        );
        int visibleHeight = Mth.clamp(Mth.ceil(FIRE_HEIGHT * remaining), 0, FIRE_HEIGHT);
        if (visibleHeight <= 0) return;
        int sourceY = FIRE_SOURCE_Y + FIRE_HEIGHT - visibleHeight;
        int destinationY = FIRE_DESTINATION_Y + FIRE_HEIGHT - visibleHeight;
        blitPlacedRegion(graphics, FIRE_DESTINATION_X, destinationY,
                FIRE_SOURCE_X, sourceY, FIRE_WIDTH, visibleHeight);
    }

    private void drawEnergyState(GuiGraphicsExtractor graphics) {
        int x = leftPos + ENERGY_ICON_X;
        int y = topPos - ProcessorMenu.MACHINE_LIFT + ENERGY_ICON_Y;
        if (menu.hasEnergy()) {
            graphics.blit(ENERGY_ICON, x, y, x + 16, y + 16, 0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            float wave = (Mth.sin((frameNanos - openedNanos) / 220_000_000.0F) + 1.0F) * 0.5F;
            drawEmptyEnergyPulse(graphics, x, y, 42 + Mth.floor(wave * 94.0F));
        }
    }

    private void drawEmptyEnergyPulse(GuiGraphicsExtractor graphics, int x, int y, int alpha) {
        int color = (Mth.clamp(alpha, 0, 255) << 24) | 0x00FFFFFF;
        int[] starts = {6, 5, 5, 4, 4, 3, 3, 3, 6, 6, 5, 5, 4, 4};
        int[] widths = {7, 8, 7, 7, 10, 11, 11, 10, 6, 5, 5, 4, 4, 3};
        for (int row = 0; row < starts.length; row++) {
            graphics.fill(x + starts[row], y + row + 1,
                    x + starts[row] + widths[row], y + row + 2, color);
        }
    }

    private void drawPlayerInventory(GuiGraphicsExtractor graphics) {
        int panelX = leftPos + 126;
        int panelY = topPos + 108;
        int panelWidth = 176;
        int panelHeight = 92;
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF15151F);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xFF292B3E);
        graphics.fill(panelX + 3, panelY + 3, panelX + panelWidth - 3, panelY + panelHeight - 3, 0xFF292839);
        graphics.text(font, Component.translatable("container.inventory").withStyle(style -> style.withBold(true)),
                leftPos + ProcessorMenu.PLAYER_INVENTORY_X, topPos + 110, 0xFFB8B5C4, true);

        for (int index = ProcessorMenu.PROCESSOR_SLOTS; index < menu.slots.size(); index++) {
            var slot = menu.getSlot(index);
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xFF15151F);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF202230);
            graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF292839);
        }
    }

    private void drawGhostRecipe(GuiGraphicsExtractor graphics) {
        ItemStack input = menu.getSlot(ProcessorBlockEntity.INPUT_SLOT).getItem();
        ProcessorRecipe recipe;
        if (input.isEmpty()) {
            int index = Math.floorMod((int)((frameNanos - openedNanos) / 2_200_000_000L),
                    ProcessorRecipes.all().size());
            recipe = ProcessorRecipes.all().get(index);
            drawGhost(graphics, new ItemStack(recipe.input()), ProcessorMenu.INPUT_X, ProcessorMenu.INPUT_Y, false);
        } else {
            recipe = ProcessorRecipes.forInput(input).orElse(null);
        }

        if (menu.getSlot(ProcessorBlockEntity.FUEL_SLOT).getItem().isEmpty()) {
            drawGhost(graphics, new ItemStack(Items.COAL), ProcessorMenu.FUEL_X, ProcessorMenu.FUEL_Y, false);
        }
        if (recipe == null) return;
        if (menu.getSlot(ProcessorBlockEntity.CATALYST_SLOT).getItem().isEmpty()) {
            drawGhost(graphics, new ItemStack(recipe.catalyst()),
                    ProcessorMenu.CATALYST_X, ProcessorMenu.CATALYST_Y, true);
        }
        if (menu.getSlot(ProcessorBlockEntity.OUTPUT_SLOT).getItem().isEmpty()) {
            drawGhost(graphics, recipe.outputStack(), ProcessorMenu.OUTPUT_X, ProcessorMenu.OUTPUT_Y, true);
        }
    }

    private void drawGhost(GuiGraphicsExtractor graphics, ItemStack stack, int slotX, int slotY, boolean pulse) {
        int x = leftPos + slotX;
        int y = topPos + slotY;
        graphics.item(stack, x, y);
        int alpha = 214;
        if (pulse) {
            float wave = (Mth.sin((frameNanos - openedNanos) / 210_000_000.0F) + 1.0F) * 0.5F;
            alpha = 204 + Mth.floor(wave * 18.0F);
        }
        graphics.fill(x, y, x + 16, y + 16, (alpha << 24) | 0x00090A10);
    }

    private void drawEnergyTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + ENERGY_ICON_X;
        int y = topPos - ProcessorMenu.MACHINE_LIFT + ENERGY_ICON_Y;
        if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) return;
        Component tooltip = menu.hasEnergy()
                ? Component.literal("Powered - " + BaseEnergyRules.PROCESSOR_DEMAND + " E/S")
                : Component.literal("No energy. Machine cannot work.");
        graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
    }

    private void blitRegion(GuiGraphicsExtractor graphics, int sourceX, int sourceY,
                            int regionWidth, int regionHeight) {
        if (regionWidth <= 0 || regionHeight <= 0) return;
        int destinationX = leftPos + sourceX;
        int destinationY = topPos - ProcessorMenu.MACHINE_LIFT + sourceY;
        graphics.blit(TEXTURE,
                destinationX, destinationY, destinationX + regionWidth, destinationY + regionHeight,
                sourceX / (float)TEXTURE_WIDTH, (sourceX + regionWidth) / (float)TEXTURE_WIDTH,
                sourceY / (float)TEXTURE_HEIGHT, (sourceY + regionHeight) / (float)TEXTURE_HEIGHT);
    }

    private void blitPlacedRegion(GuiGraphicsExtractor graphics, int destinationX, int destinationY,
                                  int sourceX, int sourceY, int regionWidth, int regionHeight) {
        if (regionWidth <= 0 || regionHeight <= 0) return;
        int x = leftPos + destinationX;
        int y = topPos - ProcessorMenu.MACHINE_LIFT + destinationY;
        graphics.blit(TEXTURE, x, y, x + regionWidth, y + regionHeight,
                sourceX / (float)TEXTURE_WIDTH, (sourceX + regionWidth) / (float)TEXTURE_WIDTH,
                sourceY / (float)TEXTURE_HEIGHT, (sourceY + regionHeight) / (float)TEXTURE_HEIGHT);
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

    private static float spring(float progress) {
        if (progress >= 1.0F) return 1.0F;
        double damping = 6.2D;
        double frequency = 11.4D;
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
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
}
