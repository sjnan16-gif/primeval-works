package com.primevalworks.client.model.block;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.MagicTurretBlockEntity;
import net.minecraft.resources.Identifier;

public final class MagicTurretGeoModel extends GeoModel<MagicTurretBlockEntity> {
    public static final DataTicket<Boolean> FIRING =
            DataTicket.create("primevalworks_magic_turret_firing", Boolean.class);
    private static final Identifier MODEL = asset("block/magic_turret");
    private static final Identifier STATIC_TEXTURE = texture("magic_turret");
    private static final Identifier FIRING_TEXTURE = texture("magic_turret_firing");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(FIRING, false) ? FIRING_TEXTURE : STATIC_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MagicTurretBlockEntity animatable) {
        return MODEL;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/block/" + name + ".png");
    }
}
