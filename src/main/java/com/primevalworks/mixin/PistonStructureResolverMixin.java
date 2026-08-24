package com.primevalworks.mixin;

import com.primevalworks.world.block.HeavyPistonRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonStructureResolver.class)
abstract class PistonStructureResolverMixin {
    @Shadow
    private Level level;

    @Shadow
    private BlockPos pistonPos;

    @Redirect(
            method = {"resolve", "addBlockLine"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;isPushable(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;ZLnet/minecraft/core/Direction;)Z"
            )
    )
    private boolean primevalworks$allowHeavyBlocks(
            BlockState state,
            Level level,
            BlockPos pos,
            Direction direction,
            boolean allowDestroyable,
            Direction connectionDirection
    ) {
        if (HeavyPistonRules.isReinforcedPistonAt(this.level, pistonPos)
                && HeavyPistonRules.canMoveObsidian(state, level, pos, direction)) {
            return true;
        }
        return PistonBaseBlock.isPushable(
                state, level, pos, direction, allowDestroyable, connectionDirection
        );
    }
}
