package com.primevalworks.client.screen;

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
    private static WhistleFollowerPickerScreen active;
    private final WhistleFollowerListPayload payload;
    private long openedAt;

    private WhistleFollowerPickerScreen(WhistleFollowerListPayload payload) {
        super(Component.literal("Choose a Companion"));
        this.payload = payload;
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
        PrimevalUiSounds.open(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = 264;
        int panelHeight = 49 + Math.max(1, payload.entries().size()) * 48;
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        float progress = Mth.clamp((Util.getNanos() - openedAt) / 240_000_000.0F, 0.0F, 1.0F);
        float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0D);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + panelWidth / 2.0F, y + panelHeight / 2.0F);
        graphics.pose().scale(0.84F + eased * 0.16F, 0.84F + eased * 0.16F);
        graphics.pose().translate(-(x + panelWidth / 2.0F), -(y + panelHeight / 2.0F));
        graphics.fill(x + 5, y + 6, x + panelWidth + 5, y + panelHeight + 6, 0x65000000);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF2C9A77F);
        graphics.fill(x + 3, y + 3, x + panelWidth - 3, y + panelHeight - 3, 0xFFDDBB91);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFF5F4231);
        bold(graphics, "CHOOSE A FOLLOWER", x + 12, y + 8, 0xFF6B392D, 0.88F);
        DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode());
        text(graphics, DinoFieldWorkRules.specialtyName(mode) + "  /  " + payload.range() + " block leash",
                x + 12, y + 22, 0xFF5C514A, 0.68F);
        if (payload.entries().isEmpty()) {
            bold(graphics, "NO FOLLOWERS AVAILABLE", x + 22, y + 53, 0xFFA4473A, 0.80F);
            text(graphics, "Set an active companion to Follow first.", x + 22, y + 67, 0xFF5C514A, 0.68F);
        } else {
            for (int index = 0; index < payload.entries().size(); index++) {
                drawEntry(graphics, payload.entries().get(index), entryRect(x, y, panelWidth, index), mouseX, mouseY);
            }
        }
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEntry(GuiGraphicsExtractor graphics, WhistleFollowerListPayload.Entry entry,
                           Rect rect, int mouseX, int mouseY) {
        boolean hovered = rect.contains(mouseX, mouseY);
        int accent = entry.compatible() ? 0xFF4E8053 : 0xFF8B5E58;
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), hovered ? 0xFFE9C99B : 0xFFC6A17B);
        graphics.outline(rect.x, rect.y, rect.w, rect.h, accent);
        graphics.fill(rect.x + 2, rect.y + 2, rect.x + 6, rect.bottom() - 2, accent);
        graphics.item(spawnEgg(entry.species()), rect.x + 11, rect.y + 10);
        bold(graphics, entry.name().toUpperCase(), rect.x + 36, rect.y + 7,
                entry.compatible() ? 0xFF4D3931 : 0xFF78645C, 0.78F);
        String rating = entry.rating() + "/4 FIELD RATING";
        text(graphics, entry.compatible() ? rating : rating + "  /  CANNOT WORK TARGET",
                rect.x + 36, rect.y + 23, entry.compatible() ? 0xFF46704B : 0xFFA4473A, 0.66F);
        if (hovered && entry.compatible()) {
            graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, 0x1FFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int panelWidth = 264;
        int panelHeight = 49 + Math.max(1, payload.entries().size()) * 48;
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        for (int index = 0; index < payload.entries().size(); index++) {
            WhistleFollowerListPayload.Entry entry = payload.entries().get(index);
            if (entryRect(x, y, panelWidth, index).contains(event.x(), event.y())) {
                if (!entry.compatible()) {
                    PrimevalUiSounds.click(0.72F);
                    return true;
                }
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
        return new Rect(x + 10, y + 38 + index * 48, panelWidth - 20, 42);
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

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
