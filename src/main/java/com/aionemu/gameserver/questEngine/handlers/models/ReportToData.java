package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.ReportTo;

/**
 * 简单汇报类任务的 XML 数据模型（接取 → 携带物品 → 汇报），注册 {@link ReportTo} 模板。
 * XML data model for simple report-to quests (accept → carry item → report); registers {@link ReportTo}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReportToData")
public class ReportToData extends XMLQuest {

	/**
	 * 接取任务的起始 NPC ID 列表。
	 * Start NPC ids that offer the quest.
	 */
	@XmlAttribute(name = "start_npc_ids")
	protected List<Integer> startNpcIds;

	/**
	 * 汇报/交还的结束 NPC ID 列表。
	 * End NPC ids for report/turn-in.
	 */
	@XmlAttribute(name = "end_npc_ids")
	protected List<Integer> endNpcIds;

	/**
	 * 起始对话 dialog ID。
	 * Start dialogue dialog id.
	 */
	@XmlAttribute(name = "start_dialog_id")
	protected int startDialogId;

	/**
	 * 备用起始对话 dialog ID。
	 * Alternate start dialogue dialog id.
	 */
	@XmlAttribute(name = "start_dialog_id2")
	protected int startDialogId2;

	/**
	 * 接取时发放、汇报时回收的物品 ID。
	 * Item id given on accept and collected on report.
	 */
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;

	/**
	 * 注册 {@link ReportTo} 模板处理器。
	 * Registers the {@link ReportTo} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		ReportTo template = new ReportTo(id, startNpcIds, endNpcIds, startDialogId, startDialogId2, itemId);
		questEngine.addQuestHandler(template);
	}
}
