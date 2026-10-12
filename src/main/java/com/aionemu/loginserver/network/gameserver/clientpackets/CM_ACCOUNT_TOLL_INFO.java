package com.aionemu.loginserver.network.gameserver.clientpackets;



import lombok.RequiredArgsConstructor;

import com.aionemu.loginserver.persistence.dao.AccountDataDao;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：同步账号 toll / luna 点数。
 * luna points.
 * @author xTz
 */
@RequiredArgsConstructor
public class CM_ACCOUNT_TOLL_INFO extends GsClientPacket {

    private final AccountDataDao accountDataDao;
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
        Account account = accountDataDao.getAccount(accountName);

        if (account != null) {
            accountDataDao.updatePoints(account.getId(), toll, 0);
            accountDataDao.updateLuna(account.getId(), luna);
        }
    }
}
