package com.aionemu.loginserver.network.aion;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.PacketProcessor;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.controller.AccountTimeController;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.aion.serverpackets.SM_INIT;
import com.aionemu.loginserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.loginserver.network.ncrypt.CryptEngine;
import com.aionemu.loginserver.network.ncrypt.EncryptedRSAKeyPair;
import com.aionemu.loginserver.network.ncrypt.KeyGen;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录服与 Aion 客户端之间的连接对象。
 * Connection object between the login server and an Aion client.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class LoginConnection extends AConnection {

    /**
     * 执行客户端包的包处理器。
     * Packet processor for client packets.
     */
    private final static PacketProcessor<LoginConnection> processor = new PacketProcessor<LoginConnection>(1, 8, 50, 3);
    /**
     * 待发送服务端包队列。
     * Outgoing server-packet queue.
     */
    private final Deque<AionServerPacket> sendMsgQueue = new ArrayDeque<AionServerPacket>();
    /**
     * 本连接唯一会话 ID。
     * Unique session id of this connection.
     */
    @Getter
    private int sessionId = hashCode();
    /**
     * 本连接绑定的账号；状态为 AUTHED_LOGIN 时不为 null。
     * Bound account; non-null when state is AUTHED_LOGIN.
     */
    @Getter
    @Setter
    private Account account;
    /**
     * 加解密引擎。
     * Crypt engine for encrypt/decrypt.
     */
    private CryptEngine cryptEngine;
    /**
     * 是否已进入游戏服连接流程。
     * Whether this user is joining a game server.
     */
    private boolean joinedGs;
    /**
     * RSA 加扰密钥对。
     * Scrambled RSA key pair.
     */
    private EncryptedRSAKeyPair encryptedRSAKeyPair;
    /**
     * 本连接会话密钥。
     * Session key for this connection.
     */
    @Getter
    @Setter
    private SessionKey sessionKey;
    /**
     * 当前连接状态。
     * Current connection state.
     */
    @Getter
    @Setter
    private State state;

    /**
     * Aion 连接可能的状态。
     * Possible states of an Aion connection.
     */
    public static enum State {

        /**
         * 客户端刚连上。
         * Client just connected.
         */
        CONNECTED,
        /**
         * GameGuard 已通过。
         * GameGuard authenticated.
         */
        AUTHED_GG,
        /**
         * 账号登录完成。
         * Client logged in.
         */
        AUTHED_LOGIN
    }

    /**
     * 基于传输创建登录连接。
     * Create a login connection on the given transport.
     *
     * @param transport 连接传输 / Connection transport
     */
    public LoginConnection(ConnectionTransport transport) {
        super(transport, 8192 * 2, 8192 * 2);
    }

    /**
     * 传输层回调：处理缓冲中的一包数据（解密、解析并投递执行）。
     * Transport callback: process one packet from the buffer (decrypt, parse, execute).
     *
     * @param data 包数据 / Packet data
     * @return 成功为 true；失败需立即关闭连接 / True on success; false means close now
     */
    @Override
    protected final boolean processData(ByteBuffer data) {
        if (!decrypt(data)) {
            return false;
        }

        AionClientPacket pck = AionPacketHandlerFactory.handle(data, this);

        /**
         * 仅当包存在且读取成功时执行。
         * Execute only when packet exists and read succeeded.
         */
        if ((pck != null) && pck.read()) {
            processor.executePacket(pck);
        }

        return true;
    }

    /**
     * 传输层回调：向缓冲写入下一待发包，无数据返回 false。
     * Transport callback: write next pending packet; false when queue empty.
     *
     * @param data 写出缓冲 / Write buffer
     * @return 写入了数据则为 true / True if data was written
     */
    @Override
    protected final synchronized boolean writeData(ByteBuffer data) {
        AionServerPacket packet = sendMsgQueue.pollFirst();

        if (packet == null) {
            return false;
        }

        packet.setBuf(data);
        packet.write(this);

        return true;
    }

    /**
     * 连接可关闭时由传输调用；返回调用 onDisconnect 前的延迟（毫秒）。
     * Called by transport when close is ready; delay in ms before onDisconnect.
     *
     * @return 恒为 0 / always 0
     */
    @Override
    protected final long getDisconnectionDelay() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void onDisconnect() {
        /**
         * 尚未进入游戏服时才从 LS 移除账号。
         * Remove account from LS only if not yet joined GS.
         */
        if ((account != null) && !joinedGs) {
            AccountController.removeAccountOnLS(account);
            AccountTimeController.updateOnLogout(account);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void onServerClose() {
        close( /* packet, */true);
    }

    /**
     * 解密入站包。
     * Decrypt inbound packet.
     *
     * @param buf 数据缓冲 / Data buffer
     * @return 解密成功为 true / true on success
     */
    private boolean decrypt(ByteBuffer buf) {
        int size = buf.remaining();
        final int offset = buf.arrayOffset() + buf.position();
        boolean ret = cryptEngine.decrypt(buf.array(), offset, size);

        if (!ret) {
            log.warn(I18n.get("log.9a856e31c496", this));
        }

        return ret;
    }

    /**
     * 加密出站包。
     * Encrypt outbound packet.
     *
     * @param buf 数据缓冲 / Data buffer
     * @return 加密后包体大小 / Encrypted payload size
     */
    public final int encrypt(ByteBuffer buf) {
        int size = buf.limit() - 2;
        final int offset = buf.arrayOffset() + buf.position();

        size = cryptEngine.encrypt(buf.array(), offset, size);

        return size;
    }

    /**
     * 向客户端发送服务端包。
     * Send a server packet to this client.
     *
     * @param bp 待发送包 / Packet to send
     */
    public final synchronized void sendPacket(AionServerPacket bp) {
        /**
         * 连接已关闭或正在发送关闭包。
         * Connection already closed or waiting for close packet.
         */
        if (isWriteDisabled()) {
            return;
        }

        log.debug("sending packet: " + bp);
        sendMsgQueue.addLast(bp);
        enableWriteInterest();
    }

    /**
     * 保证关闭包先于断开发送；清空其它待发包。forced 表示不等待清理。
     * Guarantee closePacket is sent before disconnect; drop other queued packets. forced skips wait.
     *
     * @param closePacket 关闭前发送的包 / Packet sent before close
     * @param forced 本实现无实际影响 / Has no effect in this implementation
     */
    public final synchronized void close(AionServerPacket closePacket, boolean forced) {
        if (isWriteDisabled()) {
            return;
        }

        log.info(I18n.get("log.38bd4ad41d74", closePacket));

        pendingClose = true;
        isForcedClosing = forced;
        sendMsgQueue.clear();
        sendMsgQueue.addLast(closePacket);
        enableWriteInterest();
    }

    /**
     * 返回加扰后的 RSA 模数。
     * Return scrambled RSA modulus.
     *
     * @return 加扰后的模数 / scrambled modulus
     */
    public final byte[] getEncryptedModulus() {
        return encryptedRSAKeyPair.getEncryptedModulus();
    }

    /**
     * 返回 RSA 私钥。
     * Return RSA private key.
     *
     * @return RSA 私钥 / RSA private key
     */
    public final RSAPrivateKey getRSAPrivateKey() {
        return (RSAPrivateKey) encryptedRSAKeyPair.getRSAKeyPair().getPrivate();
    }

    /**
     * 标记已进入游戏服。
     * Mark that the client joined a game server.
     */
    public final void setJoinedGs() {
        joinedGs = true;
    }

    /**
     * @return 连接描述信息 / Connection description
     */
    @Override
    public String toString() {
        return (account != null) ? account + " " + getIP() : "not loged " + getIP();
    }

    /**
     * 勿改：hashCode 用于保证连接唯一 ID。
     * Do not change: hashCode ensures each connection has a unique id.
     *
     * @return 唯一标识 / unique identifier
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * 立即关闭连接。
     * Close the connection immediately.
     */
    public void closeNow() {
        this.close(false);
    }

    /**
     * 连接初始化：设置状态、生成密钥并发送 SM_INIT。
     * Initialize connection: set state, generate keys and send SM_INIT.
     */
    @Override
    protected void initialized() {
        state = State.CONNECTED;
        log.info(I18n.get("log.1ecf9752f093", getIP()));
        encryptedRSAKeyPair = KeyGen.getEncryptedRSAKeyPair();
        SecretKey blowfishKey = KeyGen.generateBlowfishKey();

        cryptEngine = new CryptEngine();
        cryptEngine.updateKey(blowfishKey.getEncoded());

        /**
         * 发送 Init 包。
         * Send Init packet.
         */
        sendPacket(new SM_INIT(this, blowfishKey));
    }
}
