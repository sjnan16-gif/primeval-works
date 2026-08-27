package com.primevalworks.client.effect;

import com.primevalworks.PrimevalWorks;
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
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Locale;

public final class DinoWhistleClient {
    private static final long CONFIGURE_HOLD_NANOS = 750_000_000L;
    private static final int TOOLTIP_WIDTH = 184;
    private static final int TOOLTIP_HEIGHT = 66;
    private static final int INK = 0xFF494341;
    private static final int MUTED = 0xFF6E6764;
    private static final int RED = 0xFFC74F43;
    private static final Identifier[] WORK_PROGRESS = progressFrames();

    private static BlockPos areaFirst;
    private static DinoWhistleSettings.FieldMode areaMode;
    private static long hoverStarted;
    private static int hoveredInventorySlot = -1;
    private static boolean openedFromHold;

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

    public static void cancelVanillaTooltip(RenderTooltipEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getItemStack().is(ModItems.DINO_WHISTLE.get())
                || !(minecraft.screen instanceof AbstractContainerScreen<?> container)
                || minecraft.player == null) return;
        Slot slot = container.getHoveredSlot();
        if (slot != null && slot.container == minecraft.player.getInventory()
                && slot.getItem().is(ModItems.DINO_WHISTLE.get())) {
            event.setCanceled(true);
        }
    }

    public static void renderInventoryHover(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)
                || minecraft.player == null) {
            resetHover();
            return;
        }
        Slot slot = container.getHoveredSlot();
        if (slot == null || slot.container != minecraft.player.getInventory()
                || !slot.getItem().is(ModItems.DINO_WHISTLE.get())) {
            resetHover();
            return;
        }
        int inventorySlot = slot.getContainerSlot();
        long now = Util.getNanos();
        if (inventorySlot != hoveredInventorySlot) {
            hoverStarted = now;
            hoveredInventorySlot = inventorySlot;
            openedFromHold = false;
        }
        boolean holdingShift = minecraft.options.keyShift.isDown();
        if (!holdingShift) {
            hoverStarted = now;
            openedFromHold = false;
        }
        float progress = holdingShift
                ? Mth.clamp((now - hoverStarted) / (float)CONFIGURE_HOLD_NANOS, 0.0F, 1.0F)
                : 0.0F;
        drawTooltip(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(),
                slot.getItem(), progress, holdingShift);
        if (progress >= 1.0F && !openedFromHold) {
            openedFromHold = true;
            DinoWhistleScreen.open(slot.getItem(), inventorySlot);
        }
    }

    private static void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                    ItemStack whistle, float progress, boolean holdingShift) {
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        int x = mouseX + 13;
        int y = mouseY + 11;
        if (x + TOOLTIP_WIDTH > graphics.guiWidth() - 4) x = mouseX - TOOLTIP_WIDTH - 13;
        if (y + TOOLTIP_HEIGHT > graphics.guiHeight() - 4) y = graphics.guiHeight() - TOOLTIP_HEIGHT - 4;
        x = Math.max(4, x);
        y = Math.max(4, y);

        graphics.nextStratum();
        graphics.fill(x + 4, y + 5, x + TOOLTIP_WIDTH + 4, y + TOOLTIP_HEIGHT + 5, 0x54000000);
        PrimevalBubbleUi.draw(graphics, x, y, TOOLTIP_WIDTH, TOOLTIP_HEIGHT);
        bold(graphics, "DINO WHISTLE", x + 8, y + 6, RED, 0.82F);
        rightBold(graphics, settings.mode().title().toUpperCase(Locale.ROOT),
                x + TOOLTIP_WIDTH - 8, y + 6, 80, modeColor(settings.mode()), 0.68F);
        graphics.fill(x + 8, y + 18, x + TOOLTIP_WIDTH - 8, y + 19, 0xFF8C6A50);

        fit(graphics, settings.mode().targetDescription(settings.pattern()),
                x + 8, y + 24, TOOLTIP_WIDTH - 16, INK, 0.65F, true);
        String detail = switch (settings.mode()) {
            case QUARRY -> settings.pattern() == DinoWhistleSettings.Pattern.AREA
                    ? "Matching block only  /  " + settings.range() + " block leash"
                    : "Connected match  /  " + settings.range() + " block leash";
            case LUMBER -> "Connected logs  /  " + settings.range() + " block leash";
            case HARVEST -> "Replants crops  /  " + settings.range() + " block leash";
            case COLLECT -> (settings.filtersItems() ? shortIdentifier(settings.itemFilter()) : "Any loose item")
                    + "  /  " + settings.range() + " block search";
        };
        fit(graphics, detail.toUpperCase(Locale.ROOT), x + 8, y + 36,
                TOOLTIP_WIDTH - 16, MUTED, 0.58F, true);
        fit(graphics, holdingShift ? "KEEP HOLDING SHIFT" : "HOLD SHIFT TO CONFIGURE",
                x + 8, y + 51, TOOLTIP_WIDTH - 48,
                holdingShift ? modeColor(settings.mode()) : MUTED, 0.61F, true);
        int frame = Math.min(WORK_PROGRESS.length - 1, Math.max(0,
                (int)Math.floor(progress * WORK_PROGRESS.length)));
        graphics.blit(WORK_PROGRESS[frame], x + TOOLTIP_WIDTH - 39, y + 34,
                x + TOOLTIP_WIDTH - 7, y + 66, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static void resetHover() {
        hoverStarted = 0L;
        hoveredInventorySlot = -1;
        openedFromHold = false;
    }

    private static String shortIdentifier(String identifier) {
        Identifier id = Identifier.tryParse(identifier);
        if (id == null) return "Selected item";
        String[] words = id.getPath().replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return result.toString().trim();
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
