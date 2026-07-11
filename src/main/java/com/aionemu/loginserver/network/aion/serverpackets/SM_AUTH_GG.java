package com.aionemu.loginserver.network.aion.serverpackets;

import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;

/**
 * 登录服→客户端：GG 校验通过响应。
 * LoginServer → client: GG auth OK response.
 *
 * @author -Nemesiss-
 */
public class SM_AUTH_GG extends AionServerPacket {

    /**
     * 本连接的会话 ID。
     * Session id of this connection.
     */
    private final int sessionId;

    /**
     * 构造 SM_AUTH_GG 包。
     * Constructs a new SM_AUTH_GG packet.
     *
     * session id
     */
    public SM_AUTH_GG(int sessionId) {
        super(0x0b);

        this.sessionId = sessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(LoginConnection con) {
        writeD(sessionId);
        writeB(new byte[35]);
    }
}
