package com.primevalworks.world.block.entity;

import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.inventory.AutomationConfigurableContainer;
import com.primevalworks.world.inventory.ProcessorMenu;
import com.primevalworks.world.block.ProcessorBlock;
import com.primevalworks.world.processor.ProcessorRecipe;
import com.primevalworks.world.processor.ProcessorRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ProcessorBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, AutomationConfigurableContainer, ActiveEnergyConsumer {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int CATALYST_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    private static final int[] INSERT_SLOTS = {INPUT_SLOT, FUEL_SLOT, CATALYST_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private static final Component DEFAULT_NAME = Component.translatable("container.primevalworks.processor");

    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration;
    private int processProgress;
    private int processDuration;
    private String activeRecipeId = "";
    private int insertMask = 0b0111;
    private int extractMask = 0b1000;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnDuration;
                case 2 -> processProgress;
                case 3 -> processDuration;
                case 4 -> insertMask;
                case 5 -> extractMask;
                case 6 -> level != null && BaseEnergyRules.isPowered(level, worldPosition) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnDuration = value;
                case 2 -> processProgress = value;
                case 3 -> processDuration = value;
                case 4 -> insertMask = value & 0b1111;
                case 5 -> extractMask = value & 0b1111;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public ProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProcessorBlockEntity processor) {
        boolean changed = false;
        ProcessorRecipe recipe = ProcessorRecipes.find(
                processor.items.get(INPUT_SLOT), processor.items.get(CATALYST_SLOT)
        ).orElse(null);
        boolean powered = BaseEnergyRules.isPowered(level, pos);
        boolean outputHasRoom = recipe != null && processor.canAccept(recipe.outputStack());
        boolean processing = false;

        if (recipe == null) {
            if (processor.processProgress != 0 || processor.processDuration != 0
                    || !processor.activeRecipeId.isEmpty()) {
                processor.processProgress = 0;
                processor.processDuration = 0;
                processor.activeRecipeId = "";
                changed = true;
            }
        } else {
            String recipeId = recipe.id().toString();
            if (!processor.activeRecipeId.equals(recipeId)) {
                processor.activeRecipeId = recipeId;
                processor.processProgress = 0;
                changed = true;
            }
            int configuredDuration = configuredDuration(recipe);
            if (processor.processDuration != configuredDuration) {
                processor.processDuration = configuredDuration;
                processor.processProgress = Math.min(processor.processProgress, processor.processDuration);
                changed = true;
            }

            if (powered && outputHasRoom) {
                if (processor.burnTime <= 0 && processor.consumeFuel(level)) {
                    changed = true;
                }
                if (processor.burnTime > 0) {
                    processor.burnTime--;
                    processing = true;
                    changed = true;
                }
            }

            if (processing) {
                processor.processProgress++;
                if (processor.processProgress >= processor.processDuration) {
                    processor.finish(recipe);
                }
            }
        }

        ProcessorBlock.setProcessing(level, pos, state, processing);

        if (changed) {
            processor.setChanged();
        }
    }

    public boolean addWorkerProgress(int ticks) {
        if (level == null || ticks <= 0 || burnTime <= 0
                || !BaseEnergyRules.isPowered(level, worldPosition)) return false;
        ProcessorRecipe recipe = ProcessorRecipes.find(items.get(INPUT_SLOT), items.get(CATALYST_SLOT)).orElse(null);
        if (recipe == null || !canAccept(recipe.outputStack())) return false;
        String recipeId = recipe.id().toString();
        if (!activeRecipeId.equals(recipeId)) {
            activeRecipeId = recipeId;
            processProgress = 0;
        }
        processDuration = configuredDuration(recipe);
        processProgress = Math.min(processDuration, processProgress + ticks);
        if (processProgress >= processDuration) finish(recipe);
        setChanged();
        return true;
    }

    public boolean canBeTended() {
        if (level == null || !BaseEnergyRules.isPowered(level, worldPosition)) return false;
        ProcessorRecipe recipe = ProcessorRecipes.find(items.get(INPUT_SLOT), items.get(CATALYST_SLOT)).orElse(null);
        return recipe != null && canAccept(recipe.outputStack())
                && (burnTime > 0 || level.fuelValues().isFuel(items.get(FUEL_SLOT)));
    }

    @Override
    public boolean requestsBaseEnergy(Level level) {
        ProcessorRecipe recipe = ProcessorRecipes.find(items.get(INPUT_SLOT), items.get(CATALYST_SLOT)).orElse(null);
        return recipe != null && canAccept(recipe.outputStack())
                && (burnTime > 0 || level.fuelValues().isFuel(items.get(FUEL_SLOT)));
    }

    private boolean consumeFuel(Level level) {
        ItemStack fuel = items.get(FUEL_SLOT);
        int duration = level.fuelValues().burnDuration(fuel);
        if (duration <= 0) return false;
        burnTime = duration;
        burnDuration = duration;
        var remainderTemplate = fuel.getCraftingRemainder();
        ItemStack remainder = remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create();
        fuel.shrink(1);
        if (fuel.isEmpty()) items.set(FUEL_SLOT, remainder);
        return true;
    }

    private static int configuredDuration(ProcessorRecipe recipe) {
        return Math.max(1, (int)Math.ceil(recipe.processTicks() / PrimevalTuning.server().processorSpeed()));
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(output, result)) return false;
        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize(output));
    }

    private void finish(ProcessorRecipe recipe) {
        if (!recipe.matches(items.get(INPUT_SLOT), items.get(CATALYST_SLOT)) || !canAccept(recipe.outputStack())) {
            processProgress = 0;
            return;
        }
        items.get(INPUT_SLOT).shrink(1);
        items.get(CATALYST_SLOT).shrink(1);
        ItemStack result = recipe.outputStack();
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) items.set(OUTPUT_SLOT, result);
        else output.grow(result.getCount());
        processProgress = 0;
        processDuration = 0;
        activeRecipeId = "";
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnDuration", burnDuration);
        output.putInt("ProcessProgress", processProgress);
        output.putInt("ProcessDuration", processDuration);
        output.putString("ActiveRecipe", activeRecipeId);
        output.putInt("AutomationInsertMask", insertMask);
        output.putInt("AutomationExtractMask", extractMask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        burnTime = input.getIntOr("BurnTime", 0);
        burnDuration = input.getIntOr("BurnDuration", 0);
        processProgress = input.getIntOr("ProcessProgress", 0);
        processDuration = input.getIntOr("ProcessDuration", 0);
        activeRecipeId = input.getStringOr("ActiveRecipe", "");
        insertMask = input.getIntOr("AutomationInsertMask", 0b0111) & 0b1111;
        extractMask = input.getIntOr("AutomationExtractMask", 0b1000) & 0b1111;
    }

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> ProcessorRecipes.isInput(stack);
            case FUEL_SLOT -> level != null && level.fuelValues().isFuel(stack);
            case CATALYST_SLOT -> ProcessorRecipes.isCatalyst(stack);
            default -> false;
        };
    }

    @Override
    public boolean canTakeItem(Container destination, int slot, ItemStack stack) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return allowsAutomationInsert(slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT && allowsAutomationExtract(slot);
    }

    @Override
    public boolean allowsAutomationInsert(int slot) {
        return slot >= 0 && slot < getContainerSize() && (insertMask & (1 << slot)) != 0;
    }

    @Override
    public boolean allowsAutomationExtract(int slot) {
        return slot >= 0 && slot < getContainerSize() && (extractMask & (1 << slot)) != 0;
    }

    @Override
    public void toggleAutomationInsert(int slot) {
        if (slot >= INPUT_SLOT && slot <= CATALYST_SLOT) {
            insertMask ^= 1 << slot;
            setChanged();
        }
    }

    @Override
    public void toggleAutomationExtract(int slot) {
        if (slot == OUTPUT_SLOT) {
            extractMask ^= 1 << slot;
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ProcessorMenu(containerId, inventory, this, data);
    }
}
