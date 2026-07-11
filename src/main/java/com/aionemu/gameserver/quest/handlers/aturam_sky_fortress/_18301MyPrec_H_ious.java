package com.aionemu.gameserver.quest.handlers.aturam_sky_fortress;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.item.ItemService;

import java.util.Collections;

/**
 * 阿图拉姆天空要塞任务脚本：My Prec H ious（任务 ID 18301）。
 * Aturam Sky Fortress quest script: My Prec H ious (quest ID 18301).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18301MyPrec_H_ious extends QuestHandler
{
	private final static int questId = 18301;
	
	public _18301MyPrec_H_ious() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799530).addOnQuestStart(questId);
		qe.registerQuestNpc(799530).addOnTalkEvent(questId);
		qe.registerQuestNpc(730373).addOnTalkEvent(questId);
		qe.registerQuestNpc(730374).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799530) {
				switch (env.getDialog()) {
					case START_DIALOG:
						playQuestMovie(env, 468);
						return sendQuestDialog(env, 4762);
					default:
						return sendQuestStartDialog(env);
				}
			}
		} if (qs == null) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 730373 && var < 7) {
				switch (env.getDialog()) {
					case USE_OBJECT:
						return sendQuestDialog(env, 1011);
					case STEP_TO_1:
						if (env.getVisibleObject() instanceof Npc) {
							targetId = ((Npc) env.getVisibleObject()).getNpcId();
							Npc npc = (Npc) env.getVisibleObject();
							npc.getController().onDelete();
							QuestService.addNewSpawn(npc.getWorldId(), npc.getInstanceId(), 700978, npc.getX(), npc.getY(), npc.getZ(), (byte) 0);
							qs.setQuestVarById(0, var + 1);
							updateQuestStatus(env);
							return closeDialogWindow(env);
						}
					default:
						return sendQuestEndDialog(env);
				}
			} else if (targetId == 730374 && var == 7) {
				switch (env.getDialog()) {
					case USE_OBJECT:
						return sendQuestDialog(env, 1352);
					case STEP_TO_2:
						ItemService.addQuestItems(player, Collections.singletonList(new QuestItems(182212110, 1)));
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					default:
						return sendQuestEndDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799530) {
				switch (env.getDialog()) {
					case USE_OBJECT:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
