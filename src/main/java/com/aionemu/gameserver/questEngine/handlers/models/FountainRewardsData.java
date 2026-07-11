package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.FountainRewards;

/**
 * 喷泉奖励类任务的 XML 数据模型，注册 {@link FountainRewards} 模板。
 * XML data model for fountain-reward quests; registers the {@link FountainRewards} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FountainRewardsData")
public class FountainRewardsData extends XMLQuest {

	/**
	 * 可接取任务的起始 NPC ID 列表。
	 * Start NPC ids that offer the quest.
	 */
	@XmlAttribute(name = "start_npc_ids", required = true)
	protected List<Integer> startNpcIds;

	/**
	 * 注册 {@link FountainRewards} 模板处理器。
	 * Registers the {@link FountainRewards} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		FountainRewards template = new FountainRewards(id, startNpcIds);
		questEngine.addQuestHandler(template);
	}
}
