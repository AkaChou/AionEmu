package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.NpcInfos;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多段汇报任务模板：按变量顺序与多个 NPC 对话推进，最终到结束 NPC 领奖；可选用物品接取。
 * Multi-step report quest template: advances through ordered NPC talks by quest var, then rewards at end NPCs; optional item start.
 */
public class ReportToMany extends QuestHandler {
	/** 起始物品 ID，0 表示用 NPC 接取 / start item id, 0 means NPC start */
	private final int startItem;
	/** 起始 NPC ID 集合 / start NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 结束 NPC ID 集合 / end NPC id set */
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	/** 起始对话页 ID / start dialog page id */
	private final int startDialog;
	/** 结束对话页 ID / end dialog page id */
	private final int endDialog;
	/** 最大 questvarlastreportstep / max quest var (last report step) */
	private final int maxVar;
	/** NPC ID 到汇报信息的映射 / map of NPC id to report info */
	private final Map<Integer, NpcInfos> npcInfos;

	/**
	 * 构造多段汇报任务处理器。
	 * Constructs a multi-step report-to quest handler.
	 *
	 * quest id
	 * start item id
	 * @param startNpcIds 起始 NPC 列表，可为 null / start NPC list, may be null
	 * @param endNpcIds 结束 NPC 列表，可为 null / end NPC list, may be null
	 * @param npcInfos 中途汇报 NPC 信息 / mid-report NPC infos
	 * @param startDialog 起始对话页 / start dialog page
	 * @param endDialog 结束对话页 / end dialog page
	 * max var
	 */
	public ReportToMany(int questId, int startItem, List<Integer> startNpcIds, List<Integer> endNpcIds, Map<Integer, NpcInfos> npcInfos, int startDialog, int endDialog, int maxVar) {
		super(questId);
		this.startItem = startItem;
		if (startNpcIds != null) {
			startNpcs.addAll(startNpcIds);
			startNpcs.remove(0);
		}
		if (endNpcIds != null) {
			endNpcs.addAll(endNpcIds);
			endNpcs.remove(0);
		}
		this.npcInfos = npcInfos;
		this.startDialog = startDialog;
		this.endDialog = endDialog;
		this.maxVar = maxVar;
	}

	/**
	 * 注册起始物品或起始 NPC、中途汇报 NPC 与结束 NPC 事件。
	 * Registers start item or start NPCs, mid-report NPCs and end NPC events.
	 */
	@Override
	public void register() {
		if (startItem != 0) {
			qe.registerQuestItem(startItem, getQuestId());
		} else {
			Iterator<Integer> iterator = startNpcs.iterator();
			while (iterator.hasNext()) {
				int startNpc = iterator.next();
				qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
				qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
			}
		}
		for (int npcId : npcInfos.keySet()) {
			qe.registerQuestNpc(npcId).addOnTalkEvent(getQuestId());
		}
		Iterator<Integer> iterator = endNpcs.iterator();
		while (iterator.hasNext()) {
			int endNpc = iterator.next();
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
	}

	/**
	 * 处理接取、按 var 顺序的多段汇报与奖励对话。
	 * Handles accept, ordered multi-step report dialogs by var, and reward dialogs.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startItem != 0) {
				if (dialog == QuestDialog.ACCEPT_QUEST) {
					QuestService.startQuest(env);
					return closeDialogWindow(env);
				}
			}
			if (startNpcs.isEmpty() || startNpcs.contains(targetId)) {
				if (dialog == QuestDialog.START_DIALOG) {
					if (startDialog != 0) {
						return sendQuestDialog(env, startDialog);
					} else {
						return sendQuestDialog(env, 1011);
					}
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			NpcInfos targetNpcInfo = npcInfos.get(targetId);
			if (var <= maxVar) {
				if (targetNpcInfo != null && var == targetNpcInfo.getVar()) {
					int closeDialog;
					if (targetNpcInfo.getCloseDialog() == 0) {
						closeDialog = 10000 + targetNpcInfo.getVar();
					} else {
						closeDialog = targetNpcInfo.getCloseDialog();
					}
					if (dialog == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, targetNpcInfo.getQuestDialog());
					} else if (dialog.id() == targetNpcInfo.getQuestDialog() + 1 && targetNpcInfo.getMovie() != 0) {
						sendQuestDialog(env, targetNpcInfo.getQuestDialog() + 1);
						return playQuestMovie(env, targetNpcInfo.getMovie());
					} else if (dialog.id() == closeDialog) {
						if ((dialog != QuestDialog.CHECK_COLLECTED_ITEMS && dialog != QuestDialog.CHECK_COLLECTED_ITEMS_SIMPLE) || QuestService.collectItemCheck(env, true)) {
							if (var == maxVar) {
								qs.setStatus(QuestStatus.REWARD);
							    updateQuestStatus(env);
								if (closeDialog == 1009 || closeDialog == 20002 || closeDialog == 39) {
									return sendQuestDialog(env, 5);
								}
							} else {
								qs.setQuestVarById(0, var + 1);
							    updateQuestStatus(env);
							}
						}
						return sendQuestSelectionDialog(env);
					}
				}
			} else if (var > maxVar) {
				if (endNpcs.contains(targetId)) {
					if (dialog == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, endDialog);
					} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						if (startItem != 0) {
							if (!removeQuestItem(env, startItem, 1)) {
								return false;
							}
						}
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestEndDialog(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD && endNpcs.contains(targetId)) {
			NpcInfos targetNpcInfo = npcInfos.get(targetId);
			if (dialog == QuestDialog.USE_OBJECT && targetNpcInfo != null && targetNpcInfo.getQuestDialog() != 0) {
				return sendQuestDialog(env, targetNpcInfo.getQuestDialog());
			}
			return sendQuestEndDialog(env);
		}
		return false;
	}

	/**
	 * 处理起始物品使用：未接取时打开接取对话。
	 * Handles start-item use: opens the accept dialog when the quest is not yet taken.
	 *
	 * @param env 任务环境 / quest environment
	 * @param item 被使用的物品 / used item
	 * handler result
	 */
	@Override
	public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
		if (startItem != 0) {
			Player player = env.getPlayer();
			QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
			}
		}
		return HandlerResult.UNKNOWN;
	}
}
