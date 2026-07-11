package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.MonsterHunt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 猎杀怪物类任务的 XML 数据模型，注册 {@link MonsterHunt} 模板；亦为 {@link KillSpawnedData}/{@link MentorMonsterHuntData} 基类。
 * XML data model for monster-hunt quests; registers {@link MonsterHunt}; base for {@link KillSpawnedData}/{@link MentorMonsterHuntData}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MonsterHuntData", propOrder = { "monster" })
@XmlSeeAlso({ KillSpawnedData.class, MentorMonsterHuntData.class })
public class MonsterHuntData extends XMLQuest {

	/**
	 * 需要击杀的怪物目标列表。
	 * Monster targets to kill.
	 */
	@XmlElement(name = "monster", required = true)
	protected List<Monster> monster;

	/**
	 * 接取任务的起始 NPC ID 列表。
	 * Start NPC ids that offer the quest.
	 */
	@XmlAttribute(name = "start_npc_ids", required = true)
	protected List<Integer> startNpcIds;

	/**
	 * 交还任务的结束 NPC ID 列表。
	 * End NPC ids for turn-in.
	 */
	@XmlAttribute(name = "end_npc_ids")
	protected List<Integer> endNpcIds;

	/**
	 * 起始对话 dialog ID。
	 * Start dialogue dialog id.
	 */
	@XmlAttribute(name = "start_dialog_id")
	protected int startDialog;

	/**
	 * 结束对话 dialog ID。
	 * End dialogue dialog id.
	 */
	@XmlAttribute(name = "end_dialog_id")
	protected int endDialog;

	/**
	 * 接取后会对其产生仇恨的 NPC ID 列表（可选）。
	 * NPC ids that become aggro after quest start (optional).
	 */
	@XmlAttribute(name = "aggro_start_npcs")
	protected List<Integer> aggroNpcs;

	/**
	 * 入侵相关世界 ID（可选）。
	 * Invasion-related world id (optional).
	 */
	@XmlAttribute(name = "invasion_world")
	protected int invasionWorld;

	/**
	 * 是否在击杀过程中直接发放奖励。
	 * Whether to grant reward during kills.
	 */
	@XmlAttribute(name = "reward")
	protected boolean reward = false;

	/**
	 * 注册 {@link MonsterHunt} 模板处理器。
	 * Registers the {@link MonsterHunt} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		Map<Monster, Set<Integer>> monsterNpcs = new LinkedHashMap<Monster, Set<Integer>>();
		for (Monster m : monster) {
			monsterNpcs.put(m, new HashSet<Integer>(m.getNpcIds()));
		}
		MonsterHunt template = new MonsterHunt(id, startNpcIds, endNpcIds, monsterNpcs, startDialog, endDialog, aggroNpcs, invasionWorld, reward);
		questEngine.addQuestHandler(template);
	}
}
