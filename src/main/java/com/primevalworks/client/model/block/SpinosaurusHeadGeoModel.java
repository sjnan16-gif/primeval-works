package com.primevalworks.client.model.block;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.SpinosaurusHeadBlockEntity;
import net.minecraft.resources.Identifier;

public final class SpinosaurusHeadGeoModel extends GeoModel<SpinosaurusHeadBlockEntity> {
    private static final Identifier MODEL = asset("block/spinosaurus_head");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/entity/spino.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(SpinosaurusHeadBlockEntity animatable) {
        return MODEL;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }
}
