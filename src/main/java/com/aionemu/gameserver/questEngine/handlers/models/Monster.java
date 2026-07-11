package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.Getter;

/**
 * 猎杀类任务中单个怪物目标的 XML 配置（变量索引、计数上下限、NPC 列表）。
 * XML config for a single monster target in hunt quests (var index, count bounds, NPC ids).
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Monster")
public class Monster {

	/**
	 * 任务进度变量索引（quest var）。
	 * Quest progress variable index.
	 */
	@XmlAttribute(name = "var", required = true)
	protected int var;

	/**
	 * 起始计数（可选；未配置时由模板默认）。
	 * Starting kill count (optional; template default when absent).
	 */
	@XmlAttribute(name = "start_var")
	protected Integer startVar;

	/**
	 * 目标击杀数量（达到后该步完成）。
	 * Target kill count required to complete this step.
	 */
	@XmlAttribute(name = "end_var", required = true)
	protected int endVar;

	/**
	 * 计入该目标的 NPC 模板 ID 列表。
	 * NPC template ids that count toward this target.
	 */
	@XmlAttribute(name = "npc_ids", required = true)
	protected List<Integer> npcIds;
}
