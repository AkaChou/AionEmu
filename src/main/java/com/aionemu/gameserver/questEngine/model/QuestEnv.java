package com.aionemu.gameserver.questEngine.model;

import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务事件处理上下文包，携带玩家、目标对象、任务 ID 与对话框 ID 等运行时信息。
 * Quest event processing context bag carrying the player, target object, quest id, dialog id and related runtime data.
 * @author MrPoke
 */
@Getter
@Setter
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
	 * @param visibleObject 事件目标对象 / Event target object
	 * @param player 玩家 / Player
	 * @param questId 任务 ID / Quest id
	 * @param dialogId 对话框 ID / Dialog id
	 */
	public QuestEnv(VisibleObject visibleObject, Player player, Integer questId, Integer dialogId) {
		super();
		this.visibleObject = visibleObject;
		this.player = player;
		this.questId = questId;
		this.dialogId = dialogId;
	}

	/**
	 * 返回任务 ID。
	 * Returns the quest id.
	 * @return 任务 ID / Quest id
	 */
	public Integer getQuestId() {
		return questId;
	}

	/**
	 * 设置任务 ID。
	 * Sets the quest id.
	 * @param questId 任务 ID / Quest id
	 */
	public void setQuestId(Integer questId) {
		this.questId = questId;
	}

	/**
	 * 返回对话框 ID。
	 * Returns the dialog id.
	 * @return 对话框 ID / Dialog id
	 */
	public Integer getDialogId() {
		return dialogId;
	}

	/**
	 * 设置对话框 ID。
	 * Sets the dialog id.
	 * @param dialogId 对话框 ID / Dialog id
	 */
	public void setDialogId(Integer dialogId) {
		this.dialogId = dialogId;
	}

	/**
	 * 根据目标类型返回模板/NPC ID；无目标时返回 0。
	 * Returns the template/NPC id based on target type; 0 when there is no target.
	 * @return 目标模板 ID / Target template id
	 */
	public int getTargetId() {
        return switch (visibleObject) {
            case null -> 0;
            case Npc npc -> npc.getNpcId();
            case Gatherable gatherable -> gatherable.getObjectTemplate().getTemplateId();
            case StaticObject staticObject -> visibleObject.getObjectTemplate().getTemplateId();
            default -> 0;
        };
	}
}
