package com.aionemu.gameserver.questEngine.exceptions;

import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 任务对话框事件处理异常，在 onDialogEvent 失败时携带任务环境诊断信息。
 * Quest dialog-event exception carrying diagnostic info from the quest environment when onDialogEvent fails.
 *
 * @author vlog
 */
public class QuestDialogException extends RuntimeException {

	/**
	 * 序列化版本 UID。
	 * Serialization version UID.
	 */
	private static final long serialVersionUID = -4323594385872762590L;

	/**
	 * 根据任务环境构造诊断消息。
	 * Builds a diagnostic message from the quest environment.
	 *
	 * @param env 当前任务事件环境 / Current quest event environment
	 */
	public QuestDialogException(QuestEnv env) {
		super(new String("Info: QuestID: " + env.getQuestId() + ", DialogID: " + env.getDialogId()
				+ env.getVisibleObject().getObjectTemplate().getTemplateId() == null
						? "0"
						: ", TargetID: " + env.getVisibleObject().getObjectTemplate().getTemplateId() + "."
								+ env.getPlayer().getQuestStateList().getQuestState(env.getQuestId()) == null
										? " QuestState not initialized."
										: " QuestState: "
												+ env.getPlayer().getQuestStateList().getQuestState(env.getQuestId())
														.getStatus().toString()
												+ env.getPlayer().getQuestStateList().getQuestState(env.getQuestId())
														.getQuestVarById(0)));
	}
}
