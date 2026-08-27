package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.network.payload.AssignPassiveWhistleWorkPayload;
import com.primevalworks.network.payload.ConfigureDinoWhistlePayload;
import com.primevalworks.network.payload.PassiveWhistleFollowersPayload;
import com.primevalworks.network.payload.RequestPassiveWhistleFollowersPayload;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DinoWhistleScreen extends Screen {
    private static final int PANEL_WIDTH = 196;
    private static final int PANEL_HEIGHT = 142;
    private static final int PANEL_GAP = 8;
    private static final int SEARCH_PANEL_WIDTH = 198;
    private static final int SEARCH_PANEL_HEIGHT = 128;
    private static final float MAX_PANEL_SCALE = 1.08F;
    private static final int INK = 0xFF494341;
    private static final int MUTED = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
    private static final int LINE = 0xFF8A6750;
    private static final int[] MODE_COLORS = {0xFFC54B2D, 0xFF547B3F, 0xFFD09A16, 0xFF477895};
    private static final Identifier HOTBAR = texture("hotbar.png");
    private static final Identifier RANGE_BUTTON = texture("whistle_range_button.png");
    private static DinoWhistleScreen active;

    private final int inventorySlot;
    private final long[] hoverStarted = new long[16];
    private final List<PassiveWhistleFollowersPayload.Entry> followers = new ArrayList<>();
    private final List<ItemStack> catalogueItems = new ArrayList<>();
    private DinoWhistleSettings settings;
    private EditBox searchBox;
    private ItemStack draggedItem = ItemStack.EMPTY;
    private List<Component> hoverTooltip;
    private long openedAt;
    private long renderNow;
    private long previousFrame;
    private long pressedAt;
    private int pressedKey = -1;
    private float parallaxX;
    private float parallaxY;
    private float searchReveal;
    private float searchVelocity;
    private boolean searchOpen;
    private boolean draggingRange;
    private int searchScrollRow;

    private DinoWhistleScreen(ItemStack whistle, int inventorySlot) {
        super(Component.literal("Dino Whistle"));
        settings = DinoWhistleSettings.read(whistle);
        this.inventorySlot = inventorySlot;
    }

    public static void open(ItemStack whistle, int inventorySlot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !whistle.is(ModItems.DINO_WHISTLE.get())) return;
        active = new DinoWhistleScreen(whistle, inventorySlot);
        minecraft.setScreen(active);
    }

    public static void acceptFollowers(PassiveWhistleFollowersPayload payload) {
        DinoWhistleScreen screen = active;
        if (screen == null || screen.inventorySlot != payload.inventorySlot()) return;
        screen.followers.clear();
        screen.followers.addAll(payload.entries());
    }

    @Override
    protected void init() {
        openedAt = Util.getNanos();
        previousFrame = openedAt;
        searchBox = new EditBox(font, 0, 0, 80, 12, Component.literal("Search item types"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(40);
        searchBox.setTextColor(INK);
        searchBox.setTextShadow(true);
        searchBox.setHint(Component.literal("Search item types"));
        searchBox.setVisible(false);
        addRenderableWidget(searchBox);
        refreshCatalogueItems();
        requestFollowers();
        PrimevalUiSounds.open(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        hoverTooltip = null;
        float deltaTime = frameDelta();
        updateParallax(mouseX, mouseY, deltaTime);
        updateSearchMotion(deltaTime);
        Motion motion = motion();
        float logicalMouseX = (float)motion.inverseX(mouseX);
        float logicalMouseY = (float)motion.inverseY(mouseY);
        updateSearchBox(motion);

        graphics.fill(0, 0, width, height, 0x9608050D);
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        drawPanel(graphics, mainPanel(), logicalMouseX, logicalMouseY);
        if (searchReveal > 0.005F) {
            hoverTooltip = null;
            drawSearchPicker(graphics, searchPanel(), logicalMouseX, logicalMouseY);
        }
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (hoverTooltip != null) {
            graphics.nextStratum();
            graphics.setComponentTooltipForNextFrame(font, hoverTooltip, mouseX, mouseY);
        }
        if (!draggedItem.isEmpty()) {
            graphics.nextStratum();
            graphics.item(draggedItem, mouseX - 8, mouseY - 8);
            graphics.itemDecorations(font, draggedItem, mouseX - 8, mouseY - 8);
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        Rect header = headerRect(panel);
        drawBubble(graphics, header);
        bold(graphics, "DINO WHISTLE", header.x + 7, header.y + 5, LABEL, 0.86F);
        Rect duty = new Rect(header.right() - 75, header.y + 3, 72, header.h - 6);
        drawInsetBubble(graphics, duty);
        centeredText(graphics, settings.mode().isPassive() ? "AUTOMATIC DUTY" : "MARKED DUTY",
                duty, MUTED, 0.66F);

        Rect order = orderRect(panel);
        boolean orderHovered = order.contains(mouseX, mouseY);
        drawInteractiveRegion(graphics, order, 0, orderHovered, () -> {
            drawCycleRow(graphics, order, orderHovered, modeColor(),
                    "ORDER", settings.mode().title().toUpperCase(Locale.ROOT));
            if (orderHovered) {
                hoverTooltip = tooltip(settings.mode().title(), modeColor(),
                        settings.mode().description(), "Click to choose the next field order.");
            }
        });

        Rect behavior = behaviorRect(panel);
        boolean cycles = settings.mode() == DinoWhistleSettings.FieldMode.QUARRY;
        boolean behaviorHovered = behavior.contains(mouseX, mouseY);
        drawInteractiveRegion(graphics, behavior, 1, behaviorHovered, () -> {
            drawBubble(graphics, behavior);
            if (behaviorHovered) {
                graphics.fill(behavior.x + 2, behavior.y + 2, behavior.right() - 2, behavior.bottom() - 2,
                        0x18FFFFFF);
                if (cycles) graphics.requestCursor(CursorTypes.POINTING_HAND);
                hoverTooltip = tooltip(settings.mode().targetTitle(settings.pattern()), modeColor(),
                        settings.mode().targetDescription(settings.pattern()),
                        cycles ? "Click to switch between a connected vein and a marked area."
                                : settings.mode().markHint(settings.pattern()));
            }
            Rect behaviorValue = new Rect(behavior.x + 61, behavior.y + 3, behavior.w - 64, behavior.h - 6);
            drawInsetBubble(graphics, behaviorValue);
            bold(graphics, settings.mode().isPassive() ? "BEHAVIOR" : "TARGET",
                    behavior.x + 7, behavior.y + 11, behaviorHovered ? modeColor() : MUTED, 0.72F);
            String target = settings.mode().targetTitle(settings.pattern()).toUpperCase(Locale.ROOT)
                    + (cycles ? "  >" : "");
            bold(graphics, target,
                    behaviorValue.x + 5, behaviorValue.y + 4,
                    behaviorHovered ? modeColor() : INK, 0.74F);
            drawWrappedText(graphics, settings.mode().targetDescription(settings.pattern()),
                    behaviorValue.x + 5, behaviorValue.y + 13, behaviorValue.w - 10,
                    behaviorHovered ? modeColor() : MUTED, 0.62F, 2);
        });

        Rect range = rangeRect(panel);
        boolean rangeHovered = range.contains(mouseX, mouseY);
        drawInteractiveRegion(graphics, range, 2, rangeHovered || draggingRange,
                () -> drawRange(graphics, range, mouseX, mouseY));
        Rect followers = followerRect(panel);
        boolean followersHovered = followers.contains(mouseX, mouseY);
        drawInteractiveRegion(graphics, followers, 3, followersHovered,
                () -> drawFollowerRow(graphics, followers, mouseX, mouseY));
    }

    private void drawFollowerRow(GuiGraphicsExtractor graphics, Rect row, float mouseX, float mouseY) {
        drawBubble(graphics, row);
        if (!settings.mode().isPassive()) {
            bold(graphics, "ASSIGN", row.x + 7, row.y + 12, MUTED, 0.70F);
            Rect instruction = new Rect(row.x + 52, row.y + 4, row.w - 56, row.h - 8);
            drawInsetBubble(graphics, instruction);
            drawWrappedText(graphics, settings.mode().markHint(settings.pattern()).toUpperCase(Locale.ROOT),
                    instruction.x + 5, instruction.y + 5, instruction.w - 10, modeColor(), 0.66F, 2);
            if (row.contains(mouseX, mouseY)) {
                hoverTooltip = tooltip("Assign in the world", modeColor(),
                        settings.mode().targetDescription(settings.pattern()), settings.mode().markHint(settings.pattern()));
            }
            return;
        }

        bold(graphics, "FOLLOWER", row.x + 6, row.y + 13, MUTED, 0.64F);
        int maximum = 3;
        for (int index = 0; index < Math.min(maximum, followers.size()); index++) {
            Rect slot = followerSlot(row, index);
            PassiveWhistleFollowersPayload.Entry entry = followers.get(index);
            boolean hovered = slot.contains(mouseX, mouseY);
            drawFollowerSlot(graphics, slot, entry, hovered, index);
            if (hovered) {
                hoverTooltip = tooltip(entry.name(), entry.compatible() ? modeColor() : 0xFF9C5149,
                        entry.compatible() ? entry.rating() + " star field rating" : "This species cannot do this order.",
                        entry.assigned() ? "This duty is currently assigned." : "Click to assign this follower.");
            }
        }
        if (followers.isEmpty()) {
            Rect empty = new Rect(row.x + 52, row.y + 5,
                    settings.mode() == DinoWhistleSettings.FieldMode.COLLECT ? 89 : row.w - 57, row.h - 10);
            drawInsetBubble(graphics, empty);
            centeredText(graphics, "NO COMPATIBLE FOLLOWER", empty, MUTED, 0.61F);
        }
        if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT) {
            Rect filter = filterSlot(row);
            boolean hovered = filter.contains(mouseX, mouseY);
            drawHotbar(graphics, filter, hovered);
            ItemStack selected = filterStack();
            if (selected.isEmpty()) centeredText(graphics, "+", filter, modeColor(), 1.0F);
            else graphics.item(selected, filter.x + 1, filter.y + 1);
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                hoverTooltip = tooltip(selected.isEmpty() ? "Any loose item" : selected.getHoverName().getString(),
                        modeColor(), selected.isEmpty() ? "No item filter is set." : "Only this item will be collected.",
                        "Click to open the inventory filter.");
            }
        }
    }

    private void drawFollowerSlot(GuiGraphicsExtractor graphics, Rect slot,
                                  PassiveWhistleFollowersPayload.Entry entry, boolean hovered, int index) {
        updateHover(index + 4, hovered);
        float scale = 1.0F + interactionMotion(index + 4, hovered) * 0.07F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.centerX(), slot.centerY());
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-slot.centerX(), -slot.centerY());
        drawHotbar(graphics, slot, hovered);
        graphics.pose().popMatrix();
        if (entry.assigned()) {
            graphics.fill(slot.x + 2, slot.y + 2, slot.right() - 2, slot.bottom() - 2, 0x3654A36A);
        }
        FieldDodoEntity dinosaur = entity(entry.entityId());
        if (dinosaur != null) {
            DinosaurPreviewUi.draw(graphics, dinosaur, slot.x + 1, slot.y + 3,
                    slot.w - 4, slot.h - 5, 42.0F, -25.0F);
        }
        if (!entry.compatible()) {
            graphics.fill(slot.x + 2, slot.y + 2, slot.right() - 2, slot.bottom() - 2, 0x92211C20);
        } else if (hovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void drawRange(GuiGraphicsExtractor graphics, Rect range, float mouseX, float mouseY) {
        boolean hovered = range.contains(mouseX, mouseY);
        drawBubble(graphics, range);
        if (hovered || draggingRange) {
            graphics.fill(range.x + 2, range.y + 2, range.right() - 2, range.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            hoverTooltip = tooltip(settings.mode() == DinoWhistleSettings.FieldMode.COLLECT ? "Search range" : "Leash",
                    modeColor(), "The dinosaur stops field work outside this distance.",
                    "Drag the marker to set the range.");
        }
        bold(graphics, settings.mode() == DinoWhistleSettings.FieldMode.COLLECT ? "SEARCH" : "LEASH",
                range.x + 7, range.y + 8, hovered || draggingRange ? modeColor() : MUTED, 0.66F);
        Rect control = new Rect(range.x + 53, range.y + 3, range.w - 56, range.h - 6);
        drawInsetBubble(graphics, control);
        rightText(graphics, settings.range() + "M", control.right() - 6, control.y + 4,
                30, hovered || draggingRange ? modeColor() : INK, 0.66F);
        int trackLeft = control.x + 6;
        int trackRight = control.right() - 35;
        int trackY = control.y + control.h / 2;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, LINE);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = trackLeft + Math.round(ratio * (trackRight - trackLeft));
        blit(graphics, RANGE_BUTTON, new Rect(knobX - 5, trackY - 5, 10, 10));
    }

    private void drawCycleRow(GuiGraphicsExtractor graphics, Rect row, boolean hovered,
                              int accent, String label, String value) {
        drawBubble(graphics, row);
        if (hovered) {
            graphics.fill(row.x + 2, row.y + 2, row.right() - 2, row.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        Rect valueBubble = new Rect(row.x + 54, row.y + 3, row.w - 57, row.h - 6);
        drawInsetBubble(graphics, valueBubble);
        bold(graphics, label, row.x + 7, row.y + 7, hovered ? accent : MUTED, 0.70F);
        centeredText(graphics, value + "  >", valueBubble, hovered ? accent : INK, 0.74F);
    }

    private void drawSearchPicker(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        float settled = PrimevalBubbleUi.spring(Mth.clamp(searchReveal, 0.0F, 1.0F), 6.4F, 10.8F);
        float scale = 0.88F + 0.12F * settled;
        float offsetY = 12.0F * (1.0F - settled);
        int alpha = Math.round(255.0F * Mth.clamp(searchReveal * 1.25F, 0.0F, 1.0F));
        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.centerX(), panel.bottom() + offsetY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-panel.centerX(), -panel.bottom());

        Rect target = searchTarget(panel);
        Rect search = searchRect(panel);
        boolean targetHovered = target.contains(mouseX, mouseY);
        drawSearchInventorySlot(graphics, target, targetHovered, 0, alpha);
        ItemStack selected = filterStack();
        if (selected.isEmpty()) centeredText(graphics, "+", target, modeColor(), 1.0F);
        else graphics.item(selected, target.x + 2, target.y + 2);
        if (targetHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            hoverTooltip = tooltip(selected.isEmpty() ? "Any item" : selected.getHoverName().getString(),
                    modeColor(), selected.isEmpty() ? "No filter is active." : "The current collection filter.",
                    selected.isEmpty() ? "Drag an inventory item into this slot." : "Click to clear it.");
        }
        int revealedWidth = Math.max(4, Math.round(search.w * searchReveal));
        drawBubble(graphics, new Rect(search.x, search.y, revealedWidth, search.h));
        if (searchBox != null && searchBox.getValue().isBlank() && searchReveal > 0.82F) {
            bold(graphics, "SEARCH ITEM TYPES", search.x + 6, search.y + 5, MUTED, 0.66F);
        }

        Rect results = searchResults(panel);
        drawSearchPaperPanel(graphics, results, alpha);
        List<ItemStack> items = filteredCatalogue();
        int maximumRow = Math.max(0, (items.size() - 1) / 9 - 3);
        searchScrollRow = Mth.clamp(searchScrollRow, 0, maximumRow);
        String header = items.isEmpty() ? "NO MATCHES" : items.size() + " ITEM TYPES  /  DRAG A FILTER";
        bold(graphics, header, results.x + 8, results.y + 6,
                modeColor(), 0.70F);

        int startIndex = searchScrollRow * 9;
        for (int index = 0; index < 36; index++) {
            Rect slot = searchItemSlot(results, index);
            int itemIndex = startIndex + index;
            ItemStack stack = itemIndex < items.size() ? items.get(itemIndex) : ItemStack.EMPTY;
            boolean hovered = slot.contains(mouseX, mouseY);
            drawSearchInventorySlot(graphics, slot, hovered && !stack.isEmpty(), index, alpha);
            if (!stack.isEmpty()) {
                graphics.item(stack, slot.x + 2, slot.y + 2);
                graphics.itemDecorations(font, stack, slot.x + 2, slot.y + 2);
            }
            if (hovered && !stack.isEmpty()) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                hoverTooltip = List.of(
                        stack.getHoverName().copy().withStyle(Style.EMPTY.withBold(true)),
                        Component.literal("Drag into the cargo filter slot.").withStyle(style -> style.withColor(0x6E6764))
                );
            }
        }
        if (maximumRow > 0) {
            String page = (searchScrollRow + 1) + " / " + (maximumRow + 1) + "  •  SCROLL";
            rightText(graphics, page, results.right() - 8, results.bottom() - 10,
                    results.w - 16, MUTED, 0.58F);
        }
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Motion motion = motion();
        double mouseX = motion.inverseX(event.x());
        double mouseY = motion.inverseY(event.y());
        if (searchReveal > 0.5F) {
            Rect panel = searchPanel();
            Rect results = searchResults(panel);
            if (searchRect(panel).contains(mouseX, mouseY)) {
                setFocused(searchBox);
                searchBox.setFocused(true);
                return true;
            }
            List<ItemStack> items = filteredCatalogue();
            int startIndex = searchScrollRow * 9;
            for (int index = 0; index < 36; index++) {
                int itemIndex = startIndex + index;
                if (!searchItemSlot(results, index).contains(mouseX, mouseY) || itemIndex >= items.size()) continue;
                ItemStack stack = items.get(itemIndex);
                if (!stack.isEmpty()) {
                    draggedItem = stack.copyWithCount(1);
                    pressed(6);
                    return true;
                }
            }
            if (searchTarget(panel).contains(mouseX, mouseY) && !settings.itemFilter().isBlank()) {
                settings = copy(settings.mode(), settings.pattern(), settings.range(), "");
                changed(0.88F, 6);
                return true;
            }
            if (!panel.contains(mouseX, mouseY)) closeSearch();
            return true;
        }

        double localX = mouseX;
        Rect panel = mainPanel();
        if (orderRect(panel).contains(localX, mouseY)) {
            DinoWhistleSettings.FieldMode[] values = DinoWhistleSettings.FieldMode.values();
            DinoWhistleSettings.FieldMode mode = values[(settings.mode().ordinal() + 1) % values.length];
            settings = copy(mode, mode.normalizePattern(settings.pattern()), settings.range(), settings.itemFilter());
            changed(0.96F, 0);
            requestFollowers();
            return true;
        }
        if (settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                && behaviorRect(panel).contains(localX, mouseY)) {
            DinoWhistleSettings.Pattern pattern = settings.pattern() == DinoWhistleSettings.Pattern.AREA
                    ? DinoWhistleSettings.Pattern.CONNECTED : DinoWhistleSettings.Pattern.AREA;
            settings = copy(settings.mode(), pattern, settings.range(), settings.itemFilter());
            changed(1.02F, 1);
            return true;
        }
        if (rangeRect(panel).contains(localX, mouseY)) {
            draggingRange = true;
            pressed(2);
            updateRange(localX);
            return true;
        }
        if (settings.mode().isPassive()) {
            Rect row = followerRect(panel);
            int maximum = 3;
            for (int index = 0; index < Math.min(maximum, followers.size()); index++) {
                if (!followerSlot(row, index).contains(localX, mouseY)) continue;
                PassiveWhistleFollowersPayload.Entry entry = followers.get(index);
                if (entry.compatible()) {
                    ClientPacketDistributor.sendToServer(
                            new AssignPassiveWhistleWorkPayload(inventorySlot, entry.uuid()));
                    PrimevalUiSounds.click(1.08F);
                } else PrimevalUiSounds.click(0.72F);
                return true;
            }
            if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT
                    && filterSlot(row).contains(localX, mouseY)) {
                openSearch();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Motion motion = motion();
        double logicalX = motion.inverseX(mouseX);
        double logicalY = motion.inverseY(mouseY);
        Rect panel = searchPanel();
        if (searchReveal > 0.08F && searchResults(panel).contains(logicalX, logicalY)) {
            List<ItemStack> filtered = filteredCatalogue();
            int maximumRow = Math.max(0, (filtered.size() - 1) / 9 - 3);
            searchScrollRow = Mth.clamp(searchScrollRow + (scrollY < 0.0D ? 1 : -1), 0, maximumRow);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRange && event.button() == 0) {
            updateRange(motion().inverseX(event.x()));
            return true;
        }
        return !draggedItem.isEmpty() || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingRange) {
            draggingRange = false;
            return true;
        }
        if (event.button() == 0 && !draggedItem.isEmpty()) {
            Motion motion = motion();
            double mouseX = motion.inverseX(event.x());
            double mouseY = motion.inverseY(event.y());
            Rect panel = searchPanel();
            boolean choseItem = searchTarget(panel).contains(mouseX, mouseY);
            if (!choseItem) {
                Rect results = searchResults(panel);
                for (int index = 0; index < 36; index++) {
                    if (searchItemSlot(results, index).contains(mouseX, mouseY)) {
                        choseItem = true;
                        break;
                    }
                }
            }
            if (choseItem) {
                settings = copy(settings.mode(), settings.pattern(), settings.range(),
                        BuiltInRegistries.ITEM.getKey(draggedItem.getItem()).toString());
                changed(1.12F, 6);
                closeSearch();
            }
            draggedItem = ItemStack.EMPTY;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && searchOpen) {
            closeSearch();
            return true;
        }
        return super.keyPressed(event);
    }

    private void openSearch() {
        searchOpen = true;
        searchScrollRow = 0;
        if (searchBox != null) {
            setFocused(searchBox);
            searchBox.setFocused(true);
        }
        PrimevalUiSounds.click(1.02F);
    }

    private void closeSearch() {
        searchOpen = false;
        draggedItem = ItemStack.EMPTY;
        if (searchBox != null) {
            searchBox.setFocused(false);
            if (getFocused() == searchBox) setFocused(null);
        }
    }

    private void updateRange(double mouseX) {
        Rect range = rangeRect(mainPanel());
        Rect control = new Rect(range.x + 53, range.y + 3, range.w - 56, range.h - 6);
        int left = control.x + 6;
        int right = control.right() - 35;
        float ratio = Mth.clamp((float)((mouseX - left) / (right - left)), 0.0F, 1.0F);
        int value = Math.round(Mth.lerp(ratio, DinoWhistleSettings.MIN_RANGE, DinoWhistleSettings.MAX_RANGE));
        if (value != settings.range()) {
            settings = copy(settings.mode(), settings.pattern(), value, settings.itemFilter());
            send();
        }
    }

    private void updateSearchBox(Motion motion) {
        if (searchBox == null) return;
        Rect logical = searchRect(searchPanel());
        float left = motion.screenX(logical.x + 5);
        float top = motion.screenY(logical.y + 2);
        float right = motion.screenX(logical.right() - 5);
        float bottom = motion.screenY(logical.bottom() - 2);
        searchBox.setX(Math.round(left));
        searchBox.setY(Math.round(top));
        searchBox.setWidth(Math.max(20, Math.round(right - left)));
        searchBox.setHeight(Math.max(9, Math.round(bottom - top)));
        searchBox.setAlpha(Mth.clamp(searchReveal, 0.0F, 1.0F));
        searchBox.setVisible(searchOpen && searchReveal > 0.82F);
    }

    private void refreshCatalogueItems() {
        catalogueItems.clear();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (!stack.isEmpty() && item != Items.AIR) catalogueItems.add(stack.copyWithCount(1));
        }
        catalogueItems.sort(Comparator
                .comparing((ItemStack stack) -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
    }

    private List<ItemStack> filteredCatalogue() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return catalogueItems;
        String normalized = query.replace('_', ' ').trim();
        String stem = normalized.endsWith("ies") && normalized.length() > 4
                ? normalized.substring(0, normalized.length() - 3)
                : normalized.endsWith("y") && normalized.length() > 2
                        ? normalized.substring(0, normalized.length() - 1)
                        : normalized.endsWith("s") && normalized.length() > 3
                                ? normalized.substring(0, normalized.length() - 1)
                                : normalized;
        return catalogueItems.stream().filter(stack -> {
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            return name.contains(normalized) || id.contains(normalized.replace(' ', '_'))
                    || stem.length() >= 3 && (name.contains(stem) || id.contains(stem.replace(' ', '_')));
        }).toList();
    }

    private ItemStack filterStack() {
        if (!settings.filtersItems()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(settings.itemFilter());
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.get(id)
                .map(holder -> holder.value().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }

    private FieldDodoEntity entity(int entityId) {
        if (minecraft == null || minecraft.level == null) return null;
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof FieldDodoEntity dinosaur ? dinosaur : null;
    }

    private DinoWhistleSettings copy(DinoWhistleSettings.FieldMode mode,
                                      DinoWhistleSettings.Pattern pattern, int range, String itemFilter) {
        return new DinoWhistleSettings(mode, pattern, range,
                mode == DinoWhistleSettings.FieldMode.COLLECT ? itemFilter : "");
    }

    private void changed(float pitch, int key) {
        pressed(key);
        PrimevalUiSounds.click(pitch);
        send();
    }

    private void pressed(int key) {
        pressedKey = key;
        pressedAt = Util.getNanos();
    }

    private void send() {
        ClientPacketDistributor.sendToServer(new ConfigureDinoWhistlePayload(inventorySlot,
                settings.mode().ordinal(), settings.pattern().ordinal(), settings.range(), settings.itemFilter()));
    }

    private void requestFollowers() {
        followers.clear();
        if (settings.mode().isPassive()) {
            ClientPacketDistributor.sendToServer(new RequestPassiveWhistleFollowersPayload(inventorySlot));
        }
    }

    @Override
    public void onClose() {
        send();
        active = null;
        PrimevalUiSounds.close(this);
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private Rect mainPanel() {
        float slide = smoothStep(searchReveal);
        int closedX = (width - PANEL_WIDTH) / 2;
        int openX = (width - PANEL_WIDTH - PANEL_GAP - SEARCH_PANEL_WIDTH) / 2;
        return new Rect(Math.round(Mth.lerp(slide, closedX, openX)),
                (height - PANEL_HEIGHT) / 2, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private Rect searchPanel() {
        float slide = smoothStep(searchReveal);
        int closedX = (width - SEARCH_PANEL_WIDTH) / 2;
        int openMainX = (width - PANEL_WIDTH - PANEL_GAP - SEARCH_PANEL_WIDTH) / 2;
        int openX = openMainX + PANEL_WIDTH + PANEL_GAP;
        return new Rect(Math.round(Mth.lerp(slide, closedX, openX)),
                (height - SEARCH_PANEL_HEIGHT) / 2, SEARCH_PANEL_WIDTH, SEARCH_PANEL_HEIGHT);
    }
    private Rect headerRect(Rect panel) { return new Rect(panel.x, panel.y, panel.w, 19); }
    private Rect orderRect(Rect panel) { return new Rect(panel.x, panel.y + 22, panel.w, 23); }
    private Rect behaviorRect(Rect panel) { return new Rect(panel.x, panel.y + 48, panel.w, 32); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x, panel.y + 83, panel.w, 25); }
    private Rect followerRect(Rect panel) { return new Rect(panel.x, panel.y + 111, panel.w, 31); }
    private Rect followerSlot(Rect row, int index) { return new Rect(row.x + 53 + index * 24, row.y + 6, 18, 18); }
    private Rect filterSlot(Rect row) { return new Rect(row.right() - 24, row.y + 6, 18, 18); }
    private Rect searchTarget(Rect panel) { return new Rect(panel.x, panel.y, 20, 20); }
    private Rect searchRect(Rect panel) { return new Rect(panel.x + 23, panel.y + 1, panel.w - 23, 18); }
    private Rect searchResults(Rect panel) { return new Rect(panel.x, panel.y + 24, panel.w, panel.h - 24); }
    private Rect searchItemSlot(Rect results, int index) {
        return new Rect(results.x + 9 + index % 9 * 20, results.y + 22 + index / 9 * 20, 20, 20);
    }

    private int modeColor() { return MODE_COLORS[settings.mode().ordinal()]; }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect) {
        graphics.fill(rect.x + 3, rect.y + 4, rect.right() + 3, rect.bottom() + 4, 0x52000000);
        PrimevalBubbleUi.draw(graphics, rect.x, rect.y, rect.w, rect.h);
    }

    private void drawInsetBubble(GuiGraphicsExtractor graphics, Rect rect) {
        PrimevalBubbleUi.draw(graphics, rect.x, rect.y, rect.w, rect.h);
        graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x0D5D4436);
    }

    private void drawSearchPaperPanel(GuiGraphicsExtractor graphics, Rect panel, int alpha) {
        graphics.fill(panel.x + 4, panel.y + 5, panel.right() + 4, panel.bottom() + 5,
                withAlpha(0xFF000000, Math.round(alpha * 0.28F)));
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), withAlpha(0xFF4A332C, alpha));
        graphics.fill(panel.x + 2, panel.y + 2, panel.right() - 2, panel.bottom() - 2,
                withAlpha(0xFF88664F, alpha));
        graphics.fill(panel.x + 4, panel.y + 4, panel.right() - 4, panel.bottom() - 4,
                withAlpha(0xFFD0AD89, alpha));
        for (int y = panel.y + 7; y < panel.bottom() - 5; y += 7) {
            graphics.fill(panel.x + 5, y, panel.right() - 5, y + 1,
                    withAlpha(0xFFFFFFFF, Math.round(alpha * 0.07F)));
        }
    }

    private void drawSearchInventorySlot(GuiGraphicsExtractor graphics, Rect rect,
                                         boolean hovered, int index, int alpha) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), withAlpha(0xFF4A332C, alpha));
        graphics.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1,
                withAlpha(0xFF9A765A, alpha));
        graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2,
                withAlpha(0xFFD7B392, alpha));
        if (hovered) {
            int pulse = 92 + Math.round((Mth.sin(renderNow / 50_000_000.0F * 0.24F + index) + 1.0F) * 46.0F);
            outline(graphics, new Rect(rect.x - 1, rect.y - 1, rect.w + 2, rect.h + 2),
                    withAlpha(0xFFFFFFFF, pulse));
            graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2,
                    withAlpha(modeColor(), 34));
        }
    }

    private void drawHotbar(GuiGraphicsExtractor graphics, Rect rect, boolean hovered) {
        blit(graphics, HOTBAR, rect);
        if (hovered) graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x24FFFFFF);
    }

    private void outline(GuiGraphicsExtractor graphics, Rect rect, int color) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.y + 1, color);
        graphics.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x, rect.y + 1, rect.x + 1, rect.bottom() - 1, color);
        graphics.fill(rect.right() - 1, rect.y + 1, rect.right(), rect.bottom() - 1, color);
    }

    private static int withAlpha(int color, int alpha) {
        return Mth.clamp(alpha, 0, 255) << 24 | color & 0x00FFFFFF;
    }

    private List<Component> tooltip(String title, int color, String detail, String action) {
        List<Component> result = new ArrayList<>();
        result.add(Component.literal(title).withStyle(style -> style.withBold(true).withColor(color & 0xFFFFFF)));
        result.add(Component.literal("----------------").withStyle(style -> style.withColor(0x5F5652)));
        wrapTooltip(detail, 130).forEach(line -> result.add(Component.literal(line)
                .withStyle(style -> style.withColor(0xB6AFAB))));
        wrapTooltip(action, 130).forEach(line -> result.add(Component.literal(line)
                .withStyle(style -> style.withBold(true).withColor(0xE6C36F))));
        return List.copyOf(result);
    }

    private List<String> wrapTooltip(String value, int maximumWidth) {
        List<String> result = new ArrayList<>();
        String current = "";
        for (String word : value.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.width(candidate) > maximumWidth) {
                result.add(current);
                current = word;
            } else {
                current = candidate;
            }
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    private float frameDelta() {
        float delta = Mth.clamp((renderNow - previousFrame) / 1_000_000_000.0F, 0.001F, 0.05F);
        previousFrame = renderNow;
        return delta;
    }

    private void updateParallax(int mouseX, int mouseY, float delta) {
        float targetX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * -0.7F;
        float targetY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * -0.45F;
        float blend = 1.0F - (float)Math.exp(-delta * 10.0F);
        parallaxX = Mth.lerp(blend, parallaxX, targetX);
        parallaxY = Mth.lerp(blend, parallaxY, targetY);
    }

    private void updateSearchMotion(float deltaTime) {
        float target = searchOpen ? 1.0F : 0.0F;
        float acceleration = (target - searchReveal) * 58.0F - searchVelocity * 14.0F;
        searchVelocity += acceleration * deltaTime;
        searchReveal = Mth.clamp(searchReveal + searchVelocity * deltaTime, 0.0F, 1.0F);
    }

    private Motion motion() {
        long now = renderNow == 0L ? Util.getNanos() : renderNow;
        float progress = Mth.clamp((now - openedAt) / 380_000_000.0F, 0.0F, 1.0F);
        float settled = PrimevalBubbleUi.spring(progress, 6.4F, 11.6F);
        float fitted = Math.min(MAX_PANEL_SCALE,
                Math.min((width - 12.0F) / (PANEL_WIDTH + PANEL_GAP + SEARCH_PANEL_WIDTH),
                        (height - 12.0F) / Math.max(PANEL_HEIGHT, SEARCH_PANEL_HEIGHT)));
        float scale = Math.max(0.62F, fitted) * Math.max(0.1F, 0.72F + 0.28F * settled);
        return new Motion(width * 0.5F, height * 0.5F, parallaxX,
                18.0F * (1.0F - settled) + parallaxY, scale);
    }

    private void applyMotion(GuiGraphicsExtractor graphics, Motion motion) {
        graphics.pose().translate(motion.pivotX + motion.offsetX, motion.pivotY + motion.offsetY);
        graphics.pose().scale(motion.scale, motion.scale);
        graphics.pose().translate(-motion.pivotX, -motion.pivotY);
    }

    private void drawMovingText(GuiGraphicsExtractor graphics, Rect rect, int key, boolean hovered, Runnable draw) {
        updateHover(key, hovered);
        float amount = interactionMotion(key, hovered);
        float press = key == pressedKey
                ? Mth.clamp(1.0F - (renderNow - pressedAt) / 260_000_000.0F, 0.0F, 1.0F) : 0.0F;
        float scale = 1.0F + amount * 0.035F - Mth.sin(press * Mth.PI) * 0.035F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.centerX(), rect.centerY());
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-rect.centerX(), -rect.centerY());
        draw.run();
        graphics.pose().popMatrix();
    }

    private void drawInteractiveRegion(GuiGraphicsExtractor graphics, Rect rect, int key,
                                       boolean hovered, Runnable draw) {
        updateHover(key, hovered);
        float amount = interactionMotion(key, hovered);
        float wobbleX = Mth.sin(renderNow / 1_000_000_000.0F * 7.1F + key * 0.83F) * 0.45F * amount;
        float wobbleY = Mth.sin(renderNow / 1_000_000_000.0F * 8.4F + key * 1.17F) * 0.26F * amount;
        float press = key == pressedKey
                ? Mth.clamp(1.0F - (renderNow - pressedAt) / 260_000_000.0F, 0.0F, 1.0F) : 0.0F;
        float scale = 1.0F + amount * 0.026F - Mth.sin(press * Mth.PI) * 0.032F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.centerX() + wobbleX, rect.centerY() + wobbleY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-rect.centerX(), -rect.centerY());
        draw.run();
        graphics.pose().popMatrix();
    }

    private void updateHover(int key, boolean hovered) {
        if (hovered) {
            if (hoverStarted[key] == 0L) hoverStarted[key] = renderNow;
        } else hoverStarted[key] = 0L;
    }

    private float interactionMotion(int key, boolean hovered) {
        if (!hovered || hoverStarted[key] == 0L) return 0.0F;
        float seconds = (renderNow - hoverStarted[key]) / 1_000_000_000.0F;
        float motion = (1.0F - (float)Math.exp(-seconds * 18.0F)) * (float)Math.exp(-seconds * 2.8F);
        return seconds >= 1.35F ? 0.0F : motion;
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void bold(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value).withStyle(Style.EMPTY.withBold(true)), x, y, color, scale);
    }

    private void rightText(GuiGraphicsExtractor graphics, String value, float right, float y,
                           float maxWidth, int color, float requestedScale) {
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(requestedScale, maxWidth / textWidth);
        drawText(graphics, component, right - textWidth * scale, y, color, scale);
    }

    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale, boolean heavy) {
        Component component = heavy
                ? Component.literal(value).withStyle(Style.EMPTY.withBold(true)) : Component.literal(value);
        int textWidth = Math.max(1, font.width(component));
        drawText(graphics, component, x, y, color, Math.min(requestedScale, maxWidth / textWidth));
    }

    private void centeredText(GuiGraphicsExtractor graphics, String value, Rect rect,
                              int color, float requestedScale) {
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(requestedScale, Math.max(0.52F, (rect.w - 6.0F) / textWidth));
        float x = rect.centerX() - textWidth * scale * 0.5F;
        float y = rect.centerY() - font.lineHeight * scale * 0.5F;
        drawText(graphics, component, x, y, color, scale);
    }

    private void drawWrappedText(GuiGraphicsExtractor graphics, String value, float x, float y,
                                 float maxWidth, int color, float scale, int maximumLines) {
        List<String> lines = new ArrayList<>();
        String current = "";
        for (String word : value.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.width(candidate) * scale > maxWidth) {
                lines.add(current);
                current = word;
                if (lines.size() >= maximumLines) break;
            } else {
                current = candidate;
            }
        }
        if (lines.size() < maximumLines && !current.isEmpty()) lines.add(current);
        for (int index = 0; index < Math.min(maximumLines, lines.size()); index++) {
            fitText(graphics, lines.get(index), x, y + index * 7.0F, maxWidth, color, scale, true);
        }
    }

    private void drawText(GuiGraphicsExtractor graphics, Component value,
                          float x, float y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, value, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier texture, Rect rect) {
        graphics.blit(texture, rect.x, rect.y, rect.right(), rect.bottom(), 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/gui/" + name);
    }

    private record Motion(float pivotX, float pivotY, float offsetX, float offsetY, float scale) {
        double inverseX(double screenX) { return pivotX + (screenX - pivotX - offsetX) / scale; }
        double inverseY(double screenY) { return pivotY + (screenY - pivotY - offsetY) / scale; }
        float screenX(float logicalX) { return pivotX + offsetX + (logicalX - pivotX) * scale; }
        float screenY(float logicalY) { return pivotY + offsetY + (logicalY - pivotY) * scale; }
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        float centerX() { return x + w * 0.5F; }
        float centerY() { return y + h * 0.5F; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
