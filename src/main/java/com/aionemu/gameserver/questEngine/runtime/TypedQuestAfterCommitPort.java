package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;

import java.util.Objects;

/** 将封闭的提交后动作集路由到类型化协议端口。 / Routes the closed after-commit action set to typed protocol ports. */
public final class TypedQuestAfterCommitPort implements QuestAfterCommitPort {
	private final QuestDialogPort dialogPort;
	private final QuestTeleportPort teleportPort;
	private final QuestMoviePort moviePort;
	private final QuestSpawnPort spawnPort;
	private final QuestAiPort aiPort;
	private final QuestTimerPort timerPort;
	private final QuestStateSyncPort stateSyncPort;
	private final QuestStatsPort statsPort;
	private final QuestEffectPort effectPort;
	private final QuestNpcPort npcPort;
	private final QuestSystemMessagePort systemMessagePort;
	private volatile QuestBroadcastPort broadcastPort;

	public static TypedQuestAfterCommitPort fullyComposed(QuestDialogPort dialogPort,
			QuestTeleportPort teleportPort, QuestMoviePort moviePort, QuestSpawnPort spawnPort,
			QuestAiPort aiPort, QuestTimerPort timerPort, QuestStateSyncPort stateSyncPort,
			QuestStatsPort statsPort, QuestEffectPort effectPort, QuestNpcPort npcPort) {
		return fullyComposed(dialogPort, teleportPort, moviePort, spawnPort, aiPort, timerPort,
			stateSyncPort, statsPort, effectPort, npcPort, null);
	}

	public static TypedQuestAfterCommitPort fullyComposed(QuestDialogPort dialogPort,
			QuestTeleportPort teleportPort, QuestMoviePort moviePort, QuestSpawnPort spawnPort,
			QuestAiPort aiPort, QuestTimerPort timerPort, QuestStateSyncPort stateSyncPort,
			QuestStatsPort statsPort, QuestEffectPort effectPort, QuestNpcPort npcPort,
			QuestSystemMessagePort systemMessagePort) {
		return new TypedQuestAfterCommitPort(
			Objects.requireNonNull(dialogPort, "dialogPort"),
			Objects.requireNonNull(teleportPort, "teleportPort"),
			Objects.requireNonNull(moviePort, "moviePort"),
			Objects.requireNonNull(spawnPort, "spawnPort"),
			Objects.requireNonNull(aiPort, "aiPort"),
			Objects.requireNonNull(timerPort, "timerPort"),
			Objects.requireNonNull(stateSyncPort, "stateSyncPort"),
			Objects.requireNonNull(statsPort, "statsPort"),
			Objects.requireNonNull(effectPort, "effectPort"),
			Objects.requireNonNull(npcPort, "npcPort"), systemMessagePort);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort) {
		this(dialogPort, null, null, null, null, null, null, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort) {
		this(dialogPort, teleportPort, null, null, null, null, null, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort) {
		this(dialogPort, teleportPort, moviePort, null, null, null, null, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort) {
		this(dialogPort, teleportPort, moviePort, spawnPort, null, null, null, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort, QuestAiPort aiPort) {
		this(dialogPort, teleportPort, moviePort, spawnPort, aiPort, null, null, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort, QuestAiPort aiPort, QuestTimerPort timerPort) {
		this(dialogPort, teleportPort, moviePort, spawnPort, aiPort, timerPort, null, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort, QuestAiPort aiPort, QuestTimerPort timerPort,
			QuestStateSyncPort stateSyncPort) {
		this(dialogPort, teleportPort, moviePort, spawnPort, aiPort, timerPort, stateSyncPort, null, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort, QuestAiPort aiPort, QuestTimerPort timerPort,
			QuestStateSyncPort stateSyncPort, QuestStatsPort statsPort) {
		this(dialogPort, teleportPort, moviePort, spawnPort, aiPort, timerPort, stateSyncPort, statsPort, null, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort, QuestAiPort aiPort, QuestTimerPort timerPort,
			QuestStateSyncPort stateSyncPort, QuestStatsPort statsPort, QuestEffectPort effectPort,
			QuestNpcPort npcPort) {
		this(dialogPort, teleportPort, moviePort, spawnPort, aiPort, timerPort, stateSyncPort, statsPort,
			effectPort, npcPort, null);
	}

	public TypedQuestAfterCommitPort(QuestDialogPort dialogPort, QuestTeleportPort teleportPort,
			QuestMoviePort moviePort, QuestSpawnPort spawnPort, QuestAiPort aiPort, QuestTimerPort timerPort,
			QuestStateSyncPort stateSyncPort, QuestStatsPort statsPort, QuestEffectPort effectPort,
			QuestNpcPort npcPort, QuestSystemMessagePort systemMessagePort) {
		this.dialogPort = Objects.requireNonNull(dialogPort, "dialogPort");
		this.teleportPort = teleportPort;
		this.moviePort = moviePort;
		this.spawnPort = spawnPort;
		this.aiPort = aiPort;
		this.timerPort = timerPort;
		this.stateSyncPort = stateSyncPort;
		this.statsPort = statsPort;
		this.effectPort = effectPort;
		this.npcPort = npcPort;
		this.systemMessagePort = systemMessagePort;
	}

	/** 广播 port 由生产侧在 dispatcher 构造后注入，打破 composition 循环。 / The broadcast port is injected after dispatcher construction to break the composition cycle. */
	public void withBroadcastPort(QuestBroadcastPort broadcastPort) {
		this.broadcastPort = Objects.requireNonNull(broadcastPort, "broadcastPort");
	}

	private void requireAiPort() {
		if (aiPort == null) {
			throw new IllegalArgumentException("AI commands require an ai port");
		}
	}

	private void requireTimerPort() {
		if (timerPort == null) {
			throw new IllegalArgumentException("quest timer actions require a timer port");
		}
	}

	@Override
	public void execute(AfterCommitAction action, QuestSnapshot snapshot, QuestMutationPlan plan) {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		switch (action) {
			case AfterCommitAction.CloseDialog closeDialog:
				requireSuccess(dialogPort.closeDialog(snapshot, plan), action, snapshot);
				return;
			case AfterCommitAction.ShowQuestDialog(int dialogId2):
				requireSuccess(dialogPort.showDialog(snapshot, plan, dialogId2), action, snapshot);
				return;
			case AfterCommitAction.ShowQuestSelectionDialog(int dialogId1):
				requireSuccess(dialogPort.showSelectionDialog(snapshot, plan, dialogId1), action, snapshot);
				return;
			case AfterCommitAction.ShowDialogWindow(int dialogId):
				requireSuccess(dialogPort.showDialogWindow(snapshot, plan, dialogId), action, snapshot);
				return;
			case AfterCommitAction.SyncQuestState(
				com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode mode
			):
				if (stateSyncPort == null) {
					throw new IllegalArgumentException("syncQuestState requires a state sync port");
				}
				requireSuccess(stateSyncPort.sync(snapshot, plan, mode), action, snapshot);
				return;
			case AfterCommitAction.RefreshPlayerStats refreshPlayerStats:
				if (statsPort == null) {
					throw new IllegalArgumentException("refreshPlayerStats requires a stats port");
				}
				requireSuccess(statsPort.refresh(snapshot, plan), action, snapshot);
				return;
			case AfterCommitAction.TeleportPlayer(
				com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget instanceTarget, int worldId, float x3,
				float y3, float z3, byte heading
			):
				if (teleportPort == null) {
					throw new IllegalArgumentException("teleportPlayer requires a teleport port");
				}
				requireSuccess(teleportPort.teleportPlayer(snapshot, plan, instanceTarget,
					worldId, x3, y3, z3, heading), action, snapshot);
				return;
			case AfterCommitAction.PlayMovie(
				int movieId, com.aionemu.gameserver.questEngine.definition.QuestMovieType type
			):
				if (moviePort == null) {
					throw new IllegalArgumentException("playMovie requires a movie port");
				}
				requireSuccess(moviePort.playMovie(snapshot, plan, movieId, type), action, snapshot);
				return;
			case AfterCommitAction.PlayMovieRandom(java.util.List<Integer> movieIds):
				if (moviePort == null) {
					throw new IllegalArgumentException("playMovieRandom requires a movie port");
				}
				requireSuccess(moviePort.playMovie(snapshot, plan, movieIds.get(
						java.util.concurrent.ThreadLocalRandom.current().nextInt(movieIds.size()))),
					action, snapshot);
				return;
			case AfterCommitAction.SpawnNpc(
				String slot10, int templateId1,
				com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation location
			):
				if (spawnPort == null) {
					throw new IllegalArgumentException("spawnNpc requires a spawn port");
				}
				requireSuccess(spawnPort.spawnNpc(snapshot, plan, slot10, templateId1,
					location), action, snapshot);
				return;
			case AfterCommitAction.SpawnNpcRandom(
				String slot9, java.util.List<com.aionemu.gameserver.questEngine.definition.QuestSpawnVariant> variants,
				boolean replaceExisting
			):
				if (spawnPort == null) {
					throw new IllegalArgumentException("spawnNpcRandom requires a spawn port");
				}
				requireSuccess(spawnPort.spawnNpcRandom(snapshot, plan, slot9, variants,
					replaceExisting), action, snapshot);
				return;
			case AfterCommitAction.DespawnNpc(String slot8):
				if (spawnPort == null) {
					throw new IllegalArgumentException("despawnNpc requires a spawn port");
				}
				requireSuccess(spawnPort.despawnNpc(snapshot, plan, slot8), action, snapshot);
				return;
			case AfterCommitAction.StartFollow(String slot7):
				requireAiPort();
				requireSuccess(aiPort.startFollow(snapshot, plan, slot7), action, snapshot);
				return;
			case AfterCommitAction.StartFollowCurrentTargetToPoint(float x2, float y2, float z2):
				requireAiPort();
				requireSuccess(aiPort.startFollowCurrentTargetToPoint(snapshot, plan, x2, y2, z2),
					action, snapshot);
				return;
			case AfterCommitAction.StartFollowCurrentTargetToNpc(int npcId):
				requireAiPort();
				requireSuccess(aiPort.startFollowCurrentTargetToNpc(snapshot, plan, npcId), action, snapshot);
				return;
			case AfterCommitAction.StopFollow(String slot6):
				requireAiPort();
				requireSuccess(aiPort.stopFollow(snapshot, plan, slot6), action, snapshot);
				return;
			case AfterCommitAction.AttackTarget(String slot5):
				requireAiPort();
				requireSuccess(aiPort.attackTarget(snapshot, plan, slot5), action, snapshot);
				return;
			case AfterCommitAction.AttackNpcTemplate(String slot4, int templateId):
				requireAiPort();
				requireSuccess(aiPort.attackNpcTemplate(snapshot, plan, slot4, templateId), action, snapshot);
				return;
			case AfterCommitAction.StartWalking(String slot3):
				requireAiPort();
				requireSuccess(aiPort.startWalking(snapshot, plan, slot3), action, snapshot);
				return;
			case AfterCommitAction.BroadcastNpcEmotion(
				String slot2, com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion emotion3
			):
				requireAiPort();
				requireSuccess(aiPort.broadcastEmotion(snapshot, plan, slot2, emotion3), action, snapshot);
				return;
			case AfterCommitAction.BroadcastInteractionNpcEmotion(
				com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion emotion2
			):
				requireAiPort();
				requireSuccess(aiPort.broadcastInteractionEmotion(snapshot, plan, emotion2), action, snapshot);
				return;
			case AfterCommitAction.WatchFollowZone(String slot1, String zone):
				requireAiPort();
				requireSuccess(aiPort.watchFollowZone(snapshot, plan, slot1, zone), action, snapshot);
				return;
			case AfterCommitAction.WatchFollowCoordinate(String slot, float x1, float y1, float z1):
				requireAiPort();
				requireSuccess(aiPort.watchFollowCoordinate(snapshot, plan, slot,
					x1, y1, z1), action, snapshot);
				return;
			case AfterCommitAction.WatchLuredNpcCoordinate(
				float x, float y, float z, float radius,
				com.aionemu.gameserver.questEngine.definition.QuestLureCompletion completion
			):
				requireAiPort();
				requireSuccess(aiPort.watchLuredNpcCoordinate(snapshot, plan,
					x, y, z, radius, completion), action, snapshot);
				return;
			case AfterCommitAction.StartQuestTimer(
				int seconds1, com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy policy1
			):
				requireTimerPort();
				requireSuccess(timerPort.startQuestTimer(snapshot, plan, seconds1, policy1), action, snapshot);
				return;
			case AfterCommitAction.StartInvisibleTimer(
				int seconds, com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy policy
			):
				requireTimerPort();
				requireSuccess(timerPort.startInvisibleTimer(snapshot, plan, seconds, policy), action, snapshot);
				return;
			case AfterCommitAction.CancelQuestTimer cancel:
				requireTimerPort();
				requireSuccess(timerPort.cancelQuestTimer(snapshot, plan, cancel.identity()), action, snapshot);
				return;
			case AfterCommitAction.Morph(int ascensionId):
				if (effectPort == null) {
					throw new IllegalArgumentException("morph requires an effect port");
				}
				requireSuccess(effectPort.morph(snapshot, plan, ascensionId), action, snapshot);
				return;
			case AfterCommitAction.SetPlayerClass(com.aionemu.gameserver.model.PlayerClass playerClass):
				if (effectPort == null) {
					throw new IllegalArgumentException("set-class requires an effect port");
				}
				requireSuccess(effectPort.setPlayerClass(snapshot, plan, playerClass), action, snapshot);
				return;
			case AfterCommitAction.StartNpcFactionQuest(int id):
				if (effectPort == null) {
					throw new IllegalArgumentException("NPC faction quest start requires an effect port");
				}
				requireSuccess(effectPort.startNpcFactionQuest(snapshot, plan, id), action, snapshot);
				return;
			case AfterCommitAction.CompleteNpcFactionQuest(int factionId):
				if (effectPort == null) {
					throw new IllegalArgumentException("NPC faction quest completion requires an effect port");
				}
				requireSuccess(effectPort.completeNpcFactionQuest(snapshot, plan, factionId), action, snapshot);
				return;
			case AfterCommitAction.AbortNpcFactionQuest(int npcFactionId):
				if (effectPort == null) {
					throw new IllegalArgumentException("NPC faction quest abort requires an effect port");
				}
				requireSuccess(effectPort.abortNpcFactionQuest(snapshot, plan, npcFactionId), action, snapshot);
				return;
			case AfterCommitAction.ApplyEffect(int skillId, int durationMillis):
				if (effectPort == null) {
					throw new IllegalArgumentException("applyEffect requires an effect port");
				}
				requireSuccess(effectPort.applyEffect(snapshot, plan, skillId, durationMillis),
					action, snapshot);
				return;
			case AfterCommitAction.RemoveEffect(int effectId):
				if (effectPort == null) {
					throw new IllegalArgumentException("removeEffect requires an effect port");
				}
				requireSuccess(effectPort.removeEffect(snapshot, plan, effectId), action, snapshot);
				return;
			case AfterCommitAction.SendSystemMessage(
				com.aionemu.gameserver.questEngine.definition.QuestSystemMessage message2
			):
				if (systemMessagePort == null) {
					throw new IllegalArgumentException("systemMessage requires a system-message port");
				}
				requireSuccess(systemMessagePort.send(snapshot, plan, message2), action, snapshot);
				return;
			case AfterCommitAction.SendSystemMessagePacket(
				com.aionemu.gameserver.questEngine.definition.QuestSystemMessagePacket message1
			):
				if (systemMessagePort == null) {
					throw new IllegalArgumentException("systemMessage requires a system-message port");
				}
				requireSuccess(systemMessagePort.send(snapshot, plan, message1), action, snapshot);
				return;
			case AfterCommitAction.PlayerEmotion(
				com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion emotion1
			):
				if (effectPort == null) {
					throw new IllegalArgumentException("playerEmotion requires an effect port");
				}
				requireSuccess(effectPort.playerEmotion(snapshot, plan, emotion1), action, snapshot);
				return;
			case AfterCommitAction.FlightTeleport(int flightTeleportId):
				if (effectPort == null) {
					throw new IllegalArgumentException("flightTeleport requires an effect port");
				}
				requireSuccess(effectPort.flightTeleport(snapshot, plan, flightTeleportId), action, snapshot);
				return;
			case AfterCommitAction.DeleteInteractionNpc(boolean scheduleRespawn):
				if (npcPort == null) {
					throw new IllegalArgumentException("deleteInteractionNpc requires an npc port");
				}
				requireSuccess(npcPort.deleteInteractionNpc(snapshot, plan, scheduleRespawn), action, snapshot);
				return;
			case AfterCommitAction.DeleteWorldNpcs deleteWorldNpcs:
				if (npcPort == null) {
					throw new IllegalArgumentException("deleteWorldNpcs requires an npc port");
				}
				requireSuccess(npcPort.deleteWorldNpcs(snapshot, plan), action, snapshot);
				return;
			case AfterCommitAction.AddNpcAggro(int npcTemplateId, int damage):
				if (npcPort == null) {
					throw new IllegalArgumentException("addNpcAggro requires an npc port");
				}
				requireSuccess(npcPort.addNpcAggro(snapshot, plan, npcTemplateId, damage), action, snapshot);
				return;
			case AfterCommitAction.BroadcastZoneMissionEnd(int[] questIds):
				if (broadcastPort == null) {
					throw new IllegalArgumentException("broadcastZoneMissionEnd requires a broadcast port");
				}
				requireSuccess(broadcastPort.broadcastZoneMissionEnd(snapshot, plan, questIds), action, snapshot);
				return;
			case AfterCommitAction.ScheduleEventQuestRefresh refresh:
				if (broadcastPort == null) {
					throw new IllegalArgumentException("scheduleEventQuestRefresh requires a broadcast port");
				}
				requireSuccess(broadcastPort.scheduleEventQuestRefresh(snapshot, plan, refresh.seconds(),
					refresh.questIds()), action, snapshot);
				return;
			default:
				break;
		}
		throw new IllegalArgumentException("unsupported after-commit action: " + action.getClass().getName());
	}

	private static void requireSuccess(boolean success, AfterCommitAction action, QuestSnapshot snapshot) {
		if (!success) {
			throw new QuestAfterCommitException(action, snapshot);
		}
	}
}
