package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * 按当前任务指定变量槽位值与配置值比较的条件。
 * Condition that compares a quest-var slot of the current quest against a configured value.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestVarCondition")
public class QuestVarCondition extends QuestCondition {

	/** 期望的变量值 / Expected var value */
	@XmlAttribute(required = true)
	protected int value;
	/** 任务变量槽位 ID / Quest-var slot id */
	@XmlAttribute(name = "var_id", required = true)
	protected int varId;

	/**
	 * 比较当前任务指定槽位变量与配置值。
	 * Compares the current quest's var at the given slot with the configured value.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 比较是否成立；无任务状态时为 false / Whether it holds; false when quest state is missing
	 */
	@Override
	public boolean doCheck(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(env.getQuestId());
		if (qs == null) {
			return false;
		}
		int var = qs.getQuestVars().getVarById(varId);
		switch (getOp()) {
		case EQUAL:
			return var == value;
		case GREATER:
			return var > value;
		case GREATER_EQUAL:
			return var >= value;
		case LESSER:
			return var < value;
		case LESSER_EQUAL:
			return var <= value;
		case NOT_EQUAL:
			return var != value;
		default:
			return false;
		}
	}
}
