package com.aionemu.gameserver.questEngine.model;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;

/**
 * 任务事件处理上下文包，携带玩家、目标对象、任务 ID 与对话框 ID 等运行时信息。
 * Quest event processing context bag carrying the player, target object, quest id, dialog id and related runtime data.
 *
 * @author MrPoke
 */
public class QuestEnv {

	/** 事件关联的可见目标（NPC/采集物/静态物等）。 Visible target related to the event (NPC/gatherable/static object, etc.). */
	private VisibleObject visibleObject;
	/** 触发事件的玩家。 Player who triggered the event. */
	private Player player;
	/** 任务 ID。 Quest id. */
	private int questId;
	/** 交互 ID / interaction id */
	private int dialogId;
	/** 扩展奖励索引。 Extended reward index. */
	private int extendedRewardIndex;

	/**
	 * 构造任务事件环境。
	 * Constructs a quest event environment.
	 *
	 * @param visibleObject 事件目标对象 / Event target object
	 * 玩家 / Player
	 * Quest id
	 * Dialog id
	 */
	public QuestEnv(VisibleObject visibleObject, Player player, Integer questId, Integer dialogId) {
		super();
		this.visibleObject = visibleObject;
		this.player = player;
		this.questId = questId;
		this.dialogId = dialogId;
	}

	/**
	 * 返回事件目标可见对象。
	 * Returns the event target visible object.
	 *
	 * Visible object
	 */
	public VisibleObject getVisibleObject() {
		return visibleObject;
	}

	/**
	 * 设置事件目标可见对象。
	 * Sets the event target visible object.
	 *
	 * Visible object
	 */
	public void setVisibleObject(VisibleObject visibleObject) {
		this.visibleObject = visibleObject;
	}

	/**
	 * 返回触发事件的玩家。
	 * Returns the player who triggered the event.
	 *
	 * @return 玩家 / Player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * 设置触发事件的玩家。
	 * Sets the player who triggered the event.
	 *
	 * @param player 玩家 / Player
	 */
	public void setPlayer(Player player) {
		this.player = player;
	}

	/**
	 * 返回任务 ID。
	 * Returns the quest id.
	 *
	 * Quest id
	 */
	public Integer getQuestId() {
		return questId;
	}

	/**
	 * 设置任务 ID。
	 * Sets the quest id.
	 *
	 * Quest id
	 */
	public void setQuestId(Integer questId) {
		this.questId = questId;
	}

	/**
	 * 返回对话框 ID。
	 * Returns the dialog id.
	 *
	 * Dialog id
	 */
	public Integer getDialogId() {
		return dialogId;
	}

	/**
	 * 将当前 dialogId 解析为 {@link QuestDialog} 枚举；未知 ID 返回 {@link QuestDialog#NULL}。
	 * Resolves the current dialogId to a {@link QuestDialog} enum; unknown ids yield {@link QuestDialog#NULL}.
	 *
	 * @return 对话框枚举 / Dialog enum
	 */
	public QuestDialog getDialog() {
		QuestDialog dialog = GameEngineServices.questEngine().getDialog(dialogId);
		if (dialog == null) {
			return QuestDialog.NULL;
		}
		return dialog;
	}

	/**
	 * 设置对话框 ID。
	 * Sets the dialog id.
	 *
	 * Dialog id
	 */
	public void setDialogId(Integer dialogId) {
		this.dialogId = dialogId;
	}

	/**
	 * 根据目标类型返回模板/NPC ID；无目标时返回 0。
	 * Returns the template/NPC id based on target type; 0 when there is no target.
	 *
	 * Target template id
	 */
	public int getTargetId() {
		if (visibleObject == null) {
			return 0;
		} else if (visibleObject instanceof Npc) {
			return ((Npc) visibleObject).getNpcId();
		} else if (visibleObject instanceof Gatherable) {
			return ((Gatherable) visibleObject).getObjectTemplate().getTemplateId();
		} else if (visibleObject instanceof StaticObject) {
			return ((StaticObject) visibleObject).getObjectTemplate().getTemplateId();
		}
		return 0;
	}

	/**
	 * 设置扩展奖励索引。
	 * Sets the extended reward index.
	 *
	 * @param index 奖励索引 / Reward index
	 */
	public void setExtendedRewardIndex(int index) {
		this.extendedRewardIndex = index;
	}

	/**
	 * 返回扩展奖励索引。
	 * Returns the extended reward index.
	 *
	 * Reward index
	 */
	public int getExtendedRewardIndex() {
		return this.extendedRewardIndex;
	}
}
