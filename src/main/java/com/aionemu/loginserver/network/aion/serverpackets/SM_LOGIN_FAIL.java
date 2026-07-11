package com.aionemu.loginserver.network.aion.serverpackets;

import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;

/**
 * 登录服→客户端：登录失败响应（携带原因码）。
 * LoginServer → client: login failure response with reason code.
 *
 * @author KID
 */
public class SM_LOGIN_FAIL extends AionServerPacket {

    /**
     * 登录失败原因。
     * Reason why login failed.
     */
    private AionAuthResponse response;

    /**
     * 构造 SM_LOGIN_FAIL 包。
     * Constructs a new SM_LOGIN_FAIL packet.
     *
     * auth response
     */
    public SM_LOGIN_FAIL(AionAuthResponse response) {
        super(0x01);
        this.response = response;
    }

    @Override
    protected void writeImpl(LoginConnection con) {
        writeD(response.getMessageId());
    }
}
