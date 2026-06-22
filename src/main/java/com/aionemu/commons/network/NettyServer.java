package com.aionemu.commons.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer implements ServerTransport {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    private final NettyServerCfg[] cfgs;
    private final List<Channel> serverChannels = new ArrayList<>();
    private final ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(NettyServerCfg... cfgs) {
        this.cfgs = cfgs;
    }

    @Override
    public synchronized void connect() {
        if (bossGroup != null) {
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            for (NettyServerCfg cfg : cfgs) {
                ChannelFuture bindFuture = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            clientChannels.add(channel);
                            channel.pipeline().addLast(new NettyConnectionHandler(cfg.factory()));
                        }
                    })
                    .bind(address(cfg))
                    .syncUninterruptibly();

                serverChannels.add(bindFuture.channel());
                log.info("Netty server listening on {}:{} for {}", cfg.hostName(), cfg.port(), cfg.connectionName());
            }
        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

    @Override
    public synchronized void shutdown() {
        for (Channel serverChannel : serverChannels) {
            serverChannel.close().syncUninterruptibly();
        }
        serverChannels.clear();
        clientChannels.close().syncUninterruptibly();

        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
    }

    @Override
    public int getActiveConnections() {
        return clientChannels.size();
    }

    private InetSocketAddress address(NettyServerCfg cfg) {
        if ("*".equals(cfg.hostName())) {
            return new InetSocketAddress(cfg.port());
        }
        return new InetSocketAddress(cfg.hostName(), cfg.port());
    }
}
