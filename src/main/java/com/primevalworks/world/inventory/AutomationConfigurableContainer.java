package com.primevalworks.world.inventory;

public interface AutomationConfigurableContainer {
    boolean allowsAutomationInsert(int slot);

    boolean allowsAutomationExtract(int slot);

    void toggleAutomationInsert(int slot);

    void toggleAutomationExtract(int slot);
}
