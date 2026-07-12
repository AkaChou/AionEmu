package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.ItemCollecting;

/**
 * 物品收集类任务的 XML 数据模型，注册 {@link ItemCollecting} 模板。
 * XML data model for item-collecting quests; registers the {@link ItemCollecting} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemCollectingData")
public class ItemCollectingData extends XMLQuest {

	/**
	 * 接取任务的起始 NPC ID 列表。
	 * Start NPC ids that offer the quest.
	 */
	@XmlAttribute(name = "start_npc_ids", required = true)
	protected List<Integer> startNpcIds;

	/**
	 * 可交互以获取物品的动作物品 / 可采集物 ID 列表。
	 * gatherable object ids used to obtain items.
	 */
	@XmlAttribute(name = "action_item_ids")
	protected List<Integer> actionItemIds;

	/**
	 * 交还任务的结束 NPC ID 列表。
	 * End NPC ids for turn-in.
	 */
	@XmlAttribute(name = "end_npc_ids")
	protected List<Integer> endNpcIds;

	/**
	 * 中间推进对话的下一 NPC ID。
	 * Intermediate next-NPC id for progress dialogue.
	 */
	@XmlAttribute(name = "next_npc_id", required = true)
	protected int nextNpcId;

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
	 * 需要收集的物品模板 ID。
	 * Item template id that must be collected.
	 */
	@XmlAttribute(name = "item_id")
	protected int itemId;

	/**
	 * 注册 {@link ItemCollecting} 模板处理器。
	 * Registers the {@link ItemCollecting} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		ItemCollecting template = new ItemCollecting(id, startNpcIds, nextNpcId, actionItemIds, endNpcIds, questMovie,
				startDialogId, startDialogId2, itemId);
		questEngine.addQuestHandler(template);
	}
}
