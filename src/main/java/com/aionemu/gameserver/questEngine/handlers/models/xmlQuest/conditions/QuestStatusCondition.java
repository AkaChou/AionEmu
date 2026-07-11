package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 按任务状态（可选指定任务 ID）与配置值比较的条件。
 * Condition that compares a quest status (optionally for another quest id) against a configured value.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestStatusCondition")
public class QuestStatusCondition extends QuestCondition {

	/** 期望的任务状态 / Expected quest status */
	@XmlAttribute(required = true)
	protected QuestStatus value;
	/** 可选目标任务 ID；为空时使用当前任务 / Optional target quest id; current quest when null */
	@XmlAttribute(name = "quest_id")
	protected Integer questId;

	/**
	 * 比较指定（或当前）任务的状态序值与配置状态。
	 * Compares the ordinal of the target (or current) quest status with the configured status.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 比较是否成立 / Whether the comparison holds
	 */
	@Override
	public boolean doCheck(QuestEnv env) {
		Player player = env.getPlayer();
		int qstatus = 0;
		int id = env.getQuestId();
		if (questId != null) {
			id = questId;
		}
		QuestState qs = player.getQuestStateList().getQuestState(id);
		if (qs != null) {
			qstatus = qs.getStatus().value();
		}
		switch (getOp()) {
		case EQUAL:
			return qstatus == value.value();
		case GREATER:
			return qstatus > value.value();
		case GREATER_EQUAL:
			return qstatus >= value.value();
		case LESSER:
			return qstatus < value.value();
		case LESSER_EQUAL:
			return qstatus <= value.value();
		case NOT_EQUAL:
			return qstatus != value.value();
		default:
			return false;
		}
	}
}
