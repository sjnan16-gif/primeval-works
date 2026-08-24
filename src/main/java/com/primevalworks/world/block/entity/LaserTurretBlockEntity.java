package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.base.BaseEnergyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public final class LaserTurretBlockEntity extends BlockEntity implements TargetingTurret {
    private static final int FIRE_INTERVAL_TICKS = 28;
    private static final double RANGE = 24.0D;
    private static final double PIVOT_HEIGHT = 0.75D;

    private final TurretAimController aim = new TurretAimController();
    private int cooldown;
    private int targetRefresh;

    public LaserTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_TURRET.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, LaserTurretBlockEntity turret) {
        if (!BaseEnergyRules.isConnected(level, pos)) {
            turret.setTarget(null);
            return;
        }

        LivingEntity target = turret.currentTarget(level, pos);
        if (target == null || turret.targetRefresh-- <= 0) {
            target = findNearestThreat(level, pos);
            turret.setTarget(target);
            turret.targetRefresh = 5;
        }
        if (target == null) return;
        if (!BaseEnergyRules.isPowered(level, pos)) return;
        if (turret.cooldown > 0) {
            turret.cooldown--;
            return;
        }

        target.hurtServer(level, level.damageSources().magic(), 14.0F);
        target.igniteForSeconds(4.0F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT,
                SoundSource.BLOCKS, 0.75F, 1.35F);
        turret.cooldown = FIRE_INTERVAL_TICKS;
        turret.setChanged();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LaserTurretBlockEntity turret) {
        turret.aim.clientTick(level, pos, PIVOT_HEIGHT);
    }

    private static @Nullable LivingEntity findNearestThreat(ServerLevel level, BlockPos pos) {
        Vec3 origin = Vec3.atCenterOf(pos).add(0.0D, PIVOT_HEIGHT - 0.5D, 0.0D);
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos).inflate(RANGE),
                        entity -> entity.isAlive() && entity instanceof Enemy
                ).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
                .orElse(null);
    }

    private @Nullable LivingEntity currentTarget(ServerLevel level, BlockPos pos) {
        LivingEntity target = aim.target(level);
        return target != null && target.distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE * RANGE
                && target instanceof Enemy ? target : null;
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cooldown = Math.max(0, input.getIntOr("FireCooldown", 0));
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
