package com.primevalworks.world.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.client.render.item.PrimordialSwordRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

public final class PrimordialSwordItem extends Item implements GeoItem {
    public static final float ATTACK_DAMAGE_BASELINE = 3.0F;
    public static final float ATTACK_SPEED_BASELINE = -2.4F;
    public static final double SWEEP_HORIZONTAL_INFLATION = 2.15D;
    public static final double SWEEP_VERTICAL_INFLATION = 0.45D;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public PrimordialSwordItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PrimordialSwordRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new PrimordialSwordRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
        return target.getBoundingBox().inflate(SWEEP_HORIZONTAL_INFLATION,
                SWEEP_VERTICAL_INFLATION, SWEEP_HORIZONTAL_INFLATION);
    }
}
