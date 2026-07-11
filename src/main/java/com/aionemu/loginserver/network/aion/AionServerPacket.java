package com.aionemu.loginserver.network.aion;

import com.aionemu.commons.network.packet.BaseServerPacket;
import java.nio.ByteBuffer;

/**
 * 所有登录服 → Aion 客户端服务端包的基类。
 * Base class for every login-server → Aion client server packet.
 *
 * @author -Nemesiss-
 */
public abstract class AionServerPacket extends BaseServerPacket {

    /**
     * 以指定 opcode 构造服务端包。
     * Construct server packet with the given opcode.
     *
     * Packet opcode
     */
    protected AionServerPacket(int opcode) {
        super(opcode);
    }

    /**
     * 将本包写入并加密到连接缓冲。
     * Write and encrypt this packet into the connection buffer.
     *
     * @param con 目标登录连接 / Target login connection
     */
    public final void write(LoginConnection con) {
        buf.putShort((short) 0);
        buf.put((byte) getOpcode());
        writeImpl(con);
        buf.flip();
        buf.putShort((short) 0);
        ByteBuffer b = buf.slice();

        short size = (short) (con.encrypt(b) + 2);
        buf.putShort(0, size);
        buf.position(0).limit(size);
    }

    /**
     * 将包体数据写入缓冲（子类实现）。
     * Write packet payload to the buffer (implemented by subclasses).
     *
     * @param con 目标登录连接 / Target login connection
     */
    protected abstract void writeImpl(LoginConnection con);
}
