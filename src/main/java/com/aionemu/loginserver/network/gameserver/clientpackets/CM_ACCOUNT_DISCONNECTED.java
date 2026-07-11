package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.controller.AccountTimeController;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：通知某账号已从游戏服断开。
 * GS→LS: inform LoginServer that an account disconnected from GameServer.
 *
 * @author -Nemesiss-
 */
public class CM_ACCOUNT_DISCONNECTED extends GsClientPacket {

    /**
     * 已断开账号 ID。
     * Disconnected account id.
     */
    private int accountId;

    /**
     * 读取已断开账号 ID。
     * Reads the disconnected account id.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
    }

    /**
     * 从 GS 在线表移除账号；若非空则更新累计在线时间。
     * Removes account from GS online table; if present, updates accumulated online time.
     * <p>
     * 账号可能为 null（例如快速重连场景，见 {@link CM_ACCOUNT_RECONNECT_KEY}）。
     * Account may be null (e.g. fast reconnect; see {@link CM_ACCOUNT_RECONNECT_KEY}).
     */
    @Override
    protected void runImpl() {
        Account account = this.getConnection().getGameServerInfo().removeAccountFromGameServer(accountId);

        // 若玩家从 GS 登出，account 可能为 null（见 CM_ACCOUNT_RECONNECT_KEY） / account can be null if a player logged out from gs (see CM_ACCOUNT_RECONNECT_KEY)
        if (account != null) {
            AccountTimeController.updateOnLogout(account);
        }
    }
}
