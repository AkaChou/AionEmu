package com.aionemu.chatserver.network.aion.serverpackets;

import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 服务端玩家聊天鉴权成功响应包。
 * Server packet for successful player chat authentication.
 *
 * @author ATracer
 */
public class SM_PLAYER_AUTH_RESPONSE extends AbstractServerPacket {

    /**
     * 构造玩家鉴权成功响应包。
     * Constructs a player auth success response packet.
     */
    public SM_PLAYER_AUTH_RESPONSE() {
        super(0x02);
    }

    /**
     * 写入鉴权成功固定字段。
     * Writes the fixed fields of the auth success response.
     *
     * @param clientChannelHandler 客户端通道处理器 / client channel handler
     * @param buf 包写入器 / packet writer
     */
    @Override
    protected void writeImpl(ClientChannelHandler clientChannelHandler, PacketWriter buf) {
        writeC(buf, getOpCode());
        writeC(buf, 0x40); // ?
        writeH(buf, 0x01); // ?
        writeD(buf, 0x00); // ?
        writeH(buf, 0x0822); // ?
    }
}
