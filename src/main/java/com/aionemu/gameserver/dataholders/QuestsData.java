package com.aionemu.gameserver.dataholders;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.QuestService;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 任务模板数据容器，按任务 ID 索引，并按 NPC 势力分组。
 * Quest template data holder, indexed by quest id and grouped by NPC faction.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "quests")
public class QuestsData {

	@XmlElement(name = "quest", required = true)
	protected List<QuestTemplate> questsData;
	@XmlTransient
	private IntObjectHashMap<QuestTemplate> questData = new IntObjectHashMap<QuestTemplate>();
	@XmlTransient
	private IntObjectHashMap<List<QuestTemplate>> sortedByFactionId = new IntObjectHashMap<List<QuestTemplate>>();

	/**
	 * JAXB 反序列化完成后，重建任务 ID 索引与势力分组。
	 * After JAXB unmarshalling, rebuilds the quest-id index and faction groups.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		questData.clear();
		sortedByFactionId.clear();
		for (QuestTemplate quest : questsData) {
			questData.put(quest.getId(), quest);
			int npcFactionId = quest.getNpcFactionId();
			if (npcFactionId == 0 || quest.isTimeBased()) {
				continue;
			}
			if (!sortedByFactionId.containsKey(npcFactionId)) {
				List<QuestTemplate> factionQuests = new ArrayList<QuestTemplate>();
				factionQuests.add(quest);
				sortedByFactionId.put(npcFactionId, factionQuests);
			} else {
				sortedByFactionId.get(npcFactionId).add(quest);
			}
		}
	}

	/**
	 * 按任务 ID 获取任务模板。
	 * Returns the quest template for the given quest id.
	 *
	 * @param id 任务 ID / quest id
	 * @return 任务模板，不存在则为 null / quest template or null
	 */
	public QuestTemplate getQuestById(int id) {
		return questData.get(id);
	}

	/**
	 * 返回指定 NPC 势力下、玩家当前可接取的任务列表。
	 * Returns faction quests the player can currently start.
	 *
	 * NPC faction id
	 *
	 * @param npcFactionId 玩家 / player
	 * @param player @return 可接任务列表 / startable quest list
	 */
	public List<QuestTemplate> getQuestsByNpcFaction(int npcFactionId, Player player) {
		List<QuestTemplate> factionQuests = sortedByFactionId.get(npcFactionId);
		List<QuestTemplate> quests = new ArrayList<QuestTemplate>();
		QuestEnv questEnv = new QuestEnv(null, player, 0, 0);
		for (QuestTemplate questTemplate : factionQuests) {
			if (!GameEngineServices.questEngine().isHaveHandler(questTemplate.getId())) {
				continue;
			}
			if (questTemplate.getMinlevelPermitted() != 0 && player.getLevel() < questTemplate.getMinlevelPermitted()) {
				continue;
			}
			questEnv.setQuestId(questTemplate.getId());
			if (QuestService.checkStartConditions(questEnv, false)) {
				quests.add(questTemplate);
			}
		}
		return quests;
	}

	/**
	 * 返回已加载的任务模板数量。
	 * Returns the number of loaded quest templates.
	 *
	 * quest count
	 */
	public int size() {
		return questData.size();
	}

	/**
	 * 返回原始任务模板列表。
	 * Returns the raw quest template list.
	 *
	 * @return 任务模板列表 / quest template list
	 */
	public List<QuestTemplate> getQuestsData() {
		return questsData;
	}

	/**
	 * 设置任务模板列表并立即重建索引。
	 * Sets the quest template list and rebuilds indexes immediately.
	 *
	 * @param questsData 任务模板列表 / quest template list
	 */
	public void setQuestsData(List<QuestTemplate> questsData) {
		this.questsData = questsData;
		afterUnmarshal(null, null);
	}
}
