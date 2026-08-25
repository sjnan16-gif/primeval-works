package com.primevalworks.client.effect;

import com.primevalworks.config.PrimevalConfig;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.client.model.entity.DinosaurPreviewBounds;
import com.primevalworks.network.payload.HatchRevealPayload;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class DinosaurHatchReveal {
    private static final double ENTER_SECONDS = 0.58D;
    private static final double HOLD_SECONDS = 3.8D;
    private static final double EXIT_SECONDS = 0.72D;
    private static HatchRevealPayload current;
    private static FieldDodoEntity preview;
    private static long startedNanos;

    private DinosaurHatchReveal() {
    }

    public static void show(HatchRevealPayload payload) {
        if (!PrimevalConfig.CLIENT.hatchReveal.get()) return;
        current = payload;
        preview = null;
        startedNanos = System.nanoTime();
    }

    public static void render(RenderGuiEvent.Post event) {
        if (current == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui) return;
        double elapsed = (System.nanoTime() - startedNanos) / 1_000_000_000.0D;
        double lifetime = ENTER_SECONDS + HOLD_SECONDS + EXIT_SECONDS;
        if (elapsed >= lifetime) {
            current = null;
            preview = null;
            return;
        }

        float visibility = elapsed < ENTER_SECONDS
                ? backOut((float)(elapsed / ENTER_SECONDS))
                : elapsed < ENTER_SECONDS + HOLD_SECONDS
                        ? 1.0F
                        : smoothStep(1.0F - (float)((elapsed - ENTER_SECONDS - HOLD_SECONDS) / EXIT_SECONDS));
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int panelWidth = Math.min(196, Math.max(176, graphics.guiWidth() / 3));
        int panelHeight = 68;
        int settledX = graphics.guiWidth() - panelWidth;
        int x = settledX + Math.round((panelWidth + 10) * (1.0F - visibility));
        int y = 8;
        float scale = 0.94F + Math.min(1.0F, visibility) * 0.06F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + panelWidth, y + panelHeight * 0.5F);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-(x + panelWidth), -(y + panelHeight * 0.5F));
        drawPanel(graphics, minecraft, x, y, panelWidth, panelHeight);
        graphics.pose().popMatrix();
    }

    private static void drawPanel(
            GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, int width, int height
    ) {
        graphics.fill(x - 3, y + 4, x + width, y + height + 4, 0x71120E0B);
        graphics.fill(x, y, x + width, y + height, 0xF2C6A276);
        graphics.fill(x + 2, y + 2, x + width, y + height - 2, 0xF2AE855E);
        graphics.fill(x + 5, y + 5, x + width, y + height - 5, 0xF2C7A678);
        graphics.outline(x, y, width + 1, height, 0xFF503326);
        graphics.outline(x + 3, y + 3, width - 2, height - 6, 0xFFDEBE8F);
        graphics.fill(x, y + 7, x + 3, y + height - 7, 0xFFE2A23D);
        graphics.fill(x + 59, y + 7, x + 61, y + height - 7, 0xA06A4936);
        graphics.fill(x + 65, y + 17, x + width - 6, y + 18, 0x706A4936);
        graphics.fill(x + 65, y + 39, x + width - 6, y + 40, 0x706A4936);
        graphics.fill(x + 65, y + 53, x + width - 6, y + 54, 0x706A4936);

        ensurePreview(minecraft);
        if (preview != null) {
            drawDinosaur(graphics, minecraft, preview, x + 7, y + 7, 47, height - 14);
        } else {
            graphics.item(spawnEgg(current.species()), x + 22, y + 24);
        }

        Font font = minecraft.font;
        drawFitted(graphics, font, "NEW COMPANION", x + 67, y + 6,
                width - 73, 10, 0xFF67352A, 0.82F);
        int nameBottom = drawWrapped(graphics, font, current.name(), x + 67, y + 20,
                width - 73, 18, 2, 0xFFFFE7A8, 0.94F, 0.68F);
        String mutation = mutationLabel(current.mutationMask());
        int mutationColor = current.mutationMask() == 0 ? 0xFF6B5C50 : 0xFFE65C4D;
        int mutationY = Math.max(y + 42, nameBottom + 1);
        drawFitted(graphics, font, mutation, x + 67, mutationY,
                width - 73, 10, mutationColor, 0.76F);
        drawFitted(graphics, font,
                Component.translatable("hud.primevalworks.hatch_quality", current.quality()).getString(),
                x + 67, y + 56, width - 73, 10, 0xFF59473E, 0.72F);
    }

    private static void ensurePreview(Minecraft minecraft) {
        if (preview != null || minecraft.level == null || current == null) return;
        DinosaurSpecies species = DinosaurSpecies.byRegistryName(current.species());
        preview = ModEntities.typeFor(species).create(minecraft.level, EntitySpawnReason.LOAD);
        if (preview != null) {
            preview.initializeClientPreview(current.quality(), current.mutationMask(), current.hueVariant());
            preview.setCustomName(Component.literal(current.name()));
        }
    }

    private static void drawDinosaur(
            GuiGraphicsExtractor graphics, Minecraft minecraft, FieldDodoEntity dinosaur,
            int x, int y, int width, int height
    ) {
        DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forVisual(visual);
        float viewYaw = 35.0F;
        float viewPitch = 24.0F;
        float yaw = viewYaw * Mth.DEG_TO_RAD;
        float pitch = viewPitch * Mth.DEG_TO_RAD;
        float footprint = Math.abs(bounds.width() * Mth.cos(yaw)) + Math.abs(bounds.depth() * Mth.sin(yaw));
        float cameraDepth = Math.abs(bounds.width() * Mth.sin(yaw)) + Math.abs(bounds.depth() * Mth.cos(yaw));
        float projectedHeight = bounds.height() * Math.abs(Mth.cos(pitch))
                + cameraDepth * Math.abs(Mth.sin(pitch));
        float fittedScale = Math.min(width / Math.max(0.35F, footprint),
                height / Math.max(0.35F, projectedHeight)) * 0.92F;

        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf topDown = new Quaternionf().rotateX(viewPitch * Mth.DEG_TO_RAD);
        rotation.mul(topDown);
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<? super FieldDodoEntity, ?> renderer = dispatcher.getRenderer(dinosaur);
        EntityRenderState state = renderer.createRenderState(dinosaur, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F - viewYaw;
            living.yRot = 0.0F;
            living.xRot = 0.0F;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0F;
        }
        Vector3f translation = new Vector3f(0.0F,
                state.boundingBoxHeight * 0.5F + visual.modelGroundOffset(), 0.0F);
        graphics.entity(state, fittedScale, translation, rotation, topDown, x, y, x + width, y + height);
    }

    private static ItemStack spawnEgg(String species) {
        return switch (DinosaurSpecies.byRegistryName(species)) {
            case TYRANNOSAURUS -> new ItemStack(ModItems.TYRANNOSAURUS_SPAWN_EGG.get());
            case TRICERATOPS -> new ItemStack(ModItems.TRICERATOPS_SPAWN_EGG.get());
            case BRACHIOSAURUS -> new ItemStack(ModItems.BRACHIOSAURUS_SPAWN_EGG.get());
            case DILOPHOSAURUS -> new ItemStack(ModItems.DILOPHOSAURUS_SPAWN_EGG.get());
            case VELOCIRAPTOR -> new ItemStack(ModItems.VELOCIRAPTOR_SPAWN_EGG.get());
            case STEGOSAURUS -> new ItemStack(ModItems.STEGOSAURUS_SPAWN_EGG.get());
            case PARASAUROLOPHUS -> new ItemStack(ModItems.PARASAUROLOPHUS_SPAWN_EGG.get());
            case ANKYLOSAURUS -> new ItemStack(ModItems.ANKYLOSAURUS_SPAWN_EGG.get());
            case PTERANODON -> new ItemStack(ModItems.PTERANODON_SPAWN_EGG.get());
            case DODO -> new ItemStack(ModItems.FIELD_DODO_SPAWN_EGG.get());
            case SPINOSAURUS -> new ItemStack(ModItems.SPINOSAURUS_SPAWN_EGG.get());
            case PACHYCEPHALOSAURUS -> new ItemStack(ModItems.PACHYCEPHALOSAURUS_SPAWN_EGG.get());
        };
    }

    private static String mutationLabel(int mask) {
        boolean huge = (mask & FieldDodoEntity.MUTATION_HUGE) != 0;
        boolean albino = (mask & FieldDodoEntity.MUTATION_ALBINO) != 0;
        if (huge && albino) return "HUGE  +  ALBINO";
        if (huge) return "HUGE MUTATION";
        if (albino) return "ALBINO MUTATION";
        return "NO MUTATION";
    }

    private static void drawFitted(GuiGraphicsExtractor graphics, Font font, String value,
                                   int x, int y, int width, int height, int color, float preferredScale) {
        if (value == null || value.isBlank()) return;
        Component text = Component.literal(value).withStyle(style -> style.withBold(true));
        float scale = Math.min(preferredScale, Math.min(width / (float)Math.max(1, font.width(text)),
                height / (float)font.lineHeight));
        scale = Math.max(0.50F, scale);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private static int drawWrapped(GuiGraphicsExtractor graphics, Font font, String value,
                                   int x, int y, int width, int height, int maxLines,
                                   int color, float preferredScale, float minimumScale) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) return y;
        float scale = preferredScale;
        java.util.List<String> lines = wrap(font, clean, Math.max(1, Mth.floor(width / scale)));
        while ((lines.size() > maxLines || lines.size() * font.lineHeight * scale > height)
                && scale > minimumScale + 0.001F) {
            scale = Math.max(minimumScale, scale - 0.04F);
            lines = wrap(font, clean, Math.max(1, Mth.floor(width / scale)));
        }
        if (lines.size() > maxLines) {
            lines = new java.util.ArrayList<>(lines.subList(0, maxLines));
            int last = lines.size() - 1;
            String suffix = lines.get(last);
            int available = Math.max(1, Mth.floor(width / scale) - font.width("..."));
            lines.set(last, font.plainSubstrByWidth(suffix, available) + "...");
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (int line = 0; line < lines.size(); line++) {
            Component text = Component.literal(lines.get(line)).withStyle(style -> style.withBold(true));
            graphics.text(font, text, 0, line * font.lineHeight, color, true);
        }
        graphics.pose().popMatrix();
        return y + Mth.ceil(lines.size() * font.lineHeight * scale);
    }

    private static java.util.List<String> wrap(Font font, String value, int width) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(candidate) > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.isEmpty() && font.width(word) > width) {
                lines.add(font.plainSubstrByWidth(word, width));
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static float backOut(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F) - 1.0F;
        return 1.0F + 2.70158F * t * t * t + 1.70158F * t * t;
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

}
