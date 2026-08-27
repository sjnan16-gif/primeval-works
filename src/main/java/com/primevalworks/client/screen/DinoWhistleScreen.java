package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.primevalworks.network.payload.ConfigureDinoWhistlePayload;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class DinoWhistleScreen extends Screen {
    private static final int PANEL_WIDTH = 382;
    private static final int PANEL_HEIGHT = 216;
    private static final int PANEL = 0xF319171C;
    private static final int PANEL_INNER = 0xFF242027;
    private static final int CARD = 0xFF2D282E;
    private static final int CARD_HOVER = 0xFF3A3138;
    private static final int EDGE = 0xFF6D4E3B;
    private static final int TEXT = 0xFFD7D0CB;
    private static final int MUTED = 0xFF8D8584;
    private static final int TITLE = 0xFFE98A56;
    private static final int[] MODE_COLORS = {0xFFC76A43, 0xFF6F9B62, 0xFFD09A24, 0xFF5B91B0};
    private static final int[] PATTERN_COLORS = {0xFF9A7B60, 0xFF6D8E9D, 0xFF7D6D9D};
    private final long[] hoverStarted = new long[9];
    private DinoWhistleSettings settings;
    private long openedAt;
    private long renderNow;
    private long pressedAt;
    private int pressedKey = -1;
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
        PrimevalUiSounds.open(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        Rect panel = panel();
        Motion motion = motion();
        float uiMouseX = panel.centerX() + (mouseX - panel.centerX()) / motion.scale;
        float uiMouseY = panel.centerY() + (mouseY - panel.centerY() - motion.offsetY) / motion.scale;

        graphics.fill(0, 0, width, height, 0x72000000);
        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.centerX(), panel.centerY() + motion.offsetY);
        graphics.pose().scale(motion.scale, motion.scale);
        graphics.pose().translate(-panel.centerX(), -panel.centerY());
        drawPanel(graphics, panel, uiMouseX, uiMouseY);
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        graphics.fill(panel.x + 6, panel.y + 7, panel.right() + 6, panel.bottom() + 7, 0x82000000);
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), PANEL);
        graphics.fill(panel.x + 3, panel.y + 3, panel.right() - 3, panel.bottom() - 3, PANEL_INNER);
        graphics.outline(panel.x, panel.y, panel.w, panel.h, 0xFF3B2A26);
        graphics.outline(panel.x + 3, panel.y + 3, panel.w - 6, panel.h - 6, EDGE);
        graphics.fill(panel.x + 8, panel.y + 35, panel.right() - 8, panel.y + 36, 0xFF49383A);
        bold(graphics, "DINO WHISTLE", panel.x + 12, panel.y + 9, TITLE, 1.0F);
        text(graphics, "Set a field order for one of your following companions.",
                panel.x + 12, panel.y + 23, MUTED, 0.80F);

        bold(graphics, "FIELD ORDER", panel.x + 12, panel.y + 42, MUTED, 0.82F);
        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.values()[index];
            Rect card = modeRect(panel, index);
            boolean selected = settings.mode() == mode;
            boolean hovered = card.contains(mouseX, mouseY);
            drawCard(graphics, card, selected, hovered, MODE_COLORS[index]);
            int cardIndex = index;
            drawMovingText(graphics, card, index, hovered, () -> centeredBold(graphics,
                    mode.title().toUpperCase(), card, selected || hovered ? MODE_COLORS[cardIndex] : TEXT, 0.90F));
        }

        bold(graphics, "SELECTION", panel.x + 12, panel.y + 91, MUTED, 0.82F);
        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            DinoWhistleSettings.Pattern pattern = DinoWhistleSettings.Pattern.values()[index];
            Rect card = patternRect(panel, index);
            boolean selected = settings.pattern() == pattern;
            boolean hovered = card.contains(mouseX, mouseY);
            drawCard(graphics, card, selected, hovered, PATTERN_COLORS[index]);
            int cardIndex = index;
            int key = 4 + index;
            drawMovingText(graphics, card, key, hovered, () -> centeredBold(graphics,
                    pattern.title().toUpperCase(), card, selected || hovered ? PATTERN_COLORS[cardIndex] : TEXT, 0.88F));
        }

        Rect repeat = repeatRect(panel);
        boolean repeatHovered = repeat.contains(mouseX, mouseY);
        int repeatColor = settings.continuous() ? 0xFF6E9D67 : 0xFFB17A54;
        drawCard(graphics, repeat, true, repeatHovered, repeatColor);
        drawMovingText(graphics, repeat, 7, repeatHovered, () -> centeredBold(graphics,
                settings.continuous() ? "CONTINUOUS" : "ONE TIME", repeat,
                repeatHovered ? repeatColor : TEXT, 0.88F));

        Rect range = rangeRect(panel);
        boolean rangeHovered = range.contains(mouseX, mouseY);
        updateHover(8, rangeHovered || draggingRange);
        bold(graphics, "WORK RANGE", range.x, range.y - 15, rangeHovered ? 0xFFD9A04F : MUTED, 0.82F);
        rightText(graphics, settings.range() + " BLOCKS", range.right(), range.y - 15,
                range.w, rangeHovered ? 0xFFD9A04F : TEXT, 0.82F);
        graphics.fill(range.x, range.y + 4, range.right(), range.y + 10, 0xFF100F13);
        graphics.fill(range.x + 2, range.y + 6, range.right() - 2, range.y + 8, 0xFF51454A);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = range.x + Math.round(ratio * range.w);
        float pulse = interactionMotion(8, rangeHovered || draggingRange);
        float knobScale = 1.0F + pulse * 0.08F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(knobX, range.y + 7);
        graphics.pose().scale(knobScale, knobScale);
        graphics.pose().translate(-knobX, -(range.y + 7));
        graphics.fill(knobX - 5, range.y, knobX + 6, range.y + 14, 0xFF171318);
        graphics.fill(knobX - 3, range.y + 2, knobX + 4, range.y + 12, 0xFFD9A04F);
        graphics.pose().popMatrix();

        Help help = hoveredHelp(panel, mouseX, mouseY);
        Rect helpRect = new Rect(panel.x + 10, panel.bottom() - 34, panel.w - 20, 24);
        graphics.fill(helpRect.x, helpRect.y, helpRect.right(), helpRect.bottom(), 0xFF151318);
        graphics.outline(helpRect.x, helpRect.y, helpRect.w, helpRect.h, 0xFF4B3A3A);
        graphics.fill(helpRect.x + 2, helpRect.y + 2, helpRect.x + 5, helpRect.bottom() - 2, help.color);
        fitText(graphics, help.title, helpRect.x + 10, helpRect.y + 5, 92, help.color, 0.82F, true);
        fitText(graphics, help.detail, helpRect.x + 104, helpRect.y + 5,
                helpRect.w - 114, TEXT, 0.74F, true);
    }

    private Help hoveredHelp(Rect panel, float mouseX, float mouseY) {
        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            if (modeRect(panel, index).contains(mouseX, mouseY)) {
                DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.values()[index];
                return new Help(mode.title().toUpperCase(), mode.description(), MODE_COLORS[index]);
            }
        }
        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            if (patternRect(panel, index).contains(mouseX, mouseY)) {
                DinoWhistleSettings.Pattern pattern = DinoWhistleSettings.Pattern.values()[index];
                return new Help(pattern.title().toUpperCase(), pattern.description(), PATTERN_COLORS[index]);
            }
        }
        if (repeatRect(panel).contains(mouseX, mouseY)) {
            return settings.continuous()
                    ? new Help("CONTINUOUS", "Rescans after every completed pass.", 0xFF6E9D67)
                    : new Help("ONE TIME", "Stops when the marked order is complete.", 0xFFB17A54);
        }
        if (rangeRect(panel).contains(mouseX, mouseY)) {
            return new Help("WORK RANGE", "Maximum distance the worker may travel from you.", 0xFFD9A04F);
        }
        DinoWhistleSettings.FieldMode mode = settings.mode();
        return new Help(mode.title().toUpperCase(), mode.description(), MODE_COLORS[mode.ordinal()]);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Rect panel = panel();
        Motion motion = motion();
        double uiMouseX = panel.centerX() + (event.x() - panel.centerX()) / motion.scale;
        double uiMouseY = panel.centerY() + (event.y() - panel.centerY() - motion.offsetY) / motion.scale;
        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            if (modeRect(panel, index).contains(uiMouseX, uiMouseY)) {
                settings = new DinoWhistleSettings(DinoWhistleSettings.FieldMode.values()[index],
                        settings.pattern(), settings.continuous(), settings.range());
                changed(0.96F, index);
                return true;
            }
        }
        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            if (patternRect(panel, index).contains(uiMouseX, uiMouseY)) {
                settings = new DinoWhistleSettings(settings.mode(), DinoWhistleSettings.Pattern.values()[index],
                        settings.continuous(), settings.range());
                changed(1.04F, 4 + index);
                return true;
            }
        }
        if (repeatRect(panel).contains(uiMouseX, uiMouseY)) {
            settings = new DinoWhistleSettings(settings.mode(), settings.pattern(),
                    !settings.continuous(), settings.range());
            changed(1.0F, 7);
            return true;
        }
        if (rangeRect(panel).contains(uiMouseX, uiMouseY)) {
            draggingRange = true;
            pressed(8);
            updateRange(uiMouseX);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRange && event.button() == 0) {
            Rect panel = panel();
            Motion motion = motion();
            updateRange(panel.centerX() + (event.x() - panel.centerX()) / motion.scale);
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
        float ratio = Mth.clamp((float)((mouseX - range.x) / range.w), 0.0F, 1.0F);
        int value = Math.round(Mth.lerp(ratio, DinoWhistleSettings.MIN_RANGE, DinoWhistleSettings.MAX_RANGE));
        if (value != settings.range()) {
            settings = new DinoWhistleSettings(settings.mode(), settings.pattern(), settings.continuous(), value);
            send();
        }
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
                settings.pattern().ordinal(), settings.continuous(), settings.range()));
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
    private Rect modeRect(Rect panel, int index) { return new Rect(panel.x + 12 + index * 90, panel.y + 55, 84, 27); }
    private Rect patternRect(Rect panel, int index) { return new Rect(panel.x + 12 + index * 91, panel.y + 104, 84, 27); }
    private Rect repeatRect(Rect panel) { return new Rect(panel.right() - 99, panel.y + 104, 87, 27); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x + 18, panel.y + 156, panel.w - 36, 14); }

    private Motion motion() {
        long now = renderNow == 0L ? Util.getNanos() : renderNow;
        float progress = Mth.clamp((now - openedAt) / 310_000_000.0F, 0.0F, 1.0F);
        float settled = spring(progress, 6.2F, 11.4F);
        float fit = Math.min(1.0F, Math.min((width - 12.0F) / PANEL_WIDTH, (height - 12.0F) / PANEL_HEIGHT));
        return new Motion(Math.max(0.1F, fit * (0.76F + settled * 0.24F)),
                16.0F * fit * (1.0F - settled));
    }

    private void drawCard(GuiGraphicsExtractor graphics, Rect rect, boolean selected, boolean hovered, int accent) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), hovered ? CARD_HOVER : CARD);
        graphics.outline(rect.x, rect.y, rect.w, rect.h, selected || hovered ? accent : 0xFF51454A);
        graphics.fill(rect.x + 2, rect.y + 2, rect.x + 5, rect.bottom() - 2, selected ? accent : 0xFF5C5356);
        if (selected) graphics.fill(rect.x + 5, rect.y + 2, rect.right() - 2, rect.bottom() - 2, accent & 0x28FFFFFF);
        if (hovered) {
            graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x16FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void drawMovingText(GuiGraphicsExtractor graphics, Rect rect, int key, boolean hovered, Runnable draw) {
        updateHover(key, hovered);
        float motion = interactionMotion(key, hovered);
        if (motion <= 0.001F) {
            draw.run();
            return;
        }
        float time = (renderNow - openedAt) / 1_000_000_000.0F;
        float x = Mth.sin(time * 7.1F + key) * 0.55F * motion;
        float y = Mth.sin(time * 8.6F + key * 0.7F) * 0.28F * motion;
        float scale = 1.0F + Mth.sin(time * 6.4F + key) * 0.014F * motion;
        if (pressedKey == key) {
            float press = Mth.clamp(1.0F - (renderNow - pressedAt) / 280_000_000.0F, 0.0F, 1.0F);
            scale -= Mth.sin(press * Mth.PI) * 0.045F;
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
        } else {
            hoverStarted[key] = 0L;
        }
    }

    private float interactionMotion(int key, boolean hovered) {
        float amount = 0.0F;
        if (hovered && hoverStarted[key] != 0L) {
            float seconds = (renderNow - hoverStarted[key]) / 1_000_000_000.0F;
            amount = (1.0F - (float)Math.exp(-seconds * 18.0F)) * (float)Math.exp(-seconds * 2.8F);
            if (seconds >= 1.35F) amount = 0.0F;
        }
        if (pressedKey == key) {
            amount = Math.max(amount, Mth.clamp(1.0F - (renderNow - pressedAt) / 280_000_000.0F, 0.0F, 1.0F));
        }
        return amount;
    }

    private void centeredBold(GuiGraphicsExtractor graphics, String value, Rect rect, int color, float requestedScale) {
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        float scale = Math.min(requestedScale, (rect.w - 10.0F) / Math.max(1, font.width(component)));
        float x = rect.centerX() - font.width(component) * scale * 0.5F;
        float y = rect.centerY() - font.lineHeight * scale * 0.5F;
        drawText(graphics, component, x, y, color, scale);
    }

    private void bold(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value).withStyle(Style.EMPTY.withBold(true)), x, y, color, scale);
    }

    private void text(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value).withStyle(Style.EMPTY.withBold(true)), x, y, color, scale);
    }

    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale, boolean bold) {
        Component component = bold ? Component.literal(value).withStyle(Style.EMPTY.withBold(true)) : Component.literal(value);
        float scale = Math.min(requestedScale, maxWidth / Math.max(1, font.width(component)));
        drawText(graphics, component, x, y, color, scale);
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

    private static float spring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) return 1.0F;
        double wave = Math.cos(frequency * progress) + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
    }

    private record Help(String title, String detail, int color) {}

    private record Motion(float scale, float offsetY) {}

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
