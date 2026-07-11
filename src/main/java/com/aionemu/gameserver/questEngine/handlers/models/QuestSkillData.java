package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.Getter;

/**
 * 技能使用类任务中单个技能步骤的 XML 配置（技能 ID、变量与计数）。
 * XML config for one skill-use step (skill ids, var index and counts).
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestSkillData")
public class QuestSkillData {

	/**
	 * 计入进度的技能 ID 列表。
	 * Skill ids that count toward progress.
	 */
	@XmlAttribute(name = "ids", required = true)
	protected List<Integer> skillIds;

	/**
	 * 起始使用次数（默认 0）。
	 * Starting use count (default 0).
	 */
	@XmlAttribute(name = "start_var")
	protected int startVar = 0;

	/**
	 * 目标使用次数。
	 * Target use count required.
	 */
	@XmlAttribute(name = "end_var", required = true)
	protected int endVar;

	/**
	 * 使用的任务变量编号（默认 0）。
	 * Quest variable number to update (default 0).
	 */
	@XmlAttribute(name = "var_num")
	protected int varNum = 0;
}
