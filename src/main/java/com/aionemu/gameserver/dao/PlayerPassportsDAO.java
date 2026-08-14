package com.aionemu.gameserver.dao;

import java.sql.Timestamp;
import java.util.List;

import com.aionemu.commons.database.dao.DAO;

/**
 * 玩家护照（Passports）数据访问对象。
 * Player passports data access object.
 *
 * @author Ghostfur (Aion-Unique)
 */
public abstract class PlayerPassportsDAO implements DAO {

	/**
	 * 插入一条护照记录。
	 * Inserts a passport record.
	 *
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @param stamps 印章数 / stamps
	 * @param last_stamp 最后时间戳 / last stamp
	 */
	public abstract void insertPassport(int accountId, int passportId, int stamps, Timestamp last_stamp);

	/**
	 * 更新护照进度。
	 * Updates passport progress.
	 *
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @param stamps 印章数 / stamps
	 * @param rewarded 是否已领奖 / rewarded flag
	 * @param last_stamp 最后时间戳 / last stamp
	 */
	public abstract void updatePassport(int accountId, int passportId, int stamps, boolean rewarded,
			Timestamp last_stamp);

	/**
	 * 获取护照印章数。
	 * Returns the stamp count for the passport.
	 *
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @return 印章数量 / stamp count
	 */
	public abstract int getStamps(int accountId, int passportId);

	/**
	 * 获取护照最后时间戳。
	 * Returns the last stamp timestamp for the passport.
	 *
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @return 最后时间戳 / last stamp
	 */
	public abstract Timestamp getLastStamp(int accountId, int passportId);

	/**
	 * 获取账号下全部护照 ID。
	 * Returns all passport IDs for the account.
	 *
	 * @param accountId 账号 ID / account id
	 * @return 通行证 ID 列表 / list of passport ids
	 */
	public abstract List<Integer> getPassports(int accountId);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerPassportsDAO.class.getName();
	}
}
