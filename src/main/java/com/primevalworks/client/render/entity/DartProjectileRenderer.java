package com.primevalworks.client.render.entity;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.DartProjectileEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

public final class DartProjectileRenderer extends ArrowRenderer<DartProjectileEntity, ArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/entity/projectiles/dart.png");

    public DartProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
