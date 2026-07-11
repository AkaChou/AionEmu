package com.aionemu.chatserver.common.netty;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

/**
 * 基于 Netty {@link ByteBuf} 的数据包写入实现。
 * Packet writer implementation backed by a Netty {@link ByteBuf}.
 */
@RequiredArgsConstructor
public final class ByteBufPacketWriter implements PacketWriter {

    /**
     * 底层缓冲区。
     * Underlying buffer.
     */
    private final ByteBuf buffer;

    /**
     * 返回底层 {@link ByteBuf}。
     * Returns the underlying {@link ByteBuf}.
     *
     * Buffer
     */
    public ByteBuf buffer() {
        return buffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readableBytes() {
        return buffer.readableBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeC(int value) {
        buffer.writeByte(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeH(int value) {
        buffer.writeShortLE(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeD(int value) {
        buffer.writeIntLE(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeQ(long value) {
        buffer.writeLongLE(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeF(float value) {
        buffer.writeFloatLE(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDF(double value) {
        buffer.writeDoubleLE(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char value) {
        buffer.writeShortLE(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeB(byte[] data) {
        buffer.writeBytes(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setH(int index, int value) {
        buffer.setShortLE(index, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBytes(int index, byte[] destination) {
        buffer.getBytes(index, destination);
    }
}
