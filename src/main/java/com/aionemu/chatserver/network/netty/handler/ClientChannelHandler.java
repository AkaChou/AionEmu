/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.chatserver.network.netty.handler;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.chatserver.common.netty.ByteBufPacketWriter;
import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;

/**
 * @author ATracer
 */
public class ClientChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientChannelHandler.class);
    private final ClientPacketHandler clientPacketHandler;
    private State state;
    private ChatClient chatClient;
    private InetAddress inetAddress;
    private Channel nettyChannel;

    public ClientChannelHandler(ClientPacketHandler clientPacketHandler) {
        this.clientPacketHandler = clientPacketHandler;
    }

    public void nettyChannelActive(Channel channel) {
        state = State.CONNECTED;
        inetAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress();
        nettyChannel = channel;
        log.info("Channel connected Ip:" + inetAddress.getHostAddress());
    }

    public void nettyChannelInactive() {
        if (inetAddress != null) {
            log.info("Channel disconnected IP: " + inetAddress.getHostAddress());
        }
    }

    public void nettyExceptionCaught(Throwable cause) {
        if (!(cause instanceof java.io.IOException)) {
            log.error("NETTY: Exception caught: ", cause);
        }
    }

    public void nettyMessageReceived(PacketReader message) {
        handlePacket(message);
    }

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
     * @param packet
     */
    public void sendPacket(AbstractServerPacket packet) {
        if (nettyChannel == null) {
            log.warn("Cannot send chat packet without an active Netty channel: {}", packet);
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
     * Closes the channel.
     */
    public void close() {
        if (nettyChannel != null) {
            nettyChannel.close();
        }
    }

    /**
     * @return the IP address string
     */
    public String getIP() {
        return inetAddress.getHostAddress();
    }

    /**
     * Possible states of channel handler
     */
    public static enum State {

        /**
         * client just connected
         */
        CONNECTED,
        /**
         * client is authenticated
         */
        AUTHED,
    }

    /**
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * @return the chatClient
     */
    public ChatClient getChatClient() {
        return chatClient;
    }

    /**
     * @param chatClient the chatClient to set
     */
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
