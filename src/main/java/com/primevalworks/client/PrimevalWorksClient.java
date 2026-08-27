package com.primevalworks.client;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.effect.DinosaurFootstepEffects;
import com.primevalworks.client.effect.PteranodonFlightFeedback;
import com.primevalworks.client.effect.PteranodonFirstPersonPose;
import com.primevalworks.client.effect.DinosaurHatchReveal;
import com.primevalworks.client.effect.DinoWhistleClient;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.client.render.entity.FieldDodoRenderer;
import com.primevalworks.client.render.WorksiteIndicatorRenderer;
import com.primevalworks.client.render.block.PremiumEggIncubatorRenderer;
import com.primevalworks.client.render.block.TurbineRenderer;
import com.primevalworks.client.render.block.DartTurretRenderer;
import com.primevalworks.client.render.block.LaserTurretRenderer;
import com.primevalworks.client.render.block.LaserObserverRenderer;
import com.primevalworks.client.screen.CommandTableScreen;
import com.primevalworks.client.screen.CompanionScreen;
import com.primevalworks.client.screen.WorksitePlannerScreen;
import com.primevalworks.client.screen.MachineStatusScreen;
import com.primevalworks.client.screen.FoodBoxScreen;
import com.primevalworks.client.screen.ProcessorScreen;
import com.primevalworks.client.screen.EnergyNetworkScreen;
import com.primevalworks.client.screen.AncientFurnaceScreen;
import com.primevalworks.client.screen.DartTurretScreen;
import com.primevalworks.client.screen.WhistleFollowerPickerScreen;
import com.primevalworks.client.render.entity.DartProjectileRenderer;
import com.primevalworks.config.PrimevalConfig;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.registry.ModMenus;
import com.primevalworks.registry.ModItems;
import com.primevalworks.network.payload.ClaimCommandTablePayload;
import com.primevalworks.network.payload.MountedDinosaurAttackPayload;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.block.CommandTableExtensionBlock;
import com.primevalworks.world.entity.DinosaurAnimationEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PrimevalWorks.MOD_ID, dist = Dist.CLIENT)
public final class PrimevalWorksClient {
    public PrimevalWorksClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ignored, parent) -> new ConfigurationScreen(container, parent));
        DinosaurAnimationEvents.installFootstepHandler(DinosaurFootstepEffects::onAnimationFootstep);
        DinosaurAnimationEvents.installUnmountedSpinosaurusGaitSpeed(
                () -> PrimevalConfig.CLIENT.unmountedSpinosaurusGaitSpeed.get());
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, PrimevalWorksClient::registerRenderers);
        modBus.addListener(RegisterRenderPipelinesEvent.class,
                event -> event.registerPipeline(WorksitePlannerScreen.XRAY_HIGHLIGHT_PIPELINE));
        modBus.addListener(RegisterMenuScreensEvent.class, event -> {
            event.register(ModMenus.FOOD_BOX.get(), FoodBoxScreen::new);
            event.register(ModMenus.PROCESSOR.get(), ProcessorScreen::new);
            event.register(ModMenus.ANCIENT_FURNACE.get(), AncientFurnaceScreen::new);
            event.register(ModMenus.DART_TURRET.get(), DartTurretScreen::new);
        });
        NeoForge.EVENT_BUS.addListener(PrimevalWorksClient::openCompanionScreen);
        NeoForge.EVENT_BUS.addListener(PrimevalWorksClient::mountedDinosaurAttack);
        NeoForge.EVENT_BUS.addListener(DinoWhistleClient::handleAttack);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post.class,
                DinoWhistleClient::renderInventoryHover);
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, PrimevalWorksClient::hidePlannerHand);
        NeoForge.EVENT_BUS.addListener(RenderBlockScreenEffectEvent.class, PrimevalWorksClient::hidePlannerBlockOverlay);
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, WorksitePlannerScreen::submitWorldGeometry);
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, EnergyNetworkScreen::submitWorldGeometry);
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, WorksiteIndicatorRenderer::submitGeometry);
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, WhistleFollowerPickerScreen::submitWorldGeometry);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, PteranodonFlightFeedback::tickInput);
        NeoForge.EVENT_BUS.addListener(MovementInputUpdateEvent.class,
                PteranodonFlightFeedback::preserveSpinosaurusLandSprint);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, DinosaurFootstepEffects::applyCameraImpulse);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, PteranodonFlightFeedback::applyCameraBank);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFov.class, PteranodonFlightFeedback::applyFov);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, PteranodonFlightFeedback::renderFlightHud);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, DinosaurHatchReveal::render);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, DinoWhistleClient::renderHud);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModEntities.DINOSAURS.forEach(type -> event.registerEntityRenderer(
                type.get(),
                context -> new FieldDodoRenderer<>(context, DinosaurVisualProfile.forType(type.get()))
        ));
        event.registerBlockEntityRenderer(
                ModBlockEntities.PREMIUM_EGG_INCUBATOR.get(),
                PremiumEggIncubatorRenderer::new
        );
        event.registerBlockEntityRenderer(ModBlockEntities.TURBINE.get(), TurbineRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DART_TURRET.get(), DartTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LASER_TURRET.get(), LaserTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LASER_OBSERVER.get(), LaserObserverRenderer::new);
        event.registerEntityRenderer(ModEntities.DART_PROJECTILE.get(), DartProjectileRenderer::new);
    }

    private static void hidePlannerHand(RenderHandEvent event) {
        if (Minecraft.getInstance().screen instanceof WorksitePlannerScreen
                || Minecraft.getInstance().screen instanceof EnergyNetworkScreen
                || Minecraft.getInstance().screen instanceof WhistleFollowerPickerScreen) {
            event.setCanceled(true);
            return;
        }
        PteranodonFirstPersonPose.apply(event);
    }

    private static void hidePlannerBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (Minecraft.getInstance().screen instanceof WorksitePlannerScreen
                || Minecraft.getInstance().screen instanceof EnergyNetworkScreen) {
            event.setCanceled(true);
        }
    }

    private static void openCompanionScreen(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        if (minecraft.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof FieldDodoEntity dodo) {
            boolean holdingMountSaddle = minecraft.player != null
                    && (dodo.getSpecies() == com.primevalworks.world.entity.DinosaurSpecies.PTERANODON
                            && minecraft.player.getMainHandItem().is(ModItems.PTERANODON_SADDLE.get())
                    || dodo.getSpecies() == com.primevalworks.world.entity.DinosaurSpecies.SPINOSAURUS
                            && minecraft.player.getMainHandItem().is(ModItems.SPINOSAURUS_SADDLE.get()));
            boolean holdingFossilFragment = minecraft.player != null
                    && minecraft.player.getMainHandItem().is(ModItems.FOSSIL_FRAGMENT.get());
            boolean holdingNestingTreat = minecraft.player != null
                    && minecraft.player.getMainHandItem().is(ModItems.NESTING_TREAT.get());
            boolean holdingWhistle = minecraft.player != null
                    && !com.primevalworks.world.item.DinoWhistleItem.findHeld(minecraft.player).isEmpty();
            if (holdingFossilFragment || holdingNestingTreat || holdingMountSaddle || holdingWhistle || dodo.isSaddledMount()
                    && minecraft.player != null && !minecraft.player.isShiftKeyDown()) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            BlockPos tablePos = findCommandTable(minecraft, dodo);
            if (tablePos == null) {
                minecraft.player.sendOverlayMessage(Component.literal("This companion needs a nearby Command Table to manage its base."));
                return;
            }
            minecraft.setScreen(new CompanionScreen(dodo, tablePos));
            return;
        }

        if (minecraft.hitResult instanceof BlockHitResult blockHit && minecraft.level != null) {
            BlockPos commandTablePos = resolveCommandTable(minecraft, blockHit.getBlockPos());
            if (commandTablePos != null) {
                event.setCanceled(true);
                event.setSwingHand(false);
                ClientPacketDistributor.sendToServer(new ClaimCommandTablePayload(commandTablePos));
                minecraft.setScreen(new CommandTableScreen(commandTablePos));
                return;
            }
        }

        if (minecraft.hitResult instanceof BlockHitResult blockHit && minecraft.level != null) {
            Block block = minecraft.level.getBlockState(blockHit.getBlockPos()).getBlock();
            MachineStatusScreen.Descriptor descriptor = machineDescriptor(block);
            if (descriptor != null) {
                event.setCanceled(true);
                event.setSwingHand(false);
                minecraft.setScreen(new MachineStatusScreen(blockHit.getBlockPos(), descriptor));
            }
        }
    }

    private static void mountedDinosaurAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null
                || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof FieldDodoEntity dinosaur)
                || dinosaur.getSpecies() != com.primevalworks.world.entity.DinosaurSpecies.SPINOSAURUS
                || !dinosaur.isSaddledMount()) {
            return;
        }
        if (!dinosaur.isInWater() && !dinosaur.isSpinosaurusAquaticPose()) {
            dinosaur.previewMountedAttack(minecraft.player.getYRot(), minecraft.player.getXRot());
            ClientPacketDistributor.sendToServer(new MountedDinosaurAttackPayload(
                    dinosaur.getId(), minecraft.player.getYRot(), minecraft.player.getXRot()));
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private static BlockPos resolveCommandTable(Minecraft minecraft, BlockPos hitPos) {
        if (minecraft.level == null) return null;
        var state = minecraft.level.getBlockState(hitPos);
        if (state.is(ModBlocks.COMMAND_TABLE.get())) return hitPos;
        if (state.getBlock() instanceof CommandTableExtensionBlock extension) {
            BlockPos masterPos = extension.masterPos(hitPos, state);
            if (minecraft.level.getBlockState(masterPos).is(ModBlocks.COMMAND_TABLE.get())) return masterPos;
        }
        return null;
    }

    private static MachineStatusScreen.Descriptor machineDescriptor(Block block) {
        if (block == ModBlocks.ANCIENT_SPELL_STONE.get()) {
            return machine("Ancient Spell Stone", "Defense", "48-block hostile ward",
                    "Prevents hostile spawn checks while powered.",
                    "Connect power from the Energy Map. Command, breeding, and saved entity loads are unaffected.",
                    Items.AMETHYST_SHARD, Items.END_CRYSTAL);
        }
        return null;
    }

    private static MachineStatusScreen.Descriptor machine(
            String title,
            String specialty,
            String status,
            String detail,
            String assignment,
            net.minecraft.world.item.Item input,
            net.minecraft.world.item.Item output
    ) {
        return new MachineStatusScreen.Descriptor(
                title, specialty, status, detail, assignment,
                input.getDefaultInstance(), output.getDefaultInstance()
        );
    }

    private static BlockPos findCommandTable(Minecraft minecraft, FieldDodoEntity dodo) {
        if (minecraft.level == null) {
            return null;
        }
        BlockPos saved = dodo.getCommandTablePos().orElse(null);
        if (saved != null) {
            return saved;
        }
        BlockPos center = dodo.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-50, -24, -50), center.offset(50, 24, 50))) {
            if (!minecraft.level.getBlockState(cursor).is(ModBlocks.COMMAND_TABLE.get())) {
                continue;
            }
            double distance = cursor.distSqr(center);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = cursor.immutable();
            }
        }
        return nearest;
    }
}
