package com.aionemu.commons.network;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.services.ServiceContext;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Netty 客户端传输，主动连接远端并托管通道生命周期。
 * Netty client transport that connects outbound and manages channel lifecycle.
 */
@Slf4j
public class NettyClient implements ServerTransport {

    private final InetSocketAddress address;
    private final String connectionName;
    private final NettyConnectionFactory connectionFactory;
    private final Executor disconnectionExecutor;
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NettyEventLoopProvider.Allocation eventLoops;
    private Channel channel;

    /**
     * 使用默认断开连接执行器构造客户端。
     * Construct client with default disconnection executor.
     *
     * @param address 远端地址 / Remote address
     * @param connectionName 连接名称 / Connection name
     * @param connectionFactory 连接工厂 / Connection factory
     */
    public NettyClient(InetSocketAddress address, String connectionName, NettyConnectionFactory connectionFactory) {
        this(address, connectionName, connectionFactory, null);
    }

    /**
     * 构造客户端（可指定断开连接执行器）。
     * Construct client with optional disconnection executor.
     *
     * @param address 远端地址 / Remote address
     * @param connectionName 连接名称 / Connection name
     * @param connectionFactory 连接工厂 / Connection factory
     * @param disconnectionExecutor 断开连接执行器，null 时用默认线程池 / Disconnection executor, null uses default pool
     */
    NettyClient(InetSocketAddress address, String connectionName, NettyConnectionFactory connectionFactory, Executor disconnectionExecutor) {
        this.address = address;
        this.connectionName = connectionName;
        this.connectionFactory = connectionFactory;
        this.disconnectionExecutor = disconnectionExecutor;
    }

    /**
     * 连接远端（已连接则跳过）。
     * Connect to remote (no-op when already open).
     */
    @Override
    public synchronized void connect() {
        if (channel != null && channel.isOpen()) {
            return;
        }

        eventLoops = NettyEventLoopProvider.acquire();
        String serviceContext = ServiceContext.current();
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
                            channel.pipeline().addLast(new NettyConnectionHandler(connectionFactory, CommonsNetworkThreadPoolServices.threadPoolManager(), serviceContext));
                        } else {
                            channel.pipeline().addLast(new NettyConnectionHandler(connectionFactory, disconnectionExecutor, serviceContext));
                        }
                    }
                })
                .connect(address)
                .syncUninterruptibly();

            channel = connectFuture.channel();
            log.info(I18n.get("log.fe77f296ff0c", address, connectionName));
        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

    /**
     * 关闭通道并释放事件循环（若自持有）。
     * Close channels and release event loops when owned.
     */
    @Override
    public synchronized void shutdown() {
        channels.close().syncUninterruptibly();
        channel = null;

        if (eventLoops != null) {
            eventLoops.shutdownGracefully();
            eventLoops = null;
        }
    }

    /**
     * 获取活跃连接数。
     * Get active connection count.
     *
     * @return 活跃连接数 / Active connections
     */
    @Override
    public int getActiveConnections() {
        return channels.size();
    }
}
