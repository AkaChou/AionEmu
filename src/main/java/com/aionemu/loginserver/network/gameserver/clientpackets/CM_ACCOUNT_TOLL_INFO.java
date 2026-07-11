package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.dao.PremiumDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：同步账号 toll / luna 点数。
 * luna points. / luna points.
 *
 * @author xTz
 */
public class CM_ACCOUNT_TOLL_INFO extends GsClientPacket {
    /**
     * Toll 点数。
     * Toll points.
     */
    private long toll;
    /**
     * Luna 点数。
     * Luna points.
     */
    private long luna;
    /**
     * 账号名。
     * Account name.
     */
    private String accountName;

    /**
     * 读取 toll、luna 与账号名。
     * Reads toll, luna, and account name.
     */
    @Override
    protected void readImpl() {
        toll = readQ();
        luna = readQ();
        accountName = readS();
    }

    /**
     * 按账号名更新 Premium 点数。
     * Updates premium points by account name.
     */
    @Override
    protected void runImpl() {
        Account account = DAOManager.getDAO(AccountDAO.class).getAccount(accountName);

        if (account != null) {
            DAOManager.getDAO(PremiumDAO.class).updatePoints(account.getId(), toll, 0);
            DAOManager.getDAO(PremiumDAO.class).updateLuna(account.getId(), luna);
        }
    }
}
