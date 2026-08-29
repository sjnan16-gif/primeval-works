package com.primevalworks.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class HighlightPipelineCompatibilityTest {
    @Test
    void sharedWorldHighlightPipelineIsMappedForIris() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/primevalworks/client/PrimevalWorksClient.java"));
        String compatibility = Files.readString(Path.of(
                "src/main/java/com/primevalworks/client/render/IrisPipelineCompat.java"));

        assertTrue(client.contains("registerRenderPipelines"));
        assertTrue(client.contains("IrisPipelineCompat.registerLines"));
        assertTrue(compatibility.contains("net.irisshaders.iris.api.v0.IrisApi"));
        assertTrue(compatibility.contains("assignPipeline"));
        assertTrue(compatibility.contains("\"LINES\""));
    }
}
