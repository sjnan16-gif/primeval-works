package com.primevalworks.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.client.model.entity.DinosaurPreviewBounds;
import com.primevalworks.network.payload.BaseUpgradesPayload;
import com.primevalworks.network.payload.BaseEnergyPayload;
import com.primevalworks.network.payload.CommandTableActionPayload;
import com.primevalworks.network.payload.DinosaurRosterPayload;
import com.primevalworks.network.payload.PurchaseBaseUpgradePayload;
import com.primevalworks.network.payload.RequestBaseUpgradesPayload;
import com.primevalworks.network.payload.RequestBaseEnergyPayload;
import com.primevalworks.network.payload.SwapActiveDinosaurPayload;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.world.base.BaseUpgrade;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CommandTableTestScreen extends Screen {
    private static final int TEXTURE_WIDTH = 427;
    private static final int TEXTURE_HEIGHT = 240;
    private static final int ART_X = 110;
    private static final int ART_Y = 46;
    private static final int ART_WIDTH = 195;
    private static final int ART_HEIGHT = 148;
    private static final int DEPOT_X = 307;
    private static final int DEPOT_Y = 46;
    private static final int DEPOT_WIDTH = 109;
    private static final int DEPOT_HEIGHT = 153;
    static final int DEPOT_PREVIEW_X_STRIDE = 26;
    static final int DEPOT_PREVIEW_Y_STRIDE = 27;
    private static final int ACTIVE_PAGE_SIZE = 7;
    private static final float MAX_PANEL_SCALE = 1.46F;
    private static final float PANEL_SIZE_MULTIPLIER = 0.88F;
    private static final int TREE_NODE_SIZE = 24;
    private static final float TREE_COORDINATE_SCALE = 0.25F;
    private static final float ROSTER_VIEW_YAW = 45.0F;
    private static final float ROSTER_VIEW_PITCH = -30.0F;
    private static final Rect[] ACTIVE_PREVIEWS = {
            new Rect(115, 51, 23, 23), new Rect(142, 51, 23, 23),
            new Rect(169, 51, 23, 23), new Rect(196, 51, 23, 23),
            new Rect(223, 51, 23, 23), new Rect(250, 51, 23, 23),
            new Rect(277, 51, 23, 23)
    };

    private static final Identifier CONTROL_TABLE = texture("control_table.png");
    private static final Identifier LEVELS_BACK = texture("levels_back.png");
    private static final Identifier HOTBAR = texture("hotbar.png");
    private static final Identifier SPACE = texture("space.png");

    private static final Rect TREE_VIEW = new Rect(112, 123, 191, 69);
    private static final Rect ROSTER_PAGE = new Rect(252, 31, 53, 14);
    private static final Rect ENERGY_GRAY = new Rect(94, 46, 14, 134);
    private static final Rect ENERGY_FILL_SOURCE = new Rect(81, 47, 10, 131);
    private static final Rect ENERGY_LABEL = new Rect(80, 180, 29, 14);
    private static final Rect HUNGER_METER_SOURCE = new Rect(50, 72, 23, 2);
    private static final Rect MOOD_METER_SOURCE = new Rect(50, 75, 23, 2);
    private static final Rect HEALTH_METER_SOURCE = new Rect(50, 78, 23, 2);
    private static final Rect[] ACTIONS = {
            new Rect(114, 90, 45, 24),
            new Rect(161, 90, 45, 24),
            new Rect(208, 90, 44, 24),
            new Rect(254, 90, 45, 24)
    };
    private static final Rect DEPOT_SORT = new Rect(310, 49, 51, 14);
    private static final Rect DEPOT_PAGE = new Rect(360, 49, 53, 14);
    private static final String[] ACTION_LABELS = {"STORE ALL", "RECALL", "ENERGY", "DEPOT"};
    private static final String[] ACTION_HELP = {
            "Return every active companion to the depot.",
            "Bring all seven active companions back to the table.",
            "Inspect generation, capacity, and every powered block in this base.",
            "Open the dinosaur depot and edit the seven-dinosaur base crew."
    };
    private static final int[] ACTION_COLORS = {0xFFE8A15A, 0xFF88B8D0, 0xFF9DCA78, 0xFFD87568};

    private static final int INK = 0xFF3F302D;
    private static final int MUTED = 0xFFA897A8;
    private static final int MUTED_DARK = 0xFF756675;
    private static final int GOLD = 0xFFFFD66B;
    private static final int GREEN = 0xFF8ED074;
    private static final int RED = 0xFFE16A62;
    private static final int STAR_LINE = 0xFF66517F;
    private static final int STAR_LIGHT = 0xFFC7B0ED;

    private static CommandTableTestScreen active;

    private final BlockPos tablePos;
    private final List<FieldDodoEntity> activeDinos = new ArrayList<>();
    private final List<DinosaurRosterPayload.Entry> activeEntries = new ArrayList<>();
    private final List<DinosaurRosterPayload.Entry> depotEntries = new ArrayList<>();
    private final List<DinosaurRosterPayload.Entry> recoveryEntries = new ArrayList<>();
    private final Map<UUID, FieldDodoEntity> previewDinos = new HashMap<>();
    private final int[] upgradeLevels = new int[BaseUpgrade.values().length];
    private final Map<Integer, HoverDwell> dinosaurDwells = new HashMap<>();
    private final Map<Integer, HoverDwell> actionDwells = new HashMap<>();
    private final Map<Integer, HoverDwell> nodeDwells = new HashMap<>();

    private int insight;
    private float storedEnergy;
    private float energyCapacity = 500.0F;
    private float energyGenerationPerSecond;
    private int screenTicks;
    private int focusedDinosaur;
    private float treePanX;
    private float treePanY = 240.0F;
    private float treeZoom = 0.9F;
    private float targetTreePanX;
    private float targetTreePanY = 240.0F;
    private float targetTreeZoom = 0.9F;
    private float panelParallaxX;
    private float panelParallaxY;
    private float treeParallaxX;
    private float treeParallaxY;
    private float panelZoom = 1.0F;
    private float depotProgress;
    private float depotVelocity;
    private boolean depotOpen;
    private long depotTransitionNanos;
    private int depotPage;
    private int activePage;
    private SortMode depotSort = SortMode.NAME;
    private DinosaurRosterPayload.Entry draggedDinosaur;
    private boolean draggedFromActive;
    private long dragStartedNanos;
    private float draggedX;
    private float draggedY;
    private float draggedVelocityX;
    private float draggedVelocityY;
    private float draggedYaw;
    private float draggedPitch;
    private boolean draggingTree;
    private long openedNanos = Util.getNanos();
    private long lastRenderNanos = openedNanos;
    private long lastPurchaseNanos;
    private int rippleUpgradeId = -1;
    private boolean receivedBaseState;
    private long renderNowNanos = openedNanos;
    private String notice = "Choose a revealed branch to shape the base.";

    public CommandTableTestScreen(BlockPos tablePos) {
        super(Component.translatable("screen.primevalworks.command_table"));
        this.tablePos = tablePos.immutable();
        upgradeLevels[BaseUpgrade.HEARTHSTONE.id()] = 1;
    }

    public static void acceptBaseState(BaseUpgradesPayload payload) {
        if (active == null || !active.tablePos.equals(payload.tablePos())) return;
        active.insight = payload.insight();
        int upgraded = -1;
        for (int index = 0; index < active.upgradeLevels.length; index++) {
            int next = index < payload.levels().size() ? payload.levels().get(index) : 0;
            if (active.receivedBaseState && next > active.upgradeLevels[index]) upgraded = index;
            active.upgradeLevels[index] = next;
        }
        active.upgradeLevels[BaseUpgrade.HEARTHSTONE.id()] = 1;
        if (!payload.notice().isBlank()) active.notice = payload.notice();
        if (upgraded >= 0) {
            active.lastPurchaseNanos = Util.getNanos();
            active.rippleUpgradeId = upgraded;
        }
        active.receivedBaseState = true;
        active.activePage = Math.min(active.activePage, active.activePageCount() - 1);
    }

    public static void acceptDinosaurRoster(DinosaurRosterPayload payload) {
        if (active == null || !active.tablePos.equals(payload.tablePos())) return;
        var incomingIds = new java.util.HashSet<UUID>();
        active.activeEntries.clear();
        active.depotEntries.clear();
        active.recoveryEntries.clear();
        for (DinosaurRosterPayload.Entry entry : payload.entries()) {
            incomingIds.add(entry.id());
            if (entry.active()) active.activeEntries.add(entry);
            else if (entry.recoveryTicksRemaining() > 0L) active.recoveryEntries.add(entry);
            else active.depotEntries.add(entry);
        }
        active.previewDinos.keySet().removeIf(id -> !incomingIds.contains(id));
        active.rebuildActiveDinos();
        active.activePage = Math.min(active.activePage, active.activePageCount() - 1);
        active.clampDepotPage();
    }

    public static void acceptEnergyState(BaseEnergyPayload payload) {
        if (active == null || !active.tablePos.equals(payload.tablePos())) return;
        active.storedEnergy = payload.stored();
        active.energyCapacity = Math.max(1.0F, payload.capacity());
        active.energyGenerationPerSecond = payload.generationPerSecond();
    }

    @Override
    protected void init() {
        active = this;
        openedNanos = Util.getNanos();
        lastRenderNanos = openedNanos;
        requestBaseState();
    }

    @Override
    public void tick() {
        screenTicks++;
        if (minecraft.level != null && !minecraft.level.getBlockState(tablePos).is(ModBlocks.COMMAND_TABLE.get())) {
            if (minecraft.player != null) {
                minecraft.gui.setOverlayMessage(Component.literal("The Command Table was destroyed."), false);
            }
            minecraft.setScreen(null);
            return;
        }
        if (screenTicks % 20 == 0) requestBaseState();
    }

    @Override
    public void removed() {
        if (active == this) active = null;
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderNowNanos = Util.getNanos();
        float deltaSeconds = Mth.clamp((renderNowNanos - lastRenderNanos) / 1_000_000_000.0F, 0.001F, 0.05F);
        lastRenderNanos = renderNowNanos;
        updateParallax(mouseX, mouseY, deltaSeconds);
        float depotTarget = depotOpen ? 1.0F : 0.0F;
        depotVelocity += (depotTarget - depotProgress) * 112.0F * deltaSeconds;
        depotVelocity *= (float)Math.exp(-16.0F * deltaSeconds);
        depotProgress = Mth.clamp(depotProgress + depotVelocity * deltaSeconds, 0.0F, 1.0F);
        if (Math.abs(depotTarget - depotProgress) < 0.0005F && Math.abs(depotVelocity) < 0.001F) {
            depotProgress = depotTarget;
            depotVelocity = 0.0F;
        }
        if (draggedDinosaur != null) {
            updateDraggedDinosaur(mouseX, mouseY, deltaSeconds);
        }

        float time = (renderNowNanos - openedNanos) / 1_000_000_000.0F;
        Layout layout = layout();
        graphics.fill(0, 0, width, height, 0x1608050D);

        UiMotion motion = mainMotion(layout, time);
        int uiMouseX = Math.round(motion.inverseX(mouseX));
        int uiMouseY = Math.round(motion.inverseY(mouseY));
        graphics.pose().pushMatrix();
        applyMotion(graphics, motion);
        drawPanel(graphics, layout, motion, mouseX, mouseY, uiMouseX, uiMouseY, time);
        graphics.pose().popMatrix();
        drawUpgradeRipple(graphics, layout, motion);
        drawDraggedDinosaur(graphics, mouseX, mouseY, time);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void updateParallax(int mouseX, int mouseY, float deltaSeconds) {
        float normalizedX = Mth.clamp((mouseX - width * 0.5F) / Math.max(1.0F, width * 0.5F), -1.0F, 1.0F);
        float normalizedY = Mth.clamp((mouseY - height * 0.5F) / Math.max(1.0F, height * 0.5F), -1.0F, 1.0F);
        panelParallaxX = follow(panelParallaxX, normalizedX * 2.4F, 12.0F, deltaSeconds);
        panelParallaxY = follow(panelParallaxY, normalizedY * 1.7F, 12.0F, deltaSeconds);
        treeParallaxX = follow(treeParallaxX, normalizedX * 11.0F, 9.5F, deltaSeconds);
        treeParallaxY = follow(treeParallaxY, normalizedY * 7.5F, 9.5F, deltaSeconds);
        treePanX = follow(treePanX, targetTreePanX, 17.0F, deltaSeconds);
        treePanY = follow(treePanY, targetTreePanY, 17.0F, deltaSeconds);
        treeZoom = follow(treeZoom, targetTreeZoom, 12.0F, deltaSeconds);
        float zoomTarget = 1.0F + (Math.abs(normalizedX) + Math.abs(normalizedY)) * 0.006F;
        panelZoom = follow(panelZoom, zoomTarget, 9.0F, deltaSeconds);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Layout layout, UiMotion motion,
                           int screenMouseX, int screenMouseY, int mouseX, int mouseY, float time) {
        Rect panel = layout.panel;
        graphics.fill(panel.x + 4, panel.y + 6, panel.right() + 4, panel.bottom() + 6, 0x70000000);
        blitRegion(graphics, CONTROL_TABLE, panel, ART_X, ART_Y, ART_WIDTH, ART_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        drawRosterPage(graphics, layout, mouseX, mouseY);
        drawEnergyMeter(graphics, layout, mouseX, mouseY);

        Rect treeView = global(layout, TREE_VIEW);
        graphics.enableScissor(treeView.x, treeView.y, treeView.right(), treeView.bottom());
        drawTreeBackdrop(graphics, treeView, mouseX, mouseY, time);
        graphics.pose().pushMatrix();
        graphics.pose().translate(treeParallaxX, treeParallaxY);
        drawConnections(graphics, layout);
        drawUpgradeNodes(graphics, layout, Math.round(mouseX - treeParallaxX),
                Math.round(mouseY - treeParallaxY), time);
        graphics.pose().popMatrix();
        graphics.disableScissor();

        drawDinosaurs(graphics, layout, motion, screenMouseX, screenMouseY, mouseX, mouseY, time);
        drawActions(graphics, layout, mouseX, mouseY, time);
        drawTreeOverlay(graphics, layout, mouseX, mouseY, time);
        if (depotProgress > 0.015F) {
            drawDepot(graphics, layout, motion, screenMouseX, screenMouseY, mouseX, mouseY, time);
        }
    }

    private void drawTreeBackdrop(GuiGraphicsExtractor graphics, Rect viewport, int mouseX, int mouseY, float time) {
        float smoothMouseX = treeParallaxX / 11.0F;
        float smoothMouseY = treeParallaxY / 7.5F;
        float driftX = Mth.sin(time * 0.34F) * 0.016F + smoothMouseX * 0.052F - treePanX * 0.00018F;
        float driftY = Mth.cos(time * 0.29F) * 0.012F + smoothMouseY * 0.040F - treePanY * 0.00012F;
        float u0 = Mth.clamp(0.11F + driftX, 0.0F, 0.42F);
        float v0 = Mth.clamp(0.18F + driftY, 0.0F, 0.56F);
        graphics.blit(LEVELS_BACK, viewport.x, viewport.y, viewport.right(), viewport.bottom(),
                u0, u0 + 0.58F, v0, v0 + 0.42F);
        graphics.fill(viewport.x, viewport.y, viewport.right(), viewport.bottom(), 0x10000000);
        int horizon = viewport.y + Math.round(viewport.height * 0.72F + treeParallaxY * 0.15F);
        graphics.fill(viewport.x, horizon, viewport.right(), viewport.bottom(), 0x26000000);
    }

    private void drawDinosaurs(GuiGraphicsExtractor graphics, Layout layout, UiMotion motion,
                               int screenMouseX, int screenMouseY, int mouseX, int mouseY, float time) {
        DinosaurRosterPayload.Entry hoveredEntry = null;
        Rect hoveredSlot = null;
        int capacity = clientActiveCapacity();
        int pageStart = activePage * ACTIVE_PAGE_SIZE;
        for (int index = 0; index < ACTIVE_PAGE_SIZE; index++) {
            int rosterIndex = pageStart + index;
            Rect localSlot = dinosaurSlot(index);
            Rect slot = global(layout, localSlot);
            boolean locked = rosterIndex >= capacity;
            DinosaurRosterPayload.Entry entry = rosterIndex < activeEntries.size() ? activeEntries.get(rosterIndex) : null;
            FieldDodoEntity dinosaur = rosterIndex < activeDinos.size() ? activeDinos.get(rosterIndex) : null;
            boolean populated = entry != null && dinosaur != null;
            boolean expedition = entry != null && entry.onExpedition();
            boolean hovered = slot.contains(mouseX, mouseY);
            if (hovered && populated) focusedDinosaur = rosterIndex;
            float dwell = hoverAmount(dinosaurDwells, rosterIndex, hovered);
            float wobble = hovered ? dwell * Mth.sin(time * 12.0F + index * 0.7F) : 0.0F;
            if ((draggedDinosaur != null || hovered) && !locked) {
                drawSlotEdgeGlow(graphics, slot, time, hovered ? 1.0F : 0.35F);
            }
            FloatRect authoredPreview = globalSmooth(layout, ACTIVE_PREVIEWS[index]);
            if (!populated) {
                int centerX = slot.centerX();
                int centerY = slot.y + Math.round(slot.height * 0.40F);
                int emptyColor = locked ? 0x723A3035 : 0x6B725D58;
                graphics.fill(centerX - 3, centerY, centerX + 4, centerY + 1, emptyColor);
                graphics.fill(centerX, centerY - 3, centerX + 1, centerY + 4, emptyColor);
            } else if (!(draggedFromActive && draggedDinosaur != null
                    && draggedDinosaur.id().equals(entry.id()))) {
                // The model may react inside its window, but its scissor never follows
                // hover wobble/zoom outside the exact authored 23x23 interior.
                FloatRect renderedPreview = inset(motion.transformSmooth(authoredPreview, 0.0F, 0.0F), 0.35F);
                DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
                float previewScale = commandTablePreviewScale(renderedPreview, visual) * (1.0F + dwell * 0.018F);
                extractSmoothDinosaurPreview(graphics, renderedPreview, previewScale, dinosaur,
                        ROSTER_VIEW_YAW + Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F) * 5.5F + wobble,
                        ROSTER_VIEW_PITCH + Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F) * 2.5F,
                        Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F),
                        Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F));
            }
            if (populated) {
                drawAuthoredMeter(graphics, layout, localSlot, entry.hunger(), 27, HUNGER_METER_SOURCE);
                drawAuthoredMeter(graphics, layout, localSlot, entry.mood(), 30, MOOD_METER_SOURCE);
                int health = Math.round(100.0F * entry.health() / Math.max(1.0F, entry.maxHealth()));
                drawAuthoredMeter(graphics, layout, localSlot, health, 33, HEALTH_METER_SOURCE);
                if (expedition) {
                    graphics.fill(slot.x + 1, slot.y + 1, slot.right() - 1, slot.bottom() - 1, 0x8A120F16);
                }
            }
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                if (locked) {
                    graphics.setTooltipForNextFrame(Component.literal("Unlock another crew-slot upgrade first."),
                            screenMouseX, screenMouseY);
                } else if (expedition) {
                    graphics.setTooltipForNextFrame(Component.literal(expeditionStatus(entry)),
                            screenMouseX, screenMouseY);
                } else if (populated) {
                    hoveredEntry = entry;
                    hoveredSlot = slot;
                } else {
                    graphics.setTooltipForNextFrame(Component.literal("Drop a depot dinosaur here"),
                            screenMouseX, screenMouseY);
                }
            }
        }
        if (hoveredEntry != null) drawLevelBadge(graphics, hoveredSlot, hoveredEntry.level());
    }

    private void drawAuthoredMeter(GuiGraphicsExtractor graphics, Layout layout, Rect localSlot,
                                   int value, int yOffset, Rect source) {
        int filled = Math.round(source.width * Mth.clamp(value, 0, 100) / 100.0F);
        if (filled <= 0) return;
        Rect target = global(layout, new Rect(localSlot.x + 1, localSlot.y + yOffset, filled, source.height));
        blitRegion(graphics, CONTROL_TABLE, target, source.x, source.y, filled, source.height,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void drawRosterPage(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect target = global(layout, ROSTER_PAGE);
        blitRegion(graphics, CONTROL_TABLE, target, ROSTER_PAGE.x, ROSTER_PAGE.y,
                ROSTER_PAGE.width, ROSTER_PAGE.height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        boolean unlocked = activePageCount() > 1;
        boolean hovered = target.contains(mouseX, mouseY);
        if (!unlocked) graphics.fill(target.x + 2, target.y + 2, target.right() - 2, target.bottom() - 2, 0x720F0A0D);
        thickButtonText(graphics, "PAGE " + (activePage + 1) + "/" + activePageCount(), inset(target, 2),
                hovered && unlocked ? GOLD : unlocked ? INK : MUTED_DARK);
        if (hovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            graphics.setTooltipForNextFrame(Component.literal(unlocked
                    ? "Switch between active crew pages."
                    : "Unlock Crew Perches to open the second crew page."), mouseX, mouseY);
        }
    }

    private void drawEnergyMeter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect gray = global(layout, ENERGY_GRAY);
        blitRegion(graphics, CONTROL_TABLE, gray, ENERGY_GRAY.x, ENERGY_GRAY.y,
                ENERGY_GRAY.width, ENERGY_GRAY.height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        float generation = energyGenerationPerSecond;
        float ratio = Mth.clamp(storedEnergy / Math.max(1.0F, energyCapacity), 0.0F, 1.0F);
        int fullHeight = Math.max(1, gray.height - Math.max(4, Math.round(4 * layout.scale)));
        int fillHeight = Math.round(fullHeight * ratio);
        if (fillHeight > 0) {
            Rect fill = new Rect(gray.x + Math.max(3, Math.round(3 * layout.scale))
                            - Math.round(1.2F * layout.scale),
                    gray.bottom() - Math.max(3, Math.round(3 * layout.scale)) - fillHeight
                            + Math.round(layout.scale),
                    Math.max(2, Math.round(ENERGY_FILL_SOURCE.width * layout.scale)), fillHeight);
            int sourceHeight = Math.max(1, Math.round(ENERGY_FILL_SOURCE.height * ratio));
            blitRegion(graphics, CONTROL_TABLE, fill,
                    ENERGY_FILL_SOURCE.x, ENERGY_FILL_SOURCE.bottom() - sourceHeight,
                    ENERGY_FILL_SOURCE.width, sourceHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        Rect label = global(layout, ENERGY_LABEL);
        blitRegion(graphics, CONTROL_TABLE, label, ENERGY_LABEL.x, ENERGY_LABEL.y,
                ENERGY_LABEL.width, ENERGY_LABEL.height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        thickButtonText(graphics, String.format(java.util.Locale.ROOT, "%.1f E/S", generation), inset(label, 2),
                generation > 0.0F ? GOLD : MUTED_DARK);
        if (gray.contains(mouseX, mouseY) || label.contains(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.literal(String.format(java.util.Locale.ROOT,
                    "%.0f / %.0f energy stored. Generating %.1f E/S.",
                    storedEnergy, energyCapacity, generation)), mouseX, mouseY);
        }
    }

    private void drawLevelBadge(GuiGraphicsExtractor graphics, Rect slot, int level) {
        int width = Math.max(28, Math.round(34 * layout().scale));
        int height = Math.max(8, Math.round(9 * layout().scale));
        Rect badge = new Rect(slot.centerX() - width / 2, slot.y - height - 2, width, height);
        blit(graphics, SPACE, badge);
        centeredBold(graphics, "LEVEL " + level, inset(badge, 2), GOLD, 0.66F);
    }

    private void drawDepot(GuiGraphicsExtractor graphics, Layout layout, UiMotion motion,
                           int screenMouseX, int screenMouseY, int mouseX, int mouseY, float time) {
        Rect panel = layout.depot;
        float pop = 0.82F + 0.18F * spring(Mth.clamp(depotProgress, 0.0F, 1.0F), 6.8F, 10.6F);
        withMotion(graphics, panel, 0.0F, 0.0F, pop, () -> {
            graphics.fill(panel.x + 3, panel.y + 5, panel.right() + 3, panel.bottom() + 5, 0x62000000);
            blitRegion(graphics, CONTROL_TABLE, panel, DEPOT_X, DEPOT_Y, DEPOT_WIDTH, DEPOT_HEIGHT,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);

            Rect sort = depotGlobal(layout, DEPOT_SORT);
            Rect page = depotGlobal(layout, DEPOT_PAGE);
            boolean sortHover = sort.contains(mouseX, mouseY);
            boolean pageHover = page.contains(mouseX, mouseY);
            if (sortHover) graphics.fill(sort.x + 2, sort.y + 2, sort.right() - 2, sort.bottom() - 2, 0x25FFF1C4);
            if (pageHover) graphics.fill(page.x + 2, page.y + 2, page.right() - 2, page.bottom() - 2, 0x25FFF1C4);
            thickButtonText(graphics, "SORT " + depotSort.label(), sort, sortHover ? GOLD : INK);
            thickButtonText(graphics, "PAGE " + (depotPage + 1) + "/" + depotPageCount(), page,
                    pageHover ? GOLD : INK);
            if (sortHover || pageHover) graphics.requestCursor(CursorTypes.POINTING_HAND);

            List<DinosaurRosterPayload.Entry> visible = visibleDepotEntries();
            DinosaurRosterPayload.Entry hoveredEntry = null;
            Rect hoveredSlot = null;
            for (int index = 0; index < 12; index++) {
                Rect slot = depotSlot(layout, index);
                DinosaurRosterPayload.Entry entry = index < visible.size() ? visible.get(index) : null;
                boolean hovered = slot.contains(mouseX, mouseY);
                if (hovered) drawSlotEdgeGlow(graphics, slot, time, entry == null ? 0.45F : 1.0F);
                if (entry == null) continue;
                if (draggedDinosaur != null && draggedDinosaur.id().equals(entry.id())) {
                    drawSlotEdgeGlow(graphics, inset(slot, 2), time, 0.70F);
                    continue;
                }
                FieldDodoEntity dinosaur = entityFor(entry);
                if (dinosaur == null) continue;
                float dwell = hoverAmount(dinosaurDwells, 1000 + depotPage * 16 + index, hovered);
                float wobble = Mth.sin(time * 11.0F + index * 0.71F) * 0.7F * dwell;
                FloatRect authoredPreview = depotPreviewSmooth(layout, index);
                FloatRect openedPreview = transformedRectSmooth(authoredPreview, panel, 0.0F, 0.0F, pop);
                FloatRect renderedPreview = inset(motion.transformSmooth(openedPreview, 0.0F, 0.0F), 0.35F);
                DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
                float previewScale = commandTablePreviewScale(renderedPreview, visual) * (1.0F + dwell * 0.018F);
                extractSmoothDinosaurPreview(graphics, renderedPreview, previewScale, dinosaur,
                        ROSTER_VIEW_YAW + Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F) * 5.5F + wobble,
                        ROSTER_VIEW_PITCH + Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F) * 2.5F,
                        Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F),
                        Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F));
                if (hovered) {
                    hoveredEntry = entry;
                    hoveredSlot = slot;
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                }
            }
            List<DinosaurRosterPayload.Entry> recovering = visibleRecoveryEntries();
            Rect recoveryLabel = depotGlobal(layout, new Rect(309, 181, 104, 14));
            thickButtonText(graphics, recovering.isEmpty() ? "RECOVERY EMPTY"
                            : "RECOVERY " + recovering.size() + "/4",
                    recoveryLabel, recovering.isEmpty() ? MUTED_DARK : RED);
            for (int index = 0; index < 4; index++) {
                Rect slot = depotRecoverySlot(layout, index);
                DinosaurRosterPayload.Entry entry = index < recovering.size() ? recovering.get(index) : null;
                boolean hovered = slot.contains(mouseX, mouseY);
                if (hovered) drawSlotEdgeGlow(graphics, slot, time, entry == null ? 0.45F : 1.0F);
                if (entry == null) continue;
                FieldDodoEntity dinosaur = entityFor(entry);
                if (dinosaur == null) continue;
                hoverAmount(dinosaurDwells, 4000 + depotPage * 4 + index, hovered);
                float breathe = 1.0F + Mth.sin(time * 3.2F + index) * 0.012F;
                FloatRect recoveryPreview = depotRecoveryPreviewSmooth(layout, index);
                FloatRect openedPreview = transformedRectSmooth(recoveryPreview, panel, 0.0F, 0.0F, pop);
                FloatRect renderedPreview = inset(motion.transformSmooth(openedPreview, 0.0F, 0.0F), 0.35F);
                DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
                float previewScale = commandTablePreviewScale(renderedPreview, visual) * breathe;
                extractSmoothDinosaurPreview(graphics, renderedPreview, previewScale, dinosaur,
                        ROSTER_VIEW_YAW + Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F) * 5.5F,
                        ROSTER_VIEW_PITCH + Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F) * 2.5F,
                        Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F),
                        Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F));
                if (hovered) {
                    hoveredEntry = entry;
                    hoveredSlot = slot;
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                }
            }
            if (hoveredEntry != null) drawDepotHoverCard(graphics, panel, hoveredSlot, hoveredEntry);
        });
    }

    private void drawDepotHoverCard(GuiGraphicsExtractor graphics, Rect depot, Rect slot,
                                    DinosaurRosterPayload.Entry entry) {
        int width = Math.round(112 * layout().scale);
        int height = Math.round(43 * layout().scale);
        int x = depot.right() + 4;
        if (x + width > this.width - 3) x = depot.x - width - 4;
        int y = Mth.clamp(slot.centerY() - height / 2, 3, this.height - height - 3);
        Rect card = new Rect(x, y, width, height);
        Rect top = new Rect(card.x, card.y, card.width, Math.max(11, card.height / 3));
        Rect body = new Rect(card.x, top.bottom() - 1, card.width, card.height - top.height + 1);
        blit(graphics, SPACE, top);
        blit(graphics, SPACE, body);
        thickButtonText(graphics, entry.name().toUpperCase(), inset(top, 3), GOLD);
        int health = Math.round(100.0F * entry.health() / Math.max(1.0F, entry.maxHealth()));
        Rect first = new Rect(body.x + 3, body.y + 2, body.width - 6, Math.max(8, body.height / 2 - 1));
        Rect second = new Rect(first.x, first.bottom(), first.width, Math.max(8, body.bottom() - first.bottom() - 2));
        if (entry.recoveryTicksRemaining() > 0L) {
            thickButtonText(graphics, "RECOVERING  " + recoveryTime(entry.recoveryTicksRemaining()), first, RED);
            thickButtonText(graphics, "RESTING IN WARD", second, MUTED_DARK);
            return;
        }
        thickButtonText(graphics, "LEVEL " + entry.level() + "  /  " + prettySpecies(entry.species()).toUpperCase(),
                first, MUTED_DARK);
        int gap = Math.max(1, Math.round(layout().scale));
        int statWidth = (second.width - gap * 2) / 3;
        Rect hunger = new Rect(second.x, second.y, statWidth, second.height);
        Rect mood = new Rect(hunger.right() + gap, second.y, statWidth, second.height);
        Rect vitality = new Rect(mood.right() + gap, second.y, second.right() - mood.right() - gap, second.height);
        thickButtonText(graphics, "H " + entry.hunger(), hunger, GOLD);
        thickButtonText(graphics, "M " + entry.mood(), mood, GREEN);
        thickButtonText(graphics, "HP " + health, vitality, RED);
    }

    private void drawDraggedDinosaur(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        if (draggedDinosaur == null) return;
        FieldDodoEntity dinosaur = entityFor(draggedDinosaur);
        if (dinosaur == null) return;
        float pickupProgress = Mth.clamp((renderNowNanos - dragStartedNanos) / 340_000_000.0F, 0.0F, 1.0F);
        float pickupScale = 0.64F + spring(pickupProgress, 6.6F, 11.0F) * 0.36F;
        int size = Math.round(62 * pickupScale);
        Rect carried = new Rect(Math.round(draggedX) - size / 2,
                Math.round(draggedY) - size / 2, size, size);
        drawSlotEdgeGlow(graphics, inset(carried, 4), time, 0.85F);
        DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
        FloatRect preview = inset(new FloatRect(carried.x, carried.y, carried.width, carried.height), 3.0F);
        float viewYaw = ROSTER_VIEW_YAW + draggedYaw * 12.0F;
        float viewPitch = ROSTER_VIEW_PITCH + draggedPitch * 14.0F;
        float scale = commandTablePreviewScale(preview, visual, viewYaw, viewPitch);
        extractSmoothDinosaurPreview(graphics, preview, scale, dinosaur, viewYaw, viewPitch, 0.0F, 0.0F);
        graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    private void updateDraggedDinosaur(int mouseX, int mouseY, float deltaSeconds) {
        float deltaX = mouseX - draggedX;
        float deltaY = mouseY - draggedY;
        float stiffness = 125.0F;
        draggedVelocityX += deltaX * stiffness * deltaSeconds;
        draggedVelocityY += deltaY * stiffness * deltaSeconds;
        float damping = (float)Math.exp(-16.0F * deltaSeconds);
        draggedVelocityX *= damping;
        draggedVelocityY *= damping;
        draggedX += draggedVelocityX * deltaSeconds;
        draggedY += draggedVelocityY * deltaSeconds;

        float targetYaw = Mth.clamp(draggedVelocityX / 210.0F, -1.35F, 1.35F);
        float targetPitch = Mth.clamp(-draggedVelocityY / 420.0F, -0.32F, 0.32F);
        draggedYaw = follow(draggedYaw, targetYaw, 8.5F, deltaSeconds);
        draggedPitch = follow(draggedPitch, targetPitch, 9.5F, deltaSeconds);
    }

    private void drawUpgradeRipple(GuiGraphicsExtractor graphics, Layout layout, UiMotion motion) {
        if (rippleUpgradeId < 0 || lastPurchaseNanos == 0L) return;
        float progress = (renderNowNanos - lastPurchaseNanos) / 680_000_000.0F;
        if (progress >= 1.0F) {
            rippleUpgradeId = -1;
            return;
        }
        BaseUpgrade upgrade = BaseUpgrade.byId(rippleUpgradeId).orElse(null);
        if (upgrade == null) return;
        NodePoint treeOrigin = upgradePoint(layout, upgrade, false);
        float originX = motion.transformX(treeOrigin.x + treeParallaxX);
        float originY = motion.transformY(treeOrigin.y + treeParallaxY);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        int alpha = Math.round((1.0F - progress) * 218.0F);
        for (int ring = 0; ring < 2; ring++) {
            float ringProgress = Mth.clamp(eased - ring * 0.10F, 0.0F, 1.0F);
            float ringRadius = 10.0F + ringProgress * (48.0F + ring * 9.0F);
            int color = (Math.max(0, alpha - ring * 48) << 24) | (ring == 0 ? 0xFFD986 : 0xE8B95D);
            drawEllipseRing(graphics, originX, originY, ringRadius, ringRadius * 0.62F,
                    ring == 0 ? 1.8F : 1.2F, color);
        }
    }

    private List<DinosaurRosterPayload.Entry> visibleDepotEntries() {
        List<DinosaurRosterPayload.Entry> sorted = new ArrayList<>(depotEntries);
        sorted.sort(depotSort.comparator());
        int start = Math.min(sorted.size(), depotPage * 12);
        int end = Math.min(sorted.size(), start + 12);
        return List.copyOf(sorted.subList(start, end));
    }

    private List<DinosaurRosterPayload.Entry> visibleRecoveryEntries() {
        List<DinosaurRosterPayload.Entry> sorted = new ArrayList<>(recoveryEntries);
        sorted.sort(depotSort.comparator());
        int start = Math.min(sorted.size(), depotPage * 4);
        int end = Math.min(sorted.size(), start + 4);
        return List.copyOf(sorted.subList(start, end));
    }

    private int depotPageCount() {
        int livingPages = (depotEntries.size() + 11) / 12;
        int recoveryPages = (recoveryEntries.size() + 3) / 4;
        return Math.max(1, Math.max(livingPages, recoveryPages));
    }

    private void clampDepotPage() {
        depotPage = Mth.clamp(depotPage, 0, depotPageCount() - 1);
    }

    private void drawActions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        for (int index = 0; index < ACTIONS.length; index++) {
            Rect button = global(layout, ACTIONS[index]);
            boolean hovered = button.contains(mouseX, mouseY);
            float dwell = hoverAmount(actionDwells, index, hovered);
            float wobbleX = Mth.sin(time * 13.0F + index * 1.3F) * 0.75F * dwell;
            float wobbleY = Mth.sin(time * 15.0F + index * 0.8F) * 0.35F * dwell;
            float scale = 1.0F + dwell * 0.018F;
            int color = hovered ? ACTION_COLORS[index] : MUTED_DARK;
            int capturedIndex = index;
            withMotion(graphics, button, wobbleX, wobbleY, scale, () -> {
                if (hovered) {
                    graphics.fill(button.x + 2, button.y + 2, button.right() - 2, button.bottom() - 2, 0x22FFF2CC);
                }
                Rect textBounds = capturedIndex == 0
                        ? new Rect(button.x + Math.max(1, Math.round(layout.scale)), button.y,
                        Math.max(1, button.width - Math.max(1, Math.round(layout.scale))), button.height)
                        : button;
                thickButtonText(graphics, ACTION_LABELS[capturedIndex], textBounds, color);
                int underlineY = button.bottom() - 5;
                int underlineWidth = Math.round((button.width - 10) * (hovered ? 0.82F : 0.36F));
                graphics.fill(button.centerX() - underlineWidth / 2, underlineY,
                        button.centerX() + (underlineWidth + 1) / 2, underlineY + 1, color);
                if (hovered) {
                    int sweep = Math.floorMod((int)(time * 42.0F + capturedIndex * 11), button.width + 18) - 9;
                    int glintX = button.x + sweep;
                    if (glintX > button.x + 2 && glintX < button.right() - 2) {
                        graphics.fill(glintX, button.y + 3, glintX + 1, button.bottom() - 3, 0x58FFF6DA);
                    }
                }
            });
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                graphics.setTooltipForNextFrame(Component.literal(ACTION_HELP[index]), mouseX, mouseY);
            }
        }
    }

    private void drawConnections(GuiGraphicsExtractor graphics, Layout layout) {
        for (BaseUpgrade upgrade : BaseUpgrade.values()) {
            if (!isDiscovered(upgrade) || upgrade.prerequisiteId() < 0) continue;
            BaseUpgrade parent = BaseUpgrade.byId(upgrade.prerequisiteId()).orElseThrow();
            if (!isDiscovered(parent)) continue;
            NodePoint from = upgradePoint(layout, parent, true);
            NodePoint to = upgradePoint(layout, upgrade, true);
            boolean lit = prerequisiteMet(upgrade);
            int edge = lit ? STAR_LINE : 0xFF292333;
            int center = lit ? STAR_LIGHT : 0xFF4A4057;
            float midY = (from.y + to.y) * 0.5F;
            smoothBranchLine(graphics, from.x, from.y, to.x, midY, to.y, edge, 3.0F * treeZoom);
            smoothBranchLine(graphics, from.x, from.y, to.x, midY, to.y, center, Math.max(0.55F, treeZoom));
        }
    }

    private void drawUpgradeNodes(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        for (BaseUpgrade upgrade : BaseUpgrade.values()) {
            if (!isDiscovered(upgrade)) continue;
            NodePoint point = upgradePoint(layout, upgrade, true);
            Rect node = point.bounds();
            boolean hovered = point.contains(mouseX, mouseY, treeZoom);
            boolean unlocked = prerequisiteMet(upgrade);
            boolean purchased = level(upgrade) > 0;
            boolean complete = level(upgrade) >= upgrade.maxLevel();
            float dwell = hoverAmount(nodeDwells, upgrade.id(), hovered);
            float purchasePulse = lastPurchaseNanos == 0L ? 0.0F
                    : Mth.clamp(1.0F - (renderNowNanos - lastPurchaseNanos) / 420_000_000.0F, 0.0F, 1.0F);
            float wobbleX = Mth.sin(time * 10.0F + upgrade.id()) * 0.65F * dwell;
            float wobbleY = Mth.sin(time * 12.5F + upgrade.id() * 0.5F) * 0.35F * dwell;
            float nodeScale = 1.0F;
            nodeScale *= 1.0F + Mth.sin(time * 8.0F + upgrade.id()) * 0.018F * dwell;
            if (hovered) nodeScale += Mth.sin(purchasePulse * Mth.PI) * 0.07F;
            nodeScale *= treeZoom;
            graphics.pose().pushMatrix();
            graphics.pose().translate(point.x - node.centerX(), point.y - node.centerY());
            withMotion(graphics, node, wobbleX, wobbleY, nodeScale, () -> {
                blit(graphics, HOTBAR, node);
                if (hovered || purchased) {
                    graphics.fill(node.x + 2, node.y + 2, node.right() - 2, node.bottom() - 2, 0x20FFF2B8);
                }
                int tint = complete ? 0x6186D66B : unlocked ? 0x32FFD46E : 0x860C0911;
                graphics.fill(node.x + 2, node.y + 2, node.right() - 2, node.bottom() - 2, tint);
                drawItemInRect(graphics, upgradeIcon(upgrade), inset(node, 4));
                if (!unlocked) graphics.fill(node.x + 2, node.y + 2, node.right() - 2, node.bottom() - 2, 0x92100C17);
                drawRanks(graphics, node, upgrade);
            });
            graphics.pose().popMatrix();
            if (hovered) graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void drawTreeOverlay(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float time) {
        Rect viewport = global(layout, TREE_VIEW);
        BaseUpgrade hovered = hoveredUpgrade(layout, mouseX, mouseY);
        if (hovered == null) {
            Rect crewTag = new Rect(viewport.right() - Math.round(57 * layout.scale), viewport.y + 3,
                    Math.round(53 * layout.scale), Math.round(11 * layout.scale));
            blit(graphics, SPACE, crewTag);
            centeredBold(graphics, "CREW SLOTS " + clientActiveCapacity(), crewTag, GOLD, 0.68F);
            Rect hint = new Rect(viewport.x + 4, viewport.bottom() - Math.round(11 * layout.scale),
                    Math.round(65 * layout.scale), Math.round(9 * layout.scale));
            fittedText(graphics, "DRAG  ·  SCROLL", hint, 0xA8A897A8, 0.62F, true);
            return;
        }

        Rect top = new Rect(viewport.x + 7, viewport.bottom() - Math.round(29 * layout.scale),
                viewport.width - 14, Math.round(13 * layout.scale));
        Rect bottom = new Rect(top.x, top.bottom() - 1, top.width, Math.round(15 * layout.scale));
        float dwell = hoverAmount(nodeDwells, hovered.id(), true);
        float pop = 0.96F + dwell * 0.04F;
        withMotion(graphics, new Rect(top.x, top.y, top.width, top.height + bottom.height), 0.0F,
                -dwell * 1.2F, pop, () -> {
                    blit(graphics, SPACE, top);
                    blit(graphics, SPACE, bottom);
                    int rank = level(hovered);
                    String right = rank >= hovered.maxLevel() ? "MASTERED"
                            : prerequisiteMet(hovered) ? "ITEM COST"
                            : "LOCKED";
                    Rect titleArea = new Rect(top.x + 5, top.y + 2, top.width - 10, top.height - 3);
                    fittedText(graphics, hovered.title().toUpperCase() + "   " + right, titleArea,
                            prerequisiteMet(hovered) ? GOLD : RED, 0.70F, true);
                    int costWidth = rank >= hovered.maxLevel() || !prerequisiteMet(hovered) ? 0 : 66;
                    Rect detailArea = new Rect(bottom.x + 5, bottom.y + 3,
                            bottom.width - 10 - costWidth, bottom.height - 5);
                    wrappedText(graphics, hovered.detail(), detailArea, MUTED, 0.59F, 2);
                    if (costWidth > 0) drawUpgradeCosts(graphics, hovered, rank,
                            new Rect(bottom.right() - costWidth - 3, bottom.y + 1, costWidth, bottom.height - 2));
                });
    }

    private void drawUpgradeCosts(GuiGraphicsExtractor graphics, BaseUpgrade upgrade, int rank, Rect area) {
        List<BaseUpgrade.UpgradeCost> costs = upgrade.itemCostsForLevel(rank);
        int slotWidth = Math.max(1, area.width / Math.max(1, costs.size()));
        for (int index = 0; index < costs.size(); index++) {
            BaseUpgrade.UpgradeCost cost = costs.get(index);
            int owned = minecraft.player == null ? 0 : minecraft.player.getInventory().countItem(cost.item());
            Rect slot = new Rect(area.x + index * slotWidth, area.y, slotWidth, area.height);
            Rect icon = new Rect(slot.x + 1, slot.y, Math.min(13, slot.height), Math.min(13, slot.height));
            drawItemInRect(graphics, cost.stack(), icon);
            Rect count = new Rect(icon.right() - 1, slot.y + 2, Math.max(8, slot.right() - icon.right()), slot.height - 3);
            fittedText(graphics, owned + "/" + cost.count(), count,
                    owned >= cost.count() ? GREEN : RED, 0.55F, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = layout();
        UiMotion motion = currentMotion(layout);
        double uiMouseX = motion.inverseX((float)event.x());
        double uiMouseY = motion.inverseY((float)event.y());
        Rect rosterPage = global(layout, ROSTER_PAGE);
        if (rosterPage.contains(uiMouseX, uiMouseY)) {
            if (activePageCount() > 1) activePage = (activePage + 1) % activePageCount();
            else notice = "Unlock Crew Perches to open a second active-crew page.";
            return true;
        }
        if (depotOpen && depotProgress > 0.82F) {
            if (depotGlobal(layout, DEPOT_SORT).contains(uiMouseX, uiMouseY)) {
                depotSort = depotSort.next();
                depotPage = 0;
                notice = "Depot sorted by " + depotSort.label().toLowerCase() + ".";
                return true;
            }
            if (depotGlobal(layout, DEPOT_PAGE).contains(uiMouseX, uiMouseY)) {
                int pages = depotPageCount();
                depotPage = pages <= 1 ? 0 : (depotPage + 1) % pages;
                return true;
            }
            List<DinosaurRosterPayload.Entry> visible = visibleDepotEntries();
            for (int index = 0; index < visible.size(); index++) {
                if (!depotSlot(layout, index).contains(uiMouseX, uiMouseY)) continue;
                if (visible.get(index).onExpedition()) {
                    notice = expeditionStatus(visible.get(index));
                    return true;
                }
                draggedDinosaur = visible.get(index);
                draggedFromActive = false;
                dragStartedNanos = Util.getNanos();
                draggedX = (float)event.x();
                draggedY = (float)event.y();
                draggedVelocityX = 0.0F;
                draggedVelocityY = 0.0F;
                draggedYaw = 0.0F;
                draggedPitch = 0.0F;
                notice = "Drag " + draggedDinosaur.name() + " onto one of the seven base slots.";
                return true;
            }
        }
        int pageStart = activePage * ACTIVE_PAGE_SIZE;
        for (int index = 0; index < ACTIVE_PAGE_SIZE; index++) {
            int rosterIndex = pageStart + index;
            if (rosterIndex >= activeEntries.size()
                    || !global(layout, dinosaurSlot(index)).contains(uiMouseX, uiMouseY)) continue;
            if (activeEntries.get(rosterIndex).onExpedition()) {
                notice = expeditionStatus(activeEntries.get(rosterIndex));
                return true;
            }
            if (depotOpen && depotProgress > 0.82F) {
                draggedDinosaur = activeEntries.get(rosterIndex);
                draggedFromActive = true;
                dragStartedNanos = Util.getNanos();
                draggedX = (float)event.x();
                draggedY = (float)event.y();
                draggedVelocityX = 0.0F;
                draggedVelocityY = 0.0F;
                draggedYaw = 0.0F;
                draggedPitch = 0.0F;
                notice = "Drag " + draggedDinosaur.name() + " into the depot or onto another crew slot.";
                return true;
            }
            openDinosaur(rosterIndex);
            return true;
        }
        for (int index = 0; index < ACTIONS.length; index++) {
            if (!global(layout, ACTIONS[index]).contains(uiMouseX, uiMouseY)) continue;
            runAction(index);
            return true;
        }
        Rect viewport = global(layout, TREE_VIEW);
        if (viewport.contains(uiMouseX, uiMouseY)) {
            BaseUpgrade upgrade = hoveredUpgrade(layout, uiMouseX, uiMouseY);
            if (upgrade != null) {
                purchase(upgrade);
                return true;
            }
            draggingTree = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void runAction(int action) {
        switch (action) {
            case 0 -> {
                ClientPacketDistributor.sendToServer(new CommandTableActionPayload(
                        tablePos,
                        CommandTableActionPayload.STORE_ALL
                ));
                notice = "Returning the active crew to the depot...";
            }
            case 1 -> {
                ClientPacketDistributor.sendToServer(new CommandTableActionPayload(tablePos, CommandTableActionPayload.RECALL_ALL));
                notice = "The table is calling every active companion home.";
            }
            case 2 -> {
                minecraft.setScreen(new EnergyNetworkScreen(tablePos, this));
            }
            case 3 -> {
                depotOpen = !depotOpen;
                depotTransitionNanos = Util.getNanos();
                notice = depotOpen ? "Drag a depot dinosaur onto a base slot to swap the crew."
                        : "Dinosaur depot tucked away.";
            }
            default -> {
            }
        }
    }

    private void openDinosaur(int index) {
        if (activeEntries.isEmpty() || minecraft.level == null) {
            notice = "There are no active companions to inspect.";
            return;
        }
        int safeIndex = Mth.clamp(index, 0, activeEntries.size() - 1);
        DinosaurRosterPayload.Entry entry = activeEntries.get(safeIndex);
        if (entry.onExpedition()) {
            notice = expeditionStatus(entry);
            return;
        }
        Entity loaded = entry.entityId() < 0 ? null : minecraft.level.getEntity(entry.entityId());
        if (loaded instanceof FieldDodoEntity dinosaur && dinosaur.isAlive()) {
            minecraft.setScreen(new CompanionTestScreen(dinosaur, tablePos));
        } else {
            notice = entry.name() + " is outside the loaded base area. Recall the crew first.";
        }
    }

    private void purchase(BaseUpgrade upgrade) {
        if (upgrade == BaseUpgrade.HEARTHSTONE || level(upgrade) >= upgrade.maxLevel()) {
            notice = upgrade == BaseUpgrade.HEARTHSTONE ? "The Hearthstone is already awake." : upgrade.title() + " is mastered.";
            return;
        }
        if (!prerequisiteMet(upgrade)) {
            notice = prerequisiteText(upgrade);
            return;
        }
        notice = "Carving " + upgrade.title() + " into the table...";
        ClientPacketDistributor.sendToServer(new PurchaseBaseUpgradePayload(tablePos, upgrade.id()));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && draggedDinosaur != null) {
            return true;
        }
        if (event.button() == 0 && draggingTree) {
            Layout layout = layout();
            float divisor = Math.max(0.01F, layout.scale * TREE_COORDINATE_SCALE * treeZoom
                    * currentMotion(layout).scale);
            targetTreePanX = Mth.clamp(targetTreePanX + (float)dragX / divisor, -500.0F, 500.0F);
            targetTreePanY = Mth.clamp(targetTreePanY + (float)dragY / divisor, -250.0F, 520.0F);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggedDinosaur != null) {
            Layout layout = layout();
            UiMotion motion = currentMotion(layout);
            double uiMouseX = motion.inverseX((float)event.x());
            double uiMouseY = motion.inverseY((float)event.y());
            for (int index = 0; index < ACTIVE_PAGE_SIZE; index++) {
                if (!global(layout, dinosaurSlot(index)).contains(uiMouseX, uiMouseY)) continue;
                int targetSlot = activePage * ACTIVE_PAGE_SIZE + index;
                if (targetSlot >= clientActiveCapacity()) {
                    notice = "Unlock another crew-slot upgrade first.";
                    draggedDinosaur = null;
                    draggedFromActive = false;
                    return true;
                }
                if (targetSlot < activeEntries.size()
                        && !activeEntries.get(targetSlot).id().equals(draggedDinosaur.id())
                        && activeEntries.get(targetSlot).onExpedition()) {
                    notice = activeEntries.get(targetSlot).name()
                            + " is away. That crew slot stays reserved until it returns.";
                    draggedDinosaur = null;
                    draggedFromActive = false;
                    return true;
                }
                ClientPacketDistributor.sendToServer(new SwapActiveDinosaurPayload(
                        tablePos, draggedDinosaur.id(), targetSlot
                ));
                notice = "Moving " + draggedDinosaur.name() + " into base slot " + (targetSlot + 1) + "...";
                draggedDinosaur = null;
                draggedFromActive = false;
                return true;
            }
            if (draggedFromActive && layout.depot.contains(uiMouseX, uiMouseY)) {
                ClientPacketDistributor.sendToServer(new SwapActiveDinosaurPayload(
                        tablePos, draggedDinosaur.id(), -1
                ));
                notice = "Returning " + draggedDinosaur.name() + " to the depot...";
            } else {
                notice = draggedFromActive
                        ? draggedDinosaur.name() + " stayed in the active crew."
                        : draggedDinosaur.name() + " returned to the depot.";
            }
            draggedDinosaur = null;
            draggedFromActive = false;
            return true;
        }
        if (event.button() == 0 && draggingTree) {
            draggingTree = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        UiMotion motion = currentMotion(layout);
        double uiMouseX = motion.inverseX((float)mouseX);
        double uiMouseY = motion.inverseY((float)mouseY);
        if (global(layout, TREE_VIEW).contains(uiMouseX, uiMouseY)) {
            targetTreeZoom = Mth.clamp(targetTreeZoom + (float)scrollY * 0.09F, 0.62F, 1.75F);
            notice = "Tree zoom " + Math.round(targetTreeZoom * 100.0F) + "%";
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && depotOpen) {
            depotOpen = false;
            depotTransitionNanos = Util.getNanos();
            draggedDinosaur = null;
            draggedFromActive = false;
            notice = "Dinosaur depot tucked away.";
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void rebuildActiveDinos() {
        activeDinos.clear();
        for (DinosaurRosterPayload.Entry entry : activeEntries) {
            FieldDodoEntity dinosaur = entityFor(entry);
            activeDinos.add(dinosaur);
        }
        focusedDinosaur = Mth.clamp(focusedDinosaur, 0, Math.max(0, activeDinos.size() - 1));
    }

    private FieldDodoEntity entityFor(DinosaurRosterPayload.Entry entry) {
        if (minecraft.level == null) return null;
        if (!entry.onExpedition() && entry.entityId() >= 0) {
            Entity loaded = minecraft.level.getEntity(entry.entityId());
            if (loaded instanceof FieldDodoEntity dinosaur && dinosaur.getUUID().equals(entry.id())) return dinosaur;
        }
        FieldDodoEntity preview = previewDinos.computeIfAbsent(entry.id(), ignored -> {
            DinosaurSpecies species = DinosaurSpecies.byRegistryName(entry.species());
            FieldDodoEntity created = ModEntities.typeFor(species).create(minecraft.level, EntitySpawnReason.LOAD);
            if (created == null) return null;
            return created;
        });
        if (preview != null) {
            // Roster entries refresh while this screen is open. Reapply every synced
            // field so cached depot models cannot retain an old mutation or pigment.
            preview.restoreOwnedPreviewState(entry.geneticQuality(), entry.mutationMask(), entry.hueVariant(),
                    entry.hunger(), entry.mood(), entry.health(), entry.level(), entry.originalPigmentRestored());
            preview.setCustomName(Component.literal(entry.name()));
            preview.tickCount = screenTicks;
        }
        return preview;
    }

    private static String expeditionStatus(DinosaurRosterPayload.Entry entry) {
        String[] names = {"SAFE FORAGE", "RIDGE TRAIL", "DEEP WILDS", "PREDATOR RUN", "PRIMORDIAL FRONTIER"};
        int tier = Mth.clamp(entry.expeditionTier(), 0, names.length - 1);
        return names[tier] + "  /  RETURNS IN " + recoveryTime(entry.expeditionTicksRemaining());
    }

    private void requestBaseState() {
        ClientPacketDistributor.sendToServer(new RequestBaseUpgradesPayload(tablePos));
        ClientPacketDistributor.sendToServer(new RequestBaseEnergyPayload(tablePos));
    }

    private BaseUpgrade hoveredUpgrade(Layout layout, double mouseX, double mouseY) {
        Rect viewport = global(layout, TREE_VIEW);
        if (!viewport.contains(mouseX, mouseY)) return null;
        double treeMouseX = mouseX - treeParallaxX;
        double treeMouseY = mouseY - treeParallaxY;
        for (BaseUpgrade upgrade : BaseUpgrade.values()) {
            if (isDiscovered(upgrade)
                    && upgradePoint(layout, upgrade, true).contains(treeMouseX, treeMouseY, treeZoom)) return upgrade;
        }
        return null;
    }

    private NodePoint upgradePoint(Layout layout, BaseUpgrade upgrade, boolean animateReveal) {
        Rect viewport = global(layout, TREE_VIEW);
        float targetX = viewport.centerX()
                + (upgrade.treeX() + treePanX) * TREE_COORDINATE_SCALE * treeZoom * layout.scale;
        float targetY = viewport.centerY()
                + (upgrade.treeY() + treePanY) * TREE_COORDINATE_SCALE * treeZoom * layout.scale;
        return new NodePoint(targetX, targetY);
    }

    private boolean isDiscovered(BaseUpgrade upgrade) {
        if (upgrade == BaseUpgrade.HEARTHSTONE || upgrade.prerequisiteId() < 0) return true;
        BaseUpgrade parent = BaseUpgrade.byId(upgrade.prerequisiteId()).orElseThrow();
        return parent == BaseUpgrade.HEARTHSTONE || level(parent) > 0;
    }

    private Rect dinosaurSlot(int index) {
        return new Rect(114 + index * 27, 50, 25, 35);
    }

    private int clientActiveCapacity() {
        return Mth.clamp(7
                + level(BaseUpgrade.CREW_PERCHES) * 2
                + level(BaseUpgrade.PACK_HIERARCHY) * 2
                + level(BaseUpgrade.ANCIENT_BONDS) * 3, 7, 14);
    }

    private int activePageCount() {
        return clientActiveCapacity() > ACTIVE_PAGE_SIZE ? 2 : 1;
    }

    private Layout layout() {
        int leftRailWidth = ART_X - ENERGY_LABEL.x;
        int closedWidth = leftRailWidth + ART_WIDTH;
        int fullWidth = closedWidth + 2 + DEPOT_WIDTH;
        float easedDepot = smoothStep(depotProgress);
        float reservedWidth = Mth.lerp(easedDepot, closedWidth, fullWidth);
        float horizontal = (width - 14.0F) / reservedWidth;
        float vertical = (height - 10.0F) / ART_HEIGHT;
        float fittedScale = Math.min(MAX_PANEL_SCALE, Math.min(horizontal, vertical)) * PANEL_SIZE_MULTIPLIER;
        float scale = Mth.clamp(fittedScale, 0.68F, MAX_PANEL_SCALE * PANEL_SIZE_MULTIPLIER);
        int leftRailPixels = Math.round(leftRailWidth * scale);
        int panelWidth = Math.round(ART_WIDTH * scale);
        int panelHeight = Math.round(ART_HEIGHT * scale);
        int depotWidth = Math.round(DEPOT_WIDTH * scale);
        int depotHeight = Math.round(DEPOT_HEIGHT * scale);
        int openWidth = panelWidth + Math.round(2 * scale) + depotWidth;
        int closedX = (width - leftRailPixels - panelWidth) / 2 + leftRailPixels;
        int openX = (width - leftRailPixels - openWidth) / 2 + leftRailPixels;
        int x = Math.round(Mth.lerp(easedDepot, closedX, openX));
        int y = (height - panelHeight) / 2;
        int openDepotX = x + panelWidth + Math.max(1, Math.round(2 * scale));
        int hiddenDepotX = width + Math.round(10 * scale);
        int depotX = Math.round(Mth.lerp(easedDepot, hiddenDepotX, openDepotX));
        int depotY = y;
        return new Layout(new Rect(x, y, panelWidth, panelHeight),
                new Rect(depotX, depotY, depotWidth, depotHeight), scale);
    }

    private UiMotion currentMotion(Layout layout) {
        float time = (Util.getNanos() - openedNanos) / 1_000_000_000.0F;
        return mainMotion(layout, time);
    }

    private UiMotion mainMotion(Layout layout, float time) {
        float progress = Mth.clamp(time / 1.20F, 0.0F, 1.0F);
        float settled = spring(progress, 6.2F, 11.4F);
        float parallaxFade = smoothStep(Mth.clamp(time / 1.20F, 0.0F, 1.0F));
        float pivotX = Mth.lerp(depotProgress, layout.panel.centerX(),
                (layout.panel.x + layout.depot.right()) * 0.5F);
        float offsetX = panelParallaxX * parallaxFade;
        float offsetY = 18.0F * (1.0F - settled) + panelParallaxY * parallaxFade;
        float scale = (0.74F + 0.26F * settled) * panelZoom;
        return new UiMotion(pivotX, layout.panel.centerY(), offsetX, offsetY, scale);
    }

    private void applyMotion(GuiGraphicsExtractor graphics, UiMotion motion) {
        graphics.pose().translate(motion.pivotX, motion.pivotY);
        graphics.pose().translate(motion.offsetX, motion.offsetY);
        graphics.pose().scale(motion.scale, motion.scale);
        graphics.pose().translate(-motion.pivotX, -motion.pivotY);
    }

    private Rect global(Layout layout, Rect local) {
        int left = layout.panel.x + Math.round((local.x - ART_X) * layout.scale);
        int top = layout.panel.y + Math.round((local.y - ART_Y) * layout.scale);
        int right = layout.panel.x + Math.round((local.right() - ART_X) * layout.scale);
        int bottom = layout.panel.y + Math.round((local.bottom() - ART_Y) * layout.scale);
        return new Rect(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private FloatRect globalSmooth(Layout layout, Rect local) {
        return new FloatRect(
                layout.panel.x + (local.x - ART_X) * layout.scale,
                layout.panel.y + (local.y - ART_Y) * layout.scale,
                local.width * layout.scale,
                local.height * layout.scale
        );
    }

    private Rect depotGlobal(Layout layout, Rect local) {
        int left = layout.depot.x + Math.round((local.x - DEPOT_X) * layout.scale);
        int top = layout.depot.y + Math.round((local.y - DEPOT_Y) * layout.scale);
        int right = layout.depot.x + Math.round((local.right() - DEPOT_X) * layout.scale);
        int bottom = layout.depot.y + Math.round((local.bottom() - DEPOT_Y) * layout.scale);
        return new Rect(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private Rect depotSlot(Layout layout, int index) {
        int column = Math.floorMod(index, 4);
        int row = Math.floorDiv(index, 4);
        return depotGlobal(layout, new Rect(309 + column * DEPOT_PREVIEW_X_STRIDE,
                64 + row * DEPOT_PREVIEW_Y_STRIDE, 25, 27));
    }

    private FloatRect depotPreviewSmooth(Layout layout, int index) {
        int column = Math.floorMod(index, 4);
        int row = Math.floorDiv(index, 4);
        Rect local = new Rect(311 + column * DEPOT_PREVIEW_X_STRIDE,
                66 + row * DEPOT_PREVIEW_Y_STRIDE, 23, 23);
        return new FloatRect(
                layout.depot.x + (local.x - DEPOT_X) * layout.scale,
                layout.depot.y + (local.y - DEPOT_Y) * layout.scale,
                local.width * layout.scale,
                local.height * layout.scale
        );
    }

    private Rect depotRecoverySlot(Layout layout, int index) {
        return depotGlobal(layout, new Rect(309 + Math.floorMod(index, 4) * DEPOT_PREVIEW_X_STRIDE,
                152, 26, 27));
    }

    private FloatRect depotRecoveryPreviewSmooth(Layout layout, int index) {
        Rect local = new Rect(311 + Math.floorMod(index, 4) * DEPOT_PREVIEW_X_STRIDE,
                155, 23, 23);
        return new FloatRect(
                layout.depot.x + (local.x - DEPOT_X) * layout.scale,
                layout.depot.y + (local.y - DEPOT_Y) * layout.scale,
                local.width * layout.scale,
                local.height * layout.scale
        );
    }

    private static String recoveryTime(long ticks) {
        long seconds = Math.max(0L, (ticks + 19L) / 20L);
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private int level(BaseUpgrade upgrade) {
        return upgrade.id() < upgradeLevels.length ? upgradeLevels[upgrade.id()] : 0;
    }

    private boolean prerequisiteMet(BaseUpgrade upgrade) {
        if (upgrade == BaseUpgrade.HEARTHSTONE || upgrade.prerequisiteId() < 0) return true;
        BaseUpgrade parent = BaseUpgrade.byId(upgrade.prerequisiteId()).orElseThrow();
        return level(parent) >= upgrade.prerequisiteLevel();
    }

    private String prerequisiteText(BaseUpgrade upgrade) {
        BaseUpgrade parent = BaseUpgrade.byId(upgrade.prerequisiteId()).orElseThrow();
        return "Raise " + parent.title() + " to rank " + upgrade.prerequisiteLevel() + " first.";
    }

    private int clientBaseRadius() {
        return 50
                + level(BaseUpgrade.SURVEY_STAKES) * 4
                + level(BaseUpgrade.WIDE_BOUNDARIES) * 8
                + level(BaseUpgrade.FAR_HORIZON) * 12
                + level(BaseUpgrade.FRONTIER_WARDS) * 4
                + level(BaseUpgrade.ANCIENT_NETWORK) * 10;
    }

    private net.minecraft.world.item.ItemStack upgradeIcon(BaseUpgrade upgrade) {
        return switch (upgrade) {
            case HEARTHSTONE -> net.minecraft.world.item.Items.CAMPFIRE.getDefaultInstance();
            case SURVEY_STAKES -> net.minecraft.world.item.Items.COMPASS.getDefaultInstance();
            case TRAIL_MARKERS -> net.minecraft.world.item.Items.LEAD.getDefaultInstance();
            case WIDE_BOUNDARIES, FAR_HORIZON -> net.minecraft.world.item.Items.SPYGLASS.getDefaultInstance();
            case FEEDING_BELLS -> net.minecraft.world.item.Items.BELL.getDefaultInstance();
            case WORKSHOP_RHYTHM, LIVING_WORKSHOP -> net.minecraft.world.item.Items.CRAFTING_TABLE.getDefaultInstance();
            case COPPER_BUSBARS -> net.minecraft.world.item.Items.COPPER_INGOT.getDefaultInstance();
            case EXPEDITION_CHARTS, ANCIENT_CARTOGRAPHY -> net.minecraft.world.item.Items.MAP.getDefaultInstance();
            case WATCH_POSTS, FRONTIER_WARDS -> net.minecraft.world.item.Items.TARGET.getDefaultInstance();
            case QUIET_ROOSTS, NIGHT_LANTERNS, CAMP_SANCTUARY, ANCIENT_SANCTUARY -> net.minecraft.world.item.Items.RED_BED.getDefaultInstance();
            case DEEP_PANTRY -> net.minecraft.world.item.Items.BREAD.getDefaultInstance();
            case PACK_FRAMES, QUICK_HANDOFFS -> net.minecraft.world.item.Items.CHEST.getDefaultInstance();
            case FURNACE_BELLOWS, HEAT_RESERVOIR -> net.minecraft.world.item.Items.BLAST_FURNACE.getDefaultInstance();
            case MASTER_TOOLS -> net.minecraft.world.item.Items.IRON_PICKAXE.getDefaultInstance();
            case GROUNDING_RODS -> net.minecraft.world.item.Items.LIGHTNING_ROD.getDefaultInstance();
            case TRAIL_WARDS -> net.minecraft.world.item.Items.SHIELD.getDefaultInstance();
            case PATTERN_MEMORY -> net.minecraft.world.item.Items.KNOWLEDGE_BOOK.getDefaultInstance();
            case ENERGY_RESERVOIR -> net.minecraft.world.item.Items.REDSTONE.getDefaultInstance();
            case CREW_PERCHES -> net.minecraft.world.item.Items.LEAD.getDefaultInstance();
            case PACK_HIERARCHY -> net.minecraft.world.item.Items.NAME_TAG.getDefaultInstance();
            case ANCIENT_BONDS -> net.minecraft.world.item.Items.TOTEM_OF_UNDYING.getDefaultInstance();
            case ANCIENT_NETWORK -> net.minecraft.world.item.Items.AMETHYST_SHARD.getDefaultInstance();
        };
    }

    private void drawRanks(GuiGraphicsExtractor graphics, Rect node, BaseUpgrade upgrade) {
        if (upgrade.maxLevel() <= 1) return;
        int dots = upgrade.maxLevel();
        int gap = 1;
        int dotWidth = Math.max(1, (node.width - 6 - (dots - 1) * gap) / dots);
        for (int index = 0; index < dots; index++) {
            int x = node.x + 3 + index * (dotWidth + gap);
            graphics.fill(x, node.bottom() - 4, x + dotWidth, node.bottom() - 2,
                    index < level(upgrade) ? GOLD : 0x88483A54);
        }
    }

    private String displayName(FieldDodoEntity dinosaur) {
        if (dinosaur == null) return "Empty";
        if (dinosaur.hasCustomName()) return dinosaur.getDisplayName().getString();
        String value = dinosaur.getSpecies().registryName().replace('_', ' ');
        StringBuilder result = new StringBuilder(value.length());
        boolean capitalize = true;
        for (char character : value.toCharArray()) {
            result.append(capitalize ? Character.toUpperCase(character) : character);
            capitalize = character == ' ';
        }
        return result.toString();
    }

    private String prettySpecies(String registryName) {
        String value = registryName.replace('_', ' ');
        StringBuilder result = new StringBuilder(value.length());
        boolean capitalize = true;
        for (char character : value.toCharArray()) {
            result.append(capitalize ? Character.toUpperCase(character) : character);
            capitalize = character == ' ';
        }
        return result.toString();
    }

    private float hoverAmount(Map<Integer, HoverDwell> states, int key, boolean hovered) {
        HoverDwell state = states.computeIfAbsent(key, ignored -> new HoverDwell());
        if (!hovered) {
            state.startedNanos = 0L;
            return 0.0F;
        }
        if (state.startedNanos == 0L) state.startedNanos = renderNowNanos;
        float seconds = (renderNowNanos - state.startedNanos) / 1_000_000_000.0F;
        if (seconds >= 1.35F) return 0.0F;
        return (1.0F - (float)Math.exp(-seconds * 18.0F)) * (float)Math.exp(-seconds * 2.8F);
    }

    private void centeredBold(GuiGraphicsExtractor graphics, String value, Rect rect, int color, float requestedScale) {
        Component text = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        float fitScale = Math.min(requestedScale, (rect.width - 6.0F) / Math.max(1.0F, font().width(text)));
        float scale = Math.max(0.48F, fitScale);
        float x = rect.centerX() - font().width(text) * scale * 0.5F;
        float y = rect.centerY() - font().lineHeight * scale * 0.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font(), text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void thickButtonText(GuiGraphicsExtractor graphics, String value, Rect rect, int color) {
        Component text = Component.literal(value).withStyle(Style.EMPTY.withBold(true));
        float textWidth = Math.max(1.0F, font().width(text));
        float xScale = Math.min(0.86F, (rect.width - 7.0F) / textWidth);
        xScale = Math.max(0.43F, xScale);
        float yScale = Math.min(1.06F, Math.max(0.70F, (rect.height - 6.0F) / font().lineHeight));
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.centerX(), rect.centerY());
        graphics.pose().scale(xScale, yScale);
        int x = -font().width(text) / 2;
        int y = -font().lineHeight / 2;
        graphics.text(font(), text, x + 1, y + 1, 0x9A4C302D, false);
        graphics.text(font(), text, x, y, color, false);
        graphics.pose().popMatrix();
    }

    private void fittedText(GuiGraphicsExtractor graphics, String value, Rect rect, int color, float requestedScale, boolean bold) {
        boolean renderBold = bold || requestedScale <= 0.68F;
        Component text = Component.literal(value).withStyle(Style.EMPTY.withBold(renderBold));
        float scale = Math.min(requestedScale, rect.width / Math.max(1.0F, font().width(text)));
        scale = Math.max(0.42F, scale);
        String fitted = value;
        int maximumWidth = Math.max(1, Math.round(rect.width / scale));
        if (font().width(text) > maximumWidth) {
            fitted = font().plainSubstrByWidth(value, Math.max(0, maximumWidth - font().width("..."))) + "...";
            text = Component.literal(fitted).withStyle(Style.EMPTY.withBold(renderBold));
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.x, rect.centerY() - font().lineHeight * scale * 0.5F);
        graphics.pose().scale(scale, scale);
        graphics.text(font(), text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void wrappedText(GuiGraphicsExtractor graphics, String value, Rect rect, int color,
                             float preferredScale, int maximumLines) {
        float scale = preferredScale;
        List<String> lines = wrapWords(value, Math.max(1, Math.round(rect.width / scale)));
        while ((lines.size() > maximumLines || lines.size() * font().lineHeight * scale > rect.height)
                && scale > 0.43F) {
            scale = Math.max(0.43F, scale - 0.03F);
            lines = wrapWords(value, Math.max(1, Math.round(rect.width / scale)));
        }
        if (lines.size() > maximumLines) lines = new ArrayList<>(lines.subList(0, maximumLines));
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.x, rect.centerY() - lines.size() * font().lineHeight * scale * 0.5F);
        graphics.pose().scale(scale, scale);
        for (int index = 0; index < lines.size(); index++) {
            Component line = Component.literal(lines.get(index)).withStyle(Style.EMPTY.withBold(true));
            graphics.text(font(), line, 0, index * font().lineHeight, color, true);
        }
        graphics.pose().popMatrix();
    }

    private List<String> wrapWords(String value, int maximumWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font().width(candidate) > maximumWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(word);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private void drawEllipseRing(GuiGraphicsExtractor graphics, float centerX, float centerY,
                                 float radiusX, float radiusY, float thickness, int color) {
        int top = Math.round(centerY - radiusY);
        int bottom = Math.round(centerY + radiusY);
        float innerX = Math.max(0.0F, radiusX - thickness);
        float innerY = Math.max(0.0F, radiusY - thickness);
        for (int y = top; y <= bottom; y++) {
            float dy = y + 0.5F - centerY;
            float outerTerm = 1.0F - dy * dy / Math.max(1.0F, radiusY * radiusY);
            if (outerTerm < 0.0F) continue;
            float outer = radiusX * Mth.sqrt(outerTerm);
            float inner = 0.0F;
            if (innerX > 0.0F && innerY > 0.0F && Math.abs(dy) < innerY) {
                inner = innerX * Mth.sqrt(Math.max(0.0F, 1.0F - dy * dy / (innerY * innerY)));
            }
            int leftOuter = Mth.floor(centerX - outer);
            int rightOuter = Mth.ceil(centerX + outer);
            int leftInner = Mth.floor(centerX - inner);
            int rightInner = Mth.ceil(centerX + inner);
            graphics.fill(leftOuter, y, Math.max(leftOuter + 1, leftInner), y + 1, color);
            graphics.fill(Math.min(rightOuter - 1, rightInner), y, rightOuter, y + 1, color);
        }
    }

    private void drawFilledEllipse(GuiGraphicsExtractor graphics, float centerX, float centerY,
                                   float radiusX, float radiusY, int color) {
        int top = Math.round(centerY - radiusY);
        int bottom = Math.round(centerY + radiusY);
        for (int y = top; y <= bottom; y++) {
            float dy = y + 0.5F - centerY;
            float term = 1.0F - dy * dy / Math.max(1.0F, radiusY * radiusY);
            if (term < 0.0F) continue;
            float extent = radiusX * Mth.sqrt(term);
            graphics.fill(Mth.floor(centerX - extent), y, Mth.ceil(centerX + extent), y + 1, color);
        }
    }

    private void drawItemInRect(GuiGraphicsExtractor graphics, net.minecraft.world.item.ItemStack stack, Rect rect) {
        float scale = Math.max(0.25F, Math.min(rect.width, rect.height) / 16.0F);
        float x = rect.centerX() - 8.0F * scale;
        float y = rect.centerY() - 8.0F * scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void drawSlotEdgeGlow(GuiGraphicsExtractor graphics, Rect slot, float time, float strength) {
        int alpha = Math.round((38.0F + (Mth.sin(time * 4.6F) + 1.0F) * 13.0F) * Mth.clamp(strength, 0.0F, 1.0F));
        int bright = (alpha << 24) | 0x00FFF0D2;
        int soft = (Math.max(1, alpha / 3) << 24) | 0x00FFF0D2;
        graphics.fill(slot.x, slot.y, slot.right(), slot.y + 1, bright);
        graphics.fill(slot.x, slot.bottom() - 1, slot.right(), slot.bottom(), bright);
        graphics.fill(slot.x, slot.y + 1, slot.x + 1, slot.bottom() - 1, bright);
        graphics.fill(slot.right() - 1, slot.y + 1, slot.right(), slot.bottom() - 1, bright);
        if (slot.width > 4 && slot.height > 4) {
            graphics.fill(slot.x + 1, slot.y + 1, slot.right() - 1, slot.y + 2, soft);
            graphics.fill(slot.x + 1, slot.bottom() - 2, slot.right() - 1, slot.bottom() - 1, soft);
        }
    }

    private void smoothBranchLine(GuiGraphicsExtractor graphics, float x1, float y1, float x2,
                                  float middleY, float y2, int color, float thickness) {
        float half = thickness * 0.5F;
        fillFloat(graphics, x1 - half, Math.min(y1, middleY) - half,
                x1 + half, Math.max(y1, middleY) + half, color);
        fillFloat(graphics, Math.min(x1, x2) - half, middleY - half,
                Math.max(x1, x2) + half, middleY + half, color);
        fillFloat(graphics, x2 - half, Math.min(middleY, y2) - half,
                x2 + half, Math.max(middleY, y2) + half, color);
    }

    private void fillFloat(GuiGraphicsExtractor graphics, float left, float top, float right, float bottom, int color) {
        float width = Math.max(0.25F, right - left);
        float height = Math.max(0.25F, bottom - top);
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, top);
        graphics.pose().scale(width, height);
        graphics.fill(0, 0, 1, 1, color);
        graphics.pose().popMatrix();
    }

    private void withMotion(GuiGraphicsExtractor graphics, Rect rect, float offsetX, float offsetY, float scale, Runnable draw) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.centerX() + offsetX, rect.centerY() + offsetY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-rect.centerX(), -rect.centerY());
        draw.run();
        graphics.pose().popMatrix();
    }

    private FloatRect transformedRectSmooth(Rect rect, Rect pivot, float offsetX, float offsetY, float scale) {
        float left = pivot.centerX() + (rect.x - pivot.centerX()) * scale + offsetX;
        float top = pivot.centerY() + (rect.y - pivot.centerY()) * scale + offsetY;
        return new FloatRect(left, top, Math.max(1.0F, rect.width * scale),
                Math.max(1.0F, rect.height * scale));
    }

    private FloatRect transformedRectSmooth(FloatRect rect, Rect pivot, float offsetX, float offsetY, float scale) {
        float left = pivot.centerX() + (rect.x - pivot.centerX()) * scale + offsetX;
        float top = pivot.centerY() + (rect.y - pivot.centerY()) * scale + offsetY;
        return new FloatRect(left, top, Math.max(1.0F, rect.width * scale),
                Math.max(1.0F, rect.height * scale));
    }

    private void extractSmoothDinosaurPreview(GuiGraphicsExtractor graphics, FloatRect target, float scale,
                                              FieldDodoEntity dinosaur) {
        float smoothCursorX = Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F);
        float smoothCursorY = Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F);
        float viewYaw = ROSTER_VIEW_YAW + smoothCursorX * 5.5F;
        float viewPitch = ROSTER_VIEW_PITCH + smoothCursorY * 2.5F;
        extractSmoothDinosaurPreview(graphics, target, scale, dinosaur, viewYaw, viewPitch,
                smoothCursorX, smoothCursorY);
    }

    private void extractSmoothDinosaurPreview(GuiGraphicsExtractor graphics, FloatRect target, float scale,
                                              FieldDodoEntity dinosaur, float viewYaw, float viewPitch,
                                              float smoothCursorX, float smoothCursorY) {
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
        float parallaxX = -smoothCursorX * target.width * 0.035F;
        float parallaxY = -smoothCursorY * target.height * 0.025F;
        Vector3f translation = new Vector3f(
                (target.centerX() - renderCenterX + parallaxX) * inverseScale,
                renderState.boundingBoxHeight * 0.5F
                        + DinosaurVisualProfile.forType(dinosaur.getType()).modelGroundOffset()
                        + (target.centerY() - renderCenterY + parallaxY) * inverseScale,
                0.0F
        );
        graphics.entity(renderState, scale, translation, rotation, topDownRotation, x0, y0, x1, y1);
    }

    private float commandTablePreviewScale(FloatRect target, DinosaurVisualProfile visual) {
        float smoothCursorX = Mth.clamp(panelParallaxX / 2.4F, -1.0F, 1.0F);
        float smoothCursorY = Mth.clamp(panelParallaxY / 1.7F, -1.0F, 1.0F);
        return commandTablePreviewScale(target, visual,
                ROSTER_VIEW_YAW + smoothCursorX * 5.5F,
                ROSTER_VIEW_PITCH + smoothCursorY * 2.5F);
    }

    private float commandTablePreviewScale(FloatRect target, DinosaurVisualProfile visual,
                                           float viewYaw, float viewPitch) {
        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forVisual(visual);
        float yaw = viewYaw * Mth.DEG_TO_RAD;
        float pitch = viewPitch * Mth.DEG_TO_RAD;
        float footprint = Math.abs(bounds.width() * Mth.cos(yaw))
                + Math.abs(bounds.depth() * Mth.sin(yaw));
        float cameraDepth = Math.abs(bounds.width() * Mth.sin(yaw))
                + Math.abs(bounds.depth() * Mth.cos(yaw));
        float projectedHeight = bounds.height() * Math.abs(Mth.cos(pitch))
                + cameraDepth * Math.abs(Mth.sin(pitch));
        float fitted = Math.min(
                target.width / Math.max(0.35F, footprint),
                target.height / Math.max(0.35F, projectedHeight)
        ) * 0.97F;
        return Mth.clamp(fitted, 1.5F, 44.0F);
    }

    private static float follow(float current, float target, float speed, float deltaSeconds) {
        return Mth.lerp(1.0F - (float)Math.exp(-speed * deltaSeconds), current, target);
    }

    private static float spring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) return 1.0F;
        double wave = Math.cos(frequency * progress) + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float)(Math.exp(-damping * progress) * wave);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void blit(GuiGraphicsExtractor graphics, Identifier texture, Rect rect) {
        graphics.blit(texture, rect.x, rect.y, rect.right(), rect.bottom(), 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private void blitRegion(GuiGraphicsExtractor graphics, Identifier texture, Rect target,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                            int textureWidth, int textureHeight) {
        graphics.blit(texture, target.x, target.y, target.right(), target.bottom(),
                sourceX / (float)textureWidth, (sourceX + sourceWidth) / (float)textureWidth,
                sourceY / (float)textureHeight, (sourceY + sourceHeight) / (float)textureHeight);
    }

    private Font font() {
        return minecraft.font;
    }

    private static Rect inset(Rect rect, int amount) {
        return new Rect(rect.x + amount, rect.y + amount,
                Math.max(1, rect.width - amount * 2), Math.max(1, rect.height - amount * 2));
    }

    private static FloatRect inset(FloatRect rect, float amount) {
        return new FloatRect(rect.x + amount, rect.y + amount,
                Math.max(1.0F, rect.width - amount * 2.0F), Math.max(1.0F, rect.height - amount * 2.0F));
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/gui/" + name);
    }

    private static final class HoverDwell {
        private long startedNanos;
    }

    private enum SortMode {
        NAME("NAME", Comparator.comparing(entry -> entry.name().toLowerCase())),
        SPECIES("SPECIES", Comparator.comparing(entry -> entry.species().toLowerCase())),
        LEVEL("LEVEL", Comparator.comparingInt(DinosaurRosterPayload.Entry::level).reversed()),
        MOOD("MOOD", Comparator.comparingInt(DinosaurRosterPayload.Entry::mood).reversed());

        private final String label;
        private final Comparator<DinosaurRosterPayload.Entry> comparator;

        SortMode(String label, Comparator<DinosaurRosterPayload.Entry> comparator) {
            this.label = label;
            this.comparator = comparator;
        }

        private String label() {
            return label;
        }

        private Comparator<DinosaurRosterPayload.Entry> comparator() {
            return comparator;
        }

        private SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private record Layout(Rect panel, Rect depot, float scale) {
    }

    private record UiMotion(float pivotX, float pivotY, float offsetX, float offsetY, float scale) {
        private float inverseX(float screenX) {
            return pivotX + (screenX - pivotX - offsetX) / Math.max(0.01F, scale);
        }

        private float inverseY(float screenY) {
            return pivotY + (screenY - pivotY - offsetY) / Math.max(0.01F, scale);
        }

        private float transformX(float uiX) {
            return pivotX + (uiX - pivotX) * scale + offsetX;
        }

        private float transformY(float uiY) {
            return pivotY + (uiY - pivotY) * scale + offsetY;
        }

        private FloatRect transformSmooth(FloatRect rect, float extraX, float extraY) {
            return new FloatRect(
                    transformX(rect.x) + extraX,
                    transformY(rect.y) + extraY,
                    Math.max(1.0F, rect.width * scale),
                    Math.max(1.0F, rect.height * scale)
            );
        }

    }

    private record FloatRect(float x, float y, float width, float height) {
        private float right() { return x + width; }
        private float bottom() { return y + height; }
        private float centerX() { return x + width * 0.5F; }
        private float centerY() { return y + height * 0.5F; }
    }

    private record NodePoint(float x, float y) {
        private Rect bounds() {
            return new Rect(Mth.floor(x - TREE_NODE_SIZE * 0.5F), Mth.floor(y - TREE_NODE_SIZE * 0.5F),
                    TREE_NODE_SIZE, TREE_NODE_SIZE);
        }

        private boolean contains(double mouseX, double mouseY, float zoom) {
            float half = TREE_NODE_SIZE * 0.5F * zoom;
            return mouseX >= x - half && mouseX < x + half && mouseY >= y - half && mouseY < y + half;
        }
    }

    private record Rect(int x, int y, int width, int height) {
        private int right() { return x + width; }
        private int bottom() { return y + height; }
        private int centerX() { return x + width / 2; }
        private int centerY() { return y + height / 2; }
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
