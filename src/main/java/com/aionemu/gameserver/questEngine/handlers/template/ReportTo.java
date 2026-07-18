package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 汇报任务模板：从起始 NPC 接取（可选发放任务物品），到结束 NPC 汇报并领奖。
 * Report-to quest template: accept from start NPCs (optional quest item), report to end NPCs and claim reward.
 */
public class ReportTo extends QuestHandler {
	/** 起始 NPC ID 集合 / start NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 结束 NPC ID 集合 / end NPC id set */
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	/** 任务物品 ID，0 表示无 / quest item id, 0 if none */
	private final int itemId;
	/** 起始对话页 ID，0 用默认 1011 / start dialog page, 0 uses default 1011 */
	private final int startDialogId;
	/** 交任务对话页 ID，0 用默认 2375 / turn-in dialog page, 0 uses default 2375 */
	private final int startDialogId2;

	/**
	 * 构造汇报任务处理器。
	 * Constructs a report-to quest handler.
	 *
	 * quest id
	 * start NPC list
	 * @param endNpcIds 结束 NPC 列表，可为 null / end NPC list, may be null
	 * @param startDialogId 起始对话页 / start dialog page
	 * @param startDialogId2 交任务对话页 / turn-in dialog page
	 * quest item id
	 */
	public ReportTo(int questId, List<Integer> startNpcIds, List<Integer> endNpcIds, int startDialogId,
			int startDialogId2, int itemId) {
		super(questId);
		startNpcs.addAll(startNpcIds);
		startNpcs.remove(0);
		if (endNpcIds != null) {
			endNpcs.addAll(endNpcIds);
			endNpcs.remove(0);
		}
		this.startDialogId = startDialogId;
		this.startDialogId2 = startDialogId2;
		this.itemId = itemId;
	}

	/**
	 * 注册起始与结束 NPC 的接取/对话事件。
	 * Registers start and end NPC quest-start/talk events.
	 */
	@Override
	public void register() {
		Iterator<Integer> iterator = startNpcs.iterator();
		while (iterator.hasNext()) {
			int startNpc = iterator.next();
			qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
		}
		iterator = endNpcs.iterator();
		while (iterator.hasNext()) {
			int endNpc = iterator.next();
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
	}

	/**
	 * 处理接取、汇报与奖励对话事件；交任务时回收可选任务物品。
	 * Handles accept, report and reward dialogs; removes the optional quest item on turn-in.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if ((startNpcs.isEmpty()) || (startNpcs.contains(targetId))) {
				switch (dialog) {
				case START_DIALOG: {
					if (startDialogId != 0) {
						return sendQuestDialog(env, startDialogId);
					} else {
						return sendQuestDialog(env, 1011);
					}
				}
				case ACCEPT_QUEST:
				case ACCEPT_QUEST_SIMPLE: {
					if (itemId != 0) {
						if (giveQuestItem(env, itemId, 1)) {
							return sendQuestStartDialog(env);
						}
						return false;
					} else {
						return sendQuestStartDialog(env);
					}
				}
				default: {
					return sendQuestStartDialog(env);
				}
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (endNpcs.contains(targetId)) {
				switch (dialog) {
				case START_DIALOG: {
					if (startDialogId2 != 0) {
						return sendQuestDialog(env, startDialogId2);
					} else {
						return sendQuestDialog(env, 2375);
					}
				}
				case SELECT_REWARD: {
					if (itemId != 0) {
						if (player.getInventory().getItemCountByItemId(itemId) < 1) {
							return sendQuestSelectionDialog(env);
						}
					}
					removeQuestItem(env, itemId, 1);
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
				}
			} else if (startNpcs.contains(targetId) && dialog == QuestDialog.FINISH_DIALOG) {
				return sendQuestSelectionDialog(env);
			}
		} else if ((qs.getStatus() == QuestStatus.REWARD) && (endNpcs.contains(targetId))) {
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
