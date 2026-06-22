package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;

class NioServerTest {

    @Test
    void shutdownReleasesListeningPortAndStopsAcceptDispatcher() throws Exception {
        int port = findFreePort();
        NioServer server = new NioServer(1, new ServerCfg("127.0.0.1", port, "test", TestConnection::new));

        server.connect();
        Dispatcher acceptDispatcher = server.getAcceptDispatcher();

        server.shutdown();

        assertFalse(acceptDispatcher.isAlive());
        assertDoesNotThrow(() -> bind(port));
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void bind(int port) throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", port));
        }
    }

    private static final class TestConnection extends AConnection {

        private TestConnection(SocketChannel sc, Dispatcher d) throws IOException {
            super(sc, d, 8, 8);
        }

        @Override
        protected boolean processData(ByteBuffer buf) {
            return true;
        }

        @Override
        protected boolean writeData(ByteBuffer buf) {
            return false;
        }

        @Override
        protected void initialized() {
        }

        @Override
        protected long getDisconnectionDelay() {
            return 0;
        }

        @Override
        protected void onDisconnect() {
        }

        @Override
        protected void onServerClose() {
        }
    }
}
