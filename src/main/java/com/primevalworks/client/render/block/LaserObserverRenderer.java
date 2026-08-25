package com.primevalworks.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.primevalworks.world.block.BeamLineOfSight;
import com.primevalworks.world.block.PoweredObserverBlock;
import com.primevalworks.world.block.entity.LaserObserverBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class LaserObserverRenderer
        implements BlockEntityRenderer<LaserObserverBlockEntity, LaserObserverRenderState> {
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/block/white_concrete.png");
    private static final float START_DISTANCE = 0.505F;
    private static final float END_DISTANCE = PoweredObserverBlock.DETECTION_RANGE + 0.50F;

    public LaserObserverRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public LaserObserverRenderState createRenderState() {
        return new LaserObserverRenderState();
    }

    @Override
    public void extractRenderState(
            LaserObserverBlockEntity observer,
            LaserObserverRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(observer, state, partialTick, cameraPosition, breakProgress);
        if (observer.getBlockState().hasProperty(PoweredObserverBlock.FACING)) {
            state.facing = observer.getBlockState().getValue(PoweredObserverBlock.FACING);
        }
        state.animationTime = observer.getLevel() == null
                ? partialTick
                : observer.getLevel().getGameTime() + partialTick;
        state.endDistance = observer.getLevel() == null
                ? END_DISTANCE
                : BeamLineOfSight.visibleAxisDistance(
                        observer.getLevel(), observer.getBlockPos(), state.facing, START_DISTANCE, END_DISTANCE);
    }

    @Override
    public void submit(
            LaserObserverRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submits,
            net.minecraft.client.renderer.state.level.CameraRenderState camera
    ) {
        if (state.endDistance <= START_DISTANCE + 0.002F) return;
        BeamBox outer = beamBox(state.facing, 0.026F, START_DISTANCE, state.endDistance);
        BeamBox core = beamBox(state.facing, 0.009F, START_DISTANCE - 0.006F, state.endDistance);
        submits.order(1).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                (matrix, vertices) -> renderCuboid(matrix, vertices, outer, 0x66A70B19)
        );
        submits.order(2).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                (matrix, vertices) -> renderCuboid(matrix, vertices, core, 0xB8FF5661)
        );

        float spacing = 0.72F;
        float phase = Mth.positiveModulo(state.animationTime * 0.18F, spacing);
        submits.order(3).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE),
                (matrix, vertices) -> {
                    for (float distance = START_DISTANCE + phase; distance < state.endDistance; distance += spacing) {
                        float end = Math.min(state.endDistance, distance + 0.075F);
                        renderCuboid(matrix, vertices,
                                beamBox(state.facing, 0.015F, distance, end), 0xC8FFD1D5);
                    }
                }
        );
    }

    private static BeamBox beamBox(Direction facing, float halfWidth, float startDistance, float endDistance) {
        float startX = 0.5F + facing.getStepX() * startDistance;
        float startY = 0.5F + facing.getStepY() * startDistance;
        float startZ = 0.5F + facing.getStepZ() * startDistance;
        float endX = 0.5F + facing.getStepX() * endDistance;
        float endY = 0.5F + facing.getStepY() * endDistance;
        float endZ = 0.5F + facing.getStepZ() * endDistance;
        return switch (facing.getAxis()) {
            case X -> new BeamBox(
                    Math.min(startX, endX), 0.5F - halfWidth, 0.5F - halfWidth,
                    Math.max(startX, endX), 0.5F + halfWidth, 0.5F + halfWidth);
            case Y -> new BeamBox(
                    0.5F - halfWidth, Math.min(startY, endY), 0.5F - halfWidth,
                    0.5F + halfWidth, Math.max(startY, endY), 0.5F + halfWidth);
            case Z -> new BeamBox(
                    0.5F - halfWidth, 0.5F - halfWidth, Math.min(startZ, endZ),
                    0.5F + halfWidth, 0.5F + halfWidth, Math.max(startZ, endZ));
        };
    }

    private static void renderCuboid(PoseStack.Pose matrix, VertexConsumer vertices, BeamBox box, int color) {
        quad(vertices, matrix,
                box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ,
                color, 0.0F, 0.0F, -1.0F);
        quad(vertices, matrix,
                box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                color, 0.0F, 0.0F, 1.0F);
        quad(vertices, matrix,
                box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ,
                box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ,
                color, -1.0F, 0.0F, 0.0F);
        quad(vertices, matrix,
                box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ,
                color, 1.0F, 0.0F, 0.0F);
        quad(vertices, matrix,
                box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                color, 0.0F, 1.0F, 0.0F);
        quad(vertices, matrix,
                box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ,
                box.maxX, box.minY, box.minZ, box.minX, box.minY, box.minZ,
                color, 0.0F, -1.0F, 0.0F);
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack.Pose matrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int color,
            float normalX, float normalY, float normalZ
    ) {
        vertex(vertices, matrix, x0, y0, z0, color, 0.0F, 1.0F, normalX, normalY, normalZ);
        vertex(vertices, matrix, x1, y1, z1, color, 1.0F, 1.0F, normalX, normalY, normalZ);
        vertex(vertices, matrix, x2, y2, z2, color, 1.0F, 0.0F, normalX, normalY, normalZ);
        vertex(vertices, matrix, x3, y3, z3, color, 0.0F, 0.0F, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose matrix,
            float x, float y, float z,
            int color,
            float u, float v,
            float normalX, float normalY, float normalZ
    ) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(normalX, normalY, normalZ);
    }

    @Override
    public AABB getRenderBoundingBox(LaserObserverBlockEntity observer) {
        BlockPos pos = observer.getBlockPos();
        Direction facing = observer.getBlockState().hasProperty(PoweredObserverBlock.FACING)
                ? observer.getBlockState().getValue(PoweredObserverBlock.FACING)
                : Direction.NORTH;
        return new AABB(pos)
                .minmax(new AABB(pos.relative(facing, PoweredObserverBlock.DETECTION_RANGE)))
                .inflate(0.1D);
    }

    private record BeamBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    }
}
