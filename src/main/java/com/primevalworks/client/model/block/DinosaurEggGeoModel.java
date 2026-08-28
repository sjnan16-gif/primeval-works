package com.primevalworks.client.model.block;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.DinosaurEggBlockEntity;
import com.primevalworks.world.egg.DinosaurEggSize;
import net.minecraft.resources.Identifier;

public final class DinosaurEggGeoModel extends GeoModel<DinosaurEggBlockEntity> {
    public static final DataTicket<DinosaurEggSize> SIZE =
            DataTicket.create("primevalworks_dinosaur_egg_size", DinosaurEggSize.class);
    private static final Identifier ANIMATION = asset("block/dinosaur_egg");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return asset("block/" + assetName(renderState.getOrDefaultGeckolibData(SIZE, DinosaurEggSize.SMALL)));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID,
                "textures/block/" + assetName(renderState.getOrDefaultGeckolibData(SIZE, DinosaurEggSize.SMALL)) + ".png");
    }

    @Override
    public Identifier getAnimationResource(DinosaurEggBlockEntity animatable) {
        return ANIMATION;
    }

    private static String assetName(DinosaurEggSize size) {
        return switch (size) {
            case SMALL -> "small_dinosaur_egg";
            case BIG -> "big_dinosaur_egg";
            case LARGE -> "large_dinosaur_egg";
        };
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }
}
