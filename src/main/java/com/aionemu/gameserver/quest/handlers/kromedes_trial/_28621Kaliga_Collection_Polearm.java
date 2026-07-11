package com.aionemu.gameserver.quest.handlers.kromedes_trial;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 克罗米德试炼任务脚本：Kaliga Collection Polearm（任务 ID 28621）。
 * Kromedes Trial quest script: Kaliga Collection Polearm (quest ID 28621).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28621Kaliga_Collection_Polearm extends QuestHandler
{
	private final static int questId = 28621;
	
	public _28621Kaliga_Collection_Polearm() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(730329).addOnQuestStart(questId); //Kaliga's Polearm Rack.
		qe.registerQuestNpc(730329).addOnTalkEvent(questId); //Kaliga's Polearm Rack.
	}
	
	@Override
	public boolean onCanAct(QuestEnv env, QuestActionType questEventType, Object... objects) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(env.getQuestId());
		int targetId = env.getTargetId();
		if (targetId == 730329) { //Kaliga's Polearm Rack.
			if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 730329) { //Kaliga's Polearm Rack.
			PlayerClass playerClass = player.getCommonData().getPlayerClass();
			if ((playerClass == PlayerClass.WARRIOR || playerClass == PlayerClass.GLADIATOR ||
				playerClass == PlayerClass.TEMPLAR) && player.getCommonData().getRace() == Race.ASMODIANS) {
				if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
					if (env.getDialog() == QuestDialog.USE_OBJECT)
						return sendQuestDialog(env, 1011);
					else
						return sendQuestStartDialog(env);
				} else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
					if (env.getDialog() == QuestDialog.USE_OBJECT)
						return sendQuestDialog(env, 2375);
					else if (env.getDialogId() == 39) {
						if (player.getInventory().getItemCountByItemId(185000102) >= 1) {
							removeQuestItem(env, 185000102, 1);
							qs.setStatus(QuestStatus.REWARD);
							qs.setQuestVar(1);
							qs.setCompleteCount(0);
							updateQuestStatus(env);
							return sendQuestDialog(env, 5);
						} else
							return sendQuestDialog(env, 2716);
					}
				} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
					int var = qs.getQuestVarById(0);
					switch (env.getDialog()) {
						case USE_OBJECT:
							if (var == 1)
								return sendQuestDialog(env, 5);
						case SELECT_NO_REWARD:
							QuestService.finishQuest(env, qs.getQuestVars().getQuestVars() - 1);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
					}
				}
			}
			return false;
		}
		return false;
	}
}
