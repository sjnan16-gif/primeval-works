package com.primevalworks.client.effect;

import com.primevalworks.PrimevalWorks;
import com.mojang.blaze3d.platform.InputConstants;
import com.primevalworks.client.screen.DinoWhistleScreen;
import com.primevalworks.client.screen.PrimevalBubbleUi;
import com.primevalworks.network.payload.RequestWhistleFollowersPayload;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Locale;

public final class DinoWhistleClient {
    private static final long CONFIGURE_HOLD_NANOS = 750_000_000L;
    private static final int SIDEBAR_WIDTH = 138;
    private static final int SIDEBAR_HEIGHT = 45;
    private static final int INK = 0xFF494341;
    private static final int MUTED = 0xFF6E6764;
    private static final int RED = 0xFFC74F43;
    private static final Identifier[] WORK_PROGRESS = progressFrames();

    private static BlockPos areaFirst;
    private static DinoWhistleSettings.FieldMode areaMode;
    private static long hoverStarted;
    private static int hoveredInventorySlot = -1;
    private static boolean openedFromHold;
    private static float worldSidebarVisibility;
    private static long lastWorldFrame;

    private DinoWhistleClient() {}

    public static void handleAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack whistle = DinoWhistleItem.findHeld(minecraft.player);
        if (whistle.isEmpty()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        if (!settings.mode().requiresMark()) {
            areaFirst = null;
            areaMode = null;
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            minecraft.player.sendOverlayMessage(Component.literal(settings.mode().markHint(settings.pattern())));
            return;
        }
        BlockPos selected = hit.getBlockPos().immutable();
        boolean areaOrder = settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                && settings.pattern() == DinoWhistleSettings.Pattern.AREA;
        boolean choosingFirst = !areaOrder || areaFirst == null;
        if (choosingFirst && !DinoFieldWorkRules.validTarget(minecraft.level, selected, settings.mode(), 4)) {
            minecraft.player.sendOverlayMessage(Component.literal(settings.mode().markHint(settings.pattern())));
            return;
        }
        if (areaOrder) {
            if (areaFirst == null || areaMode != settings.mode()) {
                areaFirst = selected;
                areaMode = settings.mode();
                minecraft.player.sendOverlayMessage(Component.literal(
                        "Block type saved. Mark the opposite corner."));
                return;
            }
            BlockPos first = areaFirst;
            areaFirst = null;
            areaMode = null;
            ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(first, selected, true));
            return;
        }
        areaFirst = null;
        areaMode = null;
        ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(selected, selected, false));
    }

    public static void renderInventoryHover(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)
                || minecraft.player == null) {
            resetHover();
            return;
        }
        Slot slot = container.getHoveredSlot();
        if (slot == null || !slot.getItem().is(ModItems.DINO_WHISTLE.get())) {
            resetHover();
            return;
        }
        int inventorySlot = resolveInventorySlot(minecraft, slot);
        if (inventorySlot < 0) {
            resetHover();
            return;
        }
        long now = Util.getNanos();
        if (inventorySlot != hoveredInventorySlot) {
            hoverStarted = now;
            hoveredInventorySlot = inventorySlot;
            openedFromHold = false;
        }
        boolean holdingShift = shiftDown(minecraft);
        if (!holdingShift) {
            hoverStarted = now;
            openedFromHold = false;
        }
        float progress = holdingShift
                ? Mth.clamp((now - hoverStarted) / (float)CONFIGURE_HOLD_NANOS, 0.0F, 1.0F)
                : 0.0F;
        drawSidebar(event.getGuiGraphics(), slot.getItem(), progress, holdingShift, 1.0F);
        if (progress >= 1.0F && !openedFromHold) {
            openedFromHold = true;
            DinoWhistleScreen.open(slot.getItem(), inventorySlot);
        }
    }

    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack whistle = minecraft.player == null || minecraft.screen != null
                ? ItemStack.EMPTY : DinoWhistleItem.findHeld(minecraft.player);
        boolean targetVisible = !whistle.isEmpty() && !minecraft.options.hideGui;
        long now = Util.getNanos();
        float delta = lastWorldFrame == 0L ? 1.0F / 60.0F
                : Mth.clamp((now - lastWorldFrame) / 1_000_000_000.0F, 0.001F, 0.05F);
        lastWorldFrame = now;
        float response = targetVisible ? 13.0F : 9.0F;
        worldSidebarVisibility = Mth.lerp(1.0F - (float)Math.exp(-response * delta),
                worldSidebarVisibility, targetVisible ? 1.0F : 0.0F);
        if (worldSidebarVisibility < 0.01F) return;
        ItemStack shown = whistle.isEmpty() ? new ItemStack(ModItems.DINO_WHISTLE.get()) : whistle;
        drawSidebar(event.getGuiGraphics(), shown, 0.0F, false, worldSidebarVisibility);
    }

    private static void drawSidebar(GuiGraphicsExtractor graphics, ItemStack whistle,
                                    float progress, boolean holdingShift, float visibility) {
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        float eased = visibility * visibility * (3.0F - 2.0F * visibility);
        int x = graphics.guiWidth() - SIDEBAR_WIDTH - 5;
        int y = Math.max(7, graphics.guiHeight() / 2 - SIDEBAR_HEIGHT / 2);
        float slide = (1.0F - eased) * (SIDEBAR_WIDTH + 10.0F);
        float settle = 0.97F + PrimevalBubbleUi.spring(eased, 7.0F, 10.8F) * 0.03F;

        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + SIDEBAR_WIDTH * 0.5F + slide, y + SIDEBAR_HEIGHT * 0.5F);
        graphics.pose().scale(settle, settle);
        graphics.pose().translate(-(x + SIDEBAR_WIDTH * 0.5F), -(y + SIDEBAR_HEIGHT * 0.5F));
        int alpha = Mth.clamp(Math.round(visibility * 255.0F), 0, 255);
        graphics.fill(x + 4, y + 5, x + SIDEBAR_WIDTH + 4, y + SIDEBAR_HEIGHT + 5,
                alpha * 0x4E / 0xFF << 24);
        PrimevalBubbleUi.draw(graphics, x, y, SIDEBAR_WIDTH, SIDEBAR_HEIGHT);
        graphics.fill(x + 5, y + 5, x + SIDEBAR_WIDTH - 5, y + SIDEBAR_HEIGHT - 5,
                alpha * 0x18 / 0xFF << 24 | 0x1B1210);
        bold(graphics, "DINO WHISTLE", x + 8, y + 6, withAlpha(RED, alpha), 0.76F);
        rightBold(graphics, settings.mode().title().toUpperCase(Locale.ROOT),
                x + SIDEBAR_WIDTH - 8, y + 6, 70, withAlpha(modeColor(settings.mode()), alpha), 0.66F);
        graphics.fill(x + 8, y + 18, x + SIDEBAR_WIDTH - 8, y + 19, withAlpha(0xFF8C6A50, alpha));
        boolean showingProgress = holdingShift || progress > 0.0F;
        fit(graphics, sidebarInstruction(settings), x + 8, y + 23,
                showingProgress ? SIDEBAR_WIDTH - 51 : SIDEBAR_WIDTH - 16,
                withAlpha(INK, alpha), 0.61F, true);
        fit(graphics, holdingShift ? "KEEP HOLDING SHIFT" : "HOLD SHIFT IN INVENTORY TO CONFIGURE",
                x + 8, y + 34, showingProgress ? SIDEBAR_WIDTH - 43 : SIDEBAR_WIDTH - 16,
                withAlpha(holdingShift ? modeColor(settings.mode()) : MUTED, alpha), 0.54F, true);
        if (showingProgress) {
            int frame = Math.min(WORK_PROGRESS.length - 1, Math.max(0,
                    (int)Math.floor(progress * WORK_PROGRESS.length)));
            graphics.blit(WORK_PROGRESS[frame], x + SIDEBAR_WIDTH - 37, y + 13,
                    x + SIDEBAR_WIDTH - 7, y + 43, 0.0F, 1.0F, 0.0F, 1.0F);
        }
        graphics.pose().popMatrix();
    }

    private static String sidebarInstruction(DinoWhistleSettings settings) {
        if (!settings.mode().requiresMark()) return settings.mode().targetDescription(settings.pattern());
        if (areaFirst != null) return "Mark the opposite corner";
        return settings.mode().markHint(settings.pattern());
    }

    private static int resolveInventorySlot(Minecraft minecraft, Slot hovered) {
        if (minecraft.player == null) return -1;
        if (hovered.container == minecraft.player.getInventory()) return hovered.getContainerSlot();
        ItemStack hoveredStack = hovered.getItem();
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            ItemStack candidate = minecraft.player.getInventory().getItem(index);
            if (candidate == hoveredStack) return index;
        }
        int match = -1;
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            ItemStack candidate = minecraft.player.getInventory().getItem(index);
            if (!ItemStack.isSameItemSameComponents(candidate, hoveredStack)) continue;
            if (match >= 0) return -1;
            match = index;
        }
        return match;
    }

    private static boolean shiftDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_RSHIFT);
    }

    private static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 0x00FFFFFF;
    }

    private static void resetHover() {
        hoverStarted = 0L;
        hoveredInventorySlot = -1;
        openedFromHold = false;
    }

    private static int modeColor(DinoWhistleSettings.FieldMode mode) {
        return switch (mode) {
            case QUARRY -> 0xFFC54B2D;
            case LUMBER -> 0xFF547B3F;
            case HARVEST -> 0xFFD09A16;
            case COLLECT -> 0xFF477895;
        };
    }

    private static void bold(GuiGraphicsExtractor graphics, String text, float x, float y, int color, float scale) {
        Component component = Component.literal(text).withStyle(Style.EMPTY.withBold(true));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(Minecraft.getInstance().font, component, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static void rightBold(GuiGraphicsExtractor graphics, String text, float right, float y,
                                  float maxWidth, int color, float requestedScale) {
        Component component = Component.literal(text).withStyle(Style.EMPTY.withBold(true));
        int width = Math.max(1, Minecraft.getInstance().font.width(component));
        float scale = Math.min(requestedScale, maxWidth / width);
        bold(graphics, text, right - width * scale, y, color, scale);
    }

    private static void fit(GuiGraphicsExtractor graphics, String text, float x, float y,
                            float maxWidth, int color, float requestedScale, boolean bold) {
        Component component = bold
                ? Component.literal(text).withStyle(Style.EMPTY.withBold(true))
                : Component.literal(text);
        int width = Math.max(1, Minecraft.getInstance().font.width(component));
        float scale = Math.min(requestedScale, maxWidth / width);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(Minecraft.getInstance().font, component, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static Identifier[] progressFrames() {
        Identifier[] frames = new Identifier[16];
        for (int index = 0; index < frames.length; index++) {
            frames[index] = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID,
                    "textures/entity/indicator/work/classic_work" + (index + 1) + ".png");
        }
        return frames;
    }
}
