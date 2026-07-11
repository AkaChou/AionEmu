package com.aionemu.chatserver.network.gameserver.serverpackets;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.network.gameserver.GsAuthResponse;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.network.gameserver.GsServerPacket;

import lombok.RequiredArgsConstructor;

/**
 * 回复游戏服认证结果的服务端包（含聊天服地址）。
 * Server packet that replies with game-server auth result and chat server address.
 *
 * @author ATracer
 */
@RequiredArgsConstructor
public class SM_GS_AUTH_RESPONSE extends GsServerPacket {

    /**
     * 认证应答结果。
     * Authentication response result.
     */
    private final GsAuthResponse response;

    /**
     * 写出认证结果与聊天服监听地址、端口。
     * Writes auth result plus chat server listen address and port.
     *
     * @param con 目标游戏服连接 / target game-server connection
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(0);
        writeC(response.getResponseId());
        writeB(Config.CHAT_ADDRESS.getAddress().getAddress());
        writeH(Config.CHAT_ADDRESS.getPort());
    }
}
