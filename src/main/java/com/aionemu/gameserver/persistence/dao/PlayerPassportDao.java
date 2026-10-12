package com.aionemu.gameserver.persistence.dao;

import java.sql.Timestamp;
import java.util.List;

import com.aionemu.gameserver.persistence.entity.PlayerPassportPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家护照（Passports）数据访问层。
 * Player passports data access layer.
 * @author Ghostfur (Aion-Unique)
 */
public interface PlayerPassportDao extends IService<PlayerPassportPo> {

	/**
	 * 插入一条护照记录。
	 * Inserts a passport record.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @param stamps 印章数 / stamps
	 * @param lastStamp 最后时间戳 / last stamp
	 */
	void insertPassport(int accountId, int passportId, int stamps, Timestamp lastStamp);

	/**
	 * 更新护照进度。
	 * Updates passport progress.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @param stamps 印章数 / stamps
	 * @param rewarded 是否已领奖 / rewarded flag
	 * @param lastStamp 最后时间戳 / last stamp
	 */
	void updatePassport(int accountId, int passportId, int stamps, boolean rewarded, Timestamp lastStamp);

	/**
	 * 获取护照印章数。
	 * Returns the stamp count for the passport.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @return 印章数量 / stamp count
	 */
	int getStamps(int accountId, int passportId);

	/**
	 * 获取护照最后时间戳。
	 * Returns the last stamp timestamp for the passport.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @return 最后时间戳 / last stamp
	 */
	Timestamp getLastStamp(int accountId, int passportId);

	/**
	 * 获取账号下全部护照 ID。
	 * Returns all passport IDs for the account.
	 * @param accountId 账号 ID / account id
	 * @return 通行证 ID 列表 / list of passport ids
	 */
	List<Integer> getPassports(int accountId);
}
