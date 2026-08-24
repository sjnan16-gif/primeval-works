package com.primevalworks.world.entity;

import com.primevalworks.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DartProjectileEntity extends Arrow {
    private int lifetime;

    public DartProjectileEntity(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
        pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++lifetime > 160) discard();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.DART.get());
    }
}
