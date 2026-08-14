package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.NettyEventLoopProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty 传输生命周期：管理 boss/worker 事件循环，并提供端点绑定。
 * Netty transport lifecycle: manages boss/worker event loops and endpoint binding.
 */
@Slf4j
@Component
public class NettyTransportLifecycle implements AionTransportLifecycle {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * {@inheritDoc}
     */
    @Override
    public TransportMode mode() {
        return TransportMode.NETTY;
    }

    /**
     * 初始化并注册共享 boss/worker 事件循环（已启动则跳过）。
     * Initialize and register shared boss/worker event loops (skip if already started).
     */
    @Override
    public synchronized void start() {
        if (bossGroup != null) {
            return;
        }

        bossGroup = NettyEventLoopProvider.newBossGroup();
        workerGroup = NettyEventLoopProvider.newWorkerGroup();
        NettyEventLoopProvider.useShared(bossGroup, workerGroup);
        log.info(I18n.get("log.486cc96056f0"));
    }

    /**
     * 在共享事件循环上绑定服务端端点。
     * Bind a server endpoint on the shared event loops.
     *
     * @param endpoint 监听端点 / Listen endpoint
     * @param initializer 子通道初始化器 / Child channel initializer
     * @return 绑定完成的通道 Future / Channel future after bind completes
     * @throws InterruptedException 绑定等待被中断 / Interrupted while waiting for bind
     */
    public synchronized ChannelFuture bind(AionNettyEndpoint endpoint, ChannelInitializer<SocketChannel> initializer)
        throws InterruptedException {
        start();
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(initializer);

        log.info(I18n.get("log.41517cd8e76f", endpoint.getName(), endpoint.getHost(), endpoint.getPort()));
        return bootstrap.bind(endpoint.getAddress()).sync();
    }

    /**
     * 优雅关闭事件循环并清除共享引用。
     * Gracefully shut down event loops and clear shared references.
     */
    @Override
    public synchronized void stop() {
        if (bossGroup == null) {
            return;
        }

        NettyEventLoopProvider.clearShared(bossGroup, workerGroup);
        workerGroup.shutdownGracefully().syncUninterruptibly();
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup = null;
        bossGroup = null;
        log.info(I18n.get("log.dea51a6fd4fe"));
    }
}
