package com.primevalworks.client.render.block;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.primevalworks.client.model.block.LaserTurretGeoModel;
import com.primevalworks.world.block.BeamLineOfSight;
import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LaserTurretRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<LaserTurretBlockEntity, R> {
    private static final DataTicket<Float> AIM_YAW =
            DataTicket.create("primevalworks_laser_turret_yaw", Float.class);
    private static final DataTicket<Float> AIM_PITCH =
            DataTicket.create("primevalworks_laser_turret_pitch", Float.class);
    private static final DataTicket<Float> BEAM_LENGTH =
            DataTicket.create("primevalworks_laser_turret_beam_length", Float.class);
    private static final DataTicket<Float> ANIMATION_TIME =
            DataTicket.create("primevalworks_laser_turret_animation_time", Float.class);
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/white_concrete.png");
    private static final int[] BEAM_COLORS = {
            0x59A20E12,
            0x86E3262E,
            0xB6FF5A54,
            0xDCFFD0C8,
            0xFFFFFFFF
    };
    private static final float[] BEAM_WIDTHS = {0.112F, 0.082F, 0.056F, 0.032F, 0.013F};
    private static final UvRegion FULL_UV = new UvRegion(0.0F, 0.0F, 1.0F, 1.0F);

    public LaserTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new LaserTurretGeoModel());
    }

    @Override
    public void addRenderData(LaserTurretBlockEntity turret, Void relatedObject,
                              R renderState, float partialTick) {
        float yaw = turret.aimYaw(partialTick);
        float pitch = turret.aimPitch(partialTick);
        LivingEntity target = turret.getLevel() == null ? null : turret.renderTarget(turret.getLevel());
        renderState.addGeckolibData(AIM_YAW, yaw);
        renderState.addGeckolibData(AIM_PITCH, pitch);
        renderState.addGeckolibData(LaserTurretGeoModel.FIRING, target != null);
        renderState.addGeckolibData(ANIMATION_TIME,
                turret.getLevel() == null ? partialTick : turret.getLevel().getGameTime() + partialTick);
        renderState.addGeckolibData(BEAM_LENGTH,
                target == null ? 0.0F : visibleBeamLength(turret, target, yaw, pitch, partialTick));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        float yaw = renderPassInfo.getOrDefaultGeckolibData(AIM_YAW, 0.0F);
        float pitch = renderPassInfo.getOrDefaultGeckolibData(AIM_PITCH, 0.0F);
        snapshots.ifPresent("head", bone -> {
            bone.setRotY(bone.getRotY() - yaw * Mth.DEG_TO_RAD);
            bone.setRotZ(bone.getRotZ() - pitch * Mth.DEG_TO_RAD);
        });
    }

    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        float length = renderPassInfo.getOrDefaultGeckolibData(BEAM_LENGTH, 0.0F);
        if (length <= 0.02F) return;

        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        poseStack.translate(0.5F, LaserTurretBlockEntity.PIVOT_HEIGHT, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                renderPassInfo.getOrDefaultGeckolibData(AIM_YAW, 0.0F)));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                renderPassInfo.getOrDefaultGeckolibData(AIM_PITCH, 0.0F)));
        renderBeam(renderPassInfo, poseStack, renderTasks, length);
        poseStack.popPose();
    }

    private static float visibleBeamLength(LaserTurretBlockEntity turret, LivingEntity target,
                                           float yaw, float pitch, float partialTick) {
        Vec3 pivot = Vec3.atLowerCornerOf(turret.getBlockPos()).add(
                0.5D, LaserTurretBlockEntity.PIVOT_HEIGHT, 0.5D);
        Vec3 targetEye = target.getEyePosition(partialTick);
        double targetDistance = targetEye.distanceTo(pivot);
        double yawRadians = yaw * Mth.DEG_TO_RAD;
        double pitchRadians = pitch * Mth.DEG_TO_RAD;
        double horizontal = Math.cos(pitchRadians);
        Vec3 renderedEnd = pivot.add(
                -Math.sin(yawRadians) * horizontal * targetDistance,
                Math.sin(pitchRadians) * targetDistance,
                -Math.cos(yawRadians) * horizontal * targetDistance
        );
        Vec3 sightStart = BeamLineOfSight.justOutside(turret.getBlockPos(), pivot, renderedEnd);
        double visibleDistance = pivot.distanceTo(sightStart)
                + BeamLineOfSight.visibleDistance(turret.getLevel(), sightStart, renderedEnd);
        return (float)Math.max(0.0D,
                Math.min(targetDistance, visibleDistance) - LaserTurretBlockEntity.MUZZLE_OFFSET);
    }

    private static void renderBeam(RenderPassInfo<?> state, PoseStack poseStack,
                                   SubmitNodeCollector submits, float beamLength) {
        for (int layer = BEAM_WIDTHS.length - 1; layer >= 0; layer--) {
            float width = BEAM_WIDTHS[layer];
            int color = BEAM_COLORS[layer];
            submits.order(4 + (BEAM_WIDTHS.length - 1 - layer)).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                    (matrix, vertices) -> renderSquarePrism(
                            matrix, vertices, width,
                            -LaserTurretBlockEntity.MUZZLE_OFFSET,
                            -LaserTurretBlockEntity.MUZZLE_OFFSET - beamLength,
                            color)
            );
        }

        float spacing = 0.78F;
        float phase = Mth.positiveModulo(
                state.getOrDefaultGeckolibData(ANIMATION_TIME, 0.0F) * 0.23F, spacing);
        submits.order(10).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                (matrix, vertices) -> {
                    for (float travelled = phase; travelled < beamLength; travelled += spacing) {
                        float segmentStart = -LaserTurretBlockEntity.MUZZLE_OFFSET - travelled;
                        float segmentEnd = Math.max(
                                -LaserTurretBlockEntity.MUZZLE_OFFSET - beamLength,
                                segmentStart - 0.11F);
                        renderSquareCollar(matrix, vertices, 0.118F, 0.023F,
                                segmentStart, segmentEnd, 0xD9E9D5FF);
                    }
                }
        );
    }

    private static void renderSquarePrism(PoseStack.Pose matrix, VertexConsumer vertices,
                                          float halfWidth, float startZ, float endZ, int color) {
        renderCuboid(matrix, vertices,
                -halfWidth, -halfWidth, Math.min(startZ, endZ),
                halfWidth, halfWidth, Math.max(startZ, endZ), color, FULL_UV);
    }

    private static void renderSquareCollar(PoseStack.Pose matrix, VertexConsumer vertices,
                                           float halfWidth, float thickness,
                                           float startZ, float endZ, int color) {
        float inner = halfWidth - thickness;
        float minZ = Math.min(startZ, endZ);
        float maxZ = Math.max(startZ, endZ);
        renderCuboid(matrix, vertices, -halfWidth, inner, minZ, halfWidth, halfWidth, maxZ, color, FULL_UV);
        renderCuboid(matrix, vertices, -halfWidth, -halfWidth, minZ, halfWidth, -inner, maxZ, color, FULL_UV);
        renderCuboid(matrix, vertices, -halfWidth, -inner, minZ, -inner, inner, maxZ, color, FULL_UV);
        renderCuboid(matrix, vertices, inner, -inner, minZ, halfWidth, inner, maxZ, color, FULL_UV);
    }

    private static void renderCuboid(PoseStack.Pose matrix, VertexConsumer vertices,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ, int color, UvRegion uv) {
        quad(vertices, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                color, uv, 0.0F, 0.0F, -1.0F);
        quad(vertices, matrix, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                color, uv, 0.0F, 0.0F, 1.0F);
        quad(vertices, matrix, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ,
                color, uv, -1.0F, 0.0F, 0.0F);
        quad(vertices, matrix, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                color, uv, 1.0F, 0.0F, 0.0F);
        quad(vertices, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                color, uv, 0.0F, 1.0F, 0.0F);
        quad(vertices, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ,
                color, uv, 0.0F, -1.0F, 0.0F);
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose matrix,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             int color, UvRegion uv, float normalX, float normalY, float normalZ) {
        vertex(vertices, matrix, x0, y0, z0, color, uv.minU, uv.maxV, normalX, normalY, normalZ);
        vertex(vertices, matrix, x1, y1, z1, color, uv.maxU, uv.maxV, normalX, normalY, normalZ);
        vertex(vertices, matrix, x2, y2, z2, color, uv.maxU, uv.minV, normalX, normalY, normalZ);
        vertex(vertices, matrix, x3, y3, z3, color, uv.minU, uv.minV, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose matrix,
                               float x, float y, float z, int color, float u, float v,
                               float normalX, float normalY, float normalZ) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(normalX, normalY, normalZ);
    }

    @Override
    public AABB getRenderBoundingBox(LaserTurretBlockEntity turret) {
        BlockPos pos = turret.getBlockPos();
        return new AABB(pos).inflate(25.0D);
    }

    private record UvRegion(float minU, float minV, float maxU, float maxV) {
    }
}
