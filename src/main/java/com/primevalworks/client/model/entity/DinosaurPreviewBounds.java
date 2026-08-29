package com.primevalworks.client.model.entity;

public record DinosaurPreviewBounds(float width, float height, float depth) {
    public static DinosaurPreviewBounds forVisual(DinosaurVisualProfile visual) {
        return forAsset(visual.assetName(), visual.modelScale());
    }

    static DinosaurPreviewBounds forAsset(String assetName, float modelScale) {
        return switch (assetName) {
            case "dodo" -> new DinosaurPreviewBounds(0.88F, 1.56F, 1.69F);
            case "t_rex" -> new DinosaurPreviewBounds(2.0F, 3.25F, 6.19F);
            case "stegosaurus" -> new DinosaurPreviewBounds(1.75F, 2.88F, 5.94F);
            case "parasaurolophus" -> new DinosaurPreviewBounds(1.50F, 3.32F, 6.29F);
            case "velociraptor" -> new DinosaurPreviewBounds(0.77F, 1.55F, 3.47F);
            case "pteranodon" -> new DinosaurPreviewBounds(2.60F, 2.25F, 4.88F);
            case "spino" -> new DinosaurPreviewBounds(2.03F, 5.05F, 10.06F);
            default -> new DinosaurPreviewBounds(
                    0.88F * modelScale,
                    1.56F * modelScale,
                    1.69F * modelScale
            );
        };
    }
}
