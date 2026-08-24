package com.primevalworks.client.screen;

final class CommandTableUpgradeRevealRules {
    private CommandTableUpgradeRevealRules() {
    }

    static boolean startsReveal(boolean wasVisible, boolean isVisible) {
        return !wasVisible && isVisible;
    }
}
