package com.primevalworks.world.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.primevalworks.client.render.item.TurbineItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class TurbineBlockItem extends BlockItem implements GeoItem {
    public enum Variant {
        WIND("wind_turbine", "wind_turbine", "Wind Turbine", 1.08F, -0.98F, 0.5F / 16.0F),
        UPGRADED_WIND("wind_turbine", "wind_turbine_upgraded", "Upgraded Wind Turbine", 1.08F, -0.98F, 0.5F / 16.0F),
        WATER("water_turbine", "water_turbine", "Water Turbine", 0.52F, -1.28F, 0.0F);

        private final String model;
        private final String texture;
        private final String displayName;
        private final float guiOffsetX;
        private final float guiOffsetY;
        private final float guiOffsetZ;

        Variant(String model, String texture, String displayName,
                float guiOffsetX, float guiOffsetY, float guiOffsetZ) {
            this.model = model;
            this.texture = texture;
            this.displayName = displayName;
            this.guiOffsetX = guiOffsetX;
            this.guiOffsetY = guiOffsetY;
            this.guiOffsetZ = guiOffsetZ;
        }

        public String model() {
            return model;
        }

        public String texture() {
            return texture;
        }

        public String displayName() {
            return displayName;
        }

        public float guiOffsetX() {
            return guiOffsetX;
        }

        public float guiOffsetY() {
            return guiOffsetY;
        }

        public float guiOffsetZ() {
            return guiOffsetZ;
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
    public Component getName(ItemStack stack) {
        return Component.literal(variant.displayName());
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
