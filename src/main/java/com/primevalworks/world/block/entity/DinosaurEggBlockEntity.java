package com.primevalworks.world.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DinosaurEggBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation WOBBLE = RawAnimation.begin()
            .thenLoop("animation.dinosaur_egg.wobble");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public DinosaurEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DINOSAUR_EGG.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("Egg", 12, state -> state.setAndContinue(WOBBLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
