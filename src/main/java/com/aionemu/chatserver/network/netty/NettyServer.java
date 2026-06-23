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


package com.aionemu.chatserver.network.netty;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.gameserver.GsConnectionFactoryImpl;
import com.aionemu.chatserver.network.netty.pipeline.LoginToClientPipeLineFactory;
import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerCfg;
import com.aionemu.commons.network.ServerTransport;

/**
 * @author ATracer
 */
public class NettyServer {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private final ChannelGroup channelGroup = new DefaultChannelGroup(NettyServer.class.getName());
    private final LoginToClientPipeLineFactory loginToClientPipeLineFactory;
    private ChannelFactory loginToClientChannelFactory;
    private Netty4ChatClientServer netty4ChatClientServer;
    private ServerTransport gameServerTransport;
    private static NettyServer instance;

    public static synchronized NettyServer getInstance() {
        if (instance == null) {
            instance = new NettyServer();
        }
        return instance;
    }

    public static void shutdownIfInitialized() {
        NettyServer server;
        synchronized (NettyServer.class) {
            server = instance;
            instance = null;
        }
        if (server != null) {
            server.shutdownAll();
        }
    }

    static synchronized boolean isInitialized() {
        return instance != null;
    }

    public NettyServer() {
        this.loginToClientPipeLineFactory = new LoginToClientPipeLineFactory(new ClientPacketHandler());
        initialize();
    }

    /**
     * Initialize listening on login port
     */
    public void initialize() {
        boolean nettyTransport = Boolean.getBoolean("aion.transport.netty");
        if (nettyTransport) {
            netty4ChatClientServer = new Netty4ChatClientServer(Config.CHAT_ADDRESS, new ClientPacketHandler());
            netty4ChatClientServer.connect();
        } else {
            loginToClientChannelFactory = initChannelFactory();
            Channel loginToClientChannel = initChannel(loginToClientChannelFactory, Config.CHAT_ADDRESS, loginToClientPipeLineFactory);
            channelGroup.add(loginToClientChannel);
        }
        String gameHost = Config.GAME_ADDRESS.getAddress().getHostAddress();
        int gamePort = Config.GAME_ADDRESS.getPort();
        if (nettyTransport) {
            gameServerTransport = new com.aionemu.commons.network.NettyServer(
                new NettyServerCfg(gameHost, gamePort, "Gs Connections", new GsConnectionFactoryImpl())
            );
        } else {
            ServerCfg gs = new ServerCfg(gameHost, gamePort, "Gs Connections", new GsConnectionFactoryImpl());
            gameServerTransport = new NioServer(5, gs);
        }
        gameServerTransport.connect();
    }

    /**
     * @return NioServerSocketChannelFactory
     */
    private NioServerSocketChannelFactory initChannelFactory() {
        return new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), Runtime.getRuntime().availableProcessors() * 2 + 1);
    }

    /**
     * @param channelFactory
     * @param listenAddress
     * @param port
     * @param channelPipelineFactory
     * @return Channel
     */
    private Channel initChannel(ChannelFactory channelFactory, InetSocketAddress address, ChannelPipelineFactory channelPipelineFactory) {
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(channelPipelineFactory);
        bootstrap.setOption("child.bufferFactory", HeapChannelBufferFactory.getInstance(ByteOrder.LITTLE_ENDIAN));
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.reuseAddress", true);
        bootstrap.setOption("child.connectTimeoutMillis", 100);
        bootstrap.setOption("readWriteFair", true);

        return bootstrap.bind(address);
    }

    /**
     * Shutdown server
     */
    public void shutdownAll() {
        ChannelGroupFuture future = channelGroup.close();
        future.awaitUninterruptibly();
        if (loginToClientChannelFactory != null) {
            loginToClientChannelFactory.releaseExternalResources();
        }
        if (netty4ChatClientServer != null) {
            netty4ChatClientServer.shutdown();
        }
        if (gameServerTransport != null) {
            gameServerTransport.shutdown();
        }
    }
}
