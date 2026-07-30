package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dao.QuestGraphResourceOperationDAO.ObjectIdReservationConflictException;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SpawnInstanceNpcAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.DialogTargetPlacement;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.FixedPlacement;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.NpcSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.PlacementKind;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.PlayerPlacement;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.PlayerSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.SpawnCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.SpawnRequest;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.SpawnResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.SpawnSpot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInstanceSpawnAdapter.SpotQuery;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

class QuestGraphInstanceSpawnAdapterTest {

	@Test
	void spawnsFromAuthoritativeSpotIdempotently() {
		AtomicInteger spawns = new AtomicInteger();
		AtomicReference<SpawnCommand> command = new AtomicReference<>();
		AtomicReference<SpotQuery> query = new AtomicReference<>();
		QuestGraphInstanceSpawnAdapter adapter = new QuestGraphInstanceSpawnAdapter(7, spotQuery -> {
			query.set(spotQuery);
			return new SpawnSpot(210040000, 1, 10f, 20f, 30f, (byte) 90);
		}, spawnCommand -> {
			command.set(spawnCommand);
			spawns.incrementAndGet();
			return new SpawnResult(ActionResult.APPLIED, 900001);
		}, despawn -> ActionResult.APPLIED);

		ActionInvocation invocation = invocation(7, new SpawnInstanceNpcAction(700759, 216608), "spawn-key");
		assertEquals(PreflightResult.READY, adapter.preflight(invocation));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(1, spawns.get());
		assertEquals(new SpotQuery(700759), query.get());
		assertEquals(700759, command.get().spawnerObjectId());
		assertEquals(216608, command.get().npcId());
		assertEquals(PlacementKind.STATIC_SPAWN, command.get().placement());
		assertEquals(210040000, command.get().worldId());
		assertEquals(1, command.get().instanceId());
		assertEquals(1, adapter.size());
	}

	@Test
	void dialogTargetUsesLiveObjectCoordinatesAndReplaysAfterObjectDisappears() {
		AtomicInteger lookups = new AtomicInteger();
		AtomicInteger spawns = new AtomicInteger();
		AtomicReference<QuestGraphInstanceSpawnAdapter.DialogTargetQuery> query = new AtomicReference<>();
		AtomicReference<SpawnCommand> command = new AtomicReference<>();
		QuestGraphInstanceSpawnAdapter adapter = adapter(
			() -> new PlayerSnapshot(7, 210040000, 3, 1f, 2f, 3f, (byte) 4),
			target -> {
				query.set(target);
				if (lookups.incrementAndGet() > 2) {
					throw new IllegalStateException("dialog object disappeared");
				}
				return new NpcSnapshot(700060, 990060, 210040000, 3, 11f, 12f, 13f, (byte) 14);
			},
			spawn -> {
				command.set(spawn);
				spawns.incrementAndGet();
				return new SpawnResult(ActionResult.APPLIED, 880001);
			});
		ActionInvocation invocation = dialogInvocation(7, 700060, 990060, "dialog-target");
		SpawnRequest request = new SpawnRequest(210634, new DialogTargetPlacement(700060));

		assertEquals(PreflightResult.READY, adapter.preflight(invocation, request));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation, request));
		assertEquals(PreflightResult.READY, adapter.preflight(invocation, request));
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(invocation, request));
		assertEquals(1, spawns.get());
		assertEquals(2, lookups.get());
		assertEquals(990060, query.get().objectId());
		assertEquals(PlacementKind.DIALOG_TARGET, command.get().placement());
		assertEquals(700060, command.get().sourceNpcId());
		assertEquals(990060, command.get().sourceObjectId());
		assertEquals(11f, command.get().x());
		assertEquals(12f, command.get().y());
		assertEquals(13f, command.get().z());
	}

	@Test
	void dialogTargetFailsClosedForMissingOrChangedObjectAuthority() {
		AtomicInteger spawns = new AtomicInteger();
		SpawnRequest request = new SpawnRequest(210634, new DialogTargetPlacement(700060));
		ActionInvocation valid = dialogInvocation(7, 700060, 990060, "valid");
		QuestGraphInstanceSpawnAdapter missing = adapter(player(), target -> null, spawn -> {
			spawns.incrementAndGet();
			return new SpawnResult(ActionResult.APPLIED, 1);
		});
		assertEquals(PreflightResult.FAILED, missing.preflight(valid, request));
		assertEquals(ActionResult.FAILED, missing.execute(valid, request));

		QuestGraphInstanceSpawnAdapter wrongTemplate = adapter(player(),
			target -> new NpcSnapshot(700061, 990060, 210040000, 3, 1, 2, 3, (byte) 0), failedSpawner());
		assertEquals(PreflightResult.FAILED, wrongTemplate.preflight(valid, request));
		QuestGraphInstanceSpawnAdapter wrongWorld = adapter(player(),
			target -> new NpcSnapshot(700060, 990060, 210050000, 3, 1, 2, 3, (byte) 0), failedSpawner());
		assertEquals(ActionResult.FAILED, wrongWorld.execute(valid, request));
		QuestGraphInstanceSpawnAdapter wrongInstance = adapter(player(),
			target -> new NpcSnapshot(700060, 990060, 210040000, 4, 1, 2, 3, (byte) 0), failedSpawner());
		assertEquals(ActionResult.FAILED, wrongInstance.execute(valid, request));
		assertEquals(ActionResult.FAILED,
			missing.execute(new DialogEventInvocation("missing-object", 7, 700060, 0).invocation(), request));
		assertEquals(ActionResult.FAILED,
			missing.execute(dialogInvocation(7, 700061, 990060, "wrong-event-template"), request));
		assertEquals(0, spawns.get());
	}

	@Test
	void playerAndFixedPlacementsRequireExactPlayerWorldInstance() {
		AtomicReference<SpawnCommand> playerCommand = new AtomicReference<>();
		AtomicReference<SpawnCommand> fixedCommand = new AtomicReference<>();
		AtomicInteger objectIds = new AtomicInteger(800000);
		QuestGraphInstanceSpawnAdapter adapter = adapter(player(), target -> null, command -> {
			if (command.placement() == PlacementKind.PLAYER) {
				playerCommand.set(command);
			} else {
				fixedCommand.set(command);
			}
			return new SpawnResult(ActionResult.APPLIED, objectIds.incrementAndGet());
		});
		ActionInvocation invocation = invocation(7, new SpawnInstanceNpcAction(700759, 204830), "player");

		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation, new SpawnRequest(204830, new PlayerPlacement())));
		assertEquals(7, playerCommand.get().sourceObjectId());
		assertEquals(10f, playerCommand.get().x());
		assertEquals(ActionResult.APPLIED, adapter.execute(
			invocation(7, new SpawnInstanceNpcAction(700759, 799339), "fixed"),
			new SpawnRequest(799339, new FixedPlacement(210040000, 3, 21f, 22f, 23f, (byte) 24))));
		assertEquals(PlacementKind.FIXED, fixedCommand.get().placement());
		assertEquals(21f, fixedCommand.get().x());

		assertEquals(PreflightResult.FAILED, adapter.preflight(
			invocation(7, new SpawnInstanceNpcAction(700759, 799339), "wrong-world"),
			new SpawnRequest(799339, new FixedPlacement(600110000, 3, 21, 22, 23, (byte) 0))));
		assertEquals(ActionResult.FAILED, adapter.execute(
			invocation(7, new SpawnInstanceNpcAction(700759, 799339), "wrong-instance"),
			new SpawnRequest(799339, new FixedPlacement(210040000, 1, 21, 22, 23, (byte) 0))));
	}

	@Test
	void replayKeyCannotBeReusedForDifferentNpcOrPlacement() {
		QuestGraphInstanceSpawnAdapter adapter = adapter(player(), target -> null,
			command -> new SpawnResult(ActionResult.APPLIED, 800001));
		ActionInvocation invocation = invocation(7, new SpawnInstanceNpcAction(700759, 204830), "same-key");
		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation, new SpawnRequest(204830, new PlayerPlacement())));
		assertEquals(PreflightResult.FAILED,
			adapter.preflight(invocation, new SpawnRequest(204831, new PlayerPlacement())));
		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation, new SpawnRequest(204830, new FixedPlacement(210040000, 3, 1, 2, 3, (byte) 0))));

		QuestGraphInstanceSpawnAdapter fixed = adapter(player(), target -> null,
			command -> new SpawnResult(ActionResult.APPLIED, 800002));
		ActionInvocation fixedInvocation = invocation(7, new SpawnInstanceNpcAction(700759, 799339), "fixed-key");
		assertEquals(ActionResult.APPLIED, fixed.execute(fixedInvocation,
			new SpawnRequest(799339, new FixedPlacement(210040000, 3, 1, 2, 3, (byte) 4))));
		assertEquals(ActionResult.FAILED, fixed.execute(fixedInvocation,
			new SpawnRequest(799339, new FixedPlacement(210040000, 3, 1, 2, 4, (byte) 4))));

		QuestGraphInstanceSpawnAdapter staticSpawn = new QuestGraphInstanceSpawnAdapter(7,
			query -> new SpawnSpot(210040000, 3, 1, 2, 3, (byte) 4),
			command -> new SpawnResult(ActionResult.APPLIED, 800003), command -> ActionResult.APPLIED);
		ActionInvocation staticInvocation = invocation(7, new SpawnInstanceNpcAction(700759, 216608), "static-key");
		assertEquals(ActionResult.APPLIED, staticSpawn.execute(staticInvocation));
		assertEquals(ActionResult.FAILED, staticSpawn.execute(
			invocation(7, new SpawnInstanceNpcAction(700760, 216608), "static-key")));
	}

	@Test
	void rejectsMissingAuthorityWrongOwnerOrActionAndClears() {
		AtomicInteger spawns = new AtomicInteger();
		QuestGraphInstanceSpawnAdapter missing = new QuestGraphInstanceSpawnAdapter(7, query -> {
			throw new IllegalStateException("missing spot");
		}, command -> new SpawnResult(ActionResult.APPLIED, 900001), despawn -> ActionResult.APPLIED);
		assertEquals(PreflightResult.FAILED,
			missing.preflight(invocation(7, new SpawnInstanceNpcAction(700759, 216608), "missing")));
		assertEquals(ActionResult.FAILED,
			missing.execute(invocation(7, new SpawnInstanceNpcAction(700759, 216608), "missing")));

		QuestGraphInstanceSpawnAdapter adapter = new QuestGraphInstanceSpawnAdapter(7,
			query -> new SpawnSpot(210040000, 0, 1f, 2f, 3f, (byte) 0), command -> {
				spawns.incrementAndGet();
				return new SpawnResult(ActionResult.APPLIED, 900002);
			}, despawn -> ActionResult.APPLIED);
		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation(8, new SpawnInstanceNpcAction(700759, 216608), "wrong-owner")));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, new PlayMovieAction(913), "wrong-action")));
		ActionInvocation valid = invocation(7, new SpawnInstanceNpcAction(700759, 216608), "session");
		assertEquals(ActionResult.APPLIED, adapter.execute(valid));
		assertEquals(ActionResult.APPLIED, adapter.clear());
		assertEquals(0, adapter.size());
		assertEquals(ActionResult.APPLIED, adapter.execute(valid));
		assertEquals(2, spawns.get());
	}

	@Test
	void cleanupUsesSpawnedObjectIdAndRetainsFailures() {
		AtomicReference<QuestGraphInstanceSpawnAdapter.DespawnCommand> cleanup = new AtomicReference<>();
		QuestGraphInstanceSpawnAdapter adapter = new QuestGraphInstanceSpawnAdapter(7,
			query -> new SpawnSpot(210040000, 3, 1f, 2f, 3f, (byte) 0),
			command -> new SpawnResult(ActionResult.APPLIED, 990123), command -> {
				cleanup.set(command);
				return ActionResult.FAILED;
			});
		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation(7, new SpawnInstanceNpcAction(700759, 216608), "cleanup")));

		assertEquals(ActionResult.FAILED, adapter.clear());
		assertEquals(1, adapter.size());
		assertEquals(990123, cleanup.get().objectId());
		assertEquals(216608, cleanup.get().npcId());
		assertEquals(3, cleanup.get().instanceId());
	}

	@Test
	void typedLeaseRehydratesAndCleansAfterAdapterRecreation() {
		AtomicInteger spawns = new AtomicInteger();
		AtomicReference<QuestGraphInstanceSpawnAdapter.DespawnCommand> cleanup = new AtomicReference<>();
		QuestGraphInstanceSpawnAdapter first = adapter(player(), target -> null, command -> {
			spawns.incrementAndGet();
			return new SpawnResult(ActionResult.APPLIED, 990321);
		});
		SpawnRequest request = new SpawnRequest(204830, new PlayerPlacement());
		ActionInvocation invocation = invocation(7, new SpawnInstanceNpcAction(700759, 204830), "durable-spawn");
		CleanupLease plan = first.prepareLease(invocation, request);
		ActionInvocation prepared = withLease(invocation, plan);

		assertEquals(ActionResult.APPLIED, first.execute(prepared, request));
		CleanupLease materialized = first.leaseFor(invocation);
		InstanceSpawnResourceIdentity identity = (InstanceSpawnResourceIdentity) materialized.identity();
		assertEquals(990321, identity.objectId());
		assertEquals(210040000, identity.worldId());
		assertEquals(3, identity.instanceId());

		QuestGraphInstanceSpawnAdapter recovered = new QuestGraphInstanceSpawnAdapter(7, () -> {
			throw new AssertionError("dynamic player authority must not be read during rehydrate");
		}, query -> {
			throw new AssertionError("spot authority must not be read during rehydrate");
		}, target -> {
			throw new AssertionError("dialog authority must not be read during rehydrate");
		}, command -> {
			throw new AssertionError("spawn endpoint must not run for a materialized lease");
		}, command -> {
			cleanup.set(command);
			return ActionResult.APPLIED;
		});
		assertEquals(ActionResult.ALREADY_APPLIED, recovered.execute(withLease(invocation, materialized), request));
		assertEquals(ActionResult.APPLIED, recovered.clear(materialized));
		assertEquals(990321, cleanup.get().objectId());
		assertEquals(204830, cleanup.get().npcId());
		assertEquals(1, spawns.get());
		assertEquals(ActionResult.FAILED,
			recovered.clear(new CleanupLease("INSTANCE_SCOPED_SPAWN", "legacy")));
	}

	@Test
	void durableOperationIdentityPreventsDuplicateSpawnBeforeJournalMaterialization() {
		AtomicReference<CleanupLease> durableRow = new AtomicReference<>();
		QuestGraphResourceOperationRegistry operations = new QuestGraphResourceOperationRegistry(
			(playerId, key) -> durableRow.get(), candidate -> {
				durableRow.compareAndSet(null, candidate);
				return durableRow.get();
			}, expected -> durableRow.compareAndSet(expected, null));
		Set<Integer> worldObjects = new HashSet<>();
		AtomicInteger physicalSpawns = new AtomicInteger();
		AtomicInteger allocations = new AtomicInteger();
		java.util.function.Function<SpawnCommand, SpawnResult> spawner = command -> {
			if (worldObjects.add(command.objectId())) {
				physicalSpawns.incrementAndGet();
				return new SpawnResult(ActionResult.APPLIED, command.objectId());
			}
			return new SpawnResult(ActionResult.ALREADY_APPLIED, command.objectId());
		};
		ActionInvocation invocation = invocation(7, new SpawnInstanceNpcAction(700759, 204830), "durable-window");
		SpawnRequest request = new SpawnRequest(204830, new PlayerPlacement());
		QuestGraphInstanceSpawnAdapter first = durableAdapter(operations, spawner,
			() -> 990321 + allocations.getAndIncrement());
		CleanupLease plan = first.prepareLease(invocation, request);

		assertEquals(ActionResult.APPLIED, first.execute(withLease(invocation, plan), request));
		QuestGraphInstanceSpawnAdapter recreated = durableAdapter(operations, spawner,
			() -> 990321 + allocations.getAndIncrement());
		assertEquals(ActionResult.ALREADY_APPLIED, recreated.execute(withLease(invocation, plan), request));
		assertEquals(1, physicalSpawns.get());
		assertEquals(1, allocations.get());
		assertEquals(990321,
			((InstanceSpawnResourceIdentity) recreated.leaseFor(invocation).identity()).objectId());
	}

	@Test
	void objectIdReservationConflictReleasesOnlyTheFreshLocalAllocation() {
		AtomicInteger releasedId = new AtomicInteger();
		QuestGraphResourceOperationRegistry operations = new QuestGraphResourceOperationRegistry(
			(playerId, key) -> null, candidate -> {
				throw new ObjectIdReservationConflictException(new IllegalStateException("duplicate object id"));
			}, expected -> false);
		QuestGraphInstanceSpawnAdapter adapter = new QuestGraphInstanceSpawnAdapter(7, player(),
			query -> new SpawnSpot(210040000, 3, 0, 0, 0, (byte) 0), target -> null,
			command -> {
				throw new AssertionError("spawn must not run after reservation conflict");
			}, command -> ActionResult.APPLIED, operations, () -> 990654, releasedId::set);
		ActionInvocation invocation = invocation(7, new SpawnInstanceNpcAction(700759, 204830), "object-id-conflict");
		SpawnRequest request = new SpawnRequest(204830, new PlayerPlacement());

		assertEquals(ActionResult.FAILED, adapter.execute(withLease(invocation, adapter.prepareLease(invocation, request)), request));
		assertEquals(990654, releasedId.get());
	}

	private static QuestGraphInstanceSpawnAdapter adapter(java.util.function.Supplier<PlayerSnapshot> player,
			java.util.function.Function<QuestGraphInstanceSpawnAdapter.DialogTargetQuery, NpcSnapshot> dialogTarget,
			java.util.function.Function<SpawnCommand, SpawnResult> spawner) {
		return new QuestGraphInstanceSpawnAdapter(7, player,
			query -> new SpawnSpot(210040000, 3, 0, 0, 0, (byte) 0), dialogTarget, spawner, command -> ActionResult.APPLIED);
	}

	private static QuestGraphInstanceSpawnAdapter durableAdapter(QuestGraphResourceOperationRegistry operations,
			java.util.function.Function<SpawnCommand, SpawnResult> spawner, java.util.function.IntSupplier ids) {
		return new QuestGraphInstanceSpawnAdapter(7, player(),
			query -> new SpawnSpot(210040000, 3, 0, 0, 0, (byte) 0), target -> null, spawner,
			command -> ActionResult.APPLIED, operations, ids, id -> { });
	}

	private static java.util.function.Supplier<PlayerSnapshot> player() {
		return () -> new PlayerSnapshot(7, 210040000, 3, 10f, 20f, 30f, (byte) 40);
	}

	private static java.util.function.Function<SpawnCommand, SpawnResult> failedSpawner() {
		return command -> {
			throw new AssertionError("spawn endpoint must not be called");
		};
	}

	private static ActionInvocation invocation(int playerId,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action, String key) {
		return new ActionInvocation(action, 1, 0, QuestStatus.START,
			new DialogEvent("event", playerId, 1_700_000_000_000L, 700759, 990759, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}

	private static ActionInvocation dialogInvocation(int playerId, int npcId, int npcObjectId, String key) {
		return new ActionInvocation(new SpawnInstanceNpcAction(npcId, 210634), 1, 0, QuestStatus.START,
			new DialogEvent("event", playerId, 1_700_000_000_000L, npcId, npcObjectId, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}

	private static ActionInvocation withLease(ActionInvocation invocation, CleanupLease lease) {
		return new ActionInvocation(invocation.action(), invocation.questId(), invocation.actionIndex(), invocation.questStatus(),
			invocation.event(), invocation.repeatDeadlineResolution(), invocation.itemMutationPlan(),
			Map.of(invocation.idempotencyKey(), lease), invocation.idempotencyKey());
	}

	private record DialogEventInvocation(String key, int playerId, int npcId, int npcObjectId) {
		private ActionInvocation invocation() {
			return dialogInvocation(playerId, npcId, npcObjectId, key);
		}
	}
}
