package com.primevalworks.world.work;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record DinoWhistleSettings(FieldMode mode, Pattern pattern, int range, String itemFilter) {
    public static final int MIN_RANGE = DinoWhistleRules.MIN_RANGE;
    public static final int MAX_RANGE = DinoWhistleRules.MAX_RANGE;
    public static final DinoWhistleSettings DEFAULT = new DinoWhistleSettings(
            FieldMode.QUARRY, Pattern.CONNECTED, 48, "");

    public DinoWhistleSettings(FieldMode mode, Pattern pattern, int range) {
        this(mode, pattern, range, "");
    }

    public DinoWhistleSettings {
        mode = mode == null ? FieldMode.QUARRY : mode;
        pattern = mode.normalizePattern(pattern);
        range = DinoWhistleRules.clampRange(range);
        itemFilter = itemFilter == null ? "" : itemFilter.trim();
    }

    public static DinoWhistleSettings read(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = root.getCompound("PrimevalWhistle").orElse(null);
        if (tag == null) return DEFAULT;
        return new DinoWhistleSettings(
                FieldMode.byId(tag.getIntOr("Mode", DEFAULT.mode.ordinal())),
                Pattern.byId(tag.getIntOr("Pattern", DEFAULT.pattern.ordinal())),
                tag.getIntOr("Range", DEFAULT.range),
                tag.getStringOr("ItemFilter", "")
        );
    }

    public void write(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Mode", mode.ordinal());
            tag.putInt("Pattern", pattern.ordinal());
            tag.putInt("Range", range);
            if (!itemFilter.isBlank()) tag.putString("ItemFilter", itemFilter);
            root.put("PrimevalWhistle", tag);
        });
    }

    public String shortLabel() {
        return mode.title + " / " + mode.targetTitle(pattern);
    }

    public boolean filtersItems() {
        return mode == FieldMode.COLLECT && !itemFilter.isBlank();
    }

    public enum FieldMode {
        QUARRY("Quarry", "Breaks stone and ore. Harder blocks need a stronger field rating."),
        LUMBER("Lumber", "Fells a chosen log or a bounded connected tree."),
        HARVEST("Harvest", "Harvests mature crops without touching storage or machines."),
        COLLECT("Collect", "Collects loose items around the follower and brings them to you.");

        private final String title;
        private final String description;

        FieldMode(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String title() { return title; }
        public String description() { return description; }

        public boolean requiresMark() {
            return this == QUARRY || this == LUMBER;
        }

        public boolean isPassive() {
            return !requiresMark();
        }

        public Pattern normalizePattern(Pattern requested) {
            if (this == QUARRY) return requested == Pattern.AREA ? Pattern.AREA : Pattern.CONNECTED;
            return this == LUMBER ? Pattern.CONNECTED : Pattern.AREA;
        }

        public String targetTitle(Pattern pattern) {
            return switch (this) {
                case QUARRY -> switch (pattern) {
                    case SINGLE, CONNECTED -> "Vein";
                    case AREA -> "Area";
                };
                case LUMBER -> "Tree";
                case HARVEST -> "Nearby crops";
                case COLLECT -> "Nearby items";
            };
        }

        public String targetDescription(Pattern pattern) {
            return switch (this) {
                case QUARRY -> switch (pattern) {
                    case SINGLE, CONNECTED -> "Mine matching blocks connected to the one you mark.";
                    case AREA -> "Mine only the marked block type between two corners.";
                };
                case LUMBER -> "Cut every connected log in the tree you mark.";
                case HARVEST -> "Harvest and replant mature crops near the follower.";
                case COLLECT -> "Retrieve matching loose items near the follower.";
            };
        }

        public String markHint(Pattern pattern) {
            if (this == HARVEST) return "Assign a follower; mature crops are handled automatically.";
            if (this == COLLECT) return "Assign a follower; loose items are gathered automatically.";
            if (this == LUMBER) return "Mark a log to choose the tree.";
            return pattern == Pattern.AREA
                    ? "Mark the block type, then mark the opposite corner."
                    : "Mark stone or ore to choose a connected vein.";
        }

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
