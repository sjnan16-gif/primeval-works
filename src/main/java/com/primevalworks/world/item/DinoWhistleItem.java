package com.primevalworks.world.item;

import com.primevalworks.network.payload.OpenDinoWhistlePayload;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public final class DinoWhistleItem extends Item {
    public static final int OPEN_TICKS = 18;

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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return OPEN_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && player.connection.hasChannel(OpenDinoWhistlePayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new OpenDinoWhistlePayload());
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        DinoWhistleSettings settings = DinoWhistleSettings.read(stack);
        tooltip.accept(Component.literal(settings.mode().title()).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.literal(settings.mode().description()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal(settings.pattern().title() + " / "
                + (settings.continuous() ? "Continuous" : "One time") + " / " + settings.range() + " blocks")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.literal("Hold use to configure. Mark a block with attack.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
