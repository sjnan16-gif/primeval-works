package com.primevalworks.client.effect;

import com.primevalworks.config.PrimevalConfig;
import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.network.payload.SpinosaurusLandSprintPayload;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class PteranodonFlightFeedback {
    private static final float FULL_FLIGHT_SPEED = 1.72F;
    private static float fovBoost;
    private static float smoothedFlightSpeed;
    private static float takeoffKick;
    private static boolean wasAirborne;
    private static float cameraBank;
    private static float hudVisibility;
    private static float displayedStamina = 100.0F;
    private static long lastFovNanos;
    private static long lastCameraNanos;
    private static long lastHudNanos;
    private static int lastSpinosaurusSprintEntity = -1;
    private static boolean lastSpinosaurusSprintState;
    private static PteranodonFlightSoundInstance flightSound;

    private PteranodonFlightFeedback() {
    }

    public static void tickInput(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        FieldDodoEntity mount = mountedSaddledDinosaur(minecraft);
        if (mount != null) {
            if (mount.getSpecies() == DinosaurSpecies.PTERANODON) {
                mount.setPteranodonClientDescendInput(minecraft.options.keySprint.isDown());
                if (mount.isPteranodonAirborne()
                        && minecraft.player instanceof net.minecraft.client.player.LocalPlayer localPlayer
                        && (flightSound == null || flightSound.isStopped())) {
                    flightSound = new PteranodonFlightSoundInstance(localPlayer, mount);
                    minecraft.getSoundManager().play(flightSound);
                }
            } else if (mount.getSpecies() == DinosaurSpecies.SPINOSAURUS) {
                mount.setSpinosaurusClientDescendInput(minecraft.options.keySprint.isDown());
                boolean landSprint = !mount.isInWater()
                        && !mount.isSpinosaurusBreaching()
                        && minecraft.options.keyShift.isDown()
                        && minecraft.options.keyUp.isDown();
                mount.setSpinosaurusLandSprinting(landSprint);
                if (lastSpinosaurusSprintEntity != mount.getId()
                        || lastSpinosaurusSprintState != landSprint) {
                    ClientPacketDistributor.sendToServer(
                            new SpinosaurusLandSprintPayload(mount.getId(), landSprint));
                    lastSpinosaurusSprintEntity = mount.getId();
                    lastSpinosaurusSprintState = landSprint;
                }
            }
            if (minecraft.player != null) {
                minecraft.player.setSprinting(false);
            }
        }
    }

    public static void preserveSpinosaurusLandSprint(MovementInputUpdateEvent event) {
        if (!(event.getEntity().getVehicle() instanceof FieldDodoEntity mount)
                || mount.getSpecies() != DinosaurSpecies.SPINOSAURUS
                || mount.isInWater()
                || mount.isSpinosaurusBreaching()) {
            return;
        }
        Input input = event.getInput().keyPresses;
        boolean sprintingMount = input.forward() && input.shift();
        event.getInput().keyPresses = new Input(
                input.forward(), input.backward(), input.left(), input.right(), false,
                sprintingMount ? false : input.shift(), input.sprint());
    }

    public static void applyFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        FieldDodoEntity mount = mountedSaddledDinosaur(minecraft);
        boolean spinosaurusLandSprint = mount != null
                && mount.getSpecies() == DinosaurSpecies.SPINOSAURUS
                && !mount.isSpinosaurusAquaticPose()
                && mount.isSpinosaurusLandSprinting();
        if (mount != null && mount.getSpecies() == DinosaurSpecies.SPINOSAURUS
                && !mount.isSpinosaurusAquaticPose() && !spinosaurusLandSprint) {
            mount = null;
        }
        float fullSpeed = mount != null && mount.getSpecies() == DinosaurSpecies.SPINOSAURUS
                ? spinosaurusLandSprint
                        ? 0.36F * (float)PrimevalTuning.server().spinosaurusSprintSpeed()
                        : 1.62F * (float)PrimevalTuning.server().spinosaurusSwimSpeed()
                : FULL_FLIGHT_SPEED * (float)PrimevalTuning.server().pteranodonFlightSpeed();
        float speed = mount == null ? 0.0F : mount.getSpecies() == DinosaurSpecies.SPINOSAURUS
                ? spinosaurusLandSprint ? (float)mount.getDeltaMovement().horizontalDistance()
                : mount.getSpinosaurusSwimSpeed() : mount.getPteranodonFlightSpeed();
        float deltaSeconds = frameSeconds(true);
        boolean airborne = mount != null && mount.isPteranodonAirborne();
        if (airborne && !wasAirborne) {
            takeoffKick = 3.2F;
        }
        wasAirborne = airborne;
        takeoffKick = follow(takeoffKick, 0.0F, 2.8F, deltaSeconds);
        smoothedFlightSpeed = follow(smoothedFlightSpeed, speed,
                speed > smoothedFlightSpeed ? 5.5F : 3.8F, deltaSeconds);
        float normalized = Mth.clamp((smoothedFlightSpeed - 0.30F)
                / (fullSpeed - 0.30F), 0.0F, 1.0F);
        float frontLoaded = 1.0F - (float)Math.pow(1.0F - normalized, 1.75D);
        float target = mount == null ? 0.0F
                : (frontLoaded * (spinosaurusLandSprint ? 16.0F : 13.0F) + takeoffKick)
                * PrimevalConfig.CLIENT.mountFovStrength.get().floatValue();
        fovBoost = follow(fovBoost, target, target > fovBoost ? 7.0F : 5.0F, deltaSeconds);
        float accessibility = minecraft.options.fovEffectScale().get().floatValue();
        event.setFOV(event.getFOV() + fovBoost * accessibility);
    }

    public static void applyCameraBank(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        FieldDodoEntity mount = mountedFlightDinosaur(minecraft);
        float target = 0.0F;
        if (mount != null && minecraft.player != null) {
            float speed = mount.getSpecies() == DinosaurSpecies.SPINOSAURUS
                    ? Mth.clamp(mount.getSpinosaurusSwimSpeed()
                            / (1.62F * (float)PrimevalTuning.server().spinosaurusSwimSpeed()), 0.0F, 1.0F)
                    : Mth.clamp(mount.getPteranodonFlightSpeed()
                            / (FULL_FLIGHT_SPEED * (float)PrimevalTuning.server().pteranodonFlightSpeed()),
                            0.0F, 1.0F);
            float steeringError = Mth.wrapDegrees(minecraft.player.getYRot() - mount.getYRot());
            target = Mth.clamp(-steeringError * 0.075F, -4.2F, 4.2F) * speed;
        }
        target *= PrimevalConfig.CLIENT.mountBankStrength.get().floatValue();
        cameraBank = follow(cameraBank, target, 8.0F, frameSeconds(false));
        float accessibility = minecraft.options.screenEffectScale().get().floatValue();
        event.setRoll(event.getRoll() + cameraBank * accessibility);
    }

    public static void renderFlightHud(RenderGuiEvent.Post event) {
        if (!PrimevalConfig.CLIENT.staminaHud.get()) {
            hudVisibility = 0.0F;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        FieldDodoEntity currentMount = mountedStaminaDinosaur(minecraft);
        boolean mounted = currentMount != null
                && minecraft.screen == null
                && !minecraft.options.hideGui;
        float hudDelta = hudFrameSeconds();
        hudVisibility = follow(hudVisibility, mounted ? 1.0F : 0.0F, mounted ? 10.0F : 7.0F,
                hudDelta);
        if (hudVisibility < 0.015F) {
            return;
        }

        FieldDodoEntity mount = mounted ? currentMount : null;
        float staminaTarget = mount == null ? 100.0F
                : mount.getSpecies() == DinosaurSpecies.SPINOSAURUS
                ? mount.getSpinosaurusLandStamina()
                : mount.getPteranodonStamina();
        displayedStamina = follow(displayedStamina, staminaTarget, 9.0F, hudDelta);
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int alpha = Mth.clamp(Math.round(hudVisibility * 164.0F), 0, 255);
        int settledBarY = graphics.guiHeight() - 22;
        int barY = settledBarY + Math.round((1.0F - hudVisibility) * 5.0F);
        drawStamina(graphics, minecraft.font, mount, displayedStamina, barY, alpha);
    }

    private static void drawStamina(
            GuiGraphicsExtractor graphics,
            Font font,
            FieldDodoEntity mount,
            float stamina,
            int barY,
            int alpha
    ) {
        int width = 88;
        int x = graphics.guiWidth() - width - 10;
        float ratio = Mth.clamp(stamina / 100.0F, 0.0F, 1.0F);
        boolean spinosaurus = mount != null && mount.getSpecies() == DinosaurSpecies.SPINOSAURUS;
        boolean exhausted = mount != null && (spinosaurus
                ? mount.isSpinosaurusLandExhausted()
                : mount.isPteranodonExhausted());
        Component label = exhausted
                ? Component.translatable(spinosaurus
                        ? "hud.primevalworks.spinosaurus_stamina.exhausted"
                        : "hud.primevalworks.pteranodon_stamina.exhausted")
                : Component.translatable("hud.primevalworks.pteranodon_stamina", Math.round(stamina));
        label = label.copy().withStyle(style -> style.withBold(true));
        float labelScale = 0.62F;
        int labelColor = (alpha << 24) | (exhausted
                ? 0xFF7B67 : 0xD8D0C2);

        int labelWidth = Math.round(font.width(label) * labelScale);
        int labelX = x + Math.max(0, (width - labelWidth) / 2);
        graphics.pose().pushMatrix();
        graphics.pose().translate(labelX, barY - 8);
        graphics.pose().scale(labelScale, labelScale);
        graphics.text(font, label, 0, 0, labelColor, true);
        graphics.pose().popMatrix();

        int borderAlpha = Math.round(alpha * 0.72F);
        graphics.fill(x - 1, barY - 1, x + width + 1, barY + 6,
                (borderAlpha << 24) | 0x241A18);
        graphics.fill(x, barY, x + width, barY + 5,
                (Math.round(alpha * 0.42F) << 24) | 0x120D0C);
        int fill = Math.round((width - 2) * ratio);
        if (fill > 0) {
            int rgb = staminaColor(ratio);
            graphics.fill(x + 1, barY + 1, x + 1 + fill, barY + 4,
                    (alpha << 24) | rgb);
            if (fill > 2) {
                graphics.fill(x + 1, barY + 1, x + fill, barY + 2,
                        (Math.round(alpha * 0.72F) << 24) | 0xFFF0C7);
            }
        }
    }

    private static int staminaColor(float ratio) {
        if (ratio < 0.35F) {
            return 0xD75443;
        }
        if (ratio < 0.68F) {
            return 0xD89B3C;
        }
        return 0x73A85B;
    }

    private static FieldDodoEntity mountedStaminaDinosaur(Minecraft minecraft) {
        FieldDodoEntity dinosaur = mountedSaddledDinosaur(minecraft);
        if (dinosaur == null) return null;
        if (dinosaur.getSpecies() == DinosaurSpecies.PTERANODON) return dinosaur;
        return dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                && !dinosaur.isInWater()
                && !dinosaur.isSpinosaurusBreaching() ? dinosaur : null;
    }

    private static FieldDodoEntity mountedFlightDinosaur(Minecraft minecraft) {
        FieldDodoEntity dinosaur = mountedSaddledDinosaur(minecraft);
        if (dinosaur == null) return null;
        if (dinosaur.getSpecies() == DinosaurSpecies.PTERANODON) return dinosaur;
        return dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                && (dinosaur.isSpinosaurusSwimming() || dinosaur.isSpinosaurusBreaching()) ? dinosaur : null;
    }

    private static FieldDodoEntity mountedSaddledDinosaur(Minecraft minecraft) {
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof FieldDodoEntity dinosaur)
                || !dinosaur.isSaddledMount()) {
            return null;
        }
        return dinosaur;
    }

    private static float frameSeconds(boolean fov) {
        long now = System.nanoTime();
        long previous = fov ? lastFovNanos : lastCameraNanos;
        if (fov) lastFovNanos = now;
        else lastCameraNanos = now;
        return previous == 0L ? 1.0F / 60.0F
                : Mth.clamp((now - previous) / 1_000_000_000.0F, 0.001F, 0.05F);
    }

    private static float hudFrameSeconds() {
        long now = System.nanoTime();
        long previous = lastHudNanos;
        lastHudNanos = now;
        return previous == 0L ? 1.0F / 60.0F
                : Mth.clamp((now - previous) / 1_000_000_000.0F, 0.001F, 0.05F);
    }

    private static float follow(float current, float target, float speed, float deltaSeconds) {
        return Mth.lerp(1.0F - (float)Math.exp(-speed * deltaSeconds), current, target);
    }
}
