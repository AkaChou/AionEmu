package com.aionemu.chatserver.network.gameserver.clientpackets;

import java.nio.ByteBuffer;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.network.gameserver.GsClientPacket;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.service.ChatCoreServices;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服通知玩家登出聊天服的客户端包。
 * Client packet notifying the chat server that a player has logged out.
 *
 * @author ATracer
 */
@Slf4j
public class CM_PLAYER_LOGOUT extends GsClientPacket {

    /**
     * 登出玩家 ID。
     * Logging-out player id.
     */
    private int playerId;

    /**
     * 构造玩家登出客户端包。
     * Constructs the player logout client packet.
     *
     * @param buf 原始字节缓冲 / raw byte buffer
     * @param connection 所属游戏服连接 / owning game-server connection
     */
    public CM_PLAYER_LOGOUT(ByteBuffer buf, GsConnection connection) {
        super(buf, connection, 0x02);
    }

    /**
     * 读取登出玩家 ID。
     * Reads the logging-out player id.
     */
    @Override
    protected void readImpl() {
        playerId = readD();
    }

    /**
     * 处理玩家登出并记录日志。
     * Handles player logout and logs the event.
     */
    @Override
    protected void runImpl() {
        ChatCoreServices.chatService().playerLogout(playerId);
        log.info(I18n.get("log.855c53b4ef96", playerId));
    }
}
