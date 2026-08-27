package com.primevalworks.world.work;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class DinoFieldWorkRules {
    public static final int MAX_CONNECTED_BLOCKS = DinoFieldWorkLimits.MAX_CONNECTED_BLOCKS;
    public static final int MAX_AREA_BLOCKS = DinoFieldWorkLimits.MAX_AREA_BLOCKS;
    public static final int MAX_AREA_SPAN = DinoFieldWorkLimits.MAX_AREA_SPAN;

    private DinoFieldWorkRules() {
    }

    public static int rating(FieldDodoEntity dinosaur, DinoWhistleSettings.FieldMode mode) {
        return switch (mode) {
            case QUARRY -> Math.max(dinosaur.getSpecialtyStars(1), dinosaur.getSpecialtyStars(2));
            case LUMBER -> Math.max(dinosaur.getSpecialtyStars(3), dinosaur.getSpecialtyStars(4));
            case HARVEST -> dinosaur.getSpecialtyStars(4);
            case COLLECT -> dinosaur.getSpecialtyStars(0);
        };
    }

    public static String specialtyName(DinoWhistleSettings.FieldMode mode) {
        return switch (mode) {
            case QUARRY -> "Quarrying";
            case LUMBER -> "Lumbering";
            case HARVEST -> "Field Harvest";
            case COLLECT -> "Ground Retrieval";
        };
    }

    public static boolean validTarget(Level level, BlockPos pos, DinoWhistleSettings.FieldMode mode, int rating) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (mode == DinoWhistleSettings.FieldMode.COLLECT) return true;
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || level.getBlockEntity(pos) != null) return false;
        if (state.getBlock().getDescriptionId().startsWith("block." + PrimevalWorks.MOD_ID + ".")) return false;
        return switch (mode) {
            case QUARRY -> state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                    && state.getDestroySpeed(level, pos) <= maximumHardness(rating);
            case LUMBER -> state.is(BlockTags.LOGS);
            case HARVEST -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
            case COLLECT -> true;
        };
    }

    public static float maximumHardness(int rating) {
        return switch (Mth.clamp(rating, 0, 4)) {
            case 0 -> 1.2F;
            case 1 -> 2.0F;
            case 2 -> 3.5F;
            case 3 -> 12.0F;
            default -> 55.0F;
        };
    }

    public static int workTicks(BlockState state, Level level, BlockPos pos, int rating) {
        float hardness = Math.max(0.4F, state.getDestroySpeed(level, pos));
        int base = Math.round(22.0F + hardness * 18.0F);
        return WorkSpecialtyRules.actionDurationTicks(Math.min(360, base), rating);
    }

    public static boolean areaWithinLimits(BlockPos first, BlockPos second) {
        return areaWithinLimits(first.getX(), first.getY(), first.getZ(),
                second.getX(), second.getY(), second.getZ());
    }

    public static boolean areaWithinLimits(int firstX, int firstY, int firstZ,
                                           int secondX, int secondY, int secondZ) {
        return DinoFieldWorkLimits.areaWithinLimits(firstX, firstY, firstZ, secondX, secondY, secondZ);
    }
}
