package com.aionemu.chatserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.model.channel.ChatChannels;
import com.aionemu.chatserver.model.message.Message;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.aion.serverpackets.SM_CHANNEL_MESSAGE;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.service.BroadcastService;

/**
 * 客户端频道聊天消息包。
 * Client packet for channel chat messages.
 *
 * @author ATracer
 */
@Slf4j(topic = "CHAT_LOG")
public class CM_CHANNEL_MESSAGE extends AbstractClientPacket {

    private int channelId;
    private byte[] content;
    private BroadcastService broadcastService;

    /**
     * 构造频道消息客户端包。
     * Constructs a channel message client packet.
     *
     * packet reader
     * @param gameChannelHandler 客户端通道处理器 / client channel handler
     * broadcast service
     */
    public CM_CHANNEL_MESSAGE(PacketReader packetReader, ClientChannelHandler gameChannelHandler, BroadcastService broadcastService) {
        super(packetReader, gameChannelHandler, 0x18);
        this.broadcastService = broadcastService;
    }

    /**
     * 读取频道 ID 与消息内容。
     * Reads the channel id and message content.
     */
    @Override
    protected void readImpl() {
        readH();
        readC();
        readD();
        readD();
        readD();
        readD();
        channelId = readD();
        readC();
        int lenght = readH() * 2;
        content = readB(lenght);
    }

    /**
     * 校验发言频率/禁言后广播消息，可选写聊天日志。
     * Broadcasts after rate/gag checks; optionally writes chat logs.
     */
    @Override
    protected void runImpl() {
        Channel channel = ChatChannels.getChannelById(channelId);
        Message message = new Message(channel, content, clientChannelHandler.getChatClient());
        if (!clientChannelHandler.getChatClient().verifyLastMessage()) {
            message.setText("You can use chat only once every 30 second.");
            clientChannelHandler.sendPacket(new SM_CHANNEL_MESSAGE(message));
            return;
        }
        if (clientChannelHandler.getChatClient().isGagged()) {
            long endTime = (clientChannelHandler.getChatClient().getGagTime() - System.currentTimeMillis()) / 1000 / 60;
            message.setText("You have been gagged for " + endTime + " minutes");
            clientChannelHandler.sendPacket(new SM_CHANNEL_MESSAGE(message));
            return;
        }
        broadcastService.broadcastMessage(message);

        if (Config.LOG_CHAT) {
            log.info(I18n.get("log.da62952559a1", message.getChannelString(), message.getSenderString(),
                    message.getTextString()));
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
        return "CM_CHANNEL_MESSAGE [channelId=" + channelId + ", content=" + Arrays.toString(content) + "]";
    }
}
