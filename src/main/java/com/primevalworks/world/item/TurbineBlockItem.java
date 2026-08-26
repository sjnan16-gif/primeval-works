package com.primevalworks.world.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.client.render.item.TurbineItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class TurbineBlockItem extends BlockItem implements GeoItem {
    public enum Variant {
        WIND("wind_turbine", "wind_turbine"),
        UPGRADED_WIND("wind_turbine", "wind_turbine_upgraded"),
        WATER("water_turbine", "water_turbine");

        private final String model;
        private final String texture;

        Variant(String model, String texture) {
            this.model = model;
            this.texture = texture;
        }

        public String model() {
            return model;
        }

        public String texture() {
            return texture;
        }
    }

    private final Variant variant;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public TurbineBlockItem(Block block, Properties properties, Variant variant) {
        super(block, properties);
        this.variant = variant;
        GeoItem.registerSyncedAnimatable(this);
    }

    public Variant variant() {
        return variant;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private TurbineItemRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new TurbineItemRenderer(variant);
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
}
