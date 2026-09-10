package com.aionemu.gameserver.questEngine.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 任务进度状态，描述玩家任务列表中任务的生命周期阶段。
 * Quest progress status describing the lifecycle stage of a quest in a player's quest list.
 *
 * @author MrPoke
 */
@XmlEnum
public enum QuestStatus {
	/**
	 * 默认/中止/计时结束状态；用于开启新任务。与其他任务一并存储但不应计入已接任务，任务列表中不可见。
	 * aborted / timer-ended status; used to begin a new quest. Stored with other quests but must not be counted; invisible in the quest list.
	 */
	NONE(0),
	/**
	 * 已接取的进行中任务。
	 * Accepted quests currently in progress.
	 */
	START(3),
	/**
	 * 已完成目标、等待领取奖励（“去领取你的奖励”）。
	 * Objectives finished; waiting to claim reward ("Go and get your reward").
	 */
	REWARD(4),
	/**
	 * 已完成并领奖的任务。
	 * Fully completed quests.
	 */
	COMPLETE(5),
	/**
	 * 尚未解锁/不可用的任务。
	 * Quests that are not (yet) available.
	 */
	LOCKED(6);

	/** 协议/持久化用的状态 ID。 Protocol/persistence status id. */
	private final int id;

	/**
	 * 使用给定 ID 构造状态。
	 * Constructs a status with the given id.
	 *
	 * @param id 状态 ID / Status id
	 */
	QuestStatus(int id) {
		this.id = id;
	}

	/**
	 * 返回状态的数值 ID。
	 * Returns the numeric status id.
	 *
	 * @return 状态 ID / Status id
	 */
	public int value() {
		return id;
	}

	/**
	 * 判断任务是否应出现在客户端进行中任务列表。
	 * Returns whether this status belongs in the client's active quest list.
	 *
	 * <p>{@code LOCKED} is a persisted legacy placeholder and must be re-evaluated before it becomes visible.
	 * {@code LOCKED} 是持久化的旧版占位状态，在重新评估前不得显示为进行中任务。</p>
	 *
	 * @return 是否已接取且处于进行中 / {@code true} when the quest is accepted and active
	 */
	public boolean isClientQuestListVisible() {
		return this == START || this == REWARD;
	}
}
