package com.aionemu.loginserver.network.gameserver.clientpackets;

import java.sql.Timestamp;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.dao.AccountTimeDAO;
import com.aionemu.loginserver.service.LoginProtectionServices;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_BAN_RESPONSE;

/**
 * GS→LS：统一账号/IP 封禁（或解封）请求。
 * GS→LS: universal account/IP ban (or unban) request.
 *
 * @author Watson
 */
public class CM_BAN extends GsClientPacket {

    /**
     * 封禁类型：1=账号，2=IP，3=账号+IP。
     * Ban type: 1 = account, 2 = IP, 3 = full ban (account and IP).
     */
    private byte type;
    /**
     * 待封禁账号 ID。
     * Account to ban.
     */
    private int accountId;
    /**
     * 待封禁 IP 或掩码。
     * IP or mask to ban.
     */
    private String ip;
    /**
     * 时长（分钟）。0=永久；&lt;0 表示解封。
     * Time in minutes. 0 = infinity; if time &lt; 0 then unban.
     */
    private int time;
    /**
     * 发起封禁的管理员对象 ID。
     * Object ID of the admin who requested the ban.
     */
    private int adminObjId;

    /**
     * 读取封禁类型、账号、IP、时长与管理员 ID。
     * Reads ban type, account, IP, duration, and admin object id.
     */
    @Override
    protected void readImpl() {
        this.type = (byte) readC();
        this.accountId = readD();
        this.ip = readS();
        this.time = readD();
        this.adminObjId = readD();
    }

    /**
     * 执行账号/IP 封禁或解封，踢下线并回复 GS。
     * Performs account/IP ban or unban, kicks account, and replies to GS.
     */
    @Override
    protected void runImpl() {
        boolean result = false;

        // 封禁账号 / Ban account
        if ((type == 1 || type == 3) && accountId != 0) {
            Account account = null;

            // 在游戏服务器上查找账号 / Find account on GameServers
            for (GameServerInfo gsi : GameServerTable.getGameServers()) {
                if (gsi.isAccountOnGameServer(accountId)) {
                    account = gsi.getAccountFromGameServer(accountId);
                    break;
                }
            }

            // 1000 表示“无限”值 / 1000 is 'infinity' value
            Timestamp newTime = null;
            if (time >= 0) {
                newTime = new Timestamp(time == 0 ? 1000 : System.currentTimeMillis() + time * 60000);
            }

            if (account != null) {
                AccountTime accountTime = account.getAccountTime();
                accountTime.setPenaltyEnd(newTime);
                account.setAccountTime(accountTime);
                result = true;
            } else {
                AccountTime accountTime = DAOManager.getDAO(AccountTimeDAO.class).getAccountTime(accountId);
                accountTime.setPenaltyEnd(newTime);
                result = DAOManager.getDAO(AccountTimeDAO.class).updateAccountTime(accountId, accountTime);
            }
        }

        // 封禁 IP / Ban IP
        if (type == 2 || type == 3) {
            if (accountId != 0) // 有账号 ID 则封禁其最后 IP / If we got account ID, then ban last IP
            {
                String newip = DAOManager.getDAO(AccountDAO.class).getLastIp(accountId);
                if (!newip.isEmpty()) {
                    ip = newip;
                }
            }
            if (!ip.isEmpty()) {
                // 先解封。封禁需要更新时间 / Unban first. For banning it needs to update time
                if (LoginProtectionServices.bannedIpService().isBanned(ip)) {
                    // 解封请求的结果集 / Result set for unban request
                    result = LoginProtectionServices.bannedIpService().unbanIp(ip);
                }
                if (time >= 0) // 执行封禁 / Ban
                {
                    Timestamp newTime = time != 0 ? new Timestamp(System.currentTimeMillis() + time * 60000) : null;
                    result = LoginProtectionServices.bannedIpService().banIp(ip, newTime);
                }
            }
        }

        // 现在踢出账号 / Now kick account
        if (accountId != 0) {
            AccountController.kickAccount(accountId);
        }

        // 响应游戏服务器 / Respond to GS
        sendPacket(new SM_BAN_RESPONSE(type, accountId, ip, time, adminObjId, result));
    }
}
