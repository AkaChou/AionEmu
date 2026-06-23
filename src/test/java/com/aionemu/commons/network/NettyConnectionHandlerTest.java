package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class NettyConnectionHandlerTest {

    @Test
    void parsesSplitAndCoalescedFrames() {
        Holder holder = new Holder();
        EmbeddedChannel channel = channel(holder);

        ByteBuf split = frame(new byte[] {1, 2, 3});
        channel.writeInbound(split.readRetainedSlice(3));
        assertEquals(0, holder.connection.received.size());
        channel.writeInbound(split);

        ByteBuf coalesced = Unpooled.buffer();
        coalesced.writeBytes(frame(new byte[] {4}));
        coalesced.writeBytes(frame(new byte[] {5, 6}));
        channel.writeInbound(coalesced);

        assertEquals(ByteOrder.LITTLE_ENDIAN, holder.connection.lastOrder);
        assertArrayEquals(new byte[] {1, 2, 3}, holder.connection.received.get(0));
        assertArrayEquals(new byte[] {4}, holder.connection.received.get(1));
        assertArrayEquals(new byte[] {5, 6}, holder.connection.received.get(2));

        channel.finishAndReleaseAll();
    }

    @Test
    void flushesQueuedFrames() {
        Holder holder = new Holder();
        EmbeddedChannel channel = channel(holder);

        holder.connection.enqueue(new byte[] {7, 8});
        channel.runPendingTasks();

        ByteBuf outbound = channel.readOutbound();
        assertNotNull(outbound);
        assertEquals(4, outbound.readShortLE());
        assertEquals(7, outbound.readByte());
        assertEquals(8, outbound.readByte());
        outbound.release();

        channel.finishAndReleaseAll();
    }

    @Test
    void notifiesDisconnectOnce() {
        Holder holder = new Holder();
        EmbeddedChannel channel = channel(holder);

        channel.close().syncUninterruptibly();
        channel.close().syncUninterruptibly();

        assertEquals(1, holder.connection.disconnects);
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

    private static ByteBuf frame(byte[] payload) {
        ByteBuf frame = Unpooled.buffer(Short.BYTES + payload.length);
        frame.writeShortLE(Short.BYTES + payload.length);
        frame.writeBytes(payload);
        return frame;
    }

    private static final class Holder {
        private TestConnection connection;
    }

    private static final class TestConnection extends AConnection {
        private final List<byte[]> received = new ArrayList<>();
        private final Deque<byte[]> queued = new ArrayDeque<>();
        private ByteOrder lastOrder;
        private int disconnects;

        private TestConnection(ConnectionTransport transport) {
            super(transport, 16, 16);
        }

        private void enqueue(byte[] payload) {
            queued.addLast(payload);
            enableWriteInterest();
        }

        @Override
        protected boolean processData(ByteBuffer buf) {
            lastOrder = buf.order();
            byte[] payload = new byte[buf.remaining()];
            buf.get(payload);
            received.add(payload);
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
