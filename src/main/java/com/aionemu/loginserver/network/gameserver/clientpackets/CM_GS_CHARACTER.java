package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：上报账号在本游戏服上的角色数量。
 * GS→LS: report character count of an account on this GameServer.
 *
 * @author cura
 */
public class CM_GS_CHARACTER extends GsClientPacket {

    /**
     * 账号 ID。
     * Account id.
     */
    private int accountId;
    /**
     * 角色数量。
     * Character count.
     */
    private int characterCount;

    /**
     * 读取账号 ID 与角色数。
     * Reads account id and character count.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
        characterCount = readC();
    }

    /**
     * 记录角色数；若所有 GS 均已上报则下发服务器列表。
     * Records character count; if all GS reported, sends server list.
     */
    @Override
    protected void runImpl() {
        GameServerInfo gsi = this.getConnection().getGameServerInfo();

        AccountController.addGSCharacterCountFor(accountId, gsi.getId(), characterCount);

        if (AccountController.hasAllGSCharacterCounts(accountId)) {
            AccountController.sendServerListFor(accountId);
        }
    }
}
