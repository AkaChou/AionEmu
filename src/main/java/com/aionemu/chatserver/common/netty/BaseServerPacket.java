package com.aionemu.chatserver.common.netty;

/**
 * 服务端出站网络包基类。
 * Base class for outbound server network packets.
 *
 * @author ATracer
 */
public abstract class BaseServerPacket extends AbstractPacket {

    /**
     * 使用操作码创建服务端包。
     * Creates a server packet with the given opcode.
     *
     * @param opCode 操作码 / Opcode
     */
    public BaseServerPacket(int opCode) {
        super(opCode);
    }

    /**
     * 向缓冲区写入 int。
     * Writes an int to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param value 整数值 / Integer value
     */
    protected final void writeD(PacketWriter buf, int value) {
        buf.writeD(value);
    }

    /**
     * 向缓冲区写入 short。
     * Writes a short to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param value 短整数值 / Short value
     */
    protected final void writeH(PacketWriter buf, int value) {
        buf.writeH(value);
    }

    /**
     * 向缓冲区写入 byte。
     * Writes a byte to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param value 字节值 / Byte value
     */
    protected final void writeC(PacketWriter buf, int value) {
        buf.writeC(value);
    }

    /**
     * 向缓冲区写入 double。
     * Writes a double to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param value 双精度值 / Double value
     */
    protected final void writeDF(PacketWriter buf, double value) {
        buf.writeDF(value);
    }

    /**
     * 向缓冲区写入 float。
     * Writes a float to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param value 单精度值 / Float value
     */
    protected final void writeF(PacketWriter buf, float value) {
        buf.writeF(value);
    }

    /**
     * 向缓冲区写入字节数组。
     * Writes a byte array to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param data 字节数据 / Byte data
     */
    protected final void writeB(PacketWriter buf, byte[] data) {
        buf.writeB(data);
    }

    /**
     * 向缓冲区写入以 \\0 结尾的字符串。
     * Writes a null-terminated string to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param text 文本内容，可为 null / Text content, may be null
     */
    protected final void writeS(PacketWriter buf, String text) {
        if (text == null) {
            buf.writeChar('\000');
        } else {
            final int len = text.length();
            for (int i = 0; i < len; i++) {
                buf.writeChar(text.charAt(i));
            }
            buf.writeChar('\000');
        }
    }

    /**
     * 向缓冲区写入 long。
     * Writes a long to the buffer.
     *
     * @param buf 数据包写入器 / Packet writer
     * @param data 长整数值 / Long value
     */
    protected final void writeQ(PacketWriter buf, long data) {
        buf.writeQ(data);
    }
}
