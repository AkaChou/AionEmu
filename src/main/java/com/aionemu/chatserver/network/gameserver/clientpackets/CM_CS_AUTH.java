package com.aionemu.chatserver.network.gameserver.clientpackets;

import java.nio.ByteBuffer;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.network.gameserver.GsAuthResponse;
import com.aionemu.chatserver.network.gameserver.GsClientPacket;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.network.gameserver.GsConnection.State;
import com.aionemu.chatserver.network.gameserver.serverpackets.SM_GS_AUTH_RESPONSE;
import com.aionemu.chatserver.service.ChatCoreServices;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服向聊天服发起认证的客户端包。
 * Client packet used by a game server to authenticate with the chat server.
 *
 * @author ATracer
 */
@Slf4j
public class CM_CS_AUTH extends GsClientPacket {

    /**
     * 认证密码。
     * Password for authentication.
     */
    private String password;

    /**
     * 游戏服 ID。
     * Game server id.
     */
    private byte gameServerId;

    /**
     * 游戏服默认地址。
     * Default address of the game server.
     */
    private byte[] defaultAddress;

    /**
     * 构造认证客户端包。
     * Constructs the authentication client packet.
     *
     * @param buf 原始字节缓冲 / raw byte buffer
     * @param connection 所属游戏服连接 / owning game-server connection
     */
    public CM_CS_AUTH(ByteBuffer buf, GsConnection connection) {
        super(buf, connection, 0x00);
    }

    /**
     * 读取游戏服 ID、默认地址与密码。
     * Reads game server id, default address, and password.
     */
    @Override
    protected void readImpl() {
        gameServerId = (byte) readC();
        defaultAddress = readB(readC());
        password = readS();
    }

    /**
     * 注册游戏服并根据认证结果回复。
     * Registers the game server and replies according to the auth result.
     */
    @Override
    protected void runImpl() {
        GsAuthResponse resp = ChatCoreServices.gameServerService().registerGameServer(gameServerId, defaultAddress, password);

        switch (resp) {
            case AUTHED:
                getConnection().setState(State.AUTHED);
                getConnection().sendPacket(new SM_GS_AUTH_RESPONSE(resp));
                log.info(I18n.get("log.8317d73f1707", gameServerId));
                break;
            case NOT_AUTHED:
                getConnection().sendPacket(new SM_GS_AUTH_RESPONSE(resp));
                break;
            case ALREADY_REGISTERED:
                log.info(I18n.get("log.29ce1a6300e7", gameServerId));
                getConnection().sendPacket(new SM_GS_AUTH_RESPONSE(resp));
                break;
            //	default:
            //	getConnection().close(new SM_GS_AUTH_RESPONSE(resp), false);
        }
    }
}
