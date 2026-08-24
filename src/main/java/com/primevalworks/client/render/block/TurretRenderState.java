package com.primevalworks.client.render.block;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class TurretRenderState extends BlockEntityRenderState {
    public float yaw;
    public float pitch;
    public float beamLength;
    public float animationTime;
    public boolean laser;
    public boolean hasTarget;
}
