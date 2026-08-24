package com.primevalworks.client.render.entity;

final class AlbinoEquipmentMask {
    private AlbinoEquipmentMask() {
    }

    static boolean contains(int plainColor, int equippedColor) {
        return plainColor != equippedColor;
    }
}
