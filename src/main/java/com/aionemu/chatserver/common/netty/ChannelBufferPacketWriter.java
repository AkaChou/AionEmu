package com.aionemu.chatserver.common.netty;

import org.jboss.netty.buffer.ChannelBuffer;

public final class ChannelBufferPacketWriter implements PacketWriter {

    private final ChannelBuffer buffer;

    public ChannelBufferPacketWriter(ChannelBuffer buffer) {
        this.buffer = buffer;
    }

    public ChannelBuffer buffer() {
        return buffer;
    }

    @Override
    public int readableBytes() {
        return buffer.readableBytes();
    }

    @Override
    public void writeC(int value) {
        buffer.writeByte((byte) value);
    }

    @Override
    public void writeH(int value) {
        buffer.writeShort((short) value);
    }

    @Override
    public void writeD(int value) {
        buffer.writeInt(value);
    }

    @Override
    public void writeQ(long value) {
        buffer.writeLong(value);
    }

    @Override
    public void writeF(float value) {
        buffer.writeFloat(value);
    }

    @Override
    public void writeDF(double value) {
        buffer.writeDouble(value);
    }

    @Override
    public void writeChar(char value) {
        buffer.writeChar(value);
    }

    @Override
    public void writeB(byte[] data) {
        buffer.writeBytes(data);
    }

    @Override
    public void setH(int index, int value) {
        buffer.setShort(index, (short) value);
    }

    @Override
    public void getBytes(int index, byte[] destination) {
        buffer.getBytes(index, destination);
    }
}
