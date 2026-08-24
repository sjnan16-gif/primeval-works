package com.primevalworks.client.render.block;

import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class LaserTurretRenderer extends AbstractTurretRenderer<LaserTurretBlockEntity> {
    public LaserTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
