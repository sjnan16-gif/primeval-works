package com.primevalworks.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.primevalworks.world.block.entity.MagicTurretBlockEntity;
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
    private static final Identifier MAGIC_METAL = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/iron_block.png");
    private static final Identifier MAGIC_ENERGY = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/amethyst_block.png");
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/white_concrete.png");
    private static final int[] BEAM_COLORS = {
            0x59501782,
            0x867B2BC4,
            0xB6AD66F0,
            0xDCE3C8FF,
            0xFFFFFFFF
    };
    private static final float[] BEAM_WIDTHS = {0.112F, 0.082F, 0.056F, 0.032F, 0.013F};
    private static final float PIVOT_HEIGHT = 0.75F;
    private static final UvRegion FULL_UV = new UvRegion(0.0F, 0.0F, 1.0F, 1.0F);

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
        state.magic = turret instanceof MagicTurretBlockEntity;
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

        if (state.magic) {
            renderMagicHead(poseStack, submits);
        } else {
            renderDartHead(poseStack, submits);
        }

        if (state.magic && state.hasTarget && state.beamLength > 0.02F) {
            renderMagicBeam(state, poseStack, submits);
        }
        poseStack.popPose();
    }

    private static void renderDartHead(PoseStack poseStack, SubmitNodeCollector submits) {
        submits.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(DART_METAL),
                (matrix, vertices) -> {
                    renderCuboid(matrix, vertices,
                            -0.3125F, -0.1875F, -0.3125F,
                            0.3125F, 0.1875F, 0.3125F, 0xFFFFBE75, FULL_UV);
                    renderCuboid(matrix, vertices,
                            -0.125F, -0.0625F, -0.50F,
                            0.125F, 0.125F, -0.25F, 0xFFFFD19A, FULL_UV);
                });
    }

    private static void renderMagicHead(PoseStack poseStack, SubmitNodeCollector submits) {
        submits.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(MAGIC_METAL),
                (matrix, vertices) -> renderCuboid(matrix, vertices,
                        -0.3125F, -0.1875F, -0.3125F,
                        0.3125F, 0.1875F, 0.3125F, 0xFFFFFFFF, FULL_UV));
        submits.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(MAGIC_ENERGY),
                (matrix, vertices) -> renderCuboid(matrix, vertices,
                        -0.125F, -0.0625F, -0.50F,
                        0.125F, 0.125F, -0.25F, 0xFFFFFFFF, FULL_UV));
    }

    private static void renderMagicBeam(TurretRenderState state, PoseStack poseStack, SubmitNodeCollector submits) {
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
                        float segmentEnd = Math.max(-0.48F - state.beamLength, segmentStart - 0.11F);
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
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
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
    public AABB getRenderBoundingBox(T turret) {
        BlockPos pos = turret.getBlockPos();
        return turret instanceof MagicTurretBlockEntity
                ? new AABB(pos).inflate(25.0D)
                : new AABB(pos).inflate(1.0D);
    }

    private record UvRegion(float minU, float minV, float maxU, float maxV) {
    }
}
