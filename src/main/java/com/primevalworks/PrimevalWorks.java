package com.primevalworks;

import com.mojang.logging.LogUtils;
import com.primevalworks.command.PrimevalCommands;
import com.primevalworks.network.ModNetworking;
import com.primevalworks.gametest.PrimevalGameTests;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.registry.ModCreativeTabs;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.registry.ModItems;
import com.primevalworks.registry.ModMenus;
import com.primevalworks.registry.ModSounds;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.entity.DinosaurThreatTargeting;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.client.PrimevalItemTooltips;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

@Mod(PrimevalWorks.MOD_ID)
public final class PrimevalWorks {
    public static final String MOD_ID = "primevalworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PrimevalWorks(IEventBus modBus) {
        ModEntities.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);
        ModCreativeTabs.register(modBus);
        PrimevalGameTests.register(modBus);
        ModNetworking.register(modBus);
        NeoForge.EVENT_BUS.addListener(PrimevalCommands::register);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.Clone.class, event -> {
            CommandTableBlock.copyClaim(event.getOriginal(), event.getEntity());
            if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
                DinosaurOwnership.syncLoaded(original);
                DinosaurOwnership.copy(original, replacement);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommandTableBlock.getClaimedTable(player).ifPresent(table ->
                        DinosaurOwnership.activateForTable(player, table.pos(), false));
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                DinosaurOwnership.syncLoaded(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, event -> {
            if (!(event.getEntity() instanceof FieldDodoEntity dinosaur)
                    || !dinosaur.shouldRecoverFromLethalDamage(event.getSource())) {
                return;
            }
            float survivableHealth = dinosaur.getHealth() + dinosaur.getAbsorptionAmount();
            if (event.getAmount() >= survivableHealth) {
                event.setAmount(0.0F);
                dinosaur.recoverFromIncomingLethalDamage();
            }
        });
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, DinosaurThreatTargeting::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(BlockEvent.NeighborNotifyEvent.class,
                com.primevalworks.world.block.PoweredObserverBlock::onDistantBlockUpdate);
        NeoForge.EVENT_BUS.addListener(MobSpawnEvent.PositionCheck.class, event -> {
            if (!(event.getEntity() instanceof Enemy)
                    || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
            if (com.primevalworks.world.base.BaseEnergyRules.hasPoweredBlockNearby(
                    level, BlockPos.containing(event.getX(), event.getY(), event.getZ()),
                    ModBlocks.ANCIENT_SPELL_STONE.get(), 48)) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            }
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent.class,
                PrimevalItemTooltips::add);
    }
}
