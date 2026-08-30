package com.primevalworks.release;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class DodoFeatherRemovalContractTest {
    @Test
    void removedDodoFeatherCannotReturnToRuntimeContent() throws Exception {
        Path main = Path.of("src/main");
        try (var paths = Files.walk(main)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String normalizedPath = path.toString().toLowerCase(Locale.ROOT);
                assertFalse(normalizedPath.contains("dodo_feather"), path.toString());
                if (normalizedPath.endsWith(".java") || normalizedPath.endsWith(".json")) {
                    String content = Files.readString(path).toLowerCase(Locale.ROOT);
                    assertFalse(content.contains("dodo_feather"), path.toString());
                    assertFalse(content.contains("dodo feather"), path.toString());
                }
            }
        }
    }
}
