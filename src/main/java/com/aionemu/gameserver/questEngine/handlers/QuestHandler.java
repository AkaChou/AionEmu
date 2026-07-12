package com.aionemu.gameserver.questEngine.handlers;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collections;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.XMLStartCondition;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.task.QuestTasks;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 具体任务处理器基类：提供接取、奖励、改变量、给/扣物品、击杀、跟随等通用辅助方法。
 * Concrete quest-handler base with helpers for start, reward, var changes,
 * give/remove items, kills, escort follow-ups, and related common operations.
 *
 * @author MrPoke
 * @modified vlog
 */
public abstract class QuestHandler extends AbstractQuestHandler {
	/** 本处理器绑定的任务 ID / Quest id bound to this handler */
	private final int questId;
	/** 任务引擎引用 / Quest-engine reference */
	protected QuestEngine qe;

	/**
	 * 创建处理器并绑定任务 ID。
	 * Create a handler bound to the given quest id.
	 *
	 * Quest id
	 */
	protected QuestHandler(int questId) {
		this.questId = questId;
		this.qe = GameEngineServices.questEngine();
	}

	/**
	 * 刷新玩家日志中的任务状态。
	 * Update the quest status in the player's journal.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public synchronized void updateQuestStatus(QuestEnv env) {
		sendUpdatePacket(env);
	}

	/**
	 * 将任务步骤推进到下一步（变量 0），或设为可领奖。
	 * Advance the quest step (var 0) or mark it as rewardable.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * @param reward 是否进入 REWARD 状态 / Whether to enter REWARD status
	 */
	public void changeQuestStep(QuestEnv env, int step, int nextStep, boolean reward) {
		changeQuestStep(env, step, nextStep, reward, 0);
	}

	/**
	 * 将指定任务变量的步骤推进到下一步，或设为可领奖。
	 * Advance the given quest-var step, or mark the quest as rewardable.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * @param reward 是否进入 REWARD 状态 / Whether to enter REWARD status
	 * @param varNum 任务变量索引 / Quest-var index
	 */
	public void changeQuestStep(QuestEnv env, int step, int nextStep, boolean reward, int varNum) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getQuestVarById(varNum) == step) {
			if (reward) {
				qs.setStatus(QuestStatus.REWARD);
			} else {
				if (nextStep != step) {
					qs.setQuestVarById(varNum, nextStep);
				}
			}
			if (reward || nextStep != step) {
				updateQuestStatus(env);
			}
		}
	}

	/**
	 * 向玩家发送任务对话框；奖励类 dialogId 需任务处于 REWARD。
	 * Send a quest dialog to the player; reward dialog ids require REWARD status.
	 *
	 * @param env 任务环境 / Quest environment
	 * Dialog id
	 *
	 * @return 是否成功发送 / Whether sent
	 */
	public boolean sendQuestDialog(QuestEnv env, int dialogId) {
		switch (dialogId) {
		case 5:
		case 6:
		case 7:
		case 8:
		case 45:
		case 46:
		case 47:
		case 48:
		case 49:
		case 50:
			Player player = env.getPlayer();
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs == null || qs.getStatus() != QuestStatus.REWARD) {
				return false;
			}
			break;
		}
		sendDialogPacket(env, dialogId);
		return true;
	}

	/**
	 * 发送任务选择列表对话框。
	 * Send the quest-selection list dialog.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return Always {@code true}。
	 */
	public boolean sendQuestSelectionDialog(QuestEnv env) {
		sendQuestSelectionPacket(env, 10);
		return true;
	}

	/**
	 * 关闭当前对话框窗口。
	 * Close the current dialog window.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return Always {@code true}。
	 */
	public boolean closeDialogWindow(QuestEnv env) {
		sendQuestSelectionPacket(env, 0);
		return true;
	}

	/**
	 * 发送默认接取对话框并启动任务（不附带起始物品）。
	 * Send the default start dialog and begin the quest (no starter item).
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestStartDialog(QuestEnv env) {
		return sendQuestStartDialog(env, 0, 0);
	}

	/**
	 * 发送默认接取对话框并启动任务，可附带起始物品。
	 * Send the default start dialog and begin the quest, optionally granting a starter item.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param itemId 起始物品 ID；0 表示不发放 / Starter item id; 0 means none
	 * @param itemCount 起始物品数量 / Starter item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestStartDialog(QuestEnv env, int itemId, int itemCount) {
		switch (env.getDialog()) {
		case ASK_ACCEPTION: {
			return sendQuestDialog(env, 4);
		}
		case ACCEPT_QUEST: {
			if (itemId != 0 && itemCount != 0) {
				if (!env.getPlayer().getInventory().isFullSpecialCube()) {
					if (QuestService.startQuest(env)) {
						giveQuestItem(env, itemId, itemCount);
						return sendQuestDialog(env, 1003);
					}
				}
			} else {
				if (QuestService.startQuest(env)) {
					if (env.getVisibleObject() == null || env.getVisibleObject() instanceof Player) {
						return closeDialogWindow(env);
					} else {
						return sendQuestDialog(env, 1003);
					}
				}
			}
		}
		case ACCEPT_QUEST_SIMPLE: {
			if (itemId != 0 && itemCount != 0) {
				if (!env.getPlayer().getInventory().isFullSpecialCube() && QuestService.startQuest(env)) {
					giveQuestItem(env, itemId, itemCount);
					return closeDialogWindow(env);
				}
			} else if (QuestService.startQuest(env)) {
				if (env.getVisibleObject() == null || (env.getVisibleObject() instanceof Player)) {
					return closeDialogWindow(env);
				}
				return closeDialogWindow(env);
			}
		}
		case REFUSE_QUEST:
		case REFUSE_QUEST_2:
		case REFUSE_QUEST_SIMPLE:
			return closeDialogWindow(env);
		case FINISH_DIALOG: {
			return sendQuestSelectionDialog(env);
		}
		}
		return false;
	}

	/**
	 * 先移除指定任务物品，再发送完成对话框。
	 * Remove the listed quest items, then send the end dialog.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param questItemsToRemove 待移除物品 ID 数组 / Item ids to remove
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestEndDialog(QuestEnv env, int[] questItemsToRemove) {
		Player player = env.getPlayer();
		for (int item : questItemsToRemove) {
			long count = player.getInventory().getItemCountByItemId(item);
			if (count > 0) {
				player.getInventory().decreaseByItemId(item, count);
			}
		}
		return sendQuestEndDialog(env);
	}

	/**
	 * 发送完成对话框并结算奖励（奖励索引 0）。
	 * Send the completion dialog and finish with reward index 0.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestEndDialog(QuestEnv env) {
		return sendQuestEndDialog(env, 0);
	}

	/**
	 * 发送完成对话框并结算指定索引的奖励。
	 * Send the completion dialog and finish with the given reward index.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param reward 奖励列表索引 / Index into the reward list
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestEndDialog(QuestEnv env, int reward) {
		Player player = env.getPlayer();
		int dialogId = env.getDialogId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (dialogId >= 8 && dialogId <= 23) {
			if (qs == null || qs.getStatus() != QuestStatus.REWARD) {
				return false;
			}
			if (QuestService.finishQuest(env, reward)) {
				Npc npc = (Npc) env.getVisibleObject();
				if ("useitem".equals(npc.getAi2().getName()) || ("quest_use_item".equals(npc.getAi2().getName()))) {
					return closeDialogWindow(env);
				} else {
					return sendQuestSelectionDialog(env);
				}
			}
			return false;
		} else if (dialogId == 1009 || dialogId == -1) {
			if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				return sendQuestDialog(env, 5 + reward);
			}
		}
		return false;
	}

	/**
	 * 关闭对话框并推进步骤（无物品/奖励副作用）。
	 * Close dialog and advance step (no item/reward side effects).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep) {
		return defaultCloseDialog(env, step, nextStep, false, false, 0, 0, 0, 0, 0);
	}

	/**
	 * 关闭对话框并推进指定任务变量的步骤。
	 * Close dialog and advance the given quest-var step.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, int varNum) {
		return defaultCloseDialog(env, step, nextStep, false, false, 0, 0, 0, 0, 0, varNum);
	}

	/**
	 * 关闭对话框、推进步骤，可选进入 REWARD 或向同一 NPC 结算。
	 * Close dialog and advance step; optionally enter REWARD or settle with the same NPC.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param sameNpc 是否向同一 NPC 结算 / Settle with the same NPC
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, boolean reward, boolean sameNpc) {
		return defaultCloseDialog(env, step, nextStep, reward, sameNpc, 0, 0, 0, 0, 0);
	}

	/**
	 * 关闭对话框并推进步骤，可指定奖励索引。
	 * Close dialog and advance step with an explicit reward index.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param sameNpc 是否向同一 NPC 结算 / Settle with the same NPC
	 * Reward index
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, boolean reward, boolean sameNpc, int rewardId) {
		return defaultCloseDialog(env, step, nextStep, reward, sameNpc, rewardId, 0, 0, 0, 0);
	}

	/**
	 * 关闭对话框并推进步骤，同时发放/移除任务物品。
	 * Close dialog, advance step, and give/remove quest items.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Item id to give
	 * Count to give
	 * Item id to remove
	 * Count to remove
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, int giveItemId, int giveItemCount, int removeItemId, int removeItemCount) {
		return defaultCloseDialog(env, step, nextStep, false, false, 0, giveItemId, giveItemCount, removeItemId, removeItemCount);
	}

	/**
	 * 关闭对话框并推进步骤，可进入 REWARD 并给/扣物品。
	 * Close dialog, advance step, optionally enter REWARD, and give/remove items.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param sameNpc 是否向同一 NPC 结算 / Settle with the same NPC
	 * Item id to give
	 * Count to give
	 * Item id to remove
	 * Count to remove
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, boolean reward, boolean sameNpc, int giveItemId, int giveItemCount, int removeItemId, int removeItemCount) {
		return defaultCloseDialog(env, step, nextStep, reward, sameNpc, 0, giveItemId, giveItemCount, removeItemId, removeItemCount);
	}

	/**
	 * 关闭对话框并推进步骤（完整参数，变量索引 0）。
	 * Close dialog and advance step (full params, var index 0).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param sameNpc 是否向同一 NPC 结算 / Settle with the same NPC
	 * Reward index
	 * Item id to give
	 * Count to give
	 * Item id to remove
	 * Count to remove
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, boolean reward, boolean sameNpc, int rewardId, int giveItemId, int giveItemCount, int removeItemId, int removeItemCount) {
		return defaultCloseDialog(env, step, nextStep, reward, sameNpc, rewardId, giveItemId, giveItemCount, removeItemId, removeItemCount, 0);
	}

	/**
	 * 关闭对话框时改状态并给/扣任务物品。
	 * Handle close-dialog: change status and give/remove quest items.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param sameNpc 是否向同一 NPC 结算 / Settle with the same NPC
	 * Reward index
	 * Item id to give
	 * Count to give
	 * Item id to remove
	 * Count to remove
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultCloseDialog(QuestEnv env, int step, int nextStep, boolean reward, boolean sameNpc, int rewardId, int giveItemId, int giveItemCount, int removeItemId, int removeItemCount, int varNum) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		if (qs.getQuestVarById(0) == step) {
			if (giveItemId != 0 && giveItemCount != 0) {
				if (!giveQuestItem(env, giveItemId, giveItemCount)) {
					return false;
				}
			}
			removeQuestItem(env, removeItemId, removeItemCount);
			changeQuestStep(env, step, nextStep, reward, varNum);
			if (sameNpc) {
				return sendQuestEndDialog(env, rewardId);
			}
			Npc npc = (Npc) env.getVisibleObject();
			if ("useitem".equals(npc.getAi2().getName())) {
				return closeDialogWindow(env);
			} else {
				return sendQuestSelectionDialog(env);
			}
		}
		return false;
	}

	/**
	 * 检查 quest_data.xml 收集物品是否齐全并推进步骤。
	 * Check collect items from quest_data.xml and advance the step.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param checkOkId 成功对话框 ID / Success dialog id
	 * @param checkFailId 失败对话框 ID / Fail dialog id
	 * @return 是否已处理 / Whether handled
	 */
	public boolean checkQuestItems(QuestEnv env, int step, int nextStep, boolean reward, int checkOkId, int checkFailId) {
		return checkQuestItems(env, step, nextStep, reward, checkOkId, checkFailId, 0, 0);
	}

	/**
	 * 检查 quest_data.xml 收集物品是否齐全，可额外发放物品。
	 * Check collect items from quest_data.xml and optionally grant an extra item.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param checkOkId 成功对话框 ID / Success dialog id
	 * @param checkFailId 失败对话框 ID / Fail dialog id
	 * @param giveItemId 额外发放物品 ID / Extra item id
	 * @param giveItemCount 额外发放数量 / Extra item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean checkQuestItems(QuestEnv env, int step, int nextStep, boolean reward, int checkOkId, int checkFailId, int giveItemId, int giveItemCount) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs.getQuestVarById(0) == step) {
			if (QuestService.collectItemCheck(env, true)) {
				if (giveItemId != 0 && giveItemCount != 0) {
					if (!giveQuestItem(env, giveItemId, giveItemCount)) {
						return false;
					}
				}
				changeQuestStep(env, step, nextStep, reward);
				return sendQuestDialog(env, checkOkId);
			} else {
				return sendQuestDialog(env, checkFailId);
			}
		}
		return false;
	}

	/**
	 * 简化版收集物品检查：失败时直接关闭窗口。
	 * Simplified collect-item check; closes the window on failure.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param checkOkId 成功对话框 ID / Success dialog id
	 * @param giveItemId 额外发放物品 ID / Extra item id
	 * @param giveItemCount 额外发放数量 / Extra item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean checkQuestItemsSimple(QuestEnv env, int step, int nextStep, boolean reward, int checkOkId, int giveItemId, int giveItemCount) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs.getQuestVarById(0) == step) {
			if (QuestService.collectItemCheck(env, true)) {
				if (giveItemId != 0 && giveItemCount != 0) {
					if (!giveQuestItem(env, giveItemId, giveItemCount)) {
						return false;
					}
				}
				changeQuestStep(env, step, nextStep, reward);
				return sendQuestDialog(env, checkOkId);
			} else {
				return closeDialogWindow(env);
			}
		}
		return false;
	}

	/**
	 * 检查未列入 collect_items 的物品是否存在，并推进步骤。
	 * Check items not listed in collect_items and advance the step.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * Item id to check
	 * Required count
	 * Whether to remove
	 * @param checkOkId 成功对话框 ID / Success dialog id
	 * @param checkFailId 失败对话框 ID / Fail dialog id
	 * @param giveItemId 额外发放物品 ID / Extra item id
	 * @param giveItemCount 额外发放数量 / Extra item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean checkItemExistence(QuestEnv env, int step, int nextStep, boolean reward, int itemId, int itemCount, boolean remove, int checkOkId, int checkFailId, int giveItemId, int giveItemCount) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs.getQuestVarById(0) == step) {
			if (checkItemExistence(env, itemId, itemCount, remove)) {
				if (giveItemId != 0 && giveItemCount != 0) {
					if (!giveQuestItem(env, giveItemId, giveItemCount)) {
						return false;
					}
				}
				changeQuestStep(env, step, nextStep, reward);
				return sendQuestDialog(env, checkOkId);
			} else {
				return sendQuestDialog(env, checkFailId);
			}
		}
		return false;
	}

	/**
	 * 检查背包中是否有足够物品，可选扣除。
	 * Check whether the inventory holds enough of an item; optionally remove it.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 * Required count
	 * Whether to remove
	 * Whether the check passes
	 */
	public boolean checkItemExistence(QuestEnv env, int itemId, int itemCount, boolean remove) {
		Player player = env.getPlayer();
		if (player.getInventory().getItemCountByItemId(itemId) >= itemCount) {
			if (remove) {
				if (!removeQuestItem(env, itemId, itemCount)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 播放表情动作。
	 * Broadcast an emotion animation.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param emoteCreature 做出表情的生物 / Emoting creature
	 * Emotion id
	 * Whether to broadcast
	 */
	public void sendEmotion(QuestEnv env, Creature emoteCreature, EmotionId emotion, boolean broadcast) {
		Player player = env.getPlayer();
		int targetId = player.equals(emoteCreature) ? env.getVisibleObject().getObjectId() : player.getObjectId();
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(emoteCreature, EmotionType.EMOTE, emotion.id(), targetId), broadcast);
	}

	/**
	 * 向玩家发放任务物品（不足部分补齐）。
	 * Give quest items to the player (top up to the required count).
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 * Desired total count
	 *
	 * @return 是否成功（已达上限也视为 true） / Whether successful (already enough also returns true)
	 */
	public boolean giveQuestItem(QuestEnv env, int itemId, int itemCount) {
		Player player = env.getPlayer();
		ItemTemplate item = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (itemId != 0 && itemCount != 0) {
			long existentItemCount = player.getInventory().getItemCountByItemId(itemId);
			if (existentItemCount < itemCount) {
				int itemsToGive = (int) (itemCount - existentItemCount);
				return (ItemService.addQuestItems(player,
						Collections.singletonList(new QuestItems(itemId, itemsToGive))));
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CAN_NOT_GET_LORE_ITEM((new DescriptionId(item.getNameId()))));
				return true;
			}
		}
		return false;
	}

	/**
	 * 从玩家背包移除任务物品。
	 * Remove quest items from the player's inventory.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 * Count to remove
	 * Whether removed
	 */
	public boolean removeQuestItem(QuestEnv env, int itemId, long itemCount) {
		Player player = env.getPlayer();
		if (itemId != 0 && itemCount != 0) {
			return player.getInventory().decreaseByItemId(itemId, itemCount);
		}
		return false;
	}

	/**
	 * 播放指定 ID 的任务动画。
	 * Play a quest movie by id.
	 *
	 * @param env 任务环境 / Quest environment
	 * Movie id
	 * Always {@code false}。
	 */
	public boolean playQuestMovie(QuestEnv env, int MovieId) {
		Player player = env.getPlayer();
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, MovieId));
		return false;
	}

	/**
	 * 单目标击杀：在 [startVar, endVar) 区间递增变量。
	 * Single-target kill: increment var within [startVar, endVar).
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC id
	 * @param startVar 起始变量值 / Start var
	 * @param endVar 结束变量值（不含） / End var (exclusive)
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int npcId, int startVar, int endVar) {
		int[] mobids = { npcId };
		return defaultOnKillEvent(env, mobids, startVar, endVar);
	}

	/**
	 * 多目标击杀：在 [startVar, endVar) 区间递增变量。
	 * Multi-target kill: increment var within [startVar, endVar).
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC ids
	 * @param startVar 起始变量值 / Start var
	 * @param endVar 结束变量值（不含） / End var (exclusive)
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int[] npcIds, int startVar, int endVar) {
		return defaultOnKillEvent(env, npcIds, startVar, endVar, 0);
	}

	/**
	 * 单目标击杀：在指定任务变量上递增。
	 * Single-target kill on a specific quest var.
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC id
	 * @param startVar 起始变量值 / Start var
	 * @param endVar 结束变量值（不含） / End var (exclusive)
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int npcId, int startVar, int endVar, int varNum) {
		int[] mobids = { npcId };
		return defaultOnKillEvent(env, mobids, startVar, endVar, varNum);
	}

	/**
	 * 处理击杀事件：匹配目标后在变量区间内递增。
	 * Handle kill event: increment the var when the target matches.
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC ids
	 * @param startVar 起始变量值 / Start var
	 * @param endVar 结束变量值（不含） / End var (exclusive)
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int[] npcIds, int startVar, int endVar, int varNum) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(varNum);
			int targetId = env.getTargetId();
			for (int id : npcIds) {
				if (targetId == id) {
					if (var >= startVar && var < endVar) {
						qs.setQuestVarById(varNum, var + 1);
						updateQuestStatus(env);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 单目标击杀后进入 REWARD 或推进一步。
	 * Single-target kill that enters REWARD or advances one step.
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC id
	 * @param startVar 匹配的变量值 / Matching var value
	 * Enter REWARD
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int npcId, int startVar, boolean reward) {
		int[] mobids = { npcId };
		return (defaultOnKillEvent(env, mobids, startVar, reward, 0));
	}

	/**
	 * 单目标击杀（指定变量）后进入 REWARD 或推进一步。
	 * Single-target kill on a specific var that enters REWARD or advances one step.
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC id
	 * @param startVar 匹配的变量值 / Matching var value
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int npcId, int startVar, boolean reward, int varNum) {
		int[] mobids = { npcId };
		return (defaultOnKillEvent(env, mobids, startVar, reward, varNum));
	}

	/**
	 * 多目标击杀后进入 REWARD 或推进一步。
	 * Multi-target kill that enters REWARD or advances one step.
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC ids
	 * @param startVar 匹配的变量值 / Matching var value
	 * Enter REWARD
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int[] npcIds, int startVar, boolean reward) {
		return (defaultOnKillEvent(env, npcIds, startVar, reward, 0));
	}

	/**
	 * 处理击杀事件并在匹配时进入 REWARD 状态。
	 * Handle kill event and optionally enter REWARD status.
	 *
	 * @param env 任务环境 / Quest environment
	 * Target NPC ids
	 * @param startVar 匹配的变量值 / Matching var value
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillEvent(QuestEnv env, int[] npcIds, int startVar, boolean reward, int varNum) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(varNum);
			int targetId = env.getTargetId();
			for (int id : npcIds) {
				if (targetId == id) {
					if (var == startVar) {
						if (reward) {
							qs.setStatus(QuestStatus.REWARD);
						} else {
							qs.setQuestVarById(varNum, var + 1);
						}
						updateQuestStatus(env);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 处理击杀指定军衔玩家事件。
	 * Handle kill of a ranked player.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param startVar 起始变量值 / Start var
	 * @param endVar 结束变量值 / End var
	 * @param reward 最后一击是否进入 REWARD / Whether the last kill enters REWARD
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnKillRankedEvent(QuestEnv env, int startVar, int endVar, boolean reward) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var >= startVar && var < (endVar - 1)) {
				changeQuestStep(env, var, var + 1, false);
				return true;
			} else if (var == (endVar - 1)) {
				if (reward) {
					qs.setStatus(QuestStatus.REWARD);
				} else {
					qs.setQuestVarById(0, var + 1);
				}
				updateQuestStatus(env);
				return true;
			}
		}
		return false;
	}

	/**
	 * 默认使用技能事件：在变量区间内递增。
	 * Default skill-use event: increment var within a range.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param startVar 起始变量值 / Start var
	 * @param endVar 结束变量值（不含） / End var (exclusive)
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnUseSkillEvent(QuestEnv env, int startVar, int endVar, int varNum) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(varNum);
			if (var >= startVar && var < endVar) {
				changeQuestStep(env, var, var + 1, false, varNum);
				return true;
			}
		}
		return false;
	}

	/**
	 * NPC 开始跟随玩家至目标 NPC；配合 onLostTarget / onReachTarget 使用。
	 * onReachTarget.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * Target NPC id
	 * @param step 当前步骤；0 表示不改 / Current step; 0 means no change
	 * Next step
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultStartFollowEvent(QuestEnv env, Npc follower, int targetNpcId, int step, int nextStep) {
		final Player player = env.getPlayer();
		if (!(env.getVisibleObject() instanceof Npc)) {
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_NPC_INFO(follower, player));
		follower.getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, player);
		player.getController().addTask(TaskId.QUEST_FOLLOW,
				QuestTasks.newFollowingToTargetCheckTask(env, follower, targetNpcId));
		if (step == 0 && nextStep == 0) {
			return true;
		} else {
			return defaultCloseDialog(env, step, nextStep);
		}
	}

	/**
	 * NPC 开始跟随玩家至坐标目标。
	 * NPC starts following the player to a coordinate target.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * @param step 当前步骤；0 表示不改 / Current step; 0 means no change
	 * Next step
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultStartFollowEvent(QuestEnv env, Npc follower, float x, float y, float z, int step, int nextStep) {
		final Player player = env.getPlayer();
		if (!(env.getVisibleObject() instanceof Npc)) {
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_NPC_INFO(follower, player));
		follower.getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, player);
		player.getController().addTask(TaskId.QUEST_FOLLOW,
				QuestTasks.newFollowingToTargetCheckTask(env, follower, x, y, z));
		if (step == 0 && nextStep == 0) {
			return true;
		} else {
			return defaultCloseDialog(env, step, nextStep);
		}
	}

	/**
	 * NPC 开始跟随玩家至指定区域。
	 * NPC starts following the player to a named zone.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * Target zone
	 * @param step 当前步骤；0 表示不改 / Current step; 0 means no change
	 * Next step
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultStartFollowEvent(QuestEnv env, Npc follower, ZoneName zonename, int step, int nextStep) {
		final Player player = env.getPlayer();
		if (!(env.getVisibleObject() instanceof Npc)) {
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_NPC_INFO(follower, player));
		follower.getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, player);
		player.getController().addTask(TaskId.QUEST_FOLLOW, QuestTasks.newFollowingToTargetCheckTask(env, follower, zonename));
		if (step == 0 && nextStep == 0) {
			return true;
		} else {
			return defaultCloseDialog(env, step, nextStep);
		}
	}

	/**
	 * 结束跟随（到达或丢失目标）并推进步骤，可播放动画。
	 * End follow (reach/lost target), advance step, optionally play a movie.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param movie 动画 ID；0 表示不播放 / Movie id; 0 means none
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultFollowEndEvent(QuestEnv env, int step, int nextStep, boolean reward, int movie) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (qs.getQuestVarById(0) == step) {
				changeQuestStep(env, step, nextStep, reward);
				if (movie != 0) {
					playQuestMovie(env, movie);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 结束跟随并推进步骤（无动画）。
	 * End follow and advance step (no movie).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultFollowEndEvent(QuestEnv env, int step, int nextStep, boolean reward) {
		return defaultFollowEndEvent(env, step, nextStep, reward, 0);
	}

	/**
	 * 获得物品时推进步骤。
	 * Advance step when an item is obtained.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean defaultOnGetItemEvent(QuestEnv env, int step, int nextStep, boolean reward) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (qs.getQuestVarById(0) == step) {
				changeQuestStep(env, step, nextStep, reward);
				return true;
			}
		}
		return false;
	}

	/**
	 * 使用任务对象（可令对象死亡）。
	 * Use a quest object (optionally kill it).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param die 是否使对象死亡 / Whether the object dies
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestObject(QuestEnv env, int step, int nextStep, boolean reward, boolean die) {
		return useQuestObject(env, step, nextStep, reward, 0, 0, 0, 0, 0, 0, die);
	}

	/**
	 * 使用任务对象并推进指定变量。
	 * Use a quest object and advance a specific var.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestObject(QuestEnv env, int step, int nextStep, boolean reward, int varNum) {
		return useQuestObject(env, step, nextStep, reward, varNum, 0, 0, 0, 0, 0, false);
	}

	/**
	 * 使用任务对象并额外发放物品。
	 * Use a quest object and grant an extra item.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * Item id to give
	 * Count to give
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestObject(QuestEnv env, int step, int nextStep, boolean reward, int varNum, int addItemId, int addItemCount) {
		return useQuestObject(env, step, nextStep, reward, varNum, addItemId, addItemCount, 0, 0, 0, false);
	}

	/**
	 * 使用任务对象并给/扣物品。
	 * Use a quest object and give/remove items.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * Item id to give
	 * Count to give
	 * Item id to remove
	 * Count to remove
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestObject(QuestEnv env, int step, int nextStep, boolean reward, int varNum, int addItemId, int addItemCount, int removeItemId, int removeItemCount) {
		return useQuestObject(env, step, nextStep, reward, varNum, addItemId, addItemCount, removeItemId, removeItemCount, 0, false);
	}

	/**
	 * 使用任务对象并播放动画。
	 * Use a quest object and play a movie.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * Movie id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestObject(QuestEnv env, int step, int nextStep, boolean reward, int varNum, int movieId) {
		return useQuestObject(env, step, nextStep, reward, varNum, 0, 0, 0, 0, movieId, false);
	}

	/**
	 * 处理使用任务对象事件的完整实现。
	 * Full implementation of the use-quest-object event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * @param varNum 任务变量索引 / Quest-var index
	 * Item id to give
	 * Count to give
	 * Item id to remove
	 * Count to remove
	 * Movie id
	 * @param dieObject 是否使对象死亡 / Whether the object dies
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestObject(QuestEnv env, int step, int nextStep, boolean reward, int varNum, int addItemId, int addItemCount, int removeItemId, int removeItemCount, int movieId, boolean dieObject) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
		if (qs.getQuestVarById(varNum) == step) {
			if (addItemId != 0 && addItemCount != 0) {
				if (!giveQuestItem(env, addItemId, addItemCount)) {
					return false;
				}
			}
			if (removeItemId != 0 && removeItemCount != 0) {
				removeQuestItem(env, removeItemId, removeItemCount);
			}
			if (movieId != 0) {
				playQuestMovie(env, movieId);
			}
			if (dieObject) {
				Npc npc = (Npc) player.getTarget();
				if (npc == null || npc.getObjectId() != env.getVisibleObject().getObjectId()) {
					return false;
				}
				npc.getController().onDie(player);
			}
			changeQuestStep(env, step, nextStep, reward, varNum);
			return true;
		}
		return false;
	}

	/**
	 * 使用任务物品并推进步骤。
	 * Use a quest item and advance the step.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestItem(QuestEnv env, Item item, int step, int nextStep, boolean reward) {
		return useQuestItem(env, item, step, nextStep, reward, 0, 0, 0);
	}

	/**
	 * 使用任务物品并额外发放物品。
	 * Use a quest item and grant an extra item.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * Item id to give
	 * Count to give
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestItem(QuestEnv env, Item item, int step, int nextStep, boolean reward, final int addItemId, final int addItemCount) {
		return useQuestItem(env, item, step, nextStep, reward, addItemId, addItemCount, 0);
	}

	/**
	 * 使用任务物品并播放动画。
	 * Use a quest item and play a movie.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * Movie id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestItem(QuestEnv env, Item item, int step, int nextStep, boolean reward, int movieId) {
		return useQuestItem(env, item, step, nextStep, reward, 0, 0, movieId);
	}

	/**
	 * 使用任务物品（变量索引 0）。
	 * Use a quest item (var index 0).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * Item id to give
	 * Count to give
	 * Movie id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestItem(final QuestEnv env, final Item item, final int step, final int nextStep, final boolean reward, final int addItemId, final int addItemCount, final int movieId) {
		return useQuestItem(env, item, step, nextStep, reward, addItemId, addItemCount, movieId, 0);
	}

	/**
	 * 处理使用任务物品事件：播放 3 秒动画后扣物、发物、推进步骤。
	 * Handle use-item: play a 3s animation, then remove/give items and advance the step.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * @param step 当前步骤 / Current step
	 * Next step
	 * Enter REWARD
	 * Item id to give
	 * Count to give
	 * Movie id
	 * @param varNum 任务变量索引 / Quest-var index
	 * @return 是否已处理 / Whether handled
	 */
	public boolean useQuestItem(final QuestEnv env, final Item item, final int step, final int nextStep, final boolean reward, final int addItemId, final int addItemCount, final int movieId, final int varNum) {
		final Player player = env.getPlayer();
		if (player == null) {
			return false;
		}
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
		final int itemId = item.getItemId();
		final int objectId = item.getObjectId();
		if (qs.getQuestVarById(varNum) == step) {
			PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), objectId, itemId, 3000, 0, 0), true);
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), objectId, itemId, 0, 1, 0), true);
					removeQuestItem(env, itemId, 1);
					if (addItemId != 0 && addItemCount != 0) {
						if (!giveQuestItem(env, addItemId, addItemCount)) {
							return;
						}
					}
					if (movieId != 0) {
						playQuestMovie(env, movieId);
					}
					changeQuestStep(env, step, nextStep, reward, varNum);
				}
			}, 3000);
			return true;
		}
		return false;
	}

	/**
	 * 区域任务完成后启动本任务（无前置条件）。
	 * Start this mission after a zone mission ends (no preconditions).
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnZoneMissionEndEvent(QuestEnv env) {
		int[] quests = { 0 };
		return defaultOnZoneMissionEndEvent(env, quests);
	}

	/**
	 * 区域任务完成后启动本任务（单个前置任务）。
	 * Start this mission after a zone mission ends (one precondition).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param quest 前置任务 ID / Precondition quest id
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnZoneMissionEndEvent(QuestEnv env, int quest) {
		int[] quests = { quest };
		return defaultOnZoneMissionEndEvent(env, quests);
	}

	/**
	 * 区域任务完成后检查条件并启动或锁定本任务（仅应由区域任务处理器调用）。
	 * After a zone mission ends, check requirements and start or lock this mission
	 * (should only be called from onEnterZone mission handlers).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param quests 需先完成的前置任务 / Prerequisite quests
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnZoneMissionEndEvent(QuestEnv env, int[] quests) {
		Player player = env.getPlayer();
		env.setQuestId(questId);
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null) {
			return false;
		}
		if (!QuestService.checkMissionStatConditions(env)) {
			return false;
		}
		if (!QuestService.checkLevelRequirement(questId, player.getCommonData().getLevel())) {
			QuestService.startMission(env, QuestStatus.LOCKED);
			return false;
		}
		for (int id : quests) {
			if (id != 0) {
				QuestState qs2 = player.getQuestStateList().getQuestState(id);
				if (qs2 == null || qs2.getStatus() != QuestStatus.COMPLETE) {
					QuestService.startMission(env, QuestStatus.LOCKED);
					return false;
				}
			}
		}
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(env.getQuestId());
		for (XMLStartCondition startCondition : template.getXMLStartConditions()) {
			if (!startCondition.check(player, false)) {
				if (qs == null) {
					QuestService.startMission(env, QuestStatus.LOCKED);
				}
				return false;
			}

		}
		QuestService.startMission(env, QuestStatus.START);
		return true;
	}

	/**
	 * 升级时启动普通任务（无前置）。
	 * Start a normal mission on level-up (no preconditions).
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnLvlUpEvent(QuestEnv env) {
		int[] quests = { 0 };
		return defaultOnLvlUpEvent(env, quests, false);
	}

	/**
	 * 升级时启动普通任务（单个前置）。
	 * Start a normal mission on level-up (one precondition).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param quest 前置任务 ID / Precondition quest id
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnLvlUpEvent(QuestEnv env, int quest) {
		int[] quests = { quest };
		return defaultOnLvlUpEvent(env, quests, false);
	}

	/**
	 * 升级时启动任务（单个前置，可标记为区域任务）。
	 * Start a mission on level-up (one precondition; may be a zone mission).
	 *
	 * @param env 任务环境 / Quest environment
	 * @param quest 前置任务 ID / Precondition quest id
	 * @param isZoneMission 是否区域任务 / Whether zone mission
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnLvlUpEvent(QuestEnv env, int quest, boolean isZoneMission) {
		int[] quests = { quest };
		return defaultOnLvlUpEvent(env, quests, isZoneMission);
	}

	/**
	 * 升级时检查启动条件并启动任务。
	 * Check mission start conditions on level-up and start the quest.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param quests 需先完成的前置任务 / Prerequisite quests
	 * @param isZoneMission 是否区域任务（未满足前置时不 LOCK） / Whether zone mission (skip LOCK when unmet)
	 * @return 是否成功启动 / Whether started
	 */
	public boolean defaultOnLvlUpEvent(QuestEnv env, int[] quests, boolean isZoneMission) {
		Player player = env.getPlayer();
		env.setQuestId(questId);
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() != QuestStatus.LOCKED) {
			return false;
		}
		if (!QuestService.checkMissionStatConditions(env)) {
			return false;
		}
		if (!QuestService.checkLevelRequirement(questId, player.getCommonData().getLevel())) {
			return false;
		}
		for (int id : quests) {
			if (id != 0) {
				QuestState qs2 = player.getQuestStateList().getQuestState(id);
				if (qs2 == null || qs2.getStatus() != QuestStatus.COMPLETE) {
					if (qs == null && !isZoneMission) {
						QuestService.startMission(env, QuestStatus.LOCKED);
					}
					return false;
				}
			}
		}
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(env.getQuestId());
		for (XMLStartCondition startCondition : template.getXMLStartConditions()) {
			if (!startCondition.check(player, false)) {
				if (qs == null && !isZoneMission) {
					QuestService.startMission(env, QuestStatus.LOCKED);
				}
				return false;
			}
		}
		if (qs == null) {
			QuestService.startMission(env, QuestStatus.START);
		} else {
			qs.setStatus(QuestStatus.START);
			updateQuestStatus(env);
		}
		return true;
	}

	/**
	 * 进入任务区域时自动接取任务。
	 * Auto-start the quest when entering the quest zone.
	 *
	 * @param env 任务环境 / Quest environment
	 * Current zone
	 * @param questZoneName 任务目标区域 / Quest target zone
	 * @return 是否成功接取 / Whether started
	 */
	public boolean defaultOnEnterZoneEvent(QuestEnv env, ZoneName currentZoneName, ZoneName questZoneName) {
		if (questZoneName == currentZoneName) {
			Player player = env.getPlayer();
			if (player == null) {
				return false;
			}
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs == null) {
				env.setQuestId(questId);
				if (QuestService.startQuest(env)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 向奖励 NPC 发送奖励对话框（奖励索引 0）。
	 * Send the reward dialog at the reward NPC (reward index 0).
	 *
	 * @param env 任务环境 / Quest environment
	 * Reward NPC id
	 * @param reportDialogId 汇报对话框 ID / Report dialog id
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestRewardDialog(QuestEnv env, int rewardNpcId, int reportDialogId) {
		return sendQuestRewardDialog(env, rewardNpcId, reportDialogId, 0);
	}

	/**
	 * 向奖励 NPC 发送奖励对话框。
	 * Send the reward dialog at the reward NPC.
	 *
	 * @param env 任务环境 / Quest environment
	 * Reward NPC id
	 * @param reportDialogId 汇报对话框 ID / Report dialog id
	 * Reward index
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestRewardDialog(QuestEnv env, int rewardNpcId, int reportDialogId, int rewardId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == rewardNpcId) {
				if (env.getDialog() == QuestDialog.USE_OBJECT && reportDialogId != 0) {
					return sendQuestDialog(env, reportDialogId);
				} else {
					return sendQuestEndDialog(env, rewardId);
				}
			}
		}
		return false;
	}

	/**
	 * 未接取时向起始 NPC 发送默认对话框（dialog 1011）。
	 * Send the default none-status dialog at the start NPC (dialog 1011).
	 *
	 * @param env 任务环境 / Quest environment
	 * Start NPC id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestNoneDialog(QuestEnv env, int startNpcId) {
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(questId);
		return sendQuestNoneDialog(env, template, startNpcId, 1011);
	}

	/**
	 * 未接取时向起始 NPC 发送指定对话框。
	 * Send a none-status dialog at the start NPC with a custom dialog id.
	 *
	 * @param env 任务环境 / Quest environment
	 * Start NPC id
	 * Dialog id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestNoneDialog(QuestEnv env, int startNpcId, int dialogId) {
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(questId);
		return sendQuestNoneDialog(env, template, startNpcId, dialogId);
	}

	/**
	 * 未接取时按模板向起始 NPC 发送对话框。
	 * Send a none-status dialog at the start NPC using the given template.
	 *
	 * @param env 任务环境 / Quest environment
	 * Quest template
	 * Start NPC id
	 * Dialog id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestNoneDialog(QuestEnv env, QuestTemplate template, int startNpcId, int dialogId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (env.getTargetId() == startNpcId) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, dialogId);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		}
		return false;
	}

	/**
	 * 未接取时发送对话框，接受时附带起始物品。
	 * Send a none-status dialog; grant a starter item on accept.
	 *
	 * @param env 任务环境 / Quest environment
	 * Start NPC id
	 * Dialog id
	 * Starter item id
	 * @param itemCout 起始物品数量 / Starter item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestNoneDialog(QuestEnv env, int startNpcId, int dialogId, int itemId, int itemCout) {
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(questId);
		return sendQuestNoneDialog(env, template, startNpcId, dialogId, itemId, itemCout);
	}

	/**
	 * 未接取时发送默认对话框，接受时附带起始物品。
	 * Send the default none-status dialog and grant a starter item on accept.
	 *
	 * @param env 任务环境 / Quest environment
	 * Start NPC id
	 * Starter item id
	 * @param itemCout 起始物品数量 / Starter item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestNoneDialog(QuestEnv env, int startNpcId, int itemId, int itemCout) {
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(questId);
		return sendQuestNoneDialog(env, template, startNpcId, 1011, itemId, itemCout);
	}

	/**
	 * 未接取时按模板发送对话框，接受时附带起始物品。
	 * Send a none-status dialog from the template and grant a starter item on accept.
	 *
	 * @param env 任务环境 / Quest environment
	 * Quest template
	 * Start NPC id
	 * Dialog id
	 * Starter item id
	 * @param itemCout 起始物品数量 / Starter item count
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendQuestNoneDialog(QuestEnv env, QuestTemplate template, int startNpcId, int dialogId, int itemId, int itemCout) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (env.getTargetId() == startNpcId) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, dialogId);
				}
				if (itemId != 0 && itemCout != 0) {
					if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
						if (giveQuestItem(env, itemId, itemCout)) {
							return sendQuestStartDialog(env);
						} else {
							return true;
						}
					} else {
						return sendQuestStartDialog(env);
					}
				} else {
					return sendQuestStartDialog(env);
				}
			}
		}
		return false;
	}

	/**
	 * 收集类任务的简易接取/拒绝对话框。
	 * Simple accept/refuse dialog for item-collecting quests.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean sendItemCollectingStartDialog(QuestEnv env) {
		switch (env.getDialog()) {
		case ACCEPT_QUEST: {
			QuestService.startQuest(env);
			return sendQuestSelectionDialog(env);
		}
		case REFUSE_QUEST: {
			return sendQuestSelectionDialog(env);
		}
		}
		return false;
	}

	/**
	 * 返回本处理器绑定的任务 ID。
	 * Return the quest id bound to this handler.
	 *
	 * Quest id
	 */
	public int getQuestId() {
		return questId;
	}

	/**
	 * 发送任务状态更新包，并在完成/可领奖时刷新附近任务。
	 * Send the quest-status packet; refresh nearby quests when COMPLETE/REWARD.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	private void sendUpdatePacket(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, qs.getStatus(), qs.getQuestVars().getQuestVars()));
		if (qs.getStatus() == QuestStatus.COMPLETE || qs.getStatus() == QuestStatus.REWARD) {
			GameEngineServices.questEngine().onLvlUp(env);
			player.getController().updateZone();
			player.getController().updateNearbyQuests();
		}
	}

	/**
	 * 向玩家发送带任务 ID 的对话框窗口包。
	 * Send a dialog-window packet that includes the quest id.
	 *
	 * @param env 任务环境 / Quest environment
	 * Dialog id
	 */
	private void sendDialogPacket(QuestEnv env, int dialogId) {
		int objId = 0;
		if (env.getVisibleObject() != null) {
			objId = env.getVisibleObject().getObjectId();
		}
		PacketSendUtility.sendPacket(env.getPlayer(), new SM_DIALOG_WINDOW(objId, dialogId, questId));
	}

	/**
	 * 向玩家发送任务选择列表包（无任务 ID）。
	 * Send a quest-selection packet (no quest id).
	 *
	 * @param env 任务环境 / Quest environment
	 * Dialog id
	 */
	private void sendQuestSelectionPacket(QuestEnv env, int dialogId) {
		int objId = 0;
		if (env.getVisibleObject() != null) {
			objId = env.getVisibleObject().getObjectId();
		}
		PacketSendUtility.sendPacket(env.getPlayer(), new SM_DIALOG_WINDOW(objId, dialogId));
	}

	/**
	 * 子类必须实现：向任务引擎注册本任务关心的事件。
	 * Subclasses must register the events this quest cares about.
	 *
	 * @see com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler#register()
	 */
	@Override
	public abstract void register();
}
