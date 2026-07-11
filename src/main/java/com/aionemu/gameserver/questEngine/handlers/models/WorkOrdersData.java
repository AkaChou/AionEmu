package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.WorkOrders;

/**
 * 工作订单（制作委托）类任务的 XML 数据模型，注册 {@link WorkOrders} 模板。
 * XML data model for work-order (crafting commission) quests; registers the {@link WorkOrders} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WorkOrdersData", propOrder = { "giveComponent" })
public class WorkOrdersData extends XMLQuest {

	/**
	 * 接取时发放的制作材料列表。
	 * Crafting materials given on accept.
	 */
	@XmlElement(name = "give_component", required = true)
	protected List<QuestItems> giveComponent;

	/**
	 * 接取任务的起始 NPC ID 列表。
	 * Start NPC ids that offer the quest.
	 */
	@XmlAttribute(name = "start_npc_ids", required = true)
	protected List<Integer> startNpcIds;

	/**
	 * 关联的配方 ID。
	 * Related recipe id.
	 */
	@XmlAttribute(name = "recipe_id", required = true)
	protected int recipeId;

	/**
	 * 返回材料列表；若尚未初始化则惰性创建空列表。
	 * Returns the material list; lazily creates an empty list when null.
	 *
	 * Material list
	 */
	public List<QuestItems> getGiveComponent() {
		if (giveComponent == null) {
			giveComponent = new ArrayList<QuestItems>();
		}
		return this.giveComponent;
	}

	/**
	 * 返回起始 NPC ID 列表。
	 * Returns the start NPC id list.
	 *
	 * Start NPC ids
	 */
	public List<Integer> getStartNpcIds() {
		return startNpcIds;
	}

	/**
	 * 返回配方 ID。
	 * Returns the recipe id.
	 *
	 * Recipe id
	 */
	public int getRecipeId() {
		return recipeId;
	}

	/**
	 * 注册 {@link WorkOrders} 模板处理器。
	 * Registers the {@link WorkOrders} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		questEngine.addQuestHandler(new WorkOrders(this));
	}
}
