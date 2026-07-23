package com.aionemu.gameserver.questEngine.handlers.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;

import lombok.Getter;

/**
 * 任务脚本 XML 数据模型的抽象基类；子类通过 {@link #register(QuestEngine)} 注册对应模板处理器。
 * Abstract base for quest-script XML data models; subclasses register template handlers via {@link #register(QuestEngine)}.
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestScriptData")
@XmlSeeAlso({ ReportToData.class, RelicRewardsData.class, CraftingRewardsData.class, ReportToManyData.class, MonsterHuntData.class, ItemCollectingData.class, WorkOrdersData.class, MentorMonsterHuntData.class, ItemOrdersData.class, FountainRewardsData.class, SkillUseData.class, DataDrivenQuestData.class })
public abstract class XMLQuest {

	/**
	 * 任务 ID（对应 quests 数据中的 id）。
	 * Quest id (matches the id in quest data).
	 */
	@XmlAttribute(name = "id", required = true)
	protected int id;

	/**
	 * 接取/推进时可播放的过场电影 ID；0 表示无。
	 * Cutscene movie id played on accept/progress; 0 means none.
	 */
	@XmlAttribute(name = "movie", required = false)
	protected int questMovie;

	/** Whether this definition was generated from the retail quest data. */
	@XmlAttribute(name = "retail")
	protected boolean retail;

	/**
	 * 将本 XML 配置注册为 {@link QuestEngine} 中的模板任务处理器。
	 * Registers this XML configuration as a template quest handler in {@link QuestEngine}.
	 *
	 * @param questEngine 任务引擎实例 / Quest engine instance
	 */
	public abstract void register(QuestEngine questEngine);
}
