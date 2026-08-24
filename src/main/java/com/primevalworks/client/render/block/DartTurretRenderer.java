package com.primevalworks.client.render.block;

import com.primevalworks.world.block.entity.DartTurretBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DartTurretRenderer extends AbstractTurretRenderer<DartTurretBlockEntity> {
    public DartTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
