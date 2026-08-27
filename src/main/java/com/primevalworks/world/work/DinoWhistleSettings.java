package com.primevalworks.world.work;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record DinoWhistleSettings(FieldMode mode, Pattern pattern, boolean continuous, int range, String itemFilter) {
    public static final int MIN_RANGE = DinoWhistleRules.MIN_RANGE;
    public static final int MAX_RANGE = DinoWhistleRules.MAX_RANGE;
    public static final DinoWhistleSettings DEFAULT = new DinoWhistleSettings(
            FieldMode.QUARRY, Pattern.SINGLE, false, 48, "");

    public DinoWhistleSettings(FieldMode mode, Pattern pattern, boolean continuous, int range) {
        this(mode, pattern, continuous, range, "");
    }

    public DinoWhistleSettings {
        mode = mode == null ? FieldMode.QUARRY : mode;
        pattern = pattern == null ? Pattern.SINGLE : pattern;
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
                tag.getBooleanOr("Continuous", DEFAULT.continuous),
                tag.getIntOr("Range", DEFAULT.range),
                tag.getStringOr("ItemFilter", "")
        );
    }

    public void write(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Mode", mode.ordinal());
            tag.putInt("Pattern", pattern.ordinal());
            tag.putBoolean("Continuous", continuous);
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
        COLLECT("Collect", "Collects loose items from the marked ground and brings them to you.");

        private final String title;
        private final String description;

        FieldMode(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String title() { return title; }
        public String description() { return description; }

        public String targetTitle(Pattern pattern) {
            return switch (this) {
                case QUARRY -> switch (pattern) {
                    case SINGLE -> "Block";
                    case CONNECTED -> "Vein";
                    case AREA -> "Area";
                };
                case LUMBER -> switch (pattern) {
                    case SINGLE -> "Log";
                    case CONNECTED -> "Tree";
                    case AREA -> "Grove";
                };
                case HARVEST -> switch (pattern) {
                    case SINGLE -> "Crop";
                    case CONNECTED -> "Patch";
                    case AREA -> "Field";
                };
                case COLLECT -> switch (pattern) {
                    case SINGLE -> "Spot";
                    case CONNECTED -> "Nearby";
                    case AREA -> "Zone";
                };
            };
        }

        public String targetDescription(Pattern pattern) {
            return switch (this) {
                case QUARRY -> switch (pattern) {
                    case SINGLE -> "Break the stone or ore you mark.";
                    case CONNECTED -> "Follow matching stone or ore through one vein.";
                    case AREA -> "Mine valid blocks between two marked corners.";
                };
                case LUMBER -> switch (pattern) {
                    case SINGLE -> "Cut the log you mark.";
                    case CONNECTED -> "Follow connected logs through one tree.";
                    case AREA -> "Cut valid logs between two marked corners.";
                };
                case HARVEST -> switch (pattern) {
                    case SINGLE -> "Harvest the mature crop you mark.";
                    case CONNECTED -> "Harvest a connected patch of mature crops.";
                    case AREA -> "Harvest mature crops between two marked corners.";
                };
                case COLLECT -> switch (pattern) {
                    case SINGLE -> "Collect loose items around the marked spot.";
                    case CONNECTED -> "Search a wider circle around the marked spot.";
                    case AREA -> "Collect loose items between two marked corners.";
                };
            };
        }

        public String markHint(Pattern pattern) {
            String target = switch (this) {
                case QUARRY -> "stone or ore";
                case LUMBER -> "a log";
                case HARVEST -> "a mature crop";
                case COLLECT -> "the ground";
            };
            return pattern == Pattern.AREA
                    ? "Mark two corners around " + target + "."
                    : "Mark " + target + ".";
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
