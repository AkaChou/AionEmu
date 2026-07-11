package com.aionemu.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;

/**
 * 账号高级货币（Toll / Luna）数据访问抽象层。
 * DAO that manages premium points (toll) and luna.
 *
 * @author KID
 */
public abstract class PremiumDAO implements DAO {

    /**
     * 查询账号 Toll 点数（含待领取奖励）。
     * Returns account toll points (including pending rewards).
     *
     * 账号 ID / Account id
     * Toll points
     */
    public abstract long getPoints(int accountId);

    /**
     * 查询账号 Luna。
     * Returns account luna amount.
     *
     * 账号 ID / Account id
     * Luna amount
     */
    public abstract long getLuna(int accountId);

    /**
     * 扣减并写回 Toll 点数。
     * Updates toll points after deducting the required amount.
     *
     * 账号 ID / Account id
     * Current points
     * @param required 需扣减数量 / Required amount to deduct
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updatePoints(int accountId, long points, long required);

    /**
     * 写回账号 Luna。
     * Updates account luna amount.
     *
     * 账号 ID / Account id
     * Luna amount
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateLuna(int accountId, long luna);

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return PremiumDAO.class.getName();
    }
}
