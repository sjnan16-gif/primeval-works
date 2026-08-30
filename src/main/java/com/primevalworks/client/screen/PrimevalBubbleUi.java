package com.primevalworks.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PrimevalBubbleUi {
    private PrimevalBubbleUi() {}

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        PrimevalUiCrop.paperBubble(graphics, x, y, width, height);
    }

    public static float spring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) return 1.0F;
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
    }

}
