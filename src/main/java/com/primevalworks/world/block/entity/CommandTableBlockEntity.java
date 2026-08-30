package com.primevalworks.world.block.entity;

import com.primevalworks.config.PrimevalTuning;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.base.BaseUpgrade;
import com.primevalworks.world.ownership.FollowerCapacityRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CommandTableBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final int SCHEMA_VERSION = 3;
    private static final int STARTING_INSIGHT = 7;
    private static final int BASE_RADIUS = 50;
    private static final float BASE_ENERGY_CAPACITY = 500.0F;
    private static final int MAX_ENERGY_CONSUMERS = 256;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID owner;
    private int insight = STARTING_INSIGHT;
    private final int[] upgradeLevels = new int[BaseUpgrade.values().length];
    private final Set<BlockPos> enabledEnergyConsumers = new LinkedHashSet<>();
    private float storedEnergy;
    private float generatedThisSecond;
    private float generationPerSecond;
    private float consumptionPerSecond;
    private boolean energyConsumersPowered;
    private int energyWindowTicks;

    public CommandTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMMAND_TABLE.get(), pos, state);
        upgradeLevels[BaseUpgrade.HEARTHSTONE.id()] = 1;
    }

    @Override
    public void onLoad() {
        if (level instanceof ServerLevel serverLevel) {
            CommandTableBlock.scheduleLegacyCleanup(serverLevel, worldPosition);
            BaseEnergyRules.registerLoadedTable(serverLevel, worldPosition);
            enabledEnergyConsumers.forEach(pos -> BaseEnergyRules.bindConsumer(
                    serverLevel, worldPosition, pos, true
            ));
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            enabledEnergyConsumers.forEach(pos -> BaseEnergyRules.bindConsumer(
                    level, worldPosition, pos, false
            ));
            BaseEnergyRules.unregisterLoadedTable(level, worldPosition);
        }
        super.setRemoved();
    }

    public boolean claim(UUID playerId) {
        if (owner != null && !owner.equals(playerId)) {
            return false;
        }
        owner = playerId;
        upgradeLevels[BaseUpgrade.HEARTHSTONE.id()] = 1;
        setChanged();
        return true;
    }

    public boolean isOwnedBy(UUID playerId) {
        return owner == null || owner.equals(playerId);
    }

    public int insight() {
        return insight;
    }

    public int level(BaseUpgrade upgrade) {
        return upgradeLevels[upgrade.id()];
    }

    public List<Integer> levels() {
        return Arrays.stream(upgradeLevels).boxed().toList();
    }

    public void addInsight(int amount) {
        if (amount <= 0) return;
        insight = Mth.clamp(insight + amount, 0, 999);
        setChanged();
    }

    public PurchaseResult purchase(ServerPlayer player, int upgradeId) {
        if (!isOwnedBy(player.getUUID())) {
            return PurchaseResult.failure(Component.literal("This Command Table answers to another keeper."));
        }
        BaseUpgrade upgrade = BaseUpgrade.byId(upgradeId).orElse(null);
        if (upgrade == null || upgrade == BaseUpgrade.HEARTHSTONE) {
            return PurchaseResult.failure(Component.literal("That branch cannot be changed."));
        }
        int current = level(upgrade);
        if (current >= upgrade.maxLevel()) {
            return PurchaseResult.failure(Component.literal(upgrade.title() + " is already complete."));
        }
        if (upgrade.prerequisiteId() >= 0) {
            BaseUpgrade prerequisite = BaseUpgrade.byId(upgrade.prerequisiteId()).orElseThrow();
            if (level(prerequisite) < upgrade.prerequisiteLevel()) {
                return PurchaseResult.failure(Component.literal(
                        "Raise " + prerequisite.title() + " to rank " + upgrade.prerequisiteLevel() + " first."
                ));
            }
        }
        List<BaseUpgrade.UpgradeCost> costs = upgrade.itemCostsForLevel(current);
        BaseUpgrade.UpgradeCost missing = costs.stream()
                .filter(cost -> countItem(player, cost) < cost.count())
                .findFirst()
                .orElse(null);
        if (missing != null && !player.getAbilities().instabuild) {
            return PurchaseResult.failure(Component.literal(
                    "This upgrade needs " + missing.count() + " "
                            + missing.stack().getHoverName().getString() + "."
            ));
        }
        if (!player.getAbilities().instabuild) {
            costs.forEach(cost -> consumeItem(player, cost));
        }
        upgradeLevels[upgrade.id()] = current + 1;
        setChanged();
        return PurchaseResult.success(Component.literal(
                upgrade.title() + " reached rank " + (current + 1) + "."
        ));
    }

    public int activeDinosaurCapacity() {
        return 7
                + level(BaseUpgrade.CREW_PERCHES) * 2
                + level(BaseUpgrade.PACK_HIERARCHY) * 2
                + level(BaseUpgrade.ANCIENT_BONDS) * 3;
    }

    public int followerCapacity() {
        PrimevalTuning.Server tuning = PrimevalTuning.server();
        return FollowerCapacityRules.capacity(
                tuning.startingFollowerSlots(),
                tuning.followerSlotsPerFieldCommandRank(),
                tuning.maximumFollowerSlots(),
                level(BaseUpgrade.FIELD_COMMAND));
    }

    private static int countItem(ServerPlayer player, BaseUpgrade.UpgradeCost cost) {
        int found = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(cost.item())) found += stack.getCount();
        }
        return found;
    }

    private static void consumeItem(ServerPlayer player, BaseUpgrade.UpgradeCost cost) {
        int remaining = cost.count();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (remaining <= 0) break;
            if (!stack.is(cost.item())) continue;
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
        player.getInventory().setChanged();
    }

    public int baseRadius() {
        return BASE_RADIUS
                + level(BaseUpgrade.SURVEY_STAKES) * 4
                + level(BaseUpgrade.WIDE_BOUNDARIES) * 8
                + level(BaseUpgrade.FAR_HORIZON) * 12
                + level(BaseUpgrade.FRONTIER_WARDS) * 4
                + level(BaseUpgrade.ANCIENT_NETWORK) * 10;
    }

    public float workDurationMultiplier(int jobIndex) {
        float reduction = level(BaseUpgrade.TRAIL_MARKERS) * 0.05F
                + level(BaseUpgrade.ANCIENT_NETWORK) * 0.14F;
        reduction += switch (jobIndex) {
            case 0 -> level(BaseUpgrade.PACK_FRAMES) * 0.06F
                    + level(BaseUpgrade.QUICK_HANDOFFS) * 0.07F
                    + level(BaseUpgrade.LIVING_WORKSHOP) * 0.10F;
            case 1 -> level(BaseUpgrade.WORKSHOP_RHYTHM) * 0.06F
                    + level(BaseUpgrade.FURNACE_BELLOWS) * 0.07F
                    + level(BaseUpgrade.HEAT_RESERVOIR) * 0.08F
                    + level(BaseUpgrade.LIVING_WORKSHOP) * 0.10F;
            case 2 -> level(BaseUpgrade.COPPER_BUSBARS) * 0.08F
                    + level(BaseUpgrade.GROUNDING_RODS) * 0.09F
                    + level(BaseUpgrade.ENERGY_RESERVOIR) * 0.10F;
            case 3 -> level(BaseUpgrade.WORKSHOP_RHYTHM) * 0.06F
                    + level(BaseUpgrade.MASTER_TOOLS) * 0.07F
                    + level(BaseUpgrade.PATTERN_MEMORY) * 0.08F
                    + level(BaseUpgrade.LIVING_WORKSHOP) * 0.10F;
            default -> 0.0F;
        };
        return Mth.clamp(1.0F - reduction, 0.34F, 1.0F);
    }

    public float hungerIntervalMultiplier() {
        return 1.0F
                + level(BaseUpgrade.FEEDING_BELLS) * 0.08F
                + level(BaseUpgrade.DEEP_PANTRY) * 0.12F
                + level(BaseUpgrade.CAMP_SANCTUARY) * 0.08F
                + level(BaseUpgrade.ANCIENT_SANCTUARY) * 0.15F;
    }

    public float moodDrainMultiplier() {
        float reduction = level(BaseUpgrade.QUIET_ROOSTS) * 0.09F
                + level(BaseUpgrade.NIGHT_LANTERNS) * 0.12F
                + level(BaseUpgrade.CAMP_SANCTUARY) * 0.08F
                + level(BaseUpgrade.ANCIENT_SANCTUARY) * 0.15F;
        return Mth.clamp(1.0F - reduction, 0.38F, 1.0F);
    }

    public float expeditionRewardMultiplier() {
        return 1.0F
                + level(BaseUpgrade.EXPEDITION_CHARTS) * 0.10F
                + level(BaseUpgrade.ANCIENT_CARTOGRAPHY) * 0.15F
                + level(BaseUpgrade.FAR_HORIZON) * 0.20F;
    }

    public float expeditionDurationMultiplier() {
        float reduction = level(BaseUpgrade.ANCIENT_CARTOGRAPHY) * 0.05F
                + level(BaseUpgrade.FAR_HORIZON) * 0.08F;
        return Mth.clamp(1.0F - reduction, 0.72F, 1.0F);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  CommandTableBlockEntity table) {
        if (level instanceof ServerLevel serverLevel) table.tickEnergy(serverLevel);
    }

    private void tickEnergy(ServerLevel level) {
        if (energyWindowTicks % 20 == 0) pruneEnergyConsumers(level);
        consumptionPerSecond = calculateDemand(level);
        boolean poweredBefore = energyConsumersPowered;
        float nextTickDemand = consumptionPerSecond / 20.0F;
        energyConsumersPowered = nextTickDemand > 0.0F && storedEnergy + 1.0E-4F >= nextTickDemand;
        if (energyConsumersPowered) storedEnergy = Math.max(0.0F, storedEnergy - nextTickDemand);
        if (poweredBefore != energyConsumersPowered) notifyAllEnergyConsumers(level);
        energyWindowTicks++;
        if (energyWindowTicks >= 20) {
            generationPerSecond = generatedThisSecond;
            generatedThisSecond = 0.0F;
            energyWindowTicks = 0;
            setChanged();
        }
    }

    public void receiveGeneratedEnergy(float amount) {
        if (amount <= 0.0F) return;
        generatedThisSecond += amount;
        storedEnergy = Math.min(energyCapacity(), storedEnergy + amount);
    }

    public boolean toggleEnergyConsumer(ServerLevel level, BlockPos consumerPos) {
        BaseEnergyRules.registerLoadedTable(level, worldPosition);
        BlockPos immutable = consumerPos.immutable();
        if (enabledEnergyConsumers.remove(immutable)) {
            BaseEnergyRules.bindConsumer(level, worldPosition, immutable, false);
            refreshEnergyAvailability(level);
            notifyEnergyStateChanged(level, immutable);
            setChanged();
            return false;
        }
        if (enabledEnergyConsumers.size() >= MAX_ENERGY_CONSUMERS
                || immutable.distSqr(worldPosition) > (double)baseRadius() * baseRadius()
                || BaseEnergyRules.demandPerSecond(level, immutable) <= 0) {
            return false;
        }
        enabledEnergyConsumers.add(immutable);
        BaseEnergyRules.bindConsumer(level, worldPosition, immutable, true);
        refreshEnergyAvailability(level);
        notifyEnergyStateChanged(level, immutable);
        setChanged();
        return true;
    }

    private void refreshEnergyAvailability(ServerLevel level) {
        boolean poweredBefore = energyConsumersPowered;
        consumptionPerSecond = calculateDemand(level);
        float nextTickDemand = consumptionPerSecond / 20.0F;
        energyConsumersPowered = nextTickDemand > 0.0F && storedEnergy + 1.0E-4F >= nextTickDemand;
        if (poweredBefore != energyConsumersPowered) notifyAllEnergyConsumers(level);
    }

    private void notifyAllEnergyConsumers(ServerLevel level) {
        enabledEnergyConsumers.forEach(pos -> notifyEnergyStateChanged(level, pos));
    }

    private static void notifyEnergyStateChanged(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        level.neighborChanged(pos, state.getBlock(), null);
    }

    public boolean isEnergyConsumerEnabled(BlockPos consumerPos) {
        return enabledEnergyConsumers.contains(consumerPos);
    }

    public boolean isEnergyConsumerPowered(BlockPos consumerPos) {
        if (!isEnergyConsumerEnabled(consumerPos)) return false;
        if (level != null && BaseEnergyRules.activeDemandPerSecond(level, consumerPos) <= 0.0F) {
            return storedEnergy > 1.0E-4F || generatedThisSecond > 1.0E-4F || generationPerSecond > 1.0E-4F;
        }
        return energyConsumersPowered;
    }

    public Set<BlockPos> enabledEnergyConsumers() {
        return Set.copyOf(enabledEnergyConsumers);
    }

    public float storedEnergy() {
        return storedEnergy;
    }

    public float energyCapacity() {
        return (BASE_ENERGY_CAPACITY + level(BaseUpgrade.ENERGY_RESERVOIR) * 250.0F)
                * (float)PrimevalTuning.server().energyStorage();
    }

    public float generationPerSecond() {
        return generationPerSecond;
    }

    public float consumptionPerSecond() {
        return consumptionPerSecond;
    }

    private float calculateDemand(ServerLevel level) {
        float demand = 0.0F;
        for (BlockPos pos : enabledEnergyConsumers) {
            if (level.isLoaded(pos)) demand += BaseEnergyRules.activeDemandPerSecond(level, pos);
        }
        return demand;
    }

    private void pruneEnergyConsumers(ServerLevel level) {
        enabledEnergyConsumers.removeIf(pos -> {
            boolean invalid = pos.distSqr(worldPosition) > (double)baseRadius() * baseRadius()
                    || level.isLoaded(pos) && BaseEnergyRules.demandPerSecond(level, pos) <= 0;
            BaseEnergyRules.bindConsumer(level, worldPosition, pos, !invalid);
            return invalid;
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CommandTableBlockEntity>("Movement", 0,
                state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public int threatAwarenessBonus() {
        return level(BaseUpgrade.WATCH_POSTS) * 3
                + level(BaseUpgrade.TRAIL_WARDS) * 4
                + level(BaseUpgrade.FRONTIER_WARDS) * 8;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        if (owner != null) output.putString("Owner", owner.toString());
        output.putInt("Insight", insight);
        output.putFloat("StoredEnergy", storedEnergy);
        var consumers = output.list("EnabledEnergyConsumers", BlockPos.CODEC);
        enabledEnergyConsumers.stream().limit(MAX_ENERGY_CONSUMERS).forEach(consumers::add);
        for (BaseUpgrade upgrade : BaseUpgrade.values()) {
            output.putInt("Upgrade" + upgrade.id(), level(upgrade));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int schema = input.getIntOr("SchemaVersion", 0);
        String ownerValue = input.getStringOr("Owner", "");
        try {
            owner = ownerValue.isBlank() ? null : UUID.fromString(ownerValue);
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }
        insight = Mth.clamp(input.getIntOr("Insight", schema == 0 ? STARTING_INSIGHT : 0), 0, 999);
        float loadedEnergy = Math.max(0.0F, input.getFloatOr("StoredEnergy", 0.0F));
        enabledEnergyConsumers.clear();
        input.listOrEmpty("EnabledEnergyConsumers", BlockPos.CODEC).stream()
                .map(BlockPos::immutable)
                .distinct()
                .limit(MAX_ENERGY_CONSUMERS)
                .forEach(enabledEnergyConsumers::add);
        for (BaseUpgrade upgrade : BaseUpgrade.values()) {
            int fallback = upgrade == BaseUpgrade.HEARTHSTONE ? 1 : 0;
            upgradeLevels[upgrade.id()] = Mth.clamp(
                    input.getIntOr("Upgrade" + upgrade.id(), fallback),
                    0,
                    upgrade.maxLevel()
            );
        }
        upgradeLevels[BaseUpgrade.HEARTHSTONE.id()] = 1;
        storedEnergy = Math.min(loadedEnergy, energyCapacity());
    }

    public record PurchaseResult(boolean success, Component message) {
        private static PurchaseResult success(Component message) { return new PurchaseResult(true, message); }
        private static PurchaseResult failure(Component message) { return new PurchaseResult(false, message); }
    }
}
