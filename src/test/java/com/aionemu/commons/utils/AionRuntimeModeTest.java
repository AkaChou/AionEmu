package com.aionemu.commons.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AionRuntimeModeTest {

    @AfterEach
    void clearEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
    }

    @Test
    void defaultsToStandaloneMode() {
        assertFalse(AionRuntimeMode.isBootEmbedded());
    }

    @Test
    void enablesBootEmbeddedMode() {
        AionRuntimeMode.enableBootEmbeddedMode();

        assertTrue(AionRuntimeMode.isBootEmbedded());
    }
}
