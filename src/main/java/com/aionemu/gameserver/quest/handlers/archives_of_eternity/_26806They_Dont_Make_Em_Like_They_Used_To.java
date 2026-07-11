package com.aionemu.gameserver.quest.handlers.archives_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 永恒档案馆任务脚本：They Dont Make Em Like They Used To（任务 ID 26806）。
 * Archives of Eternity quest script: They Dont Make Em Like They Used To (quest ID 26806).
 *
 * @author (Encom)
 */
public class _26806They_Dont_Make_Em_Like_They_Used_To extends QuestHandler {

    private final static int questId = 26806;
	private final static int[] IDEternity01Mobs = {220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330};
	private final static int[] IDEternity01Boss = {857450, 857452, 857454, 857456, 857458, 857459};
    public _26806They_Dont_Make_Em_Like_They_Used_To() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerOnEnterWorld(questId);
		qe.registerQuestNpc(806149).addOnTalkEvent(questId);
		for (int mob: IDEternity01Mobs) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		} for (int mob: IDEternity01Boss) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 806149) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		}
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806149) {
                if (env.getDialogId() == 31) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialogId() == 1009) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
			else { // 赏金任务（DragonicK？） / Bounty Quest made DragonicK?
				// 所选物品不是可选的。 / Selected item is not optional.
				env.setDialogId(8);
				env.setExtendedRewardIndex(1);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(806149, 0));
				if (QuestService.finishQuest(env)) {
					return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
	
	@Override
    public boolean onEnterWorldEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (player.getWorldId() == 301540000) {
            if (qs == null || qs.canRepeat()) {
                env.setQuestId(questId);
                if (QuestService.startQuest(env)) {
					return true;
				}
            }
        }
        return false;
    }
	
	public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 220306:
				case 220309:
				case 220312:
				case 220315:
				case 220318:
				case 220324:
				case 220327:
				case 220330:
				    if (qs.getQuestVarById(1) < 30) {
					    qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					    updateQuestStatus(env);
				    } if (qs.getQuestVarById(1) >= 30 && qs.getQuestVarById(2) >= 2) {
						qs.setQuestVarById(0, 1);
					    qs.setStatus(QuestStatus.REWARD);
					    updateQuestStatus(env);
				    }
				break;
				case 857450:
				case 857452:
				case 857454:
				case 857456:
				case 857458:
				case 857459:
			        if (qs.getQuestVarById(2) < 2) {
					    qs.setQuestVarById(2, qs.getQuestVarById(2) + 1);
					    updateQuestStatus(env);
				    } if (qs.getQuestVarById(2) >= 2 && qs.getQuestVarById(1) >= 30) {
						qs.setQuestVarById(0, 1);
					    qs.setStatus(QuestStatus.REWARD);
					    updateQuestStatus(env);
				    }
				break;
            }
        }
        return false;
    }
}
