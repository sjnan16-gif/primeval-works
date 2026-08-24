package com.primevalworks.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import com.primevalworks.world.block.entity.TargetingTurret;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractTurretRenderer<T extends BlockEntity & TargetingTurret>
        implements BlockEntityRenderer<T, TurretRenderState> {
    private static final Identifier DART_METAL = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/copper_block.png");
    private static final Identifier LASER_METAL = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/iron_block.png");
    private static final Identifier LASER_ENERGY = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/redstone_block.png");
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/white_concrete.png");
    private static final int[] BEAM_COLORS = {
            0x62660018,
            0x86FF1238,
            0xA8FF5B24,
            0xD8FFC857,
            0xFFFFFFFF
    };
    private static final float[] BEAM_WIDTHS = {0.120F, 0.091F, 0.064F, 0.039F, 0.017F};
    private static final float PIVOT_HEIGHT = 0.75F;

    protected AbstractTurretRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TurretRenderState createRenderState() {
        return new TurretRenderState();
    }

    @Override
    public void extractRenderState(
            T turret,
            TurretRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(turret, state, partialTick, cameraPosition, breakProgress);
        state.laser = turret instanceof LaserTurretBlockEntity;
        state.yaw = turret.aimYaw(partialTick);
        state.pitch = turret.aimPitch(partialTick);
        state.animationTime = turret.getLevel() == null ? partialTick : turret.getLevel().getGameTime() + partialTick;
        LivingEntity target = turret.getLevel() == null ? null : turret.renderTarget(turret.getLevel());
        state.hasTarget = target != null;
        if (target != null) {
            Vec3 pivot = Vec3.atLowerCornerOf(turret.getBlockPos()).add(0.5D, PIVOT_HEIGHT, 0.5D);
            state.beamLength = (float)Math.max(0.0D, target.getEyePosition(partialTick).distanceTo(pivot) - 0.48D);
        } else {
            state.beamLength = 0.0F;
        }
    }

    @Override
    public void submit(
            TurretRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submits,
            net.minecraft.client.renderer.state.level.CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_HEIGHT, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));

        Identifier metal = state.laser ? LASER_METAL : DART_METAL;
        int metalTint = state.laser ? 0xFFFFFFFF : 0xFFFFBE75;
        submits.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(metal),
                (matrix, vertices) -> renderCuboid(matrix, vertices,
                        -0.3125F, -0.1875F, -0.3125F,
                        0.3125F, 0.1875F, 0.3125F, metalTint));

        Identifier barrelTexture = state.laser ? LASER_ENERGY : metal;
        int barrelTint = state.laser ? 0xFFFF435B : 0xFFFFD19A;
        submits.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(barrelTexture),
                (matrix, vertices) -> renderCuboid(matrix, vertices,
                        -0.125F, -0.0625F, -0.50F,
                        0.125F, 0.125F, -0.25F, barrelTint));

        if (state.laser && state.hasTarget && state.beamLength > 0.02F) {
            renderLaserBeam(state, poseStack, submits);
        }
        poseStack.popPose();
    }

    private static void renderLaserBeam(TurretRenderState state, PoseStack poseStack, SubmitNodeCollector submits) {
        // Submit the opaque core first, then the translucent shells. Each layer is a
        // closed square prism, so the beam keeps a real volume from every camera angle.
        for (int layer = BEAM_WIDTHS.length - 1; layer >= 0; layer--) {
            float width = BEAM_WIDTHS[layer];
            int color = BEAM_COLORS[layer];
            submits.order(4 + (BEAM_WIDTHS.length - 1 - layer)).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                    (matrix, vertices) -> renderSquarePrism(
                            matrix, vertices, width, -0.48F, -0.48F - state.beamLength, color)
            );
        }

        float spacing = 0.78F;
        float phase = Mth.positiveModulo(state.animationTime * 0.23F, spacing);
        submits.order(10).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                (matrix, vertices) -> {
                    for (float travelled = phase; travelled < state.beamLength; travelled += spacing) {
                        float segmentStart = -0.48F - travelled;
                        float segmentEnd = Math.max(-0.48F - state.beamLength, segmentStart - 0.13F);
                        renderSquareCollar(matrix, vertices, 0.132F, 0.026F,
                                segmentStart, segmentEnd, 0xE8FFF2C1);
                    }
                }
        );
    }

    private static void renderSquarePrism(PoseStack.Pose matrix, VertexConsumer vertices,
                                          float halfWidth, float startZ, float endZ, int color) {
        renderCuboid(matrix, vertices,
                -halfWidth, -halfWidth, Math.min(startZ, endZ),
                halfWidth, halfWidth, Math.max(startZ, endZ), color);
    }

    private static void renderSquareCollar(PoseStack.Pose matrix, VertexConsumer vertices,
                                           float halfWidth, float thickness,
                                           float startZ, float endZ, int color) {
        float inner = halfWidth - thickness;
        float minZ = Math.min(startZ, endZ);
        float maxZ = Math.max(startZ, endZ);
        renderCuboid(matrix, vertices, -halfWidth, inner, minZ, halfWidth, halfWidth, maxZ, color);
        renderCuboid(matrix, vertices, -halfWidth, -halfWidth, minZ, halfWidth, -inner, maxZ, color);
        renderCuboid(matrix, vertices, -halfWidth, -inner, minZ, -inner, inner, maxZ, color);
        renderCuboid(matrix, vertices, inner, -inner, minZ, halfWidth, inner, maxZ, color);
    }

    private static void renderCuboid(PoseStack.Pose matrix, VertexConsumer vertices,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ, int color) {
        quad(vertices, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                color, 0.0F, 0.0F, -1.0F);
        quad(vertices, matrix, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                color, 0.0F, 0.0F, 1.0F);
        quad(vertices, matrix, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ,
                color, -1.0F, 0.0F, 0.0F);
        quad(vertices, matrix, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                color, 1.0F, 0.0F, 0.0F);
        quad(vertices, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                color, 0.0F, 1.0F, 0.0F);
        quad(vertices, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ,
                color, 0.0F, -1.0F, 0.0F);
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose matrix,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int color, float normalX, float normalY, float normalZ) {
        vertex(vertices, matrix, x0, y0, z0, color, 0.0F, 1.0F, normalX, normalY, normalZ);
        vertex(vertices, matrix, x1, y1, z1, color, 1.0F, 1.0F, normalX, normalY, normalZ);
        vertex(vertices, matrix, x2, y2, z2, color, 1.0F, 0.0F, normalX, normalY, normalZ);
        vertex(vertices, matrix, x3, y3, z3, color, 0.0F, 0.0F, normalX, normalY, normalZ);
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
    public AABB getRenderBoundingBox(T turret) {
        BlockPos pos = turret.getBlockPos();
        return turret instanceof LaserTurretBlockEntity
                ? new AABB(pos).inflate(25.0D)
                : new AABB(pos).inflate(1.0D);
    }
}
