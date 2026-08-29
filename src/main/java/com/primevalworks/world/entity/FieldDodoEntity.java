package com.primevalworks.world.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import com.primevalworks.world.work.PriorityRouting;
import com.primevalworks.world.work.DinoSpeciesWorkProfile;
import com.primevalworks.world.work.WorkSpecialtyRules;
import com.primevalworks.world.work.ExpeditionRewards;
import com.primevalworks.world.work.DinosaurCommandMode;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.config.PrimevalConfig;
import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.world.work.BaseInventoryIndex;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.egg.DinosaurEggSize;
import com.primevalworks.world.progression.PrimevalAdvancements;
import com.primevalworks.world.breeding.DinosaurBreeding;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModItems;
import com.primevalworks.registry.ModItemTags;
import com.primevalworks.world.block.entity.FoodBoxBlockEntity;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.block.entity.AncientFurnaceBlockEntity;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.block.TurbineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.EnumSet;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class FieldDodoEntity extends PathfinderMob implements GeoEntity {
    public static final int MUTATION_HUGE = DinosaurMutationRules.HUGE;
    public static final int MUTATION_ALBINO = DinosaurMutationRules.ALBINO;
    public static final int MUTATION_SCHEMA = 2;
    private static final int MUTATION_MASK_ALLOWED = MUTATION_HUGE | MUTATION_ALBINO;
    private static final int WORK_STATE_SCHEMA = 4;
    private static final int OWNERSHIP_SYNC_INTERVAL_TICKS = 100;
    private static final int FOOD_BOX_BASE_FOOD_VALUE = 28;
    private static final double T_REX_MOUTH_REACH = 2.50D;
    private static final float T_REX_ATTACK_ARC_DEGREES = 34.0F;
    private static final int T_REX_DAMAGE_DELAY_TICKS = 9;
    private static final double SPINO_MOUTH_REACH = 4.35D;
    private static final float SPINO_ATTACK_ARC_DEGREES = 38.0F;
    private static final int SPINO_DAMAGE_DELAY_TICKS = 5;
    private static final int DEFEAT_TRANSFER_TICKS = 22;
    private static final int PTERO_FLIGHT_GROUNDED = 0;
    private static final int PTERO_FLIGHT_HOVERING = 1;
    private static final int PTERO_FLIGHT_POWERED = 2;
    private static final int PTERO_FLIGHT_GLIDING = 3;
    private static final double PTERO_CRUISE_SPEED = 0.94D;
    private static final double PTERO_BOOST_SPEED = 1.72D;
    private static final double PTERO_GLIDE_ENTRY_SPEED = 0.26D;
    private static final double PTERO_GLIDE_EXIT_SPEED = 0.13D;
    private static final float PTERO_MAX_STAMINA = 100.0F;
    private static final float PTERO_MIN_TAKEOFF_STAMINA = 12.0F;
    private static final float PTERO_EXHAUSTION_RECOVERY = 20.0F;
    private static final float PTERO_GROUND_STAMINA_RECOVERY = 0.45F;
    private static final float PTERO_GLIDE_STAMINA_RECOVERY = 0.07F;
    private static final double SPINO_CRUISE_SPEED = 1.05D;
    private static final double SPINO_MAX_SWIM_SPEED = 1.62D;
    private static final int SPINO_BREACH_TICKS = 62;
    private static final double SPINO_SEAT_HEIGHT = 68.5D / 16.0D;
    private static final double SPINO_SEAT_FORWARD = 18.5D / 16.0D;
    private static final double SPINO_RIDER_VERTICAL_ADJUSTMENT = -3.0D / 16.0D;
    private static final float SPINO_MOUNTED_STEP_HEIGHT = 2.05F;
    private static final float SPINO_LAND_SPRINT_MULTIPLIER = 1.58F;
    private static final float SPINO_MAX_LAND_STAMINA = 100.0F;
    private static final float SPINO_LAND_STAMINA_DRAIN = 0.30F;
    private static final float SPINO_LAND_STAMINA_RECOVERY = 0.34F;
    private static final float SPINO_LAND_EXHAUSTION_RECOVERY = 18.0F;
    private static final int SPINO_MOUNTED_ATTACK_COOLDOWN_TICKS = 22;
    private static final int RAPTOR_ATTACK_ANIMATION_TICKS = 12;
    private static final int RAPTOR_DAMAGE_DELAY_TICKS = 7;
    private static final RawAnimation DODO_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DODO_WALK = RawAnimation.begin().thenLoop("Walk");
    private static final RawAnimation DODO_RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation DODO_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation DODO_SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation T_REX_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation T_REX_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation T_REX_RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation T_REX_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation T_REX_SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation T_REX_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation STEGO_IDLE = RawAnimation.begin().thenLoop("Idle");
    private static final RawAnimation STEGO_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation STEGO_RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation STEGO_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation STEGO_SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation PTERO_IDLE = RawAnimation.begin().thenLoop("Idle(Ground)");
    private static final RawAnimation PTERO_AIR_IDLE = RawAnimation.begin().thenLoop("Idle(Air)");
    private static final RawAnimation PTERO_WALK = RawAnimation.begin().thenLoop("Walking");
    private static final RawAnimation PTERO_FLY = RawAnimation.begin().thenLoop("Flying");
    private static final RawAnimation PTERO_GLIDE = RawAnimation.begin().thenLoop("gliding");
    private static final RawAnimation PTERO_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation PARASAUR_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation PARASAUR_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation PARASAUR_RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation PARASAUR_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation PARASAUR_SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation RAPTOR_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RAPTOR_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RAPTOR_RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation RAPTOR_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation RAPTOR_SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation RAPTOR_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation SPINO_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPINO_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SPINO_WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation SPINO_SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation SPINO_SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation SPINO_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation PLACEHOLDER_IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation PLACEHOLDER_WALK = RawAnimation.begin().thenLoop("move.walk");
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<ItemStack> CARRIED_STACK = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.ITEM_STACK
    );
    private static final EntityDataAccessor<Integer> MOOD = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> INDICATOR_ICON = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> INDICATOR_TICKS = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> INDICATOR_AGE = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> EYES_CLOSED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DINOSAUR_SLEEPING = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> PTERO_FLIGHT_MODE = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Float> PTERO_BANK = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> PTERO_AIRSPEED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> PTERO_STAMINA = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Boolean> PTERO_EXHAUSTED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> SPINO_SWIMMING = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Float> SPINO_BANK = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> SPINO_SWIM_SPEED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Integer> SPINO_BREACH_TIME = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> SPINO_LAND_SPRINTING = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Float> SPINO_LAND_STAMINA = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Boolean> SPINO_LAND_EXHAUSTED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> DEFEAT_TRANSFER_TIME = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_TICKS = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> RAPTOR_RUNNING = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> GENETIC_QUALITY = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> MUTATION_MASK = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> ORIGINAL_PIGMENT_RESTORED = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> HUE_VARIANT = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> WORK_ACTION = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> WORK_JOB_INDEX = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> WORK_ACTION_PROGRESS = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> WORK_ACTION_DURATION = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Optional<BlockPos>> WORK_ACTION_POS = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private static final EntityDataAccessor<Optional<BlockPos>> COMMAND_TABLE_POS = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private static final EntityDataAccessor<Integer> COMMAND_MODE = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> FIELD_WORK_MODE = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> FIELD_WORK_PATTERN = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Optional<BlockPos>> FIELD_WORK_FIRST = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private static final EntityDataAccessor<Optional<BlockPos>> FIELD_WORK_SECOND = SynchedEntityData.defineId(
            FieldDodoEntity.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int workJobIndex;
    private @Nullable UUID dinosaurOwner;
    private boolean worldAuthorityChecked;
    private int dinosaurLevel = 1;
    private int dinosaurExperience;
    private BlockPos commandTablePos;
    private BlockPos workSourcePos;
    private BlockPos workWorkstationPos;
    private BlockPos workDestinationPos;
    private BlockPos workAreaEndPos;
    private BlockPos workFallbackPos;
    private final List<BlockPos> workSourcePositions = new ArrayList<>();
    private final List<BlockPos> workWorkstationPositions = new ArrayList<>();
    private final List<BlockPos> workDestinationPositions = new ArrayList<>();
    private final List<BlockPos> workFallbackPositions = new ArrayList<>();
    private final Map<BlockPos, Integer> workBlockPriorities = new LinkedHashMap<>();
    private String workItemFilter = "";
    private final List<String> workItemFilters = new ArrayList<>();
    private final List<String> workFuelFilters = new ArrayList<>();
    private int expeditionTier;
    private int workPriority = 1;
    private int workBatchSize = 16;
    private int workSchedule;
    private int workSourceReserve;
    private int workDestinationTarget;
    private int workRepeatMode;
    private int workRoutePolicy = 1;
    private int workMoodDrainUnits;
    private boolean exactItemMatch = true;
    private boolean avoidDanger = true;
    private boolean workEnabled = true;
    private int workerCooldown;
    private int workWorkstationCursor;
    private DinosaurCommandMode commandMode = DinosaurCommandMode.HOME;
    private @Nullable BlockPos stayPosition;
    private boolean fieldWorkEnabled;
    private DinoWhistleSettings.FieldMode fieldWorkMode = DinoWhistleSettings.FieldMode.QUARRY;
    private DinoWhistleSettings.Pattern fieldWorkPattern = DinoWhistleSettings.Pattern.SINGLE;
    private boolean fieldWorkContinuous;
    private int fieldWorkRange = 48;
    private String fieldWorkItemFilter = "";
    private @Nullable BlockPos fieldWorkFirst;
    private @Nullable BlockPos fieldWorkSecond;
    private final List<BlockPos> fieldWorkTargets = new ArrayList<>();
    private int fieldWorkCursor;
    private int fieldWorkRescanCooldown;
    private int fieldTargetApproachTicks;
    private boolean lumberFastFelling;
    private @Nullable UUID fieldCollectionTargetId;
    private int fieldCollectionApproachTicks;
    private final Map<UUID, Long> fieldCollectionRetryAfter = new HashMap<>();
    private BlockPos workActionPos;
    private Vec3 workLockedPosition;
    private @Nullable CraftingOrder pendingCraftingOrder;
    private boolean onExpedition;
    private long expeditionEndTick;
    private boolean breedingPrimed;
    private long breedingCooldownUntilTick;
    private @Nullable UUID breedingPartnerId;
    private @Nullable UUID breedingOwnerId;
    private int breedingCourtshipTicks;
    private int breedingPartnerMissingTicks;
    private long incapacitatedUntilTick;
    private boolean pendingOwnerRecovery;
    private int runAnimationHoldTicks;
    private int walkAnimationHoldTicks;
    private int turnAnimationHoldTicks;
    private int turnAnimationSettleTicks;
    private int turnAnimationDirection;
    private float turnYawAccumulator;
    private int turnAnimationCooldownTicks;
    private int sleepVisualTicks;
    private int hungerIndicatorCooldown;
    private long nextHungerDrainTick;
    private int calmingCallCacheTicks;
    private boolean calmingCallCached;
    private float calmingCallStrengthCached = 1.0F;
    private float lastPresentationYaw = Float.NaN;
    private float renderedHeadYaw;
    private float renderedHeadPitch;
    private float renderedHeadYawVelocity;
    private float renderedHeadPitchVelocity;
    private double lastHeadRenderAge = Double.NaN;
    private float renderedBodyLead;
    private float renderedBodyLeadVelocity;
    private float renderedTailYaw;
    private float renderedTailYawVelocity;
    private float renderedTailTipYaw;
    private float renderedTailTipYawVelocity;
    private float renderedTurnStep;
    private float renderedTurnStepVelocity;
    private float renderedCargoScale;
    private float renderedCargoScaleVelocity;
    private float renderedLeftFootOffset;
    private float renderedLeftFootOffsetVelocity;
    private float renderedRightFootOffset;
    private float renderedRightFootOffsetVelocity;
    private float lastRenderedBodyYaw = Float.NaN;
    private int blinkTicksRemaining;
    private int ticksUntilBlink = -1;
    private boolean secondBlinkQueued;
    private int pendingAttackTargetId = -1;
    private long pendingAttackContactTick = Long.MAX_VALUE;
    private float raptorMomentum;
    private int raptorPounceTicks;
    private int raptorTransportRunTicks;
    private int raptorRunAnimationHoldTicks;
    private float raptorAnimationSpeed = 0.76F;
    private float raptorAnimationSpeedPrevious = 0.76F;
    private boolean raptorPounceContactConfirmed;
    private BlockPos foodTargetPos;
    private int foodSearchCooldown;
    private BlockPos navigationTarget;
    private @Nullable BlockPos spinosaurusAquaticWorkTarget;
    private double lastNavigationDistance = Double.MAX_VALUE;
    private Vec3 lastNavigationSamplePosition;
    private int stalledNavigationTicks;
    private int recoveryWaypointTicks;
    private boolean ownerCatchupActive;
    private float pteranodonThrottle;
    private int pteranodonForwardHoldTicks;
    private float pteranodonFlightBlend;
    private float pteranodonFlightBlendVelocity;
    private float pteranodonBankPrevious;
    private float pteranodonBankVelocity;
    private float pteranodonControllerYawPrevious = Float.NaN;
    private float pteranodonSteeringVelocity;
    private float pteranodonAnimationSpeed = 0.92F;
    private float pteranodonAnimationSpeedPrevious = 0.92F;
    private double pteranodonRiderFlapPhase;
    private double pteranodonRiderFlapPhasePrevious;
    private float pteranodonRiderFlapBlend;
    private float pteranodonRiderFlapBlendPrevious;
    private float pteranodonRiderRootPitch;
    private float pteranodonRiderRootPitchPrevious;
    private float pteranodonRiderPosture;
    private float pteranodonRiderPosturePrevious;
    private int pteranodonPoweredAnimationHoldTicks;
    private int pteranodonTakeoffChargeTicks;
    private int pteranodonTakeoffGraceTicks;
    private boolean pteranodonClientDescendInput;
    private boolean autonomousTransportFlight;
    private BlockPos autonomousTransportTarget;
    private double autonomousTransportAltitude;
    private float spinosaurusThrottle;
    private float spinosaurusBankPrevious;
    private float spinosaurusBankVelocity;
    private float spinosaurusControllerYawPrevious = Float.NaN;
    private float spinosaurusSteeringVelocity;
    private boolean spinosaurusClientDescendInput;
    private boolean spinosaurusWasInWater;
    private int spinosaurusPredictedBreachTicks;
    private int spinosaurusGroundDropGraceTicks;
    private double spinosaurusGroundDropEntrySpeed;
    private final Set<UUID> spinosaurusBreachHits = new HashSet<>();
    private int spinosaurusMountedAttackCooldown;
    private int spinosaurusMountedAimTicks;
    private float spinosaurusMountedAimYaw;
    private float spinosaurusMountedAimPitch;
    private boolean permanentPlayerKill;

    public FieldDodoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        navigation.getNodeEvaluator().setCanOpenDoors(true);
        navigation.getNodeEvaluator().setCanPassDoors(true);
        navigation.setCanFloat(true);
        setPathfindingMalus(PathType.WATER,
                getSpecies() == DinosaurSpecies.SPINOSAURUS ? 0.0F : 3.0F);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        moveControl = new SizeAwareMoveControl(this);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return getSpecies() == DinosaurSpecies.PTERANODON
                ? super.createNavigation(level)
                : new AmphibiousPathNavigation(this, level);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        initializeGenetics(false);
        return result;
    }

    public void improveGeneticsFromIncubator() {
        if (level().isClientSide()) {
            return;
        }
        initializeGenetics(true);
    }

    public void initializeWildHatch() {
        if (!level().isClientSide()) {
            initializeGenetics(false);
        }
    }

    public void applyIncubatedGenetics(int quality, int mutationMask, int hueVariant) {
        if (level().isClientSide()) {
            return;
        }
        entityData.set(GENETIC_QUALITY, Mth.clamp(quality, 0, 100));
        entityData.set(MUTATION_MASK, mutationMask & MUTATION_MASK_ALLOWED);
        entityData.set(HUE_VARIANT, Mth.clamp(hueVariant, -8, 8));
        applyGeneticAttributes(true);
    }

    public void initializeClientPreview(int quality, int mutationMask, int hueVariant) {
        if (!level().isClientSide()) return;
        entityData.set(GENETIC_QUALITY, Mth.clamp(quality, 0, 100));
        entityData.set(MUTATION_MASK, mutationMask & MUTATION_MASK_ALLOWED);
        entityData.set(HUE_VARIANT, Mth.clamp(hueVariant, -8, 8));
        applyGeneticAttributes(true);
    }

    public void restoreOwnedState(int quality, int mutationMask, int hueVariant, int hunger, int mood,
                                  float health, int level) {
        entityData.set(GENETIC_QUALITY, Mth.clamp(quality, 0, 100));
        entityData.set(MUTATION_MASK, mutationMask & MUTATION_MASK_ALLOWED);
        entityData.set(HUE_VARIANT, Mth.clamp(hueVariant, -8, 8));
        entityData.set(HUNGER, Mth.clamp(hunger, 0, 100));
        entityData.set(MOOD, Mth.clamp(mood, 0, 100));
        dinosaurLevel = Mth.clamp(level, 1, 100);
        applyGeneticAttributes(false);
        setHealth(Mth.clamp(health, 1.0F, getMaxHealth()));
    }

    public void restoreOwnedPreviewState(int quality, int mutationMask, int hueVariant, int hunger, int mood,
                                         float health, int level, boolean originalPigmentRestored) {
        restoreOwnedState(quality, mutationMask, hueVariant, hunger, mood, health, level);
        entityData.set(ORIGINAL_PIGMENT_RESTORED, originalPigmentRestored);
    }

    private void initializeGenetics(boolean incubated) {
        int existingQuality = entityData.get(GENETIC_QUALITY);
        int quality = existingQuality < 0
                ? (random.nextInt(101) + random.nextInt(101)) / 2
                : existingQuality;
        if (incubated) {
            quality = Mth.clamp(Math.max(quality, 62) + 10 + random.nextInt(12), 0, 100);
        }

        int mutations = existingQuality < 0 ? rollMutationMask(incubated) : entityData.get(MUTATION_MASK);
        if (existingQuality < 0) {
            quality = Mth.clamp(quality + DinosaurMutationRules.qualityBonus(mutations, random.nextFloat()), 0, 100);
        }
        entityData.set(GENETIC_QUALITY, quality);
        entityData.set(MUTATION_MASK, mutations);
        if (existingQuality < 0) {
            entityData.set(HUE_VARIANT, random.nextInt(17) - 8);
        }
        applyGeneticAttributes(true);
    }

    private int rollMutationMask(boolean incubated) {
        return DinosaurMutationRules.roll(incubated, random.nextFloat(), random.nextFloat());
    }

    private void applyGeneticAttributes(boolean healToFull) {
        DinosaurSpecies species = getSpecies();
        float previousHealth = getHealth();
        float quality = getGeneticQuality() / 100.0F;
        double attackMultiplier = (0.90D + quality * 0.20D)
                * getMutationStatMultiplier()
                * DinosaurProgression.attackMultiplier(dinosaurLevel)
                * PrimevalTuning.server().dinosaurDamage();
        double movementMultiplier = (0.94D + quality * 0.12D)
                * getMutationStatMultiplier()
                * DinosaurProgression.movementMultiplier(dinosaurLevel);
        getAttribute(Attributes.SCALE).setBaseValue(getGeneticScale());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(expectedMaxHealth(
                species, getGeneticQuality(), getMutationMask(), dinosaurLevel));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(species.baseAttackDamage() * attackMultiplier);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(species.baseMovementSpeed() * movementMultiplier);
        setHealth(healToFull ? getMaxHealth() : Math.min(previousHealth, getMaxHealth()));
        refreshDimensions();
    }

    public static float expectedMaxHealth(DinosaurSpecies species, int quality, int mutationMask, int level) {
        double multiplier = 0.90D + Mth.clamp(quality, 0, 100) * 0.002D;
        if ((mutationMask & MUTATION_HUGE) != 0) multiplier *= PrimevalTuning.server().hugeStats();
        if ((mutationMask & MUTATION_ALBINO) != 0) multiplier *= PrimevalTuning.server().albinoHealth();
        multiplier *= DinosaurProgression.healthMultiplier(level);
        multiplier *= PrimevalTuning.server().dinosaurHealth();
        return (float)(species.baseHealth() * multiplier);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new WeightedBodyRotationControl(this);
    }

    public int getTransportAptitude() {
        return getWorkEfficiencyPercent(0);
    }

    public DinoSpeciesWorkProfile getSpeciesWorkProfile() {
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        return switch (species) {
            case "tyrannosaurus" -> DinoSpeciesWorkProfile.TYRANNOSAURUS;
            case "triceratops" -> DinoSpeciesWorkProfile.TRICERATOPS;
            case "brachiosaurus" -> DinoSpeciesWorkProfile.BRACHIOSAURUS;
            case "dilophosaurus" -> DinoSpeciesWorkProfile.DILOPHOSAURUS;
            case "velociraptor" -> DinoSpeciesWorkProfile.VELOCIRAPTOR;
            case "stegosaurus" -> DinoSpeciesWorkProfile.STEGOSAURUS;
            case "parasaurolophus" -> DinoSpeciesWorkProfile.PARASAUROLOPHUS;
            case "ankylosaurus" -> DinoSpeciesWorkProfile.ANKYLOSAURUS;
            case "pteranodon" -> DinoSpeciesWorkProfile.PTERANODON;
            case "spinosaurus" -> DinoSpeciesWorkProfile.SPINOSAURUS;
            case "pachycephalosaurus" -> DinoSpeciesWorkProfile.PACHYCEPHALOSAURUS;
            default -> DinoSpeciesWorkProfile.DODO;
        };
    }

    public boolean usesRunAnimation() {
        if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR) {
            return entityData.get(RAPTOR_RUNNING);
        }
        return isMountedFlight() && getDeltaMovement().lengthSqr() > 0.012D
                || runAnimationHoldTicks > 0 && walkAnimation.speed() > 0.025F;
    }

    public boolean usesWalkAnimation() {
        if (isSpinosaurusSwimming() || isSpinosaurusBreaching()) return false;
        if (isMountedFlight()) {
            return !isPteranodonAirborne()
                    && (walkAnimationHoldTicks > 0
                    || walkAnimation.speed() > 0.025F
                    || getDeltaMovement().horizontalDistanceSqr() > 0.0016D);
        }
        return walkAnimationHoldTicks > 0 || walkAnimation.speed() > 0.025F;
    }

    private float followerLocomotionAnimationSpeed(boolean running) {
        if (getCommandMode() != DinosaurCommandMode.FOLLOW) return 1.0F;
        return DinosaurFollowRules.locomotionAnimationSpeed(walkAnimation.speed(), running);
    }

    public int getTurnAnimationDirection() {
        return turnAnimationHoldTicks > 0 ? turnAnimationDirection : 0;
    }

    public boolean areEyesClosed() {
        return entityData.get(EYES_CLOSED) || isDinosaurSleeping();
    }

    public boolean isDinosaurSleeping() {
        return entityData.get(DINOSAUR_SLEEPING);
    }

    public boolean isSaddledMount() {
        return entityData.get(SADDLED);
    }

    public boolean isMountedFlight() {
        return getSpecies() == DinosaurSpecies.PTERANODON && isSaddledMount() && getControllingPassenger() != null;
    }

    public boolean isPteranodonAirborne() {
        return getSpecies() == DinosaurSpecies.PTERANODON
                && entityData.get(PTERO_FLIGHT_MODE) != PTERO_FLIGHT_GROUNDED;
    }

    public boolean isPteranodonGliding() {
        return getSpecies() == DinosaurSpecies.PTERANODON
                && isSaddledMount()
                && entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_GLIDING;
    }

    public boolean isPteranodonHovering() {
        return getSpecies() == DinosaurSpecies.PTERANODON
                && isSaddledMount()
                && entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_HOVERING;
    }

    public float getPteranodonFlightSpeed() {
        return Math.max((float)getDeltaMovement().length(), entityData.get(PTERO_AIRSPEED));
    }

    public float getPteranodonStamina() {
        return Mth.clamp(entityData.get(PTERO_STAMINA), 0.0F, PTERO_MAX_STAMINA);
    }

    public float getPteranodonStaminaRatio() {
        return getPteranodonStamina() / PTERO_MAX_STAMINA;
    }

    public boolean isPteranodonExhausted() {
        return entityData.get(PTERO_EXHAUSTED);
    }

    public float getPteranodonAnimationSpeed(float partialTick) {
        return Mth.lerp(partialTick, pteranodonAnimationSpeedPrevious, pteranodonAnimationSpeed);
    }

    public float getPteranodonBankDegrees() {
        return entityData.get(PTERO_BANK);
    }

    public float getPteranodonBankDegrees(float partialTick) {
        return Mth.lerp(partialTick, pteranodonBankPrevious, getPteranodonBankDegrees());
    }

    public float getPteranodonRiderFlap(float ageInTicks) {
        if (!isPteranodonAirborne()) {
            return 0.0F;
        }
        float partialTick = Mth.clamp(ageInTicks - Mth.floor(ageInTicks), 0.0F, 1.0F);
        double phase = Mth.lerp(partialTick, pteranodonRiderFlapPhasePrevious, pteranodonRiderFlapPhase);
        float blend = Mth.lerp(partialTick, pteranodonRiderFlapBlendPrevious, pteranodonRiderFlapBlend);
        return Mth.sin((float)phase) * blend;
    }

    public float getPteranodonRiderBodyPitch(float ageInTicks) {
        if (!isPteranodonAirborne()) {
            return 0.0F;
        }
        float flap = getPteranodonRiderFlap(ageInTicks);
        return (isPteranodonHovering() ? -7.5F : -5.0F) * flap * flap;
    }

    public float getPteranodonRiderBob(float ageInTicks) {
        float flap = getPteranodonRiderFlap(ageInTicks);
        if (!isPteranodonAirborne()) {
            return 0.0F;
        }
        float hoverWeight = isPteranodonHovering() ? 1.0F : 0.46F;
        float wingLift = flap * flap * 0.082F * hoverWeight;
        float breathingSway = (Mth.sin(ageInTicks * 0.105F)
                + Mth.sin(ageInTicks * 0.043F) * 0.42F) * 0.010F;
        return -wingLift - breathingSway;
    }

    public float getPteranodonRiderRootPitch(float ageInTicks) {
        float partialTick = Mth.clamp(ageInTicks - Mth.floor(ageInTicks), 0.0F, 1.0F);
        return Mth.lerp(partialTick, pteranodonRiderRootPitchPrevious, pteranodonRiderRootPitch);
    }

    public float getPteranodonRiderPosture(float ageInTicks) {
        float partialTick = Mth.clamp(ageInTicks - Mth.floor(ageInTicks), 0.0F, 1.0F);
        return Mth.lerp(partialTick, pteranodonRiderPosturePrevious, pteranodonRiderPosture);
    }

    public boolean usesPoweredPteranodonAnimation() {
        return entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_POWERED
                || pteranodonPoweredAnimationHoldTicks > 0;
    }

    public float getPteranodonRideSwayRoll(float ageInTicks) {
        if (!isPteranodonAirborne()) {
            return 0.0F;
        }
        float strength = isPteranodonHovering() ? 0.72F : 0.18F;
        return (Mth.sin(ageInTicks * 0.078F) * 1.12F
                + Mth.sin(ageInTicks * 0.031F + 1.4F) * 0.48F) * strength;
    }

    public float getPteranodonRideSwayPitch(float ageInTicks) {
        if (!isPteranodonAirborne()) {
            return 0.0F;
        }
        float strength = isPteranodonHovering() ? 0.70F : 0.16F;
        return (Mth.sin(ageInTicks * 0.066F + 0.7F) * 0.72F
                + Mth.sin(ageInTicks * 0.027F) * 0.30F) * strength;
    }

    public void setPteranodonClientDescendInput(boolean descending) {
        pteranodonClientDescendInput = descending;
    }

    public boolean isSpinosaurusSwimming() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS && entityData.get(SPINO_SWIMMING);
    }

    public boolean isSpinosaurusBreaching() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS
                && (entityData.get(SPINO_BREACH_TIME) > 0
                || level().isClientSide() && spinosaurusPredictedBreachTicks > 0);
    }

    public boolean isSpinosaurusAquaticPose() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS
                && (entityData.get(SPINO_SWIMMING) || isSpinosaurusBreaching());
    }

    public boolean isSpinosaurusLandSprinting() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS && entityData.get(SPINO_LAND_SPRINTING);
    }

    public void setSpinosaurusLandSprinting(boolean sprinting) {
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            entityData.set(SPINO_LAND_SPRINTING, sprinting
                    && !isInWater()
                    && !isSpinosaurusBreaching()
                    && !isSpinosaurusLandExhausted()
                    && getSpinosaurusLandStamina() > 0.0F);
        }
    }

    public float getSpinosaurusLandStamina() {
        return Mth.clamp(entityData.get(SPINO_LAND_STAMINA), 0.0F, SPINO_MAX_LAND_STAMINA);
    }

    public float getSpinosaurusLandStaminaRatio() {
        return getSpinosaurusLandStamina() / SPINO_MAX_LAND_STAMINA;
    }

    public boolean isSpinosaurusLandExhausted() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS && entityData.get(SPINO_LAND_EXHAUSTED);
    }

    public float getSpinosaurusSwimSpeed() {
        return Math.max((float)getDeltaMovement().length(), entityData.get(SPINO_SWIM_SPEED));
    }

    public float getSpinosaurusBankDegrees() {
        return entityData.get(SPINO_BANK);
    }

    public float getSpinosaurusBankDegrees(float partialTick) {
        return Mth.lerp(partialTick, spinosaurusBankPrevious, getSpinosaurusBankDegrees());
    }

    public void setSpinosaurusClientDescendInput(boolean descending) {
        spinosaurusClientDescendInput = descending;
    }

    public boolean isDefeatTransferActive() {
        return entityData.get(DEFEAT_TRANSFER_TIME) > 0;
    }

    public float getDefeatTransferProgress(float partialTick) {
        int remaining = entityData.get(DEFEAT_TRANSFER_TIME);
        return remaining <= 0 ? 0.0F
                : Mth.clamp((DEFEAT_TRANSFER_TICKS - remaining + partialTick) / DEFEAT_TRANSFER_TICKS, 0.0F, 1.0F);
    }

    public int getSleepVisualTicks() {
        return sleepVisualTicks;
    }

    public void updateRenderedProceduralMotion(
            float targetYaw,
            float targetPitch,
            float bodyYaw,
            double renderAgeTicks,
            boolean moving,
            boolean trackingTarget,
            float leftFootTarget,
            float rightFootTarget
    ) {
        targetYaw = Mth.clamp(targetYaw, -52.0F, 52.0F);
        targetPitch = Mth.clamp(targetPitch, -28.0F, 28.0F);
        if (Double.isNaN(lastHeadRenderAge)) {
            lastHeadRenderAge = renderAgeTicks;
            lastRenderedBodyYaw = bodyYaw;
            renderedHeadYaw = targetYaw;
            renderedHeadPitch = targetPitch;
            return;
        }

        float elapsedSeconds = (float)Mth.clamp((renderAgeTicks - lastHeadRenderAge) / 20.0D, 0.0D, 0.10D);
        lastHeadRenderAge = renderAgeTicks;
        if (elapsedSeconds <= 0.0F) {
            return;
        }

        float bodyDelta = Float.isNaN(lastRenderedBodyYaw)
                ? 0.0F
                : Mth.wrapDegrees(bodyYaw - lastRenderedBodyYaw);
        lastRenderedBodyYaw = bodyYaw;
        float angularSpeed = Mth.clamp(bodyDelta / elapsedSeconds, -170.0F, 170.0F);
        float bodyLeadTarget = Mth.clamp(targetYaw * 0.13F + angularSpeed * 0.022F, -7.0F, 7.0F);
        float tailTarget = Mth.clamp(-angularSpeed * 0.075F - targetYaw * 0.11F, -18.0F, 18.0F);
        if (moving) {
            tailTarget += Mth.sin((float)renderAgeTicks * 0.18F) * 2.2F;
        } else if (!trackingTarget) {
            tailTarget += Mth.sin((float)renderAgeTicks * 0.055F) * 1.15F;
        }
        float turnStepTarget = moving ? 0.0F : Mth.clamp(angularSpeed / 72.0F, -1.0F, 1.0F);
        float cargoScaleTarget = getCarriedStack().isEmpty() ? 0.0F : 1.0F;

        int steps = Math.max(1, Mth.ceil(elapsedSeconds / (1.0F / 120.0F)));
        float step = elapsedSeconds / steps;
        for (int index = 0; index < steps; index++) {
            renderedHeadYawVelocity += (targetYaw - renderedHeadYaw) * 58.0F * step;
            renderedHeadPitchVelocity += (targetPitch - renderedHeadPitch) * 54.0F * step;
            renderedBodyLeadVelocity += (bodyLeadTarget - renderedBodyLead) * 43.0F * step;
            renderedTailYawVelocity += (tailTarget - renderedTailYaw) * 30.0F * step;
            renderedTailTipYawVelocity += (renderedTailYaw * 1.22F - renderedTailTipYaw) * 23.0F * step;
            renderedTurnStepVelocity += (turnStepTarget - renderedTurnStep) * 52.0F * step;
            renderedCargoScaleVelocity += (cargoScaleTarget - renderedCargoScale) * 64.0F * step;
            renderedLeftFootOffsetVelocity += (leftFootTarget - renderedLeftFootOffset) * 74.0F * step;
            renderedRightFootOffsetVelocity += (rightFootTarget - renderedRightFootOffset) * 74.0F * step;
            renderedHeadYawVelocity *= (float)Math.exp(-9.8F * step);
            renderedHeadPitchVelocity *= (float)Math.exp(-10.5F * step);
            renderedBodyLeadVelocity *= (float)Math.exp(-10.0F * step);
            renderedTailYawVelocity *= (float)Math.exp(-7.4F * step);
            renderedTailTipYawVelocity *= (float)Math.exp(-6.8F * step);
            renderedTurnStepVelocity *= (float)Math.exp(-10.5F * step);
            renderedCargoScaleVelocity *= (float)Math.exp(-10.0F * step);
            renderedLeftFootOffsetVelocity *= (float)Math.exp(-13.0F * step);
            renderedRightFootOffsetVelocity *= (float)Math.exp(-13.0F * step);
            renderedHeadYaw += renderedHeadYawVelocity * step;
            renderedHeadPitch += renderedHeadPitchVelocity * step;
            renderedBodyLead += renderedBodyLeadVelocity * step;
            renderedTailYaw += renderedTailYawVelocity * step;
            renderedTailTipYaw += renderedTailTipYawVelocity * step;
            renderedTurnStep += renderedTurnStepVelocity * step;
            renderedCargoScale += renderedCargoScaleVelocity * step;
            renderedLeftFootOffset += renderedLeftFootOffsetVelocity * step;
            renderedRightFootOffset += renderedRightFootOffsetVelocity * step;
        }
    }

    public float getRenderedHeadYaw() {
        return renderedHeadYaw;
    }

    public float getRenderedHeadPitch() {
        return renderedHeadPitch;
    }

    public float getRenderedBodyLead() {
        return renderedBodyLead;
    }

    public float getRenderedTailYaw() {
        return renderedTailYaw;
    }

    public float getRenderedTailTipYaw() {
        return renderedTailTipYaw;
    }

    public float getRenderedTurnStep() {
        return renderedTurnStep;
    }

    public float getRenderedCargoScale() {
        return Mth.clamp(renderedCargoScale, 0.0F, 1.08F);
    }

    public float getRenderedLeftFootOffset() {
        return renderedLeftFootOffset;
    }

    public float getRenderedRightFootOffset() {
        return renderedRightFootOffset;
    }

    public int getGeneticQuality() {
        return Math.max(0, entityData.get(GENETIC_QUALITY));
    }

    public int getMutationMask() {
        return entityData.get(MUTATION_MASK);
    }

    public int getHueVariant() {
        return entityData.get(HUE_VARIANT);
    }

    public float getGeneticScale() {
        int quality = entityData.get(GENETIC_QUALITY);
        float baseScale = quality < 0 ? 1.0F : 0.90F + quality * 0.002F;
        return baseScale * (hasHugeMutation() ? (float)PrimevalTuning.server().hugeScale() : 1.0F);
    }

    public boolean hasHugeMutation() {
        return (getMutationMask() & MUTATION_HUGE) != 0;
    }

    public boolean hasAlbinoMutation() {
        return (getMutationMask() & MUTATION_ALBINO) != 0;
    }

    public boolean usesAlbinoAppearance() {
        return hasAlbinoMutation() && !entityData.get(ORIGINAL_PIGMENT_RESTORED);
    }

    public boolean hasRestoredOriginalPigment() {
        return entityData.get(ORIGINAL_PIGMENT_RESTORED);
    }

    public float getMutationStatMultiplier() {
        double multiplier = 1.0D;
        if (hasHugeMutation()) multiplier *= PrimevalTuning.server().hugeStats();
        if (hasAlbinoMutation()) multiplier *= PrimevalTuning.server().albinoStats();
        return (float)multiplier;
    }

    public void setMutationMaskForTesting(int mutationMask) {
        if (level().isClientSide()) return;
        float healthRatio = getHealth() / Math.max(1.0F, getMaxHealth());
        entityData.set(MUTATION_MASK, mutationMask & MUTATION_MASK_ALLOWED);
        entityData.set(ORIGINAL_PIGMENT_RESTORED, false);
        applyGeneticAttributes(false);
        setHealth(Mth.clamp(getMaxHealth() * healthRatio, 1.0F, getMaxHealth()));
        DinosaurOwnership.syncRecord(this);
    }

    public DinosaurSpecies getSpecies() {
        return DinosaurSpecies.byRegistryName(BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath());
    }

    public int getSpecialtyStars(int jobIndex) {
        return getSpeciesWorkProfile().stars(jobIndex);
    }

    public int getWorkEfficiencyPercent(int jobIndex) {
        return Math.max(1, Math.round(WorkSpecialtyRules.efficiencyPercent(getSpecialtyStars(jobIndex))
                * getMutationStatMultiplier()
                * DinosaurGeneticPerformanceRules.workSpeedMultiplier(getGeneticQuality())
                * getSpecies().passiveWorkSpeedMultiplier(jobIndex, getPassiveStrength())));
    }

    public float getPassiveStrength() {
        return DinosaurGeneticPerformanceRules.passiveStrength(
                getGeneticQuality(), dinosaurLevel, getMutationMask());
    }

    public float getRaptorMomentum() {
        return getSpecies() == DinosaurSpecies.VELOCIRAPTOR ? raptorMomentum : 0.0F;
    }

    public float getRaptorAnimationSpeed(float partialTick) {
        return Mth.lerp(partialTick, raptorAnimationSpeedPrevious, raptorAnimationSpeed);
    }

    public int getWorkAction() {
        return entityData.get(WORK_ACTION);
    }

    public int getWorkActionProgress() {
        return entityData.get(WORK_ACTION_PROGRESS);
    }

    public int getWorkActionDuration() {
        return entityData.get(WORK_ACTION_DURATION);
    }

    public Optional<BlockPos> getWorkActionPos() {
        return entityData.get(WORK_ACTION_POS);
    }

    public int getHunger() {
        return entityData.get(HUNGER);
    }

    public void feed(int amount) {
        entityData.set(HUNGER, Mth.clamp(getHunger() + amount, 0, 100));
        if (amount > 0) {
            entityData.set(MOOD, Mth.clamp(getMood() + Math.max(1, amount / 4), 0, 100));
        }
    }

    public boolean canEat(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (getSpecies().diet()) {
            case HERBIVORE -> stack.is(ModItemTags.HERBIVORE_FOOD);
            case CARNIVORE -> stack.is(ModItemTags.CARNIVORE_FOOD);
            case OMNIVORE -> stack.is(ModItemTags.HERBIVORE_FOOD) || stack.is(ModItemTags.CARNIVORE_FOOD);
        };
    }

    public void eat(ItemStack stack, int baseFoodValue) {
        if (!canEat(stack) || baseFoodValue <= 0) {
            return;
        }
        int restored = Math.max(4, Math.round(baseFoodValue / getSpecies().appetite()));
        feed(restored);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void die(DamageSource source) {
        clearBreedingCourtship();
        if (!level().isClientSide()
                && source.getDirectEntity() instanceof ServerPlayer killer
                && (getDinosaurOwner().isEmpty() || isOwnedBy(killer.getUUID()))) {
            permanentPlayerKill = true;
            if (getDinosaurOwner().isPresent()) {
                DinosaurOwnership.permanentlyRemove(killer, this);
            }
            dropPlayerKillMaterials();
            super.die(source);
            discard();
            return;
        }
        if (!level().isClientSide() && getDinosaurOwner().isPresent()) {
            recoverFromDefeat();
            return;
        }
        super.die(source);
    }

    @Override
    public void kill(ServerLevel level) {
        if (getDinosaurOwner().isPresent() && !permanentPlayerKill) {
            clearBreedingCourtship();
            if (commandTablePos != null) {
                recoverFromDefeat();
            } else if (!DinosaurOwnership.returnToReserveAfterDefeat(this)) {
                waitForOwnerRecovery();
            }
            return;
        }
        super.kill(level);
    }

    public boolean shouldRecoverFromLethalDamage(DamageSource source) {
        if (level().isClientSide() || getDinosaurOwner().isEmpty()) return false;
        return !(source.getDirectEntity() instanceof ServerPlayer killer && isOwnedBy(killer.getUUID()));
    }

    public void recoverFromIncomingLethalDamage() {
        clearBreedingCourtship();
        recoverFromDefeat();
    }

    private void recoverFromDefeat() {
        if (isDefeatTransferActive()) return;
        if (commandTablePos != null) {
            setHealth(1.0F);
            setTarget(null);
            setAggressive(false);
            ejectPassengers();
            navigation.stop();
            cancelWorkAction();
            entityData.set(ATTACK_ANIMATION_TICKS, 0);
            pendingAttackTargetId = -1;
            pendingAttackContactTick = -1L;
            hurtTime = 0;
            deathTime = 0;
            setDeltaMovement(Vec3.ZERO);
            setNoAi(false);
            setInvulnerable(true);
            setNoGravity(true);
            noPhysics = true;
            entityData.set(DEFEAT_TRANSFER_TIME, DEFEAT_TRANSFER_TICKS);
            return;
        }
        if (!DinosaurOwnership.returnToReserveAfterDefeat(this)) waitForOwnerRecovery();
    }

    private void tickDefeatTransfer() {
        int remaining = entityData.get(DEFEAT_TRANSFER_TIME);
        if (remaining <= 0) return;
        navigation.stop();
        setDeltaMovement(Vec3.ZERO);
        if (remaining > 1) {
            entityData.set(DEFEAT_TRANSFER_TIME, remaining - 1);
            return;
        }
        entityData.set(DEFEAT_TRANSFER_TIME, 0);
        setNoAi(false);
        setInvulnerable(false);
        setNoGravity(false);
        noPhysics = false;
        if (DinosaurOwnership.returnToReserveAfterDefeat(this)) return;
        waitForOwnerRecovery();
    }

    private void waitForOwnerRecovery() {
        pendingOwnerRecovery = true;
        setHealth(1.0F);
        setTarget(null);
        setAggressive(false);
        ejectPassengers();
        navigation.stop();
        cancelWorkAction();
        setDeltaMovement(Vec3.ZERO);
        setNoAi(false);
        setNoGravity(false);
        noPhysics = false;
        setInvulnerable(true);
    }

    public void prepareForRecoverySnapshot() {
        pendingOwnerRecovery = false;
        entityData.set(DEFEAT_TRANSFER_TIME, 0);
        setNoAi(false);
        setInvulnerable(false);
        setNoGravity(false);
        noPhysics = false;
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
    }

    private void tickPendingOwnerRecovery() {
        if (!pendingOwnerRecovery || tickCount % 20 != 0) return;
        pendingOwnerRecovery = false;
        setInvulnerable(false);
        setNoGravity(false);
        noPhysics = false;
        if (DinosaurOwnership.returnToReserveAfterDefeat(this)) return;
        waitForOwnerRecovery();
    }

    public boolean isPermanentPlayerKill() {
        return permanentPlayerKill;
    }

    private void dropPlayerKillMaterials() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        DinosaurSpecies species = getSpecies();
        boolean small = DinosaurEggSize.SMALL.contains(species);
        boolean large = DinosaurEggSize.LARGE.contains(species);
        int meatCount = small ? 1 + random.nextInt(2) : large ? 3 + random.nextInt(3) : 2 + random.nextInt(2);
        int boneCount = small ? 1 + random.nextInt(2) : large ? 2 + random.nextInt(3) : 1 + random.nextInt(3);
        spawnAtLocation(serverLevel, new ItemStack(
                small ? ModItems.SMALL_DINO_MEAT.get() : ModItems.BIG_DINO_MEAT.get(), meatCount));
        spawnAtLocation(serverLevel, new ItemStack(
                small ? ModItems.SMALL_DINO_BONE.get() : ModItems.BIG_DINO_BONE.get(), boneCount));
        if (species == DinosaurSpecies.PTERANODON) {
            spawnAtLocation(serverLevel, new ItemStack(ModItems.PTERANODON_WING_FRAGMENT.get()));
        }
        if (species == DinosaurSpecies.TYRANNOSAURUS) {
            spawnAtLocation(serverLevel,
                    new ItemStack(ModItems.TYRANNOSAURUS_TOOTH.get(), 1 + random.nextInt(2)));
        }
        if (species == DinosaurSpecies.SPINOSAURUS
                && SpinosaurusTrophyRules.rollsManualKillDrop(random)) {
            spawnAtLocation(serverLevel, new ItemStack(ModItems.SPINOSAURUS_HEAD.get()));
        }
    }

    @Override
    protected float getSoundVolume() {
        return Mth.clamp(0.34F + getSpecies().collisionWidth() * 0.22F, 0.42F, 1.65F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player && getDinosaurOwner().isPresent()) {
            target = null;
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof Player && getDinosaurOwner().isPresent() && isAlive()) {
            setTarget(null);
            setAggressive(false);
            workerCooldown = 0;
        }
        return hurt;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
    }

    @Override
    public float maxUpStep() {
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS
                && getControllingPassenger() != null
                && !isInWater()
                && !isSpinosaurusBreaching()) {
            return Math.max(getSpecies().stepHeight(), SPINO_MOUNTED_STEP_HEIGHT * getScale());
        }
        return getSpecies().stepHeight();
    }

    @Override
    public int getHeadRotSpeed() {
        return Math.max(4, Math.round(getSpecies().turnDegreesPerTick() * 1.4F));
    }

    @Override
    public int getMaxHeadYRot() {
        return switch (getSpecies()) {
            case TYRANNOSAURUS -> 42;
            case STEGOSAURUS -> 52;
            default -> 58;
        };
    }

    public int getMood() {
        return entityData.get(MOOD);
    }

    public int getIndicatorIcon() {
        return entityData.get(INDICATOR_ICON);
    }

    public int getIndicatorTicks() {
        return entityData.get(INDICATOR_TICKS);
    }

    public int getIndicatorAge() {
        return entityData.get(INDICATOR_AGE);
    }

    public void assignWork(
            int jobIndex,
            BlockPos tablePos,
            List<BlockPos> sourcePositions,
            List<BlockPos> workstationPositions,
            List<BlockPos> destinationPositions,
            BlockPos areaEndPos,
            List<BlockPos> fallbackPositions,
            List<String> itemFilters,
            List<String> fuelFilters,
            Map<BlockPos, Integer> blockPriorities,
            int expeditionTier,
            int priority,
            int batchSize,
            int schedule,
            int sourceReserve,
            int destinationTarget,
            int repeatMode,
            int routePolicy,
            boolean exactMatch,
            boolean avoidDanger
    ) {
        workJobIndex = Mth.clamp(jobIndex, 0, 4);
        entityData.set(WORK_JOB_INDEX, workJobIndex);
        setCommandTableLink(tablePos);
        replacePositions(workSourcePositions, sourcePositions);
        replacePositions(workWorkstationPositions, workstationPositions);
        replacePositions(workDestinationPositions, destinationPositions);
        replacePositions(workFallbackPositions, fallbackPositions);
        workSourcePos = firstPosition(workSourcePositions);
        workWorkstationPos = firstPosition(workWorkstationPositions);
        workDestinationPos = firstPosition(workDestinationPositions);
        workAreaEndPos = areaEndPos == null ? null : areaEndPos.immutable();
        workFallbackPos = firstPosition(workFallbackPositions);
        workItemFilters.clear();
        if (itemFilters != null) {
            itemFilters.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(16).forEach(workItemFilters::add);
        }
        workItemFilter = workItemFilters.isEmpty() ? "" : workItemFilters.getFirst();
        workFuelFilters.clear();
        if (fuelFilters != null) {
            fuelFilters.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(4).forEach(workFuelFilters::add);
        }
        replaceBlockPriorities(blockPriorities);
        this.expeditionTier = Mth.clamp(expeditionTier, 0, 4);
        workPriority = Mth.clamp(priority, 0, 3);
        workBatchSize = Mth.clamp(batchSize, 1, 64);
        workSchedule = Mth.clamp(schedule, 0, 2);
        workSourceReserve = Mth.clamp(sourceReserve, 0, 4096);
        workDestinationTarget = Mth.clamp(destinationTarget, 0, 4096);
        workRepeatMode = Mth.clamp(repeatMode, 0, 2);
        workRoutePolicy = Mth.clamp(routePolicy, 0, 2);
        exactItemMatch = exactMatch;
        this.avoidDanger = avoidDanger;
        workEnabled = true;
        workerCooldown = 0;
        workWorkstationCursor = 0;
        navigation.stop();
        navigationTarget = null;
        stalledNavigationTicks = 0;
        recoveryWaypointTicks = 0;
        cancelWorkAction();
        DinosaurOwnership.syncRecord(this);
    }

    private static void replacePositions(List<BlockPos> target, List<BlockPos> source) {
        target.clear();
        if (source != null) {
            source.stream().filter(java.util.Objects::nonNull).map(BlockPos::immutable).distinct().limit(8).forEach(target::add);
        }
    }

    private static BlockPos firstPosition(List<BlockPos> positions) {
        return positions.isEmpty() ? null : positions.getFirst();
    }

    public int getWorkJobIndex() {
        return level().isClientSide() ? entityData.get(WORK_JOB_INDEX) : workJobIndex;
    }

    public Optional<BlockPos> getCommandTablePos() {
        return entityData.get(COMMAND_TABLE_POS);
    }

    public Optional<UUID> getDinosaurOwner() {
        return Optional.ofNullable(dinosaurOwner);
    }

    public boolean isOwnedBy(UUID ownerId) {
        return dinosaurOwner != null && dinosaurOwner.equals(ownerId);
    }

    public void setDinosaurOwner(UUID ownerId) {
        dinosaurOwner = ownerId;
        setPersistenceRequired();
    }

    public DinosaurCommandMode getCommandMode() {
        return level().isClientSide()
                ? DinosaurCommandMode.byId(entityData.get(COMMAND_MODE))
                : commandMode;
    }

    public boolean isFollowingOwner() {
        return getCommandMode() == DinosaurCommandMode.FOLLOW;
    }

    public boolean hasFieldWork() {
        boolean enabled = level().isClientSide() ? entityData.get(FIELD_WORK_MODE) >= 0 : fieldWorkEnabled;
        return enabled && DinoFieldWorkRules.supports(getSpecies(), getFieldWorkMode());
    }

    public boolean isFieldWorkActive() {
        return hasFieldWork() && getCommandMode() == DinosaurCommandMode.FOLLOW;
    }

    public DinoWhistleSettings.FieldMode getFieldWorkMode() {
        int value = level().isClientSide() ? Math.max(0, entityData.get(FIELD_WORK_MODE)) : fieldWorkMode.ordinal();
        return DinoWhistleSettings.FieldMode.byId(value);
    }

    public DinoWhistleSettings.Pattern getFieldWorkPattern() {
        return level().isClientSide()
                ? DinoWhistleSettings.Pattern.byId(entityData.get(FIELD_WORK_PATTERN))
                : fieldWorkPattern;
    }

    public boolean isFieldWorkContinuous() {
        return fieldWorkContinuous;
    }

    public int getFieldWorkRange() {
        return fieldWorkRange;
    }

    public String getFieldWorkItemFilter() {
        return fieldWorkItemFilter;
    }

    public Optional<BlockPos> getFieldWorkFirst() {
        return level().isClientSide() ? entityData.get(FIELD_WORK_FIRST) : Optional.ofNullable(fieldWorkFirst);
    }

    public Optional<BlockPos> getFieldWorkSecond() {
        return level().isClientSide() ? entityData.get(FIELD_WORK_SECOND) : Optional.ofNullable(fieldWorkSecond);
    }

    public void setCommandMode(DinosaurCommandMode mode) {
        if (level().isClientSide() || mode == null) return;
        if (commandMode == mode) {
            if (mode == DinosaurCommandMode.STAY && stayPosition == null) {
                stayPosition = blockPosition().immutable();
            }
            if (navigation.isStuck()) {
                navigation.stop();
                getMoveControl().setWait();
                ownerCatchupActive = false;
            }
            DinosaurOwnership.syncRecord(this);
            return;
        }
        if (commandMode == DinosaurCommandMode.FOLLOW && mode != DinosaurCommandMode.FOLLOW) {
            settleFieldCargo();
        }
        commandMode = mode;
        entityData.set(COMMAND_MODE, mode.ordinal());
        stayPosition = mode == DinosaurCommandMode.STAY ? blockPosition().immutable() : null;
        cancelWorkAction();
        navigation.stop();
        getMoveControl().setWait();
        navigationTarget = null;
        stalledNavigationTicks = 0;
        recoveryWaypointTicks = 0;
        ownerCatchupActive = false;
        fieldTargetApproachTicks = 0;
        fieldCollectionApproachTicks = 0;
        fieldCollectionTargetId = null;
        fieldCollectionRetryAfter.clear();
        fieldWorkRescanCooldown = 0;
        workerCooldown = 0;
        setTarget(null);
        setSprinting(false);
        if (getSpecies() == DinosaurSpecies.PTERANODON && !isVehicle()) {
            stopAutonomousTransportFlight();
        }
        if (mode == DinosaurCommandMode.FOLLOW && isDinosaurSleeping()) {
            entityData.set(DINOSAUR_SLEEPING, false);
        }
        if (mode == DinosaurCommandMode.STAY) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(0.0D, movement.y, 0.0D);
        }
        DinosaurOwnership.syncRecord(this);
    }

    public void holdAfterCommandTableRecall() {
        if (level().isClientSide()) return;
        if (commandMode == DinosaurCommandMode.FOLLOW) settleFieldCargo();
        commandMode = DinosaurCommandMode.STAY;
        entityData.set(COMMAND_MODE, DinosaurCommandMode.STAY.ordinal());
        stayPosition = blockPosition().immutable();
        cancelWorkAction();
        navigation.stop();
        getMoveControl().setWait();
        navigationTarget = null;
        spinosaurusAquaticWorkTarget = null;
        stalledNavigationTicks = 0;
        recoveryWaypointTicks = 0;
        ownerCatchupActive = false;
        fieldTargetApproachTicks = 0;
        fieldCollectionApproachTicks = 0;
        fieldCollectionTargetId = null;
        fieldCollectionRetryAfter.clear();
        workerCooldown = 0;
        setTarget(null);
        setSprinting(false);
        stopAutonomousTransportFlight();
        entityData.set(SPINO_SWIMMING, false);
        entityData.set(SPINO_SWIM_SPEED, 0.0F);
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        DinosaurOwnership.syncRecord(this);
    }

    private void settleFieldCargo() {
        ItemStack carried = getCarriedStack().copy();
        if (carried.isEmpty() || !(level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner = commandOwner();
        if (owner != null && owner.level() == level()) {
            owner.getInventory().add(carried);
            if (!carried.isEmpty()) {
                ItemEntity dropped = new ItemEntity(serverLevel, owner.getX(), owner.getY() + 0.35D, owner.getZ(), carried);
                dropped.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(dropped);
            }
        } else if (commandTablePos != null) {
            spawnBaseCargo(serverLevel, commandTablePos, carried);
        } else {
            spawnAtLocation(serverLevel, carried);
        }
        entityData.set(CARRIED_STACK, ItemStack.EMPTY);
    }

    public void assignFieldWork(DinoWhistleSettings settings, BlockPos first, @Nullable BlockPos second) {
        if (level().isClientSide() || commandMode != DinosaurCommandMode.FOLLOW) return;
        if (!DinoFieldWorkRules.supports(getSpecies(), settings.mode())
                || DinoFieldWorkRules.rating(this, settings.mode()) <= 0) return;
        if (!getCarriedStack().isEmpty()) settleFieldCargo();
        fieldWorkMode = settings.mode();
        fieldWorkPattern = settings.pattern();
        fieldWorkContinuous = settings.mode().isPassive();
        fieldWorkRange = settings.range();
        fieldWorkItemFilter = settings.mode() == DinoWhistleSettings.FieldMode.COLLECT
                ? settings.itemFilter() : "";
        fieldWorkFirst = first.immutable();
        fieldWorkSecond = second == null ? null : second.immutable();
        fieldWorkEnabled = true;
        fieldWorkTargets.clear();
        fieldWorkCursor = 0;
        fieldWorkRescanCooldown = 0;
        fieldTargetApproachTicks = 0;
        lumberFastFelling = false;
        fieldCollectionTargetId = null;
        fieldCollectionApproachTicks = 0;
        fieldCollectionRetryAfter.clear();
        entityData.set(FIELD_WORK_MODE, fieldWorkMode.ordinal());
        entityData.set(FIELD_WORK_PATTERN, fieldWorkPattern.ordinal());
        entityData.set(FIELD_WORK_FIRST, Optional.of(fieldWorkFirst));
        entityData.set(FIELD_WORK_SECOND, Optional.ofNullable(fieldWorkSecond));
        cancelWorkAction();
        navigation.stop();
        navigationTarget = null;
        stalledNavigationTicks = 0;
        recoveryWaypointTicks = 0;
        DinosaurOwnership.syncRecord(this);
    }

    public void assignPassiveFieldWork(DinoWhistleSettings settings) {
        if (!settings.mode().isPassive()) return;
        assignFieldWork(settings, blockPosition(), null);
    }

    public boolean togglePassiveFieldWork(DinoWhistleSettings settings) {
        if (level().isClientSide() || !settings.mode().isPassive()
                || !DinoFieldWorkRules.supports(getSpecies(), settings.mode())) return false;
        if (fieldWorkEnabled && fieldWorkMode == settings.mode()) {
            settleFieldCargo();
            clearFieldWork();
            return false;
        }
        assignPassiveFieldWork(settings);
        return fieldWorkEnabled && fieldWorkMode == settings.mode();
    }

    public void updatePassiveFieldSettings(DinoWhistleSettings settings) {
        if (level().isClientSide() || !fieldWorkEnabled || commandMode != DinosaurCommandMode.FOLLOW
                || !settings.mode().isPassive() || fieldWorkMode != settings.mode()) return;
        fieldWorkRange = settings.range();
        fieldWorkItemFilter = fieldWorkMode == DinoWhistleSettings.FieldMode.COLLECT
                ? settings.itemFilter() : "";
        fieldCollectionTargetId = null;
        fieldCollectionApproachTicks = 0;
        fieldCollectionRetryAfter.clear();
        fieldWorkRescanCooldown = 0;
        navigation.stop();
        navigationTarget = null;
        DinosaurOwnership.syncRecord(this);
    }

    public void clearFieldWork() {
        if (!level().isClientSide() && !getCarriedStack().isEmpty()) settleFieldCargo();
        fieldWorkEnabled = false;
        fieldWorkTargets.clear();
        fieldWorkCursor = 0;
        fieldWorkRescanCooldown = 0;
        fieldTargetApproachTicks = 0;
        lumberFastFelling = false;
        fieldCollectionTargetId = null;
        fieldCollectionApproachTicks = 0;
        fieldCollectionRetryAfter.clear();
        fieldWorkFirst = null;
        fieldWorkSecond = null;
        fieldWorkItemFilter = "";
        entityData.set(FIELD_WORK_MODE, -1);
        entityData.set(FIELD_WORK_FIRST, Optional.empty());
        entityData.set(FIELD_WORK_SECOND, Optional.empty());
        cancelWorkAction();
        navigation.stop();
        navigationTarget = null;
        stalledNavigationTicks = 0;
        recoveryWaypointTicks = 0;
        if (!level().isClientSide()) DinosaurOwnership.syncRecord(this);
    }

    public int getDinosaurLevel() {
        return dinosaurLevel;
    }

    public int getDinosaurExperience() {
        return dinosaurExperience;
    }

    public int getExperienceForNextLevel() {
        return DinosaurProgression.experienceForNextLevel(dinosaurLevel);
    }

    public void awardWorkExperience(int amount) {
        if (level().isClientSide() || amount <= 0 || dinosaurLevel >= DinosaurProgression.MAX_LEVEL) return;
        int remaining = amount;
        boolean leveled = false;
        float healthRatio = getHealth() / Math.max(1.0F, getMaxHealth());
        while (remaining > 0 && dinosaurLevel < DinosaurProgression.MAX_LEVEL) {
            int required = DinosaurProgression.experienceForNextLevel(dinosaurLevel);
            int accepted = Math.min(remaining, required - dinosaurExperience);
            dinosaurExperience += accepted;
            remaining -= accepted;
            if (dinosaurExperience >= required) {
                dinosaurExperience = 0;
                dinosaurLevel++;
                leveled = true;
            }
        }
        if (dinosaurLevel >= DinosaurProgression.MAX_LEVEL) dinosaurExperience = 0;
        if (leveled) {
            applyGeneticAttributes(false);
            setHealth(Mth.clamp(getMaxHealth() * healthRatio, 1.0F, getMaxHealth()));
        }
    }

    public void linkToCommandTable(BlockPos tablePos) {
        boolean sameBase = commandTablePos != null && commandTablePos.equals(tablePos);
        setCommandTableLink(tablePos);
        if (!sameBase) {
            workEnabled = false;
            cancelWorkAction();
        }
        navigation.stop();
    }

    public void unlinkFromCommandTable() {
        setCommandTableLink(null);
        workEnabled = false;
        navigation.stop();
    }

    private void setCommandTableLink(@Nullable BlockPos tablePos) {
        commandTablePos = tablePos == null ? null : tablePos.immutable();
        entityData.set(COMMAND_TABLE_POS, Optional.ofNullable(commandTablePos));
    }

    public Optional<BlockPos> getWorkSourcePos() {
        return Optional.ofNullable(workSourcePos);
    }

    public Optional<BlockPos> getWorkWorkstationPos() {
        return Optional.ofNullable(workWorkstationPos);
    }

    public Optional<BlockPos> getWorkDestinationPos() {
        return Optional.ofNullable(workDestinationPos);
    }

    public Optional<BlockPos> getWorkAreaEndPos() {
        return Optional.ofNullable(workAreaEndPos);
    }

    public Optional<BlockPos> getWorkFallbackPos() {
        return Optional.ofNullable(workFallbackPos);
    }

    public List<BlockPos> getWorkSourcePositions() {
        return List.copyOf(workSourcePositions);
    }

    public List<BlockPos> getWorkWorkstationPositions() {
        return List.copyOf(workWorkstationPositions);
    }

    public List<BlockPos> getWorkDestinationPositions() {
        return List.copyOf(workDestinationPositions);
    }

    public List<BlockPos> getWorkFallbackPositions() {
        return List.copyOf(workFallbackPositions);
    }

    public String getWorkItemFilter() {
        return workItemFilter;
    }

    public List<String> getWorkItemFilters() {
        return List.copyOf(workItemFilters);
    }

    public List<String> getWorkFuelFilters() {
        return List.copyOf(workFuelFilters);
    }

    public int getExpeditionTier() {
        return expeditionTier;
    }

    public boolean isOnExpedition() {
        return onExpedition && expeditionEndTick > level().getGameTime();
    }

    public boolean isBreedingPrimed() {
        return breedingPrimed;
    }

    public void setBreedingPrimed(boolean breedingPrimed) {
        this.breedingPrimed = breedingPrimed;
        if (!breedingPrimed) {
            breedingPartnerId = null;
            breedingOwnerId = null;
            breedingCourtshipTicks = 0;
            breedingPartnerMissingTicks = 0;
        }
    }

    public boolean isBreedingWith(UUID partnerId) {
        return breedingPartnerId != null && breedingPartnerId.equals(partnerId);
    }

    public void startBreedingCourtship(FieldDodoEntity partner, ServerPlayer owner) {
        breedingPrimed = true;
        breedingPartnerId = partner.getUUID();
        breedingOwnerId = owner.getUUID();
        breedingCourtshipTicks = 0;
        breedingPartnerMissingTicks = 0;
        entityData.set(DINOSAUR_SLEEPING, false);
        setTarget(null);
        cancelWorkAction();
        navigation.stop();
        DinosaurOwnership.syncRecord(this);
    }

    public void clearBreedingCourtship() {
        breedingPrimed = false;
        breedingPartnerId = null;
        breedingOwnerId = null;
        breedingCourtshipTicks = 0;
        breedingPartnerMissingTicks = 0;
        navigation.stop();
    }

    public long getBreedingCooldownRemaining() {
        return Math.max(0L, breedingCooldownUntilTick - level().getGameTime());
    }

    public void beginBreedingCooldown(long durationTicks) {
        clearBreedingCourtship();
        breedingCooldownUntilTick = level().getGameTime() + Math.max(0L, durationTicks);
    }

    public boolean isIncapacitated() {
        return level().getGameTime() < incapacitatedUntilTick;
    }

    public long getExpeditionTicksRemaining() {
        return onExpedition ? Math.max(0L, expeditionEndTick - level().getGameTime()) : 0L;
    }

    public void reconcilePersistentTimedState() {
        if (!level().isClientSide() && onExpedition && expeditionEndTick <= level().getGameTime()) {
            tickExpedition();
        }
    }

    public Map<BlockPos, Integer> getWorkBlockPriorities() {
        return Map.copyOf(workBlockPriorities);
    }

    public int getWorkPriority() {
        return workPriority;
    }

    public int getWorkBatchSize() {
        return workBatchSize;
    }

    public int getWorkSchedule() {
        return workSchedule;
    }

    public int getWorkSourceReserve() {
        return workSourceReserve;
    }

    public int getWorkDestinationTarget() {
        return workDestinationTarget;
    }

    public int getWorkRepeatMode() {
        return workRepeatMode;
    }

    public int getWorkRoutePolicy() {
        return workRoutePolicy;
    }

    public boolean isExactItemMatch() {
        return exactItemMatch;
    }

    public boolean shouldAvoidDanger() {
        return avoidDanger;
    }

    public ItemStack getCarriedStack() {
        return entityData.get(CARRIED_STACK);
    }

    public ItemStack takeCarriedStackForStorage() {
        ItemStack carried = getCarriedStack().copy();
        entityData.set(CARRIED_STACK, ItemStack.EMPTY);
        return carried;
    }

    public boolean isWorkEnabled() {
        return workEnabled;
    }

    public int getWorkMoodDrainUnits() {
        return workMoodDrainUnits;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUNGER, 64);
        builder.define(CARRIED_STACK, ItemStack.EMPTY);
        builder.define(MOOD, 68);
        builder.define(INDICATOR_ICON, 0);
        builder.define(INDICATOR_TICKS, 0);
        builder.define(INDICATOR_AGE, 0);
        builder.define(EYES_CLOSED, false);
        builder.define(DINOSAUR_SLEEPING, false);
        builder.define(SADDLED, false);
        builder.define(PTERO_FLIGHT_MODE, PTERO_FLIGHT_GROUNDED);
        builder.define(PTERO_BANK, 0.0F);
        builder.define(PTERO_AIRSPEED, 0.0F);
        builder.define(PTERO_STAMINA, PTERO_MAX_STAMINA);
        builder.define(PTERO_EXHAUSTED, false);
        builder.define(SPINO_SWIMMING, false);
        builder.define(SPINO_BANK, 0.0F);
        builder.define(SPINO_SWIM_SPEED, 0.0F);
        builder.define(SPINO_BREACH_TIME, 0);
        builder.define(SPINO_LAND_SPRINTING, false);
        builder.define(SPINO_LAND_STAMINA, SPINO_MAX_LAND_STAMINA);
        builder.define(SPINO_LAND_EXHAUSTED, false);
        builder.define(DEFEAT_TRANSFER_TIME, 0);
        builder.define(ATTACK_ANIMATION_TICKS, 0);
        builder.define(RAPTOR_RUNNING, false);
        builder.define(GENETIC_QUALITY, -1);
        builder.define(MUTATION_MASK, 0);
        builder.define(ORIGINAL_PIGMENT_RESTORED, false);
        builder.define(HUE_VARIANT, 0);
        builder.define(WORK_JOB_INDEX, 0);
        builder.define(WORK_ACTION, 0);
        builder.define(WORK_ACTION_PROGRESS, 0);
        builder.define(WORK_ACTION_DURATION, 0);
        builder.define(WORK_ACTION_POS, Optional.empty());
        builder.define(COMMAND_TABLE_POS, Optional.empty());
        builder.define(COMMAND_MODE, DinosaurCommandMode.HOME.ordinal());
        builder.define(FIELD_WORK_MODE, -1);
        builder.define(FIELD_WORK_PATTERN, DinoWhistleSettings.Pattern.SINGLE.ordinal());
        builder.define(FIELD_WORK_FIRST, Optional.empty());
        builder.define(FIELD_WORK_SECOND, Optional.empty());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("PrimevalWorkStateSchema", WORK_STATE_SCHEMA);
        output.putInt("PrimevalHunger", getHunger());
        output.putLong("PrimevalNextHungerDrain", nextHungerDrainTick);
        output.putInt("PrimevalMood", getMood());
        output.putInt("PrimevalGeneticQuality", entityData.get(GENETIC_QUALITY));
        output.putInt("PrimevalMutationSchema", MUTATION_SCHEMA);
        output.putInt("PrimevalMutationMask", getMutationMask());
        output.putBoolean("PrimevalOriginalPigmentRestored", hasRestoredOriginalPigment());
        output.putInt("PrimevalHueVariant", getHueVariant());
        if (dinosaurOwner != null) output.putString("PrimevalDinosaurOwner", dinosaurOwner.toString());
        output.putInt("PrimevalDinosaurLevel", dinosaurLevel);
        output.putInt("PrimevalDinosaurExperience", dinosaurExperience);
        output.putInt("PrimevalWorkJob", workJobIndex);
        output.putInt("PrimevalCommandMode", commandMode.ordinal());
        if (stayPosition != null) output.putLong("PrimevalStayPosition", stayPosition.asLong());
        output.putBoolean("PrimevalFieldWorkEnabled", fieldWorkEnabled);
        output.putInt("PrimevalFieldWorkMode", fieldWorkMode.ordinal());
        output.putInt("PrimevalFieldWorkPattern", fieldWorkPattern.ordinal());
        output.putInt("PrimevalFieldWorkRange", fieldWorkRange);
        if (!fieldWorkItemFilter.isBlank()) output.putString("PrimevalFieldWorkItem", fieldWorkItemFilter);
        if (fieldWorkFirst != null) output.putLong("PrimevalFieldWorkFirst", fieldWorkFirst.asLong());
        if (fieldWorkSecond != null) output.putLong("PrimevalFieldWorkSecond", fieldWorkSecond.asLong());
        output.putBoolean("PrimevalLumberFastFelling", lumberFastFelling);
        if (commandTablePos != null) {
            output.putLong("PrimevalCommandTable", commandTablePos.asLong());
        }
        if (workSourcePos != null) {
            output.putLong("PrimevalWorkSource", workSourcePos.asLong());
        }
        if (workWorkstationPos != null) {
            output.putLong("PrimevalWorkstation", workWorkstationPos.asLong());
        }
        if (workDestinationPos != null) {
            output.putLong("PrimevalWorkDestination", workDestinationPos.asLong());
        }
        if (workAreaEndPos != null) {
            output.putLong("PrimevalWorkAreaEnd", workAreaEndPos.asLong());
        }
        if (workFallbackPos != null) {
            output.putLong("PrimevalWorkFallback", workFallbackPos.asLong());
        }
        writePositionList(output, "PrimevalWorkSources", workSourcePositions);
        writePositionList(output, "PrimevalWorkstations", workWorkstationPositions);
        writePositionList(output, "PrimevalWorkDestinations", workDestinationPositions);
        writePositionList(output, "PrimevalWorkOverflows", workFallbackPositions);
        var savedFilters = output.list("PrimevalWorkItems", Codec.STRING);
        workItemFilters.forEach(savedFilters::add);
        var savedFuelFilters = output.list("PrimevalWorkFuels", Codec.STRING);
        workFuelFilters.forEach(savedFuelFilters::add);
        output.putInt("PrimevalExpeditionTier", expeditionTier);
        writeBlockPriorities(output);
        output.putString("PrimevalWorkItem", workItemFilter);
        output.putInt("PrimevalWorkPriority", workPriority);
        output.putInt("PrimevalWorkBatch", workBatchSize);
        output.putInt("PrimevalWorkSchedule", workSchedule);
        output.putInt("PrimevalWorkSourceReserve", workSourceReserve);
        output.putInt("PrimevalWorkDestinationTarget", workDestinationTarget);
        output.putInt("PrimevalWorkRepeat", workRepeatMode);
        output.putInt("PrimevalWorkRoutePolicy", workRoutePolicy);
        output.putInt("PrimevalWorkMoodDrainUnits", workMoodDrainUnits);
        output.putBoolean("PrimevalWorkExact", exactItemMatch);
        output.putBoolean("PrimevalWorkAvoidDanger", avoidDanger);
        output.putBoolean("PrimevalWorkEnabled", workEnabled);
        output.putInt("PrimevalWorkerCooldown", Math.max(0, workerCooldown));
        output.putInt("PrimevalWorkstationCursor", Math.max(0, workWorkstationCursor));
        output.putInt("PrimevalWorkAction", entityData.get(WORK_ACTION));
        output.putInt("PrimevalWorkActionProgress", entityData.get(WORK_ACTION_PROGRESS));
        output.putInt("PrimevalWorkActionDuration", entityData.get(WORK_ACTION_DURATION));
        if (workActionPos != null) output.putLong("PrimevalWorkActionPos", workActionPos.asLong());
        output.putBoolean("PrimevalSaddled", isSaddledMount());
        output.putFloat("PrimevalPteranodonStamina", getPteranodonStamina());
        output.putBoolean("PrimevalPteranodonExhausted", isPteranodonExhausted());
        output.putFloat("PrimevalSpinosaurusLandStamina", getSpinosaurusLandStamina());
        output.putBoolean("PrimevalSpinosaurusLandExhausted", isSpinosaurusLandExhausted());
        output.putInt("PrimevalDefeatTransferTime", entityData.get(DEFEAT_TRANSFER_TIME));
        output.putBoolean("PrimevalOnExpedition", onExpedition);
        output.putLong("PrimevalExpeditionEnd", expeditionEndTick);
        output.putLong("PrimevalIncapacitatedUntil", incapacitatedUntilTick);
        output.putBoolean("PrimevalPendingOwnerRecovery", pendingOwnerRecovery);
        output.putBoolean("PrimevalBreedingPrimed", breedingPrimed);
        output.putLong("PrimevalBreedingCooldownUntil", breedingCooldownUntilTick);
        if (breedingPartnerId != null) output.putString("PrimevalBreedingPartner", breedingPartnerId.toString());
        if (breedingOwnerId != null) output.putString("PrimevalBreedingOwner", breedingOwnerId.toString());
        output.putInt("PrimevalBreedingCourtshipTicks", breedingCourtshipTicks);
        if (!getCarriedStack().isEmpty()) {
            output.store("PrimevalCarriedStack", ItemStack.CODEC, getCarriedStack());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(HUNGER, Mth.clamp(input.getIntOr("PrimevalHunger", 64), 0, 100));
        nextHungerDrainTick = Math.max(0L, input.getLongOr("PrimevalNextHungerDrain", 0L));
        entityData.set(MOOD, Mth.clamp(input.getIntOr("PrimevalMood", 68), 0, 100));
        entityData.set(GENETIC_QUALITY, Mth.clamp(input.getIntOr("PrimevalGeneticQuality", -1), -1, 100));
        int savedMutationMask = input.getIntOr("PrimevalMutationSchema", 0) >= MUTATION_SCHEMA
                ? input.getIntOr("PrimevalMutationMask", 0)
                : 0;
        entityData.set(MUTATION_MASK, savedMutationMask & MUTATION_MASK_ALLOWED);
        entityData.set(ORIGINAL_PIGMENT_RESTORED,
                input.getBooleanOr("PrimevalOriginalPigmentRestored", false));
        entityData.set(HUE_VARIANT, Mth.clamp(input.getIntOr("PrimevalHueVariant", 0), -8, 8));
        String ownerValue = input.getStringOr("PrimevalDinosaurOwner", "");
        try {
            dinosaurOwner = ownerValue.isBlank() ? null : UUID.fromString(ownerValue);
        } catch (IllegalArgumentException ignored) {
            dinosaurOwner = null;
        }
        dinosaurLevel = Mth.clamp(input.getIntOr("PrimevalDinosaurLevel", 1), 1, 100);
        dinosaurExperience = dinosaurLevel >= DinosaurProgression.MAX_LEVEL ? 0 : Mth.clamp(
                input.getIntOr("PrimevalDinosaurExperience", 0),
                0,
                Math.max(0, DinosaurProgression.experienceForNextLevel(dinosaurLevel) - 1)
        );
        if (dinosaurOwner != null) setPersistenceRequired();
        workJobIndex = Mth.clamp(input.getIntOr("PrimevalWorkJob", 0), 0, 4);
        entityData.set(WORK_JOB_INDEX, workJobIndex);
        commandMode = DinosaurCommandMode.byId(input.getIntOr("PrimevalCommandMode", 0));
        entityData.set(COMMAND_MODE, commandMode.ordinal());
        stayPosition = input.getLong("PrimevalStayPosition").map(BlockPos::of).orElse(null);
        if (commandMode == DinosaurCommandMode.STAY && stayPosition == null) {
            stayPosition = blockPosition().immutable();
        }
        fieldWorkEnabled = input.getBooleanOr("PrimevalFieldWorkEnabled", false);
        fieldWorkMode = DinoWhistleSettings.FieldMode.byId(input.getIntOr("PrimevalFieldWorkMode", 0));
        fieldWorkPattern = fieldWorkMode.normalizePattern(
                DinoWhistleSettings.Pattern.byId(input.getIntOr("PrimevalFieldWorkPattern", 0)));
        fieldWorkContinuous = fieldWorkMode.isPassive();
        fieldWorkRange = Mth.clamp(input.getIntOr("PrimevalFieldWorkRange", 48),
                DinoWhistleSettings.MIN_RANGE, DinoWhistleSettings.MAX_RANGE);
        fieldWorkItemFilter = input.getStringOr("PrimevalFieldWorkItem", "");
        fieldWorkFirst = input.getLong("PrimevalFieldWorkFirst").map(BlockPos::of).orElse(null);
        fieldWorkSecond = input.getLong("PrimevalFieldWorkSecond").map(BlockPos::of).orElse(null);
        lumberFastFelling = fieldWorkEnabled
                && fieldWorkMode == DinoWhistleSettings.FieldMode.LUMBER
                && input.getBooleanOr("PrimevalLumberFastFelling", false);
        if (fieldWorkEnabled && !DinoFieldWorkRules.supports(getSpecies(), fieldWorkMode)) {
            fieldWorkEnabled = false;
            fieldWorkFirst = null;
            fieldWorkSecond = null;
            fieldWorkItemFilter = "";
            lumberFastFelling = false;
        }
        fieldWorkTargets.clear();
        fieldWorkCursor = 0;
        fieldWorkRescanCooldown = 0;
        fieldTargetApproachTicks = 0;
        fieldCollectionTargetId = null;
        fieldCollectionApproachTicks = 0;
        fieldCollectionRetryAfter.clear();
        entityData.set(FIELD_WORK_MODE, fieldWorkEnabled ? fieldWorkMode.ordinal() : -1);
        entityData.set(FIELD_WORK_PATTERN, fieldWorkPattern.ordinal());
        entityData.set(FIELD_WORK_FIRST, fieldWorkEnabled
                ? Optional.ofNullable(fieldWorkFirst) : Optional.empty());
        entityData.set(FIELD_WORK_SECOND, fieldWorkEnabled
                ? Optional.ofNullable(fieldWorkSecond) : Optional.empty());
        setCommandTableLink(input.getLong("PrimevalCommandTable").map(BlockPos::of).orElse(null));
        workSourcePos = input.getLong("PrimevalWorkSource").map(BlockPos::of).orElse(null);
        workWorkstationPos = input.getLong("PrimevalWorkstation").map(BlockPos::of).orElse(null);
        workDestinationPos = input.getLong("PrimevalWorkDestination").map(BlockPos::of).orElse(null);
        workAreaEndPos = input.getLong("PrimevalWorkAreaEnd").map(BlockPos::of).orElse(null);
        workFallbackPos = input.getLong("PrimevalWorkFallback").map(BlockPos::of).orElse(null);
        readPositionList(input, "PrimevalWorkSources", workSourcePositions, workSourcePos);
        readPositionList(input, "PrimevalWorkstations", workWorkstationPositions, workWorkstationPos);
        readPositionList(input, "PrimevalWorkDestinations", workDestinationPositions, workDestinationPos);
        readPositionList(input, "PrimevalWorkOverflows", workFallbackPositions, workFallbackPos);
        workSourcePos = firstPosition(workSourcePositions);
        workWorkstationPos = firstPosition(workWorkstationPositions);
        workDestinationPos = firstPosition(workDestinationPositions);
        workFallbackPos = firstPosition(workFallbackPositions);
        workItemFilter = input.getStringOr("PrimevalWorkItem", "");
        workItemFilters.clear();
        input.listOrEmpty("PrimevalWorkItems", Codec.STRING).stream().filter(value -> !value.isBlank()).distinct().limit(16).forEach(workItemFilters::add);
        if (workItemFilters.isEmpty() && !workItemFilter.isBlank()) {
            workItemFilters.add(workItemFilter);
        }
        workItemFilter = workItemFilters.isEmpty() ? "" : workItemFilters.getFirst();
        workFuelFilters.clear();
        input.listOrEmpty("PrimevalWorkFuels", Codec.STRING).stream().filter(value -> !value.isBlank()).distinct().limit(4).forEach(workFuelFilters::add);
        expeditionTier = Mth.clamp(input.getIntOr("PrimevalExpeditionTier", 0), 0, 4);
        readBlockPriorities(input);
        workPriority = Mth.clamp(input.getIntOr("PrimevalWorkPriority", 1), 0, 3);
        workBatchSize = Mth.clamp(input.getIntOr("PrimevalWorkBatch", 16), 1, 64);
        workSchedule = Mth.clamp(input.getIntOr("PrimevalWorkSchedule", 0), 0, 2);
        workSourceReserve = Mth.clamp(input.getIntOr("PrimevalWorkSourceReserve", 0), 0, 4096);
        workDestinationTarget = Mth.clamp(input.getIntOr("PrimevalWorkDestinationTarget", 0), 0, 4096);
        workRepeatMode = Mth.clamp(input.getIntOr("PrimevalWorkRepeat", 0), 0, 2);
        workRoutePolicy = Mth.clamp(input.getIntOr("PrimevalWorkRoutePolicy", 1), 0, 2);
        workMoodDrainUnits = Mth.clamp(input.getIntOr("PrimevalWorkMoodDrainUnits", 0),
                0, WorkSpecialtyRules.WORK_MOOD_DRAIN_UNITS_PER_POINT - 1);
        exactItemMatch = input.getBooleanOr("PrimevalWorkExact", true);
        avoidDanger = input.getBooleanOr("PrimevalWorkAvoidDanger", true);
        workEnabled = input.getBooleanOr("PrimevalWorkEnabled", commandTablePos != null);
        workerCooldown = Mth.clamp(input.getIntOr("PrimevalWorkerCooldown", 0), 0, 1_200);
        workWorkstationCursor = Math.max(0, input.getIntOr("PrimevalWorkstationCursor", 0));
        int savedWorkAction = Mth.clamp(input.getIntOr("PrimevalWorkAction", 0), 0, 6);
        int savedWorkDuration = Mth.clamp(input.getIntOr("PrimevalWorkActionDuration", 0), 0, 72_000);
        int savedWorkProgress = Mth.clamp(input.getIntOr("PrimevalWorkActionProgress", 0), 0, savedWorkDuration);
        workActionPos = input.getLong("PrimevalWorkActionPos").map(BlockPos::of).orElse(null);
        if (savedWorkAction > 0 && savedWorkDuration > 0 && workActionPos != null
                && (workEnabled || fieldWorkEnabled)) {
            entityData.set(WORK_ACTION, savedWorkAction);
            entityData.set(WORK_ACTION_PROGRESS, savedWorkProgress);
            entityData.set(WORK_ACTION_DURATION, savedWorkDuration);
            entityData.set(WORK_ACTION_POS, Optional.of(workActionPos));
        } else {
            cancelWorkAction();
        }
        entityData.set(SADDLED, input.getBooleanOr("PrimevalSaddled", false));
        entityData.set(PTERO_STAMINA, Mth.clamp(
                input.getFloatOr("PrimevalPteranodonStamina", PTERO_MAX_STAMINA),
                0.0F,
                PTERO_MAX_STAMINA
        ));
        entityData.set(PTERO_EXHAUSTED, input.getBooleanOr("PrimevalPteranodonExhausted", false)
                && getPteranodonStamina() < PTERO_EXHAUSTION_RECOVERY);
        entityData.set(SPINO_LAND_STAMINA, Mth.clamp(
                input.getFloatOr("PrimevalSpinosaurusLandStamina", SPINO_MAX_LAND_STAMINA),
                0.0F,
                SPINO_MAX_LAND_STAMINA
        ));
        entityData.set(SPINO_LAND_EXHAUSTED,
                input.getBooleanOr("PrimevalSpinosaurusLandExhausted", false)
                        && getSpinosaurusLandStamina() < SPINO_LAND_EXHAUSTION_RECOVERY);
        int defeatTransferTime = Mth.clamp(input.getIntOr("PrimevalDefeatTransferTime", 0),
                0, DEFEAT_TRANSFER_TICKS);
        entityData.set(DEFEAT_TRANSFER_TIME, defeatTransferTime);
        if (getSpecies() == DinosaurSpecies.PTERANODON) {
            autonomousTransportFlight = false;
            autonomousTransportTarget = null;
            setNoGravity(false);
            entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_GROUNDED);
            entityData.set(PTERO_AIRSPEED, 0.0F);
            entityData.set(PTERO_BANK, 0.0F);
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            entityData.set(SPINO_SWIMMING, false);
            entityData.set(SPINO_BANK, 0.0F);
            entityData.set(SPINO_SWIM_SPEED, 0.0F);
            entityData.set(SPINO_BREACH_TIME, 0);
            entityData.set(SPINO_LAND_SPRINTING, false);
        }
        onExpedition = input.getBooleanOr("PrimevalOnExpedition", false);
        expeditionEndTick = input.getLongOr("PrimevalExpeditionEnd", 0L);
        incapacitatedUntilTick = input.getLongOr("PrimevalIncapacitatedUntil", 0L);
        pendingOwnerRecovery = input.getBooleanOr("PrimevalPendingOwnerRecovery", false);
        breedingPrimed = input.getBooleanOr("PrimevalBreedingPrimed", false);
        breedingCooldownUntilTick = Math.max(0L, input.getLongOr("PrimevalBreedingCooldownUntil", 0L));
        breedingPartnerId = readSavedUuid(input, "PrimevalBreedingPartner");
        breedingOwnerId = readSavedUuid(input, "PrimevalBreedingOwner");
        breedingCourtshipTicks = Math.max(0, input.getIntOr("PrimevalBreedingCourtshipTicks", 0));
        if (!breedingPrimed || breedingPartnerId == null || breedingOwnerId == null) {
            breedingPartnerId = null;
            breedingOwnerId = null;
            breedingCourtshipTicks = 0;
        }
        noPhysics = onExpedition;
        if (onExpedition) setNoGravity(true);
        setInvisible(onExpedition);
        setInvulnerable(onExpedition);
        if (defeatTransferTime > 0) {
            setHealth(Math.max(1.0F, getHealth()));
            setNoAi(false);
            setInvulnerable(true);
            setNoGravity(true);
            noPhysics = true;
        } else if (pendingOwnerRecovery) {
            setHealth(Math.max(1.0F, getHealth()));
            setInvulnerable(true);
            setNoGravity(false);
            noPhysics = false;
        }
        entityData.set(CARRIED_STACK, input.read("PrimevalCarriedStack", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        if (entityData.get(GENETIC_QUALITY) >= 0) {
            applyGeneticAttributes(false);
        }
    }

    private static void writePositionList(ValueOutput output, String key, List<BlockPos> positions) {
        var saved = output.list(key, BlockPos.CODEC);
        positions.forEach(saved::add);
    }

    private static @Nullable UUID readSavedUuid(ValueInput input, String key) {
        String value = input.getStringOr(key, "");
        try {
            return value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void readPositionList(ValueInput input, String key, List<BlockPos> positions, BlockPos legacy) {
        positions.clear();
        input.listOrEmpty(key, BlockPos.CODEC).stream().map(BlockPos::immutable).distinct().limit(8).forEach(positions::add);
        if (positions.isEmpty() && legacy != null) {
            positions.add(legacy.immutable());
        }
    }

    private void replaceBlockPriorities(Map<BlockPos, Integer> priorities) {
        workBlockPriorities.clear();
        if (priorities == null || priorities.isEmpty()) {
            return;
        }
        priorities.forEach((pos, value) -> {
            if (workBlockPriorities.size() < 33 && isSelectedWorkPosition(pos)) {
                workBlockPriorities.put(pos.immutable(), Mth.clamp(value, 0, 3));
            }
        });
    }

    private boolean isSelectedWorkPosition(BlockPos pos) {
        return pos != null && (workSourcePositions.contains(pos)
                || workWorkstationPositions.contains(pos)
                || workDestinationPositions.contains(pos)
                || workFallbackPositions.contains(pos)
                || pos.equals(workAreaEndPos));
    }

    private void writeBlockPriorities(ValueOutput output) {
        var savedPositions = output.list("PrimevalWorkPriorityPositions", BlockPos.CODEC);
        var savedValues = output.list("PrimevalWorkPriorityValues", Codec.INT);
        workBlockPriorities.forEach((pos, value) -> {
            savedPositions.add(pos);
            savedValues.add(Mth.clamp(value, 0, 3));
        });
    }

    private void readBlockPriorities(ValueInput input) {
        List<BlockPos> positions = input.listOrEmpty("PrimevalWorkPriorityPositions", BlockPos.CODEC).stream().limit(33).toList();
        List<Integer> values = input.listOrEmpty("PrimevalWorkPriorityValues", Codec.INT).stream().limit(33).toList();
        Map<BlockPos, Integer> loaded = new LinkedHashMap<>();
        for (int index = 0; index < Math.min(positions.size(), values.size()); index++) {
            loaded.put(positions.get(index), values.get(index));
        }
        replaceBlockPriorities(loaded);
    }

    @Override
    public void aiStep() {
        pteranodonBankPrevious = getPteranodonBankDegrees();
        spinosaurusBankPrevious = getSpinosaurusBankDegrees();
        super.aiStep();
        sleepVisualTicks = isDinosaurSleeping() ? sleepVisualTicks + 1 : 0;
        updatePteranodonFlightBlend();
        updateLocomotionPresentation();
        if (!level().isClientSide()) {
            if (pendingOwnerRecovery) {
                tickPendingOwnerRecovery();
                return;
            }
            if (isDefeatTransferActive()) return;
            if (entityData.get(GENETIC_QUALITY) < 0 && !isDeadOrDying() && !isRemoved()) {
                initializeGenetics(false);
            }
            if (getSpecies() == DinosaurSpecies.PTERANODON && getControllingPassenger() == null) {
                adjustPteranodonStamina(PTERO_GROUND_STAMINA_RECOVERY);
            }
            tickSleepingState();
            tickThreatAwareness();
            updateCombatSprinting();
            tickRaptorMomentum();
            tickBlinking();
            if (tickBreedingCourtship()) {
                cancelWorkAction();
                return;
            }
            if (entityData.get(ATTACK_ANIMATION_TICKS) > 0) {
                entityData.set(ATTACK_ANIMATION_TICKS, entityData.get(ATTACK_ANIMATION_TICKS) - 1);
            }
            if (spinosaurusMountedAttackCooldown > 0) {
                spinosaurusMountedAttackCooldown--;
            }
            tickSpinosaurusMountedAim();
            if (getSpecies() == DinosaurSpecies.SPINOSAURUS && getControllingPassenger() == null) {
                entityData.set(SPINO_LAND_SPRINTING, false);
                adjustSpinosaurusLandStamina(SPINO_LAND_STAMINA_RECOVERY);
            }
            tickPendingAttack();
            tickWorkMoodDrain();
            tickNeedsAndIndicator();
            runAssignedWork();
            if (autonomousTransportFlight && (!isAutonomousPteranodonFlightAllowed()
                    || isVehicle() || isDinosaurSleeping()
                    || getTarget() != null || onExpedition)) {
                stopAutonomousTransportFlight();
            }
            if ((tickCount + getId()) % OWNERSHIP_SYNC_INTERVAL_TICKS == 0) {
                DinosaurOwnership.syncRecord(this);
            }
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide()
                && (!worldAuthorityChecked || Math.floorMod(tickCount + getId(), 20) == 0)
                && !DinosaurOwnership.hasActiveWorldAuthority(this)) {
            discard();
            return;
        }
        worldAuthorityChecked = true;
        super.tick();
        if (!level().isClientSide() && isDefeatTransferActive()) {
            tickDefeatTransfer();
        }
    }

    private void updatePteranodonFlightBlend() {
        if (getSpecies() != DinosaurSpecies.PTERANODON) {
            return;
        }
        float target = entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_GROUNDED ? 0.0F : 1.0F;
        pteranodonFlightBlend = Mth.approach(pteranodonFlightBlend, target, 1.0F / 8.0F);
        pteranodonFlightBlendVelocity = target - pteranodonFlightBlend;

        pteranodonAnimationSpeedPrevious = pteranodonAnimationSpeed;
        float animationTarget = targetPteranodonAnimationSpeed();
        float animationResponse = animationTarget > pteranodonAnimationSpeed ? 0.16F : 0.10F;
        pteranodonAnimationSpeed = Mth.lerp(animationResponse, pteranodonAnimationSpeed, animationTarget);

        pteranodonRiderFlapPhasePrevious = pteranodonRiderFlapPhase;
        pteranodonRiderFlapBlendPrevious = pteranodonRiderFlapBlend;
        float flapTarget = isPteranodonAirborne() && !isPteranodonGliding() ? 1.0F : 0.0F;
        pteranodonRiderFlapBlend = Mth.lerp(
                flapTarget > pteranodonRiderFlapBlend ? 0.22F : 0.16F,
                pteranodonRiderFlapBlend,
                flapTarget
        );
        if (pteranodonRiderFlapBlend > 0.001F) {
            double phaseRate = isPteranodonHovering() ? Mth.TWO_PI / 20.0D : Mth.PI / 20.0D;
            pteranodonRiderFlapPhase += phaseRate * pteranodonAnimationSpeed;
        }
        pteranodonRiderRootPitchPrevious = pteranodonRiderRootPitch;
        float rootPitchTarget = isPteranodonHovering() ? -18.0F : 0.0F;
        pteranodonRiderRootPitch = Mth.approach(pteranodonRiderRootPitch, rootPitchTarget, 2.25F);

        int flightMode = entityData.get(PTERO_FLIGHT_MODE);
        if (flightMode == PTERO_FLIGHT_POWERED) {
            pteranodonPoweredAnimationHoldTicks = 7;
        } else if (pteranodonPoweredAnimationHoldTicks > 0) {
            pteranodonPoweredAnimationHoldTicks--;
        }
        pteranodonRiderPosturePrevious = pteranodonRiderPosture;
        float postureTarget = switch (flightMode) {
            case PTERO_FLIGHT_HOVERING -> -9.0F;
            case PTERO_FLIGHT_GLIDING -> 8.0F;
            case PTERO_FLIGHT_POWERED -> 17.0F;
            default -> 0.0F;
        };
        pteranodonRiderPosture = Mth.approach(pteranodonRiderPosture, postureTarget, 2.75F);
    }

    private float targetPteranodonAnimationSpeed() {
        int mode = entityData.get(PTERO_FLIGHT_MODE);
        if (mode == PTERO_FLIGHT_GROUNDED) {
            return 1.0F;
        }
        float horizontalSpeed = Math.max(
                (float)getDeltaMovement().horizontalDistance(),
                entityData.get(PTERO_AIRSPEED)
        );
        float normalizedSpeed = Mth.clamp(
                horizontalSpeed / (float)(PTERO_BOOST_SPEED * getMutationStatMultiplier()
                        * PrimevalTuning.server().pteranodonFlightSpeed()), 0.0F, 1.0F
        );
        if (mode == PTERO_FLIGHT_HOVERING) {
            return 0.92F;
        }
        if (mode == PTERO_FLIGHT_GLIDING) {
            return Mth.lerp(normalizedSpeed, 0.70F, 1.04F);
        }
        float verticalEffort = Mth.clamp(Math.abs((float)getDeltaMovement().y) / 0.72F, 0.0F, 1.0F);
        float speedCurve = (float)Math.pow(normalizedSpeed, 0.72D);
        float firstFlapBurst = Mth.clamp(1.0F - pteranodonForwardHoldTicks / 9.0F, 0.0F, 1.0F) * 0.42F;
        return Mth.clamp(0.72F + speedCurve * 1.12F + verticalEffort * 0.14F + firstFlapBurst,
                0.72F, 2.12F);
    }

    private void updateLocomotionPresentation() {
        float smoothedWalkSpeed = walkAnimation.speed();
        boolean translating = smoothedWalkSpeed > 0.018F;
        turnAnimationHoldTicks = 0;
        turnAnimationSettleTicks = 0;
        turnAnimationDirection = 0;
        turnYawAccumulator = 0.0F;
        if (translating) {
            walkAnimationHoldTicks = 6;
        } else if (walkAnimationHoldTicks > 0) {
            walkAnimationHoldTicks--;
        }
        if (isSprinting()) {
            runAnimationHoldTicks = 6;
        } else if (runAnimationHoldTicks > 0) {
            runAnimationHoldTicks--;
        }
        if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR) {
            raptorAnimationSpeedPrevious = raptorAnimationSpeed;
            float horizontalSpeed = (float)getDeltaMovement().horizontalDistance();
            float target = usesRunAnimation()
                    ? Mth.clamp(0.78F + horizontalSpeed * 2.45F, 0.82F, 2.30F)
                    : Mth.clamp(0.76F + horizontalSpeed * 1.85F, 0.76F, 1.34F);
            float response = target > raptorAnimationSpeed ? 0.18F : 0.11F;
            raptorAnimationSpeed = Mth.lerp(response, raptorAnimationSpeed, target);
        }
    }

    private void tickSleepingState() {
        boolean night = isNightWorkWindow();
        boolean nightDuty = commandMode == DinosaurCommandMode.FOLLOW
                || commandMode == DinosaurCommandMode.HOME
                && workEnabled && commandTablePos != null && (workSchedule == 0 || workSchedule == 2);
        boolean safeToStayAsleep = night
                && !nightDuty
                && !isVehicle()
                && !onExpedition
                && getTarget() == null
                && getHunger() >= PrimevalTuning.server().foodBoxThreshold()
                && !isInWater();
        if (isVehicle()) {
            if (isDinosaurSleeping()) entityData.set(DINOSAUR_SLEEPING, false);
            cancelWorkAction();
            return;
        }
        if (isDinosaurSleeping()) {
            if (!safeToStayAsleep) {
                entityData.set(DINOSAUR_SLEEPING, false);
            } else {
                navigation.stop();
                setDeltaMovement(Vec3.ZERO);
                cancelWorkAction();
            }
        } else if (safeToStayAsleep && onGround() && hasSuitableSleepingArea() && random.nextInt(80) == 0) {
            entityData.set(DINOSAUR_SLEEPING, true);
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);
            cancelWorkAction();
        }
    }

    public boolean hasSuitableSleepingArea() {
        if (!getSpecies().heavyweight()) return true;
        BlockPos centerSupport = BlockPos.containing(getX(), getY() - 0.05D, getZ());
        double expectedTop = Double.NaN;
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                BlockPos supportPos = centerSupport.offset(offsetX, 0, offsetZ);
                BlockState support = level().getBlockState(supportPos);
                var shape = support.getCollisionShape(level(), supportPos);
                if (shape.isEmpty() || !support.isFaceSturdy(level(), supportPos, Direction.UP)) return false;
                double top = supportPos.getY() + shape.max(Direction.Axis.Y);
                if (Double.isNaN(expectedTop)) expectedTop = top;
                if (Math.abs(top - expectedTop) > 0.08D || Math.abs(top - getY()) > 0.16D) return false;
            }
        }
        return true;
    }

    private void updateCombatSprinting() {
        if (getSpecies() == DinosaurSpecies.PTERANODON && isVehicle()) {
            return;
        }
        LivingEntity target = getTarget();
        Vec3 movement = getDeltaMovement();
        boolean movingTowardThreat = movement.x * movement.x + movement.z * movement.z > 0.0025D;
        setSprinting(!isDinosaurSleeping()
                && getSpecies().combatCapable()
                && target instanceof Monster
                && target.isAlive()
                && movingTowardThreat);
    }

    private void tickRaptorMomentum() {
        if (getSpecies() != DinosaurSpecies.VELOCIRAPTOR) {
            raptorMomentum = 0.0F;
            raptorPounceTicks = 0;
            raptorTransportRunTicks = 0;
            raptorRunAnimationHoldTicks = 0;
            entityData.set(RAPTOR_RUNNING, false);
            return;
        }
        if (raptorPounceTicks > 0) raptorPounceTicks--;
        if (raptorTransportRunTicks > 0) raptorTransportRunTicks--;
        boolean pursuitActive = RaptorMomentumRules.pursuitActive(
                isRaptorTransportPursuitActive(),
                getTarget() != null && getTarget().isAlive(),
                raptorPounceTicks > 0
        );
        boolean mayRun = pursuitActive
                && !isDinosaurSleeping()
                && getWorkAction() == 0
                && (onGround() || raptorPounceTicks > 0);
        boolean advancing = mayRun && (raptorPounceTicks > 0
                || getDeltaMovement().horizontalDistanceSqr() > 0.0016D);
        if (advancing) {
            raptorRunAnimationHoldTicks = 8;
        } else if (raptorRunAnimationHoldTicks > 0) {
            raptorRunAnimationHoldTicks--;
        }
        raptorMomentum = RaptorMomentumRules.nextMomentum(raptorMomentum, advancing);
        entityData.set(RAPTOR_RUNNING, mayRun && raptorRunAnimationHoldTicks > 0
                && raptorMomentum > 0.06F);
    }

    private boolean isRaptorTransportPursuitActive() {
        boolean transporting = raptorTransportRunTicks > 0
                && isTransportWorkActive()
                && commandTablePos != null
                && workerCooldown <= 0
                && (commandMode == DinosaurCommandMode.FOLLOW || scheduleAllowsWork());
        ServerPlayer owner = commandMode == DinosaurCommandMode.FOLLOW ? commandOwner() : null;
        boolean catchingOwner = fieldDutyAllowsOwnerPursuit() && owner != null && owner.level() == level()
                && distanceToSqr(owner) > 100.0D;
        return !onExpedition
                && getHunger() >= PrimevalTuning.server().foodBoxThreshold()
                && (transporting || catchingOwner);
    }

    private void tickThreatAwareness() {
        if (onExpedition || tickCount % 10 != 0) {
            return;
        }

        ServerPlayer owner = commandOwner();
        if (commandMode == DinosaurCommandMode.FOLLOW && fieldDutyAllowsOwnerPursuit()
                && owner != null && getSpecies().combatCapable()) {
            LivingEntity attacked = owner.getLastHurtMob();
            LivingEntity attacker = owner.getLastHurtByMob();
            LivingEntity requestedTarget = attacked != null && attacked.isAlive() ? attacked
                    : attacker != null && attacker.isAlive() ? attacker : null;
            if (requestedTarget != null
                    && requestedTarget != this
                    && requestedTarget.distanceToSqr(owner) <= 32.0D * 32.0D
                    && canAttack(requestedTarget)) {
                setTarget(requestedTarget);
            }
        }

        List<Monster> nearbyThreats = level().getEntitiesOfClass(
                Monster.class,
                getBoundingBox().inflate(18.0D + threatAwarenessBonus(), 8.0D, 18.0D + threatAwarenessBonus()),
                threat -> threat.isAlive() && insideBaseBoundary(threat.position(), 6.0D)
        );
        for (Monster threat : nearbyThreats) {
            LivingEntity threatTarget = threat.getTarget();
            if ((threatTarget == null || !threatTarget.isAlive()) && threat.canAttack(this)) {
                threat.setTarget(this);
            }
        }

        if (getSpecies().combatCapable() && (getTarget() == null || !getTarget().isAlive())) {
            nearbyThreats.stream()
                    .filter(this::canAttack)
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .ifPresent(this::setTarget);
        }
    }

    private void tickBlinking() {
        if (isDinosaurSleeping()) {
            entityData.set(EYES_CLOSED, false);
            blinkTicksRemaining = 0;
            ticksUntilBlink = -1;
            secondBlinkQueued = false;
            return;
        }
        if (blinkTicksRemaining > 0) {
            blinkTicksRemaining--;
            if (blinkTicksRemaining == 0) {
                entityData.set(EYES_CLOSED, false);
                if (secondBlinkQueued) {
                    secondBlinkQueued = false;
                    ticksUntilBlink = 3;
                } else {
                    ticksUntilBlink = 60 + random.nextInt(101);
                }
            }
            return;
        }

        if (ticksUntilBlink < 0) {
            ticksUntilBlink = 40 + random.nextInt(101);
            return;
        }

        if (--ticksUntilBlink <= 0) {
            entityData.set(EYES_CLOSED, true);
            blinkTicksRemaining = 3;
            secondBlinkQueued = random.nextInt(8) == 0;
        }
    }

    private void tickPendingAttack() {
        if (pendingAttackTargetId < 0) {
            return;
        }

        if (level().getEntity(pendingAttackTargetId) instanceof LivingEntity watchedTarget
                && watchedTarget.isAlive()) {
            getLookControl().setLookAt(watchedTarget, getSpecies().turnDegreesPerTick() * 2.0F, 32.0F);
            yHeadRot = Mth.approachDegrees(yHeadRot, yawTo(watchedTarget),
                    getSpecies().turnDegreesPerTick() * 1.75F);
            if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR
                    && getBoundingBox().inflate(0.55D, 0.35D, 0.55D)
                    .intersects(watchedTarget.getBoundingBox())) {
                raptorPounceContactConfirmed = true;
            }
        }
        if (level().getGameTime() < pendingAttackContactTick) return;

        int targetId = pendingAttackTargetId;
        pendingAttackTargetId = -1;
        pendingAttackContactTick = Long.MAX_VALUE;
        boolean raptorContact = raptorPounceContactConfirmed;
        raptorPounceContactConfirmed = false;
        if (level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(targetId) instanceof LivingEntity target
                && target.isAlive()
                && (getSpecies() == DinosaurSpecies.VELOCIRAPTOR
                        ? raptorContact || isRaptorPounceContact(target)
                        : isWithinMeleeAttackRange(target))
                && getSensing().hasLineOfSight(target)) {
            boolean landed = super.doHurtTarget(serverLevel, target);
            if (landed && getSpecies() == DinosaurSpecies.VELOCIRAPTOR) {
                Vec3 launch = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
                if (launch.horizontalDistanceSqr() < 1.0E-5D) {
                    launch = Vec3.directionFromRotation(0.0F, getYRot());
                } else {
                    launch = launch.normalize();
                }
                target.push(launch.x * 0.82D, 0.30D, launch.z * 0.82D);
            }
        }
    }

    private boolean isRaptorPounceContact(LivingEntity target) {
        double horizontalReach = getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + 2.35D;
        return horizontalDistanceTo(target) <= horizontalReach
                && Math.abs(target.getY() - getY()) <= Math.max(2.25D, target.getBbHeight() + 0.5D);
    }

    private void tickNeedsAndIndicator() {
        if (hungerIndicatorCooldown > 0) {
            hungerIndicatorCooldown--;
        }
        int indicatorTicks = entityData.get(INDICATOR_TICKS);
        if (indicatorTicks > 0) {
            entityData.set(INDICATOR_AGE, entityData.get(INDICATOR_AGE) + 1);
            entityData.set(INDICATOR_TICKS, indicatorTicks - 1);
            if (indicatorTicks == 1) {
                entityData.set(INDICATOR_ICON, 0);
                entityData.set(INDICATOR_AGE, 0);
            }
        }
        if (tickCount % 1200 == 0) {
            int moodChange = getHunger() < 25 ? -5 : getHunger() > 70 ? 2 : 0;
            entityData.set(MOOD, Mth.clamp(getMood() + moodChange, 0, 100));
        }
        if (!onExpedition && entityData.get(INDICATOR_TICKS) == 0 && random.nextInt(6000) == 0) {
            showIndicator(getMood() >= 70 ? 1 : getMood() >= 35 ? 2 : 3, 64);
        }
        if (!onExpedition && getHunger() <= 18 && hungerIndicatorCooldown == 0
                && entityData.get(INDICATOR_ICON) < 4) {
            showIndicator(4, 70);
            hungerIndicatorCooldown = 600;
        } else if (getHunger() > 18) {
            hungerIndicatorCooldown = 0;
        }
    }

    private void tickWorkMoodDrain() {
        boolean baseDuty = commandMode == DinosaurCommandMode.HOME && workEnabled
                && commandTablePos != null
                && scheduleAllowsWork();
        boolean fieldDuty = fieldDutyOwnsNavigation();
        boolean onDuty = (baseDuty || fieldDuty)
                && !onExpedition
                && level().getGameTime() >= incapacitatedUntilTick
                && getTarget() == null
                && !isDinosaurSleeping()
                && getHunger() > 10;
        if (!onDuty) {
            return;
        }

        int drainUnits = WorkSpecialtyRules.workMoodDrainUnitsPerTick(
                isNightWorkWindow() ? 2 : workSchedule);
        if (drainUnits <= 0) return;
        CommandTableBlockEntity table = commandTableEntity();
        if (table != null) {
            drainUnits = Math.max(1, Math.round(drainUnits * table.moodDrainMultiplier()));
        }
        if (getSpecies() != DinosaurSpecies.PARASAUROLOPHUS && hasCalmingCallNearby()) {
            drainUnits = Math.max(1, Math.round(drainUnits
                    * Math.max(0.68F, 1.0F - 0.18F * calmingCallStrengthCached)));
        }
        drainUnits = Math.max(1, Math.round(drainUnits
                * DinosaurGeneticPerformanceRules.moodDrainMultiplier(getGeneticQuality())));
        workMoodDrainUnits += drainUnits;
        while (workMoodDrainUnits >= WorkSpecialtyRules.WORK_MOOD_DRAIN_UNITS_PER_POINT) {
            workMoodDrainUnits -= WorkSpecialtyRules.WORK_MOOD_DRAIN_UNITS_PER_POINT;
            entityData.set(MOOD, Math.max(0, getMood() - 1));
        }
    }

    private boolean hasCalmingCallNearby() {
        if (calmingCallCacheTicks-- > 0) {
            return calmingCallCached;
        }
        UUID owner = getDinosaurOwner().orElse(null);
        if (owner == null) {
            calmingCallCacheTicks = 39;
            calmingCallCached = false;
            calmingCallStrengthCached = 1.0F;
            return false;
        }
        calmingCallCacheTicks = 39;
        List<FieldDodoEntity> calmingDinosaurs = level().getEntitiesOfClass(
                        FieldDodoEntity.class,
                        getBoundingBox().inflate(12.0D, 5.0D, 12.0D),
                        dinosaur -> dinosaur != this
                                && dinosaur.isAlive()
                                && dinosaur.getSpecies() == DinosaurSpecies.PARASAUROLOPHUS
                                && dinosaur.isOwnedBy(owner)
                );
        calmingCallCached = !calmingDinosaurs.isEmpty();
        calmingCallStrengthCached = calmingDinosaurs.stream()
                .map(FieldDodoEntity::getPassiveStrength)
                .max(Float::compare)
                .orElse(1.0F);
        return calmingCallCached;
    }

    private void showIndicator(int icon, int ticks) {
        if (entityData.get(INDICATOR_ICON) != icon) {
            entityData.set(INDICATOR_ICON, icon);
            entityData.set(INDICATOR_AGE, 0);
        }
        entityData.set(INDICATOR_TICKS, Math.max(entityData.get(INDICATOR_TICKS), Math.max(1, ticks)));
    }

    private void requestCappedHungerDrain() {
        float multiplier = 1.0F;
        CommandTableBlockEntity table = commandTableEntity();
        if (table != null) multiplier = table.hungerIntervalMultiplier();
        DinosaurNeedsRules.DrainResult result = DinosaurNeedsRules.hungerDrain(
                level().getGameTime(),
                nextHungerDrainTick,
                getSpecies().hungerDrainIntervalTicks(),
                multiplier * DinosaurGeneticPerformanceRules.hungerIntervalMultiplier(getGeneticQuality())
        );
        nextHungerDrainTick = result.nextDrainTick();
        if (result.drain()) feed(-1);
    }

    private void runAssignedWork() {
        if (breedingPartnerId != null) {
            cancelWorkAction();
            navigation.stop();
            return;
        }
        requestCappedHungerDrain();
        if (isVehicle()) {
            cancelWorkAction();
            navigation.stop();
            return;
        }
        if (isDinosaurSleeping()) {
            cancelWorkAction();
            navigation.stop();
            return;
        }
        if (getSpecies().combatCapable() && getTarget() != null) {
            cancelWorkAction();
            return;
        }
        if (onExpedition) {
            cancelWorkAction();
            tickExpedition();
            return;
        }
        if (level().getGameTime() < incapacitatedUntilTick) {
            cancelWorkAction();
            navigation.stop();
            return;
        }
        if (commandMode != DinosaurCommandMode.FOLLOW && seekFoodWhenHungry()) {
            cancelWorkAction();
            return;
        }
        if (commandMode == DinosaurCommandMode.STAY) {
            cancelWorkAction();
            return;
        }
        if (commandMode == DinosaurCommandMode.FOLLOW) {
            if (fieldDutyOwnsNavigation()) runFieldWork();
            else cancelWorkAction();
            return;
        }
        if (!workEnabled || commandTablePos == null || getHunger() <= 10 || !scheduleAllowsWork()) {
            cancelWorkAction();
            return;
        }
        CommandTableBlockEntity table = commandTableEntity();
        if (table == null || !assignedWorkTargetsInsideBase(table.baseRadius())) {
            workEnabled = false;
            cancelWorkAction();
            navigation.stop();
            DinosaurOwnership.syncRecord(this);
            return;
        }
        if (workerCooldown > 0) {
            workerCooldown--;
            cancelWorkAction();
            return;
        }
        switch (workJobIndex) {
            case 0 -> runTransportWork();
            case 1 -> runFireWork();
            case 2 -> runEnergyWork();
            case 3 -> runCraftingWork();
            case 4 -> startExpedition();
            default -> cancelWorkAction();
        }
    }

    private boolean tickBreedingCourtship() {
        if (breedingPartnerId == null || !(level() instanceof ServerLevel serverLevel)) return false;
        Entity found = serverLevel.getEntity(breedingPartnerId);
        if (!(found instanceof FieldDodoEntity partner)
                || !partner.isAlive()
                || partner.getSpecies() != getSpecies()
                || !partner.isBreedingWith(getUUID())) {
            if (++breedingPartnerMissingTicks > 200) clearBreedingCourtship();
            return true;
        }
        breedingPartnerMissingTicks = 0;
        setTarget(null);
        entityData.set(DINOSAUR_SLEEPING, false);
        double desiredDistance = (getBbWidth() + partner.getBbWidth()) * 0.5D + 0.7D;
        double dx = partner.getX() - getX();
        double dz = partner.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        getLookControl().setLookAt(partner, 22.0F, 22.0F);
        if (horizontalDistance > desiredDistance) {
            breedingCourtshipTicks = 0;
            navigation.moveTo(partner, 1.05D);
            return true;
        }
        navigation.stop();
        setDeltaMovement(getDeltaMovement().multiply(0.25D, 1.0D, 0.25D));
        breedingCourtshipTicks++;
        if (breedingCourtshipTicks % 10 == 0) {
            DinosaurBreeding.showHearts(serverLevel, this, 2);
        }
        if (getUUID().compareTo(partner.getUUID()) < 0
                && breedingCourtshipTicks >= 50
                && breedingOwnerId != null) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(breedingOwnerId);
            DinosaurBreeding.completeCourtship(owner, breedingOwnerId, this, partner);
        }
        return true;
    }

    private boolean assignedWorkTargetsInsideBase(int radius) {
        if (commandTablePos == null) return false;
        double radiusSquared = (double)radius * radius;
        for (List<BlockPos> positions : List.of(
                workSourcePositions,
                workWorkstationPositions,
                workDestinationPositions,
                workFallbackPositions
        )) {
            if (positions.stream().anyMatch(pos -> pos.distSqr(commandTablePos) > radiusSquared)) return false;
        }
        return workAreaEndPos == null || workAreaEndPos.distSqr(commandTablePos) <= radiusSquared;
    }

    private void runFieldWork() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner = commandOwner();
        if (owner == null || owner.level() != level() || !owner.isAlive()) {
            cancelWorkAction();
            navigation.stop();
            return;
        }
        if (fieldWorkFirst == null && fieldWorkMode.isPassive()) {
            fieldWorkFirst = blockPosition().immutable();
            entityData.set(FIELD_WORK_FIRST, Optional.of(fieldWorkFirst));
            DinosaurOwnership.syncRecord(this);
        }
        if (fieldWorkFirst == null) {
            clearFieldWork();
            return;
        }
        if (getHunger() <= 10) {
            cancelWorkAction();
            navigation.stop();
            return;
        }
        int rating = DinoFieldWorkRules.rating(this, fieldWorkMode);
        if (rating <= 0) {
            clearFieldWork();
            return;
        }
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.COLLECT) {
            runFieldCollection(owner);
            return;
        }
        if (fieldWorkTargets.isEmpty() || fieldWorkCursor >= fieldWorkTargets.size()) {
            if (fieldWorkContinuous && fieldWorkRescanCooldown-- > 0) {
                cancelWorkAction();
                keepInsideFieldLeash();
                return;
            }
            rebuildFieldTargets();
            if (fieldWorkTargets.isEmpty()) {
                cancelWorkAction();
                if (fieldWorkContinuous) {
                    fieldWorkRescanCooldown = 80;
                    keepInsideFieldLeash();
                } else {
                    clearFieldWork();
                }
                return;
            }
        }
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.LUMBER && lumberFastFelling) {
            fellLumberBatch(serverLevel, owner, rating);
            return;
        }
        BlockPos target = fieldWorkTargets.get(fieldWorkCursor);
        if (!DinoFieldWorkRules.validTarget(level(), target, fieldWorkMode, rating)) {
            fieldWorkCursor++;
            fieldTargetApproachTicks = 0;
            cancelWorkAction();
            return;
        }
        if (!closeToFieldTarget(target)) {
            if (++fieldTargetApproachTicks > 600) {
                fieldTargetApproachTicks = 0;
                fieldWorkCursor++;
                navigation.stop();
                navigationTarget = null;
                cancelWorkAction();
                if (!fieldWorkContinuous && fieldWorkCursor >= fieldWorkTargets.size()) clearFieldWork();
                return;
            }
            moveTo(target);
            return;
        }
        fieldTargetApproachTicks = 0;
        int duration = fieldWorkDuration(
                DinoFieldWorkRules.workTicks(level().getBlockState(target), level(), target, rating));
        if (!advanceFieldWorkAction(target, duration)) return;
        if (breakFieldBlock(serverLevel, owner, target, rating)) {
            requestCappedHungerDrain();
            awardWorkExperience(DinosaurProgression.workExperience(fieldSupportJobIndex()));
        }
        fieldWorkCursor++;
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.LUMBER
                && fieldWorkCursor < fieldWorkTargets.size()) {
            lumberFastFelling = true;
            fieldWorkFirst = fieldWorkTargets.get(fieldWorkCursor).immutable();
            entityData.set(FIELD_WORK_FIRST, Optional.of(fieldWorkFirst));
            workerCooldown = 0;
            return;
        }
        if (!fieldWorkContinuous && fieldWorkCursor >= fieldWorkTargets.size()) {
            clearFieldWork();
        } else {
            workerCooldown = 3;
        }
    }

    private void rebuildFieldTargets() {
        fieldWorkTargets.clear();
        fieldWorkCursor = 0;
        fieldTargetApproachTicks = 0;
        int rating = DinoFieldWorkRules.rating(this, fieldWorkMode);
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.HARVEST) {
            rebuildNearbyHarvestTargets(rating);
            return;
        }
        if (fieldWorkPattern == DinoWhistleSettings.Pattern.SINGLE || fieldWorkSecond == null
                && fieldWorkPattern == DinoWhistleSettings.Pattern.AREA) {
            if (DinoFieldWorkRules.validTarget(level(), fieldWorkFirst, fieldWorkMode, rating)) {
                fieldWorkTargets.add(fieldWorkFirst.immutable());
            }
            return;
        }
        if (fieldWorkPattern == DinoWhistleSettings.Pattern.AREA) {
            if (!DinoFieldWorkRules.areaWithinLimits(fieldWorkFirst, fieldWorkSecond, dinosaurLevel)) return;
            BlockPos min = new BlockPos(Math.min(fieldWorkFirst.getX(), fieldWorkSecond.getX()),
                    Math.min(fieldWorkFirst.getY(), fieldWorkSecond.getY()),
                    Math.min(fieldWorkFirst.getZ(), fieldWorkSecond.getZ()));
            BlockPos max = new BlockPos(Math.max(fieldWorkFirst.getX(), fieldWorkSecond.getX()),
                    Math.max(fieldWorkFirst.getY(), fieldWorkSecond.getY()),
                    Math.max(fieldWorkFirst.getZ(), fieldWorkSecond.getZ()));
            for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
                if (fieldWorkTargets.size() >= DinoFieldWorkRules.MAX_AREA_BLOCKS) break;
                if (DinoFieldWorkRules.validTarget(level(), candidate, fieldWorkMode, rating)) {
                    fieldWorkTargets.add(candidate.immutable());
                }
            }
            fieldWorkTargets.sort(Comparator.comparingDouble(pos -> distanceToSqr(pos.getCenter())));
            return;
        }
        BlockState originState = level().getBlockState(fieldWorkFirst);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(fieldWorkFirst.immutable());
        while (!queue.isEmpty() && fieldWorkTargets.size() < DinoFieldWorkRules.MAX_CONNECTED_BLOCKS) {
            BlockPos candidate = queue.removeFirst();
            if (!visited.add(candidate) || candidate.distManhattan(fieldWorkFirst) > 12) continue;
            BlockState state = level().getBlockState(candidate);
            boolean connectedMatch = switch (fieldWorkMode) {
                case QUARRY -> state.is(originState.getBlock());
                case LUMBER -> state.is(BlockTags.LOGS);
                case HARVEST -> state.is(originState.getBlock());
                case COLLECT -> false;
            };
            if (!connectedMatch || !DinoFieldWorkRules.validTarget(level(), candidate, fieldWorkMode, rating)) continue;
            fieldWorkTargets.add(candidate.immutable());
            if (fieldWorkMode == DinoWhistleSettings.FieldMode.LUMBER) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x != 0 || y != 0 || z != 0) queue.addLast(candidate.offset(x, y, z));
                        }
                    }
                }
            } else {
                for (Direction direction : Direction.values()) queue.addLast(candidate.relative(direction));
            }
        }
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.LUMBER) {
            fieldWorkTargets.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingDouble(pos -> distanceToSqr(pos.getCenter())));
        } else {
            fieldWorkTargets.sort(Comparator.comparingDouble(pos -> distanceToSqr(pos.getCenter())));
        }
    }

    private void fellLumberBatch(ServerLevel serverLevel, ServerPlayer owner, int rating) {
        navigation.stop();
        setSpeed(0.0F);
        setDeltaMovement(Vec3.ZERO);
        int broken = 0;
        while (fieldWorkCursor < fieldWorkTargets.size() && broken < 4) {
            BlockPos target = fieldWorkTargets.get(fieldWorkCursor++);
            if (!DinoFieldWorkRules.validTarget(
                    serverLevel, target, DinoWhistleSettings.FieldMode.LUMBER, rating)) continue;
            getLookControl().setLookAt(target.getCenter());
            if (breakFieldBlock(serverLevel, owner, target, rating)) broken++;
        }
        if (broken > 0) {
            requestCappedHungerDrain();
            awardWorkExperience(DinosaurProgression.workExperience(fieldSupportJobIndex()));
        }
        if (fieldWorkCursor >= fieldWorkTargets.size()) {
            clearFieldWork();
            return;
        }
        fieldWorkFirst = fieldWorkTargets.get(fieldWorkCursor).immutable();
        entityData.set(FIELD_WORK_FIRST, Optional.of(fieldWorkFirst));
    }

    private void rebuildNearbyHarvestTargets(int rating) {
        BlockPos center = fieldWorkFirst == null ? blockPosition() : fieldWorkFirst;
        int radius = fieldWorkRange;
        BlockPos min = center.offset(-radius, -2, -radius);
        BlockPos max = center.offset(radius, 2, radius);
        for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
            if (fieldWorkTargets.size() >= 128) break;
            if (DinoFieldWorkRules.validTarget(level(), candidate, DinoWhistleSettings.FieldMode.HARVEST, rating)) {
                fieldWorkTargets.add(candidate.immutable());
            }
        }
        fieldWorkTargets.sort(Comparator.comparingDouble(pos -> distanceToSqr(pos.getCenter())));
    }

    private boolean breakFieldBlock(ServerLevel serverLevel, ServerPlayer owner, BlockPos target, int rating) {
        if (!serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)
                || !serverLevel.isLoaded(target)) return false;
        BlockState state = serverLevel.getBlockState(target);
        if (!DinoFieldWorkRules.validTarget(serverLevel, target, fieldWorkMode, rating)) return false;
        ItemStack tool = switch (fieldWorkMode) {
            case QUARRY -> new ItemStack(rating >= 4 ? Items.DIAMOND_PICKAXE : rating >= 2 ? Items.IRON_PICKAXE : Items.STONE_PICKAXE);
            case LUMBER -> new ItemStack(rating >= 4 ? Items.DIAMOND_AXE : rating >= 2 ? Items.IRON_AXE : Items.STONE_AXE);
            case HARVEST, COLLECT -> ItemStack.EMPTY;
        };
        if (!owner.mayUseItemAt(target, Direction.UP, tool)
                || CommonHooks.fireBlockBreak(serverLevel, owner.gameMode.getGameModeForPlayer(), owner, target, state).isCanceled()) {
            return false;
        }
        BlockEntity blockEntity = serverLevel.getBlockEntity(target);
        Block.dropResources(state, serverLevel, target, blockEntity, this, tool);
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.HARVEST && state.getBlock() instanceof CropBlock crop) {
            serverLevel.setBlock(target, crop.getStateForAge(0), Block.UPDATE_ALL);
        } else {
            serverLevel.destroyBlock(target, false, this, Block.UPDATE_LIMIT);
        }
        return true;
    }

    private void runFieldCollection(ServerPlayer owner) {
        ItemStack carried = getCarriedStack();
        if (!carried.isEmpty()) {
            fieldCollectionTargetId = null;
            fieldCollectionApproachTicks = 0;
            Vec3 anchor = fieldWorkFirst == null ? position() : fieldWorkFirst.getCenter();
            double handoffRange = Math.max(DinoWhistleSettings.MIN_RANGE, fieldWorkRange);
            boolean ownerAtField = owner.position().distanceToSqr(anchor) <= handoffRange * handoffRange;
            if (ownerAtField && distanceToSqr(owner) > 7.0D) {
                markRaptorTransportRoute();
                moveTo(owner.blockPosition());
                return;
            }
            if (!ownerAtField) {
                cancelWorkAction();
                keepInsideFieldLeash();
                return;
            }
            ItemStack remaining = carried.copy();
            owner.getInventory().add(remaining);
            entityData.set(CARRIED_STACK, remaining);
            if (remaining.isEmpty()) {
                workerCooldown = 4;
                requestCappedHungerDrain();
            }
            return;
        }
        AABB searchArea = fieldCollectionArea();
        ItemEntity item = fieldCollectionTarget();
        if (item == null && fieldWorkRescanCooldown-- > 0) {
            cancelWorkAction();
            keepInsideFieldLeash();
            return;
        }
        long gameTime = level().getGameTime();
        fieldCollectionRetryAfter.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
        if (item == null) item = level().getEntitiesOfClass(ItemEntity.class, searchArea,
                        entity -> entity.isAlive() && !entity.getItem().isEmpty()
                                && fieldCollectionRetryAfter.getOrDefault(entity.getUUID(), 0L) <= gameTime
                                && (fieldWorkItemFilter.isBlank()
                                || matchesIdentifiers(List.of(fieldWorkItemFilter), entity.getItem())))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (item == null) {
            fieldCollectionTargetId = null;
            fieldCollectionApproachTicks = 0;
            cancelWorkAction();
            navigation.stop();
            navigationTarget = null;
            if (fieldWorkContinuous) {
                fieldWorkRescanCooldown = 40;
                keepInsideFieldLeash();
            }
            else clearFieldWork();
            return;
        }
        fieldWorkRescanCooldown = 0;
        if (!item.getUUID().equals(fieldCollectionTargetId)) fieldCollectionApproachTicks = 0;
        fieldCollectionTargetId = item.getUUID();
        double deltaX = item.getX() - getX();
        double deltaZ = item.getZ() - getZ();
        double horizontalDistance = deltaX * deltaX + deltaZ * deltaZ;
        double verticalReach = Math.max(3.25D, getBbHeight() + 1.25D);
        boolean withinPickupReach = horizontalDistance <= 3.0D
                && Math.abs(item.getY() - getY()) <= verticalReach;
        if (!withinPickupReach) {
            if (++fieldCollectionApproachTicks > 400) {
                fieldCollectionRetryAfter.put(item.getUUID(), gameTime + 200L);
                fieldCollectionTargetId = null;
                fieldCollectionApproachTicks = 0;
                navigation.stop();
                navigationTarget = null;
                return;
            }
            markRaptorTransportRoute();
            if (distanceToSqr(item) <= 64.0D && Math.abs(item.getY() - getY()) <= maxUpStep() + 1.0D) {
                cancelWorkAction();
                navigation.stop();
                navigationTarget = null;
                getMoveControl().setWantedPosition(
                        item.getX(), item.getY(), item.getZ(), movementSpeedForWork());
            } else {
                moveTo(item.blockPosition());
            }
            return;
        }
        ItemStack loose = item.getItem();
        int amount = Math.min(transportPickupLimit(loose.getMaxStackSize()), loose.getCount());
        ItemStack picked = loose.copyWithCount(amount);
        loose.shrink(amount);
        if (loose.isEmpty()) item.discard();
        else item.setItem(loose);
        entityData.set(CARRIED_STACK, picked);
        fieldCollectionTargetId = null;
        fieldCollectionApproachTicks = 0;
    }

    private @Nullable ItemEntity fieldCollectionTarget() {
        if (fieldCollectionTargetId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(fieldCollectionTargetId);
        if (entity instanceof ItemEntity item && item.isAlive() && !item.getItem().isEmpty()
                && fieldCollectionArea().contains(item.position())
                && fieldCollectionRetryAfter.getOrDefault(item.getUUID(), 0L) <= level().getGameTime()
                && (fieldWorkItemFilter.isBlank()
                || matchesIdentifiers(List.of(fieldWorkItemFilter), item.getItem()))) return item;
        fieldCollectionTargetId = null;
        fieldCollectionApproachTicks = 0;
        return null;
    }

    private int fieldSupportJobIndex() {
        int source = DinoFieldWorkRules.sourceJobIndex(getSpecies());
        return source < 0 ? 0 : source;
    }

    private int fieldWorkDuration(int durationTicks) {
        int job = fieldSupportJobIndex();
        int duration = Math.max(1, Math.round(durationTicks / getMutationStatMultiplier()));
        CommandTableBlockEntity table = commandTableEntity();
        if (table != null) duration = Math.max(1, Math.round(duration * table.workDurationMultiplier(job)));
        float performance = getSpecies().passiveWorkSpeedMultiplier(job, getPassiveStrength())
                * DinosaurGeneticPerformanceRules.workSpeedMultiplier(getGeneticQuality());
        return Math.max(1, Math.round(duration / performance
                * DinosaurProgression.workDurationMultiplier(dinosaurLevel)));
    }

    private AABB fieldCollectionArea() {
        if (fieldWorkMode == DinoWhistleSettings.FieldMode.COLLECT) {
            double radius = fieldWorkRange;
            Vec3 center = fieldWorkFirst == null ? position() : fieldWorkFirst.getCenter();
            double vertical = Math.min(32.0D, Math.max(8.0D, radius * 0.50D));
            return new AABB(center, center).inflate(radius, vertical, radius);
        }
        if (fieldWorkPattern != DinoWhistleSettings.Pattern.AREA || fieldWorkSecond == null) {
            return new AABB(fieldWorkFirst).inflate(fieldWorkPattern == DinoWhistleSettings.Pattern.CONNECTED ? 6.0D : 1.5D);
        }
        BlockPos min = new BlockPos(Math.min(fieldWorkFirst.getX(), fieldWorkSecond.getX()),
                Math.min(fieldWorkFirst.getY(), fieldWorkSecond.getY()),
                Math.min(fieldWorkFirst.getZ(), fieldWorkSecond.getZ()));
        BlockPos max = new BlockPos(Math.max(fieldWorkFirst.getX(), fieldWorkSecond.getX()) + 1,
                Math.max(fieldWorkFirst.getY(), fieldWorkSecond.getY()) + 1,
                Math.max(fieldWorkFirst.getZ(), fieldWorkSecond.getZ()) + 1);
        return new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max)).inflate(1.0D);
    }

    private boolean advanceFieldWorkAction(BlockPos targetPos, int durationTicks) {
        navigation.stop();
        setSpeed(0.0F);
        xxa = 0.0F;
        zza = 0.0F;
        getLookControl().setLookAt(targetPos.getCenter());
        int duration = Math.max(1, durationTicks);
        boolean starting = entityData.get(WORK_ACTION) != 6 || workActionPos == null
                || !workActionPos.equals(targetPos) || entityData.get(WORK_ACTION_DURATION) != duration;
        if (starting) {
            workActionPos = targetPos.immutable();
            workLockedPosition = position();
            entityData.set(WORK_ACTION_POS, Optional.of(workActionPos));
            entityData.set(WORK_ACTION, 6);
            entityData.set(WORK_ACTION_PROGRESS, 0);
            entityData.set(WORK_ACTION_DURATION, duration);
        }
        if (workLockedPosition == null) workLockedPosition = position();
        setPos(workLockedPosition.x, workLockedPosition.y, workLockedPosition.z);
        setDeltaMovement(Vec3.ZERO);
        int progress = entityData.get(WORK_ACTION_PROGRESS) + 1;
        entityData.set(WORK_ACTION_PROGRESS, progress);
        if (progress <= duration) return false;
        cancelWorkAction();
        return true;
    }

    private void runTransportWork() {
        if (getCarriedStack().isEmpty()) {
            if (!collectPendingBaseCargo() && !collectLooseCraftingOutput()) {
                collectFromSource();
            }
        } else {
            deliverCarriedStack();
        }
    }

    private void runFireWork() {
        BlockPos stationPos = chooseWorkstationPosition();
        if (stationPos == null || !level().isLoaded(stationPos)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        if (level().getBlockState(stationPos).is(ModBlocks.PROCESSOR.get())) {
            runProcessorWork(stationPos);
            return;
        }
        if (!(level().getBlockState(stationPos).getBlock() instanceof AbstractFurnaceBlock)
                || !(level().getBlockEntity(stationPos) instanceof AbstractFurnaceBlockEntity station)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        ItemStack input = station.getItem(0);
        boolean ancient = station instanceof AncientFurnaceBlockEntity;
        ItemStack fuel = ancient ? ItemStack.EMPTY : station.getItem(1);
        if (ancient) {
            CommandTableBlockEntity table = commandTableEntity();
            if (table == null || !table.isEnergyConsumerPowered(stationPos)) {
                cancelWorkAction();
                workerCooldown = 30;
                return;
            }
        }
        if (!input.isEmpty() && !matchesIdentifiers(workItemFilters, input)
                || !fuel.isEmpty() && !matchesIdentifiers(workFuelFilters, fuel)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        if (!closeTo(stationPos)) {
            moveTo(stationPos);
            return;
        }
        if (level() instanceof ServerLevel serverLevel
                && advanceWorkAction(stationPos, WorkSpecialtyRules.FIRE_TENDING_TICKS)) {
            if (station instanceof AncientFurnaceBlockEntity ancientFurnace) {
                ancientFurnace.addWorkerProgress(20);
            } else {
                for (int tick = 0; tick < 20; tick++) {
                    AbstractFurnaceBlockEntity.serverTick(serverLevel, stationPos,
                            level().getBlockState(stationPos), station);
                }
            }
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    stationPos.getX() + 0.5D, stationPos.getY() + 1.08D, stationPos.getZ() + 0.5D,
                    5, 0.22D, 0.10D, 0.22D, 0.01D);
            requestCappedHungerDrain();
            advanceWorkstationCursor();
            workerCooldown = 4;
        }
    }

    private void runProcessorWork(BlockPos stationPos) {
        if (!(level().getBlockEntity(stationPos) instanceof ProcessorBlockEntity processor)
                || !processor.canBeTended()) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        ItemStack input = processor.getItem(ProcessorBlockEntity.INPUT_SLOT);
        ItemStack fuel = processor.getItem(ProcessorBlockEntity.FUEL_SLOT);
        if (!input.isEmpty() && !matchesIdentifiers(workItemFilters, input)
                || !fuel.isEmpty() && !matchesIdentifiers(workFuelFilters, fuel)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        if (!closeTo(stationPos)) {
            moveTo(stationPos);
            return;
        }
        if (advanceWorkAction(stationPos, WorkSpecialtyRules.ORE_PROCESSING_TICKS)
                && processor.addWorkerProgress(80)) {
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        stationPos.getX() + 0.5D, stationPos.getY() + 1.05D, stationPos.getZ() + 0.5D,
                        10, 0.22D, 0.12D, 0.22D, 0.025D);
            }
            requestCappedHungerDrain();
            advanceWorkstationCursor();
            workerCooldown = 4;
        }
    }

    private void runEnergyWork() {
        BlockPos turbinePos = chooseWorkstationPosition();
        if (turbinePos == null || !level().isLoaded(turbinePos)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        BlockState turbineState = level().getBlockState(turbinePos);
        if (turbineState.is(ModBlocks.TURBINE_PART.get())) {
            BlockPos master = TurbinePartBlock.masterPos(turbinePos, turbineState);
            if (TurbinePartBlock.isExpectedMaster(level(), master, turbineState)) turbinePos = master;
        }
        if (!TurbineBlock.isTurbine(level().getBlockState(turbinePos))) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        if (!(level().getBlockEntity(turbinePos) instanceof TurbineBlockEntity turbine)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        if (!closeToEnergyStation(turbinePos)) {
            moveTo(turbinePos);
            return;
        }
        CommandTableBlockEntity table = commandTableEntity();
        boolean activelyGenerating = turbine.markWorkerActive();
        List<TurbineBlockEntity.CoupledTurbine> coupled = activelyGenerating
                ? turbine.coupledWaterTurbines(table) : List.of();
        if (table != null && activelyGenerating) {
            float generatedPerTick = WorkSpecialtyRules.energyPerSecond(
                            getSpecialtyStars(2), dinosaurLevel)
                    * getMutationStatMultiplier()
                    * getSpecies().passiveWorkSpeedMultiplier(2, getPassiveStrength())
                    * turbine.generationMultiplier() / 20.0F;
            table.receiveGeneratedEnergy(generatedPerTick);
            coupled.forEach(link -> table.receiveGeneratedEnergy(
                    generatedPerTick * link.outputMultiplier()));
        }
        if (advanceContinuousWorkAction(turbinePos, WorkSpecialtyRules.ENERGY_GENERATION_TICKS)) {
            boolean generated = turbine.recordGenerationPulse();
            if (generated) coupled.forEach(link -> link.turbine().recordGenerationPulse());
            if (level() instanceof ServerLevel serverLevel && generated) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        turbinePos.getX() + 0.5D, turbinePos.getY() + 1.15D, turbinePos.getZ() + 0.5D,
                        7, 0.3D, 0.18D, 0.3D, 0.015D);
            }
            if (generated) requestCappedHungerDrain();
            if (generated) advanceWorkstationCursor();
            workerCooldown = generated ? 0 : 20;
        }
    }

    private boolean advanceWorkAction(BlockPos targetPos, int baseDurationTicks) {
        return advanceWorkActionWithDuration(targetPos, workActionDuration(baseDurationTicks));
    }

    private boolean advanceContinuousWorkAction(BlockPos targetPos, int baseDurationTicks) {
        return advanceWorkActionWithDuration(targetPos, workActionDuration(baseDurationTicks), true);
    }

    private int workActionDuration(int baseDurationTicks) {
        int duration = WorkSpecialtyRules.actionDurationTicks(baseDurationTicks, getSpecialtyStars(workJobIndex));
        return applyWorkMutation(duration);
    }

    private boolean advanceTransportAction(BlockPos targetPos, int baseDurationTicks, int count, int maximumStackSize) {
        int duration = WorkSpecialtyRules.transportHandlingDurationTicks(
                baseDurationTicks,
                getSpecialtyStars(0),
                count,
                maximumStackSize
        );
        return advanceWorkActionWithDuration(targetPos, applyWorkMutation(duration));
    }

    private int applyWorkMutation(int durationTicks) {
        int mutatedDuration = Math.max(1, Math.round(durationTicks / getMutationStatMultiplier()));
        CommandTableBlockEntity table = commandTableEntity();
        int upgradedDuration = table == null
                ? mutatedDuration
                : Math.max(1, Math.round(mutatedDuration * table.workDurationMultiplier(workJobIndex)));
        float passiveMultiplier = getSpecies().passiveWorkSpeedMultiplier(workJobIndex, getPassiveStrength())
                * DinosaurGeneticPerformanceRules.workSpeedMultiplier(getGeneticQuality());
        return Math.max(1, Math.round(upgradedDuration / passiveMultiplier
                * DinosaurProgression.workDurationMultiplier(dinosaurLevel)));
    }

    private boolean advanceWorkActionWithDuration(BlockPos targetPos, int durationTicks) {
        return advanceWorkActionWithDuration(targetPos, durationTicks, false);
    }

    private boolean advanceWorkActionWithDuration(BlockPos targetPos, int durationTicks, boolean continuous) {
        navigation.stop();
        setSpeed(0.0F);
        xxa = 0.0F;
        zza = 0.0F;
        if (getSpecies() == DinosaurSpecies.PTERANODON && autonomousTransportFlight && !onGround()) {
            setNoGravity(true);
            entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_HOVERING);
            entityData.set(PTERO_AIRSPEED, 0.0F);
        }
        getLookControl().setLookAt(
                targetPos.getX() + 0.5D,
                targetPos.getY() + 0.55D,
                targetPos.getZ() + 0.5D,
                35.0F,
                35.0F
        );
        int action = workJobIndex + 1;
        int duration = Math.max(1, durationTicks);
        boolean startingAction = entityData.get(WORK_ACTION) != action
                || workActionPos == null
                || !workActionPos.equals(targetPos)
                || entityData.get(WORK_ACTION_DURATION) != duration;
        if (startingAction) {
            workActionPos = targetPos.immutable();
            workLockedPosition = position();
            entityData.set(WORK_ACTION_POS, Optional.of(workActionPos));
            entityData.set(WORK_ACTION, action);
            entityData.set(WORK_ACTION_PROGRESS, 0);
            entityData.set(WORK_ACTION_DURATION, duration);
        }
        if (workLockedPosition == null) {
            workLockedPosition = position();
        }
        setPos(workLockedPosition.x, workLockedPosition.y, workLockedPosition.z);
        setDeltaMovement(Vec3.ZERO);
        int progress = entityData.get(WORK_ACTION_PROGRESS) + 1;
        entityData.set(WORK_ACTION_PROGRESS, progress);
        if (progress <= duration) {
            return false;
        }
        if (continuous) {
            entityData.set(WORK_ACTION_PROGRESS, 0);
        } else {
            cancelWorkAction();
        }
        awardWorkExperience(DinosaurProgression.workExperience(workJobIndex));
        return true;
    }

    private void cancelWorkAction() {
        workActionPos = null;
        workLockedPosition = null;
        pendingCraftingOrder = null;
        if (entityData.get(WORK_ACTION) != 0
                || entityData.get(WORK_ACTION_PROGRESS) != 0
                || entityData.get(WORK_ACTION_DURATION) != 0) {
            entityData.set(WORK_ACTION, 0);
            entityData.set(WORK_ACTION_PROGRESS, 0);
            entityData.set(WORK_ACTION_DURATION, 0);
            entityData.set(WORK_ACTION_POS, Optional.empty());
        }
    }

    private boolean scheduleAllowsWork() {
        boolean night = isNightWorkWindow();
        return workSchedule == 0 || workSchedule == 1 && !night || workSchedule == 2 && night;
    }

    private boolean isNightWorkWindow() {
        long clockTime = Math.floorMod(level().getDefaultClockTime(), 24_000L);
        return clockTime >= 13_000L && clockTime < 23_000L;
    }

    private void collectFromSource() {
        ContainerTarget sourceTarget = chooseSource();
        if (sourceTarget == null) {
            cancelWorkAction();
            workerCooldown = 20;
            return;
        }
        if (!closeTo(sourceTarget.pos)) {
            markRaptorTransportRoute();
            moveTo(sourceTarget.pos);
            return;
        }
        if (workerCooldown > 0) {
            return;
        }
        Container source = sourceTarget.container;
        int available = countExtractableMatching(source);
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty() || !matchesFilter(stack) || !canExtract(source, slot, stack)) {
                continue;
            }
            ContainerTarget destinationTarget = chooseDestination(stack);
            if (destinationTarget == null) {
                cancelWorkAction();
                workerCooldown = 20;
                return;
            }
            int amountAllowed = Math.min(transportPickupLimit(stack.getMaxStackSize()),
                    Math.max(0, available - workSourceReserve));
            amountAllowed = Math.min(amountAllowed, insertCapacity(destinationTarget.container, stack));
            if (workDestinationTarget > 0) {
                amountAllowed = Math.min(amountAllowed,
                        Math.max(0, workDestinationTarget - countMatching(destinationTarget.container)));
            }
            amountAllowed = Math.min(amountAllowed, stack.getCount());
            if (amountAllowed <= 0) {
                cancelWorkAction();
                workerCooldown = 20;
                return;
            }
            if (!advanceTransportAction(sourceTarget.pos, WorkSpecialtyRules.CHEST_EXTRACT_TICKS,
                    amountAllowed, stack.getMaxStackSize())) {
                return;
            }
            ItemStack current = source.getItem(slot);
            if (current.isEmpty() || !matchesFilter(current) || !canExtract(source, slot, current)) {
                cancelWorkAction();
                workerCooldown = 20;
                return;
            }
            ContainerTarget currentDestination = chooseDestination(current);
            int currentAvailable = countExtractableMatching(source);
            int committedAmount = Math.min(transportPickupLimit(current.getMaxStackSize()),
                    Math.max(0, currentAvailable - workSourceReserve));
            committedAmount = currentDestination == null
                    ? 0
                    : Math.min(committedAmount, insertCapacity(currentDestination.container, current));
            if (workDestinationTarget > 0 && currentDestination != null) {
                committedAmount = Math.min(committedAmount,
                        Math.max(0, workDestinationTarget - countMatching(currentDestination.container)));
            }
            committedAmount = Math.min(committedAmount, current.getCount());
            if (committedAmount <= 0) {
                cancelWorkAction();
                workerCooldown = 20;
                return;
            }
            ItemStack taken = source.removeItem(slot, committedAmount);
            source.setChanged();
            entityData.set(CARRIED_STACK, taken);
            workerCooldown = 4;
            return;
        }
        cancelWorkAction();
        workerCooldown = 20;
    }

    private void deliverCarriedStack() {
        ContainerTarget target = chooseDestination(getCarriedStack());
        if (target == null) {
            cancelWorkAction();
            workerCooldown = 20;
            return;
        }
        Container destination = target.container;
        if (!closeTo(target.pos)) {
            markRaptorTransportRoute();
            moveTo(target.pos);
            return;
        }
        if (workerCooldown > 0) {
            return;
        }
        ItemStack carried = getCarriedStack().copy();
        if (workDestinationTarget > 0) {
            int roomBeforeTarget = Math.max(0, workDestinationTarget - countMatching(destination));
            if (roomBeforeTarget == 0) {
                cancelWorkAction();
                workerCooldown = 30;
                return;
            }
            carried.setCount(Math.min(carried.getCount(), roomBeforeTarget));
        }
        if (!advanceTransportAction(target.pos, WorkSpecialtyRules.CHEST_INSERT_TICKS,
                carried.getCount(), carried.getMaxStackSize())) {
            return;
        }
        int inserted = insert(destination, carried);
        if (inserted <= 0) {
            cancelWorkAction();
            workerCooldown = 20;
            return;
        }
        ItemStack remainder = getCarriedStack().copy();
        remainder.shrink(inserted);
        entityData.set(CARRIED_STACK, remainder);
        requestCappedHungerDrain();
        workerCooldown = 4;
        if (remainder.isEmpty() && workRepeatMode == 2) {
            stopWorkOrder();
        } else if (remainder.isEmpty() && workRepeatMode == 1 && workDestinationTarget > 0
                && countMatching(destination) >= workDestinationTarget) {
            stopWorkOrder();
        }
    }

    private ContainerTarget chooseSource() {
        for (BlockPos pos : orderedPositions(workSourcePositions)) {
            Container container = containerAt(pos);
            if (container != null && countExtractableMatching(container) > workSourceReserve) {
                return new ContainerTarget(pos, container);
            }
            if (workRoutePolicy == 0) {
                break;
            }
        }
        return null;
    }

    private ContainerTarget chooseDestination(ItemStack incoming) {
        List<BlockPos> primary = workJobIndex == 1 || workJobIndex == 3
                ? workWorkstationPositions
                : workDestinationPositions;
        ContainerTarget target = chooseDestinationFrom(primary, incoming);
        if (target == null && workRoutePolicy != 0) {
            target = chooseDestinationFrom(workFallbackPositions, incoming);
        }
        return target;
    }

    private ContainerTarget chooseDestinationFrom(List<BlockPos> positions, ItemStack incoming) {
        for (BlockPos pos : orderedPositions(positions)) {
            Container container = containerAt(pos);
            boolean hasCapacity = incoming.isEmpty() || hasRoom(container, incoming);
            boolean belowTarget = workDestinationTarget == 0 || container != null && countMatching(container) < workDestinationTarget;
            if (container != null && hasCapacity && belowTarget) {
                return new ContainerTarget(pos, container);
            }
            if (workRoutePolicy == 0) {
                break;
            }
        }
        return null;
    }

    private BlockPos choosePosition(List<BlockPos> positions) {
        List<BlockPos> ordered = orderedPositions(positions);
        if (!ordered.isEmpty()) {
            return ordered.getFirst();
        }
        return workRoutePolicy == 0 ? null : orderedPositions(workFallbackPositions).stream().findFirst().orElse(null);
    }

    private BlockPos chooseWorkstationPosition() {
        List<BlockPos> ordered = new ArrayList<>(workWorkstationPositions);
        ordered.sort(Comparator
                .comparingInt((BlockPos pos) -> blockPriority(pos)).reversed()
                .thenComparingLong(BlockPos::asLong));
        if (ordered.isEmpty()) return null;
        return ordered.get(Math.floorMod(workWorkstationCursor, ordered.size()));
    }

    private void advanceWorkstationCursor() {
        if (workWorkstationPositions.size() > 1) {
            workWorkstationCursor = Math.floorMod(workWorkstationCursor + 1, workWorkstationPositions.size());
        }
    }

    private void runCraftingWork() {
        BlockPos stationPos = chooseWorkstationPosition();
        if (stationPos == null || workItemFilters.isEmpty() || !level().isLoaded(stationPos)
                || !(level().getBlockState(stationPos).getBlock() instanceof CraftingTableBlock)) {
            cancelWorkAction();
            workerCooldown = 30;
            return;
        }
        if (!closeTo(stationPos)) {
            moveTo(stationPos);
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CraftingOrder order = pendingCraftingOrder;
        if (order == null) {
            order = buildCraftingOrder(serverLevel, workItemFilters.getFirst(), workBatchSize);
            pendingCraftingOrder = order;
        }
        if (order == null) {
            cancelWorkAction();
            workerCooldown = 60;
            return;
        }
        if (!canCommitCraftingOrder(order)) {
            cancelWorkAction();
            workerCooldown = 40;
            return;
        }
        int baseDuration = Math.multiplyExact(WorkSpecialtyRules.CRAFTING_TICKS, Math.max(1, order.crafts));
        if (!advanceWorkAction(stationPos, baseDuration)) {
            return;
        }
        if (!canCommitCraftingOrder(order)) {
            cancelWorkAction();
            workerCooldown = 40;
            return;
        }
        for (SlotTake take : order.takes) {
            ItemStack removed = take.container.removeItem(take.slot, 1);
            if (!removed.isEmpty()) {
                var remainder = removed.getCraftingRemainder();
                if (remainder != null) {
                    spawnWorkOutput(serverLevel, stationPos, remainder.create());
                }
                take.container.setChanged();
            }
        }
        int remaining = order.output.getCount() * order.crafts;
        while (remaining > 0) {
            int count = Math.min(remaining, order.output.getMaxStackSize());
            spawnWorkOutput(serverLevel, stationPos, order.output.copyWithCount(count));
            remaining -= count;
        }
        requestCappedHungerDrain();
        advanceWorkstationCursor();
        workerCooldown = 4;
        if (workRepeatMode == 2) {
            stopWorkOrder();
        }
    }

    private CraftingOrder buildCraftingOrder(ServerLevel serverLevel, String outputIdentifier, int maximumCrafts) {
        Identifier wanted = Identifier.tryParse(outputIdentifier);
        if (wanted == null) return null;
        List<ContainerTarget> containers = baseContainers();
        var context = SlotDisplayContext.fromLevel(serverLevel);
        for (RecipeHolder<CraftingRecipe> holder : serverLevel.getServer().getRecipeManager().recipeMap().byType(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (recipe.placementInfo().isImpossibleToPlace()) continue;
            ItemStack output = recipe.display().stream()
                    .flatMap(display -> display.result().resolveForStacks(context).stream())
                    .filter(stack -> !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(wanted))
                    .findFirst().orElse(ItemStack.EMPTY);
            if (output.isEmpty()) continue;
            List<Ingredient> ingredients = recipe.placementInfo().ingredients();
            if (ingredients.isEmpty()) continue;
            List<SlotTake> takes = new ArrayList<>();
            int crafts = 0;
            for (; crafts < maximumCrafts; crafts++) {
                int mark = takes.size();
                boolean complete = true;
                for (Ingredient ingredient : ingredients) {
                    SlotTake take = findIngredient(containers, ingredient, takes);
                    if (take == null) {
                        complete = false;
                        break;
                    }
                    takes.add(take);
                }
                if (!complete) {
                    takes.subList(mark, takes.size()).clear();
                    break;
                }
            }
            if (crafts > 0) {
                return new CraftingOrder(output.copy(), crafts, List.copyOf(takes));
            }
        }
        return null;
    }

    private SlotTake findIngredient(List<ContainerTarget> containers, Ingredient ingredient, List<SlotTake> planned) {
        for (ContainerTarget target : containers) {
            Container container = target.container;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !ingredient.test(stack) || !canExtract(container, slot, stack)) continue;
                int alreadyPlanned = 0;
                for (SlotTake take : planned) {
                    if (take.container == container && take.slot == slot) alreadyPlanned++;
                }
                if (alreadyPlanned < stack.getCount()) {
                    return new SlotTake(container, slot, stack.copyWithCount(1));
                }
            }
        }
        return null;
    }

    private static boolean canCommitCraftingOrder(CraftingOrder order) {
        for (SlotTake take : order.takes) {
            ItemStack current = take.container.getItem(take.slot);
            if (!ItemStack.isSameItemSameComponents(current, take.expected)) return false;
            int required = 0;
            for (SlotTake planned : order.takes) {
                if (planned.container == take.container && planned.slot == take.slot) required++;
            }
            if (current.getCount() < required) return false;
        }
        return true;
    }

    private boolean collectLooseCraftingOutput() {
        for (BlockPos sourcePos : orderedPositions(workSourcePositions)) {
            if (!(level().getBlockState(sourcePos).getBlock() instanceof CraftingTableBlock)) continue;
            ItemEntity target = level().getEntitiesOfClass(ItemEntity.class,
                            new AABB(sourcePos).inflate(1.75D),
                            item -> item.isAlive() && matchesFilter(item.getItem()))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (target == null) continue;
            if (distanceToSqr(target) > 2.25D) {
                cancelWorkAction();
                markRaptorTransportRoute();
                navigation.moveTo(target, 1.05D);
                return true;
            }
            ItemStack loose = target.getItem();
            int amount = Math.min(transportPickupLimit(loose.getMaxStackSize()), loose.getCount());
            if (!advanceTransportAction(target.blockPosition(), WorkSpecialtyRules.LOOSE_ITEM_PICKUP_TICKS,
                    amount, loose.getMaxStackSize())) {
                return true;
            }
            entityData.set(CARRIED_STACK, loose.copyWithCount(amount));
            loose.shrink(amount);
            if (loose.isEmpty()) target.discard(); else target.setItem(loose);
            workerCooldown = 4;
            return true;
        }
        return false;
    }

    private boolean collectPendingBaseCargo() {
        if (commandTablePos == null) return false;
        ItemEntity target = level().getEntitiesOfClass(ItemEntity.class,
                        new AABB(commandTablePos).inflate(3.0D),
                        item -> item.isAlive() && item.entityTags().contains("primeval_base_cargo")
                                && chooseDestination(item.getItem()) != null)
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null) return false;
        if (distanceToSqr(target) > 2.25D) {
            cancelWorkAction();
            markRaptorTransportRoute();
            navigation.moveTo(target, movementSpeedForWork());
            return true;
        }
        ItemStack loose = target.getItem();
        int amount = Math.min(transportPickupLimit(loose.getMaxStackSize()), loose.getCount());
        if (!advanceTransportAction(target.blockPosition(), WorkSpecialtyRules.LOOSE_ITEM_PICKUP_TICKS,
                amount, loose.getMaxStackSize())) return true;
        entityData.set(CARRIED_STACK, loose.copyWithCount(amount));
        loose.shrink(amount);
        if (loose.isEmpty()) target.discard(); else target.setItem(loose);
        workerCooldown = 4;
        return true;
    }

    private void startExpedition() {
        if (!WorkSpecialtyRules.canAttemptExpedition(expeditionTier, getSpecialtyStars(4))) {
            stopWorkOrder();
            workerCooldown = 20;
            return;
        }
        clearBreedingCourtship();
        onExpedition = true;
        long duration = WorkSpecialtyRules.expeditionDurationTicks(
                expeditionTier, getSpecialtyStars(4), getMutationStatMultiplier());
        CommandTableBlockEntity table = commandTableEntity();
        if (table != null) duration = Math.max(1L, Math.round(duration * table.expeditionDurationMultiplier()));
        expeditionEndTick = level().getGameTime() + duration;
        cancelWorkAction();
        navigation.stop();
        setDeltaMovement(Vec3.ZERO);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
        setInvulnerable(true);
        DinosaurOwnership.syncRecord(this);
    }

    private void tickExpedition() {
        navigation.stop();
        setDeltaMovement(Vec3.ZERO);
        if (level().getGameTime() < expeditionEndTick) {
            noPhysics = true;
            setNoGravity(true);
            setInvisible(true);
            setInvulnerable(true);
            return;
        }
        onExpedition = false;
        noPhysics = false;
        setNoGravity(false);
        setInvisible(false);
        setInvulnerable(false);
        if (commandTablePos != null) {
            teleportTo(commandTablePos.getX() + 0.5D, commandTablePos.getY() + 1.0D, commandTablePos.getZ() + 0.5D);
        }
        deliverExpeditionReward();
        getDinosaurOwner()
                .map(level().getServer().getPlayerList()::getPlayer)
                .ifPresent(PrimevalAdvancements::awardFirstExpedition);
        awardWorkExperience(DinosaurProgression.expeditionExperience(expeditionTier));
        int risk = WorkSpecialtyRules.expeditionRiskPercent(
                expeditionTier, getSpecialtyStars(4), dinosaurLevel, getMutationStatMultiplier());
        if (random.nextInt(100) < risk) {
            incapacitatedUntilTick = level().getGameTime() + (60L + expeditionTier * 30L) * 20L;
        }
        feed(-Math.max(4, 5 + expeditionTier * 2));
        workEnabled = false;
        workerCooldown = 0;
        DinosaurOwnership.syncRecord(this);
    }

    private void stopWorkOrder() {
        workEnabled = false;
        DinosaurOwnership.syncRecord(this);
    }

    private void deliverExpeditionReward() {
        if (!(level() instanceof ServerLevel serverLevel) || commandTablePos == null) return;
        CommandTableBlockEntity table = commandTableEntity();
        float multiplier = table == null ? 1.0F : table.expeditionRewardMultiplier();
        if (getSpecies() == DinosaurSpecies.DODO) {
            multiplier += 0.08F * getPassiveStrength();
        }
        for (ItemStack reward : ExpeditionRewards.roll(
                expeditionTier, getSpecialtyStars(4), multiplier, random)) {
            spawnBaseCargo(serverLevel, commandTablePos, reward);
        }
    }

    private void spawnBaseCargo(ServerLevel serverLevel, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 1.15D,
                pos.getZ() + 0.5D, stack);
        item.addTag("primeval_base_cargo");
        item.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(item);
    }

    private void spawnWorkOutput(ServerLevel serverLevel, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 1.15D, pos.getZ() + 0.5D, stack);
        item.addTag("primeval_crafting_output");
        item.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(item);
    }

    private List<BlockPos> orderedPositions(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return List.of();
        }
        return PriorityRouting.order(
                positions,
                this::blockPriority,
                pos -> distanceToSqr(pos.getCenter()),
                workRoutePolicy == 2
        );
    }

    private int blockPriority(BlockPos pos) {
        return Mth.clamp(workBlockPriorities.getOrDefault(pos, 1), 0, 3);
    }

    private void gatherLooseItem() {
        if (workSourcePos == null || workAreaEndPos == null) {
            return;
        }
        AABB area = new AABB(Vec3.atLowerCornerOf(workSourcePos), Vec3.atLowerCornerOf(workAreaEndPos).add(1.0D, 1.0D, 1.0D)).inflate(1.0D);
        ItemEntity target = level().getEntitiesOfClass(ItemEntity.class, area, item -> item.isAlive() && matchesFilter(item.getItem()))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (target == null) {
            cancelWorkAction();
            workerCooldown = 20;
            return;
        }
        if (distanceToSqr(target) > 2.25D) {
            cancelWorkAction();
            navigation.moveTo(target, 1.05D);
            return;
        }
        ItemStack loose = target.getItem();
        int amount = Math.min(transportPickupLimit(loose.getMaxStackSize()), loose.getCount());
        if (!advanceTransportAction(target.blockPosition(), WorkSpecialtyRules.LOOSE_ITEM_PICKUP_TICKS,
                amount, loose.getMaxStackSize())) {
            return;
        }
        ItemStack pickedUp = loose.copyWithCount(amount);
        loose.shrink(amount);
        if (loose.isEmpty()) {
            target.discard();
        } else {
            target.setItem(loose);
        }
        entityData.set(CARRIED_STACK, pickedUp);
        workerCooldown = 4;
    }

    private int insert(Container destination, ItemStack incoming) {
        int remaining = incoming.getCount();
        for (int slot = 0; slot < destination.getContainerSize() && remaining > 0; slot++) {
            ItemStack existing = destination.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, incoming) && canInsert(destination, slot, incoming)) {
                int maximum = Math.min(destination.getMaxStackSize(existing), existing.getMaxStackSize());
                int moved = Math.min(remaining, Math.max(0, maximum - existing.getCount()));
                if (moved > 0) {
                    existing.grow(moved);
                    remaining -= moved;
                }
            }
        }
        for (int slot = 0; slot < destination.getContainerSize() && remaining > 0; slot++) {
            if (!destination.getItem(slot).isEmpty() || !canInsert(destination, slot, incoming)) {
                continue;
            }
            int moved = Math.min(remaining, Math.min(destination.getMaxStackSize(incoming), incoming.getMaxStackSize()));
            destination.setItem(slot, incoming.copyWithCount(moved));
            remaining -= moved;
        }
        int inserted = incoming.getCount() - remaining;
        if (inserted > 0) {
            destination.setChanged();
        }
        return inserted;
    }

    private boolean hasRoom(Container container, ItemStack incoming) {
        if (container == null || incoming.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!canInsert(container, slot, incoming)) {
                continue;
            }
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                int maximum = Math.min(container.getMaxStackSize(existing), existing.getMaxStackSize());
                if (existing.getCount() < maximum) {
                    return true;
                }
            }
        }
        return false;
    }

    private int insertCapacity(Container container, ItemStack incoming) {
        if (container == null || incoming.isEmpty()) {
            return 0;
        }
        int capacity = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!canInsert(container, slot, incoming)) {
                continue;
            }
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                capacity += Math.min(container.getMaxStackSize(incoming), incoming.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                int maximum = Math.min(container.getMaxStackSize(existing), existing.getMaxStackSize());
                capacity += Math.max(0, maximum - existing.getCount());
            }
            if (capacity >= incoming.getCount()) {
                return incoming.getCount();
            }
        }
        return capacity;
    }

    private int countExtractableMatching(Container container) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && matchesFilter(stack) && canExtract(container, slot, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean canInsert(Container container, int slot, ItemStack stack) {
        if (!container.canPlaceItem(slot, stack)) {
            return false;
        }
        if (!(container instanceof WorldlyContainer sided)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (containsSlot(sided.getSlotsForFace(direction), slot)
                    && sided.canPlaceItemThroughFace(slot, stack, direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean canExtract(Container container, int slot, ItemStack stack) {
        if (!container.canTakeItem(container, slot, stack)) {
            return false;
        }
        if (!(container instanceof WorldlyContainer sided)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (containsSlot(sided.getSlotsForFace(direction), slot)
                    && sided.canTakeItemThroughFace(slot, stack, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSlot(int[] slots, int target) {
        for (int slot : slots) {
            if (slot == target) {
                return true;
            }
        }
        return false;
    }

    private int countMatching(Container container) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && matchesFilter(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean matchesFilter(ItemStack stack) {
        if (workItemFilters.isEmpty()) {
            return true;
        }
        String identifier = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (workItemFilters.contains(identifier)) {
            return true;
        }
        if (exactItemMatch) {
            return false;
        }
        for (String filter : workItemFilters) {
            Identifier filterId = Identifier.tryParse(filter);
            if (filterId == null) {
                continue;
            }
            var selected = BuiltInRegistries.ITEM.get(filterId).orElse(null);
            if (selected != null && selected.tags().anyMatch(stack::is)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIdentifiers(List<String> identifiers, ItemStack stack) {
        if (stack.isEmpty() || identifiers.isEmpty()) return true;
        String identifier = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return identifiers.contains(identifier);
    }

    private boolean baseContainsItem(String itemIdentifier) {
        Identifier wanted = Identifier.tryParse(itemIdentifier);
        if (wanted == null) return false;
        for (ContainerTarget target : baseContainers()) {
            for (int slot = 0; slot < target.container.getContainerSize(); slot++) {
                ItemStack stack = target.container.getItem(slot);
                if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(wanted)) return true;
            }
        }
        return false;
    }

    private List<ContainerTarget> baseContainers() {
        if (!(level() instanceof ServerLevel serverLevel) || commandTablePos == null) return List.of();
        return BaseInventoryIndex.scan(serverLevel, commandTablePos, baseRadius()).stream()
                .map(entry -> new ContainerTarget(entry.pos(), entry.container()))
                .toList();
    }

    private @Nullable CommandTableBlockEntity commandTableEntity() {
        return commandTablePos == null ? null : CommandTableBlock.tableEntity(level(), commandTablePos);
    }

    private @Nullable ServerPlayer commandOwner() {
        if (!(level() instanceof ServerLevel serverLevel) || dinosaurOwner == null) return null;
        ServerPlayer local = serverLevel.players().stream()
                .filter(player -> player.getUUID().equals(dinosaurOwner))
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        return local != null ? local : serverLevel.getServer().getPlayerList().getPlayer(dinosaurOwner);
    }

    private int baseRadius() {
        return commandTablePos == null ? 50 : CommandTableBlock.baseRadius(level(), commandTablePos);
    }

    private boolean insideBaseBoundary(Vec3 position, double padding) {
        if (commandMode == DinosaurCommandMode.FOLLOW) {
            if (fieldDutyOwnsNavigation() && fieldWorkFirst != null) {
                double radius = Math.max(12.0D, fieldWorkRange + padding);
                return fieldWorkFirst.getCenter().distanceToSqr(position) <= radius * radius;
            }
            ServerPlayer owner = commandOwner();
            if (owner == null || owner.level() != level()) return false;
            double radius = Math.max(12.0D, 32.0D + padding);
            return owner.position().distanceToSqr(position) <= radius * radius;
        }
        if (commandMode == DinosaurCommandMode.STAY && stayPosition != null) {
            double radius = Math.max(8.0D, 12.0D + padding);
            return stayPosition.getCenter().distanceToSqr(position) <= radius * radius;
        }
        if (commandTablePos == null) return true;
        Vec3 center = commandTablePos.getCenter();
        double x = position.x - center.x;
        double z = position.z - center.z;
        double radius = Math.max(8.0D, baseRadius() + padding);
        return x * x + z * z <= radius * radius;
    }

    private int threatAwarenessBonus() {
        CommandTableBlockEntity table = commandTableEntity();
        return table == null ? 0 : table.threatAwarenessBonus();
    }

    private boolean seekFoodWhenHungry() {
        if (getHunger() >= PrimevalTuning.server().foodBoxThreshold() || commandTablePos == null) {
            foodTargetPos = null;
            return false;
        }
        if (foodSearchCooldown > 0) foodSearchCooldown--;
        if (foodTargetPos == null || !hasEdibleFood(foodTargetPos)) {
            if (foodSearchCooldown > 0) return false;
            foodSearchCooldown = 40;
            foodTargetPos = baseContainers().stream()
                    .filter(target -> level().getBlockEntity(target.pos) instanceof FoodBoxBlockEntity)
                    .filter(target -> firstFoodSlot(target.container) >= 0)
                    .min(Comparator.comparingDouble(target -> distanceToSqr(target.pos.getCenter())))
                    .map(ContainerTarget::pos).orElse(null);
        }
        if (foodTargetPos == null) return false;
        if (!closeTo(foodTargetPos)) {
            showIndicator(4, 24);
            moveTo(foodTargetPos);
            return true;
        }
        Container foodBox = containerAt(foodTargetPos);
        int slot = firstFoodSlot(foodBox);
        if (slot < 0) {
            foodTargetPos = null;
            return false;
        }
        boolean ate = false;
        while (getHunger() < 100 && slot >= 0) {
            ItemStack food = foodBox.removeItem(slot, 1);
            if (food.isEmpty()) break;
            int restored = Math.max(4, Math.round(FOOD_BOX_BASE_FOOD_VALUE / getSpecies().appetite()));
            feed(restored);
            ate = true;
            slot = firstFoodSlot(foodBox);
        }
        if (ate) {
            foodBox.setChanged();
            showIndicator(5, 40);
            workerCooldown = workJobIndex == 2 ? 0 : 20;
        }
        foodTargetPos = null;
        return true;
    }

    private boolean hasEdibleFood(BlockPos pos) {
        return level().getBlockEntity(pos) instanceof FoodBoxBlockEntity && firstFoodSlot(containerAt(pos)) >= 0;
    }

    private int firstFoodSlot(Container container) {
        if (container == null) return -1;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (canEat(container.getItem(slot))) return slot;
        }
        return -1;
    }

    private Container containerAt(BlockPos pos) {
        if (pos == null || !level().isLoaded(pos)) {
            return null;
        }
        if (level() instanceof ServerLevel serverLevel) {
            return BaseInventoryIndex.containerAt(serverLevel, pos);
        }
        var state = level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            Container combined = ChestBlock.getContainer(chest, state, level(), pos, true);
            if (combined != null) {
                return combined;
            }
        }
        BlockEntity blockEntity = level().getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    private boolean closeTo(BlockPos pos) {
        double reach = Math.max(0.35D,
                workInteractionDistance() - getBbWidth() * 0.5D - 0.5D);
        double verticalReach = Math.max(2.25D, getBbHeight() * 0.65D);
        return getBoundingBox().inflate(reach, verticalReach, reach).intersects(new AABB(pos));
    }

    private boolean closeToEnergyStation(BlockPos pos) {
        if (getSpecies() != DinosaurSpecies.SPINOSAURUS || !isWaterTurbineTarget(pos)) {
            return closeTo(pos);
        }
        Vec3 turbineCenter = pos.getCenter().add(0.0D, 0.85D, 0.0D);
        Vec3 workCenter = position().add(0.0D, getBbHeight() * 0.38D, 0.0D);
        double horizontalReach = Math.max(2.1D, workInteractionDistance() + getBbWidth() * 0.35D);
        return isInWater()
                && workCenter.multiply(1.0D, 0.0D, 1.0D)
                .distanceToSqr(turbineCenter.multiply(1.0D, 0.0D, 1.0D)) <= horizontalReach * horizontalReach
                && Math.abs(workCenter.y - turbineCenter.y) <= 1.15D;
    }

    private boolean closeToFieldTarget(BlockPos pos) {
        double horizontalReach = getSpecies().fieldWorkReach() * getScale();
        double verticalReach = Math.max(3.25D, horizontalReach * 0.82D);
        return getBoundingBox().inflate(horizontalReach, verticalReach, horizontalReach)
                .intersects(new AABB(pos));
    }

    private double workInteractionDistance() {
        return Math.max(2.35D, getSpecies().workReach() * getScale());
    }

    private Vec3 workApproachPoint(BlockPos pos) {
        Vec3 center = pos.getCenter();
        Vec3 away = position().subtract(center);
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.horizontalDistanceSqr() < 1.0E-5D) {
            away = Vec3.directionFromRotation(0.0F, getYRot()).scale(-1.0D);
        } else {
            away = away.normalize();
        }
        double collisionClearance = getBbWidth() * 0.5D + 0.60D;
        Vec3 approach = center.add(away.scale(Math.max(0.82D, collisionClearance)));
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS && isWaterTurbineTarget(pos)) {
            double bodyWorkOffset = getBbHeight() * 0.38D;
            approach = new Vec3(approach.x, pos.getY() + 1.35D - bodyWorkOffset, approach.z);
        }
        if (fieldDutyOwnsNavigation()) {
            double verticalReach = Math.max(3.25D, getSpecies().fieldWorkReach() * getScale() * 0.82D);
            if (Math.abs(center.y - getY()) <= verticalReach) {
                approach = new Vec3(approach.x, getY(), approach.z);
            }
        }
        return approach;
    }

    private void moveTo(BlockPos pos) {
        if (pos == null) return;
        cancelWorkAction();
        double horizontalDistanceSquared = Vec3.atCenterOf(pos).subtract(position()).horizontalDistanceSqr();
        if (getSpecies() == DinosaurSpecies.PTERANODON && isAutonomousPteranodonFlightAllowed() && !isVehicle()) {
            double flightThreshold = 196.0D / Math.max(0.82F, 0.84F + getPassiveStrength() * 0.16F);
            double verticalDistance = Math.abs(pos.getY() + 0.5D - getY());
            if (autonomousTransportFlight || horizontalDistanceSquared >= flightThreshold
                    || verticalDistance >= 2.5D || stalledNavigationTicks >= 40) {
                tickAutonomousTransportFlight(pos);
                return;
            }
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS && isWaterTurbineTarget(pos)) {
            if (isInWater()) {
                tickSpinosaurusAquaticWorkMovement(pos);
                return;
            }
            if (tickSpinosaurusWaterEntryMovement(pos)) return;
        }
        if (!pos.equals(navigationTarget)) {
            navigation.stop();
            navigationTarget = pos.immutable();
            lastNavigationDistance = position().distanceTo(pos.getCenter());
            lastNavigationSamplePosition = position();
            stalledNavigationTicks = 0;
            recoveryWaypointTicks = 0;
            Vec3 approach = workApproachPoint(pos);
            boolean pathStarted = navigation.moveTo(
                    approach.x, approach.y, approach.z, movementSpeedForWork());
            if (!pathStarted && getSpecies() == DinosaurSpecies.PTERANODON
                    && isAutonomousPteranodonFlightAllowed() && !isVehicle()) {
                tickAutonomousTransportFlight(pos);
                return;
            }
        }
        if (recoveryWaypointTicks > 0) {
            recoveryWaypointTicks--;
            return;
        }
        double distanceSquared = distanceToSqr(pos.getCenter());
        double distance = Math.sqrt(distanceSquared);
        if (tickCount % 20 == 0) {
            double displacementSquared = lastNavigationSamplePosition == null
                    ? 0.0D : position().distanceToSqr(lastNavigationSamplePosition);
            if (!DinosaurFollowRules.madeMeaningfulProgress(
                    lastNavigationDistance, distance, displacementSquared, navigation.isStuck())) {
                stalledNavigationTicks += 20;
            } else {
                stalledNavigationTicks = Math.max(0, stalledNavigationTicks - 20);
            }
            lastNavigationDistance = distance;
            lastNavigationSamplePosition = position();
        }
        double speed = movementSpeedForWork();
        if (DinosaurFollowRules.shouldTeleportAfterStall(stalledNavigationTicks, distanceSquared)
                && trySafeNavigationTeleport(pos)) {
            stalledNavigationTicks = 0;
            recoveryWaypointTicks = 0;
            return;
        }
        if (DinosaurFollowRules.shouldTryLocalRecovery(stalledNavigationTicks)) {
            navigation.stop();
            Vec3 approach = workApproachPoint(pos);
            if (!navigation.moveTo(approach.x, approach.y, approach.z, speed)) {
                navigation.recomputePath();
            }
            recoveryWaypointTicks = 16;
            stalledNavigationTicks = Math.max(50, stalledNavigationTicks - 5);
            return;
        }
        if (navigation.isDone() || navigation.isStuck()) {
            Vec3 approach = workApproachPoint(pos);
            boolean pathStarted = navigation.moveTo(approach.x, approach.y, approach.z, speed);
            if (!pathStarted) stalledNavigationTicks = Math.min(220, stalledNavigationTicks + 10);
            if (!pathStarted && getSpecies() == DinosaurSpecies.PTERANODON
                    && isAutonomousPteranodonFlightAllowed() && !isVehicle()) {
                tickAutonomousTransportFlight(pos);
            }
        }
    }

    private boolean isWaterTurbineTarget(BlockPos pos) {
        return level().isLoaded(pos) && level().getBlockState(pos).is(ModBlocks.WATER_TURBINE.get());
    }

    private boolean tickSpinosaurusWaterEntryMovement(BlockPos target) {
        spinosaurusAquaticWorkTarget = null;
        BlockPos entry = findSpinosaurusWaterEntry(target);
        if (entry == null) return false;
        if (!target.equals(navigationTarget)) {
            navigation.stop();
            navigationTarget = target.immutable();
            lastNavigationDistance = position().distanceTo(entry.getCenter());
            lastNavigationSamplePosition = position();
            stalledNavigationTicks = 0;
            recoveryWaypointTicks = 0;
        }

        Vec3 entryCenter = entry.getCenter();
        double distance = position().distanceTo(entryCenter);
        double targetDistanceSquared = distanceToSqr(target.getCenter());
        if (tickCount % 20 == 0) {
            double displacementSquared = lastNavigationSamplePosition == null
                    ? 0.0D : position().distanceToSqr(lastNavigationSamplePosition);
            if (!DinosaurFollowRules.madeMeaningfulProgress(
                    lastNavigationDistance, distance, displacementSquared, navigation.isStuck())) {
                stalledNavigationTicks = Math.min(220, stalledNavigationTicks + 20);
            } else {
                stalledNavigationTicks = Math.max(0, stalledNavigationTicks - 20);
            }
            lastNavigationDistance = distance;
            lastNavigationSamplePosition = position();
        }

        Vec3 horizontal = entryCenter.subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        boolean waterBelow = entry.getY() + 0.5D < getY();
        if (waterBelow && horizontal.horizontalDistanceSqr() > 0.01D) {
            Vec3 advance = horizontal.normalize().scale(Math.min(horizontal.horizontalDistance(), 8.0D));
            AABB lane = getBoundingBox().expandTowards(advance.x, 0.0D, advance.z)
                    .inflate(0.04D, 0.0D, 0.04D);
            if (level().noCollision(this, lane)) {
                navigation.stop();
                getMoveControl().setWantedPosition(entryCenter.x, getY(), entryCenter.z,
                        Math.max(1.08D, movementSpeedForWork()));
                float desiredYaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * Mth.RAD_TO_DEG) - 90.0F;
                setYRot(Mth.approachDegrees(getYRot(), desiredYaw,
                        getSpecies().turnDegreesPerTick() * 1.35F));
                if (horizontal.horizontalDistanceSqr() <= 12.25D) {
                    Vec3 edgeVelocity = horizontal.normalize().scale(0.18D * getMutationStatMultiplier());
                    Vec3 current = getDeltaMovement();
                    setDeltaMovement(Mth.lerp(0.22D, current.x, edgeVelocity.x), current.y,
                            Mth.lerp(0.22D, current.z, edgeVelocity.z));
                }
                if (stalledNavigationTicks >= 100 && targetDistanceSquared > 16.0D
                        && trySafeNavigationTeleport(target)) {
                    stalledNavigationTicks = 0;
                }
                return true;
            }
        }

        if (stalledNavigationTicks >= 100 && targetDistanceSquared > 16.0D
                && trySafeNavigationTeleport(target)) {
            stalledNavigationTicks = 0;
            return true;
        }
        if (navigation.isDone() || navigation.isStuck() || tickCount % 12 == 0) {
            boolean started = navigation.moveTo(entryCenter.x, entry.getY(), entryCenter.z,
                    movementSpeedForWork());
            if (!started) stalledNavigationTicks = Math.min(220, stalledNavigationTicks + 10);
        }
        return true;
    }

    private @Nullable BlockPos findSpinosaurusWaterEntry(BlockPos target) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int topY = Math.min(level().getMaxY() - 2, Math.max(target.getY() + 8, Mth.floor(getY()) + 1));
        int bottomY = Math.max(level().getMinY() + 1, target.getY() - 3);
        int minimumRadius = 2;
        if (target.getY() + 3 < getY()) {
            minimumRadius = Math.max(minimumRadius, Mth.ceil(getBbWidth() + 1.5F));
        }
        for (int radius = minimumRadius; radius <= 10; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    for (int y = topY; y >= bottomY; y--) {
                        BlockPos candidate = target.offset(x, y - target.getY(), z);
                        if (!level().getFluidState(candidate).is(FluidTags.WATER)
                                || !isSafeTeleportDestination(candidate, true)) continue;
                        double distance = candidate.getCenter().distanceToSqr(position());
                        if (distance < bestDistance) {
                            best = candidate.immutable();
                            bestDistance = distance;
                        }
                        break;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private void tickSpinosaurusAquaticWorkMovement(BlockPos target) {
        Vec3 approach = spinosaurusAquaticWorkPoint(target);
        Vec3 offset = approach.subtract(position());
        navigation.stop();
        getMoveControl().setWait();
        navigationTarget = target.immutable();
        double distance = offset.length();
        if (!target.equals(spinosaurusAquaticWorkTarget)) {
            spinosaurusAquaticWorkTarget = target.immutable();
            lastNavigationDistance = distance;
            lastNavigationSamplePosition = position();
            stalledNavigationTicks = 0;
        } else if (tickCount % 20 == 0) {
            double displacementSquared = lastNavigationSamplePosition == null
                    ? 0.0D : position().distanceToSqr(lastNavigationSamplePosition);
            if (!DinosaurFollowRules.madeMeaningfulProgress(
                    lastNavigationDistance, distance, displacementSquared, false)) {
                stalledNavigationTicks = Math.min(220, stalledNavigationTicks + 20);
            } else {
                stalledNavigationTicks = Math.max(0, stalledNavigationTicks - 20);
            }
            lastNavigationDistance = distance;
            lastNavigationSamplePosition = position();
        }
        if (stalledNavigationTicks >= 80) {
            BlockPos safeWorkPoint = BlockPos.containing(approach);
            if (isSafeTeleportDestination(safeWorkPoint, true)) {
                teleportTo(approach.x, safeWorkPoint.getY(), approach.z);
                setDeltaMovement(Vec3.ZERO);
                resetFallDistance();
                stalledNavigationTicks = 0;
                lastNavigationDistance = 0.0D;
                lastNavigationSamplePosition = position();
                return;
            }
        }
        if (offset.lengthSqr() > 0.02D) {
            double speed = Mth.clamp(offset.length() * 0.055D, 0.20D, 0.43D)
                    * getMutationStatMultiplier();
            Vec3 desired = offset.normalize().scale(speed);
            desired = new Vec3(desired.x, Mth.clamp(desired.y, -0.20D, 0.20D), desired.z);
            Vec3 movement = getDeltaMovement().lerp(desired, 0.24D);
            move(MoverType.SELF, movement);
            if (horizontalCollision) movement = new Vec3(movement.x * 0.30D, movement.y, movement.z * 0.30D);
            if (verticalCollision) movement = new Vec3(movement.x, 0.0D, movement.z);
            setDeltaMovement(movement.scale(0.35D));
            resetFallDistance();
            float desiredYaw = (float)(Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.approachDegrees(getYRot(), desiredYaw,
                    getSpecies().turnDegreesPerTick() * 1.45F));
            yBodyRot = Mth.approachDegrees(yBodyRot, getYRot(), getSpecies().turnDegreesPerTick());
            yHeadRot = Mth.approachDegrees(yHeadRot, desiredYaw, getSpecies().turnDegreesPerTick() * 1.8F);
            entityData.set(SPINO_SWIMMING, true);
            entityData.set(SPINO_SWIM_SPEED, (float)movement.length());
        }
    }

    private Vec3 spinosaurusAquaticWorkPoint(BlockPos target) {
        double horizontalReach = Math.max(2.1D, workInteractionDistance() + getBbWidth() * 0.35D);
        int minimumRadius = Math.max(2, Mth.ceil(getBbWidth() * 0.5D + 0.85D));
        int maximumRadius = Math.max(minimumRadius, Mth.floor(horizontalReach - 0.35D));
        int preferredY = Mth.floor(target.getY() + 1.35D - getBbHeight() * 0.38D);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = minimumRadius; radius <= maximumRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    for (int yOffset : new int[]{0, 1, -1, 2, -2}) {
                        BlockPos candidate = new BlockPos(
                                target.getX() + x, preferredY + yOffset, target.getZ() + z);
                        if (!level().getFluidState(candidate).is(FluidTags.WATER)
                                || !isSafeTeleportDestination(candidate, true)) continue;
                        double distance = candidate.getCenter().distanceToSqr(position());
                        if (distance < bestDistance) {
                            best = candidate.immutable();
                            bestDistance = distance;
                        }
                        break;
                    }
                }
            }
            if (best != null) break;
        }
        return best == null ? workApproachPoint(target) : best.getCenter();
    }

    private void markRaptorTransportRoute() {
        if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR && isTransportWorkActive()) {
            raptorTransportRunTicks = 3;
        }
    }

    private boolean isTransportWorkActive() {
        return commandMode == DinosaurCommandMode.HOME && workEnabled && workJobIndex == 0
                || fieldDutyOwnsNavigation()
                && fieldWorkMode == DinoWhistleSettings.FieldMode.COLLECT;
    }

    private boolean isAutonomousPteranodonFlightAllowed() {
        if (getSpecies() != DinosaurSpecies.PTERANODON) return false;
        if (commandMode == DinosaurCommandMode.HOME && workEnabled
                && workJobIndex >= 0 && workJobIndex <= 3) return true;
        if (isTransportWorkActive()) return true;
        ServerPlayer owner = commandOwner();
        return commandMode == DinosaurCommandMode.FOLLOW
                && owner != null
                && owner.level() == level()
                && owner.isAlive();
    }

    private void tickAutonomousTransportFlight(BlockPos target) {
        if (isInWater()) {
            stopAutonomousTransportFlight();
            setDeltaMovement(getDeltaMovement().multiply(0.45D, 0.0D, 0.45D).add(0.0D, 0.32D, 0.0D));
            return;
        }
        BlockPos immutableTarget = target.immutable();
        if (!autonomousTransportFlight) {
            autonomousTransportFlight = true;
            autonomousTransportAltitude = Math.min(level().getMaxY() - 3.0D,
                    Math.max(getY() + 3.0D, target.getY() + 4.0D));
            navigation.stop();
            pteranodonForwardHoldTicks = 0;
        } else if (autonomousTransportTarget == null
                || autonomousTransportTarget.distSqr(immutableTarget) >= 16.0D) {
            double refreshedAltitude = Math.min(level().getMaxY() - 3.0D,
                    Math.max(getY() + 2.0D, target.getY() + 4.0D));
            autonomousTransportAltitude = Mth.lerp(0.18D, autonomousTransportAltitude, refreshedAltitude);
        }
        autonomousTransportTarget = immutableTarget;
        navigationTarget = immutableTarget;

        Vec3 targetCenter = target.getCenter();
        Vec3 horizontalOffset = new Vec3(targetCenter.x - getX(), 0.0D, targetCenter.z - getZ());
        double horizontalDistance = horizontalOffset.horizontalDistance();
        double desiredY = horizontalDistance > 4.0D ? autonomousTransportAltitude : target.getY() + 1.35D;
        Vec3 desiredPoint = new Vec3(targetCenter.x, desiredY, targetCenter.z);
        Vec3 offset = desiredPoint.subtract(position());

        if (horizontalDistance <= 2.1D && Math.abs(offset.y) <= 1.35D) {
            if (commandMode == DinosaurCommandMode.HOME && workEnabled
                    && workJobIndex >= 0 && workJobIndex <= 3 && !onGround()) {
                setNoGravity(true);
                entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_HOVERING);
                setDeltaMovement(getDeltaMovement().lerp(Vec3.ZERO, 0.30D));
                entityData.set(PTERO_AIRSPEED, (float)getDeltaMovement().horizontalDistance());
            } else {
                stopAutonomousTransportFlight();
                navigation.moveTo(targetCenter.x, target.getY() + 0.5D, targetCenter.z, movementSpeedForWork());
            }
            return;
        }

        setNoGravity(true);
        entityData.set(PTERO_FLIGHT_MODE, horizontalDistance <= 2.8D
                ? PTERO_FLIGHT_HOVERING : PTERO_FLIGHT_POWERED);
        pteranodonForwardHoldTicks = Math.min(80, pteranodonForwardHoldTicks + 1);
        double speed = 0.52D * getMutationStatMultiplier();
        Vec3 desiredVelocity = offset.lengthSqr() < 1.0E-5D ? Vec3.ZERO : offset.normalize().scale(speed);
        desiredVelocity = new Vec3(desiredVelocity.x, Mth.clamp(desiredVelocity.y, -0.22D, 0.24D), desiredVelocity.z);
        Vec3 velocity = getDeltaMovement().lerp(desiredVelocity, 0.16D);
        setDeltaMovement(velocity);
        entityData.set(PTERO_AIRSPEED, (float)velocity.horizontalDistance());

        if (horizontalOffset.lengthSqr() > 0.01D) {
            float desiredYaw = (float)(Mth.atan2(horizontalOffset.z, horizontalOffset.x) * Mth.RAD_TO_DEG) - 90.0F;
            float yawError = Mth.wrapDegrees(desiredYaw - getYRot());
            float nextYaw = Mth.approachDegrees(getYRot(), desiredYaw, 6.5F);
            setYRot(nextYaw);
            yBodyRot = Mth.approachDegrees(yBodyRot, nextYaw, 7.5F);
            yHeadRot = Mth.approachDegrees(yHeadRot, desiredYaw, 9.0F);
            entityData.set(PTERO_BANK, Mth.lerp(0.16F, getPteranodonBankDegrees(),
                    Mth.clamp(-yawError * 0.30F, -20.0F, 20.0F)));
        }
        setXRot(Mth.approachDegrees(getXRot(), (float)Mth.clamp(-offset.y * 10.0D, -18.0D, 24.0D), 2.5F));
        resetFallDistance();
    }

    private void stopAutonomousTransportFlight() {
        if (!autonomousTransportFlight) return;
        autonomousTransportFlight = false;
        autonomousTransportTarget = null;
        setNoGravity(false);
        entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_GROUNDED);
        entityData.set(PTERO_AIRSPEED, 0.0F);
        entityData.set(PTERO_BANK, 0.0F);
        setXRot(0.0F);
    }

    private double movementSpeedForWork() {
        double speed = avoidDanger ? 1.02D : 1.14D;
        ItemStack carried = getCarriedStack();
        if (isTransportWorkActive() && !carried.isEmpty()) {
            speed *= WorkSpecialtyRules.transportMovementMultiplier(
                    getSpecialtyStars(0), carried.getCount(), carried.getMaxStackSize());
        }
        return speed;
    }

    private int transportPickupLimit(int maximumStackSize) {
        if (!isTransportWorkActive()) return Math.min(workBatchSize, maximumStackSize);
        int capacity = WorkSpecialtyRules.transportCapacity(
                getSpecialtyStars(0), dinosaurLevel, workBatchSize, maximumStackSize);
        if (getSpecies() == DinosaurSpecies.TRICERATOPS) {
            float loadBrace = 1.0F + 0.25F * getPassiveStrength();
            capacity = Math.min(Math.min(workBatchSize, maximumStackSize),
                    Math.max(1, Math.round(capacity * loadBrace)));
        }
        return capacity;
    }

    @Override
    protected void registerGoals() {
        DinosaurSpecies species = getSpecies();
        goalSelector.addGoal(0, species == DinosaurSpecies.SPINOSAURUS
                ? new SpinosaurusSurfaceFloatGoal()
                : new FloatGoal(this));
        if (species.combatCapable()) {
            double combatSpeed = species == DinosaurSpecies.VELOCIRAPTOR ? 1.30D
                    : species == DinosaurSpecies.TYRANNOSAURUS || species == DinosaurSpecies.SPINOSAURUS ? 1.12D
                    : 1.18D;
            goalSelector.addGoal(1, new DinosaurMeleeAttackGoal(combatSpeed));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                    this,
                    Monster.class,
                    10,
                    true,
                    false,
                    (candidate, serverLevel) -> PrimevalTuning.server().hostileMobTargeting()
                            && candidate.isAlive()
                            && insideBaseBoundary(candidate.position(), 6.0D)
            ));
        } else {
            goalSelector.addGoal(1, new PanicGoal(this, 1.35D) {
                @Override
                protected boolean shouldPanic() {
                    DamageSource source = FieldDodoEntity.this.getLastDamageSource();
                    if (source != null
                            && source.getEntity() instanceof Player player
                            && FieldDodoEntity.this.isOwnedBy(player.getUUID())) {
                        return false;
                    }
                    return super.shouldPanic();
                }
            });
        }
        goalSelector.addGoal(2, new FollowCommandOwnerGoal());
        goalSelector.addGoal(2, new StayCommandGoal());
        goalSelector.addGoal(2, new ReturnToBaseGoal());
        goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        goalSelector.addGoal(5, new BaseRoamGoal());
        goalSelector.addGoal(6, new DinosaurLookAtPlayerGoal());
        goalSelector.addGoal(7, new DinosaurAmbientLookGoal());
    }

    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        if (getSpecies() == DinosaurSpecies.TYRANNOSAURUS
                || getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            boolean spinosaurus = getSpecies() == DinosaurSpecies.SPINOSAURUS;
            double mouthReach = spinosaurus ? SPINO_MOUTH_REACH : T_REX_MOUTH_REACH;
            float attackArc = spinosaurus ? SPINO_ATTACK_ARC_DEGREES : T_REX_ATTACK_ARC_DEGREES;
            double horizontalDistance = horizontalDistanceTo(target);
            double maximumDistance = getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + mouthReach;
            double verticalDistance = Math.abs(target.getY() + target.getBbHeight() * 0.5D
                    - (getY() + getBbHeight() * 0.58D));
            return horizontalDistance <= maximumDistance
                    && verticalDistance <= Math.max(spinosaurus ? 4.4D : 2.8D, target.getBbHeight())
                    && Math.abs(yawErrorTo(target)) <= attackArc;
        }

        return super.isWithinMeleeAttackRange(target);
    }

    private boolean beginLargePredatorAttack(LivingEntity target) {
        if (pendingAttackTargetId >= 0) {
            return false;
        }

        pendingAttackTargetId = target.getId();
        boolean spinosaurus = getSpecies() == DinosaurSpecies.SPINOSAURUS;
        pendingAttackContactTick = level().getGameTime()
                + (spinosaurus ? SPINO_DAMAGE_DELAY_TICKS : T_REX_DAMAGE_DELAY_TICKS);
        entityData.set(ATTACK_ANIMATION_TICKS, spinosaurus ? 14 : 16);
        return true;
    }

    public boolean requestMountedAttack(ServerPlayer rider) {
        return requestMountedAttack(rider, rider.getYRot(), rider.getXRot());
    }

    public boolean requestMountedAttack(ServerPlayer rider, float lookYaw, float lookPitch) {
        if (getSpecies() != DinosaurSpecies.SPINOSAURUS
                || getControllingPassenger() != rider
                || !isAlive()
                || isInWater()
                || isSpinosaurusAquaticPose()
                || spinosaurusMountedAttackCooldown > 0
                || entityData.get(ATTACK_ANIMATION_TICKS) > 0) {
            return false;
        }

        spinosaurusMountedAimYaw = Mth.wrapDegrees(lookYaw);
        spinosaurusMountedAimPitch = Mth.clamp(lookPitch, -45.0F, 45.0F);
        spinosaurusMountedAimTicks = 14;
        faceMountedAttackAim();
        spinosaurusMountedAttackCooldown = SPINO_MOUNTED_ATTACK_COOLDOWN_TICKS;
        entityData.set(ATTACK_ANIMATION_TICKS, 14);

        Vec3 attackOrigin = position().add(0.0D, getBbHeight() * 0.68D, 0.0D);
        Vec3 aim = Vec3.directionFromRotation(spinosaurusMountedAimPitch, spinosaurusMountedAimYaw).normalize();
        LivingEntity target = level().getEntitiesOfClass(
                        LivingEntity.class,
                        getBoundingBox().inflate(SPINO_MOUTH_REACH + 1.5D, 2.5D, SPINO_MOUTH_REACH + 1.5D),
                        candidate -> candidate != this
                                && candidate != rider
                                && candidate.isAlive()
                                && !(candidate instanceof FieldDodoEntity)
                                && insideMountedAttackCone(candidate, attackOrigin, aim)
                                && getSensing().hasLineOfSight(candidate)
                ).stream()
                .min(Comparator.comparingDouble(candidate -> candidate.getBoundingBox().getCenter()
                        .distanceToSqr(attackOrigin)))
                .orElse(null);
        if (target != null) {
            pendingAttackTargetId = target.getId();
            pendingAttackContactTick = level().getGameTime() + SPINO_DAMAGE_DELAY_TICKS;
        }
        return true;
    }

    public void previewMountedAttack(float lookYaw, float lookPitch) {
        if (!level().isClientSide()
                || getSpecies() != DinosaurSpecies.SPINOSAURUS
                || getControllingPassenger() == null
                || isInWater()
                || isSpinosaurusAquaticPose()
                || entityData.get(ATTACK_ANIMATION_TICKS) > 0) {
            return;
        }
        spinosaurusMountedAimYaw = Mth.wrapDegrees(lookYaw);
        spinosaurusMountedAimPitch = Mth.clamp(lookPitch, -45.0F, 45.0F);
        spinosaurusMountedAimTicks = 14;
        entityData.set(ATTACK_ANIMATION_TICKS, 14);
        faceMountedAttackAim();
    }

    private boolean insideMountedAttackCone(LivingEntity target, Vec3 origin, Vec3 aim) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
        double distance = toTarget.length();
        if (distance <= 0.001D || distance > SPINO_MOUTH_REACH + getBbWidth() * 0.55D
                + target.getBbWidth() * 0.5D) return false;
        return aim.dot(toTarget.scale(1.0D / distance)) >= Math.cos(SPINO_ATTACK_ARC_DEGREES * Mth.DEG_TO_RAD);
    }

    private void tickSpinosaurusMountedAim() {
        if (spinosaurusMountedAimTicks <= 0 || getSpecies() != DinosaurSpecies.SPINOSAURUS) return;
        spinosaurusMountedAimTicks--;
        faceMountedAttackAim();
    }

    private void faceMountedAttackAim() {
        float headYaw = Mth.approachDegrees(yHeadRot, spinosaurusMountedAimYaw, 16.0F);
        yHeadRot = headYaw;
        getLookControl().setLookAt(position().add(Vec3.directionFromRotation(
                spinosaurusMountedAimPitch, spinosaurusMountedAimYaw).scale(12.0D)));
        float bodyYaw = Mth.approachDegrees(getYRot(), spinosaurusMountedAimYaw, 7.0F);
        setYRot(bodyYaw);
        yBodyRot = Mth.approachDegrees(yBodyRot, bodyYaw, 5.0F);
    }

    private double horizontalDistanceTo(LivingEntity target) {
        double x = target.getX() - getX();
        double z = target.getZ() - getZ();
        return Math.sqrt(x * x + z * z);
    }

    private float yawTo(LivingEntity target) {
        double x = target.getX() - getX();
        double z = target.getZ() - getZ();
        return (float)(Mth.atan2(z, x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private float yawErrorTo(LivingEntity target) {
        return Mth.wrapDegrees(yawTo(target) - getYRot());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return super.mobInteract(player, hand);
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.NESTING_TREAT.get())) {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
            return DinosaurBreeding.useTreat(serverPlayer, this, held);
        }
        if (held.is(ModItems.FOSSIL_FRAGMENT.get())
                && hasAlbinoMutation()
                && !hasRestoredOriginalPigment()
                && isOwnedBy(player.getUUID())) {
            if (!level().isClientSide()) {
                entityData.set(ORIGINAL_PIGMENT_RESTORED, true);
                held.consume(1, player);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.62D, getZ(),
                            14, getBbWidth() * 0.24D, getBbHeight() * 0.16D, getBbWidth() * 0.24D, 0.015D);
                }
                player.sendOverlayMessage(Component.translatable(
                        "message.primevalworks.fossil_fragment.restored", getDisplayName()));
                DinosaurOwnership.syncRecord(this);
            }
            return InteractionResult.SUCCESS;
        }
        boolean matchingSaddle = (getSpecies() == DinosaurSpecies.PTERANODON
                && held.is(ModItems.PTERANODON_SADDLE.get()))
                || (getSpecies() == DinosaurSpecies.SPINOSAURUS
                && held.is(ModItems.SPINOSAURUS_SADDLE.get()));
        if (matchingSaddle && !isSaddledMount() && isOwnedBy(player.getUUID())) {
            if (!level().isClientSide() && !isSaddledMount()) {
                entityData.set(SADDLED, true);
                held.consume(1, player);
                navigation.stop();
                DinosaurOwnership.syncRecord(this);
            }
            return InteractionResult.SUCCESS;
        }
        if (isSaddledMount() && isOwnedBy(player.getUUID()) && !player.isShiftKeyDown()) {
            if (!level().isClientSide()) {
                setTarget(null);
                navigation.stop();
                player.startRiding(this);
                DinosaurOwnership.syncRecord(this);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide()) {
            player.sendSystemMessage(Component.translatable(
                    "message.primevalworks.dinosaur_reading",
                    getDisplayName(),
                    bestSpecialtyStars(),
                    getHunger()
            ));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !isVehicle() && passenger instanceof Player && isSaddledMount();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            resetSpinosaurusAfterDismount();
        }
        if (!level().isClientSide()) DinosaurOwnership.syncRecord(this);
    }

    private void resetSpinosaurusAfterDismount() {
        boolean swimming = isInWater();
        clearSpinosaurusBreach();
        setNoGravity(false);
        entityData.set(SPINO_SWIMMING, swimming);
        entityData.set(SPINO_SWIM_SPEED, swimming ? (float)getDeltaMovement().length() : 0.0F);
        entityData.set(SPINO_BANK, 0.0F);
        entityData.set(SPINO_LAND_SPRINTING, false);
        spinosaurusWasInWater = swimming;
        spinosaurusThrottle = 0.0F;
        spinosaurusBankVelocity = 0.0F;
        spinosaurusControllerYawPrevious = Float.NaN;
        spinosaurusSteeringVelocity = 0.0F;
        spinosaurusGroundDropGraceTicks = 0;
        spinosaurusGroundDropEntrySpeed = 0.0D;
        if (!swimming) setXRot(0.0F);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return isSaddledMount() && getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        if (getSpecies() == DinosaurSpecies.PTERANODON) {
            float blend = pteranodonFlightBlend * pteranodonFlightBlend
                    * (3.0F - 2.0F * pteranodonFlightBlend);
            float hoverBlend = Mth.clamp(-pteranodonRiderRootPitch / 18.0F, 0.0F, 1.0F) * blend;
            Vec3 saddleSeat = new Vec3(
                    0.0D,
                    (Mth.lerp(blend, 1.39F, 2.08F) - hoverBlend * 0.55F) * scale,
                    (Mth.lerp(blend, 0.06F, -0.11F) - hoverBlend * 0.78F) * scale
            );
            float attachedBank = getPteranodonBankDegrees() * blend;
            float attachedPitch = getXRot() * blend;
            return saddleSeat
                    .zRot(attachedBank * Mth.DEG_TO_RAD)
                    .xRot(-attachedPitch * Mth.DEG_TO_RAD)
                    .yRot(-getYRot() * Mth.DEG_TO_RAD);
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            return getSpinosaurusPassengerOffset(1.0F);
        }
        float height = dimensions.height() * 0.77F;
        return new Vec3(0.0D, height, -0.18F * scale).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    public Vec3 getSpinosaurusPassengerOffset(float partialTick) {
        float modelScale = getScale();
        Vec3 seat = new Vec3(
                0.0D,
                (SPINO_SEAT_HEIGHT + SPINO_RIDER_VERTICAL_ADJUSTMENT) * modelScale,
                SPINO_SEAT_FORWARD * modelScale
        );
        float attachedPitch = Mth.lerp(partialTick, xRotO, getXRot());
        float attachedYaw = Mth.rotLerp(partialTick, yRotO, getYRot());
        float attachedBank = isSpinosaurusAquaticPose() ? getSpinosaurusBankDegrees(partialTick) : 0.0F;
        return seat.zRot(attachedBank * Mth.DEG_TO_RAD)
                .xRot(-attachedPitch * Mth.DEG_TO_RAD)
                .yRot(-attachedYaw * Mth.DEG_TO_RAD);
    }

    public double getSpinosaurusRiderVerticalAdjustment() {
        return SPINO_RIDER_VERTICAL_ADJUSTMENT * getScale();
    }

    @Override
    public boolean isFlyingVehicle() {
        return isPteranodonAirborne();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS || super.canBreatheUnderwater();
    }

    @Override
    public boolean isPushable() {
        return !getSpecies().heavyweight() && super.isPushable();
    }

    @Override
    public boolean skipAttackInteraction(Entity source) {
        return source == getControllingPassenger() || super.skipAttackInteraction(source);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!getSpecies().heavyweight()) super.knockback(strength, x, z);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        if (getSpecies() == DinosaurSpecies.PTERANODON) {
            float forward;
            boolean climb;
            boolean descend;
            if (controller instanceof ServerPlayer serverPlayer) {
                Input input = serverPlayer.getLastClientInput();
                forward = input.forward() == input.backward() ? 0.0F : input.forward() ? 1.0F : -0.42F;
                climb = input.jump();
                descend = input.sprint();
            } else {
                forward = controller.zza;
                if (forward < 0.0F) forward *= 0.42F;
                climb = controller.isJumping();
                descend = pteranodonClientDescendInput;
            }
            float vertical = (climb ? 1.0F : 0.0F) - (descend ? 1.0F : 0.0F);
            return new Vec3(0.0D, vertical, forward);
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            float forward;
            boolean ascend;
            boolean descend;
            if (controller instanceof ServerPlayer serverPlayer) {
                Input input = serverPlayer.getLastClientInput();
                forward = input.forward() == input.backward() ? 0.0F : input.forward() ? 1.0F : -0.35F;
                ascend = input.jump();
                descend = input.sprint();
            } else {
                forward = controller.zza;
                if (forward < 0.0F) forward *= 0.35F;
                ascend = controller.isJumping();
                descend = spinosaurusClientDescendInput;
            }
            double vertical = isInWater() || isSpinosaurusBreaching()
                    ? (ascend ? 1.0D : 0.0D) - (descend ? 1.0D : 0.0D)
                    : 0.0D;
            return new Vec3(0.0D, vertical, forward);
        }
        float forward = controller.zza;
        if (forward < 0.0F) forward *= 0.42F;
        return new Vec3(controller.xxa * 0.58F, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        if (getSpecies() == DinosaurSpecies.PTERANODON
                && entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_GROUNDED) {
            return (float)getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.72F;
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS
                && !isInWater()
                && !isSpinosaurusBreaching()) {
            return (float)getAttributeValue(Attributes.MOVEMENT_SPEED)
                    * (isSpinosaurusLandSprinting()
                    ? SPINO_LAND_SPRINT_MULTIPLIER * (float)PrimevalTuning.server().spinosaurusSprintSpeed()
                    : 1.10F);
        }
        return (float)getAttributeValue(Attributes.MOVEMENT_SPEED)
                * (controller.isSprinting() ? 1.36F : 1.05F);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        navigation.stop();
        setTarget(null);
        setJumping(false);
        if (getSpecies() == DinosaurSpecies.PTERANODON) {
            tickPteranodonFlight(controller, riddenInput);
            return;
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            tickSpinosaurusRide(controller, riddenInput);
            return;
        }
        float yawDelta = Mth.wrapDegrees(controller.getYRot() - getYRot());
        float turnRate = 5.5F;
        float yaw = getYRot() + Mth.clamp(yawDelta, -turnRate, turnRate);
        float pitch = Mth.approachDegrees(getXRot(), 0.0F, 3.0F);
        setRot(yaw, pitch);
        yBodyRot = Mth.rotLerp(0.22F, yBodyRot, yaw);
        yHeadRot = yaw;
    }

    private void tickPteranodonFlight(Player controller, Vec3 riddenInput) {
        Vec3 current = getDeltaMovement();
        if (pteranodonTakeoffGraceTicks > 0) {
            pteranodonTakeoffGraceTicks--;
        }
        if (isInWater()) {
            resetPteranodonGroundState();
            setXRot(Mth.approachDegrees(getXRot(), -28.0F, 6.0F));
            setDeltaMovement(current.x * 0.55D, Math.max(0.34D, current.y), current.z * 0.55D);
            resetFallDistance();
            return;
        }
        float currentSpeed = (float)current.length();
        boolean flightActive = entityData.get(PTERO_FLIGHT_MODE) != PTERO_FLIGHT_GROUNDED;
        if (flightActive && pteranodonTakeoffGraceTicks == 0 && onGround() && current.y <= 0.08D) {
            resetPteranodonGroundState();
            flightActive = false;
            current = getDeltaMovement();
        }
        if (!flightActive) {
            adjustPteranodonStamina(PTERO_GROUND_STAMINA_RECOVERY);
        }
        boolean staminaAvailable = !isPteranodonExhausted()
                && (flightActive || getPteranodonStamina() >= PTERO_MIN_TAKEOFF_STAMINA);
        float rawForward = !flightActive || staminaAvailable ? Math.max(0.0F, (float)riddenInput.z) : 0.0F;
        pteranodonThrottle = Mth.approach(pteranodonThrottle, rawForward,
                rawForward > pteranodonThrottle ? 0.20F : 0.12F);
        float forward = pteranodonThrottle;
        boolean forwardPressed = rawForward > 0.05F;
        boolean climbing = riddenInput.y > 0.01D && staminaAvailable;
        boolean descending = riddenInput.y < -0.01D;
        boolean braking = riddenInput.z < -0.05D && !isPteranodonExhausted();
        double mountSpeedMultiplier = getMutationStatMultiplier();
        double configuredFlightSpeed = PrimevalTuning.server().pteranodonFlightSpeed();
        double cruiseSpeed = PTERO_CRUISE_SPEED * mountSpeedMultiplier * configuredFlightSpeed;
        double boostSpeed = PTERO_BOOST_SPEED * mountSpeedMultiplier * configuredFlightSpeed;
        float speedFactor = Mth.clamp(getPteranodonFlightSpeed() / (float)boostSpeed, 0.0F, 1.0F);
        float lookPitch = Mth.clamp(controller.getXRot(), -55.0F, 58.0F);
        boolean wasGliding = entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_GLIDING;
        double glideThreshold = wasGliding ? PTERO_GLIDE_EXIT_SPEED : PTERO_GLIDE_ENTRY_SPEED;
        double stableAirspeed = Math.max(current.horizontalDistance(), entityData.get(PTERO_AIRSPEED));
        boolean coastingGlide = !forwardPressed && stableAirspeed > glideThreshold;
        float diveGlideAngle = wasGliding ? 11.0F : 19.0F;
        double diveGlideSpeed = wasGliding ? 0.58D : 0.82D;
        boolean poweredDiveGlide = forwardPressed
                && lookPitch > diveGlideAngle
                && stableAirspeed > diveGlideSpeed;
        boolean gliding = flightActive && !climbing && !descending && !braking
                && (isPteranodonExhausted() || coastingGlide || poweredDiveGlide);

        float controllerYaw = controller.getYRot();
        float cameraTurn = Float.isNaN(pteranodonControllerYawPrevious)
                ? 0.0F
                : Mth.wrapDegrees(controllerYaw - pteranodonControllerYawPrevious);
        pteranodonControllerYawPrevious = controllerYaw;
        pteranodonSteeringVelocity = Mth.lerp(
                flightActive ? 0.32F : 0.48F,
                pteranodonSteeringVelocity,
                flightActive ? cameraTurn : 0.0F
        );
        float steeringError = Mth.wrapDegrees(controllerYaw - getYRot());
        float previousYaw = getYRot();
        float turnRate = flightActive
                ? Mth.lerp(speedFactor, 4.5F, 8.5F)
                : Mth.lerp(speedFactor, 6.0F, 9.0F);
        float yaw = Mth.approachDegrees(getYRot(), controllerYaw, turnRate);
        float exhaustedGlidePitch = isPteranodonExhausted() ? Math.max(14.0F, lookPitch) : lookPitch;
        float desiredPitch = flightActive
                ? (descending ? Math.max(lookPitch, 38.0F)
                        : forwardPressed ? lookPitch
                        : gliding ? Mth.clamp(exhaustedGlidePitch, -8.0F, 46.0F)
                        : 0.0F)
                : 0.0F;
        float pitchRate = !flightActive ? 5.0F : descending ? 7.5F : gliding ? 2.15F : 3.2F;
        float pitch = Mth.approachDegrees(getXRot(), desiredPitch, pitchRate);
        setRot(yaw, pitch);
        yBodyRot = Mth.approachDegrees(yBodyRot, yaw, turnRate * 1.18F);
        yHeadRot = yaw;
        controller.yBodyRot = Mth.approachDegrees(
                controller.yBodyRot,
                yaw,
                flightActive ? turnRate * 1.45F : turnRate * 1.9F
        );
        float appliedTurn = Mth.wrapDegrees(yaw - previousYaw);
        float targetBank = flightActive
                ? Mth.clamp(
                        -steeringError * 0.30F
                                - appliedTurn * 2.25F
                                - pteranodonSteeringVelocity * 4.8F,
                        -35.0F,
                        35.0F
                )
                        * Mth.clamp(0.42F + speedFactor * 0.75F, 0.0F, 1.0F)
                : 0.0F;
        float bank = getPteranodonBankDegrees();
        pteranodonBankVelocity += (targetBank - bank) * (flightActive ? 0.105F : 0.18F);
        pteranodonBankVelocity *= flightActive ? 0.70F : 0.60F;
        pteranodonBankVelocity = Mth.clamp(pteranodonBankVelocity, -2.35F, 2.35F);
        entityData.set(PTERO_BANK, Mth.clamp(bank + pteranodonBankVelocity, -28.0F, 28.0F));

        if (flightActive && rawForward > 0.05F) {
            pteranodonForwardHoldTicks = Math.min(80, pteranodonForwardHoldTicks + 1);
        } else if (flightActive) {
            pteranodonForwardHoldTicks = Math.max(0, pteranodonForwardHoldTicks - 4);
        } else {
            pteranodonForwardHoldTicks = 0;
        }
        float accelerationProgress = Mth.clamp((pteranodonForwardHoldTicks - 4) / 52.0F, 0.0F, 1.0F);
        accelerationProgress = accelerationProgress * accelerationProgress * (3.0F - 2.0F * accelerationProgress);
        setSprinting(flightActive && accelerationProgress > 0.72F && forwardPressed);

        if (!flightActive) {
            entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_GROUNDED);
            entityData.set(PTERO_AIRSPEED, 0.0F);
            setNoGravity(false);
            if (!climbing) {
                pteranodonTakeoffChargeTicks = 0;
                resetFallDistance();
                return;
            }
            pteranodonTakeoffChargeTicks++;
            if (pteranodonTakeoffChargeTicks < 3) {
                resetFallDistance();
                return;
            }
            pteranodonTakeoffChargeTicks = 0;
            Vec3 launchDirection = Vec3.directionFromRotation(0.0F, yaw);
            if (launchDirection.lengthSqr() < 1.0E-5D) {
                launchDirection = Vec3.directionFromRotation(0.0F, yaw);
            }
            setNoGravity(true);
            entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_POWERED);
            pteranodonTakeoffGraceTicks = 8;
            Vec3 launchMovement = forwardPressed
                    ? launchDirection.normalize().scale(0.24D)
                    : Vec3.ZERO;
            setDeltaMovement(launchMovement.add(0.0D, 0.30D, 0.0D));
            entityData.set(PTERO_AIRSPEED, forwardPressed ? 0.24F : 0.0F);
            resetFallDistance();
            return;
        }

        setNoGravity(true);
        Vec3 forwardDirection = Vec3.directionFromRotation(pitch, yaw);
        double sustainedSpeed = cruiseSpeed + (boostSpeed - cruiseSpeed) * accelerationProgress;
        double targetSpeed = sustainedSpeed * forward;
        float dive = Mth.clamp(pitch / 58.0F, 0.0F, 1.0F);
        if (forwardPressed) {
            targetSpeed += dive * (0.15D + accelerationProgress * 0.10D);
        }

        boolean powered = forwardPressed || climbing || descending || braking;
        Vec3 desired;
        if (forwardPressed) {
            desired = forwardDirection.scale(targetSpeed);
            double lift = climbing ? 0.27D : descending ? -0.24D : 0.018D;
            desired = desired.add(0.0D, lift, 0.0D);
        } else if (climbing) {
            desired = new Vec3(current.x * 0.68D, 0.34D, current.z * 0.68D);
        } else if (descending) {
            desired = new Vec3(current.x * 0.76D, -0.38D, current.z * 0.76D);
        } else if (braking) {
            desired = new Vec3(current.x * 0.42D, 0.0D, current.z * 0.42D);
        } else if (gliding) {
            float pullUp = Mth.clamp(-pitch / 8.0F, 0.0F, 1.0F);
            double horizontalSpeed = Math.max(current.horizontalDistance(), entityData.get(PTERO_AIRSPEED));
            double glideSpeed = Mth.clamp(
                    horizontalSpeed * 0.992D + 0.008D + dive * 0.022D - pullUp * 0.006D,
                    PTERO_GLIDE_EXIT_SPEED,
                    boostSpeed + 0.24D * mountSpeedMultiplier
            );
            Vec3 glideDirection = Vec3.directionFromRotation(0.0F, yaw).normalize();
            double sink = -0.052D - dive * (0.26D + speedFactor * 0.10D) + pullUp * 0.040D;
            desired = glideDirection.scale(glideSpeed).add(0.0D, sink, 0.0D);
        } else {
            desired = Vec3.ZERO;
        }

        double response = powered ? 0.18D + accelerationProgress * 0.04D
                : gliding ? 0.075D : 0.30D;
        Vec3 next = current.lerp(desired, response);
        double maximum = boostSpeed + 0.28D * mountSpeedMultiplier;
        if (next.length() > maximum) {
            next = next.normalize().scale(maximum);
        }
        next = new Vec3(next.x, Mth.clamp(next.y, -0.90D, 0.72D), next.z);
        if (!powered && !gliding && next.horizontalDistanceSqr() < 0.000225D) {
            next = new Vec3(0.0D, Math.abs(next.y) < 0.012D ? 0.0D : next.y, 0.0D);
        }
        setDeltaMovement(next);
        float currentAirspeed = entityData.get(PTERO_AIRSPEED);
        float airspeedTarget;
        float airspeedResponse;
        if (forwardPressed) {
            airspeedTarget = (float)Math.max(next.horizontalDistance(), targetSpeed);
            airspeedResponse = 0.20F;
        } else if (gliding) {
            airspeedTarget = (float)next.horizontalDistance();
            airspeedResponse = 0.12F;
        } else if (climbing || descending) {
            airspeedTarget = (float)next.horizontalDistance();
            airspeedResponse = 0.20F;
        } else {
            airspeedTarget = 0.0F;
            airspeedResponse = braking ? 0.34F : 0.18F;
        }
        entityData.set(PTERO_AIRSPEED, Mth.clamp(
                Mth.lerp(airspeedResponse, currentAirspeed, airspeedTarget),
                0.0F,
                (float)(boostSpeed + 0.24D * mountSpeedMultiplier)
        ));
        int mode = gliding ? PTERO_FLIGHT_GLIDING
                : !powered ? PTERO_FLIGHT_HOVERING
                : PTERO_FLIGHT_POWERED;
        entityData.set(PTERO_FLIGHT_MODE, mode);
        updatePteranodonFlightStamina(forwardPressed, climbing, descending, gliding, accelerationProgress);
        resetFallDistance();
    }

    private void tickSpinosaurusRide(Player controller, Vec3 riddenInput) {
        Vec3 current = getDeltaMovement();
        boolean submerged = isInWater();
        if (isSpinosaurusBreaching()) {
            if (!(submerged && current.y < -0.055D)) {
                adjustSpinosaurusLandStamina(SPINO_LAND_STAMINA_RECOVERY);
                tickSpinosaurusBreach(controller, current);
                return;
            }
            emitSpinosaurusWaterEntryParticles(current.length());
            clearSpinosaurusBreach();
        }
        if (submerged) {
            adjustSpinosaurusLandStamina(SPINO_LAND_STAMINA_RECOVERY);
            clearSpinosaurusBreach();
            entityData.set(SPINO_SWIMMING, true);
            entityData.set(SPINO_LAND_SPRINTING, false);
            setNoGravity(true);
            resetFallDistance();
            spinosaurusWasInWater = true;
            if (current.y > 0.10D && tickCount % 3 == 0) {
                emitSpinosaurusAscentBubbles(current.y);
            }

            if (!level().isClientSide()) {
                if (controller.tickCount % 10 != 0) {
                    controller.setAirSupply(Math.min(controller.getMaxAirSupply(), controller.getAirSupply() + 1));
                }
            }

            float rawForward = Math.max(0.0F, (float)riddenInput.z);
            spinosaurusThrottle = Mth.approach(spinosaurusThrottle, rawForward,
                    rawForward > spinosaurusThrottle ? 0.085F : 0.12F);
            float speedProgress = spinosaurusThrottle * spinosaurusThrottle
                    * (3.0F - 2.0F * spinosaurusThrottle);
            double mountMultiplier = getMutationStatMultiplier()
                    * DinosaurProgression.movementMultiplier(dinosaurLevel);
            double configuredSwimSpeed = PrimevalTuning.server().spinosaurusSwimSpeed();
            double maximumSpeed = SPINO_MAX_SWIM_SPEED * mountMultiplier * configuredSwimSpeed;
            double targetSpeed = Mth.lerp(speedProgress, SPINO_CRUISE_SPEED * configuredSwimSpeed, maximumSpeed)
                    * spinosaurusThrottle;

            float controllerYaw = controller.getYRot();
            float cameraTurn = Float.isNaN(spinosaurusControllerYawPrevious)
                    ? 0.0F
                    : Mth.wrapDegrees(controllerYaw - spinosaurusControllerYawPrevious);
            spinosaurusControllerYawPrevious = controllerYaw;
            spinosaurusSteeringVelocity = Mth.lerp(0.24F, spinosaurusSteeringVelocity, cameraTurn);
            float steeringError = Mth.wrapDegrees(controllerYaw - getYRot());
            float speedFactor = Mth.clamp((float)(current.length() / Math.max(0.01D, maximumSpeed)), 0.0F, 1.0F);
            float turnRate = Mth.lerp(speedFactor, 6.0F, 9.5F);
            float yaw = Mth.approachDegrees(getYRot(), controllerYaw, turnRate);
            float lookPitch = Mth.clamp(controller.getXRot(), -58.0F, 58.0F);
            if (riddenInput.y > 0.01D) lookPitch = Math.min(lookPitch, -30.0F);
            if (riddenInput.y < -0.01D) lookPitch = Math.max(lookPitch, 30.0F);
            float pitch = Mth.approachDegrees(getXRot(), rawForward > 0.02F ? lookPitch : 0.0F, 2.6F);
            setRot(yaw, pitch);
            yBodyRot = Mth.approachDegrees(yBodyRot, yaw, turnRate * 1.15F);
            yHeadRot = Mth.approachDegrees(yHeadRot, controllerYaw, turnRate * 1.65F);
            getLookControl().setLookAt(position().add(
                    Vec3.directionFromRotation(controller.getXRot(), controllerYaw).scale(16.0D)));
            controller.yBodyRot = Mth.approachDegrees(controller.yBodyRot, yaw, turnRate * 1.35F);

            float targetBank = Mth.clamp(
                    -steeringError * 0.24F - cameraTurn * 2.2F - spinosaurusSteeringVelocity * 2.8F,
                    -29.0F, 29.0F) * Mth.clamp(0.3F + speedFactor, 0.0F, 1.0F);
            float bank = getSpinosaurusBankDegrees();
            spinosaurusBankVelocity += (targetBank - bank) * 0.085F;
            spinosaurusBankVelocity *= 0.74F;
            spinosaurusBankVelocity = Mth.clamp(spinosaurusBankVelocity, -1.8F, 1.8F);
            entityData.set(SPINO_BANK, Mth.clamp(bank + spinosaurusBankVelocity, -27.0F, 27.0F));

            Vec3 desired = rawForward > 0.02F
                    ? Vec3.directionFromRotation(pitch, yaw).scale(targetSpeed)
                    : Vec3.ZERO;
            if (riddenInput.y > 0.01D) desired = desired.add(0.0D, 0.18D, 0.0D);
            if (riddenInput.y < -0.01D) desired = desired.add(0.0D, -0.18D, 0.0D);
            Vec3 next = current.lerp(desired, rawForward > 0.02F ? 0.115D : 0.19D);
            if (next.length() > maximumSpeed) next = next.normalize().scale(maximumSpeed);
            setDeltaMovement(next);
            entityData.set(SPINO_SWIM_SPEED, Mth.lerp(0.18F, entityData.get(SPINO_SWIM_SPEED),
                    (float)next.length()));
            return;
        }

        boolean launch = spinosaurusWasInWater;
        spinosaurusWasInWater = false;
        if (launch) {
            adjustSpinosaurusLandStamina(SPINO_LAND_STAMINA_RECOVERY);
            float exitPitch = Mth.clamp(controller.getXRot(), -58.0F, 32.0F);
            float upwardAngle = Mth.clamp(-exitPitch / 58.0F, 0.0F, 1.0F);
            double speed = Math.max(0.26D, current.length());
            double configuredSwimSpeed = PrimevalTuning.server().spinosaurusSwimSpeed();
            double speedRatio = Mth.clamp(speed / (SPINO_MAX_SWIM_SPEED * getMutationStatMultiplier()
                    * configuredSwimSpeed), 0.0D, 1.0D);
            double horizontal = Mth.clamp(current.horizontalDistance(), 0.34D,
                    SPINO_MAX_SWIM_SPEED * getMutationStatMultiplier() * configuredSwimSpeed);
            double vertical = 0.34D + speedRatio * 0.72D + upwardAngle * 0.94D;
            Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
            setDeltaMovement(forward.scale(horizontal).add(0.0D, vertical, 0.0D));
            int breachTicks = Mth.clamp((int)Math.round(34.0D + speedRatio * 24.0D + upwardAngle * 18.0D),
                    30, SPINO_BREACH_TICKS + 14);
            if (level().isClientSide()) {
                spinosaurusPredictedBreachTicks = breachTicks;
            } else {
                entityData.set(SPINO_BREACH_TIME, breachTicks);
            }
            entityData.set(SPINO_SWIMMING, true);
            entityData.set(SPINO_LAND_SPRINTING, false);
            entityData.set(SPINO_SWIM_SPEED, (float)speed);
            spinosaurusBreachHits.clear();
            setNoGravity(false);
            resetFallDistance();
            return;
        }

        setNoGravity(false);
        entityData.set(SPINO_SWIMMING, false);
        boolean sprinting = isSpinosaurusLandSprinting() && riddenInput.z > 0.05D;
        if (!sprinting) entityData.set(SPINO_LAND_SPRINTING, false);
        adjustSpinosaurusLandStamina(sprinting
                ? -SPINO_LAND_STAMINA_DRAIN
                : SPINO_LAND_STAMINA_RECOVERY);
        spinosaurusThrottle = 0.0F;
        entityData.set(SPINO_SWIM_SPEED, Mth.approach(entityData.get(SPINO_SWIM_SPEED), 0.0F, 0.12F));
        entityData.set(SPINO_BANK, Mth.approach(getSpinosaurusBankDegrees(), 0.0F, 2.4F));
        float yaw = Mth.approachDegrees(getYRot(), controller.getYRot(), 7.5F);
        float terrainPitch = riddenInput.z > 0.02D ? spinosaurusTerrainPitch(yaw) : 0.0F;
        setRot(yaw, Mth.rotLerp(0.18F, getXRot(), terrainPitch));
        yBodyRot = Mth.approachDegrees(yBodyRot, yaw, 7.0F);
        yHeadRot = Mth.approachDegrees(yHeadRot, controller.getYRot(), 11.0F);
        getLookControl().setLookAt(position().add(
                Vec3.directionFromRotation(controller.getXRot(), controller.getYRot()).scale(16.0D)));
    }

    private void tickSpinosaurusBreach(Player controller, Vec3 current) {
        int remaining;
        if (level().isClientSide()) {
            remaining = Math.max(spinosaurusPredictedBreachTicks, entityData.get(SPINO_BREACH_TIME));
            spinosaurusPredictedBreachTicks = Math.max(0, remaining - 1);
        } else {
            remaining = entityData.get(SPINO_BREACH_TIME);
            entityData.set(SPINO_BREACH_TIME, Math.max(0, remaining - 1));
        }
        if (remaining <= 1 || onGround()) {
            clearSpinosaurusBreach();
            return;
        }

        entityData.set(SPINO_SWIMMING, true);
        setNoGravity(false);
        float desiredYaw = controller.getYRot();
        float yaw = Mth.approachDegrees(getYRot(), desiredYaw, 6.5F);
        double horizontalSpeed = current.horizontalDistance();
        Vec3 desiredHorizontal = Vec3.directionFromRotation(0.0F, yaw).scale(horizontalSpeed);
        Vec3 steered = current.lerp(new Vec3(desiredHorizontal.x, current.y, desiredHorizontal.z), 0.14D);
        setDeltaMovement(steered);

        float trajectoryPitch = steered.y > 0.12D ? -24.0F
                : Mth.lerp(Mth.clamp((float)-steered.y / 1.1F, 0.0F, 1.0F), -4.0F, 48.0F);
        float lookPitch = Mth.clamp(controller.getXRot(), -50.0F, 52.0F);
        float targetPitch = Mth.lerp(0.30F, trajectoryPitch, lookPitch);
        setRot(yaw, Mth.approachDegrees(getXRot(), targetPitch, 2.8F));
        yBodyRot = Mth.approachDegrees(yBodyRot, yaw, 4.4F);
        yHeadRot = Mth.approachDegrees(yHeadRot, desiredYaw, 7.2F);
        getLookControl().setLookAt(position().add(
                Vec3.directionFromRotation(lookPitch, desiredYaw).scale(16.0D)));
        entityData.set(SPINO_BANK, Mth.approach(getSpinosaurusBankDegrees(), 0.0F, 1.35F));
    }

    private void emitSpinosaurusWaterEntryParticles(double speed) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        double spread = Math.max(0.65D, getBbWidth() * 0.34D);
        double motion = Mth.clamp(speed * 0.16D, 0.08D, 0.34D);
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                getX(), getY() + 0.32D, getZ(),
                34, spread, 0.24D, spread, motion);
        serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                getX(), getY() + 0.48D, getZ(),
                22, spread * 0.86D, 0.16D, spread * 0.86D, motion * 0.72D);
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                getX(), getY() + 0.58D, getZ(),
                18, spread, 0.10D, spread, motion * 0.52D);
    }

    private void emitSpinosaurusAscentBubbles(double verticalSpeed) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        double spread = Math.max(0.24D, getBbWidth() * 0.18D);
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                getX(), getY() + getBbHeight() * 0.34D, getZ(),
                5, spread, getBbHeight() * 0.12D, spread,
                Mth.clamp(verticalSpeed * 0.08D, 0.02D, 0.12D));
    }

    private void clearSpinosaurusBreach() {
        entityData.set(SPINO_BREACH_TIME, 0);
        spinosaurusPredictedBreachTicks = 0;
        spinosaurusBreachHits.clear();
    }

    private float spinosaurusTerrainPitch(float yaw) {
        double radians = yaw * Mth.DEG_TO_RAD;
        double distance = Math.max(1.35D, getBbWidth() * 0.36D);
        double forwardX = -Mth.sin((float)radians);
        double forwardZ = Mth.cos((float)radians);
        Double frontHeight = spinosaurusGroundHeight(
                getX() + forwardX * distance,
                getZ() + forwardZ * distance
        );
        Double rearHeight = spinosaurusGroundHeight(
                getX() - forwardX * distance,
                getZ() - forwardZ * distance
        );
        if (frontHeight == null || rearHeight == null) return 0.0F;
        return SpinosaurusGroundRideRules.terrainPitchDegrees(
                frontHeight, rearHeight, distance * 2.0D);
    }

    private @Nullable Double spinosaurusGroundHeight(double sampleX, double sampleZ) {
        Vec3 from = new Vec3(sampleX, getY() + 2.2D, sampleZ);
        Vec3 to = new Vec3(sampleX, getY() - 3.2D, sampleZ);
        HitResult hit = level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation().y;
    }

    private void damageSpinosaurusBreachContacts() {
        if (!(level() instanceof ServerLevel serverLevel) || !isSpinosaurusBreaching()) return;
        double speed = getDeltaMovement().length();
        if (speed < 0.72D) return;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(0.45D), candidate -> candidate != this
                        && candidate.isAlive() && !hasPassenger(candidate))) {
            if (!spinosaurusBreachHits.add(target.getUUID())) continue;
            target.hurtServer(serverLevel, damageSources().mobAttack(this),
                    (float)Math.min(22.0D, 7.0D + speed * 7.5D));
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS
                && (getControllingPassenger() != null || isSpinosaurusBreaching())) {
            resetFallDistance();
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    private void updatePteranodonFlightStamina(
            boolean forwardPressed,
            boolean climbing,
            boolean descending,
            boolean gliding,
            float accelerationProgress
    ) {
        if (gliding) {
            adjustPteranodonStamina(PTERO_GLIDE_STAMINA_RECOVERY);
            return;
        }
        if (descending) {
            adjustPteranodonStamina(0.08F);
            return;
        }

        float drain = 0.065F;
        if (forwardPressed) {
            drain += 0.085F + accelerationProgress * 0.070F;
        }
        if (climbing) {
            drain += 0.190F;
        }
        adjustPteranodonStamina(-drain);
    }

    private void adjustPteranodonStamina(float amount) {
        if (level().isClientSide()) {
            return;
        }
        amount *= amount < 0.0F
                ? (float)PrimevalTuning.server().pteranodonStaminaDrain()
                : (float)PrimevalTuning.server().pteranodonStaminaRecovery();
        float stamina = Mth.clamp(getPteranodonStamina() + amount, 0.0F, PTERO_MAX_STAMINA);
        if (Math.abs(stamina - entityData.get(PTERO_STAMINA)) > 0.0001F) {
            entityData.set(PTERO_STAMINA, stamina);
        }

        boolean exhausted = entityData.get(PTERO_EXHAUSTED);
        if (!exhausted && stamina <= 0.0F) {
            entityData.set(PTERO_EXHAUSTED, true);
        } else if (exhausted && stamina >= PTERO_EXHAUSTION_RECOVERY) {
            entityData.set(PTERO_EXHAUSTED, false);
        }
    }

    private void adjustSpinosaurusLandStamina(float amount) {
        if (level().isClientSide() || getSpecies() != DinosaurSpecies.SPINOSAURUS) return;
        amount *= amount < 0.0F
                ? (float)PrimevalTuning.server().spinosaurusStaminaDrain()
                : (float)PrimevalTuning.server().spinosaurusStaminaRecovery();
        float stamina = Mth.clamp(getSpinosaurusLandStamina() + amount,
                0.0F, SPINO_MAX_LAND_STAMINA);
        if (Math.abs(stamina - entityData.get(SPINO_LAND_STAMINA)) > 0.0001F) {
            entityData.set(SPINO_LAND_STAMINA, stamina);
        }

        boolean exhausted = entityData.get(SPINO_LAND_EXHAUSTED);
        if (!exhausted && stamina <= 0.0F) {
            entityData.set(SPINO_LAND_EXHAUSTED, true);
            entityData.set(SPINO_LAND_SPRINTING, false);
        } else if (exhausted && stamina >= SPINO_LAND_EXHAUSTION_RECOVERY) {
            entityData.set(SPINO_LAND_EXHAUSTED, false);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (getWorkAction() > 0 && workLockedPosition != null) {
            navigation.stop();
            setSpeed(0.0F);
            setDeltaMovement(Vec3.ZERO);
            setPos(workLockedPosition.x, workLockedPosition.y, workLockedPosition.z);
            calculateEntityAnimation(false);
            return;
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS && getControllingPassenger() != null) {
            if (isSpinosaurusBreaching()) {
                Vec3 movement = getDeltaMovement();
                move(MoverType.SELF, movement);
                damageSpinosaurusBreachContacts();
                if (horizontalCollision) movement = new Vec3(movement.x * 0.30D, movement.y, movement.z * 0.30D);
                if (verticalCollision) movement = new Vec3(movement.x, 0.0D, movement.z);
                movement = movement.add(0.0D, -0.08D, 0.0D).scale(0.98D);
                setDeltaMovement(movement);
                calculateEntityAnimation(true);
                resetFallDistance();
                return;
            }
            if (isSpinosaurusSwimming()) {
                Vec3 movement = getDeltaMovement();
                move(MoverType.SELF, movement);
                if (horizontalCollision) movement = new Vec3(movement.x * 0.32D, movement.y, movement.z * 0.32D);
                if (verticalCollision) movement = new Vec3(movement.x, movement.y * 0.25D, movement.z);
                setDeltaMovement(movement.scale(0.992D));
                calculateEntityAnimation(true);
                resetFallDistance();
                return;
            }
            boolean wasGrounded = onGround();
            double horizontalSpeedBefore = getDeltaMovement().horizontalDistance();
            super.travel(input);
            preserveSpinosaurusStepDownMomentum(input, wasGrounded, horizontalSpeedBefore);
            return;
        }
        if (getSpecies() == DinosaurSpecies.PTERANODON && getControllingPassenger() != null) {
            if (entityData.get(PTERO_FLIGHT_MODE) == PTERO_FLIGHT_GROUNDED) {
                setNoGravity(false);
                super.travel(input);
                return;
            }
            Vec3 movement = getDeltaMovement();
            move(MoverType.SELF, movement);
            if (horizontalCollision) movement = new Vec3(movement.x * 0.18D, movement.y, movement.z * 0.18D);
            if (horizontalCollision) entityData.set(PTERO_AIRSPEED, entityData.get(PTERO_AIRSPEED) * 0.35F);
            if (verticalCollision) movement = new Vec3(movement.x, 0.0D, movement.z);
            if (pteranodonTakeoffGraceTicks == 0 && onGround() && movement.y <= 0.0D) {
                resetPteranodonGroundState();
            }
            setDeltaMovement(movement.scale(0.997D));
            calculateEntityAnimation(true);
            return;
        }
        if (getSpecies() == DinosaurSpecies.PTERANODON && autonomousTransportFlight) {
            Vec3 movement = getDeltaMovement();
            move(MoverType.SELF, movement);
            if (horizontalCollision) {
                autonomousTransportAltitude = Math.min(level().getMaxY() - 3.0D,
                        autonomousTransportAltitude + 1.0D);
                movement = new Vec3(movement.x * 0.35D, Math.max(0.16D, movement.y), movement.z * 0.35D);
            }
            if (verticalCollision) movement = new Vec3(movement.x, 0.0D, movement.z);
            setDeltaMovement(movement.scale(0.992D));
            calculateEntityAnimation(true);
            resetFallDistance();
            return;
        }
        if (getSpecies() == DinosaurSpecies.PTERANODON) {
            entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_GROUNDED);
            entityData.set(PTERO_AIRSPEED, 0.0F);
            pteranodonThrottle = 0.0F;
            pteranodonForwardHoldTicks = 0;
            pteranodonTakeoffChargeTicks = 0;
            pteranodonClientDescendInput = false;
            pteranodonBankVelocity = 0.0F;
            pteranodonControllerYawPrevious = Float.NaN;
            pteranodonSteeringVelocity = 0.0F;
            entityData.set(PTERO_BANK, Mth.approach(getPteranodonBankDegrees(), 0.0F, 3.8F));
        }
        if (getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            boolean unmountedInWater = getControllingPassenger() == null && isInWater();
            entityData.set(SPINO_SWIMMING, unmountedInWater);
            entityData.set(SPINO_BREACH_TIME, 0);
            entityData.set(SPINO_SWIM_SPEED, unmountedInWater
                    ? (float)getDeltaMovement().length()
                    : 0.0F);
            entityData.set(SPINO_LAND_SPRINTING, false);
            entityData.set(SPINO_BANK, Mth.approach(getSpinosaurusBankDegrees(), 0.0F, 2.4F));
            spinosaurusThrottle = 0.0F;
            spinosaurusWasInWater = false;
            spinosaurusPredictedBreachTicks = 0;
        }
        setNoGravity(false);
        super.travel(input);
    }

    private void preserveSpinosaurusStepDownMomentum(
            Vec3 input,
            boolean wasGrounded,
            double horizontalSpeedBefore
    ) {
        Vec3 movement = getDeltaMovement();
        if (SpinosaurusGroundRideRules.shouldPreserveDropMomentum(
                wasGrounded,
                onGround(),
                input.z,
                movement.y,
                fallDistance,
                horizontalSpeedBefore)) {
            spinosaurusGroundDropGraceTicks = SpinosaurusGroundRideRules.dropMomentumGraceTicks();
            spinosaurusGroundDropEntrySpeed = Math.max(horizontalSpeedBefore, movement.horizontalDistance());
        }
        if (onGround() || fallDistance > 1.35F || movement.y > 0.08D) {
            spinosaurusGroundDropGraceTicks = 0;
            spinosaurusGroundDropEntrySpeed = 0.0D;
            return;
        }
        if (spinosaurusGroundDropGraceTicks <= 0 || input.z <= 0.05D) return;

        double minimumSpeed = SpinosaurusGroundRideRules.preservedHorizontalSpeed(
                spinosaurusGroundDropEntrySpeed, spinosaurusGroundDropGraceTicks);
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.horizontalDistanceSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, getYRot());
        }
        if (horizontal.horizontalDistance() < minimumSpeed) {
            horizontal = horizontal.normalize().scale(minimumSpeed);
            setDeltaMovement(horizontal.x, movement.y, horizontal.z);
        }
        spinosaurusGroundDropGraceTicks--;
    }

    private final class SpinosaurusSurfaceFloatGoal extends Goal {
        private int waterlineGraceTicks;

        private SpinosaurusSurfaceFloatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
            navigation.setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return getControllingPassenger() == null && isInWater() && !hasDirectedMovementIntent();
        }

        @Override
        public boolean canContinueToUse() {
            return getControllingPassenger() == null
                    && !hasDirectedMovementIntent()
                    && (isInWater() || waterlineGraceTicks > 0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 movement = getDeltaMovement();
            navigation.stop();
            if (!isInWater()) {
                waterlineGraceTicks--;
                setDeltaMovement(movement.x, Math.min(-0.075D, movement.y), movement.z);
                entityData.set(SPINO_SWIMMING, false);
                entityData.set(SPINO_SWIM_SPEED, 0.0F);
                return;
            }
            waterlineGraceTicks = 12;
            double currentDepth = getFluidHeight(FluidTags.WATER);
            double targetDepth = Mth.clamp(getBbHeight() * 0.62D, 2.45D, 3.30D);
            double targetVertical = Mth.clamp((currentDepth - targetDepth) * 0.050D, -0.085D, 0.070D);
            double vertical = Mth.lerp(0.14D, movement.y, targetVertical);
            vertical = Mth.clamp(vertical, -0.09D, 0.075D);
            setDeltaMovement(movement.x * 0.90D, vertical, movement.z * 0.90D);
            setXRot(Mth.approachDegrees(getXRot(), 0.0F, 2.4F));
            entityData.set(SPINO_SWIMMING, true);
            entityData.set(SPINO_SWIM_SPEED, (float)getDeltaMovement().length());
        }

        @Override
        public void stop() {
            if (getControllingPassenger() == null) {
                entityData.set(SPINO_SWIMMING, false);
                entityData.set(SPINO_SWIM_SPEED, 0.0F);
                setXRot(Mth.approachDegrees(getXRot(), 0.0F, 4.0F));
            }
        }
    }

    private boolean hasDirectedMovementIntent() {
        if (getTarget() != null || onExpedition || isVehicle()) return true;
        if (commandMode == DinosaurCommandMode.FOLLOW) {
            ServerPlayer owner = commandOwner();
            return fieldDutyOwnsNavigation()
                    || ownerCatchupActive
                    || !navigation.isDone()
                    || getWorkAction() != 0
                    || fieldDutyAllowsOwnerPursuit() && owner != null && owner.level() == level()
                    && distanceToSqr(owner) > followerStartDistanceSqr();
        }
        if (commandMode == DinosaurCommandMode.STAY) {
            return stayPosition != null && distanceToSqr(stayPosition.getCenter()) > 1.0D;
        }
        return commandTablePos != null && (getWorkAction() != 0
                || hasActiveSpinosaurusWaterWorkIntent()
                || !navigation.isDone()
                || distanceToSqr(commandTablePos.getCenter()) > baseRadius() * baseRadius());
    }

    private boolean hasActiveSpinosaurusWaterWorkIntent() {
        return getSpecies() == DinosaurSpecies.SPINOSAURUS
                && commandMode == DinosaurCommandMode.HOME
                && workEnabled
                && workJobIndex == 2
                && scheduleAllowsWork()
                && workWorkstationPositions.stream().anyMatch(this::isWaterTurbineTarget);
    }

    private void resetPteranodonGroundState() {
        setNoGravity(false);
        entityData.set(PTERO_FLIGHT_MODE, PTERO_FLIGHT_GROUNDED);
        entityData.set(PTERO_AIRSPEED, 0.0F);
        entityData.set(PTERO_BANK, Mth.approach(getPteranodonBankDegrees(), 0.0F, 4.5F));
        pteranodonThrottle = 0.0F;
        pteranodonForwardHoldTicks = 0;
        pteranodonTakeoffChargeTicks = 0;
        pteranodonTakeoffGraceTicks = 0;
    }

    private int bestSpecialtyStars() {
        int best = 0;
        for (int specialty = 0; specialty < 5; specialty++) {
            best = Math.max(best, getSpecialtyStars(specialty));
        }
        return best;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<FieldDodoEntity> movementController = new AnimationController<FieldDodoEntity>("Movement", 6, test -> {
            FieldDodoEntity dinosaur = test.animatable();
            if (dinosaur.isDefeatTransferActive()) {
                test.setControllerSpeed(0.0F);
                return PlayState.CONTINUE;
            }
            String species = BuiltInRegistries.ENTITY_TYPE.getKey(dinosaur.getType()).getPath();
            boolean dodo = species.equals("field_dodo");
            boolean tyrannosaurus = species.equals("tyrannosaurus");
            boolean stegosaurus = species.equals("stegosaurus");
            boolean pteranodon = species.equals("pteranodon");
            boolean parasaurolophus = species.equals("parasaurolophus");
            boolean velociraptor = species.equals("velociraptor");
            boolean spinosaurus = species.equals("spinosaurus");
            if (velociraptor) test.controller().setTransitionTicks(10);
            if (pteranodon) {
                test.controller().setTransitionTicks(9);
                if (dinosaur.isDinosaurSleeping()) {
                    return playPteranodonAnimation(test, PTERO_IDLE, 1.0F);
                }
                if (dinosaur.getWorkAction() > 0) {
                    return playPteranodonAnimation(test, PTERO_WORK, 1.0F);
                }
                if (dinosaur.isPteranodonAirborne()) {
                    if (dinosaur.isPteranodonGliding()) {
                        return playPteranodonAnimation(
                                test,
                                PTERO_GLIDE,
                                dinosaur.getPteranodonAnimationSpeed(test.renderState().getPartialTick())
                        );
                    }
                    if (dinosaur.isPteranodonHovering()) {
                        return playPteranodonAnimation(
                                test,
                                PTERO_AIR_IDLE,
                                dinosaur.getPteranodonAnimationSpeed(test.renderState().getPartialTick())
                        );
                    }
                    return playPteranodonAnimation(test,
                            dinosaur.usesPoweredPteranodonAnimation() ? PTERO_FLY : PTERO_AIR_IDLE,
                            dinosaur.getPteranodonAnimationSpeed(test.renderState().getPartialTick()));
                }
                return playPteranodonAnimation(
                        test,
                        dinosaur.usesWalkAnimation() ? PTERO_WALK : PTERO_IDLE,
                        Mth.clamp(0.82F + dinosaur.walkAnimation.speed() * 1.7F, 0.82F, 1.35F)
                );
            }
            if (spinosaurus) {
                test.controller().setTransitionTicks(8);
                if (dinosaur.isDinosaurSleeping()) {
                    return playSpeciesAnimation(test, SPINO_SLEEP, 1.0F);
                }
                if (dinosaur.getWorkAction() > 0) {
                    return playSpeciesAnimation(test, SPINO_WORK, 1.0F);
                }
                if (dinosaur.isSpinosaurusAquaticPose()) {
                    float speed = Mth.clamp(0.62F + dinosaur.getSpinosaurusSwimSpeed() * 0.62F, 0.62F, 1.55F);
                    return playSpeciesAnimation(test, SPINO_SWIM, speed);
                }
                if (dinosaur.usesWalkAnimation()) {
                    float landSpeed = Mth.clamp(0.50F + dinosaur.walkAnimation.speed() * 2.15F,
                            0.58F, dinosaur.isSpinosaurusLandSprinting() ? 2.35F : 1.45F);
                    if (dinosaur.getControllingPassenger() == null) {
                        landSpeed *= DinosaurAnimationEvents.unmountedSpinosaurusGaitSpeed();
                    }
                    return playSpeciesAnimation(test, SPINO_WALK, landSpeed);
                }
                return playSpeciesAnimation(test, SPINO_IDLE, 1.0F);
            }
            if (dinosaur.isDinosaurSleeping()
                    && (dodo || tyrannosaurus || stegosaurus || parasaurolophus || velociraptor)) {
                return test.setAndContinue(dodo ? DODO_SLEEP
                        : tyrannosaurus ? T_REX_SLEEP
                        : stegosaurus ? STEGO_SLEEP
                        : parasaurolophus ? PARASAUR_SLEEP
                        : RAPTOR_SLEEP);
            }
            if (dinosaur.getWorkAction() > 0
                    && (dodo || tyrannosaurus || stegosaurus || parasaurolophus || velociraptor)) {
                return test.setAndContinue(dodo ? DODO_WORK
                        : tyrannosaurus ? T_REX_WORK
                        : stegosaurus ? STEGO_WORK
                        : parasaurolophus ? PARASAUR_WORK
                        : RAPTOR_WORK);
            }
            if (!dinosaur.usesWalkAnimation()) {
                return test.setAndContinue(dodo ? DODO_IDLE
                        : tyrannosaurus ? T_REX_IDLE
                        : stegosaurus ? STEGO_IDLE
                        : parasaurolophus ? PARASAUR_IDLE
                        : velociraptor ? RAPTOR_IDLE
                        : PLACEHOLDER_IDLE);
            }
            if (dinosaur.usesRunAnimation()
                    && (dodo || tyrannosaurus || stegosaurus || parasaurolophus || velociraptor)) {
                if (velociraptor) {
                    return playSpeciesAnimation(test, RAPTOR_RUN,
                            dinosaur.getRaptorAnimationSpeed(test.renderState().getPartialTick()));
                }
                return playSpeciesAnimation(test,
                        dodo ? DODO_RUN
                                : tyrannosaurus ? T_REX_RUN
                                : stegosaurus ? STEGO_RUN
                                : PARASAUR_RUN,
                        dinosaur.followerLocomotionAnimationSpeed(true));
            }
            if (velociraptor) {
                return playSpeciesAnimation(test, RAPTOR_WALK,
                        dinosaur.getRaptorAnimationSpeed(test.renderState().getPartialTick()));
            }
            return playSpeciesAnimation(test,
                    dodo ? DODO_WALK
                            : tyrannosaurus ? T_REX_WALK
                            : stegosaurus ? STEGO_WALK
                            : parasaurolophus ? PARASAUR_WALK
                            : PLACEHOLDER_WALK,
                    dinosaur.followerLocomotionAnimationSpeed(false));
        });
        movementController.setSoundKeyframeHandler(DinosaurAnimationEvents::handleFootstep);
        controllers.add(movementController);
        AnimationController<FieldDodoEntity> actionController = new AnimationController<FieldDodoEntity>("Action", 4, test -> {
            FieldDodoEntity dinosaur = test.animatable();
            if (dinosaur.isDefeatTransferActive()) return PlayState.STOP;
            if (dinosaur.getSpecies() == DinosaurSpecies.TYRANNOSAURUS
                    && dinosaur.entityData.get(ATTACK_ANIMATION_TICKS) > 0) {
                return test.setAndContinue(T_REX_ATTACK);
            }
            if (dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                    && dinosaur.entityData.get(ATTACK_ANIMATION_TICKS) > 0) {
                return test.setAndContinue(SPINO_ATTACK);
            }
            if (dinosaur.getSpecies() == DinosaurSpecies.VELOCIRAPTOR
                    && dinosaur.entityData.get(ATTACK_ANIMATION_TICKS) > 0) {
                return test.setAndContinue(RAPTOR_ATTACK);
            }
            return PlayState.STOP;
        });
        controllers.add(actionController);
    }

    private static PlayState playPteranodonAnimation(
            AnimationTest<FieldDodoEntity> test,
            RawAnimation animation,
            float speed
    ) {
        test.setControllerSpeed(speed);
        return test.setAndContinue(animation);
    }

    private static PlayState playSpeciesAnimation(
            AnimationTest<FieldDodoEntity> test,
            RawAnimation animation,
            float speed
    ) {
        test.setControllerSpeed(speed);
        return test.setAndContinue(animation);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private record ContainerTarget(BlockPos pos, Container container) {
    }

    private record SlotTake(Container container, int slot, ItemStack expected) {
    }

    private record CraftingOrder(ItemStack output, int crafts, List<SlotTake> takes) {
    }

    private final class DinosaurMeleeAttackGoal extends Goal {
        private final double speedModifier;
        private int pathRefreshTicks;
        private int attackCooldownTicks;
        private int closeShoveCooldownTicks;
        private double lastTargetX;
        private double lastTargetY;
        private double lastTargetZ;

        private DinosaurMeleeAttackGoal(double speedModifier) {
            this.speedModifier = speedModifier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && insideBaseBoundary(target.position(), 6.0D);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive()
                    && distanceToSqr(target) <= 64.0D * 64.0D
                    && insideBaseBoundary(target.position(), 8.0D);
        }

        @Override
        public void start() {
            setAggressive(true);
            pathRefreshTicks = 0;
            attackCooldownTicks = 0;
        }

        @Override
        public void stop() {
            navigation.stop();
            setAggressive(false);
            LivingEntity target = getTarget();
            if (target != null && !insideBaseBoundary(target.position(), 8.0D)) setTarget(null);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            attackCooldownTicks = Math.max(0, attackCooldownTicks - 1);
            closeShoveCooldownTicks = Math.max(0, closeShoveCooldownTicks - 1);
            if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR) {
                tickRaptorHunt(target);
            } else if (getSpecies() == DinosaurSpecies.TYRANNOSAURUS
                    || getSpecies() == DinosaurSpecies.SPINOSAURUS) {
                tickLargePredatorHunt(target);
            } else {
                tickConventionalHunt(target);
            }
        }

        private void tickRaptorHunt(LivingEntity target) {
            getLookControl().setLookAt(target, getSpecies().turnDegreesPerTick() * 1.8F, 30.0F);
            if (raptorPounceTicks > 0 || pendingAttackTargetId >= 0) {
                return;
            }
            if (shouldRefreshPath(target)) {
                navigation.moveTo(target, speedModifier);
            }

            double pounceRange = getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + 3.0D;
            if (attackCooldownTicks > 0
                    || horizontalDistanceTo(target) > pounceRange
                    || Math.abs(target.getY() - getY()) > 1.8D
                    || !getSensing().hasLineOfSight(target)) {
                return;
            }

            attackCooldownTicks = adjustedTickDelay(28);
            swing(InteractionHand.MAIN_HAND);
            pendingAttackTargetId = target.getId();
            pendingAttackContactTick = level().getGameTime() + RAPTOR_DAMAGE_DELAY_TICKS;
            entityData.set(ATTACK_ANIMATION_TICKS, RAPTOR_ATTACK_ANIMATION_TICKS);
            raptorPounceContactConfirmed = getBoundingBox().inflate(0.85D, 0.45D, 0.85D)
                    .intersects(target.getBoundingBox());
            launchRaptorPounce(target);
        }

        private void launchRaptorPounce(LivingEntity target) {
            Vec3 toward = target.position().add(0.0D, target.getBbHeight() * 0.30D, 0.0D)
                    .subtract(position().add(0.0D, getBbHeight() * 0.30D, 0.0D))
                    .multiply(1.0D, 0.0D, 1.0D);
            if (toward.horizontalDistanceSqr() < 1.0E-5D) {
                toward = Vec3.directionFromRotation(0.0F, getYRot());
            } else {
                toward = toward.normalize();
            }

            Vec3 current = getDeltaMovement();
            Vec3 currentHorizontal = new Vec3(current.x, 0.0D, current.z);
            Vec3 launchDirection = currentHorizontal.horizontalDistanceSqr() > 0.0064D
                    ? currentHorizontal.normalize().scale(0.68D).add(toward.scale(0.32D)).normalize()
                    : toward;
            double launchSpeed = RaptorMomentumRules.pounceHorizontalSpeed(
                    currentHorizontal.horizontalDistance(), getPassiveStrength());
            navigation.stop();
            getMoveControl().setWait();
            setYRot(Mth.approachDegrees(getYRot(), yawTo(target), 28.0F));
            setDeltaMovement(
                    launchDirection.x * launchSpeed,
                    Math.max(current.y, RaptorMomentumRules.pounceVerticalSpeed(getPassiveStrength())),
                    launchDirection.z * launchSpeed
            );
            raptorPounceTicks = RAPTOR_ATTACK_ANIMATION_TICKS;
        }

        private void tickConventionalHunt(LivingEntity target) {
            getLookControl().setLookAt(target, getSpecies().turnDegreesPerTick() * 1.5F, 24.0F);
            if (shouldRefreshPath(target)) {
                navigation.moveTo(target, speedModifier);
            }
            if (attackCooldownTicks == 0
                    && isWithinMeleeAttackRange(target)
                    && getSensing().hasLineOfSight(target)) {
                attackCooldownTicks = adjustedTickDelay(20);
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(getServerLevel(FieldDodoEntity.this), target);
            }
        }

        private void tickLargePredatorHunt(LivingEntity target) {
            boolean spinosaurus = getSpecies() == DinosaurSpecies.SPINOSAURUS;
            double mouthReach = spinosaurus ? SPINO_MOUTH_REACH : T_REX_MOUTH_REACH;
            double distance = horizontalDistanceTo(target);
            double idealDistance = getBbWidth() * 0.5D
                    + target.getBbWidth() * 0.5D
                    + mouthReach * (spinosaurus ? 0.72D : 0.70D);
            double closeDistance = idealDistance - (spinosaurus ? 0.55D : 0.82D);
            double approachDistance = idealDistance + (spinosaurus ? 0.24D : 0.38D);
            float yawError = yawErrorTo(target);
            getLookControl().setLookAt(target, getSpecies().turnDegreesPerTick() * 1.25F, 20.0F);
            rejectWrongWayMomentum(target);

            if (distance < closeDistance) {
                haltHorizontalMovement(0.42D);
                setYRot(Mth.approachDegrees(getYRot(), yawTo(target), getSpecies().turnDegreesPerTick()));
                if (spinosaurus && closeShoveCooldownTicks == 0 && distance < closeDistance - 0.22D) {
                    pushCrowdingTarget(target);
                    closeShoveCooldownTicks = 18 + random.nextInt(9);
                }
            } else if (distance > approachDistance) {
                if (Math.abs(yawError) > 46.0F) {
                    haltHorizontalMovement(0.38D);
                    setYRot(Mth.approachDegrees(getYRot(), yawTo(target), getSpecies().turnDegreesPerTick()));
                } else if (hasClearPursuitLane(target, distance, approachDistance)) {
                    navigation.stop();
                    Vec3 stop = pursuitStop(target, idealDistance);
                    getMoveControl().setWantedPosition(stop.x, target.getY(), stop.z, speedModifier);
                } else if (shouldRefreshPath(target)) {
                    navigation.moveTo(target, speedModifier);
                }
            } else {
                haltHorizontalMovement(0.36D);
                setYRot(Mth.approachDegrees(getYRot(), yawTo(target), getSpecies().turnDegreesPerTick()));
            }

            boolean readyToCommit = spinosaurus
                    ? distance <= idealDistance + 0.22D
                            && Math.abs(yawError) <= 62.0F
                    : isWithinMeleeAttackRange(target);
            if (attackCooldownTicks == 0
                    && readyToCommit
                    && getSensing().hasLineOfSight(target)
                    && beginLargePredatorAttack(target)) {
                attackCooldownTicks = adjustedTickDelay(spinosaurus ? 25 : 22);
                swing(InteractionHand.MAIN_HAND);
            }
        }

        private void pushCrowdingTarget(LivingEntity target) {
            Vec3 away = target.position().subtract(position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.horizontalDistanceSqr() < 1.0E-5D) {
                away = Vec3.directionFromRotation(0.0F, getYRot());
            } else {
                away = away.normalize();
            }
            target.push(away.x * 0.32D, 0.08D, away.z * 0.32D);
        }

        private Vec3 pursuitStop(LivingEntity target, double idealDistance) {
            Vec3 away = position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.horizontalDistanceSqr() < 1.0E-5D) {
                away = Vec3.directionFromRotation(0.0F, getYRot()).scale(-1.0D);
            } else {
                away = away.normalize();
            }
            return target.position().add(away.scale(idealDistance));
        }

        private void rejectWrongWayMomentum(LivingEntity target) {
            Vec3 toward = target.position().subtract(position());
            toward = new Vec3(toward.x, 0.0D, toward.z);
            Vec3 motion = getDeltaMovement();
            Vec3 horizontalMotion = new Vec3(motion.x, 0.0D, motion.z);
            if (toward.horizontalDistanceSqr() < 1.0E-5D
                    || horizontalMotion.horizontalDistanceSqr() < 0.0004D) return;
            double alignment = horizontalMotion.normalize().dot(toward.normalize());
            if (alignment >= 0.10D) return;
            navigation.stop();
            getMoveControl().setWait();
            setSpeed(0.0F);
            setDeltaMovement(motion.x * 0.10D, motion.y, motion.z * 0.10D);
        }

        private void haltHorizontalMovement(double damping) {
            navigation.stop();
            getMoveControl().setWait();
            setSpeed(0.0F);
            setXxa(0.0F);
            setZza(0.0F);
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * damping, motion.y, motion.z * damping);
        }

        private boolean hasClearPursuitLane(LivingEntity target, double distance, double stoppingDistance) {
            double advance = distance - stoppingDistance;
            if (advance <= 0.0D || Math.abs(target.getY() - getY()) > maxUpStep()) {
                return false;
            }
            Vec3 direction = target.position().subtract(position());
            direction = new Vec3(direction.x, 0.0D, direction.z);
            if (direction.horizontalDistanceSqr() < 1.0E-4D) {
                return false;
            }
            direction = direction.normalize().scale(advance);
            AABB pursuitLane = getBoundingBox()
                    .expandTowards(direction.x, 0.0D, direction.z)
                    .inflate(0.08D, 0.0D, 0.08D);
            return level().noCollision(FieldDodoEntity.this, pursuitLane);
        }

        private boolean shouldRefreshPath(LivingEntity target) {
            pathRefreshTicks = Math.max(0, pathRefreshTicks - 1);
            boolean targetMoved = target.distanceToSqr(lastTargetX, lastTargetY, lastTargetZ) >= 0.64D;
            if (pathRefreshTicks > 0 && !targetMoved && !navigation.isDone()) {
                return false;
            }
            lastTargetX = target.getX();
            lastTargetY = target.getY();
            lastTargetZ = target.getZ();
            pathRefreshTicks = adjustedTickDelay(6 + random.nextInt(5));
            return true;
        }
    }

    private final class DinosaurLookAtPlayerGoal extends LookAtPlayerGoal {
        private DinosaurLookAtPlayerGoal() {
            super(FieldDodoEntity.this, Player.class, 3.0F, 0.12F);
        }

        @Override
        public boolean canUse() {
            return getWorkAction() == 0
                    && getTarget() == null
                    && !isDinosaurSleeping()
                    && navigation.isDone()
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getWorkAction() == 0
                    && getTarget() == null
                    && !isDinosaurSleeping()
                    && navigation.isDone()
                    && super.canContinueToUse();
        }
    }

    private final class DinosaurAmbientLookGoal extends Goal {
        private Entity observedEntity;
        private double lookX;
        private double lookY;
        private double lookZ;
        private int lookTicks;

        private DinosaurAmbientLookGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getWorkAction() != 0
                    || getTarget() != null
                    || isDinosaurSleeping()
                    || !navigation.isDone()
                    || random.nextInt(28) != 0) {
                return false;
            }

            List<Entity> nearbyPointsOfInterest = level().getEntities(
                    FieldDodoEntity.this,
                    getBoundingBox().inflate(7.0D, 4.0D, 7.0D),
                    entity -> entity.isAlive()
                            && !(entity instanceof Player)
                            && !(entity instanceof Monster)
                            && (entity instanceof LivingEntity || entity instanceof ItemEntity)
            );
            if (!nearbyPointsOfInterest.isEmpty() && random.nextFloat() < 0.65F) {
                observedEntity = nearbyPointsOfInterest.get(random.nextInt(nearbyPointsOfInterest.size()));
            } else {
                observedEntity = null;
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 3.0D + random.nextDouble() * 5.0D;
                lookX = getX() + Math.cos(angle) * distance;
                lookY = getEyeY() - 0.5D + random.nextDouble() * 2.25D;
                lookZ = getZ() + Math.sin(angle) * distance;
            }
            return true;
        }

        @Override
        public void start() {
            lookTicks = 42 + random.nextInt(62);
        }

        @Override
        public boolean canContinueToUse() {
            return lookTicks > 0
                    && getWorkAction() == 0
                    && getTarget() == null
                    && !isDinosaurSleeping()
                    && navigation.isDone()
                    && (observedEntity == null || observedEntity.isAlive());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            lookTicks--;
            if (observedEntity != null) {
                lookX = observedEntity.getX();
                lookY = observedEntity.getEyeY();
                lookZ = observedEntity.getZ();
            }
            float yawSpeed = Mth.clamp(getSpecies().turnDegreesPerTick() * 0.65F, 1.8F, 4.0F);
            getLookControl().setLookAt(lookX, lookY, lookZ, yawSpeed, 3.5F);
        }

        @Override
        public void stop() {
            observedEntity = null;
        }
    }

    private final class SizeAwareMoveControl extends MoveControl {
        private SizeAwareMoveControl(FieldDodoEntity dinosaur) {
            super(dinosaur);
        }

        @Override
        protected float rotlerp(float current, float target, float maximumChange) {
            float turnRate = getSpecies().turnDegreesPerTick();
            if (ownerCatchupActive) {
                turnRate *= getSpecies() == DinosaurSpecies.SPINOSAURUS
                        || getSpecies() == DinosaurSpecies.TYRANNOSAURUS ? 2.0F : 1.55F;
            }
            float headError = Mth.wrapDegrees(target - yHeadRot);
            turnRate *= DinosaurFollowRules.headLeadScale(headError);
            return super.rotlerp(current, target, Math.min(maximumChange, turnRate));
        }

        @Override
        public void tick() {
            boolean movingTo = operation == Operation.MOVE_TO;
            double authoredSpeedModifier = speedModifier;
            if (movingTo && getSpecies() == DinosaurSpecies.VELOCIRAPTOR
                    && RaptorMomentumRules.pursuitActive(
                            isRaptorTransportPursuitActive(),
                            getTarget() != null && getTarget().isAlive(),
                            raptorPounceTicks > 0)) {
                speedModifier *= RaptorMomentumRules.movementMultiplier(
                        raptorMomentum, getPassiveStrength());
            }
            double targetX = wantedX;
            double targetY = wantedY;
            double targetZ = wantedZ;
            float desiredYaw = movingTo
                    ? (float)(Mth.atan2(targetZ - getZ(), targetX - getX()) * Mth.RAD_TO_DEG) - 90.0F
                    : getYRot();
            if (movingTo) {
                getLookControl().setLookAt(
                        targetX,
                        targetY + getEyeHeight() * 0.55D,
                        targetZ,
                        getSpecies().turnDegreesPerTick() * 1.75F,
                        18.0F
                );
            }
            super.tick();
            speedModifier = authoredSpeedModifier;
            if (!movingTo) {
                return;
            }
            float yawError = Math.abs(Mth.wrapDegrees(desiredYaw - getYRot()));
            float visibleBodyError = Math.abs(Mth.wrapDegrees(desiredYaw - yBodyRot));
            if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR) {
                float turnScale = RaptorMomentumRules.turnSpeedMultiplier(yawError, visibleBodyError);
                setSpeed(getSpeed() * turnScale);
                zza *= turnScale;
                Vec3 movement = getDeltaMovement();
                double velocityScale = Mth.lerp(0.36D, 1.0D, turnScale);
                double horizontalSpeed = movement.horizontalDistance() * velocityScale;
                double desiredX = targetX - getX();
                double desiredZ = targetZ - getZ();
                double desiredLength = Math.sqrt(desiredX * desiredX + desiredZ * desiredZ);
                if (horizontalSpeed > 1.0E-5D && desiredLength > 1.0E-5D) {
                    double currentX = movement.x / Math.max(1.0E-5D, movement.horizontalDistance());
                    double currentZ = movement.z / Math.max(1.0E-5D, movement.horizontalDistance());
                    desiredX /= desiredLength;
                    desiredZ /= desiredLength;
                    float response = RaptorMomentumRules.steeringResponse(raptorMomentum, yawError);
                    double steeredX = Mth.lerp(response, currentX, desiredX);
                    double steeredZ = Mth.lerp(response, currentZ, desiredZ);
                    double steeredLength = Math.sqrt(steeredX * steeredX + steeredZ * steeredZ);
                    if (steeredLength > 1.0E-5D) {
                        setDeltaMovement(
                                steeredX / steeredLength * horizontalSpeed,
                                movement.y,
                                steeredZ / steeredLength * horizontalSpeed
                        );
                    }
                } else {
                    setDeltaMovement(movement.x * velocityScale, movement.y, movement.z * velocityScale);
                }
            } else {
                float turnScale = DinosaurFollowRules.movementTurnScale(yawError, visibleBodyError);
                setSpeed(getSpeed() * turnScale);
                zza *= turnScale;
                Vec3 movement = getDeltaMovement();
                double velocityScale = Mth.lerp(0.52D, 1.0D, turnScale);
                setDeltaMovement(movement.x * velocityScale, movement.y, movement.z * velocityScale);
            }

            if (getSpecies() == DinosaurSpecies.VELOCIRAPTOR) {
                return;
            }

            Vec3 movement = getDeltaMovement();
            double horizontalSpeed = movement.horizontalDistance();
            if (horizontalSpeed > 0.035D) {
                double yaw = getYRot() * Mth.DEG_TO_RAD;
                double facingX = -Math.sin(yaw);
                double facingZ = Math.cos(yaw);
                double alignment = (facingX * movement.x + facingZ * movement.z) / horizontalSpeed;
                if (alignment < -0.15D) {
                    double correctedX = Mth.lerp(0.22D, movement.x, facingX * horizontalSpeed);
                    double correctedZ = Mth.lerp(0.22D, movement.z, facingZ * horizontalSpeed);
                    setDeltaMovement(correctedX, movement.y, correctedZ);
                }
            }
        }
    }

    private final class WeightedBodyRotationControl extends BodyRotationControl {
        private WeightedBodyRotationControl(FieldDodoEntity dinosaur) {
            super(dinosaur);
        }

        @Override
        public void clientTick() {
            double x = getX() - xo;
            double z = getZ() - zo;
            boolean moving = x * x + z * z > 2.5000003E-7D;
            float bodyTurnRate = getSpecies().turnDegreesPerTick() * (moving ? 0.82F : 0.58F);
            float headDifference = Mth.wrapDegrees(yHeadRot - yBodyRot);
            float idleTurnThreshold = switch (getSpecies()) {
                case STEGOSAURUS -> 46.0F;
                case TYRANNOSAURUS, BRACHIOSAURUS, SPINOSAURUS -> 32.0F;
                default -> 36.0F;
            };
            if (moving) {
                yBodyRot = Mth.approachDegrees(yBodyRot, getYRot(), bodyTurnRate);
            } else if (Math.abs(headDifference) > idleTurnThreshold) {
                yBodyRot = Mth.approachDegrees(yBodyRot, yHeadRot, bodyTurnRate);
            }
            float limitedHead = yBodyRot + Mth.clamp(Mth.wrapDegrees(yHeadRot - yBodyRot), -getMaxHeadYRot(), getMaxHeadYRot());
            yHeadRot = Mth.approachDegrees(yHeadRot, limitedHead, getSpecies().turnDegreesPerTick() * 1.4F);
        }
    }

    private final class ReturnToBaseGoal extends Goal {
        private int pathRefreshTicks;
        private int stalledTicks;
        private double lastDistance;
        private Vec3 lastSamplePosition;

        private ReturnToBaseGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return commandMode == DinosaurCommandMode.HOME
                    && commandTablePos != null
                    && !isVehicle()
                    && !onExpedition
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && getWorkAction() == 0
                    && !insideBaseBoundary(position(), 3.0D);
        }

        @Override
        public boolean canContinueToUse() {
            return commandMode == DinosaurCommandMode.HOME
                    && commandTablePos != null
                    && !isVehicle()
                    && !onExpedition
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && !insideBaseBoundary(position(), -8.0D);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            pathRefreshTicks = 0;
            stalledTicks = 0;
            lastDistance = commandTablePos == null ? Double.MAX_VALUE
                    : position().distanceTo(commandTablePos.getCenter());
            lastSamplePosition = position();
            routeHome();
        }

        @Override
        public void tick() {
            if (commandTablePos == null) return;
            double distanceSquared = distanceToSqr(commandTablePos.getCenter());
            if (tickCount % 20 == 0) {
                double distance = Math.sqrt(distanceSquared);
                double displacementSquared = lastSamplePosition == null
                        ? 0.0D : position().distanceToSqr(lastSamplePosition);
                if (!DinosaurFollowRules.madeMeaningfulProgress(
                        lastDistance, distance, displacementSquared, navigation.isStuck())) {
                    stalledTicks = Math.min(260, stalledTicks + 20);
                } else {
                    stalledTicks = Math.max(0, stalledTicks - 20);
                }
                lastDistance = distance;
                lastSamplePosition = position();
            }
            if (DinosaurFollowRules.shouldTeleportAfterStall(stalledTicks, distanceSquared)
                    && trySafeNavigationTeleport(commandTablePos)) {
                stalledTicks = 0;
                pathRefreshTicks = 20;
                return;
            }
            if (--pathRefreshTicks <= 0 || navigation.isDone() || navigation.isStuck()) routeHome();
        }

        private void routeHome() {
            if (commandTablePos == null) return;
            Vec3 center = commandTablePos.getCenter();
            Vec3 outward = position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            if (outward.horizontalDistanceSqr() < 1.0E-4D) outward = new Vec3(0.0D, 0.0D, 1.0D);
            Vec3 safeRing = center.add(outward.normalize().scale(4.0D));
            if (!navigation.moveTo(safeRing.x, commandTablePos.getY() + 1.0D, safeRing.z, 1.02D)) {
                stalledTicks = Math.min(260, stalledTicks + 10);
            }
            pathRefreshTicks = adjustedTickDelay(20);
        }

        @Override
        public void stop() {
            stalledTicks = 0;
            lastSamplePosition = null;
        }
    }

    private final class BaseRoamGoal extends Goal {
        private Vec3 target;

        private BaseRoamGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (commandMode != DinosaurCommandMode.HOME
                    || onExpedition || isDinosaurSleeping() || getHunger() < PrimevalTuning.server().foodBoxThreshold()
                    || getTarget() != null || getWorkAction() != 0
                    || commandTablePos != null && workEnabled && workJobIndex == 2
                    || commandTablePos != null && workEnabled && workerCooldown <= 10
                    || random.nextInt(85) != 0) {
                return false;
            }
            for (int attempt = 0; attempt < 5; attempt++) {
                double roamRadius = Math.min(18.0D, Math.max(9.0D, baseRadius() * 0.38D));
                double returnRadius = roamRadius + 6.0D;
                Vec3 candidate = commandTablePos != null && distanceToSqr(commandTablePos.getCenter()) > returnRadius * returnRadius
                        ? DefaultRandomPos.getPosTowards(FieldDodoEntity.this, 12, 6, commandTablePos.getCenter(), Math.PI / 2.0D)
                        : DefaultRandomPos.getPos(FieldDodoEntity.this, 14, 6);
                if (candidate != null && (commandTablePos == null || candidate.distanceToSqr(commandTablePos.getCenter()) <= roamRadius * roamRadius)) {
                    target = candidate;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            navigation.moveTo(target.x, target.y, target.z, 0.82D);
        }

        @Override
        public boolean canContinueToUse() {
            return commandMode == DinosaurCommandMode.HOME
                    && !navigation.isDone()
                    && !onExpedition
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && getWorkAction() == 0
                    && insideBaseBoundary(position(), 1.0D);
        }
    }

    private final class FollowCommandOwnerGoal extends Goal {
        private ServerPlayer owner;
        private int refreshTicks;
        private int stalledTicks;
        private double lastDistance;
        private Vec3 lastSamplePosition;
        private BlockPos lastPathOwnerPosition;
        private int recoveryCooldown;

        private FollowCommandOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            owner = commandOwner();
            return commandMode == DinosaurCommandMode.FOLLOW
                    && fieldDutyAllowsOwnerPursuit()
                    && owner != null
                    && owner.level() == level()
                    && !onExpedition
                    && !isVehicle()
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && distanceToSqr(owner) > followStartDistanceSqr();
        }

        @Override
        public boolean canContinueToUse() {
            return commandMode == DinosaurCommandMode.FOLLOW
                    && fieldDutyAllowsOwnerPursuit()
                    && owner != null
                    && owner.isAlive()
                    && owner.level() == level()
                    && !onExpedition
                    && !isVehicle()
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && distanceToSqr(owner) > followStopDistanceSqr();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ownerCatchupActive = true;
            refreshTicks = 0;
            stalledTicks = 0;
            lastDistance = owner == null ? Double.MAX_VALUE : distanceTo(owner);
            lastSamplePosition = position();
            lastPathOwnerPosition = null;
            recoveryCooldown = 0;
        }

        @Override
        public void tick() {
            if (owner == null) return;
            getLookControl().setLookAt(owner, getSpecies().turnDegreesPerTick(), 28.0F);
            double distanceSquared = distanceToSqr(owner);
            double distance = Math.sqrt(distanceSquared);
            setSprinting(DinosaurFollowRules.shouldRun(distanceSquared)
                    && getSpecies() != DinosaurSpecies.PTERANODON
                    && !(getSpecies() == DinosaurSpecies.SPINOSAURUS && isInWater()));
            if (recoveryCooldown > 0) recoveryCooldown--;
            if (tickCount % 20 == 0) {
                double displacementSquared = lastSamplePosition == null
                        ? 0.0D : position().distanceToSqr(lastSamplePosition);
                if (!DinosaurFollowRules.madeMeaningfulProgress(
                        lastDistance, distance, displacementSquared, navigation.isStuck())) {
                    stalledTicks = Math.min(260, stalledTicks + 20);
                }
                else stalledTicks = Math.max(0, stalledTicks - 30);
                lastDistance = distance;
                lastSamplePosition = position();
            }
            if (DinosaurFollowRules.shouldTeleportAfterStall(stalledTicks, distanceSquared)
                    && trySafeFollowerTeleport(owner)) {
                stalledTicks = 0;
                recoveryCooldown = 20;
                return;
            }
            if (getSpecies() == DinosaurSpecies.PTERANODON
                    && (autonomousTransportFlight || distanceSquared > 10.0D * 10.0D
                    || Math.abs(owner.getY() - getY()) > 2.5D)) {
                tickAutonomousTransportFlight(owner.blockPosition());
                return;
            }
            if (getSpecies() == DinosaurSpecies.PTERANODON && autonomousTransportFlight) {
                stopAutonomousTransportFlight();
            }
            if (getSpecies() == DinosaurSpecies.SPINOSAURUS && isInWater()) {
                navigation.stop();
                getMoveControl().setWait();
                Vec3 offset = owner.position()
                        .add(0.0D, owner.getBbHeight() * 0.25D, 0.0D)
                        .subtract(position());
                if (offset.lengthSqr() > 0.01D) {
                    double swimSpeed = Mth.clamp(distance * 0.035D, 0.24D, 0.48D)
                            * getMutationStatMultiplier();
                    Vec3 desired = offset.normalize().scale(swimSpeed);
                    desired = new Vec3(desired.x, Mth.clamp(desired.y, -0.22D, 0.22D), desired.z);
                    setDeltaMovement(getDeltaMovement().lerp(desired, 0.24D));
                    setYRot(Mth.approachDegrees(getYRot(), yawTo(owner),
                            getSpecies().turnDegreesPerTick() * 1.6F));
                }
                refreshTicks = 2;
                return;
            }
            if (isInWater() && getSpecies() != DinosaurSpecies.PTERANODON) {
                tickTerrestrialWaterCatchup(owner, distance);
                refreshTicks = 2;
                return;
            }
            if (DinosaurFollowRules.shouldTryLocalRecovery(stalledTicks) && recoveryCooldown <= 0) {
                if (tryDirectGroundCatchup()) {
                    stalledTicks = Math.max(20, stalledTicks - 20);
                    refreshTicks = adjustedTickDelay(5);
                    recoveryCooldown = 18;
                    return;
                }
                navigation.stop();
                boolean started = navigation.moveTo(owner, followerMovementSpeed(distanceSquared));
                if (!started) {
                    navigation.recomputePath();
                }
                stalledTicks = Math.max(30, stalledTicks - 10);
                refreshTicks = adjustedTickDelay(8);
                recoveryCooldown = 18;
                lastPathOwnerPosition = owner.blockPosition();
                return;
            }
            if (--refreshTicks <= 0) {
                refreshTicks = adjustedTickDelay(distanceSquared > 256.0D ? 4 : 8);
                BlockPos ownerPosition = owner.blockPosition();
                boolean ownerMoved = lastPathOwnerPosition == null
                        || lastPathOwnerPosition.distSqr(ownerPosition) >= 6.25D;
                if (ownerMoved || navigation.isDone() || navigation.isStuck()) {
                    boolean started = navigation.moveTo(owner, followerMovementSpeed(distanceSquared));
                    lastPathOwnerPosition = ownerPosition;
                    if (!started) {
                        stalledTicks = Math.min(260, stalledTicks + 12);
                        tryDirectGroundCatchup();
                    }
                }
            }
        }

        private boolean tryDirectGroundCatchup() {
            if (owner == null || isInWater()
                    || Math.abs(owner.getY() - getY()) > maxUpStep() + 1.25D) return false;
            Vec3 offset = owner.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            double distance = offset.horizontalDistance();
            if (distance < 0.01D) return false;
            Vec3 advance = offset.normalize().scale(Math.min(8.0D,
                    Math.max(0.0D, distance - Math.sqrt(followerStopDistanceSqr()))));
            if (advance.horizontalDistanceSqr() < 0.04D) return false;
            AABB lane = getBoundingBox().expandTowards(advance.x, 0.0D, advance.z).inflate(0.05D, 0.0D, 0.05D);
            if (!level().noCollision(FieldDodoEntity.this, lane)) return false;
            getMoveControl().setWantedPosition(
                    owner.getX(), owner.getY(), owner.getZ(), followerMovementSpeed(distanceToSqr(owner)));
            return true;
        }

        private void tickTerrestrialWaterCatchup(ServerPlayer owner, double distance) {
            navigation.stop();
            getMoveControl().setWait();
            setSprinting(false);
            Vec3 horizontal = owner.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (horizontal.horizontalDistanceSqr() < 1.0E-5D) return;
            double speed = Mth.clamp(distance * 0.025D, 0.18D, 0.31D)
                    * getMutationStatMultiplier();
            Vec3 direction = horizontal.normalize();
            double verticalTarget = owner.isInWater()
                    ? Mth.clamp((owner.getY() - getY()) * 0.08D, -0.08D, 0.13D)
                    : 0.13D;
            Vec3 desired = new Vec3(direction.x * speed, verticalTarget, direction.z * speed);
            setDeltaMovement(getDeltaMovement().lerp(desired, 0.24D));
            float desiredYaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.approachDegrees(getYRot(), desiredYaw,
                    getSpecies().turnDegreesPerTick() * 1.6F));
            if (horizontalCollision) getJumpControl().jump();
        }

        @Override
        public void stop() {
            if (getSpecies() == DinosaurSpecies.PTERANODON
                    && autonomousTransportFlight && !isTransportWorkActive()) {
                stopAutonomousTransportFlight();
            }
            owner = null;
            ownerCatchupActive = false;
            if (getTarget() == null) setSprinting(false);
            navigation.stop();
            getMoveControl().setWait();
            stalledTicks = 0;
            lastSamplePosition = null;
            lastPathOwnerPosition = null;
            recoveryCooldown = 0;
        }

        private double followerMovementSpeed(double distanceSquared) {
            double distance = Math.sqrt(distanceSquared);
            double catchup = Mth.clamp((distance - 5.0D) / 18.0D, 0.0D, 1.0D);
            double base = getSpecies() == DinosaurSpecies.VELOCIRAPTOR ? 1.20D : 1.12D;
            double maximum = getSpecies() == DinosaurSpecies.VELOCIRAPTOR ? 1.72D
                    : getSpecies() == DinosaurSpecies.SPINOSAURUS ? 1.58D : 1.55D;
            return Mth.lerp(catchup, base, maximum);
        }

        private double followStartDistanceSqr() {
            return FieldDodoEntity.this.followerStartDistanceSqr();
        }

        private double followStopDistanceSqr() {
            return FieldDodoEntity.this.followerStopDistanceSqr();
        }
    }

    private double followerStartDistanceSqr() {
        double distance = Math.max(3.35D, getBbWidth() * 0.9D + 2.0D);
        return distance * distance;
    }

    private double followerStopDistanceSqr() {
        double distance = Math.max(2.35D, getBbWidth() * 0.62D + 1.25D);
        return distance * distance;
    }

    private boolean fieldDutyAllowsOwnerPursuit() {
        return !fieldDutyOwnsNavigation();
    }

    private boolean fieldDutyOwnsNavigation() {
        if (commandMode != DinosaurCommandMode.FOLLOW || !fieldWorkEnabled
                || onExpedition || isVehicle() || isDinosaurSleeping()
                || level().getGameTime() < incapacitatedUntilTick
                || getTarget() != null || getHunger() <= 10
                || !DinoFieldWorkRules.supports(getSpecies(), fieldWorkMode)
                || DinoFieldWorkRules.rating(this, fieldWorkMode) <= 0) {
            return false;
        }
        ServerPlayer owner = commandOwner();
        return owner != null && owner.isAlive() && owner.level() == level();
    }

    private void keepInsideFieldLeash() {
        if (fieldWorkFirst == null) return;
        double leash = Math.max(DinoWhistleSettings.MIN_RANGE, fieldWorkRange);
        if (position().distanceToSqr(fieldWorkFirst.getCenter()) <= leash * leash) return;
        moveTo(fieldWorkFirst);
    }

    private final class StayCommandGoal extends Goal {
        private StayCommandGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return commandMode == DinosaurCommandMode.STAY
                    && stayPosition != null
                    && !onExpedition
                    && !isVehicle()
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && getHunger() >= PrimevalTuning.server().foodBoxThreshold()
                    && distanceToSqr(stayPosition.getCenter()) > Math.max(1.0D, getBbWidth() * getBbWidth());
        }

        @Override
        public boolean canContinueToUse() {
            return commandMode == DinosaurCommandMode.STAY
                    && stayPosition != null
                    && !onExpedition
                    && !isVehicle()
                    && !isDinosaurSleeping()
                    && getTarget() == null
                    && !navigation.isDone()
                    && distanceToSqr(stayPosition.getCenter()) > 0.8D;
        }

        @Override
        public void start() {
            if (stayPosition != null) navigation.moveTo(stayPosition.getX() + 0.5D,
                    stayPosition.getY(), stayPosition.getZ() + 0.5D, 0.86D);
        }
    }

    private boolean trySafeFollowerTeleport(ServerPlayer owner) {
        boolean allowWater = owner.isInWater() || getSpecies() == DinosaurSpecies.SPINOSAURUS;
        boolean teleported = trySafeTeleportNear(owner.blockPosition(), allowWater);
        if (teleported) {
            setYRot(owner.getYRot());
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        return teleported;
    }

    private boolean trySafeNavigationTeleport(BlockPos target) {
        boolean allowWater = getSpecies() == DinosaurSpecies.SPINOSAURUS
                || level().getFluidState(target).is(FluidTags.WATER);
        return trySafeTeleportNear(target, allowWater);
    }

    private boolean trySafeTeleportNear(BlockPos origin, boolean allowWater) {
        int[][] offsets = {
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {3, 1}, {-3, 1}, {1, 3}, {1, -3},
                {4, 0}, {-4, 0}, {0, 4}, {0, -4},
                {4, 2}, {-4, 2}, {2, 4}, {2, -4},
                {5, 0}, {-5, 0}, {0, 5}, {0, -5}
        };
        for (int[] offset : offsets) {
            int x = origin.getX() + offset[0];
            int z = origin.getZ() + offset[1];
            for (int yOffset = 3; yOffset >= -10; yOffset--) {
                BlockPos destination = new BlockPos(x, origin.getY() + yOffset, z);
                if (isSafeTeleportDestination(destination, allowWater)) {
                    stopAutonomousTransportFlight();
                    navigation.stop();
                    getMoveControl().setWait();
                    teleportTo(x + 0.5D, destination.getY(), z + 0.5D);
                    setDeltaMovement(Vec3.ZERO);
                    fallDistance = 0.0F;
                    navigationTarget = null;
                    lastNavigationSamplePosition = position();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSafeTeleportDestination(BlockPos destination, boolean allowWater) {
        if (!level().isLoaded(destination) || !level().getWorldBorder().isWithinBounds(destination)) return false;
        if (level().getFluidState(destination).is(FluidTags.LAVA)
                || level().getFluidState(destination.above()).is(FluidTags.LAVA)) return false;
        Vec3 feet = new Vec3(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
        AABB moved = getDimensions(getPose()).makeBoundingBox(feet);
        if (!level().noCollision(this, moved)) return false;
        if (allowWater && level().getFluidState(destination).is(FluidTags.WATER)) return true;
        BlockPos support = destination.below();
        BlockState supportState = level().getBlockState(support);
        return supportState.isFaceSturdy(level(), support, Direction.UP)
                && !supportState.is(BlockTags.FIRE)
                && !supportState.is(Blocks.CACTUS)
                && !supportState.is(Blocks.MAGMA_BLOCK)
                && !supportState.is(Blocks.POWDER_SNOW);
    }
}
