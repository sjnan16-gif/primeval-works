package com.primevalworks.release;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class DartRemovalContractTest {
    @Test
    void removedDartContentCannotReturnToTheRuntime() throws Exception {
        Path main = Path.of("src/main");
        try (var paths = Files.walk(main)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                assertFalse(path.toString().toLowerCase(Locale.ROOT).contains("dart"), path.toString());
                if (path.toString().endsWith(".java") || path.toString().endsWith(".json")) {
                    assertFalse(Files.readString(path).toLowerCase(Locale.ROOT).contains("dart"), path.toString());
                }
            }
        }
    }
}
