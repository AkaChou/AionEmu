package com.aionemu.loginserver.network.aion.clientpackets;

import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.AionClientPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.LoginConnection.State;
import com.aionemu.loginserver.network.aion.serverpackets.SM_AUTH_GG;
import com.aionemu.loginserver.network.aion.serverpackets.SM_LOGIN_FAIL;
import java.nio.ByteBuffer;

/**
 * 客户端 GameGuard 鉴权包：校验 sessionId 后进入 AUTHED_GG。
 * Client GameGuard auth packet: verify sessionId then move to AUTHED_GG.
 *
 * @author -Nemesiss-
 */
public class CM_AUTH_GG extends AionClientPacket {

    /**
     * 会话 ID，应与 Init 包中一致。
     * Session id; should match the one sent in Init.
     */
    private int sessionId;

    /*
     * private final int data1; private final int data2; private final int data3; private final int data4;
     */

    /**
     * 构造 CM_AUTH_GG 包。
     * Construct CM_AUTH_GG packet.
     *
     * @param buf 包体数据 / Packet data
     * @param client 登录连接 / Login connection
     */
    public CM_AUTH_GG(ByteBuffer buf, LoginConnection client) {
        super(buf, client, 0x07);
    }

    /**
     * 读取 sessionId 并跳过后续 27 字节。
     * Read sessionId and skip the following 27 bytes.
     */
    @Override
    protected void readImpl() {
        sessionId = readD();
        readB(27);
    }

    /**
     * 校验 sessionId；成功则回 SM_AUTH_GG，否则关闭连接。
     * Validate sessionId; send SM_AUTH_GG on success, otherwise close.
     */
    @Override
    protected void runImpl() {
        LoginConnection con = getConnection();
        if (con.getSessionId() == sessionId) {
            con.setState(State.AUTHED_GG);
            con.sendPacket(new SM_AUTH_GG(sessionId));
        } else {
            /**
             * sessionId 不匹配：通知客户端并断开。
             * sessionId mismatch: notify client and disconnect.
             */
            con.close(new SM_LOGIN_FAIL(AionAuthResponse.SYSTEM_ERROR), false);
        }
    }
}
