package com.aionemu.chatserver.network.aion.serverpackets;

import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 服务端聊天初始化响应包。
 * Server packet for chat initialization response.
 *
 * @author ginho1
 */
public class SM_CHAT_INI extends AbstractServerPacket {

    /**
     * 构造聊天初始化响应包。
     * Constructs a chat init response packet.
     */
    public SM_CHAT_INI() {
        super(0x31);
    }

    /**
     * 写入初始化响应固定字段。
     * Writes the fixed fields of the init response.
     *
     * @param cHandler 客户端通道处理器 / client channel handler
     * @param buf 包写入器 / packet writer
     */
    @Override
    protected void writeImpl(ClientChannelHandler cHandler, PacketWriter buf) {
        writeC(buf, getOpCode());
        writeC(buf, 0x40);
        writeD(buf, 0x02);
        writeH(buf, 0x00);
    }
}
