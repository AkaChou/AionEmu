package com.aionemu.chatserver.network.gameserver;

import java.nio.ByteBuffer;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.packet.BaseClientPacket;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服 → 聊天服客户端数据包的抽象基类。
 * Abstract base class for game server → chat server client packets.
 *
 * @author KID
 */
@Slf4j
public abstract class GsClientPacket extends BaseClientPacket<GsConnection> {

    /**
     * 构造游戏服客户端数据包。
     * Constructs a game-server client packet.
     *
     * @param buffer 原始字节缓冲 / raw byte buffer
     * @param connection 所属游戏服连接 / owning game-server connection
     * @param opCode 数据包操作码 / packet opcode
     */
    public GsClientPacket(ByteBuffer buffer, GsConnection connection, int opCode) {
        super(opCode);
    }

    /**
     * 在线程池中执行包逻辑，并捕获异常。
     * Executes packet logic on the worker thread and traps unexpected errors.
     */
    @Override
    public final void run() {
        try {
            runImpl();
        } catch (Throwable e) {
            log.warn(I18n.get("log.8fd86409bd46", getConnection().getIP(), this, e));
        }
    }

    /**
     * 通过当前连接发送服务端包。
     * Sends a server packet through the current connection.
     *
     * @param msg 待发送的服务端包 / server packet to send
     */
    protected void sendPacket(GsServerPacket msg) {
        getConnection().sendPacket(msg);
    }
}
