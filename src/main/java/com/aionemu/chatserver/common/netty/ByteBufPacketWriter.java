package com.aionemu.chatserver.common.netty;

import io.netty.buffer.ByteBuf;

public final class ByteBufPacketWriter implements PacketWriter {

    private final ByteBuf buffer;

    public ByteBufPacketWriter(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public ByteBuf buffer() {
        return buffer;
    }

    @Override
    public int readableBytes() {
        return buffer.readableBytes();
    }

    @Override
    public void writeC(int value) {
        buffer.writeByte(value);
    }

    @Override
    public void writeH(int value) {
        buffer.writeShortLE(value);
    }

    @Override
    public void writeD(int value) {
        buffer.writeIntLE(value);
    }

    @Override
    public void writeQ(long value) {
        buffer.writeLongLE(value);
    }

    @Override
    public void writeF(float value) {
        buffer.writeFloatLE(value);
    }

    @Override
    public void writeDF(double value) {
        buffer.writeDoubleLE(value);
    }

    @Override
    public void writeChar(char value) {
        buffer.writeShortLE(value);
    }

    @Override
    public void writeB(byte[] data) {
        buffer.writeBytes(data);
    }

    @Override
    public void setH(int index, int value) {
        buffer.setShortLE(index, value);
    }

    @Override
    public void getBytes(int index, byte[] destination) {
        buffer.getBytes(index, destination);
    }
}
