package com.aionemu.loginserver.controller;

import java.sql.Timestamp;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.AccountPlayTimeDAO;
import com.aionemu.loginserver.dao.AccountTimeDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.AccountTime;

import lombok.experimental.UtilityClass;

/**
 * 账号在线/休息时间控制；部分游戏内功能依赖累计在线时长。
 * Account time controller. Some in-game features depend on accumulated online time.
 *
 * @author EvilSpirit
 */
@UtilityClass
public class AccountTimeController {

    /**
     * 角色登录时更新账号时间：LastLoginTime、RestTime 等。
     * Updates account time on login: LastLoginTime, RestTime, etc.
     *
     * @param account 账号 / Account
     */
    public void updateOnLogin(Account account) {
        AccountTime accountTime = account.getAccountTime();

        /**
         * 账号刚创建时需新建 AccountTime。
         * New accounts need a fresh AccountTime.
         */
        if (accountTime == null) {
            accountTime = new AccountTime();
        }

        int lastLoginDay = getDays(accountTime.getLastLoginTime().getTime());
        int currentDay = getDays(System.currentTimeMillis());
        int returnday = getDays(accountTime.getLastLoginTime().getTime() + + 30L * 24 * 60 * 60 * 1000);

        /**
         * 非当日登录则清零当日累计在线/休息时间。
         * Not online today: reset daily accumulated online/rest times.
         */
        if (lastLoginDay < currentDay) {
            DAOManager.getDAO(AccountPlayTimeDAO.class).update(account.getId(), accountTime);
            accountTime.setAccumulatedOnlineTime(0);
            accountTime.setAccumulatedRestTime(0);
        } else {
            long restTime = System.currentTimeMillis() - accountTime.getLastLoginTime().getTime() - accountTime.getSessionDuration();
            accountTime.setAccumulatedRestTime(accountTime.getAccumulatedRestTime() + restTime);

        }

        accountTime.setLastLoginTime(new Timestamp(System.currentTimeMillis()));

        DAOManager.getDAO(AccountTimeDAO.class).updateAccountTime(account.getId(), accountTime);
        account.setAccountTime(accountTime);

        if (currentDay >= returnday && account.getReturn() == 0) {
            account.setReturn((byte) 1);
            account.setReturnEnd(new Timestamp(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
        }

        if (currentDay >= account.getReturnEnd().getTime()) {
            account.setReturn((byte) 0);
        }

    }

    /**
     * 角色登出时更新会话时长与累计在线时间。
     * Updates session duration and accumulated online time on logout.
     *
     * @param account 账号 / Account
     */
    public void updateOnLogout(Account account) {
        AccountTime accountTime = account.getAccountTime();

        accountTime.setSessionDuration(System.currentTimeMillis() - accountTime.getLastLoginTime().getTime());
        accountTime.setAccumulatedOnlineTime(accountTime.getAccumulatedOnlineTime() + accountTime.getSessionDuration());
        DAOManager.getDAO(AccountTimeDAO.class).updateAccountTime(account.getId(), accountTime);
        account.setAccountTime(accountTime);
    }

    /**
     * 判断账号是否已过期。
     * Checks whether the account is expired.
     *
     * @param account 账号 / Account
     * @return 已过期时为 {@code true} / {@code true} if expired
     */
    public boolean isAccountExpired(Account account) {
        AccountTime accountTime = account.getAccountTime();

        return accountTime != null && accountTime.getExpirationTime() != null && accountTime.getExpirationTime().getTime() < System.currentTimeMillis();
    }

    /**
     * 判断账号处罚是否仍生效。
     * Checks whether account penalty is still active.
     *
     * @param account 账号 / Account
     * @return 处罚生效为 true / True if penalty is active
     */
    public boolean isAccountPenaltyActive(Account account) {
        AccountTime accountTime = account.getAccountTime();

        // 1000 表示“无限”值 / 1000 is 'infinity' value
        return accountTime != null && accountTime.getPenaltyEnd() != null && (accountTime.getPenaltyEnd().getTime() == 1000 || accountTime.getPenaltyEnd().getTime() >= System.currentTimeMillis());
    }

    /**
     * 将毫秒时间换算为天数。
     * Converts milliseconds to whole days.
     *
     * @param millis 毫秒时间 / time in ms
     * @return 天数 / days
     */
    public int getDays(long millis) {
        return (int) (millis / 1000 / 3600 / 24);
    }
}
