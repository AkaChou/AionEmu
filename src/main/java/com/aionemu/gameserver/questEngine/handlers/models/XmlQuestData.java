package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events.OnKillEvent;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events.OnTalkEvent;
import com.aionemu.gameserver.questEngine.handlers.template.XmlQuest;

/**
 * 通用 XML 脚本任务数据模型（对话/击杀事件驱动），注册 {@link XmlQuest} 模板。
 * Generic XML-script quest data model (talk/kill event driven); registers the {@link XmlQuest} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "XmlQuest", propOrder = { "onTalkEvent", "onKillEvent" })
public class XmlQuestData extends XMLQuest {

	/**
	 * 对话事件列表。
	 * On-talk event list.
	 */
	@XmlElement(name = "on_talk_event")
	protected List<OnTalkEvent> onTalkEvent;

	/**
	 * 击杀事件列表。
	 * On-kill event list.
	 */
	@XmlElement(name = "on_kill_event")
	protected List<OnKillEvent> onKillEvent;

	/**
	 * 接取任务的起始 NPC ID（可选）。
	 * Start NPC id that offers the quest (optional).
	 */
	@XmlAttribute(name = "start_npc_id")
	protected Integer startNpcId;

	/**
	 * 交还任务的结束 NPC ID（可选）。
	 * End NPC id for turn-in (optional).
	 */
	@XmlAttribute(name = "end_npc_id")
	protected Integer endNpcId;

	/**
	 * 返回对话事件列表；若尚未初始化则惰性创建空列表。
	 * Returns the on-talk event list; lazily creates an empty list when null.
	 *
	 * @return 对话事件列表 / On-talk events
	 */
	public List<OnTalkEvent> getOnTalkEvent() {
		if (onTalkEvent == null) {
			onTalkEvent = new ArrayList<OnTalkEvent>();
		}
		return this.onTalkEvent;
	}

	/**
	 * 返回击杀事件列表；若尚未初始化则惰性创建空列表。
	 * Returns the on-kill event list; lazily creates an empty list when null.
	 *
	 * @return 击杀事件列表 / On-kill events
	 */
	public List<OnKillEvent> getOnKillEvent() {
		if (onKillEvent == null) {
			onKillEvent = new ArrayList<OnKillEvent>();
		}
		return this.onKillEvent;
	}

	/**
	 * 返回起始 NPC ID。
	 * Returns the start NPC id.
	 *
	 * Start NPC id
	 */
	public Integer getStartNpcId() {
		return startNpcId;
	}

	/**
	 * 返回结束 NPC ID。
	 * Returns the end NPC id.
	 *
	 * End NPC id
	 */
	public Integer getEndNpcId() {
		return endNpcId;
	}

	/**
	 * 注册 {@link XmlQuest} 模板处理器。
	 * Registers the {@link XmlQuest} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		questEngine.addQuestHandler(new XmlQuest(this));
	}
}
