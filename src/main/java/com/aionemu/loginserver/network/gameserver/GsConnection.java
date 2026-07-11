package com.aionemu.loginserver.network.gameserver;

import com.aionemu.boot.i18n.I18n;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.PingPongThread;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.network.factories.GsPacketHandlerFactory;
import com.aionemu.loginserver.service.LoginThreadPoolServices;
import lombok.extern.slf4j.Slf4j;

/**
 * 表示 LoginServer 与 GameServer 之间的一条网络连接。
 * Object representing a connection between LoginServer and GameServer.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class GsConnection extends AConnection {

    /**
     * GS 连接可能的状态。
     * Possible states of a GsConnection.
     */
    public enum State {

        /**
         * 已连接但尚未认证。
         * Connected but not yet authenticated.
         */
        CONNECTED,
        /**
         * 游戏服已通过认证。
         * GameServer is authenticated.
         */
        AUTHED
    }

    /**
     * 待发送服务端封包队列。
     * Queue of server packets waiting to be sent.
     */
    private final Deque<GsServerPacket> sendMsgQueue = new ArrayDeque<>();
    /**
     * 当前连接状态。
     * Current connection state.
     */
    private State state;
    /**
     * 本连接对应的游戏服信息。
     * GameServerInfo bound to this connection.
     */
    private GameServerInfo gameServerInfo = null;
    private PingPongThread pingThread;

    /**
     * 基于传输层创建 GS 连接。
     * Create a GS connection over the given transport.
     *
     * Connection transport
     */
    public GsConnection(ConnectionTransport transport) {
        super(transport, 8192 * 8, 8192 * 8);
    }

    /**
     * 由传输层帧处理器调用；缓冲区中包含一个待处理封包。
     * Called by the transport frame handler; buffer holds one packet to process.
     *
     * @param data 封包数据 / Packet data
     * @return 是否处理成功；失败时应立即关闭连接 / True if processed OK; false to close connection now
     */
    @Override
    public boolean processData(ByteBuffer data) {
        GsClientPacket pck = GsPacketHandlerFactory.handle(data, this);

        if (pck != null && pck.read()) {
            LoginThreadPoolServices.threadPoolManager().executeLsPacket(pck);
        }

        return true;
    }

    /**
     * 由传输层反复调用直至返回 false，用于写出下一个待发封包。
     * Called repeatedly by the transport until false; writes the next pending packet.
     *
     * @param data 输出缓冲区 / Output buffer
     * @return 是否写入了数据；false 表示无更多数据 / True if data was written; false if nothing left
     */
    @Override
    protected final boolean writeData(ByteBuffer data) {
        synchronized (guard) {
            GsServerPacket packet = sendMsgQueue.pollFirst();
            if (packet == null) {
                return false;
            }

            packet.write(this, data);
            return true;
        }
    }

    /**
     * 连接准备关闭时由传输层调用，返回断开前回调延迟。
     * Called by the transport when connection is ready to close; returns delay before onDisconnect.
     *
     * @return 毫秒延迟，本实现恒为 0 / Delay in ms; always 0 here
     */
    @Override
    protected final long getDisconnectionDelay() {
        return 0;
    }

    /**
     * 连接断开后的清理：停止 ping、解绑游戏服账号。
     * Cleanup after disconnect: stop ping and unbind game-server accounts.
     */
    @Override
    protected final void onDisconnect() {
        if (Config.ENABLE_PINGPONG) {
            this.pingThread.closeMe();
        }
        log.info(I18n.get("log.e803a1d01bbf", this));
        if (gameServerInfo != null) {
            gameServerInfo.setConnection(null);
            gameServerInfo.clearAccountsOnGameServer();
            gameServerInfo = null;
        }
    }

    /**
     * 登录服关闭时关闭本连接。
     * Close this connection when the login server shuts down.
     */
    @Override
    protected final void onServerClose() {
        close(/* packet, */true);
    }

    /**
     * 向本连接发送 GS 服务端封包。
     * Send a GsServerPacket to this connection.
     *
     * @param bp 待发送封包 / Packet to send
     */
    public final void sendPacket(GsServerPacket bp) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }

            sendMsgQueue.addLast(bp);
            enableWriteInterest();
        }
    }

    /**
     * 保证 closePacket 在关闭前发出，清空历史/后续封包；forced 在本实现无额外效果。
     * Guarantees closePacket is sent before close and clears past/future packets; forced has no extra effect here.
     *
     * @param closePacket 关闭前发送的封包 / Packet sent before closing
     * @param forced 是否强制关闭（本实现无额外影响） / Forced close flag (no extra effect here)
     */
    public final void close(GsServerPacket closePacket, boolean forced) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }

            pendingClose = true;
            isForcedClosing = forced;
            sendMsgQueue.clear();
            sendMsgQueue.addLast(closePacket);
            enableWriteInterest();
        }
    }

    /**
     * 返回当前连接状态。
     * Return current connection state.
     *
     * Current state
     */
    public State getState() {
        return state;
    }

    /**
     * 设置连接状态；进入 AUTHED 时按配置启动 ping。
     * Set connection state; when AUTHED, start ping if configured.
     *
     * New state
     */
    public void setState(State state) {
        this.state = state;
        if (state == State.AUTHED) {
            if (Config.ENABLE_PINGPONG) {
                LoginThreadPoolServices.threadPoolManager().schedule(pingThread, 5000);
            }
        }
    }

    /**
     * 返回本连接的游戏服信息；未认证时为 null。
     * Return GameServerInfo for this connection, or null if not authenticated yet.
     *
     * @return 游戏服信息或 null / GameServerInfo or null
     */
    public GameServerInfo getGameServerInfo() {
        return gameServerInfo;
    }

    /**
     * 绑定本连接的游戏服信息。
     * Bind GameServerInfo to this connection.
     *
     * @param gameServerInfo 游戏服信息 / Game server info
     */
    public void setGameServerInfo(GameServerInfo gameServerInfo) {
        this.gameServerInfo = gameServerInfo;
    }

    /**
     * 返回连接的可读描述（服务器 ID 与 IP）。
     * Return a human-readable description (server id and IP).
     *
     * @return 连接描述字符串 / Connection description
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GameServer [ID:");
        if (gameServerInfo != null) {
            sb.append(gameServerInfo.getId());
        } else {
            sb.append("null");
        }
        sb.append("] ").append(getIP());
        return sb.toString();
    }

    /**
     * 处理游戏服 pong 响应。
     * Handle GameServer pong response.
     *
     * Ping id
     */
    public void pong(int pid) {
        if (Config.ENABLE_PINGPONG) {
            this.pingThread.onResponse(pid);
        }
    }

    /**
     * 连接初始化：置为 CONNECTED 并按需创建 ping 线程。
     * Connection init: set CONNECTED and create ping thread if needed.
     */
    @Override
    protected void initialized() {
        state = State.CONNECTED;
        String ip = getIP();

        if (Config.ENABLE_PINGPONG) {
            pingThread = new PingPongThread(this);
        }

        log.info(I18n.get("log.25ae763a9d55", ip));
    }
}
