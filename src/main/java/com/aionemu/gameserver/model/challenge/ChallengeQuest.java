package com.aionemu.gameserver.model.challenge;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.templates.challenge.ChallengeQuestTemplate;

/**
 * 挑战任务模型。
 * Challenge Quest model.
 */

public class ChallengeQuest {
	private final ChallengeQuestTemplate template;
	private int completeCount;
	private PersistentState persistentState;

	public ChallengeQuest(ChallengeQuestTemplate template, int completeCount) {
		this.template = template;
		this.completeCount = completeCount;
	}

	/** 返回任务 ID / Returns the quest id */
	public int getQuestId() {
		return template.getId();
	}

	/** 获取任务模板。 / Returns the quest template. */
	public ChallengeQuestTemplate getQuestTemplate() {
		return template;
	}

	/** 返回 max repeats / Returns the max repeats */
	public int getMaxRepeats() {
		return template.getRepeatCount();
	}

	/** 返回 score per quest / Returns the score per quest */
	public int getScorePerQuest() {
		return template.getScore();
	}

	/** 返回 complete count / Returns the complete count */
	public int getCompleteCount() {
		return completeCount;
	}

	/** Increasecomplete 次数 / Increase complete count */
	public synchronized void increaseCompleteCount() {
		this.completeCount++;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		if (this.persistentState == PersistentState.NEW && persistentState == PersistentState.UPDATE_REQUIRED) {
			return;
		}
		this.persistentState = persistentState;
	}
}
