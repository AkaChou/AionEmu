package com.aionemu.gameserver.quest.handlers.daevanion;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 大天使之路任务脚本：Durable Daevanion Weapon（任务 ID 80291）。
 * Daevanion quest script: Durable Daevanion Weapon (quest ID 80291).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80291Durable_Daevanion_Weapon extends QuestHandler
{
	private final static int questId = 80291;
	
	public _80291Durable_Daevanion_Weapon() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(831384).addOnQuestStart(questId);
		qe.registerQuestNpc(831384).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
		targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 831384) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					int plate = player.getEquipment().itemSetPartsEquipped(299);
					int chain = player.getEquipment().itemSetPartsEquipped(298);
					int leather = player.getEquipment().itemSetPartsEquipped(297);
					int cloth = player.getEquipment().itemSetPartsEquipped(296);
					int gunslinger = player.getEquipment().itemSetPartsEquipped(371);
					if (plate != 5 &&
					    chain != 5 &&
						leather != 5 &&
						cloth != 5 &&
						gunslinger != 5) {
						return sendQuestDialog(env, 1003);
					} else {
						return sendQuestDialog(env, 4762);
					}
				} else {
					return sendQuestStartDialog(env);
				}
			}
		}
		if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		int var1 = qs.getQuestVarById(1);
		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 831384) {
				switch (env.getDialog()) {
					case START_DIALOG:
					if (var == 0) {
						return sendQuestDialog(env, 1011);
					}
					case CHECK_COLLECTED_ITEMS:
					if (var == 0) {
						return checkQuestItems(env, 0, 1, true, 5, 0);
					}
					break;
					case SELECT_ACTION_1352:
					if (var == 0) {
						return sendQuestDialog(env, 1352);
					}
				}
			}
			return false;
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 831384) {
				return sendQuestEndDialog(env);
			}
			return false;
		}
		return false;
	}
}
