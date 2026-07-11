package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_LS_CONTROL_RESPONSE;

/**
 * GS→LS：修改账号权限等级或会员等级。
 * GS→LS: change account access level or membership level.
 *
 * @author Aionchs-Wylovech
 */
public class CM_LS_CONTROL extends GsClientPacket {

    /**
     * 账号名。
     * Account name.
     */
    private String accountName;
    /**
     * 新参数值（权限/会员等级）。
     * New parameter value (access/membership level).
     */
    private int param;
    /**
     * 控制类型：1=权限等级，2=会员等级。
     * Control type: 1 = access level, 2 = membership.
     */
    private int type;
    /**
     * 目标玩家名。
     * Target player name.
     */
    private String playerName;
    /**
     * 操作管理员名。
     * Admin name who requested the change.
     */
    private String adminName;
    /**
     * 更新是否成功。
     * Whether the update succeeded.
     */
    private boolean result;

    /**
     * 读取控制类型、管理员、账号、玩家与参数。
     * Reads control type, admin, account, player, and param.
     */
    @Override
    protected void readImpl() {

        type = readC();
        adminName = readS();
        accountName = readS();
        playerName = readS();
        param = readC();
    }

    /**
     * 更新账号权限/会员并回复 GS。
     * Updates account access/membership and replies to GS.
     */
    @Override
    protected void runImpl() {

        Account account = DAOManager.getDAO(AccountDAO.class).getAccount(accountName);
        switch (type) {
            case 1:
                account.setAccessLevel((byte) param);
                break;
            case 2:
                account.setMembership((byte) param);
                break;
        }
        result = DAOManager.getDAO(AccountDAO.class).updateAccount(account);
        sendPacket(new SM_LS_CONTROL_RESPONSE(type, result, playerName, account.getId(), param, adminName));
    }
}
