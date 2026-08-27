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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DinoWhistleScreen extends Screen {
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 110;
    private static final int PICKER_WIDTH = 178;
    private static final int PICKER_HEIGHT = 112;
    private static final float MAX_PANEL_SCALE = 1.0F;
    private static final int INK = 0xFF494341;
    private static final int MUTED = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
    private static final int LINE = 0xFF8A6750;
    private static final int[] MODE_COLORS = {0xFFC54B2D, 0xFF547B3F, 0xFFD09A16, 0xFF477895};
    private static final Identifier HOTBAR = texture("hotbar.png");
    private static final Identifier RANGE_BUTTON = texture("whistle_range_button.png");
    private static DinoWhistleScreen active;

    private final int inventorySlot;
    private final long[] hoverStarted = new long[8];
    private final List<PassiveWhistleFollowersPayload.Entry> followers = new ArrayList<>();
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
        searchBox = new EditBox(font, 0, 0, 80, 12, Component.literal("Search inventory"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(40);
        searchBox.setTextColor(INK);
        searchBox.setTextShadow(true);
        searchBox.setHint(Component.literal("Search inventory"));
        searchBox.setVisible(false);
        addRenderableWidget(searchBox);
        requestFollowers();
        PrimevalUiSounds.open(this);
    }

    @Override
    public void tick() {
        super.tick();
        float target = searchOpen ? 1.0F : 0.0F;
        float acceleration = (target - searchReveal) * 64.0F - searchVelocity * 15.0F;
        searchVelocity += acceleration * 0.05F;
        searchReveal = Mth.clamp(searchReveal + searchVelocity * 0.05F, 0.0F, 1.0F);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        hoverTooltip = null;
        updateParallax(mouseX, mouseY);
        Motion motion = motion();
        float logicalMouseX = (float)motion.inverseX(mouseX);
        float logicalMouseY = (float)motion.inverseY(mouseY);
        float mainSlide = -190.0F * searchReveal;
        float pickerSlide = 190.0F * (1.0F - searchReveal);
        updateSearchBox(motion, pickerSlide);

        graphics.fill(0, 0, width, height, 0x9608050D);
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        if (searchReveal < 0.995F) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(mainSlide, 0.0F);
            drawPanel(graphics, mainPanel(), logicalMouseX - mainSlide, logicalMouseY);
            graphics.pose().popMatrix();
        }
        if (searchReveal > 0.005F) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(pickerSlide, 0.0F);
            drawSearchPicker(graphics, pickerPanel(), logicalMouseX - pickerSlide, logicalMouseY);
            graphics.pose().popMatrix();
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
        bold(graphics, "DINO WHISTLE", header.x + 7, header.y + 4, LABEL, 0.74F);
        rightText(graphics, settings.mode().isPassive() ? "AUTO DUTY" : "MARKED DUTY",
                header.right() - 7, header.y + 4, 67, MUTED, 0.58F);

        Rect order = orderRect(panel);
        drawCycleRow(graphics, order, 0, order.contains(mouseX, mouseY), modeColor(),
                "ORDER", settings.mode().title().toUpperCase(Locale.ROOT));
        if (order.contains(mouseX, mouseY)) {
            hoverTooltip = tooltip(settings.mode().title(), modeColor(),
                    settings.mode().description(), "Click to choose the next field order.");
        }

        Rect behavior = behaviorRect(panel);
        boolean cycles = settings.mode() == DinoWhistleSettings.FieldMode.QUARRY;
        boolean behaviorHovered = behavior.contains(mouseX, mouseY);
        drawBubble(graphics, behavior);
        if (behaviorHovered) {
            graphics.fill(behavior.x + 2, behavior.y + 2, behavior.right() - 2, behavior.bottom() - 2,
                    0x18FFFFFF);
            if (cycles) graphics.requestCursor(CursorTypes.POINTING_HAND);
            hoverTooltip = tooltip(settings.mode().targetTitle(settings.pattern()), modeColor(),
                    settings.mode().targetDescription(settings.pattern()),
                    cycles ? "Click to switch between a vein and a marked area." : settings.mode().markHint(settings.pattern()));
        }
        drawSeparator(graphics, behavior, 54);
        drawMovingText(graphics, behavior, 1, behaviorHovered, () -> {
            bold(graphics, settings.mode().isPassive() ? "BEHAVIOR" : "TARGET",
                    behavior.x + 7, behavior.y + 5, behaviorHovered ? modeColor() : MUTED, 0.63F);
            rightText(graphics, settings.mode().targetTitle(settings.pattern()).toUpperCase(Locale.ROOT)
                            + (cycles ? "  >" : ""), behavior.right() - 7, behavior.y + 5,
                    behavior.w - 65, behaviorHovered ? modeColor() : INK, 0.65F);
        });

        drawRange(graphics, rangeRect(panel), mouseX, mouseY);
        drawFollowerRow(graphics, followerRect(panel), mouseX, mouseY);
    }

    private void drawFollowerRow(GuiGraphicsExtractor graphics, Rect row, float mouseX, float mouseY) {
        drawBubble(graphics, row);
        if (!settings.mode().isPassive()) {
            bold(graphics, "ASSIGN", row.x + 7, row.y + 8, MUTED, 0.62F);
            drawSeparator(graphics, row, 49);
            fitText(graphics, settings.mode().markHint(settings.pattern()).toUpperCase(Locale.ROOT),
                    row.x + 56, row.y + 8, row.w - 63, modeColor(), 0.55F, true);
            if (row.contains(mouseX, mouseY)) {
                hoverTooltip = tooltip("Assign in the world", modeColor(),
                        settings.mode().targetDescription(settings.pattern()), settings.mode().markHint(settings.pattern()));
            }
            return;
        }

        bold(graphics, "FOLLOWER", row.x + 7, row.y + 8, MUTED, 0.58F);
        drawSeparator(graphics, row, 47);
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
            fitText(graphics, "NO COMPATIBLE FOLLOWER", row.x + 54, row.y + 8,
                    settings.mode() == DinoWhistleSettings.FieldMode.COLLECT ? 61 : 100, MUTED, 0.53F, true);
        }
        if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT) {
            Rect filter = filterSlot(row);
            boolean hovered = filter.contains(mouseX, mouseY);
            drawHotbar(graphics, filter, hovered);
            ItemStack selected = filterStack();
            graphics.item(selected.isEmpty() ? Items.HOPPER.getDefaultInstance() : selected,
                    filter.x + 1, filter.y + 1);
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
        updateHover(index + 2, hovered);
        float scale = 1.0F + interactionMotion(index + 2, hovered) * 0.07F;
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
            DinosaurPreviewUi.draw(graphics, dinosaur, slot.x + 2, slot.y + 2,
                    slot.w - 4, slot.h - 4, 42.0F, -25.0F);
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
                range.x + 7, range.y + 4, hovered || draggingRange ? modeColor() : MUTED, 0.58F);
        rightText(graphics, settings.range() + "M", range.right() - 7, range.y + 4,
                28, hovered || draggingRange ? modeColor() : INK, 0.58F);
        int trackLeft = range.x + 7;
        int trackRight = range.right() - 7;
        int trackY = range.y + 14;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, LINE);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = trackLeft + Math.round(ratio * (trackRight - trackLeft));
        blit(graphics, RANGE_BUTTON, new Rect(knobX - 5, trackY - 5, 10, 10));
    }

    private void drawCycleRow(GuiGraphicsExtractor graphics, Rect row, int key, boolean hovered,
                              int accent, String label, String value) {
        drawBubble(graphics, row);
        if (hovered) {
            graphics.fill(row.x + 2, row.y + 2, row.right() - 2, row.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        drawSeparator(graphics, row, 45);
        drawMovingText(graphics, row, key, hovered, () -> {
            bold(graphics, label, row.x + 7, row.y + 5, hovered ? accent : MUTED, 0.63F);
            rightText(graphics, value + "  >", row.right() - 7, row.y + 5,
                    row.w - 59, hovered ? accent : INK, 0.66F);
        });
    }

    private void drawSearchPicker(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        Rect header = new Rect(panel.x, panel.y, panel.w, 16);
        Rect body = new Rect(panel.x, panel.y + 19, panel.w, panel.h - 19);
        drawBubble(graphics, header);
        drawBubble(graphics, body);
        bold(graphics, "COLLECT FILTER", header.x + 7, header.y + 4, modeColor(), 0.68F);
        Rect back = new Rect(header.right() - 39, header.y, 39, header.h);
        boolean backHovered = back.contains(mouseX, mouseY);
        rightText(graphics, "BACK", header.right() - 7, header.y + 4, 30,
                backHovered ? modeColor() : MUTED, 0.60F);
        if (backHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            hoverTooltip = tooltip("Back", modeColor(), "Return to whistle settings.", "Your filter is saved automatically.");
        }

        Rect target = searchTarget(body);
        boolean targetHovered = target.contains(mouseX, mouseY);
        drawHotbar(graphics, target, targetHovered);
        ItemStack selected = filterStack();
        graphics.item(selected.isEmpty() ? Items.HOPPER.getDefaultInstance() : selected,
                target.x + 1, target.y + 1);
        if (targetHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            hoverTooltip = tooltip(selected.isEmpty() ? "Any item" : selected.getHoverName().getString(),
                    modeColor(), selected.isEmpty() ? "No filter is active." : "The current collection filter.",
                    selected.isEmpty() ? "Drag an inventory item here." : "Click to clear it.");
        }
        PrimevalBubbleUi.draw(graphics, searchRect(body).x, searchRect(body).y,
                searchRect(body).w, searchRect(body).h);

        List<ItemStack> items = filteredInventory();
        for (int index = 0; index < 36; index++) {
            Rect slot = searchItemSlot(body, index);
            ItemStack stack = index < items.size() ? items.get(index) : ItemStack.EMPTY;
            boolean hovered = slot.contains(mouseX, mouseY);
            drawHotbar(graphics, slot, hovered && !stack.isEmpty());
            if (!stack.isEmpty()) {
                graphics.item(stack, slot.x + 1, slot.y + 1);
                graphics.itemDecorations(font, stack, slot.x + 1, slot.y + 1);
            }
            if (hovered && !stack.isEmpty()) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                hoverTooltip = List.of(
                        stack.getHoverName().copy().withStyle(Style.EMPTY.withBold(true)),
                        Component.literal("Drag into the filter slot.").withStyle(style -> style.withColor(0x6E6764))
                );
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Motion motion = motion();
        double mouseX = motion.inverseX(event.x());
        double mouseY = motion.inverseY(event.y());
        if (searchReveal > 0.5F) {
            double localX = mouseX - 190.0F * (1.0F - searchReveal);
            Rect panel = pickerPanel();
            Rect header = new Rect(panel.x, panel.y, panel.w, 16);
            Rect body = new Rect(panel.x, panel.y + 19, panel.w, panel.h - 19);
            if (new Rect(header.right() - 39, header.y, 39, header.h).contains(localX, mouseY)) {
                closeSearch();
                return true;
            }
            List<ItemStack> items = filteredInventory();
            for (int index = 0; index < 36; index++) {
                if (!searchItemSlot(body, index).contains(localX, mouseY) || index >= items.size()) continue;
                ItemStack stack = items.get(index);
                if (!stack.isEmpty()) {
                    draggedItem = stack.copyWithCount(1);
                    pressed(6);
                    return true;
                }
            }
            if (searchTarget(body).contains(localX, mouseY) && !settings.itemFilter().isBlank()) {
                settings = copy(settings.mode(), settings.pattern(), settings.range(), "");
                changed(0.88F, 6);
                return true;
            }
            return true;
        }

        double localX = mouseX + 190.0F * searchReveal;
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
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRange && event.button() == 0) {
            updateRange(motion().inverseX(event.x()) + 190.0F * searchReveal);
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
            double mouseX = motion.inverseX(event.x()) - 190.0F * (1.0F - searchReveal);
            double mouseY = motion.inverseY(event.y());
            Rect panel = pickerPanel();
            Rect body = new Rect(panel.x, panel.y + 19, panel.w, panel.h - 19);
            if (searchTarget(body).contains(mouseX, mouseY)) {
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
        if (searchBox != null) searchBox.setFocused(true);
        PrimevalUiSounds.click(1.02F);
    }

    private void closeSearch() {
        searchOpen = false;
        draggedItem = ItemStack.EMPTY;
        if (searchBox != null) searchBox.setFocused(false);
    }

    private void updateRange(double mouseX) {
        Rect range = rangeRect(mainPanel());
        int left = range.x + 7;
        int right = range.right() - 7;
        float ratio = Mth.clamp((float)((mouseX - left) / (right - left)), 0.0F, 1.0F);
        int value = Math.round(Mth.lerp(ratio, DinoWhistleSettings.MIN_RANGE, DinoWhistleSettings.MAX_RANGE));
        if (value != settings.range()) {
            settings = copy(settings.mode(), settings.pattern(), value, settings.itemFilter());
            send();
        }
    }

    private void updateSearchBox(Motion motion, float pickerSlide) {
        if (searchBox == null) return;
        Rect panel = pickerPanel();
        Rect body = new Rect(panel.x, panel.y + 19, panel.w, panel.h - 19);
        Rect logical = searchRect(body);
        float left = motion.screenX(logical.x + 4 + pickerSlide);
        float top = motion.screenY(logical.y + 2);
        float right = motion.screenX(logical.right() - 4 + pickerSlide);
        float bottom = motion.screenY(logical.bottom() - 2);
        searchBox.setX(Math.round(left));
        searchBox.setY(Math.round(top));
        searchBox.setWidth(Math.max(20, Math.round(right - left)));
        searchBox.setHeight(Math.max(9, Math.round(bottom - top)));
        searchBox.setAlpha(Mth.clamp(searchReveal, 0.0F, 1.0F));
        searchBox.setVisible(searchOpen && searchReveal > 0.82F);
    }

    private List<ItemStack> filteredInventory() {
        if (minecraft == null || minecraft.player == null) return List.of();
        Inventory inventory = minecraft.player.getInventory();
        List<ItemStack> ordered = new ArrayList<>(36);
        for (int slot = 9; slot < 36; slot++) ordered.add(inventory.getItem(slot));
        for (int slot = 0; slot < 9; slot++) ordered.add(inventory.getItem(slot));
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return ordered;
        return ordered.stream().filter(stack -> !stack.isEmpty()).filter(stack -> {
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            return name.contains(query) || id.contains(query.replace(' ', '_'));
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

    private Rect mainPanel() { return new Rect((width - PANEL_WIDTH) / 2, (height - PANEL_HEIGHT) / 2, PANEL_WIDTH, PANEL_HEIGHT); }
    private Rect pickerPanel() { return new Rect((width - PICKER_WIDTH) / 2, (height - PICKER_HEIGHT) / 2, PICKER_WIDTH, PICKER_HEIGHT); }
    private Rect headerRect(Rect panel) { return new Rect(panel.x, panel.y, panel.w, 16); }
    private Rect orderRect(Rect panel) { return new Rect(panel.x, panel.y + 19, panel.w, 18); }
    private Rect behaviorRect(Rect panel) { return new Rect(panel.x, panel.y + 40, panel.w, 18); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x, panel.y + 61, panel.w, 20); }
    private Rect followerRect(Rect panel) { return new Rect(panel.x, panel.y + 84, panel.w, 26); }
    private Rect followerSlot(Rect row, int index) { return new Rect(row.x + 53 + index * 22, row.y + 4, 18, 18); }
    private Rect filterSlot(Rect row) { return new Rect(row.right() - 23, row.y + 4, 18, 18); }
    private Rect searchTarget(Rect body) { return new Rect(body.x + 7, body.y + 5, 18, 18); }
    private Rect searchRect(Rect body) { return new Rect(body.x + 29, body.y + 5, body.w - 36, 18); }
    private Rect searchItemSlot(Rect body, int index) {
        return new Rect(body.x + 8 + index % 9 * 18, body.y + 26 + index / 9 * 18, 18, 18);
    }

    private int modeColor() { return MODE_COLORS[settings.mode().ordinal()]; }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect) {
        graphics.fill(rect.x + 3, rect.y + 4, rect.right() + 3, rect.bottom() + 4, 0x52000000);
        PrimevalBubbleUi.draw(graphics, rect.x, rect.y, rect.w, rect.h);
    }

    private void drawHotbar(GuiGraphicsExtractor graphics, Rect rect, boolean hovered) {
        blit(graphics, HOTBAR, rect);
        if (hovered) graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x24FFFFFF);
    }

    private void drawSeparator(GuiGraphicsExtractor graphics, Rect rect, int offset) {
        int x = rect.x + offset;
        graphics.fill(x, rect.y + 4, x + 1, rect.bottom() - 4, 0x8F6D503D);
        graphics.fill(x + 1, rect.y + 4, x + 2, rect.bottom() - 4, 0x45E6C4A3);
    }

    private List<Component> tooltip(String title, int color, String detail, String action) {
        return List.of(
                Component.literal(title).withStyle(style -> style.withBold(true).withColor(color & 0xFFFFFF)),
                Component.literal("────────────").withStyle(style -> style.withColor(0x5F5652)),
                Component.literal(detail).withStyle(style -> style.withColor(0xB6AFAB)),
                Component.literal(action).withStyle(style -> style.withColor(0xE6C36F))
        );
    }

    private void updateParallax(int mouseX, int mouseY) {
        float delta = Mth.clamp((renderNow - previousFrame) / 1_000_000_000.0F, 0.0F, 0.05F);
        previousFrame = renderNow;
        float targetX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * -0.7F;
        float targetY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * -0.45F;
        float blend = 1.0F - (float)Math.exp(-delta * 10.0F);
        parallaxX = Mth.lerp(blend, parallaxX, targetX);
        parallaxY = Mth.lerp(blend, parallaxY, targetY);
    }

    private Motion motion() {
        long now = renderNow == 0L ? Util.getNanos() : renderNow;
        float elapsedTicks = (now - openedAt) / 50_000_000.0F;
        float progress = Mth.clamp(elapsedTicks / 20.0F, 0.0F, 1.0F);
        float settled = PrimevalBubbleUi.spring(progress, 6.2F, 11.4F);
        float fitted = Math.min(MAX_PANEL_SCALE,
                Math.min((width - 12.0F) / PICKER_WIDTH, (height - 12.0F) / PICKER_HEIGHT));
        float scale = Math.max(0.62F, fitted) * Math.max(0.1F, 0.76F + 0.24F * settled);
        return new Motion(width * 0.5F, height * 0.5F, parallaxX,
                12.0F * (1.0F - settled) + parallaxY, scale);
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
