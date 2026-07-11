package com.aionemu.chatserver.network.netty.handler;


import com.aionemu.boot.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import com.aionemu.chatserver.common.netty.ByteBufPacketWriter;
import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;

/**
 * 聊天客户端通道业务处理器：管理连接状态、收发数据包并关联 {@link ChatClient}。
 * Chat client channel business handler: manages connection state, packet I/O, and {@link ChatClient} binding.
 *
 * @author ATracer
 */
@Slf4j
@RequiredArgsConstructor
public class ClientChannelHandler {

    private final ClientPacketHandler clientPacketHandler;
    private State state;
    private ChatClient chatClient;
    private InetAddress inetAddress;
    private Channel nettyChannel;

    /**
     * 通道激活：记录远程地址并将状态设为已连接。
     * On channel active: record remote address and set state to connected.
     *
     * Netty channel
     */
    public void nettyChannelActive(Channel channel) {
        state = State.CONNECTED;
        inetAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress();
        nettyChannel = channel;
        log.info(I18n.get("log.46d6c0a90b2c", inetAddress.getHostAddress()));
    }

    /**
     * 通道失活：记录断开日志。
     * On channel inactive: log the disconnect.
     */
    public void nettyChannelInactive() {
        if (inetAddress != null) {
            log.info(I18n.get("log.7149651e7d31", inetAddress.getHostAddress()));
        }
    }

    /**
     * 处理通道异常；忽略纯 IO 异常以免刷屏。
     * Handle channel exceptions; ignore pure I/O exceptions to reduce noise.
     *
     * @param cause 异常原因 / Exception cause
     */
    public void nettyExceptionCaught(Throwable cause) {
        if (!(cause instanceof java.io.IOException)) {
            log.error(I18n.get("log.68d65b13fd02", cause));
        }
    }

    /**
     * 收到完整客户端数据包时入口。
     * Entry point when a complete client packet frame is received.
     *
     * Packet reader
     */
    public void nettyMessageReceived(PacketReader message) {
        handlePacket(message);
    }

    /**
     * 解析并执行客户端包。
     * Parse and execute a client packet.
     *
     * Packet reader
     */
    private void handlePacket(PacketReader message) {
        AbstractClientPacket clientPacket = clientPacketHandler.handle(message, this);
        if (clientPacket != null && clientPacket.read()) {
            clientPacket.run();
        }
        if (clientPacket != null && log.isDebugEnabled()) {
            log.debug("Received packet: " + clientPacket);
        }
    }

    /**
     * 向当前客户端通道写入并刷出服务端数据包。
     * Write and flush a server packet to the current client channel.
     *
     * @param packet 服务端数据包 / Server packet
     */
    public void sendPacket(AbstractServerPacket packet) {
        if (nettyChannel == null) {
            log.warn(I18n.get("log.03eaa2a4f828", packet));
            return;
        }

        ByteBuf output = Unpooled.buffer(2 * 8192);
        ByteBufPacketWriter writer = new ByteBufPacketWriter(output);
        packet.write(this, writer);
        writer.setH(0, writer.readableBytes());
        nettyChannel.writeAndFlush(output);
        if (log.isDebugEnabled()) {
            log.debug("Sent packet: " + packet);
        }
    }

    /**
     * 关闭底层 Netty 通道。
     * Close the underlying Netty channel.
     */
    public void close() {
        if (nettyChannel != null) {
            nettyChannel.close();
        }
    }

    /**
     * 获取客户端 IP 地址字符串。
     * Get the client IP address string.
     *
     * IP address
     */
    public String getIP() {
        return inetAddress.getHostAddress();
    }

    /**
     * 通道处理器可能的状态。
     * Possible states of the channel handler.
     */
    public static enum State {

        /**
         * 客户端刚连接。
         * Client just connected.
         */
        CONNECTED,
        /**
         * 客户端已认证。
         * Client is authenticated.
         */
        AUTHED,
    }

    /**
     * 获取当前连接状态。
     * Get the current connection state.
     *
     * State
     */
    public State getState() {
        return state;
    }

    /**
     * 设置连接状态。
     * Set the connection state.
     *
     * @param state 目标状态 / Target state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * 获取绑定的聊天客户端。
     * Get the bound chat client.
     *
     * @return 聊天客户端 / Chat client
     */
    public ChatClient getChatClient() {
        return chatClient;
    }

    /**
     * 绑定聊天客户端。
     * Bind a chat client.
     *
     * @param chatClient 聊天客户端 / Chat client
     */
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
