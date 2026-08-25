package com.primevalworks.world.block.entity;

import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.inventory.AncientFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class AncientFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements ActiveEnergyConsumer {
    public static final int INPUT_SLOT = 0;
    public static final int ENERGY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final float MINIMUM_ENERGY_PER_SECOND = 2.5F;
    public static final float DEFAULT_ENERGY_PER_SECOND = 3.0F;
    public static final float MAXIMUM_ENERGY_PER_SECOND = 10.5F;
    public static final float DEFAULT_THROTTLE = 0.20F;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};

    private float throttle = DEFAULT_THROTTLE;
    private float workAccumulator;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> dataAccess.get(DATA_COOKING_PROGRESS);
                case 1 -> dataAccess.get(DATA_COOKING_TOTAL_TIME);
                case 2 -> Math.round(throttle * 1000.0F);
                case 3 -> level != null && BaseEnergyRules.isPowered(level, worldPosition) ? 1 : 0;
                case 4 -> Math.round(energyPerSecond() * 100.0F);
                case 5 -> Math.round(speedMultiplier() * 100.0F);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 2) setThrottle(value / 1000.0F);
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public AncientFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_FURNACE.get(), pos, state, RecipeType.SMELTING);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  AncientFurnaceBlockEntity furnace) {
        furnace.releaseLegacyFuel(level);
        boolean powered = BaseEnergyRules.isPowered(level, pos);
        boolean hasInput = !furnace.getItem(INPUT_SLOT).isEmpty();
        furnace.setLit(level, pos, state, powered && hasInput);
        if (!powered || !hasInput) {
            furnace.dataAccess.set(DATA_LIT_TIME, 0);
            return;
        }

        int maximumSteps = Math.max(4, Mth.ceil(4.2F
                * (float)PrimevalTuning.server().ancientFurnaceSpeed()));
        furnace.workAccumulator = Math.min(maximumSteps, furnace.workAccumulator + furnace.speedMultiplier());
        int steps = Math.min(maximumSteps, Mth.floor(furnace.workAccumulator));
        furnace.workAccumulator -= steps;
        furnace.runCookingSteps(level, pos, steps);
    }

    public boolean addWorkerProgress(int ticks) {
        if (!(level instanceof ServerLevel serverLevel) || ticks <= 0
                || !BaseEnergyRules.isPowered(level, worldPosition) || getItem(INPUT_SLOT).isEmpty()) {
            return false;
        }
        runCookingSteps(serverLevel, worldPosition, Math.min(80, ticks));
        return true;
    }

    private void runCookingSteps(ServerLevel level, BlockPos pos, int steps) {
        for (int step = 0; step < steps; step++) {
            dataAccess.set(DATA_LIT_TIME, 2);
            dataAccess.set(DATA_LIT_DURATION, 2);
            AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), this);
        }
    }

    private void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        if (state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, lit), 3);
        }
    }

    private void releaseLegacyFuel(ServerLevel level) {
        ItemStack legacyFuel = getItem(ENERGY_SLOT);
        if (legacyFuel.isEmpty()) return;
        setItem(ENERGY_SLOT, ItemStack.EMPTY);
        ItemEntity dropped = new ItemEntity(level, worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.05D, worldPosition.getZ() + 0.5D, legacyFuel);
        dropped.setDefaultPickUpDelay();
        level.addFreshEntity(dropped);
    }

    public void setThrottle(float value) {
        float next = Mth.clamp(value, 0.0F, 1.0F);
        if (Math.abs(next - throttle) < 0.0005F) return;
        throttle = next;
        setChanged();
    }

    public float throttle() {
        return throttle;
    }

    public float energyPerSecond() {
        float configured = (float)PrimevalTuning.server().machineEnergyUse();
        if (throttle <= DEFAULT_THROTTLE) {
            return Mth.lerp(throttle / DEFAULT_THROTTLE,
                    MINIMUM_ENERGY_PER_SECOND, DEFAULT_ENERGY_PER_SECOND) * configured;
        }
        return Mth.lerp((throttle - DEFAULT_THROTTLE) / (1.0F - DEFAULT_THROTTLE),
                DEFAULT_ENERGY_PER_SECOND, MAXIMUM_ENERGY_PER_SECOND) * configured;
    }

    @Override
    public boolean requestsBaseEnergy(net.minecraft.world.level.Level level) {
        return !getItem(INPUT_SLOT).isEmpty();
    }

    public float speedMultiplier() {
        float configured = (float)PrimevalTuning.server().ancientFurnaceSpeed();
        if (throttle <= DEFAULT_THROTTLE) {
            return Mth.lerp(throttle / DEFAULT_THROTTLE, 0.75F, 1.0F) * configured;
        }
        return Mth.lerp((throttle - DEFAULT_THROTTLE) / (1.0F - DEFAULT_THROTTLE), 1.0F, 4.2F)
                * configured;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.primevalworks.ancient_furnace");
    }

    @Override
    protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AncientFurnaceMenu(containerId, inventory, this, menuData);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && super.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("EnergyThrottle", throttle);
        output.putFloat("WorkAccumulator", workAccumulator);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        throttle = Mth.clamp(input.getFloatOr("EnergyThrottle", DEFAULT_THROTTLE), 0.0F, 1.0F);
        workAccumulator = Mth.clamp(input.getFloatOr("WorkAccumulator", 0.0F), 0.0F, 4.0F);
    }
}
