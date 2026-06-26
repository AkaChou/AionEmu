package com.aionemu.commons.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer implements ServerTransport {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    private final NettyServerCfg[] cfgs;
    private final Supplier<Executor> connectionExecutor;
    private final List<Channel> serverChannels = new ArrayList<>();
    private final ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NettyEventLoopProvider.Allocation eventLoops;

    public NettyServer(NettyServerCfg... cfgs) {
        this(CommonsNetworkThreadPoolServices::threadPoolManager, cfgs);
    }

    public NettyServer(Executor connectionExecutor, NettyServerCfg... cfgs) {
        this(() -> connectionExecutor, cfgs);
    }

    private NettyServer(Supplier<Executor> connectionExecutor, NettyServerCfg... cfgs) {
        this.connectionExecutor = connectionExecutor;
        this.cfgs = cfgs;
    }

    @Override
    public synchronized void connect() {
        if (eventLoops != null) {
            return;
        }

        eventLoops = NettyEventLoopProvider.acquire();
        try {
            for (NettyServerCfg cfg : cfgs) {
                ChannelFuture bindFuture = new ServerBootstrap()
                    .group(eventLoops.bossGroup(), eventLoops.workerGroup())
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            clientChannels.add(channel);
                            channel.pipeline().addLast(new NettyConnectionHandler(cfg.factory(), connectionExecutor.get()));
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

        if (eventLoops != null) {
            eventLoops.shutdownGracefully();
            eventLoops = null;
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
