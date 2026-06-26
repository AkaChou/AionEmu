package com.aionemu.commons.network;

import com.aionemu.commons.services.ServiceContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyConnectionHandler extends ChannelInboundHandlerAdapter implements ConnectionTransport {

    private static final Logger log = LoggerFactory.getLogger(NettyConnectionHandler.class);

    private final NettyConnectionFactory connectionFactory;
    private final Executor disconnectionExecutor;
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);

    private ChannelHandlerContext context;
    private AConnection connection;

    public NettyConnectionHandler(NettyConnectionFactory connectionFactory) {
        this(connectionFactory, CommonsNetworkThreadPoolServices.threadPoolManager());
    }

    public NettyConnectionHandler(NettyConnectionFactory connectionFactory, Executor disconnectionExecutor) {
        this.connectionFactory = connectionFactory;
        this.disconnectionExecutor = disconnectionExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws IOException {
        this.context = context;
        this.connection = connectionFactory.create(this);
        if (this.connection == null) {
            context.close();
            return;
        }
        runInConnectionContext(new Runnable() {
            @Override
            public void run() {
                connection.initialized();
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) {
        if (connection != null) {
            runInConnectionContext(new Runnable() {
                @Override
                public void run() {
                    read(context, message);
                }
            });
            return;
        }
        read(context, message);
    }

    private void read(ChannelHandlerContext context, Object message) {
        if (!(message instanceof ByteBuf byteBuf)) {
            context.fireChannelRead(message);
            return;
        }

        try {
            if (connection == null) {
                context.close();
                return;
            }

            while (byteBuf.isReadable()) {
                ByteBuffer readBuffer = connection.readBuffer;
                int writableBytes = Math.min(byteBuf.readableBytes(), readBuffer.remaining());
                if (writableBytes == 0) {
                    close(true);
                    return;
                }

                byte[] chunk = new byte[writableBytes];
                byteBuf.readBytes(chunk);
                readBuffer.put(chunk);

                if (!processReadyFrames()) {
                    close(true);
                    return;
                }
            }
        } finally {
            byteBuf.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (connection != null) {
            notifyDisconnect();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.debug("Netty connection error from {}", getIP(), cause);
        close(true);
    }

    @Override
    public String getIP() {
        if (context == null || context.channel().remoteAddress() == null) {
            return "unknown";
        }
        if (context.channel().remoteAddress() instanceof InetSocketAddress address) {
            return address.getAddress().getHostAddress();
        }
        return context.channel().remoteAddress().toString();
    }

    @Override
    public void enableWriteInterest() {
        if (context != null) {
            context.executor().execute(new Runnable() {
                @Override
                public void run() {
                    flushWrites();
                }
            });
        }
    }

    @Override
    public void close(boolean forced) {
        if (onlyClose()) {
            notifyDisconnect();
        }
    }

    @Override
    public boolean onlyClose() {
        if (connection == null || connection.closed) {
            return false;
        }
        connection.closed = true;
        if (context != null) {
            context.close();
        }
        return true;
    }

    private boolean processReadyFrames() {
        ByteBuffer readBuffer = connection.readBuffer;
        readBuffer.flip();
        try {
            while (readBuffer.remaining() > 2 && readBuffer.remaining() >= readBuffer.getShort(readBuffer.position())) {
                if (!parse(readBuffer)) {
                    return false;
                }
            }
            return true;
        } finally {
            if (readBuffer.hasRemaining()) {
                readBuffer.compact();
            } else {
                readBuffer.clear();
            }
        }
    }

    private boolean parse(ByteBuffer buffer) {
        short size = 0;
        try {
            size = buffer.getShort();
            if (size > 1) {
                size -= 2;
            }
            ByteBuffer packetBuffer = (ByteBuffer) buffer.slice().limit(size);
            packetBuffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.position(buffer.position() + size);
            return connection.processData(packetBuffer);
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing input from client - account: {} packet size: {} real size: {}", connection, size, buffer.remaining(), e);
            return false;
        }
    }

    private void flushWrites() {
        if (connection == null || context == null) {
            return;
        }
        try (ServiceContext.Scope ignored = ServiceContext.use(connection.getServiceContext())) {
            flushWritesInConnectionContext();
        }
    }

    private void flushWritesInConnectionContext() {

        synchronized (connection.guard) {
            ByteBuffer writeBuffer = connection.writeBuffer;
            if (writeBuffer.hasRemaining()) {
                writeAndConsume(writeBuffer);
            }

            while (!connection.closed) {
                writeBuffer.clear();
                if (!connection.writeData(writeBuffer)) {
                    writeBuffer.limit(0);
                    break;
                }
                writeAndConsume(writeBuffer);
            }

            context.flush();
            if (connection.isPendingClose()) {
                close(connection.isForcedClosing);
            }
        }
    }

    private void writeAndConsume(ByteBuffer source) {
        if (!source.hasRemaining()) {
            return;
        }
        ByteBuf output = Unpooled.copiedBuffer(source);
        source.position(source.limit());
        context.write(output);
    }

    private void notifyDisconnect() {
        if (disconnectNotified.compareAndSet(false, true)) {
            disconnectionExecutor.execute(new DisconnectionTask(connection));
        }
    }

    private void runInConnectionContext(Runnable runnable) {
        try (ServiceContext.Scope ignored = ServiceContext.use(connection.getServiceContext())) {
            runnable.run();
        }
    }
}
