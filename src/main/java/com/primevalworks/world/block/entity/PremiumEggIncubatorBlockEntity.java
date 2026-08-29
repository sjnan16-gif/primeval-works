package com.primevalworks.world.block.entity;

import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.egg.DinosaurEggSize;
import com.primevalworks.world.egg.DinosaurHatching;
import com.primevalworks.world.egg.DinosaurEggGenome;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.entity.DinosaurMutationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class PremiumEggIncubatorBlockEntity extends BlockEntity {
    public static final int INCUBATION_TICKS = DinosaurEggSize.SMALL.baseIncubationTicks();
    private static final int SCHEMA_VERSION = 2;

    private ItemStack egg = ItemStack.EMPTY;
    private int progress;
    private int requiredTicks;
    private DinosaurSpecies species = DinosaurSpecies.DODO;
    private int quality;
    private int mutationMask;
    private int hueVariant;
    private @Nullable UUID owner;
    private @Nullable BlockPos commandTablePos;

    public PremiumEggIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PREMIUM_EGG_INCUBATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PremiumEggIncubatorBlockEntity incubator) {
        if (incubator.egg.isEmpty() || incubator.requiredTicks <= 0 || incubator.commandTablePos == null || incubator.owner == null) {
            return;
        }
        if (incubator.progress < incubator.requiredTicks) {
            incubator.progress++;
            // Mark every elapsed tick for disk persistence. Client packets remain throttled,
            // but quitting between visual updates can no longer roll the timer back.
            incubator.setChanged();
            if (incubator.progress % 20 == 0) {
                incubator.sync();
                if (incubator.progress % 80 == 0 && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.WAX_ON,
                            pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                            3, 0.18D, 0.08D, 0.18D, 0.005D
                    );
                }
            }
            return;
        }
        // A completed egg no longer requests energy. Resolve the hatch before any power
        // check so a save/rejoin at exactly 100% cannot strand the timer forever.
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 20L != 0L) {
            return;
        }

        DinosaurHatching.Genome genome = DinosaurHatching.Genome.incubated(
                incubator.species, incubator.quality, incubator.mutationMask, incubator.hueVariant);
        DinosaurHatching.HatchResult result = DinosaurHatching.hatchAtTable(
                serverLevel,
                incubator.commandTablePos,
                incubator.owner,
                genome
        );
        if (!result.success()) {
            return;
        }

        incubator.clearIncubation();
        incubator.sync();
        serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                18, 0.34D, 0.28D, 0.34D, 0.025D
        );
    }

    public InsertResult insertEgg(ServerPlayer player, ItemStack heldStack) {
        if (!egg.isEmpty()) {
            return InsertResult.failure(Component.translatable("message.primevalworks.incubator.occupied"));
        }
        DinosaurEggSize size = DinosaurEggSize.fromItem(heldStack).orElse(null);
        if (size == null) {
            return InsertResult.failure(Component.translatable("message.primevalworks.incubator.invalid_egg"));
        }
        CommandTableBlock.ClaimedTable table = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (table == null) {
            return InsertResult.failure(Component.translatable("message.primevalworks.egg.no_base"));
        }
        int baseRadius = CommandTableBlock.baseRadius(table.level(), table.pos());
        if (table.level() != level || table.pos().distSqr(worldPosition) > (double) baseRadius * baseRadius) {
            return InsertResult.failure(Component.translatable("message.primevalworks.incubator.outside_base"));
        }

        RandomSource random = level.getRandom();
        DinosaurEggGenome existingGenome = DinosaurEggGenome.read(heldStack).orElse(null);
        if (existingGenome == null) {
            species = size.randomSpecies(random);
            quality = Mth.clamp(62 + random.nextInt(20) + random.nextInt(20), 62, 100);
            mutationMask = rollMutationMask(random);
            quality = Mth.clamp(quality + DinosaurMutationRules.qualityBonus(mutationMask, random.nextFloat()), 0, 100);
            hueVariant = random.nextInt(17) - 8;
        } else if (existingGenome.origin() == DinosaurEggGenome.Origin.INCUBATED) {
            species = existingGenome.species();
            quality = existingGenome.quality();
            mutationMask = existingGenome.mutationMask();
            hueVariant = existingGenome.hueVariant();
        } else {
            species = existingGenome.species();
            quality = Mth.clamp(existingGenome.quality() + 4 + random.nextInt(5), 0, 100);
            int addedMutations = rollMutationMask(random) & ~existingGenome.mutationMask();
            mutationMask = existingGenome.mutationMask() | addedMutations;
            quality = Mth.clamp(quality
                    + DinosaurMutationRules.qualityBonus(addedMutations, random.nextFloat()), 0, 100);
            hueVariant = existingGenome.hueVariant();
        }
        egg = new DinosaurEggGenome(
                species, DinosaurEggGenome.Origin.INCUBATED, quality, mutationMask, hueVariant
        ).writeTo(heldStack.copyWithCount(1));
        int mutationCount = Integer.bitCount(mutationMask);
        double genomeTime = 1.0D + (quality - 62) * 0.0025D + mutationCount * 0.12D;
        requiredTicks = Math.max(1, Mth.ceil(size.baseIncubationTicks() * genomeTime
                / PrimevalTuning.server().incubatorSpeed()));
        progress = 0;
        owner = player.getUUID();
        commandTablePos = table.pos();
        sync();
        return InsertResult.success(Component.translatable(
                "message.primevalworks.incubator.started",
                Component.translatable(size.translationKey())
        ));
    }

    public ItemStack removeEgg() {
        ItemStack removed = egg;
        clearIncubation();
        sync();
        return removed;
    }

    public ItemStack getEgg() {
        return egg;
    }

    public boolean hasEgg() {
        return !egg.isEmpty();
    }

    public int getProgress() {
        return progress;
    }

    public int getRequiredTicks() {
        return requiredTicks;
    }

    public DinosaurSpecies getSelectedSpecies() {
        return species;
    }

    public int getGeneticQuality() {
        return quality;
    }

    public int getMutationMask() {
        return mutationMask;
    }

    public int getRemainingTicks() {
        return Math.max(0, requiredTicks - progress);
    }

    public float getProgressFraction() {
        return requiredTicks <= 0 ? 0.0F : Mth.clamp(progress / (float)requiredTicks, 0.0F, 1.0F);
    }

    public Component statusMessage() {
        if (egg.isEmpty()) {
            return Component.translatable("message.primevalworks.incubator.empty");
        }
        int seconds = Mth.ceil(getRemainingTicks() / 20.0F);
        return Component.translatable("message.primevalworks.incubator.status", seconds / 60, seconds % 60);
    }

    private int rollMutationMask(RandomSource random) {
        return DinosaurMutationRules.roll(true, random.nextFloat(), random.nextFloat());
    }

    private void clearIncubation() {
        egg = ItemStack.EMPTY;
        progress = 0;
        requiredTicks = 0;
        species = DinosaurSpecies.DODO;
        quality = 0;
        mutationMask = 0;
        hueVariant = 0;
        owner = null;
        commandTablePos = null;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        if (!egg.isEmpty()) output.store("Egg", ItemStack.CODEC, egg);
        output.putInt("Progress", progress);
        output.putInt("RequiredTicks", requiredTicks);
        output.putString("Species", species.registryName());
        output.putInt("Quality", quality);
        output.putInt("MutationMask", mutationMask);
        output.putInt("HueVariant", hueVariant);
        if (owner != null) output.putString("Owner", owner.toString());
        if (commandTablePos != null) output.putLong("CommandTable", commandTablePos.asLong());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int schema = input.getIntOr("SchemaVersion", 0);
        if (schema > SCHEMA_VERSION) {
            clearIncubation();
            return;
        }
        egg = input.read("Egg", ItemStack.CODEC).filter(stack -> DinosaurEggSize.fromItem(stack).isPresent()).orElse(ItemStack.EMPTY);
        progress = Math.max(0, input.getIntOr("Progress", 0));
        requiredTicks = Math.max(0, input.getIntOr("RequiredTicks", 0));
        species = DinosaurSpecies.byRegistryName(input.getStringOr("Species", DinosaurSpecies.DODO.registryName()));
        quality = Mth.clamp(input.getIntOr("Quality", 0), 0, 100);
        mutationMask = schema >= SCHEMA_VERSION
                ? input.getIntOr("MutationMask", 0)
                        & (FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO)
                : 0;
        hueVariant = Mth.clamp(input.getIntOr("HueVariant", 0), -8, 8);
        String ownerValue = input.getStringOr("Owner", "");
        try {
            owner = ownerValue.isBlank() ? null : UUID.fromString(ownerValue);
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }
        commandTablePos = input.getLong("CommandTable").map(BlockPos::of).orElse(null);
        if (egg.isEmpty() || requiredTicks <= 0 || owner == null || commandTablePos == null) {
            clearIncubation();
        } else {
            progress = Math.min(progress, requiredTicks);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record InsertResult(boolean success, Component message) {
        public static InsertResult success(Component message) {
            return new InsertResult(true, message);
        }

        public static InsertResult failure(Component message) {
            return new InsertResult(false, message);
        }
    }
}
