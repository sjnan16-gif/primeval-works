package com.primevalworks.client.render.block;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class LaserObserverRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public float animationTime;
    public float endDistance;
}
