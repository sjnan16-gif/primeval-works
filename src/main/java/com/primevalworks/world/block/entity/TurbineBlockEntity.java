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
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.work.WaterTurbineCouplingRules;
import com.primevalworks.world.work.WorkSpecialtyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TurbineBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final float BASIC_WIND_OUTPUT_MULTIPLIER = 0.6F;
    public static final float UPGRADED_WIND_OUTPUT_MULTIPLIER = 1.0F;
    public static final float PASSIVE_WATER_OUTPUT_FACTOR = 0.20F;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int generationPulseCount;
    private boolean workerActive;
    private boolean passiveActive;
    private long directWorkerActiveUntilGameTime = Long.MIN_VALUE;
    private long coupledWorkerActiveUntilGameTime = Long.MIN_VALUE;

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
        directWorkerActiveUntilGameTime = level.getGameTime() + 3L;
        setWorkerActive(true);
        return true;
    }

    public boolean markCoupledWorkerActive() {
        if (level == null || level.isClientSide() || !hasValidEnvironment()) return false;
        coupledWorkerActiveUntilGameTime = level.getGameTime() + 3L;
        setWorkerActive(true);
        return true;
    }

    public boolean hasDirectWorkerHeartbeat() {
        return level != null && level.getGameTime() <= directWorkerActiveUntilGameTime;
    }

    public List<CoupledTurbine> coupledWaterTurbines(CommandTableBlockEntity table) {
        if (!(level instanceof ServerLevel serverLevel)
                || table == null
                || !getBlockState().is(ModBlocks.WATER_TURBINE.get())
                || !hasDirectWorkerHeartbeat()) {
            return List.of();
        }
        double baseRadiusSquared = (double)table.baseRadius() * table.baseRadius();
        List<TurbineBlockEntity> candidates = new ArrayList<>();
        BlockPos min = worldPosition.offset(-WaterTurbineCouplingRules.RANGE,
                -WaterTurbineCouplingRules.RANGE, -WaterTurbineCouplingRules.RANGE);
        BlockPos max = worldPosition.offset(WaterTurbineCouplingRules.RANGE,
                WaterTurbineCouplingRules.RANGE, WaterTurbineCouplingRules.RANGE);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            if (cursor.equals(worldPosition)
                    || cursor.distSqr(worldPosition) > WaterTurbineCouplingRules.RANGE
                    * (double)WaterTurbineCouplingRules.RANGE
                    || cursor.distSqr(table.getBlockPos()) > baseRadiusSquared
                    || !serverLevel.isLoaded(cursor)
                    || !(serverLevel.getBlockEntity(cursor) instanceof TurbineBlockEntity candidate)
                    || !candidate.getBlockState().is(ModBlocks.WATER_TURBINE.get())
                    || !candidate.hasValidEnvironment()
                    || candidate.hasDirectWorkerHeartbeat()
                    || !worldPosition.equals(candidate.couplingLeader())) {
                continue;
            }
            candidates.add(candidate);
        }
        candidates.sort(Comparator
                .comparingDouble((TurbineBlockEntity candidate) -> candidate.worldPosition.distSqr(worldPosition))
                .thenComparingLong(candidate -> candidate.worldPosition.asLong()));
        List<CoupledTurbine> linked = new ArrayList<>(WaterTurbineCouplingRules.MAX_FOLLOWERS);
        for (int index = 0; index < Math.min(candidates.size(), WaterTurbineCouplingRules.MAX_FOLLOWERS); index++) {
            TurbineBlockEntity candidate = candidates.get(index);
            float multiplier = WaterTurbineCouplingRules.outputMultiplier(index);
            if (candidate.markCoupledWorkerActive()) linked.add(new CoupledTurbine(candidate, multiplier));
        }
        return List.copyOf(linked);
    }

    private @Nullable BlockPos couplingLeader() {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        BlockPos leader = null;
        double leaderDistance = Double.MAX_VALUE;
        BlockPos min = worldPosition.offset(-WaterTurbineCouplingRules.RANGE,
                -WaterTurbineCouplingRules.RANGE, -WaterTurbineCouplingRules.RANGE);
        BlockPos max = worldPosition.offset(WaterTurbineCouplingRules.RANGE,
                WaterTurbineCouplingRules.RANGE, WaterTurbineCouplingRules.RANGE);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            double distance = cursor.distSqr(worldPosition);
            if (distance > WaterTurbineCouplingRules.RANGE * (double)WaterTurbineCouplingRules.RANGE
                    || !serverLevel.isLoaded(cursor)
                    || !(serverLevel.getBlockEntity(cursor) instanceof TurbineBlockEntity candidate)
                    || !candidate.getBlockState().is(ModBlocks.WATER_TURBINE.get())
                    || !candidate.hasDirectWorkerHeartbeat()) {
                continue;
            }
            if (distance < leaderDistance
                    || distance == leaderDistance && (leader == null || cursor.asLong() < leader.asLong())) {
                leader = cursor.immutable();
                leaderDistance = distance;
            }
        }
        return leader;
    }

    public boolean isWorkerActive() {
        return workerActive;
    }

    public boolean isPassiveActive() {
        return passiveActive;
    }

    public float passiveWaterEnergyPerSecond() {
        if (!getBlockState().is(ModBlocks.WATER_TURBINE.get())) return 0.0F;
        return WorkSpecialtyRules.energyPerSecond(4, 1)
                * generationMultiplier() * PASSIVE_WATER_OUTPUT_FACTOR;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TurbineBlockEntity turbine) {
        boolean heartbeat = level.getGameTime() <= turbine.directWorkerActiveUntilGameTime
                || level.getGameTime() <= turbine.coupledWorkerActiveUntilGameTime;
        boolean valid = turbine.hasValidEnvironment();
        boolean passive = false;
        if (valid && state.is(ModBlocks.WATER_TURBINE.get()) && level instanceof ServerLevel serverLevel) {
            CommandTableBlockEntity table = BaseEnergyRules.nearestLoadedTable(serverLevel, pos);
            if (table != null) {
                table.receiveGeneratedEnergy(turbine.passiveWaterEnergyPerSecond() / 20.0F);
                passive = true;
            }
        }
        turbine.setGenerationState(heartbeat && valid, passive);
    }

    private void setWorkerActive(boolean active) {
        setGenerationState(active, passiveActive);
    }

    private void setGenerationState(boolean worked, boolean passive) {
        if (workerActive == worked && passiveActive == passive) return;
        workerActive = worked;
        passiveActive = passive;
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
                state -> {
                    if ((!workerActive && !passiveActive) || !hasValidEnvironment()) return PlayState.STOP;
                    state.setControllerSpeed(workerActive ? 1.0F : PASSIVE_WATER_OUTPUT_FACTOR);
                    return state.setAndContinue(SPIN);
                }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workerActive = input.getBooleanOr("WorkerActive", false);
        passiveActive = input.getBooleanOr("PassiveActive", false);
        directWorkerActiveUntilGameTime = Long.MIN_VALUE;
        coupledWorkerActiveUntilGameTime = Long.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("WorkerActive", workerActive);
        tag.putBoolean("PassiveActive", passiveActive);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record CoupledTurbine(TurbineBlockEntity turbine, float outputMultiplier) {
    }

}
