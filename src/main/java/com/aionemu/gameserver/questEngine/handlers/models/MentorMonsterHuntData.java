package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.MentorMonsterHunt;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

/**
 * 导师带徒猎杀类任务的 XML 数据模型，注册 {@link MentorMonsterHunt} 模板。
 * XML data model for mentor monster-hunt quests; registers the {@link MentorMonsterHunt} template.
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MentorMonsterHuntData")
public class MentorMonsterHuntData extends MonsterHuntData {

	/**
	 * 被指导者最低等级（默认 1）。
	 * Minimum mentee level (default 1).
	 */
	@XmlAttribute(name = "min_mente_level")
	protected int minMenteLevel = 1;

	/**
	 * 被指导者最高等级（默认 999）。
	 * Maximum mentee level (default 999).
	 */
	@XmlAttribute(name = "max_mente_level")
	protected int maxMenteLevel = 999;

	/**
	 * 注册 {@link MentorMonsterHunt} 模板处理器。
	 * Registers the {@link MentorMonsterHunt} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		Map<Monster, Set<Integer>> monsterNpcs = new LinkedHashMap<Monster, Set<Integer>>();
		for (Monster m : monster) {
			monsterNpcs.put(m, new HashSet<Integer>(m.getNpcIds()));
		}
		MentorMonsterHunt template = new MentorMonsterHunt(id, startNpcIds, endNpcIds, monsterNpcs, minMenteLevel,
				maxMenteLevel);
		questEngine.addQuestHandler(template);
	}
}
