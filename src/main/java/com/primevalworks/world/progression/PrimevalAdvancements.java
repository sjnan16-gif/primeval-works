package com.primevalworks.world.progression;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class PrimevalAdvancements {
    private PrimevalAdvancements() {
    }

    public static void awardHatch(ServerPlayer player, FieldDodoEntity dinosaur, boolean incubated) {
        award(player, "primeval/first_hatch", "hatch");
        award(player, "primeval/hatch_" + advancementSpeciesName(dinosaur.getSpecies()), "hatch");
        if (dinosaur.getMutationMask() != 0) award(player, "primeval/rare_mutation", "mutation");
        if (dinosaur.hasAlbinoMutation()) award(player, "primeval/albino_hatch", "albino");
        if (Integer.bitCount(dinosaur.getMutationMask()) >= 2) award(player, "primeval/jackpot", "jackpot");
        if (incubated) award(player, "primeval/premium_hatch", "hatch");
    }

    public static void awardFirstExpedition(ServerPlayer player) {
        award(player, "primeval/first_expedition", "return");
    }

    public static void awardBreed(ServerPlayer player) {
        award(player, "primeval/first_bred_egg", "breed");
    }

    private static void award(ServerPlayer player, String path, String criterion) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path)
        );
        if (advancement != null) player.getAdvancements().award(advancement, criterion);
    }

    private static String advancementSpeciesName(DinosaurSpecies species) {
        return species == DinosaurSpecies.DODO ? "dodo" : species.registryName();
    }
}
