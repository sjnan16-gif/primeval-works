package com.primevalworks.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class FieldDodoModel extends DefaultedEntityGeoModel<FieldDodoEntity> {
    public static final DataTicket<Boolean> EYES_CLOSED =
            DataTicket.create("primevalworks_eyes_closed", Boolean.class);
    public static final DataTicket<Boolean> SADDLED =
            DataTicket.create("primevalworks_saddled", Boolean.class);
    public static final DataTicket<Boolean> AQUATIC_MOUNT =
            DataTicket.create("primevalworks_aquatic_mount", Boolean.class);
    public static final DataTicket<Boolean> ALBINO =
            DataTicket.create("primevalworks_albino", Boolean.class);
    private final Identifier texture;
    private final Identifier blinkTexture;
    private final Identifier saddledTexture;
    private final Identifier saddledBlinkTexture;
    private final Identifier saddledAquaticTexture;
    private final Identifier saddledAquaticBlinkTexture;

    public FieldDodoModel(DinosaurVisualProfile profile) {
        super(Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, profile.assetName()));
        texture = profile.texture();
        blinkTexture = profile.blinkTexture();
        saddledTexture = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID,
                "textures/entity/" + profile.assetName() + "_saddled.png");
        saddledBlinkTexture = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID,
                "textures/entity/" + profile.assetName() + "_saddled_blink.png");
        saddledAquaticTexture = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID,
                "textures/entity/" + profile.assetName() + "_saddled_aquatic.png");
        saddledAquaticBlinkTexture = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID,
                "textures/entity/" + profile.assetName() + "_saddled_aquatic_blink.png");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        boolean saddled = renderState.getOrDefaultGeckolibData(SADDLED, false);
        boolean blinking = renderState.getOrDefaultGeckolibData(EYES_CLOSED, false);
        boolean aquaticMount = renderState.getOrDefaultGeckolibData(AQUATIC_MOUNT, false);
        Identifier selected;
        if (saddled && aquaticMount && blinking
                && Minecraft.getInstance().getResourceManager().getResource(saddledAquaticBlinkTexture).isPresent()) {
            selected = saddledAquaticBlinkTexture;
        } else if (saddled && aquaticMount
                && Minecraft.getInstance().getResourceManager().getResource(saddledAquaticTexture).isPresent()) {
            selected = saddledAquaticTexture;
        } else if (saddled && blinking
                && Minecraft.getInstance().getResourceManager().getResource(saddledBlinkTexture).isPresent()) {
            selected = saddledBlinkTexture;
        } else if (saddled && Minecraft.getInstance().getResourceManager().getResource(saddledTexture).isPresent()) {
            selected = saddledTexture;
        } else if (blinking
                && Minecraft.getInstance().getResourceManager().getResource(blinkTexture).isPresent()) {
            selected = blinkTexture;
        } else {
            selected = texture;
        }
        return renderState.getOrDefaultGeckolibData(ALBINO, false)
                ? com.primevalworks.client.render.entity.AlbinoTextureManager.textureFor(selected)
                : selected;
    }
}
