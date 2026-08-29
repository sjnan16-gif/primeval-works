package com.primevalworks.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.primevalworks.PrimevalWorks;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class IrisPipelineCompat {
    private IrisPipelineCompat() {
    }

    public static void registerLines(RenderPipeline pipeline) {
        if (!ModList.get().isLoaded("iris")) return;
        try {
            Class<?> apiType = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Class<?> programType = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object lines = enumConstant(programType, "LINES");
            Object api = apiType.getMethod("getInstance").invoke(null);
            Method assignPipeline = apiType.getMethod("assignPipeline", RenderPipeline.class, programType);
            assignPipeline.invoke(api, pipeline, lines);
            PrimevalWorks.LOGGER.info("Registered world highlights with Iris");
        } catch (ReflectiveOperationException | LinkageError exception) {
            PrimevalWorks.LOGGER.warn("Could not register the world-highlight pipeline with Iris", exception);
        }
    }

    private static Object enumConstant(Class<?> type, String name) throws ReflectiveOperationException {
        Object[] constants = type.getEnumConstants();
        if (constants != null) {
            for (Object constant : constants) {
                if (constant instanceof Enum<?> value && value.name().equals(name)) return constant;
            }
        }
        throw new ReflectiveOperationException("Missing Iris program " + name);
    }
}
