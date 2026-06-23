package com.aionemu.chatserver.common.netty;

import org.jboss.netty.buffer.ChannelBuffer;

public final class ChannelBufferPacketReader implements PacketReader {

    private final ChannelBuffer buffer;

    public ChannelBufferPacketReader(ChannelBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int readableBytes() {
        return buffer.readableBytes();
    }

    @Override
    public int readC() {
        return buffer.readByte() & 0xFF;
    }

    @Override
    public int readH() {
        return buffer.readShort() & 0xFFFF;
    }

    @Override
    public int readD() {
        return buffer.readInt();
    }

    @Override
    public long readQ() {
        return buffer.readLong();
    }

    @Override
    public float readF() {
        return buffer.readFloat();
    }

    @Override
    public double readDF() {
        return buffer.readDouble();
    }

    @Override
    public char readChar() {
        return buffer.readChar();
    }

    @Override
    public void readBytes(byte[] destination) {
        buffer.readBytes(destination);
    }
}
