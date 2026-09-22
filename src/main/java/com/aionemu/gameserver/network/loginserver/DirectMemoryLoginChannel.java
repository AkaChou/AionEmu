package com.aionemu.gameserver.network.loginserver;

import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection.State;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 内嵌模式下 GameServer 与 LoginServer 的进程内直连通道。
 * In-process direct channel between GameServer and LoginServer for embedded mode.
 * <p>通道仅短路本地 TCP/握手开销，封包序列化、opcode 分发与认证状态机保持不变。
 * The channel only removes local socket/handshake overhead; packet encoding, opcode dispatch,
 * and authentication state machines stay unchanged.</p>
 */
public final class DirectMemoryLoginChannel implements LoginMessageDispatcher {

    private final DirectLoginServerConnection gameServerSide;
    private final DirectGsConnection loginServerSide;

    private DirectMemoryLoginChannel(DirectLoginServerConnection gameServerSide, DirectGsConnection loginServerSide) {
        this.gameServerSide = gameServerSide;
        this.loginServerSide = loginServerSide;
    }

    /**
     * 打开进程内双向通道并触发 GS 认证。
     * Opens the in-process bidirectional channel and starts GS authentication.
     * @return 直连通道 / direct channel
     * @throws IllegalStateException 连接创建失败 / connection creation failed
     */
    public static DirectMemoryLoginChannel open() {
        DirectGsConnection loginServerSide;
        DirectLoginServerConnection gameServerSide;
        try (ServiceContext.Scope ignored = ServiceContext.use("game")) {
            gameServerSide = new DirectLoginServerConnection(new DirectTransport("127.0.0.1"));
        }
        try (ServiceContext.Scope ignored = ServiceContext.use("login")) {
            loginServerSide = new DirectGsConnection(new DirectTransport("127.0.0.1"));
        }
        gameServerSide.transportBridge().connectTo(gameServerSide, loginServerSide);
        loginServerSide.transportBridge().connectTo(loginServerSide, gameServerSide);
        DirectMemoryLoginChannel channel = new DirectMemoryLoginChannel(gameServerSide, loginServerSide);
        try (ServiceContext.Scope ignored = ServiceContext.use("login")) {
            loginServerSide.initializeDirect();
        }
        try (ServiceContext.Scope ignored = ServiceContext.use("game")) {
            gameServerSide.initializeDirect();
        }
        return channel;
    }

    DirectLoginServerConnection gameServerConnection() {
        return gameServerSide;
    }

    @Override
    public boolean sendPacket(LsServerPacket packet) {
        if (!isAuthed()) {
            return false;
        }
        gameServerSide.sendPacket(packet);
        return true;
    }

    @Override
    public boolean isAuthed() {
        return gameServerSide.getState() == State.AUTHED;
    }

    @Override
    public void close() {
        gameServerSide.close(true);
        loginServerSide.close(true);
    }

    /**
     * 把源端写出缓冲中的整帧直接交给对端处理。
     * Hand whole frames from the source write buffer directly to the peer.
     * <p>{@code writeData} 完成后缓冲已处于读模式（position=0、limit=帧长），因此禁止再次 flip；
     * 载荷视图必须与 socket 读缓冲一致使用小端序；投递给对端时必须切到接收方自己的服务上下文，
     * 否则对端的状态机与延迟任务会带着发送方上下文运行（socket 模式下每端各自在自身上下文中处理入站）。
     * After {@code writeData} the buffer is already in read mode (position=0, limit=frame size), so it must not
     * be flipped again; the payload view must be little-endian to match the socket read buffer; and delivery
     * must switch to the receiving side's service context, otherwise the peer's state machine and delayed tasks
     * inherit the sender's context (in socket mode each side handles inbound frames in its own context).</p>
     * @param source 写出端 / write side
     * @param target 接收端 / receiving side
     */
    static void pumpWrites(DirectEndpoint source, DirectEndpoint target) {
        try (ServiceContext.Scope ignored = ServiceContext.use(source.serviceContext())) {
            ByteBuffer buffer = source.writeBuffer();
            synchronized (source.syncGuard()) {
                buffer.clear();
                while (source.isWriteEnabled() && source.writeDirect(buffer)) {
                    if (buffer.hasRemaining()) {
                        int frameSize = Short.toUnsignedInt(buffer.getShort());
                        int payloadSize = frameSize - Short.BYTES;
                        if (payloadSize < 0 || payloadSize > buffer.remaining()) {
                            throw new IllegalStateException("Invalid direct login frame size: " + frameSize);
                        }
                        ByteBuffer packet = buffer.slice().limit(payloadSize);
                        packet.order(ByteOrder.LITTLE_ENDIAN);
                        try (ServiceContext.Scope ignoredTarget = ServiceContext.use(target.serviceContext())) {
                            target.processDirect(packet);
                        }
                    }
                    buffer.clear();
                }
            }
        }
    }

    /**
     * 直连端点的受保护生命周期访问桥。
     * Bridge exposing protected endpoint lifecycle operations.
     */
    interface DirectEndpoint {
        boolean writeDirect(ByteBuffer buffer);

        boolean processDirect(ByteBuffer buffer);

        boolean isWriteEnabled();

        Object syncGuard();

        ByteBuffer writeBuffer();

        String serviceContext();

        void markClosed();

        void cleanupDirect();
    }

    /**
     * GameServer 侧连接桥。
     * GameServer-side connection bridge.
     */
    private static final class DirectLoginServerConnection extends LoginServerConnection implements DirectEndpoint {

        DirectLoginServerConnection(ConnectionTransport transport) {
            super(transport);
        }

        void initializeDirect() {
            initialized();
        }

        @Override
        public boolean writeDirect(ByteBuffer buffer) {
            return writeData(buffer);
        }

        @Override
        public boolean processDirect(ByteBuffer buffer) {
            return processData(buffer);
        }

        @Override
        public boolean isWriteEnabled() {
            return !isWriteDisabled();
        }

        @Override
        public Object syncGuard() {
            return guard;
        }

        @Override
        public ByteBuffer writeBuffer() {
            return writeBuffer;
        }

        @Override
        public String serviceContext() {
            return getServiceContext();
        }

        @Override
        public void markClosed() {
            closed = true;
        }

        @Override
        public void cleanupDirect() {
            onDisconnect();
        }

        DirectTransport transportBridge() {
            return (DirectTransport) transport();
        }
    }

    /**
     * LoginServer 侧 GS 连接桥。
     * LoginServer-side GS connection bridge.
     */
    private static final class DirectGsConnection extends GsConnection implements DirectEndpoint {

        DirectGsConnection(ConnectionTransport transport) {
            super(transport);
        }

        void initializeDirect() {
            initialized();
        }

        @Override
        public boolean writeDirect(ByteBuffer buffer) {
            return writeData(buffer);
        }

        @Override
        public boolean processDirect(ByteBuffer buffer) {
            return processData(buffer);
        }

        @Override
        public boolean isWriteEnabled() {
            return !isWriteDisabled();
        }

        @Override
        public Object syncGuard() {
            return guard;
        }

        @Override
        public ByteBuffer writeBuffer() {
            return writeBuffer;
        }

        @Override
        public String serviceContext() {
            return getServiceContext();
        }

        @Override
        public void markClosed() {
            closed = true;
        }

        @Override
        public void cleanupDirect() {
            onDisconnect();
        }

        DirectTransport transportBridge() {
            return (DirectTransport) transport();
        }
    }

    /**
     * 单向内存传输：写出端直接把封包缓冲交给对端 processData。
     * One-way in-memory transport: writes are handed directly to peer processData.
     */
    private static final class DirectTransport implements ConnectionTransport {

        private final String ip;
        private volatile DirectEndpoint owner;
        private volatile DirectEndpoint peer;

        DirectTransport(String ip) {
            this.ip = ip;
        }

        void connectTo(DirectEndpoint owner, DirectEndpoint peer) {
            this.owner = owner;
            this.peer = peer;
        }

        @Override
        public String getIP() {
            return ip;
        }

        @Override
        public void enableWriteInterest() {
            DirectEndpoint source = owner;
            DirectEndpoint target = peer;
            if (source == null || target == null) {
                return;
            }

            pumpWrites(source, target);
        }

        @Override
        public void close(boolean forced) {
            cleanupEndpoint(owner);
            cleanupEndpoint(peer);
        }

        @Override
        public boolean onlyClose() {
            return false;
        }
    }

    /**
     * 在端点自身服务上下文中执行关闭清理。
     * Runs an endpoint's close cleanup inside its own service context.
     * @param endpoint 待清理端点 / endpoint to clean up
     */
    static void cleanupEndpoint(DirectEndpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        try (ServiceContext.Scope ignored = ServiceContext.use(endpoint.serviceContext())) {
            endpoint.markClosed();
            endpoint.cleanupDirect();
        }
    }
}
