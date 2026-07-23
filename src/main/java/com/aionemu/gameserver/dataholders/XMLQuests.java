package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;

/**
 * 任务脚本 XML 数据容器，按元素名多态绑定各类任务处理器模型。
 * Quest-script XML data holder, polymorphically binding quest-handler models by element name.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "quest_scripts")
public class XMLQuests {

	@XmlElements({
			@XmlElement(name = "report_to", type = com.aionemu.gameserver.questEngine.handlers.models.ReportToData.class),
			@XmlElement(name = "monster_hunt", type = com.aionemu.gameserver.questEngine.handlers.models.MonsterHuntData.class),
			@XmlElement(name = "item_collecting", type = com.aionemu.gameserver.questEngine.handlers.models.ItemCollectingData.class),
			@XmlElement(name = "relic_rewards", type = com.aionemu.gameserver.questEngine.handlers.models.RelicRewardsData.class),
			@XmlElement(name = "crafting_rewards", type = com.aionemu.gameserver.questEngine.handlers.models.CraftingRewardsData.class),
			@XmlElement(name = "report_to_many", type = com.aionemu.gameserver.questEngine.handlers.models.ReportToManyData.class),
			@XmlElement(name = "kill_in_world", type = com.aionemu.gameserver.questEngine.handlers.models.KillInWorldData.class),
			@XmlElement(name = "skill_use", type = com.aionemu.gameserver.questEngine.handlers.models.SkillUseData.class),
			@XmlElement(name = "kill_spawned", type = com.aionemu.gameserver.questEngine.handlers.models.KillSpawnedData.class),
			@XmlElement(name = "mentor_monster_hunt", type = com.aionemu.gameserver.questEngine.handlers.models.MentorMonsterHuntData.class),
			@XmlElement(name = "fountain_rewards", type = com.aionemu.gameserver.questEngine.handlers.models.FountainRewardsData.class),
				@XmlElement(name = "item_order", type = com.aionemu.gameserver.questEngine.handlers.models.ItemOrdersData.class),
				@XmlElement(name = "data_driven_quest", type = com.aionemu.gameserver.questEngine.handlers.models.DataDrivenQuestData.class),
				@XmlElement(name = "work_order", type = com.aionemu.gameserver.questEngine.handlers.models.WorkOrdersData.class) })
	protected List<XMLQuest> data;

	/**
	 * 返回已加载的任务脚本列表。
	 * Returns the loaded quest-script list.
	 *
	 * @return 任务脚本列表 / quest-script list
	 */
	public List<XMLQuest> getQuest() {
		return data;
	}

	/**
	 * 设置任务脚本列表。
	 * Sets the quest-script list.
	 *
	 * @param data 任务脚本列表 / quest-script list
	 */
	public void setData(List<XMLQuest> data) {
		this.data = data;
	}
}
