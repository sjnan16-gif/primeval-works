package com.primevalworks.world.base;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum BaseUpgrade {
    HEARTHSTONE(0, "Hearthstone", "The heart of the camp.", "Awakens the base network and reveals its first two branches.", 1, 0, -1, 0, 0, -300),

    SURVEY_STAKES(1, "Survey Stakes", "Push the boundary outward.", "Adds 4 blocks to the working radius at each rank.", 3, 1, 0, 1, -140, -225),
    TRAIL_MARKERS(2, "Trail Markers", "Lose less time between tasks.", "Makes every dinosaur complete work 5% faster at each rank.", 3, 1, 0, 1, 140, -225),

    WIDE_BOUNDARIES(3, "Wide Boundaries", "Claim the distant ground.", "Adds another 8 blocks to the working radius at each rank.", 2, 2, 1, 1, -210, -140),
    FEEDING_BELLS(4, "Feeding Bells", "Call workers before hunger bites.", "Slows hunger drain by 8% at each rank.", 3, 2, 1, 1, -70, -140),
    WORKSHOP_RHYTHM(6, "Workshop Rhythm", "Keep every station moving.", "Adds 6% cooking and crafting speed at each rank.", 3, 2, 2, 1, 70, -140),
    COPPER_BUSBARS(7, "Copper Busbars", "Carry power without waste.", "Adds 8% energy work speed at each rank.", 3, 2, 2, 1, 210, -140),

    EXPEDITION_CHARTS(9, "Expedition Charts", "Mark the routes worth taking.", "Expeditions return 10% more resources at each rank.", 3, 2, 3, 1, -245, -45),
    WATCH_POSTS(10, "Watch Posts", "Spot trouble before it arrives.", "Extends threat awareness by 3 blocks at each rank.", 2, 2, 3, 1, -175, -45),
    QUIET_ROOSTS(5, "Quiet Roosts", "Recover after a hard shift.", "Reduces mood drain by 9% at each rank.", 3, 2, 4, 1, -105, -45),
    DEEP_PANTRY(17, "Deep Pantry", "Make every meal last.", "Slows hunger drain by another 12% at each rank.", 2, 2, 4, 1, -35, -45),
    PACK_FRAMES(8, "Pack Frames", "Carry weight without losing pace.", "Adds 6% transport work speed at each rank.", 3, 2, 6, 1, 35, -45),
    FURNACE_BELLOWS(12, "Furnace Bellows", "Turn patience into heat.", "Adds 7% fire-work speed at each rank.", 3, 2, 6, 1, 105, -45),
    MASTER_TOOLS(13, "Master Tools", "Give careful claws better tools.", "Adds 7% crafting speed at each rank.", 3, 2, 7, 1, 175, -45),
    GROUNDING_RODS(14, "Grounding Rods", "Tame an unstable current.", "Adds 9% energy work speed at each rank.", 3, 2, 7, 1, 245, -45),

    ANCIENT_CARTOGRAPHY(15, "Ancient Cartography", "Read paths lost to time.", "Adds 15% expedition rewards and shortens expeditions by 5% per rank.", 2, 3, 9, 2, -245, 55),
    TRAIL_WARDS(16, "Trail Wards", "Warn the camp before an ambush.", "Adds 4 blocks of threat awareness at each rank.", 2, 3, 10, 1, -175, 55),
    NIGHT_LANTERNS(18, "Night Lanterns", "Keep tired workers grounded.", "Reduces mood drain by another 12% at each rank.", 2, 3, 5, 2, -105, 55),
    CAMP_SANCTUARY(19, "Camp Sanctuary", "Let the whole camp exhale.", "Improves both hunger and mood endurance by 8% at each rank.", 2, 3, 17, 1, -35, 55),
    QUICK_HANDOFFS(20, "Quick Handoffs", "Never set a load down twice.", "Adds another 7% transport speed at each rank.", 2, 3, 8, 2, 35, 55),
    HEAT_RESERVOIR(21, "Heat Reservoir", "Hold heat between cycles.", "Adds another 8% fire-work speed at each rank.", 2, 3, 12, 2, 105, 55),
    PATTERN_MEMORY(22, "Pattern Memory", "Teach a station to remember.", "Adds another 8% crafting speed at each rank.", 2, 3, 13, 2, 175, 55),
    ENERGY_RESERVOIR(23, "Energy Reservoir", "Save every useful spark.", "Adds another 10% energy speed at each rank.", 2, 3, 14, 2, 245, 55),

    FAR_HORIZON(24, "Far Horizon", "Build where the old maps end.", "Adds 12 blocks of base radius and 20% expedition rewards.", 1, 5, 15, 2, -245, 150),
    FRONTIER_WARDS(26, "Frontier Wards", "Make the boundary watch back.", "Adds 8 blocks of threat awareness and 4 blocks of base radius.", 1, 5, 16, 2, -140, 150),
    ANCIENT_SANCTUARY(27, "Ancient Sanctuary", "Rest beneath older protection.", "Reduces hunger and mood drain by another 15%.", 1, 5, 19, 2, -35, 150),
    LIVING_WORKSHOP(25, "Living Workshop", "Make the base move as one.", "Adds 10% speed to transport, fire, and crafting work.", 1, 5, 22, 2, 140, 150),
    ANCIENT_NETWORK(11, "Ancient Network", "Wake the buried grid.", "Adds 14% speed to every specialty and 10 blocks of radius.", 1, 6, 23, 2, 245, 150),

    CREW_PERCHES(28, "+2 Slots", "Make room for two more workers.", "Adds two active dinosaur slots to this base.", 1, 2, 4, 1, -315, -45),
    PACK_HIERARCHY(29, "+2 Slots", "Settle two more workers into the crew.", "Adds two active dinosaur slots to this base.", 1, 2, 8, 2, 315, 55),
    ANCIENT_BONDS(30, "+3 Slots", "Hold a larger crew together.", "Adds three active dinosaur slots to this base.", 1, 6, 25, 1, 140, 245);

    private final int id;
    private final String title;
    private final String summary;
    private final String detail;
    private final int maxLevel;
    private final int baseCost;
    private final int prerequisiteId;
    private final int prerequisiteLevel;
    private final int treeX;
    private final int treeY;

    BaseUpgrade(int id, String title, String summary, String detail, int maxLevel, int baseCost,
                int prerequisiteId, int prerequisiteLevel, int treeX, int treeY) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.detail = detail;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.prerequisiteId = prerequisiteId;
        this.prerequisiteLevel = prerequisiteLevel;
        this.treeX = treeX;
        this.treeY = treeY;
    }

    public int id() { return id; }
    public String title() { return title; }
    public String summary() { return summary; }
    public String detail() { return detail; }
    public int maxLevel() { return maxLevel; }
    public int prerequisiteId() { return prerequisiteId; }
    public int prerequisiteLevel() { return prerequisiteLevel; }
    public int treeX() { return treeX; }
    public int treeY() { return treeY; }

    public int costForLevel(int currentLevel) {
        return baseCost == 0 ? 0 : baseCost + Math.max(0, currentLevel);
    }

    public List<UpgradeCost> itemCostsForLevel(int currentLevel) {
        if (this == HEARTHSTONE) return List.of();
        int rank = Math.max(0, currentLevel) + 1;
        if (this == CREW_PERCHES || this == PACK_HIERARCHY) {
            return List.of(cost("primevalworks:hardwood", 12), cost("minecraft:lead", 2));
        }
        if (this == ANCIENT_BONDS) {
            return List.of(cost("primevalworks:ancient_metal_ingot", 6), cost("primevalworks:compressed_core", 2));
        }
        return switch (baseCost) {
            case 1 -> List.of(
                    cost("minecraft:copper_ingot", 4 + rank * 2),
                    cost("minecraft:oak_log", 6 + rank * 4)
            );
            case 2 -> List.of(
                    cost("minecraft:iron_ingot", 6 + rank * 4),
                    cost("minecraft:leather", 3 + rank * 2)
            );
            case 3 -> List.of(
                    cost("minecraft:gold_ingot", 5 + rank * 3),
                    cost("minecraft:amethyst_shard", 4 + rank * 2)
            );
            case 5 -> List.of(
                    cost("primevalworks:raw_ancient_metal_ingot", 2 + rank),
                    cost("minecraft:diamond", 2 + rank)
            );
            default -> List.of(
                    cost("primevalworks:ancient_metal_ingot", 3 + rank),
                    cost("primevalworks:compressed_core", rank)
            );
        };
    }

    private static UpgradeCost cost(String itemId, int count) {
        return new UpgradeCost(Identifier.parse(itemId), count);
    }

    public record UpgradeCost(Identifier itemId, int count) {
        public Item item() {
            return BuiltInRegistries.ITEM.get(itemId).map(holder -> holder.value()).orElse(Items.AIR);
        }

        public ItemStack stack() {
            return new ItemStack(item(), count);
        }
    }

    public static Optional<BaseUpgrade> byId(int id) {
        return Arrays.stream(values()).filter(upgrade -> upgrade.id == id).findFirst();
    }
}
