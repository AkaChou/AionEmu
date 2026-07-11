package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 按当前可见 NPC 模板 ID 与配置值比较的条件。
 * Condition that compares the visible NPC template id against a configured value.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NpcIdCondition")
public class NpcIdCondition extends QuestCondition {

	/** 期望的 NPC 模板 ID / Expected NPC template id */
	@XmlAttribute(required = true)
	protected int values;

	/**
	 * 比较当前可见对象的 NPC ID 与配置值。
	 * Compares the visible object's NPC id with the configured value.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 比较是否成立 / Whether the comparison holds
	 */
	@Override
	public boolean doCheck(QuestEnv env) {
		int id = 0;
		VisibleObject visibleObject = env.getVisibleObject();
		if (visibleObject != null && visibleObject instanceof Npc) {
			id = ((Npc) visibleObject).getNpcId();
		}
		switch (getOp()) {
		case EQUAL:
			return id == values;
		case GREATER:
			return id > values;
		case GREATER_EQUAL:
			return id >= values;
		case LESSER:
			return id < values;
		case LESSER_EQUAL:
			return id <= values;
		case NOT_EQUAL:
			return id != values;
		default:
			return false;
		}
	}
}
