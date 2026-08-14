package com.aionemu.chatserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.aion.serverpackets.SM_CHANNEL_RESPONSE;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.service.ChatService;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;

/**
 * 客户端加入/请求聊天频道包。
 * Client packet for joining or requesting a chat channel.
 *
 * @author ATracer
 */
@Slf4j
public class CM_CHANNEL_REQUEST extends AbstractClientPacket {

    private int channelIndex;
    private byte[] channelIdentifier;
    private ChatService chatService;

    /**
     * 构造频道请求客户端包。
     * Constructs a channel request client packet.
     *
     * @param packetReader 包读取器 / packet reader
     * @param gameChannelHandler 客户端通道处理器 / client channel handler
     * @param chatService 聊天服务 / chat service
     */
    public CM_CHANNEL_REQUEST(PacketReader packetReader, ClientChannelHandler gameChannelHandler, ChatService chatService) {
        super(packetReader, gameChannelHandler, 0x10);
        this.chatService = chatService;
    }

    /**
     * 读取频道索引与标识。
     * Reads the channel index and identifier.
     */
    @Override
    protected void readImpl() {
        readC(); // 0x40
        readH(); // 0x00
        channelIndex = readH();
        readB(18); //?
        int length = (readH() * 2);
        channelIdentifier = readB(length);
        readD(); // ?
    }

    /**
     * 将玩家注册到频道并回送频道响应。
     * Registers the player with the channel and sends the channel response.
     */
    @Override
    protected void runImpl() {
        try {
            if (Config.LOG_CHANNEL_REQUEST) {
                log.info(I18n.get("log.97c4098f2607", new String(channelIdentifier, "UTF-16le")));
            }
        } catch (UnsupportedEncodingException e) {
            log.error(I18n.get("log.6c1460337508", e));
        }
        ChatClient chatClient = clientChannelHandler.getChatClient();
        Channel channel = chatService.registerPlayerWithChannel(chatClient, channelIndex, channelIdentifier);
        if (channel != null) {
            clientChannelHandler.sendPacket(new SM_CHANNEL_RESPONSE(channel, channelIndex));
        }
    }

    /**
     * 返回调试用字符串表示。
     * Returns a debug string representation.
     *
     * @return 调试字符串 / debug string
     */
    @Override
    public String toString() {
        return "CM_CHANNEL_REQUEST [channelIndex=" + channelIndex + ", channelIdentifier=" + new String(channelIdentifier) + "]";
    }
}
