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
import com.aionemu.gameserver.services.QuestService;

/**
 * 物品收集任务模板：从起始 NPC 接取，可选中间 NPC/交互物，到结束 NPC 校验收集物品后领奖。
 * Item-collecting quest template: accept from start NPCs, optional mid NPC/action items, turn in collected items at end NPCs.
 */
public class ItemCollecting extends QuestHandler {
	/** 起始 NPC ID 集合 / start NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 可交互的任务物品/物体 ID 集合 / action item/object id set */
	private final Set<Integer> actionItems = new HashSet<Integer>();
	/** 结束 NPC ID 集合 / end NPC id set */
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	/** 接取时播放的电影 ID，0 表示无 / movie id on accept, 0 if none */
	private final int questMovie;
	/** 中间推进 NPC ID，0 表示无 / mid-step NPC id, 0 if none */
	private final int nextNpcId;
	/** 起始对话页 ID，0 时用默认 1011 / start dialog page id, 0 uses default 1011 */
	private final int startDialogId;
	/** 交任务对话页 ID，0 时用默认 2375 / turn-in dialog page id, 0 uses default 2375 */
	private final int startDialogId2;
	/** 接取时发放、领奖时回收的任务物品 ID，0 表示无 / quest item given on start and removed on reward, 0 if none */
	private final int itemId;

	/**
	 * 构造物品收集任务处理器。
	 * Constructs an item-collecting quest handler.
	 *
	 * quest id
	 * start NPC id list
	 * mid-step NPC id
	 * @param actionItemIds 交互物 ID 列表，可为 null / action item id list, may be null
	 * @param endNpcIds 结束 NPC ID 列表，null 则复用起始 NPC / end NPC id list, null reuses start NPCs
	 * movie id
	 * @param startDialogId 起始对话页 / start dialog page
	 * @param startDialogId2 交任务对话页 / turn-in dialog page
	 * quest item id
	 */
	public ItemCollecting(int questId, List<Integer> startNpcIds, int nextNpcId, List<Integer> actionItemIds, List<Integer> endNpcIds, int questMovie, int startDialogId, int startDialogId2, int itemId) {
		super(questId);
		startNpcs.addAll(startNpcIds);
		startNpcs.remove(0);
		this.nextNpcId = nextNpcId;
		if (actionItemIds != null) {
			actionItems.addAll(actionItemIds);
			actionItems.remove(0);
		}
		if (endNpcIds == null) {
			endNpcs.addAll(startNpcs);
		} else {
			endNpcs.addAll(endNpcIds);
			endNpcs.remove(0);
		}
		this.questMovie = questMovie;
		this.startDialogId = startDialogId;
		this.startDialogId2 = startDialogId2;
		this.itemId = itemId;
	}

	/**
	 * 注册起始、中间、交互物与结束 NPC 相关事件。
	 * Registers events for start, mid, action-item and end NPCs.
	 */
	@Override
	public void register() {
		Iterator<Integer> iterator = startNpcs.iterator();
		while (iterator.hasNext()) {
			int startNpc = iterator.next();
			qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
		}
		if (nextNpcId != 0) {
			qe.registerQuestNpc(nextNpcId).addOnTalkEvent(getQuestId());
		}
		iterator = actionItems.iterator();
		while (iterator.hasNext()) {
			int actionItem = iterator.next();
			qe.registerQuestNpc(actionItem).addOnTalkEvent(getQuestId());
			qe.registerCanAct(getQuestId(), actionItem);
		}
		iterator = endNpcs.iterator();
		while (iterator.hasNext()) {
			int endNpc = iterator.next();
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
	}

	/**
	 * 处理接取、中间推进、收集校验与奖励对话事件。
	 * Handles accept, mid-step, collect-check and reward dialog events.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理该对话事件 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startNpcs.isEmpty() || startNpcs.contains(targetId)) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestDialog(env, startDialogId != 0 ? startDialogId : 1011);
				}
				case STEP_TO_1: {
					QuestService.startQuest(env);
					return closeDialogWindow(env);
				}
				case SELECT_ACTION_1012: {
					if (questMovie != 0) {
						playQuestMovie(env, questMovie);
					}
					return sendQuestDialog(env, 1012);
				}
				default: {
					if (itemId != 0) {
						giveQuestItem(env, itemId, 1);
					}
					return sendQuestStartDialog(env);
				}
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == nextNpcId && var == 0) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestDialog(env, 1352);
				}
				case STEP_TO_1: {
					return defaultCloseDialog(env, 0, 1);
				}
				}
			} else if (endNpcs.contains(targetId)) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestDialog(env, startDialogId2 != 0 ? startDialogId2 : 2375);
				}
				case CHECK_COLLECTED_ITEMS: {
					return checkQuestItems(env, var, var, true, 5, 2716);
				}
				case CHECK_COLLECTED_ITEMS_SIMPLE: {
					return checkQuestItemsSimple(env, var, var, true, 5, 0, 0);
				}
				case SET_REWARD: {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
				case STEP_TO_1: {
					return checkQuestItemsSimple(env, var, var, true, 5, 0, 0);
				}
				case STEP_TO_2: {
					return checkQuestItemsSimple(env, var, var, true, 6, 0, 0);
				}
				case STEP_TO_3: {
					return checkQuestItemsSimple(env, var, var, true, 7, 0, 0);
				}
				case STEP_TO_4: {
					return checkQuestItemsSimple(env, var, var, true, 8, 0, 0);
				}
				}
			} else if (targetId != 0 && actionItems.contains(targetId)) {
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (endNpcs.contains(targetId)) {
				if (itemId != 0) {
					removeQuestItem(env, itemId, 1);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
