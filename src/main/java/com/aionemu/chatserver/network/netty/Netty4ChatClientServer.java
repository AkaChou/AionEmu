package com.aionemu.chatserver.network.netty;

import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Netty4ChatClientServer {

    private static final Logger log = LoggerFactory.getLogger(Netty4ChatClientServer.class);
    private static final int MAX_PACKET_LENGTH = 8192 * 2;

    private final InetSocketAddress address;
    private final ClientPacketHandler clientPacketHandler;
    private final ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    Netty4ChatClientServer(InetSocketAddress address, ClientPacketHandler clientPacketHandler) {
        this.address = address;
        this.clientPacketHandler = clientPacketHandler;
    }

    synchronized void connect() {
        if (bossGroup != null) {
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            serverChannel = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        clientChannels.add(channel);
                        channel.pipeline().addLast(
                            new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, MAX_PACKET_LENGTH, 0, 2, -2, 2, true),
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
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
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
                byte[] bytes = new byte[input.readableBytes()];
                input.readBytes(bytes);
                ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, bytes);
                delegate.nettyMessageReceived(channelBuffer);
            } finally {
                input.release();
            }
        }
    }
}
