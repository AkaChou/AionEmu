package com.aionemu.gameserver.quest.handlers.nightmare_circus;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 梦魇马戏团任务脚本：A Hallowed Eve（任务 ID 80341）。
 * Nightmare Circus quest script: A Hallowed Eve (quest ID 80341).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80341A_Hallowed_Eve extends QuestHandler
{
    private final static int questId = 80341;
	
    public _80341A_Hallowed_Eve() {
        super(questId);
    }
	
	@Override
	public void register() {
		// 异界普卡斯。 / Otherworldly Pucas.
		qe.registerQuestNpc(831541).addOnQuestStart(questId);
		qe.registerQuestNpc(831542).addOnQuestStart(questId);
		qe.registerQuestNpc(831543).addOnQuestStart(questId);
		qe.registerQuestNpc(831544).addOnQuestStart(questId);
		qe.registerQuestNpc(831545).addOnQuestStart(questId);
		qe.registerQuestNpc(831546).addOnQuestStart(questId);
		qe.registerQuestNpc(831547).addOnQuestStart(questId);
		qe.registerQuestNpc(831548).addOnQuestStart(questId);
		qe.registerQuestNpc(831541).addOnTalkEvent(questId); 
		qe.registerQuestNpc(831542).addOnTalkEvent(questId);
		qe.registerQuestNpc(831543).addOnTalkEvent(questId);
		qe.registerQuestNpc(831544).addOnTalkEvent(questId);
		qe.registerQuestNpc(831545).addOnTalkEvent(questId);
		qe.registerQuestNpc(831546).addOnTalkEvent(questId);
		qe.registerQuestNpc(831547).addOnTalkEvent(questId);
		qe.registerQuestNpc(831548).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			// 异界普卡斯。 / Otherworldly Pucas.
			if (targetId == 831541 || targetId == 831542 ||
				targetId == 831543 || targetId == 831544 ||
				targetId == 831545 || targetId == 831546 ||
				targetId == 831547 || targetId == 831548) {
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case ASK_ACCEPTION:
					    return sendQuestDialog(env, 4);
					case ACCEPT_QUEST:
					    QuestService.startQuest(env);
					    return sendQuestDialog(env, 1003);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			// 异界普卡斯。 / Otherworldly Pucas.
			switch (targetId) {
				case 831541:
				case 831542:
				case 831543:
				case 831544:
				case 831545:
				case 831546:
				case 831547:
				case 831548: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    // 异界普卡斯。 / Otherworldly Pucas.
			if (targetId == 831541 || targetId == 831542 ||
				targetId == 831543 || targetId == 831544 ||
				targetId == 831545 || targetId == 831546 ||
				targetId == 831547 || targetId == 831548) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
