package com.aionemu.gameserver.questEngine.model;

/**
 * 任务交互动作类型，区分普通物品使用与任务动作物品使用。
 * Quest interaction action type distinguishing normal item use from quest action-item use.
 *
 * @author MrPoke
 */
public enum QuestActionType {
	/** 普通物品使用触发的任务事件。 Quest event triggered by normal item use. */
	ITEM_USE,
	/** 任务动作物品使用触发的任务事件。 Quest event triggered by action-item use. */
	ACTION_ITEM_USE
}
