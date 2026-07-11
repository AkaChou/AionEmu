package com.aionemu.loginserver.network.aion.serverpackets;

import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.SessionKey;

/**
 * 登录服→客户端：登录成功，下发会话密钥片段（accountId / loginOk）。
 * loginOk). / loginOk).
 *
 * @author -Nemesiss-
 */
public class SM_LOGIN_OK extends AionServerPacket {

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
     * 构造 SM_LOGIN_OK 包。
     * Constructs a new SM_LOGIN_OK packet.
     *
     * @param key 会话密钥 / session key
     */
    public SM_LOGIN_OK(SessionKey key) {
        super(3);
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
    	writeD(0);
    	writeD(0);
    	writeD(1002);
    	writeD(126282165);
    	writeB(new byte[47]);
    }
}
