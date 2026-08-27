package com.primevalworks.client.effect;

import com.primevalworks.network.payload.RequestWhistleFollowersPayload;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class DinoWhistleClient {
    private static BlockPos areaFirst;
    private static float visibility;
    private static long lastFrame;

    private DinoWhistleClient() {}

    public static void handleAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack whistle = DinoWhistleItem.findHeld(minecraft.player);
        if (whistle.isEmpty() || !(minecraft.hitResult instanceof BlockHitResult hit)) return;
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        event.setCanceled(true);
        event.setSwingHand(false);
        BlockPos selected = hit.getBlockPos().immutable();
        if (settings.pattern() == DinoWhistleSettings.Pattern.AREA) {
            if (areaFirst == null) {
                areaFirst = selected;
                minecraft.player.sendOverlayMessage(Component.literal("First corner marked. Attack the opposite corner."));
                return;
            }
            BlockPos first = areaFirst;
            areaFirst = null;
            ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(first, selected, true));
        } else {
            areaFirst = null;
            ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(selected, selected, false));
        }
    }

    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack whistle = minecraft.player == null ? ItemStack.EMPTY : DinoWhistleItem.findHeld(minecraft.player);
        boolean visible = !whistle.isEmpty() && minecraft.screen == null && !minecraft.options.hideGui;
        if (whistle.isEmpty()) areaFirst = null;
        long now = System.nanoTime();
        float delta = lastFrame == 0L ? 1.0F / 60.0F
                : Mth.clamp((now - lastFrame) / 1_000_000_000.0F, 0.001F, 0.05F);
        lastFrame = now;
        visibility = Mth.lerp(1.0F - (float)Math.exp(-(visible ? 12.0F : 8.0F) * delta),
                visibility, visible ? 1.0F : 0.0F);
        if (visibility < 0.015F) return;

        DinoWhistleSettings settings = whistle.isEmpty() ? DinoWhistleSettings.DEFAULT : DinoWhistleSettings.read(whistle);
        if (settings.pattern() != DinoWhistleSettings.Pattern.AREA) areaFirst = null;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int width = 152;
        int height = areaFirst == null ? 27 : 38;
        int x = graphics.guiWidth() - width - 9 + Math.round((1.0F - visibility) * 12.0F);
        int y = Math.max(8, graphics.guiHeight() / 2 - height / 2);
        int alpha = Mth.clamp(Math.round(visibility * 235.0F), 0, 255);
        graphics.fill(x + 4, y + 4, x + width + 4, y + height + 4, alpha / 3 << 24);
        graphics.fill(x, y, x + width, y + height, alpha << 24 | 0xC9A77F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, alpha << 24 | 0xDDBB91);
        graphics.outline(x, y, width, height, alpha << 24 | 0x5F4231);
        graphics.fill(x + 3, y + 3, x + 6, y + height - 3, alpha << 24 | 0xB85E35);
        drawFitText(graphics, Component.literal(settings.shortLabel()).withStyle(Style.EMPTY.withBold(true)),
                x + 10, y + 6, width - 18, alpha << 24 | 0x4D3931, 0.67F);
        drawFitText(graphics, Component.literal("ATTACK: MARK  /  HOLD USE: CONFIGURE"),
                x + 10, y + 16, width - 18, alpha << 24 | 0x645750, 0.54F);
        if (areaFirst != null) {
            drawText(graphics, Component.literal("CORNER A  " + areaFirst.getX() + "  "
                            + areaFirst.getY() + "  " + areaFirst.getZ()).withStyle(Style.EMPTY.withBold(true)),
                    x + 10, y + 27, alpha << 24 | 0x477895, 0.56F);
        }

        if (visible && minecraft.player.isUsingItem()
                && minecraft.player.getUseItem().is(com.primevalworks.registry.ModItems.DINO_WHISTLE.get())) {
            float progress = 1.0F - minecraft.player.getUseItemRemainingTicks() / (float)DinoWhistleItem.OPEN_TICKS;
            int fill = Math.round((width - 6) * Mth.clamp(progress, 0.0F, 1.0F));
            graphics.fill(x + 3, y + height - 3, x + 3 + fill, y + height - 1, alpha << 24 | 0xE2A23D);
        }
    }

    private static void drawText(GuiGraphicsExtractor graphics, Component value,
                                 float x, float y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(Minecraft.getInstance().font, value, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static void drawFitText(GuiGraphicsExtractor graphics, Component value,
                                    float x, float y, float maxWidth, int color, float requestedScale) {
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(value));
        drawText(graphics, value, x, y, color, Math.min(requestedScale, maxWidth / textWidth));
    }
}
