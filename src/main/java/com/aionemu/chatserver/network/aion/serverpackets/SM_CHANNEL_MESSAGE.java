package com.aionemu.chatserver.network.aion.serverpackets;

import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.model.message.Message;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 服务端频道聊天消息包。
 * Server packet for channel chat messages.
 *
 * @author ATracer
 */
public class SM_CHANNEL_MESSAGE extends AbstractServerPacket {

    private Message message;

    /**
     * 构造频道消息服务端包。
     * Constructs a channel message server packet.
     *
     * chat message
     */
    public SM_CHANNEL_MESSAGE(Message message) {
        super(0x1A);
        this.message = message;
    }

    /**
     * 写入频道、发送者与消息正文。
     * Writes the channel, sender and message body.
     *
     * @param cHandler 客户端通道处理器 / client channel handler
     * @param buf 包写入器 / packet writer
     */
    @Override
    protected void writeImpl(ClientChannelHandler cHandler, PacketWriter buf) {
        writeC(buf, getOpCode());
        writeC(buf, 0x00);
        writeC(buf, 0xB7);
        writeC(buf, 0x32);
        writeH(buf, 0x6401);
        writeD(buf, 0x00);
        writeD(buf, message.getChannel().getChannelId());
        writeD(buf, message.getSender().getClientId());
        writeD(buf, 0x00);
        writeC(buf, 0x00);
        writeH(buf, message.getSender().getIdentifier().length / 2);
        writeB(buf, message.getSender().getIdentifier());
        writeH(buf, message.size() / 2);
        writeB(buf, message.getText());
    }
}
