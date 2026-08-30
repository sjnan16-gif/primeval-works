package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.render.WorksiteIndicatorRenderer;
import com.primevalworks.network.payload.BaseEnergyPayload;
import com.primevalworks.network.payload.RequestBaseEnergyPayload;
import com.primevalworks.network.payload.ToggleBaseEnergyConsumerPayload;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class EnergyNetworkScreen extends Screen {
    private static final Identifier ENERGY_TOP = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/gui/energy_top.png");
    private static final Identifier ENERGY_ICON = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/gui/energy_above_block_icon.png");
    private static final int CANVAS_WIDTH = 427;
    private static final int CANVAS_HEIGHT = 240;
    private static final int DEFAULT_BASE_RADIUS = 50;
    private static final int INK = 0xFF3F302D;
    private static final int MUTED = 0xFF776D68;
    private static final int GOLD = 0xFFFFD66B;
    private static final int GREEN = 0xFF8ED074;
    private static final int RED = 0xFFE16A62;
    private static EnergyNetworkScreen active;

    private final BlockPos tablePos;
    private final Screen parent;
    private final List<BlockPos> generators = new ArrayList<>();
    private final List<BlockPos> consumers = new ArrayList<>();
    private final Set<BlockPos> enabledConsumers = new HashSet<>();
    private final List<ColumnOffset> scanColumns = new ArrayList<>();
    private Entity previousCamera;
    private CameraType previousCameraType;
    private ArmorStand cameraAnchor;
    private Vec3 cameraPosition;
    private Vec3 cameraVelocity = Vec3.ZERO;
    private float cameraYaw;
    private float cameraPitch;
    private float cameraYawVelocity;
    private float cameraPitchVelocity;
    private float orbitYaw = 42.0F;
    private float orbitPitch = 34.0F;
    private float orbitDistance = 18.0F;
    private int baseRadius = DEFAULT_BASE_RADIUS;
    private BlockPos cameraFocusPos;
    private float cameraFocusBlend;
    private boolean draggingCamera;
    private int scanY;
    private int scanMinY;
    private int scanMaxY;
    private int scanColumnIndex;
    private boolean scanComplete;
    private int screenTicks;
    private float storedEnergy;
    private float energyCapacity = 500.0F;
    private float generationPerSecond;
    private float consumptionPerSecond;
    private BlockPos hoveredPos;
    private long openedNanos = Util.getNanos();
    private long lastCameraFrame = openedNanos;

    public EnergyNetworkScreen(BlockPos tablePos, Screen parent) {
        super(Component.literal("Base Energy Network"));
        this.tablePos = tablePos.immutable();
        this.parent = parent;
    }

    public static void acceptEnergyState(BaseEnergyPayload payload) {
        if (active == null || !active.tablePos.equals(payload.tablePos())) return;
        active.storedEnergy = payload.stored();
        active.energyCapacity = Math.max(1.0F, payload.capacity());
        active.generationPerSecond = payload.generationPerSecond();
        active.consumptionPerSecond = payload.consumptionPerSecond();
        int receivedRadius = Mth.clamp(payload.baseRadius(), 8, 128);
        if (active.baseRadius != receivedRadius) {
            active.baseRadius = receivedRadius;
            if (active.minecraft != null && active.minecraft.level != null) active.beginScan();
        }
        active.enabledConsumers.clear();
        active.enabledConsumers.addAll(payload.enabledConsumers());
    }

    @Override
    protected void init() {
        active = this;
        openedNanos = Util.getNanos();
        PrimevalUiSounds.open(this);
        Entity currentCamera = minecraft.getCameraEntity();
        if (previousCamera == null && currentCamera != cameraAnchor) previousCamera = currentCamera;
        if (previousCameraType == null) previousCameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        cameraPosition = desiredCameraPosition();
        faceTargetImmediately();
        cameraAnchor = new ArmorStand(minecraft.level, cameraPosition.x, cameraPosition.y, cameraPosition.z);
        cameraAnchor.setInvisible(true);
        cameraAnchor.setNoGravity(true);
        applyCameraTransform();
        minecraft.setCameraEntity(cameraAnchor);
        beginScan();
        scanStep(Integer.MAX_VALUE);
        requestEnergyState();
    }

    @Override
    public void tick() {
        scanStep(8_000);
        screenTicks++;
        if (screenTicks % 10 == 0) requestEnergyState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateCameraMotion();
        hoveredPos = rayTrace(mouseX, mouseY);
        drawInterface(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawInterface(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Canvas canvas = canvas();
        float elapsed = Mth.clamp((Util.getNanos() - openedNanos) / 420_000_000.0F, 0.0F, 1.0F);
        float reveal = spring(elapsed);
        float slide = (1.0F - reveal) * -18.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(canvas.x, canvas.y + slide);
        graphics.pose().scale(canvas.scale, canvas.scale);
        drawEnergyChrome(graphics);

        boldFit(graphics, "ENERGY MAP", new Rect(4, 4, 82, 10), GOLD, 0.72F);
        boldFit(graphics, generators.size() + " SOURCES / " + consumers.size() + " DEVICES",
                new Rect(301, 4, 122, 10), MUTED, 0.62F);
        drawRateDivider(graphics);
        boldFit(graphics, String.format(Locale.ROOT, "+%.1f", generationPerSecond),
                new Rect(5, 24, 33, 10), GREEN, 0.86F);
        boldFit(graphics, String.format(Locale.ROOT, "-%.1f", consumptionPerSecond),
                new Rect(42, 24, 33, 10), RED, 0.86F);

        drawAvailableEnergyBar(graphics, storedEnergy / Math.max(1.0F, energyCapacity));

        if (hoveredPos != null) {
            BlockState state = minecraft.level.getBlockState(hoveredPos);
            float cost = BaseEnergyRules.demandPerSecond(minecraft.level, hoveredPos);
            boolean generator = BaseEnergyRules.isGenerator(state);
            String line = state.getBlock().getName().getString().toUpperCase();
            boolean enabled = enabledConsumers.contains(hoveredPos);
            String detail = generator
                    ? turbineReadout(hoveredPos)
                    : formatEnergy(cost) + " E/S  /  " + (enabled ? "CLICK TO DISCONNECT" : "CLICK TO ENERGIZE");
            float localMouseX = (mouseX - canvas.x) / canvas.scale;
            float localMouseY = (mouseY - canvas.y - slide) / canvas.scale;
            drawAuthoredTooltip(graphics, line, detail, localMouseX, localMouseY);
        } else {
            float localMouseX = (mouseX - canvas.x) / canvas.scale;
            float localMouseY = (mouseY - canvas.y - slide) / canvas.scale;
            if (localMouseX >= 0.0F && localMouseX <= 79.0F
                    && localMouseY >= 19.0F && localMouseY <= 184.0F) {
                String status = String.format(Locale.ROOT, "%.0f / %.0f E STORED", storedEnergy, energyCapacity);
                drawAuthoredTooltip(graphics, "ENERGY CAPACITY",
                        scanComplete ? status.toUpperCase(Locale.ROOT) : "MAPPING THE BASE NETWORK...",
                        localMouseX, localMouseY);
            }
        }
        graphics.pose().popMatrix();
    }

    private String turbineReadout(BlockPos pos) {
        if (minecraft.level.getBlockEntity(pos) instanceof TurbineBlockEntity turbine) {
            return turbine.hasValidEnvironment() ? "ACTIVE SOURCE" : "SOURCE NEEDS A VALID SITE";
        }
        return "ENERGY SOURCE";
    }

    private void drawEnergyChrome(GuiGraphicsExtractor graphics) {
        blitRegion(graphics, new Rect(0, 0, CANVAS_WIDTH, 36), 0, 0, CANVAS_WIDTH, 36);
        blitRegion(graphics, new Rect(0, 36, 16, 149), 0, 36, 16, 149);
    }

    private void drawAvailableEnergyBar(GuiGraphicsExtractor graphics, float ratio) {
        int sourceTop = 37;
        int sourceHeight = 148;
        int visible = Math.round(sourceHeight * Mth.clamp(ratio, 0.0F, 1.0F));
        if (visible <= 0) return;
        blitRegion(graphics, new Rect(3, sourceTop + sourceHeight - visible, 11, visible),
                18, sourceTop + sourceHeight - visible, 11, visible);
    }

    private void drawRateDivider(GuiGraphicsExtractor graphics) {
        PrimevalUiCrop.paperVerticalRule(graphics, 39, 22, 3, 13, 255);
    }

    private void drawAuthoredTooltip(GuiGraphicsExtractor graphics, String title, String detail,
                                     float mouseX, float mouseY) {
        int width = 154;
        int x = Mth.clamp(Math.round(mouseX + 8.0F), 34, CANVAS_WIDTH - width - 4);
        int y = Mth.clamp(Math.round(mouseY - 18.0F), 18, CANVAS_HEIGHT - 32);
        Rect titleBubble = new Rect(x, y, width, 14);
        Rect detailBubble = new Rect(x, y + 13, width, 14);
        drawSpaceBubble(graphics, titleBubble);
        drawSpaceBubble(graphics, detailBubble);
        boldFit(graphics, title, new Rect(x + 5, y + 2, width - 10, 10), GOLD, 0.70F);
        boldFit(graphics, detail, new Rect(x + 5, y + 15, width - 10, 10), MUTED, 0.62F);
    }

    private void drawSpaceBubble(GuiGraphicsExtractor graphics, Rect rect) {
        PrimevalUiCrop.paperBubble(graphics, rect.x, rect.y, rect.width, rect.height);
    }

    private void blitRegion(GuiGraphicsExtractor graphics, Rect target, int sx, int sy, int sw, int sh) {
        graphics.blit(ENERGY_TOP, target.x, target.y, target.right(), target.bottom(),
                sx / (float)CANVAS_WIDTH, (sx + sw) / (float)CANVAS_WIDTH,
                sy / (float)CANVAS_HEIGHT, (sy + sh) / (float)CANVAS_HEIGHT);
    }

    private void beginScan() {
        generators.clear();
        consumers.clear();
        scanColumns.clear();
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int z = -baseRadius; z <= baseRadius; z++) {
                if (x * x + z * z <= baseRadius * baseRadius) scanColumns.add(new ColumnOffset(x, z));
            }
        }
        scanColumns.sort(Comparator.comparingInt(ColumnOffset::distanceSquared));
        scanColumnIndex = 0;
        scanMinY = Math.max(minecraft.level.getMinY(), tablePos.getY() - 24);
        scanMaxY = Math.min(minecraft.level.getMaxY() - 1, tablePos.getY() + 24);
        scanY = scanMinY;
        scanComplete = false;
    }

    private void scanStep(int budget) {
        if (scanComplete || minecraft.level == null) return;
        for (int checked = 0; checked < budget && !scanComplete; checked++) {
            ColumnOffset column = scanColumns.get(scanColumnIndex);
            BlockPos pos = tablePos.offset(column.x, scanY - tablePos.getY(), column.z);
            if (pos.distSqr(tablePos) <= (double)baseRadius * baseRadius
                    && minecraft.level.isLoaded(pos)) {
                BlockState state = minecraft.level.getBlockState(pos);
                if (BaseEnergyRules.isGenerator(state)) addDistinct(generators, pos.immutable());
                else if (state.is(ModBlocks.TURBINE_PART.get())) {
                    BlockPos master = TurbinePartBlock.masterPos(pos, state);
                    if (TurbinePartBlock.isExpectedMaster(minecraft.level, master, state)) {
                        addDistinct(generators, master.immutable());
                    }
                }
                else if (BaseEnergyRules.demandPerSecond(minecraft.level, pos) > 0) consumers.add(pos.immutable());
            }
            scanY++;
            if (scanY > scanMaxY) {
                scanY = scanMinY;
                scanColumnIndex++;
                if (scanColumnIndex >= scanColumns.size()) scanComplete = true;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 || event.button() == 1) PrimevalUiSounds.click();
        if (event.button() == 1) {
            draggingCamera = true;
            return true;
        }
        if (event.button() == 0 && hoveredPos != null) {
            BlockState state = minecraft.level.getBlockState(hoveredPos);
            if (BaseEnergyRules.demandPerSecond(minecraft.level, hoveredPos) > 0) {
                if (!enabledConsumers.add(hoveredPos)) enabledConsumers.remove(hoveredPos);
                ClientPacketDistributor.sendToServer(new ToggleBaseEnergyConsumerPayload(tablePos, hoveredPos));
            }
            cameraFocusPos = hoveredPos.immutable();
            cameraFocusBlend = 0.0F;
            orbitDistance = Math.min(orbitDistance, 6.0F);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 1 && draggingCamera) {
            orbitYaw = Mth.wrapDegrees(orbitYaw - (float)dragX * 0.42F);
            orbitPitch = Mth.clamp(orbitPitch + (float)dragY * 0.32F, 15.0F, 72.0F);
            cameraFocusPos = null;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1) {
            draggingCamera = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0.0D && cameraFocusPos != null) cameraFocusPos = null;
        orbitDistance = Mth.clamp(orbitDistance - (float)scrollY * 2.25F,
                cameraFocusPos == null ? 5.0F : 3.0F, Math.max(50.0F, baseRadius + 5.0F));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        if (active == this) active = null;
        restoreCamera();
    }

    @Override
    public void onClose() {
        PrimevalUiSounds.close(this);
        restoreCamera();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private BlockPos rayTrace(int mouseX, int mouseY) {
        if (minecraft.level == null || cameraAnchor == null || width <= 0 || height <= 0) return null;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        float normalizedX = mouseX / (float)width * 2.0F - 1.0F;
        float normalizedY = 1.0F - mouseY / (float)height * 2.0F;
        Vec3 direction = camera.getNearPlane(camera.getFov()).getPointOnPlane(normalizedX, normalizedY).normalize();
        Vec3 start = camera.position();
        Vec3 end = start.add(direction.scale(baseRadius + 5.0D));
        BlockPos highlighted = rayTraceKnownBlock(start, end);
        if (highlighted != null) return highlighted;
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cameraAnchor));
        if (hit.getType() == HitResult.Type.MISS) return null;
        BlockPos candidate = canonicalEnergyPosition(hit.getBlockPos());
        BlockState state = minecraft.level.getBlockState(candidate);
        return BaseEnergyRules.isGenerator(state) || BaseEnergyRules.demandPerSecond(minecraft.level, candidate) > 0
                ? candidate : null;
    }

    private BlockPos rayTraceKnownBlock(Vec3 start, Vec3 end) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : energyBlocks()) {
            Optional<Vec3> intersection = bounds(pos).inflate(0.10D).clip(start, end);
            if (intersection.isEmpty()) continue;
            double distance = start.distanceToSqr(intersection.get());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos;
            }
        }
        return nearest;
    }

    private List<BlockPos> energyBlocks() {
        List<BlockPos> positions = new ArrayList<>(generators.size() + consumers.size());
        positions.addAll(generators);
        positions.addAll(consumers);
        return positions;
    }

    private AABB bounds(BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        if (TurbineBlock.isTurbine(state)) return turbineBounds(pos, state);
        VoxelShape shape = state.getShape(minecraft.level, pos);
        return (shape.isEmpty() ? Shapes.block().bounds() : shape.bounds()).move(pos);
    }

    private static AABB turbineBounds(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(TurbineBlock.FACING);
        boolean eastWest = facing.getAxis() == Direction.Axis.Z;
        boolean wind = TurbineBlock.isWindTurbine(state);
        double height = wind ? 4.0D : 3.0D;
        double minimumWidthOffset = wind ? -1.0D : -2.0D;
        double maximumWidthOffset = wind ? 2.0D : 3.0D;
        return eastWest
                ? new AABB(pos.getX() + minimumWidthOffset, pos.getY(), pos.getZ(),
                        pos.getX() + maximumWidthOffset, pos.getY() + height, pos.getZ() + 1.0D)
                : new AABB(pos.getX(), pos.getY(), pos.getZ() + minimumWidthOffset,
                        pos.getX() + 1.0D, pos.getY() + height, pos.getZ() + maximumWidthOffset);
    }

    private Vec3 desiredCameraPosition() {
        Vec3 center = cameraTarget();
        double yaw = Math.toRadians(orbitYaw);
        double pitch = Math.toRadians(orbitPitch);
        double horizontal = Math.cos(pitch) * orbitDistance;
        return center.add(Math.sin(yaw) * horizontal, Math.sin(pitch) * orbitDistance, Math.cos(yaw) * horizontal);
    }

    private Vec3 cameraTarget() {
        Vec3 table = tablePos.getCenter().add(0.0D, 0.7D, 0.0D);
        if (cameraFocusPos == null || minecraft == null || minecraft.level == null) return table;
        Vec3 focused = bounds(cameraFocusPos).getCenter().add(0.0D, 0.15D, 0.0D);
        float eased = cameraFocusBlend * cameraFocusBlend * (3.0F - 2.0F * cameraFocusBlend);
        return table.lerp(focused, eased);
    }

    private void faceTargetImmediately() {
        Vec3 delta = cameraTarget().subtract(cameraPosition);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        cameraYaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
        cameraPitch = (float)Math.toDegrees(Math.atan2(-delta.y, horizontal));
    }

    private void updateCameraMotion() {
        if (cameraAnchor == null || minecraft.level == null) return;
        long now = Util.getNanos();
        double deltaTime = Mth.clamp((now - lastCameraFrame) / 1_000_000_000.0D, 0.001D, 0.05D);
        lastCameraFrame = now;
        float targetBlend = cameraFocusPos == null ? 0.0F : 1.0F;
        cameraFocusBlend = Mth.lerp(1.0F - (float)Math.exp(-6.5D * deltaTime), cameraFocusBlend, targetBlend);
        Vec3 desired = desiredCameraPosition();
        Vec3 acceleration = desired.subtract(cameraPosition).scale(30.0D).subtract(cameraVelocity.scale(9.5D));
        cameraVelocity = cameraVelocity.add(acceleration.scale(deltaTime));
        cameraPosition = cameraPosition.add(cameraVelocity.scale(deltaTime));
        Vec3 delta = cameraTarget().subtract(cameraPosition);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float targetPitch = (float)Math.toDegrees(Math.atan2(-delta.y, horizontal));
        cameraYawVelocity += (Mth.wrapDegrees(targetYaw - cameraYaw) * 34.0F - cameraYawVelocity * 10.5F) * (float)deltaTime;
        cameraPitchVelocity += ((targetPitch - cameraPitch) * 34.0F - cameraPitchVelocity * 10.5F) * (float)deltaTime;
        cameraYaw = Mth.wrapDegrees(cameraYaw + cameraYawVelocity * (float)deltaTime);
        cameraPitch = Mth.clamp(cameraPitch + cameraPitchVelocity * (float)deltaTime, -89.0F, 89.0F);
        applyCameraTransform();
    }

    private void applyCameraTransform() {
        if (cameraAnchor == null) return;
        cameraAnchor.snapTo(cameraPosition.x, cameraPosition.y - cameraAnchor.getEyeHeight(), cameraPosition.z,
                cameraYaw, cameraPitch);
        cameraAnchor.setYHeadRot(cameraYaw);
        cameraAnchor.setYBodyRot(cameraYaw);
    }

    private void restoreCamera() {
        if (minecraft != null) {
            minecraft.setCameraEntity(previousCamera != null ? previousCamera : minecraft.player);
            if (previousCameraType != null) minecraft.options.setCameraType(previousCameraType);
        }
        cameraAnchor = null;
        draggingCamera = false;
    }

    private void requestEnergyState() {
        ClientPacketDistributor.sendToServer(new RequestBaseEnergyPayload(tablePos));
    }

    private Canvas canvas() {
        float scale = Math.min(width / (float)CANVAS_WIDTH, height / (float)CANVAS_HEIGHT);
        return new Canvas((width - CANVAS_WIDTH * scale) * 0.5F,
                (height - CANVAS_HEIGHT * scale) * 0.5F, scale);
    }

    private void boldFit(GuiGraphicsExtractor graphics, String value, Rect rect, int color, float requestedScale) {
        Component component = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        Font font = minecraft.font;
        float scale = Math.min(requestedScale, Math.min(rect.width / (float)Math.max(1, font.width(component)),
                rect.height / (float)font.lineHeight));
        float x = rect.x + (rect.width - font.width(component) * scale) * 0.5F;
        float y = rect.y + (rect.height - font.lineHeight * scale) * 0.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, component, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static float spring(float value) {
        if (value >= 1.0F) return 1.0F;
        double damping = 6.4D;
        double frequency = 10.0D;
        double wave = Math.cos(frequency * value) + damping / frequency * Math.sin(frequency * value);
        return 1.0F - (float)(Math.exp(-damping * value) * wave);
    }

    public static void submitWorldGeometry(SubmitCustomGeometryEvent event) {
        EnergyNetworkScreen screen = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (screen == null || minecraft.level == null) return;
        SubmitNodeCollector submits = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (BlockPos pos : screen.generators) {
            boolean active = minecraft.level.getBlockEntity(pos) instanceof TurbineBlockEntity turbine
                    && (turbine.isWorkerActive() || turbine.isPassiveActive());
            screen.renderHighlight(pose, submits, pos, active ? 0xFFFFECA0 : 0xC8FFD66B,
                    active ? 4.0F : 3.0F, camera);
        }
        for (BlockPos pos : screen.consumers) {
            boolean enabled = screen.enabledConsumers.contains(pos);
            screen.renderHighlight(pose, submits, pos, enabled ? 0xE88ED074 : 0x8CFFF4DF,
                    enabled ? 3.1F : 2.2F, camera);
        }
        if (screen.hoveredPos != null) screen.renderHighlight(pose, submits, screen.hoveredPos, 0xFFFFFFFF, 4.2F, camera);
        screen.renderEnergyIcons(event, pose, submits, camera);
    }

    private void renderHighlight(PoseStack pose, SubmitNodeCollector submits, BlockPos pos,
                                 int color, float width, Vec3 camera) {
        BlockState state = minecraft.level.getBlockState(pos);
        if (TurbineBlock.isTurbine(state)) {
            AABB authoredBounds = turbineBounds(pos, state);
            VoxelShape authoredShape = Shapes.create(authoredBounds.move(-pos.getX(), -pos.getY(), -pos.getZ()));
            double x = pos.getX() - camera.x;
            double y = pos.getY() - camera.y;
            double z = pos.getZ() - camera.z;
            submits.submitCustomGeometry(pose, WorksitePlannerScreen.XRAY_HIGHLIGHT_TYPE,
                    (matrix, vertices) -> renderShape(matrix, vertices, authoredShape, x, y, z, color, width));
            return;
        }
        renderSingleHighlight(pose, submits, pos, color, width, camera);
    }

    private void renderSingleHighlight(PoseStack pose, SubmitNodeCollector submits, BlockPos pos,
                                       int color, float width, Vec3 camera) {
        BlockState state = minecraft.level.getBlockState(pos);
        VoxelShape shape = state.getShape(minecraft.level, pos);
        if (shape.isEmpty()) shape = Shapes.block();
        VoxelShape submittedShape = shape;
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;
        submits.submitCustomGeometry(pose, WorksitePlannerScreen.XRAY_HIGHLIGHT_TYPE,
                (matrix, vertices) -> renderShape(matrix, vertices, submittedShape, x, y, z, color, width));
    }

    private void renderEnergyIcons(SubmitCustomGeometryEvent event, PoseStack pose,
                                   SubmitNodeCollector submits, Vec3 camera) {
        for (BlockPos pos : generators) {
            AABB sourceBounds = bounds(pos);
            renderEnergyIcon(event, pose, submits, camera, pos, sourceBounds, true);
        }
        for (BlockPos pos : consumers) {
            AABB blockBounds = WorksiteIndicatorRenderer.indicatorBounds(minecraft.level, pos);
            renderEnergyIcon(event, pose, submits, camera, pos, blockBounds, false);

            boolean focused = pos.equals(hoveredPos);
            if (!focused) continue;
            Vec3 center = blockBounds.getCenter();
            Component label = Component.literal(formatEnergy(BaseEnergyRules.demandPerSecond(minecraft.level, pos)) + " E/S")
                    .withStyle(Style.EMPTY.withBold(true));
            pose.pushPose();
            pose.translate(center.x - camera.x, blockBounds.maxY + 0.20D - camera.y, center.z - camera.z);
            pose.mulPose(event.getLevelRenderState().cameraRenderState.orientation);
            pose.scale(0.025F, -0.025F, 0.025F);
            submits.submitText(pose, -minecraft.font.width(label) * 0.5F, 11.0F,
                    label.getVisualOrderText(), true, Font.DisplayMode.SEE_THROUGH,
                    0x00F000F0, 0xFFFFFFFF, 0x720C0910, 0);
            pose.popPose();
        }
    }

    private void renderEnergyIcon(
            SubmitCustomGeometryEvent event,
            PoseStack pose,
            SubmitNodeCollector submits,
            Vec3 camera,
            BlockPos pos,
            AABB blockBounds,
            boolean generator
    ) {
        Vec3 center = blockBounds.getCenter();
        boolean focused = pos.equals(hoveredPos);
        pose.pushPose();
        double distance = camera.distanceTo(center);
        double bob = Math.sin((minecraft.level.getGameTime() + pos.asLong() * 0.01D) * 0.12D) * 0.08D;
        pose.translate(center.x - camera.x, blockBounds.maxY + 0.25D + bob - camera.y, center.z - camera.z);
        pose.mulPose(event.getLevelRenderState().cameraRenderState.orientation);
        float distanceScale = Mth.clamp(0.34F + (float)Math.max(0.0D, distance - 8.0D) * 0.010F,
                0.34F, 0.62F);
        double footprint = Math.max(blockBounds.getXsize(), blockBounds.getZsize());
        float blockScale = (float)Math.min(1.42D, Math.max(0.86D, 0.80D + Math.sqrt(footprint) * 0.20D));
        float iconScale = (focused ? distanceScale * 1.28F : distanceScale) * blockScale;
        pose.scale(iconScale, iconScale, iconScale);
        int iconColor = focused ? 0xFFFFFFFF : generator ? 0xE8FFF0A8 : 0xB8FFFFFF;
        submits.submitCustomGeometry(pose, RenderTypes.entityTranslucent(ENERGY_ICON),
                (matrix, vertices) -> renderIconQuad(matrix, vertices, iconColor));
        pose.popPose();
    }

    private static void renderIconQuad(PoseStack.Pose matrix, VertexConsumer vertices, int color) {
        vertices.addVertex(matrix, -0.5F, 0.0F, 0.0F).setColor(color).setUv(0.0F, 1.0F).setOverlay(0).setLight(0x00F000F0).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, 0.5F, 0.0F, 0.0F).setColor(color).setUv(1.0F, 1.0F).setOverlay(0).setLight(0x00F000F0).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, 0.5F, 1.0F, 0.0F).setColor(color).setUv(1.0F, 0.0F).setOverlay(0).setLight(0x00F000F0).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, -0.5F, 1.0F, 0.0F).setColor(color).setUv(0.0F, 0.0F).setOverlay(0).setLight(0x00F000F0).setNormal(0.0F, 0.0F, 1.0F);
    }

    private static String formatEnergy(float value) {
        float rounded = Math.round(value);
        return Math.abs(value - rounded) < 0.025F
                ? Integer.toString((int)rounded)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    private static void renderShape(PoseStack.Pose matrix, VertexConsumer vertices, VoxelShape shape,
                                    double x, double y, double z, int color, float width) {
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            Vector3f normal = new Vector3f((float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1)).normalize();
            vertices.addVertex(matrix, (float)(x1 + x), (float)(y1 + y), (float)(z1 + z))
                    .setColor(color).setNormal(matrix, normal).setLineWidth(width);
            vertices.addVertex(matrix, (float)(x2 + x), (float)(y2 + y), (float)(z2 + z))
                    .setColor(color).setNormal(matrix, normal).setLineWidth(width);
        });
    }

    private BlockPos canonicalEnergyPosition(BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.is(ModBlocks.TURBINE_PART.get())) {
            BlockPos master = TurbinePartBlock.masterPos(pos, state);
            if (TurbinePartBlock.isExpectedMaster(minecraft.level, master, state)) return master.immutable();
        }
        return pos.immutable();
    }

    private static void addDistinct(List<BlockPos> positions, BlockPos pos) {
        if (!positions.contains(pos)) positions.add(pos);
    }

    private record ColumnOffset(int x, int z) {
        private int distanceSquared() {
            return x * x + z * z;
        }
    }

    private record Canvas(float x, float y, float scale) {
    }

    private record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }
}
