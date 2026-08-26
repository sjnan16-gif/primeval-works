package com.primevalworks.client.model.item;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.item.TurbineBlockItem;
import net.minecraft.resources.Identifier;

public final class TurbineItemGeoModel extends GeoModel<TurbineBlockItem> {
    private final Identifier model;
    private final Identifier texture;

    public TurbineItemGeoModel(TurbineBlockItem.Variant variant) {
        model = asset("block/" + variant.model());
        texture = asset("textures/block/" + variant.texture() + ".png");
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return texture;
    }

    @Override
    public Identifier getAnimationResource(TurbineBlockItem animatable) {
        return model;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }
}
