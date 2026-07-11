package com.aionemu.chatserver.network.gameserver.serverpackets;

import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.network.gameserver.GsServerPacket;

/**
 * 回复玩家聊天认证结果（客户端 ID 与令牌）的服务端包。
 * Server packet that returns player chat auth result (client id and token).
 *
 * @author ATracer
 */
public class SM_PLAYER_AUTH_RESPONSE extends GsServerPacket {

    /**
     * 聊天客户端玩家 ID。
     * Chat client player id.
     */
    private int playerId;

    /**
     * 聊天认证令牌。
     * Chat authentication token.
     */
    private byte[] token;

    /**
     * 根据聊天客户端构造认证应答包。
     * Builds the auth response packet from the chat client.
     *
     * @param chatClient 已注册的聊天客户端 / registered chat client
     */
    public SM_PLAYER_AUTH_RESPONSE(ChatClient chatClient) {
        this.playerId = chatClient.getClientId();
        token = chatClient.getToken();
    }

    /**
     * 写出玩家 ID 与令牌。
     * Writes the player id and token.
     *
     * @param con 目标游戏服连接 / target game-server connection
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(1);
        writeD(playerId);
        writeC(token.length);
        writeB(token);
    }
}
