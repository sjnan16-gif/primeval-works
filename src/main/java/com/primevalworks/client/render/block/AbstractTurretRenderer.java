package com.primevalworks.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractTurretRenderer<T extends BlockEntity & TargetingTurret>
        implements BlockEntityRenderer<T, TurretRenderState> {
    private static final Identifier DART_METAL = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/copper_block.png");
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
        state.yaw = turret.aimYaw(partialTick);
        state.pitch = turret.aimPitch(partialTick);
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

        renderDartHead(poseStack, submits);
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
        return new AABB(pos).inflate(1.0D);
    }

    private record UvRegion(float minU, float minV, float maxU, float maxV) {
    }
}
