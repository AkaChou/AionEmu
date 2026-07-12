package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.QuestVar;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * 对话事件：条件通过后按任务变量节点分发到 NPC / 对话处理。
 * dialog handlers.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OnTalkEvent", propOrder = { "var" })
public class OnTalkEvent extends QuestEvent {

	/** 按任务变量分支的处理节点 / Handlers branched by quest var */
	protected List<QuestVar> var;

	/**
	 * 条件通过时依次尝试变量节点。
	 * Tries quest-var nodes in order when conditions pass.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 任一变量节点处理成功则为 true / True if any var node handled the talk
	 */
	public boolean operate(QuestEnv env) {
		if (conditions == null || conditions.checkConditionOfSet(env)) {
			QuestState qs = env.getPlayer().getQuestStateList().getQuestState(env.getQuestId());
			for (QuestVar questVar : var) {
				if (questVar.operate(env, qs)) {
					return true;
				}
			}
		}
		return false;
	}
}
