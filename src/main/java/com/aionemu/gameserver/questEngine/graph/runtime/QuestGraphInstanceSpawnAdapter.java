package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.aionemu.gameserver.dao.QuestGraphResourceOperationDAO.ObjectIdReservationConflictException;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SpawnInstanceNpcAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;
import com.aionemu.gameserver.services.QuestService;

/**
 * Resolves closed NPC placement policies and owns instance-scoped spawn leases.
 */
public final class QuestGraphInstanceSpawnAdapter {

	private final int playerId;
	private final Supplier<PlayerSnapshot> playerAuthority;
	private final Function<SpotQuery, SpawnSpot> spotAuthority;
	private final Function<DialogTargetQuery, NpcSnapshot> dialogTargetAuthority;
	private final Function<SpawnCommand, SpawnResult> spawner;
	private final Function<DespawnCommand, ActionResult> despawner;
	private final QuestGraphResourceOperationRegistry operations;
	private final IntSupplier resourceIds;
	private final IntConsumer unusedResourceIdReleaser;
	private final Map<String, SpawnLease> activeSpawns = new HashMap<>();

	/** Creates an online-player adapter backed by server-authoritative world objects and QuestService. */
	public QuestGraphInstanceSpawnAdapter(Player player, Function<SpotQuery, SpawnSpot> spotAuthority) {
		this(requirePlayer(player).getObjectId(), () -> PlayerSnapshot.from(player), spotAuthority,
			query -> dialogTarget(player, query), command -> spawn(player, command), QuestGraphInstanceSpawnAdapter::despawn,
			QuestGraphResourceOperationRegistry.production(), () -> GameWorldBootstrapServices.idFactory().nextId(),
			id -> GameWorldBootstrapServices.idFactory().releaseId(id));
	}

	/** Compatibility constructor for focused tests of the original static-spawn policy. */
	QuestGraphInstanceSpawnAdapter(int playerId, Function<SpotQuery, SpawnSpot> spotAuthority,
			Function<SpawnCommand, SpawnResult> spawner, Function<DespawnCommand, ActionResult> despawner) {
		this(playerId, () -> null, spotAuthority, query -> null, spawner, despawner);
	}

	/** Creates a focused-test adapter with injectable player, object, spawn, and cleanup authorities. */
	QuestGraphInstanceSpawnAdapter(int playerId, Supplier<PlayerSnapshot> playerAuthority,
			Function<SpotQuery, SpawnSpot> spotAuthority, Function<DialogTargetQuery, NpcSnapshot> dialogTargetAuthority,
			Function<SpawnCommand, SpawnResult> spawner, Function<DespawnCommand, ActionResult> despawner) {
		this(playerId, playerAuthority, spotAuthority, dialogTargetAuthority, spawner, despawner,
			QuestGraphResourceOperationRegistry.passthrough(), () -> 0, id -> { });
	}

	QuestGraphInstanceSpawnAdapter(int playerId, Supplier<PlayerSnapshot> playerAuthority,
			Function<SpotQuery, SpawnSpot> spotAuthority, Function<DialogTargetQuery, NpcSnapshot> dialogTargetAuthority,
			Function<SpawnCommand, SpawnResult> spawner, Function<DespawnCommand, ActionResult> despawner,
			QuestGraphResourceOperationRegistry operations, IntSupplier resourceIds, IntConsumer unusedResourceIdReleaser) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Instance spawn adapter player id is invalid");
		}
		this.playerId = playerId;
		this.playerAuthority = Objects.requireNonNull(playerAuthority, "playerAuthority");
		this.spotAuthority = Objects.requireNonNull(spotAuthority, "spotAuthority");
		this.dialogTargetAuthority = Objects.requireNonNull(dialogTargetAuthority, "dialogTargetAuthority");
		this.spawner = Objects.requireNonNull(spawner, "spawner");
		this.despawner = Objects.requireNonNull(despawner, "despawner");
		this.operations = Objects.requireNonNull(operations, "operations");
		this.resourceIds = Objects.requireNonNull(resourceIds, "resourceIds");
		this.unusedResourceIdReleaser = Objects.requireNonNull(unusedResourceIdReleaser, "unusedResourceIdReleaser");
	}

	/** Preflights the original static-spawn action. */
	public PreflightResult preflight(ActionInvocation invocation) {
		try {
			return preflight(invocation, request(invocation));
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/** Preflights an explicitly lowered placement request. */
	public synchronized PreflightResult preflight(ActionInvocation invocation, SpawnRequest request) {
		try {
			InvocationIdentity identity = identity(invocation, request);
			SpawnLease lease = activeSpawns.get(identity.idempotencyKey());
			if (lease != null) {
				return lease.matches(identity) ? PreflightResult.READY : PreflightResult.FAILED;
			}
			CleanupLease reserved = operations.find(playerId, identity.idempotencyKey());
			if (reserved != null) {
				return matches(requireIdentity(reserved, true), identity) ? PreflightResult.READY : PreflightResult.FAILED;
			}
			command(invocation, request);
			return PreflightResult.READY;
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/** Freezes the complete placement plan before PREPARED persistence. */
	public synchronized CleanupLease prepareLease(ActionInvocation invocation) {
		return prepareLease(invocation, request(invocation));
	}

	/** Freezes an explicitly lowered placement plan before PREPARED persistence. */
	public synchronized CleanupLease prepareLease(ActionInvocation invocation, SpawnRequest request) {
		return CleanupLease.instanceSpawn(resource(command(invocation, request), 0));
	}

	/** Executes the original static-spawn action. */
	public ActionResult execute(ActionInvocation invocation) {
		try {
			return execute(invocation, request(invocation));
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** Executes an explicitly lowered placement request without duplicating an occupied lease. */
	public synchronized ActionResult execute(ActionInvocation invocation, SpawnRequest request) {
		InvocationIdentity identity;
		try {
			identity = identity(invocation, request);
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		SpawnLease existing = activeSpawns.get(identity.idempotencyKey());
		if (existing != null) {
			if (!existing.matches(identity)) {
				return ActionResult.FAILED;
			}
			if (!operations.durable()) {
				return ActionResult.ALREADY_APPLIED;
			}
			try {
				return Objects.requireNonNull(spawner.apply(command(existing.identity())), "spawn result").result();
			} catch (RuntimeException e) {
				return ActionResult.FAILED;
			}
		}
		SpawnCommand command;
		InstanceSpawnResourceIdentity operationIdentity = null;
		try {
			InstanceSpawnResourceIdentity persisted = persistedIdentity(invocation);
			if (operations.durable()) {
				operationIdentity = reserveOperationIdentity(invocation, request, identity, persisted);
				command = command(operationIdentity);
			} else if (persisted != null) {
				if (!matches(persisted, identity)) {
					return ActionResult.FAILED;
				}
				if (persisted.materialized()) {
					activeSpawns.put(identity.idempotencyKey(), lease(persisted));
					return ActionResult.ALREADY_APPLIED;
				}
				command = command(persisted);
			} else {
				command = command(invocation, request);
			}
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		SpawnResult result;
		try {
			result = Objects.requireNonNull(spawner.apply(command), "spawn result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		if (result.result() == ActionResult.APPLIED || result.result() == ActionResult.ALREADY_APPLIED) {
			InstanceSpawnResourceIdentity materialized = operationIdentity == null
				? resource(command, result.objectId()) : operationIdentity;
			if (operationIdentity != null && result.objectId() != operationIdentity.objectId()) {
				return ActionResult.FAILED;
			}
			activeSpawns.put(command.idempotencyKey(), lease(materialized));
		}
		return result.result();
	}

	/** Returns the materialized typed lease produced by a successful execution. */
	public synchronized CleanupLease leaseFor(ActionInvocation invocation) {
		if (invocation == null) {
			return null;
		}
		SpawnLease lease = activeSpawns.get(invocation.idempotencyKey());
		return lease == null ? null : CleanupLease.instanceSpawn(lease.identity());
	}

	/** Rehydrates a materialized lease without consulting dynamic placement authorities. */
	public synchronized ActionResult rehydrate(CleanupLease cleanupLease) {
		try {
			InstanceSpawnResourceIdentity identity = requireIdentity(cleanupLease, true);
			if (operations.durable() && !operations.reserve(cleanupLease).equals(cleanupLease)) {
				return ActionResult.FAILED;
			}
			SpawnLease prior = activeSpawns.putIfAbsent(identity.idempotencyKey(), lease(identity));
			return prior == null ? ActionResult.APPLIED
				: prior.identity().equals(identity) ? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** Cleans one persisted lease directly; unresolved legacy leases fail closed. */
	public synchronized ActionResult clear(CleanupLease cleanupLease) {
		InstanceSpawnResourceIdentity identity;
		try {
			identity = requireIdentity(cleanupLease, true);
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		ActionResult result = cleanup(identity);
		if (result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) {
			if (operations.durable() && !operations.release(cleanupLease)) {
				return ActionResult.FAILED;
			}
			activeSpawns.remove(identity.idempotencyKey());
		}
		return result;
	}

	/** Clears active session leases; failed cleanup remains retryable. */
	public synchronized ActionResult clear() {
		ActionResult outcome = activeSpawns.isEmpty() ? ActionResult.ALREADY_APPLIED : ActionResult.APPLIED;
		var iterator = activeSpawns.entrySet().iterator();
		while (iterator.hasNext()) {
			SpawnLease lease = iterator.next().getValue();
			try {
				ActionResult result = cleanup(lease.identity());
				if (result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) {
					if (operations.durable() && !operations.release(CleanupLease.instanceSpawn(lease.identity()))) {
						outcome = ActionResult.FAILED;
						continue;
					}
					iterator.remove();
				} else {
					outcome = result;
				}
			} catch (RuntimeException ignored) {
				outcome = ActionResult.FAILED;
			}
		}
		return outcome;
	}

	/** Returns the active lease count for deterministic audit. */
	public synchronized int size() {
		return activeSpawns.size();
	}

	private ActionResult cleanup(InstanceSpawnResourceIdentity identity) {
		try {
			return Objects.requireNonNull(despawner.apply(new DespawnCommand(identity.playerId(), identity.objectId(),
				identity.npcId(), identity.worldId(), identity.instanceId(), identity.idempotencyKey())), "despawn result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	private InstanceSpawnResourceIdentity persistedIdentity(ActionInvocation invocation) {
		CleanupLease lease = invocation.cleanupLeases().values().stream()
			.filter(candidate -> invocation.idempotencyKey().equals(candidate.resourceKey()))
			.findFirst().orElse(null);
		return lease == null ? null : requireIdentity(lease, false);
	}

	private InstanceSpawnResourceIdentity requireIdentity(CleanupLease lease, boolean materialized) {
		if (lease == null || !"INSTANCE_SCOPED_SPAWN".equals(lease.capability())
				|| !(lease.identity() instanceof InstanceSpawnResourceIdentity identity)
				|| identity.playerId() != playerId || materialized && !identity.materialized()) {
			throw new IllegalArgumentException("Instance spawn cleanup lease is unresolved or owned by another player");
		}
		return identity;
	}

	private static boolean matches(InstanceSpawnResourceIdentity resource, InvocationIdentity invocation) {
		if (resource.questId() != invocation.questId() || resource.npcId() != invocation.npcId()) {
			return false;
		}
		if (resource.placement() != placementKind(invocation.placement().kind())
				|| !resource.idempotencyKey().equals(invocation.idempotencyKey())) {
			return false;
		}
		return switch (invocation.placement()) {
			case StaticSpawnPlacement placement -> resource.sourceNpcId() == placement.spawnerNpcId();
			case DialogTargetPlacement placement -> resource.sourceNpcId() == placement.npcId();
			case PlayerPlacement ignored -> true;
			case FixedPlacement placement -> resource.worldId() == placement.worldId()
				&& resource.instanceId() == placement.instanceId() && Float.compare(resource.x(), placement.x()) == 0
				&& Float.compare(resource.y(), placement.y()) == 0 && Float.compare(resource.z(), placement.z()) == 0
				&& resource.heading() == placement.heading();
		};
	}

	private static InstanceSpawnResourceIdentity resource(SpawnCommand command, int objectId) {
		return new InstanceSpawnResourceIdentity(command.playerId(), command.questId(), objectId, command.npcId(),
			placementKind(command.placement()), command.sourceNpcId(), command.sourceObjectId(), command.worldId(), command.instanceId(),
			command.x(), command.y(), command.z(), command.heading(), command.idempotencyKey());
	}

	private InstanceSpawnResourceIdentity reserveOperationIdentity(ActionInvocation invocation, SpawnRequest request,
			InvocationIdentity invocationIdentity, InstanceSpawnResourceIdentity persisted) {
		CleanupLease existingLease = operations.find(playerId, invocationIdentity.idempotencyKey());
		if (existingLease != null) {
			InstanceSpawnResourceIdentity existing = requireIdentity(existingLease, true);
			if (!matches(existing, invocationIdentity) || persisted != null && persisted.materialized() && !persisted.equals(existing)) {
				throw new IllegalArgumentException("Reserved instance spawn identity conflicts with the invocation journal");
			}
			return existing;
		}
		SpawnCommand frozen = persisted == null ? command(invocation, request) : command(persisted);
		if (persisted != null && !matches(persisted, invocationIdentity)) {
			throw new IllegalArgumentException("Persisted instance spawn plan conflicts with the invocation");
		}
		int allocatedId = persisted != null && persisted.materialized() ? persisted.objectId() : resourceIds.getAsInt();
		if (allocatedId <= 0) {
			throw new IllegalStateException("Resource operation allocator returned an invalid object id");
		}
		InstanceSpawnResourceIdentity candidate = persisted != null && persisted.materialized()
			? persisted : resource(frozen, allocatedId);
		CleanupLease reservedLease;
		try {
			reservedLease = operations.reserve(CleanupLease.instanceSpawn(candidate));
		} catch (ObjectIdReservationConflictException e) {
			if (persisted == null || !persisted.materialized()) {
				releaseUnusedId(allocatedId, e);
			}
			throw e;
		}
		InstanceSpawnResourceIdentity reserved = requireIdentity(reservedLease, true);
		if (!candidate.equals(reserved) && (persisted == null || !persisted.materialized())) {
			unusedResourceIdReleaser.accept(allocatedId);
		}
		if (!matches(reserved, invocationIdentity)) {
			throw new IllegalArgumentException("Reserved instance spawn identity conflicts with the invocation");
		}
		return reserved;
	}

	private void releaseUnusedId(int objectId, RuntimeException cause) {
		try {
			unusedResourceIdReleaser.accept(objectId);
		} catch (RuntimeException releaseFailure) {
			cause.addSuppressed(releaseFailure);
		}
	}

	private static SpawnCommand command(InstanceSpawnResourceIdentity identity) {
		return new SpawnCommand(identity.questId(), identity.playerId(), identity.objectId(), identity.npcId(),
			PlacementKind.valueOf(identity.placement().name()), identity.sourceNpcId(), identity.sourceObjectId(), identity.worldId(),
			identity.instanceId(), identity.x(), identity.y(), identity.z(), identity.heading(), identity.idempotencyKey());
	}

	private static SpawnPlacementKind placementKind(PlacementKind placement) {
		return SpawnPlacementKind.valueOf(placement.name());
	}

	private static SpawnLease lease(InstanceSpawnResourceIdentity identity) {
		return new SpawnLease(identity);
	}

	private static SpawnRequest request(ActionInvocation invocation) {
		if (invocation == null || !(invocation.action() instanceof SpawnInstanceNpcAction action)) {
			throw new IllegalArgumentException("Instance spawn invocation is invalid");
		}
		return new SpawnRequest(action.npcId(), new StaticSpawnPlacement(action.spawnerObjectId()));
	}

	private InvocationIdentity identity(ActionInvocation invocation, SpawnRequest request) {
		Objects.requireNonNull(request, "spawn request");
		if (invocation == null || invocation.event().playerId() != playerId || invocation.questId() <= 0
				|| !(invocation.action() instanceof SpawnInstanceNpcAction action) || !matches(action, request)
				|| invocation.idempotencyKey() == null || invocation.idempotencyKey().isBlank()) {
			throw new IllegalArgumentException("Instance spawn invocation is invalid");
		}
		return new InvocationIdentity(invocation.questId(), request.npcId(), request.placement(),
			invocation.idempotencyKey());
	}

	private static boolean matches(SpawnInstanceNpcAction action, SpawnRequest request) {
		if (action.npcId() != request.npcId()) {
			return false;
		}
		return switch (request.placement()) {
			case StaticSpawnPlacement placement -> action.spawnerObjectId() == placement.spawnerNpcId();
			case DialogTargetPlacement placement -> action.spawnerObjectId() == placement.npcId();
			case PlayerPlacement ignored -> true;
			case FixedPlacement ignored -> true;
		};
	}

	private SpawnCommand command(ActionInvocation invocation, SpawnRequest request) {
		InvocationIdentity identity = identity(invocation, request);
		ResolvedPlacement resolved = resolve(invocation, request.placement());
		return new SpawnCommand(identity.questId(), playerId, 0, request.npcId(), resolved.kind(), resolved.sourceNpcId(),
			resolved.sourceObjectId(), resolved.spot().worldId(), resolved.spot().instanceId(), resolved.spot().x(),
			resolved.spot().y(), resolved.spot().z(), resolved.spot().heading(), identity.idempotencyKey());
	}

	private ResolvedPlacement resolve(ActionInvocation invocation, SpawnPlacement placement) {
		PlayerSnapshot player = playerSnapshot(placement.kind() != PlacementKind.STATIC_SPAWN);
		return switch (placement) {
			case StaticSpawnPlacement staticSpawn -> {
				SpawnSpot spot = Objects.requireNonNull(spotAuthority.apply(new SpotQuery(staticSpawn.spawnerNpcId())), "spawn spot");
				validateContext(player, spot);
				yield new ResolvedPlacement(PlacementKind.STATIC_SPAWN, staticSpawn.spawnerNpcId(), 0, spot);
			}
			case DialogTargetPlacement dialogTarget -> {
				if (!(invocation.event() instanceof DialogEvent dialog) || dialog.npcObjectId() <= 0
						|| dialog.npcId() != dialogTarget.npcId()) {
					throw new IllegalArgumentException("Dialog-target spawn requires an authoritative dialog object");
				}
				NpcSnapshot target = Objects.requireNonNull(dialogTargetAuthority.apply(
					new DialogTargetQuery(playerId, dialogTarget.npcId(), dialog.npcObjectId(), player.worldId(), player.instanceId())),
					"dialog target");
				if (target.npcId() != dialogTarget.npcId() || target.objectId() != dialog.npcObjectId()
						|| target.worldId() != player.worldId() || target.instanceId() != player.instanceId()) {
					throw new IllegalArgumentException("Dialog target identity or location context changed");
				}
				yield new ResolvedPlacement(PlacementKind.DIALOG_TARGET, target.npcId(), target.objectId(), target.spot());
			}
			case PlayerPlacement ignored -> new ResolvedPlacement(PlacementKind.PLAYER, 0, player.objectId(), player.spot());
			case FixedPlacement fixed -> {
				SpawnSpot spot = fixed.spot();
				validateContext(player, spot);
				yield new ResolvedPlacement(PlacementKind.FIXED, 0, 0, spot);
			}
		};
	}

	private PlayerSnapshot playerSnapshot(boolean required) {
		PlayerSnapshot snapshot = playerAuthority.get();
		if (snapshot == null) {
			if (required) {
				throw new IllegalStateException("Player authority is unavailable for dynamic placement");
			}
			return null;
		}
		if (snapshot.objectId() != playerId) {
			throw new IllegalArgumentException("Player authority owner changed");
		}
		return snapshot;
	}

	private static void validateContext(PlayerSnapshot player, SpawnSpot spot) {
		if (player != null && (spot.worldId() != player.worldId() || spot.instanceId() != player.instanceId())) {
			throw new IllegalArgumentException("Spawn placement is outside the player's world instance");
		}
	}

	private static NpcSnapshot dialogTarget(Player player, DialogTargetQuery query) {
		if (player.getObjectId() != query.playerId() || player.getWorldId() != query.worldId()
				|| player.getInstanceId() != query.instanceId()
				|| !(player.getKnownList().getObject(query.objectId()) instanceof Npc npc)) {
			return null;
		}
		return NpcSnapshot.from(npc);
	}

	private static SpawnResult spawn(Player player, SpawnCommand command) {
		if (player.getObjectId() != command.playerId() || player.getWorldId() != command.worldId()
				|| player.getInstanceId() != command.instanceId() || command.objectId() <= 0) {
			return new SpawnResult(ActionResult.FAILED, 0);
		}
		var visible = GameWorldBootstrapServices.world().findVisibleObject(command.objectId());
		if (visible != null) {
			return visible instanceof Npc npc && npc.isSpawned() && npc.getNpcId() == command.npcId() && npc.getWorldId() == command.worldId()
					&& npc.getInstanceId() == command.instanceId()
				? new SpawnResult(ActionResult.ALREADY_APPLIED, command.objectId()) : new SpawnResult(ActionResult.FAILED, 0);
		}
		var spawned = QuestService.addNewSpawnWithReservedObjectId(command.worldId(), command.instanceId(), command.npcId(), command.x(),
			command.y(), command.z(), command.heading(), command.objectId());
		if (spawned == null) {
			return new SpawnResult(ActionResult.FAILED, 0);
		}
		if (!(spawned instanceof Npc npc) || npc.getNpcId() != command.npcId() || npc.getWorldId() != command.worldId()
				|| npc.getInstanceId() != command.instanceId()) {
			spawned.getController().onDelete();
			return new SpawnResult(ActionResult.FAILED, 0);
		}
		return npc.getObjectId() == command.objectId() && npc.isSpawned()
				&& GameWorldBootstrapServices.world().findVisibleObject(command.objectId()) == npc
			? new SpawnResult(ActionResult.APPLIED, npc.getObjectId()) : new SpawnResult(ActionResult.FAILED, 0);
	}

	private static ActionResult despawn(DespawnCommand command) {
		var object = GameWorldBootstrapServices.world().findVisibleObject(command.objectId());
		if (object == null) {
			return ActionResult.ALREADY_APPLIED;
		}
		if (!(object instanceof Npc npc) || npc.getNpcId() != command.npcId()
				|| npc.getWorldId() != command.worldId() || npc.getInstanceId() != command.instanceId()) {
			return ActionResult.FAILED;
		}
		npc.getController().onDelete();
		return GameWorldBootstrapServices.world().findVisibleObject(command.objectId()) == null
			? ActionResult.APPLIED : ActionResult.FAILED;
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** Closed placement policies accepted by the runtime. */
	public enum PlacementKind {
		STATIC_SPAWN,
		DIALOG_TARGET,
		PLAYER,
		FIXED
	}

	/** Closed placement request contract. */
	public sealed interface SpawnPlacement permits StaticSpawnPlacement, DialogTargetPlacement, PlayerPlacement, FixedPlacement {
		PlacementKind kind();
	}

	/** Uses the first authoritative static spawn spot for an NPC template. */
	public record StaticSpawnPlacement(int spawnerNpcId) implements SpawnPlacement {
		public StaticSpawnPlacement {
			if (spawnerNpcId <= 0) {
				throw new IllegalArgumentException("Static spawn placement is invalid");
			}
		}

		@Override
		public PlacementKind kind() {
			return PlacementKind.STATIC_SPAWN;
		}
	}

	/** Uses the live server-authoritative dialog NPC position. */
	public record DialogTargetPlacement(int npcId) implements SpawnPlacement {
		public DialogTargetPlacement {
			if (npcId <= 0) {
				throw new IllegalArgumentException("Dialog-target placement is invalid");
			}
		}

		@Override
		public PlacementKind kind() {
			return PlacementKind.DIALOG_TARGET;
		}
	}

	/** Uses the live player position. */
	public record PlayerPlacement() implements SpawnPlacement {
		@Override
		public PlacementKind kind() {
			return PlacementKind.PLAYER;
		}
	}

	/** Uses explicit coordinates and requires the player to remain in that world instance. */
	public record FixedPlacement(int worldId, int instanceId, float x, float y, float z, byte heading) implements SpawnPlacement {
		public FixedPlacement {
			new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}

		@Override
		public PlacementKind kind() {
			return PlacementKind.FIXED;
		}

		private SpawnSpot spot() {
			return new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}
	}

	/** A fully lowered spawn request independent of Java handler expressions. */
	public record SpawnRequest(int npcId, SpawnPlacement placement) {
		public SpawnRequest {
			if (npcId <= 0 || placement == null) {
				throw new IllegalArgumentException("Spawn request is invalid");
			}
		}
	}

	/** Queries a static spawn spot by NPC template id. */
	public record SpotQuery(int spawnerObjectId) {
		public SpotQuery {
			if (spawnerObjectId <= 0) {
				throw new IllegalArgumentException("Spot query is invalid");
			}
		}
	}

	/** Binds a dialog object lookup to owner, template, and world-instance context. */
	public record DialogTargetQuery(int playerId, int npcId, int objectId, int worldId, int instanceId) {
		public DialogTargetQuery {
			if (playerId <= 0 || npcId <= 0 || objectId <= 0 || worldId <= 0 || instanceId < 0) {
				throw new IllegalArgumentException("Dialog-target query is invalid");
			}
		}
	}

	/** Immutable world-instance coordinate snapshot. */
	public record SpawnSpot(int worldId, int instanceId, float x, float y, float z, byte heading) {
		public SpawnSpot {
			if (worldId <= 0 || instanceId < 0 || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Spawn spot is invalid");
			}
		}

		public static SpawnSpot from(SpawnSearchResult result, int worldId, int instanceId) {
			Objects.requireNonNull(result, "spawn search result");
			Objects.requireNonNull(result.getSpot(), "spawn spot");
			return new SpawnSpot(worldId, instanceId, result.getSpot().getX(), result.getSpot().getY(),
				result.getSpot().getZ(), result.getSpot().getHeading());
		}
	}

	/** Immutable player authority snapshot. */
	public record PlayerSnapshot(int objectId, int worldId, int instanceId, float x, float y, float z, byte heading) {
		public PlayerSnapshot {
			if (objectId <= 0) {
				throw new IllegalArgumentException("Player snapshot object id is invalid");
			}
			new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}

		public static PlayerSnapshot from(Player player) {
			Objects.requireNonNull(player, "player");
			return new PlayerSnapshot(player.getObjectId(), player.getWorldId(), player.getInstanceId(), player.getX(),
				player.getY(), player.getZ(), player.getHeading());
		}

		private SpawnSpot spot() {
			return new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}
	}

	/** Immutable NPC object authority snapshot. */
	public record NpcSnapshot(int npcId, int objectId, int worldId, int instanceId, float x, float y, float z, byte heading) {
		public NpcSnapshot {
			if (npcId <= 0 || objectId <= 0) {
				throw new IllegalArgumentException("NPC snapshot identity is invalid");
			}
			new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}

		public static NpcSnapshot from(Npc npc) {
			Objects.requireNonNull(npc, "npc");
			return new NpcSnapshot(npc.getNpcId(), npc.getObjectId(), npc.getWorldId(), npc.getInstanceId(), npc.getX(),
				npc.getY(), npc.getZ(), npc.getHeading());
		}

		private SpawnSpot spot() {
			return new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}
	}

	/** Fully resolved spawn endpoint command. */
	public record SpawnCommand(int questId, int playerId, int objectId, int npcId, PlacementKind placement, int sourceNpcId,
			int sourceObjectId, int worldId, int instanceId, float x, float y, float z, byte heading, String idempotencyKey) {
		public SpawnCommand {
			if (questId <= 0 || playerId <= 0 || objectId < 0 || npcId <= 0 || placement == null || sourceNpcId < 0 || sourceObjectId < 0
					|| placement == PlacementKind.STATIC_SPAWN && (sourceNpcId <= 0 || sourceObjectId != 0)
					|| placement == PlacementKind.DIALOG_TARGET && (sourceNpcId <= 0 || sourceObjectId <= 0)
					|| placement == PlacementKind.PLAYER && (sourceNpcId != 0 || sourceObjectId != playerId)
					|| placement == PlacementKind.FIXED && (sourceNpcId != 0 || sourceObjectId != 0)
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Spawn command is invalid");
			}
			new SpawnSpot(worldId, instanceId, x, y, z, heading);
		}

		/** Compatibility alias for the original static-spawner field. */
		public int spawnerObjectId() {
			return sourceNpcId;
		}
	}

	/** Spawn endpoint result; successful results carry the real object id. */
	public record SpawnResult(ActionResult result, int objectId) {
		public SpawnResult {
			Objects.requireNonNull(result, "result");
			if (objectId < 0 || ((result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) && objectId == 0)) {
				throw new IllegalArgumentException("Spawn result object id is invalid");
			}
		}
	}

	/** Cleanup command bound to the spawned object identity and world instance. */
	public record DespawnCommand(int playerId, int objectId, int npcId, int worldId, int instanceId, String idempotencyKey) {
		public DespawnCommand {
			if (playerId <= 0 || objectId <= 0 || npcId <= 0 || worldId <= 0 || instanceId < 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Despawn command is invalid");
			}
		}
	}

	private record InvocationIdentity(int questId, int npcId, SpawnPlacement placement, String idempotencyKey) {
	}

	private record ResolvedPlacement(PlacementKind kind, int sourceNpcId, int sourceObjectId, SpawnSpot spot) {
		private ResolvedPlacement {
			Objects.requireNonNull(kind, "placement kind");
			Objects.requireNonNull(spot, "spawn spot");
		}
	}

	private record SpawnLease(InstanceSpawnResourceIdentity identity) {
		private boolean matches(InvocationIdentity identity) {
			return QuestGraphInstanceSpawnAdapter.matches(this.identity, identity);
		}
	}
}
