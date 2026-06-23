package com.aionemu.commons.network.packet;

import com.aionemu.commons.network.AConnection;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端数据包基类
 * Base class for client-side packets
 *
 * 提供了从ByteBuffer读取各种数据类型的方法，并实现了基本的数据包处理流程
 * Provides methods for reading various data types from ByteBuffer and implements basic packet processing flow
 *
 * @param <T> 连接类型 / Connection type
 */
public abstract class BaseClientPacket<T extends AConnection> extends BasePacket implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BaseClientPacket.class);
    
    /**
     * 客户端连接实例
     * Client connection instance
     */
    private T client;
    
    /**
     * 数据缓冲区
     * Data buffer
     */
    private ByteBuffer buf;

    /**
     * 构造函数
     * Constructor
     *
     * @param buf 数据缓冲区 / Data buffer
     * @param opcode 操作码 / Operation code
     */
    public BaseClientPacket(ByteBuffer buf, int opcode) {
        this(opcode);
        this.buf = buf;
    }

    /**
     * 构造函数
     * Constructor
     *
     * @param opcode 操作码 / Operation code
     */
    public BaseClientPacket(int opcode) {
        super(BasePacket.PacketType.CLIENT, opcode);
    }

    /**
     * 设置数据缓冲区
     * Set data buffer
     *
     * @param buf 数据缓冲区 / Data buffer
     */
    public void setBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * 设置客户端连接
     * Set client connection
     *
     * @param client 客户端连接 / Client connection
     */
    public void setConnection(T client) {
        this.client = client;
    }

    /**
     * 读取数据包内容
     * Read packet content
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
            log.error("Reading failed for packet " + this, var2);
            return false;
        }
    }

    /**
     * 实现具体的读取逻辑
     * Implement specific reading logic
     */
    protected abstract void readImpl();

    /**
     * 获取剩余字节数
     * Get remaining bytes
     *
     * @return 剩余字节数 / Number of remaining bytes
     */
    public final int getRemainingBytes() {
        return this.buf.remaining();
    }

    /**
     * 读取32位整数
     * Read 32-bit integer
     *
     * @return 整数值 / Integer value
     */
    protected final int readD() {
        try {
            return this.buf.getInt();
        } catch (Exception var2) {
            log.error("Missing D for: " + this);
            return 0;
        }
    }

    /**
     * 读取8位无符号字节
     * Read 8-bit unsigned byte
     *
     * @return 字节值 / Byte value
     */
    protected final int readC() {
        try {
            return this.buf.get() & 255;
        } catch (Exception var2) {
            log.error("Missing C for: " + this);
            return 0;
        }
    }

    /**
     * 读取8位有符号字节
     * Read 8-bit signed byte
     *
     * @return 字节值 / Byte value
     */
    protected final byte readSC() {
        try {
            return this.buf.get();
        } catch (Exception var2) {
            log.error("Missing C for: " + this);
            return 0;
        }
    }

    /**
     * 读取16位有符号短整数
     * Read 16-bit signed short integer
     *
     * @return 短整数值 / Short integer value
     */
    protected final short readSH() {
        try {
            return this.buf.getShort();
        } catch (Exception var2) {
            log.error("Missing H for: " + this);
            return 0;
        }
    }

    /**
     * 读取16位无符号短整数
     * Read 16-bit unsigned short integer
     *
     * @return 短整数值 / Short integer value
     */
    protected final int readH() {
        try {
            return this.buf.getShort() & '\uffff';
        } catch (Exception var2) {
            log.error("Missing H for: " + this);
            return 0;
        }
    }

    /**
     * 读取双精度浮点数
     * Read double-precision floating point
     *
     * @return 双精度浮点数值 / Double value
     */
    protected final double readDF() {
        try {
            return this.buf.getDouble();
        } catch (Exception var2) {
            log.error("Missing DF for: " + this);
            return 0.0D;
        }
    }

    /**
     * 读取单精度浮点数
     * Read single-precision floating point
     *
     * @return 单精度浮点数值 / Float value
     */
    protected final float readF() {
        try {
            return this.buf.getFloat();
        } catch (Exception var2) {
            log.error("Missing F for: " + this);
            return 0.0F;
        }
    }

    /**
     * 读取64位长整数
     * Read 64-bit long integer
     *
     * @return 长整数值 / Long value
     */
    protected final long readQ() {
        try {
            return this.buf.getLong();
        } catch (Exception var2) {
            log.error("Missing Q for: " + this);
            return 0L;
        }
    }

    /**
     * 读取字符串
     * Read string
     *
     * @return 字符串值 / String value
     */
    protected final String readS() {
        StringBuffer sb = new StringBuffer();

        char ch;
        try {
            while((ch = this.buf.getChar()) != 0) {
                sb.append(ch);
            }
        } catch (Exception var4) {
            log.error("Missing S for: " + this);
        }

        return sb.toString();
    }

    /**
     * 读取指定长度的字节数组
     * Read byte array of specified length
     *
     * @param length 要读取的字节长度 / Length of bytes to read
     * @return 字节数组 / Byte array
     */
    protected final byte[] readB(int length) {
        byte[] result = new byte[length];

        try {
            this.buf.get(result);
        } catch (Exception var4) {
            log.error("Missing byte[] for: " + this);
        }

        return result;
    }

    /**
     * 读取十六进制字符串表示的字节数组
     * Read byte array represented by hexadecimal string
     *
     * @param string 十六进制字符串 / Hexadecimal string
     * @return 字节数组 / Byte array
     */
    protected final byte[] readB(String string) {
        String finalString = string.replaceAll("\\s+", "");
        byte[] bytes = new byte[finalString.length() / 2];

        for(int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte)Integer.parseInt(finalString.substring(2 * i, 2 * i + 2), 16);
        }

        try {
            this.buf.get(bytes);
        } catch (Exception var5) {
            log.error("Missing byte[] for: " + this);
        }

        return bytes;
    }

    /**
     * 实现具体的运行逻辑
     * Implement specific running logic
     */
    protected abstract void runImpl();

    /**
     * 获取客户端连接
     * Get client connection
     *
     * @return 客户端连接 / Client connection
     */
    public final T getConnection() {
        return this.client;
    }
}
