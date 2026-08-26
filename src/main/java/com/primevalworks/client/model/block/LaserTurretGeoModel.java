package com.primevalworks.client.model.block;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import net.minecraft.resources.Identifier;

public final class LaserTurretGeoModel extends GeoModel<LaserTurretBlockEntity> {
    public static final DataTicket<Boolean> FIRING =
            DataTicket.create("primevalworks_laser_turret_firing", Boolean.class);
    private static final Identifier MODEL = asset("block/laser_turret");
    private static final Identifier STATIC_TEXTURE = texture("laser_turret");
    private static final Identifier FIRING_TEXTURE = texture("laser_turret_firing");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(FIRING, false) ? FIRING_TEXTURE : STATIC_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(LaserTurretBlockEntity animatable) {
        return MODEL;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/block/" + name + ".png");
    }
}
