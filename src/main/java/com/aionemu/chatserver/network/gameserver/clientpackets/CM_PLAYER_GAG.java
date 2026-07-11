package com.aionemu.chatserver.network.gameserver.clientpackets;

import java.nio.ByteBuffer;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.network.gameserver.GsClientPacket;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.service.ChatCoreServices;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服请求禁言玩家的客户端包。
 * Client packet by which a game server gags (mutes) a player.
 *
 * @author ViAl
 */
@Slf4j(topic = "com.aionemu.chatserver.network.gameserver.clientpackets.CM_PLAYER_LOGOUT")
public class CM_PLAYER_GAG extends GsClientPacket {

    /**
     * 被禁言的玩家 ID。
     * Id of the player to gag.
     */
    private int playerId;

    /**
     * 禁言时长（毫秒）。
     * Gag duration in milliseconds.
     */
    private long gagTime;

    /**
     * 构造玩家禁言客户端包。
     * Constructs the player gag client packet.
     *
     * @param buf 原始字节缓冲 / raw byte buffer
     * @param connection 所属游戏服连接 / owning game-server connection
     */
    public CM_PLAYER_GAG(ByteBuffer buf, GsConnection connection) {
        super(buf, connection, 0x03);
    }

    /**
     * 读取玩家 ID 与禁言时长。
     * Reads player id and gag duration.
     */
    @Override
    protected void readImpl() {
        playerId = readD();
        gagTime = readQ();
    }

    /**
     * 执行玩家禁言并记录日志。
     * Applies the player gag and logs the action.
     */
    @Override
    protected void runImpl() {
        ChatCoreServices.chatService().gagPlayer(playerId, gagTime);
        log.info(I18n.get("log.792412a33c48", playerId, (gagTime / 1000 / 60)));
    }
}
