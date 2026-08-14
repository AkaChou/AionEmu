package com.aionemu.commons.network;

import com.aionemu.boot.i18n.I18n;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Netty 入站处理器，桥接通道与 {@link AConnection}，负责帧解析与写出。
 * Netty inbound handler bridging channel and {@link AConnection} for framing and writes.
 */
@Slf4j
public class NettyConnectionHandler extends ChannelInboundHandlerAdapter implements ConnectionTransport {

    private final NettyConnectionFactory connectionFactory;
    private final Executor disconnectionExecutor;
    private final String serviceContext;
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);

    private ChannelHandlerContext context;
    private AConnection connection;

    /**
     * 使用默认断开连接执行器构造。
     * Construct with default disconnection executor.
     *
     * @param connectionFactory 连接工厂 / Connection factory
     */
    public NettyConnectionHandler(NettyConnectionFactory connectionFactory) {
        this(connectionFactory, CommonsNetworkThreadPoolServices.threadPoolManager());
    }

    /**
     * 使用指定断开连接执行器构造。
     * Construct with a disconnection executor.
     *
     * @param connectionFactory 连接工厂 / Connection factory
     * @param disconnectionExecutor 断开连接执行器 / Disconnection executor
     */
    public NettyConnectionHandler(NettyConnectionFactory connectionFactory, Executor disconnectionExecutor) {
        this(connectionFactory, disconnectionExecutor, ServiceContext.current());
    }

    /**
     * 完整构造。
     * Full constructor.
     *
     * @param connectionFactory 连接工厂 / Connection factory
     * @param disconnectionExecutor 断开连接执行器 / Disconnection executor
     * @param serviceContext 服务上下文 / Service context
     */
    NettyConnectionHandler(NettyConnectionFactory connectionFactory, Executor disconnectionExecutor, String serviceContext) {
        this.connectionFactory = connectionFactory;
        this.disconnectionExecutor = disconnectionExecutor;
        this.serviceContext = serviceContext;
    }

    /**
     * 通道激活时创建业务连接并初始化。
     * Create business connection and initialize on channel active.
     *
     * @param context 通道上下文 / Channel context
     * @throws IOException 连接创建失败 / Connection creation failure
     */
    @Override
    public void channelActive(ChannelHandlerContext context) throws IOException {
        this.context = context;
        try (ServiceContext.Scope ignored = ServiceContext.use(serviceContext)) {
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
    }

    /**
     * 读取入站消息并在连接上下文中处理。
     * Read inbound message and process in connection context.
     *
     * @param context 通道上下文 / Channel context
     * @param message 入站消息 / Inbound message
     */
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

    /**
     * 将 ByteBuf 数据写入读缓冲并解析完整帧。
     * Copy ByteBuf data into read buffer and parse complete frames.
     *
     * @param context 通道上下文 / Channel context
     * @param message 入站消息 / Inbound message
     */
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

    /**
     * 通道失活时通知断开。
     * Notify disconnect on channel inactive.
     *
     * @param context 通道上下文 / Channel context
     */
    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (connection != null) {
            notifyDisconnect();
        }
    }

    /**
     * 捕获异常并强制关闭。
     * Catch exception and force close.
     *
     * @param context 通道上下文 / Channel context
     * @param cause 异常原因 / Cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.debug("Netty connection error from {}", getIP(), cause);
        close(true);
    }

    /**
     * 获取对端 IP。
     * Get remote IP.
     *
     * @return 对端 IP 或 "unknown" / IP or "unknown"
     */
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

    /**
     * 在事件循环中刷写待发送数据。
     * Flush pending writes on the event loop.
     */
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

    /**
     * 关闭连接并通知断开。
     * Close connection and notify disconnect.
     *
     * @param forced 是否强制关闭 / Whether to force close
     */
    @Override
    public void close(boolean forced) {
        if (onlyClose()) {
            notifyDisconnect();
        }
    }

    /**
     * 仅关闭底层通道（幂等）。
     * Close underlying channel only (idempotent).
     *
     * @return 是否实际关闭 / Whether close was performed
     */
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

    /**
     * 解析读缓冲中已就绪的完整帧。
     * Parse complete ready frames from the read buffer.
     *
     * @return 是否解析成功 / Whether parsing succeeded
     */
    private boolean processReadyFrames() {
        ByteBuffer readBuffer = connection.readBuffer;
        readBuffer.flip();
        try {
            while (readBuffer.remaining() >= Short.BYTES && readBuffer.remaining() >= frameSize(readBuffer)) {
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

    /**
     * 读取帧头中的帧长度。
     * Read frame size from frame header.
     *
     * @param buffer 缓冲 / Buffer
     * @return 帧大小 / Frame size
     */
    private int frameSize(ByteBuffer buffer) {
        return Short.toUnsignedInt(buffer.getShort(buffer.position()));
    }

    /**
     * 解析单帧并交给连接处理。
     * Parse a single frame and hand off to connection.
     *
     * @param buffer 缓冲 / Buffer
     * @return 是否处理成功 / Whether processing succeeded
     */
    private boolean parse(ByteBuffer buffer) {
        int size = 0;
        try {
            size = Short.toUnsignedInt(buffer.getShort());
            if (size < Short.BYTES) {
                return false;
            }
            int payloadSize = size - Short.BYTES;
            ByteBuffer packetBuffer = (ByteBuffer) buffer.slice().limit(payloadSize);
            packetBuffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.position(buffer.position() + payloadSize);
            return connection.processData(packetBuffer);
        } catch (IllegalArgumentException e) {
            log.warn(I18n.get("log.cd089b86f7e3", connection, size, buffer.remaining(), e));
            return false;
        }
    }

    /**
     * 在连接服务上下文中刷写。
     * Flush writes in connection service context.
     */
    private void flushWrites() {
        if (connection == null || context == null) {
            return;
        }
        try (ServiceContext.Scope ignored = ServiceContext.use(connection.getServiceContext())) {
            flushWritesInConnectionContext();
        }
    }

    /**
     * 同步写出缓冲中的数据并刷写通道。
     * Synchronously write buffered data and flush the channel.
     */
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

    /**
     * 将 NIO 缓冲复制为 ByteBuf 写出并消费源位置。
     * Copy NIO buffer to ByteBuf, write it, and advance source position.
     *
     * @param source 源缓冲 / Source buffer
     */
    private void writeAndConsume(ByteBuffer source) {
        if (!source.hasRemaining()) {
            return;
        }
        ByteBuf output = Unpooled.copiedBuffer(source);
        source.position(source.limit());
        context.write(output);
    }

    /**
     * 仅通知一次断开连接任务。
     * Notify disconnection task at most once.
     */
    private void notifyDisconnect() {
        if (disconnectNotified.compareAndSet(false, true)) {
            disconnectionExecutor.execute(new DisconnectionTask(connection));
        }
    }

    /**
     * 在连接绑定的服务上下文中运行任务。
     * Run task in the connection's service context.
     *
     * @param runnable 任务 / Task
     */
    private void runInConnectionContext(Runnable runnable) {
        try (ServiceContext.Scope ignored = ServiceContext.use(connection.getServiceContext())) {
            runnable.run();
        }
    }
}
