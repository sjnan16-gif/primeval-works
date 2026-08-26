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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

public final class TurbineBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final float BASIC_WIND_OUTPUT_MULTIPLIER = 0.6F;
    public static final float UPGRADED_WIND_OUTPUT_MULTIPLIER = 1.0F;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int generationPulseCount;
    private boolean workerActive;
    private long workerActiveUntilGameTime = Long.MIN_VALUE;

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
        if (TurbineBlock.isWindTurbine(getBlockState())) {
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

    public boolean markWorkerActive() {
        if (level == null || level.isClientSide() || !hasValidEnvironment()) return false;
        workerActiveUntilGameTime = level.getGameTime() + 3L;
        setWorkerActive(true);
        return true;
    }

    public boolean isWorkerActive() {
        return workerActive;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TurbineBlockEntity turbine) {
        if (turbine.workerActive && level.getGameTime() > turbine.workerActiveUntilGameTime) {
            turbine.setWorkerActive(false);
        }
    }

    private void setWorkerActive(boolean active) {
        if (workerActive == active) return;
        workerActive = active;
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public float generationMultiplier() {
        if (getBlockState().is(ModBlocks.WATER_TURBINE.get())) {
            return (float)PrimevalTuning.server().waterTurbineOutput();
        }
        return TurbineBlock.isUpgradedWindTurbine(getBlockState())
                ? UPGRADED_WIND_OUTPUT_MULTIPLIER : BASIC_WIND_OUTPUT_MULTIPLIER;
    }

    public int getGenerationPulseCount() {
        return generationPulseCount;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TurbineBlockEntity>("Rotor", 8,
                state -> workerActive && hasValidEnvironment()
                        ? state.setAndContinue(SPIN) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workerActive = input.getBooleanOr("WorkerActive", false);
        workerActiveUntilGameTime = Long.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("WorkerActive", workerActive);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
