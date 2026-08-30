package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.primevalworks.client.model.entity.DinosaurPreviewBounds;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.network.payload.AssignWhistleFieldWorkPayload;
import com.primevalworks.network.payload.WhistleFollowerListPayload;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WhistleFollowerPickerScreen extends Screen {
    private static final int SLOT_SIZE = 38;
    private static final int SLOT_GAP = 7;
    private static final int INK = 0xFF494341;
    private static final int MUTED_INK = 0xFF6E6764;
    private static WhistleFollowerPickerScreen active;

    private final WhistleFollowerListPayload payload;
    private final long[] hoverStarted;
    private long openedAt;
    private long renderNow;
    private long pressedAt;
    private int pressedIndex = -1;
    private boolean assignmentSent;

    private WhistleFollowerPickerScreen(WhistleFollowerListPayload payload) {
        super(Component.literal("Choose a Companion"));
        this.payload = payload;
        this.hoverStarted = new long[Math.max(1, payload.entries().size())];
    }

    public static void open(WhistleFollowerListPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (payload.entries().isEmpty()) {
            DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode());
            minecraft.player.sendOverlayMessage(Component.literal(
                    "No following companion can handle " + DinoFieldWorkRules.specialtyName(mode).toLowerCase() + "."));
            return;
        }
        active = new WhistleFollowerPickerScreen(payload);
        minecraft.setScreen(active);
    }

    @Override
    protected void init() {
        openedAt = Util.getNanos();
        PrimevalUiSounds.open(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        int count = payload.entries().size();
        int rowWidth = count * SLOT_SIZE + Math.max(0, count - 1) * SLOT_GAP;
        int startX = (width - rowWidth) / 2;
        int slotY = height / 2 - 58;
        int hoveredIndex = -1;

        for (int index = 0; index < count; index++) {
            Rect slot = new Rect(startX + index * (SLOT_SIZE + SLOT_GAP), slotY, SLOT_SIZE, SLOT_SIZE);
            boolean hovered = slot.contains(mouseX, mouseY);
            if (hovered) hoveredIndex = index;
            drawFollowerSlot(graphics, payload.entries().get(index), slot, hovered, index);
        }
        if (hoveredIndex >= 0) drawHoverLabel(graphics, payload.entries().get(hoveredIndex), slotY + SLOT_SIZE + 5);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFollowerSlot(GuiGraphicsExtractor graphics, WhistleFollowerListPayload.Entry entry,
                                  Rect slot, boolean hovered, int index) {
        updateHover(index, hovered);
        float reveal = slotReveal(index);
        float hover = interactionMotion(index, hovered);
        float time = (renderNow - openedAt) / 1_000_000_000.0F;
        float wobbleX = Mth.sin(time * 6.8F + index * 1.4F) * 0.55F * hover;
        float wobbleY = Mth.sin(time * 7.9F + index * 0.9F) * 0.34F * hover;
        float scale = Math.max(0.05F, (0.68F + reveal * 0.32F)
                * (1.0F + Mth.sin(time * 6.1F + index) * 0.018F * hover));
        if (pressedIndex == index) {
            float press = Mth.clamp(1.0F - (renderNow - pressedAt) / 260_000_000.0F, 0.0F, 1.0F);
            scale *= 1.0F - Mth.sin(press * Mth.PI) * 0.08F;
        }
        float rise = (1.0F - reveal) * 18.0F;
        float visualCenterX = slot.centerX() + wobbleX;
        float visualCenterY = slot.centerY() + rise + wobbleY;
        graphics.pose().pushMatrix();
        graphics.pose().translate(visualCenterX, visualCenterY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-slot.centerX(), -slot.centerY());
        PrimevalUiCrop.paperSlot(graphics, slot.x, slot.y, slot.w, slot.h, 255);
        graphics.pose().popMatrix();

        FloatRect visualSlot = new FloatRect(
                visualCenterX + (slot.x - slot.centerX()) * scale,
                visualCenterY + (slot.y - slot.centerY()) * scale,
                slot.w * scale,
                slot.h * scale);
        if (hovered) fill(graphics, inset(visualSlot, 3.0F * scale), 0x22FFF0CB);
        FieldDodoEntity dinosaur = entity(entry.entityId());
        if (dinosaur != null) {
            FloatRect preview = inset(visualSlot, 3.0F * scale);
            float previewScale = previewScale(preview, DinosaurVisualProfile.forType(dinosaur.getType()), 42.0F, -25.0F);
            extractPreview(graphics, preview, previewScale, dinosaur, 42.0F, -25.0F);
        }
        if (!entry.compatible()) fill(graphics, inset(visualSlot, 3.0F * scale), 0x8A211C20);
        if (hovered && entry.compatible()) graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    private void drawHoverLabel(GuiGraphicsExtractor graphics, WhistleFollowerListPayload.Entry entry, int y) {
        String name = entry.name().toUpperCase();
        String detail = entry.compatible()
                ? "LEVEL " + entry.level() + "  /  " + entry.rating() + " STAR  /  CLICK TO ASSIGN"
                : quarryLevelMessage(entry);
        int width = Mth.clamp(Math.max(font.width(name), font.width(detail)) + 18, 76, 164);
        int x = (this.width - width) / 2;
        PrimevalBubbleUi.draw(graphics, x, y, width, 25);
        fitText(graphics, name, x + 7, y + 5, width - 14,
                entry.compatible() ? INK : 0xFF9C5149, 0.68F, true);
        fitText(graphics, detail, x + 7, y + 15, width - 14, MUTED_INK, 0.55F, true);
    }

    private String quarryLevelMessage(WhistleFollowerListPayload.Entry entry) {
        DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode());
        DinoWhistleSettings.Pattern pattern = DinoWhistleSettings.Pattern.byId(payload.pattern());
        if (mode == DinoWhistleSettings.FieldMode.QUARRY
                && pattern == DinoWhistleSettings.Pattern.AREA
                && payload.hasSecond()) {
            int required = DinoFieldWorkRules.requiredLevel(payload.first(), payload.second());
            if (required > entry.level()) return "LEVEL " + required + " REQUIRED FOR THIS QUARRY";
        }
        return "THIS COMPANION CANNOT DO THIS ORDER";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (assignmentSent) return true;
        int count = payload.entries().size();
        int rowWidth = count * SLOT_SIZE + Math.max(0, count - 1) * SLOT_GAP;
        int startX = (width - rowWidth) / 2;
        int slotY = height / 2 - 58;
        for (int index = 0; index < count; index++) {
            Rect slot = new Rect(startX + index * (SLOT_SIZE + SLOT_GAP), slotY, SLOT_SIZE, SLOT_SIZE);
            if (!slot.contains(event.x(), event.y())) continue;
            WhistleFollowerListPayload.Entry entry = payload.entries().get(index);
            if (!entry.compatible()) {
                PrimevalUiSounds.click(0.72F);
                return true;
            }
            pressedIndex = index;
            pressedAt = Util.getNanos();
            assignmentSent = true;
            ClientPacketDistributor.sendToServer(new AssignWhistleFieldWorkPayload(
                    entry.uuid(), payload.selectionToken()));
            PrimevalUiSounds.click(1.08F);
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        active = null;
        PrimevalUiSounds.close(this);
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    public static void submitWorldGeometry(SubmitCustomGeometryEvent event) {
        WhistleFollowerPickerScreen screen = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (screen == null || minecraft.level == null) return;
        if (minecraft.screen != screen) {
            active = null;
            return;
        }
        renderTarget(event, screen.payload.first(), 0xFFFFFFFF, 4.0F);
        if (screen.payload.hasSecond()) renderTarget(event, screen.payload.second(), 0xFF77D3A0, 4.0F);
    }

    private static void renderTarget(SubmitCustomGeometryEvent event, BlockPos pos, int color, float width) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = minecraft.level.getBlockState(pos);
        VoxelShape shape = state.getShape(minecraft.level, pos);
        if (shape.isEmpty()) shape = Shapes.block();
        VoxelShape submittedShape = shape;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;
        SubmitNodeCollector submits = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        submits.submitCustomGeometry(pose, WorksitePlannerScreen.XRAY_HIGHLIGHT_TYPE,
                (matrix, vertices) -> submittedShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    Vector3f normal = new Vector3f((float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1)).normalize();
                    vertices.addVertex(matrix, (float)(x1 + x), (float)(y1 + y), (float)(z1 + z))
                            .setColor(color).setNormal(matrix, normal).setLineWidth(width);
                    vertices.addVertex(matrix, (float)(x2 + x), (float)(y2 + y), (float)(z2 + z))
                            .setColor(color).setNormal(matrix, normal).setLineWidth(width);
                }));
    }

    private FieldDodoEntity entity(int entityId) {
        if (minecraft == null || minecraft.level == null) return null;
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof FieldDodoEntity dinosaur ? dinosaur : null;
    }

    private void extractPreview(GuiGraphicsExtractor graphics, FloatRect target, float scale,
                                FieldDodoEntity dinosaur, float viewYaw, float viewPitch) {
        int x0 = Mth.ceil(target.x);
        int y0 = Mth.ceil(target.y);
        int x1 = Mth.floor(target.right());
        int y1 = Mth.floor(target.bottom());
        if (x1 <= x0 || y1 <= y0) return;
        float renderCenterX = (x0 + x1) * 0.5F;
        float renderCenterY = (y0 + y1) * 0.5F;
        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf topDownRotation = new Quaternionf().rotateX(viewPitch * Mth.DEG_TO_RAD);
        rotation.mul(topDownRotation);
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<? super FieldDodoEntity, ?> renderer = dispatcher.getRenderer(dinosaur);
        EntityRenderState renderState = renderer.createRenderState(dinosaur, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 180.0F - viewYaw;
            livingState.yRot = 0.0F;
            livingState.xRot = 0.0F;
            livingState.boundingBoxWidth /= livingState.scale;
            livingState.boundingBoxHeight /= livingState.scale;
            livingState.scale = 1.0F;
        }
        float inverseScale = 1.0F / Math.max(0.001F, scale);
        Vector3f translation = new Vector3f(
                (target.centerX() - renderCenterX) * inverseScale,
                renderState.boundingBoxHeight * 0.5F
                        + DinosaurVisualProfile.forType(dinosaur.getType()).modelGroundOffset()
                        + (target.centerY() - renderCenterY) * inverseScale,
                0.0F);
        graphics.entity(renderState, scale, translation, rotation, topDownRotation, x0, y0, x1, y1);
    }

    private float previewScale(FloatRect target, DinosaurVisualProfile visual, float viewYaw, float viewPitch) {
        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forVisual(visual);
        float yaw = viewYaw * Mth.DEG_TO_RAD;
        float pitch = viewPitch * Mth.DEG_TO_RAD;
        float footprint = Math.abs(bounds.width() * Mth.cos(yaw)) + Math.abs(bounds.depth() * Mth.sin(yaw));
        float cameraDepth = Math.abs(bounds.width() * Mth.sin(yaw)) + Math.abs(bounds.depth() * Mth.cos(yaw));
        float projectedHeight = bounds.height() * Math.abs(Mth.cos(pitch))
                + cameraDepth * Math.abs(Mth.sin(pitch));
        return Mth.clamp(Math.min(target.width / Math.max(0.35F, footprint),
                target.height / Math.max(0.35F, projectedHeight)) * 1.08F, 1.5F, 44.0F);
    }

    private float slotReveal(int index) {
        float elapsed = (renderNow - openedAt) / 1_000_000_000.0F - index * 0.055F;
        float progress = Mth.clamp(elapsed / 0.42F, 0.0F, 1.0F);
        return PrimevalBubbleUi.spring(progress, 6.4F, 11.6F);
    }

    private void updateHover(int index, boolean hovered) {
        if (hovered) {
            if (hoverStarted[index] == 0L) hoverStarted[index] = renderNow;
        } else hoverStarted[index] = 0L;
    }

    private float interactionMotion(int index, boolean hovered) {
        if (!hovered || hoverStarted[index] == 0L) return 0.0F;
        float seconds = (renderNow - hoverStarted[index]) / 1_000_000_000.0F;
        float amount = (1.0F - (float)Math.exp(-seconds * 18.0F)) * (float)Math.exp(-seconds * 2.8F);
        return seconds >= 1.35F ? 0.0F : amount;
    }

    private void fitText(GuiGraphicsExtractor graphics, String value, float x, float y,
                         float maxWidth, int color, float requestedScale, boolean bold) {
        Component component = bold ? Component.literal(value).withStyle(Style.EMPTY.withBold(true)) : Component.literal(value);
        float scale = Math.min(requestedScale, maxWidth / Math.max(1, font.width(component)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, component, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static FloatRect inset(FloatRect rect, float amount) {
        return new FloatRect(rect.x + amount, rect.y + amount,
                Math.max(1.0F, rect.width - amount * 2.0F), Math.max(1.0F, rect.height - amount * 2.0F));
    }

    private static void fill(GuiGraphicsExtractor graphics, FloatRect rect, int color) {
        graphics.fill(Mth.ceil(rect.x), Mth.ceil(rect.y), Mth.floor(rect.right()), Mth.floor(rect.bottom()), color);
    }

    private record FloatRect(float x, float y, float width, float height) {
        float right() { return x + width; }
        float bottom() { return y + height; }
        float centerX() { return x + width * 0.5F; }
        float centerY() { return y + height * 0.5F; }
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        float centerX() { return x + w * 0.5F; }
        float centerY() { return y + h * 0.5F; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
