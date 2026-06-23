package com.aionemu.commons.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClient implements ServerTransport {

    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

    private final InetSocketAddress address;
    private final String connectionName;
    private final NettyConnectionFactory connectionFactory;
    private final Executor disconnectionExecutor;
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NettyEventLoopProvider.Allocation eventLoops;
    private Channel channel;

    public NettyClient(InetSocketAddress address, String connectionName, NettyConnectionFactory connectionFactory) {
        this(address, connectionName, connectionFactory, null);
    }

    NettyClient(InetSocketAddress address, String connectionName, NettyConnectionFactory connectionFactory, Executor disconnectionExecutor) {
        this.address = address;
        this.connectionName = connectionName;
        this.connectionFactory = connectionFactory;
        this.disconnectionExecutor = disconnectionExecutor;
    }

    @Override
    public synchronized void connect() {
        if (channel != null && channel.isOpen()) {
            return;
        }

        eventLoops = NettyEventLoopProvider.acquire();
        try {
            ChannelFuture connectFuture = new Bootstrap()
                .group(eventLoops.workerGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channels.add(channel);
                        if (disconnectionExecutor == null) {
                            channel.pipeline().addLast(new NettyConnectionHandler(connectionFactory));
                        } else {
                            channel.pipeline().addLast(new NettyConnectionHandler(connectionFactory, disconnectionExecutor));
                        }
                    }
                })
                .connect(address)
                .syncUninterruptibly();

            channel = connectFuture.channel();
            log.info("Netty client connected to {} for {}", address, connectionName);
        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

    @Override
    public synchronized void shutdown() {
        channels.close().syncUninterruptibly();
        channel = null;

        if (eventLoops != null) {
            eventLoops.shutdownGracefully();
            eventLoops = null;
        }
    }

    @Override
    public int getActiveConnections() {
        return channels.size();
    }
}
