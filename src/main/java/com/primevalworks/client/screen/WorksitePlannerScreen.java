package com.primevalworks.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.InputConstants;
import com.primevalworks.network.payload.AssignDodoWorkPayload;
import com.primevalworks.network.payload.DinosaurWorkStatePayload;
import com.primevalworks.network.payload.RequestDinosaurWorkStatePayload;
import com.primevalworks.network.payload.OpenBaseMachineMenuPayload;
import com.primevalworks.network.payload.BaseMachineRoutingPayload;
import com.primevalworks.network.payload.ConfigureBaseMachineSlotPayload;
import com.primevalworks.network.payload.BaseInventoryPayload;
import com.primevalworks.network.payload.RequestBaseInventoryPayload;
import com.primevalworks.network.payload.RequestCraftingCataloguePayload;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.block.entity.FoodBoxBlockEntity;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.work.BaseInventoryIndex;
import com.primevalworks.world.work.ExpeditionRewards;
import com.primevalworks.world.work.WorkSpecialtyRules;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorksitePlannerScreen extends Screen {
    private static final Identifier SPACE_TEXTURE = Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/space.png");
    private static final Identifier PLANNER_TEXTURE = Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/worksite_planner.png");
    private static final int PLANNER_TEXTURE_WIDTH = 427;
    private static final int PLANNER_TEXTURE_HEIGHT = 240;
    private static final int PLANNER_VISIBLE_HEIGHT = 25;
    private static final int PLANNER_LEFT_WIDTH = 26;
    private static final int PLANNER_LEFT_TOP = 32;
    private static final int PLANNER_LEFT_BOTTOM = 208;
    private static final int HELP_X = 150;
    private static final int HELP_Y = 92;
    private static final int HELP_WIDTH = 144;
    private static final int HELP_HEIGHT = 99;
    private static final int DOCK_RIGHT_X = 406;
    private static final int DOCK_RIGHT_Y = 80;
    private static final int DOCK_RIGHT_WIDTH = 21;
    private static final int DOCK_RIGHT_HEIGHT = 160;
    private static final int DOCK_BOTTOM_X = 268;
    private static final int DOCK_BOTTOM_Y = 219;
    private static final int DOCK_BOTTOM_WIDTH = 159;
    private static final int DOCK_BOTTOM_HEIGHT = 21;
    private static final int SEARCH_SLOT_X = 0;
    private static final int SEARCH_SLOT_Y = 214;
    private static final int SEARCH_SLOT_WIDTH = 26;
    private static final int SEARCH_SLOT_HEIGHT = 26;
    private static final int SEARCH_BAR_X = 26;
    private static final int SEARCH_BAR_Y = 226;
    private static final int SEARCH_BAR_WIDTH = 86;
    private static final int SEARCH_BAR_HEIGHT = 14;
    private static final int DEFAULT_BASE_RADIUS = 50;
    public static final RenderPipeline XRAY_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("primevalworks", "pipeline/xray_highlights"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    public static final RenderType XRAY_HIGHLIGHT_TYPE = RenderType.create(
            "primevalworks_xray_highlights",
            RenderSetup.builder(XRAY_HIGHLIGHT_PIPELINE)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );
    private static final int PANEL = 0xC8D0AD89;
    private static final int PANEL_DARK = 0xDDA67B5B;
    private static final int EDGE = 0xFF654638;
    private static final int INK = 0xFF403936;
    private static final int MUTED = 0xFF776D68;
    private static final int CREAM = 0xFFFFE9CB;
    private static final int AMBER = 0xFFF09A3D;
    private static final int CYAN = 0xFF63C2D1;
    private static final int GREEN = 0xFF77B85A;
    private static final int VIOLET = 0xFFB487D6;
    private static final int CORAL = 0xFFE26048;
    private static final String[] JOBS = {"TRANSPORT", "FIRE", "ENERGY", "CRAFTING", "EXPEDITION"};
    private static final Identifier[] JOB_ICONS = {
            Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/transport.png"),
            Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/fire.png"),
            Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/energy.png"),
            Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/crafting.png"),
            Identifier.fromNamespaceAndPath("primevalworks", "textures/gui/gathering.png")
    };
    private static final String[] PRIORITIES = {"LOW", "NORMAL", "HIGH", "URGENT"};
    private static final int[] BATCHES = {1, 8, 16, 32, 64};
    private static final String[] SCHEDULES = {"ALWAYS", "DAYLIGHT", "NIGHT SHIFT"};
    private static final String[] REPEATS = {"CONTINUOUS", "UNTIL TARGET", "ONE RUN"};
    private static final String[] ROUTES = {"STRICT", "PREFERRED", "NEAREST VALID"};
    private static final int[] STOCK_LEVELS = {0, 8, 16, 32, 64, 128, 256, 512};
    private static WorksitePlannerScreen active;
    private static WorksitePlannerScreen pendingMachineMenuReturn;
    private static long pendingMachineMenuDeadline;

    private final FieldDodoEntity dodo;
    private final BlockPos tablePos;
    private int jobIndex;
    private final Screen parent;
    private Entity previousCamera;
    private CameraType previousCameraType;
    private ArmorStand cameraAnchor;
    private Vec3 cameraPosition;
    private Vec3 cameraVelocity = Vec3.ZERO;
    private float cameraYaw;
    private float cameraPitch;
    private float cameraYawVelocity;
    private float cameraPitchVelocity;
    private long lastCameraFrame;
    private float orbitYaw = 42.0F;
    private float orbitPitch = 34.0F;
    private float orbitDistance = 18.0F;
    private int baseRadius = DEFAULT_BASE_RADIUS;
    private BlockPos cameraFocusPos;
    private float cameraFocusBlend;
    private BlockPos hoveredPos;
    private BlockPos sourcePos;
    private BlockPos workstationPos;
    private BlockPos destinationPos;
    private BlockPos areaEndPos;
    private BlockPos fallbackPos;
    private final List<BlockPos> sourcePositions = new ArrayList<>();
    private final List<BlockPos> workstationPositions = new ArrayList<>();
    private final List<BlockPos> destinationPositions = new ArrayList<>();
    private final List<BlockPos> fallbackPositions = new ArrayList<>();
    private final Map<BlockPos, Integer> blockPriorities = new LinkedHashMap<>();
    private BlockPos priorityTargetPos;
    private Selection selection;
    private String itemFilter = "";
    private final List<String> itemFilters = new ArrayList<>();
    private final List<String> fuelFilters = new ArrayList<>();
    private int expeditionTier;
    private List<ItemStack> filterItems = List.of();
    private int priority = 1;
    private int batchSize = 16;
    private int schedule;
    private int sourceReserve;
    private int destinationTarget;
    private int repeatMode;
    private int routePolicy = 1;
    private boolean exactItemMatch = true;
    private boolean avoidDanger = true;
    private int rulePage;
    private boolean draggingCamera;
    private long plannerStartedNanos = Util.getNanos();
    private long lastInterfaceFrameNanos = Util.getNanos();
    private float topDrawerReveal = 1.0F;
    private float topDrawerVelocity;
    private float topDrawerHover = 1.0F;
    private float leftDrawerReveal = 1.0F;
    private float leftDrawerVelocity;
    private float leftDrawerHover = 1.0F;
    private int hoveredWorkflowSlot = -1;
    private PlannerTool hoveredTool;
    private PlannerTool pinnedTool;
    private PlannerTool pressedTool;
    private final Map<PlannerTool, Long> toolHoverStartedNanos = new EnumMap<>(PlannerTool.class);
    private long pressedToolNanos;
    private EditBox searchBox;
    private boolean searchOpen;
    private float searchReveal;
    private float searchVelocity;
    private int searchScrollRow;
    private int searchIndexAge;
    private List<BaseItemEntry> baseItems = List.of();
    private List<BaseItemEntry> pickerItems = List.of();
    private List<String> craftingCatalogue = List.of();
    private List<BaseInventoryPayload.ContainerEntry> baseContainers = List.of();
    private final List<BlockPos> baseContainerPositions = new ArrayList<>();
    private final List<BlockPos> suitableBlockPositions = new ArrayList<>();
    private final List<BlockPos> matchingBlockPositions = new ArrayList<>();
    private final List<BlockPos> indexedFireWorkstations = new ArrayList<>();
    private final List<BlockPos> indexedEnergyWorkstations = new ArrayList<>();
    private final List<BlockPos> indexedCraftingTables = new ArrayList<>();
    private int workstationScanX;
    private int workstationScanY;
    private int workstationScanZ;
    private int workstationScanMinimumY;
    private int workstationScanMaximumY;
    private boolean workstationScanStarted;
    private boolean workstationScanComplete;
    private BaseItemEntry draggedBaseItem;
    private int pointerX;
    private int pointerY;
    private float helpReveal;
    private float helpVelocity;
    private long helpOpenedNanos;
    private long feedbackUntilNanos;
    private float renderTimeTicks;
    private boolean workStateReceived;
    private boolean resumingFromMachineMenu;
    private BaseMachineRoutingPayload machineRouting;
    private boolean machineRoutingOpen;
    private float machineRoutingReveal;
    private float machineRoutingVelocity;
    private String feedback = "Choose a highlighted step, then click a block in the base.";

    public WorksitePlannerScreen(FieldDodoEntity dodo, BlockPos tablePos, int jobIndex, Screen parent) {
        super(Component.literal("Base Work Planner"));
        this.dodo = dodo;
        this.tablePos = tablePos.immutable();
        this.jobIndex = Mth.clamp(jobIndex, 0, 4);
        this.parent = parent;
        this.sourcePositions.addAll(dodo.getWorkSourcePositions());
        this.workstationPositions.addAll(dodo.getWorkWorkstationPositions());
        this.destinationPositions.addAll(dodo.getWorkDestinationPositions());
        this.fallbackPositions.addAll(dodo.getWorkFallbackPositions());
        this.blockPriorities.putAll(dodo.getWorkBlockPriorities());
        this.sourcePos = firstPosition(sourcePositions);
        this.workstationPos = firstPosition(workstationPositions);
        this.destinationPos = firstPosition(destinationPositions);
        this.areaEndPos = dodo.getWorkAreaEndPos().orElse(null);
        this.fallbackPos = firstPosition(fallbackPositions);
        this.itemFilters.addAll(dodo.getWorkItemFilters());
        this.itemFilter = itemFilters.isEmpty() ? "" : itemFilters.getFirst();
        this.fuelFilters.addAll(dodo.getWorkFuelFilters());
        this.expeditionTier = dodo.getExpeditionTier();
        this.priority = dodo.getWorkPriority();
        this.batchSize = dodo.getWorkBatchSize();
        this.schedule = dodo.getWorkSchedule();
        this.sourceReserve = dodo.getWorkSourceReserve();
        this.destinationTarget = dodo.getWorkDestinationTarget();
        this.repeatMode = dodo.getWorkRepeatMode();
        this.routePolicy = dodo.getWorkRoutePolicy();
        this.exactItemMatch = dodo.isExactItemMatch();
        this.avoidDanger = dodo.shouldAvoidDanger();
        normalizeDraftForSpecialty();
        Selection[] initialSelections = selections();
        this.selection = jobIndex == 4 ? initialSelections[expeditionTier] : initialSelections[0];
    }

    @Override
    protected void init() {
        boolean resumeDraft = resumingFromMachineMenu;
        resumingFromMachineMenu = false;
        active = this;
        plannerStartedNanos = Util.getNanos();
        lastInterfaceFrameNanos = plannerStartedNanos;
        topDrawerReveal = 1.0F;
        topDrawerVelocity = 0.0F;
        topDrawerHover = 1.0F;
        leftDrawerReveal = 1.0F;
        leftDrawerVelocity = 0.0F;
        leftDrawerHover = 1.0F;
        hoveredWorkflowSlot = -1;
        hoveredTool = null;
        pinnedTool = null;
        pressedTool = null;
        pressedToolNanos = 0L;
        toolHoverStartedNanos.clear();
        searchOpen = false;
        searchReveal = 0.0F;
        searchVelocity = 0.0F;
        machineRouting = null;
        machineRoutingOpen = false;
        machineRoutingReveal = 0.0F;
        machineRoutingVelocity = 0.0F;
        searchScrollRow = 0;
        searchIndexAge = 0;
        draggedBaseItem = null;
        helpReveal = 0.0F;
        helpVelocity = 0.0F;
        helpOpenedNanos = plannerStartedNanos;
        Entity currentCamera = minecraft.getCameraEntity();
        if (previousCamera == null && currentCamera != cameraAnchor) {
            previousCamera = currentCamera;
        }
        if (previousCameraType == null) {
            previousCameraType = minecraft.options.getCameraType();
        }
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        cameraPosition = desiredCameraPosition();
        cameraVelocity = Vec3.ZERO;
        cameraYawVelocity = 0.0F;
        cameraPitchVelocity = 0.0F;
        cameraFocusPos = null;
        cameraFocusBlend = 0.0F;
        priorityTargetPos = null;
        faceTableImmediately();
        lastCameraFrame = plannerStartedNanos;
        cameraAnchor = new ArmorStand(minecraft.level, cameraPosition.x, cameraPosition.y, cameraPosition.z);
        cameraAnchor.setInvisible(true);
        cameraAnchor.setNoGravity(true);
        applyCameraTransform();
        minecraft.setCameraEntity(cameraAnchor);
        refreshFilterItems();
        refreshBaseInventoryIndex();
        beginWorkstationScan();
        // Workstations are sparse but the original tick-sliced volume scan made
        // the first useful glow arrive seconds after the planner opened.
        scanWorkstationIndexStep(500_000);
        refreshSuitableBlocks();
        searchBox = new EditBox(font(), 0, 0, 80, 12, Component.literal("Search base items"));
        searchBox.setBordered(false);
        searchBox.setTextColor(INK);
        searchBox.setTextShadow(true);
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.literal(searchPrompt()));
        searchBox.setResponder(value -> searchScrollRow = 0);
        searchBox.setVisible(false);
        addRenderableWidget(searchBox);
        ClientPacketDistributor.sendToServer(new RequestCraftingCataloguePayload(tablePos));
        ClientPacketDistributor.sendToServer(new RequestBaseInventoryPayload(tablePos));
        if (!resumeDraft) {
            ClientPacketDistributor.sendToServer(new RequestDinosaurWorkStatePayload(dodo.getId(), tablePos));
        }
    }

    public static void acceptCraftingCatalogue(List<String> identifiers) {
        WorksitePlannerScreen screen = active;
        if (screen == null) return;
        screen.craftingCatalogue = identifiers == null ? List.of() : List.copyOf(identifiers);
        screen.refreshPickerItems();
        if (screen.jobIndex == 3) {
            screen.feedback(screen.craftingCatalogue.size() + " crafting results indexed from the server.");
        }
    }

    public static void acceptBaseInventory(BaseInventoryPayload payload) {
        WorksitePlannerScreen screen = active;
        if (screen == null || !screen.tablePos.equals(payload.commandTablePos())) return;
        screen.installBaseInventory(payload.containers());
    }

    public static void acceptMachineRouting(BaseMachineRoutingPayload payload) {
        WorksitePlannerScreen screen = active;
        if (screen == null || !screen.tablePos.equals(payload.tablePos())) return;
        screen.machineRouting = payload;
        screen.machineRoutingOpen = true;
        screen.refreshBaseInventoryIndex();
        screen.refreshSuitableBlocks();
        screen.feedback("Routing shown for " + payload.machineName() + ". IN delivers; OUT collects.");
    }

    public static void acceptWorkState(DinosaurWorkStatePayload payload) {
        WorksitePlannerScreen screen = active;
        AssignDodoWorkPayload saved = payload.assignment();
        if (screen == null
                || screen.dodo.getId() != saved.entityId()
                || !screen.tablePos.equals(saved.commandTablePos())) return;
        screen.workStateReceived = true;
        int receivedRadius = Mth.clamp(payload.baseRadius(), 8, 128);
        if (screen.baseRadius != receivedRadius) {
            screen.baseRadius = receivedRadius;
            screen.restartWorkstationScan();
        }
        if (screen.jobIndex != saved.jobIndex()) return;

        screen.sourcePositions.clear();
        screen.sourcePositions.addAll(saved.sourcePositions());
        screen.workstationPositions.clear();
        screen.workstationPositions.addAll(saved.workstationPositions());
        screen.destinationPositions.clear();
        screen.destinationPositions.addAll(saved.destinationPositions());
        screen.fallbackPositions.clear();
        screen.fallbackPositions.addAll(saved.fallbackPositions());
        screen.blockPriorities.clear();
        screen.blockPriorities.putAll(saved.blockPriorities());
        screen.sourcePos = firstPosition(screen.sourcePositions);
        screen.workstationPos = firstPosition(screen.workstationPositions);
        screen.destinationPos = firstPosition(screen.destinationPositions);
        screen.areaEndPos = saved.areaEndPos().orElse(null);
        screen.fallbackPos = firstPosition(screen.fallbackPositions);
        screen.itemFilters.clear();
        screen.itemFilters.addAll(saved.itemFilters());
        screen.itemFilter = screen.itemFilters.isEmpty() ? "" : screen.itemFilters.getFirst();
        screen.fuelFilters.clear();
        screen.fuelFilters.addAll(saved.fuelFilters());
        screen.expeditionTier = Mth.clamp(saved.expeditionTier(), 0, 4);
        screen.priority = Mth.clamp(saved.priority(), 0, 3);
        screen.batchSize = Mth.clamp(saved.batchSize(), 1, 64);
        screen.schedule = Mth.clamp(saved.schedule(), 0, 2);
        screen.sourceReserve = Mth.clamp(saved.sourceReserve(), 0, 4096);
        screen.destinationTarget = Mth.clamp(saved.destinationTarget(), 0, 4096);
        screen.repeatMode = Mth.clamp(saved.repeatMode(), 0, 2);
        screen.routePolicy = Mth.clamp(saved.routePolicy(), 0, 2);
        screen.exactItemMatch = saved.exactItemMatch();
        screen.avoidDanger = saved.avoidDanger();
        screen.normalizeDraftForSpecialty();
        Selection[] selections = screen.selections();
        screen.selection = screen.jobIndex == 4
                ? selections[screen.expeditionTier]
                : selections[0];
        screen.refreshPickerItems();
        screen.refreshSuitableBlocks();
        screen.feedback(payload.enabled() ? "Saved work order loaded." : "Saved order loaded and ready to restart.");
    }

    @Override
    public void tick() {
        scanWorkstationIndexStep(6_000);
        if (searchOpen && usesPlayerInventoryPicker()) {
            // The fire picker mirrors the player's live inventory. This is a tiny local copy and
            // deliberately never asks the server to rescan every storage block in the base.
            refreshPickerItems();
        } else if ((searchOpen || selection == Selection.SOURCE || selection == Selection.DESTINATION)
                && ++searchIndexAge >= 40) {
            searchIndexAge = 0;
            refreshBaseInventoryIndex();
            refreshSuitableBlocks();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateCameraMotion();
        renderTimeTicks = (Util.getNanos() - plannerStartedNanos) / 50_000_000.0F;
        pointerX = mouseX;
        pointerY = mouseY;
        hoveredPos = rayTrace(mouseX, mouseY);
        updateTopDrawer(mouseX, mouseY);
        drawTopDrawer(graphics, mouseX, mouseY);
        drawLeftDrawer(graphics, mouseX, mouseY);
        drawSpecialtyDock(graphics, mouseX, mouseY);
        drawHelpPanel(graphics, mouseX, mouseY);
        drawSearchDock(graphics, mouseX, mouseY);
        drawMachineRoutingPanel(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawDraggedBaseItem(graphics);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && worldViewport().contains(event.x(), event.y())) {
            if (hoveredPos != null && minecraft.level != null
                    && minecraft.level.getBlockEntity(hoveredPos) instanceof Container) {
                ClientPacketDistributor.sendToServer(new OpenBaseMachineMenuPayload(tablePos, hoveredPos));
                feedback("Loading automation slots for " + blockName(hoveredPos) + "...");
                return true;
            }
            draggingCamera = true;
            return true;
        }
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        if (machineRoutingOpen && machineRoutingReveal > 0.08F && handleMachineRoutingClick(event.x(), event.y())) {
            return true;
        }
        SearchLayout search = searchLayout(searchReveal);
        if (supportsItemFilter() && search.button.contains(event.x(), event.y())) {
            if (searchOpen) {
                closeSearchPicker();
            } else {
                openSearchPicker();
            }
            return true;
        }
        if (searchReveal > 0.08F) {
            BaseItemEntry clickedEntry = searchEntryAt(search, event.x(), event.y());
            if (clickedEntry != null) {
                if (usesPlayerInventoryPicker() && !isFirePickerItem(clickedEntry.stack)) {
                    feedback("That inventory item is neither fuel nor a smeltable input.");
                    return true;
                }
                draggedBaseItem = clickedEntry;
                feedback("Drag " + clickedEntry.stack.getHoverName().getString() + " to the " + filterSlotName().toLowerCase() + " slot.");
                return true;
            }
            if (search.results.contains(event.x(), event.y())) {
                return true;
            }
            if (search.bar.contains(event.x(), event.y())) {
                return super.mouseClicked(event, doubleClick);
            }
        }
        TopDrawerLayout top = topDrawerLayout(topDrawerReveal);
        if (top.previousJob.contains(event.x(), event.y())) {
            cycleJob(-1);
            return true;
        }
        if (top.nextJob.contains(event.x(), event.y())) {
            cycleJob(1);
            return true;
        }
        LeftDrawerLayout left = leftDrawerLayout(leftDrawerReveal);
        Selection[] available = selections();
        for (int index = 0; index < available.length; index++) {
            if (left.slots[index].contains(event.x(), event.y())) {
                selection = available[index];
                if (jobIndex == 4) {
                    if (!WorkSpecialtyRules.canAttemptExpedition(index, dodo.getSpecialtyStars(4))) {
                        feedback("Primordial Frontier requires at least two-star expedition skill.");
                        return true;
                    }
                    expeditionTier = index;
                    feedback(expeditionName(index) + " selected. " + expeditionDurationMinutes(index)
                            + " minutes, " + expeditionRiskPercent(index) + "% incapacitation risk.");
                    refreshPickerItems();
                    return true;
                }
                refreshSuitableBlocks();
                feedback(selectionInstruction(selection));
                return true;
            }
        }
        if (supportsItemFilter() && available.length < left.slots.length
                && left.slots[available.length].contains(event.x(), event.y())) {
            openSearchPicker();
            return true;
        }
        HelpPanelLayout help = helpPanelLayout();
        if (pinnedTool == PlannerTool.FILTER && jobIndex == 1) {
            int fuelSlot = slotAt(fireFuelSlots(help), event.x(), event.y());
            int inputSlot = slotAt(fireInputSlots(help), event.x(), event.y());
            if (fuelSlot >= 0) {
                removeFilterAt(fuelFilters, fuelSlot, "fuel");
                return true;
            }
            if (inputSlot >= 0) {
                removeFilterAt(itemFilters, inputSlot, "input");
                itemFilter = itemFilters.isEmpty() ? "" : itemFilters.getFirst();
                return true;
            }
        }
        if (pinnedTool == PlannerTool.FILTER && jobIndex != 1 && help.anyItem.contains(event.x(), event.y())) {
            itemFilters.clear();
            itemFilter = "";
            refreshSuitableBlocks();
            feedback("This order now accepts any item found in its sources.");
            return true;
        }
        if (pinnedTool == PlannerTool.FILTER && jobIndex != 1 && help.filterTarget.contains(event.x(), event.y())) {
            feedback(itemFilters.isEmpty()
                    ? "Open the compass and drag an item here."
                    : "Drag another item here to replace " + shortIdentifier(itemFilters.getFirst()).toLowerCase() + ".");
            return true;
        }
        SpecialtyDockLayout dock = specialtyDockLayout((topDrawerReveal + leftDrawerReveal) * 0.5F);
        PlannerTool clickedTool = toolAt(dock, event.x(), event.y());
        if (clickedTool != null) {
            activateTool(clickedTool);
            return true;
        }
        if (helpReveal > 0.08F && help.outer.contains(event.x(), event.y())) {
            int option = helpOptionAt(help, event.x(), event.y());
            if (option >= 0 && pinnedTool != null) {
                applyHelpOption(pinnedTool, option);
            }
            return true;
        }
        if (worldViewport().contains(event.x(), event.y()) && hoveredPos != null) {
            if (pinnedTool == PlannerTool.PRIORITY && isSelectedWorkBlock(hoveredPos)) {
                priorityTargetPos = hoveredPos.immutable();
                helpOpenedNanos = Util.getNanos();
                feedback("Choose a priority for " + blockName(hoveredPos) + " at " + compactPos(hoveredPos) + ".");
                return true;
            }
            assignSelection(hoveredPos);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void cycleJob(int direction) {
        jobIndex = Math.floorMod(jobIndex + direction, JOBS.length);
        normalizeDraftForSpecialty();
        Selection[] nextSelections = selections();
        selection = jobIndex == 4 ? nextSelections[expeditionTier] : nextSelections[0];
        priorityTargetPos = null;
        pinnedTool = searchOpen && supportsItemFilter() ? PlannerTool.FILTER : null;
        hoveredTool = null;
        if (searchBox != null) {
            searchBox.setHint(Component.literal(searchPrompt()));
        }
        refreshPickerItems();
        refreshSuitableBlocks();
        feedback(JOBS[jobIndex] + " selected. " + selection.instruction);
    }

    private void openSearchPicker() {
        if (!supportsItemFilter()) {
            feedback("This specialty uses placed blocks, not an item search.");
            return;
        }
        searchOpen = true;
        if (supportsItemFilter()) {
            pinnedTool = PlannerTool.FILTER;
        }
        helpOpenedNanos = Util.getNanos();
        refreshPickerItems();
        if (!usesPlayerInventoryPicker()) {
            refreshBaseInventoryIndex();
        }
        refreshSuitableBlocks();
        if (searchBox != null) {
            searchBox.setHint(Component.literal(searchPrompt()));
            searchBox.setFocused(!usesPlayerInventoryPicker());
        }
    }

    private void closeSearchPicker() {
        searchOpen = false;
        draggedBaseItem = null;
        if (pinnedTool == PlannerTool.FILTER) {
            pinnedTool = null;
        }
        if (searchBox != null) {
            searchBox.setFocused(false);
        }
        helpOpenedNanos = Util.getNanos();
    }

    private void cycleRule(int index) {
        switch (index) {
            case 0 -> priority = (priority + 1) % PRIORITIES.length;
            case 1 -> batchSize = BATCHES[(indexOf(BATCHES, batchSize) + 1) % BATCHES.length];
            case 2 -> sourceReserve = STOCK_LEVELS[(indexOf(STOCK_LEVELS, sourceReserve) + 1) % STOCK_LEVELS.length];
            case 3 -> destinationTarget = STOCK_LEVELS[(indexOf(STOCK_LEVELS, destinationTarget) + 1) % STOCK_LEVELS.length];
            case 4 -> schedule = (schedule + 1) % SCHEDULES.length;
            case 5 -> repeatMode = (repeatMode + 1) % REPEATS.length;
            case 6 -> routePolicy = (routePolicy + 1) % ROUTES.length;
            case 7 -> avoidDanger = !avoidDanger;
            default -> {
            }
        }
        feedback(index == 4 && schedule == 2 ? nightShiftWarning() : ruleHelp(index));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && draggedBaseItem != null) {
            return true;
        }
        if (event.button() == 1 && draggingCamera) {
            orbitYaw = Mth.wrapDegrees(orbitYaw - (float) dragX * 0.42F);
            orbitPitch = Mth.clamp(orbitPitch + (float) dragY * 0.32F, 15.0F, 72.0F);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggedBaseItem != null) {
            HelpPanelLayout help = helpPanelLayout();
            if (jobIndex == 1) {
                int fuelSlot = slotAt(fireFuelSlots(help), event.x(), event.y());
                int inputSlot = slotAt(fireInputSlots(help), event.x(), event.y());
                if (fuelSlot >= 0 && minecraft.level.fuelValues().isFuel(draggedBaseItem.stack)) {
                    setFilterAt(fuelFilters, fuelSlot, draggedBaseItem.stack);
                    feedback(draggedBaseItem.stack.getHoverName().getString() + " added to allowed fuels.");
                } else if (inputSlot >= 0 && minecraft.level.recipeAccess()
                        .propertySet(net.minecraft.world.item.crafting.RecipePropertySet.FURNACE_INPUT)
                        .test(draggedBaseItem.stack)) {
                    setFilterAt(itemFilters, inputSlot, draggedBaseItem.stack);
                    itemFilter = itemFilters.isEmpty() ? "" : itemFilters.getFirst();
                    feedback(draggedBaseItem.stack.getHoverName().getString() + " added to furnace inputs.");
                } else if (fuelSlot >= 0) {
                    feedback("That item is not valid fuel.");
                } else if (inputSlot >= 0) {
                    feedback("That item has no smelting recipe.");
                } else {
                    feedback("Drop fuel on the upper row or a smeltable input on the lower row.");
                }
                refreshSuitableBlocks();
            } else if (help.filterTarget.contains(event.x(), event.y())) {
                String identifier = BuiltInRegistries.ITEM.getKey(draggedBaseItem.stack.getItem()).toString();
                itemFilters.clear();
                itemFilters.add(identifier);
                itemFilter = identifier;
                pinnedTool = PlannerTool.FILTER;
                refreshSuitableBlocks();
                feedback(draggedBaseItem.stack.getHoverName().getString() + " assigned to this work order.");
            } else {
                feedback("Item not assigned. Drop it on the " + filterSlotName().toLowerCase() + " slot.");
            }
            draggedBaseItem = null;
            return true;
        }
        if (event.button() == 1) {
            draggingCamera = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        SearchLayout search = searchLayout(searchReveal);
        if (searchReveal > 0.08F && search.results.contains(mouseX, mouseY)) {
            if (usesPlayerInventoryPicker()) {
                return true;
            }
            List<BaseItemEntry> filtered = filteredBaseItems();
            int maximumRow = Math.max(0, (filtered.size() - 1) / 9 - 3);
            searchScrollRow = Mth.clamp(searchScrollRow + (scrollY < 0.0D ? 1 : -1), 0, maximumRow);
            return true;
        }
        if (topDrawerLayout(topDrawerReveal).carousel.contains(mouseX, mouseY)) {
            cycleJob(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        if (worldViewport().contains(mouseX, mouseY)) {
            if (scrollY > 0.0D) {
                BlockPos focus = findZoomFocus(mouseX, mouseY);
                if (focus != null) {
                    if (!focus.equals(cameraFocusPos)) {
                        focusCameraOn(focus);
                    }
                } else if (cameraFocusPos != null) {
                    releaseCameraFocus("Camera unlocked. Zooming around the Command Table.");
                }
            } else if (scrollY < 0.0D && cameraFocusPos != null) {
                releaseCameraFocus("Camera unlocked. Zooming back toward the Command Table.");
            }
            orbitDistance = Mth.clamp(orbitDistance - (float) scrollY * 2.25F,
                    cameraFocusPos == null ? 5.0F : 2.75F, Math.max(50.0F, baseRadius + 5.0F));
            if (cameraFocusPos == null && feedbackUntilNanos <= Util.getNanos()) {
                feedback("Camera range: " + Math.round(orbitDistance) + " blocks");
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && machineRoutingOpen) {
            closeMachineRouting();
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE && searchOpen) {
            closeSearchPicker();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
        restoreCamera();
    }

    private void restoreCamera() {
        if (minecraft != null) {
            minecraft.setCameraEntity(previousCamera != null ? previousCamera : minecraft.player);
            if (previousCameraType != null) {
                minecraft.options.setCameraType(previousCameraType);
            }
        }
        draggingCamera = false;
        cameraAnchor = null;
    }

    @Override
    public void onClose() {
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

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The planner is a world overlay. Vanilla's in-game screen gradient would dim the base beneath it.
    }

    private void updateTopDrawer(int mouseX, int mouseY) {
        long now = Util.getNanos();
        float deltaTime = (float) Mth.clamp((now - lastInterfaceFrameNanos) / 1_000_000_000.0D, 0.001D, 0.05D);
        lastInterfaceFrameNanos = now;

        TopDrawerLayout expandedTop = topDrawerLayout(1.0F);
        LeftDrawerLayout expandedLeft = leftDrawerLayout(1.0F);
        SpecialtyDockLayout expandedDock = specialtyDockLayout(1.0F);
        HelpPanelLayout expandedHelp = helpPanelLayout();
        SearchLayout expandedSearch = searchLayout(1.0F);
        boolean openingGrace = now - plannerStartedNanos < 1_250_000_000L;
        boolean topNear = mouseY <= expandedTop.visibleBottom + 10
                && mouseX >= expandedTop.canvas.x - 18
                && mouseX <= expandedTop.canvas.right() + 18;
        boolean leftNear = mouseX <= expandedLeft.rail.right() + 16
                && mouseY >= expandedLeft.rail.y - 12
                && mouseY <= expandedLeft.rail.bottom() + 12;
        boolean rightDockNear = mouseX >= expandedDock.rightRail.x - 16
                && mouseX <= expandedDock.rightRail.right() + 18
                && mouseY >= expandedDock.rightRail.y - 14
                && mouseY <= expandedDock.rightRail.bottom() + 14;
        boolean bottomDockNear = mouseX >= expandedDock.bottomRail.x - 14
                && mouseX <= expandedDock.bottomRail.right() + 14
                && mouseY >= expandedDock.bottomRail.y - 16
                && mouseY <= expandedDock.bottomRail.bottom() + 18;
        boolean dockNear = rightDockNear || bottomDockNear;
        boolean helpNear = currentHelpContent() != null
                && helpReveal > 0.05F
                && expandedHelp.outer.contains(mouseX, mouseY);
        boolean searchNear = expandedSearch.button.contains(mouseX, mouseY)
                || searchReveal > 0.05F && (expandedSearch.bar.contains(mouseX, mouseY) || expandedSearch.results.contains(mouseX, mouseY));
        boolean anyNear = openingGrace || topNear || leftNear || dockNear || helpNear || searchNear || searchOpen;

        float topTarget = anyNear ? 1.0F : 0.0F;
        float topAcceleration = (topTarget - topDrawerReveal) * 36.0F - topDrawerVelocity * 12.0F;
        topDrawerVelocity += topAcceleration * deltaTime;
        topDrawerReveal = clampSpring(topDrawerReveal + topDrawerVelocity * deltaTime, true);

        float leftTarget = anyNear ? 1.0F : 0.0F;
        float leftAcceleration = (leftTarget - leftDrawerReveal) * 36.0F - leftDrawerVelocity * 12.0F;
        leftDrawerVelocity += leftAcceleration * deltaTime;
        leftDrawerReveal = clampSpring(leftDrawerReveal + leftDrawerVelocity * deltaTime, false);

        float hoverBlend = 1.0F - (float) Math.exp(-7.0F * deltaTime);
        topDrawerHover = Mth.lerp(hoverBlend, topDrawerHover, anyNear ? 1.0F : 0.0F);
        leftDrawerHover = Mth.lerp(hoverBlend, leftDrawerHover, anyNear ? 1.0F : 0.0F);

        float searchTarget = searchOpen ? 1.0F : 0.0F;
        float searchAcceleration = (searchTarget - searchReveal) * 58.0F - searchVelocity * 14.0F;
        searchVelocity += searchAcceleration * deltaTime;
        searchReveal = Mth.clamp(searchReveal + searchVelocity * deltaTime, 0.0F, 1.0F);
        updateSearchBox(searchLayout(searchReveal));

        float routingTarget = machineRoutingOpen ? 1.0F : 0.0F;
        float routingAcceleration = (routingTarget - machineRoutingReveal) * 72.0F
                - machineRoutingVelocity * 16.0F;
        machineRoutingVelocity += routingAcceleration * deltaTime;
        machineRoutingReveal = Mth.clamp(machineRoutingReveal + machineRoutingVelocity * deltaTime, 0.0F, 1.0F);
        if (!machineRoutingOpen && machineRoutingReveal <= 0.001F) machineRouting = null;

        LeftDrawerLayout liveLeft = leftDrawerLayout(leftDrawerReveal);
        int hoveredSlot = workflowSlotAt(liveLeft, mouseX, mouseY);
        boolean filterSlotHovered = supportsItemFilter() && hoveredSlot == selections().length;
        if (hoveredSlot >= selections().length) {
            hoveredSlot = -1;
        }
        SpecialtyDockLayout liveDock = specialtyDockLayout((topDrawerReveal + leftDrawerReveal) * 0.5F);
        PlannerTool newHoveredTool = toolAt(liveDock, mouseX, mouseY);
        if (newHoveredTool == null && filterSlotHovered) {
            newHoveredTool = PlannerTool.FILTER;
        }
        if (hoveredSlot != hoveredWorkflowSlot || newHoveredTool != hoveredTool) {
            helpOpenedNanos = now;
        }
        hoveredWorkflowSlot = hoveredSlot;
        hoveredTool = newHoveredTool;
        float helpTarget = anyNear ? 1.0F : 0.0F;
        float helpAcceleration = (helpTarget - helpReveal) * 64.0F - helpVelocity * 15.0F;
        helpVelocity += helpAcceleration * deltaTime;
        helpReveal = Mth.clamp(helpReveal + helpVelocity * deltaTime, 0.0F, 1.0F);
    }

    private float clampSpring(float value, boolean top) {
        if (value < 0.0F) {
            if (top) {
                topDrawerVelocity = Math.max(0.0F, topDrawerVelocity);
            } else {
                leftDrawerVelocity = Math.max(0.0F, leftDrawerVelocity);
            }
            return 0.0F;
        }
        if (value > 1.0F) {
            if (top) {
                topDrawerVelocity = Math.min(0.0F, topDrawerVelocity);
            } else {
                leftDrawerVelocity = Math.min(0.0F, leftDrawerVelocity);
            }
            return 1.0F;
        }
        return value;
    }

    private void drawTopDrawer(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TopDrawerLayout layout = topDrawerLayout(topDrawerReveal);
        float subpixelY = topDrawerExactY(topDrawerReveal) - layout.canvas.y;
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, subpixelY);
        int alpha = Math.round(Mth.lerp(topDrawerHover, 112.0F, 255.0F) * plannerFocusAlpha());
        drawPlannerRegion(graphics, layout.canvas, layout.clip, alpha);

        String identityText = displayName() + "  /  ASSIGN";
        BlockPos selectedPosition = position(selection);
        String stepText = selectionLabel(selection) + "  /  "
                + (jobIndex == 4 ? expeditionStatusLabel(expeditionTier)
                : selectedPosition == null ? "SELECT" : compactPosTight(selectedPosition));
        int identityColor = layout.identity.contains(mouseX, mouseY) ? CORAL : INK;
        int stepColor = layout.step.contains(mouseX, mouseY) ? selection.color : INK;
        boldCenteredFit(graphics, identityText, layout.identity, withAlpha(identityColor, alpha));
        boldCenteredFit(graphics, stepText, layout.step, withAlpha(stepColor, alpha));

        boldCenteredFit(graphics, "<", layout.previousJob, withAlpha(layout.previousJob.contains(mouseX, mouseY) ? CORAL : INK, alpha));
        boldCenteredFit(graphics, ">", layout.nextJob, withAlpha(layout.nextJob.contains(mouseX, mouseY) ? CORAL : INK, alpha));
        Rect icon = new Rect(layout.jobName.x + 5, layout.jobName.centerY() - 4, 9, 8);
        drawTintedTexture(graphics, JOB_ICONS[jobIndex], icon, withAlpha(0xFFFFFFFF, alpha));
        Rect jobText = new Rect(icon.right() + 3, layout.jobName.y, layout.jobName.right() - icon.right() - 6, layout.jobName.height);
        boldCenteredFit(graphics, JOBS[jobIndex] + "  " + dodo.getSpecialtyStars(jobIndex)
                        + " STAR  " + dodo.getWorkEfficiencyPercent(jobIndex) + "%", jobText,
                withAlpha(jobColor(jobIndex), alpha));
        graphics.pose().popMatrix();
    }

    private void drawLeftDrawer(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        LeftDrawerLayout layout = leftDrawerLayout(leftDrawerReveal);
        float subpixelX = leftDrawerExactX(leftDrawerReveal) - layout.canvas.x;
        graphics.pose().pushMatrix();
        graphics.pose().translate(subpixelX, 0.0F);
        int alpha = Math.round(Mth.lerp(leftDrawerHover, 112.0F, 255.0F) * plannerFocusAlpha());
        drawPlannerRegion(graphics, layout.canvas, layout.clip, alpha);

        Selection[] available = selections();
        int completed = 0;
        if (jobIndex == 4) {
            completed = expeditionTier + 1;
        } else {
            for (Selection option : available) if (position(option) != null) completed++;
        }
        if (completed > 0) {
            int fillHeight = Math.max(1, Math.round(layout.progress.height * (completed / (float) available.length)));
            graphics.fill(
                    layout.progress.x,
                    layout.progress.bottom() - fillHeight,
                    layout.progress.right(),
                    layout.progress.bottom(),
                    withAlpha(GREEN, Math.round(alpha * 0.48F))
            );
        }

        for (int index = 0; index < layout.slots.length; index++) {
            Rect slot = layout.slots[index];
            if (index < available.length) {
                Selection option = available[index];
                boolean selected = jobIndex == 4 ? expeditionTier == index : option == selection;
                boolean assigned = jobIndex == 4 ? expeditionTier == index : position(option) != null;
                if (selected) {
                    int pulse = 28 + Math.round((Mth.sin(renderTimeTicks * 0.16F) + 1.0F) * 15.0F);
                    graphics.fill(slot.x, slot.y, slot.right(), slot.bottom(), withAlpha(option.color, pulse));
                    outline(graphics, slot, withAlpha(option.color, alpha));
                } else if (assigned) {
                    graphics.fill(slot.x + 1, slot.y + 1, slot.right() - 1, slot.bottom() - 1, withAlpha(GREEN, 42));
                }
                drawLeftItem(graphics, option.icon, slot);
                if (searchReveal > 0.01F) {
                    graphics.fill(slot.x - 1, slot.y - 1, slot.right() + 1, slot.bottom() + 1,
                            withAlpha(0xFFD0AD89, Math.round(searchReveal * 190.0F)));
                }
                if (assigned) {
                    graphics.fill(slot.right() - 4, slot.y + 1, slot.right() - 1, slot.y + 4, withAlpha(GREEN, alpha));
                }
            }
        }
        if (supportsItemFilter() && available.length < layout.slots.length) {
            Rect slot = layout.slots[available.length];
            boolean hovered = slot.contains(mouseX, mouseY);
            boolean activeFilter = pinnedTool == PlannerTool.FILTER || !itemFilters.isEmpty();
            if (hovered || activeFilter) {
                int glowAlpha = hovered ? 70 : 36;
                graphics.fill(slot.x, slot.y, slot.right(), slot.bottom(), withAlpha(AMBER, glowAlpha));
                outline(graphics, slot, withAlpha(hovered ? CREAM : AMBER, alpha));
            }
            ItemStack selectedItem = selectedFilterStack();
            drawLeftItem(graphics, selectedItem.isEmpty() ? Items.HOPPER.getDefaultInstance() : selectedItem, slot);
            if (!itemFilters.isEmpty()) {
                graphics.fill(slot.right() - 4, slot.y + 1, slot.right() - 1, slot.y + 4, withAlpha(GREEN, alpha));
            }
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                graphics.setComponentTooltipForNextFrame(font(), List.of(
                        Component.literal(filterTitle()).withStyle(style -> style.withBold(true)),
                        Component.literal(selectedItem.isEmpty()
                                ? "Click to choose an item type."
                                : "Current: " + selectedItem.getHoverName().getString())
                ), mouseX, mouseY);
            }
        }
        graphics.pose().popMatrix();
    }

    private void drawSpecialtyDock(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float reveal = (topDrawerReveal + leftDrawerReveal) * 0.5F;
        SpecialtyDockLayout layout = specialtyDockLayout(reveal);
        int alpha = Math.round(Mth.lerp((topDrawerHover + leftDrawerHover) * 0.5F, 112.0F, 255.0F) * plannerFocusAlpha());

        graphics.pose().pushMatrix();
        graphics.pose().translate(specialtyRightOffset(reveal) - layout.rightOffset, 0.0F);
        drawPlannerRegion(graphics, layout.rightCanvas, layout.rightRail, alpha);
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, specialtyBottomOffset(reveal) - layout.bottomOffset);
        drawPlannerRegion(graphics, layout.bottomCanvas, layout.bottomRail, alpha);
        graphics.pose().popMatrix();

        int verticalCount = Math.min(6, layout.tools.length);
        float rightResidual = specialtyRightOffset(reveal) - layout.rightOffset;
        float bottomResidual = specialtyBottomOffset(reveal) - layout.bottomOffset;
        long now = Util.getNanos();
        for (int index = 0; index < layout.tools.length; index++) {
            PlannerTool tool = layout.tools[index];
            Rect slot = layout.slots[index];
            boolean hovered = slot.contains(mouseX, mouseY);
            boolean pinned = tool == pinnedTool;
            if (hovered) {
                toolHoverStartedNanos.putIfAbsent(tool, now);
            } else {
                toolHoverStartedNanos.remove(tool);
            }
            float hoverSeconds = hovered
                    ? (now - toolHoverStartedNanos.getOrDefault(tool, now)) / 1_000_000_000.0F
                    : 0.0F;
            float hoverKick = hovered
                    ? (1.0F - (float) Math.exp(-hoverSeconds * 18.0F)) * (float) Math.exp(-hoverSeconds * 2.8F)
                    : 0.0F;
            if (hoverSeconds > 1.35F) {
                hoverKick = 0.0F;
            }
            float pressSeconds = tool == pressedTool ? (now - pressedToolNanos) / 1_000_000_000.0F : 1.0F;
            float pressKick = pressSeconds < 0.28F ? Mth.sin(Mth.clamp(pressSeconds / 0.28F, 0.0F, 1.0F) * Mth.PI) : 0.0F;
            float introProgress = Mth.clamp(reveal * 1.18F - index * 0.025F, 0.0F, 1.0F);
            float introScale = 0.42F + 0.58F * popupSpring(introProgress, 6.8F, 10.8F);
            float pinnedPulse = pinned ? (Mth.sin(renderTimeTicks * 0.18F + index) + 1.0F) * 0.008F : 0.0F;
            float animatedScale = introScale * (1.0F + hoverKick * 0.075F + pinnedPulse - pressKick * 0.12F);
            float wobbleX = Mth.sin(renderTimeTicks * 0.54F + index * 1.37F) * 0.8F * hoverKick;
            float wobbleY = Mth.sin(renderTimeTicks * 0.69F + index * 0.91F) * 0.45F * hoverKick;
            graphics.pose().pushMatrix();
            graphics.pose().translate(index < verticalCount ? rightResidual : 0.0F, index < verticalCount ? 0.0F : bottomResidual);
            graphics.pose().translate(slot.centerX() + wobbleX, slot.centerY() + wobbleY);
            graphics.pose().scale(animatedScale, animatedScale);
            graphics.pose().translate(-slot.centerX(), -slot.centerY());
            if (hovered || pinned) {
                int glowAlpha = hovered ? 78 : 46;
                Rect inner = new Rect(slot.x + 1, slot.y + 1, Math.max(1, slot.width - 2), Math.max(1, slot.height - 2));
                graphics.fill(inner.x, inner.y, inner.right(), inner.bottom(), withAlpha(toolColor(tool), glowAlpha));
                outline(graphics, inner, withAlpha(toolColor(tool), alpha));
            }
            drawDockItem(graphics, tool.icon, slot);
            if (searchReveal > 0.01F) {
                graphics.fill(slot.x, slot.y, slot.right(), slot.bottom(),
                        withAlpha(0xFFD0AD89, Math.round(searchReveal * 190.0F)));
            }
            graphics.pose().popMatrix();
        }
        if (pressedTool != null && (now - pressedToolNanos) > 280_000_000L) {
            pressedTool = null;
        }
    }

    private void drawSearchDock(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!supportsItemFilter()) {
            return;
        }
        SearchLayout layout = searchLayout(searchReveal);
        float leftResidual = leftDrawerExactX(leftDrawerReveal) - leftDrawerLayout(leftDrawerReveal).canvas.x;
        int baseAlpha = Math.round(Mth.lerp(leftDrawerHover, 112.0F, 255.0F));
        graphics.pose().pushMatrix();
        graphics.pose().translate(leftResidual, 0.0F);
        drawPlannerRegion(graphics, layout.canvas, layout.slotFrame, baseAlpha);
        boolean hoveredButton = layout.button.contains(mouseX, mouseY);
        float hoverLift = hoveredButton ? Mth.sin(renderTimeTicks * 0.42F) * 0.55F : 0.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, hoverLift);
        if (hoveredButton || searchOpen) {
            graphics.fill(layout.button.x + 1, layout.button.y + 1, layout.button.right() - 1, layout.button.bottom() - 1,
                    withAlpha(CYAN, hoveredButton ? 72 : 42));
            outline(graphics, layout.button, withAlpha(CYAN, baseAlpha));
        }
        drawDockItem(graphics, Items.COMPASS.getDefaultInstance(), layout.button);
        graphics.pose().popMatrix();
        graphics.pose().popMatrix();

        if (searchReveal > 0.01F) {
            int revealedWidth = Math.max(1, Math.round(layout.bar.width * searchReveal));
            graphics.enableScissor(layout.bar.x, layout.bar.y, layout.bar.x + revealedWidth, layout.bar.bottom());
            drawSpaceBubble(graphics, layout.bar);
            if (usesPlayerInventoryPicker()) {
                boldCenteredFit(graphics, "INVENTORY  /  TEMPLATE ONLY", layout.bar, INK);
            }
            graphics.disableScissor();
            drawSearchResults(graphics, layout, mouseX, mouseY);
        }

        if (hoveredButton) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            graphics.setComponentTooltipForNextFrame(font(), List.of(
                    Component.literal(searchPrompt()).withStyle(style -> style.withBold(true)),
                    Component.literal(searchOpen ? "Click to close the picker." : searchScopeDescription())
            ), mouseX, mouseY);
        }
    }

    private void drawSearchResults(GuiGraphicsExtractor graphics, SearchLayout layout, int mouseX, int mouseY) {
        List<BaseItemEntry> filtered = filteredBaseItems();
        boolean inventoryPicker = usesPlayerInventoryPicker();
        int maximumRow = inventoryPicker ? 0 : Math.max(0, (filtered.size() - 1) / 9 - 3);
        searchScrollRow = Mth.clamp(searchScrollRow, 0, maximumRow);
        float settled = popupSpring(Mth.clamp(searchReveal, 0.0F, 1.0F), 6.4F, 10.8F);
        float scale = 0.88F + 0.12F * settled;
        float offsetY = 12.0F * (1.0F - settled);
        int alpha = Math.round(255.0F * Mth.clamp(searchReveal * 1.25F, 0.0F, 1.0F));
        graphics.pose().pushMatrix();
        graphics.pose().translate(layout.results.centerX(), layout.results.bottom() + offsetY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-layout.results.centerX(), -layout.results.bottom());
        drawSearchPaperPanel(graphics, layout.results, alpha);
        String count = inventoryPicker
                ? "YOUR INVENTORY  /  DRAG A TEMPLATE"
                : filtered.isEmpty() ? "NO MATCHES" : filtered.size() + " " + searchResultLabel(filtered.size());
        boldLine(graphics, count, layout.results.x + 9, layout.results.y + 7,
                layout.results.width - 18, withAlpha(CYAN, alpha), Mth.clamp(0.78F * plannerScale(), 0.72F, 1.0F));

        int startIndex = searchScrollRow * 9;
        for (int slotIndex = 0; slotIndex < layout.itemSlots.length; slotIndex++) {
            int entryIndex = startIndex + slotIndex;
            Rect slot = layout.itemSlots[slotIndex];
            boolean hovered = slot.contains(mouseX, mouseY);
            drawSearchInventorySlot(graphics, slot, hovered, slotIndex, alpha);
            if (entryIndex >= filtered.size()) {
                continue;
            }
            BaseItemEntry entry = filtered.get(entryIndex);
            if (entry.stack.isEmpty()) {
                continue;
            }
            drawSearchItem(graphics, entry.stack, slot);
            if (inventoryPicker && entry.totalCount > 1) {
                drawInventoryStackCount(graphics, entry.stack, slot);
            }
            boolean invalidInventoryItem = inventoryPicker && !isFirePickerItem(entry.stack);
            if (invalidInventoryItem) {
                graphics.fill(slot.x + 2, slot.y + 2, slot.right() - 2, slot.bottom() - 2, withAlpha(INK, hovered ? 58 : 94));
            }
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                if (invalidInventoryItem) {
                    graphics.setComponentTooltipForNextFrame(font(), List.of(
                            entry.stack.getHoverName().copy().withStyle(style -> style.withBold(true)),
                            Component.literal("This is not fuel and has no smelting recipe.")
                    ), mouseX, mouseY);
                } else {
                    graphics.setComponentTooltipForNextFrame(font(), List.of(
                            entry.stack.getHoverName().copy().withStyle(style -> style.withBold(true)),
                            Component.literal(inventoryPicker ? "Inventory stack: " + entry.totalCount : "Stored in base: " + entry.totalCount),
                            Component.literal("Drag this item to the " + filterSlotName().toLowerCase() + " slot."),
                            Component.literal(inventoryPicker ? "Template only - the item is not consumed." : "Automation moves the real stacks for you.")
                    ), mouseX, mouseY);
                }
            }
        }
        if (!inventoryPicker && filtered.size() > layout.itemSlots.length) {
            int first = Math.min(filtered.size(), startIndex + 1);
            int last = Math.min(filtered.size(), startIndex + layout.itemSlots.length);
            boldLine(graphics, first + "–" + last + " / " + filtered.size(), layout.results.x + 9,
                    layout.results.bottom() - 12, layout.results.width - 18, withAlpha(INK, alpha), 0.72F);
        }
        graphics.pose().popMatrix();
    }

    private void drawMachineRoutingPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (machineRouting == null || machineRoutingReveal <= 0.001F) return;
        float eased = popupSpring(machineRoutingReveal, 7.4F, 11.8F);
        int alpha = Mth.clamp(Math.round(machineRoutingReveal * 255.0F), 0, 255);
        Rect panel = machineRoutingPanel();
        int slide = Math.round((1.0F - eased) * 24.0F);
        panel = new Rect(panel.x, panel.y + slide, panel.width, panel.height);

        graphics.fill(panel.x + 4, panel.y + 5, panel.right() + 4, panel.bottom() + 5,
                withAlpha(0xFF09070B, Math.round(alpha * 0.52F)));
        drawSpaceBubble(graphics, panel);
        graphics.fill(panel.x + 2, panel.y + 2, panel.right() - 2, panel.bottom() - 2,
                withAlpha(0xFF6D503F, Math.round(alpha * 0.12F)));

        Rect title = new Rect(panel.x + 5, panel.y + 5, panel.width - 27, 16);
        drawSpaceBubble(graphics, title);
        boldCenteredFit(graphics, machineRouting.machineName().toUpperCase() + "  /  ROUTING", title,
                withAlpha(CREAM, alpha));
        Rect close = machineRoutingClose(panel);
        drawSpaceBubble(graphics, close);
        boldCenteredFit(graphics, "X", close, withAlpha(close.contains(mouseX, mouseY) ? CORAL : INK, alpha));

        Rect instruction = new Rect(panel.x + 7, title.bottom() + 3, panel.width - 14, 13);
        boldCenteredFit(graphics, "IN: DINOS DELIVER   /   OUT: DINOS COLLECT   /   AUTO: SLOT IS RESOLVED FOR YOU",
                instruction, withAlpha(MUTED, alpha));

        List<BaseMachineRoutingPayload.SlotInfo> visible = visibleRoutingSlots();
        List<Rect> cards = machineRoutingCards(panel, visible.size());
        for (int index = 0; index < visible.size(); index++) {
            BaseMachineRoutingPayload.SlotInfo slot = visible.get(index);
            Rect card = cards.get(index);
            drawSpaceBubble(graphics, card);
            boolean hovered = card.contains(mouseX, mouseY);
            if (hovered) {
                graphics.fill(card.x + 2, card.y + 2, card.right() - 2, card.bottom() - 2,
                        withAlpha(CREAM, 24));
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            Rect icon = new Rect(card.centerX() - 9, card.y + 5, 18, 18);
            drawSearchInventorySlot(graphics, icon, hovered, index, alpha);
            ItemStack stack = machineRoutingStack(slot);
            if (stack.isEmpty()) {
                boldCenteredFit(graphics, "+", new Rect(icon.x + 2, icon.y + 2,
                        icon.width - 4, icon.height - 4), withAlpha(MUTED, alpha));
            } else {
                drawSearchItem(graphics, stack, icon);
                if (slot.count() > 1) drawInventoryStackCount(graphics, stack.copyWithCount(slot.count()), icon);
            }
            Rect role = new Rect(card.x + 3, icon.bottom() + 2, card.width - 6, 10);
            boldCenteredFit(graphics, slot.role(), role, withAlpha(hovered ? CREAM : INK, alpha));

            Rect in = machineRoutingIn(card);
            Rect out = machineRoutingOut(card);
            drawRoutingToggle(graphics, in, "IN", slot.canInsert(), slot.insertEnabled(), slot.configurable(), mouseX, mouseY, alpha);
            drawRoutingToggle(graphics, out, "OUT", slot.canExtract(), slot.extractEnabled(), slot.configurable(), mouseX, mouseY, alpha);

            if (hovered) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(slot.role()).withStyle(style -> style.withBold(true)));
                if (!stack.isEmpty()) tooltip.add(stack.getHoverName());
                if (slot.configurable()) {
                    tooltip.add(Component.literal("Click IN or OUT to change this machine's automation route."));
                } else {
                    tooltip.add(Component.literal("Automatic: the machine exposes only the correct role for this slot."));
                }
                graphics.setComponentTooltipForNextFrame(font(), tooltip, mouseX, mouseY);
            }
        }
        if (machineRouting.slots().size() > visible.size()) {
            Rect footer = new Rect(panel.x + 7, panel.bottom() - 14, panel.width - 14, 10);
            boldCenteredFit(graphics, "+ " + (machineRouting.slots().size() - visible.size())
                    + " UNIFORM STORAGE SLOTS  /  ROUTED AUTOMATICALLY", footer, withAlpha(MUTED, alpha));
        }
    }

    private void drawRoutingToggle(GuiGraphicsExtractor graphics, Rect rect, String label, boolean available,
                                   boolean enabled, boolean configurable, int mouseX, int mouseY, int alpha) {
        int color = !available ? MUTED : enabled ? GREEN : CORAL;
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), withAlpha(color, available ? 54 : 20));
        outline(graphics, rect, withAlpha(color, Math.round(alpha * (rect.contains(mouseX, mouseY) ? 1.0F : 0.65F))));
        String text = !available ? "-" : configurable ? label : enabled ? "AUTO" : "-";
        boldCenteredFit(graphics, text, rect, withAlpha(available ? color : MUTED, alpha));
    }

    private boolean handleMachineRoutingClick(double mouseX, double mouseY) {
        if (machineRouting == null) return false;
        Rect panel = machineRoutingPanel();
        int slide = Math.round((1.0F - popupSpring(machineRoutingReveal, 7.4F, 11.8F)) * 24.0F);
        panel = new Rect(panel.x, panel.y + slide, panel.width, panel.height);
        if (machineRoutingClose(panel).contains(mouseX, mouseY)) {
            closeMachineRouting();
            return true;
        }
        if (!panel.contains(mouseX, mouseY)) return false;
        List<BaseMachineRoutingPayload.SlotInfo> visible = visibleRoutingSlots();
        List<Rect> cards = machineRoutingCards(panel, visible.size());
        for (int index = 0; index < visible.size(); index++) {
            BaseMachineRoutingPayload.SlotInfo slot = visible.get(index);
            Rect card = cards.get(index);
            boolean insert = machineRoutingIn(card).contains(mouseX, mouseY);
            boolean extract = machineRoutingOut(card).contains(mouseX, mouseY);
            if (!insert && !extract) continue;
            if (!slot.configurable()) {
                feedback(slot.role() + " is routed automatically because this machine has one valid slot for that role.");
                return true;
            }
            if (insert && !slot.canInsert() || extract && !slot.canExtract()) {
                feedback(slot.role() + " cannot be used in that direction.");
                return true;
            }
            ClientPacketDistributor.sendToServer(new ConfigureBaseMachineSlotPayload(
                    tablePos, machineRouting.machinePos(), slot.index(), insert));
            feedback((insert ? "Delivery" : "Collection") + " route updated for " + slot.role().toLowerCase() + ".");
            return true;
        }
        return true;
    }

    private void closeMachineRouting() {
        machineRoutingOpen = false;
        feedback("Slot routing closed. Your automation draft is still here.");
    }

    private Rect machineRoutingPanel() {
        int width = Math.min(this.width - 24, 356);
        int visibleSlots = machineRouting == null ? 1 : Math.min(8, machineRouting.slots().size());
        int rows = Math.max(1, (visibleSlots + 3) / 4);
        int height = 47 + rows * 66 + (machineRouting != null && machineRouting.slots().size() > 8 ? 14 : 4);
        return new Rect((this.width - width) / 2, (this.height - height) / 2, width, height);
    }

    private static Rect machineRoutingClose(Rect panel) {
        return new Rect(panel.right() - 20, panel.y + 5, 15, 16);
    }

    private static Rect machineRoutingIn(Rect card) {
        return new Rect(card.x + 4, card.bottom() - 16, (card.width - 11) / 2, 12);
    }

    private static Rect machineRoutingOut(Rect card) {
        Rect in = machineRoutingIn(card);
        return new Rect(in.right() + 3, in.y, card.right() - 4 - in.right() - 3, in.height);
    }

    private static List<Rect> machineRoutingCards(Rect panel, int count) {
        int columns = Math.min(4, Math.max(1, count));
        int gap = 4;
        int cardWidth = (panel.width - 16 - gap * (columns - 1)) / columns;
        List<Rect> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int row = index / columns;
            int column = index % columns;
            result.add(new Rect(panel.x + 8 + column * (cardWidth + gap), panel.y + 40 + row * 66,
                    cardWidth, 62));
        }
        return result;
    }

    private List<BaseMachineRoutingPayload.SlotInfo> visibleRoutingSlots() {
        if (machineRouting == null) return List.of();
        List<BaseMachineRoutingPayload.SlotInfo> relevant = machineRouting.slots().stream()
                .filter(slot -> slot.configurable() || !slot.itemIdentifier().isBlank())
                .limit(8)
                .toList();
        if (!relevant.isEmpty()) return relevant;
        return machineRouting.slots().stream().limit(8).toList();
    }

    private static ItemStack machineRoutingStack(BaseMachineRoutingPayload.SlotInfo slot) {
        Identifier identifier = Identifier.tryParse(slot.itemIdentifier());
        var holder = identifier == null ? null : BuiltInRegistries.ITEM.get(identifier).orElse(null);
        return holder == null ? ItemStack.EMPTY : holder.value().getDefaultInstance();
    }

    private void drawSearchPaperPanel(GuiGraphicsExtractor graphics, Rect panel, int alpha) {
        graphics.fill(panel.x + 4, panel.y + 5, panel.right() + 4, panel.bottom() + 5, withAlpha(0xFF000000, Math.round(alpha * 0.28F)));
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), withAlpha(0xFF4A332C, alpha));
        graphics.fill(panel.x + 2, panel.y + 2, panel.right() - 2, panel.bottom() - 2, withAlpha(0xFF88664F, alpha));
        graphics.fill(panel.x + 4, panel.y + 4, panel.right() - 4, panel.bottom() - 4, withAlpha(0xFFD0AD89, alpha));
        for (int y = panel.y + 7; y < panel.bottom() - 5; y += 7) {
            graphics.fill(panel.x + 5, y, panel.right() - 5, y + 1, withAlpha(0xFFFFFFFF, Math.round(alpha * 0.07F)));
        }
    }

    private void drawSearchInventorySlot(GuiGraphicsExtractor graphics, Rect rect, boolean hovered, int index, int alpha) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), withAlpha(0xFF4A332C, alpha));
        graphics.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, withAlpha(0xFF9A765A, alpha));
        graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, withAlpha(0xFFD7B392, alpha));
        if (hovered) {
            int pulse = 92 + Math.round((Mth.sin(renderTimeTicks * 0.24F + index) + 1.0F) * 46.0F);
            outline(graphics, new Rect(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2), withAlpha(0xFFFFFFFF, pulse));
            graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 2, withAlpha(CYAN, 34));
        }
    }

    private void drawDraggedBaseItem(GuiGraphicsExtractor graphics) {
        if (draggedBaseItem == null) {
            return;
        }
        graphics.pose().pushMatrix();
        float pulse = 1.0F + Mth.sin(renderTimeTicks * 0.32F) * 0.045F;
        graphics.pose().translate(pointerX, pointerY);
        graphics.pose().scale(pulse, pulse);
        graphics.item(draggedBaseItem.stack, -8, -8);
        graphics.pose().popMatrix();
    }

    private void drawHelpPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (helpReveal <= 0.01F) {
            return;
        }
        HelpContent content = currentHelpContent();
        if (content == null) {
            return;
        }
        HelpPanelLayout layout = helpPanelLayout();
        float progress = Mth.clamp((Util.getNanos() - helpOpenedNanos) / 240_000_000.0F, 0.0F, 1.0F);
        float settled = popupSpring(progress, 6.2F, 11.4F);
        float popScale = 0.74F + 0.26F * settled;
        float lift = 18.0F * (1.0F - settled);
        float helpFocus = pinnedTool == PlannerTool.FILTER && searchReveal > 0.01F ? 1.0F : plannerFocusAlpha();
        int alpha = Math.round(255.0F * Mth.clamp(helpReveal * 1.25F, 0.0F, 1.0F) * helpFocus);

        graphics.pose().pushMatrix();
        graphics.pose().translate(layout.outer.centerX(), layout.outer.centerY() + lift);
        graphics.pose().scale(popScale, popScale);
        graphics.pose().translate(-layout.outer.centerX(), -layout.outer.centerY());
        drawPlannerRegion(graphics, layout.canvas, layout.outer, alpha);
        List<HelpOption> options = currentHelpOptions();
        int hoveredOption = helpOptionAt(layout, mouseX, mouseY);
        int titleColor = hoveredOption >= 0 && hoveredOption < options.size()
                ? options.get(hoveredOption).color
                : content.accent;
        boldCenteredFit(graphics, content.title, layout.title, withAlpha(titleColor, alpha));
        drawHelpBody(graphics, layout, content, mouseX, mouseY, alpha);
        graphics.pose().popMatrix();
    }

    private void drawHelpBody(
            GuiGraphicsExtractor graphics,
            HelpPanelLayout layout,
            HelpContent content,
            int mouseX,
            int mouseY,
            int alpha
    ) {
        int lineX = layout.body.x + Math.max(3, Math.round(3.0F * plannerScale()));
        int maxWidth = layout.body.width - Math.max(6, Math.round(6.0F * plannerScale()));
        float textScale = Mth.clamp(0.88F * plannerScale(), 0.82F, 1.0F);
        int lineStep = Math.max(7, Math.round(font().lineHeight * textScale + 1.0F));
        int y = layout.body.y + Math.max(3, Math.round(3.0F * plannerScale()));
        boolean filterPicker = hoveredWorkflowSlot < 0
                && (hoveredTool == PlannerTool.FILTER || hoveredTool == null && pinnedTool == PlannerTool.FILTER);
        if (jobIndex == 4 && hoveredWorkflowSlot >= 0 && hoveredWorkflowSlot < selections().length) {
            drawExpeditionRewardPreview(graphics, layout, expeditionIndex(selections()[hoveredWorkflowSlot]),
                    mouseX, mouseY, alpha);
            return;
        }
        List<HelpOption> options = currentHelpOptions();
        if (!options.isEmpty() && !filterPicker) {
            drawHelpOptions(graphics, layout, options, mouseX, mouseY, alpha);
            return;
        }
        if (filterPicker && jobIndex == 1) {
            drawFireFilterPicker(graphics, layout, mouseX, mouseY, alpha);
            return;
        }
        int lineLimit = filterPicker
                ? Math.max(1, (layout.anyItem.y - y - 1) / lineStep)
                : Math.max(3, (layout.body.height - 6) / lineStep);
        List<String> wrapped = wrapHelpLines(content.lines, maxWidth, textScale, lineLimit);
        for (int index = 0; index < wrapped.size(); index++) {
            String line = wrapped.get(index);
            boldLine(graphics, line, lineX, y + index * lineStep, maxWidth,
                    withAlpha(helpLineColor(line, content.accent), alpha), textScale);
        }
        if (filterPicker) {
            drawFilterPicker(graphics, layout, mouseX, mouseY, alpha);
        }
    }

    private void drawExpeditionRewardPreview(GuiGraphicsExtractor graphics, HelpPanelLayout layout,
                                             int tier, int mouseX, int mouseY, int alpha) {
        ExpeditionRewards.Tier expedition = ExpeditionRewards.tier(tier);
        int padding = Math.max(3, Math.round(3.0F * plannerScale()));
        boolean availableToDinosaur = canAttemptExpedition(tier);
        boldLine(graphics,
                availableToDinosaur
                        ? expeditionDurationMinutes(tier) + " MIN  ·  " + expeditionRiskPercent(tier) + "% INJURY RISK"
                        : "UNAVAILABLE  ·  REQUIRES 2-STAR EXPEDITION",
                layout.body.x + padding, layout.body.y + padding, layout.body.width - padding * 2,
                withAlpha(!availableToDinosaur || expeditionRiskPercent(tier) >= 16
                        ? CORAL : expeditionColor(tier), alpha), 0.76F);
        boldLine(graphics, "POSSIBLE REWARDS  ·  " + expedition.rolls() + " ROLLS",
                layout.body.x + padding, layout.body.y + padding + 10, layout.body.width - padding * 2,
                withAlpha(MUTED, alpha), 0.70F);

        List<ExpeditionRewards.Reward> rewards = expedition.rewards();
        int gap = Math.max(1, Math.round(plannerScale()));
        int available = layout.body.width - padding * 2;
        int slotSize = Math.min(Math.max(14, Math.round(17.0F * plannerScale())),
                Math.max(10, (available - Math.max(0, rewards.size() - 1) * gap) / Math.max(1, rewards.size())));
        int rowWidth = rewards.size() * slotSize + Math.max(0, rewards.size() - 1) * gap;
        int x = layout.body.centerX() - rowWidth / 2;
        int y = layout.body.bottom() - slotSize - padding;
        for (int index = 0; index < rewards.size(); index++) {
            ExpeditionRewards.Reward reward = rewards.get(index);
            Rect slot = new Rect(x + index * (slotSize + gap), y, slotSize, slotSize);
            boolean hovered = slot.contains(mouseX, mouseY);
            drawSearchInventorySlot(graphics, slot, hovered, index, alpha);
            ItemStack preview = new ItemStack(reward.item().get(), reward.maximum());
            drawSearchItem(graphics, preview, slot);
            String count = reward.minimum() == reward.maximum()
                    ? Integer.toString(reward.maximum())
                    : reward.minimum() + "-" + reward.maximum();
            boldLine(graphics, count, slot.x + 2, slot.bottom() - 7, slot.width - 3,
                    withAlpha(CREAM, alpha), 0.55F);
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                graphics.setComponentTooltipForNextFrame(font(), List.of(
                        preview.getHoverName().copy().withStyle(style -> style.withBold(true)),
                        Component.literal("Possible stack: " + count),
                        Component.literal("One of " + expedition.rolls() + " reward rolls.")
                ), mouseX, mouseY);
            }
        }
    }

    private void drawHelpOptions(
            GuiGraphicsExtractor graphics,
            HelpPanelLayout layout,
            List<HelpOption> options,
            int mouseX,
            int mouseY,
            int alpha
    ) {
        float textScale = Mth.clamp(0.88F * plannerScale(), 0.82F, 1.0F);
        for (int index = 0; index < options.size(); index++) {
            HelpOption option = options.get(index);
            Rect row = helpOptionRect(layout, options.size(), index);
            boolean hovered = row.contains(mouseX, mouseY);
            if (hovered || option.selected) {
                graphics.fill(row.x, row.y, row.right(), row.bottom(),
                        withAlpha(option.color, hovered ? 38 : 18));
            }
            int colorAlpha = hovered ? alpha : Math.round(alpha * (option.selected ? 0.92F : 0.78F));
            int indicatorAlpha = hovered ? alpha : Math.round(alpha * (option.selected ? 0.82F : 0.52F));
            graphics.fill(row.x, row.y + 1, row.x + 1, row.bottom() - 1, withAlpha(option.color, indicatorAlpha));
            int lineStep = Math.max(6, Math.round(font().lineHeight * textScale));
            if (row.height >= lineStep * 2 + 2) {
                boldLine(graphics, option.label + ":", row.x + 4, row.y + 1,
                        row.width - 7, withAlpha(hovered ? option.color : INK, colorAlpha), textScale);
                boldLine(graphics, option.description, row.x + 4, row.y + 1 + lineStep,
                        row.width - 7, withAlpha(hovered ? option.color : MUTED, colorAlpha), textScale);
            } else {
                boldLine(graphics, option.label + ":  " + option.description, row.x + 4, row.y + 1,
                        row.width - 7, withAlpha(hovered ? option.color : INK, colorAlpha), textScale);
            }
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
    }

    private List<HelpOption> currentHelpOptions() {
        if (hoveredWorkflowSlot >= 0) {
            return List.of();
        }
        PlannerTool tool = hoveredTool != null ? hoveredTool : pinnedTool;
        if (tool == null) {
            return List.of();
        }
        return switch (tool) {
            case REPEAT -> List.of(
                    new HelpOption("Continue", "Keeps checking.", GREEN, repeatMode == 0),
                    new HelpOption("Until target", "Stops at target stock.", AMBER, repeatMode == 1),
                    new HelpOption("One time", "Stops after one trip.", CORAL, repeatMode == 2)
            );
            case SCHEDULE -> List.of(
                    new HelpOption("Always", "Works whenever ready.", GREEN, schedule == 0),
                    new HelpOption("Daylight", "Works during the day.", AMBER, schedule == 1),
                    new HelpOption("Night shift", nightShiftOptionCopy(), CORAL, schedule == 2)
            );
            case ROUTE -> List.of(
                    new HelpOption("Strict", "Uses your location order.", CORAL, routePolicy == 0),
                    new HelpOption("Preferred", "Tries your order, then adapts.", AMBER, routePolicy == 1),
                    new HelpOption("Nearest", "Uses the closest block.", GREEN, routePolicy == 2)
            );
            case PRIORITY -> List.of(
                    new HelpOption("Low", priorityTargetPos == null ? "Runs after ordinary work." : "Use after other selected blocks.", GREEN, activePriority() == 0),
                    new HelpOption("Normal", priorityTargetPos == null ? "Standard work order." : "Use in the normal routing order.", CYAN, activePriority() == 1),
                    new HelpOption("High", priorityTargetPos == null ? "Runs before normal work." : "Try before normal and low blocks.", AMBER, activePriority() == 2),
                    new HelpOption("Urgent", priorityTargetPos == null ? "Runs before all lower work." : "Always try this block first.", CORAL, activePriority() == 3)
            );
            case SAFETY -> List.of(
                    new HelpOption("Safe", "Avoids dangerous routes.", GREEN, avoidDanger),
                    new HelpOption("Direct", "Takes the shortest route.", CORAL, !avoidDanger)
            );
            case MATCH -> List.of(
                    new HelpOption("Exact", "Only the selected item matches.", CYAN, exactItemMatch),
                    new HelpOption("Flexible", "Equivalent tagged items may match.", AMBER, !exactItemMatch)
            );
            default -> List.of();
        };
    }

    private Rect helpOptionRect(HelpPanelLayout layout, int count, int index) {
        int padding = Math.max(3, Math.round(3.0F * plannerScale()));
        int availableHeight = layout.body.height - padding * 2;
        int rowHeight = Math.max(10, availableHeight / count);
        return new Rect(layout.body.x + padding, layout.body.y + padding + index * rowHeight,
                layout.body.width - padding * 2, rowHeight);
    }

    private int helpOptionAt(HelpPanelLayout layout, double mouseX, double mouseY) {
        List<HelpOption> options = currentHelpOptions();
        for (int index = 0; index < options.size(); index++) {
            if (helpOptionRect(layout, options.size(), index).contains(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private void applyHelpOption(PlannerTool tool, int option) {
        switch (tool) {
            case REPEAT -> repeatMode = Mth.clamp(option, 0, 2);
            case SCHEDULE -> schedule = Mth.clamp(option, 0, 2);
            case ROUTE -> routePolicy = Mth.clamp(option, 0, 2);
            case PRIORITY -> {
                int selectedPriority = Mth.clamp(option, 0, 3);
                if (priorityTargetPos == null) {
                    priority = selectedPriority;
                } else if (isSelectedWorkBlock(priorityTargetPos)) {
                    blockPriorities.put(priorityTargetPos.immutable(), selectedPriority);
                } else {
                    priorityTargetPos = null;
                    priority = selectedPriority;
                }
            }
            case SAFETY -> avoidDanger = option == 0;
            case MATCH -> exactItemMatch = option == 0;
            default -> {
            }
        }
        pinnedTool = tool;
        if (tool == PlannerTool.MATCH) {
            refreshSuitableBlocks();
        }
        feedback(tool == PlannerTool.SCHEDULE && schedule == 2
                ? nightShiftWarning()
                : tool == PlannerTool.PRIORITY && priorityTargetPos != null
                ? blockName(priorityTargetPos) + " is now " + PRIORITIES[blockPriority(priorityTargetPos)].toLowerCase() + " priority."
                : "Work rule updated.");
    }

    private List<String> wrapHelpLines(List<String> paragraphs, int maximumWidth, float scale, int maximumLines) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String current = "";
            for (String word : paragraph.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                Component styled = boldComponent(candidate);
                if (!current.isEmpty() && font().width(styled) * scale > maximumWidth) {
                    lines.add(current);
                    current = word;
                    if (lines.size() >= maximumLines) {
                        return lines;
                    }
                } else {
                    current = candidate;
                }
            }
            if (!current.isEmpty()) {
                lines.add(current);
                if (lines.size() >= maximumLines) {
                    return lines;
                }
            }
        }
        return lines;
    }

    private void drawFilterPicker(GuiGraphicsExtractor graphics, HelpPanelLayout layout, int mouseX, int mouseY, int alpha) {
        boolean anySelected = itemFilters.isEmpty();
        boolean anyHovered = layout.anyItem.contains(mouseX, mouseY);
        if (anySelected || anyHovered) {
            graphics.fill(layout.anyItem.x, layout.anyItem.y, layout.anyItem.right(), layout.anyItem.bottom(),
                    withAlpha(anySelected ? GREEN : AMBER, anySelected ? 62 : 38));
            outline(graphics, layout.anyItem, withAlpha(anySelected ? GREEN : AMBER, alpha));
        }
        boldCenteredFit(graphics, "ANY", layout.anyItem, withAlpha(anySelected ? GREEN : INK, alpha));
        boolean targetHovered = layout.filterTarget.contains(mouseX, mouseY);
        drawSearchInventorySlot(graphics, layout.filterTarget, targetHovered, 0, alpha);
        ItemStack selected = selectedFilterStack();
        if (!selected.isEmpty()) {
            drawSearchItem(graphics, selected, layout.filterTarget);
        } else {
            boldCenteredFit(graphics, "+", layout.filterTarget, withAlpha(CYAN, alpha));
        }
        if (targetHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            graphics.setComponentTooltipForNextFrame(font(), List.of(
                    Component.literal(filterSlotName()).withStyle(style -> style.withBold(true)),
                    Component.literal(selected.isEmpty()
                            ? "Open the compass and drag an item here."
                            : "Drag another base item here to replace " + selected.getHoverName().getString() + ".")
            ), mouseX, mouseY);
        }
    }

    private void drawFireFilterPicker(GuiGraphicsExtractor graphics, HelpPanelLayout layout, int mouseX, int mouseY, int alpha) {
        int labelScale = Math.max(6, Math.round(font().lineHeight * 0.74F));
        boldLine(graphics, "FUEL  ·  UP TO FOUR", layout.body.x + 3, layout.body.y + 2,
                layout.body.width - 6, withAlpha(AMBER, alpha), 0.72F);
        List<Rect> fuels = fireFuelSlots(layout);
        for (int index = 0; index < fuels.size(); index++) {
            drawRuleFilterSlot(graphics, fuels.get(index), filterStack(fuelFilters, index), mouseX, mouseY, alpha,
                    "Fuel " + (index + 1), "Only furnace fuel belongs here.", AMBER);
        }
        int inputLabelY = fuels.getFirst().bottom() + Math.max(2, labelScale / 3);
        boldLine(graphics, "INPUTS  ·  UP TO THREE", layout.body.x + 3, inputLabelY,
                layout.body.width - 6, withAlpha(CORAL, alpha), 0.72F);
        List<Rect> inputs = fireInputSlots(layout);
        for (int index = 0; index < inputs.size(); index++) {
            drawRuleFilterSlot(graphics, inputs.get(index), filterStack(itemFilters, index), mouseX, mouseY, alpha,
                    "Smelting input " + (index + 1), "Only items with a furnace recipe belong here.", CORAL);
        }
    }

    private void drawRuleFilterSlot(GuiGraphicsExtractor graphics, Rect slot, ItemStack stack, int mouseX, int mouseY,
                                    int alpha, String title, String emptyDescription, int accent) {
        boolean hovered = slot.contains(mouseX, mouseY);
        drawSearchInventorySlot(graphics, slot, hovered, 0, alpha);
        if (stack.isEmpty()) {
            boldCenteredFit(graphics, "+", slot, withAlpha(accent, alpha));
        } else {
            drawSearchItem(graphics, stack, slot);
        }
        if (hovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            graphics.setComponentTooltipForNextFrame(font(), List.of(
                    Component.literal(title).withStyle(style -> style.withBold(true)),
                    Component.literal(stack.isEmpty() ? emptyDescription : stack.getHoverName().getString()),
                    Component.literal(stack.isEmpty() ? "Drag an item from search into this slot." : "Click to clear this slot.")
            ), mouseX, mouseY);
        }
    }

    private List<Rect> fireFuelSlots(HelpPanelLayout layout) {
        int size = Math.max(14, Math.round(16.0F * plannerScale()));
        int gap = Math.max(2, Math.round(2.0F * plannerScale()));
        int y = layout.body.y + Math.max(10, Math.round(11.0F * plannerScale()));
        int x = layout.body.x + 3;
        List<Rect> slots = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) slots.add(new Rect(x + index * (size + gap), y, size, size));
        return slots;
    }

    private List<Rect> fireInputSlots(HelpPanelLayout layout) {
        int size = Math.max(14, Math.round(16.0F * plannerScale()));
        int gap = Math.max(2, Math.round(2.0F * plannerScale()));
        int y = layout.body.bottom() - size - 2;
        int x = layout.body.x + 3;
        List<Rect> slots = new ArrayList<>(3);
        for (int index = 0; index < 3; index++) slots.add(new Rect(x + index * (size + gap), y, size, size));
        return slots;
    }

    private static int slotAt(List<Rect> slots, double x, double y) {
        for (int index = 0; index < slots.size(); index++) if (slots.get(index).contains(x, y)) return index;
        return -1;
    }

    private static ItemStack filterStack(List<String> filters, int index) {
        if (index < 0 || index >= filters.size()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(filters.get(index));
        var item = id == null ? null : BuiltInRegistries.ITEM.get(id).orElse(null);
        return item == null ? ItemStack.EMPTY : item.value().getDefaultInstance();
    }

    private static void setFilterAt(List<String> filters, int index, ItemStack stack) {
        String identifier = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        filters.remove(identifier);
        while (filters.size() < index) filters.add("");
        if (index < filters.size()) filters.set(index, identifier); else filters.add(identifier);
        filters.removeIf(String::isBlank);
    }

    private void removeFilterAt(List<String> filters, int index, String kind) {
        if (index >= filters.size()) {
            feedback("Drag an allowed " + kind + " into this slot.");
            return;
        }
        String removed = filters.remove(index);
        feedback(shortIdentifier(removed) + " removed from allowed " + kind + "s.");
    }

    private void activateTool(PlannerTool tool) {
        pressedTool = tool;
        pressedToolNanos = Util.getNanos();
        if (tool == PlannerTool.CANCEL) {
            minecraft.setScreen(parent);
            return;
        }
        if (tool == PlannerTool.SAVE) {
            applyAssignment();
            return;
        }
        if (tool == PlannerTool.FILTER) {
            openSearchPicker();
            feedback(itemFilters.isEmpty()
                    ? "Choose an item type, then drag it to the highlighted slot."
                    : "Choose another item type to replace the current filter.");
            return;
        }
        switch (tool) {
            case PRIORITY -> {
                priorityTargetPos = null;
                feedback("Block priorities: click any selected source, machine, destination or overflow block.");
            }
            case BATCH -> cycleRule(1);
            case RESERVE -> cycleRule(2);
            case TARGET -> cycleRule(3);
            case SCHEDULE -> cycleRule(4);
            case REPEAT -> cycleRule(5);
            case ROUTE -> cycleRule(6);
            case SAFETY -> cycleRule(7);
            case MATCH -> {
                exactItemMatch = !exactItemMatch;
                refreshSuitableBlocks();
                feedback(exactItemMatch ? "Exact items only." : "Equivalent tagged items are allowed.");
            }
            default -> {
            }
        }
        pinnedTool = tool;
        helpOpenedNanos = Util.getNanos();
    }

    private HelpContent currentHelpContent() {
        Selection[] available = selections();
        if (hoveredWorkflowSlot >= 0 && hoveredWorkflowSlot < available.length) {
            return selectionHelp(available[hoveredWorkflowSlot]);
        }
        PlannerTool tool = hoveredTool != null ? hoveredTool : pinnedTool;
        return tool == null ? null : toolHelp(tool);
    }

    private HelpContent selectionHelp(Selection option) {
        if (isExpeditionSelection(option)) {
            int tier = expeditionIndex(option);
            if (!canAttemptExpedition(tier)) {
                return new HelpContent(expeditionName(tier) + "  ·  UNAVAILABLE", List.of(
                        "THIS ROUTE REQUIRES AT LEAST TWO EXPEDITION STARS.",
                        "RAISE A SPECIES WITH A STRONGER EXPEDITION SPECIALTY.",
                        "NO DINOSAUR IS SENT AND NO TIMER STARTS WHILE LOCKED."
                ), CORAL);
            }
            return new HelpContent(expeditionName(tier) + "  ·  " + expeditionDurationMinutes(tier) + " MIN", List.of(
                    "SENDS THIS DINOSAUR AWAY UNTIL THE EXPEDITION TIMER ENDS.",
                    "POSSIBLE: " + ExpeditionRewards.shortPoolDescription(tier).toUpperCase() + ".",
                    "RISK: " + expeditionRiskPercent(tier) + "% CHANCE TO RETURN TEMPORARILY INCAPACITATED.",
                    "A TRANSPORT DINOSAUR COLLECTS THE RETURNED SUPPLIES.",
                    expeditionTier == tier ? "CURRENT EXPEDITION TIER." : "CLICK THIS SLOT TO SELECT THE TIER."
            ), expeditionColor(tier));
        }
        List<BlockPos> selected = positions(option);
        String count = supportsMultiple(option) ? selected.size() + "/8" : selected.isEmpty() ? "NOT SET" : "SET";
        boolean routingAvailable = hoveredPos != null && minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(hoveredPos) instanceof Container;
        return new HelpContent(
                selectionLabel(option) + "  ·  " + count,
                List.of(
                        selectionPurpose(option),
                        "LEFT-CLICK A HIGHLIGHTED BLOCK TO ADD OR REMOVE IT.",
                        supportsMultiple(option) ? "YOU CAN CHOOSE UP TO EIGHT LOCATIONS." : "THIS STEP USES ONE WORLD POSITION.",
                        selected.isEmpty() ? selectionInstruction(option) : "CURRENT: " + (selected.size() == 1 ? compactPos(selected.getFirst()) : selected.size() + " BLOCKS"),
                        routingAvailable ? "RIGHT-CLICK THIS CONTAINER TO INSPECT ITS IN / OUT SLOTS."
                                : suitableBlockPositions.size() + " SUITABLE BLOCKS ARE VISIBLE IN THIS BASE."
                ),
                option.color
        );
    }

    private HelpContent toolHelp(PlannerTool tool) {
        return switch (tool) {
            case FILTER -> new HelpContent(filterTitle(), List.of(
                    filterPurpose(),
                    "CLICK THIS TOOL TO PIN THE ITEM PICKER.",
                    jobIndex == 1 ? "FUEL AND SMELTING INPUTS HAVE SEPARATE SLOTS." : "NO SELECTED ITEMS MEANS ANY VALID ITEM.",
                    jobIndex == 3 ? "THE DINO FINDS RECIPE INGREDIENTS ACROSS THE BASE." : "THE SEARCH INDEX COMES FROM BASE STORAGE."
            ), AMBER);
            case BATCH -> new HelpContent((jobIndex == 3 ? "CRAFT BATCH  ·  " : "TRIP SIZE  ·  ") + batchSize, List.of(
                    jobIndex == 3 ? "MAXIMUM RECIPE CRAFTS COMPLETED IN ONE WORK CYCLE." : "MAXIMUM ITEMS THIS DINO TAKES PER TRIP.",
                    jobIndex == 3 ? "THE DINO FETCHES ONLY INGREDIENTS NEEDED FOR THIS BATCH." : "SMALL LOADS RESPOND FASTER; LARGE LOADS MOVE MORE.",
                    "CLICK TO CYCLE 1, 8, 16, 32 OR 64."
            ), batchSize >= 32 ? AMBER : GREEN);
            case RESERVE -> new HelpContent("SOURCE RESERVE  ·  " + stockName(sourceReserve), List.of(
                    "THE DINO LEAVES THIS MANY ITEMS IN EACH SOURCE.",
                    "USE IT TO PROTECT FUEL, FOOD OR CRAFTING STOCK.",
                    "CLICK TO CYCLE THE RESERVE AMOUNT."
            ), sourceReserve == 0 ? CORAL : AMBER);
            case TARGET -> new HelpContent("TARGET STOCK  ·  " + stockName(destinationTarget), List.of(
                    "WORK STOPS WHEN EACH DESTINATION REACHES THIS AMOUNT.",
                    "ANY MEANS THERE IS NO STOCK LIMIT.",
                    "CLICK TO CYCLE THE TARGET."
            ), destinationTarget == 0 ? AMBER : GREEN);
            case PRIORITY -> priorityTargetPos == null
                    ? new HelpContent("ORDER PRIORITY  ·  " + PRIORITIES[priority], List.of(
                    "SETS THIS DINOSAUR'S OVERALL WORK-ORDER PRIORITY.",
                    "CLICK A SELECTED BLOCK TO GIVE THAT LOCATION ITS OWN PRIORITY.",
                    "URGENT LOCATIONS ARE TRIED BEFORE HIGH, NORMAL AND LOW."
            ), priorityColor(priority))
                    : new HelpContent("BLOCK PRIORITY  ·  " + PRIORITIES[blockPriority(priorityTargetPos)], List.of(
                    blockName(priorityTargetPos).toUpperCase() + "  " + compactPos(priorityTargetPos),
                    "THIS LOCATION IS TRIED BEFORE LOWER-PRIORITY LOCATIONS OF THE SAME ROLE.",
                    "CHOOSE LOW, NORMAL, HIGH OR URGENT BELOW."
            ), priorityColor(blockPriority(priorityTargetPos)));
            case SCHEDULE -> schedule == 2
                    ? new HelpContent("SCHEDULE  ·  " + SCHEDULES[schedule] + "  !", List.of(
                    nightShiftDutyCopy().toUpperCase(),
                    nightShiftDetailCopy().toUpperCase(),
                    nightShiftAlternativeCopy().toUpperCase()
            ), CORAL)
                    : new HelpContent("SCHEDULE  ·  " + SCHEDULES[schedule], List.of(
                    "CONTROLS WHEN THIS ORDER IS ALLOWED TO RUN.",
                    "DINOS STILL REST WHEN THEIR NEEDS REQUIRE IT.",
                    "CLICK TO CYCLE THE SCHEDULE."
            ), scheduleColor(schedule));
            case REPEAT -> new HelpContent("REPEAT  ·  " + REPEATS[repeatMode], List.of(
                    "CONTINUOUS KEEPS CHECKING; ONE RUN STOPS AFTER A TRIP.",
                    "UNTIL TARGET PAUSES AFTER THE STOCK GOAL IS MET.",
                    "CLICK TO CYCLE THE MODE."
            ), repeatColor(repeatMode));
            case ROUTE -> new HelpContent("ROUTE  ·  " + ROUTES[routePolicy], List.of(
                    "STRICT USES YOUR ORDER; PREFERRED TRIES IT FIRST.",
                    "NEAREST VALID CHOOSES THE SHORTEST WORKING ROUTE.",
                    "CLICK TO CYCLE THE ROUTE POLICY."
            ), routeColor(routePolicy));
            case SAFETY -> new HelpContent(avoidDanger ? "PATHING  ·  SAFE" : "PATHING  ·  DIRECT", List.of(
                    avoidDanger ? "THE DINO AVOIDS DANGER EVEN IF THE ROUTE IS LONGER." : "THE DINO TAKES THE FASTEST ROUTE IT CAN FIND.",
                    "SAFE PATHING IS BETTER FOR UNDEFENDED WORKERS.",
                    "CLICK TO SWITCH THE PATHING RULE."
            ), avoidDanger ? GREEN : CORAL);
            case MATCH -> new HelpContent(exactItemMatch ? "ITEM MATCH  ·  EXACT" : "ITEM MATCH  ·  FLEXIBLE", List.of(
                    exactItemMatch ? "ONLY THE EXACT SELECTED ITEM MAY BE USED." : "EQUIVALENT ITEMS IN THE SAME TAG MAY BE USED.",
                    "FLEXIBLE MATCHING KEEPS AUTOMATION RUNNING LONGER.",
                    "CLICK TO SWITCH MATCHING."
            ), exactItemMatch ? CYAN : AMBER);
            case SAVE -> new HelpContent("SAVE WORK ORDER", List.of(
                    "CHECKS THE REQUIRED LOCATIONS AND ASSIGNS THIS ORDER.",
                    "THE DINO STARTS WHEN ITS SCHEDULE AND NEEDS ALLOW.",
                    missingRequiredSelection() == null ? "READY TO SAVE." : "STILL NEEDED: " + missingRequiredSelection().toUpperCase()
            ), GREEN);
            case CANCEL -> new HelpContent("LEAVE WITHOUT SAVING", List.of(
                    "CLOSES THE PLANNER AND DISCARDS THESE UNSAVED CHANGES.",
                    "THE DINO KEEPS ITS PREVIOUS WORK ORDER."
            ), CORAL);
        };
    }

    private String filterTitle() {
        return switch (jobIndex) {
            case 1 -> "FURNACE INPUTS + FUEL";
            case 3 -> "RECIPE TO CRAFT";
            case 4 -> "EXPEDITION REWARD";
            default -> "ALLOWED CARGO";
        };
    }

    private String filterPurpose() {
        return switch (jobIndex) {
            case 1 -> "CHOOSE UP TO THREE SMELTABLE INPUTS AND FOUR FUELS SEPARATELY.";
            case 3 -> "CHOOSE THE FINISHED ITEM; THE DINO RESOLVES ITS RECIPE AND INGREDIENTS.";
            case 4 -> "CHOOSE AN ITEM ALREADY STORED IN THE BASE TO SEEK ON EXPEDITION.";
            default -> "CHOOSE WHICH ITEMS THIS DINO MAY TRANSPORT.";
        };
    }

    private static String stockName(int value) {
        return value == 0 ? "ANY" : Integer.toString(value);
    }

    private static int jobColor(int index) {
        return switch (index) {
            case 0 -> AMBER;
            case 1 -> CORAL;
            case 2 -> 0xFFF0C448;
            case 3 -> CYAN;
            case 4 -> GREEN;
            default -> INK;
        };
    }

    private static int priorityColor(int value) {
        return switch (value) {
            case 0 -> GREEN;
            case 1 -> CYAN;
            case 2 -> AMBER;
            default -> CORAL;
        };
    }

    private static int scheduleColor(int value) {
        return switch (value) {
            case 0 -> GREEN;
            case 1 -> AMBER;
            default -> CORAL;
        };
    }

    private static int repeatColor(int value) {
        return switch (value) {
            case 0 -> GREEN;
            case 1 -> AMBER;
            default -> CORAL;
        };
    }

    private static int routeColor(int value) {
        return switch (value) {
            case 0 -> CORAL;
            case 1 -> AMBER;
            default -> GREEN;
        };
    }

    private static int helpLineColor(String value, int accent) {
        String line = value.toUpperCase();
        if (line.contains("STILL NEEDED") || line.contains("DISCARD") || line.contains("DANGER")) {
            return CORAL;
        }
        if (line.contains("READY") || line.contains("CURRENT") || line.contains("SUITABLE") || line.contains("STARTS WHEN")) {
            return GREEN;
        }
        if (line.contains("CLICK") || line.contains("DRAG") || line.contains("SELECT")) {
            return CYAN;
        }
        if (line.contains("CYCLE") || line.contains("UP TO") || line.contains("ANY MEANS")) {
            return AMBER;
        }
        return accent == CORAL ? INK : accent;
    }

    private String ruleHelp(int index) {
        return switch (index) {
            case 0 -> "PRIORITY: WHICH ORDER THIS DINO CHOOSES FIRST";
            case 1 -> "BATCH: MAXIMUM ITEMS CARRIED PER TRIP";
            case 2 -> "KEEP: ITEMS NEVER REMOVED FROM A SOURCE";
            case 3 -> "FILL: STOP WHEN EACH TARGET REACHES THIS STOCK";
            case 4 -> "TIME: WHEN THIS ORDER IS ALLOWED TO RUN";
            case 5 -> "RUN: CONTINUOUS, UNTIL STOCKED, OR ONE TRIP";
            case 6 -> "ROUTE: FIXED ORDER, PREFERRED ORDER, OR NEAREST VALID";
            default -> "PATH: AVOID DANGER OR TAKE THE FASTEST ROUTE";
        };
    }

    private String nightShiftOptionCopy() {
        return Component.translatable("ui.primevalworks.night_shift.option").getString();
    }

    private String nightShiftDetailCopy() {
        return Component.translatable("ui.primevalworks.night_shift.detail").getString();
    }

    private String nightShiftDutyCopy() {
        return Component.translatable("ui.primevalworks.night_shift.duty").getString();
    }

    private String nightShiftAlternativeCopy() {
        return Component.translatable("ui.primevalworks.night_shift.alternative").getString();
    }

    private String nightShiftWarning() {
        return Component.translatable("ui.primevalworks.night_shift.warning").getString();
    }

    private SpecialtyDockLayout specialtyDockLayout(float reveal) {
        float scale = plannerScale();
        int canvasWidth = Math.round(PLANNER_TEXTURE_WIDTH * scale);
        int canvasHeight = Math.round(PLANNER_TEXTURE_HEIGHT * scale);
        int baseX = width - canvasWidth;
        int baseY = height - canvasHeight;
        int rightOffset = Math.round(specialtyRightOffset(reveal));
        int bottomOffset = Math.round(specialtyBottomOffset(reveal));
        Rect rightCanvas = new Rect(baseX + rightOffset, baseY, canvasWidth, canvasHeight);
        Rect bottomCanvas = new Rect(baseX, baseY + bottomOffset, canvasWidth, canvasHeight);
        int rightRailX = rightCanvas.x + Math.round(DOCK_RIGHT_X * scale);
        int rightRailY = rightCanvas.y + Math.round(DOCK_RIGHT_Y * scale);
        Rect rightRail = new Rect(
                rightRailX,
                rightRailY,
                rightCanvas.right() - rightRailX,
                rightCanvas.bottom() - rightRailY
        );
        int bottomRailX = bottomCanvas.x + Math.round(DOCK_BOTTOM_X * scale);
        int bottomRailY = bottomCanvas.y + Math.round(DOCK_BOTTOM_Y * scale);
        Rect bottomRail = new Rect(
                bottomRailX,
                bottomRailY,
                bottomCanvas.right() - bottomRailX,
                bottomCanvas.bottom() - bottomRailY
        );
        PlannerTool[] tools = toolsForJob();
        Rect[] slots = new Rect[tools.length];
        int verticalCount = Math.min(6, tools.length);
        for (int index = 0; index < verticalCount; index++) {
            slots[index] = scaledRect(baseX + rightOffset, baseY, scale,
                    409, 85 + index * 21, 18, 18);
        }
        int horizontalCount = tools.length - verticalCount;
        for (int index = 0; index < horizontalCount; index++) {
            slots[verticalCount + index] = scaledRect(baseX, baseY + bottomOffset, scale,
                    273 + index * 21, 221, 18, 18);
        }
        return new SpecialtyDockLayout(
                rightCanvas,
                bottomCanvas,
                rightRail,
                bottomRail,
                tools,
                slots,
                rightOffset,
                bottomOffset
        );
    }

    private HelpPanelLayout helpPanelLayout() {
        float scale = plannerScale();
        int outerWidth = Math.round(HELP_WIDTH * scale);
        int outerHeight = Math.round(HELP_HEIGHT * scale);
        int centeredX = (width - outerWidth) / 2;
        int centeredY = Math.max(Math.round(32.0F * scale), (height - outerHeight) / 2);
        int plannerX = (width - Math.round(PLANNER_TEXTURE_WIDTH * scale)) / 2;
        int rightRailX = plannerX + Math.round(DOCK_RIGHT_X * scale);
        int searchX = Math.max(4, rightRailX - outerWidth - Math.round(6.0F * scale));
        int searchY = Math.max(4, Math.round((PLANNER_VISIBLE_HEIGHT + 5.0F) * scale));
        float move = searchReveal * searchReveal * (3.0F - 2.0F * searchReveal);
        int outerX = Math.round(Mth.lerp(move, centeredX, searchX));
        int outerY = Math.round(Mth.lerp(move, centeredY, searchY));
        Rect outer = new Rect(outerX, outerY, outerWidth, outerHeight);
        int canvasX = outerX - Math.round(HELP_X * scale);
        int canvasY = outerY - Math.round(HELP_Y * scale);
        Rect canvas = new Rect(
                canvasX,
                canvasY,
                Math.round(PLANNER_TEXTURE_WIDTH * scale),
                Math.round(PLANNER_TEXTURE_HEIGHT * scale)
        );
        Rect title = scaledRect(canvasX, canvasY, scale, 158, 100, 128, 12);
        Rect body = scaledRect(canvasX, canvasY, scale, 158, 122, 128, 61);
        int padding = Math.max(3, Math.round(3.0F * scale));
        int slotSize = Math.max(18, Math.round(20.0F * scale));
        int pickerY = body.bottom() - slotSize - padding;
        Rect filterTarget = new Rect(body.x + padding, pickerY, slotSize, slotSize);
        int anyWidth = Math.max(30, Math.round(34.0F * scale));
        Rect anyItem = new Rect(filterTarget.right() + padding, pickerY, anyWidth, slotSize);
        return new HelpPanelLayout(canvas, outer, title, body, filterTarget, anyItem);
    }

    private SearchLayout searchLayout(float reveal) {
        float scale = plannerScale();
        LeftDrawerLayout left = leftDrawerLayout(leftDrawerReveal);
        Rect canvas = left.canvas;
        Rect slotFrame = scaledRect(canvas.x, 0, scale,
                SEARCH_SLOT_X, SEARCH_SLOT_Y, SEARCH_SLOT_WIDTH, SEARCH_SLOT_HEIGHT);
        Rect button = scaledRect(canvas.x, 0, scale, 3, 219, 18, 18);
        int naturalWidth = Math.round(SEARCH_BAR_WIDTH * scale);
        int maximumWidth = Math.round(SEARCH_BAR_WIDTH * 2.3F * scale);
        int requestedWidth = font().width(searchPrompt()) + Math.round(28.0F * scale);
        int barWidth = Mth.clamp(Math.max(naturalWidth, requestedWidth), naturalWidth, maximumWidth);
        Rect bar = new Rect(
                canvas.x + Math.round(SEARCH_BAR_X * scale),
                Math.round(SEARCH_BAR_Y * scale),
                barWidth,
                Math.max(10, Math.round(SEARCH_BAR_HEIGHT * scale))
        );
        int slotSize = Math.max(18, Math.round(20.0F * scale));
        int padding = Math.max(6, Math.round(7.0F * scale));
        int headerHeight = Math.max(15, Math.round(16.0F * scale));
        int footerHeight = Math.max(12, Math.round(13.0F * scale));
        int resultsWidth = padding * 2 + slotSize * 9;
        int resultsHeight = padding * 2 + headerHeight + slotSize * 4 + footerHeight;
        int resultsBottom = bar.y - Math.max(5, Math.round(5.0F * scale));
        Rect results = new Rect(
                Math.max(3, slotFrame.x),
                Math.max(28, resultsBottom - resultsHeight),
                Math.min(resultsWidth, width - Math.max(3, slotFrame.x) - 4),
                resultsHeight
        );
        Rect[] itemSlots = new Rect[36];
        int gridX = results.x + padding;
        int gridY = results.y + padding + headerHeight;
        for (int index = 0; index < itemSlots.length; index++) {
            itemSlots[index] = new Rect(
                    gridX + index % 9 * slotSize,
                    gridY + index / 9 * slotSize,
                    slotSize,
                    slotSize
            );
        }
        return new SearchLayout(canvas, slotFrame, button, bar, results, itemSlots);
    }

    private void updateSearchBox(SearchLayout layout) {
        if (searchBox == null) {
            return;
        }
        int inset = Math.max(5, Math.round(5.0F * plannerScale()));
        int boxHeight = Math.max(10, font().lineHeight + 2);
        searchBox.setX(layout.bar.x + inset);
        searchBox.setY(layout.bar.y + (layout.bar.height - boxHeight) / 2);
        searchBox.setWidth(Math.max(20, layout.bar.width - inset * 2));
        searchBox.setHeight(boxHeight);
        searchBox.setAlpha(Mth.clamp(searchReveal, 0.0F, 1.0F));
        searchBox.setVisible(searchOpen && !usesPlayerInventoryPicker() && searchReveal > 0.56F);
    }

    private float plannerFocusAlpha() {
        return 1.0F - searchReveal * 0.82F;
    }

    private String searchPrompt() {
        return switch (jobIndex) {
            case 0 -> "Search cargo types";
            case 1 -> "Choose fuel or inputs from inventory";
            case 2 -> "Energy uses selected turbine blocks";
            case 3 -> "Search craftable recipe outputs";
            case 4 -> "Expedition rewards are fixed by route";
            default -> "Search base items";
        };
    }

    private String searchScopeDescription() {
        return switch (jobIndex) {
            case 0 -> "Browse every transportable item type; stored counts come from all base chests.";
            case 1 -> "Drag up to three inputs and four fuels from your inventory. They are templates and are not consumed.";
            case 2 -> "Energy has no item search. Select a placed Primeval wind or water turbine.";
            case 3 -> "Choose the recipe result. The crafter fetches its ingredients from base storage.";
            case 4 -> "Each route has a fixed, balanced reward pool shown in its details.";
            default -> "Search this base.";
        };
    }

    private String searchResultLabel(int count) {
        String suffix = count == 1 ? "" : "S";
        return switch (jobIndex) {
            case 0 -> "CARGO TYPE" + suffix;
            case 2 -> "ENERGY SOURCE" + suffix;
            case 3 -> "RECIPE RESULT" + suffix;
            case 4 -> "ELIGIBLE REWARD" + suffix;
            default -> "BASE ITEM" + suffix;
        };
    }

    private boolean supportsItemFilter() {
        return jobIndex == 0 || jobIndex == 1 || jobIndex == 3;
    }

    private String filterSlotName() {
        return switch (jobIndex) {
            case 0 -> "Cargo";
            case 1 -> "Input / fuel";
            case 2 -> "Turbine";
            case 3 -> "Recipe output";
            case 4 -> "Reward pool";
            default -> "Assigned item";
        };
    }

    private List<BaseItemEntry> filteredBaseItems() {
        if (usesPlayerInventoryPicker()) {
            return pickerItems;
        }
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
        if (query.isEmpty()) {
            return pickerItems;
        }
        return pickerItems.stream()
                .filter(entry -> searchMatches(entry, query))
                .toList();
    }

    private boolean searchMatches(BaseItemEntry entry, String query) {
        String name = entry.stack.getHoverName().getString().toLowerCase();
        String identifier = BuiltInRegistries.ITEM.getKey(entry.stack.getItem()).toString().toLowerCase();
        String normalized = query.replace('_', ' ').trim();
        if (name.contains(normalized) || identifier.contains(normalized.replace(' ', '_'))) {
            return true;
        }
        String stem = normalized.endsWith("y") && normalized.length() > 2
                ? normalized.substring(0, normalized.length() - 1)
                : normalized.endsWith("ies") && normalized.length() > 4
                        ? normalized.substring(0, normalized.length() - 3)
                        : normalized.endsWith("s") && normalized.length() > 3
                                ? normalized.substring(0, normalized.length() - 1)
                                : normalized;
        return stem.length() >= 3 && (name.contains(stem) || identifier.contains(stem.replace(' ', '_')));
    }

    private BaseItemEntry searchEntryAt(SearchLayout layout, double mouseX, double mouseY) {
        List<BaseItemEntry> filtered = filteredBaseItems();
        int startIndex = usesPlayerInventoryPicker() ? 0 : searchScrollRow * 9;
        for (int slotIndex = 0; slotIndex < layout.itemSlots.length; slotIndex++) {
            int entryIndex = startIndex + slotIndex;
            if (entryIndex < filtered.size() && layout.itemSlots[slotIndex].contains(mouseX, mouseY)) {
                BaseItemEntry entry = filtered.get(entryIndex);
                if (entry.stack.isEmpty()) {
                    return null;
                }
                return entry;
            }
        }
        return null;
    }

    private ItemStack selectedFilterStack() {
        if (itemFilters.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String selected = itemFilters.getFirst();
        ItemStack visible = pickerItems.stream()
                .filter(entry -> BuiltInRegistries.ITEM.getKey(entry.stack.getItem()).toString().equals(selected))
                .map(entry -> entry.stack)
                .findFirst()
                .orElse(ItemStack.EMPTY);
        if (!visible.isEmpty()) {
            return visible;
        }
        Identifier identifier = Identifier.tryParse(selected);
        var holder = identifier == null ? null : BuiltInRegistries.ITEM.get(identifier).orElse(null);
        return holder == null ? ItemStack.EMPTY : holder.value().getDefaultInstance();
    }

    private void refreshBaseInventoryIndex() {
        if (minecraft == null || minecraft.player == null) return;
        ClientPacketDistributor.sendToServer(new RequestBaseInventoryPayload(tablePos));
    }

    private void installBaseInventory(List<BaseInventoryPayload.ContainerEntry> snapshots) {
        Map<Identifier, BaseItemAccumulator> items = new LinkedHashMap<>();
        baseContainers = snapshots == null ? List.of() : List.copyOf(snapshots);
        baseContainerPositions.clear();
        for (BaseInventoryPayload.ContainerEntry container : baseContainers) {
            BlockPos pos = container.pos();
            baseContainerPositions.add(pos);
            for (BaseInventoryPayload.ItemEntry item : container.items()) {
                if (item.extractableCount() <= 0) continue;
                Identifier identifier = Identifier.tryParse(item.identifier());
                var holder = identifier == null ? null : BuiltInRegistries.ITEM.get(identifier).orElse(null);
                if (holder == null) continue;
                BaseItemAccumulator accumulator = items.computeIfAbsent(identifier,
                        ignored -> new BaseItemAccumulator(holder.value().getDefaultInstance()));
                accumulator.totalCount += item.extractableCount();
                if (!accumulator.locations.contains(pos)) {
                    accumulator.locations.add(pos);
                }
            }
        }
        baseItems = items.values().stream()
                .map(accumulator -> new BaseItemEntry(accumulator.stack, accumulator.totalCount, List.copyOf(accumulator.locations)))
                .sorted(Comparator.comparing(entry -> entry.stack.getHoverName().getString()))
                .toList();
        refreshPickerItems();
        refreshFilterItems();
        refreshSuitableBlocks();
    }

    private void refreshPickerItems() {
        if (jobIndex == 1) {
            pickerItems = playerInventoryEntries();
            return;
        }
        if (jobIndex == 4) {
            pickerItems = List.of();
            return;
        }
        if (jobIndex == 2) {
            pickerItems = List.of();
            return;
        }
        if (jobIndex == 3) {
            pickerItems = craftingRecipeOutputs();
            return;
        }
        Map<Identifier, BaseItemEntry> stored = new LinkedHashMap<>();
        for (BaseItemEntry entry : baseItems) {
            stored.put(BuiltInRegistries.ITEM.getKey(entry.stack.getItem()), entry);
        }
        List<BaseItemEntry> catalogue = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (stack.isEmpty() || item == Items.AIR) {
                continue;
            }
            Identifier identifier = BuiltInRegistries.ITEM.getKey(item);
            catalogue.add(stored.getOrDefault(identifier,
                    new BaseItemEntry(stack.copyWithCount(1), 0, List.of())));
        }
        pickerItems = catalogue.stream()
                .sorted(Comparator.comparing(entry -> entry.stack.getHoverName().getString()))
                .toList();
    }

    private boolean isFirePickerItem(ItemStack stack) {
        return !stack.isEmpty() && minecraft.level != null && (minecraft.level.fuelValues().isFuel(stack)
                || minecraft.level.recipeAccess()
                .propertySet(net.minecraft.world.item.crafting.RecipePropertySet.FURNACE_INPUT)
                .test(stack));
    }

    private boolean usesPlayerInventoryPicker() {
        return jobIndex == 1;
    }

    private List<BaseItemEntry> playerInventoryEntries() {
        if (minecraft == null || minecraft.player == null) {
            return List.of();
        }
        Inventory inventory = minecraft.player.getInventory();
        List<BaseItemEntry> entries = new ArrayList<>(36);
        // Match the vanilla inventory layout: three storage rows, then the hotbar.
        for (int slot = 9; slot < 36; slot++) {
            entries.add(inventoryEntry(inventory.getItem(slot)));
        }
        for (int slot = 0; slot < 9; slot++) {
            entries.add(inventoryEntry(inventory.getItem(slot)));
        }
        return List.copyOf(entries);
    }

    private BaseItemEntry inventoryEntry(ItemStack stack) {
        ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        return new BaseItemEntry(copy, copy.getCount(), List.of());
    }

    private void drawInventoryStackCount(GuiGraphicsExtractor graphics, ItemStack stack, Rect slot) {
        String value = Integer.toString(stack.getCount());
        float scale = Mth.clamp(plannerScale() * 0.72F, 0.62F, 0.82F);
        int textWidth = Math.round(font().width(value) * scale);
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.right() - textWidth - 2, slot.bottom() - Math.round(font().lineHeight * scale) - 1);
        graphics.pose().scale(scale, scale);
        graphics.text(font(), boldComponent(value), 0, 0, CREAM, true);
        graphics.pose().popMatrix();
    }

    private void refreshSuitableBlocks() {
        suitableBlockPositions.clear();
        matchingBlockPositions.clear();
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        if (isExpeditionSelection(selection)) {
            return;
        }
        if (baseContainerPositions.isEmpty()) {
            refreshBaseInventoryIndex();
        }
        ItemStack selectedItem = selectedFilterStack();
        if (selection == Selection.SOURCE || selection == Selection.DESTINATION) {
            for (BaseInventoryPayload.ContainerEntry container : baseContainers) {
                BlockPos pos = container.pos();
                boolean matchingSource = containsSelectableItem(container, selectedItem);
                boolean suitable = selection == Selection.SOURCE
                        ? matchingSource
                        : acceptsItem(container, selectedItem);
                if (!suitable) continue;
                suitableBlockPositions.add(pos.immutable());
                if (selection == Selection.SOURCE && matchingSource) {
                    matchingBlockPositions.add(pos.immutable());
                }
            }
            if (selection == Selection.SOURCE && jobIndex == 0) {
                addNearbyCraftingTables(suitableBlockPositions);
            }
            return;
        }
        if (selection == Selection.WORKSTATION) {
            List<BlockPos> indexed = switch (jobIndex) {
                case 1 -> indexedFireWorkstations;
                case 2 -> indexedEnergyWorkstations;
                case 3 -> indexedCraftingTables;
                default -> List.of();
            };
            indexed.forEach(pos -> addDistinct(suitableBlockPositions, pos));
        }
    }

    private void addNearbyCraftingTables(List<BlockPos> output) {
        indexedCraftingTables.forEach(pos -> addDistinct(output, pos));
    }

    private void beginWorkstationScan() {
        if (minecraft == null || minecraft.level == null || workstationScanStarted) return;
        workstationScanMinimumY = Math.max(minecraft.level.getMinY(), tablePos.getY() - 24);
        workstationScanMaximumY = Math.min(minecraft.level.getMaxY() - 1, tablePos.getY() + 24);
        workstationScanX = tablePos.getX() - baseRadius;
        workstationScanZ = tablePos.getZ() - baseRadius;
        workstationScanY = workstationScanMinimumY;
        workstationScanStarted = true;
    }

    private void scanWorkstationIndexStep(int budget) {
        if (minecraft == null || minecraft.level == null || workstationScanComplete) return;
        beginWorkstationScan();
        int maximumX = tablePos.getX() + baseRadius;
        int maximumZ = tablePos.getZ() + baseRadius;
        int radiusSquared = baseRadius * baseRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean discovered = false;
        for (int checked = 0; checked < budget && !workstationScanComplete; checked++) {
            int dx = workstationScanX - tablePos.getX();
            int dz = workstationScanZ - tablePos.getZ();
            cursor.set(workstationScanX, workstationScanY, workstationScanZ);
            if (dx * dx + dz * dz <= radiusSquared && minecraft.level.isLoaded(cursor)) {
                BlockState state = minecraft.level.getBlockState(cursor);
                BlockPos found = cursor.immutable();
                if (state.getBlock() instanceof AbstractFurnaceBlock || state.is(ModBlocks.PROCESSOR.get())) {
                    discovered |= addDistinctAndReport(indexedFireWorkstations, found);
                }
                if (state.is(ModBlocks.WIND_TURBINE.get()) || state.is(ModBlocks.WATER_TURBINE.get())) {
                    discovered |= addDistinctAndReport(indexedEnergyWorkstations, found);
                } else if (state.is(ModBlocks.TURBINE_PART.get())) {
                    BlockPos master = TurbinePartBlock.masterPos(found, state);
                    if (TurbinePartBlock.isExpectedMaster(minecraft.level, master, state)) {
                        discovered |= addDistinctAndReport(indexedEnergyWorkstations, master.immutable());
                    }
                }
                if (state.getBlock() instanceof CraftingTableBlock) {
                    discovered |= addDistinctAndReport(indexedCraftingTables, found);
                }
            }
            advanceWorkstationScan(maximumX, maximumZ);
        }
        if (discovered && (selection == Selection.WORKSTATION
                || selection == Selection.SOURCE && jobIndex == 0)) {
            refreshSuitableBlocks();
        }
    }

    private void advanceWorkstationScan(int maximumX, int maximumZ) {
        if (++workstationScanY <= workstationScanMaximumY) return;
        workstationScanY = workstationScanMinimumY;
        if (++workstationScanZ <= maximumZ) return;
        workstationScanZ = tablePos.getZ() - baseRadius;
        if (++workstationScanX > maximumX) workstationScanComplete = true;
    }

    private void restartWorkstationScan() {
        indexedFireWorkstations.clear();
        indexedEnergyWorkstations.clear();
        indexedCraftingTables.clear();
        workstationScanStarted = false;
        workstationScanComplete = false;
        beginWorkstationScan();
        refreshSuitableBlocks();
    }

    private List<BaseItemEntry> craftingRecipeOutputs() {
        if (minecraft.player == null || minecraft.level == null) return List.of();
        Map<Identifier, BaseItemEntry> outputs = new LinkedHashMap<>();
        for (String value : craftingCatalogue) {
            Identifier id = Identifier.tryParse(value);
            var holder = id == null ? null : BuiltInRegistries.ITEM.get(id).orElse(null);
            if (holder != null) {
                outputs.putIfAbsent(id, new BaseItemEntry(holder.value().getDefaultInstance(), countInBase(id), List.of()));
            }
        }
        if (!outputs.isEmpty()) {
            return outputs.values().stream()
                    .sorted(Comparator.comparing(entry -> entry.stack.getHoverName().getString()))
                    .toList();
        }
        var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);
        for (var collection : minecraft.player.getRecipeBook().getCollections()) {
            for (var entry : collection.getRecipes()) {
                if (!(entry.display() instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay)
                        && !(entry.display() instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay)) {
                    continue;
                }
                for (ItemStack stack : entry.resultItems(context)) {
                    if (stack.isEmpty()) continue;
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    outputs.putIfAbsent(id, new BaseItemEntry(stack.copyWithCount(1), countInBase(id), List.of()));
                }
            }
        }
        return outputs.values().stream()
                .sorted(Comparator.comparing(entry -> entry.stack.getHoverName().getString()))
                .toList();
    }

    private int countInBase(Identifier identifier) {
        return baseItems.stream()
                .filter(entry -> BuiltInRegistries.ITEM.getKey(entry.stack.getItem()).equals(identifier))
                .mapToInt(BaseItemEntry::totalCount)
                .findFirst().orElse(0);
    }

    private boolean containsSelectableItem(BaseInventoryPayload.ContainerEntry container, ItemStack selectedItem) {
        boolean currentlyContainsSelection = container.items().stream().anyMatch(item -> item.extractableCount() > 0
                && (selectedItem.isEmpty() || filterIdentifiersMatch(item.identifier(), selectedItem)));
        if (currentlyContainsSelection) return true;
        // With no item filter, any future-capable source is useful. With a selected
        // item, keep ordinary storage highlights honest while still allowing an empty
        // machine output to be wired before its first cycle completes.
        return container.canSupplyItems() && (selectedItem.isEmpty() || !container.acceptsAnyItem());
    }

    private boolean filterItemsMatch(ItemStack candidate, ItemStack selected) {
        if (ItemStack.isSameItemSameComponents(candidate, selected)) {
            return true;
        }
        return !exactItemMatch && selected.typeHolder().tags().anyMatch(candidate::is);
    }

    private boolean filterIdentifiersMatch(String candidateIdentifier, ItemStack selected) {
        Identifier id = Identifier.tryParse(candidateIdentifier);
        var candidate = id == null ? null : BuiltInRegistries.ITEM.get(id).orElse(null);
        return candidate != null && filterItemsMatch(candidate.value().getDefaultInstance(), selected);
    }

    private boolean acceptsItem(BaseInventoryPayload.ContainerEntry container, ItemStack incoming) {
        if (container.acceptsAnyItem()) return true;
        if (!incoming.isEmpty() && minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(container.pos()) instanceof Container liveContainer) {
            for (int slot = 0; slot < liveContainer.getContainerSize(); slot++) {
                if (BaseInventoryIndex.canInsert(liveContainer, slot, incoming)) return true;
            }
        }
        if (incoming.isEmpty()) return container.canReceiveItems()
                || container.items().stream().anyMatch(BaseInventoryPayload.ItemEntry::acceptsMore);
        String identifier = BuiltInRegistries.ITEM.getKey(incoming.getItem()).toString();
        return container.items().stream().anyMatch(item -> item.acceptsMore() && item.identifier().equals(identifier));
    }

    static Screen claimMachineMenuReturn() {
        WorksitePlannerScreen screen = pendingMachineMenuReturn;
        pendingMachineMenuReturn = null;
        if (screen == null || Util.getNanos() > pendingMachineMenuDeadline) return null;
        screen.resumingFromMachineMenu = true;
        return screen;
    }

    private BlockPos connectedChestPosition(BlockPos pos) {
        if (minecraft == null || minecraft.level == null) return null;
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return ChestBlock.getConnectedBlockPos(pos, state).immutable();
    }

    private PlannerTool[] toolsForJob() {
        return switch (jobIndex) {
            case 0 -> new PlannerTool[]{
                    PlannerTool.BATCH, PlannerTool.RESERVE, PlannerTool.TARGET,
                    PlannerTool.ROUTE, PlannerTool.SCHEDULE, PlannerTool.REPEAT,
                    PlannerTool.MATCH, PlannerTool.SAFETY, PlannerTool.PRIORITY, PlannerTool.SAVE, PlannerTool.CANCEL
            };
            case 1 -> new PlannerTool[]{
                    PlannerTool.FILTER, PlannerTool.SCHEDULE, PlannerTool.REPEAT,
                    PlannerTool.PRIORITY, PlannerTool.SAFETY, PlannerTool.SAVE, PlannerTool.CANCEL
            };
            case 2 -> new PlannerTool[]{
                    PlannerTool.SCHEDULE, PlannerTool.PRIORITY,
                    PlannerTool.SAFETY, PlannerTool.SAVE, PlannerTool.CANCEL
            };
            case 3 -> new PlannerTool[]{
                    PlannerTool.FILTER, PlannerTool.BATCH, PlannerTool.SCHEDULE,
                    PlannerTool.REPEAT, PlannerTool.PRIORITY, PlannerTool.SAFETY,
                    PlannerTool.SAVE, PlannerTool.CANCEL
            };
            case 4 -> new PlannerTool[]{
                    PlannerTool.SAVE, PlannerTool.CANCEL
            };
            default -> new PlannerTool[]{PlannerTool.SAVE, PlannerTool.CANCEL};
        };
    }

    private PlannerTool toolAt(SpecialtyDockLayout layout, double mouseX, double mouseY) {
        for (int index = 0; index < layout.slots.length; index++) {
            if (layout.slots[index].contains(mouseX, mouseY)) {
                return layout.tools[index];
            }
        }
        return null;
    }

    private float specialtyRightOffset(float reveal) {
        float scale = plannerScale();
        return (1.0F - reveal) * Math.max(0.0F, DOCK_RIGHT_WIDTH * scale - 2.0F);
    }

    private float specialtyBottomOffset(float reveal) {
        float scale = plannerScale();
        return (1.0F - reveal) * Math.max(0.0F, DOCK_BOTTOM_HEIGHT * scale - 2.0F);
    }

    private int toolColor(PlannerTool tool) {
        return switch (tool) {
            case FILTER, BATCH, RESERVE -> AMBER;
            case TARGET, REPEAT, SAVE -> GREEN;
            case SCHEDULE, MATCH -> CYAN;
            case ROUTE -> VIOLET;
            case PRIORITY, SAFETY, CANCEL -> CORAL;
        };
    }

    private TopDrawerLayout topDrawerLayout(float reveal) {
        float scale = plannerScale();
        int canvasWidth = Math.round(PLANNER_TEXTURE_WIDTH * scale);
        int canvasHeight = Math.round(PLANNER_TEXTURE_HEIGHT * scale);
        int visibleHeight = Math.round(PLANNER_VISIBLE_HEIGHT * scale);
        int y = Math.round(topDrawerExactY(reveal));
        int x = (width - canvasWidth) / 2;
        Rect canvas = new Rect(x, y, canvasWidth, canvasHeight);
        Rect clip = new Rect(Math.max(0, x), 0, Math.min(width, x + canvasWidth) - Math.max(0, x), Math.max(1, y + visibleHeight));
        Rect identity = scaledRect(x, y, scale, 4, 4, 82, 10);
        Rect step = scaledRect(x, y, scale, 301, 4, 122, 10);
        Rect carousel = scaledRect(x, y, scale, 96, 13, 195, 10);
        int arrowWidth = Math.max(14, Math.round(18.0F * scale));
        Rect previousJob = new Rect(carousel.x, carousel.y, arrowWidth, carousel.height);
        Rect nextJob = new Rect(carousel.right() - arrowWidth, carousel.y, arrowWidth, carousel.height);
        Rect jobName = new Rect(previousJob.right(), carousel.y, carousel.width - arrowWidth * 2, carousel.height);
        return new TopDrawerLayout(canvas, clip, identity, step, carousel, previousJob, jobName, nextJob, y + visibleHeight);
    }

    private LeftDrawerLayout leftDrawerLayout(float reveal) {
        float scale = plannerScale();
        int canvasWidth = Math.round(PLANNER_TEXTURE_WIDTH * scale);
        int canvasHeight = Math.round(PLANNER_TEXTURE_HEIGHT * scale);
        int leftWidth = Math.round(PLANNER_LEFT_WIDTH * scale);
        int x = Math.round(leftDrawerExactX(reveal));
        Rect canvas = new Rect(x, 0, canvasWidth, canvasHeight);
        int railTop = Math.round(PLANNER_LEFT_TOP * scale);
        int railBottom = Math.round(PLANNER_LEFT_BOTTOM * scale);
        int clipLeft = Math.max(0, x);
        int clipRight = Math.min(width, x + leftWidth);
        Rect clip = new Rect(clipLeft, railTop, Math.max(1, clipRight - clipLeft), railBottom - railTop);
        Rect rail = new Rect(x, railTop, leftWidth, railBottom - railTop);
        Rect[] slots = new Rect[5];
        for (int index = 0; index < slots.length; index++) {
            slots[index] = scaledRect(x, 0, scale, 5, 39 + index * 22, 14, 14);
        }
        Rect progress = scaledRect(x, 0, scale, 7, 149, 10, 52);
        return new LeftDrawerLayout(canvas, clip, rail, slots, progress, scale);
    }

    private float plannerScale() {
        float horizontalFit = (width - 10.0F) / PLANNER_TEXTURE_WIDTH;
        float verticalFit = (height - 2.0F) / PLANNER_TEXTURE_HEIGHT;
        return Math.max(0.55F, Math.min(horizontalFit, verticalFit));
    }

    private float topDrawerExactY(float reveal) {
        float scale = plannerScale();
        float visibleHeight = PLANNER_VISIBLE_HEIGHT * scale;
        float tuckedY = -visibleHeight + Math.max(2.0F, 2.0F * scale);
        return Mth.lerp(reveal, tuckedY, 0.0F);
    }

    private float leftDrawerExactX(float reveal) {
        float scale = plannerScale();
        float openX = (width - PLANNER_TEXTURE_WIDTH * scale) * 0.5F;
        float leftWidth = PLANNER_LEFT_WIDTH * scale;
        float tuckedX = -leftWidth + Math.max(2.0F, 2.0F * scale);
        return Mth.lerp(reveal, tuckedX, openX);
    }

    private int workflowSlotAt(LeftDrawerLayout layout, int mouseX, int mouseY) {
        for (int index = 0; index < layout.slots.length; index++) {
            if (layout.slots[index].contains(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private Rect workflowBubbleRect(LeftDrawerLayout layout, int slot, float reveal) {
        Rect anchor = layout.slots[Mth.clamp(slot, 0, layout.slots.length - 1)];
        int fullWidth = Math.round(176.0F * layout.scale);
        int fullHeight = Math.round(54.0F * layout.scale);
        int width = Math.max(anchor.height, Math.round(Mth.lerp(reveal, anchor.height, fullWidth)));
        int height = Math.max(anchor.height, Math.round(Mth.lerp(reveal, anchor.height, fullHeight)));
        int y = Mth.clamp(anchor.centerY() - height / 2, Math.round(28.0F * layout.scale), this.height - height - 6);
        return new Rect(anchor.right() + Math.max(2, Math.round(2.0F * layout.scale)), y, width, height);
    }

    private void drawWorkflowDetail(GuiGraphicsExtractor graphics, Rect bubble, int slot, Selection[] available, int alpha) {
        if (bubble.height < font().lineHeight + 5) {
            return;
        }
        if (slot < available.length) {
            Selection option = available[slot];
            if (isExpeditionSelection(option)) {
                int tier = expeditionIndex(option);
                textFit(graphics, expeditionName(tier), bubble.x + 7, bubble.y + 6, bubble.width - 14,
                        withAlpha(expeditionColor(tier), alpha));
                if (!canAttemptExpedition(tier)) {
                    textFit(graphics, "UNAVAILABLE  ·  NEEDS 2 STARS",
                            bubble.x + 7, bubble.y + 22, bubble.width - 14, withAlpha(CORAL, alpha));
                    textFit(graphics, "CHOOSE A STRONGER EXPEDITION DINOSAUR",
                            bubble.x + 7, bubble.y + 39, bubble.width - 14, withAlpha(MUTED, alpha));
                    return;
                }
                textFit(graphics, expeditionDurationMinutes(tier) + " MIN  ·  YIELD " + expeditionRewardMultiplier(tier) + "/5",
                        bubble.x + 7, bubble.y + 19, bubble.width - 14, withAlpha(INK, alpha));
                textFit(graphics, expeditionRiskPercent(tier) + "% INCAPACITATION RISK",
                        bubble.x + 7, bubble.y + 32, bubble.width - 14, withAlpha(MUTED, alpha));
                textFit(graphics, expeditionTier == tier ? "CURRENT TIER" : "CLICK TO SELECT",
                        bubble.x + 7, bubble.y + 45, bubble.width - 14, withAlpha(CREAM, alpha));
                return;
            }
            List<BlockPos> selected = positions(option);
            String count = supportsMultiple(option) ? selected.size() + "/8 LOCATIONS" : selected.isEmpty() ? "NOT SET" : "SET";
            textFit(graphics, selectionLabel(option) + "  •  " + count, bubble.x + 7, bubble.y + 6, bubble.width - 14, withAlpha(option.color, alpha));
            textFit(graphics, selectionPurpose(option), bubble.x + 7, bubble.y + 19, bubble.width - 14, withAlpha(INK, alpha));
            String state = selected.isEmpty() ? selectionInstruction(option) : selected.size() == 1 ? compactPos(selected.getFirst()) : selected.size() + " BLOCKS SELECTED";
            textFit(graphics, state, bubble.x + 7, bubble.y + 32, bubble.width - 14, withAlpha(MUTED, alpha));
            textFit(graphics, "CLICK BLOCK: ADD / REMOVE", bubble.x + 7, bubble.y + 45, bubble.width - 14, withAlpha(CREAM, alpha));
            return;
        }
        if (slot == 4) {
            textFit(graphics, "ORDER RULES", bubble.x + 7, bubble.y + 6, bubble.width - 14, withAlpha(VIOLET, alpha));
            textFit(graphics, "Controls amounts, timing, routing and safety.", bubble.x + 7, bubble.y + 19, bubble.width - 14, withAlpha(INK, alpha));
            textFit(graphics, "Use the dock along the bottom of the screen.", bubble.x + 7, bubble.y + 32, bubble.width - 14, withAlpha(MUTED, alpha));
            return;
        }
        textFit(graphics, "ITEM FILTERS", bubble.x + 7, bubble.y + 6, bubble.width - 14, withAlpha(AMBER, alpha));
        textFit(graphics, "Choose exactly what this dinosaur may handle.", bubble.x + 7, bubble.y + 19, bubble.width - 14, withAlpha(INK, alpha));
        textFit(graphics, "Items appear after at least one source is selected.", bubble.x + 7, bubble.y + 32, bubble.width - 14, withAlpha(MUTED, alpha));
    }

    private String selectionPurpose(Selection option) {
        return switch (jobIndex) {
            case 0 -> option == Selection.SOURCE ? "Containers the Dodo is allowed to take items from."
                    : "Containers that should receive the carried items.";
            case 1 -> "Furnaces, Ancient Furnaces and Processors this dino tends. Transporters handle every input and output move.";
            case 2 -> "Only placed Primeval Wind Turbines and Water Turbines can power this order.";
            case 3 -> "A normal crafting table where this dino fetches ingredients and crafts the chosen result.";
            case 4 -> "Choose expedition time, danger and reward size. The dino is locked away until it returns.";
            default -> option.instruction;
        };
    }

    private Rect scaledRect(int originX, int originY, float scale, int x, int y, int width, int height) {
        return new Rect(
                originX + Math.round(x * scale),
                originY + Math.round(y * scale),
                Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale))
        );
    }

    private void drawSelectionPanel(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY) {
        panel(graphics, rect, CYAN);
        text(graphics, "WORKFLOW", rect.x + 11, rect.y + 9, CREAM);
        textFit(graphics, jobDescription(), rect.x + 11, rect.y + 23, rect.width - 22, INK);
        Selection[] available = selections();
        for (int i = 0; i < available.length; i++) {
            Selection option = available[i];
            Rect row = selectionRect(rect, i);
            boolean selected = option == selection;
            boolean hovered = row.contains(mouseX, mouseY);
            int accent = option.color;
            graphics.fill(row.x, row.y, row.right(), row.bottom(), selected ? 0xE8F2D4B5 : hovered ? 0xDDE2C3A4 : 0xC8C6A27F);
            graphics.fill(row.x, row.y, row.x + (selected ? 4 : 2), row.bottom(), accent);
            graphics.item(option.icon, row.x + 8, row.y + 7);
            textFit(graphics, (i + 1) + "  " + option.label, row.x + 29, row.y + 6, row.width - 35, selected || hovered ? accent : INK);
            BlockPos position = position(option);
            textFit(graphics, position == null ? option.shortHint : compactPos(position), row.x + 29, row.y + 18, row.width - 35, position == null ? MUTED : INK);
        }
        int y = rect.bottom() - 47;
        graphics.fill(rect.x + 9, y, rect.right() - 9, y + 1, EDGE);
        text(graphics, "CURRENT STEP", rect.x + 11, y + 8, MUTED);
        textFit(graphics, selection.instruction, rect.x + 11, y + 21, rect.width - 22, selection.color);
    }

    private void drawRulesPanel(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY) {
        panel(graphics, rect, GREEN);
        text(graphics, "AUTOMATION RULES", rect.x + 11, rect.y + 9, CREAM);
        drawRuleTab(graphics, routeTab(rect), "ROUTE", rulePage == 0, mouseX, mouseY);
        drawRuleTab(graphics, stockTab(rect), "STOCK + ITEMS", rulePage == 1, mouseX, mouseY);
        if (rulePage == 0) {
            drawRule(graphics, ruleRect(rect, 0), "PRIORITY", PRIORITIES[priority], mouseX, mouseY, CORAL);
            drawRule(graphics, ruleRect(rect, 1), "SCHEDULE", schedule == 2 ? SCHEDULES[schedule] + "  !" : SCHEDULES[schedule],
                    mouseX, mouseY, schedule == 2 ? CORAL : CYAN);
            drawRule(graphics, ruleRect(rect, 2), "REPEAT", REPEATS[repeatMode], mouseX, mouseY, GREEN);
            drawRule(graphics, ruleRect(rect, 3), "ROUTE", ROUTES[routePolicy], mouseX, mouseY, VIOLET);
            drawRule(graphics, ruleRect(rect, 4), "SAFETY", avoidDanger ? "AVOID DANGER" : "DIRECT ROUTE", mouseX, mouseY, CORAL);
        } else {
            drawRule(graphics, ruleRect(rect, 0), "BATCH", batchSize + " ITEMS", mouseX, mouseY, AMBER);
            drawRule(graphics, ruleRect(rect, 1), "SOURCE RESERVE", sourceReserve == 0 ? "NONE" : sourceReserve + " LEFT", mouseX, mouseY, AMBER);
            drawRule(graphics, ruleRect(rect, 2), "TARGET STOCK", destinationTarget == 0 ? "NO LIMIT" : Integer.toString(destinationTarget), mouseX, mouseY, GREEN);
            drawRule(graphics, ruleRect(rect, 3), "MATCH", exactItemMatch ? "EXACT ITEM" : "FLEXIBLE TAG", mouseX, mouseY, CYAN);
            String filterName = itemFilter.isEmpty() ? "ANY VALID ITEM" : shortIdentifier(itemFilter);
            int filterY = ruleRect(rect, 3).bottom() + 9;
            textFit(graphics, "ITEM  /  " + filterName, rect.x + 11, filterY, rect.width - 22, itemFilter.isEmpty() ? MUTED : CREAM);
            Rect any = anyItemRect(rect);
            drawSmallButton(graphics, any, "ANY", itemFilter.isEmpty(), mouseX, mouseY);
            for (int i = 0; i < filterItems.size(); i++) {
                Rect slot = filterSlot(rect, i);
                boolean selected = BuiltInRegistries.ITEM.getKey(filterItems.get(i).getItem()).toString().equals(itemFilter);
                graphics.fill(slot.x, slot.y, slot.right(), slot.bottom(), selected ? 0xFFF4D28D : slot.contains(mouseX, mouseY) ? 0xE8E7CBAA : 0xD0B78F70);
                outline(graphics, slot, selected ? AMBER : EDGE);
                graphics.item(filterItems.get(i), slot.x + 3, slot.y + 3);
            }
        }
    }

    private void drawRuleTab(GuiGraphicsExtractor graphics, Rect rect, String label, boolean selected, int mouseX, int mouseY) {
        int color = selected ? 0xFFE8C18F : rect.contains(mouseX, mouseY) ? 0xFFD4B18B : 0xFFAD876A;
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x, rect.bottom() - 2, rect.right(), rect.bottom(), selected ? GREEN : EDGE);
        centered(graphics, label, rect.centerX(), rect.y + 5, selected ? INK : MUTED);
    }

    private void drawRule(GuiGraphicsExtractor graphics, Rect rect, String label, String value, int mouseX, int mouseY, int accent) {
        boolean hovered = rect.contains(mouseX, mouseY);
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), hovered ? 0xE8E8C9A7 : 0xCDBF9978);
        graphics.fill(rect.x, rect.y, rect.x + (hovered ? 4 : 2), rect.bottom(), hovered ? accent : EDGE);
        text(graphics, label, rect.x + 8, rect.y + 6, MUTED);
        textRight(graphics, value, rect.right() - 8, rect.y + 6, hovered ? accent : INK);
    }

    private void drawBottomBar(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY) {
        panel(graphics, rect, VIOLET);
        int noteColor = Util.getNanos() < feedbackUntilNanos ? CREAM : MUTED;
        Layout layout = layout();
        textFit(graphics, feedback, rect.x + 12, rect.y + 13, layout.cancel.x - rect.x - 22, noteColor);
        drawButton(graphics, layout.cancel(), "CANCEL", false, mouseX, mouseY);
        drawButton(graphics, layout.apply(), "SAVE AUTOMATION", true, mouseX, mouseY);
    }

    private void drawWorldReadout(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY) {
        if (hoveredPos == null || minecraft.level == null) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(hoveredPos);
        String blockName = state.getBlock().getName().getString();
        float pulse = 0.5F + 0.5F * Mth.sin(renderTimeTicks * 0.13F);
        int lift = Math.round(pulse * 2.0F);
        Rect lifted = new Rect(rect.x, rect.y - lift, rect.width, rect.height);
        graphics.fill(lifted.x + 3, lifted.y + 4, lifted.right() + 3, lifted.bottom() + 4, 0x5A000000);
        graphics.fill(lifted.x, lifted.y, lifted.right(), lifted.bottom(), 0xE8B79370);
        outline(graphics, lifted, selection.color);

        Rect preview = new Rect(lifted.x + 3, lifted.y + 3, 29, 29);
        drawSpaceBubble(graphics, preview);
        ItemStack blockPreview = state.getBlock().asItem().getDefaultInstance();
        if (!blockPreview.isEmpty()) {
            float previewScale = 1.38F + pulse * 0.04F;
            float renderedSize = 16.0F * previewScale;
            graphics.pose().pushMatrix();
            graphics.pose().translate(preview.centerX() - renderedSize * 0.5F,
                    preview.centerY() - renderedSize * 0.5F);
            graphics.pose().scale(previewScale, previewScale);
            graphics.item(blockPreview, 0, 0);
            graphics.pose().popMatrix();
        }

        int copyX = preview.right() + 7;
        int copyWidth = lifted.right() - copyX - 7;
        textFit(graphics, selection.label + "  /  " + compactPos(hoveredPos),
                copyX, lifted.y + 6, copyWidth, selection.color);
        textFit(graphics, blockName, copyX, lifted.y + 19, copyWidth, CREAM);
    }

    private void assignSelection(BlockPos position) {
        BlockPos selectedPos = canonicalSelectionPosition(selection, position);
        if (!validForSelection(selection, selectedPos)) {
            feedback(selectionInstruction(selection) + ". That block cannot fill this role.");
            return;
        }
        if (supportsMultiple(selection) && !positions(selection).contains(selectedPos) && positions(selection).size() >= 8) {
            feedback(selectionLabel(selection) + " already has the maximum of 8 locations.");
            return;
        }
        boolean added = false;
        switch (selection) {
            case SOURCE -> {
                added = togglePosition(sourcePositions, selectedPos);
                sourcePos = firstPosition(sourcePositions);
                refreshFilterItems();
            }
            case AREA_START -> {
                added = !selectedPos.equals(sourcePos);
                sourcePositions.clear();
                if (added) {
                    sourcePositions.add(selectedPos);
                    sourcePos = selectedPos;
                } else {
                    sourcePos = null;
                }
            }
            case WORKSTATION -> {
                added = togglePosition(workstationPositions, selectedPos);
                workstationPos = firstPosition(workstationPositions);
            }
            case DESTINATION -> {
                added = togglePosition(destinationPositions, selectedPos);
                destinationPos = firstPosition(destinationPositions);
            }
            case AREA_END -> {
                added = !selectedPos.equals(areaEndPos);
                areaEndPos = added ? selectedPos : null;
            }
            case FALLBACK -> {
                added = togglePosition(fallbackPositions, selectedPos);
                fallbackPos = firstPosition(fallbackPositions);
            }
        }
        int count = positions(selection).size();
        if (added) {
            blockPriorities.putIfAbsent(selectedPos, 1);
        } else {
            pruneBlockPriorities();
            if (selectedPos.equals(priorityTargetPos) && !isSelectedWorkBlock(selectedPos)) {
                priorityTargetPos = null;
            }
        }
        refreshBaseInventoryIndex();
        refreshSuitableBlocks();
        feedback(selectionLabel(selection) + (added ? " added" : " removed") + " at " + compactPos(selectedPos)
                + (supportsMultiple(selection) ? ". " + count + "/8 selected." : "."));
    }

    private BlockPos canonicalSelectionPosition(Selection option, BlockPos position) {
        BlockPos selected = position.immutable();
        if (option == Selection.WORKSTATION && jobIndex == 2 && minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(selected);
            if (state.is(ModBlocks.TURBINE_PART.get())) {
                BlockPos master = TurbinePartBlock.masterPos(selected, state);
                if (TurbinePartBlock.isExpectedMaster(minecraft.level, master, state)) return master.immutable();
            }
        }
        if (option != Selection.SOURCE && option != Selection.DESTINATION) return selected;
        BlockPos connected = connectedChestPosition(selected);
        return connected != null && connected.asLong() < selected.asLong() ? connected : selected;
    }

    private boolean validForSelection(Selection option, BlockPos pos) {
        if (minecraft.level == null) {
            return false;
        }
        if (positions(option).contains(pos)) {
            return true;
        }
        if (isExpeditionSelection(option)) return false;
        if (option == Selection.WORKSTATION) {
            BlockState state = minecraft.level.getBlockState(pos);
            return switch (jobIndex) {
                case 1 -> state.getBlock() instanceof AbstractFurnaceBlock || state.is(ModBlocks.PROCESSOR.get());
                case 2 -> state.is(ModBlocks.WIND_TURBINE.get()) || state.is(ModBlocks.WATER_TURBINE.get());
                case 3 -> state.getBlock() instanceof CraftingTableBlock;
                default -> false;
            };
        }
        return suitableBlockPositions.contains(pos);
    }

    private static boolean togglePosition(List<BlockPos> positions, BlockPos pos) {
        if (positions.remove(pos)) {
            return false;
        }
        if (positions.size() >= 8) {
            return false;
        }
        positions.add(pos);
        return true;
    }

    private boolean supportsMultiple(Selection option) {
        return option != Selection.AREA_START && option != Selection.AREA_END && !isExpeditionSelection(option);
    }

    private void normalizeDraftForSpecialty() {
        areaEndPos = null;
        switch (jobIndex) {
            case 0 -> {
                workstationPositions.clear();
                fallbackPositions.clear();
                fuelFilters.clear();
            }
            case 1 -> {
                sourcePositions.clear();
                destinationPositions.clear();
                fallbackPositions.clear();
                while (itemFilters.size() > 3) itemFilters.removeLast();
                while (fuelFilters.size() > 4) fuelFilters.removeLast();
            }
            case 2 -> {
                sourcePositions.clear();
                destinationPositions.clear();
                fallbackPositions.clear();
                itemFilters.clear();
                fuelFilters.clear();
            }
            case 3 -> {
                sourcePositions.clear();
                destinationPositions.clear();
                fallbackPositions.clear();
                fuelFilters.clear();
                while (itemFilters.size() > 1) itemFilters.removeLast();
            }
            case 4 -> {
                sourcePositions.clear();
                workstationPositions.clear();
                destinationPositions.clear();
                fallbackPositions.clear();
                fuelFilters.clear();
                itemFilters.clear();
            }
            default -> {
            }
        }
        sourcePos = firstPosition(sourcePositions);
        workstationPos = firstPosition(workstationPositions);
        destinationPos = firstPosition(destinationPositions);
        fallbackPos = firstPosition(fallbackPositions);
        itemFilter = itemFilters.isEmpty() ? "" : itemFilters.getFirst();
        pruneBlockPriorities();
    }

    private static boolean isExpeditionSelection(Selection option) {
        return option == Selection.EXPEDITION_ONE || option == Selection.EXPEDITION_TWO
                || option == Selection.EXPEDITION_THREE || option == Selection.EXPEDITION_FOUR
                || option == Selection.EXPEDITION_FIVE;
    }

    private static int expeditionIndex(Selection option) {
        return switch (option) {
            case EXPEDITION_ONE -> 0;
            case EXPEDITION_TWO -> 1;
            case EXPEDITION_THREE -> 2;
            case EXPEDITION_FOUR -> 3;
            case EXPEDITION_FIVE -> 4;
            default -> 0;
        };
    }

    private static String expeditionName(int tier) {
        return switch (Mth.clamp(tier, 0, 4)) {
            case 0 -> "SAFE FORAGE";
            case 1 -> "RIDGE TRAIL";
            case 2 -> "DEEP WILDS";
            case 3 -> "PREDATOR RUN";
            default -> "PRIMORDIAL FRONTIER";
        };
    }

    private int expeditionDurationMinutes(int tier) {
        long ticks = WorkSpecialtyRules.expeditionDurationTicks(
                tier, dodo.getSpecialtyStars(4), dodo.getMutationStatMultiplier());
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table) {
            ticks = Math.max(1L, Math.round(ticks * table.expeditionDurationMultiplier()));
        }
        return (int)Math.ceil(ticks / 1200.0D);
    }

    private boolean canAttemptExpedition(int tier) {
        return WorkSpecialtyRules.canAttemptExpedition(tier, dodo.getSpecialtyStars(4));
    }

    private String expeditionStatusLabel(int tier) {
        return canAttemptExpedition(tier) ? expeditionDurationMinutes(tier) + " MIN" : "UNAVAILABLE";
    }

    private int expeditionRiskPercent(int tier) {
        return WorkSpecialtyRules.expeditionRiskPercent(
                tier, dodo.getSpecialtyStars(4), dodo.getDinosaurLevel(), dodo.getMutationStatMultiplier());
    }

    private static int expeditionRewardMultiplier(int tier) {
        return WorkSpecialtyRules.expeditionRewardCount(tier);
    }

    private static int expeditionColor(int tier) {
        return new int[]{GREEN, CYAN, AMBER, CORAL, VIOLET}[Mth.clamp(tier, 0, 4)];
    }

    private void advanceSelection() {
        Selection[] available = selections();
        for (int i = 0; i < available.length - 1; i++) {
            if (available[i] == selection) {
                selection = available[i + 1];
                return;
            }
        }
    }

    private boolean handleRuleClick(Rect panel, double mouseX, double mouseY) {
        int rowCount = rulePage == 0 ? 5 : 4;
        for (int i = 0; i < rowCount; i++) {
            if (!ruleRect(panel, i).contains(mouseX, mouseY)) {
                continue;
            }
            if (rulePage == 0) {
                switch (i) {
                    case 0 -> priority = (priority + 1) % PRIORITIES.length;
                    case 1 -> schedule = (schedule + 1) % SCHEDULES.length;
                    case 2 -> repeatMode = (repeatMode + 1) % REPEATS.length;
                    case 3 -> routePolicy = (routePolicy + 1) % ROUTES.length;
                    case 4 -> avoidDanger = !avoidDanger;
                    default -> {
                    }
                }
            } else {
                switch (i) {
                    case 0 -> batchSize = BATCHES[(indexOf(BATCHES, batchSize) + 1) % BATCHES.length];
                    case 1 -> sourceReserve = STOCK_LEVELS[(indexOf(STOCK_LEVELS, sourceReserve) + 1) % STOCK_LEVELS.length];
                    case 2 -> destinationTarget = STOCK_LEVELS[(indexOf(STOCK_LEVELS, destinationTarget) + 1) % STOCK_LEVELS.length];
                    case 3 -> exactItemMatch = !exactItemMatch;
                    default -> {
                    }
                }
            }
            feedback(rulePage == 0 && i == 1 && schedule == 2 ? nightShiftWarning() : "Rule updated.");
            return true;
        }
        return false;
    }

    private void applyAssignment() {
        if (!workStateReceived) {
            feedback("Loading this dinosaur's saved work order...");
            return;
        }
        String missing = missingRequiredSelection();
        if (missing != null) {
            feedback("Choose " + missing + " before saving.");
            return;
        }
        AssignDodoWorkPayload payload = new AssignDodoWorkPayload(
                dodo.getId(),
                jobIndex,
                tablePos,
                List.copyOf(sourcePositions),
                List.copyOf(workstationPositions),
                List.copyOf(destinationPositions),
                Optional.ofNullable(areaEndPos),
                List.copyOf(fallbackPositions),
                List.copyOf(itemFilters),
                List.copyOf(fuelFilters),
                Map.copyOf(blockPriorities),
                expeditionTier,
                priority,
                batchSize,
                schedule,
                sourceReserve,
                destinationTarget,
                repeatMode,
                routePolicy,
                exactItemMatch,
                avoidDanger
        );
        ClientPacketDistributor.sendToServer(payload);
        minecraft.setScreen(parent);
    }

    private String missingRequiredSelection() {
        return switch (jobIndex) {
            case 0 -> sourcePos == null ? "a source container" : destinationPos == null ? "a destination container" : null;
            case 1 -> workstationPos == null ? "a furnace, Ancient Furnace or Processor" : null;
            case 2 -> workstationPos == null ? "a wind or water turbine" : null;
            case 3 -> workstationPos == null ? "a crafting table" : itemFilters.isEmpty() ? "a recipe result" : null;
            case 4 -> null;
            default -> null;
        };
    }

    private Selection[] selections() {
        return switch (jobIndex) {
            case 0 -> new Selection[]{Selection.SOURCE, Selection.DESTINATION};
            case 1, 2, 3 -> new Selection[]{Selection.WORKSTATION};
            case 4 -> new Selection[]{Selection.EXPEDITION_ONE, Selection.EXPEDITION_TWO, Selection.EXPEDITION_THREE,
                    Selection.EXPEDITION_FOUR, Selection.EXPEDITION_FIVE};
            default -> new Selection[]{Selection.SOURCE, Selection.DESTINATION};
        };
    }

    private BlockPos position(Selection option) {
        List<BlockPos> positions = positions(option);
        return positions.isEmpty() ? null : positions.getFirst();
    }

    private List<BlockPos> positions(Selection option) {
        return switch (option) {
            case SOURCE, AREA_START -> sourcePositions;
            case WORKSTATION -> workstationPositions;
            case DESTINATION -> destinationPositions;
            case AREA_END -> areaEndPos == null ? List.of() : List.of(areaEndPos);
            case FALLBACK -> fallbackPositions;
            case EXPEDITION_ONE, EXPEDITION_TWO, EXPEDITION_THREE, EXPEDITION_FOUR, EXPEDITION_FIVE -> List.of();
        };
    }

    private boolean isSelectedWorkBlock(BlockPos pos) {
        return pos != null && (sourcePositions.contains(pos)
                || workstationPositions.contains(pos)
                || destinationPositions.contains(pos)
                || fallbackPositions.contains(pos)
                || pos.equals(areaEndPos));
    }

    private List<BlockPos> selectedWorkBlocks() {
        List<BlockPos> selected = new ArrayList<>();
        sourcePositions.forEach(pos -> addDistinct(selected, pos));
        workstationPositions.forEach(pos -> addDistinct(selected, pos));
        destinationPositions.forEach(pos -> addDistinct(selected, pos));
        fallbackPositions.forEach(pos -> addDistinct(selected, pos));
        addDistinct(selected, areaEndPos);
        return selected;
    }

    private static void addDistinct(List<BlockPos> positions, BlockPos pos) {
        if (pos != null && !positions.contains(pos)) {
            positions.add(pos.immutable());
        }
    }

    private static boolean addDistinctAndReport(List<BlockPos> positions, BlockPos pos) {
        if (pos == null || positions.contains(pos)) return false;
        positions.add(pos.immutable());
        return true;
    }

    private void pruneBlockPriorities() {
        blockPriorities.keySet().removeIf(pos -> !isSelectedWorkBlock(pos));
    }

    private int activePriority() {
        return priorityTargetPos == null ? priority : blockPriority(priorityTargetPos);
    }

    private int blockPriority(BlockPos pos) {
        return Mth.clamp(blockPriorities.getOrDefault(pos, 1), 0, 3);
    }

    private String blockName(BlockPos pos) {
        if (pos == null || minecraft == null || minecraft.level == null) {
            return "Selected block";
        }
        return minecraft.level.getBlockState(pos).getBlock().getName().getString();
    }

    private void refreshFilterItems() {
        if (minecraft == null || minecraft.level == null || sourcePositions.isEmpty()) {
            filterItems = List.of();
            return;
        }
        Map<Identifier, ItemStack> unique = new LinkedHashMap<>();
        for (BlockPos source : sourcePositions) {
            BaseInventoryPayload.ContainerEntry container = baseContainers.stream()
                    .filter(entry -> entry.pos().equals(source)).findFirst().orElse(null);
            if (container == null) continue;
            for (BaseInventoryPayload.ItemEntry item : container.items()) {
                if (item.extractableCount() <= 0 || unique.size() >= 12) continue;
                Identifier id = Identifier.tryParse(item.identifier());
                var holder = id == null ? null : BuiltInRegistries.ITEM.get(id).orElse(null);
                if (holder != null) unique.putIfAbsent(id, holder.value().getDefaultInstance());
            }
            if (unique.size() >= 12) {
                break;
            }
        }
        filterItems = new ArrayList<>(unique.values());
    }

    private BlockPos rayTrace(int mouseX, int mouseY) {
        if (minecraft.level == null || cameraAnchor == null || width <= 0 || height <= 0) {
            return null;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        float normalizedX = mouseX / (float) width * 2.0F - 1.0F;
        float normalizedY = 1.0F - mouseY / (float) height * 2.0F;
        Vec3 direction = camera.getNearPlane(camera.getFov()).getPointOnPlane(normalizedX, normalizedY).normalize();
        Vec3 start = camera.position();
        Vec3 end = start.add(direction.scale(55.0D));
        BlockPos throughWallTarget = rayTraceHighlightedBlock(start, end);
        if (throughWallTarget != null) {
            return throughWallTarget;
        }
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                cameraAnchor
        ));
        if (hit.getType() == HitResult.Type.MISS || !insideBase(hit.getBlockPos())) {
            return null;
        }
        return canonicalSelectionPosition(selection, hit.getBlockPos());
    }

    private BlockPos rayTraceHighlightedBlock(Vec3 start, Vec3 end) {
        List<BlockPos> candidates = new ArrayList<>(suitableBlockPositions);
        List<BlockPos> selectable = pinnedTool == PlannerTool.PRIORITY ? selectedWorkBlocks() : positions(selection);
        for (BlockPos selected : selectable) {
            if (!candidates.contains(selected)) {
                candidates.add(selected);
            }
        }
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            if (!insideBase(pos)) {
                continue;
            }
            AABB bounds = highlightBounds(pos).inflate(0.08D);
            Optional<Vec3> intersection = bounds.clip(start, end);
            if (intersection.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(intersection.get());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos;
            }
        }
        return nearest;
    }

    private BlockPos findZoomFocus(double mouseX, double mouseY) {
        if (minecraft.level == null || cameraAnchor == null || width <= 0 || height <= 0) {
            return null;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        float normalizedX = (float) mouseX / width * 2.0F - 1.0F;
        float normalizedY = 1.0F - (float) mouseY / height * 2.0F;
        Vec3 direction = camera.getNearPlane(camera.getFov()).getPointOnPlane(normalizedX, normalizedY).normalize();
        Vec3 start = camera.position();
        List<BlockPos> candidates = new ArrayList<>(suitableBlockPositions);
        List<BlockPos> selectable = pinnedTool == PlannerTool.PRIORITY ? selectedWorkBlocks() : positions(selection);
        for (BlockPos selected : selectable) {
            if (!candidates.contains(selected)) {
                candidates.add(selected);
            }
        }
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            Vec3 offset = highlightBounds(pos).getCenter().subtract(start);
            double forward = offset.dot(direction);
            if (forward <= 0.0D || forward > baseRadius + 5.0D) {
                continue;
            }
            double perpendicular = Math.sqrt(Math.max(0.0D, offset.lengthSqr() - forward * forward));
            double forgivingRadius = Math.max(1.5D, forward * 0.115D);
            if (perpendicular > forgivingRadius) {
                continue;
            }
            double score = perpendicular / forgivingRadius + forward * 0.001D;
            if (score < bestScore) {
                bestScore = score;
                best = pos;
            }
        }
        return best;
    }

    private void focusCameraOn(BlockPos focus) {
        cameraFocusPos = focus.immutable();
        cameraFocusBlend = 0.0F;
        orbitDistance = Math.min(orbitDistance, 5.0F);
        cameraVelocity = cameraVelocity.scale(0.35D);
        feedback("Focusing " + blockName(focus) + " at " + compactPos(focus));
    }

    private void releaseCameraFocus(String message) {
        cameraFocusPos = null;
        cameraFocusBlend = 0.0F;
        cameraVelocity = cameraVelocity.scale(0.55D);
        feedback(message);
    }

    private AABB highlightBounds(BlockPos pos) {
        AABB bounds = blockBounds(pos);
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.is(ModBlocks.TURBINE_PART.get())) {
            BlockPos master = TurbinePartBlock.masterPos(pos, state);
            if (TurbinePartBlock.isExpectedMaster(minecraft.level, master, state)) {
                pos = master;
                state = minecraft.level.getBlockState(master);
                bounds = blockBounds(master);
            }
        }
        if (state.is(ModBlocks.WIND_TURBINE.get()) || state.is(ModBlocks.WATER_TURBINE.get())) {
            for (BlockPos part : TurbineBlock.structurePositions(pos, state)) bounds = bounds.minmax(blockBounds(part));
        }
        BlockPos connected = connectedChestPosition(pos);
        return connected == null ? bounds : bounds.minmax(blockBounds(connected));
    }

    private AABB blockBounds(BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        VoxelShape shape = state.getShape(minecraft.level, pos);
        return (shape.isEmpty() ? Shapes.block().bounds() : shape.bounds()).move(pos);
    }

    private boolean insideBase(BlockPos pos) {
        return pos.distSqr(tablePos) <= (double)baseRadius * baseRadius;
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
        if (cameraFocusPos == null || minecraft == null || minecraft.level == null) {
            return table;
        }
        Vec3 focused = highlightBounds(cameraFocusPos).getCenter().add(0.0D, 0.15D, 0.0D);
        float eased = cameraFocusBlend * cameraFocusBlend * (3.0F - 2.0F * cameraFocusBlend);
        return table.lerp(focused, eased);
    }

    private void faceTableImmediately() {
        Vec3 target = cameraTarget();
        Vec3 delta = target.subtract(cameraPosition);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        cameraYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        cameraPitch = (float) Math.toDegrees(Math.atan2(-delta.y, horizontal));
    }

    private void applyCameraTransform() {
        if (cameraAnchor == null) {
            return;
        }
        cameraAnchor.snapTo(
                cameraPosition.x,
                cameraPosition.y - cameraAnchor.getEyeHeight(),
                cameraPosition.z,
                cameraYaw,
                cameraPitch
        );
        cameraAnchor.setYHeadRot(cameraYaw);
        cameraAnchor.setYBodyRot(cameraYaw);
    }

    private void updateCameraMotion() {
        if (cameraAnchor == null || minecraft.level == null) {
            return;
        }
        long now = Util.getNanos();
        double deltaTime = Mth.clamp((now - lastCameraFrame) / 1_000_000_000.0D, 0.001D, 0.05D);
        lastCameraFrame = now;
        float focusTarget = cameraFocusPos == null ? 0.0F : 1.0F;
        float focusResponse = 1.0F - (float) Math.exp(-6.5D * deltaTime);
        cameraFocusBlend = Mth.lerp(focusResponse, cameraFocusBlend, focusTarget);
        Vec3 desired = desiredCameraPosition();
        Vec3 acceleration = desired.subtract(cameraPosition).scale(30.0D).subtract(cameraVelocity.scale(9.5D));
        cameraVelocity = cameraVelocity.add(acceleration.scale(deltaTime));
        cameraPosition = cameraPosition.add(cameraVelocity.scale(deltaTime));

        Vec3 target = cameraTarget();
        Vec3 delta = target.subtract(cameraPosition);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float targetPitch = (float) Math.toDegrees(Math.atan2(-delta.y, horizontal));
        float yawError = Mth.wrapDegrees(targetYaw - cameraYaw);
        float pitchError = targetPitch - cameraPitch;
        cameraYawVelocity += (yawError * 34.0F - cameraYawVelocity * 10.5F) * (float) deltaTime;
        cameraPitchVelocity += (pitchError * 34.0F - cameraPitchVelocity * 10.5F) * (float) deltaTime;
        cameraYaw = Mth.wrapDegrees(cameraYaw + cameraYawVelocity * (float) deltaTime);
        cameraPitch = Mth.clamp(cameraPitch + cameraPitchVelocity * (float) deltaTime, -89.0F, 89.0F);
        applyCameraTransform();
    }

    private String jobDescription() {
        return switch (jobIndex) {
            case 0 -> "Move selected items between storage.";
            case 1 -> "Tend selected heat stations; transporters move their materials.";
            case 2 -> "Operate selected Primeval wind and water turbines.";
            case 3 -> "Fetch recipe ingredients and craft at normal crafting tables.";
            case 4 -> "Leave on a timed expedition; transporters collect the reward.";
            default -> "Build a reliable work rule.";
        };
    }

    private String selectionLabel(Selection option) {
        return switch (jobIndex) {
            case 0 -> switch (option) {
                case SOURCE -> "PICKUP";
                case DESTINATION -> "DROP-OFF";
                case FALLBACK -> "OVERFLOW";
                default -> option.label;
            };
            case 1 -> switch (option) {
                case SOURCE -> "FOOD / FUEL";
                case WORKSTATION -> "HEAT STATION";
                case DESTINATION -> "OUTPUT";
                case FALLBACK -> "OVERFLOW";
                default -> option.label;
            };
            case 2 -> switch (option) {
                case WORKSTATION -> "TURBINE";
                case DESTINATION -> "POWER TARGET";
                case FALLBACK -> "BACKUP TURBINE";
                default -> option.label;
            };
            case 3 -> switch (option) {
                case SOURCE -> "INPUT";
                case WORKSTATION -> "CRAFTER";
                case DESTINATION -> "OUTPUT";
                case FALLBACK -> "OVERFLOW";
                default -> option.label;
            };
            case 4 -> switch (option) {
                case AREA_START -> "CORNER A";
                case AREA_END -> "CORNER B";
                case DESTINATION -> "DROP-OFF";
                case FALLBACK -> "OVERFLOW";
                default -> option.label;
            };
            default -> option.label;
        };
    }

    private String selectionInstruction(Selection option) {
        return switch (jobIndex) {
            case 0 -> option == Selection.SOURCE ? "CLICK A SOURCE" : option == Selection.DESTINATION ? "CLICK A DROP-OFF" : "CLICK OVERFLOW STORAGE";
            case 1 -> option == Selection.SOURCE ? "CLICK INPUT STORAGE" : option == Selection.WORKSTATION ? "CLICK A FURNACE OR PROCESSOR" : option == Selection.DESTINATION ? "CLICK OUTPUT STORAGE" : "CLICK OVERFLOW STORAGE";
            case 2 -> option == Selection.WORKSTATION ? "CLICK A WIND OR WATER TURBINE" : option == Selection.DESTINATION ? "CLICK A POWER TARGET" : "CLICK A BACKUP TURBINE";
            case 3 -> option == Selection.SOURCE ? "CLICK INPUT STORAGE" : option == Selection.WORKSTATION ? "CLICK A CRAFTER" : option == Selection.DESTINATION ? "CLICK OUTPUT STORAGE" : "CLICK OVERFLOW STORAGE";
            case 4 -> option == Selection.AREA_START ? "CLICK FIRST CORNER" : option == Selection.AREA_END ? "CLICK SECOND CORNER" : option == Selection.DESTINATION ? "CLICK A DROP-OFF" : "CLICK OVERFLOW STORAGE";
            default -> option.instruction.toUpperCase();
        };
    }

    private Layout layout() {
        int margin = 12;
        int topWidth = Math.min(width - margin * 2, 680);
        Rect top = new Rect((width - topWidth) / 2, margin, topWidth, 36);
        int sideTop = top.bottom() + 8;
        int sideBottom = height - 58;
        int leftWidth = Mth.clamp(width / 6, 166, 196);
        int rightWidth = Mth.clamp(width / 5, 210, 246);
        int leftHeight = Math.min(sideBottom - sideTop, 91 + selections().length * 36);
        int rightHeight = Math.min(sideBottom - sideTop, 211);
        Rect left = new Rect(margin, sideTop, leftWidth, leftHeight);
        Rect right = new Rect(width - margin - rightWidth, sideTop, rightWidth, rightHeight);
        int bottomWidth = Math.min(width - margin * 2, 680);
        Rect bottom = new Rect((width - bottomWidth) / 2, height - 50, bottomWidth, 38);
        Rect cancel = new Rect(bottom.right() - 236, bottom.y + 6, 86, 26);
        Rect apply = new Rect(bottom.right() - 142, bottom.y + 6, 132, 26);
        Rect readout = new Rect((width - 254) / 2, top.bottom() + 13, 254, 35);
        return new Layout(top, left, right, bottom, cancel, apply, readout);
    }

    private Rect worldViewport() {
        return new Rect(0, 0, width, height);
    }

    private Rect selectionRect(Rect panel, int index) {
        return new Rect(panel.x + 8, panel.y + 41 + index * 36, panel.width - 16, 31);
    }

    private Rect ruleRect(Rect panel, int index) {
        return new Rect(panel.x + 8, panel.y + 59 + index * 25, panel.width - 16, 21);
    }

    private Rect routeTab(Rect panel) {
        return new Rect(panel.x + 8, panel.y + 35, (panel.width - 19) / 2, 19);
    }

    private Rect stockTab(Rect panel) {
        Rect route = routeTab(panel);
        return new Rect(route.right() + 3, route.y, panel.right() - 8 - route.right() - 3, 19);
    }

    private Rect anyItemRect(Rect panel) {
        return new Rect(panel.x + 9, ruleRect(panel, 3).bottom() + 23, 34, 23);
    }

    private Rect filterSlot(Rect panel, int index) {
        Rect any = anyItemRect(panel);
        return new Rect(any.right() + 4 + index * 24, any.y, 22, 22);
    }

    private void drawSpaceBubble(GuiGraphicsExtractor graphics, Rect rect) {
        int border = 2;
        int middleWidth = Math.max(0, rect.width - border * 2);
        int middleHeight = Math.max(0, rect.height - border * 2);

        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x, rect.y, border, border), 0, 0, 2, 2, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x + border, rect.y, middleWidth, border), 2, 0, 82, 2, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.right() - border, rect.y, border, border), 84, 0, 2, 2, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x, rect.y + border, border, middleHeight), 0, 2, 2, 10, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x + border, rect.y + border, middleWidth, middleHeight), 2, 2, 82, 10, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.right() - border, rect.y + border, border, middleHeight), 84, 2, 2, 10, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x, rect.bottom() - border, border, border), 0, 12, 2, 2, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.x + border, rect.bottom() - border, middleWidth, border), 2, 12, 82, 2, 86, 14);
        blitRegion(graphics, SPACE_TEXTURE, new Rect(rect.right() - border, rect.bottom() - border, border, border), 84, 12, 2, 2, 86, 14);
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
        if (target.width <= 0 || target.height <= 0) {
            return;
        }
        graphics.blit(
                texture,
                target.x,
                target.y,
                target.right(),
                target.bottom(),
                sourceX / (float) textureWidth,
                (sourceX + sourceWidth) / (float) textureWidth,
                sourceY / (float) textureHeight,
                (sourceY + sourceHeight) / (float) textureHeight
        );
    }

    private void drawTintedTexture(GuiGraphicsExtractor graphics, Identifier texture, Rect rect, int color) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                rect.x,
                rect.y,
                0.0F,
                0.0F,
                rect.width,
                rect.height,
                rect.width,
                rect.height,
                color
        );
    }

    private void drawPlannerRegion(GuiGraphicsExtractor graphics, Rect canvas, Rect clip, int alpha) {
        if (clip.width <= 0 || clip.height <= 0) {
            return;
        }
        graphics.enableScissor(clip.x, clip.y, clip.right(), clip.bottom());
        drawTintedTexture(graphics, PLANNER_TEXTURE, canvas, withAlpha(0xFFFFFFFF, alpha));
        graphics.disableScissor();
    }

    private void drawItemInSlot(GuiGraphicsExtractor graphics, ItemStack stack, Rect slot) {
        float scale = Math.min(1.0F, Math.max(0.45F, (Math.min(slot.width, slot.height) - 2.0F) / 16.0F));
        float renderedSize = 16.0F * scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.centerX() - renderedSize / 2.0F, slot.centerY() - renderedSize / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void drawDockItem(GuiGraphicsExtractor graphics, ItemStack stack, Rect slot) {
        float scale = Math.max(0.45F, Math.min(slot.width, slot.height) / 18.0F);
        float renderedSize = 16.0F * scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.centerX() - renderedSize / 2.0F, slot.centerY() - renderedSize / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void drawLeftItem(GuiGraphicsExtractor graphics, ItemStack stack, Rect slot) {
        float scale = Math.max(0.55F, plannerScale());
        float renderedSize = 16.0F * scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.centerX() - renderedSize / 2.0F, slot.centerY() - renderedSize / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void drawSearchItem(GuiGraphicsExtractor graphics, ItemStack stack, Rect slot) {
        float scale = Math.max(0.55F, Math.min(slot.width, slot.height) / 20.0F);
        float renderedSize = 16.0F * scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.centerX() - renderedSize / 2.0F, slot.centerY() - renderedSize / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void panel(GuiGraphicsExtractor graphics, Rect rect, int accent) {
        graphics.fill(rect.x + 3, rect.y + 4, rect.right() + 3, rect.bottom() + 4, 0x62000000);
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), PANEL);
        graphics.fill(rect.x, rect.y, rect.right(), rect.y + 32, PANEL_DARK);
        outline(graphics, rect, EDGE);
        graphics.fill(rect.x + 1, rect.y + 1, rect.x + 6, rect.y + 31, accent);
        graphics.fill(rect.x + 7, rect.y + 31, rect.right() - 1, rect.y + 32, 0x8F6E4E3C);
    }

    private void outline(GuiGraphicsExtractor graphics, Rect rect, int color) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.y + 1, color);
        graphics.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x, rect.y, rect.x + 1, rect.bottom(), color);
        graphics.fill(rect.right() - 1, rect.y, rect.right(), rect.bottom(), color);
    }

    private void drawButton(GuiGraphicsExtractor graphics, Rect rect, String label, boolean primary, int mouseX, int mouseY) {
        boolean hovered = rect.contains(mouseX, mouseY);
        int background = primary ? hovered ? 0xFFF0A451 : 0xFFE18B39 : hovered ? 0xFFE3C39E : 0xFFC9A680;
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), background);
        outline(graphics, rect, primary ? CORAL : EDGE);
        centered(graphics, label, rect.centerX(), rect.y + 9, primary ? 0xFF382923 : INK);
    }

    private void drawSmallButton(GuiGraphicsExtractor graphics, Rect rect, String label, boolean selected, int mouseX, int mouseY) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), selected ? 0xFFF0C875 : rect.contains(mouseX, mouseY) ? 0xFFE2C6A3 : 0xFFB89070);
        outline(graphics, rect, selected ? AMBER : EDGE);
        centered(graphics, label, rect.centerX(), rect.y + 7, INK);
    }

    private void boldCenteredFit(GuiGraphicsExtractor graphics, String value, Rect rect, int color) {
        Component component = boldComponent(value);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(1.0F, Math.max(0.01F, (rect.width - 4.0F) / measuredWidth));
        float x = rect.centerX() - measuredWidth * scale / 2.0F;
        float y = rect.y + (rect.height - font().lineHeight * scale) / 2.0F;
        drawScaledComponent(graphics, component, x, y, scale, color);
    }

    private void boldLine(
            GuiGraphicsExtractor graphics,
            String value,
            int x,
            int y,
            int maximumWidth,
            int color,
            float preferredScale
    ) {
        Component component = boldComponent(value);
        int measuredWidth = Math.max(1, font().width(component));
        float scale = Math.min(preferredScale, maximumWidth / (float) measuredWidth);
        drawScaledComponent(graphics, component, x, y, Math.max(0.01F, scale), color);
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

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        graphics.text(font(), boldComponent(fit(value, Math.max(0, width - x - 8))), x, y, color, true);
    }

    private void textFit(GuiGraphicsExtractor graphics, String value, int x, int y, int maximumWidth, int color) {
        graphics.text(font(), boldComponent(fit(value, maximumWidth)), x, y, color, true);
    }

    private void textRight(GuiGraphicsExtractor graphics, String value, int right, int y, int color) {
        Component text = boldComponent(value);
        graphics.text(font(), text, right - font().width(text), y, color, true);
    }

    private void centered(GuiGraphicsExtractor graphics, String value, int centerX, int y, int color) {
        graphics.centeredText(font(), boldComponent(value), centerX, y, color);
    }

    private void centeredFit(GuiGraphicsExtractor graphics, String value, Rect rect, int color) {
        graphics.centeredText(font(), boldComponent(fit(value, rect.width - 8)), rect.centerX(), rect.y + 3, color);
    }

    private Component boldComponent(String value) {
        return Component.literal(value).withStyle(style -> style.withBold(true));
    }

    private String fit(String value, int maximumWidth) {
        if (font().width(value) <= maximumWidth) {
            return value;
        }
        return font().plainSubstrByWidth(value, Math.max(0, maximumWidth - font().width("..."))) + "...";
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private Font font() {
        return minecraft.font;
    }

    private void feedback(String message) {
        feedback = message;
        feedbackUntilNanos = Util.getNanos() + 2_750_000_000L;
    }

    private String displayName() {
        return dodo.hasCustomName() ? dodo.getDisplayName().getString().toUpperCase() : "MOSS";
    }

    private static String compactPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String compactPosTight(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos firstPosition(List<BlockPos> positions) {
        return positions.isEmpty() ? null : positions.getFirst();
    }

    private static String shortIdentifier(String value) {
        int separator = value.indexOf(':');
        return (separator >= 0 ? value.substring(separator + 1) : value).replace('_', ' ').toUpperCase();
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private static float spring(float value) {
        if (value >= 1.0F) {
            return 1.0F;
        }
        double damping = 5.6D;
        double frequency = Math.PI * 2.5D;
        double wave = Math.cos(value * frequency)
                + damping / frequency * Math.sin(value * frequency);
        return 1.0F - (float) (Math.exp(-value * damping) * wave);
    }

    private static float popupSpring(float progress, float damping, float frequency) {
        if (progress >= 1.0F) {
            return 1.0F;
        }
        double wave = Math.cos(frequency * progress)
                + damping / frequency * Math.sin(frequency * progress);
        return 1.0F - (float) (Math.exp(-damping * progress) * wave);
    }

    public static void submitWorldGeometry(SubmitCustomGeometryEvent event) {
        WorksitePlannerScreen screen = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (screen == null || minecraft.level == null) {
            return;
        }
        SubmitNodeCollector submits = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        screen.renderConnectedHighlight(pose, submits, screen.tablePos, 0xFFF09A3D, 2.0F, camera);
        int suitableColor = 0xA6FFF4DF;
        screen.suitableBlockPositions.forEach(pos -> screen.renderConnectedHighlight(pose, submits, pos, suitableColor, 2.0F, camera));
        screen.matchingBlockPositions.forEach(pos -> screen.renderConnectedHighlight(pose, submits, pos, 0xEDFFFFFF, 3.0F, camera));
        screen.sourcePositions.forEach(pos -> screen.renderSelectedHighlight(pose, submits, pos, 0xFFFFA64D, 3.0F, camera));
        screen.workstationPositions.forEach(pos -> screen.renderSelectedHighlight(pose, submits, pos, 0xFF63C2D1, 3.0F, camera));
        screen.destinationPositions.forEach(pos -> screen.renderSelectedHighlight(pose, submits, pos, 0xFF77D76A, 3.0F, camera));
        screen.renderSelectedHighlight(pose, submits, screen.areaEndPos, 0xFFB487D6, 3.0F, camera);
        screen.fallbackPositions.forEach(pos -> screen.renderSelectedHighlight(pose, submits, pos, 0xFFE26048, 2.0F, camera));
        if (screen.hoveredPos != null) {
            int hoverColor = screen.pinnedTool == PlannerTool.PRIORITY && screen.isSelectedWorkBlock(screen.hoveredPos)
                    ? priorityColor(screen.blockPriority(screen.hoveredPos))
                    : screen.validForSelection(screen.selection, screen.hoveredPos) ? screen.selection.color : 0xFFE26048;
            screen.renderConnectedHighlight(pose, submits, screen.hoveredPos, hoverColor, 4.0F, camera);
        }
        if (screen.cameraFocusPos != null) {
            float pulse = (Mth.sin(screen.renderTimeTicks * 0.24F) + 1.0F) * 0.5F;
            int alpha = 210 + Math.round(pulse * 45.0F);
            screen.renderConnectedHighlight(pose, submits, screen.cameraFocusPos,
                    withAlpha(0xFFFFFFFF, alpha), 4.5F + pulse, camera);
        }
    }

    private void renderSelectedHighlight(
            PoseStack pose,
            SubmitNodeCollector submits,
            BlockPos pos,
            int roleColor,
            float width,
            Vec3 camera
    ) {
        if (pos == null) {
            return;
        }
        boolean priorityMode = pinnedTool == PlannerTool.PRIORITY;
        int color = priorityMode ? priorityColor(blockPriority(pos)) : roleColor;
        float priorityWidth = pos.equals(priorityTargetPos) ? 5.0F : priorityMode ? width + 0.75F : width;
        renderConnectedHighlight(pose, submits, pos, color, priorityWidth, camera);
    }

    private void renderConnectedHighlight(
            PoseStack pose,
            SubmitNodeCollector submits,
            BlockPos pos,
            int color,
            float width,
            Vec3 camera
    ) {
        if (pos == null) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.is(ModBlocks.TURBINE_PART.get())) {
            BlockPos master = TurbinePartBlock.masterPos(pos, state);
            if (TurbinePartBlock.isExpectedMaster(minecraft.level, master, state)) {
                pos = master;
                state = minecraft.level.getBlockState(master);
            }
        }
        if (state.is(ModBlocks.WIND_TURBINE.get()) || state.is(ModBlocks.WATER_TURBINE.get())) {
            for (BlockPos part : TurbineBlock.structurePositions(pos, state)) {
                renderHighlight(pose, submits, part, color, width, camera);
            }
            return;
        }
        renderHighlight(pose, submits, pos, color, width, camera);
        BlockPos connected = connectedChestPosition(pos);
        if (connected != null) {
            renderHighlight(pose, submits, connected, color, width, camera);
        }
    }

    private void renderHighlight(PoseStack pose, SubmitNodeCollector submits, BlockPos pos, int color, float width, Vec3 camera) {
        if (pos == null) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(pos);
        VoxelShape shape = state.getShape(minecraft.level, pos);
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }
        VoxelShape submittedShape = shape;
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;
        submits.submitCustomGeometry(pose, XRAY_HIGHLIGHT_TYPE,
                (matrix, vertices) -> renderShape(matrix, vertices, submittedShape, x, y, z, color, width));
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

    private enum PlannerTool {
        FILTER(Items.HOPPER.getDefaultInstance()),
        BATCH(Items.BUNDLE.getDefaultInstance()),
        RESERVE(Items.CHEST.getDefaultInstance()),
        TARGET(Items.TARGET.getDefaultInstance()),
        PRIORITY(Items.REDSTONE_TORCH.getDefaultInstance()),
        SCHEDULE(Items.CLOCK.getDefaultInstance()),
        REPEAT(Items.REPEATER.getDefaultInstance()),
        ROUTE(Items.COMPASS.getDefaultInstance()),
        SAFETY(Items.SHIELD.getDefaultInstance()),
        MATCH(Items.COMPARATOR.getDefaultInstance()),
        SAVE(Items.LIME_DYE.getDefaultInstance()),
        CANCEL(Items.BARRIER.getDefaultInstance());

        private final ItemStack icon;

        PlannerTool(ItemStack icon) {
            this.icon = icon;
        }
    }

    private enum Selection {
        SOURCE("SOURCE", "Click the source container.", "Choose container", Items.CHEST.getDefaultInstance(), AMBER),
        WORKSTATION("WORKSTATION", "Click the machine this dino should operate.", "Choose machine", Items.CRAFTING_TABLE.getDefaultInstance(), CYAN),
        DESTINATION("DESTINATION", "Click the output or drop-off container.", "Choose drop-off", Items.BARREL.getDefaultInstance(), GREEN),
        AREA_START("AREA CORNER A", "Click the first corner of the work area.", "Choose first corner", Items.WOODEN_HOE.getDefaultInstance(), AMBER),
        AREA_END("AREA CORNER B", "Click the opposite corner of the work area.", "Choose opposite corner", Items.MAP.getDefaultInstance(), VIOLET),
        FALLBACK("OVERFLOW", "Optionally choose overflow storage or a spare station.", "Optional overflow", Items.COMPASS.getDefaultInstance(), CORAL),
        EXPEDITION_ONE("SAFE FORAGE", "Choose the safest ten-minute expedition.", "10 min / no risk", Items.MAP.getDefaultInstance(), GREEN),
        EXPEDITION_TWO("RIDGE TRAIL", "Choose a modest fifteen-minute expedition.", "15 min / low risk", Items.COMPASS.getDefaultInstance(), CYAN),
        EXPEDITION_THREE("DEEP WILDS", "Choose a rewarding twenty-minute expedition.", "20 min / medium risk", Items.SPYGLASS.getDefaultInstance(), AMBER),
        EXPEDITION_FOUR("PREDATOR RUN", "Choose a dangerous twenty-five-minute expedition.", "25 min / high risk", Items.RECOVERY_COMPASS.getDefaultInstance(), CORAL),
        EXPEDITION_FIVE("PRIMORDIAL FRONTIER", "Choose the most dangerous thirty-minute expedition.", "30 min / extreme reward", Items.ENDER_EYE.getDefaultInstance(), VIOLET);

        private final String label;
        private final String instruction;
        private final String shortHint;
        private final ItemStack icon;
        private final int color;

        Selection(String label, String instruction, String shortHint, ItemStack icon, int color) {
            this.label = label;
            this.instruction = instruction;
            this.shortHint = shortHint;
            this.icon = icon;
            this.color = color;
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

    private record Layout(Rect top, Rect left, Rect right, Rect bottom, Rect cancel, Rect apply, Rect worldReadout) {
    }

    private record TopDrawerLayout(
            Rect canvas,
            Rect clip,
            Rect identity,
            Rect step,
            Rect carousel,
            Rect previousJob,
            Rect jobName,
            Rect nextJob,
            int visibleBottom
    ) {
    }

    private record LeftDrawerLayout(
            Rect canvas,
            Rect clip,
            Rect rail,
            Rect[] slots,
            Rect progress,
            float scale
    ) {
    }

    private record SpecialtyDockLayout(
            Rect rightCanvas,
            Rect bottomCanvas,
            Rect rightRail,
            Rect bottomRail,
            PlannerTool[] tools,
            Rect[] slots,
            int rightOffset,
            int bottomOffset
    ) {
    }

    private record HelpPanelLayout(
            Rect canvas,
            Rect outer,
            Rect title,
            Rect body,
            Rect filterTarget,
            Rect anyItem
    ) {
    }

    private record HelpContent(String title, List<String> lines, int accent) {
    }

    private record HelpOption(String label, String description, int color, boolean selected) {
    }

    private record SearchLayout(
            Rect canvas,
            Rect slotFrame,
            Rect button,
            Rect bar,
            Rect results,
            Rect[] itemSlots
    ) {
    }

    private record BaseItemEntry(ItemStack stack, int totalCount, List<BlockPos> locations) {
    }

    private static final class BaseItemAccumulator {
        private final ItemStack stack;
        private final List<BlockPos> locations = new ArrayList<>();
        private int totalCount;

        private BaseItemAccumulator(ItemStack stack) {
            this.stack = stack;
        }
    }

}
