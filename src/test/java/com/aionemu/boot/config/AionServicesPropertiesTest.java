package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import org.junit.jupiter.api.Test;

class AionServicesPropertiesTest {

    @Test
    void defaultsToSingleBootNettyRuntimeWithOptionalChat() {
        AionServicesProperties properties = new AionServicesProperties();

        assertTrue(properties.getLogin().isEnabled());
        assertTrue(properties.getGame().isEnabled());
        assertFalse(properties.getChat().isEnabled());
        assertSame(TransportMode.NETTY, properties.getTransport().getMode());
    }
}
