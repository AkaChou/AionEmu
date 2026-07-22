package com.aionemu.chatserver.common.netty;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端入站网络包基类。
 * Base class for inbound client network packets.
 */
@Slf4j
public abstract class BaseClientPacket extends AbstractPacket {

    /**
     * 数据包读取器。
     * Packet reader.
     */
    private PacketReader buf;

    /**
     * 使用读取器与操作码创建客户端包。
     * Creates a client packet with the given reader and opcode.
     *
     * @param packetReader 数据包读取器 / Packet reader
     * Opcode
     */
    public BaseClientPacket(PacketReader packetReader, int opCode) {
        super(opCode);
        this.buf = packetReader;
    }

    /**
     * 返回缓冲区中剩余可读字节数。
     * Returns the number of remaining readable bytes in the buffer.
     *
     * @return 剩余字节数 / Remaining bytes
     */
    public int getRemainingBytes() {
        return buf.readableBytes();
    }

    /**
     * 执行数据包读取。
     * Performs packet reading.
     *
     * @return 是否读取成功 / Whether reading succeeded
     */
    public boolean read() {
        try {
            readImpl();
            if (getRemainingBytes() > 0) {
                log.debug("Packet " + this + " not fully readed!");
            }
            return true;
        } catch (Exception ex) {
            log.error(I18n.get("log.909185c9f5d6", this, ex), ex);
            return false;
        }

    }

    /**
     * 执行数据包业务逻辑。
     * Runs the packet business logic.
     */
    public void run() {
        try {
            runImpl();
        } catch (Exception ex) {
            log.error(I18n.get("log.0567970c60e1", this, ex), ex);
        }
    }

    /**
     * 子类实现的读取逻辑。
     * Packet-specific read implementation.
     */
    protected abstract void readImpl();

    /**
     * 子类实现的运行逻辑。
     * Packet-specific run implementation.
     */
    protected abstract void runImpl();

    /**
     * 从缓冲区读取 int。
     * Reads an int from this packet buffer.
     *
     * @return 整数值，失败时返回 0 / Integer value, or 0 on failure
     */
    protected final int readD() {
        try {
            return buf.readD();
        } catch (Exception e) {
            log.error(I18n.get("log.aa48cc356cee", this));
        }
        return 0;
    }

    /**
     * 从缓冲区读取 byte（无符号）。
     * Reads a byte from this packet buffer.
     *
     * @return 字节值，失败时返回 0 / Byte value, or 0 on failure
     */
    protected final int readC() {
        try {
            return buf.readC();
        } catch (Exception e) {
            log.error(I18n.get("log.b44155a94d66", this));
        }
        return 0;
    }

    /**
     * 从缓冲区读取 short（无符号）。
     * Reads a short from this packet buffer.
     *
     * @return 短整数值，失败时返回 0 / Short value, or 0 on failure
     */
    protected final int readH() {
        try {
            return buf.readH();
        } catch (Exception e) {
            log.error(I18n.get("log.40b83b0b1a39", this));
        }
        return 0;
    }

    /**
     * 从缓冲区读取 double。
     * Reads a double from this packet buffer.
     *
     * @return 双精度值，失败时返回 0 / Double value, or 0 on failure
     */
    protected final double readDF() {
        try {
            return buf.readDF();
        } catch (Exception e) {
            log.error(I18n.get("log.e76069d83a06", this));
        }
        return 0;
    }

    /**
     * 从缓冲区读取 float。
     * Reads a float from this packet buffer.
     *
     * @return 单精度值，失败时返回 0 / Float value, or 0 on failure
     */
    protected final float readF() {
        try {
            return buf.readF();
        } catch (Exception e) {
            log.error(I18n.get("log.6a94012b598f", this));
        }
        return 0;
    }

    /**
     * 从缓冲区读取 long。
     * Reads a long from this packet buffer.
     *
     * @return 长整数值，失败时返回 0 / Long value, or 0 on failure
     */
    protected final long readQ() {
        try {
            return buf.readQ();
        } catch (Exception e) {
            log.error(I18n.get("log.38936ac94da3", this));
        }
        return 0;
    }

    /**
     * 从缓冲区读取以 \\0 结尾的字符串。
     * Reads a null-terminated string from this packet buffer.
     *
     * String value
     */
    protected final String readS() {
        StringBuffer sb = new StringBuffer();
        char ch;
        try {
            while ((ch = buf.readChar()) != 0) {
                sb.append(ch);
            }
        } catch (Exception e) {
            log.error(I18n.get("log.ceb21154a1ce", this));
        }
        return sb.toString();

    }

    /**
     * 从缓冲区读取指定长度的字节数组。
     * Reads n bytes from this packet buffer, where n is length.
     *
     * Number of bytes to read
     * Byte array
     */
    protected final byte[] readB(int length) {
        byte[] result = new byte[length];
        try {
            buf.readBytes(result);
        } catch (Exception e) {
            log.error(I18n.get("log.b84a8a529031", this));
        }
        return result;
    }
}
