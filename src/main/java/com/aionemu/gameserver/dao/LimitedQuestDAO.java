package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;

/** 全服共享限量任务名额持久化。 */
public abstract class LimitedQuestDAO implements DAO {

	@Override
	public final String getClassName() {
		return LimitedQuestDAO.class.getName();
	}

	/** 初始化名额并原子扣减一个，名额耗尽时返回 {@code false}。 */
	public abstract boolean tryAcquire(int questId, int maxCount);

	/** 将名额恢复指定数量，但不超过上限。 */
	public abstract boolean recover(int questId, int amount, int maxCount);
}
