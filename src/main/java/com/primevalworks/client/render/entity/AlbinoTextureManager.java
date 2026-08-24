package com.primevalworks.client.render.entity;

import com.mojang.blaze3d.platform.NativeImage;
import com.primevalworks.PrimevalWorks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AlbinoTextureManager {
    private static final Map<Identifier, Identifier> GENERATED = new HashMap<>();

    private AlbinoTextureManager() {
    }

    public static Identifier textureFor(Identifier source) {
        Identifier cached = GENERATED.get(source);
        if (cached != null) return cached;

        Identifier generated = Identifier.fromNamespaceAndPath(
                PrimevalWorks.MOD_ID,
                "generated/albino/" + source.getNamespace() + "/" + source.getPath()
        );
        try {
            NativeImage albino = createAlbinoImage(source);
            Minecraft.getInstance().getTextureManager().register(
                    generated,
                    new DynamicTexture(() -> "Primeval Works albino " + source, albino)
            );
            GENERATED.put(source, generated);
            return generated;
        } catch (IOException | RuntimeException exception) {
            PrimevalWorks.LOGGER.warn("Could not build albino texture from {}", source, exception);
            GENERATED.put(source, source);
            return source;
        }
    }

    private static NativeImage createAlbinoImage(Identifier source) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(source)
                .orElseThrow(() -> new IOException("Missing dinosaur texture " + source));
        try (InputStream stream = resource.open(); NativeImage original = NativeImage.read(stream)) {
            NativeImage blink = readBlinkReference(source, original.getWidth(), original.getHeight()).orElse(null);
            try {
                NativeImage result = new NativeImage(original.getWidth(), original.getHeight(), false);
                boolean[][] equipmentMask = readEquipmentMask(
                        source,
                        original.getWidth(),
                        original.getHeight()
                ).orElse(null);
                boolean[][] eyeMask = blink == null
                        ? buildManualEyeMask(source, original.getWidth(), original.getHeight())
                        : buildEyeMask(source, original, blink);
                for (int y = 0; y < original.getHeight(); y++) {
                    for (int x = 0; x < original.getWidth(); x++) {
                        int color = original.getPixel(x, y);
                        result.setPixel(x, y, equipmentMask != null && equipmentMask[x][y]
                                ? color
                                : albinoPixel(
                                        color,
                                        blink == null ? color : blink.getPixel(x, y),
                                        eyeMask != null && eyeMask[x][y]
                                ));
                    }
                }
                return result;
            } finally {
                if (blink != null) blink.close();
            }
        }
    }

    private static boolean[][] buildEyeMask(Identifier source, NativeImage open, NativeImage blink) {
        int width = open.getWidth();
        int height = open.getHeight();
        boolean[][] candidates = new boolean[width][height];
        boolean[][] eyes = new boolean[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = open.getPixel(x, y);
                float luminance = (ARGB.red(color) * 0.2126F
                        + ARGB.green(color) * 0.7152F
                        + ARGB.blue(color) * 0.0722F) / 255.0F;
                candidates[x][y] = AlbinoEyeMasks.contains(source.getPath(), x, y)
                        && isOpenEyePixel(color, blink.getPixel(x, y), luminance);
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!candidates[x][y]) continue;
                eyes[x][y] = hasCandidateNeighbour(candidates, x, y, width, height);
            }
        }
        return eyes;
    }

    private static boolean[][] buildManualEyeMask(Identifier source, int width, int height) {
        boolean[][] eyes = new boolean[width][height];
        if (source.getPath().endsWith("_blink.png")) return eyes;

        // Keep an explicit fallback for packs that omit the authored blink atlas.
        // These are the pupil texels isolated by comparing the open and closed
        // Spinosaurus textures, so chest and sail accents can never become eyes.
        if (source.getPath().endsWith("/spino.png")) {
            for (int x = 191; x <= 194; x++) mark(eyes, x, 183);
            for (int x = 220; x <= 223; x++) mark(eyes, x, 183);
        }
        return eyes;
    }

    private static void mark(boolean[][] mask, int x, int y) {
        if (x >= 0 && x < mask.length && y >= 0 && y < mask[0].length) mask[x][y] = true;
    }

    private static boolean hasCandidateNeighbour(boolean[][] candidates, int x, int y, int width, int height) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (offsetX == 0 && offsetY == 0) continue;
                int neighbourX = x + offsetX;
                int neighbourY = y + offsetY;
                if (neighbourX >= 0 && neighbourX < width && neighbourY >= 0 && neighbourY < height
                        && candidates[neighbourX][neighbourY]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<NativeImage> readBlinkReference(Identifier source, int width, int height) throws IOException {
        if (source.getPath().endsWith("_blink.png")) return Optional.empty();
        Identifier blink = source.withPath(path -> path.substring(0, path.length() - 4) + "_blink.png");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(blink);
        if (resource.isEmpty()) return Optional.empty();
        try (InputStream stream = resource.get().open()) {
            NativeImage image = NativeImage.read(stream);
            if (image.getWidth() != width || image.getHeight() != height) {
                image.close();
                return Optional.empty();
            }
            return Optional.of(image);
        }
    }

    private static Optional<boolean[][]> readEquipmentMask(Identifier source, int width, int height) throws IOException {
        String path = source.getPath();
        Identifier plain;
        Identifier equipped;
        if (path.contains("/pteranodon_saddled")) {
            plain = source.withPath("textures/entity/pteranodon.png");
            equipped = source.withPath("textures/entity/pteranodon_saddled.png");
        } else if (path.contains("/spino_saddled")) {
            plain = source.withPath("textures/entity/spino.png");
            equipped = source.withPath("textures/entity/spino_saddled.png");
        } else {
            return Optional.empty();
        }

        Optional<Resource> plainResource = Minecraft.getInstance().getResourceManager().getResource(plain);
        Optional<Resource> equippedResource = Minecraft.getInstance().getResourceManager().getResource(equipped);
        if (plainResource.isEmpty() || equippedResource.isEmpty()) return Optional.empty();
        try (InputStream plainStream = plainResource.get().open();
             InputStream equippedStream = equippedResource.get().open();
             NativeImage plainImage = NativeImage.read(plainStream);
             NativeImage equippedImage = NativeImage.read(equippedStream)) {
            if (plainImage.getWidth() != width || plainImage.getHeight() != height
                    || equippedImage.getWidth() != width || equippedImage.getHeight() != height) {
                return Optional.empty();
            }
            boolean[][] mask = new boolean[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mask[x][y] = AlbinoEquipmentMask.contains(
                            plainImage.getPixel(x, y),
                            equippedImage.getPixel(x, y)
                    );
                }
            }
            return Optional.of(mask);
        }
    }

    static int albinoPixel(int color, int blinkColor, boolean hasBlinkReference) {
        int alpha = ARGB.alpha(color);
        if (alpha == 0) return color;

        float red = ARGB.red(color) / 255.0F;
        float green = ARGB.green(color) / 255.0F;
        float blue = ARGB.blue(color) / 255.0F;
        float luminance = red * 0.2126F + green * 0.7152F + blue * 0.0722F;

        if (hasBlinkReference && isOpenEyePixel(color, blinkColor, luminance)) {
            float eyeLight = 0.48F + luminance * 0.44F;
            return ARGB.color(alpha,
                    channel(eyeLight),
                    channel(0.025F + luminance * 0.085F),
                    channel(0.035F + luminance * 0.070F));
        }

        float whitened;
        if (luminance < 0.10F) {
            whitened = 0.035F + luminance * 0.80F;
        } else {
            whitened = Mth.clamp(0.43F + (float)Math.pow(luminance, 0.72D) * 0.55F, 0.0F, 0.98F);
        }
        float originalChroma = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
        float warmth = Math.min(0.035F, originalChroma * 0.055F);
        return ARGB.color(alpha,
                channel(whitened + warmth),
                channel(whitened - warmth * 0.30F),
                channel(whitened - warmth * 0.62F));
    }

    private static boolean isOpenEyePixel(int open, int blink, float openLuminance) {
        if (ARGB.alpha(blink) == 0) return false;
        int difference = Math.abs(ARGB.red(open) - ARGB.red(blink))
                + Math.abs(ARGB.green(open) - ARGB.green(blink))
                + Math.abs(ARGB.blue(open) - ARGB.blue(blink));
        float blinkLuminance = (ARGB.red(blink) * 0.2126F
                + ARGB.green(blink) * 0.7152F
                + ARGB.blue(blink) * 0.0722F) / 255.0F;
        return difference >= 42 && (openLuminance < 0.42F || Math.abs(openLuminance - blinkLuminance) > 0.18F);
    }

    private static int channel(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }
}
