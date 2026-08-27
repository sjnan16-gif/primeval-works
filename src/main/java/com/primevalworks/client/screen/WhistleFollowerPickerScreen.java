package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.primevalworks.network.payload.AssignWhistleFieldWorkPayload;
import com.primevalworks.network.payload.WhistleFollowerListPayload;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3f;

public final class WhistleFollowerPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 276;
    private static final int INK = 0xFF494341;
    private static final int MUTED_INK = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
    private static WhistleFollowerPickerScreen active;
    private final WhistleFollowerListPayload payload;
    private final long[] hoverStarted;
    private long openedAt;
    private long renderNow;
    private long previousFrame;
    private long pressedAt;
    private int pressedIndex = -1;
    private float parallaxX;
    private float parallaxY;

    private WhistleFollowerPickerScreen(WhistleFollowerListPayload payload) {
        super(Component.literal("Choose a Companion"));
        this.payload = payload;
        this.hoverStarted = new long[Math.max(1, payload.entries().size())];
    }

    public static void open(WhistleFollowerListPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        active = new WhistleFollowerPickerScreen(payload);
        minecraft.setScreen(active);
    }

    @Override
    protected void init() {
        openedAt = Util.getNanos();
        previousFrame = openedAt;
        PrimevalUiSounds.open(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNow = Util.getNanos();
        int panelWidth = PANEL_WIDTH;
        int panelHeight = 36 + Math.max(1, payload.entries().size()) * 44;
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        updateParallax(mouseX, mouseY);
        Motion motion = motion(x, y, panelWidth, panelHeight);
        float uiMouseX = motion.inverseX(mouseX);
        float uiMouseY = motion.inverseY(mouseY);
        graphics.fill(0, 0, width, height, 0x72000000);
        graphics.pose().pushMatrix();
        graphics.pose().translate(motion.pivotX + motion.offsetX, motion.pivotY + motion.offsetY);
        graphics.pose().scale(motion.scale, motion.scale);
        graphics.pose().translate(-motion.pivotX, -motion.pivotY);
        drawBubble(graphics, new Rect(x, y, panelWidth, 32));
        bold(graphics, "CHOOSE A FOLLOWER", x + 10, y + 6, LABEL, 0.94F);
        DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode());
        text(graphics, DinoFieldWorkRules.specialtyName(mode).toUpperCase() + "  /  " + payload.range() + " BLOCK RANGE",
                x + 10, y + 19, MUTED_INK, 0.76F);
        if (payload.entries().isEmpty()) {
            Rect empty = entryRect(x, y, panelWidth, 0);
            drawBubble(graphics, empty);
            graphics.fill(empty.x + 3, empty.y + 3, empty.x + 6, empty.bottom() - 3, 0xFFC76459);
            bold(graphics, "NO FOLLOWERS AVAILABLE", empty.x + 10, empty.y + 7, 0xFFC76459, 0.86F);
            text(graphics, "Set a companion to Follow first.", empty.x + 10, empty.y + 23, MUTED_INK, 0.76F);
        } else {
            for (int index = 0; index < payload.entries().size(); index++) {
                drawEntry(graphics, payload.entries().get(index), entryRect(x, y, panelWidth, index),
                        uiMouseX, uiMouseY, index);
            }
        }
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEntry(GuiGraphicsExtractor graphics, WhistleFollowerListPayload.Entry entry,
                           Rect rect, float mouseX, float mouseY, int index) {
        boolean hovered = rect.contains(mouseX, mouseY);
        int accent = entry.compatible() ? 0xFF62A269 : 0xFF8B5E58;
        drawBubble(graphics, rect);
        graphics.fill(rect.x + 3, rect.y + 3, rect.x + 6, rect.bottom() - 3, accent);
        if (hovered) {
            graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x20FFFFFF);
        }
        graphics.item(spawnEgg(entry.species()), rect.x + 11, rect.y + 10);
        updateHover(index, hovered);
        float motion = interactionMotion(index, hovered);
        graphics.pose().pushMatrix();
        float time = (renderNow - openedAt) / 1_000_000_000.0F;
        graphics.pose().translate(rect.centerX() + Mth.sin(time * 7.2F + index) * 0.5F * motion,
                rect.centerY() + Mth.sin(time * 8.5F + index) * 0.25F * motion);
        graphics.pose().scale(1.0F + Mth.sin(time * 6.2F + index) * 0.012F * motion,
                1.0F + Mth.sin(time * 6.2F + index) * 0.012F * motion);
        graphics.pose().translate(-rect.centerX(), -rect.centerY());
        bold(graphics, entry.name().toUpperCase(), rect.x + 36, rect.y + 6,
                hovered ? accent : INK, 0.88F);
        DinosaurSpecies species = DinosaurSpecies.byRegistryName(entry.species());
        DinoWhistleSettings.FieldMode specialty = DinoFieldWorkRules.specialty(species);
        String detail = specialty == null ? "NO FIELD SPECIALTY"
                : specialty.title().toUpperCase() + "  /  " + entry.rating() + " STAR";
        if (!entry.compatible() && specialty != null) detail += "  /  WRONG ORDER";
        text(graphics, detail, rect.x + 36, rect.y + 23,
                hovered ? accent : MUTED_INK, 0.76F);
        graphics.pose().popMatrix();
        if (hovered) {
            if (entry.compatible()) graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int panelWidth = PANEL_WIDTH;
        int panelHeight = 36 + Math.max(1, payload.entries().size()) * 44;
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        Motion motion = motion(x, y, panelWidth, panelHeight);
        double uiMouseX = motion.inverseX(event.x());
        double uiMouseY = motion.inverseY(event.y());
        for (int index = 0; index < payload.entries().size(); index++) {
            WhistleFollowerListPayload.Entry entry = payload.entries().get(index);
            if (entryRect(x, y, panelWidth, index).contains(uiMouseX, uiMouseY)) {
                if (!entry.compatible()) {
                    PrimevalUiSounds.click(0.72F);
                    return true;
                }
                pressedIndex = index;
                pressedAt = Util.getNanos();
                ClientPacketDistributor.sendToServer(new AssignWhistleFieldWorkPayload(entry.uuid(),
                        payload.first(), payload.second(), payload.hasSecond()));
                PrimevalUiSounds.click(1.08F);
                onClose();
                return true;
            }
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

    private Rect entryRect(int x, int y, int panelWidth, int index) {
        return new Rect(x, y + 36 + index * 44, panelWidth, 40);
    }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect) {
        graphics.fill(rect.x + 4, rect.y + 5, rect.right() + 4, rect.bottom() + 5, 0x43000000);
        PrimevalBubbleUi.draw(graphics, rect.x, rect.y, rect.w, rect.h);
    }

    private static ItemStack spawnEgg(String species) {
        return switch (DinosaurSpecies.byRegistryName(species)) {
            case TYRANNOSAURUS -> new ItemStack(ModItems.TYRANNOSAURUS_SPAWN_EGG.get());
            case TRICERATOPS -> new ItemStack(ModItems.TRICERATOPS_SPAWN_EGG.get());
            case VELOCIRAPTOR -> new ItemStack(ModItems.VELOCIRAPTOR_SPAWN_EGG.get());
            case STEGOSAURUS -> new ItemStack(ModItems.STEGOSAURUS_SPAWN_EGG.get());
            case PARASAUROLOPHUS -> new ItemStack(ModItems.PARASAUROLOPHUS_SPAWN_EGG.get());
            case PTERANODON -> new ItemStack(ModItems.PTERANODON_SPAWN_EGG.get());
            case SPINOSAURUS -> new ItemStack(ModItems.SPINOSAURUS_SPAWN_EGG.get());
            default -> new ItemStack(ModItems.FIELD_DODO_SPAWN_EGG.get());
        };
    }

    private void bold(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value).withStyle(Style.EMPTY.withBold(true)), x, y, color, scale);
    }
    private void text(GuiGraphicsExtractor graphics, String value, float x, float y, int color, float scale) {
        drawText(graphics, Component.literal(value), x, y, color, scale);
    }
    private void drawText(GuiGraphicsExtractor graphics, Component value, float x, float y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, value, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void updateHover(int index, boolean hovered) {
        if (hovered) {
            if (hoverStarted[index] == 0L) hoverStarted[index] = renderNow;
        } else {
            hoverStarted[index] = 0L;
        }
    }

    private float interactionMotion(int index, boolean hovered) {
        float amount = 0.0F;
        if (hovered && hoverStarted[index] != 0L) {
            float seconds = (renderNow - hoverStarted[index]) / 1_000_000_000.0F;
            amount = (1.0F - (float)Math.exp(-seconds * 18.0F)) * (float)Math.exp(-seconds * 2.8F);
            if (seconds >= 1.35F) amount = 0.0F;
        }
        if (pressedIndex == index) {
            amount = Math.max(amount, Mth.clamp(1.0F - (renderNow - pressedAt) / 280_000_000.0F, 0.0F, 1.0F));
        }
        return amount;
    }

    private void updateParallax(int mouseX, int mouseY) {
        float delta = Mth.clamp((renderNow - previousFrame) / 1_000_000_000.0F, 0.0F, 0.05F);
        previousFrame = renderNow;
        float targetX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * -3.0F;
        float targetY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * -1.8F;
        float blend = 1.0F - (float)Math.exp(-delta * 9.0F);
        parallaxX = Mth.lerp(blend, parallaxX, targetX);
        parallaxY = Mth.lerp(blend, parallaxY, targetY);
    }

    private Motion motion(int x, int y, int panelWidth, int panelHeight) {
        long now = renderNow == 0L ? Util.getNanos() : renderNow;
        float elapsedTicks = (now - openedAt) / 50_000_000.0F;
        float progress = Mth.clamp(elapsedTicks / 24.0F, 0.0F, 1.0F);
        float settled = PrimevalBubbleUi.spring(progress, 6.2F, 11.4F);
        float fade = smoothStep(Mth.clamp(elapsedTicks / 18.0F, 0.0F, 1.0F));
        float fit = Math.min(1.0F, Math.min((width - 12.0F) / panelWidth, (height - 12.0F) / panelHeight));
        float scale = Math.max(0.1F, fit * (0.74F + settled * 0.26F));
        float offsetX = parallaxX * fade;
        float offsetY = 18.0F * (1.0F - settled) + parallaxY * fade;
        return new Motion(x + panelWidth * 0.5F, y + panelHeight * 0.5F, offsetX, offsetY, scale);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private record Motion(float pivotX, float pivotY, float offsetX, float offsetY, float scale) {
        float inverseX(double mouseX) {
            return pivotX + ((float)mouseX - pivotX - offsetX) / scale;
        }

        float inverseY(double mouseY) {
            return pivotY + ((float)mouseY - pivotY - offsetY) / scale;
        }
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
