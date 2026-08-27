package com.primevalworks.world.work;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record DinoWhistleSettings(FieldMode mode, Pattern pattern, boolean continuous, int range) {
    public static final int MIN_RANGE = DinoWhistleRules.MIN_RANGE;
    public static final int MAX_RANGE = DinoWhistleRules.MAX_RANGE;
    public static final DinoWhistleSettings DEFAULT = new DinoWhistleSettings(
            FieldMode.QUARRY, Pattern.SINGLE, false, 48);

    public DinoWhistleSettings {
        mode = mode == null ? FieldMode.QUARRY : mode;
        pattern = pattern == null ? Pattern.SINGLE : pattern;
        range = DinoWhistleRules.clampRange(range);
    }

    public static DinoWhistleSettings read(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = root.getCompound("PrimevalWhistle").orElse(null);
        if (tag == null) return DEFAULT;
        return new DinoWhistleSettings(
                FieldMode.byId(tag.getIntOr("Mode", DEFAULT.mode.ordinal())),
                Pattern.byId(tag.getIntOr("Pattern", DEFAULT.pattern.ordinal())),
                tag.getBooleanOr("Continuous", DEFAULT.continuous),
                tag.getIntOr("Range", DEFAULT.range)
        );
    }

    public void write(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Mode", mode.ordinal());
            tag.putInt("Pattern", pattern.ordinal());
            tag.putBoolean("Continuous", continuous);
            tag.putInt("Range", range);
            root.put("PrimevalWhistle", tag);
        });
    }

    public String shortLabel() {
        return mode.title + " / " + pattern.title + (continuous ? " / CONTINUOUS" : " / ONE TIME");
    }

    public enum FieldMode {
        QUARRY("Quarry", "Breaks stone and ore. Harder blocks need a stronger field rating."),
        LUMBER("Lumber", "Fells a chosen log or a bounded connected tree."),
        HARVEST("Harvest", "Harvests mature crops without touching storage or machines."),
        COLLECT("Collect", "Collects loose items from the marked ground and brings them to you.");

        private final String title;
        private final String description;

        FieldMode(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String title() { return title; }
        public String description() { return description; }

        public static FieldMode byId(int id) {
            return values()[Math.max(0, Math.min(id, values().length - 1))];
        }
    }

    public enum Pattern {
        SINGLE("Single", "Works only on the block you mark."),
        CONNECTED("Connected", "Follows matching blocks, with a strict size and travel limit."),
        AREA("Area", "Uses two corners. The selected volume is capped for balance and safety.");

        private final String title;
        private final String description;

        Pattern(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String title() { return title; }
        public String description() { return description; }

        public static Pattern byId(int id) {
            return values()[Math.max(0, Math.min(id, values().length - 1))];
        }
    }
}
