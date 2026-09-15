package com.aionemu.gameserver.network.loginserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.network.loginserver.DirectMemoryLoginChannel.DirectEndpoint;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class DirectMemoryLoginChannelTest {

    @Test
    void deliversLittleEndianFramesToPeer() {
        SenderEndpoint source = new SenderEndpoint(new PortPacket());
        CollectorEndpoint target = new CollectorEndpoint();

        DirectMemoryLoginChannel.pumpWrites(source, target);

        assertEquals(1, target.frames.size());
        assertEquals("game", target.lastDeliveryContext);
        ByteBuffer received = target.frames.get(0);
        assertEquals(ByteOrder.LITTLE_ENDIAN, received.order());
        assertEquals(0x05, Byte.toUnsignedInt(received.get()));
        assertEquals(9014, Short.toUnsignedInt(received.getShort()));
        assertEquals(0x01020304, received.getInt());
    }

    @Test
    void cleansUpEachEndpointInItsOwnServiceContext() {
        SenderEndpoint source = new SenderEndpoint();
        CollectorEndpoint target = new CollectorEndpoint();

        DirectMemoryLoginChannel.cleanupEndpoint(source);
        DirectMemoryLoginChannel.cleanupEndpoint(target);

        assertEquals("login", source.lastCleanupContext);
        assertEquals("game", target.lastCleanupContext);
    }

    @Test
    void deliversMultipleFramesInWriteOrder() {
        SenderEndpoint source = new SenderEndpoint(new PortPacket(), new PortPacket());
        CollectorEndpoint target = new CollectorEndpoint();

        DirectMemoryLoginChannel.pumpWrites(source, target);

        assertEquals(2, target.frames.size());
        for (ByteBuffer received : target.frames) {
            assertEquals(9014, Short.toUnsignedInt(received.getShort(1)));
        }
        assertEquals(0, source.buffer.position());
        assertEquals(source.buffer.capacity(), source.buffer.limit());
    }

    /**
     * 测试用登录服封包：写入端口与整型字段以覆盖多字节端序。
     * Test login packet: writes port and int fields to cover multi-byte endianness.
     */
    private static final class PortPacket extends LsServerPacket {

        private PortPacket() {
            super(0x05);
        }

        @Override
        protected void writeImpl(LoginServerConnection con) {
            writeH(9014);
            writeD(0x01020304);
        }
    }

    /**
     * 复用真实 {@link LsServerPacket#write} 帧编码的写出端。
     * Write side reusing the real {@link LsServerPacket#write} framing.
     */
    private static final class SenderEndpoint implements DirectEndpoint {

        private final Deque<LsServerPacket> pending = new ArrayDeque<>();
        private final ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN).flip();
        private String lastCleanupContext;

        private SenderEndpoint(LsServerPacket... packets) {
            for (LsServerPacket packet : packets) {
                pending.addLast(packet);
            }
        }

        @Override
        public boolean writeDirect(ByteBuffer target) {
            LsServerPacket packet = pending.pollFirst();
            if (packet == null) {
                return false;
            }
            packet.write(null, target);
            return true;
        }

        @Override
        public boolean processDirect(ByteBuffer buffer) {
            return true;
        }

        @Override
        public boolean isWriteEnabled() {
            return true;
        }

        @Override
        public Object syncGuard() {
            return this;
        }

        @Override
        public ByteBuffer writeBuffer() {
            return buffer;
        }

        @Override
        public String serviceContext() {
            return "login";
        }

        @Override
        public void markClosed() {
        }

        @Override
        public void cleanupDirect() {
            lastCleanupContext = ServiceContext.current();
        }
    }

    /**
     * 记录对端收到的载荷视图。
     * Records payload views received by the peer.
     */
    private static final class CollectorEndpoint implements DirectEndpoint {

        private final List<ByteBuffer> frames = new ArrayList<>();
        private String lastDeliveryContext;
        private String lastCleanupContext;

        @Override
        public boolean writeDirect(ByteBuffer buffer) {
            return false;
        }

        @Override
        public boolean processDirect(ByteBuffer buffer) {
            lastDeliveryContext = ServiceContext.current();
            ByteBuffer copy = ByteBuffer.allocate(buffer.remaining()).order(buffer.order());
            copy.put(buffer);
            copy.flip();
            frames.add(copy);
            return true;
        }

        @Override
        public boolean isWriteEnabled() {
            return true;
        }

        @Override
        public Object syncGuard() {
            return this;
        }

        @Override
        public ByteBuffer writeBuffer() {
            return ByteBuffer.allocate(16);
        }

        @Override
        public String serviceContext() {
            return "game";
        }

        @Override
        public void markClosed() {
        }

        @Override
        public void cleanupDirect() {
            lastCleanupContext = ServiceContext.current();
        }
    }
}
