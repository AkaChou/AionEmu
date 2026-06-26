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

    @Override
    public void destroy() {
        nettyServerProvider = null;
    }

    private static NettyServer fallbackNettyServer() {
        return Fallbacks.NETTY_SERVER;
    }

    private static final class Fallbacks {

        private static final NettyServer NETTY_SERVER = NettyServer.getInstance();
    }
}
