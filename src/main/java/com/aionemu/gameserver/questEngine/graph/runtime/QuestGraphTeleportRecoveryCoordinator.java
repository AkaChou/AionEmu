package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand.InstanceRecoveryMode;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;

/** Reconciles accepted graph teleports before the reconnecting player is spawned. */
public final class QuestGraphTeleportRecoveryCoordinator {

	private final int playerId;
	private final QuestGraphTeleportOutbox.Store store;
	private final Function<Integer, PlayerQuestGraphState> graphStates;
	private final Consumer<TeleportOutboxCommand> positionRestorer;
	private final LongSupplier clock;

	public static void recoverBeforeSpawn(Player player) {
		Player owner = Objects.requireNonNull(player, "player");
		new QuestGraphTeleportRecoveryCoordinator(owner.getObjectId(), QuestGraphTeleportOutbox.productionStore(),
			owner.getQuestGraphStateList()::get, command -> restorePosition(owner, command), System::currentTimeMillis).recover();
	}

	QuestGraphTeleportRecoveryCoordinator(int playerId, QuestGraphTeleportOutbox.Store store,
			Function<Integer, PlayerQuestGraphState> graphStates, Consumer<TeleportOutboxCommand> positionRestorer, LongSupplier clock) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Teleport recovery player id is invalid");
		}
		this.playerId = playerId;
		this.store = Objects.requireNonNull(store, "store");
		this.graphStates = Objects.requireNonNull(graphStates, "graphStates");
		this.positionRestorer = Objects.requireNonNull(positionRestorer, "positionRestorer");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	void recover() {
		List<QuestGraphActionOutboxRecord> records = store.listPendingForPlayer(playerId);
		for (QuestGraphActionOutboxRecord initial : records) {
			TeleportOutboxCommand command = initial.command();
			if (command.playerId() != playerId) {
				throw new IllegalStateException("Teleport recovery row belongs to another player");
			}
			if (initial.deletable()) {
				if (!store.deleteAcked(playerId, command.operationKey()) && currentOrTerminal(command) != null) {
					return;
				}
				continue;
			}
			QuestGraphActionOutboxRecord current = currentOrTerminal(command);
			if (current == null) {
				continue;
			}
			if (current.completedAt() == null) {
				long now = now();
				QuestGraphActionOutboxRecord claimed = store.reclaimForRecovery(playerId, command.operationKey(), now,
					Math.addExact(now, QuestGraphTeleportOutbox.CLAIM_LEASE_MILLIS));
				if (claimed == null) {
					current = currentOrTerminal(command);
					if (current == null) {
						continue;
					}
					if (current.completedAt() == null) {
						throw new IllegalStateException("Teleport recovery could not claim an incomplete command");
					}
				} else {
					positionRestorer.accept(claimed.command());
					boolean completed = store.complete(playerId, command.operationKey(), claimed.claimGeneration(), now);
					current = currentOrTerminal(command);
					if (current == null) {
						continue;
					}
					if (!completed && current.completedAt() == null) {
						throw new IllegalStateException("Teleport recovery completion was not durable");
					}
				}
			}

			current = currentOrTerminal(command);
			if (current == null) {
				continue;
			}
			if (!current.graphAcked() && graphHasAdvanced(command)) {
				if (!store.ackGraph(playerId, command.operationKey())) {
					current = currentOrTerminal(command);
					if (current == null) {
						continue;
					}
					throw new IllegalStateException("Teleport recovery graph acknowledgement failed");
				}
				current = currentOrTerminal(command);
				if (current == null) {
					continue;
				}
			}
			if (current.deletable()) {
				if (!store.deleteAcked(playerId, command.operationKey()) && currentOrTerminal(command) != null) {
					return;
				}
			} else {
				// A completed-but-unacknowledged head must remain ahead of every later physical teleport.
				return;
			}
		}
	}

	private QuestGraphActionOutboxRecord currentOrTerminal(TeleportOutboxCommand command) {
		QuestGraphActionOutboxRecord current = store.find(playerId, command.operationKey());
		if (current != null && !current.command().equals(command)) {
			throw new IllegalStateException("Teleport recovery command changed during reconciliation");
		}
		return current;
	}

	private boolean graphHasAdvanced(TeleportOutboxCommand command) {
		PlayerQuestGraphState state = graphStates.apply(command.questId());
		if (state == null || state.getLifecycle() == PlayerQuestGraphState.Lifecycle.QUARANTINED) {
			return false;
		}
		if (state.getLifecycle() == PlayerQuestGraphState.Lifecycle.PREPARED
				&& state.getJournal().getBaseRevision() == command.baseRevision()
				&& state.getJournal().getTransitionId().equals(command.transitionId())) {
			return state.getJournal().getNextActionIndex() > command.actionIndex();
		}
		long requiredRevision = Math.addExact(Math.addExact(command.baseRevision(), command.actionIndex()), 2);
		return state.getRevision() >= requiredRevision;
	}

	private long now() {
		long value = clock.getAsLong();
		if (value <= 0) {
			throw new IllegalStateException("Teleport recovery clock is invalid");
		}
		return value;
	}

	private static void restorePosition(Player player, TeleportOutboxCommand command) {
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(command.worldId());
		WorldMapInstance instance = resolveRecoveryInstance(command.instanceRecoveryMode(),
			() -> map.getWorldMapInstanceById(command.instanceId()),
			() -> fallbackInstance(player, command, map));
		int recoveredInstanceId = instance.getInstanceId();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, command.worldId(), recoveredInstanceId,
			command.x(), command.y(), command.z(), command.heading());
		if (player.getWorldId() != command.worldId() || player.getInstanceId() != recoveredInstanceId
				|| Float.compare(player.getX(), command.x()) != 0 || Float.compare(player.getY(), command.y()) != 0
				|| Float.compare(player.getZ(), command.z()) != 0 || player.getHeading() != command.heading()) {
			throw new IllegalStateException("Teleport recovery did not apply the frozen target");
		}
	}

	static <T> T resolveRecoveryInstance(InstanceRecoveryMode mode, Supplier<T> frozenLookup, Supplier<T> fallback) {
		Objects.requireNonNull(mode, "instance recovery mode");
		Objects.requireNonNull(frozenLookup, "frozen instance lookup");
		Objects.requireNonNull(fallback, "instance fallback");
		T instance;
		try {
			instance = frozenLookup.get();
		} catch (IllegalArgumentException e) {
			if (mode == InstanceRecoveryMode.EXACT || mode == InstanceRecoveryMode.PLAYER_CURRENT) {
				throw new IllegalStateException("Frozen teleport recovery target instance was rejected", e);
			}
			instance = null;
		}
		if (instance != null) {
			return instance;
		}
		if (mode == InstanceRecoveryMode.EXACT || mode == InstanceRecoveryMode.PLAYER_CURRENT) {
			throw new IllegalStateException("Frozen teleport recovery target instance is unavailable");
		}
		T recovered = fallback.get();
		if (recovered == null) {
			throw new IllegalStateException("Teleport recovery target instance fallback is unavailable");
		}
		return recovered;
	}

	private static WorldMapInstance fallbackInstance(Player player, TeleportOutboxCommand command, WorldMap map) {
		return switch (command.instanceRecoveryMode()) {
			case PLAYER_REGISTERED_OR_CREATE -> InstanceService.getRegisteredOrCreateAndRegister(command.worldId(), player);
			case PLAYER_CURRENT -> throw new IllegalStateException("Player-current teleport recovery must reuse its frozen instance");
			case DEFAULT_INSTANCE -> {
				WorldMapInstance current = player.getWorldId() == command.worldId()
					? lookupInstanceOrNull(map, player.getInstanceId()) : null;
				yield current != null ? current : lookupInstanceOrNull(map, 1);
			}
			case EXACT -> throw new IllegalStateException("Exact teleport recovery must not use a fallback");
		};
	}

	private static WorldMapInstance lookupInstanceOrNull(WorldMap map, int instanceId) {
		int normalizedInstanceId = instanceId == 0 ? 1 : instanceId;
		return map.getInstances().stream()
			.filter(instance -> instance.getInstanceId() == normalizedInstanceId)
			.findFirst()
			.orElse(null);
	}
}
