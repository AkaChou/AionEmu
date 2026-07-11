package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.RelicRewards;

/**
 * 遗物兑换奖励类任务的 XML 数据模型，注册 {@link RelicRewards} 模板。
 * XML data model for relic-exchange reward quests; registers the {@link RelicRewards} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RelicRewardsData")
public class RelicRewardsData extends XMLQuest {

	/**
	 * 接取/兑换的起始 NPC ID 列表。
	 * Start NPC ids for accept/exchange.
	 */
	@XmlAttribute(name = "start_npc_ids", required = true)
	protected List<Integer> startNpcIds;

	/**
	 * 遗物变量 1（对应某种遗物物品 ID）。
	 * Relic variable 1 (maps to a relic item id).
	 */
	@XmlAttribute(name = "relic_var1")
	protected int relicVar1;

	/**
	 * 遗物变量 2。
	 * Relic variable 2.
	 */
	@XmlAttribute(name = "relic_var2")
	protected int relicVar2;

	/**
	 * 遗物变量 3。
	 * Relic variable 3.
	 */
	@XmlAttribute(name = "relic_var3")
	protected int relicVar3;

	/**
	 * 遗物变量 4。
	 * Relic variable 4.
	 */
	@XmlAttribute(name = "relic_var4")
	protected int relicVar4;

	/**
	 * 每次兑换所需遗物数量。
	 * Relic count required per exchange.
	 */
	@XmlAttribute(name = "relic_count")
	protected int relicCount;

	/**
	 * 注册 {@link RelicRewards} 模板处理器。
	 * Registers the {@link RelicRewards} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		RelicRewards template = new RelicRewards(id, startNpcIds, relicVar1, relicVar2, relicVar3, relicVar4,
				relicCount);
		questEngine.addQuestHandler(template);
	}
}
