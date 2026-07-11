package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * XML 驱动任务中的任务变量节点：变量值匹配后依次尝试 NPC 子节点。
 * Quest-var node in an XML-driven quest: when the var matches, tries NPC children.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestVar", propOrder = { "npc" })
public class QuestVar {

	/** 该变量下的 NPC 列表 / NPC list for this quest var */
	protected List<QuestNpc> npc;
	/** 期望的任务变量值 / Expected quest-var value */
	@XmlAttribute(required = true)
	protected int value;

	/**
	 * 若当前任务变量等于配置值，则依次执行 NPC 节点。
	 * When the current quest var equals the configured value, tries NPC nodes.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param qs 任务状态 / Quest state
	 * @return 任一 NPC 处理成功则为 true / True if any NPC handled the event
	 */
	public boolean operate(QuestEnv env, QuestState qs) {
		int var = -1;
		if (qs != null) {
			var = qs.getQuestVars().getQuestVars();
		}
		if (var != value) {
			return false;
		}
		for (QuestNpc questNpc : npc) {
			if (questNpc.operate(env, qs)) {
				return true;
			}
		}
		return false;
	}
}
