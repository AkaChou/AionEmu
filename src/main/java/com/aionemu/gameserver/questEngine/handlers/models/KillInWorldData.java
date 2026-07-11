package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.Iterator;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.KillInWorld;

/**
 * 指定世界内击杀类任务的 XML 数据模型，注册 {@link KillInWorld} 模板。
 * XML data model for kill-in-world quests; registers the {@link KillInWorld} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KillInWorldData")
public class KillInWorldData extends XMLQuest {

	/**
	 * 接取任务的起始 NPC ID 列表（可选）。
	 * Start NPC ids that offer the quest (optional).
	 */
	@XmlAttribute(name = "start_npc_ids")
	protected List<Integer> startNpcIds;

	/**
	 * 交还任务的结束 NPC ID 列表。
	 * End NPC ids for turn-in.
	 */
	@XmlAttribute(name = "end_npc_ids", required = true)
	protected List<Integer> endNpcIds;

	/**
	 * 需要击杀的目标数量。
	 * Required kill amount.
	 */
	@XmlAttribute(name = "amount")
	protected int amount;

	/**
	 * 计入进度的世界地图 ID 列表；仅含 0 时展开为全部地图。
	 * World map ids that count; a sole 0 expands to all maps.
	 */
	@XmlAttribute(name = "worlds", required = true)
	protected List<Integer> worldIds;

	/**
	 * 入侵相关世界 ID（可选；0 表示无）。
	 * Invasion-related world id (optional; 0 means none).
	 */
	@XmlAttribute(name = "invasion_world")
	protected int invasionWorld;

	/**
	 * 注册 {@link KillInWorld} 模板；若 worlds 仅含 0 则展开为全部地图。
	 * Registers the {@link KillInWorld} template; expands worlds containing only 0 to all maps.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		if (worldIds.size() == 1 && worldIds.contains(0)) {
			Iterator<WorldMapTemplate> itr = DataManager.WORLD_MAPS_DATA.iterator();
			worldIds.clear();
			while (itr.hasNext()) {
				WorldMapTemplate template = itr.next();
				worldIds.add(template.getMapId());
			}
		}
		KillInWorld template = new KillInWorld(id, endNpcIds, startNpcIds, worldIds, amount, invasionWorld);
		questEngine.addQuestHandler(template);
	}
}
