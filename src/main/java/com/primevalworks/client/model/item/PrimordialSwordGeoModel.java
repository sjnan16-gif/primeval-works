package com.primevalworks.client.model.item;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.item.PrimordialSwordItem;
import net.minecraft.resources.Identifier;

public final class PrimordialSwordGeoModel extends GeoModel<PrimordialSwordItem> {
    private static final Identifier MODEL = asset("item/primordial_sword");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/item/primordial_sword_model.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(PrimordialSwordItem animatable) {
        return MODEL;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }
}
