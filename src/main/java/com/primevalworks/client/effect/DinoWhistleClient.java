package com.primevalworks.client.effect;

import com.primevalworks.client.screen.DinoWhistleScreen;
import com.primevalworks.network.payload.RequestWhistleFollowersPayload;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class DinoWhistleClient {
    private static BlockPos areaFirst;
    private static DinoWhistleSettings.FieldMode areaMode;

    private DinoWhistleClient() {}

    public static void handleAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack whistle = DinoWhistleItem.findHeld(minecraft.player);
        if (whistle.isEmpty()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        if (!settings.mode().requiresMark()) {
            areaFirst = null;
            areaMode = null;
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            minecraft.player.sendOverlayMessage(Component.literal(settings.mode().markHint(settings.pattern())));
            return;
        }
        BlockPos selected = hit.getBlockPos().immutable();
        boolean areaOrder = settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                && settings.pattern() == DinoWhistleSettings.Pattern.AREA;
        boolean choosingFirst = !areaOrder || areaFirst == null;
        if (choosingFirst && !DinoFieldWorkRules.validTarget(minecraft.level, selected, settings.mode(), 4)) {
            minecraft.player.sendOverlayMessage(Component.literal(settings.mode().markHint(settings.pattern())));
            return;
        }
        if (areaOrder) {
            if (areaFirst == null || areaMode != settings.mode()) {
                areaFirst = selected;
                areaMode = settings.mode();
                minecraft.player.sendOverlayMessage(Component.literal(
                        "Block type saved. Mark the opposite corner."));
                return;
            }
            BlockPos first = areaFirst;
            areaFirst = null;
            areaMode = null;
            ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(first, selected, true));
            return;
        }
        areaFirst = null;
        areaMode = null;
        ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(selected, selected, false));
    }

    public static void handleInventoryRightClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1
                || !(event.getScreen() instanceof AbstractContainerScreen<?> container)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Slot hovered = container.getHoveredSlot();
        if (hovered == null || !hovered.getItem().is(ModItems.DINO_WHISTLE.get())) return;
        int inventorySlot = resolveInventorySlot(minecraft, hovered);
        if (inventorySlot < 0) return;
        event.setCanceled(true);
        DinoWhistleScreen.open(hovered.getItem(), inventorySlot);
    }

    public static void handleHeldRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        InteractionHand hand = event.getHand();
        ItemStack whistle = minecraft.player.getItemInHand(hand);
        if (!whistle.is(ModItems.DINO_WHISTLE.get())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        int inventorySlot = hand == InteractionHand.MAIN_HAND
                ? minecraft.player.getInventory().getSelectedSlot()
                : minecraft.player.getInventory().getContainerSize() - 1;
        DinoWhistleScreen.open(whistle, inventorySlot);
    }

    private static int resolveInventorySlot(Minecraft minecraft, Slot hovered) {
        if (minecraft.player == null) return -1;
        if (hovered.container == minecraft.player.getInventory()) return hovered.getContainerSlot();
        ItemStack hoveredStack = hovered.getItem();
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            if (minecraft.player.getInventory().getItem(index) == hoveredStack) return index;
        }
        int match = -1;
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            ItemStack candidate = minecraft.player.getInventory().getItem(index);
            if (!ItemStack.isSameItemSameComponents(candidate, hoveredStack)) continue;
            if (match >= 0) return -1;
            match = index;
        }
        return match;
    }
}
