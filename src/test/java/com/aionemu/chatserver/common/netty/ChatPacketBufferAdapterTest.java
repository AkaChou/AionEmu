package com.aionemu.chatserver.common.netty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.jupiter.api.Test;

class ChatPacketBufferAdapterTest {

    @Test
    void channelBufferAndByteBufWritersProduceSameLittleEndianBytes() {
        ChannelBuffer channelBuffer = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, 128);
        ByteBuf byteBuf = Unpooled.buffer(128);

        writeSample(new ChannelBufferPacketWriter(channelBuffer));
        writeSample(new ByteBufPacketWriter(byteBuf));

        assertArrayEquals(bytes(channelBuffer), bytes(byteBuf));
        byteBuf.release();
    }

    @Test
    void channelBufferAndByteBufReadersReadSameLittleEndianValues() {
        ChannelBuffer channelBuffer = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, 128);
        writeSample(new ChannelBufferPacketWriter(channelBuffer));
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes(channelBuffer));

        assertSample(new ChannelBufferPacketReader(channelBuffer));
        assertSample(new ByteBufPacketReader(byteBuf));
        byteBuf.release();
    }

    private static void writeSample(PacketWriter writer) {
        writer.writeC(0x11);
        writer.writeH(0x2233);
        writer.writeD(0x44556677);
        writer.writeQ(0x1122334455667788L);
        writer.writeF(3.5F);
        writer.writeDF(7.25D);
        writer.writeChar('A');
        writer.writeChar('\0');
        writer.writeB(new byte[] {(byte) 0xFE, (byte) 0xDC});
    }

    private static void assertSample(PacketReader reader) {
        assertEquals(0x11, reader.readC());
        assertEquals(0x2233, reader.readH());
        assertEquals(0x44556677, reader.readD());
        assertEquals(0x1122334455667788L, reader.readQ());
        assertEquals(3.5F, reader.readF());
        assertEquals(7.25D, reader.readDF());
        assertEquals('A', reader.readChar());
        assertEquals('\0', reader.readChar());
        byte[] tail = new byte[2];
        reader.readBytes(tail);
        assertArrayEquals(new byte[] {(byte) 0xFE, (byte) 0xDC}, tail);
        assertEquals(0, reader.readableBytes());
    }

    private static byte[] bytes(ChannelBuffer buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(0, bytes);
        return bytes;
    }

    private static byte[] bytes(ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(0, bytes);
        return bytes;
    }
}
