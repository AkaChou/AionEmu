package com.aionemu.gameserver.questEngine.definition;

/**
 * 任务对话框的封闭类型集合。
 * Closed set of quest dialog types.
 */
public enum QuestDialogType {
	/** 与 NPC 对话 / talk to an NPC */
	TALK_TO_NPC,
	/** 任务动作 / quest action */
	QUEST_ACTION,
	/** 展示任务页面 / show a quest page */
	SHOW_QUEST_PAGE,
	/** 展示选择页面 / show a selection page */
	SHOW_SELECTION_PAGE,
	/** NPC 交付任务 / NPC starts a quest */
	NPC_START,
	/** NPC 报告完成 / NPC report */
	NPC_REPORT
}
