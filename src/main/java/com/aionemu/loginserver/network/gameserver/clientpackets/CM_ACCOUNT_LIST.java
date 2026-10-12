package com.aionemu.loginserver.network.gameserver.clientpackets;

import lombok.RequiredArgsConstructor;

import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_REQUEST_KICK_ACCOUNT;

/**
 * GS→LS：上报当前已登录游戏服的账号名列表。
 * GS→LS: report the list of account names currently logged into this GameServer.
 * @author SoulKeeper
 */
@RequiredArgsConstructor
public class CM_ACCOUNT_LIST extends GsClientPacket {

    private final AccountController accountController;
    private final GameServerTable gameServerTable;

    /**
     * 当前在线账号名数组。
     * Array of account names currently logged in.
     */
    private String[] accountNames;

    /**
     * 读取账号名列表。
     * Reads the account-name list.
     */
    @Override
    protected void readImpl() {
        accountNames = new String[readD()];
        for (int i = 0; i < accountNames.length; i++) {
            accountNames[i] = readS();
        }
    }

    /**
     * 将账号登记到本 GS；若已在其他 GS 在线则请求踢下线。
     * Registers each account on this GS; requests kick if already online on another GS.
     */
    @Override
    protected void runImpl() {
        for (String s : accountNames) {
            Account a = accountController.loadAccount(s);
            if (gameServerTable.isAccountOnAnyGameServer(a)) {
                this.getConnection().sendPacket(new SM_REQUEST_KICK_ACCOUNT(a.getId()));
                continue;
            }
            getConnection().getGameServerInfo().addAccountToGameServer(a);
        }
    }
}
