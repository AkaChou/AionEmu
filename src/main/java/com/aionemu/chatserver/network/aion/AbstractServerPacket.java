package com.aionemu.chatserver.network.aion;

import com.aionemu.chatserver.common.netty.BaseServerPacket;
import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 聊天服务端数据包抽象基类。
 * Abstract base class for chat server packets.
 *
 * @author ATracer
 */
public abstract class AbstractServerPacket extends BaseServerPacket {

    /**
     * 构造服务端数据包。
     * Constructs a server packet.
     *
     * @param opCode 操作码 / operation code
     */
    public AbstractServerPacket(int opCode) {
        super(opCode);
    }

    /**
     * 写入完整数据包（含长度占位后委托具体实现）。
     * Writes the full packet (length placeholder then delegates to the concrete implementation).
     *
     * @param clientChannelHandler 客户端通道处理器 / client channel handler
     * @param buf 包写入器 / packet writer
     */
    public void write(ClientChannelHandler clientChannelHandler, PacketWriter buf) {
        buf.writeH(0);
        writeImpl(clientChannelHandler, buf);
    }

    /**
     * 由子类实现的具体写入逻辑。
     * Concrete write logic implemented by subclasses.
     *
     * @param cHandler 客户端通道处理器 / client channel handler
     * @param buf 包写入器 / packet writer
     */
    protected abstract void writeImpl(ClientChannelHandler cHandler, PacketWriter buf);
}
