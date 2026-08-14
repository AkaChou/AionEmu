package com.aionemu.gameserver.questEngine.model;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.util.Calendar;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;

/**
 * 玩家单个任务的运行时状态，包含进度变量、状态、完成次数与持久化标记。
 * Runtime state of a single player quest, including progress vars, status, completion count and persistence flag.
 *
 * @author MrPoke
 * @modified vlog, Rolandas
 */
@Slf4j
public class QuestState {

	/** 任务 ID。 Quest id. */
	private final int questId;
	/** 任务进度变量。 Quest progress variables. */
	private QuestVars questVars;
	/** 当前任务状态。 Current quest status. */
	private QuestStatus status;
	/** 完成次数。 Completion count. */
	private int completeCount;
	/** 最近完成时间。 Last completion time. */
	private Timestamp completeTime;
	/** 下次可重复时间。 Next allowed repeat time. */
	private Timestamp nextRepeatTime;
	/** 已选奖励索引。 Selected reward index. */
	private Integer reward;
	/** 数据库持久化状态。 Database persistent state. */
	private PersistentState persistentState;


	/**
	 * 构造任务状态。
	 * Constructs a quest state.
	 *
	 * @param questId 任务 ID / Quest id
	 * @param status 初始状态 / Initial status
	 * @param questVars 打包的任务变量 / Packed quest vars
	 * @param completeCount 完成计数 / Completion count
	 * @param nextRepeatTime 下次可重复时间 / Next repeat time
	 * @param reward 奖励索引 / Reward index
	 * @param completeTime 完成时间 / Completion time
	 */
	public QuestState(int questId, QuestStatus status, int questVars, int completeCount, Timestamp nextRepeatTime,
			Integer reward, Timestamp completeTime) {
		this.questId = questId;
		this.status = status;
		this.questVars = new QuestVars(questVars);
		this.completeCount = completeCount;
		this.nextRepeatTime = nextRepeatTime;
		this.reward = reward;
		this.completeTime = completeTime;
		this.persistentState = PersistentState.NEW;
	}

	/**
	 * 返回任务变量集合。
	 * Returns the quest variable set.
	 *
	 * @return 任务变量 / Quest vars
	 */
	public QuestVars getQuestVars() {
		return questVars;
	}

	/**
	 * 按索引设置任务子变量，并标记需要持久化更新。
	 * Sets a quest sub-variable by index and marks the state for persistence update.
	 *
	 * @param id 子变量索引 / Sub-variable index
	 * @param var 子变量值 / Sub-variable value
	 */
	public void setQuestVarById(int id, int var) {
		questVars.setVarById(id, var);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 按索引获取任务子变量。
	 * Returns the quest sub-variable at the given index.
	 *
	 * @param id 子变量索引 / Sub-variable index
	 * @return 子变量值 / Sub-variable value
	 */
	public int getQuestVarById(int id) {
		return questVars.getVarById(id);
	}

	/**
	 * 用打包整型设置全部任务变量，并标记需要持久化更新。
	 * Sets all quest variables from a packed int and marks the state for persistence update.
	 *
	 * @param var 打包的任务变量值 / Packed quest-var value
	 */
	public void setQuestVar(int var) {
		questVars.setVar(var);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回当前任务状态。
	 * Returns the current quest status.
	 *
	 * @return 任务状态 / Quest status
	 */
	public QuestStatus getStatus() {
		return status;
	}

	/**
	 * 设置任务状态；首次进入 COMPLETE 时自动更新完成时间，并标记持久化。
	 * Sets quest status; auto-updates completion time on first transition to COMPLETE and marks for persistence.
	 *
	 * @param status 新状态 / New status
	 */
	public void setStatus(QuestStatus status) {
		if (status == QuestStatus.COMPLETE && this.status != QuestStatus.COMPLETE)
			updateCompleteTime();
		this.status = status;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回最近完成时间。
	 * Returns the last completion time.
	 *
	 * @return 完成时间戳 / Completion timestamp
	 */
	public Timestamp getCompleteTime() {
		return completeTime;
	}

	/**
	 * 设置完成时间。
	 * Sets the completion time.
	 *
	 * @param time 完成时间 / Completion time
	 */
	public void setCompleteTime(Timestamp time) {
		completeTime = time;
	}

	/**
	 * 将完成时间更新为当前时刻。
	 * Updates the completion time to now.
	 */
	public void updateCompleteTime() {
		completeTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
	}

	/**
	 * 返回任务 ID。
	 * Returns the quest id.
	 *
	 * @return 任务 ID / Quest id
	 */
	public int getQuestId() {
		return questId;
	}

	/**
	 * 设置完成次数，并标记需要持久化更新。
	 * Sets the completion count and marks the state for persistence update.
	 *
	 * @param completeCount 完成计数 / Completion count
	 */
	public void setCompleteCount(int completeCount) {
		this.completeCount = completeCount;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回完成次数。
	 * Returns the completion count.
	 *
	 * @return 完成计数 / Completion count
	 */
	public int getCompleteCount() {
		return completeCount;
	}

	/**
	 * 设置下次可重复时间。
	 * Sets the next allowed repeat time.
	 *
	 * @param nextRepeatTime 下次可重复时间 / Next repeat time
	 */
	public void setNextRepeatTime(Timestamp nextRepeatTime) {
		this.nextRepeatTime = nextRepeatTime;
	}

	/**
	 * 返回下次可重复时间。
	 * Returns the next allowed repeat time.
	 *
	 * @return 下次可重复时间 / Next repeat time
	 */
	public Timestamp getNextRepeatTime() {
		return nextRepeatTime;
	}

	/**
	 * 设置已选奖励索引，并标记需要持久化更新。
	 * Sets the selected reward index and marks the state for persistence update.
	 *
	 * @param reward 奖励索引 / Reward index
	 */
	public void setReward(Integer reward) {
		this.reward = reward;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回已选奖励索引；未设置时记警告并返回 0。
	 * Returns the selected reward index; logs a warning and returns 0 when unset.
	 *
	 * @return 奖励索引，缺省为 0 / Reward index, default 0
	 */
	public Integer getReward() {
		if (reward == null) {
			log.warn(I18n.get("log.b673a467afac", String.valueOf(questId)));
		} else {
			return reward;
		}
		return 0;
	}

	/** Returns the persisted reward index without applying the legacy zero fallback. */
	public Integer getRewardOrNull() {
		return reward;
	}

	/**
	 * 判断该任务当前是否允许再次接取（状态、完成次数、变量与时间限制）。
	 * Returns whether the quest may currently be repeated (status, completion count, vars and time limits).
	 *
	 * @return true 可重复；false 不可 / true if repeatable; false otherwise
	 */
	public boolean canRepeat() {
		QuestMetadata metadata = GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null);
		return canRepeat(metadata);
	}

	public boolean canRepeat(QuestMetadata metadata) {
		if (metadata == null) {
			return false;
		}
		var repeat = metadata.repeatPolicy();
		if (status != QuestStatus.NONE && (status != QuestStatus.COMPLETE
				|| (completeCount >= repeat.maxRepeatCount() && repeat.maxRepeatCount() != 255))) {
			return false;
		}
		if (questVars.getQuestVars() != 0) {
			return false;
		}
		boolean timeBased = repeat.daily() || repeat.weekly() || repeat.cooldownSeconds() > 0
			|| !metadata.repeatCycles().isEmpty();
		if (timeBased && nextRepeatTime != null) {
			Timestamp currentTime = new Timestamp(System.currentTimeMillis());
			if (currentTime.before(nextRepeatTime)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 返回数据库持久化状态。
	 * Returns the database persistent state.
	 *
	 * @return 持久化状态 / Persistent state
	 */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/**
	 * 设置持久化状态；禁止 NEW→DELETED，且 NEW 状态下忽略 UPDATE_REQUIRED。
	 * Sets persistent state; forbids NEW→DELETED and ignores UPDATE_REQUIRED while still NEW.
	 *
	 * @param persistentState 目标持久化状态 / Target persistent state
	 */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.persistentState == PersistentState.NEW) {
				throw new IllegalArgumentException("Cannot change state to DELETED from NEW");
			}
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			this.persistentState = persistentState;
		}
	}
}
