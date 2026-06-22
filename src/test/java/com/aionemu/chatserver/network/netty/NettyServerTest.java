package com.aionemu.chatserver.network.netty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
}
