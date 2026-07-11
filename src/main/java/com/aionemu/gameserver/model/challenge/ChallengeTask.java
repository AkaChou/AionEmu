package com.aionemu.gameserver.model.challenge;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.templates.challenge.ChallengeQuestTemplate;
import com.aionemu.gameserver.model.templates.challenge.ChallengeTaskTemplate;

/**
 * 挑战任务模型。
 * Challenge Task model.
 */

public class ChallengeTask {
	private final int taskId;
	private final int ownerId;
	private Map<Integer, ChallengeQuest> quests;
	private Timestamp completeTime;
	private ChallengeTaskTemplate template;

	/**
	 * 用于从 DAO 加载任务。 / Used for loading tasks from DAO.
	 */
	public ChallengeTask(int taskId, int ownerId, Map<Integer, ChallengeQuest> quests, Timestamp completeTime) {
		this.taskId = taskId;
		this.ownerId = ownerId;
		this.quests = quests;
		this.completeTime = completeTime;
		this.template = DataManager.CHALLENGE_DATA.getTaskByTaskId(taskId);
	}

	/**
	 * 用于运行时创建新任务。 / Used for creating new tasks in runtime.
	 */
	public ChallengeTask(int ownerId, ChallengeTaskTemplate template) {
		this.taskId = template.getId();
		this.ownerId = ownerId;
		Map<Integer, ChallengeQuest> quests = new HashMap<Integer, ChallengeQuest>();
		for (ChallengeQuestTemplate qt : template.getQuests()) {
			ChallengeQuest quest = new ChallengeQuest(qt, 0);
			quest.setPersistentState(PersistentState.NEW);
			quests.put(qt.getId(), quest);
		}
		this.quests = quests;
		this.completeTime = new Timestamp(1000);
		this.template = template;
	}

	/** 返回任务 ID / Returns the task id */
	public int getTaskId() {
		return this.taskId;
	}

	/** 返回所有者 ID / Returns the owner id */
	public int getOwnerId() {
		return this.ownerId;
	}

	/** 返回 quests count / Returns the quests count */
	public int getQuestsCount() {
		return quests.size();
	}

	/** 返回 quests / Returns the quests */
	public Map<Integer, ChallengeQuest> getQuests() {
		return quests;
	}

	/** 获取任务。 / Returns the quest. */
	public ChallengeQuest getQuest(int questId) {
		return quests.get(questId);
	}

	/** 返回 complete time / Returns the complete time */
	public Timestamp getCompleteTime() {
		return completeTime;
	}

	/** 更新 complete time / Update complete time */
	public synchronized void updateCompleteTime() {
		completeTime.setTime(System.currentTimeMillis());
	}

	/** 获取模板。 / Returns the template. */
	public ChallengeTaskTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @return Whether completed / Whether completed
	 */
	public boolean isCompleted() {
		boolean isCompleted = true;
		for (ChallengeQuest quest : quests.values()) {
			if (quest.getCompleteCount() < quest.getMaxRepeats()) {
				isCompleted = false;
				break;
			}
		}
		return isCompleted;
	}
}
