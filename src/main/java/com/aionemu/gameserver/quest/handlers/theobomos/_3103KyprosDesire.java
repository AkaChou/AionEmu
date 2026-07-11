package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.quest.Rewards;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 西奥博莫斯任务脚本：Kypros Desire（任务 ID 3103）。
 * Theobomos quest script: Kypros Desire (quest ID 3103).
 *
 * @author Balthazar
 */
public class _3103KyprosDesire extends QuestHandler {

	private final static int questId = 3103;

	public _3103KyprosDesire() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798225).addOnQuestStart(questId);
		qe.registerQuestNpc(798225).addOnTalkEvent(questId);
		qe.registerQuestNpc(798226).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);

		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();

		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798225) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					default:
						return sendQuestStartDialog(env);
				}
			}
		}

		if (qs == null)
			return false;

		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798226: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 5);
						}
						case SELECT_NO_REWARD: {
							qs.setStatus(QuestStatus.COMPLETE);
							qs.setCompleteCount(1);
							Rewards rewards = DataManager.QUEST_DATA.getQuestById(questId).getRewards().get(0);
							int rewardExp = rewards.getExp();
							player.getCommonData().addExp(rewardExp, RewardType.QUEST);
							PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, QuestStatus.COMPLETE, 2));
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 0));
							updateQuestStatus(env);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
