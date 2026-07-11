package com.aionemu.loginserver.network.gameserver;

import java.nio.ByteBuffer;

import com.aionemu.commons.network.packet.BaseServerPacket;

/**
 * 所有 LoginServer → GameServer 服务端封包的基类。
 * Base class for every LoginServer → GameServer server packet.
 *
 * @author -Nemesiss-
 */
public abstract class GsServerPacket extends BaseServerPacket {

    /**
     * 构造空 opcode 的 GS 服务端封包。
     * Construct a GS server packet with empty opcode.
     */
    protected GsServerPacket() {
        super(0);
    }

    /**
     * 将本封包写入指定连接的缓冲区（含长度前缀）。
     * Write this packet into the buffer for the given connection (including length prefix).
     *
     * @param con 目标 GS 连接 / Target GS connection
     * @param buffer 输出缓冲区 / Output buffer
     */
    public final synchronized void write(GsConnection con, ByteBuffer buffer) {
        setBuf(buffer);
        buf.putShort((short) 0);
        writeImpl(con);
        buf.flip();
        buf.putShort((short) buf.limit());
        buf.position(0);
    }

    /**
     * 将封包载荷写入缓冲区。
     * Write packet payload into the buffer.
     *
     * @param con 目标 GS 连接 / Target GS connection
     */
    protected abstract void writeImpl(GsConnection con);
}
