package com.primevalworks.world.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.DinosaurEggBlock;
import com.primevalworks.world.egg.DinosaurEggGenome;
import com.primevalworks.world.egg.DinosaurEggSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class DinosaurEggBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation WOBBLE = RawAnimation.begin()
            .thenLoop("animation.dinosaur_egg.wobble");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack geneticEgg = ItemStack.EMPTY;

    public DinosaurEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DINOSAUR_EGG.get(), pos, state);
    }

    public ItemStack geneticEgg() {
        return geneticEgg.copy();
    }

    public boolean setGeneticEgg(ItemStack stack) {
        DinosaurEggGenome genome = DinosaurEggGenome.read(stack).orElse(null);
        DinosaurEggSize itemSize = DinosaurEggSize.fromItem(stack).orElse(null);
        DinosaurEggSize blockSize = getBlockState().getBlock() instanceof DinosaurEggBlock eggBlock
                ? eggBlock.size() : null;
        if (genome == null || itemSize != blockSize) return false;
        geneticEgg = stack.copyWithCount(1);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!geneticEgg.isEmpty()) output.store("GeneticEgg", ItemStack.CODEC, geneticEgg);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ItemStack loaded = input.read("GeneticEgg", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        geneticEgg = ItemStack.EMPTY;
        setGeneticEgg(loaded);
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
