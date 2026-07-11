package com.aionemu.loginserver.network.aion.serverpackets;

import javax.crypto.SecretKey;

import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;

/**
 * 登录服→客户端：握手初始化包，下发会话 ID、RSA 公钥与 Blowfish 密钥。
 * LoginServer → client: handshake init with session id, RSA public key and Blowfish key.
 *
 * <pre>
 * Format: dd b dddd s
 * d: session id
 * d: protocol revision
 * b: 0x90 bytes — 0x80 scrambled RSA public key + 0x10 zeros
 * d: unknown
 * d: unknown
 * d: unknown
 * d: unknown
 * s: blowfish key
 * </pre>
 */
public final class SM_INIT extends AionServerPacket {

    /**
     * 本连接的会话 ID。
     * Session id of this connection.
     */
    private final int sessionId;
    /**
     * 客户端用于加密登录密码的 RSA 公钥（已打乱模数）。
     * Public RSA key (scrambled modulus) the client uses to encrypt login/password.
     */
    private final byte[] publicRsaKey;
    /**
     * 后续包加解密用的 Blowfish 密钥。
     * Blowfish key for subsequent packet encryption/decryption.
     */
    private final byte[] blowfishKey;

    /**
     * 从连接与 Blowfish 密钥构造 SM_INIT。
     * Constructs SM_INIT from the login connection and Blowfish key.
     *
     * login connection
     * Blowfish secret key
     */
    public SM_INIT(LoginConnection client, SecretKey blowfishKey) {
        this(client.getEncryptedModulus(), blowfishKey.getEncoded(), client.getSessionId());
    }

    /**
     * 创建 SM_INIT 包。
     * Creates a new SM_INIT packet.
     *
     * public RSA key
     * Blowfish key
     * session identifier
     */
    private SM_INIT(byte[] publicRsaKey, byte[] blowfishKey, int sessionId) {
        super(0x00);
        this.sessionId = sessionId;
        this.publicRsaKey = publicRsaKey;
        this.blowfishKey = blowfishKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(LoginConnection con) {
        writeD(sessionId); // session id
        writeD(0x0000c621); // protocol revision
        writeB(publicRsaKey); // RSA Public Key
        // 未知 / unk
        writeB(new byte[16]);
        writeB(blowfishKey); // BlowFish key
        writeD(197635); // 未知 / unk
        writeD(2097152); // 未知 / unk
    }
}
