package com.primevalworks.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.primevalworks.client.screen.DinoWhistleScreen;
import com.primevalworks.client.screen.WorksitePlannerScreen;
import com.primevalworks.network.payload.RequestWhistleFollowersPayload;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3f;

public final class DinoWhistleClient {
    private static BlockPos areaFirst;
    private static DinoWhistleSettings.FieldMode areaMode;
    private static ResourceKey<Level> areaDimension;
    private static long firstCornerMarkedAt;

    private DinoWhistleClient() {}

    public static void handleAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack whistle = DinoWhistleItem.findHeld(minecraft.player);
        if (whistle.isEmpty()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        if (!settings.mode().requiresMark()) {
            clearAreaSelection();
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            minecraft.player.sendOverlayMessage(Component.literal(settings.mode().markHint(settings.pattern())));
            return;
        }
        BlockPos selected = hit.getBlockPos().immutable();
        boolean areaOrder = settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                && settings.pattern() == DinoWhistleSettings.Pattern.AREA;
        boolean choosingFirst = !areaOrder || areaFirst == null || areaMode != settings.mode()
                || !minecraft.level.dimension().equals(areaDimension);
        if (choosingFirst && !DinoFieldWorkRules.validTarget(minecraft.level, selected, settings.mode(), 4)) {
            minecraft.player.sendOverlayMessage(Component.literal(settings.mode().markHint(settings.pattern())));
            return;
        }
        if (areaOrder) {
            if (areaFirst == null || areaMode != settings.mode()) {
                areaFirst = selected;
                areaMode = settings.mode();
                areaDimension = minecraft.level.dimension();
                firstCornerMarkedAt = Util.getNanos();
                minecraft.player.sendOverlayMessage(Component.literal("First corner saved. Mark the opposite corner."));
                return;
            }
            if (Util.getNanos() - firstCornerMarkedAt < 180_000_000L) return;
            if (!DinoFieldWorkRules.areaWithinLimits(areaFirst, selected)) {
                minecraft.player.sendOverlayMessage(Component.literal(
                        "That quarry is beyond the maximum field boundary."));
                return;
            }
            int requiredLevel = DinoFieldWorkRules.requiredLevel(areaFirst, selected);
            if (bestAvailableQuarryLevel(minecraft) < requiredLevel) {
                minecraft.player.sendOverlayMessage(Component.literal(
                        "Level a Quarry dinosaur to " + requiredLevel + " before marking this area."));
                return;
            }
            BlockPos first = areaFirst;
            clearAreaSelection();
            ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(first, selected, true));
            return;
        }
        clearAreaSelection();
        ClientPacketDistributor.sendToServer(new RequestWhistleFollowersPayload(selected, selected, false));
    }

    public static void handleInventoryRightClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1
                || !(event.getScreen() instanceof AbstractContainerScreen<?> container)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Slot hovered = container.getHoveredSlot();
        if (hovered == null || !hovered.getItem().is(ModItems.DINO_WHISTLE.get())) return;
        int inventorySlot = resolveInventorySlot(minecraft, hovered);
        if (inventorySlot < 0) return;
        event.setCanceled(true);
        clearAreaSelection();
        DinoWhistleScreen.open(hovered.getItem(), inventorySlot);
    }

    public static void handleHeldRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        InteractionHand hand = event.getHand();
        ItemStack whistle = minecraft.player.getItemInHand(hand);
        if (!whistle.is(ModItems.DINO_WHISTLE.get())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        int inventorySlot = hand == InteractionHand.MAIN_HAND
                ? minecraft.player.getInventory().getSelectedSlot()
                : minecraft.player.getInventory().getContainerSize() - 1;
        clearAreaSelection();
        DinoWhistleScreen.open(whistle, inventorySlot);
    }

    private static int resolveInventorySlot(Minecraft minecraft, Slot hovered) {
        if (minecraft.player == null) return -1;
        if (hovered.container == minecraft.player.getInventory()) return hovered.getContainerSlot();
        ItemStack hoveredStack = hovered.getItem();
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            if (minecraft.player.getInventory().getItem(index) == hoveredStack) return index;
        }
        int match = -1;
        for (int index = 0; index < minecraft.player.getInventory().getContainerSize(); index++) {
            ItemStack candidate = minecraft.player.getInventory().getItem(index);
            if (!ItemStack.isSameItemSameComponents(candidate, hoveredStack)) continue;
            if (match >= 0) return -1;
            match = index;
        }
        return match;
    }

    private static void clearAreaSelection() {
        areaFirst = null;
        areaMode = null;
        areaDimension = null;
        firstCornerMarkedAt = 0L;
    }

    public static void submitWorldGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        ItemStack whistle = DinoWhistleItem.findHeld(minecraft.player);
        if (whistle.isEmpty()) return;
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);

        if (areaFirst != null) {
            boolean currentSelection = areaMode == DinoWhistleSettings.FieldMode.QUARRY
                    && settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                    && settings.pattern() == DinoWhistleSettings.Pattern.AREA
                    && minecraft.level.dimension().equals(areaDimension);
            if (!currentSelection) {
                clearAreaSelection();
            } else {
                renderBlock(event, areaFirst, 0xE96FE49A, 4.0F);
                if (minecraft.hitResult instanceof BlockHitResult hit) {
                    BlockPos second = hit.getBlockPos();
                    boolean absoluteValid = DinoFieldWorkRules.areaWithinLimits(areaFirst, second);
                    boolean levelValid = absoluteValid
                            && bestAvailableQuarryLevel(minecraft) >= DinoFieldWorkRules.requiredLevel(areaFirst, second);
                    renderArea(event, areaFirst, second,
                            levelValid ? 0x706FE49A : 0x70E65A54, 2.5F, levelValid);
                }
            }
        }

        AABB search = minecraft.player.getBoundingBox().inflate(DinoWhistleSettings.MAX_RANGE + 18.0D);
        for (FieldDodoEntity dinosaur : minecraft.level.getEntitiesOfClass(
                FieldDodoEntity.class,
                search,
                candidate -> candidate.isOwnedBy(minecraft.player.getUUID())
                        && candidate.hasFieldWork()
                        && candidate.getFieldWorkMode() == DinoWhistleSettings.FieldMode.QUARRY)) {
            BlockPos first = dinosaur.getFieldWorkFirst().orElse(null);
            if (first == null) continue;
            BlockPos second = dinosaur.getFieldWorkSecond().orElse(null);
            if (dinosaur.getFieldWorkPattern() == DinoWhistleSettings.Pattern.AREA && second != null) {
                renderArea(event, first, second, 0x586FE49A, 2.0F, true);
            } else {
                renderBlock(event, first, 0x786FE49A, 2.5F);
            }
        }
    }

    private static void renderBlock(SubmitCustomGeometryEvent event, BlockPos pos, int color, float width) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = minecraft.level.getBlockState(pos);
        VoxelShape shape = state.getShape(minecraft.level, pos);
        if (shape.isEmpty()) shape = Shapes.block();
        submitShape(event, shape.move(pos.getX(), pos.getY(), pos.getZ()), color, width);
    }

    private static int bestAvailableQuarryLevel(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return 0;
        AABB search = minecraft.player.getBoundingBox().inflate(DinoWhistleSettings.MAX_RANGE + 18.0D);
        return minecraft.level.getEntitiesOfClass(FieldDodoEntity.class, search,
                        dinosaur -> dinosaur.isOwnedBy(minecraft.player.getUUID())
                                && dinosaur.getCommandMode() == DinosaurCommandMode.FOLLOW
                                && !dinosaur.isOnExpedition()
                                && !dinosaur.isIncapacitated()
                                && DinoFieldWorkRules.rating(
                                        dinosaur, DinoWhistleSettings.FieldMode.QUARRY) > 0)
                .stream().mapToInt(FieldDodoEntity::getDinosaurLevel).max().orElse(0);
    }

    private static void renderArea(SubmitCustomGeometryEvent event, BlockPos first, BlockPos second,
                                   int color, float width, boolean movingStripes) {
        double minX = Math.min(first.getX(), second.getX());
        double minY = Math.min(first.getY(), second.getY());
        double minZ = Math.min(first.getZ(), second.getZ());
        double maxX = Math.max(first.getX(), second.getX()) + 1.0D;
        double maxY = Math.max(first.getY(), second.getY()) + 1.0D;
        double maxZ = Math.max(first.getZ(), second.getZ()) + 1.0D;
        VoxelShape shape = Shapes.create(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        submitShape(event, shape, color, width);
        if (movingStripes) submitMovingStripes(event, shape, 0xE9C9FFD9, width + 0.8F);
    }

    private static void submitShape(SubmitCustomGeometryEvent event, VoxelShape shape, int color, float width) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        SubmitNodeCollector submits = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        submits.submitCustomGeometry(pose, WorksitePlannerScreen.XRAY_HIGHLIGHT_TYPE,
                (matrix, vertices) -> shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    Vector3f normal = new Vector3f((float)(x2 - x1), (float)(y2 - y1),
                            (float)(z2 - z1)).normalize();
                    vertices.addVertex(matrix, (float)(x1 - camera.x), (float)(y1 - camera.y),
                                    (float)(z1 - camera.z))
                            .setColor(color).setNormal(matrix, normal).setLineWidth(width);
                    vertices.addVertex(matrix, (float)(x2 - camera.x), (float)(y2 - camera.y),
                                    (float)(z2 - camera.z))
                            .setColor(color).setNormal(matrix, normal).setLineWidth(width);
                }));
    }

    private static void submitMovingStripes(SubmitCustomGeometryEvent event, VoxelShape shape,
                                            int color, float width) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        SubmitNodeCollector submits = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        double period = 0.48D;
        double stripeLength = 0.19D;
        double phase = ((Util.getNanos() / 1_000_000_000.0D) * 0.82D) % period;
        submits.submitCustomGeometry(pose, WorksitePlannerScreen.XRAY_HIGHLIGHT_TYPE,
                (matrix, vertices) -> shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    double dx = x2 - x1;
                    double dy = y2 - y1;
                    double dz = z2 - z1;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length < 1.0E-5D) return;
                    Vector3f normal = new Vector3f((float)dx, (float)dy, (float)dz).normalize();
                    for (double start = phase - period; start < length; start += period) {
                        double from = Math.max(0.0D, start);
                        double to = Math.min(length, start + stripeLength);
                        if (to <= from) continue;
                        double fromRatio = from / length;
                        double toRatio = to / length;
                        vertices.addVertex(matrix,
                                        (float)(x1 + dx * fromRatio - camera.x),
                                        (float)(y1 + dy * fromRatio - camera.y),
                                        (float)(z1 + dz * fromRatio - camera.z))
                                .setColor(color).setNormal(matrix, normal).setLineWidth(width);
                        vertices.addVertex(matrix,
                                        (float)(x1 + dx * toRatio - camera.x),
                                        (float)(y1 + dy * toRatio - camera.y),
                                        (float)(z1 + dz * toRatio - camera.z))
                                .setColor(color).setNormal(matrix, normal).setLineWidth(width);
                    }
                }));
    }
}
