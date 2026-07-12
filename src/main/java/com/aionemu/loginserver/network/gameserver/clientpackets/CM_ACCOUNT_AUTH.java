package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.aion.SessionKey;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：校验账号会话密钥是否仍有效。
 * GS→LS: ask whether the account session key is still valid on LoginServer.
 *
 * @author -Nemesiss-
 */
public class CM_ACCOUNT_AUTH extends GsClientPacket {

    /**
     * 待校验的会话密钥。
     * Session key that GameServer needs to validate on LoginServer.
     */
    private SessionKey sessionKey;

    /**
     * loginOk / playOk1 / playOk2 并组装 SessionKey。
     * loginOk / playOk1 / playOk2 and builds SessionKey.
     */
    @Override
    protected void readImpl() {
        int accountId = readD();
        int loginOk = readD();
        int playOk1 = readD();
        int playOk2 = readD();

        sessionKey = new SessionKey(accountId, loginOk, playOk1, playOk2);
    }

    /**
     * 委托 AccountController 校验会话密钥。
     * Delegates session-key validation to AccountController.
     */
    @Override
    protected void runImpl() {
        AccountController.checkAuth(sessionKey, this.getConnection());
    }
}
