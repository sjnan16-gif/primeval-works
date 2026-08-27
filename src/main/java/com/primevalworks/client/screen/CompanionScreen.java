package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.network.payload.FeedDodoPayload;
import com.primevalworks.network.payload.DinosaurCommandPayload;
import com.primevalworks.network.payload.DinosaurCommandStatePayload;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.work.DinosaurCommandMode;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.HashMap;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CompanionScreen extends Screen {
    private static final int TEXTURE_WIDTH = 427;
    private static final int TEXTURE_HEIGHT = 240;
    private static final int ART_X = 110;
    private static final int ART_Y = 43;
    private static final int ART_WIDTH = 196;
    private static final int ART_HEIGHT = 153;
    private static final int ACTION_EXTENSION_WIDTH = 66;

    private static final Identifier MENU_TEXTURE = texture("dino_menu.png");
    private static final Identifier SPACE_TEXTURE = texture("space.png");
    private static final Identifier HUNGER_BAR_TEXTURE = texture("hunger_bar.png");
    private static final Identifier HEALTH_BAR_TEXTURE = texture("health_bar.png");
    private static final Identifier MOOD_BAR_TEXTURE = texture("mood_bar.png");
    private static final Identifier STAR_TEXTURE = texture("star.png");
    private static final Identifier[] MUTATION_FRAMES = textureFrames("mutation", 10);
    private static final Identifier[] SPECIALTY_ICONS = {
            texture("transport.png"),
            texture("fire.png"),
            texture("energy.png"),
            texture("crafting.png"),
            texture("gathering.png")
    };
    private static final int[] SPECIALTY_ICON_WIDTHS = {9, 8, 6, 10, 9};

    private static final int INK = 0xFF494341;
    private static final int MUTED_INK = 0xFF6E6764;
    private static final int LABEL = 0xFFC74F43;
    private static final int TITLE_END = 0xFFF19A54;
    private static final int EDGE_DARK = 0xFF6D4E3B;
    private static final int EDGE = 0xFF88664F;
    private static final int PAPER_DARK = 0xFFB99472;
    private static final int PAPER = 0xFFD7B392;
    private static final int PAPER_LIGHT = 0xFFE7C9AA;
    private static final String[] SPECIALTIES = {
            "Transport", "Fire", "Energy", "Crafting", "Expedition"
    };
    private static final String[] SPECIALTY_NOTES = {
            "Carries loose items between storage and workstations.",
            "Fuels furnaces and operates heat-based machines.",
            "Produces power and keeps the base network supplied.",
            "Operates dinosaur workbenches and queued recipes.",
            "Leaves the base on timed routes and returns with expedition rewards."
    };
    private static final int[] SPECIALTY_COLORS = {
            0xFF8A512E,
            0xFFC54B2D,
            0xFFD09A16,
            0xFF477895,
            0xFF547B3F
    };

    private static final Rect NAME = new Rect(115, 51, 82, 10);
    private static final Rect LEVEL = new Rect(115, 65, 41, 10);
    private static final Rect FIRST_MUTATION = new Rect(115, 79, 13, 12);
    private static final Rect SECOND_MUTATION = new Rect(131, 79, 13, 12);
    private static final Rect PASSIVE = new Rect(242, 177, 58, 12);
    private static final Rect MODEL = new Rect(262, 51, 39, 39);
    private static final Rect HUNGER_HITBOX = new Rect(251, 93, 51, 10);
    private static final Rect MOOD_HITBOX = new Rect(251, 105, 51, 10);
    private static final Rect HEALTH_HITBOX = new Rect(251, 117, 51, 10);
    private static final Rect INFO = new Rect(115, 155, 121, 34);
    private static final Rect FEED = new Rect(309, 82, 58, 14);
    private static final Rect JOBS = new Rect(309, 101, 58, 14);
    private static final Rect COMMAND = new Rect(309, 120, 58, 14);

    private final FieldDodoEntity dodo;
    private final BlockPos commandTablePos;
    private final PrototypeState state;
    private boolean feedPickerOpen;
    private boolean jobMenuOpen;
    private int pendingJobIndex;
    private String feedPickerNote = "Choose food from your inventory";
    private Action lastAction;
    private float displayedHunger;
    private float displayedHealth;
    private float displayedMood;
    private float parallaxX;
    private float targetParallaxX;
    private float parallaxY;
    private float targetParallaxY;
    private long transitionStartedNanos = Util.getNanos();
    private long feedPickerStartedNanos = transitionStartedNanos;
    private long lastRenderNanos = transitionStartedNanos;
    private long actionPressedNanos;
    private long renderNowNanos = transitionStartedNanos;
    private float renderDeltaSeconds = 1.0F / 60.0F;
    private final Map<Rect, HoverDwell> textHoverDwells = new HashMap<>();

    public CompanionScreen(FieldDodoEntity dodo, BlockPos commandTablePos) {
        super(Component.literal("Dinosaur Record"));
        this.dodo = dodo;
        this.commandTablePos = commandTablePos.immutable();
        this.state = PrototypeState.create(dodo);
        this.pendingJobIndex = state.assignmentIndex;
        this.displayedHunger = dodo.getHunger();
        this.displayedHealth = healthPercent();
        this.displayedMood = state.mood;
    }

    @Override
    protected void init() {
        super.init();
        PrimevalUiSounds.open(this);
        ClientPacketDistributor.sendToServer(new DinosaurCommandPayload(dodo.getId(), -1));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNowNanos = Util.getNanos();
        renderDeltaSeconds = Mth.clamp((renderNowNanos - lastRenderNanos) / 1_000_000_000.0F, 0.001F, 0.05F);
        lastRenderNanos = renderNowNanos;
        targetParallaxX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F) * 2.4F;
        targetParallaxY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F) * 1.7F;
        float parallaxEase = 1.0F - (float) Math.exp(-12.0F * renderDeltaSeconds);
        parallaxX += (targetParallaxX - parallaxX) * parallaxEase;
        parallaxY += (targetParallaxY - parallaxY) * parallaxEase;
        float meterEase = 1.0F - (float) Math.exp(-9.0F * renderDeltaSeconds);
        displayedHunger += (dodo.getHunger() - displayedHunger) * meterEase;
        displayedHealth += (healthPercent() - displayedHealth) * meterEase;
        state.mood = dodo.getMood();
        state.assignmentIndex = dodo.getWorkJobIndex();
        displayedMood += (dodo.getMood() - displayedMood) * meterEase;
        float time = elapsedTicks(transitionStartedNanos);
        if (jobMenuOpen) {
            drawJobMenu(graphics, mouseX, mouseY, time);
        } else {
            drawMainMenu(graphics, mouseX, mouseY, time);
        }
        if (feedPickerOpen) {
            drawFeedPicker(graphics, mouseX, mouseY, time);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return feedPickerOpen || jobMenuOpen || super.mouseClicked(event, doubleClick);
        }
        if (feedPickerOpen) {
            return handleFeedPickerClick(event.x(), event.y());
        }
        if (jobMenuOpen) {
            return handleJobMenuClick(event.x(), event.y());
        }

        Layout layout = layout();
        if (global(layout, FEED).contains(event.x(), event.y())) {
            feedPickerOpen = true;
            feedPickerStartedNanos = Util.getNanos();
            feedPickerNote = "Choose food from your inventory";
            playUiClick(1.04F);
            pressed(Action.FEED);
            return true;
        }
        if (global(layout, JOBS).contains(event.x(), event.y())) {
            pendingJobIndex = state.assignmentIndex;
            jobMenuOpen = true;
            restartTransition();
            playUiClick(1.10F);
            pressed(Action.JOBS);
            return true;
        }
        if (global(layout, INFO).contains(event.x(), event.y())) {
            pendingJobIndex = state.assignmentIndex;
            jobMenuOpen = true;
            restartTransition();
            playUiClick(1.10F);
            pressed(Action.JOBS);
            return true;
        }
        if (global(layout, COMMAND).contains(event.x(), event.y())) {
            DinosaurCommandMode next = nextCommandMode(state.commandMode);
            if (next == DinosaurCommandMode.FOLLOW && state.followers >= state.followerLimit) {
                state.notice = "Follower slots are full. Upgrade Field Command first.";
                playUiClick(0.72F);
                return true;
            }
            requestCommandMode(next);
            playUiClick(next == DinosaurCommandMode.HOME ? 0.92F
                    : next == DinosaurCommandMode.STAY ? 0.98F : 1.08F);
            pressed(Action.COMMAND);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (feedPickerOpen) {
                feedPickerOpen = false;
                return true;
            }
            if (jobMenuOpen) {
                jobMenuOpen = false;
                restartTransition();
                playUiClick(0.88F);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        PrimevalUiSounds.close(this);
        super.onClose();
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void drawMainMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        Layout layout = layout();
        Rect panel = layout.panel();
        UiMotion motion = mainMotion(panel, mouseX, mouseY, time);
        int uiMouseX = Math.round(motion.inverseX(mouseX));
        int uiMouseY = Math.round(motion.inverseY(mouseY));
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        graphics.fill(panel.x() + 4, panel.y() + 5, panel.right() + 4, panel.bottom() + 5, 0x43000000);
        graphics.blit(
                MENU_TEXTURE,
                panel.x(),
                panel.y(),
                panel.right(),
                panel.bottom(),
                ART_X / (float) TEXTURE_WIDTH,
                (ART_X + ART_WIDTH) / (float) TEXTURE_WIDTH,
                ART_Y / (float) TEXTURE_HEIGHT,
                (ART_Y + ART_HEIGHT) / (float) TEXTURE_HEIGHT
        );
        drawIdentity(graphics, layout, uiMouseX, uiMouseY, time);
        drawMutations(graphics, layout, uiMouseX, uiMouseY, time);
        drawPassive(graphics, layout, uiMouseX, uiMouseY, time);
        drawModel(graphics, layout, mouseX, mouseY, uiMouseX, uiMouseY, time, motion);
        drawBars(graphics, layout, time);
        drawJobSummary(graphics, layout, uiMouseX, uiMouseY, time);
        drawAction(graphics, global(layout, FEED), "FEED", 0xFFD86B32, Action.FEED, uiMouseX, uiMouseY, time);
        drawAction(graphics, global(layout, JOBS), "JOBS", 0xFF477895, Action.JOBS, uiMouseX, uiMouseY, time);
        drawCommandAction(graphics, global(layout, COMMAND), uiMouseX, uiMouseY, time);

        HoverInfo hover = mainHoverInfo(layout, uiMouseX, uiMouseY);
        if (hover != null) {
            drawHoverBubble(graphics, hover, uiMouseX, uiMouseY);
        }
        graphics.pose().popMatrix();
    }

    private void drawIdentity(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        Rect name = global(layout, NAME);
        Rect level = global(layout, LEVEL);
        boolean hovered = name.contains(mouseX, mouseY);
        withTextMotion(graphics, name, time, hovered, false, () -> {
            String companion = companionName().toUpperCase();
            String species = speciesName().toUpperCase();
            if (companion.equalsIgnoreCase(species)) {
                fittedCenteredText(graphics, species, inset(name, 4), LABEL, 0.96F, true);
            } else {
                int divider = name.x() + Math.round(name.width() * 0.46F);
                textLine(graphics, companion, name.x() + 5,
                        name.y() + (name.height() - font().lineHeight) / 2,
                        divider - name.x() - 8, LABEL, 0.94F, true);
                rightText(graphics, species, name.right() - 5,
                        name.y() + (name.height() - font().lineHeight) / 2,
                        name.right() - divider - 8, hovered ? LABEL : MUTED_INK, 0.88F, true);
            }
        });
        fittedCenteredText(graphics, "LEVEL 04", level, level.contains(mouseX, mouseY) ? LABEL : INK, 1.0F, true);
    }

    private void drawMutations(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        drawMutation(graphics, global(layout, FIRST_MUTATION), mutationAt(0), 0, mouseX, mouseY, time);
        drawMutation(graphics, global(layout, SECOND_MUTATION), mutationAt(1), 1, mouseX, mouseY, time);
    }

    private void drawMutation(
            GuiGraphicsExtractor graphics,
            Rect slot,
            Mutation mutation,
            int slotIndex,
            int mouseX,
            int mouseY,
        float time
    ) {
        if (mutation == null) {
            drawEmptyMutation(graphics, slot);
        } else {
            boolean hovered = slot.contains(mouseX, mouseY);
            int frame = mutationFrame(time, slotIndex, hovered);
            blitTinted(graphics, MUTATION_FRAMES[frame], slot, mutationColor(mutation.kind()));
        }
        if (slot.contains(mouseX, mouseY)) {
            glow(graphics, slot, time);
        }
    }

    private void drawEmptyMutation(GuiGraphicsExtractor graphics, Rect slot) {
        int size = Math.max(7, Math.min(slot.width(), slot.height()) - 8);
        int startX = slot.centerX() - size / 2;
        int startY = slot.y() + (slot.height() - size) / 2;
        int thickness = Math.max(1, Math.round(size / 7.0F));
        for (int pixel = 0; pixel < size; pixel++) {
            graphics.fill(startX + pixel + 1, startY + pixel + 1, startX + pixel + thickness + 1, startY + pixel + thickness + 1, 0x7438292A);
            graphics.fill(startX + size - pixel, startY + pixel + 1, startX + size - pixel + thickness, startY + pixel + thickness + 1, 0x7438292A);
            graphics.fill(startX + pixel, startY + pixel, startX + pixel + thickness, startY + pixel + thickness, 0xFF766F68);
            graphics.fill(startX + size - pixel - 1, startY + pixel, startX + size - pixel - 1 + thickness, startY + pixel + thickness, 0xFF766F68);
        }
    }

    private void drawPassive(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        Rect passive = global(layout, PASSIVE);
        boolean hovered = passive.contains(mouseX, mouseY);
        int color = passiveColor();
        withTextMotion(graphics, passive, time, hovered, false, () -> {
            fittedCenteredText(graphics, passiveTitle(), inset(passive, 3), hovered ? color : MUTED_INK,
                    0.84F, true);
        });
        int sweep = Math.floorMod((int) (time * 2.2F), passive.width() + 20) - 10;
        int glintX = passive.x() + sweep;
        if (glintX > passive.x() + 2 && glintX < passive.right() - 3) {
            graphics.fill(glintX, passive.y() + 2, Math.min(glintX + 2, passive.right() - 2), passive.bottom() - 2, 0x46FFF1C7);
        }
    }

    private void drawModel(
            GuiGraphicsExtractor graphics,
            Layout layout,
            int screenMouseX,
            int screenMouseY,
            int uiMouseX,
            int uiMouseY,
            float time,
            UiMotion motion
    ) {
        Rect model = global(layout, MODEL);
        FloatRect renderedModel = inset(motion.transformSmooth(model, 0.0F, 0.0F), 1.25F);
        DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dodo.getType());
        float entityHeight = Math.max(0.35F, visual.previewHeight());
        float entityWidth = Math.max(0.35F, visual.previewWidth());
        float heightScale = (renderedModel.height() - 8.0F) / entityHeight;
        float widthScale = (renderedModel.width() - 8.0F) / entityWidth;
        float previewScale = Mth.clamp(Math.min(heightScale, widthScale), 1.5F, 72.0F);
        if (dodo.getType() == ModEntities.PTERANODON.get()) {
            previewScale = Math.min(72.0F, previewScale * 1.48F);
        }
        extractSmoothDinosaurPreview(graphics, renderedModel, previewScale,
                screenMouseX, screenMouseY, dodo);
        if (model.contains(uiMouseX, uiMouseY)) {
            glow(graphics, model, time);
        }
    }

    private void drawBars(GuiGraphicsExtractor graphics, Layout layout, float time) {
        drawBar(
                graphics,
                global(layout, new Rect(261, 93, 41, 10)),
                HUNGER_BAR_TEXTURE,
                displayedHunger,
                time,
                0xFFFFD37A
        );
        drawBar(
                graphics,
                global(layout, new Rect(261, 105, 41, 10)),
                MOOD_BAR_TEXTURE,
                displayedMood,
                time,
                0xFFA8E7FF
        );
        drawBar(
                graphics,
                global(layout, new Rect(261, 117, 41, 10)),
                HEALTH_BAR_TEXTURE,
                displayedHealth,
                time,
                0xFFFFA0A0
        );
    }

    private void drawBar(GuiGraphicsExtractor graphics, Rect rect, Identifier texture, float value, float time, int textColor) {
        int visibleWidth = Mth.clamp(Math.round(rect.width() * value / 100.0F), 0, rect.width());
        if (visibleWidth > 0) {
            float uEnd = visibleWidth / (float) rect.width();
            graphics.blit(texture, rect.x(), rect.y(), rect.x() + visibleWidth, rect.bottom(), 0.0F, uEnd, 0.0F, 1.0F);
        }
        fittedCenteredText(graphics, Math.round(value) + "%", new Rect(rect.x() + 1, rect.y(), rect.width() - 2, rect.height()), textColor, 0.68F, true);
        if (value < 25.0F && visibleWidth > 3) {
            int alpha = 35 + Math.round((Mth.sin(time * 0.22F) + 1.0F) * 24.0F);
            graphics.fill(
                    rect.x() + visibleWidth - 1,
                    rect.y() + 1,
                    rect.x() + visibleWidth,
                    rect.bottom() - 1,
                    withAlpha(0xFFFFFFFF, alpha)
            );
        }
    }

    private void drawJobSummary(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        Rect info = global(layout, INFO);
        int color = SPECIALTY_COLORS[state.assignmentIndex];
        boolean hovered = info.contains(mouseX, mouseY);
        int accentWidth = Math.max(3, Math.round(2.0F * layout.scale()));
        graphics.fill(info.x() + 2, info.y() + 3, info.x() + 2 + accentWidth, info.bottom() - 3, hovered ? color : MUTED_INK);
        int textX = info.x() + accentWidth + 7;
        int textWidth = info.right() - textX - 4;
        int textColor = hovered ? color : INK;
        withTextMotion(graphics, info, time, hovered, false, () -> {
            textLine(graphics, "ACTIVE JOB  /  " + state.assignment().toUpperCase(),
                    textX, info.y() + 3, textWidth, textColor, 0.88F, true);
            if (state.assignmentIndex == 4) {
                textLine(graphics, "Timed route outside the base.",
                        textX, info.y() + 13, textWidth, INK, 0.76F, true);
                textLine(graphics, "Returns with fixed tier rewards.",
                        textX, info.y() + 20, textWidth, INK, 0.76F, true);
                textLine(graphics, specialtyStars(state.assignmentIndex) + " STAR  /  " + specialtySpeedLabel(state.assignmentIndex),
                        textX, info.y() + 27, textWidth, hovered ? color : MUTED_INK, 0.70F, true);
            } else {
                textLine(graphics, shortJobDescription(state.assignmentIndex),
                        textX, info.y() + 14, textWidth, INK, 0.82F, true);
                textLine(graphics, specialtyStars(state.assignmentIndex) + " STAR  /  " + specialtySpeedLabel(state.assignmentIndex),
                        textX, info.y() + 24, textWidth, hovered ? color : MUTED_INK, 0.82F, true);
            }
        });
    }

    private void drawAction(
            GuiGraphicsExtractor graphics,
            Rect rect,
            String label,
            int accent,
            Action action,
            int mouseX,
            int mouseY,
            float time
    ) {
        boolean hovered = rect.contains(mouseX, mouseY);
        drawBubble(graphics, rect, 2);
        int inset = Math.max(2, rect.height() / 7);
        graphics.fill(rect.x() + inset, rect.y() + inset, rect.x() + inset + Math.max(2, rect.width() / 18), rect.bottom() - inset, accent);
        if (hovered) {
            glow(graphics, new Rect(rect.x() + 2, rect.y() + 2, rect.width() - 4, rect.height() - 4), time);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        if (lastAction == action && feedbackActive()) {
            graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, 0x35FFFFFF);
        }
        Rect labelRect = new Rect(rect.x() + inset + 5, rect.y(), rect.width() - inset - 8, rect.height());
        withTextMotion(graphics, labelRect, time, hovered, lastAction == action && feedbackActive(),
                () -> fittedCenteredText(graphics, label, labelRect, hovered ? accent : INK, 1.0F, true));
    }

    private void drawCommandAction(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY, float time) {
        DinosaurCommandMode mode = state.commandMode;
        DinosaurCommandMode next = nextCommandMode(mode);
        int accent = commandColor(mode);
        boolean locked = next == DinosaurCommandMode.FOLLOW && state.followers >= state.followerLimit;
        drawAction(graphics, rect, mode.title().toUpperCase(), locked ? 0xFF766F68 : accent,
                Action.COMMAND, mouseX, mouseY, time);
        outline(graphics, inset(rect, 2), withAlpha(accent, 210));
        graphics.fill(rect.right() - 5, rect.y() + 4, rect.right() - 3, rect.bottom() - 4, accent);
        if (locked) {
            graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, 0x60706A67);
        }
    }

    private HoverInfo mainHoverInfo(Layout layout, int mouseX, int mouseY) {
        if (global(layout, FEED).contains(mouseX, mouseY)) {
            return new HoverInfo("FEED", "Choose suitable food from your inventory and feed this companion now.", 0xFFD86B32);
        }
        if (global(layout, JOBS).contains(mouseX, mouseY)) {
            return new HoverInfo("BASE JOBS", "Review specialties and assign permanent work inside the Command Table's base.", 0xFF477895);
        }
        if (global(layout, COMMAND).contains(mouseX, mouseY)) {
            DinosaurCommandMode current = state.commandMode;
            DinosaurCommandMode next = nextCommandMode(current);
            String detail = current.description() + " Click to switch to " + next.title() + ".";
            if (next == DinosaurCommandMode.FOLLOW) {
                detail += " Followers: " + state.followers + "/" + state.followerLimit + ".";
                if (state.followers >= state.followerLimit) {
                    detail += " Upgrade Field Command to unlock another slot.";
                }
            }
            return new HoverInfo(current.title().toUpperCase(), detail, commandColor(current));
        }
        if (global(layout, FIRST_MUTATION).contains(mouseX, mouseY)) {
            return mutationInfo(mutationAt(0));
        }
        if (global(layout, SECOND_MUTATION).contains(mouseX, mouseY)) {
            return mutationInfo(mutationAt(1));
        }
        if (global(layout, PASSIVE).contains(mouseX, mouseY)) {
            int color = passiveColor();
            return new HoverInfo(passiveTitle(), passiveDetail(), color, lerpRgb(INK, color, 0.55F));
        }
        if (global(layout, MODEL).contains(mouseX, mouseY)) {
            return new HoverInfo(speciesName().toUpperCase() + " / LEVEL 04", companionName() + " is ready for base duty.", LABEL);
        }
        if (global(layout, HUNGER_HITBOX).contains(mouseX, mouseY)) {
            return new HoverInfo("HUNGER • " + dodo.getHunger() + "%", hungerNote(), 0xFFAA6937);
        }
        if (global(layout, HEALTH_HITBOX).contains(mouseX, mouseY)) {
            return new HoverInfo("HEALTH • " + healthPercent() + "%", healthNote(), 0xFFC54B3F);
        }
        if (global(layout, MOOD_HITBOX).contains(mouseX, mouseY)) {
            return new HoverInfo("MOOD • " + state.mood + "%", moodNote(), 0xFF477895);
        }
        if (global(layout, INFO).contains(mouseX, mouseY)) {
            int index = state.assignmentIndex;
            return new HoverInfo(
                    SPECIALTIES[index].toUpperCase() + "  " + specialtyStars(index) + "/4",
                    SPECIALTY_NOTES[index] + " " + specialtySpeedDetail(index),
                    SPECIALTY_COLORS[index]
            );
        }
        return null;
    }

    private HoverInfo mutationInfo(Mutation mutation) {
        if (mutation == null) {
            return new HoverInfo("EMPTY MUTATION", "No rare trait was inherited in this slot.", 0xFF766F68);
        }
        int color = mutationColor(mutation.kind());
        return new HoverInfo(
                mutation.name().toUpperCase(),
                mutation.effect(),
                color,
                lerpRgb(INK, color, 0.62F)
        );
    }

    private Mutation mutationAt(int slot) {
        int mutationMask = dodo.getMutationMask();
        boolean huge = (mutationMask & FieldDodoEntity.MUTATION_HUGE) != 0;
        boolean albino = (mutationMask & FieldDodoEntity.MUTATION_ALBINO) != 0;
        if (slot == 0) {
            if (huge) return new Mutation(MutationKind.HUGE, "Huge", "+20% stats and health / +18% size");
            if (albino) return new Mutation(MutationKind.ALBINO, "Albino", "+40% stats and mount speed / -20% health");
        } else if (huge && albino) {
            return new Mutation(MutationKind.ALBINO, "Albino", "+40% stats and mount speed / -20% health");
        }
        return null;
    }

    private void drawHoverBubble(GuiGraphicsExtractor graphics, HoverInfo info, int mouseX, int mouseY) {
        int contentWidth = Math.min(204, Math.max(118,
                Math.max(displayWidth(info.title(), true), Math.min(204, font().width(info.detail())))));
        java.util.List<String> detailLines = wrapTooltipText(info.detail(), contentWidth);
        int bubbleWidth = contentWidth + 16;
        int bubbleHeight = 19 + detailLines.size() * (font().lineHeight + 1);
        int x = mouseX + 12;
        int y = mouseY - 8;
        if (x + bubbleWidth > width - 4) {
            x = mouseX - bubbleWidth - 12;
        }
        if (y + bubbleHeight > height - 4) {
            y = height - bubbleHeight - 4;
        }
        y = Math.max(4, y);
        Rect bubble = new Rect(x, y, bubbleWidth, bubbleHeight);
        drawBubble(graphics, bubble, 2);
        graphics.fill(bubble.x() + 3, bubble.y() + 3, bubble.x() + 6, bubble.bottom() - 3, info.color());
        textLine(graphics, info.title(), bubble.x() + 10, bubble.y() + 4,
                bubble.width() - 14, info.color(), 1.0F, true);
        for (int index = 0; index < detailLines.size(); index++) {
            textLine(graphics, detailLines.get(index), bubble.x() + 10,
                    bubble.y() + 15 + index * (font().lineHeight + 1),
                    bubble.width() - 14, info.detailColor(), 1.0F, true);
        }
    }

    private java.util.List<String> wrapTooltipText(String value, int maximumWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String current = "";
        for (String word : value.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font().width(candidate) > maximumWidth) {
                lines.add(current);
                current = word;
            } else {
                current = candidate;
            }
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        return lines.isEmpty() ? java.util.List.of("") : lines;
    }

    private void drawJobMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        JobMenuLayout layout = jobMenuLayout();
        UiMotion motion = popupMotion(layout.panel(), time);
        int uiMouseX = Math.round(motion.inverseX(mouseX));
        int uiMouseY = Math.round(motion.inverseY(mouseY));
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        drawPaperPanel(graphics, layout.panel());
        textLine(graphics, "ASSIGN WORK", layout.panel().x() + 13, layout.panel().y() + 8, layout.panel().width() / 2, LABEL, 1.0F, true);
        String record = companionName().toUpperCase() + " • LEVEL 04";
        rightText(graphics, record, layout.panel().right() - 13, layout.panel().y() + 8, layout.panel().width() / 2, LABEL, 0.76F, true);
        textLine(
                graphics,
                "Choose one base priority. Field work is reserved for species specialists.",
                layout.panel().x() + 13,
                layout.panel().y() + 20,
                layout.panel().width() - 26,
                MUTED_INK,
                0.78F,
                false
        );

        for (int index = 0; index < SPECIALTIES.length; index++) {
            drawJobRow(graphics, layout.row(index), index, uiMouseX, uiMouseY, time);
        }
        drawFieldJobRow(graphics, layout.row(SPECIALTIES.length), uiMouseX, uiMouseY, time);

        Rect footerText = layout.footerText();
        int selectedColor = SPECIALTY_COLORS[pendingJobIndex];
        textLine(graphics, SPECIALTIES[pendingJobIndex].toUpperCase() + " • " + specialtySpeedLabel(pendingJobIndex), footerText.x(), footerText.y() + 4, footerText.width(), MUTED_INK, 1.0F, true);
        drawWorksiteButton(graphics, layout.worksite(), selectedColor, uiMouseX, uiMouseY, time);
        drawDialogButton(graphics, layout.cancel(), "CANCEL", MUTED_INK, uiMouseX, uiMouseY, time);
        drawDialogButton(graphics, layout.confirm(), "ASSIGN", selectedColor, uiMouseX, uiMouseY, time);
        graphics.pose().popMatrix();
    }

    private void drawJobRow(
            GuiGraphicsExtractor graphics,
            Rect row,
            int index,
            int mouseX,
            int mouseY,
            float time
    ) {
        boolean selected = pendingJobIndex == index;
        boolean active = state.assignmentIndex == index;
        boolean hovered = row.contains(mouseX, mouseY);
        int color = SPECIALTY_COLORS[index];

        graphics.fill(row.x(), row.y(), row.right(), row.bottom(), EDGE_DARK);
        graphics.fill(row.x() + 1, row.y() + 1, row.right() - 1, row.bottom() - 1, hovered ? PAPER_LIGHT : PAPER_DARK);
        graphics.fill(row.x() + 2, row.y() + 2, row.x() + 6, row.bottom() - 2, color);
        if (selected) {
            graphics.fill(row.x() + 6, row.y() + 2, row.right() - 2, row.bottom() - 2, withAlpha(color, 35));
        }
        if (hovered) {
            glow(graphics, row, time);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        int iconHeight = 16;
        int iconWidth = Math.max(12, Math.round(SPECIALTY_ICON_WIDTHS[index] * (iconHeight / 8.0F)));
        blit(graphics, SPECIALTY_ICONS[index], new Rect(row.x() + 11, row.y() + (row.height() - iconHeight) / 2, iconWidth, iconHeight));
        int textX = row.x() + 36;
        withTextMotion(graphics, new Rect(textX, row.y() + 2, row.width() - 92, row.height() - 4), time, hovered, selected, () -> {
            textLine(graphics, SPECIALTIES[index].toUpperCase(), textX, row.y() + 4, 100, hovered ? color : INK, 1.0F, true);
            if (active) {
                textLine(graphics, "CURRENT", textX + 105, row.y() + 4, 54, hovered ? color : MUTED_INK, 1.0F, true);
            }
            textLine(graphics, shortJobDescription(index), textX, row.y() + 16, row.width() - 230,
                    hovered ? color : MUTED_INK, 1.0F, true);
            rightText(graphics, specialtySpeedLabel(index), row.right() - 66, row.y() + 16, 112, hovered ? color : MUTED_INK, 1.0F, true);
        });

        int starX = row.right() - 54;
        int starY = row.y() + (row.height() - 11) / 2;
        for (int star = 0; star < 4; star++) {
            Rect starRect = new Rect(starX + star * 12, starY, 11, 11);
            if (star < specialtyStars(index)) {
                blit(graphics, STAR_TEXTURE, starRect);
            } else {
                blitTinted(graphics, STAR_TEXTURE, starRect, 0x676A625D);
            }
        }
    }

    private void drawFieldJobRow(GuiGraphicsExtractor graphics, Rect row, int mouseX, int mouseY, float time) {
        boolean hovered = row.contains(mouseX, mouseY);
        DinoWhistleSettings.FieldMode specialty = DinoFieldWorkRules.specialty(dodo.getSpecies());
        int source = DinoFieldWorkRules.sourceJobIndex(dodo.getSpecies());
        int rating = source < 0 ? 0 : dodo.getSpecialtyStars(source);
        int color = source < 0 ? 0xFF766F68 : SPECIALTY_COLORS[source];
        drawBubble(graphics, row, 2);
        graphics.fill(row.x() + 2, row.y() + 2, row.right() - 2, row.bottom() - 2,
                hovered ? 0xB9393238 : 0xCA242126);
        graphics.fill(row.x() + 3, row.y() + 3, row.x() + 6, row.bottom() - 3, color);
        if (source >= 0) {
            int iconHeight = 16;
            int iconWidth = Math.max(12, Math.round(SPECIALTY_ICON_WIDTHS[source] * (iconHeight / 8.0F)));
            blitTinted(graphics, SPECIALTY_ICONS[source],
                    new Rect(row.x() + 11, row.y() + (row.height() - iconHeight) / 2, iconWidth, iconHeight),
                    hovered ? 0xFFFFFFFF : 0xFFD0C6BE);
        } else {
            graphics.item(Items.IRON_PICKAXE.getDefaultInstance(), row.x() + 10, row.y() + 4);
            graphics.fill(row.x() + 10, row.y() + 4, row.x() + 26, row.y() + 20, 0x880D0C0E);
        }
        int textX = row.x() + 36;
        String title = specialty == null ? "FIELD WORK" : DinoFieldWorkRules.specialtyName(specialty).toUpperCase();
        String description = specialty == null
                ? "No field specialty."
                : "Assign with the Dino Whistle.";
        String speed = specialty == null ? "NOT AVAILABLE" : specialtySpeedLabel(source);
        withTextMotion(graphics, new Rect(textX, row.y() + 2, row.width() - 92, row.height() - 4),
                time, hovered, false, () -> {
                    textLine(graphics, title, textX, row.y() + 4, 160,
                            hovered ? color : 0xFFD3CBC5, 1.0F, true);
                    textLine(graphics, description, textX, row.y() + 16, row.width() - 230,
                            hovered ? color : 0xFF928A88, 1.0F, true);
                    rightText(graphics, speed, row.right() - 66, row.y() + 16, 112,
                            hovered ? color : 0xFF928A88, 1.0F, true);
                });
        if (specialty != null) {
            int starX = row.right() - 54;
            int starY = row.y() + (row.height() - 11) / 2;
            for (int star = 0; star < 4; star++) {
                Rect starRect = new Rect(starX + star * 12, starY, 11, 11);
                if (star < rating) blit(graphics, STAR_TEXTURE, starRect);
                else blitTinted(graphics, STAR_TEXTURE, starRect, 0x453E3B40);
            }
        }
        if (hovered) {
            glow(graphics, row, time);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void drawDialogButton(
            GuiGraphicsExtractor graphics,
            Rect rect,
            String label,
            int color,
            int mouseX,
            int mouseY,
            float time
    ) {
        drawBubble(graphics, rect, 2);
        if (rect.contains(mouseX, mouseY)) {
            glow(graphics, rect, time);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        boolean hovered = rect.contains(mouseX, mouseY);
        withTextMotion(graphics, rect, time, hovered, false,
                () -> fittedCenteredText(graphics, label, rect, hovered ? color : INK, 1.0F, true));
    }

    private void drawWorksiteButton(GuiGraphicsExtractor graphics, Rect rect, int color, int mouseX, int mouseY, float time) {
        boolean hovered = rect.contains(mouseX, mouseY);
        drawBubble(graphics, rect, 2);
        graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, withAlpha(color, hovered ? 112 : 76));
        outline(graphics, new Rect(rect.x() + 1, rect.y() + 1, rect.width() - 2, rect.height() - 2), hovered ? 0xFFFFFFFF : color);
        withTextMotion(graphics, rect, time, hovered, false,
                () -> fittedCenteredText(graphics, "ASSIGN WORKSITE", rect, hovered ? 0xFFFFFFFF : INK, 1.0F, true));
        if (hovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private boolean handleJobMenuClick(double mouseX, double mouseY) {
        JobMenuLayout layout = jobMenuLayout();
        for (int index = 0; index < SPECIALTIES.length; index++) {
            if (layout.row(index).contains(mouseX, mouseY)) {
                pendingJobIndex = index;
                playUiClick(0.96F + index * 0.035F);
                return true;
            }
        }
        if (layout.row(SPECIALTIES.length).contains(mouseX, mouseY)) {
            state.notice = DinoFieldWorkRules.specialty(dodo.getSpecies()) == null
                    ? "This species has no follower specialty."
                    : "Set this companion to Follow, then mark field work with a Dino Whistle.";
            playUiClick(0.84F);
            return true;
        }
        if (layout.cancel().contains(mouseX, mouseY)) {
            jobMenuOpen = false;
            restartTransition();
            playUiClick(0.88F);
            return true;
        }
        if (layout.worksite().contains(mouseX, mouseY)) {
            playUiClick(1.22F);
            minecraft.setScreen(new WorksitePlannerScreen(dodo, commandTablePos, pendingJobIndex, this));
            return true;
        }
        if (layout.confirm().contains(mouseX, mouseY)) {
            selectSpecialty(pendingJobIndex);
            jobMenuOpen = false;
            restartTransition();
            playUiClick(1.15F);
            pressed(Action.JOBS);
            return true;
        }
        return true;
    }

    private void drawFeedPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        FeedPickerLayout picker = feedPickerLayout();
        float popupTime = elapsedTicks(feedPickerStartedNanos);
        UiMotion motion = popupMotion(picker.panel(), popupTime);
        int uiMouseX = Math.round(motion.inverseX(mouseX));
        int uiMouseY = Math.round(motion.inverseY(mouseY));
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        drawPaperPanel(graphics, picker.panel());
        gradientTextLine(
                graphics,
                "CHOOSE FOOD",
                picker.panel().x() + 12,
                picker.panel().y() + 8,
                90,
                LABEL,
                TITLE_END,
                0.88F
        );
        rightText(
                graphics,
                feedPickerNote,
                picker.panel().right() - 12,
                picker.panel().y() + 8,
                picker.panel().width() - 110,
                LABEL,
                0.66F,
                true
        );

        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            Rect rect = feedSlot(picker, slot);
            if (rect == null) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            boolean edible = canFeed(stack);
            boolean hovered = rect.contains(uiMouseX, uiMouseY);
            drawInventorySlot(graphics, rect, edible, hovered, time, slot);
            if (!stack.isEmpty()) {
                graphics.item(stack, rect.x() + 2, rect.y() + 2);
                graphics.itemDecorations(font(), stack, rect.x() + 2, rect.y() + 2);
                if (!edible) {
                    graphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xB0000000);
                } else if (hovered) {
                    graphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0x25FFFFFF);
                }
            }
            if (hovered && !stack.isEmpty()) {
                if (edible) {
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                    graphics.setTooltipForNextFrame(font(), stack, uiMouseX, uiMouseY);
                } else {
                    graphics.setTooltipForNextFrame(
                            Component.literal(companionName() + " won't eat " + stack.getHoverName().getString().toLowerCase() + "."),
                            uiMouseX,
                            uiMouseY
                    );
                }
            }
        }
        graphics.pose().popMatrix();
    }

    private void drawInventorySlot(
            GuiGraphicsExtractor graphics,
            Rect rect,
            boolean edible,
            boolean hovered,
            float time,
            int index
    ) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), EDGE_DARK);
        graphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, PAPER_DARK);
        graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, PAPER_LIGHT);
        if (edible) {
            int alpha = 72 + Math.round((Mth.sin(time * 0.14F + index) + 1.0F) * 35.0F);
            outline(graphics, new Rect(rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2), withAlpha(0xFFFFFFFF, alpha));
        }
        if (hovered) {
            glow(graphics, rect, time);
        }
    }

    private boolean handleFeedPickerClick(double mouseX, double mouseY) {
        FeedPickerLayout picker = feedPickerLayout();
        if (!picker.panel().contains(mouseX, mouseY)) {
            feedPickerOpen = false;
            playUiClick(0.88F);
            return true;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            Rect rect = feedSlot(picker, slot);
            if (rect == null || !rect.contains(mouseX, mouseY)) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            if (!canFeed(stack)) {
                feedPickerNote = stack.isEmpty() ? "That slot is empty" : "That isn't dino food";
                playUiClick(0.72F);
                return true;
            }
            playUiClick(1.15F);
            ClientPacketDistributor.sendToServer(new FeedDodoPayload(dodo.getId(), slot));
            state.mood = Math.min(100, state.mood + 3);
            state.notice = companionName() + " happily takes the " + stack.getHoverName().getString().toLowerCase() + ".";
            feedPickerOpen = false;
            pressed(Action.FEED);
            return true;
        }
        return true;
    }

    private void drawPaperPanel(GuiGraphicsExtractor graphics, Rect panel) {
        graphics.fill(panel.x() + 4, panel.y() + 5, panel.right() + 4, panel.bottom() + 5, 0x46000000);
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), EDGE_DARK);
        graphics.fill(panel.x() + 2, panel.y() + 2, panel.right() - 2, panel.bottom() - 2, EDGE);
        graphics.fill(panel.x() + 4, panel.y() + 4, panel.right() - 4, panel.bottom() - 4, PAPER);
        for (int y = panel.y() + 7; y < panel.bottom() - 5; y += 7) {
            graphics.fill(panel.x() + 5, y, panel.right() - 5, y + 1, 0x12FFFFFF);
        }
        graphics.fill(panel.x() + 2, panel.y() + 2, panel.x() + 10, panel.y() + 4, PAPER_LIGHT);
        graphics.fill(panel.right() - 10, panel.y() + 2, panel.right() - 2, panel.y() + 4, PAPER_LIGHT);
        graphics.fill(panel.x() + 2, panel.bottom() - 4, panel.x() + 10, panel.bottom() - 2, PAPER_DARK);
        graphics.fill(panel.right() - 10, panel.bottom() - 4, panel.right() - 2, panel.bottom() - 2, PAPER_DARK);
    }

    private void drawBubble(GuiGraphicsExtractor graphics, Rect rect, int border) {
        int safeBorder = Math.max(1, Math.min(border, Math.min(rect.width(), rect.height()) / 2));
        int sourceBorder = 2;
        int middleWidth = rect.width() - safeBorder * 2;
        int middleHeight = rect.height() - safeBorder * 2;

        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x(), rect.y(), safeBorder, safeBorder), 0, 0, sourceBorder, sourceBorder, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x() + safeBorder, rect.y(), middleWidth, safeBorder), sourceBorder, 0, 82, sourceBorder, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.right() - safeBorder, rect.y(), safeBorder, safeBorder), 84, 0, sourceBorder, sourceBorder, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x(), rect.y() + safeBorder, safeBorder, middleHeight), 0, sourceBorder, sourceBorder, 10, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x() + safeBorder, rect.y() + safeBorder, middleWidth, middleHeight), sourceBorder, sourceBorder, 82, 10, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.right() - safeBorder, rect.y() + safeBorder, safeBorder, middleHeight), 84, sourceBorder, sourceBorder, 10, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x(), rect.bottom() - safeBorder, safeBorder, safeBorder), 0, 12, sourceBorder, sourceBorder, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x() + safeBorder, rect.bottom() - safeBorder, middleWidth, safeBorder), sourceBorder, 12, 82, sourceBorder, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.right() - safeBorder, rect.bottom() - safeBorder, safeBorder, safeBorder), 84, 12, sourceBorder, sourceBorder, 86, 14);
    }

    private void glow(GuiGraphicsExtractor graphics, Rect rect, float time) {
        int pulse = Math.round((Mth.sin(time * 0.12F) + 1.0F) * 10.0F);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), withAlpha(0xFFFFFFFF, 22 + pulse));
        outline(graphics, rect, withAlpha(0xFFFFFFFF, 82 + pulse));
    }

    private void outline(GuiGraphicsExtractor graphics, Rect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y() + 1, rect.x() + 1, rect.bottom() - 1, color);
        graphics.fill(rect.right() - 1, rect.y() + 1, rect.right(), rect.bottom() - 1, color);
    }

    private int mutationFrame(float time, int slotIndex, boolean hovered) {
        if (hovered) {
            return Math.floorMod((int) (time / 2.0F), MUTATION_FRAMES.length);
        }
        int cycle = Math.floorMod((int) time + slotIndex * 47, 100);
        return cycle < MUTATION_FRAMES.length * 2 ? cycle / 2 : 0;
    }

    private int mutationColor(MutationKind kind) {
        return switch (kind) {
            case HUGE -> 0xFFFFC85A;
            case ALBINO -> 0xFFF3EEE6;
        };
    }

    private void selectSpecialty(int index) {
        state.assignmentIndex = index;
        state.notice = "Now looking for " + SPECIALTIES[index].toLowerCase() + " work.";
    }

    private void requestCommandMode(DinosaurCommandMode mode) {
        ClientPacketDistributor.sendToServer(new DinosaurCommandPayload(dodo.getId(), mode.ordinal()));
    }

    private static DinosaurCommandMode nextCommandMode(DinosaurCommandMode mode) {
        return switch (mode) {
            case HOME -> DinosaurCommandMode.STAY;
            case STAY -> DinosaurCommandMode.FOLLOW;
            case FOLLOW -> DinosaurCommandMode.HOME;
        };
    }

    private static int commandColor(DinosaurCommandMode mode) {
        return switch (mode) {
            case HOME -> 0xFF547B3F;
            case STAY -> 0xFFB67845;
            case FOLLOW -> 0xFF477895;
        };
    }

    public static void acceptCommandState(DinosaurCommandStatePayload payload) {
        if (!(net.minecraft.client.Minecraft.getInstance().screen instanceof CompanionScreen screen)
                || screen.dodo.getId() != payload.entityId()) return;
        screen.state.commandMode = DinosaurCommandMode.byId(payload.mode());
        screen.state.followers = Math.max(0, payload.followers());
        screen.state.followerLimit = Math.max(1, payload.followerLimit());
        if (!payload.message().isBlank()) screen.state.notice = payload.message();
    }

    private String shortJobDescription(int index) {
        return switch (index) {
            case 0 -> "Moves items between containers.";
            case 1 -> "Runs furnaces and heat stations.";
            case 2 -> "Supplies the base power network.";
            case 3 -> "Works through crafting queues.";
            default -> "Leaves on timed routes and returns with rewards.";
        };
    }

    private int specialtyStars(int index) {
        return dodo.getSpecialtyStars(index);
    }

    private String specialtySpeedLabel(int index) {
        return dodo.getWorkEfficiencyPercent(index) + "% WORK SPEED";
    }

    private String specialtySpeedDetail(int index) {
        int efficiency = dodo.getWorkEfficiencyPercent(index);
        return switch (specialtyStars(index)) {
            case 4 -> "Excellent • full work speed";
            case 3 -> "Strong • " + efficiency + "% work speed";
            case 2 -> "Can help • " + efficiency + "% off-specialty cap";
            default -> "Struggles here • " + efficiency + "% work speed";
        };
    }

    private String hungerNote() {
        int hunger = dodo.getHunger();
        if (hunger < 25) {
            return "Too hungry to work well. Feed it soon.";
        }
        if (hunger < 55) {
            return "Could use a meal before the next shift.";
        }
        return "Well fed and ready to work.";
    }

    private String healthNote() {
        int health = healthPercent();
        if (health < 30) {
            return "Badly hurt and preparing to return home.";
        }
        if (health < 70) {
            return "Bruised, but still able to move safely.";
        }
        return "Healthy and steady.";
    }

    private String moodNote() {
        if (state.mood < 30) {
            return "Unhappy; work speed is heavily reduced.";
        }
        if (state.mood < 70) {
            return "Doing alright, but comfort would help.";
        }
        return "Happy and comfortable around the base.";
    }

    private boolean canFeed(ItemStack stack) {
        return dodo.canEat(stack);
    }

    private int healthPercent() {
        return dodo.getMaxHealth() <= 0.0F
                ? 0
                : Mth.clamp(Math.round(dodo.getHealth() / dodo.getMaxHealth() * 100.0F), 0, 100);
    }

    private String companionName() {
        if (dodo.hasCustomName()) {
            return dodo.getDisplayName().getString();
        }
        return isTyrannosaurus() ? "Rex" : "Moss";
    }

    private String speciesName() {
        return dodo.getType().getDescription().getString();
    }

    private boolean isTyrannosaurus() {
        return dodo.getType() == ModEntities.TYRANNOSAURUS.get();
    }

    private String passiveTitle() {
        return dodo.getSpecies().passiveTitle();
    }

    private int passiveColor() {
        return dodo.getSpecies().passiveColor();
    }

    private String passiveSummary() {
        return dodo.getSpecies().passiveSummary();
    }

    private String passiveDetail() {
        return dodo.getSpecies().passiveDetail()
                + " Passive strength: " + Math.round(dodo.getPassiveStrength() * 100.0F) + "%.";
    }

    private void pressed(Action action) {
        lastAction = action;
        actionPressedNanos = Util.getNanos();
    }

    private void restartTransition() {
        transitionStartedNanos = Util.getNanos();
        textHoverDwells.clear();
    }

    private float elapsedTicks(long startedNanos) {
        return (renderNowNanos - startedNanos) / 50_000_000.0F;
    }

    private boolean feedbackActive() {
        return actionPressedNanos > 0L && renderNowNanos - actionPressedNanos < 450_000_000L;
    }

    private void playUiClick(float pitch) {
        PrimevalUiSounds.click(pitch);
    }

    private Layout layout() {
        float horizontalScale = (width - 14.0F) / (ART_WIDTH + ACTION_EXTENSION_WIDTH);
        float verticalScale = (height - 10.0F) / ART_HEIGHT;
        float scale = Mth.clamp(Math.min(1.35F, Math.min(horizontalScale, verticalScale)), 0.78F, 1.35F);
        int totalWidth = Math.round((ART_WIDTH + ACTION_EXTENSION_WIDTH) * scale);
        int panelHeight = Math.round(ART_HEIGHT * scale);
        return new Layout((width - totalWidth) / 2, (height - panelHeight) / 2, scale);
    }

    private JobMenuLayout jobMenuLayout() {
        int panelWidth = Math.min(398, width - 14);
        int panelHeight = Math.min(222, height - 10);
        Rect panel = new Rect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        int rowGap = 3;
        int rowStart = panel.y() + 34;
        int footerHeight = 24;
        int rowArea = panel.height() - 34 - footerHeight;
        int rowHeight = (rowArea - rowGap * 5) / 6;
        int footerY = panel.bottom() - 21;
        Rect footerText = new Rect(panel.x() + 13, footerY, 94, 17);
        Rect worksite = new Rect(panel.x() + 111, footerY, 116, 17);
        Rect cancel = new Rect(panel.x() + 231, footerY, 70, 17);
        Rect confirm = new Rect(panel.x() + 305, footerY, panel.right() - panel.x() - 317, 17);
        return new JobMenuLayout(panel, rowStart, rowHeight, rowGap, footerText, worksite, cancel, confirm);
    }

    private FeedPickerLayout feedPickerLayout() {
        int panelWidth = 208;
        int panelHeight = 124;
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        return new FeedPickerLayout(new Rect(x, y, panelWidth, panelHeight), x + 14, y + 30);
    }

    private Rect feedSlot(FeedPickerLayout picker, int inventorySlot) {
        if (inventorySlot >= 9 && inventorySlot < 36) {
            int index = inventorySlot - 9;
            return new Rect(picker.gridX() + index % 9 * 20, picker.gridY() + index / 9 * 20, 20, 20);
        }
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return new Rect(picker.gridX() + inventorySlot * 20, picker.gridY() + 65, 20, 20);
        }
        return null;
    }

    private Rect global(Layout layout, Rect local) {
        int x = layout.x() + Math.round((local.x() - ART_X) * layout.scale());
        int y = layout.y() + Math.round((local.y() - ART_Y) * layout.scale());
        int width = Math.max(1, Math.round(local.width() * layout.scale()));
        int height = Math.max(1, Math.round(local.height() * layout.scale()));
        return new Rect(x, y, width, height);
    }

    private UiMotion mainMotion(Rect panel, int mouseX, int mouseY, float time) {
        float progress = Mth.clamp(time / 34.0F, 0.0F, 1.0F);
        float settled = spring(progress, 7.2F, 10.5F);
        float mouseParallaxX = parallaxX;
        float mouseParallaxY = parallaxY;
        float parallaxFade = smoothStep(Mth.clamp(time / 24.0F, 0.0F, 1.0F));
        float offsetX = (width * 0.72F + panel.width() * 0.35F) * (1.0F - settled) + mouseParallaxX * parallaxFade;
        float offsetY = mouseParallaxY * parallaxFade;
        float scale = 0.955F + 0.045F * settled;
        return new UiMotion(panel.centerX(), panel.centerY(), offsetX, offsetY, scale, mouseParallaxX, mouseParallaxY);
    }

    private UiMotion popupMotion(Rect panel, float time) {
        float progress = Mth.clamp(time / 24.0F, 0.0F, 1.0F);
        float settled = spring(progress, 6.2F, 11.4F);
        float scale = 0.74F + 0.26F * settled;
        float offsetY = 18.0F * (1.0F - settled);
        return new UiMotion(panel.centerX(), panel.centerY(), 0.0F, offsetY, scale, 0.0F, 0.0F);
    }

    private void applyMotion(GuiGraphicsExtractor graphics, UiMotion motion) {
        graphics.pose().translate(motion.pivotX() + motion.offsetX(), motion.pivotY() + motion.offsetY());
        graphics.pose().scale(motion.scale(), motion.scale());
        graphics.pose().translate(-motion.pivotX(), -motion.pivotY());
    }

    private void extractSmoothDinosaurPreview(GuiGraphicsExtractor graphics, FloatRect target, float scale,
                                              float mouseX, float mouseY, FieldDodoEntity dinosaur) {
        int x0 = Mth.ceil(target.x());
        int y0 = Mth.ceil(target.y());
        int x1 = Mth.floor(target.right());
        int y1 = Mth.floor(target.bottom());
        if (x1 <= x0 || y1 <= y0) return;
        float renderCenterX = (x0 + x1) * 0.5F;
        float renderCenterY = (y0 + y1) * 0.5F;
        float xAngle = (float)Math.atan((target.centerX() - mouseX) / 40.0F);
        float yAngle = (float)Math.atan((target.centerY() - mouseY) / 40.0F);

        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * Mth.DEG_TO_RAD);
        rotation.mul(xRotation);
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<? super FieldDodoEntity, ?> renderer = dispatcher.getRenderer(dinosaur);
        EntityRenderState renderState = renderer.createRenderState(dinosaur, 1.0F);
        DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 180.0F + xAngle * 20.0F;
            livingState.yRot = xAngle * 20.0F;
            livingState.xRot = livingState.pose == Pose.FALL_FLYING ? 0.0F : -yAngle * 20.0F;
            livingState.boundingBoxWidth /= livingState.scale;
            livingState.boundingBoxHeight /= livingState.scale;
            livingState.scale = 1.0F;
        }
        float inverseScale = 1.0F / Math.max(0.001F, scale);
        Vector3f translation = new Vector3f(
                (target.centerX() - renderCenterX) * inverseScale,
                renderState.boundingBoxHeight * 0.5F + visual.modelGroundOffset()
                        + (target.centerY() - renderCenterY) * inverseScale,
                0.0F
        );
        graphics.entity(renderState, scale, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    private void withTextMotion(
            GuiGraphicsExtractor graphics,
            Rect bounds,
            float time,
            boolean hovered,
            boolean pressed,
            Runnable draw
    ) {
        HoverDwell hoverState = textHoverDwells.computeIfAbsent(bounds, ignored -> new HoverDwell());
        if (hovered) {
            if (hoverState.startedNanos == 0L) {
                hoverState.startedNanos = renderNowNanos;
            }
        } else {
            hoverState.startedNanos = 0L;
        }
        if (!hovered && !pressed) {
            draw.run();
            return;
        }
        float hoverSeconds = hovered ? (renderNowNanos - hoverState.startedNanos) / 1_000_000_000.0F : 0.0F;
        float hoverAmount = hovered
                ? (1.0F - (float) Math.exp(-hoverSeconds * 18.0F)) * (float) Math.exp(-hoverSeconds * 2.8F)
                : 0.0F;
        if (hoverSeconds >= 1.35F) {
            hoverAmount = 0.0F;
        }
        float pressSeconds = actionPressedNanos == 0L ? 1.0F : (renderNowNanos - actionPressedNanos) / 1_000_000_000.0F;
        float pressAmount = pressed ? Mth.clamp(1.0F - pressSeconds / 0.32F, 0.0F, 1.0F) : 0.0F;
        float wobbleX = Mth.sin(time * 0.48F) * 0.55F * hoverAmount;
        float wobbleY = Mth.sin(time * 0.61F + 1.7F) * 0.32F * hoverAmount;
        float scale = 1.0F + Mth.sin(time * 0.43F) * 0.012F * hoverAmount - Mth.sin(pressAmount * Mth.PI) * 0.045F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.centerX() + wobbleX, bounds.centerY() + wobbleY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-bounds.centerX(), -bounds.centerY());
        draw.run();
        graphics.pose().popMatrix();
    }

    private static float spring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) {
            return 1.0F;
        }
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float) (Math.exp(-damping * progress) * wave);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void blit(GuiGraphicsExtractor graphics, Identifier texture, Rect rect) {
        graphics.blit(texture, rect.x(), rect.y(), rect.right(), rect.bottom(), 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private void blitTinted(GuiGraphicsExtractor graphics, Identifier texture, Rect rect, int color) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                rect.x(),
                rect.y(),
                0.0F,
                0.0F,
                rect.width(),
                rect.height(),
                rect.width(),
                rect.height(),
                color
        );
    }

    private void blitRegion(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            Rect target,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            int textureWidth,
            int textureHeight
    ) {
        if (target.width() <= 0 || target.height() <= 0) {
            return;
        }
        graphics.blit(
                texture,
                target.x(),
                target.y(),
                target.right(),
                target.bottom(),
                sourceX / (float) textureWidth,
                (sourceX + sourceWidth) / (float) textureWidth,
                sourceY / (float) textureHeight,
                (sourceY + sourceHeight) / (float) textureHeight
        );
    }

    private void fittedCenteredText(
            GuiGraphicsExtractor graphics,
            String value,
            Rect rect,
            int color,
            float minimumScale,
            boolean bold
    ) {
        Component component = styled(value, bold);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(1.0F, (rect.width() - 4.0F) / measuredWidth);
        int renderedWidth = Math.round(measuredWidth * scale);
        float x = rect.centerX() - renderedWidth / 2.0F;
        float y = rect.y() + (rect.height() - font().lineHeight * scale) / 2.0F;
        drawScaledComponent(graphics, component, x, y, scale, color);
    }

    private void fittedCenteredGradientText(
            GuiGraphicsExtractor graphics,
            String value,
            Rect rect,
            int startColor,
            int endColor,
            float minimumScale
    ) {
        Component component = gradientComponent(value, startColor, endColor);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(1.0F, (rect.width() - 4.0F) / measuredWidth);
        int renderedWidth = Math.round(measuredWidth * scale);
        float x = rect.centerX() - renderedWidth / 2.0F;
        float y = rect.y() + (rect.height() - font().lineHeight * scale) / 2.0F;
        drawScaledComponent(graphics, component, x, y, scale, 0xFFFFFFFF);
    }

    private void textLine(
            GuiGraphicsExtractor graphics,
            String value,
            int x,
            int y,
            int maxWidth,
            int color,
            float minimumScale,
            boolean bold
    ) {
        Component component = styled(value, bold);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(1.0F, maxWidth / (float) measuredWidth);
        drawScaledComponent(graphics, component, x, y, scale, color);
    }

    private void compactTextLine(
            GuiGraphicsExtractor graphics,
            String value,
            int x,
            int y,
            int maxWidth,
            int color,
            float preferredScale,
            boolean bold
    ) {
        Component component = styled(value, bold);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(preferredScale, maxWidth / (float) measuredWidth);
        drawScaledComponent(graphics, component, x, y, Math.max(0.01F, scale), color);
    }

    private void gradientTextLine(
            GuiGraphicsExtractor graphics,
            String value,
            int x,
            int y,
            int maxWidth,
            int startColor,
            int endColor,
            float minimumScale
    ) {
        Component component = gradientComponent(value, startColor, endColor);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(1.0F, maxWidth / (float) measuredWidth);
        drawScaledComponent(graphics, component, x, y, scale, 0xFFFFFFFF);
    }

    private void compactGradientTextLine(
            GuiGraphicsExtractor graphics,
            String value,
            int x,
            int y,
            int maxWidth,
            int startColor,
            int endColor,
            float preferredScale
    ) {
        Component component = gradientComponent(value, startColor, endColor);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(preferredScale, maxWidth / (float) measuredWidth);
        drawScaledComponent(graphics, component, x, y, Math.max(0.01F, scale), 0xFFFFFFFF);
    }

    private void rightText(
            GuiGraphicsExtractor graphics,
            String value,
            int right,
            int y,
            int maxWidth,
            int color,
            float minimumScale,
            boolean bold
    ) {
        Component component = styled(value, bold);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(1.0F, maxWidth / (float) measuredWidth);
        drawScaledComponent(graphics, component, right - measuredWidth * scale, y, scale, color);
    }

    private void drawScaledComponent(
            GuiGraphicsExtractor graphics,
            Component component,
            float x,
            float y,
            float scale,
            int color
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font(), component, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private Component styled(String value, boolean bold) {
        return bold ? Component.literal(value).withStyle(style -> style.withBold(true)) : Component.literal(value);
    }

    private Component gradientComponent(String value, int startColor, int endColor) {
        var result = Component.empty();
        int denominator = Math.max(1, value.length() - 1);
        for (int index = 0; index < value.length(); index++) {
            int color = lerpRgb(startColor, endColor, index / (float) denominator);
            String character = String.valueOf(value.charAt(index));
            result.append(Component.literal(character).withStyle(style -> style
                    .withBold(true)
                    .withColor(color & 0x00FFFFFF)));
        }
        return result;
    }

    private int displayWidth(String value, boolean bold) {
        return font().width(styled(value, bold));
    }

    private Font font() {
        return minecraft.font;
    }

    private static float approach(float current, float target) {
        float difference = target - current;
        return Math.abs(difference) < 0.05F ? target : current + difference * 0.22F;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/gui/" + name);
    }

    private static Identifier[] textureFrames(String name, int count) {
        Identifier[] frames = new Identifier[count];
        for (int index = 0; index < count; index++) {
            frames[index] = texture(name + (index + 1) + ".png");
        }
        return frames;
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpRgb(int start, int end, float amount) {
        float clamped = Mth.clamp(amount, 0.0F, 1.0F);
        int red = Math.round(((start >> 16) & 0xFF) + (((end >> 16) & 0xFF) - ((start >> 16) & 0xFF)) * clamped);
        int green = Math.round(((start >> 8) & 0xFF) + (((end >> 8) & 0xFF) - ((start >> 8) & 0xFF)) * clamped);
        int blue = Math.round((start & 0xFF) + ((end & 0xFF) - (start & 0xFF)) * clamped);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static Rect inset(Rect rect, int amount) {
        return new Rect(rect.x + amount, rect.y + amount,
                Math.max(1, rect.width - amount * 2), Math.max(1, rect.height - amount * 2));
    }

    private static FloatRect inset(FloatRect rect, float amount) {
        return new FloatRect(rect.x + amount, rect.y + amount,
                Math.max(1.0F, rect.width - amount * 2.0F), Math.max(1.0F, rect.height - amount * 2.0F));
    }

    private enum Action {
        FEED,
        JOBS,
        COMMAND
    }

    private enum MutationKind {
        HUGE,
        ALBINO
    }

    private record Mutation(MutationKind kind, String name, String effect) {
    }

    private record HoverInfo(String title, String detail, int color, int detailColor) {
        private HoverInfo(String title, String detail, int color) {
            this(title, detail, color, INK);
        }
    }

    private record Rect(int x, int y, int width, int height) {
        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private int centerX() {
            return x + width / 2;
        }

        private int centerY() {
            return y + height / 2;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private record Layout(int x, int y, float scale) {
        private Rect panel() {
            return new Rect(x, y, Math.round(ART_WIDTH * scale), Math.round(ART_HEIGHT * scale));
        }
    }

    private record UiMotion(
            float pivotX,
            float pivotY,
            float offsetX,
            float offsetY,
            float scale,
            float parallaxX,
            float parallaxY
    ) {
        private float inverseX(float screenX) {
            return pivotX + (screenX - pivotX - offsetX) / Math.max(0.01F, scale);
        }

        private float inverseY(float screenY) {
            return pivotY + (screenY - pivotY - offsetY) / Math.max(0.01F, scale);
        }

        private Rect transform(Rect rect, float extraX, float extraY) {
            int x = Math.round(pivotX + (rect.x() - pivotX) * scale + offsetX + extraX);
            int y = Math.round(pivotY + (rect.y() - pivotY) * scale + offsetY + extraY);
            int width = Math.max(1, Math.round(rect.width() * scale));
            int height = Math.max(1, Math.round(rect.height() * scale));
            return new Rect(x, y, width, height);
        }

        private FloatRect transformSmooth(Rect rect, float extraX, float extraY) {
            return new FloatRect(
                    pivotX + (rect.x() - pivotX) * scale + offsetX + extraX,
                    pivotY + (rect.y() - pivotY) * scale + offsetY + extraY,
                    Math.max(1.0F, rect.width() * scale),
                    Math.max(1.0F, rect.height() * scale)
            );
        }
    }

    private record FloatRect(float x, float y, float width, float height) {
        private float right() { return x + width; }
        private float bottom() { return y + height; }
        private float centerX() { return x + width * 0.5F; }
        private float centerY() { return y + height * 0.5F; }
    }

    private record JobMenuLayout(
            Rect panel,
            int rowStart,
            int rowHeight,
            int rowGap,
            Rect footerText,
            Rect worksite,
            Rect cancel,
            Rect confirm
    ) {
        private Rect row(int index) {
            return new Rect(panel.x() + 12, rowStart + index * (rowHeight + rowGap), panel.width() - 24, rowHeight);
        }
    }

    private record FeedPickerLayout(Rect panel, int gridX, int gridY) {
    }

    private static final class PrototypeState {
        private int mood;
        private int assignmentIndex;
        private DinosaurCommandMode commandMode;
        private int followers;
        private int followerLimit;
        private String notice;

        private static PrototypeState create(FieldDodoEntity dodo) {
            PrototypeState state = new PrototypeState();
            state.mood = dodo.getMood();
            state.assignmentIndex = dodo.getWorkJobIndex();
            state.commandMode = dodo.getCommandMode();
            state.followerLimit = 1;
            state.notice = dodo.getType() == ModEntities.TYRANNOSAURUS.get()
                    ? "Watching the base perimeter."
                    : "Pecking loose berries into a tidy pile.";
            return state;
        }

        private String assignment() {
            return SPECIALTIES[assignmentIndex];
        }
    }

    private static final class HoverDwell {
        private long startedNanos;
    }
}
