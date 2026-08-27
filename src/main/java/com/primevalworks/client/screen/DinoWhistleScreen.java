package com.primevalworks.client.screen;

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
    private static final int PANEL_WIDTH = 316;
    private static final int PANEL_HEIGHT = 192;
    private DinoWhistleSettings settings;
    private long openedAt;
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
        Rect panel = panel();
        float progress = Mth.clamp((Util.getNanos() - openedAt) / 260_000_000.0F, 0.0F, 1.0F);
        float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0D);
        float scale = 0.86F + eased * 0.14F + Mth.sin(progress * Mth.PI) * 0.018F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.centerX(), panel.centerY());
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-panel.centerX(), -panel.centerY());
        drawPanel(graphics, panel, mouseX, mouseY);
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Rect panel, int mouseX, int mouseY) {
        graphics.fill(panel.x + 5, panel.y + 6, panel.right() + 5, panel.bottom() + 6, 0x6A000000);
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), 0xF0C9A77F);
        graphics.fill(panel.x + 3, panel.y + 3, panel.right() - 3, panel.bottom() - 3, 0xFFDDBB91);
        graphics.outline(panel.x, panel.y, panel.w, panel.h, 0xFF5F4231);
        graphics.outline(panel.x + 3, panel.y + 3, panel.w - 6, panel.h - 6, 0xFF9C7354);
        bold(graphics, "DINO WHISTLE", panel.x + 12, panel.y + 9, 0xFF6B392D, 1.05F);
        text(graphics, "Choose what a following companion should do in the field.",
                panel.x + 12, panel.y + 24, 0xFF554A44, 0.72F);

        int rowY = panel.y + 42;
        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.values()[index];
            Rect card = modeRect(panel, index);
            boolean selected = settings.mode() == mode;
            boolean hovered = card.contains(mouseX, mouseY);
            drawCard(graphics, card, selected, hovered, 0xFFB85E35);
            bold(graphics, mode.title().toUpperCase(), card.x + 7, card.y + 5,
                    selected || hovered ? 0xFF9A3F29 : 0xFF514640, 0.72F);
        }

        bold(graphics, "PATTERN", panel.x + 12, rowY + 34, 0xFF6B392D, 0.76F);
        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            DinoWhistleSettings.Pattern pattern = DinoWhistleSettings.Pattern.values()[index];
            Rect card = patternRect(panel, index);
            boolean selected = settings.pattern() == pattern;
            boolean hovered = card.contains(mouseX, mouseY);
            drawCard(graphics, card, selected, hovered, 0xFF477895);
            bold(graphics, pattern.title().toUpperCase(), card.x + 6, card.y + 5,
                    selected || hovered ? 0xFF315F7B : 0xFF514640, 0.68F);
        }

        Rect repeat = repeatRect(panel);
        drawCard(graphics, repeat, settings.continuous(), repeat.contains(mouseX, mouseY), 0xFF5B8752);
        bold(graphics, settings.continuous() ? "CONTINUOUS" : "ONE TIME", repeat.x + 8, repeat.y + 5,
                settings.continuous() ? 0xFF3C713D : 0xFF5E514A, 0.72F);

        Rect range = rangeRect(panel);
        bold(graphics, "WORK RANGE  " + settings.range() + " BLOCKS", range.x, range.y - 14, 0xFF6B392D, 0.74F);
        graphics.fill(range.x, range.y + 4, range.right(), range.y + 9, 0xFF775B47);
        graphics.fill(range.x + 2, range.y + 6, range.right() - 2, range.y + 7, 0xFFD7B48C);
        float ratio = (settings.range() - DinoWhistleSettings.MIN_RANGE)
                / (float)(DinoWhistleSettings.MAX_RANGE - DinoWhistleSettings.MIN_RANGE);
        int knobX = range.x + Math.round(ratio * range.w);
        graphics.fill(knobX - 4, range.y, knobX + 5, range.y + 13, 0xFF5B3D31);
        graphics.fill(knobX - 2, range.y + 2, knobX + 3, range.y + 11, 0xFFF1CC91);

        String help = hoveredHelp(panel, mouseX, mouseY);
        fitText(graphics, help, panel.x + 12, panel.bottom() - 18,
                panel.w - 24, 0xFF5A4B43, 0.70F);
    }

    private String hoveredHelp(Rect panel, int mouseX, int mouseY) {
        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            if (modeRect(panel, index).contains(mouseX, mouseY)) return DinoWhistleSettings.FieldMode.values()[index].description();
        }
        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            if (patternRect(panel, index).contains(mouseX, mouseY)) return DinoWhistleSettings.Pattern.values()[index].description();
        }
        if (repeatRect(panel).contains(mouseX, mouseY)) return settings.continuous()
                ? "Rescans the marked area after each pass." : "Stops when the marked work is finished.";
        return "Attack a block with the whistle to mark the job. Area mode uses two corners.";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Rect panel = panel();
        for (int index = 0; index < DinoWhistleSettings.FieldMode.values().length; index++) {
            if (modeRect(panel, index).contains(event.x(), event.y())) {
                settings = new DinoWhistleSettings(DinoWhistleSettings.FieldMode.values()[index],
                        settings.pattern(), settings.continuous(), settings.range());
                changed(0.96F);
                return true;
            }
        }
        for (int index = 0; index < DinoWhistleSettings.Pattern.values().length; index++) {
            if (patternRect(panel, index).contains(event.x(), event.y())) {
                settings = new DinoWhistleSettings(settings.mode(), DinoWhistleSettings.Pattern.values()[index],
                        settings.continuous(), settings.range());
                changed(1.04F);
                return true;
            }
        }
        if (repeatRect(panel).contains(event.x(), event.y())) {
            settings = new DinoWhistleSettings(settings.mode(), settings.pattern(),
                    !settings.continuous(), settings.range());
            changed(1.0F);
            return true;
        }
        if (rangeRect(panel).contains(event.x(), event.y())) {
            draggingRange = true;
            updateRange(event.x());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRange && event.button() == 0) {
            updateRange(event.x());
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

    private void changed(float pitch) {
        PrimevalUiSounds.click(pitch);
        send();
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
    private Rect modeRect(Rect panel, int index) { return new Rect(panel.x + 12 + index * 73, panel.y + 42, 68, 24); }
    private Rect patternRect(Rect panel, int index) { return new Rect(panel.x + 12 + index * 73, panel.y + 91, 68, 24); }
    private Rect repeatRect(Rect panel) { return new Rect(panel.right() - 85, panel.y + 91, 73, 24); }
    private Rect rangeRect(Rect panel) { return new Rect(panel.x + 18, panel.y + 143, panel.w - 36, 13); }

    private void drawCard(GuiGraphicsExtractor graphics, Rect rect, boolean selected, boolean hovered, int accent) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), selected ? 0xFFE9C99B : 0xFFC3A078);
        graphics.outline(rect.x, rect.y, rect.w, rect.h, selected ? accent : 0xFF80604A);
        if (hovered) graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x25FFFFFF);
        if (selected) graphics.fill(rect.x + 2, rect.bottom() - 4, rect.right() - 2, rect.bottom() - 2, accent);
    }

    private void bold(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value).withStyle(Style.EMPTY.withBold(true)), x, y, color, scale);
    }
    private void text(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value), x, y, color, scale);
    }
    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale) {
        float scale = Math.min(requestedScale, maxWidth / Math.max(1, font.width(value)));
        drawText(graphics, Component.literal(value), x, y, color, scale);
    }
    private void drawText(GuiGraphicsExtractor graphics, Component value, float x, float y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, value, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        int centerX() { return x + w / 2; }
        int centerY() { return y + h / 2; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
