package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class NettyServerTest {

    @Test
    void shutdownDoesNotStopSharedEventLoops() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        NettyEventLoopProvider.useShared(bossGroup, workerGroup);

        try {
            NettyServer server = new NettyServer(new NettyServerCfg("127.0.0.1", 0, "test", TestConnection::new));

            server.connect();
            server.shutdown();

            assertFalse(bossGroup.isShuttingDown());
            assertFalse(workerGroup.isShuttingDown());
        } finally {
            NettyEventLoopProvider.clearShared(bossGroup, workerGroup);
            workerGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Test
    void clientShutdownDoesNotStopSharedEventLoops() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        NettyEventLoopProvider.useShared(bossGroup, workerGroup);

        ExecutorService acceptor = Executors.newSingleThreadExecutor();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<?> accepted = acceptor.submit(() -> {
                try (Socket ignored = serverSocket.accept()) {
                    Thread.sleep(100);
                }
                return null;
            });

            NettyClient client = new NettyClient(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()), "test", TestConnection::new);
            client.connect();
            client.shutdown();
            accepted.get();

            assertFalse(bossGroup.isShuttingDown());
            assertFalse(workerGroup.isShuttingDown());
        } finally {
            acceptor.shutdownNow();
            NettyEventLoopProvider.clearShared(bossGroup, workerGroup);
            workerGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    private static final class TestConnection extends AConnection {

        private TestConnection(ConnectionTransport transport) {
            super(transport, 8, 8);
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
