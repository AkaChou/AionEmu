package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * XML 驱动任务中的 NPC 节点：匹配 NPC ID 后依次尝试对话子节点。
 * NPC node in an XML-driven quest: matches NPC id, then tries dialog children.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestNpc", propOrder = { "dialog" })
public class QuestNpc {

	/** 该 NPC 下的对话列表 / Dialog list for this NPC */
	protected List<QuestDialog> dialog;
	/** 目标 NPC 模板 ID / Target NPC template id */
	@XmlAttribute(required = true)
	protected int id;

	/**
	 * 若当前可见对象为指定 NPC，则依次执行对话节点。
	 * When the visible object is the target NPC, tries dialog nodes in order.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param qs 任务状态 / Quest state
	 * @return 任一对话处理成功则为 true / True if any dialog handled the event
	 */
	public boolean operate(QuestEnv env, QuestState qs) {
		int npcId = -1;
		if (env.getVisibleObject() instanceof Npc) {
			npcId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		if (npcId != id) {
			return false;
		}
		for (QuestDialog questDialog : dialog) {
			if (questDialog.operate(env, qs)) {
				return true;
			}
		}
		return false;
	}
}
