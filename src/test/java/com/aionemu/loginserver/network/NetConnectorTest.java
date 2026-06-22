package com.aionemu.loginserver.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class NetConnectorTest {

    @Test
    void shutdownIfInitializedDoesNotCreateTransport() {
        assertFalse(NetConnector.shutdownIfInitialized());
    }
}
