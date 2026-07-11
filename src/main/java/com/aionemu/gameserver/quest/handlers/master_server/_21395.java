package com.aionemu.gameserver.quest.handlers.master_server;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 主服务器任务脚本（任务 ID 21395）。
 * Master server quest script (quest ID 21395).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _21395 extends QuestHandler
{
    private final static int questId = 21395;
	
    public _21395() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerOnKillInWorld(220140000, questId);
		qe.registerQuestNpc(835676).addOnTalkEvent(questId);
		qe.registerQuestNpc(835676).addOnAtDistanceEvent(questId);
    }
	
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		if (env.getVisibleObject() instanceof Player && player != null) {
			if ((env.getPlayer().getLevel() >= (((Player)env.getVisibleObject()).getLevel() - 5)) &&
			    (env.getPlayer().getLevel() <= (((Player)env.getVisibleObject()).getLevel() + 9))) {
				return defaultOnKillRankedEvent(env, 0, 3, true);
			}
		}
		return false;
	}
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 835676) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
            }
        }
		return false;
	}
	
    @Override
	public boolean onAtDistanceEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			QuestService.startQuest(env);
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
			return true;
		}
		return false;
	}
}
