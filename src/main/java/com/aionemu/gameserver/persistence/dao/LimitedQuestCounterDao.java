package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.persistence.entity.LimitedQuestCounterPo;
import com.mybatisflex.core.service.IService;

/** 全服共享限量任务名额数据访问层。 / Data access layer for server-wide shared limited quest quotas. */
public interface LimitedQuestCounterDao extends IService<LimitedQuestCounterPo> {

	/**
	 * 初始化名额并原子扣减一个，名额耗尽时返回 {@code false}。
	 * Initializes the quota and atomically acquires one; returns {@code false} when the quota is exhausted.
	 * @param questId 任务 ID / quest id
	 * @param maxCount 名额上限 / quota cap
	 * @return 是否获取成功 / true if acquired
	 */
	boolean tryAcquire(int questId, int maxCount);

	/**
	 * 将名额恢复指定数量，但不超过上限。
	 * Restores the quota to the given amount, never above the cap.
	 * @param questId 任务 ID / quest id
	 * @param amount 恢复数量 / restores amount
	 * @param maxCount 名额上限 / quota cap
	 * @return 是否恢复成功 / true if restored
	 */
	boolean recover(int questId, int amount, int maxCount);
}
