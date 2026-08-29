package com.primevalworks.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.egg.DinosaurHatching;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.UUID;

public final class PrimevalCommands {
    private PrimevalCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(command("primevalworks"));
        event.getDispatcher().register(command("pw"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> help(context.getSource()))
                .then(Commands.literal("help").executes(context -> help(context.getSource())))
                .then(Commands.literal("roster").executes(context -> roster(context.getSource())))
                .then(Commands.literal("recall").executes(context -> recall(context.getSource())))
                .then(Commands.literal("egg")
                        .executes(context -> locateEgg(context.getSource(), "any"))
                        .then(Commands.argument("size", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        List.of("any", "small", "big", "large"), builder
                                ))
                                .executes(context -> locateEgg(
                                        context.getSource(), StringArgumentType.getString(context, "size")
                                ))))
                .then(Commands.literal("mutation")
                        .then(Commands.argument("trait", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        List.of("huge", "albino", "both", "clear"), builder
                                ))
                                .executes(context -> mutation(
                                        context.getSource(), StringArgumentType.getString(context, "trait")
                                ))))
                .then(Commands.literal("hatch")
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(
                                                java.util.stream.Stream.of("all"),
                                                DinosaurSpecies.playableSpecies().stream().map(DinosaurSpecies::registryName)
                                        ), builder
                                ))
                                .executes(context -> hatch(
                                        context.getSource(), StringArgumentType.getString(context, "species")
                                ))));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "/pw roster  |  /pw recall  |  /pw egg [any|small|big|large]  |  /pw hatch <species>  |  /pw mutation <huge|albino|both|clear>"
        ), false);
        return 1;
    }

    private static int mutation(CommandSourceStack source, String requested)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int mask = switch (requested.toLowerCase(java.util.Locale.ROOT)) {
            case "huge" -> FieldDodoEntity.MUTATION_HUGE;
            case "albino" -> FieldDodoEntity.MUTATION_ALBINO;
            case "both" -> FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO;
            case "clear", "none" -> 0;
            default -> -1;
        };
        if (mask < 0) {
            source.sendFailure(Component.literal("Choose huge, albino, both, or clear."));
            return 0;
        }
        FieldDodoEntity dinosaur = player.level().getEntitiesOfClass(
                        FieldDodoEntity.class,
                        player.getBoundingBox().inflate(16.0D),
                        candidate -> candidate.isAlive() && candidate.isOwnedBy(player.getUUID())
                ).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (dinosaur == null) {
            source.sendFailure(Component.literal("No owned dinosaur is within 16 blocks."));
            return 0;
        }
        dinosaur.setMutationMaskForTesting(mask);
        String label = switch (mask) {
            case FieldDodoEntity.MUTATION_HUGE -> "Huge";
            case FieldDodoEntity.MUTATION_ALBINO -> "Albino";
            case FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO -> "Huge + Albino";
            default -> "no mutation";
        };
        source.sendSuccess(() -> Component.literal("Set " + dinosaur.getDisplayName().getString() + " to " + label + "."), false);
        return 1;
    }

    private static int roster(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<DinosaurOwnership.OwnedDinosaur> owned = DinosaurOwnership.refresh(player);
        List<UUID> active = DinosaurOwnership.activeIds(player);
        source.sendSuccess(() -> Component.literal(
                "DINOSAUR ROSTER  •  " + active.size() + "/7 ACTIVE  •  "
                        + Math.max(0, owned.size() - active.size()) + " IN DEPOT  •  " + owned.size() + " OWNED"
        ), false);
        if (!active.isEmpty()) {
            String names = owned.stream().filter(record -> active.contains(record.id()))
                    .map(DinosaurOwnership.OwnedDinosaur::name).reduce((left, right) -> left + ", " + right)
                    .orElse("None");
            source.sendSuccess(() -> Component.literal("Active: " + names), false);
        }
        return owned.size();
    }

    private static int hatch(CommandSourceStack source, String requested)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (requested.equalsIgnoreCase("all")) {
            int hatched = 0;
            for (DinosaurSpecies species : DinosaurSpecies.playableSpecies()) hatched += hatchOne(source, player, species);
            return hatched;
        }
        DinosaurSpecies species = DinosaurSpecies.playableSpecies().stream()
                .filter(candidate -> candidate.registryName().equalsIgnoreCase(requested)
                        || candidate.name().equalsIgnoreCase(requested)
                        || candidate == DinosaurSpecies.DODO && requested.equalsIgnoreCase("dodo")
                        || candidate == DinosaurSpecies.TYRANNOSAURUS && requested.equalsIgnoreCase("t_rex"))
                .findFirst().orElse(null);
        if (species == null) {
            source.sendFailure(Component.literal("Unknown dinosaur species: " + requested));
            return 0;
        }
        return hatchOne(source, player, species);
    }

    private static int hatchOne(CommandSourceStack source, ServerPlayer player, DinosaurSpecies species) {
        CommandTableBlock.ClaimedTable claimed = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (claimed == null) {
            source.sendFailure(Component.literal("Place and claim a Command Table first."));
            return 0;
        }
        if (player.level() != claimed.level()) {
            source.sendFailure(Component.literal("Travel to your Command Table before hatching test dinosaurs."));
            return 0;
        }
        DinosaurHatching.HatchResult result = DinosaurHatching.hatchAtTable(
                claimed.level(), claimed.pos(), player.getUUID(), DinosaurHatching.Genome.wild(species)
        );
        if (!result.success()) {
            source.sendFailure(result.message());
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Hatched " + species.registryName() + "."), false);
        return 1;
    }

    private static int recall(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CommandTableBlock.ClaimedTable claimed = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (claimed == null) {
            source.sendFailure(Component.literal("Place and claim a Command Table first."));
            return 0;
        }
        if (player.level() != claimed.level()) {
            source.sendFailure(Component.literal("Travel to your Command Table before recalling dinosaurs."));
            return 0;
        }
        DinosaurOwnership.restoreActiveForTable(player, claimed.pos());
        int[][] offsets = {{5, 0}, {3, 4}, {-3, 4}, {-5, 0}, {-3, -4}, {3, -4}, {0, 6}};
        int recalled = 0;
        for (UUID id : DinosaurOwnership.activeIds(player)) {
            FieldDodoEntity dinosaur = DinosaurOwnership.findLoaded(player.level().getServer(), id);
            if (dinosaur == null || dinosaur.level() != claimed.level()) continue;
            int[] offset = offsets[Math.min(recalled, offsets.length - 1)];
            BlockPos table = claimed.pos();
            dinosaur.getNavigation().stop();
            dinosaur.setDeltaMovement(Vec3.ZERO);
            dinosaur.teleportTo(table.getX() + 0.5D + offset[0], table.getY() + 1.0D,
                    table.getZ() + 0.5D + offset[1]);
            recalled++;
        }
        int result = recalled;
        source.sendSuccess(() -> Component.literal("Recalled " + result + " active dinosaurs."), false);
        return recalled;
    }

    private static int locateEgg(CommandSourceStack source, String requested)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String size = requested.toLowerCase(java.util.Locale.ROOT);
        if (!List.of("any", "small", "big", "large").contains(size)) {
            source.sendFailure(Component.literal("Choose any, small, big, or large."));
            return 0;
        }

        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int radius = 224;
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                int dx = x - origin.getX();
                int dz = z - origin.getZ();
                if (dx * dx + dz * dz > radius * radius) continue;
                int surface = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = surface + 1; y >= surface - 3; y--) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!matchesEgg(player.level().getBlockState(candidate), size)) continue;
                    double distance = candidate.distSqr(origin);
                    if (distance < nearestDistance) {
                        nearest = candidate;
                        nearestDistance = distance;
                    }
                    break;
                }
            }
        }
        if (nearest == null) {
            source.sendFailure(Component.literal(
                    "No " + (size.equals("any") ? "dinosaur" : size) + " egg was found within 224 blocks. Explore new chunks and try again."
            ));
            return 0;
        }

        BlockPos destination = nearest;
        player.teleportTo(destination.getX() + 0.5D, destination.getY() + 1.05D, destination.getZ() + 0.5D);
        source.sendSuccess(() -> Component.literal(
                "Found a " + eggSizeName(player.level().getBlockState(destination)) + " egg at "
                        + destination.getX() + ", " + destination.getY() + ", " + destination.getZ() + "."
        ), false);
        return 1;
    }

    private static boolean matchesEgg(BlockState state, String size) {
        return switch (size) {
            case "small" -> state.is(ModBlocks.SMALL_DINOSAUR_EGG.get());
            case "big" -> state.is(ModBlocks.BIG_DINOSAUR_EGG.get());
            case "large" -> state.is(ModBlocks.LARGE_DINOSAUR_EGG.get());
            default -> state.is(ModBlocks.SMALL_DINOSAUR_EGG.get())
                    || state.is(ModBlocks.BIG_DINOSAUR_EGG.get())
                    || state.is(ModBlocks.LARGE_DINOSAUR_EGG.get());
        };
    }

    private static String eggSizeName(BlockState state) {
        if (state.is(ModBlocks.SMALL_DINOSAUR_EGG.get())) return "small";
        if (state.is(ModBlocks.BIG_DINOSAUR_EGG.get())) return "big";
        return "large";
    }
}
