package com.aionemu.chatserver.common.netty;

/**
 * 网络包写入抽象。
 * Abstraction for writing network packets.
 */
public interface PacketWriter {

    /**
     * 返回剩余可读字节数。
     * Returns the number of remaining readable bytes.
     *
     * @return 可读字节数 / Readable byte count
     */
    int readableBytes();

    /**
     * 写入 1 字节。
     * Writes one byte.
     *
     * Byte value
     */
    void writeC(int value);

    /**
     * 写入 2 字节短整型（小端）。
     * Writes one short in little-endian order.
     *
     * @param value 短整数值 / Short value
     */
    void writeH(int value);

    /**
     * 写入 4 字节整型（小端）。
     * Writes one int in little-endian order.
     *
     * Integer value
     */
    void writeD(int value);

    /**
     * 写入 8 字节长整型（小端）。
     * Writes one long in little-endian order.
     *
     * @param value 长整数值 / Long value
     */
    void writeQ(long value);

    /**
     * 写入 4 字节浮点（小端）。
     * Writes one float in little-endian order.
     *
     * @param value 单精度值 / Float value
     */
    void writeF(float value);

    /**
     * 写入 8 字节双精度（小端）。
     * Writes one double in little-endian order.
     *
     * @param value 双精度值 / Double value
     */
    void writeDF(double value);

    /**
     * 写入 2 字节字符（小端）。
     * Writes one character as a short in little-endian order.
     *
     * Character
     */
    void writeChar(char value);

    /**
     * 写入字节数组。
     * Writes a byte array.
     *
     * @param data 字节数据 / Byte data
     */
    void writeB(byte[] data);

    /**
     * 在指定索引处写入 short（小端）。
     * Sets a short at the given index in little-endian order.
     *
     * Index
     * @param value 短整数值 / Short value
     */
    void setH(int index, int value);

    /**
     * 从指定索引复制字节到目标数组。
     * Copies bytes from the given index into the destination array.
     *
     * @param index 起始索引 / Start index
     * Destination array
     */
    void getBytes(int index, byte[] destination);
}
