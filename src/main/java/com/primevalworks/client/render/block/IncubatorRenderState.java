package com.primevalworks.client.render.block;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.FormattedCharSequence;

public final class IncubatorRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState egg = new ItemStackRenderState();
    public FormattedCharSequence timer = FormattedCharSequence.EMPTY;
    public float animationTime;
    public float progress;
    public boolean active;
}
