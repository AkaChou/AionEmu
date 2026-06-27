package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import com.aionemu.commons.network.NettyEventLoopProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NettyTransportLifecycle implements AionTransportLifecycle {

    private static final Logger log = LoggerFactory.getLogger(NettyTransportLifecycle.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Override
    public TransportMode mode() {
        return TransportMode.NETTY;
    }

    @Override
    public synchronized void start() {
        if (bossGroup != null) {
            return;
        }

        bossGroup = NettyEventLoopProvider.newBossGroup();
        workerGroup = NettyEventLoopProvider.newWorkerGroup();
        NettyEventLoopProvider.useShared(bossGroup, workerGroup);
        log.info("Netty transport event loops started.");
    }

    public synchronized ChannelFuture bind(AionNettyEndpoint endpoint, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
        start();
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(initializer);

        log.info("Binding Netty endpoint {} on {}:{}", endpoint.getName(), endpoint.getHost(), endpoint.getPort());
        return bootstrap.bind(endpoint.getAddress()).sync();
    }

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
        log.info("Netty transport event loops stopped.");
    }
}
