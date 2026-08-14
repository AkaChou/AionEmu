package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/** 生产任务状态协议同步。仅在状态事务提交并发布后运行。 / Production quest-state protocol sync. It runs only after the state transaction committed and was published. */
public final class PlayerQuestStateSyncPort implements QuestStateSyncPort {
	private final QuestPlayerPort players;
	private final IntFunction<QuestMetadata> metadata;
	private final IntFunction<VisibleObject> interactionObjects;
	private final Consumer<Player> zoneRefresh;
	private final Consumer<Player> nearbyQuestRefresh;
	private final Consumer<QuestEnv> levelQuestRefresh;

	public PlayerQuestStateSyncPort(QuestPlayerPort players) {
		this(players,
			questId -> GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null),
			objectId -> GameWorldBootstrapServices.world().findVisibleObject(objectId),
			player -> player.getController().updateZone(),
			player -> player.getController().updateNearbyQuests(),
			env -> GameEngineServices.questEngine().onLvlUp(env));
	}

	PlayerQuestStateSyncPort(QuestPlayerPort players, IntFunction<QuestMetadata> metadata) {
		this(players, metadata,
			objectId -> GameWorldBootstrapServices.world().findVisibleObject(objectId),
			player -> player.getController().updateZone(),
			player -> player.getController().updateNearbyQuests(),
			env -> GameEngineServices.questEngine().onLvlUp(env));
	}

	PlayerQuestStateSyncPort(QuestPlayerPort players, IntFunction<QuestMetadata> metadata,
			IntFunction<VisibleObject> interactionObjects, Consumer<Player> zoneRefresh,
			Consumer<Player> nearbyQuestRefresh, Consumer<QuestEnv> levelQuestRefresh) {
		this.players = Objects.requireNonNull(players, "players");
		this.metadata = Objects.requireNonNull(metadata, "metadata");
		this.interactionObjects = Objects.requireNonNull(interactionObjects, "interactionObjects");
		this.zoneRefresh = Objects.requireNonNull(zoneRefresh, "zoneRefresh");
		this.nearbyQuestRefresh = Objects.requireNonNull(nearbyQuestRefresh, "nearbyQuestRefresh");
		this.levelQuestRefresh = Objects.requireNonNull(levelQuestRefresh, "levelQuestRefresh");
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
			sendCompletionAvailability(player, metadata.apply(plan.questId()));
		}
		SM_QUEST_ACTION statePacket = addsQuestToClientList(snapshot.status(), plan.nextStatus())
			? SM_QUEST_ACTION.addQuest(plan.questId(), plan.nextStatus(), plan.nextPackedVariables())
			: SM_QUEST_ACTION.updateQuest(plan.questId(), plan.nextStatus(), plan.nextPackedVariables());
		PacketSendUtility.sendPacket(player, statePacket);
		if (mode == QuestStateSyncMode.COMPLETION) {
			PacketSendUtility.sendPacket(player, SM_QUEST_ACTION.removeQuestFromClientList(plan.questId()));
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_COMPLETED_LIST(player.getQuestStateList().getAllFinishedQuests()));
		}
		VisibleObject interaction = snapshot.interactionObjectId() == 0 ? null
			: interactionObjects.apply(snapshot.interactionObjectId());
		QuestEnv env = new QuestEnv(interaction, player, plan.questId(), 0);
		if (mode == QuestStateSyncMode.COMPLETION) {
			zoneRefresh.accept(player);
			nearbyQuestRefresh.accept(player);
			levelQuestRefresh.accept(env);
		} else {
			if (mode.reevaluateLevelQuests()) {
				levelQuestRefresh.accept(env);
			}
			if (mode.refreshVisibility()) {
				zoneRefresh.accept(player);
				nearbyQuestRefresh.accept(player);
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

	private static void sendCompletionAvailability(Player player, QuestMetadata metadata) {
		if (metadata == null) {
			return;
		}
		var repeat = metadata.repeatPolicy();
		boolean timeBased = repeat.daily() || repeat.weekly() || !metadata.repeatCycles().isEmpty();
		if ((timeBased && player.getAccessLevel() == 0) || repeat.cooldownSeconds() > 0) {
			if (repeat.daily()) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400855, "9"));
			} else if (repeat.cooldownSeconds() > 0) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402676, repeat.cooldownSeconds()));
			} else {
				PacketSendUtility.sendPacket(player,
					new SM_SYSTEM_MESSAGE(1400857, new DescriptionId(1800667), "9"));
			}
		} else if (timeBased && player.getAccessLevel() > 0) {
			PacketSendUtility.sendMessage(player, "You're GM! So system won't apply countNextRepeatTime()");
		}
	}
}
