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
    private static final int PANEL_HEIGHT = 206;
    private static final int TEXT = 0xFFD8D1CB;
    private static final int MUTED = 0xFF918987;
    private static final int TITLE = 0xFFE98A56;
    private static final int[] MODE_COLORS = {0xFFC76A43, 0xFF6F9B62, 0xFFD09A24, 0xFF5B91B0};
    private static final int[] PATTERN_COLORS = {0xFF9A7B60, 0xFF6D8E9D, 0xFF7D6D9D};

    private final long[] hoverStarted = new long[9];
    private DinoWhistleSettings settings;
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
        if (!whistle.isEmpty()) {
            minecraft.setScreen(new DinoWhistleScreen(DinoWhistleSettings.read(whistle)));
        }
    }

    @Override
    protected void init() {
        openedAt = Util.getNanos();
        previousFrame = openedAt;
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

        graphics.fill(0, 0, width, height, 0x72000000);
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        drawPanel(graphics, panel, uiMouseX, uiMouseY);
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        graphics.fill(panel.x + 6, panel.y + 7, panel.right() + 6, panel.bottom() + 7, 0x68000000);
        PrimevalBubbleUi.drawDark(graphics, panel.x, panel.y, panel.w, panel.h);

        Rect header = new Rect(panel.x + 8, panel.y + 8, panel.w - 16, 31);
        PrimevalBubbleUi.drawDark(graphics, header.x, header.y, header.w, header.h);
        bold(graphics, "DINO WHISTLE", header.x + 9, header.y + 5, TITLE, 1.0F);
        fitText(graphics, "Set a field order for one of your following companions.",
                header.x + 9, header.y + 17, header.w - 18, MUTED, 0.76F, true);

        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.values()[index];
            Rect card = modeRect(panel, index);
            boolean selected = settings.mode() == mode;
            boolean hovered = card.contains(mouseX, mouseY);
            drawControl(graphics, card, index, selected, hovered, MODE_COLORS[index],
                    mode.title().toUpperCase(), 0.90F);
        }

        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            DinoWhistleSettings.Pattern pattern = DinoWhistleSettings.Pattern.values()[index];
            Rect card = patternRect(panel, index);
            boolean selected = settings.pattern() == pattern;
            boolean hovered = card.contains(mouseX, mouseY);
            drawControl(graphics, card, 4 + index, selected, hovered, PATTERN_COLORS[index],
                    pattern.title().toUpperCase(), 0.88F);
        }

        Rect repeat = repeatRect(panel);
        boolean repeatHovered = repeat.contains(mouseX, mouseY);
        int repeatColor = settings.continuous() ? 0xFF6E9D67 : 0xFFB17A54;
        drawControl(graphics, repeat, 7, true, repeatHovered, repeatColor,
                settings.continuous() ? "CONTINUOUS" : "ONE TIME", 0.88F);

        drawRange(graphics, panel, mouseX, mouseY);
        drawHelp(graphics, panel, mouseX, mouseY);
    }

    private void drawControl(GuiGraphicsExtractor graphics, Rect card, int key, boolean selected,
                             boolean hovered, int accent, String label, float scale) {
        PrimevalBubbleUi.drawDarkControl(graphics, card.x, card.y, card.w, card.h, accent, selected, hovered);
        if (hovered) graphics.requestCursor(CursorTypes.POINTING_HAND);
        drawMovingText(graphics, card, key, hovered, () -> centeredBold(graphics,
                label, card, selected || hovered ? accent : TEXT, scale));
    }

    private void drawRange(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        Rect range = rangeRect(panel);
        boolean hovered = range.contains(mouseX, mouseY);
        updateHover(8, hovered || draggingRange);
        PrimevalBubbleUi.drawDarkControl(graphics, range.x, range.y, range.w, range.h,
                0xFFD9A04F, draggingRange, hovered);
        if (hovered) graphics.requestCursor(CursorTypes.POINTING_HAND);

        bold(graphics, "WORK RANGE", range.x + 10, range.y + 5,
                hovered ? 0xFFD9A04F : MUTED, 0.80F);
        rightText(graphics, settings.range() + " BLOCKS", range.right() - 9, range.y + 5,
                range.w / 2.0F, hovered ? 0xFFD9A04F : TEXT, 0.80F);
        int trackLeft = range.x + 94;
        int trackRight = range.right() - 82;
        int trackY = range.y + 10;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 3, 0xFF3D3739);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = trackLeft + Math.round(ratio * (trackRight - trackLeft));
        float pulse = interactionMotion(8, hovered || draggingRange);
        float knobScale = 1.0F + pulse * 0.08F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(knobX, trackY + 1.5F);
        graphics.pose().scale(knobScale, knobScale);
        graphics.pose().translate(-knobX, -(trackY + 1.5F));
        graphics.fill(knobX - 4, range.y + 5, knobX + 5, range.bottom() - 5, 0xFFD9A04F);
        graphics.pose().popMatrix();
    }

    private void drawHelp(GuiGraphicsExtractor graphics, Rect panel, float mouseX, float mouseY) {
        Help help = hoveredHelp(panel, mouseX, mouseY);
        Rect helpRect = new Rect(panel.x + 8, panel.bottom() - 45, panel.w - 16, 37);
        PrimevalBubbleUi.drawDarkControl(graphics, helpRect.x, helpRect.y, helpRect.w, helpRect.h,
                help.color, true, false);
        fitText(graphics, help.title, helpRect.x + 10, helpRect.y + 6,
                helpRect.w - 20, help.color, 0.88F, true);
        fitText(graphics, help.detail, helpRect.x + 10, helpRect.y + 20,
                helpRect.w - 20, TEXT, 0.78F, true);
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
        Motion motion = motion(panel);
        double uiMouseX = motion.inverseX(event.x());
        double uiMouseY = motion.inverseY(event.y());
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
        int trackLeft = range.x + 94;
        int trackRight = range.right() - 82;
        float ratio = Mth.clamp((float)((mouseX - trackLeft) / (trackRight - trackLeft)), 0.0F, 1.0F);
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

    private Rect panel() {
        return new Rect((width - PANEL_WIDTH) / 2, (height - PANEL_HEIGHT) / 2, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private Rect modeRect(Rect panel, int index) {
        return new Rect(panel.x + 8 + index * 91, panel.y + 47, 86, 28);
    }

    private Rect patternRect(Rect panel, int index) {
        return new Rect(panel.x + 8 + index * 91, panel.y + 83, 86, 28);
    }

    private Rect repeatRect(Rect panel) {
        return new Rect(panel.right() - 94, panel.y + 83, 86, 28);
    }

    private Rect rangeRect(Rect panel) {
        return new Rect(panel.x + 8, panel.y + 119, panel.w - 16, 28);
    }

    private void updateParallax(int mouseX, int mouseY) {
        float delta = Mth.clamp((renderNow - previousFrame) / 1_000_000_000.0F, 0.0F, 0.05F);
        previousFrame = renderNow;
        float targetX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * -3.2F;
        float targetY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * -2.0F;
        float blend = 1.0F - (float)Math.exp(-delta * 9.0F);
        parallaxX = Mth.lerp(blend, parallaxX, targetX);
        parallaxY = Mth.lerp(blend, parallaxY, targetY);
    }

    private Motion motion(Rect panel) {
        long now = renderNow == 0L ? Util.getNanos() : renderNow;
        float elapsedTicks = (now - openedAt) / 50_000_000.0F;
        float progress = Mth.clamp(elapsedTicks / 34.0F, 0.0F, 1.0F);
        float settled = PrimevalBubbleUi.spring(progress, 7.2F, 10.5F);
        float fade = smoothStep(Mth.clamp(elapsedTicks / 24.0F, 0.0F, 1.0F));
        float fit = Math.min(1.0F, Math.min((width - 12.0F) / PANEL_WIDTH, (height - 12.0F) / PANEL_HEIGHT));
        float scale = Math.max(0.1F, fit * (0.955F + 0.045F * settled));
        float offsetX = (width * 0.72F + panel.w * 0.35F) * (1.0F - settled) + parallaxX * fade;
        float offsetY = parallaxY * fade;
        return new Motion(panel.centerX(), panel.centerY(), offsetX, offsetY, scale);
    }

    private void applyMotion(GuiGraphicsExtractor graphics, Motion motion) {
        graphics.pose().translate(motion.pivotX + motion.offsetX, motion.pivotY + motion.offsetY);
        graphics.pose().scale(motion.scale, motion.scale);
        graphics.pose().translate(-motion.pivotX, -motion.pivotY);
    }

    private void drawMovingText(GuiGraphicsExtractor graphics, Rect rect, int key, boolean hovered, Runnable draw) {
        updateHover(key, hovered);
        float amount = interactionMotion(key, hovered);
        if (amount <= 0.001F) {
            draw.run();
            return;
        }
        float time = (renderNow - openedAt) / 50_000_000.0F;
        float x = Mth.sin(time * 0.48F) * 0.55F * amount;
        float y = Mth.sin(time * 0.61F + 1.7F) * 0.32F * amount;
        float scale = 1.0F + Mth.sin(time * 0.43F) * 0.012F * amount;
        if (pressedKey == key) {
            float press = Mth.clamp(1.0F - (renderNow - pressedAt) / 320_000_000.0F, 0.0F, 1.0F);
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
            amount = Math.max(amount,
                    Mth.clamp(1.0F - (renderNow - pressedAt) / 320_000_000.0F, 0.0F, 1.0F));
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

    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale, boolean bold) {
        Component component = bold
                ? Component.literal(value).withStyle(Style.EMPTY.withBold(true))
                : Component.literal(value);
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

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private record Help(String title, String detail, int color) {}

    private record Motion(float pivotX, float pivotY, float offsetX, float offsetY, float scale) {
        float inverseX(double mouseX) {
            return pivotX + ((float)mouseX - pivotX - offsetX) / scale;
        }

        float inverseY(double mouseY) {
            return pivotY + ((float)mouseY - pivotY - offsetY) / scale;
        }
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
