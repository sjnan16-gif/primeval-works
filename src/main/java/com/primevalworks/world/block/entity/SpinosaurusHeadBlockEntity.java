package com.primevalworks.world.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SpinosaurusHeadBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public SpinosaurusHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPINOSAURUS_HEAD.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
