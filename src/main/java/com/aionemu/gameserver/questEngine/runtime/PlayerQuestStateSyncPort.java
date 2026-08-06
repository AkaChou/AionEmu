package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/** Production quest-state protocol sync. It runs only after the state transaction committed and was published. */
public final class PlayerQuestStateSyncPort implements QuestStateSyncPort {
	private final QuestPlayerPort players;

	public PlayerQuestStateSyncPort(QuestPlayerPort players) {
		this.players = Objects.requireNonNull(players, "players");
	}

	@Override
	public boolean sync(QuestSnapshot snapshot, QuestMutationPlan plan, QuestStateSyncMode mode) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(mode, "mode");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		if (mode == QuestStateSyncMode.COMPLETION) {
			sendCompletionAvailability(player, plan.questId());
		}
		SM_QUEST_ACTION statePacket = addsQuestToClientList(snapshot.status(), plan.nextStatus())
			? SM_QUEST_ACTION.addQuest(plan.questId(), plan.nextStatus(), plan.nextPackedVariables())
			: SM_QUEST_ACTION.updateQuest(plan.questId(), plan.nextStatus(), plan.nextPackedVariables());
		PacketSendUtility.sendPacket(player, statePacket);
		VisibleObject interaction = snapshot.interactionObjectId() == 0 ? null
			: GameWorldBootstrapServices.world().findVisibleObject(snapshot.interactionObjectId());
		QuestEnv env = new QuestEnv(interaction, player, plan.questId(), 0);
		if (mode == QuestStateSyncMode.COMPLETION) {
			player.getController().updateZone();
			player.getController().updateNearbyQuests();
			GameEngineServices.questEngine().onLvlUp(env);
		} else {
			if (mode.reevaluateLevelQuests()) {
				GameEngineServices.questEngine().onLvlUp(env);
			}
			if (mode.refreshVisibility()) {
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
			}
		}
		if (mode.notifyFinishedNpc()
				&& plan.requiredActions().stream().noneMatch(QuestAction.CompleteQuest.class::isInstance)) {
			throw new IllegalArgumentException("completion sync requires a complete-quest action");
		}
		if (mode.notifyFinishedNpc() && interaction instanceof Npc npc) {
			npc.getAi2().onQuestFinished(player, plan.questId());
		}
		return true;
	}

	static boolean addsQuestToClientList(QuestStatus currentStatus, QuestStatus nextStatus) {
		return !isVisibleInClientQuestList(currentStatus) && isVisibleInClientQuestList(nextStatus);
	}

	private static boolean isVisibleInClientQuestList(QuestStatus status) {
		return status != QuestStatus.NONE && status != QuestStatus.COMPLETE;
	}

	private static void sendCompletionAvailability(Player player, int questId) {
		QuestTemplate template = DataManager.QUEST_DATA == null ? null : DataManager.QUEST_DATA.getQuestById(questId);
		if (template == null) {
			return;
		}
		if ((template.getRepeatCycle() != null && player.getAccessLevel() == 0)
				|| template.getQuestCoolTime() > 0) {
			if (template.isDaily()) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400855, "9"));
			} else if (template.getQuestCoolTime() > 0) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402676, template.getQuestCoolTime()));
			} else {
				PacketSendUtility.sendPacket(player,
					new SM_SYSTEM_MESSAGE(1400857, new DescriptionId(1800667), "9"));
			}
		} else if (template.isTimeBased() && player.getAccessLevel() > 0) {
			PacketSendUtility.sendMessage(player, "You're GM! So system won't apply countNextRepeatTime()");
		}
	}
}
