package com.aionemu.chatserver.network.aion.serverpackets;

import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 服务端频道加入响应包。
 * Server packet responding to a channel join request.
 *
 * @author ATracer
 */
public class SM_CHANNEL_RESPONSE extends AbstractServerPacket {

    private Channel channel;
    private final int channelIndex;

    /**
     * 构造频道响应服务端包。
     * Constructs a channel response server packet.
     *
     * channel instance
     * channel index
     */
    public SM_CHANNEL_RESPONSE(Channel channel, int channelIndex) {
        super(0x11);
        this.channel = channel;
        this.channelIndex = channelIndex;
    }

    /**
     * 写入频道索引与频道 ID。
     * Writes the channel index and channel id.
     *
     * @param cHandler 客户端通道处理器 / client channel handler
     * @param buf 包写入器 / packet writer
     */
    @Override
    protected void writeImpl(ClientChannelHandler cHandler, PacketWriter buf) {
        writeC(buf, getOpCode());
        writeC(buf, 0x40);
        writeH(buf, channelIndex);
        writeH(buf, 0x00);
        writeH(buf, 0x00);
        writeD(buf, channel.getChannelId());
    }
}
