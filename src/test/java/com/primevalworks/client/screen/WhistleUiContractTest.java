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
    void whistleConfigMatchesTheCommandTableFootprintAndMotionLanguage() throws Exception {
        String config = Files.readString(CLIENT.resolve("screen/DinoWhistleScreen.java"));
        assertTrue(config.contains("PANEL_WIDTH = 195"));
        assertTrue(config.contains("PANEL_HEIGHT = 148"));
        assertTrue(config.contains("PrimevalBubbleUi.spring"));
        assertTrue(config.contains("PANEL_SIZE_MULTIPLIER = 0.88F"));
        assertTrue(config.contains("whistle_range_button.png"));
        assertFalse(config.contains("PANEL_INNER"));
        assertFalse(config.contains("CARD_HOVER"));
    }

    @Test
    void whistleUsesPassiveFirstRowsWithoutRunOnceOrLoopControls() throws Exception {
        String config = Files.readString(CLIENT.resolve("screen/DinoWhistleScreen.java"));
        assertTrue(config.contains("orderRect"));
        assertTrue(config.contains("behaviorRect"));
        assertTrue(config.contains("rangeRect"));
        assertTrue(config.contains("% values.length"));
        assertTrue(config.contains("settings.mode().targetTitle(settings.pattern())"));
        assertTrue(config.contains("filterSlot"));
        assertTrue(config.contains("filteredInventory"));
        assertTrue(config.contains("draggedItem"));
        assertFalse(config.contains("runRect"));
        assertFalse(config.contains("ONCE"));
        assertFalse(config.contains("LOOP"));
        assertFalse(config.contains("modeRect"));
        assertFalse(config.contains("patternRect"));
    }

    @Test
    void inventoryHoverHoldUsesVanillaTooltipAndACompactRightSidebar() throws Exception {
        String client = Files.readString(CLIENT.resolve("effect/DinoWhistleClient.java"));
        String item = Files.readString(Path.of(
                "src/main/java/com/primevalworks/world/item/DinoWhistleItem.java"));
        assertTrue(client.contains("renderInventoryHover"));
        assertTrue(client.contains("renderHud"));
        assertTrue(client.contains("CONFIGURE_HOLD_NANOS"));
        assertTrue(client.contains("classic_work"));
        assertTrue(client.contains("HOLD SHIFT IN INVENTORY TO CONFIGURE"));
        assertTrue(client.contains("InputConstants.KEY_LSHIFT"));
        assertFalse(client.contains("RenderTooltipEvent"));
        assertFalse(item.contains("InteractionResult use("));
        assertFalse(item.contains("finishUsingItem"));
    }

    @Test
    void followerChoiceIsAuthoredSlotsOverTheWorldInsteadOfAnotherPanel() throws Exception {
        String picker = Files.readString(CLIENT.resolve("screen/WhistleFollowerPickerScreen.java"));
        assertTrue(picker.contains("hotbar.png"));
        assertTrue(picker.contains("extractPreview"));
        assertTrue(picker.contains("slotReveal"));
        assertFalse(picker.contains("graphics.fill(0, 0, width, height"));
        assertFalse(picker.contains("PANEL_WIDTH"));
    }

    @Test
    void heldWhistleAlwaysSuppressesVanillaAttacks() throws Exception {
        String client = Files.readString(CLIENT.resolve("effect/DinoWhistleClient.java"));
        int cancel = client.indexOf("event.setCanceled(true)");
        int blockHit = client.indexOf("instanceof BlockHitResult");
        assertTrue(cancel >= 0 && blockHit > cancel,
                "The whistle can punch or swing when the crosshair is not on a block");
        assertTrue(client.contains("DinoFieldWorkRules.validTarget"));
    }

    private static void assertUsesBubble(Path path) throws Exception {
        String source = Files.readString(path);
        assertTrue(source.contains("PrimevalBubbleUi.draw"),
                path.getFileName() + " replaced the authored bubble with a code-drawn panel");
        assertFalse(source.contains("drawDark"),
                path.getFileName() + " covered the authored bubble with a custom dark panel");
        assertFalse(source.contains("drawDarkControl"),
                path.getFileName() + " replaced authored controls with custom cards");
    }
}
