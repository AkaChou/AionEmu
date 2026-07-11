package com.aionemu.chatserver.network.netty;


import com.aionemu.boot.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.chatserver.common.netty.ByteBufPacketReader;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.commons.network.NettyEventLoopProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;

/**
 * 基于 Netty 4 的聊天客户端接入服务端，负责绑定监听地址并转发客户端通道事件。
 * Netty 4 chat-client acceptor that binds the listen address and forwards client channel events.
 */
@Slf4j
@RequiredArgsConstructor
final class Netty4ChatClientServer {

    private static final int MAX_PACKET_LENGTH = 8192 * 2;

    private final InetSocketAddress address;
    private final ClientPacketHandler clientPacketHandler;
    private final ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NettyEventLoopProvider.Allocation eventLoops;
    private Channel serverChannel;

    /**
     * 启动并绑定聊天客户端监听端口；已启动时直接返回。
     * Start and bind the chat-client listen port; no-op when already started.
     */
    synchronized void connect() {
        if (eventLoops != null) {
            return;
        }

        eventLoops = NettyEventLoopProvider.acquire();
        try {
            serverChannel = new ServerBootstrap()
                .group(eventLoops.bossGroup(), eventLoops.workerGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    /**
                     * 初始化客户端通道管线：长度帧解码与业务处理器。
                     * Initialize the client channel pipeline: length-frame decoder and business handler.
                     *
                     * @param channel 客户端套接字通道 / Client socket channel
                     */
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        clientChannels.add(channel);
                        channel.pipeline().addLast(
                            new LengthFieldBasedFrameDecoder(java.nio.ByteOrder.LITTLE_ENDIAN, MAX_PACKET_LENGTH, 0, 2, -2, 2, true),
                            new Netty4ClientChannelHandler(new ClientChannelHandler(clientPacketHandler))
                        );
                    }
                })
                .bind(address)
                .syncUninterruptibly()
                .channel();
            log.info(I18n.get("log.47b8fdc8c11c", address.getAddress().getHostAddress(), address.getPort()));
        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

    /**
     * 关闭服务端通道、所有客户端连接并释放事件循环。
     * Close the server channel, all client connections, and release event loops.
     */
    synchronized void shutdown() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        clientChannels.close().syncUninterruptibly();
        if (eventLoops != null) {
            eventLoops.shutdownGracefully();
            eventLoops = null;
        }
    }

    /**
     * Netty 入站适配器，将通道生命周期与读事件委托给 {@link ClientChannelHandler}。
     * Netty inbound adapter that delegates channel lifecycle and read events to {@link ClientChannelHandler}.
     */
    @RequiredArgsConstructor
    private static final class Netty4ClientChannelHandler extends ChannelInboundHandlerAdapter {

        private final ClientChannelHandler delegate;

        /**
         * 通道激活时通知业务处理器。
         * Notify the business handler when the channel becomes active.
         *
         * @param context 通道上下文 / Channel handler context
         */
        @Override
        public void channelActive(ChannelHandlerContext context) {
            delegate.nettyChannelActive(context.channel());
        }

        /**
         * 通道失活时通知业务处理器。
         * Notify the business handler when the channel becomes inactive.
         *
         * @param context 通道上下文 / Channel handler context
         */
        @Override
        public void channelInactive(ChannelHandlerContext context) {
            delegate.nettyChannelInactive();
        }

        /**
         * 捕获异常后交给业务处理并关闭通道。
         * Forward exceptions to the business handler and close the channel.
         *
         * @param context 通道上下文 / Channel handler context
         * @param cause 异常原因 / Exception cause
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            delegate.nettyExceptionCaught(cause);
            context.close();
        }

        /**
         * 读取完整帧后委托解析；非 {@link ByteBuf} 继续向下游传递。
         * Delegate complete frames for parsing; non-{@link ByteBuf} messages are forwarded downstream.
         *
         * @param context 通道上下文 / Channel handler context
         * Inbound message
         */
        @Override
        public void channelRead(ChannelHandlerContext context, Object message) {
            if (!(message instanceof ByteBuf input)) {
                context.fireChannelRead(message);
                return;
            }

            try {
                delegate.nettyMessageReceived(new ByteBufPacketReader(input));
            } finally {
                input.release();
            }
        }
    }
}
