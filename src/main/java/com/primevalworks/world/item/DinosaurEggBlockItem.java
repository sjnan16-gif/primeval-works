package com.primevalworks.world.item;

import com.primevalworks.world.egg.DinosaurEggGenome;
import com.primevalworks.world.egg.DinosaurHatching;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class DinosaurEggBlockItem extends BlockItem {
    public DinosaurEggBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        DinosaurEggGenome.repairGeneratedName(stack);
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        DinosaurEggGenome genome = DinosaurEggGenome.read(context.getItemInHand()).orElse(null);
        if (genome == null) return super.useOn(context);
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;

        DinosaurHatching.Genome hatchGenome = genome.origin() == DinosaurEggGenome.Origin.INCUBATED
                ? DinosaurHatching.Genome.incubated(
                        genome.species(), genome.quality(), genome.mutationMask(), genome.hueVariant())
                : DinosaurHatching.Genome.bred(
                        genome.species(), genome.quality(), genome.mutationMask(), genome.hueVariant());
        DinosaurHatching.HatchResult result = DinosaurHatching.hatchForPlayer(player, hatchGenome);
        player.sendOverlayMessage(result.message());
        if (!result.success()) return InteractionResult.FAIL;
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        DinosaurEggGenome.read(stack).ifPresent(genome -> {
            tooltip.accept(Component.translatable(
                    "tooltip.primevalworks.genetic_egg.species",
                    Component.translatable("entity.primevalworks." + genome.species().registryName())
            ).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable(
                    "tooltip.primevalworks.genetic_egg.quality", genome.quality()
            ).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable(
                    "tooltip.primevalworks.genetic_egg.mutations",
                    mutationText(genome.mutationMask())
            ).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable(
                    "tooltip.primevalworks.genetic_egg.use"
            ).withStyle(ChatFormatting.DARK_GREEN));
        });
    }

    private static Component mutationText(int mask) {
        boolean huge = (mask & com.primevalworks.world.entity.FieldDodoEntity.MUTATION_HUGE) != 0;
        boolean albino = (mask & com.primevalworks.world.entity.FieldDodoEntity.MUTATION_ALBINO) != 0;
        if (huge && albino) return Component.translatable("mutation.primevalworks.both");
        if (huge) return Component.translatable("mutation.primevalworks.huge");
        if (albino) return Component.translatable("mutation.primevalworks.albino");
        return Component.translatable("mutation.primevalworks.none");
    }
}
