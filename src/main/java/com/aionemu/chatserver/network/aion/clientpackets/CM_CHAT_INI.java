package com.aionemu.chatserver.network.aion.clientpackets;

import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.aion.serverpackets.SM_CHAT_INI;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.service.ChatService;

/**
 * 客户端聊天初始化请求包。
 * Client packet for chat initialization request.
 *
 * @author ginho1
 */
public class CM_CHAT_INI extends AbstractClientPacket {

    /**
     * 构造聊天初始化客户端包。
     * Constructs a chat init client packet.
     *
     * @param packetReader 包读取器 / packet reader
     * @param clientChannelHandler 客户端通道处理器 / client channel handler
     * @param chatService 聊天服务（当前未使用） / chat service (currently unused)
     */
    public CM_CHAT_INI(PacketReader packetReader, ClientChannelHandler clientChannelHandler, ChatService chatService) {
        super(packetReader, clientChannelHandler, 0x30);
    }

    /**
     * 读取初始化请求的固定字段。
     * Reads the fixed fields of the init request.
     */
    @Override
    protected void readImpl() {
        readC();
        readH();
        readD();
        readD();
        readD();
    }

    /**
     * 回送聊天初始化响应包。
     * Sends the chat initialization response packet.
     */
    @Override
    protected void runImpl() {
        clientChannelHandler.getChatClient();
        clientChannelHandler.sendPacket(new SM_CHAT_INI());
    }
}
