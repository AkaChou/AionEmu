package com.aionemu.chatserver.network.netty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class NettyServerTest {

    @Test
    void classInitializationDoesNotStartListening() {
        assertDoesNotThrow(() -> Class.forName(
            "com.aionemu.chatserver.network.netty.NettyServer",
            true,
            NettyServerTest.class.getClassLoader()
        ));
    }

    @Test
    void shutdownIfInitializedDoesNotCreateServer() {
        assertFalse(NettyServer.isInitialized());

        NettyServer.shutdownIfInitialized();

        assertFalse(NettyServer.isInitialized());
    }
}
