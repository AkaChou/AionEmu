package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/** 希隆任务脚本：The Miser's Map（任务 ID 1561）。 */
public class _1561TheMisersMap extends QuestHandler {
	private static final int QUEST_ID = 1561;
	private static final int MAP_ITEM_ID = 182201728;

	public _1561TheMisersMap() {
		super(QUEST_ID);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(700188).addOnTalkEvent(QUEST_ID);
		qe.registerQuestItem(MAP_ITEM_ID, QUEST_ID);
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		env.setQuestId(QUEST_ID);
		Player player = env.getPlayer();
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		if (state != null && state.getStatus() != QuestStatus.NONE) {
			return HandlerResult.UNKNOWN;
		}
		int itemObjectId = item.getObjectId();
		PacketSendUtility.broadcastPacket(player,
			new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjectId, MAP_ITEM_ID, 3000, 0, 0), true);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjectId, MAP_ITEM_ID, 0, 1, 0), true);
			removeQuestItem(env, MAP_ITEM_ID, 1);
			QuestService.startQuest(env);
		}, 3000);
		return HandlerResult.SUCCESS;
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		if (state == null) {
			return false;
		}
		int targetId = env.getVisibleObject() instanceof Npc npc ? npc.getNpcId() : 0;
		if (state.getStatus() == QuestStatus.START && targetId == 700188
				&& state.getQuestVarById(0) == 0) {
			if (env.getDialog() == QuestDialog.START_DIALOG || env.getDialog() == QuestDialog.USE_OBJECT) {
				return sendQuestDialog(env, 2375);
			}
			if (env.getDialog() == QuestDialog.SELECT_REWARD) {
				return defaultCloseDialog(env, 0, 0, true, true);
			}
		}
		if (state.getStatus() == QuestStatus.REWARD && targetId == 700188) {
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
