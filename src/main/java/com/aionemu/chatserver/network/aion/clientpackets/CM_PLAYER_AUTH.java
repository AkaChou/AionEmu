package com.aionemu.chatserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.UnsupportedEncodingException;

import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.service.ChatService;

/**
 * 客户端玩家聊天鉴权包。
 * Client packet for player chat authentication.
 *
 * @author ATracer
 */
@Slf4j
public class CM_PLAYER_AUTH extends AbstractClientPacket {

    private ChatService chatService;
    private int playerId;
    private byte[] token;
    private byte[] identifier;
    @SuppressWarnings("unused")
    private byte[] accountName;
    private String realName;

    /**
     * 构造玩家鉴权客户端包。
     * Constructs a player auth client packet.
     *
     * packet reader
     * @param clientChannelHandler 客户端通道处理器 / client channel handler
     * chat service
     */
    public CM_PLAYER_AUTH(PacketReader packetReader, ClientChannelHandler clientChannelHandler, ChatService chatService) {
        super(packetReader, clientChannelHandler, 0x05);
        this.chatService = chatService;
    }

    /**
     * 读取玩家 ID、标识、账号名与 token，并解析真实角色名。
     * Reads player id, identifier, account name and token, then resolves the real character name.
     */
    @Override
    protected void readImpl() {
        readB(29); //AION stuff
        this.playerId = readD();
        readD(); // 0x00
        readD(); // 0x00
        readD(); // 0x00
        int length = readH() * 2;
        identifier = readB(length);
        int accountLenght = readH() * 2;
        accountName = readB(accountLenght);
        int tokenLength = readH();
        token = readB(tokenLength);

        try {
            String realid = new String(identifier, "UTF-16le");

            realName = realid.split("@")[0];
            String after = realid.split("@")[1];
            identifier = after.getBytes("UTF-16le");
        } catch (UnsupportedEncodingException e) {
            log.error(I18n.get("log.3c04a2cb63c1", playerId, e));
        }
    }

    /**
     * 向聊天服务注册玩家连接。
     * Registers the player connection with the chat service.
     */
    @Override
    protected void runImpl() {
        try {
            chatService.registerPlayerConnection(playerId, token, identifier, clientChannelHandler, realName);
        } catch (UnsupportedEncodingException e) {
            log.error(I18n.get("log.fccabf8023e0", playerId, e));
        }
    }
}
