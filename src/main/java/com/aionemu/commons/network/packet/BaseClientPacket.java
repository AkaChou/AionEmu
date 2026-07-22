package com.aionemu.commons.network.packet;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.AConnection;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端数据包基类，提供缓冲读取与处理流程。
 * Client-side packet base providing buffer reads and processing flow.
 *
 * @param <T> 连接类型 / Connection type
 */
@Slf4j
public abstract class BaseClientPacket<T extends AConnection> extends BasePacket implements Runnable {

    /**
     * 客户端连接实例。
     * Client connection instance.
     */
    private T client;

    /**
     * 数据缓冲区。
     * Data buffer.
     */
    private ByteBuffer buf;

    /**
     * 使用缓冲与操作码构造。
     * Construct with buffer and opcode.
     *
     * @param buf 数据缓冲 / Data buffer
     * Opcode
     */
    public BaseClientPacket(ByteBuffer buf, int opcode) {
        this(opcode);
        this.buf = buf;
    }

    /**
     * 使用操作码构造。
     * Construct with opcode.
     *
     * Opcode
     */
    public BaseClientPacket(int opcode) {
        super(BasePacket.PacketType.CLIENT, opcode);
    }

    /**
     * 设置数据缓冲。
     * Set data buffer.
     *
     * @param buf 数据缓冲 / Data buffer
     */
    public void setBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * 设置客户端连接。
     * Set client connection.
     *
     * @param client 客户端连接 / Client connection
     */
    public void setConnection(T client) {
        this.client = client;
    }

    /**
     * 读取数据包内容。
     * Read packet content.
     *
     * @return 是否读取成功 / Whether reading succeeded
     */
    public final boolean read() {
        try {
            this.readImpl();
            if (this.getRemainingBytes() > 0) {
                log.debug("Packet " + this + " not fully readed!");
            }

            return true;
        } catch (Exception var2) {
            log.error(I18n.get("log.909185c9f5d6", this, var2), var2);
            return false;
        }
    }

    /**
     * 实现具体读取逻辑。
     * Implement concrete reading logic.
     */
    protected abstract void readImpl();

    /**
     * 获取剩余可读字节数。
     * Get remaining readable bytes.
     *
     * @return 剩余字节数 / Remaining bytes
     */
    public final int getRemainingBytes() {
        return this.buf.remaining();
    }

    /**
     * 读取 32 位整数。
     * Read 32-bit integer.
     *
     * @return 整数值，失败返回 0 / Integer value, 0 on failure
     */
    protected final int readD() {
        try {
            return this.buf.getInt();
        } catch (Exception var2) {
            log.error(I18n.get("log.aa48cc356cee", this));
            return 0;
        }
    }

    /**
     * 读取 8 位无符号字节。
     * Read 8-bit unsigned byte.
     *
     * @return 字节值，失败返回 0 / Byte value, 0 on failure
     */
    protected final int readC() {
        try {
            return this.buf.get() & 255;
        } catch (Exception var2) {
            log.error(I18n.get("log.b44155a94d66", this));
            return 0;
        }
    }

    /**
     * 读取 8 位有符号字节。
     * Read 8-bit signed byte.
     *
     * @return 字节值，失败返回 0 / Byte value, 0 on failure
     */
    protected final byte readSC() {
        try {
            return this.buf.get();
        } catch (Exception var2) {
            log.error(I18n.get("log.b44155a94d66", this));
            return 0;
        }
    }

    /**
     * 读取 16 位有符号短整数。
     * Read 16-bit signed short.
     *
     * @return 短整数值，失败返回 0 / Short value, 0 on failure
     */
    protected final short readSH() {
        try {
            return this.buf.getShort();
        } catch (Exception var2) {
            log.error(I18n.get("log.40b83b0b1a39", this));
            return 0;
        }
    }

    /**
     * 读取 16 位无符号短整数。
     * Read 16-bit unsigned short.
     *
     * @return 短整数值，失败返回 0 / Short value, 0 on failure
     */
    protected final int readH() {
        try {
            return this.buf.getShort() & 0xffff;
        } catch (Exception var2) {
            log.error(I18n.get("log.40b83b0b1a39", this));
            return 0;
        }
    }

    /**
     * 读取双精度浮点数。
     * Read double-precision float.
     *
     * @return 双精度值，失败返回 0 / Double value, 0 on failure
     */
    protected final double readDF() {
        try {
            return this.buf.getDouble();
        } catch (Exception var2) {
            log.error(I18n.get("log.e76069d83a06", this));
            return 0.0D;
        }
    }

    /**
     * 读取单精度浮点数。
     * Read single-precision float.
     *
     * @return 单精度值，失败返回 0 / Float value, 0 on failure
     */
    protected final float readF() {
        try {
            return this.buf.getFloat();
        } catch (Exception var2) {
            log.error(I18n.get("log.6a94012b598f", this));
            return 0.0F;
        }
    }

    /**
     * 读取 64 位长整数。
     * Read 64-bit long.
     *
     * @return 长整数值，失败返回 0 / Long value, 0 on failure
     */
    protected final long readQ() {
        try {
            return this.buf.getLong();
        } catch (Exception var2) {
            log.error(I18n.get("log.38936ac94da3", this));
            return 0L;
        }
    }

    /**
     * 读取 UTF-16LE 字符串（以 \\0 结尾）。
     * Read UTF-16LE string terminated by \\0.
     *
     * String value
     */
    protected final String readS() {
        StringBuffer sb = new StringBuffer();

        char ch;
        try {
            while ((ch = this.buf.getChar()) != 0) {
                sb.append(ch);
            }
        } catch (Exception var4) {
            log.error(I18n.get("log.ceb21154a1ce", this));
        }

        return sb.toString();
    }

    /**
     * 读取指定长度字节数组。
     * Read byte array of given length.
     *
     * Byte length
     * Byte array
     */
    protected final byte[] readB(int length) {
        byte[] result = new byte[length];

        try {
            this.buf.get(result);
        } catch (Exception var4) {
            log.error(I18n.get("log.b84a8a529031", this));
        }

        return result;
    }

    /**
     * 按十六进制模板长度读取字节数组。
     * Read byte array sized by hexadecimal template length.
     *
     * @param string 十六进制模板 / Hexadecimal template
     * Byte array
     */
    protected final byte[] readB(String string) {
        String finalString = string.replaceAll("\\s+", "");
        byte[] bytes = new byte[finalString.length() / 2];

        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) Integer.parseInt(finalString.substring(2 * i, 2 * i + 2), 16);
        }

        try {
            this.buf.get(bytes);
        } catch (Exception var5) {
            log.error(I18n.get("log.b84a8a529031", this));
        }

        return bytes;
    }

    /**
     * 实现具体运行逻辑。
     * Implement concrete run logic.
     */
    protected abstract void runImpl();

    /**
     * 获取客户端连接。
     * Get client connection.
     *
     * @return 客户端连接 / Client connection
     */
    public final T getConnection() {
        return this.client;
    }
}
