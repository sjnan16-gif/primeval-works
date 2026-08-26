package com.primevalworks.client.model.block;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import net.minecraft.resources.Identifier;

public final class TurbineGeoModel extends GeoModel<TurbineBlockEntity> {
    public static final DataTicket<Boolean> WIND = DataTicket.create("primevalworks_wind_turbine", Boolean.class);
    public static final DataTicket<Boolean> UPGRADED = DataTicket.create("primevalworks_upgraded_wind_turbine", Boolean.class);
    private static final Identifier WIND_MODEL = asset("block/wind_turbine");
    private static final Identifier WATER_MODEL = asset("block/water_turbine");
    private static final Identifier WIND_TEXTURE = texture("wind_turbine");
    private static final Identifier UPGRADED_WIND_TEXTURE = texture("wind_turbine_upgraded");
    private static final Identifier WATER_TEXTURE = texture("water_turbine");
    private static final Identifier WIND_ANIMATION = asset("block/wind_turbine");
    private static final Identifier WATER_ANIMATION = asset("block/water_turbine");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(WIND, false) ? WIND_MODEL : WATER_MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        if (!renderState.getOrDefaultGeckolibData(WIND, false)) return WATER_TEXTURE;
        return renderState.getOrDefaultGeckolibData(UPGRADED, false)
                ? UPGRADED_WIND_TEXTURE : WIND_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(TurbineBlockEntity turbine) {
        return animationResource(TurbineBlock.isWindTurbine(turbine.getBlockState()));
    }

    static Identifier animationResource(boolean wind) {
        return wind ? WIND_ANIMATION : WATER_ANIMATION;
    }

    private static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "textures/block/" + name + ".png");
    }
}
