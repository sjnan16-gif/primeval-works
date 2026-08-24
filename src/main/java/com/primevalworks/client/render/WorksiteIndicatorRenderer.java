package com.primevalworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorksiteIndicatorRenderer {
    private static final Identifier[] CLASSIC_WORK = workFrames("classic_work");
    private static final Identifier[] MACHINE_WORK = workFrames("machine_work");
    private static final Identifier[] WOODY_WORK = workFrames("woody_work");

    private WorksiteIndicatorRenderer() {
    }

    public static void submitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        SubmitNodeCollector submits = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        Map<BlockPos, IndicatorGroup> groups = new LinkedHashMap<>();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof FieldDodoEntity dodo) || dodo.getWorkAction() == 0) continue;
            BlockPos target = dodo.getWorkActionPos().orElse(null);
            if (target == null) continue;
            groups.computeIfAbsent(target.immutable(), ignored -> new IndicatorGroup(dodo.getWorkAction()))
                    .add(dodo);
        }
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Map.Entry<BlockPos, IndicatorGroup> entry : groups.entrySet()) {
            BlockPos target = entry.getKey();
            IndicatorGroup group = entry.getValue();
            AABB targetBounds = indicatorBounds(minecraft.level, target);
            Vec3 center = targetBounds.getCenter();
            double distance = camera.distanceTo(center);
            if (distance > 96.0D) continue;
            Identifier texture = workTexture(group.action, group.completion());
            RenderType renderType = RenderTypes.entityTranslucent(texture);
            float progress = group.maximumProgress + partialTick;
            float appear = spring(Math.min(1.0F, progress / 10.0F));
            float distanceScale = (float) Math.min(1.6D, Math.max(1.0D,
                    1.0D + Math.max(0.0D, distance - 8.0D) * 0.014D));
            float bob = (((minecraft.level.getGameTime() + (long)partialTick) / 20L) & 1L) == 0L
                    ? 0.0F : 0.025F;

            pose.pushPose();
            double footprint = Math.max(targetBounds.getXsize(), targetBounds.getZsize());
            float blockScale = (float)Math.min(1.42D, Math.max(0.86D, 0.80D + Math.sqrt(footprint) * 0.20D));
            pose.translate(center.x - camera.x, targetBounds.maxY + 0.24D + bob - camera.y, center.z - camera.z);
            pose.mulPose(event.getLevelRenderState().cameraRenderState.orientation);
            float scale = 0.98F * blockScale * Math.max(0.0F, appear) * distanceScale;
            pose.scale(scale, scale, scale);
            submits.submitCustomGeometry(pose, renderType, WorksiteIndicatorRenderer::renderQuad);
            pose.popPose();
        }
    }

    public static AABB indicatorBounds(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.WIND_TURBINE.get()) || state.is(ModBlocks.WATER_TURBINE.get())) {
            AABB combined = null;
            for (BlockPos part : TurbineBlock.structurePositions(pos, state)) {
                AABB partBounds = blockBounds(level, part);
                combined = combined == null ? partBounds : combined.minmax(partBounds);
            }
            if (combined != null) return combined;
        }
        return blockBounds(level, pos);
    }

    private static AABB blockBounds(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) shape = Shapes.block();
        return shape.bounds().move(pos);
    }

    private static void renderQuad(PoseStack.Pose matrix, VertexConsumer vertices) {
        int light = 0x00F000F0;
        int overlay = OverlayTexture.NO_OVERLAY;
        vertices.addVertex(matrix, -0.5F, 0.0F, 0.0F).setColor(0xFFFFFFFF).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, 0.5F, 0.0F, 0.0F).setColor(0xFFFFFFFF).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, 0.5F, 1.0F, 0.0F).setColor(0xFFFFFFFF).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, -0.5F, 1.0F, 0.0F).setColor(0xFFFFFFFF).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
    }

    private static Identifier workTexture(int action, float completion) {
        Identifier[] frames = switch (action) {
            case 3 -> MACHINE_WORK;
            case 2, 5 -> WOODY_WORK;
            default -> CLASSIC_WORK;
        };
        float safeCompletion = Math.min(1.0F, Math.max(0.0F, completion));
        return frames[Math.min(frames.length - 1, (int) Math.floor(safeCompletion * frames.length))];
    }

    private static Identifier[] workFrames(String prefix) {
        Identifier[] frames = new Identifier[16];
        for (int frame = 0; frame < frames.length; frame++) {
            frames[frame] = Identifier.fromNamespaceAndPath(
                    PrimevalWorks.MOD_ID,
                    "textures/entity/indicator/work/" + prefix + (frame + 1) + ".png"
            );
        }
        return frames;
    }

    private static float spring(float value) {
        if (value >= 1.0F) return 1.0F;
        double damping = 6.2D;
        double frequency = 10.5D;
        double wave = Math.cos(frequency * value) + damping / frequency * Math.sin(frequency * value);
        return 1.0F - (float) (Math.exp(-damping * value) * wave);
    }

    private static final class IndicatorGroup {
        private final int action;
        private float combinedCompletion;
        private float maximumProgress;

        private IndicatorGroup(int action) {
            this.action = action;
        }

        private void add(FieldDodoEntity dinosaur) {
            int duration = Math.max(1, dinosaur.getWorkActionDuration());
            float progress = Math.max(0.0F, dinosaur.getWorkActionProgress());
            combinedCompletion += progress / duration;
            maximumProgress = Math.max(maximumProgress, progress);
        }

        private float completion() {
            return Math.min(1.0F, combinedCompletion);
        }
    }
}
