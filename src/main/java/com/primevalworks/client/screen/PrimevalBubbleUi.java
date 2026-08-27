package com.primevalworks.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class PrimevalBubbleUi {
    private static final Identifier SPACE = Identifier.fromNamespaceAndPath(
            "primevalworks", "textures/gui/space.png");

    private PrimevalBubbleUi() {}

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (width < 4 || height < 4) return;
        int border = 2;
        int middleWidth = width - border * 2;
        int middleHeight = height - border * 2;

        blit(graphics, x, y, border, border, 0, 0, 2, 2);
        blit(graphics, x + border, y, middleWidth, border, 2, 0, 82, 2);
        blit(graphics, x + width - border, y, border, border, 84, 0, 2, 2);
        blit(graphics, x, y + border, border, middleHeight, 0, 2, 2, 10);
        blit(graphics, x + border, y + border, middleWidth, middleHeight, 2, 2, 82, 10);
        blit(graphics, x + width - border, y + border, border, middleHeight, 84, 2, 2, 10);
        blit(graphics, x, y + height - border, border, border, 0, 12, 2, 2);
        blit(graphics, x + border, y + height - border, middleWidth, border, 2, 12, 82, 2);
        blit(graphics, x + width - border, y + height - border, border, border, 84, 12, 2, 2);
    }

    public static void drawDark(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        draw(graphics, x, y, width, height);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xC718161B);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0x18FFFFFF);
    }

    public static void drawDarkControl(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int accent,
            boolean selected,
            boolean hovered
    ) {
        drawDark(graphics, x, y, width, height);
        int alpha = selected ? 66 : hovered ? 38 : 16;
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2,
                (alpha << 24) | (accent & 0x00FFFFFF));
        graphics.fill(x + 3, y + 3, x + 6, y + height - 3,
                (selected ? 0xFF000000 : 0xA0000000) | (accent & 0x00FFFFFF));
        if (hovered) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0x16FFFFFF);
        }
    }

    public static float spring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) return 1.0F;
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
    }

    private static void blit(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        if (width <= 0 || height <= 0) return;
        graphics.blit(
                SPACE,
                x,
                y,
                x + width,
                y + height,
                sourceX / 86.0F,
                (sourceX + sourceWidth) / 86.0F,
                sourceY / 14.0F,
                (sourceY + sourceHeight) / 14.0F
        );
    }
}
