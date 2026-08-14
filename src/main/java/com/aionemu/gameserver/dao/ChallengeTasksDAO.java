package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.challenge.ChallengeTask;
import com.aionemu.gameserver.model.templates.challenge.ChallengeType;

/**
 * 挑战任务数据访问对象。
 * Challenge task data access object.
 */
public abstract class ChallengeTasksDAO implements DAO {
	/**
	 * 加载指定所有者与类型的挑战任务。
	 * Loads challenge tasks for the given owner and type.
	 *
	 * @param ownerId 所有者 ID / owner ID
	 * @param type 挑战类型 / challenge type
	 * @return 任务 ID 到任务的映射 / map of task ID to challenge task
	 */
	public abstract Map<Integer, ChallengeTask> load(int ownerId, ChallengeType type);

	/**
	 * 存储挑战任务。
	 * Stores a challenge task.
	 *
	 * @param task 挑战任务 / challenge task
	 */
	public abstract void storeTask(ChallengeTask task);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return ChallengeTasksDAO.class.getName();
	}
}
