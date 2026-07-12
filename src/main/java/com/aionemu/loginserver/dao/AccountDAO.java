package com.aionemu.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.model.Account;

/**
 * 账号数据访问抽象层。
 * DAO that manages accounts.
 *
 * @author SoulKeeper
 */
public abstract class AccountDAO implements DAO {

    /**
     * 按名称查询账号。
     * Returns account by name, or null if not found.
     *
     * Account name
     * @return 账号对象；不存在时为 null / Account object, or null
     */
    public abstract Account getAccount(String name);

    /**
     * 按 ID 查询账号。
     * Returns account by id, or null if not found.
     *
     * @param id 账号 ID / Account id
     * @return 账号对象；不存在时为 null / Account object, or null
     */
    public abstract Account getAccount(int id);

    /**
     * 按名称查询账号 ID。
     * not found.
     *
     * Account name
     * @return 账号 ID；失败时为 -1 / Id, or -1 on error
     */
    public abstract int getAccountId(String name);

    /**
     * 查询账号总数。
     * Returns account count, or -1 on error.
     *
     * Account count
     */
    public abstract int getAccountCount();

    /**
     * 插入新账号，并将数据库生成的 ID 回写到对象。
     * Inserts a new account and sets the DB-generated id on the object.
     *
     * @param account 待插入账号 / Account to insert
     * @return 是否插入成功 / True if inserted
     */
    public abstract boolean insertAccount(Account account);

    /**
     * 更新账号完整字段。
     * Updates account in database.
     *
     * @param account 待更新账号 / Account to update
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateAccount(Account account);

    /**
     * 更新账号最近登录的游戏服。
     * Updates lastServer field of account.
     *
     * 账号 ID / Account id
     * @param lastServer 最近访问的服务器 / Last accessed server
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateLastServer(int accountId, byte lastServer);

    /**
     * 更新账号最近登录 IP。
     * Updates last IP that was used to access an account.
     *
     * 账号 ID / Account id
     * @param ip IP 地址 / IP address
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateLastIp(int accountId, String ip);

    /**
     * 查询账号最近登录 IP。
     * Get last IP that was used to access an account.
     *
     * 账号 ID / Account id
     * IP address
     */
    public abstract String getLastIp(int accountId);

    /**
     * 更新账号最近登录 MAC。
     * Updates last MAC that was used to access an account.
     *
     * 账号 ID / Account id
     * MAC address
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateLastMac(int accountId, String mac);

    /**
     * 在会员过期后恢复旧会员等级。
     * Updates account membership when membership has expired.
     *
     * 账号 ID / Account id
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateMembership(int accountId);

    /**
     * 删除超过指定天数未活跃的账号。
     * Deletes accounts inactive for more than the given days.
     *
     * @param daysOfInactivity 不活跃天数 / Days of inactivity
     */
    public abstract void deleteInactiveAccounts(int daysOfInactivity);

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return AccountDAO.class.getName();
    }
}
