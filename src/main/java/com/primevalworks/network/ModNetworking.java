package com.primevalworks.network;

import com.primevalworks.network.payload.FeedDodoPayload;
import com.primevalworks.network.payload.AssignDodoWorkPayload;
import com.primevalworks.network.payload.ClaimCommandTablePayload;
import com.primevalworks.network.payload.CraftingCataloguePayload;
import com.primevalworks.network.payload.RequestCraftingCataloguePayload;
import com.primevalworks.network.payload.BaseUpgradesPayload;
import com.primevalworks.network.payload.BaseInventoryPayload;
import com.primevalworks.network.payload.RequestBaseInventoryPayload;
import com.primevalworks.network.payload.PurchaseBaseUpgradePayload;
import com.primevalworks.network.payload.RequestBaseUpgradesPayload;
import com.primevalworks.network.payload.CommandTableActionPayload;
import com.primevalworks.network.payload.DinosaurRosterPayload;
import com.primevalworks.network.payload.SwapActiveDinosaurPayload;
import com.primevalworks.network.payload.BaseEnergyPayload;
import com.primevalworks.network.payload.RequestBaseEnergyPayload;
import com.primevalworks.network.payload.ToggleBaseEnergyConsumerPayload;
import com.primevalworks.network.payload.RequestDinosaurWorkStatePayload;
import com.primevalworks.network.payload.DinosaurWorkStatePayload;
import com.primevalworks.network.payload.HatchRevealPayload;
import com.primevalworks.network.payload.OpenBaseMachineMenuPayload;
import com.primevalworks.network.payload.BaseMachineRoutingPayload;
import com.primevalworks.network.payload.ConfigureBaseMachineSlotPayload;
import com.primevalworks.network.payload.MountedDinosaurAttackPayload;
import com.primevalworks.network.payload.SpinosaurusLandSprintPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                FeedDodoPayload.TYPE,
                FeedDodoPayload.STREAM_CODEC,
                FeedDodoPayload::handle
        );
        event.registrar("1").playToServer(
                AssignDodoWorkPayload.TYPE,
                AssignDodoWorkPayload.STREAM_CODEC,
                AssignDodoWorkPayload::handle
        );
        event.registrar("1").playToServer(
                RequestDinosaurWorkStatePayload.TYPE,
                RequestDinosaurWorkStatePayload.STREAM_CODEC,
                RequestDinosaurWorkStatePayload::handle
        );
        event.registrar("1").playToClient(
                DinosaurWorkStatePayload.TYPE,
                DinosaurWorkStatePayload.STREAM_CODEC,
                DinosaurWorkStatePayload::handle
        );
        event.registrar("1").playToServer(
                ClaimCommandTablePayload.TYPE,
                ClaimCommandTablePayload.STREAM_CODEC,
                ClaimCommandTablePayload::handle
        );
        event.registrar("1").playToServer(
                RequestCraftingCataloguePayload.TYPE,
                RequestCraftingCataloguePayload.STREAM_CODEC,
                RequestCraftingCataloguePayload::handle
        );
        event.registrar("1").playToClient(
                CraftingCataloguePayload.TYPE,
                CraftingCataloguePayload.STREAM_CODEC,
                CraftingCataloguePayload::handle
        );
        event.registrar("1").playToServer(
                RequestBaseInventoryPayload.TYPE,
                RequestBaseInventoryPayload.STREAM_CODEC,
                RequestBaseInventoryPayload::handle
        );
        event.registrar("1").playToClient(
                BaseInventoryPayload.TYPE,
                BaseInventoryPayload.STREAM_CODEC,
                BaseInventoryPayload::handle
        );
        event.registrar("1").playToServer(
                RequestBaseUpgradesPayload.TYPE,
                RequestBaseUpgradesPayload.STREAM_CODEC,
                RequestBaseUpgradesPayload::handle
        );
        event.registrar("1").playToServer(
                PurchaseBaseUpgradePayload.TYPE,
                PurchaseBaseUpgradePayload.STREAM_CODEC,
                PurchaseBaseUpgradePayload::handle
        );
        event.registrar("1").playToClient(
                BaseUpgradesPayload.TYPE,
                BaseUpgradesPayload.STREAM_CODEC,
                BaseUpgradesPayload::handle
        );
        event.registrar("1").playToServer(
                CommandTableActionPayload.TYPE,
                CommandTableActionPayload.STREAM_CODEC,
                CommandTableActionPayload::handle
        );
        event.registrar("1").playToClient(
                DinosaurRosterPayload.TYPE,
                DinosaurRosterPayload.STREAM_CODEC,
                DinosaurRosterPayload::handle
        );
        event.registrar("1").playToServer(
                SwapActiveDinosaurPayload.TYPE,
                SwapActiveDinosaurPayload.STREAM_CODEC,
                SwapActiveDinosaurPayload::handle
        );
        event.registrar("1").playToServer(
                RequestBaseEnergyPayload.TYPE,
                RequestBaseEnergyPayload.STREAM_CODEC,
                RequestBaseEnergyPayload::handle
        );
        event.registrar("1").playToClient(
                BaseEnergyPayload.TYPE,
                BaseEnergyPayload.STREAM_CODEC,
                BaseEnergyPayload::handle
        );
        event.registrar("1").playToServer(
                ToggleBaseEnergyConsumerPayload.TYPE,
                ToggleBaseEnergyConsumerPayload.STREAM_CODEC,
                ToggleBaseEnergyConsumerPayload::handle
        );
        event.registrar("1").playToClient(
                HatchRevealPayload.TYPE,
                HatchRevealPayload.STREAM_CODEC,
                HatchRevealPayload::handle
        );
        event.registrar("1").playToServer(
                OpenBaseMachineMenuPayload.TYPE,
                OpenBaseMachineMenuPayload.STREAM_CODEC,
                OpenBaseMachineMenuPayload::handle
        );
        event.registrar("1").playToClient(
                BaseMachineRoutingPayload.TYPE,
                BaseMachineRoutingPayload.STREAM_CODEC,
                BaseMachineRoutingPayload::handle
        );
        event.registrar("1").playToServer(
                ConfigureBaseMachineSlotPayload.TYPE,
                ConfigureBaseMachineSlotPayload.STREAM_CODEC,
                ConfigureBaseMachineSlotPayload::handle
        );
        event.registrar("1").playToServer(
                MountedDinosaurAttackPayload.TYPE,
                MountedDinosaurAttackPayload.STREAM_CODEC,
                MountedDinosaurAttackPayload::handle
        );
        event.registrar("1").playToServer(
                SpinosaurusLandSprintPayload.TYPE,
                SpinosaurusLandSprintPayload.STREAM_CODEC,
                SpinosaurusLandSprintPayload::handle
        );
    }
}
