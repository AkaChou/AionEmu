package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.services.ServiceContext;
import io.netty.channel.EventLoopGroup;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NettyServerTest {

    @Test
    void shutdownDoesNotStopSharedEventLoops() {
        EventLoopGroup bossGroup = NettyEventLoopProvider.newBossGroup();
        EventLoopGroup workerGroup = NettyEventLoopProvider.newWorkerGroup();
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
    void acceptedConnectionsUseServiceContextCapturedAtConnect() throws Exception {
        EventLoopGroup bossGroup = NettyEventLoopProvider.newBossGroup();
        EventLoopGroup workerGroup = NettyEventLoopProvider.newWorkerGroup();
        warmUp(bossGroup);
        warmUp(workerGroup);
        NettyEventLoopProvider.useShared(bossGroup, workerGroup);

        int port = freePort();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<String> observedContext = new AtomicReference<>();
        NettyServer server = new NettyServer(new NettyServerCfg("127.0.0.1", port, "test", transport -> {
            TestConnection connection = new TestConnection(transport);
            observedContext.set(connection.getServiceContext());
            connected.countDown();
            return connection;
        }));

        try {
            try (ServiceContext.Scope ignored = ServiceContext.use("login")) {
                server.connect();
            }

            try (Socket ignored = new Socket("127.0.0.1", port)) {
                assertTrue(connected.await(2, TimeUnit.SECONDS));
            }

            assertEquals("login", observedContext.get());
        } finally {
            server.shutdown();
            NettyEventLoopProvider.clearShared(bossGroup, workerGroup);
            workerGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Test
    void clientShutdownDoesNotStopSharedEventLoops() throws Exception {
        EventLoopGroup bossGroup = NettyEventLoopProvider.newBossGroup();
        EventLoopGroup workerGroup = NettyEventLoopProvider.newWorkerGroup();
        NettyEventLoopProvider.useShared(bossGroup, workerGroup);

        ExecutorService acceptor = Executors.newSingleThreadExecutor();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<?> accepted = acceptor.submit(() -> {
                try (Socket ignored = serverSocket.accept()) {
                    Thread.sleep(100);
                }
                return null;
            });

            NettyClient client = new NettyClient(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()), "test", TestConnection::new, Runnable::run);
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

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void warmUp(EventLoopGroup group) {
        for (io.netty.util.concurrent.EventExecutor executor : group) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                }
            }).syncUninterruptibly();
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
