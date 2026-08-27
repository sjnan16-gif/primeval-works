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
    private static final int PANEL_WIDTH = 270;
    private static final int PANEL_HEIGHT = 208;
    private static final int INK = 0xFF494341;
    private static final int MUTED_INK = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
    private static final int[] MODE_COLORS = {0xFFC54B2D, 0xFF547B3F, 0xFFD09A16, 0xFF477895};
    private static final int[] PATTERN_COLORS = {0xFF8A512E, 0xFF477895, 0xFF76598E};

    private final long[] hoverStarted = new long[4];
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
        Rect header = headerRect(panel);
        drawBubble(graphics, header);
        bold(graphics, "DINO WHISTLE", header.x + 10, header.y + 5, LABEL, 0.96F);
        rightText(graphics, "CLICK A ROW TO CHANGE IT", header.right() - 9, header.y + 17,
                header.w - 18, MUTED_INK, 0.66F);

        Rect order = orderRect(panel);
        drawCycleRow(graphics, order, 0, order.contains(mouseX, mouseY), modeColor(),
                "ORDER", settings.mode().title().toUpperCase());

        Rect target = targetRect(panel);
        drawCycleRow(graphics, target, 1, target.contains(mouseX, mouseY), patternColor(),
                "TARGET", settings.pattern().title().toUpperCase());

        Rect run = runRect(panel);
        drawCycleRow(graphics, run, 2, run.contains(mouseX, mouseY),
                settings.continuous() ? 0xFF547B3F : 0xFF8A512E,
                "RUN", settings.continuous() ? "CONTINUOUS" : "ONE TIME");

        drawRange(graphics, rangeRect(panel), mouseX, mouseY);
        drawHelp(graphics, helpRect(panel), hoveredHelp(panel, mouseX, mouseY));
    }

    private void drawCycleRow(GuiGraphicsExtractor graphics, Rect row, int key, boolean hovered,
                              int accent, String label, String value) {
        drawBubble(graphics, row);
        graphics.fill(row.x + 3, row.y + 3, row.x + 6, row.bottom() - 3, accent);
        if (hovered) {
            graphics.fill(row.x + 2, row.y + 2, row.right() - 2, row.bottom() - 2, 0x20FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        drawMovingText(graphics, row, key, hovered, () -> {
            bold(graphics, label, row.x + 12, row.y + 9, hovered ? accent : MUTED_INK, 0.88F);
            rightText(graphics, value + "  >", row.right() - 10, row.y + 9,
                    row.w - 80, hovered ? accent : INK, 0.88F);
        });
    }

    private void drawRange(GuiGraphicsExtractor graphics, Rect range, float mouseX, float mouseY) {
        boolean hovered = range.contains(mouseX, mouseY);
        int accent = 0xFFD09A16;
        drawBubble(graphics, range);
        graphics.fill(range.x + 3, range.y + 3, range.x + 6, range.bottom() - 3, accent);
        if (hovered || draggingRange) {
            graphics.fill(range.x + 2, range.y + 2, range.right() - 2, range.bottom() - 2, 0x20FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        updateHover(3, hovered || draggingRange);
        bold(graphics, "RANGE", range.x + 12, range.y + 5,
                hovered || draggingRange ? accent : MUTED_INK, 0.84F);
        rightText(graphics, settings.range() + " BLOCKS", range.right() - 9, range.y + 5,
                78, hovered || draggingRange ? accent : INK, 0.84F);

        int trackLeft = range.x + 72;
        int trackRight = range.right() - 68;
        int trackY = range.y + 18;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 3, 0xFF88664F);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = trackLeft + Math.round(ratio * (trackRight - trackLeft));
        float pulse = interactionMotion(3, hovered || draggingRange);
        float knobScale = 1.0F + pulse * 0.09F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(knobX, trackY + 1.5F);
        graphics.pose().scale(knobScale, knobScale);
        graphics.pose().translate(-knobX, -(trackY + 1.5F));
        graphics.fill(knobX - 3, range.y + 13, knobX + 4, range.bottom() - 4, accent);
        graphics.pose().popMatrix();
    }

    private void drawHelp(GuiGraphicsExtractor graphics, Rect rect, Help help) {
        drawBubble(graphics, rect);
        graphics.fill(rect.x + 3, rect.y + 3, rect.x + 6, rect.bottom() - 3, help.color);
        fitText(graphics, help.title, rect.x + 12, rect.y + 6,
                rect.w - 24, help.color, 0.86F, true);
        fitText(graphics, help.detail, rect.x + 12, rect.y + 21,
                rect.w - 24, INK, 0.72F, true);
    }

    private Help hoveredHelp(Rect panel, float mouseX, float mouseY) {
        if (orderRect(panel).contains(mouseX, mouseY)) {
            return new Help("ORDER / " + settings.mode().title().toUpperCase(),
                    settings.mode().description(), modeColor());
        }
        if (targetRect(panel).contains(mouseX, mouseY)) {
            return new Help("TARGET / " + settings.pattern().title().toUpperCase(),
                    settings.pattern().description(), patternColor());
        }
        if (runRect(panel).contains(mouseX, mouseY)) {
            return settings.continuous()
                    ? new Help("RUN / CONTINUOUS", "Starts another pass after the current order is finished.", 0xFF547B3F)
                    : new Help("RUN / ONE TIME", "Stops after the marked order has been completed.", 0xFF8A512E);
        }
        if (rangeRect(panel).contains(mouseX, mouseY)) {
            return new Help("RANGE / " + settings.range() + " BLOCKS",
                    "The farthest your assigned worker may travel from you.", 0xFFD09A16);
        }
        return new Help("HOW TO USE", "Attack a block to mark it. Hold use to edit this whistle.", LABEL);
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
            settings = new DinoWhistleSettings(values[(settings.mode().ordinal() + 1) % values.length],
                    settings.pattern(), settings.continuous(), settings.range());
            changed(0.96F, 0);
            return true;
        }
        if (targetRect(panel).contains(mouseX, mouseY)) {
            DinoWhistleSettings.Pattern[] values = DinoWhistleSettings.Pattern.values();
            settings = new DinoWhistleSettings(settings.mode(),
                    values[(settings.pattern().ordinal() + 1) % values.length],
                    settings.continuous(), settings.range());
            changed(1.02F, 1);
            return true;
        }
        if (runRect(panel).contains(mouseX, mouseY)) {
            settings = new DinoWhistleSettings(settings.mode(), settings.pattern(),
                    !settings.continuous(), settings.range());
            changed(1.06F, 2);
            return true;
        }
        if (rangeRect(panel).contains(mouseX, mouseY)) {
            draggingRange = true;
            pressed(3);
            updateRange(mouseX);
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
        int trackLeft = range.x + 72;
        int trackRight = range.right() - 68;
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

    private Rect headerRect(Rect panel) { return new Rect(panel.x, panel.y, panel.w, 30); }
    private Rect orderRect(Rect panel) { return new Rect(panel.x, panel.y + 34, panel.w, 29); }
    private Rect targetRect(Rect panel) { return new Rect(panel.x, panel.y + 67, panel.w, 29); }
    private Rect runRect(Rect panel) { return new Rect(panel.x, panel.y + 100, panel.w, 29); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x, panel.y + 133, panel.w, 31); }
    private Rect helpRect(Rect panel) { return new Rect(panel.x, panel.y + 168, panel.w, 40); }

    private int modeColor() { return MODE_COLORS[settings.mode().ordinal()]; }
    private int patternColor() { return PATTERN_COLORS[settings.pattern().ordinal()]; }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect) {
        graphics.fill(rect.x + 4, rect.y + 5, rect.right() + 4, rect.bottom() + 5, 0x43000000);
        PrimevalBubbleUi.draw(graphics, rect.x, rect.y, rect.w, rect.h);
    }

    private void updateParallax(int mouseX, int mouseY) {
        float delta = Mth.clamp((renderNow - previousFrame) / 1_000_000_000.0F, 0.0F, 0.05F);
        previousFrame = renderNow;
        float targetX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * -1.8F;
        float targetY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * -1.1F;
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
        float fit = Math.min(1.0F, Math.min((width - 12.0F) / PANEL_WIDTH, (height - 12.0F) / PANEL_HEIGHT));
        float scale = Math.max(0.1F, fit * (0.74F + 0.26F * settled));
        float offsetX = parallaxX * fade;
        float offsetY = 18.0F * (1.0F - settled) + parallaxY * fade;
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
