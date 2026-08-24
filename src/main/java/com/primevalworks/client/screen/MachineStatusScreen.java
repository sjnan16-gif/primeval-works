package com.primevalworks.client.screen;

import com.primevalworks.PrimevalWorks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class MachineStatusScreen extends Screen {
    private static final Identifier PANEL = texture("ancient_companion_panel.png");
    private static final Identifier BUBBLE = texture("space.png");
    private static final Identifier SLOT = texture("ancient_slot.png");
    private static final int INK = 0xFF34282A;
    private static final int MUTED = 0xFF725D55;
    private static final int ACCENT = 0xFFC25D32;
    private final BlockPos pos;
    private final Descriptor descriptor;
    private int ticks;

    public MachineStatusScreen(BlockPos pos, Descriptor descriptor) {
        super(Component.literal(descriptor.title()));
        this.pos = pos.immutable();
        this.descriptor = descriptor;
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float progress = Mth.clamp((ticks + partialTick) / 12.0F, 0.0F, 1.0F);
        float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0D);
        int panelWidth = Math.min(356, width - 24);
        int panelHeight = Math.min(204, height - 24);
        int x = (width - panelWidth) / 2;
        int finalY = (height - panelHeight) / 2;
        int y = finalY + Math.round((1.0F - eased) * 34.0F);
        int alpha = Math.round(eased * 112.0F);
        graphics.fill(0, 0, width, height, alpha << 24);
        graphics.fill(x + 5, y + 6, x + panelWidth + 5, y + panelHeight + 6, 0x72000000);
        blit(graphics, PANEL, x, y, panelWidth, panelHeight);

        bubble(graphics, x + 18, y + 16, panelWidth - 36, 22);
        text(graphics, descriptor.title().toUpperCase(), x + 28, y + 23, ACCENT);
        rightText(graphics, descriptor.specialty().toUpperCase(), x + panelWidth - 28, y + 23, MUTED);

        bubble(graphics, x + 18, y + 47, panelWidth - 36, 39);
        text(graphics, descriptor.status(), x + 28, y + 55, INK);
        text(graphics, descriptor.detail(), x + 28, y + 69, MUTED);

        int slotY = y + 99;
        drawSlot(graphics, x + 30, slotY, descriptor.input(), "RESOURCE", mouseX, mouseY);
        drawSlot(graphics, x + 150, slotY, descriptor.output(), "EFFECT", mouseX, mouseY);
        bubble(graphics, x + 267, slotY, 65, 28);
        text(graphics, "NO SLOTS", x + 276, slotY + 5, INK);
        text(graphics, "STATUS ONLY", x + 273, slotY + 16, MUTED);

        bubble(graphics, x + 18, y + 145, panelWidth - 36, 39);
        text(graphics, "HOW IT IS CONTROLLED", x + 28, y + 153, INK);
        text(graphics, descriptor.assignment(), x + 28, y + 167, MUTED);
        text(graphics, "ESC closes", x + panelWidth - 82, y + panelHeight - 13, MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack stack,
            String label,
            int mouseX,
            int mouseY
    ) {
        blit(graphics, SLOT, x, y, 28, 28);
        graphics.item(stack, x + 6, y + 6);
        text(graphics, label, x + 34, y + 9, MUTED);
        if (mouseX >= x && mouseX < x + 70 && mouseY >= y && mouseY < y + 28) {
            graphics.setTooltipForNextFrame(Component.literal(descriptor.slotHelp(label)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void bubble(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        blit(graphics, BUBBLE, x, y, width, height);
    }

    private void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        graphics.blit(texture, x, y, x + width, y + height, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        graphics.text(font, Component.literal(value).withStyle(style -> style.withBold(true)), x, y, color, true);
    }

    private void rightText(GuiGraphicsExtractor graphics, String value, int right, int y, int color) {
        Component text = Component.literal(value).withStyle(style -> style.withBold(true));
        graphics.text(font, text, right - font.width(text), y, color, true);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/gui/" + name);
    }

    public record Descriptor(
            String title,
            String specialty,
            String status,
            String detail,
            String assignment,
            ItemStack input,
            ItemStack output
    ) {
        public String slotHelp(String label) {
            return switch (label) {
                case "RESOURCE" -> "The resource this machine checks. This is not an inventory slot.";
                default -> "The machine's automatic world effect. This is not an output slot.";
            };
        }
    }
}
