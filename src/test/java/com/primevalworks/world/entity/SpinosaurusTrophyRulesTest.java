package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpinosaurusTrophyRulesTest {
    @Test
    void headRemainsAPlayerHuntTrophyAndAnExceptionalExpeditionFind() {
        assertTrue(SpinosaurusTrophyRules.manualKillChance() > 0.05F
                        && SpinosaurusTrophyRules.manualKillChance() < 0.10F,
                "Manual Spinosaurus hunting should be a rare but believable trophy source");
        assertTrue(SpinosaurusTrophyRules.frontierExpeditionChance()
                        < SpinosaurusTrophyRules.manualKillChance() * 0.25F,
                "Expeditions should find a Spinosaurus Head much less often than a manual kill");
    }
}
