package com.aionemu.chatserver.common.netty;

/**
 * 网络包读取抽象。
 * Abstraction for reading network packets.
 */
public interface PacketReader {

    /**
     * 返回剩余可读字节数。
     * Returns the number of remaining readable bytes.
     *
     * @return 可读字节数 / Readable byte count
     */
    int readableBytes();

    /**
     * 读取 1 字节无符号值。
     * Reads one unsigned byte.
     *
     * @return 字节值 / Byte value
     */
    int readC();

    /**
     * 读取 2 字节无符号短整型（小端）。
     * Reads one unsigned short in little-endian order.
     *
     * @return 短整数值 / Short value
     */
    int readH();

    /**
     * 读取 4 字节整型（小端）。
     * Reads one int in little-endian order.
     *
     * @return 整数值 / Integer value
     */
    int readD();

    /**
     * 读取 8 字节长整型（小端）。
     * Reads one long in little-endian order.
     *
     * @return 长整数值 / Long value
     */
    long readQ();

    /**
     * 读取 4 字节浮点（小端）。
     * Reads one float in little-endian order.
     *
     * @return 单精度值 / Float value
     */
    float readF();

    /**
     * 读取 8 字节双精度（小端）。
     * Reads one double in little-endian order.
     *
     * @return 双精度值 / Double value
     */
    double readDF();

    /**
     * 读取 2 字节字符（小端）。
     * Reads one character as an unsigned short in little-endian order.
     *
     * @return 字符 / Character
     */
    char readChar();

    /**
     * 读取字节到目标数组。
     * Reads bytes into the destination array.
     *
     * @param destination 目标数组 / Destination array
     */
    void readBytes(byte[] destination);
}
