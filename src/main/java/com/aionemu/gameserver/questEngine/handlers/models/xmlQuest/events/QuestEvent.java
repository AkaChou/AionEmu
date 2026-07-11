package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions.QuestConditions;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations.QuestOperations;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * XML 驱动任务事件的抽象基类，承载条件、操作与可选 ID 列表。
 * Abstract base for XML-driven quest events, holding conditions, operations and optional ids.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestEvent", propOrder = { "conditions", "operations" })
@XmlSeeAlso({ OnKillEvent.class, OnTalkEvent.class })
public abstract class QuestEvent {

	/** 触发前条件集合 / Conditions evaluated before the event runs */
	protected QuestConditions conditions;
	/** 事件关联操作集合 / Operations associated with the event */
	protected QuestOperations operations;
	/** 可选关联 ID 列表（如 NPC / 怪物 ID） / Optional related ids (e.g. NPC / monster ids) */
	@XmlAttribute
	protected List<Integer> ids;

	/**
	 * 执行本事件；默认实现恒返回 false，由子类覆盖。
	 * Runs this event; default implementation always returns false, subclasses override.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功 / Whether the event was handled
	 */
	public boolean operate(QuestEnv env) {
		return false;
	}

	/**
	 * 返回关联 ID 的实时列表（JAXB 可变列表，非快照）。
	 * Returns the live list of related ids (JAXB live list, not a snapshot).
	 *
	 * Id list
	 */
	public List<Integer> getIds() {
		if (ids == null) {
			ids = new ArrayList<Integer>();
		}
		return this.ids;
	}
}
