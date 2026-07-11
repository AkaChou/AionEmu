package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.KillSpawned;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 击杀召唤物类任务的 XML 数据模型，注册 {@link KillSpawned} 模板。
 * XML data model for kill-spawned-monster quests; registers the {@link KillSpawned} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KillSpawnedData")
public class KillSpawnedData extends MonsterHuntData {

	/**
	 * 需先交互生成再击杀的怪物目标列表。
	 * Spawned-monster targets that must be summoned before kill.
	 */
	@XmlElement(name = "spawned_monster", required = true)
	protected List<SpawnedMonster> spawnedMonster;

	/**
	 * 注册 {@link KillSpawned} 模板处理器。
	 * Registers the {@link KillSpawned} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		Map<List<Integer>, SpawnedMonster> spawnedMonsters = new LinkedHashMap<List<Integer>, SpawnedMonster>();
		for (SpawnedMonster m : spawnedMonster) {
			spawnedMonsters.put(m.getNpcIds(), m);
		}
		KillSpawned template = new KillSpawned(id, startNpcIds, endNpcIds, spawnedMonsters);
		questEngine.addQuestHandler(template);
	}
}
