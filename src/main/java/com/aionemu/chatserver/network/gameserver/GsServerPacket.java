package com.aionemu.chatserver.network.gameserver;

import java.nio.ByteBuffer;

import com.aionemu.commons.network.packet.BaseServerPacket;

/**
 * 聊天服 → 游戏服服务端数据包的抽象基类。
 * Abstract base class for every chat server → game server server packet.
 *
 * @author -Nemesiss-
 */
public abstract class GsServerPacket extends BaseServerPacket {

    /**
     * 构造服务端数据包（默认 opcode 为 0）。
     * Constructs a server packet (default opcode is 0).
     */
    protected GsServerPacket() {
        super(0);
    }

    /**
     * 将本包数据写入指定连接的缓冲。
     * Writes this packet's data for the given connection into the buffer.
     *
     * @param con 目标游戏服连接 / target game-server connection
     * @param buffer 写出缓冲 / write buffer
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
     * 由子类实现的具体包体写入逻辑。
     * Subclass-implemented body that serializes packet-specific data.
     *
     * @param con 目标游戏服连接 / target game-server connection
     */
    protected abstract void writeImpl(GsConnection con);
}
