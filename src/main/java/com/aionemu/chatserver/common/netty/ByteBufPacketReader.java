package com.aionemu.chatserver.common.netty;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

/**
 * 基于 Netty {@link ByteBuf} 的数据包读取实现。
 * Packet reader implementation backed by a Netty {@link ByteBuf}.
 */
@RequiredArgsConstructor
public final class ByteBufPacketReader implements PacketReader {

    /**
     * 底层缓冲区。
     * Underlying buffer.
     */
    private final ByteBuf buffer;

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
    public int readC() {
        return buffer.readUnsignedByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readH() {
        return buffer.readUnsignedShortLE();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readD() {
        return buffer.readIntLE();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readQ() {
        return buffer.readLongLE();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readF() {
        return buffer.readFloatLE();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDF() {
        return buffer.readDoubleLE();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        return (char) buffer.readUnsignedShortLE();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(byte[] destination) {
        buffer.readBytes(destination);
    }
}
