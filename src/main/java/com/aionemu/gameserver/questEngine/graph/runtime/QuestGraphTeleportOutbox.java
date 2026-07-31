package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.QuestGraphActionOutboxDAO;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord.Status;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;
import com.aionemu.gameserver.services.teleport.TeleportService2;

import lombok.extern.slf4j.Slf4j;

/** Durable acceptance, lease, delivery, acknowledgement, and GC for graph teleports. */
@Slf4j
final class QuestGraphTeleportOutbox {

	static final long CLAIM_LEASE_MILLIS = 30_000;

	private final int playerId;
	private final Store store;
	private final Scheduler scheduler;
	private final TeleportEndpoint teleporter;
	private final LongSupplier clock;

	QuestGraphTeleportOutbox(Player player) {
		this(requirePlayer(player).getObjectId(), productionStore(), (task, delayMillis) -> {
			if (delayMillis == 0) {
				GameThreadPoolServices.threadPoolManager().execute(task);
			} else {
				GameThreadPoolServices.threadPoolManager().schedule(task, delayMillis);
			}
		},
			(command, authorization, completion) -> TeleportService2.teleportTo(player, command.worldId(), command.instanceId(), command.x(), command.y(),
				command.z(), command.heading(), authorization, completion), System::currentTimeMillis);
	}

	QuestGraphTeleportOutbox(int playerId, Store store, Consumer<Runnable> scheduler, TeleportEndpoint teleporter, LongSupplier clock) {
		this(playerId, store, (task, delayMillis) -> scheduler.accept(task), teleporter, clock);
	}

	QuestGraphTeleportOutbox(int playerId, Store store, Scheduler scheduler, TeleportEndpoint teleporter, LongSupplier clock) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Teleport outbox player id is invalid");
		}
		this.playerId = playerId;
		this.store = Objects.requireNonNull(store, "store");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.teleporter = Objects.requireNonNull(teleporter, "teleporter");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	ActionResult accept(TeleportOutboxCommand command) {
		if (command == null || command.playerId() != playerId) {
			return ActionResult.FAILED;
		}
		try {
			long now = now();
			QuestGraphActionOutboxRecord record = store.acceptExact(command, now);
			if (record.status() == Status.COMPLETED || record.status() == Status.GRAPH_ACKED) {
				deleteIfTerminal(record);
				return ActionResult.ALREADY_APPLIED;
			}
			if (record.status() == Status.ACCEPTED || record.leaseUntil() != null && record.leaseUntil() <= now) {
				try {
					scheduler.schedule(() -> deliver(command.operationKey()), 0);
				} catch (RuntimeException e) {
					logFailure(command, e);
				}
			}
			return ActionResult.DURABLY_ACCEPTED;
		} catch (RuntimeException e) {
			logFailure(command, e);
			return ActionResult.FAILED;
		}
	}

	ActionResult acknowledgeGraph(int questId, long baseRevision, String transitionId, int actionIndex, String operationKey) {
		try {
			QuestGraphActionOutboxRecord record = store.find(playerId, operationKey);
			if (record == null || !matchesGraphIdentity(record.command(), questId, baseRevision, transitionId, actionIndex, operationKey)) {
				return ActionResult.FAILED;
			}
			if (!store.ackGraph(playerId, operationKey)) {
				return ActionResult.FAILED;
			}
			QuestGraphActionOutboxRecord acknowledged = store.find(playerId, operationKey);
			if (acknowledged == null || !acknowledged.graphAcked()) {
				return ActionResult.FAILED;
			}
			deleteIfTerminal(acknowledged);
			return ActionResult.ALREADY_APPLIED;
		} catch (RuntimeException e) {
			logFailure(questId, operationKey, e);
			return ActionResult.FAILED;
		}
	}

	private void deliver(String operationKey) {
		QuestGraphActionOutboxRecord claimed = null;
		try {
			long now = now();
			claimed = store.claim(playerId, operationKey, now, Math.addExact(now, CLAIM_LEASE_MILLIS));
			if (claimed == null) {
				return;
			}
			QuestGraphActionOutboxRecord delivery = claimed;
			scheduleAfterLease(delivery);
			teleporter.start(delivery.command(), () -> isCurrentClaim(delivery), () -> complete(delivery));
		} catch (RuntimeException e) {
			if (claimed == null) {
				logFailure(0, operationKey, e);
			} else {
				logFailure(claimed.command(), e);
			}
		}
	}

	private void scheduleAfterLease(QuestGraphActionOutboxRecord claimed) {
		try {
			long remaining = Math.subtractExact(claimed.leaseUntil(), now());
			long delayMillis = Math.min(CLAIM_LEASE_MILLIS, Math.max(0, remaining));
			scheduler.schedule(() -> deliver(claimed.command().operationKey()), delayMillis);
		} catch (RuntimeException e) {
			logFailure(claimed.command(), e);
		}
	}

	private void complete(QuestGraphActionOutboxRecord claimed) {
		try {
			TeleportOutboxCommand command = claimed.command();
			if (!store.complete(playerId, command.operationKey(), claimed.claimGeneration(), now())) {
				return;
			}
			QuestGraphActionOutboxRecord completed = store.find(playerId, command.operationKey());
			if (completed == null || completed.completedAt() == null) {
				throw new IllegalStateException("Teleport outbox completion did not materialize");
			}
			deleteIfTerminal(completed);
		} catch (RuntimeException e) {
			logFailure(claimed.command(), e);
		}
	}

	private boolean isCurrentClaim(QuestGraphActionOutboxRecord claimed) {
		return store.isCurrentClaim(playerId, claimed.command().operationKey(), claimed.claimGeneration(), now());
	}

	private void deleteIfTerminal(QuestGraphActionOutboxRecord record) {
		if (record.deletable() && store.deleteAcked(playerId, record.command().operationKey())) {
			scheduleNextHead();
		}
	}

	private void scheduleNextHead() {
		for (QuestGraphActionOutboxRecord candidate : store.listPendingForPlayer(playerId)) {
			if (candidate.deletable()) {
				if (store.deleteAcked(playerId, candidate.command().operationKey())
						|| store.find(playerId, candidate.command().operationKey()) == null) {
					continue;
				}
				return;
			}
			if (candidate.status() == Status.ACCEPTED
					|| candidate.status() == Status.CLAIMED && candidate.leaseUntil() <= now()) {
				scheduler.schedule(() -> deliver(candidate.command().operationKey()), 0);
			}
			return;
		}
	}

	private long now() {
		long value = clock.getAsLong();
		if (value <= 0) {
			throw new IllegalStateException("Teleport outbox clock is invalid");
		}
		return value;
	}

	private static boolean matchesGraphIdentity(TeleportOutboxCommand command, int questId, long baseRevision,
			String transitionId, int actionIndex, String operationKey) {
		return command.questId() == questId && command.baseRevision() == baseRevision && command.actionIndex() == actionIndex
			&& command.transitionId().equals(transitionId) && command.operationKey().equals(operationKey);
	}

	static Store productionStore() {
		return new Store() {
			private QuestGraphActionOutboxDAO dao() {
				return DAOManager.getDAO(QuestGraphActionOutboxDAO.class);
			}

			@Override
			public QuestGraphActionOutboxRecord acceptExact(TeleportOutboxCommand command, long acceptedAt) {
				return dao().acceptExact(command, acceptedAt);
			}

			@Override
			public QuestGraphActionOutboxRecord find(int ownerId, String operationKey) {
				return dao().find(ownerId, operationKey);
			}

			@Override
			public QuestGraphActionOutboxRecord claim(int ownerId, String operationKey, long now, long leaseUntil) {
				return dao().claim(ownerId, operationKey, now, leaseUntil);
			}

			@Override
			public QuestGraphActionOutboxRecord reclaimForRecovery(int ownerId, String operationKey, long now, long leaseUntil) {
				return dao().reclaimForRecovery(ownerId, operationKey, now, leaseUntil);
			}

			@Override
			public boolean complete(int ownerId, String operationKey, long generation, long completedAt) {
				return dao().complete(ownerId, operationKey, generation, completedAt);
			}

			@Override
			public boolean isCurrentClaim(int ownerId, String operationKey, long generation, long now) {
				return dao().isCurrentClaim(ownerId, operationKey, generation, now);
			}

			@Override
			public boolean ackGraph(int ownerId, String operationKey) {
				return dao().ackGraph(ownerId, operationKey);
			}

			@Override
			public List<QuestGraphActionOutboxRecord> listPendingForPlayer(int ownerId) {
				return dao().listPendingForPlayer(ownerId);
			}

			@Override
			public boolean deleteAcked(int ownerId, String operationKey) {
				return dao().deleteAcked(ownerId, operationKey);
			}
		};
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	private static void logFailure(TeleportOutboxCommand command, RuntimeException failure) {
		log.error(I18n.get("log.quest_graph_teleport_failed", command.questId(), command.playerId(), command.worldId(),
			"DURABLE_OUTBOX", command.operationKey()), failure);
	}

	private static void logFailure(int questId, String operationKey, RuntimeException failure) {
		log.error(I18n.get("log.quest_graph_teleport_failed", questId, 0, 0, "DURABLE_OUTBOX", operationKey), failure);
	}

	interface Store {
		QuestGraphActionOutboxRecord acceptExact(TeleportOutboxCommand command, long acceptedAt);

		QuestGraphActionOutboxRecord find(int playerId, String operationKey);

		QuestGraphActionOutboxRecord claim(int playerId, String operationKey, long now, long leaseUntil);

		QuestGraphActionOutboxRecord reclaimForRecovery(int playerId, String operationKey, long now, long leaseUntil);

		boolean complete(int playerId, String operationKey, long generation, long completedAt);

		boolean isCurrentClaim(int playerId, String operationKey, long generation, long now);

		boolean ackGraph(int playerId, String operationKey);

		List<QuestGraphActionOutboxRecord> listPendingForPlayer(int playerId);

		boolean deleteAcked(int playerId, String operationKey);
	}

	@FunctionalInterface
	interface TeleportEndpoint {
		boolean start(TeleportOutboxCommand command, BooleanSupplier authorization, Runnable completion);
	}

	@FunctionalInterface
	interface Scheduler {
		/** Schedules one delivery attempt after a bounded delay. */
		void schedule(Runnable task, long delayMillis);
	}
}
