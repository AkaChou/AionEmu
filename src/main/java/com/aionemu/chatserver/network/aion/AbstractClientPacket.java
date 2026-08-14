package com.aionemu.chatserver.network.aion;

import com.aionemu.chatserver.common.netty.BaseClientPacket;
import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 聊天客户端数据包抽象基类。
 * Abstract base class for chat client packets.
 *
 * @author ATracer
 */
public abstract class AbstractClientPacket extends BaseClientPacket {

    /**
     * 关联的客户端通道处理器。
     * Associated client channel handler.
     */
    protected ClientChannelHandler clientChannelHandler;

    /**
     * 构造客户端数据包。
     * Constructs a client packet.
     *
     * @param packetReader 包读取器 / packet reader
     * @param clientChannelHandler 客户端通道处理器 / client channel handler
     * @param opCode 操作码 / operation code
     */
    public AbstractClientPacket(PacketReader packetReader, ClientChannelHandler clientChannelHandler, int opCode) {
        super(packetReader, opCode);
        this.clientChannelHandler = clientChannelHandler;
    }
}
