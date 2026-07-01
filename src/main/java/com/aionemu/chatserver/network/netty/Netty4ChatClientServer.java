package com.aionemu.chatserver.network.netty;

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
@Slf4j
final class Netty4ChatClientServer {

    private static final int MAX_PACKET_LENGTH = 8192 * 2;

    private final InetSocketAddress address;
    private final ClientPacketHandler clientPacketHandler;
    private final ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NettyEventLoopProvider.Allocation eventLoops;
    private Channel serverChannel;

    Netty4ChatClientServer(InetSocketAddress address, ClientPacketHandler clientPacketHandler) {
        this.address = address;
        this.clientPacketHandler = clientPacketHandler;
    }

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
            log.info("Netty server listening on {}:{} for Chat Client Connections", address.getAddress().getHostAddress(), address.getPort());
        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

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

    private static final class Netty4ClientChannelHandler extends ChannelInboundHandlerAdapter {

        private final ClientChannelHandler delegate;

        private Netty4ClientChannelHandler(ClientChannelHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void channelActive(ChannelHandlerContext context) {
            delegate.nettyChannelActive(context.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            delegate.nettyChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            delegate.nettyExceptionCaught(cause);
            context.close();
        }

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
