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
	private int id;

	/**
	 * 使用给定 ID 构造状态。
	 * Constructs a status with the given id.
	 *
	 * @param id 状态 ID / Status id
	 */
	private QuestStatus(int id) {
		this.id = id;
	}

	/**
	 * 返回状态的数值 ID。
	 * Returns the numeric status id.
	 *
	 * Status id
	 */
	public int value() {
		return id;
	}
}
