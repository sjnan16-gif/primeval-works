package com.primevalworks.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class PrimevalUiCrop {
    public static final int TEXTURE_WIDTH = 427;
    public static final int TEXTURE_HEIGHT = 240;

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "primevalworks", "textures/gui/ui_crop.png");
    private static final Region PAPER_PANEL = new Region(209, 23, 195, 74);
    private static final Region PAPER_SQUARE = new Region(84, 42, 25, 26);
    private static final Region PAPER_GLYPH = new Region(120, 50, 25, 11);
    private static final Region PAPER_BUBBLE = new Region(151, 49, 53, 15);
    private static final Region PAPER_SMALL_BUBBLE = new Region(80, 78, 29, 15);
    private static final Region PAPER_SLOT = new Region(117, 75, 18, 19);
    private static final Region PAPER_KNOB = new Region(140, 79, 10, 11);
    private static final Region PAPER_VERTICAL_RULE = new Region(155, 88, 2, 33);
    private static final Region PAPER_HORIZONTAL_RULE = new Region(162, 104, 44, 3);
    private static final Region PAPER_TRACK = new Region(80, 105, 69, 6);
    private static final Region DARK_SQUARE = new Region(81, 145, 18, 19);
    private static final Region DARK_VERTICAL_RULE = new Region(104, 143, 8, 23);
    private static final Region DARK_BUBBLE = new Region(118, 148, 72, 13);
    private static final Region DARK_KNOB = new Region(170, 175, 6, 11);
    private static final Region DARK_TRACK = new Region(84, 177, 80, 7);

    private PrimevalUiCrop() {}

    public static void paperPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        paperPanel(graphics, x, y, width, height, 255);
    }

    public static void paperPanel(GuiGraphicsExtractor graphics, int x, int y,
                                  int width, int height, int alpha) {
        sliced(graphics, PAPER_PANEL, x, y, width, height, 4, alpha);
    }

    public static void paperBubble(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        sliced(graphics, PAPER_BUBBLE, x, y, width, height, 3, 255);
    }

    public static void paperInset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        sliced(graphics, PAPER_SMALL_BUBBLE, x, y, width, height, 3, 255);
    }

    public static void paperSquare(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int alpha) {
        sliced(graphics, PAPER_SQUARE, x, y, width, height, 3, alpha);
    }

    public static void paperSlot(GuiGraphicsExtractor graphics, int x, int y,
                                 int width, int height, int alpha) {
        sliced(graphics, PAPER_SLOT, x, y, width, height, 3, alpha);
    }

    public static void paperGlyph(GuiGraphicsExtractor graphics, int x, int y,
                                  int width, int height, int alpha) {
        region(graphics, PAPER_GLYPH, x, y, width, height, alpha);
    }

    public static void paperHorizontalRule(GuiGraphicsExtractor graphics, int x, int y,
                                           int width, int height, int alpha) {
        region(graphics, PAPER_HORIZONTAL_RULE, x, y, width, height, alpha);
    }

    public static void paperVerticalRule(GuiGraphicsExtractor graphics, int x, int y,
                                         int width, int height, int alpha) {
        region(graphics, PAPER_VERTICAL_RULE, x, y, width, height, alpha);
    }

    public static void paperTrack(GuiGraphicsExtractor graphics, int x, int y,
                                  int width, int height, int alpha) {
        region(graphics, PAPER_TRACK, x, y, width, height, alpha);
    }

    public static void paperKnob(GuiGraphicsExtractor graphics, int x, int y,
                                 int width, int height, int alpha) {
        region(graphics, PAPER_KNOB, x, y, width, height, alpha);
    }

    public static void darkSquare(GuiGraphicsExtractor graphics, int x, int y,
                                  int width, int height, int alpha) {
        sliced(graphics, DARK_SQUARE, x, y, width, height, 3, alpha);
    }

    public static void darkBubble(GuiGraphicsExtractor graphics, int x, int y,
                                  int width, int height, int alpha) {
        sliced(graphics, DARK_BUBBLE, x, y, width, height, 3, alpha);
    }

    public static void darkVerticalRule(GuiGraphicsExtractor graphics, int x, int y,
                                        int width, int height, int alpha) {
        region(graphics, DARK_VERTICAL_RULE, x, y, width, height, alpha);
    }

    public static void darkTrack(GuiGraphicsExtractor graphics, int x, int y,
                                 int width, int height, int alpha) {
        region(graphics, DARK_TRACK, x, y, width, height, alpha);
    }

    public static void darkKnob(GuiGraphicsExtractor graphics, int x, int y,
                                int width, int height, int alpha) {
        region(graphics, DARK_KNOB, x, y, width, height, alpha);
    }

    private static void sliced(GuiGraphicsExtractor graphics, Region source,
                               int x, int y, int width, int height, int border, int alpha) {
        if (width <= 0 || height <= 0) return;
        int left = Math.min(border, width / 2);
        int right = Math.min(border, width - left);
        int top = Math.min(border, height / 2);
        int bottom = Math.min(border, height - top);
        int centerWidth = width - left - right;
        int centerHeight = height - top - bottom;
        int sourceCenterWidth = source.width - border * 2;
        int sourceCenterHeight = source.height - border * 2;

        region(graphics, source.crop(0, 0, border, border), x, y, left, top, alpha);
        region(graphics, source.crop(border, 0, sourceCenterWidth, border),
                x + left, y, centerWidth, top, alpha);
        region(graphics, source.crop(source.width - border, 0, border, border),
                x + width - right, y, right, top, alpha);
        region(graphics, source.crop(0, border, border, sourceCenterHeight),
                x, y + top, left, centerHeight, alpha);
        region(graphics, source.crop(border, border, sourceCenterWidth, sourceCenterHeight),
                x + left, y + top, centerWidth, centerHeight, alpha);
        region(graphics, source.crop(source.width - border, border, border, sourceCenterHeight),
                x + width - right, y + top, right, centerHeight, alpha);
        region(graphics, source.crop(0, source.height - border, border, border),
                x, y + height - bottom, left, bottom, alpha);
        region(graphics, source.crop(border, source.height - border, sourceCenterWidth, border),
                x + left, y + height - bottom, centerWidth, bottom, alpha);
        region(graphics, source.crop(source.width - border, source.height - border, border, border),
                x + width - right, y + height - bottom, right, bottom, alpha);
    }

    private static void region(GuiGraphicsExtractor graphics, Region source,
                               int x, int y, int width, int height, int alpha) {
        if (width <= 0 || height <= 0 || source.width <= 0 || source.height <= 0) return;
        int color = Mth.clamp(alpha, 0, 255) << 24 | 0x00FFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y,
                source.x, source.y, width, height, source.width, source.height,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    private record Region(int x, int y, int width, int height) {
        Region crop(int offsetX, int offsetY, int cropWidth, int cropHeight) {
            return new Region(x + offsetX, y + offsetY, cropWidth, cropHeight);
        }
    }
}
