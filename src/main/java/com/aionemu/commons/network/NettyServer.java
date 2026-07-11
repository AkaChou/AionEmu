package com.aionemu.commons.network;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.services.ServiceContext;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Netty 服务端传输，按配置绑定多个监听并管理客户端通道。
 * Netty server transport that binds multiple listeners and manages client channels.
 */
@Slf4j
public class NettyServer implements ServerTransport {

    private final NettyServerCfg[] cfgs;
    private final Supplier<Executor> connectionExecutor;
    private final List<Channel> serverChannels = new ArrayList<>();
    private final ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NettyEventLoopProvider.Allocation eventLoops;

    /**
     * 使用默认连接执行器构造服务端。
     * Construct server with default connection executor.
     *
     * @param cfgs 服务端配置 / Server configurations
     */
    public NettyServer(NettyServerCfg... cfgs) {
        this(CommonsNetworkThreadPoolServices::threadPoolManager, cfgs);
    }

    /**
     * 使用指定连接执行器构造服务端。
     * Construct server with a fixed connection executor.
     *
     * @param connectionExecutor 连接/断开执行器 / Connection/disconnection executor
     * @param cfgs 服务端配置 / Server configurations
     */
    public NettyServer(Executor connectionExecutor, NettyServerCfg... cfgs) {
        this(() -> connectionExecutor, cfgs);
    }

    /**
     * 使用执行器供应器构造服务端。
     * Construct server with an executor supplier.
     *
     * @param connectionExecutor 执行器供应器 / Executor supplier
     * @param cfgs 服务端配置 / Server configurations
     */
    private NettyServer(Supplier<Executor> connectionExecutor, NettyServerCfg... cfgs) {
        this.connectionExecutor = connectionExecutor;
        this.cfgs = cfgs;
    }

    /**
     * 绑定所有监听（已启动则跳过）。
     * Bind all listeners (no-op when already started).
     */
    @Override
    public synchronized void connect() {
        if (eventLoops != null) {
            return;
        }

        eventLoops = NettyEventLoopProvider.acquire();
        String serviceContext = ServiceContext.current();
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
                            channel.pipeline().addLast(new NettyConnectionHandler(cfg.factory(), connectionExecutor.get(), serviceContext));
                        }
                    })
                    .bind(address(cfg))
                    .syncUninterruptibly();

                serverChannels.add(bindFuture.channel());
                log.info(I18n.get("log.55d4847652e6", cfg.hostName(), cfg.port(), cfg.connectionName()));
            }
        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

    /**
     * 关闭监听与客户端通道，并释放自持有事件循环。
     * Close listeners and client channels, releasing owned event loops.
     */
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

    /**
     * 获取活跃客户端连接数。
     * Get active client connection count.
     *
     * @return 活跃连接数 / Active connections
     */
    @Override
    public int getActiveConnections() {
        return clientChannels.size();
    }

    /**
     * 将配置解析为监听地址。
     * Resolve bind address from configuration.
     *
     * @param cfg 服务端配置 / Server configuration
     * Bind address
     */
    private InetSocketAddress address(NettyServerCfg cfg) {
        if ("*".equals(cfg.hostName())) {
            return new InetSocketAddress(cfg.port());
        }
        return new InetSocketAddress(cfg.hostName(), cfg.port());
    }
}
