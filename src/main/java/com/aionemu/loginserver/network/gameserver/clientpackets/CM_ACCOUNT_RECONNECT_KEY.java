package com.aionemu.loginserver.network.gameserver.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.ReconnectingAccount;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_ACCOUNT_RECONNECT_KEY;

/**
 * GS→LS：玩家请求快速重连登录服，LS 回复 reconnectKey。
 * GS→LS: player requests fast reconnect to LoginServer; LS replies with reconnectKey.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class CM_ACCOUNT_RECONNECT_KEY extends GsClientPacket {

    /**
     * 即将重连的账号 ID。
     * Account id of the account that will reconnect.
     */
    private int accountId;

    /**
     * 读取待重连账号 ID。
     * Reads the account id that will reconnect.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
    }

    /**
     * 生成 reconnectKey，登记重连账号并回复 GS。
     * Generates reconnectKey, registers reconnecting account, and replies to GS.
     */
    @Override
    protected void runImpl() {
        int reconectKey = Rnd.nextInt();
        Account acc = this.getConnection().getGameServerInfo().removeAccountFromGameServer(accountId);
        if (acc == null) {
            log.info(I18n.get("log.35c23b6ec7d5"));
        } else {
            AccountController.addReconnectingAccount(new ReconnectingAccount(acc, reconectKey));
        }
        sendPacket(new SM_ACCOUNT_RECONNECT_KEY(accountId, reconectKey));
    }
}
