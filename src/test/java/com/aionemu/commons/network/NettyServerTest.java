package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.nio.ByteBuffer;
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
