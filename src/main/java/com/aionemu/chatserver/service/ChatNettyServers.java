package com.aionemu.chatserver.service;

import com.aionemu.chatserver.network.netty.NettyServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 聊天 Netty 服务端静态访问门面：提供/注册 {@link NettyServer} 并统一关闭。
 * Static facade for the chat Netty server: provides/registers {@link NettyServer} and centralizes shutdown.
 */
@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public final class ChatNettyServers implements DisposableBean {

    private static volatile ObjectProvider<NettyServer> nettyServerProvider;
    private static volatile NettyServer currentNettyServer;

    /**
     * 注册 Spring 侧 {@link NettyServer} 提供者。
     * Register the Spring {@link NettyServer} provider.
     *
     * @param nettyServerProvider Netty 服务端提供者 / Netty server provider
     */
    public ChatNettyServers(ObjectProvider<NettyServer> nettyServerProvider) {
        ChatNettyServers.nettyServerProvider = nettyServerProvider;
    }

    /**
     * 获取当前可用的 {@link NettyServer}。
     * Obtain the currently available {@link NettyServer}.
     *
     * @return Netty 服务端实例 / Netty server instance
     */
    public static NettyServer nettyServer() {
        ObjectProvider<NettyServer> provider = nettyServerProvider;
        if (provider == null) {
            return fallbackNettyServer();
        }
        return provider.getIfAvailable(ChatNettyServers::fallbackNettyServer);
    }

    /**
     * 登记当前运行中的 Netty 服务端实例。
     * Register the currently running Netty server instance.
     *
     * @param nettyServer 服务端实例 / server instance
     * @return 同一实例 / the same instance
     */
    public static NettyServer register(NettyServer nettyServer) {
        currentNettyServer = nettyServer;
        return nettyServer;
    }

    /**
     * 关闭已登记实例与遗留单例（若存在）。
     * Shut down the registered instance and the legacy singleton if present.
     */
    public static void shutdownIfInitialized() {
        NettyServer nettyServer = currentNettyServer;
        currentNettyServer = null;
        if (nettyServer != null) {
            nettyServer.shutdownAll();
        }
        NettyServer.shutdownIfInitialized();
    }

    /**
     * Bean 销毁时关闭网络并清空提供者。
     * On bean destroy: shut down networking and clear the provider.
     */
    @Override
    public void destroy() {
        shutdownIfInitialized();
        nettyServerProvider = null;
    }

    /**
     * 回退并登记本地 Netty 服务端。
     * Fall back to and register a local Netty server.
     *
     * @return 回退实例 / fallback instance
     */
    private static NettyServer fallbackNettyServer() {
        return register(Fallbacks.NETTY_SERVER);
    }

    /**
     * 非 Spring 环境下的回退实例容器。
     * Holder for the fallback instance outside Spring.
     */
    private static final class Fallbacks {

        private static final NettyServer NETTY_SERVER = new NettyServer();
    }
}
