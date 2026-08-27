package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.network.payload.ConfigureDinoWhistlePayload;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
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
    private static final int MUTED_INK = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
    private static final int[] MODE_COLORS = {0xFFC54B2D, 0xFF547B3F, 0xFFD09A16, 0xFF477895};
    private static final Identifier HOTBAR = texture("hotbar.png");
    private static final Identifier RANGE_BUTTON = texture("whistle_range_button.png");

    private final long[] hoverStarted = new long[5];
    private final List<ItemStack> itemCatalogue = new ArrayList<>();
    private DinoWhistleSettings settings;
    private EditBox itemSearch;
    private long openedAt;
    private long renderNow;
    private long previousFrame;
    private long pressedAt;
    private int pressedKey = -1;
    private float parallaxX;
    private float parallaxY;
    private boolean draggingRange;

    private DinoWhistleScreen(DinoWhistleSettings settings) {
        super(Component.literal("Dino Whistle"));
        this.settings = settings;
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack whistle = DinoWhistleItem.findHeld(minecraft.player);
        if (!whistle.isEmpty()) minecraft.setScreen(new DinoWhistleScreen(DinoWhistleSettings.read(whistle)));
    }

    @Override
    protected void init() {
        openedAt = Util.getNanos();
        previousFrame = openedAt;
        itemCatalogue.clear();
        BuiltInRegistries.ITEM.forEach(item -> {
            ItemStack stack = item.getDefaultInstance();
            if (!stack.isEmpty() && !stack.is(Items.AIR)) itemCatalogue.add(stack);
        });
        itemSearch = new EditBox(font, 0, 0, 80, 12, Component.literal("Search items"));
        itemSearch.setBordered(false);
        itemSearch.setMaxLength(40);
        itemSearch.setTextColor(INK);
        itemSearch.setTextShadow(true);
        itemSearch.setHint(Component.literal("Search items"));
        addRenderableWidget(itemSearch);
        PrimevalUiSounds.open(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        updateParallax(mouseX, mouseY);
        Rect panel = panel();
        Motion motion = motion(panel);
        float uiMouseX = motion.inverseX(mouseX);
        float uiMouseY = motion.inverseY(mouseY);
        updateSearchBox(panel, motion);

        graphics.fill(0, 0, width, height, 0x72000000);
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        drawPanel(graphics, panel, uiMouseX, uiMouseY);
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        Rect header = headerRect(panel);
        drawBubble(graphics, header);
        bold(graphics, "DINO WHISTLE", header.x + 7, header.y + 5, LABEL, 0.78F);
        rightText(graphics, settings.mode().markHint(settings.pattern()), header.right() - 6, header.y + 5,
                116, MUTED_INK, 0.60F);

        Rect order = orderRect(panel);
        drawCycleRow(graphics, order, 0, order.contains(mouseX, mouseY), modeColor(),
                "ORDER", settings.mode().title().toUpperCase(Locale.ROOT));
        Rect target = targetRect(panel);
        drawCycleRow(graphics, target, 1, target.contains(mouseX, mouseY), 0xFF8A674F,
                "TARGET", settings.mode().targetTitle(settings.pattern()).toUpperCase(Locale.ROOT));
        Rect run = runRect(panel);
        drawCycleRow(graphics, run, 2, run.contains(mouseX, mouseY), 0xFF6E7655,
                "RUN", settings.continuous() ? "LOOP" : "ONCE");
        drawRange(graphics, rangeRect(panel), mouseX, mouseY);
        drawDetails(graphics, detailsRect(panel), mouseX, mouseY);
    }

    private void drawCycleRow(GuiGraphicsExtractor graphics, Rect row, int key, boolean hovered,
                              int accent, String label, String value) {
        drawBubble(graphics, row);
        if (hovered) {
            graphics.fill(row.x + 2, row.y + 2, row.right() - 2, row.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        drawMovingText(graphics, row, key, hovered, () -> {
            bold(graphics, label, row.x + 7, row.y + 6, hovered ? accent : MUTED_INK, 0.70F);
            rightText(graphics, value + "  >", row.right() - 7, row.y + 6,
                    row.w - 57, hovered ? accent : INK, 0.72F);
        });
    }

    private void drawRange(GuiGraphicsExtractor graphics, Rect range, float mouseX, float mouseY) {
        boolean hovered = range.contains(mouseX, mouseY);
        drawBubble(graphics, range);
        if (hovered || draggingRange) {
            graphics.fill(range.x + 2, range.y + 2, range.right() - 2, range.bottom() - 2, 0x18FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        updateHover(3, hovered || draggingRange);
        bold(graphics, "RANGE", range.x + 7, range.y + 4,
                hovered || draggingRange ? 0xFFD09A16 : MUTED_INK, 0.62F);
        rightText(graphics, settings.range() + "M", range.right() - 7, range.y + 4,
                28, hovered || draggingRange ? 0xFFD09A16 : INK, 0.62F);
        int trackLeft = range.x + 7;
        int trackRight = range.right() - 7;
        int trackY = range.y + 15;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, 0xFF88664F);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = trackLeft + Math.round(ratio * (trackRight - trackLeft));
        float pulse = interactionMotion(3, hovered || draggingRange);
        float knobScale = 1.0F + pulse * 0.10F;
        Rect knob = new Rect(knobX - 5, trackY - 5, 10, 10);
        graphics.pose().pushMatrix();
        graphics.pose().translate(knob.centerX(), knob.centerY());
        graphics.pose().scale(knobScale, knobScale);
        graphics.pose().translate(-knob.centerX(), -knob.centerY());
        blit(graphics, RANGE_BUTTON, knob);
        graphics.pose().popMatrix();
    }

    private void drawDetails(GuiGraphicsExtractor graphics, Rect details, float mouseX, float mouseY) {
        drawBubble(graphics, details);
        if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT) {
            drawItemFilter(graphics, details, mouseX, mouseY);
            return;
        }
        bold(graphics, settings.mode().targetTitle(settings.pattern()).toUpperCase(Locale.ROOT),
                details.x + 7, details.y + 7, modeColor(), 0.73F);
        wrappedText(graphics, settings.mode().targetDescription(settings.pattern()),
                details.x + 7, details.y + 19, details.w - 14, INK, 0.64F, 2);
        bold(graphics, "ATTACK A VALID BLOCK TO CHOOSE A FOLLOWER", details.x + 7,
                details.bottom() - 11, MUTED_INK, 0.56F);
    }

    private void drawItemFilter(GuiGraphicsExtractor graphics, Rect details, float mouseX, float mouseY) {
        Rect search = searchRect(details);
        PrimevalBubbleUi.draw(graphics, search.x, search.y, search.w, search.h);
        List<ItemStack> visible = filteredItems();
        for (int index = 0; index < 8; index++) {
            Rect slot = filterSlot(details, index);
            blit(graphics, HOTBAR, slot);
            boolean hovered = slot.contains(mouseX, mouseY);
            if (hovered) {
                graphics.fill(slot.x + 2, slot.y + 2, slot.right() - 2, slot.bottom() - 2, 0x24FFFFFF);
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
            if (index == 0) {
                ItemStack selected = filterStack();
                graphics.item(selected.isEmpty() ? new ItemStack(Items.HOPPER) : selected, slot.x + 1, slot.y + 1);
                if (hovered) graphics.setTooltipForNextFrame(Component.literal(selected.isEmpty()
                                ? "Any loose item" : "Collect only " + selected.getHoverName().getString() + " (click to clear)"),
                        Math.round(mouseX), Math.round(mouseY));
            } else if (index - 1 < visible.size()) {
                ItemStack stack = visible.get(index - 1);
                graphics.item(stack, slot.x + 1, slot.y + 1);
                if (hovered) graphics.setTooltipForNextFrame(stack.getHoverName(), Math.round(mouseX), Math.round(mouseY));
            }
        }
        ItemStack selected = filterStack();
        String filter = selected.isEmpty() ? "ANY LOOSE ITEM" : selected.getHoverName().getString();
        fitText(graphics, filter.toUpperCase(Locale.ROOT) + "  /  " + settings.range() + " BLOCK SEARCH",
                details.x + 7, details.bottom() - 10, details.w - 14, MUTED_INK, 0.56F, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Rect panel = panel();
        Motion motion = motion(panel);
        double mouseX = motion.inverseX(event.x());
        double mouseY = motion.inverseY(event.y());
        if (orderRect(panel).contains(mouseX, mouseY)) {
            DinoWhistleSettings.FieldMode[] values = DinoWhistleSettings.FieldMode.values();
            settings = copy(values[(settings.mode().ordinal() + 1) % values.length], settings.pattern(),
                    settings.continuous(), settings.range(), settings.itemFilter());
            changed(0.96F, 0);
            return true;
        }
        if (targetRect(panel).contains(mouseX, mouseY)) {
            DinoWhistleSettings.Pattern[] values = DinoWhistleSettings.Pattern.values();
            settings = copy(settings.mode(), values[(settings.pattern().ordinal() + 1) % values.length],
                    settings.continuous(), settings.range(), settings.itemFilter());
            changed(1.02F, 1);
            return true;
        }
        if (runRect(panel).contains(mouseX, mouseY)) {
            settings = copy(settings.mode(), settings.pattern(), !settings.continuous(),
                    settings.range(), settings.itemFilter());
            changed(1.06F, 2);
            return true;
        }
        if (rangeRect(panel).contains(mouseX, mouseY)) {
            draggingRange = true;
            pressed(3);
            updateRange(mouseX);
            return true;
        }
        if (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT) {
            Rect details = detailsRect(panel);
            for (int index = 0; index < 8; index++) {
                if (!filterSlot(details, index).contains(mouseX, mouseY)) continue;
                if (index == 0) {
                    settings = copy(settings.mode(), settings.pattern(), settings.continuous(), settings.range(), "");
                    changed(0.88F, 4);
                    return true;
                }
                List<ItemStack> visible = filteredItems();
                if (index - 1 < visible.size()) {
                    String id = BuiltInRegistries.ITEM.getKey(visible.get(index - 1).getItem()).toString();
                    settings = copy(settings.mode(), settings.pattern(), settings.continuous(), settings.range(), id);
                    changed(1.12F, 4);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRange && event.button() == 0) {
            updateRange(motion(panel()).inverseX(event.x()));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingRange) {
            draggingRange = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateRange(double mouseX) {
        Rect range = rangeRect(panel());
        int trackLeft = range.x + 7;
        int trackRight = range.right() - 7;
        float ratio = Mth.clamp((float)((mouseX - trackLeft) / (trackRight - trackLeft)), 0.0F, 1.0F);
        int value = Math.round(Mth.lerp(ratio, DinoWhistleSettings.MIN_RANGE, DinoWhistleSettings.MAX_RANGE));
        if (value != settings.range()) {
            settings = copy(settings.mode(), settings.pattern(), settings.continuous(), value, settings.itemFilter());
            send();
        }
    }

    private void updateSearchBox(Rect panel, Motion motion) {
        if (itemSearch == null) return;
        boolean visible = settings.mode() == DinoWhistleSettings.FieldMode.COLLECT;
        Rect logical = searchRect(detailsRect(panel));
        float left = motion.screenX(logical.x + 5);
        float top = motion.screenY(logical.y + 2);
        float right = motion.screenX(logical.right() - 4);
        float bottom = motion.screenY(logical.bottom() - 2);
        itemSearch.setX(Math.round(left));
        itemSearch.setY(Math.round(top));
        itemSearch.setWidth(Math.max(20, Math.round(right - left)));
        itemSearch.setHeight(Math.max(9, Math.round(bottom - top)));
        itemSearch.setVisible(visible);
        if (!visible) itemSearch.setFocused(false);
    }

    private List<ItemStack> filteredItems() {
        if (itemSearch == null) return List.of();
        String query = itemSearch.getValue().trim().toLowerCase(Locale.ROOT);
        return itemCatalogue.stream().filter(stack -> {
            if (query.isEmpty()) return true;
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            return name.contains(query) || id.contains(query.replace(' ', '_'));
        }).limit(7).toList();
    }

    private ItemStack filterStack() {
        if (!settings.filtersItems()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(settings.itemFilter());
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.get(id)
                .map(holder -> holder.value().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }

    private DinoWhistleSettings copy(DinoWhistleSettings.FieldMode mode, DinoWhistleSettings.Pattern pattern,
                                      boolean continuous, int range, String itemFilter) {
        return new DinoWhistleSettings(mode, pattern, continuous, range, itemFilter);
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
        ClientPacketDistributor.sendToServer(new ConfigureDinoWhistlePayload(settings.mode().ordinal(),
                settings.pattern().ordinal(), settings.continuous(), settings.range(), settings.itemFilter()));
    }

    @Override
    public void onClose() {
        send();
        PrimevalUiSounds.close(this);
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private Rect panel() { return new Rect((width - PANEL_WIDTH) / 2, (height - PANEL_HEIGHT) / 2, PANEL_WIDTH, PANEL_HEIGHT); }
    private Rect headerRect(Rect panel) { return new Rect(panel.x, panel.y, panel.w, 18); }
    private Rect orderRect(Rect panel) { return new Rect(panel.x, panel.y + 21, panel.w, 20); }
    private Rect targetRect(Rect panel) { return new Rect(panel.x, panel.y + 44, panel.w, 20); }
    private Rect runRect(Rect panel) { return new Rect(panel.x, panel.y + 67, 96, 20); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x + 99, panel.y + 67, 96, 20); }
    private Rect detailsRect(Rect panel) { return new Rect(panel.x, panel.y + 90, panel.w, 58); }
    private Rect searchRect(Rect details) { return new Rect(details.x + 5, details.y + 5, details.w - 10, 14); }
    private Rect filterSlot(Rect details, int index) { return new Rect(details.x + 7 + index * 22, details.y + 22, 18, 18); }

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
        float time = (renderNow - openedAt) / 50_000_000.0F;
        float x = Mth.sin(time * 0.48F) * 0.45F * amount;
        float y = Mth.sin(time * 0.61F + 1.7F) * 0.24F * amount;
        float scale = 1.0F + Mth.sin(time * 0.43F) * 0.010F * amount;
        if (pressedKey == key) {
            float press = Mth.clamp(1.0F - (renderNow - pressedAt) / 320_000_000.0F, 0.0F, 1.0F);
            scale -= Mth.sin(press * Mth.PI) * 0.04F;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.centerX() + x, rect.centerY() + y);
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
        float amount = 0.0F;
        if (hovered && hoverStarted[key] != 0L) {
            float seconds = (renderNow - hoverStarted[key]) / 1_000_000_000.0F;
            amount = (1.0F - (float)Math.exp(-seconds * 18.0F)) * (float)Math.exp(-seconds * 2.8F);
            if (seconds >= 1.35F) amount = 0.0F;
        }
        if (pressedKey == key) amount = Math.max(amount,
                Mth.clamp(1.0F - (renderNow - pressedAt) / 320_000_000.0F, 0.0F, 1.0F));
        return amount;
    }

    private void wrappedText(GuiGraphicsExtractor graphics, String value, float x, float y,
                             float maxWidth, int color, float scale, int maxLines) {
        int logicalWidth = Math.max(1, Math.round(maxWidth / scale));
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(value), logicalWidth);
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + index * 8.0F);
            graphics.pose().scale(scale, scale);
            graphics.text(font, lines.get(index), 0, 0, color, true);
            graphics.pose().popMatrix();
        }
    }

    private void bold(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value).withStyle(Style.EMPTY.withBold(true)), x, y, color, scale);
    }

    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale, boolean bold) {
        Component component = bold ? Component.literal(value).withStyle(Style.EMPTY.withBold(true)) : Component.literal(value);
        drawText(graphics, component, x, y, color,
                Math.min(requestedScale, maxWidth / Math.max(1, font.width(component))));
    }

    private void rightText(GuiGraphicsExtractor graphics, String value, float right, float y,
                           float maxWidth, int color, float requestedScale) {
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        float scale = Math.min(requestedScale, maxWidth / Math.max(1, font.width(component)));
        drawText(graphics, component, right - font.width(component) * scale, y, color, scale);
    }

    private void drawText(GuiGraphicsExtractor graphics, Component value, float x, float y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, value, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void blit(GuiGraphicsExtractor graphics, Identifier texture, Rect rect) {
        graphics.blit(texture, rect.x, rect.y, rect.right(), rect.bottom(), 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/gui/" + name);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private record Motion(float pivotX, float pivotY, float offsetX, float offsetY, float scale) {
        float inverseX(double mouseX) { return pivotX + ((float)mouseX - pivotX - offsetX) / scale; }
        float inverseY(double mouseY) { return pivotY + ((float)mouseY - pivotY - offsetY) / scale; }
        float screenX(float logicalX) { return pivotX + (logicalX - pivotX) * scale + offsetX; }
        float screenY(float logicalY) { return pivotY + (logicalY - pivotY) * scale + offsetY; }
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
