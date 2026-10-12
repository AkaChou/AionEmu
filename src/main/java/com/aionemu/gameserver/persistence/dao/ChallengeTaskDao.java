package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.challenge.ChallengeTask;
import com.aionemu.gameserver.model.templates.challenge.ChallengeType;
import com.aionemu.gameserver.persistence.entity.ChallengeTaskPo;
import com.mybatisflex.core.service.IService;

/**
 * 挑战任务数据访问层。
 * Data access layer for challenge tasks.
 */
public interface ChallengeTaskDao extends IService<ChallengeTaskPo> {

	/**
	 * 加载指定所有者与类型的挑战任务。
	 * Loads challenge tasks for the given owner and type.
	 * @param ownerId 所有者 ID / owner ID
	 * @param type 挑战类型 / challenge type
	 * @return 任务 ID 到任务的映射 / map of task ID to challenge task
	 */
	Map<Integer, ChallengeTask> load(int ownerId, ChallengeType type);

	/**
	 * 存储挑战任务。
	 * Stores a challenge task.
	 * @param task 挑战任务 / challenge task
	 */
	void storeTask(ChallengeTask task);
}
