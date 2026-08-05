package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;

import java.util.Objects;

/** Routes the closed after-commit action set to typed protocol ports. */
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

	/** 广播 port 由生产侧在 dispatcher 构造后注入, 打破 composition 循环。 */
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
		if (action instanceof AfterCommitAction.CloseDialog) {
			requireSuccess(dialogPort.closeDialog(snapshot, plan), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.ShowQuestDialog show) {
			requireSuccess(dialogPort.showDialog(snapshot, plan, show.dialogId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.ShowQuestSelectionDialog show) {
			requireSuccess(dialogPort.showSelectionDialog(snapshot, plan, show.dialogId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.ShowDialogWindow show) {
			requireSuccess(dialogPort.showDialogWindow(snapshot, plan, show.dialogId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.SyncQuestState sync) {
			if (stateSyncPort == null) {
				throw new IllegalArgumentException("syncQuestState requires a state sync port");
			}
			requireSuccess(stateSyncPort.sync(snapshot, plan, sync.mode()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.RefreshPlayerStats) {
			if (statsPort == null) {
				throw new IllegalArgumentException("refreshPlayerStats requires a stats port");
			}
			requireSuccess(statsPort.refresh(snapshot, plan), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.TeleportPlayer teleport) {
			if (teleportPort == null) {
				throw new IllegalArgumentException("teleportPlayer requires a teleport port");
			}
			requireSuccess(teleportPort.teleportPlayer(snapshot, plan, teleport.instanceTarget(),
				teleport.worldId(), teleport.x(), teleport.y(), teleport.z(), teleport.heading()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.PlayMovie movie) {
			if (moviePort == null) {
				throw new IllegalArgumentException("playMovie requires a movie port");
			}
			requireSuccess(moviePort.playMovie(snapshot, plan, movie.movieId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.SpawnNpc spawnAction) {
			if (spawnPort == null) {
				throw new IllegalArgumentException("spawnNpc requires a spawn port");
			}
			requireSuccess(spawnPort.spawnNpc(snapshot, plan, spawnAction.slot(), spawnAction.templateId(),
				spawnAction.location()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.DespawnNpc despawnAction) {
			if (spawnPort == null) {
				throw new IllegalArgumentException("despawnNpc requires a spawn port");
			}
			requireSuccess(spawnPort.despawnNpc(snapshot, plan, despawnAction.slot()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.StartFollow follow) {
			requireAiPort();
			requireSuccess(aiPort.startFollow(snapshot, plan, follow.slot()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.StartFollowCurrentTargetToPoint follow) {
			requireAiPort();
			requireSuccess(aiPort.startFollowCurrentTargetToPoint(snapshot, plan, follow.x(), follow.y(), follow.z()),
				action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.StopFollow stop) {
			requireAiPort();
			requireSuccess(aiPort.stopFollow(snapshot, plan, stop.slot()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.AttackTarget attack) {
			requireAiPort();
			requireSuccess(aiPort.attackTarget(snapshot, plan, attack.slot()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.StartWalking walking) {
			requireAiPort();
			requireSuccess(aiPort.startWalking(snapshot, plan, walking.slot()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.BroadcastNpcEmotion emotion) {
			requireAiPort();
			requireSuccess(aiPort.broadcastEmotion(snapshot, plan, emotion.slot(), emotion.emotion()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.WatchFollowZone followZone) {
			requireAiPort();
			requireSuccess(aiPort.watchFollowZone(snapshot, plan, followZone.slot(), followZone.zone()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.StartQuestTimer timer) {
			requireTimerPort();
			requireSuccess(timerPort.startQuestTimer(snapshot, plan, timer.seconds(), timer.policy()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.StartInvisibleTimer timer) {
			requireTimerPort();
			requireSuccess(timerPort.startInvisibleTimer(snapshot, plan, timer.seconds(), timer.policy()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.CancelQuestTimer) {
			requireTimerPort();
			AfterCommitAction.CancelQuestTimer cancel = (AfterCommitAction.CancelQuestTimer) action;
			requireSuccess(timerPort.cancelQuestTimer(snapshot, plan, cancel.identity()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.Morph morph) {
			if (effectPort == null) {
				throw new IllegalArgumentException("morph requires an effect port");
			}
			requireSuccess(effectPort.morph(snapshot, plan, morph.ascensionId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.ApplyEffect apply) {
			if (effectPort == null) {
				throw new IllegalArgumentException("applyEffect requires an effect port");
			}
			requireSuccess(effectPort.applyEffect(snapshot, plan, apply.skillId(), apply.durationMillis()),
				action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.RemoveEffect remove) {
			if (effectPort == null) {
				throw new IllegalArgumentException("removeEffect requires an effect port");
			}
			requireSuccess(effectPort.removeEffect(snapshot, plan, remove.effectId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.SendSystemMessage message) {
			if (systemMessagePort == null) {
				throw new IllegalArgumentException("systemMessage requires a system-message port");
			}
			requireSuccess(systemMessagePort.send(snapshot, plan, message.message()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.SendSystemMessagePacket message) {
			if (systemMessagePort == null) {
				throw new IllegalArgumentException("systemMessage requires a system-message port");
			}
			requireSuccess(systemMessagePort.send(snapshot, plan, message.message()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.PlayerEmotion emotion) {
			if (effectPort == null) {
				throw new IllegalArgumentException("playerEmotion requires an effect port");
			}
			requireSuccess(effectPort.playerEmotion(snapshot, plan, emotion.emotion()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.FlightTeleport flight) {
			if (effectPort == null) {
				throw new IllegalArgumentException("flightTeleport requires an effect port");
			}
			requireSuccess(effectPort.flightTeleport(snapshot, plan, flight.flightTeleportId()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.DeleteInteractionNpc delete) {
			if (npcPort == null) {
				throw new IllegalArgumentException("deleteInteractionNpc requires an npc port");
			}
			requireSuccess(npcPort.deleteInteractionNpc(snapshot, plan, delete.scheduleRespawn()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.AddNpcAggro aggro) {
			if (npcPort == null) {
				throw new IllegalArgumentException("addNpcAggro requires an npc port");
			}
			requireSuccess(npcPort.addNpcAggro(snapshot, plan, aggro.npcTemplateId(), aggro.damage()), action, snapshot);
			return;
		}
		if (action instanceof AfterCommitAction.BroadcastZoneMissionEnd broadcast) {
			if (broadcastPort == null) {
				throw new IllegalArgumentException("broadcastZoneMissionEnd requires a broadcast port");
			}
			requireSuccess(broadcastPort.broadcastZoneMissionEnd(snapshot, plan, broadcast.questIds()), action, snapshot);
			return;
		}
		throw new IllegalArgumentException("unsupported after-commit action: " + action.getClass().getName());
	}

	private static void requireSuccess(boolean success, AfterCommitAction action, QuestSnapshot snapshot) {
		if (!success) {
			throw new QuestAfterCommitException(action, snapshot);
		}
	}
}
