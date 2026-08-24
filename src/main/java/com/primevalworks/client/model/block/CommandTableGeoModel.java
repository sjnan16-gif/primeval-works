package com.primevalworks.client.model.block;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.resources.Identifier;

public final class CommandTableGeoModel extends GeoModel<CommandTableBlockEntity> {
    private static final Identifier MODEL = asset("block/command_table");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/block/dino_command_table.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CommandTableBlockEntity animatable) {
        return MODEL;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }
}
