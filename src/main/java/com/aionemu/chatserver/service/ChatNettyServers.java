package com.aionemu.chatserver.service;

import com.aionemu.chatserver.network.netty.NettyServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public final class ChatNettyServers implements DisposableBean {

    private static volatile ObjectProvider<NettyServer> nettyServerProvider;
    private static volatile NettyServer currentNettyServer;

    public ChatNettyServers(ObjectProvider<NettyServer> nettyServerProvider) {
        ChatNettyServers.nettyServerProvider = nettyServerProvider;
    }

    public static NettyServer nettyServer() {
        ObjectProvider<NettyServer> provider = nettyServerProvider;
        if (provider == null) {
            return fallbackNettyServer();
        }
        return provider.getIfAvailable(ChatNettyServers::fallbackNettyServer);
    }

    public static NettyServer register(NettyServer nettyServer) {
        currentNettyServer = nettyServer;
        return nettyServer;
    }

    public static void shutdownIfInitialized() {
        NettyServer nettyServer = currentNettyServer;
        currentNettyServer = null;
        if (nettyServer != null) {
            nettyServer.shutdownAll();
        }
        NettyServer.shutdownIfInitialized();
    }

    @Override
    public void destroy() {
        shutdownIfInitialized();
        nettyServerProvider = null;
    }

    private static NettyServer fallbackNettyServer() {
        return register(Fallbacks.NETTY_SERVER);
    }

    private static final class Fallbacks {

        private static final NettyServer NETTY_SERVER = new NettyServer();
    }
}
