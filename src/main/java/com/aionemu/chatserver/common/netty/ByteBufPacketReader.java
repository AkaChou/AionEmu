package com.aionemu.chatserver.common.netty;

import io.netty.buffer.ByteBuf;

public final class ByteBufPacketReader implements PacketReader {

    private final ByteBuf buffer;

    public ByteBufPacketReader(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public int readableBytes() {
        return buffer.readableBytes();
    }

    @Override
    public int readC() {
        return buffer.readUnsignedByte();
    }

    @Override
    public int readH() {
        return buffer.readUnsignedShortLE();
    }

    @Override
    public int readD() {
        return buffer.readIntLE();
    }

    @Override
    public long readQ() {
        return buffer.readLongLE();
    }

    @Override
    public float readF() {
        return buffer.readFloatLE();
    }

    @Override
    public double readDF() {
        return buffer.readDoubleLE();
    }

    @Override
    public char readChar() {
        return (char) buffer.readUnsignedShortLE();
    }

    @Override
    public void readBytes(byte[] destination) {
        buffer.readBytes(destination);
    }
}
