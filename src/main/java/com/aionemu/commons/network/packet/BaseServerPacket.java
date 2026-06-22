package com.aionemu.commons.network.packet;

import com.aionemu.commons.utils.PrintUtils;
import java.nio.ByteBuffer;

/**
 * 服务器端数据包基类
 * Base class for server-side packets
 *
 * 提供了向ByteBuffer写入各种数据类型的方法
 * Provides methods for writing various data types to ByteBuffer
 */
public abstract class BaseServerPacket extends BasePacket {
    /**
     * 数据缓冲区
     * Data buffer
     */
    public ByteBuffer buf;

    /**
     * 构造函数
     * Constructor
     *
     * @param opcode 操作码 / Operation code
     */
    protected BaseServerPacket(int opcode) {
        super(BasePacket.PacketType.SERVER, opcode);
    }

    /**
     * 默认构造函数
     * Default constructor
     */
    protected BaseServerPacket() {
        super(BasePacket.PacketType.SERVER);
    }

    /**
     * 设置数据缓冲区
     * Set data buffer
     *
     * @param buf 数据缓冲区 / Data buffer
     */
    public void setBuf(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * 写入32位整数
     * Write 32-bit integer
     *
     * @param value 整数值 / Integer value
     */
    protected final void writeD(int value) {
        this.buf.putInt(value);
    }

    /**
     * 写入16位短整数
     * Write 16-bit short integer
     *
     * @param value 短整数值 / Short integer value
     */
    protected final void writeH(int value) {
        this.buf.putShort((short)value);
    }

    /**
     * 写入8位字节
     * Write 8-bit byte
     *
     * @param value 字节值 / Byte value
     */
    protected final void writeC(int value) {
        this.buf.put((byte)value);
    }

    /**
     * 写入双精度浮点数
     * Write double-precision floating point
     *
     * @param value 双精度浮点数值 / Double value
     */
    protected final void writeDF(double value) {
        this.buf.putDouble(value);
    }

    /**
     * 写入单精度浮点数
     * Write single-precision floating point
     *
     * @param value 单精度浮点数值 / Float value
     */
    protected final void writeF(float value) {
        this.buf.putFloat(value);
    }

    /**
     * 写入64位长整数
     * Write 64-bit long integer
     *
     * @param value 长整数值 / Long value
     */
    protected final void writeQ(long value) {
        this.buf.putLong(value);
    }

    /**
     * 写入字符串
     * Write string
     *
     * @param text 字符串值 / String value
     */
    protected final void writeS(String text) {
      if (text == null) {
         this.buf.putChar('\u0000');
      } else {
         int len = text.length();

         for(int i = 0; i < len; ++i) {
            this.buf.putChar(text.charAt(i));
         }

         this.buf.putChar('\u0000');
      }

   }

    /**
     * 写入字节数组
     * Write byte array
     *
     * @param data 字节数组 / Byte array
     */
    protected final void writeB(byte[] data) {
        this.buf.put(data);
    }

    /**
     * 写入十六进制字符串
     * Write hexadecimal string
     *
     * @param bytes 十六进制字符串 / Hexadecimal string
     */
    protected final void writeB(String bytes) {
        this.writeB(PrintUtils.hex2bytes(bytes));
    }
}
