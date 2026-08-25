package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.damage.PrimevalDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public final class MagicTurretBlockEntity extends BlockEntity implements TargetingTurret {
    static final int BURST_HITS = 4;
    static final int BURST_STEP_TICKS = 5;
    static final float DAMAGE_PER_HIT = 5.0F;
    private static final int RECOVERY_TICKS = 20;
    private static final double RANGE = 24.0D;
    private static final double PIVOT_HEIGHT = 0.75D;
    private static final DustParticleOptions SPELL_GLOW = new DustParticleOptions(0xA95CFF, 0.9F);
    private static final DustParticleOptions SPELL_CORE = new DustParticleOptions(0xFFF7FF, 0.46F);

    private final TurretAimController aim = new TurretAimController();
    private int cooldown;
    private int targetRefresh;
    private int burstHitsRemaining;
    private int burstStep;
    private @Nullable UUID burstTarget;

    public MagicTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_TURRET.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MagicTurretBlockEntity turret) {
        if (!BaseEnergyRules.isConnected(level, pos)) {
            turret.setTarget(null);
            turret.cancelBurst();
            return;
        }

        LivingEntity target = turret.currentTarget(level, pos);
        if (target == null || turret.targetRefresh-- <= 0) {
            target = findNearestThreat(level, pos);
            turret.setTarget(target);
            turret.targetRefresh = 5;
        }
        if (target == null) {
            turret.cancelBurst();
            return;
        }
        if (turret.burstHitsRemaining > 0 && !target.getUUID().equals(turret.burstTarget)) {
            turret.cancelBurst();
        }
        if (!BaseEnergyRules.isPowered(level, pos)) return;
        if (turret.burstHitsRemaining > 0) {
            if (turret.burstStep > 0) {
                turret.burstStep--;
                return;
            }
            turret.strike(level, pos, target);
            return;
        }
        if (turret.cooldown > 0) {
            turret.cooldown--;
            return;
        }

        turret.burstHitsRemaining = BURST_HITS;
        turret.burstTarget = target.getUUID();
        turret.strike(level, pos, target);
    }

    private void strike(ServerLevel level, BlockPos pos, LivingEntity target) {
        Vec3 pivot = Vec3.atCenterOf(pos).add(0.0D, PIVOT_HEIGHT - 0.5D, 0.0D);
        target.hurtServer(level, PrimevalDamageTypes.magicTurret(level, pivot), DAMAGE_PER_HIT);
        releaseSpellBurst(level, pos, target);
        burstHitsRemaining--;
        if (burstHitsRemaining > 0 && target.isAlive()) {
            burstStep = BURST_STEP_TICKS - 1;
        } else {
            burstHitsRemaining = 0;
            burstStep = 0;
            burstTarget = null;
            cooldown = RECOVERY_TICKS;
        }
        setChanged();
    }

    private void cancelBurst() {
        burstHitsRemaining = 0;
        burstStep = 0;
        burstTarget = null;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MagicTurretBlockEntity turret) {
        turret.aim.clientTick(level, pos, PIVOT_HEIGHT);
    }

    private void releaseSpellBurst(ServerLevel level, BlockPos pos, LivingEntity target) {
        Vec3 pivot = Vec3.atCenterOf(pos).add(0.0D, PIVOT_HEIGHT - 0.5D, 0.0D);
        Vec3 direction = target.getEyePosition().subtract(pivot).normalize();
        Vec3 focus = pivot.add(direction.scale(0.58D));
        level.sendParticles(SPELL_GLOW, focus.x, focus.y, focus.z,
                8, 0.08D, 0.08D, 0.08D, 0.015D);
        level.sendParticles(SPELL_CORE, focus.x, focus.y, focus.z,
                4, 0.035D, 0.035D, 0.035D, 0.008D);
        Vec3 impact = target.getEyePosition();
        level.sendParticles(SPELL_GLOW, impact.x, impact.y, impact.z,
                16, target.getBbWidth() * 0.22D, target.getBbHeight() * 0.12D,
                target.getBbWidth() * 0.22D, 0.035D);
        level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z,
                5, 0.12D, 0.12D, 0.12D, 0.02D);
    }

    private static @Nullable LivingEntity findNearestThreat(ServerLevel level, BlockPos pos) {
        Vec3 origin = Vec3.atCenterOf(pos).add(0.0D, PIVOT_HEIGHT - 0.5D, 0.0D);
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos).inflate(RANGE),
                        entity -> entity.isAlive() && entity instanceof Enemy
                                && BaseEnergyRules.ownsPosition(level, pos, entity.position())
                ).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
                .orElse(null);
    }

    private @Nullable LivingEntity currentTarget(ServerLevel level, BlockPos pos) {
        LivingEntity target = aim.target(level);
        return target != null && target.distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE * RANGE
                && target instanceof Enemy
                && BaseEnergyRules.ownsPosition(level, pos, target.position()) ? target : null;
    }

    private void setTarget(@Nullable LivingEntity target) {
        int id = target == null ? -1 : target.getId();
        if (!aim.setTargetEntityId(id) || level == null) return;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    public TurretAimController aimController() {
        return aim;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("FireCooldown", cooldown);
        output.putInt("BurstHits", burstHitsRemaining);
        output.putInt("BurstStep", burstStep);
        if (burstTarget != null) output.putString("BurstTarget", burstTarget.toString());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cooldown = Math.max(0, input.getIntOr("FireCooldown", 0));
        burstHitsRemaining = Mth.clamp(input.getIntOr("BurstHits", 0), 0, BURST_HITS);
        burstStep = Mth.clamp(input.getIntOr("BurstStep", 0), 0, BURST_STEP_TICKS);
        String savedTarget = input.getStringOr("BurstTarget", "");
        try {
            burstTarget = savedTarget.isBlank() ? null : UUID.fromString(savedTarget);
        } catch (IllegalArgumentException ignored) {
            burstTarget = null;
            burstHitsRemaining = 0;
            burstStep = 0;
        }
        if (burstTarget == null) {
            burstHitsRemaining = 0;
            burstStep = 0;
        }
        aim.setTargetEntityId(input.getIntOr("RenderTarget", -1));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putInt("RenderTarget", aim.targetEntityId());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
