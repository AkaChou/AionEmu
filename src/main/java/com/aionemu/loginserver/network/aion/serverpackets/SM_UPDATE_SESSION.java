package com.aionemu.loginserver.network.aion.serverpackets;

import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.SessionKey;

/**
 * 登录服→客户端：更新会话密钥（快速重连用）。
 * LoginServer → client: update session key (for fast reconnect).
 *
 * @author -Nemesiss-
 */
public class SM_UPDATE_SESSION extends AionServerPacket {

    /**
     * 会话密钥中的账号 ID，用于安全校验。
     * Account id part of the session key, used for security checks.
     */
    private final int accountId;
    /**
     * 会话密钥中的 loginOk，用于安全校验。
     * loginOk part of the session key, used for security checks.
     */
    private final int loginOk;

    /**
     * 构造 SM_UPDATE_SESSION 包。
     * Constructs a new SM_UPDATE_SESSION packet.
     *
     * @param key 会话密钥 / session key
     */
    public SM_UPDATE_SESSION(SessionKey key) {
        super(0x0c);
        this.accountId = key.accountId;
        this.loginOk = key.loginOk;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(LoginConnection con) {
        writeD(accountId);
        writeD(loginOk);
        writeC(0x00);// 出错时的系统消息 / sysmsg if smth is wrong
    }
}
