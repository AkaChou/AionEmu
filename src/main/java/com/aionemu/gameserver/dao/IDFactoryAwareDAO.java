package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;

/**
 * 通过 {@link com.aionemu.gameserver.utils.idfactory.IDFactory} 生成 ID 的 DAO 通用接口。
 * Generic interface for all DAO classes that generate their IDs using
 * {@link com.aionemu.gameserver.utils.idfactory.IDFactory}.
 *
 * @author SoulKeeper
 */
public interface IDFactoryAwareDAO extends DAO {

	/**
	 * 返回本 DAO 已使用的全部 ID。
	 * Returns an array of all IDs that are used by this DAO.
	 *
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	public int[] getUsedIDs();
}
