package com.primevalworks.client.model.entity;

import com.primevalworks.PrimevalWorks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public record DinosaurVisualProfile(
        String assetName,
        Identifier texture,
        Identifier blinkTexture,
        float modelScale,
        float modelGroundOffset,
        float shadowRadius,
        float indicatorHeight,
        float statusIconScale,
        float previewWidth,
        float previewHeight,
        String headBone
) {
    public static DinosaurVisualProfile forType(EntityType<?> type) {
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        return switch (species) {
            case "field_dodo" -> new DinosaurVisualProfile(
                    "dodo", texture("dodo"), texture("dodo_blink"),
                    1.0F, 0.0F, 0.42F, 1.75F, 0.82F, 0.88F, 1.56F, "Head"
            );
            case "tyrannosaurus" -> new DinosaurVisualProfile(
                    "t_rex", texture("t_rex"), texture("t_rex_blink"),
                    1.0F, 0.015625F, 1.25F, 3.42F, 1.34F, 2.03F, 3.25F, "head"
            );
            case "brachiosaurus" -> placeholder(1.95F, 1.15F, 3.35F);
            case "spinosaurus" -> new DinosaurVisualProfile(
                    "spino", texture("spino"), texture("spino_blink"),
                    1.0F, 0.0F, 1.52F, 5.38F, 1.48F, 2.03F, 5.05F,
                    "head2"
            );
            case "stegosaurus" -> new DinosaurVisualProfile(
                    "stegosaurus", texture("stegosaurus"), texture("stegosaurus_blink"),
                    1.0F, 0.0F, 1.12F, 3.14F, 1.18F, 1.75F, 2.88F, "Head"
            );
            case "triceratops" -> new DinosaurVisualProfile(
                    "triceratops", texture("triceratops"), texture("triceratops_blink"),
                    1.0F, 0.0F, 1.25F, 3.68F, 1.28F, 2.25F, 3.375F, "head"
            );
            case "parasaurolophus" -> new DinosaurVisualProfile(
                    "parasaurolophus", texture("parasaurolophus"), texture("parasaurolophus_blink"),
                    1.0F, 0.0F, 1.0F, 3.55F, 1.14F, 1.50F, 3.32F, "Head"
            );
            case "velociraptor" -> new DinosaurVisualProfile(
                    "velociraptor", texture("velociraptor"), texture("velociraptor_blink"),
                    1.0F, 0.0F, 0.56F, 1.78F, 0.88F, 0.77F, 1.55F,
                    "head2"
            );
            case "ankylosaurus" -> placeholder(0.98F, 0.72F, 1.62F);
            case "pteranodon" -> new DinosaurVisualProfile(
                    "pteranodon", texture("pteranodon"), texture("pteranodon_blink"),
                    1.0F, 0.0F, 0.78F, 2.42F, 1.0F, 8.75F, 1.0F, "Head"
            );
            case "dilophosaurus", "pachycephalosaurus" -> placeholder(0.88F, 0.56F, 1.52F);
            default -> placeholder(0.78F, 0.48F, 1.42F);
        };
    }

    private static DinosaurVisualProfile placeholder(float scale, float shadow, float indicatorHeight) {
        return new DinosaurVisualProfile(
                "field_dodo", texture("dodo"), texture("dodo_blink"),
                scale, 0.0F, shadow, indicatorHeight, 0.92F, 0.88F * scale, 1.56F * scale,
                "head"
        );
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/entity/" + name + ".png");
    }
}
