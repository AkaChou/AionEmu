package com.aionemu.gameserver.questEngine.handlers.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.ItemOrders;

/**
 * 物品订单类任务的 XML 数据模型，注册 {@link ItemOrders} 模板。
 * XML data model for item-order quests; registers the {@link ItemOrders} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemOrdersData")
public class ItemOrdersData extends XMLQuest {

	/**
	 * 触发任务的起始物品 ID。
	 * Start item id that triggers the quest.
	 */
	@XmlAttribute(name = "start_item_id", required = true)
	protected int startItemId;

	/**
	 * 中间对话 NPC 1。
	 * Intermediate talk NPC 1.
	 */
	@XmlAttribute(name = "talk_npc_id1")
	protected int talkNpc1;

	/**
	 * 中间对话 NPC 2。
	 * Intermediate talk NPC 2.
	 */
	@XmlAttribute(name = "talk_npc_id2")
	protected int talkNpc2;

	/**
	 * 交还任务的结束 NPC ID。
	 * End NPC id for turn-in.
	 */
	@XmlAttribute(name = "end_npc_id", required = true)
	protected int endNpcId;

	/**
	 * 注册 {@link ItemOrders} 模板处理器。
	 * Registers the {@link ItemOrders} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		ItemOrders template = new ItemOrders(id, startItemId, talkNpc1, talkNpc2, endNpcId);
		questEngine.addQuestHandler(template);
	}
}
