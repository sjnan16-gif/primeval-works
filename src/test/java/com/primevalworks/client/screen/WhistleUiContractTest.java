package com.primevalworks.client.screen;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WhistleUiContractTest {
    private static final Path CLIENT = Path.of("src/main/java/com/primevalworks/client");

    @Test
    void everyWhistleSurfaceUsesTheAuthoredSpaceBubble() throws Exception {
        BufferedImage space = ImageIO.read(Path.of(
                "src/main/resources/assets/primevalworks/textures/gui/space.png").toFile());
        assertEquals(86, space.getWidth());
        assertEquals(14, space.getHeight());

        assertUsesBubble(CLIENT.resolve("screen/DinoWhistleScreen.java"));
        assertUsesBubble(CLIENT.resolve("screen/WhistleFollowerPickerScreen.java"));
        assertUsesBubble(CLIENT.resolve("effect/DinoWhistleClient.java"));
    }

    @Test
    void whistleOpeningKeepsTheCompanionScreenMotionLanguage() throws Exception {
        String config = Files.readString(CLIENT.resolve("screen/DinoWhistleScreen.java"));
        String picker = Files.readString(CLIENT.resolve("screen/WhistleFollowerPickerScreen.java"));
        for (String source : new String[] {config, picker}) {
            assertTrue(source.contains("PrimevalBubbleUi.spring"));
            assertTrue(source.contains("parallaxX"));
            assertTrue(source.contains("graphics.pose().translate"));
            assertFalse(source.contains("PANEL_INNER"));
            assertFalse(source.contains("CARD_HOVER"));
        }
    }

    private static void assertUsesBubble(Path path) throws Exception {
        String source = Files.readString(path);
        assertTrue(source.contains("PrimevalBubbleUi.draw"),
                path.getFileName() + " replaced the authored bubble with a code-drawn panel");
    }
}
