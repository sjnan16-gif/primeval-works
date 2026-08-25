package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.sound.PrimevalSoundPlayback;
import com.primevalworks.world.entity.DartProjectileEntity;
import com.primevalworks.world.inventory.DartTurretMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import org.jspecify.annotations.Nullable;

public final class DartTurretBlockEntity extends BaseContainerBlockEntity implements TargetingTurret {
    public static final int AMMO_SLOTS = 9;
    private static final int FIRE_INTERVAL_TICKS = 24;
    private static final double RANGE = 18.0D;
    private static final float PROJECTILE_SPEED = 1.75F;
    private static final float PROJECTILE_INACCURACY = 0.35F;
    private static final double PROJECTILE_DAMAGE = 6.0D;
    private static final double PIVOT_HEIGHT = 0.75D;

    private NonNullList<ItemStack> items = NonNullList.withSize(AMMO_SLOTS, ItemStack.EMPTY);
    private final TurretAimController aim = new TurretAimController();
    private int cooldown;
    private int targetRefresh;

    public DartTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DART_TURRET.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, DartTurretBlockEntity turret) {
        if (!BaseEnergyRules.isConnected(level, pos)) {
            turret.setTarget(null);
            return;
        }
        int ammoSlot = turret.firstAmmoSlot();
        if (ammoSlot < 0) {
            turret.setTarget(null);
            return;
        }
        LivingEntity target = turret.currentTarget(level, pos);
        if (target == null || turret.targetRefresh-- <= 0) {
            target = level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos).inflate(RANGE),
                        entity -> entity.isAlive() && entity instanceof Enemy
                                && BaseEnergyRules.ownsPosition(level, pos, entity.position())
                ).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(Vec3.atCenterOf(pos))))
                .orElse(null);
            turret.setTarget(target);
            turret.targetRefresh = 5;
        }
        if (target == null) {
            return;
        }
        if (!BaseEnergyRules.isPowered(level, pos)) return;
        if (turret.cooldown > 0) {
            turret.cooldown--;
            return;
        }

        Vec3 origin = Vec3.atCenterOf(pos).add(0.0D, 0.56D, 0.0D);
        ItemStack ammo = turret.getItem(ammoSlot);
        ammo.shrink(1);
        turret.setItem(ammoSlot, ammo);
        DartProjectileEntity dart = new DartProjectileEntity(ModEntities.DART_PROJECTILE.get(), level);
        dart.setPos(origin.x, origin.y, origin.z);
        dart.setBaseDamage(PROJECTILE_DAMAGE);
        Vec3 aim = target.getEyePosition().subtract(origin);
        dart.shoot(aim.x, aim.y, aim.z, PROJECTILE_SPEED, PROJECTILE_INACCURACY);
        level.addFreshEntity(dart);
        PrimevalSoundPlayback.playAt(level, pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS,
                0.62F, 1.24F, PrimevalSoundPlayback.LARGE_RADIUS);
        turret.cooldown = FIRE_INTERVAL_TICKS;
        turret.setChanged();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DartTurretBlockEntity turret) {
        turret.aim.clientTick(level, pos, PIVOT_HEIGHT);
    }

    private @Nullable LivingEntity currentTarget(ServerLevel level, BlockPos pos) {
        LivingEntity target = aim.target(level);
        return target != null && target instanceof Enemy
                && target.distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE * RANGE
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
    public boolean requestsBaseEnergy(Level level) {
        return firstAmmoSlot() >= 0 && TargetingTurret.super.requestsBaseEnergy(level);
    }

    private int firstAmmoSlot() {
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).is(ModItems.DART.get())) return slot;
        }
        return -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("FireCooldown", cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(AMMO_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
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

    @Override
    public int getContainerSize() {
        return AMMO_SLOTS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.DART.get());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.primevalworks.dart_turret");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DartTurretMenu(containerId, inventory, this);
    }
}
