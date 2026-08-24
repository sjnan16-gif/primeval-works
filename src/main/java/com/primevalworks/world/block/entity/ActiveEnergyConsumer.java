package com.primevalworks.world.block.entity;

import net.minecraft.world.level.Level;

public interface ActiveEnergyConsumer {
    boolean requestsBaseEnergy(Level level);
}
