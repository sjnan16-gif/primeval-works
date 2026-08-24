package com.primevalworks.world.egg;

public enum EggFragmentRules {
    SMALL(0.24F, 0.05F),
    BIG(0.58F, 0.18F),
    LARGE(0.86F, 0.48F);

    private final float secondFragmentChance;
    private final float thirdFragmentChance;

    EggFragmentRules(float secondFragmentChance, float thirdFragmentChance) {
        this.secondFragmentChance = secondFragmentChance;
        this.thirdFragmentChance = thirdFragmentChance;
    }

    public int count(float secondRoll, float thirdRoll) {
        int fragments = 1;
        if (secondRoll < secondFragmentChance) fragments++;
        if (thirdRoll < thirdFragmentChance) fragments++;
        return fragments;
    }
}
