package com.primevalworks.config;

public final class PrimevalTuning {
    private static volatile Server server = Server.defaults();

    private PrimevalTuning() {
    }

    public static Server server() {
        return server;
    }

    static void apply(Server values) {
        server = values;
    }

    static void reset() {
        server = Server.defaults();
    }

    public record Server(
            double hungerDrainRate,
            double moodDrainRate,
            double nightShiftMoodRate,
            int foodBoxThreshold,
            double recoveryTime,
            double breedingCooldown,
            double wildHugeChance,
            double wildAlbinoChance,
            double incubatedHugeChance,
            double incubatedAlbinoChance,
            double bredHugeChance,
            double bredAlbinoChance,
            double oneParentInheritance,
            double twoParentInheritance,
            double hugeScale,
            double hugeStats,
            double albinoStats,
            double albinoHealth,
            double workSpeed,
            double transportCapacity,
            double energyGeneration,
            int targetsPerWorkOrder,
            double energyStorage,
            double waterTurbineOutput,
            double expeditionTime,
            double expeditionRisk,
            double expeditionRewards,
            double pteranodonFlightSpeed,
            double pteranodonStaminaDrain,
            double pteranodonStaminaRecovery,
            double spinosaurusSwimSpeed,
            double spinosaurusSprintSpeed,
            double spinosaurusStaminaDrain,
            double spinosaurusStaminaRecovery,
            double dinosaurHealth,
            double dinosaurDamage,
            boolean hostileMobTargeting,
            double turretDamage,
            double turretRange,
            double machineEnergyUse,
            double processorSpeed,
            double ancientFurnaceSpeed,
            double incubatorSpeed
    ) {
        static Server defaults() {
            return new Server(
                    1.0D, 1.0D, 2.3D, 50, 1.0D, 1.0D,
                    0.05D, 0.005D, 0.25D, 0.04D, 0.09D, 0.012D, 0.65D, 0.88D,
                    1.18D, 1.20D, 1.40D, 0.80D,
                    1.0D, 1.0D, 1.0D, 8, 1.0D, 1.5D,
                    1.0D, 1.0D, 1.0D,
                    1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D,
                    1.0D, 1.0D, true, 1.0D, 1.0D,
                    1.0D, 1.0D, 1.0D, 1.0D
            );
        }
    }
}
