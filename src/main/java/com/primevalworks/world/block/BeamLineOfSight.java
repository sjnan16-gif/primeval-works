package com.primevalworks.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BeamLineOfSight {
    private static final double HIT_EPSILON = 1.0E-5D;

    private BeamLineOfSight() {
    }

    public static boolean isClear(Level level, Vec3 start, Vec3 end) {
        BlockHitResult hit = clip(level, start, end);
        return hit.getType() == HitResult.Type.MISS
                || start.distanceToSqr(hit.getLocation()) + HIT_EPSILON >= start.distanceToSqr(end);
    }

    public static boolean isAxisClearBefore(
            Level level,
            BlockPos origin,
            Direction facing,
            int targetDistance
    ) {
        if (targetDistance <= 1) return true;
        Vec3 center = Vec3.atCenterOf(origin);
        Vec3 start = center.add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(0.505D));
        Vec3 end = center.add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(targetDistance - 0.505D));
        return isClear(level, start, end);
    }

    public static double visibleDistance(Level level, Vec3 start, Vec3 end) {
        BlockHitResult hit = clip(level, start, end);
        return hit.getType() == HitResult.Type.MISS
                ? start.distanceTo(end)
                : start.distanceTo(hit.getLocation());
    }

    public static Vec3 justOutside(BlockPos blockPos, Vec3 inside, Vec3 toward) {
        Vec3 direction = toward.subtract(inside).normalize();
        if (direction.lengthSqr() < HIT_EPSILON) return inside;
        double exitDistance = Double.POSITIVE_INFINITY;
        if (direction.x > HIT_EPSILON) {
            exitDistance = Math.min(exitDistance, (blockPos.getX() + 1.0D - inside.x) / direction.x);
        } else if (direction.x < -HIT_EPSILON) {
            exitDistance = Math.min(exitDistance, (blockPos.getX() - inside.x) / direction.x);
        }
        if (direction.y > HIT_EPSILON) {
            exitDistance = Math.min(exitDistance, (blockPos.getY() + 1.0D - inside.y) / direction.y);
        } else if (direction.y < -HIT_EPSILON) {
            exitDistance = Math.min(exitDistance, (blockPos.getY() - inside.y) / direction.y);
        }
        if (direction.z > HIT_EPSILON) {
            exitDistance = Math.min(exitDistance, (blockPos.getZ() + 1.0D - inside.z) / direction.z);
        } else if (direction.z < -HIT_EPSILON) {
            exitDistance = Math.min(exitDistance, (blockPos.getZ() - inside.z) / direction.z);
        }
        return Double.isFinite(exitDistance)
                ? inside.add(direction.scale(Math.max(0.0D, exitDistance) + 0.01D))
                : inside;
    }

    public static float visibleAxisDistance(
            Level level,
            BlockPos origin,
            Direction facing,
            float startDistance,
            float endDistance
    ) {
        Vec3 center = Vec3.atCenterOf(origin);
        Vec3 axis = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        Vec3 start = center.add(axis.scale(startDistance));
        Vec3 end = center.add(axis.scale(endDistance));
        return (float)Math.min(endDistance, startDistance + visibleDistance(level, start, end));
    }

    private static BlockHitResult clip(Level level, Vec3 start, Vec3 end) {
        return level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));
    }
}
