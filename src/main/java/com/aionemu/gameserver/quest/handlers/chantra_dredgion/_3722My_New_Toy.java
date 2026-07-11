package com.aionemu.gameserver.quest.handlers.chantra_dredgion;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 钱特拉德雷金任务脚本：My New Toy（任务 ID 3722）。
 * Chantra Dredgion quest script: My New Toy (quest ID 3722).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3722My_New_Toy extends QuestHandler
{
	private static final int questId = 3722;
	
	public _3722My_New_Toy() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799069).addOnQuestStart(questId); //Yannis.
		qe.registerQuestNpc(799069).addOnTalkEvent(questId); //Yannis.
		qe.registerQuestItem(182202194, questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 799069) { //Yannis.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env, 182202194, 1);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799069) { //Yannis.
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (player.isInsideZone(ZoneName.get("DDREADGION_02_ITEMUSEAREA_Q3722"))) {
				return HandlerResult.fromBoolean(useQuestItem(env, item, 0, 0, true));
			}
		}
		return HandlerResult.FAILED;
	}
}
