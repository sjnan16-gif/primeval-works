package com.primevalworks.client.model.item;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.item.LaserTurretBlockItem;
import net.minecraft.resources.Identifier;

public final class LaserTurretItemGeoModel extends GeoModel<LaserTurretBlockItem> {
    private static final Identifier MODEL = asset("block/laser_turret");
    private static final Identifier TEXTURE = asset("textures/block/laser_turret.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(LaserTurretBlockItem animatable) {
        return MODEL;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }
}
