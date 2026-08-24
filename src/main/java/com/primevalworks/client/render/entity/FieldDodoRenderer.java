package com.primevalworks.client.render.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DataTickets;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.client.model.entity.FieldDodoModel;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.ARGB;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FieldDodoRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<FieldDodoEntity, R> {
    private static final int FULL_BRIGHT_LIGHT = 15_728_880;
    private static final DataTicket<Float> HEAD_YAW =
            DataTicket.create("primevalworks_head_yaw", Float.class);
    private static final DataTicket<Float> HEAD_PITCH =
            DataTicket.create("primevalworks_head_pitch", Float.class);
    private static final DataTicket<Float> BODY_LEAD =
            DataTicket.create("primevalworks_body_lead", Float.class);
    private static final DataTicket<Float> TAIL_YAW =
            DataTicket.create("primevalworks_tail_yaw", Float.class);
    private static final DataTicket<Float> TAIL_TIP_YAW =
            DataTicket.create("primevalworks_tail_tip_yaw", Float.class);
    private static final DataTicket<Float> TURN_STEP =
            DataTicket.create("primevalworks_turn_step", Float.class);
    private static final DataTicket<Float> LEFT_FOOT_OFFSET =
            DataTicket.create("primevalworks_left_foot_offset", Float.class);
    private static final DataTicket<Float> RIGHT_FOOT_OFFSET =
            DataTicket.create("primevalworks_right_foot_offset", Float.class);
    private static final DataTicket<Float> FLIGHT_PITCH =
            DataTicket.create("primevalworks_flight_pitch", Float.class);
    private static final DataTicket<Float> FLIGHT_ROLL =
            DataTicket.create("primevalworks_flight_roll", Float.class);
    private static final DataTicket<Float> IDLE_BODY_BOB =
            DataTicket.create("primevalworks_idle_body_bob", Float.class);
    private static final DataTicket<Float> IDLE_BODY_ROLL =
            DataTicket.create("primevalworks_idle_body_roll", Float.class);
    public static final DataTicket<Float> DEFEAT_PROGRESS =
            DataTicket.create("primevalworks_defeat_progress", Float.class);
    public static final DataTicket<Vec3> DEFEAT_OFFSET =
            DataTicket.create("primevalworks_defeat_offset", Vec3.class);
    private static final DataTicket<Integer> ENTITY_ID =
            DataTicket.create("primevalworks_entity_id", Integer.class);
    private static final DataTicket<Boolean> TRACK_SPINO_RIDER =
            DataTicket.create("primevalworks_track_spino_rider", Boolean.class);
    private static final DataTicket<Boolean> SPINO_RIDER_AQUATIC =
            DataTicket.create("primevalworks_spino_rider_aquatic", Boolean.class);
    private final DinosaurVisualProfile profile;

    public FieldDodoRenderer(EntityRendererProvider.Context context, DinosaurVisualProfile profile) {
        super(context, new FieldDodoModel(profile));
        this.profile = profile;
        shadowRadius = profile.shadowRadius();
        withScale(profile.modelScale());
        withRenderLayer(new DodoIndicatorLayer<>(
                context,
                this,
                profile.indicatorHeight() / profile.modelScale(),
                1.0F / profile.modelScale(),
                profile.statusIconScale()
        ));
        withRenderLayer(new DinosaurMouthItemLayer<>(context, this, profile));
    }

    @Override
    public boolean shouldShowName(FieldDodoEntity dinosaur, double distanceToCameraSq) {
        return false;
    }

    @Override
    public void addRenderData(FieldDodoEntity dinosaur, Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(DataTickets.TICK, dinosaur.tickCount + (double)partialTick);
        renderState.addGeckolibData(ENTITY_ID, dinosaur.getId());
        renderState.addGeckolibData(TRACK_SPINO_RIDER,
                dinosaur.getSpecies() == com.primevalworks.world.entity.DinosaurSpecies.SPINOSAURUS
                        && dinosaur.getControllingPassenger() != null);
        renderState.addGeckolibData(SPINO_RIDER_AQUATIC, dinosaur.isSpinosaurusAquaticPose());
        boolean trackingTarget = dinosaur.getTarget() != null && dinosaur.getTarget().isAlive();
        boolean mountedFlight = dinosaur.isPteranodonAirborne();
        boolean spinosaurusMotion = profile.assetName().equals("spino")
                && (dinosaur.getControllingPassenger() != null
                || dinosaur.isSpinosaurusSwimming() || dinosaur.isSpinosaurusBreaching());
        boolean groundedPteranodonRider = profile.assetName().equals("pteranodon")
                && dinosaur.getControllingPassenger() != null
                && !mountedFlight;
        Vec3 movement = dinosaur.getDeltaMovement();
        boolean moving = movement.horizontalDistanceSqr() > 0.00045D || renderState.walkAnimationSpeed > 0.035F;
        boolean canObserve = dinosaur.getWorkAction() == 0
                && !dinosaur.isDinosaurSleeping()
                && renderState.walkAnimationSpeed < 0.08F;
        float travelTurn = 0.0F;
        if (moving && movement.horizontalDistanceSqr() > 0.0001D) {
            float travelYaw = (float)(Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
            travelTurn = Mth.clamp(Mth.wrapDegrees(travelYaw - renderState.bodyRot) * 0.78F, -40.0F, 40.0F);
        }
        float targetYaw = mountedFlight || groundedPteranodonRider ? 0.0F : trackingTarget
                ? Mth.clamp(renderState.yRot, -52.0F, 52.0F)
                : moving && Math.abs(travelTurn) > 1.0F
                        ? travelTurn
                        : canObserve ? Mth.clamp(renderState.yRot, -52.0F, 52.0F) : 0.0F;
        float targetPitch = groundedPteranodonRider ? 28.0F : trackingTarget || canObserve && !moving
                ? Mth.clamp(renderState.xRot, -28.0F, 28.0F)
                : 0.0F;
        float leftFootTarget = profile.assetName().equals("t_rex")
                ? sampleGroundOffset(dinosaur, renderState.bodyRot, -1.0F)
                : 0.0F;
        float rightFootTarget = profile.assetName().equals("t_rex")
                ? sampleGroundOffset(dinosaur, renderState.bodyRot, 1.0F)
                : 0.0F;
        dinosaur.updateRenderedProceduralMotion(
                targetYaw,
                targetPitch,
                renderState.bodyRot,
                renderState.getAnimatableAge(),
                moving,
                trackingTarget,
                leftFootTarget,
                rightFootTarget
        );
        renderState.addGeckolibData(HEAD_YAW, dinosaur.getRenderedHeadYaw());
        renderState.addGeckolibData(HEAD_PITCH, dinosaur.getRenderedHeadPitch());
        renderState.addGeckolibData(BODY_LEAD, mountedFlight ? 0.0F : dinosaur.getRenderedBodyLead());
        renderState.addGeckolibData(TAIL_YAW, dinosaur.getRenderedTailYaw());
        renderState.addGeckolibData(TAIL_TIP_YAW, dinosaur.getRenderedTailTipYaw());
        float stepCycle = Mth.sin((float)renderState.getAnimatableAge() * 0.52F);
        renderState.addGeckolibData(TURN_STEP, dinosaur.getRenderedTurnStep() * stepCycle);
        renderState.addGeckolibData(LEFT_FOOT_OFFSET, dinosaur.getRenderedLeftFootOffset());
        renderState.addGeckolibData(RIGHT_FOOT_OFFSET, dinosaur.getRenderedRightFootOffset());
        float animationAge = (float)renderState.getAnimatableAge();
        float flightSwayPitch = dinosaur.getPteranodonRideSwayPitch(animationAge);
        float flightSwayRoll = dinosaur.getPteranodonRideSwayRoll(animationAge);
        renderState.addGeckolibData(FLIGHT_PITCH, mountedFlight
                ? Mth.lerp(partialTick, dinosaur.xRotO, dinosaur.getXRot()) - flightSwayPitch
                : spinosaurusMotion ? Mth.lerp(partialTick, dinosaur.xRotO, dinosaur.getXRot()) : 0.0F);
        renderState.addGeckolibData(FLIGHT_ROLL, mountedFlight
                ? Mth.clamp(dinosaur.getPteranodonBankDegrees(partialTick)
                        + flightSwayRoll, -38.0F, 38.0F)
                : spinosaurusMotion ? dinosaur.getSpinosaurusBankDegrees(partialTick) : 0.0F);
        boolean groundedPteranodonIdle = profile.assetName().equals("pteranodon")
                && !mountedFlight
                && !moving
                && dinosaur.getWorkAction() == 0
                && !dinosaur.isDinosaurSleeping();
        renderState.addGeckolibData(IDLE_BODY_BOB, groundedPteranodonIdle
                ? Mth.sin(animationAge * 0.105F) * 0.34F : 0.0F);
        renderState.addGeckolibData(IDLE_BODY_ROLL, groundedPteranodonIdle
                ? (Mth.sin(animationAge * 0.071F) + Mth.sin(animationAge * 0.029F) * 0.45F) * 0.72F
                : 0.0F);
        renderState.addGeckolibData(FieldDodoModel.EYES_CLOSED, dinosaur.areEyesClosed());
        renderState.addGeckolibData(FieldDodoModel.SADDLED, dinosaur.isSaddledMount());
        renderState.addGeckolibData(FieldDodoModel.AQUATIC_MOUNT,
                dinosaur.getSpecies() == com.primevalworks.world.entity.DinosaurSpecies.SPINOSAURUS
                        && dinosaur.isSaddledMount()
                        && (dinosaur.isSpinosaurusSwimming() || dinosaur.isSpinosaurusBreaching()));
        renderState.addGeckolibData(FieldDodoModel.ALBINO, dinosaur.usesAlbinoAppearance());
        float defeatProgress = dinosaur.getDefeatTransferProgress(partialTick);
        Vec3 defeatOffset = Vec3.ZERO;
        if (defeatProgress > 0.0F && dinosaur.getCommandTablePos().isPresent()) {
            renderState.lightCoords = FULL_BRIGHT_LIGHT;
            float transfer = smoothStep(Mth.clamp((defeatProgress - 0.24F) / 0.76F, 0.0F, 1.0F));
            float eased = transfer * transfer * (3.0F - 2.0F * transfer);
            Vec3 target = dinosaur.getCommandTablePos().orElseThrow().getCenter().add(0.0D, 0.92D, 0.0D);
            defeatOffset = target.subtract(dinosaur.position()).scale(eased)
                    .add(0.0D, Mth.sin(transfer * Mth.PI) * 1.85D, 0.0D);
        }
        renderState.addGeckolibData(DEFEAT_PROGRESS, defeatProgress);
        renderState.addGeckolibData(DEFEAT_OFFSET, defeatOffset);
    }

    @Override
    public void preRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        if (!renderPassInfo.renderState().getOrDefaultGeckolibData(TRACK_SPINO_RIDER, false)) return;
        int entityId = renderPassInfo.renderState().getOrDefaultGeckolibData(ENTITY_ID, -1);
        boolean aquatic = renderPassInfo.renderState().getOrDefaultGeckolibData(SPINO_RIDER_AQUATIC, false);
        Vec3 entityOrigin = renderPassInfo.renderState()
                .getOrDefaultGeckolibData(DataTickets.POSITION, Vec3.ZERO);
        renderPassInfo.addBonePositionListener("whereplayersits", (worldPos, modelPos, localPos) -> {
            if (worldPos != null) {
                SpinosaurusRiderAttachment.update(entityId, worldPos.subtract(entityOrigin), aquatic);
            }
        });
    }

    @Override
    public RenderType getRenderType(R renderState, Identifier texture) {
        if (renderState.getOrDefaultGeckolibData(DEFEAT_PROGRESS, 0.0F) > 0.0F
                && !renderState.isInvisible) {
            return RenderTypes.entityTranslucentEmissive(texture);
        }
        if ((profile.assetName().equals("pteranodon")
                 || profile.assetName().equals("parasaurolophus")
                 || profile.assetName().equals("tyrannosaurus")
                 || profile.assetName().equals("spino")) && !renderState.isInvisible) {
            return RenderTypes.entityCutoutCull(texture);
        }
        return super.getRenderType(renderState, texture);
    }

    @Override
    protected void applyRotations(RenderPassInfo<R> renderPassInfo,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float rotationYaw) {
        super.applyRotations(renderPassInfo, poseStack, rotationYaw);
        if (!profile.assetName().equals("pteranodon") && !profile.assetName().equals("spino")) {
            return;
        }
        float pitch = renderPassInfo.getOrDefaultGeckolibData(FLIGHT_PITCH, 0.0F);
        float roll = renderPassInfo.getOrDefaultGeckolibData(FLIGHT_ROLL, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    @Override
    public int getRenderColor(FieldDodoEntity dinosaur, Void relatedObject, float partialTick) {
        int base = super.getRenderColor(dinosaur, relatedObject, partialTick);
        if (!dinosaur.usesAlbinoAppearance()) {
            float amount = dinosaur.getHueVariant() / 8.0F;
            int tint = amount >= 0.0F
                    ? ARGB.colorFromFloat(1.0F, 1.0F, 1.0F - amount * 0.035F, 1.0F - amount * 0.065F)
                    : ARGB.colorFromFloat(1.0F, 1.0F + amount * 0.055F, 1.0F + amount * 0.018F, 1.0F);
            base = ARGB.multiply(base, tint);
        }
        float progress = dinosaur.getDefeatTransferProgress(partialTick);
        if (progress <= 0.0F) return base;
        float fade = smoothStep(Mth.clamp((progress - 0.52F) / 0.48F, 0.0F, 1.0F));
        float red = smoothStep(Mth.clamp((progress - 0.03F) / 0.45F, 0.0F, 1.0F)) * 0.72F;
        int alpha = Mth.clamp(Math.round(ARGB.alpha(base) * (1.0F - fade * 0.94F)), 0, 255);
        int resultRed = Math.round(Mth.lerp(red, ARGB.red(base), 236.0F));
        int resultGreen = Math.round(Mth.lerp(red, ARGB.green(base), 54.0F));
        int resultBlue = Math.round(Mth.lerp(red, ARGB.blue(base), 48.0F));
        return ARGB.color(alpha, resultRed, resultGreen, resultBlue);
    }

    @Override
    public Vec3 getRenderOffset(R renderState) {
        Vec3 inherited = super.getRenderOffset(renderState);
        Vec3 offset = renderState.getOrDefaultGeckolibData(DEFEAT_OFFSET, Vec3.ZERO);
        if (profile.modelGroundOffset() != 0.0F) {
            offset = offset.add(0.0D, -profile.modelGroundOffset() * renderState.scale, 0.0D);
        }
        return inherited.add(offset);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float widthScale, float heightScale) {
        float progress = renderPassInfo.getOrDefaultGeckolibData(DEFEAT_PROGRESS, 0.0F);
        if (progress <= 0.0F) {
            super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
            return;
        }
        float snapPhase = Mth.clamp(progress / 0.16F, 0.0F, 1.0F);
        float snap = backOut(snapPhase);
        float small = Mth.lerp(snap, 1.0F, 0.31F);
        float settle = Mth.clamp((progress - 0.12F) / 0.30F, 0.0F, 1.0F);
        float wobble = Mth.sin(settle * Mth.PI * 3.0F)
                * (1.0F - smoothStep(settle)) * 0.105F;
        float transfer = smoothStep(Mth.clamp((progress - 0.24F) / 0.76F, 0.0F, 1.0F));
        float collapse = Math.max(0.045F, (small + wobble) * (1.0F - transfer * transfer * 0.86F));
        float horizontal = collapse * (1.0F + wobble * 0.42F);
        float vertical = collapse * (1.0F - wobble * 0.64F);
        super.scaleModelForRender(renderPassInfo, widthScale * horizontal, heightScale * vertical);
    }

    @Override
    protected int getBlockLightLevel(FieldDodoEntity dinosaur, BlockPos pos) {
        int sampled = super.getBlockLightLevel(dinosaur, pos);
        return dinosaur.tickCount < 4 ? Math.max(12, sampled) : sampled;
    }

    @Override
    protected int getSkyLightLevel(FieldDodoEntity dinosaur, BlockPos pos) {
        int sampled = super.getSkyLightLevel(dinosaur, pos);
        return dinosaur.tickCount < 4 ? Math.max(12, sampled) : sampled;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        float yaw = renderPassInfo.getOrDefaultGeckolibData(HEAD_YAW, 0.0F);
        float pitch = renderPassInfo.getOrDefaultGeckolibData(HEAD_PITCH, 0.0F);
        snapshots.ifPresent(profile.headBone(), bone -> {
            bone.setRotX(bone.getRotX() - pitch * Mth.DEG_TO_RAD);
            bone.setRotY(bone.getRotY() - yaw * Mth.DEG_TO_RAD);
        });
        float bodyLead = renderPassInfo.getOrDefaultGeckolibData(BODY_LEAD, 0.0F);
        float tailYaw = renderPassInfo.getOrDefaultGeckolibData(TAIL_YAW, 0.0F);
        float tailTipYaw = renderPassInfo.getOrDefaultGeckolibData(TAIL_TIP_YAW, 0.0F);
        float turnStep = renderPassInfo.getOrDefaultGeckolibData(TURN_STEP, 0.0F);
        if (!profile.assetName().equals("t_rex")) {
            boolean authoredCapitalBones = profile.assetName().equals("dodo")
                    || profile.assetName().equals("stegosaurus")
                    || profile.assetName().equals("pteranodon");
            String bodyBone = authoredCapitalBones ? "Body" : "body";
            String tailBone = profile.assetName().equals("dodo") ? "Tail"
                    : profile.assetName().equals("stegosaurus") ? "Tail" : "tail";
            String rightLegBone = profile.assetName().equals("dodo") ? "Rightleg"
                    : profile.assetName().equals("stegosaurus") ? "Frontrightleg"
                    : profile.assetName().equals("pteranodon") ? "Rightleg"
                    : profile.assetName().equals("spino") ? "rightleg" : "leg_right";
            String leftLegBone = profile.assetName().equals("dodo") ? "Leftleg"
                    : profile.assetName().equals("stegosaurus") ? "Frontleftleg"
                    : profile.assetName().equals("pteranodon") ? "Leftleg"
                    : profile.assetName().equals("spino") ? "leftleg" : "leg_left";
            snapshots.ifPresent(bodyBone, bone -> {
                bone.setRotY(bone.getRotY() - bodyLead * 0.72F * Mth.DEG_TO_RAD);
                bone.setRotZ(bone.getRotZ() + bodyLead * 0.08F * Mth.DEG_TO_RAD);
            });
            snapshots.ifPresent(tailBone, bone -> bone.setRotY(bone.getRotY() + tailYaw * 0.62F * Mth.DEG_TO_RAD));
            snapshots.ifPresent(rightLegBone, bone -> bone.setRotX(bone.getRotX() + turnStep * 8.0F * Mth.DEG_TO_RAD));
            snapshots.ifPresent(leftLegBone, bone -> bone.setRotX(bone.getRotX() - turnStep * 8.0F * Mth.DEG_TO_RAD));
            if (profile.assetName().equals("pteranodon")) {
                float idleBob = renderPassInfo.getOrDefaultGeckolibData(IDLE_BODY_BOB, 0.0F);
                float idleRoll = renderPassInfo.getOrDefaultGeckolibData(IDLE_BODY_ROLL, 0.0F);
                snapshots.ifPresent("All", bone -> bone.setTranslateY(bone.getTranslateY() + idleBob));
                snapshots.ifPresent("Body", bone -> bone.setRotZ(
                        bone.getRotZ() + idleRoll * Mth.DEG_TO_RAD
                ));
            }
            return;
        }

        float leftFootOffset = renderPassInfo.getOrDefaultGeckolibData(LEFT_FOOT_OFFSET, 0.0F);
        float rightFootOffset = renderPassInfo.getOrDefaultGeckolibData(RIGHT_FOOT_OFFSET, 0.0F);
        snapshots.ifPresent("body", bone -> {
            bone.setRotY(bone.getRotY() - bodyLead * Mth.DEG_TO_RAD);
            bone.setRotZ(bone.getRotZ() + bodyLead * 0.14F * Mth.DEG_TO_RAD);
        });
        snapshots.ifPresent("tail", bone -> bone.setRotY(bone.getRotY() + tailYaw * Mth.DEG_TO_RAD));
        snapshots.ifPresent("segment2", bone -> bone.setRotY(bone.getRotY() + tailTipYaw * Mth.DEG_TO_RAD));
        snapshots.ifPresent("rightleg", bone -> {
            bone.setRotX(bone.getRotX() + turnStep * 9.0F * Mth.DEG_TO_RAD);
            bone.setTranslateY(bone.getTranslateY() + rightFootOffset * 16.0F);
        });
        snapshots.ifPresent("leftleg", bone -> {
            bone.setRotX(bone.getRotX() - turnStep * 9.0F * Mth.DEG_TO_RAD);
            bone.setTranslateY(bone.getTranslateY() + leftFootOffset * 16.0F);
        });
    }

    private static float sampleGroundOffset(FieldDodoEntity dinosaur, float bodyYaw, float side) {
        float radians = bodyYaw * Mth.DEG_TO_RAD;
        double lateral = dinosaur.getBbWidth() * 0.29D * side;
        double forward = dinosaur.getBbWidth() * 0.07D;
        double sampleX = dinosaur.getX() + lateral * Mth.cos(radians) - forward * Mth.sin(radians);
        double sampleZ = dinosaur.getZ() + lateral * Mth.sin(radians) + forward * Mth.cos(radians);
        Vec3 from = new Vec3(sampleX, dinosaur.getY() + 0.75D, sampleZ);
        Vec3 to = new Vec3(sampleX, dinosaur.getY() - 0.72D, sampleZ);
        var hit = dinosaur.level().clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dinosaur
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return 0.0F;
        }
        return Mth.clamp((float)(hit.getLocation().y - dinosaur.getY()), -0.58F, 0.38F);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float backOut(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F) - 1.0F;
        return 1.0F + 2.70158F * t * t * t + 1.70158F * t * t;
    }
}
