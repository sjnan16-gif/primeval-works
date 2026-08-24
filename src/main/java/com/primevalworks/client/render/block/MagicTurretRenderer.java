package com.primevalworks.client.render.block;

import com.primevalworks.world.block.entity.MagicTurretBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class MagicTurretRenderer extends AbstractTurretRenderer<MagicTurretBlockEntity> {
    public MagicTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
