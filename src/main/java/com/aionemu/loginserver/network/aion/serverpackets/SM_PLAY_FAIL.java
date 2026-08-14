package com.aionemu.loginserver.network.aion.serverpackets;

import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;

/**
 * 登录服→客户端：进入游戏失败响应（携带原因码）。
 * LoginServer → client: enter-game (play) failure response with reason code.
 *
 * @author -Nemesiss-
 */
public class SM_PLAY_FAIL extends AionServerPacket {

    /**
     * 进入游戏失败原因。
     * Reason why play failed.
     */
    private AionAuthResponse response;

    /**
     * 构造 SM_PLAY_FAIL 包。
     * Constructs a new SM_PLAY_FAIL packet.
     *
     * @param response 认证响应 / auth response
     */
    public SM_PLAY_FAIL(AionAuthResponse response) {
        super(0x06);
        this.response = response;
    }

    @Override
    protected void writeImpl(LoginConnection con) {
        writeD(response.getMessageId());
    }
}
