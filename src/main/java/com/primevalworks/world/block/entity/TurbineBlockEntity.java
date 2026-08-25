package com.primevalworks.world.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.world.block.TurbineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TurbineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int generationPulseCount;

    public TurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE.get(), pos, state);
    }

    public boolean hasValidEnvironment() {
        if (level == null) {
            return false;
        }
        if (getBlockState().is(ModBlocks.WATER_TURBINE.get())) {
            for (int localX = -1; localX <= 1; localX++) {
                BlockPos submergedCell = localX == 0
                        ? worldPosition
                        : TurbineBlock.partPos(worldPosition, getBlockState(), new TurbineBlock.PartOffset(localX, 0));
                if (!level.getFluidState(submergedCell).is(FluidTags.WATER)) {
                    return false;
                }
            }
            return true;
        }
        if (getBlockState().is(ModBlocks.WIND_TURBINE.get())) {
            // The rotor occupies the first three blocks above its master. Checking those
            // cells for air made every assembled wind turbine invalidate itself.
            return level.getBlockState(worldPosition.above(4)).isAir();
        }
        return false;
    }

    public boolean recordGenerationPulse() {
        if (!hasValidEnvironment()) return false;
        generationPulseCount++;
        return true;
    }

    public float generationMultiplier() {
        return getBlockState().is(ModBlocks.WATER_TURBINE.get())
                ? (float)PrimevalTuning.server().waterTurbineOutput() : 1.0F;
    }

    public int getGenerationPulseCount() {
        return generationPulseCount;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TurbineBlockEntity>("Rotor", 8,
                state -> hasValidEnvironment() ? state.setAndContinue(SPIN) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

}
