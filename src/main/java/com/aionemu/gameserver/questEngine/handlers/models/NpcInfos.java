package com.aionemu.gameserver.questEngine.handlers.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.Getter;

/**
 * 多步汇报类任务中单个 NPC 对话步骤的配置。
 * Config for one NPC dialogue step in multi-report quests.
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NpcInfos")
public class NpcInfos {

	/**
	 * 对话目标 NPC 模板 ID。
	 * Target NPC template id.
	 */
	@XmlAttribute(name = "npc_id", required = true)
	protected int npcId;

	/**
	 * 该步骤对应的任务变量值。
	 * Quest variable value for this step.
	 */
	@XmlAttribute(name = "var", required = true)
	protected int var;

	/**
	 * 打开/推进对话的 quest dialog ID。
	 * Quest dialog id used to open/advance the talk.
	 */
	@XmlAttribute(name = "quest_dialog", required = true)
	protected int questDialog;

	/**
	 * 关闭对话时的 dialog ID（可选）。
	 * Dialog id used when closing the conversation (optional).
	 */
	@XmlAttribute(name = "close_dialog")
	protected int closeDialog;

	/**
	 * 本步可播放的过场电影 ID（可选）。
	 * Cutscene movie id for this step (optional).
	 */
	@XmlAttribute(name = "movie")
	protected int movie;

	/**
	 * 设置本步过场电影 ID。
	 * Sets the cutscene movie id for this step.
	 *
	 * Movie id
	 */
	public void setMovie(int movie) {
		this.movie = movie;
	}
}
