package com.aionemu.chatserver.network.aion;

import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.common.netty.AbstractPacketHandler;
import com.aionemu.chatserver.network.aion.clientpackets.CM_CHANNEL_MESSAGE;
import com.aionemu.chatserver.network.aion.clientpackets.CM_CHANNEL_REQUEST;
import com.aionemu.chatserver.network.aion.clientpackets.CM_CHAT_INI;
import com.aionemu.chatserver.network.aion.clientpackets.CM_PLAYER_AUTH;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler.State;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatCoreServices;
import com.aionemu.chatserver.service.ChatService;
import lombok.RequiredArgsConstructor;

/**
 * 聊天客户端数据包分发处理器。
 * Dispatcher for chat client packets.
 *
 * @author ATracer
 */
@RequiredArgsConstructor
public class ClientPacketHandler extends AbstractPacketHandler {

    private final BroadcastService broadcastService;
    private final ChatService chatService;

    /**
     * 使用核心服务默认实例构造。
     * Constructs using default core service instances.
     */
    public ClientPacketHandler() {
        this(ChatCoreServices.broadcastService(), ChatCoreServices.chatService());
    }

    /**
     * 根据连接状态与操作码解析并构造客户端包。
     * Resolves and constructs a client packet by connection state and opcode.
     *
     * @param buf 包读取器 / packet reader
     * @param channelHandler 客户端通道处理器 / client channel handler
     * @return 客户端数据包，未知时为 {@code null} / client packet, or {@code null} if unknown
     */
    public AbstractClientPacket handle(PacketReader buf, ClientChannelHandler channelHandler) {
        int opCode = buf.readC();
        State state = channelHandler.getState();
        AbstractClientPacket clientPacket = null;

        switch (state) {
            case CONNECTED:
                switch (opCode) {
                    case 0x30:
                        clientPacket = new CM_CHAT_INI(buf, channelHandler, chatService);
                        break;
                    case 0x05:
                        clientPacket = new CM_PLAYER_AUTH(buf, channelHandler, chatService);
                        break;
                    default:
                    // unknownPacket(opCode, state.toString());
                }
                break;
            case AUTHED:
                switch (opCode) {
                    case 0x10:
                        clientPacket = new CM_CHANNEL_REQUEST(buf, channelHandler, chatService);
                        break;
                    case 0x18:
                        clientPacket = new CM_CHANNEL_MESSAGE(buf, channelHandler, broadcastService);
                    default:
                    // unknownPacket(opCode, state.toString());
                }
                break;
        }

        return clientPacket;
    }
}
