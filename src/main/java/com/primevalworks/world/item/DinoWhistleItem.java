package com.primevalworks.world.item;

import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class DinoWhistleItem extends Item {
    public DinoWhistleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack findHeld(Player player) {
        if (player.getMainHandItem().is(com.primevalworks.registry.ModItems.DINO_WHISTLE.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(com.primevalworks.registry.ModItems.DINO_WHISTLE.get())) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findInventoryWhistle(Player player, int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) return ItemStack.EMPTY;
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        return stack.is(com.primevalworks.registry.ModItems.DINO_WHISTLE.get()) ? stack : ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        DinoWhistleSettings settings = DinoWhistleSettings.read(stack);
        tooltip.accept(Component.literal("Directs one of your following dinosaurs.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("────────────").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Order  ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(settings.mode().title()).withStyle(ChatFormatting.GOLD)));
        tooltip.accept(Component.literal(settings.mode().targetDescription(settings.pattern()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("Range  ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(settings.range() + " blocks").withStyle(ChatFormatting.WHITE)));
        if (settings.filtersItems()) {
            tooltip.accept(Component.literal("Filter  ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(settings.itemFilter()).withStyle(ChatFormatting.AQUA)));
        }
        tooltip.accept(Component.literal("────────────").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Right-click to configure.")
                .withStyle(ChatFormatting.YELLOW));
    }
}
