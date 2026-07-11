package com.aionemu.gameserver.questEngine.handlers.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.CraftingRewards;

/**
 * 制作等级奖励类任务的 XML 数据模型，注册 {@link CraftingRewards} 模板。
 * XML data model for crafting-level reward quests; registers the {@link CraftingRewards} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftingRewardsData")
public class CraftingRewardsData extends XMLQuest {

	/**
	 * 接取任务的起始 NPC ID。
	 * Start NPC id that offers the quest.
	 */
	@XmlAttribute(name = "start_npc_id", required = true)
	protected int startNpcId;

	/**
	 * 交还任务的结束 NPC ID（可选，可与起始相同）。
	 * End NPC id for turn-in (optional; may match start).
	 */
	@XmlAttribute(name = "end_npc_id")
	protected int endNpcId;

	/**
	 * 关联的制作技能 ID。
	 * Related crafting skill id.
	 */
	@XmlAttribute(name = "skill_id")
	protected int skillId;

	/**
	 * 完成后提升的制作技能等级奖励。
	 * Crafting skill level granted as reward on completion.
	 */
	@XmlAttribute(name = "level_reward")
	protected int levelReward;

	/**
	 * 注册 {@link CraftingRewards} 模板处理器。
	 * Registers the {@link CraftingRewards} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		CraftingRewards template = new CraftingRewards(id, startNpcId, skillId, levelReward, endNpcId, questMovie);
		questEngine.addQuestHandler(template);
	}
}
