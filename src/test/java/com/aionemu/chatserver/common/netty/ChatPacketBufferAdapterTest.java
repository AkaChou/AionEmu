package com.aionemu.chatserver.common.netty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class ChatPacketBufferAdapterTest {

    @Test
    void byteBufWriterProducesLittleEndianBytes() {
        ByteBuf byteBuf = Unpooled.buffer(128);

        writeSample(new ByteBufPacketWriter(byteBuf));

        try {
            assertArrayEquals(expectedBytes(), bytes(byteBuf));
        } finally {
            byteBuf.release();
        }
    }

    @Test
    void byteBufReaderReadsLittleEndianValues() {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(expectedBytes());

        try {
            assertSample(new ByteBufPacketReader(byteBuf));
        } finally {
            byteBuf.release();
        }
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

    private static byte[] bytes(ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(0, bytes);
        return bytes;
    }

    private static byte[] expectedBytes() {
        return new byte[] {
            0x11,
            0x33, 0x22,
            0x77, 0x66, 0x55, 0x44,
            (byte) 0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
            0x00, 0x00, 0x60, 0x40,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1D, 0x40,
            0x41, 0x00,
            0x00, 0x00,
            (byte) 0xFE, (byte) 0xDC
        };
    }
}
