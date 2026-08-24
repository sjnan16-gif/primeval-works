package com.primevalworks.mixin;

import com.primevalworks.world.block.HeavyPistonRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonBaseBlock.class)
abstract class PistonBaseBlockMixin {
    @Redirect(
            method = "triggerEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;isPushable(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;ZLnet/minecraft/core/Direction;)Z"
            )
    )
    private boolean primevalworks$allowStickyHeavyPull(
            BlockState state,
            Level level,
            BlockPos pos,
            Direction direction,
            boolean allowDestroyable,
            Direction connectionDirection
    ) {
        if (HeavyPistonRules.isStickyReinforced((PistonBaseBlock)(Object)this)
                && HeavyPistonRules.canMoveObsidian(state, level, pos, direction)) {
            return true;
        }
        return PistonBaseBlock.isPushable(
                state, level, pos, direction, allowDestroyable, connectionDirection
        );
    }

    @Redirect(
            method = "triggerEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;"
            )
    )
    private PushReaction primevalworks$normalizeHeavyPullReaction(BlockState state) {
        if (HeavyPistonRules.isStickyReinforced((PistonBaseBlock)(Object)this)
                && (state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN))) {
            return PushReaction.NORMAL;
        }
        return state.getPistonPushReaction();
    }
}
