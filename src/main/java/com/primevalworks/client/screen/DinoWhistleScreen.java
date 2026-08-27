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
    private static final int PANEL_WIDTH = 195;
    private static final int PANEL_HEIGHT = 148;
    private static final float MAX_PANEL_SCALE = 1.46F;
    private static final float PANEL_SIZE_MULTIPLIER = 0.88F;
    private static final int INK = 0xFF494341;
    private static final int MUTED = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
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
        this.settings = DinoWhistleSettings.read(whistle);
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
        addRenderableWidget(searchBox);
        requestFollowers();
        PrimevalUiSounds.open(this);
    }

    @Override
    public void tick() {
        super.tick();
        float target = searchOpen ? 1.0F : 0.0F;
        float acceleration = (target - searchReveal) * 58.0F - searchVelocity * 14.0F;
        searchVelocity += acceleration * 0.05F;
        searchReveal = Mth.clamp(searchReveal + searchVelocity * 0.05F, 0.0F, 1.0F);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        updateParallax(mouseX, mouseY);
        Rect panel = panel();
        Motion motion = motion(panel);
        float uiMouseX = (float) motion.inverseX(mouseX);
        float uiMouseY = (float) motion.inverseY(mouseY);
        updateSearchBox(panel, motion);

        graphics.fill(0, 0, width, height, 0x72000000);
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        graphics.pose().pushMatrix();
        graphics.pose().translate(-54.0F * searchReveal, 0.0F);
        drawPanel(graphics, panel, uiMouseX + 54.0F * searchReveal, uiMouseY);
        graphics.pose().popMatrix();
        if (searchReveal > 0.01F) drawSearchPicker(graphics, panel, uiMouseX, uiMouseY);
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!draggedItem.isEmpty()) {
            graphics.nextStratum();
            graphics.item(draggedItem, mouseX - 8, mouseY - 8);
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        Rect header = headerRect(panel);
        drawBubble(graphics, header);
        bold(graphics, "DINO WHISTLE", header.x + 7, header.y + 5, LABEL, 0.80F);
        rightText(graphics, settings.mode().isPassive() ? "AUTOMATIC FIELD DUTY" : "MARKED FIELD DUTY",
                header.right() - 7, header.y + 5, 105, MUTED, 0.58F);

        Rect order = orderRect(panel);
        drawCycleRow(graphics, order, 0, order.contains(mouseX, mouseY), modeColor(),
                "ORDER", settings.mode().title().toUpperCase(Locale.ROOT));

        Rect behavior = behaviorRect(panel);
        boolean targetCycles = settings.mode() == DinoWhistleSettings.FieldMode.QUARRY;
        boolean behaviorHovered = targetCycles && behavior.contains(mouseX, mouseY);
        drawBubble(graphics, behavior);
        if (behaviorHovered) {
            graphics.fill(behavior.x + 2, behavior.y + 2, behavior.right() - 2, behavior.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        drawMovingText(graphics, behavior, 1, behaviorHovered, () -> {
            bold(graphics, settings.mode().isPassive() ? "BEHAVIOR" : "TARGET",
                    behavior.x + 7, behavior.y + 6, behaviorHovered ? modeColor() : MUTED, 0.69F);
            String value = settings.mode().targetTitle(settings.pattern()).toUpperCase(Locale.ROOT)
                    + (targetCycles ? "  >" : "");
            rightText(graphics, value, behavior.right() - 7, behavior.y + 6,
                    behavior.w - 71, behaviorHovered ? modeColor() : INK, 0.70F);
        });

        drawRange(graphics, rangeRect(panel), mouseX, mouseY);
        drawDetails(graphics, detailsRect(panel), mouseX, mouseY);
    }

    private void drawDetails(GuiGraphicsExtractor graphics, Rect details, float mouseX, float mouseY) {
        drawBubble(graphics, details);
        if (!settings.mode().isPassive()) {
            bold(graphics, settings.mode().targetTitle(settings.pattern()).toUpperCase(Locale.ROOT),
                    details.x + 7, details.y + 7, modeColor(), 0.74F);
            fitText(graphics, settings.mode().targetDescription(settings.pattern()),
                    details.x + 7, details.y + 20, details.w - 14, INK, 0.65F, true);
            fitText(graphics, settings.mode().markHint(settings.pattern()).toUpperCase(Locale.ROOT),
                    details.x + 7, details.bottom() - 12, details.w - 14, MUTED, 0.57F, true);
            return;
        }

        bold(graphics, "FOLLOWER", details.x + 7, details.y + 6, modeColor(), 0.67F);
        PassiveWhistleFollowersPayload.Entry hoveredEntry = null;
        for (int index = 0; index < Math.min(3, followers.size()); index++) {
            Rect slot = followerSlot(details, index);
            PassiveWhistleFollowersPayload.Entry entry = followers.get(index);
            boolean hovered = slot.contains(mouseX, mouseY);
            if (hovered) hoveredEntry = entry;
            drawFollowerSlot(graphics, slot, entry, hovered, index);
        }
        if (followers.isEmpty()) {
            fitText(graphics, "NO COMPATIBLE FOLLOWER IS LOADED",
                    details.x + 7, details.y + 26, 103, MUTED, 0.55F, true);
        }

        int infoX = details.x + 108;
        int infoWidth = settings.mode() == DinoWhistleSettings.FieldMode.COLLECT ? 45 : 78;
        String status = hoveredEntry == null
                ? settings.mode().targetDescription(settings.pattern())
                : hoveredEntry.compatible()
                ? hoveredEntry.name() + "  /  " + hoveredEntry.rating() + " STAR"
                : hoveredEntry.name() + " CANNOT DO THIS WORK";
        wrappedText(graphics, status, infoX, details.y + 8, infoWidth, hoveredEntry == null ? INK : MUTED, 0.58F, 3);

        if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT) {
            Rect filter = filterSlot(details);
            boolean hovered = filter.contains(mouseX, mouseY);
            blit(graphics, HOTBAR, filter);
            if (hovered) {
                graphics.fill(filter.x + 3, filter.y + 3, filter.right() - 3, filter.bottom() - 3, 0x24FFFFFF);
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
            ItemStack selected = filterStack();
            graphics.item(selected.isEmpty() ? Items.HOPPER.getDefaultInstance() : selected,
                    filter.x + (filter.w - 16) / 2, filter.y + (filter.h - 16) / 2);
            fitText(graphics, selected.isEmpty() ? "ANY" : "FILTER",
                    filter.x - 2, filter.bottom() + 1, filter.w + 4, hovered ? modeColor() : MUTED, 0.50F, true);
        }
    }

    private void drawFollowerSlot(GuiGraphicsExtractor graphics, Rect slot,
                                  PassiveWhistleFollowersPayload.Entry entry,
                                  boolean hovered, int index) {
        updateHover(index + 2, hovered);
        float motion = interactionMotion(index + 2, hovered);
        float scale = 1.0F + motion * 0.07F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.centerX(), slot.centerY());
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-slot.centerX(), -slot.centerY());
        blit(graphics, HOTBAR, slot);
        graphics.pose().popMatrix();
        if (entry.assigned()) {
            graphics.fill(slot.x + 3, slot.y + 3, slot.right() - 3, slot.bottom() - 3, 0x284C8A58);
        }
        FieldDodoEntity dinosaur = entity(entry.entityId());
        if (dinosaur != null) {
            DinosaurPreviewUi.draw(graphics, dinosaur, slot.x + 3, slot.y + 3,
                    slot.w - 6, slot.h - 6, 42.0F, -25.0F);
        }
        if (!entry.compatible()) {
            graphics.fill(slot.x + 3, slot.y + 3, slot.right() - 3, slot.bottom() - 3, 0x84211C20);
        } else if (hovered) {
            graphics.fill(slot.x + 3, slot.y + 3, slot.right() - 3, slot.bottom() - 3, 0x20FFF0CB);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void drawRange(GuiGraphicsExtractor graphics, Rect range, float mouseX, float mouseY) {
        boolean hovered = range.contains(mouseX, mouseY);
        drawBubble(graphics, range);
        if (hovered || draggingRange) {
            graphics.fill(range.x + 2, range.y + 2, range.right() - 2, range.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        bold(graphics, settings.mode() == DinoWhistleSettings.FieldMode.COLLECT ? "SEARCH RANGE" : "LEASH",
                range.x + 7, range.y + 4, hovered || draggingRange ? modeColor() : MUTED, 0.62F);
        rightText(graphics, settings.range() + "M", range.right() - 7, range.y + 4,
                28, hovered || draggingRange ? modeColor() : INK, 0.62F);
        int trackLeft = range.x + 7;
        int trackRight = range.right() - 7;
        int trackY = range.y + 15;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, 0xFF88664F);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = trackLeft + Math.round(ratio * (trackRight - trackLeft));
        Rect knob = new Rect(knobX - 5, trackY - 5, 10, 10);
        blit(graphics, RANGE_BUTTON, knob);
    }

    private void drawCycleRow(GuiGraphicsExtractor graphics, Rect row, int key, boolean hovered,
                              int accent, String label, String value) {
        drawBubble(graphics, row);
        if (hovered) {
            graphics.fill(row.x + 2, row.y + 2, row.right() - 2, row.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        drawMovingText(graphics, row, key, hovered, () -> {
            bold(graphics, label, row.x + 7, row.y + 6, hovered ? accent : MUTED, 0.70F);
            rightText(graphics, value + "  >", row.right() - 7, row.y + 6,
                    row.w - 57, hovered ? accent : INK, 0.72F);
        });
    }

    private void drawSearchPicker(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        float settled = PrimevalBubbleUi.spring(Mth.clamp(searchReveal, 0.0F, 1.0F), 6.4F, 10.8F);
        float slide = (1.0F - settled) * 64.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slide, 0.0F);
        Rect title = new Rect(panel.x, panel.y + 21, panel.w, 22);
        Rect body = new Rect(panel.x, panel.y + 46, panel.w, 102);
        drawBubble(graphics, title);
        drawBubble(graphics, body);
        bold(graphics, "ITEM FILTER", title.x + 7, title.y + 6, modeColor(), 0.72F);
        rightText(graphics, "DRAG TO THE SLOT", title.right() - 7, title.y + 6, 98, MUTED, 0.58F);

        Rect target = searchTarget(body);
        blit(graphics, HOTBAR, target);
        ItemStack selected = filterStack();
        graphics.item(selected.isEmpty() ? Items.HOPPER.getDefaultInstance() : selected,
                target.x + 2, target.y + 2);
        boolean targetHovered = target.contains(mouseX - slide, mouseY);
        if (targetHovered) {
            graphics.fill(target.x + 2, target.y + 2, target.right() - 2, target.bottom() - 2, 0x28FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        Rect search = searchRect(body);
        PrimevalBubbleUi.draw(graphics, search.x, search.y, search.w, search.h);

        List<ItemStack> items = filteredInventory();
        for (int index = 0; index < 36; index++) {
            Rect slot = searchItemSlot(body, index);
            blit(graphics, HOTBAR, slot);
            boolean hovered = slot.contains(mouseX - slide, mouseY);
            ItemStack stack = index < items.size() ? items.get(index) : ItemStack.EMPTY;
            if (!stack.isEmpty()) graphics.item(stack, slot.x + 1, slot.y + 1);
            if (hovered) {
                graphics.fill(slot.x + 2, slot.y + 2, slot.right() - 2, slot.bottom() - 2, 0x24FFFFFF);
                if (!stack.isEmpty()) {
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                    graphics.setComponentTooltipForNextFrame(font, List.of(
                            stack.getHoverName().copy().withStyle(Style.EMPTY.withBold(true)),
                            Component.literal("Drag this into the filter slot.").withStyle(style -> style.withColor(0x6E6764))
                    ), Math.round(mouseX), Math.round(mouseY));
                }
            }
        }
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Rect panel = panel();
        Motion motion = motion(panel);
        double mouseX = motion.inverseX(event.x());
        double mouseY = motion.inverseY(event.y());
        if (searchReveal > 0.08F) {
            float settled = PrimevalBubbleUi.spring(Mth.clamp(searchReveal, 0.0F, 1.0F), 6.4F, 10.8F);
            double localX = mouseX - (1.0F - settled) * 64.0F;
            Rect body = new Rect(panel.x, panel.y + 46, panel.w, 102);
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
            if (titleRect(panel).contains(localX, mouseY)) {
                closeSearch();
                return true;
            }
        }
        double shiftedX = mouseX + 54.0F * searchReveal;
        if (orderRect(panel).contains(shiftedX, mouseY)) {
            DinoWhistleSettings.FieldMode[] values = DinoWhistleSettings.FieldMode.values();
            DinoWhistleSettings.FieldMode mode = values[(settings.mode().ordinal() + 1) % values.length];
            settings = copy(mode, mode.normalizePattern(settings.pattern()), settings.range(), settings.itemFilter());
            changed(0.96F, 0);
            requestFollowers();
            return true;
        }
        if (settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                && behaviorRect(panel).contains(shiftedX, mouseY)) {
            DinoWhistleSettings.Pattern pattern = settings.pattern() == DinoWhistleSettings.Pattern.AREA
                    ? DinoWhistleSettings.Pattern.CONNECTED : DinoWhistleSettings.Pattern.AREA;
            settings = copy(settings.mode(), pattern, settings.range(), settings.itemFilter());
            changed(1.02F, 1);
            return true;
        }
        if (rangeRect(panel).contains(shiftedX, mouseY)) {
            draggingRange = true;
            pressed(2);
            updateRange(shiftedX);
            return true;
        }
        if (settings.mode().isPassive()) {
            Rect details = detailsRect(panel);
            for (int index = 0; index < Math.min(3, followers.size()); index++) {
                if (!followerSlot(details, index).contains(shiftedX, mouseY)) continue;
                PassiveWhistleFollowersPayload.Entry entry = followers.get(index);
                if (entry.compatible()) {
                    ClientPacketDistributor.sendToServer(
                            new AssignPassiveWhistleWorkPayload(inventorySlot, entry.uuid()));
                    PrimevalUiSounds.click(1.08F);
                } else {
                    PrimevalUiSounds.click(0.72F);
                }
                return true;
            }
            if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT
                    && filterSlot(details).contains(shiftedX, mouseY)) {
                openSearch();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRange && event.button() == 0) {
            updateRange(motion(panel()).inverseX(event.x()) + 54.0F * searchReveal);
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
            Rect panel = panel();
            Motion motion = motion(panel);
            double mouseX = motion.inverseX(event.x());
            double mouseY = motion.inverseY(event.y());
            float settled = PrimevalBubbleUi.spring(Mth.clamp(searchReveal, 0.0F, 1.0F), 6.4F, 10.8F);
            double localX = mouseX - (1.0F - settled) * 64.0F;
            Rect body = new Rect(panel.x, panel.y + 46, panel.w, 102);
            if (searchTarget(body).contains(localX, mouseY)) {
                String id = BuiltInRegistries.ITEM.getKey(draggedItem.getItem()).toString();
                settings = copy(settings.mode(), settings.pattern(), settings.range(), id);
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
        Rect range = rangeRect(panel());
        int trackLeft = range.x + 7;
        int trackRight = range.right() - 7;
        float ratio = Mth.clamp((float)((mouseX - trackLeft) / (trackRight - trackLeft)), 0.0F, 1.0F);
        int value = Math.round(Mth.lerp(ratio, DinoWhistleSettings.MIN_RANGE, DinoWhistleSettings.MAX_RANGE));
        if (value != settings.range()) {
            settings = copy(settings.mode(), settings.pattern(), value, settings.itemFilter());
            send();
        }
    }

    private void updateSearchBox(Rect panel, Motion motion) {
        if (searchBox == null) return;
        Rect body = new Rect(panel.x, panel.y + 46, panel.w, 102);
        Rect logical = searchRect(body);
        float settled = PrimevalBubbleUi.spring(Mth.clamp(searchReveal, 0.0F, 1.0F), 6.4F, 10.8F);
        float slide = (1.0F - settled) * 64.0F;
        float left = motion.screenX(logical.x + 5 + slide);
        float top = motion.screenY(logical.y + 2);
        float right = motion.screenX(logical.right() - 4 + slide);
        float bottom = motion.screenY(logical.bottom() - 2);
        searchBox.setX(Math.round(left));
        searchBox.setY(Math.round(top));
        searchBox.setWidth(Math.max(20, Math.round(right - left)));
        searchBox.setHeight(Math.max(9, Math.round(bottom - top)));
        searchBox.setAlpha(Mth.clamp(searchReveal, 0.0F, 1.0F));
        searchBox.setVisible(searchOpen && searchReveal > 0.48F);
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

    private Rect panel() { return new Rect((width - PANEL_WIDTH) / 2, (height - PANEL_HEIGHT) / 2, PANEL_WIDTH, PANEL_HEIGHT); }
    private Rect headerRect(Rect panel) { return new Rect(panel.x, panel.y, panel.w, 18); }
    private Rect titleRect(Rect panel) { return new Rect(panel.x, panel.y + 21, panel.w, 22); }
    private Rect orderRect(Rect panel) { return new Rect(panel.x, panel.y + 21, panel.w, 20); }
    private Rect behaviorRect(Rect panel) { return new Rect(panel.x, panel.y + 44, panel.w, 20); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x, panel.y + 67, panel.w, 20); }
    private Rect detailsRect(Rect panel) { return new Rect(panel.x, panel.y + 90, panel.w, 58); }
    private Rect followerSlot(Rect details, int index) { return new Rect(details.x + 7 + index * 32, details.y + 19, 28, 28); }
    private Rect filterSlot(Rect details) { return new Rect(details.right() - 34, details.y + 17, 28, 28); }
    private Rect searchTarget(Rect body) { return new Rect(body.x + 6, body.y + 6, 18, 18); }
    private Rect searchRect(Rect body) { return new Rect(body.x + 28, body.y + 6, body.w - 34, 18); }
    private Rect searchItemSlot(Rect body, int index) {
        return new Rect(body.x + 7 + index % 9 * 20, body.y + 27 + index / 9 * 18, 18, 18);
    }

    private int modeColor() { return MODE_COLORS[settings.mode().ordinal()]; }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect) {
        graphics.fill(rect.x + 3, rect.y + 4, rect.right() + 3, rect.bottom() + 4, 0x3B000000);
        PrimevalBubbleUi.draw(graphics, rect.x, rect.y, rect.w, rect.h);
    }

    private void updateParallax(int mouseX, int mouseY) {
        float delta = Mth.clamp((renderNow - previousFrame) / 1_000_000_000.0F, 0.0F, 0.05F);
        previousFrame = renderNow;
        float targetX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * -1.5F;
        float targetY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * -0.9F;
        float blend = 1.0F - (float)Math.exp(-delta * 9.0F);
        parallaxX = Mth.lerp(blend, parallaxX, targetX);
        parallaxY = Mth.lerp(blend, parallaxY, targetY);
    }

    private Motion motion(Rect panel) {
        long now = renderNow == 0L ? Util.getNanos() : renderNow;
        float elapsedTicks = (now - openedAt) / 50_000_000.0F;
        float progress = Mth.clamp(elapsedTicks / 24.0F, 0.0F, 1.0F);
        float settled = PrimevalBubbleUi.spring(progress, 6.2F, 11.4F);
        float fade = smoothStep(Mth.clamp(elapsedTicks / 18.0F, 0.0F, 1.0F));
        float horizontal = (width - 14.0F) / PANEL_WIDTH;
        float vertical = (height - 10.0F) / PANEL_HEIGHT;
        float fitted = Math.min(MAX_PANEL_SCALE, Math.min(horizontal, vertical)) * PANEL_SIZE_MULTIPLIER;
        float baseScale = Mth.clamp(fitted, 0.68F, MAX_PANEL_SCALE * PANEL_SIZE_MULTIPLIER);
        float scale = baseScale * Math.max(0.1F, 0.74F + 0.26F * settled);
        return new Motion(panel.centerX(), panel.centerY(), parallaxX * fade,
                14.0F * (1.0F - settled) + parallaxY * fade, scale);
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
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        drawText(graphics, component, x, y, color, scale);
    }

    private void rightText(GuiGraphicsExtractor graphics, String value, float right, float y,
                           float maxWidth, int color, float requestedScale) {
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(requestedScale, maxWidth / textWidth);
        drawText(graphics, component, right - textWidth * scale, y, color, scale);
    }

    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale, boolean bold) {
        Component component = bold ? Component.literal(value).withStyle(Style.EMPTY.withBold(true)) : Component.literal(value);
        int textWidth = Math.max(1, font.width(component));
        drawText(graphics, component, x, y, color, Math.min(requestedScale, maxWidth / textWidth));
    }

    private void wrappedText(GuiGraphicsExtractor graphics, String value, int x, int y,
                             int maxWidth, int color, float scale, int maxLines) {
        int logicalWidth = Math.max(8, Math.round(maxWidth / scale));
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(
                Component.literal(value).withStyle(Style.EMPTY.withBold(true)), logicalWidth);
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + index * 8.0F);
            graphics.pose().scale(scale, scale);
            graphics.text(font, lines.get(index), 0, 0, color, true);
            graphics.pose().popMatrix();
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

    private static float smoothStep(float value) { return value * value * (3.0F - 2.0F * value); }
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
        boolean contains(double x, double y) { return x >= this.x && x < right() && y >= this.y && y < bottom(); }
    }
}
