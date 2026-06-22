package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;

class NettyConnectionPendingCloseTest {

    @Test
    void pendingCloseFlushesCloseFrameBeforeDisconnecting() {
        Holder holder = new Holder();
        EmbeddedChannel channel = channel(holder);

        holder.connection.enqueue(new byte[] {1, 2});
        holder.connection.closeAfter(new byte[] {9, 10}, true);

        assertTrue(channel.isOpen());
        assertEquals(0, holder.connection.disconnects);
        assertNull(channel.readOutbound());

        channel.runPendingTasks();

        ByteBuf outbound = channel.readOutbound();
        assertNotNull(outbound);
        assertEquals(4, outbound.readShortLE());
        assertEquals(9, outbound.readByte());
        assertEquals(10, outbound.readByte());
        outbound.release();

        assertFalse(channel.isOpen());
        assertEquals(1, holder.connection.disconnects);

        channel.close().syncUninterruptibly();
        assertEquals(1, holder.connection.disconnects);

        channel.finishAndReleaseAll();
    }

    private static EmbeddedChannel channel(Holder holder) {
        NettyConnectionHandler handler = new NettyConnectionHandler(transport -> {
            holder.connection = new TestConnection(transport);
            return holder.connection;
        }, Runnable::run);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        if (holder.connection == null) {
            channel.pipeline().fireChannelActive();
        }
        assertNotNull(holder.connection);
        return channel;
    }

    private static final class Holder {
        private TestConnection connection;
    }

    private static final class TestConnection extends AConnection {
        private final Deque<byte[]> queued = new ArrayDeque<>();
        private int disconnects;

        private TestConnection(ConnectionTransport transport) {
            super(transport, 16, 16);
        }

        private void enqueue(byte[] payload) {
            synchronized (guard) {
                if (isWriteDisabled()) {
                    return;
                }
                queued.addLast(payload);
                enableWriteInterest();
            }
        }

        private void closeAfter(byte[] payload, boolean forced) {
            synchronized (guard) {
                if (isWriteDisabled()) {
                    return;
                }
                pendingClose = true;
                isForcedClosing = forced;
                queued.clear();
                queued.addLast(payload);
                enableWriteInterest();
            }
        }

        @Override
        protected boolean processData(ByteBuffer buf) {
            return true;
        }

        @Override
        protected boolean writeData(ByteBuffer buf) {
            byte[] payload = queued.pollFirst();
            if (payload == null) {
                return false;
            }
            buf.putShort((short) 0);
            buf.put(payload);
            buf.flip();
            buf.putShort((short) buf.limit());
            buf.position(0);
            return true;
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
            disconnects++;
        }

        @Override
        protected void onServerClose() {
        }
    }
}
