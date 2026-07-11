package com.aionemu.chatserver.network.gameserver;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.network.factories.GsPacketHandlerFactory;
import com.aionemu.chatserver.service.ChatCoreServices;
import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.CommonsNetworkThreadPoolServices;
import com.aionemu.commons.network.ConnectionTransport;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天服与游戏服之间的网络连接。
 * Network connection between the chat server and a game server.
 *
 * @author KID
 */
@Slf4j
public class GsConnection extends AConnection {

    /**
     * 游戏服连接状态。
     * Game-server connection state.
     */
    public static enum State {

        /**
         * 已连接但尚未认证。
         * Connected but not yet authenticated.
         */
        CONNECTED,

        /**
         * 已通过认证。
         * Authenticated successfully.
         */
        AUTHED
    }

    /**
     * 待发送服务端包队列。
     * Outbound server-packet queue.
     */
    private final Deque<GsServerPacket> sendMsgQueue = new ArrayDeque<GsServerPacket>();

    /**
     * 当前连接状态。
     * Current connection state.
     */
    @Getter
    @Setter
    private State state;

    /**
     * 基于传输层创建游戏服连接。
     * Creates a game-server connection on the given transport.
     *
     * @param transport 底层连接传输 / underlying connection transport
     */
    public GsConnection(ConnectionTransport transport) {
        super(transport, 8192 * 8, 8192 * 8);
    }

    /**
     * 解析并调度入站游戏服客户端包。
     * Parses and dispatches an inbound game-server client packet.
     *
     * @param data 入站数据缓冲 / inbound data buffer
     * @return 是否继续处理 / whether processing should continue
     */
    @Override
    public boolean processData(ByteBuffer data) {
        GsClientPacket pck = GsPacketHandlerFactory.handle(data, this);

        if (pck != null && pck.read()) {
            CommonsNetworkThreadPoolServices.threadPoolManager().execute(pck);
        }

        return true;
    }

    /**
     * 从发送队列取出并写出一个服务端包。
     * Dequeues and writes one server packet from the send queue.
     *
     * @param data 写出缓冲 / write buffer
     * @return 是否写出了数据包 / whether a packet was written
     */
    @Override
    protected final boolean writeData(ByteBuffer data) {
        synchronized (guard) {
            GsServerPacket packet = sendMsgQueue.pollFirst();
            if (packet == null) {
                return false;
            }

            packet.write(this, data);
            return true;
        }
    }

    /**
     * 返回断线延迟（本连接立即断开）。
     * Returns the disconnection delay (this connection disconnects immediately).
     *
     * @return 延迟毫秒数 / delay in milliseconds
     */
    @Override
    protected final long getDisconnectionDelay() {
        return 0;
    }

    /**
     * 连接断开时将游戏服标记为离线。
     * Marks the game server offline when the connection drops.
     */
    @Override
    protected final void onDisconnect() {
        ChatCoreServices.gameServerService().setOffline();
    }

    /**
     * 服务器关闭时强制关闭本连接。
     * Force-closes this connection when the server shuts down.
     */
    @Override
    protected final void onServerClose() {
        close(true);
    }

    /**
     * 将服务端包加入发送队列。
     * Enqueues a server packet for sending.
     *
     * @param bp 待发送服务端包 / server packet to send
     */
    public final void sendPacket(GsServerPacket bp) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }

            sendMsgQueue.addLast(bp);
            enableWriteInterest();
        }
    }

    /**
     * 清空发送队列并以指定包关闭连接。
     * Clears the send queue and closes the connection with the given packet.
     *
     * @param closePacket 关闭前发送的包 / packet sent before close
     * @param forced 是否强制关闭 / whether the close is forced
     */
    public final void close(GsServerPacket closePacket, boolean forced) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }

            pendingClose = true;
            isForcedClosing = forced;
            sendMsgQueue.clear();
            sendMsgQueue.addLast(closePacket);
            enableWriteInterest();
        }
    }

    /**
     * 连接初始化：设为已连接状态并记录日志。
     * Initializes the connection: sets CONNECTED state and logs the event.
     */
    @Override
    protected void initialized() {
        state = State.CONNECTED;
        log.info(I18n.get("log.25ae763a9d55", getIP()));
    }
}
