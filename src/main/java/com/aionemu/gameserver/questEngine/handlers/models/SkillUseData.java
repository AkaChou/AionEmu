package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.SkillUse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 技能使用类任务的 XML 数据模型，注册 {@link SkillUse} 模板。
 * XML data model for skill-use quests; registers the {@link SkillUse} template.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillUseData")
public class SkillUseData extends XMLQuest {

	/**
	 * 接取任务的起始 NPC ID。
	 * Start NPC id that offers the quest.
	 */
	@XmlAttribute(name = "start_npc_id")
	protected int startNpc;

	/**
	 * 交还任务的结束 NPC ID。
	 * End NPC id for turn-in.
	 */
	@XmlAttribute(name = "end_npc_id")
	protected int endNpc;

	/**
	 * 需要使用的技能步骤列表。
	 * Skill-use steps required by the quest.
	 */
	@XmlElement(name = "skill", required = true)
	protected List<QuestSkillData> skills;

	/**
	 * 注册 {@link SkillUse} 模板处理器。
	 * Registers the {@link SkillUse} template handler.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		Map<List<Integer>, QuestSkillData> questSkills = new LinkedHashMap<List<Integer>, QuestSkillData>();
		for (QuestSkillData qsd : skills) {
			questSkills.put(qsd.getSkillIds(), qsd);
		}
		SkillUse questTemplate = new SkillUse(id, startNpc, endNpc, questSkills);
		questEngine.addQuestHandler(questTemplate);
	}
}
