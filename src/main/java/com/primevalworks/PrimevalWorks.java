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
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.entity.DinosaurThreatTargeting;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.client.PrimevalItemTooltips;
import com.primevalworks.config.PrimevalConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(PrimevalWorks.MOD_ID)
public final class PrimevalWorks {
    public static final String MOD_ID = "primevalworks";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Integer> PENDING_LOGIN_RESTORES = new HashMap<>();

    public PrimevalWorks(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, PrimevalConfig.CLIENT_SPEC, "primevalworks-client.toml");
        container.registerConfig(ModConfig.Type.SERVER, PrimevalConfig.SERVER_SPEC, "primevalworks-server.toml");
        modBus.addListener(ModConfigEvent.Loading.class, PrimevalConfig::loadServer);
        modBus.addListener(ModConfigEvent.Reloading.class, PrimevalConfig::loadServer);
        modBus.addListener(ModConfigEvent.Unloading.class, PrimevalConfig::unloadServer);
        ModEntities.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModItems.register(modBus);
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
                DinosaurOwnership.prepareActiveRestore(player);
                PENDING_LOGIN_RESTORES.put(player.getUUID(), 20);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PENDING_LOGIN_RESTORES.remove(player.getUUID());
                DinosaurOwnership.syncLoaded(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, PrimevalWorks::restoreLoginCompanions);
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

    private static void restoreLoginCompanions(ServerTickEvent.Post event) {
        if (PENDING_LOGIN_RESTORES.isEmpty()) return;
        var iterator = PENDING_LOGIN_RESTORES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }
            iterator.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) continue;
            CommandTableBlock.getClaimedTable(player).ifPresent(table ->
                    DinosaurOwnership.restoreActiveForTable(player, table));
        }
    }
}
