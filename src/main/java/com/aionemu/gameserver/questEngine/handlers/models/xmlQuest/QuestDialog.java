package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions.QuestConditions;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations.QuestOperations;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * XML 驱动任务中的对话节点：匹配对话 ID 后校验条件并执行操作。
 * Dialog node in an XML-driven quest: matches dialog id, then checks conditions and runs operations.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestDialog", propOrder = { "conditions", "operations" })
public class QuestDialog {

	/** 可选条件集合 / Optional condition set */
	protected QuestConditions conditions;
	/** 条件通过后执行的操作集合 / Operations run when conditions pass */
	protected QuestOperations operations;
	/** 目标对话 ID / Target dialog id */
	@XmlAttribute(required = true)
	protected int id;

	/**
	 * 若当前对话 ID 匹配且条件成立，则执行绑定操作。
	 * Runs bound operations when the current dialog id matches and conditions hold.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param qs 任务状态 / Quest state
	 * @return 是否成功处理 / Whether the dialog was handled
	 */
	public boolean operate(QuestEnv env, QuestState qs) {
		if (env.getDialogId() != id) {
			return false;
		}
		if (conditions == null || conditions.checkConditionOfSet(env)) {
			if (operations != null) {
				return operations.operate(env);
			}
		}
		return false;
	}
}
