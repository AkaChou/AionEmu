package com.aionemu.commons.network.packet;

import com.aionemu.commons.utils.PrintUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 服务端数据包基类，提供向缓冲写入各类型数据的方法。
 * Server-side packet base providing typed writes into a buffer.
 */
public abstract class BaseServerPacket extends BasePacket {

    /**
     * 数据缓冲区。
     * Data buffer.
     */
    public ByteBuffer buf;

    /**
     * 使用操作码构造。
     * Construct with opcode.
     *
     * @param opcode 操作码 / Opcode
     */
    protected BaseServerPacket(int opcode) {
        super(BasePacket.PacketType.SERVER, opcode);
    }

    /**
     * 默认构造。
     * Default constructor.
     */
    protected BaseServerPacket() {
        super(BasePacket.PacketType.SERVER);
    }

    /**
     * 设置写缓冲并强制小端序。
     * Set write buffer and force little-endian order.
     *
     * @param buf 数据缓冲 / Data buffer
     */
    public void setBuf(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        this.buf = buf;
    }

    /**
     * 写入 32 位整数。
     * Write 32-bit integer.
     *
     * @param value 整数值 / Integer value
     */
    protected final void writeD(int value) {
        this.buf.putInt(value);
    }

    /**
     * 写入 16 位短整数。
     * Write 16-bit short.
     *
     * @param value 短整数值 / Short value
     */
    protected final void writeH(int value) {
        this.buf.putShort((short) value);
    }

    /**
     * 写入 8 位字节。
     * Write 8-bit byte.
     *
     * @param value 字节值 / Byte value
     */
    protected final void writeC(int value) {
        this.buf.put((byte) value);
    }

    /**
     * 写入双精度浮点数。
     * Write double-precision float.
     *
     * @param value 双精度值 / Double value
     */
    protected final void writeDF(double value) {
        this.buf.putDouble(value);
    }

    /**
     * 写入单精度浮点数。
     * Write single-precision float.
     *
     * @param value 单精度值 / Float value
     */
    protected final void writeF(float value) {
        this.buf.putFloat(value);
    }

    /**
     * 写入 64 位长整数。
     * Write 64-bit long.
     *
     * @param value 长整数值 / Long value
     */
    protected final void writeQ(long value) {
        this.buf.putLong(value);
    }

    /**
     * 写入 UTF-16LE 字符串（以 \\0 结尾）。
     * Write UTF-16LE string terminated by \\0.
     *
     * @param text 字符串，可为 null / String, may be null
     */
    protected final void writeS(String text) {
        if (text == null) {
            this.buf.putChar('\u0000');
        } else {
            int len = text.length();

            for (int i = 0; i < len; ++i) {
                this.buf.putChar(text.charAt(i));
            }

            this.buf.putChar('\u0000');
        }
    }

    /**
     * 写入字节数组。
     * Write byte array.
     *
     * @param data 字节数组 / Byte array
     */
    protected final void writeB(byte[] data) {
        this.buf.put(data);
    }

    /**
     * 写入十六进制字符串对应的字节。
     * Write bytes from a hexadecimal string.
     *
     * @param bytes 十六进制字符串 / Hexadecimal string
     */
    protected final void writeB(String bytes) {
        this.writeB(PrintUtils.hex2bytes(bytes));
    }
}
