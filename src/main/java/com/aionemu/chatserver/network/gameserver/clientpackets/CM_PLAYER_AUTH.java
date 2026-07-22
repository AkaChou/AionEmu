package com.aionemu.chatserver.network.gameserver.clientpackets;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.network.gameserver.GsClientPacket;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.network.gameserver.serverpackets.SM_PLAYER_AUTH_RESPONSE;
import com.aionemu.chatserver.service.ChatCoreServices;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服请求为玩家注册聊天客户端的客户端包。
 * Client packet by which a game server registers a player chat client.
 *
 * @author ATracer
 */
@Slf4j
public class CM_PLAYER_AUTH extends GsClientPacket {

    /**
     * 玩家 ID。
     * Player id.
     */
    private int playerId;

    /**
     * 玩家账号登录名。
     * Player account login name.
     */
    private String playerLogin;

    /**
     * 玩家昵称。
     * Player nickname.
     */
    private String nick;

    /**
     * 构造玩家认证客户端包。
     * Constructs the player authentication client packet.
     *
     * @param buf 原始字节缓冲 / raw byte buffer
     * @param connection 所属游戏服连接 / owning game-server connection
     */
    public CM_PLAYER_AUTH(ByteBuffer buf, GsConnection connection) {
        super(buf, connection, 0x01);
    }

    /**
     * 读取玩家 ID、登录名与昵称。
     * Reads player id, login name, and nickname.
     */
    @Override
    protected void readImpl() {
        playerId = readD();
        playerLogin = readS();
        nick = readS();
    }

    /**
     * 注册聊天客户端并在成功时回复令牌。
     * Registers the chat client and replies with a token on success.
     */
    @Override
    protected void runImpl() {
        ChatClient chatClient = null;
        try {
            chatClient = ChatCoreServices.chatService().registerPlayer(playerId, playerLogin, nick);
        } catch (NoSuchAlgorithmException e) {
            log.error(I18n.get("log.ce4b198eb93c", e.getMessage()), e);
        } catch (UnsupportedEncodingException e) {
            log.error(I18n.get("log.ce4b198eb93c", e.getMessage()), e);
        }

        if (chatClient != null) {
            getConnection().sendPacket(new SM_PLAYER_AUTH_RESPONSE(chatClient));
        } else {
            log.info(I18n.get("log.1e070416317c", playerId));
        }
    }
}
