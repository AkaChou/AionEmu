package com.aionemu.gameserver.questEngine.handlers.template;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 物品指令/信件任务模板：使用起始物品接取，可选途经 NPC 对话推进，最终到结束 NPC 交任务。
 * letter quest template: start via item use, optional mid-NPC talk steps, turn in at the end NPC. / letter quest template: start via item use, optional mid-NPC talk steps, turn in at the end NPC.
 */
public class ItemOrders extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 起始物品模板 ID / start item template id */
	private final int startItemId;
	/** 途经对话 NPC 1，0 表示无 / first talk NPC, 0 if none */
	private final int talkNpc1;
	/** 途经对话 NPC 2，0 表示无 / second talk NPC, 0 if none */
	private final int talkNpc2;
	/** 结束 NPC ID / end NPC id */
	private final int endNpcId;

	/**
	 * 构造物品指令任务处理器。
	 * Constructs an item-orders quest handler.
	 *
	 * quest id
	 * start item id
	 * first talk NPC
	 * second talk NPC
	 * end NPC
	 */
	public ItemOrders(int questId, int startItemId, int talkNpc1, int talkNpc2, int endNpcId) {
		super(questId);
		this.startItemId = startItemId;
		this.questId = questId;
		this.talkNpc1 = talkNpc1;
		this.talkNpc2 = talkNpc2;
		this.endNpcId = endNpcId;
	}

	/**
	 * 注册结束 NPC、起始物品与可选途经 NPC 事件。
	 * Registers end NPC, start item and optional talk NPC events.
	 */
	@Override
	public void register() {
		qe.registerQuestNpc(endNpcId).addOnTalkEvent(questId);
		qe.registerQuestItem(startItemId, questId);
		if (talkNpc1 != 0) {
			qe.registerQuestNpc(talkNpc1).addOnTalkEvent(questId);
		}
		if (talkNpc2 != 0) {
			qe.registerQuestNpc(talkNpc2).addOnTalkEvent(questId);
		}
	}

	/**
	 * 处理物品确认接取、途经 NPC 推进与结束 NPC 交任务对话。
	 * Handles item-confirm accept, mid-NPC progress and end-NPC turn-in dialogs.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理该对话事件 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		if (targetId == 0) {
			if (env.getDialogId() == 1002) {
				QuestService.startQuest(env);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
				return true;
			}
		} else if ((targetId == talkNpc1 && talkNpc1 != 0) || (targetId == talkNpc2 && talkNpc2 != 0)) {
			if (qs != null) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				} else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
					return true;
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (targetId == endNpcId) {
			if (qs != null) {
				if (env.getDialog() == QuestDialog.START_DIALOG && qs.getStatus() == QuestStatus.START) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialogId() == 1009 && qs.getStatus() != QuestStatus.COMPLETE
						&& qs.getStatus() != QuestStatus.NONE) {
					removeQuestItem(env, startItemId, 1);
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

	/**
	 * 处理起始物品使用：播放 3 秒使用动画后打开接取对话。
	 * Handles start-item use: plays a 3s use animation then opens the accept dialog.
	 *
	 * @param env 任务环境 / quest environment
	 * @param item 被使用的物品 / used item
	 * handler result
	 */
	@Override
	public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
		final Player player = env.getPlayer();
		final int id = item.getItemTemplate().getTemplateId();
		final int itemObjId = item.getObjectId();
		if (id != startItemId) {
			return HandlerResult.UNKNOWN;
		}
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, id, 3000, 0, 0), true);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, id, 0, 1, 0), true);
				sendQuestDialog(env, 4);
			}
		}, 3000);
		return HandlerResult.SUCCESS;
	}
}
