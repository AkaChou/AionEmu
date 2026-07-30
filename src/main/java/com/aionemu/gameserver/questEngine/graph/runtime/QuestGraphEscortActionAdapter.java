package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dao.QuestGraphResourceOperationDAO.ObjectIdReservationConflictException;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortNpcDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortZoneDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortLostTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortReachedTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.NpcSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.PlayerSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.task.QuestEscortCompletionListener;
import com.aionemu.gameserver.questEngine.task.QuestTasks;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

/** 以 player/quest lease 原子拥有 escort follower、AI 与到达检查任务。 / Atomically owns escort follower, AI, and arrival task by player/quest lease. */
public final class QuestGraphEscortActionAdapter {

	private static final EscortSessionRegistry PRODUCTION_SESSIONS = new EscortSessionRegistry();
	private final int playerId;
	private final Supplier<PlayerContext> playerContext;
	private final Function<StartCommand, PreflightResult> preflight;
	private final Function<StartCommand, StartResult> starter;
	private final Function<CleanupCommand, ActionResult> cleaner;
	private final Function<QuestGraphEvent, DispatchResult> terminalDispatcher;
	private final BiPredicate<Integer, String> leaseReleased;
	private final Function<EscortResourceIdentity, ActionResult> rehydrateValidator;
	private final EscortSessionRegistry sessions;
	private final QuestGraphResourceOperationRegistry operations;
	private final IntSupplier resourceIds;
	private final IntConsumer unusedResourceIdReleaser;
	private final Function<StartCommand, FollowerReservation> followerReservation;

	/**
	 * 创建连接正式玩家、世界、AI、协议、QuestTasks 与 typed terminal dispatcher 的 adapter。
	 * Connects production player, world, AI, protocol, QuestTasks, and the typed terminal dispatcher.
	 */
	public QuestGraphEscortActionAdapter(Player player, Function<QuestGraphEvent, DispatchResult> terminalDispatcher) {
		Player owner = requirePlayer(player);
		this.playerId = owner.getObjectId();
		this.playerContext = () -> PlayerContext.from(owner);
		this.preflight = command -> preflight(owner, command);
		this.starter = command -> start(owner, command, completionListener(command));
		this.cleaner = command -> cleanup(owner, command);
		this.terminalDispatcher = Objects.requireNonNull(terminalDispatcher, "escort terminal dispatcher");
		this.leaseReleased = (questId, resourceKey) -> leaseReleased(owner, questId, resourceKey);
		this.rehydrateValidator = identity -> validateLiveEscort(owner, identity);
		this.sessions = PRODUCTION_SESSIONS;
		this.operations = QuestGraphResourceOperationRegistry.production();
		this.resourceIds = () -> GameWorldBootstrapServices.idFactory().nextId();
		this.unusedResourceIdReleaser = id -> GameWorldBootstrapServices.idFactory().releaseId(id);
		this.followerReservation = command -> reserveFollower(owner, command, resourceIds);
	}

	/** 创建可注入端点的聚焦测试 adapter。 / Creates a focused-test adapter with injectable endpoints. */
	QuestGraphEscortActionAdapter(int playerId, Function<StartCommand, PreflightResult> preflight,
			Function<StartCommand, StartResult> starter, Function<CleanupCommand, ActionResult> cleaner) {
		this(playerId, () -> null, preflight, starter, cleaner, event -> {
			throw new IllegalStateException("Escort terminal dispatcher is not configured for this endpoint test");
		}, (questId, resourceKey) -> false, identity -> ActionResult.APPLIED);
	}

	QuestGraphEscortActionAdapter(int playerId, Supplier<PlayerContext> playerContext, Function<StartCommand, PreflightResult> preflight,
			Function<StartCommand, StartResult> starter, Function<CleanupCommand, ActionResult> cleaner) {
		this(playerId, playerContext, preflight, starter, cleaner, event -> {
			throw new IllegalStateException("Escort terminal dispatcher is not configured for this endpoint test");
		}, (questId, resourceKey) -> false, identity -> ActionResult.APPLIED);
	}

	QuestGraphEscortActionAdapter(int playerId, Supplier<PlayerContext> playerContext, Function<StartCommand, PreflightResult> preflight,
			Function<StartCommand, StartResult> starter, Function<CleanupCommand, ActionResult> cleaner,
			Function<QuestGraphEvent, DispatchResult> terminalDispatcher, BiPredicate<Integer, String> leaseReleased,
			Function<EscortResourceIdentity, ActionResult> rehydrateValidator) {
		this(playerId, playerContext, preflight, starter, cleaner, terminalDispatcher, leaseReleased, rehydrateValidator,
			new EscortSessionRegistry());
	}

	QuestGraphEscortActionAdapter(int playerId, Supplier<PlayerContext> playerContext, Function<StartCommand, PreflightResult> preflight,
			Function<StartCommand, StartResult> starter, Function<CleanupCommand, ActionResult> cleaner,
			Function<QuestGraphEvent, DispatchResult> terminalDispatcher, BiPredicate<Integer, String> leaseReleased,
			Function<EscortResourceIdentity, ActionResult> rehydrateValidator, EscortSessionRegistry sessions) {
		this(playerId, playerContext, preflight, starter, cleaner, terminalDispatcher, leaseReleased, rehydrateValidator,
			sessions, QuestGraphResourceOperationRegistry.passthrough(), () -> 0, id -> { },
			command -> new FollowerReservation(0, null, false));
	}

	QuestGraphEscortActionAdapter(int playerId, Supplier<PlayerContext> playerContext, Function<StartCommand, PreflightResult> preflight,
			Function<StartCommand, StartResult> starter, Function<CleanupCommand, ActionResult> cleaner,
			Function<QuestGraphEvent, DispatchResult> terminalDispatcher, BiPredicate<Integer, String> leaseReleased,
			Function<EscortResourceIdentity, ActionResult> rehydrateValidator, EscortSessionRegistry sessions,
			QuestGraphResourceOperationRegistry operations, IntSupplier resourceIds, IntConsumer unusedResourceIdReleaser,
			Function<StartCommand, FollowerReservation> followerReservation) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Escort adapter player id is invalid");
		}
		this.playerId = playerId;
		this.playerContext = Objects.requireNonNull(playerContext, "playerContext");
		this.preflight = Objects.requireNonNull(preflight, "preflight");
		this.starter = Objects.requireNonNull(starter, "starter");
		this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
		this.terminalDispatcher = Objects.requireNonNull(terminalDispatcher, "terminalDispatcher");
		this.leaseReleased = Objects.requireNonNull(leaseReleased, "leaseReleased");
		this.rehydrateValidator = Objects.requireNonNull(rehydrateValidator, "rehydrateValidator");
		this.sessions = Objects.requireNonNull(sessions, "sessions");
		this.operations = Objects.requireNonNull(operations, "operations");
		this.resourceIds = Objects.requireNonNull(resourceIds, "resourceIds");
		this.unusedResourceIdReleaser = Objects.requireNonNull(unusedResourceIdReleaser, "unusedResourceIdReleaser");
		this.followerReservation = Objects.requireNonNull(followerReservation, "followerReservation");
	}

	/** 在 PREPARED 前验证 event NPC、目的地与单一 QUEST_FOLLOW owner。 / Validates event NPC, destination, and the single QUEST_FOLLOW owner before PREPARED. */
	public synchronized PreflightResult preflight(ActionInvocation invocation) {
		StartCommand command;
		try {
			EscortResourceIdentity persisted = persistedIdentity(invocation);
			if (operations.durable()) {
				CleanupLease reserved = operations.find(playerId, invocation.idempotencyKey());
				if (reserved != null) {
					EscortResourceIdentity operation = requireIdentity(reserved, true);
					if (persisted != null && persisted.materialized() && !persisted.equals(operation)
							|| !matches(invocation, operation)) {
						return PreflightResult.FAILED;
					}
					persisted = operation;
				}
			}
			command = persisted == null ? command(invocation) : command(persisted);
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
		EscortLease active = sessions.get(playerId, command.questId());
		if (active != null) {
			return active.materialized() && active.matches(command) && validatesLive(active)
				? PreflightResult.READY : PreflightResult.FAILED;
		}
		if (sessions.hasAny(playerId)) {
			return PreflightResult.FAILED;
		}
		try {
			return Objects.requireNonNull(preflight.apply(command), "escort preflight result");
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/** 冻结完整 escort 恢复计划，必须在 PREPARED 持久化前调用。 / Freezes the complete escort recovery plan before PREPARED persistence. */
	public synchronized CleanupLease prepareLease(ActionInvocation invocation) {
		StartCommand command = command(invocation);
		if (command.worldId() <= 0) {
			throw new IllegalStateException("Escort player context is unavailable for a durable plan");
		}
		return CleanupLease.escort(plan(command));
	}

	/** 启动 escort；同 owner/key 幂等，不同 key 或不同 quest 显式冲突。 / Starts the escort idempotently for the same owner/key and rejects conflicts. */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		StartCommand command;
		EscortResourceIdentity persisted;
		boolean durableRecovery = false;
		try {
			persisted = persistedIdentity(invocation);
			command = persisted == null ? command(invocation) : command(persisted);
			if (operations.durable()) {
				CleanupLease existingOperation = operations.find(playerId, invocation.idempotencyKey());
				durableRecovery = existingOperation != null;
				persisted = reserveOperationIdentity(invocation, command, persisted, existingOperation);
				command = command(persisted);
			}
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		EscortLease active = sessions.get(playerId, command.questId());
		if (active != null) {
			return active.materialized() && active.matches(command) && validatesLive(active)
				? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
		}
		if (sessions.hasAny(playerId)) {
			return ActionResult.FAILED;
		}
		if (persisted != null && persisted.materialized() && !operations.durable()) {
			ActionResult restored = rehydrate(CleanupLease.escort(persisted));
			return restored == ActionResult.APPLIED || restored == ActionResult.ALREADY_APPLIED
				? ActionResult.ALREADY_APPLIED : restored;
		}
		EscortLease reservation = persisted == null || !persisted.materialized() ? EscortLease.planned(command) : EscortLease.typed(persisted);
		if (!sessions.reserve(playerId, command.questId(), reservation)) {
			return ActionResult.FAILED;
		}
		StartResult result;
		try {
			result = Objects.requireNonNull(starter.apply(command), "escort start result");
		} catch (RuntimeException e) {
			sessions.remove(playerId, command.questId(), reservation);
			return ActionResult.FAILED;
		}
		if (result.result() == ActionResult.APPLIED || result.result() == ActionResult.ALREADY_APPLIED) {
			if (persisted != null && persisted.materialized() && (result.followerObjectId() != persisted.objectId()
					|| result.spawnedFollower() != persisted.spawnedFollower()
					|| !Objects.equals(result.previousWalkerId(), persisted.previousWalkerId()))) {
				sessions.remove(playerId, command.questId(), reservation);
				cleanup(EscortLease.typed(persisted.materialize(result.followerObjectId(), result.previousWalkerId())),
					CleanupReason.MANUAL);
				return ActionResult.FAILED;
			}
			EscortLease created = persisted == null
				? EscortLease.legacy(command, result)
				: EscortLease.typed(persisted.materialize(result.followerObjectId(), result.previousWalkerId()));
			if (!sessions.replace(playerId, command.questId(), reservation, created)) {
				cleanup(created, CleanupReason.MANUAL);
				return ActionResult.FAILED;
			}
		} else {
			sessions.remove(playerId, command.questId(), reservation);
		}
		return durableRecovery && (result.result() == ActionResult.APPLIED || result.result() == ActionResult.ALREADY_APPLIED)
			? ActionResult.ALREADY_APPLIED : result.result();
	}

	/** 返回成功执行产生的已物化 typed lease。 / Returns the materialized typed lease produced by successful execution. */
	public synchronized CleanupLease leaseFor(ActionInvocation invocation) {
		EscortLease lease = invocation == null ? null : sessions.get(playerId, invocation.questId());
		return lease == null || !lease.materialized() || lease.identity() == null
				|| !lease.idempotencyKey().equals(invocation.idempotencyKey())
			? null : CleanupLease.escort(lease.identity());
	}

	/** 从持久化身份严格验证世界 follower 与 task 后恢复进程内索引。 / Rehydrates the in-process index after strict world-follower and task validation. */
	public synchronized ActionResult rehydrate(CleanupLease cleanupLease) {
		try {
			EscortResourceIdentity identity = requireIdentity(cleanupLease, true);
			if (operations.durable() && !operations.reserve(cleanupLease).equals(cleanupLease)) {
				return ActionResult.FAILED;
			}
			EscortLease current = sessions.get(playerId, identity.questId());
			if (current != null) {
				return identity.equals(current.identity()) ? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
			}
			if (sessions.hasAny(playerId)) {
				return ActionResult.FAILED;
			}
			ActionResult validated = Objects.requireNonNull(rehydrateValidator.apply(identity), "escort rehydrate validation result");
			if (validated != ActionResult.APPLIED && validated != ActionResult.ALREADY_APPLIED) {
				return validated;
			}
			EscortLease prior = sessions.putIfAbsent(playerId, identity.questId(), EscortLease.typed(identity));
			return prior == null ? ActionResult.APPLIED
				: identity.equals(prior.identity()) ? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** 直接清理持久化 escort lease；旧 unresolved lease 显式失败。 / Cleans a persisted escort lease directly; legacy unresolved leases fail closed. */
	public synchronized ActionResult clear(CleanupLease cleanupLease, CleanupReason reason) {
		EscortResourceIdentity identity;
		try {
			identity = requireIdentity(cleanupLease, true);
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		EscortLease active = sessions.get(playerId, identity.questId());
		if (active != null && !identity.equals(active.identity())) {
			return ActionResult.FAILED;
		}
		ActionResult result = cleanup(EscortLease.typed(identity), reason);
		if (result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) {
			if (operations.durable() && !operations.release(cleanupLease)) {
				return ActionResult.FAILED;
			}
			sessions.remove(playerId, identity.questId(), active);
		}
		return result;
	}

	/**
	 * 不依赖进程内 adapter 索引，按持久化身份执行一次严格物理清理，供 lifecycle coordinator 恢复使用。
	 * Performs strict physical cleanup from persisted identity without relying on an in-process adapter index, for
	 * lifecycle recovery coordinators.
	 */
	public static ActionResult cleanupPersisted(Player player, CleanupLease cleanupLease, CleanupReason reason) {
		try {
			Player owner = requirePlayer(player);
			if (cleanupLease == null || !"QUEST_ESCORT".equals(cleanupLease.capability())
					|| !(cleanupLease.identity() instanceof EscortResourceIdentity identity) || !identity.materialized()
					|| identity.playerId() != owner.getObjectId() || reason == null) {
				return ActionResult.FAILED;
			}
			ActionResult result = cleanup(owner, new CleanupCommand(identity.playerId(), identity.questId(), identity.objectId(), identity.npcId(),
				identity.worldId(), identity.instanceId(), identity.spawnedFollower(), identity.previousWalkerId(), identity.action(), reason,
				identity.idempotencyKey()));
			if (result != ActionResult.APPLIED && result != ActionResult.ALREADY_APPLIED) {
				return result;
			}
			return QuestGraphResourceOperationRegistry.production().release(cleanupLease) ? result : ActionResult.FAILED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** 在持久化 CAS 成功后释放对应生产 session reservation。 / Releases the production session reservation after successful persistent CAS. */
	static void acknowledgePersistedCleanup(int playerId, CleanupLease cleanupLease) {
		if (cleanupLease == null || !(cleanupLease.identity() instanceof EscortResourceIdentity identity)
				|| identity.playerId() != playerId || !"QUEST_ESCORT".equals(cleanupLease.capability())) {
			throw new IllegalArgumentException("Acknowledged escort cleanup identity is invalid");
		}
		PRODUCTION_SESSIONS.remove(identity);
	}

	/** 放弃指定 quest 时清理其 escort lease。 / Cleans the escort lease when the specified quest is abandoned. */
	public ActionResult onAbandon(int questId) {
		return clearQuest(questId, CleanupReason.ABANDON);
	}

	/** 玩家死亡时清理全部 escort lease。 / Cleans all escort leases when the player dies. */
	public ActionResult onPlayerDeath() {
		return clearAll(CleanupReason.PLAYER_DEATH);
	}

	/** 玩家登出时清理全部 escort lease。 / Cleans all escort leases when the player logs out. */
	public ActionResult onLogout() {
		return clearAll(CleanupReason.LOGOUT);
	}

	/** 任务完成结算时清理指定 escort。 / Cleans the specified escort during quest settlement. */
	public ActionResult onFinish(int questId) {
		return clearQuest(questId, CleanupReason.FINISH);
	}

	/** 消费严格绑定当前 follower 的 typed reached 信号。 / Consumes a typed reached signal strictly bound to the active follower. */
	public ActionResult onReached(EscortReachedTargetEvent event) {
		return onTerminal(event, CleanupReason.REACHED_TARGET);
	}

	/** 消费严格绑定当前 follower 的 typed lost 信号。 / Consumes a typed lost signal strictly bound to the active follower. */
	public ActionResult onLost(EscortLostTargetEvent event) {
		return onTerminal(event, CleanupReason.LOST_TARGET);
	}

	/** 显式清理指定 quest，失败时保留 lease 供重试。 / Explicitly cleans one quest and retains a failed lease for retry. */
	public ActionResult clearQuest(int questId) {
		return clearQuest(questId, CleanupReason.MANUAL);
	}

	/** 返回活跃 lease 数，仅用于确定性审计。 / Returns active lease count for deterministic audit. */
	public synchronized int size() {
		return sessions.size(playerId);
	}

	private synchronized ActionResult clearQuest(int questId, CleanupReason reason) {
		EscortLease lease = sessions.get(playerId, questId);
		if (lease == null) {
			return ActionResult.ALREADY_APPLIED;
		}
		ActionResult result = cleanup(lease, reason);
		if (result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) {
			if (operations.durable() && lease.identity() != null
					&& !operations.release(CleanupLease.escort(lease.identity()))) {
				return ActionResult.FAILED;
			}
			sessions.remove(playerId, questId, lease);
		}
		return result;
	}

	private ActionResult onTerminal(QuestGraphEvent event, CleanupReason reason) {
		EscortLease expected;
		synchronized (this) {
			expected = sessions.get(playerId, event.targetId());
			if (event.playerId() != playerId || expected == null || !expected.matches(event)) {
				return ActionResult.FAILED;
			}
		}
		DispatchResult dispatched;
		try {
			dispatched = Objects.requireNonNull(terminalDispatcher.apply(event), "escort terminal dispatch result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		boolean released;
		try {
			released = leaseReleased.test(expected.questId(), expected.idempotencyKey());
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		if (dispatched.status() != Status.APPLIED || !released) {
			return ActionResult.FAILED;
		}
		synchronized (this) {
			EscortLease current = sessions.get(playerId, expected.questId());
			if (current == null) {
				return ActionResult.ALREADY_APPLIED;
			}
			if (current != expected) {
				return ActionResult.FAILED;
			}
			return clearQuest(expected.questId(), reason);
		}
	}

	private synchronized ActionResult clearAll(CleanupReason reason) {
		ActionResult outcome = sessions.hasAny(playerId) ? ActionResult.APPLIED : ActionResult.ALREADY_APPLIED;
		for (int questId : sessions.questIds(playerId)) {
			ActionResult result = clearQuest(questId, reason);
			if (result != ActionResult.APPLIED && result != ActionResult.ALREADY_APPLIED) {
				outcome = result;
			}
		}
		return outcome;
	}

	private ActionResult cleanup(EscortLease lease, CleanupReason reason) {
		try {
			return Objects.requireNonNull(cleaner.apply(new CleanupCommand(playerId, lease.questId(), lease.followerObjectId(),
				lease.followerNpcId(), lease.worldId(), lease.instanceId(), lease.spawnedFollower(), lease.previousWalkerId(),
				lease.action(), reason, lease.idempotencyKey())), "escort cleanup result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	private boolean validatesLive(EscortLease lease) {
		if (lease.identity() == null) {
			return true;
		}
		try {
			ActionResult result = Objects.requireNonNull(rehydrateValidator.apply(lease.identity()), "escort live validation result");
			return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private EscortResourceIdentity persistedIdentity(ActionInvocation invocation) {
		CleanupLease lease = invocation.cleanupLeases().values().stream()
			.filter(candidate -> invocation.idempotencyKey().equals(candidate.resourceKey()))
			.findFirst().orElse(null);
		if (lease == null) {
			return null;
		}
		EscortResourceIdentity identity = requireIdentity(lease, false);
		if (!(invocation.action() instanceof StartEscortAction action) || identity.questId() != invocation.questId()
				|| !identity.action().equals(action) || !identity.idempotencyKey().equals(invocation.idempotencyKey())) {
			throw new IllegalArgumentException("Persisted escort plan does not match the invocation");
		}
		return identity;
	}

	private EscortResourceIdentity requireIdentity(CleanupLease lease, boolean materialized) {
		if (lease == null || !"QUEST_ESCORT".equals(lease.capability())
				|| !(lease.identity() instanceof EscortResourceIdentity identity) || identity.playerId() != playerId
				|| materialized && !identity.materialized()) {
			throw new IllegalArgumentException("Escort cleanup lease is unresolved or owned by another player");
		}
		return identity;
	}

	private static EscortResourceIdentity plan(StartCommand command) {
		boolean spawned = command.action().source() != EscortSource.EVENT_NPC;
		int followerNpcId = spawned ? command.action().npcId() : command.eventNpcId();
		return new EscortResourceIdentity(command.playerId(), command.questId(), 0, followerNpcId, command.worldId(),
			command.instanceId(), command.x(), command.y(), command.z(), command.eventNpcId(), command.eventNpcObjectId(),
			spawned, null, command.action(), command.idempotencyKey());
	}

	private EscortResourceIdentity reserveOperationIdentity(ActionInvocation invocation, StartCommand command,
			EscortResourceIdentity persisted, CleanupLease existingOperation) {
		if (existingOperation != null) {
			EscortResourceIdentity existing = requireIdentity(existingOperation, true);
			if (!matches(invocation, existing) || persisted != null && persisted.materialized() && !persisted.equals(existing)) {
				throw new IllegalArgumentException("Reserved escort identity conflicts with the invocation journal");
			}
			return existing;
		}
		if (persisted != null && persisted.materialized()) {
			CleanupLease reserved = operations.reserve(CleanupLease.escort(persisted));
			EscortResourceIdentity identity = requireIdentity(reserved, true);
			if (!identity.equals(persisted)) {
				throw new IllegalArgumentException("Materialized escort journal conflicts with the operation registry");
			}
			return identity;
		}
		FollowerReservation follower = followerReservation.apply(command);
		if (follower.objectId() <= 0) {
			throw new IllegalStateException("Escort resource reservation returned an invalid object id");
		}
		EscortResourceIdentity candidate = plan(command).materialize(follower.objectId(), follower.previousWalkerId());
		CleanupLease reservedLease;
		try {
			reservedLease = operations.reserve(CleanupLease.escort(candidate));
		} catch (ObjectIdReservationConflictException e) {
			if (follower.allocatedObjectId()) {
				releaseUnusedId(follower.objectId(), e);
			}
			throw e;
		}
		EscortResourceIdentity reserved = requireIdentity(reservedLease, true);
		if (!candidate.equals(reserved) && follower.allocatedObjectId()) {
			unusedResourceIdReleaser.accept(follower.objectId());
		}
		if (!matches(invocation, reserved)) {
			throw new IllegalArgumentException("Reserved escort identity conflicts with the invocation");
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

	private static boolean matches(ActionInvocation invocation, EscortResourceIdentity identity) {
		return invocation != null && invocation.questId() == identity.questId()
			&& invocation.action().equals(identity.action()) && invocation.idempotencyKey().equals(identity.idempotencyKey());
	}

	private static StartCommand command(EscortResourceIdentity identity) {
		return new StartCommand(identity.questId(), identity.playerId(), identity.action(), identity.eventNpcId(),
			identity.eventNpcObjectId(), identity.worldId(), identity.instanceId(), identity.x(), identity.y(), identity.z(),
			identity.idempotencyKey(), identity.objectId(), identity.previousWalkerId());
	}

	private StartCommand command(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId
				|| !(invocation.action() instanceof StartEscortAction action)) {
			throw new IllegalArgumentException("Escort invocation is invalid");
		}
		int eventNpcId = 0;
		int eventNpcObjectId = 0;
		if (invocation.event() instanceof DialogEvent dialog) {
			eventNpcId = dialog.npcId();
			eventNpcObjectId = dialog.npcObjectId();
		}
		if (action.source() != EscortSource.PLAYER_POSITION_SPAWN && (eventNpcId <= 0 || eventNpcObjectId <= 0)) {
			throw new IllegalArgumentException("Escort event NPC identity is missing");
		}
		PlayerContext context = playerContext.get();
		if (context != null && context.playerId() != playerId) {
			throw new IllegalArgumentException("Escort player context owner changed");
		}
		return new StartCommand(invocation.questId(), playerId, action, eventNpcId, eventNpcObjectId,
			context == null ? 0 : context.worldId(), context == null ? 0 : context.instanceId(),
			context == null ? 0 : context.x(), context == null ? 0 : context.y(), context == null ? 0 : context.z(), invocation.idempotencyKey(), 0, null);
	}

	private static PreflightResult preflight(Player player, StartCommand command) {
		try {
			if (player.getObjectId() != command.playerId() || player.getWorldId() != command.worldId()
					|| player.getInstanceId() != command.instanceId()) {
				return PreflightResult.FAILED;
			}
			if (command.followerObjectId() > 0) {
				var visible = GameWorldBootstrapServices.world().findVisibleObject(command.followerObjectId());
				if (visible instanceof Npc follower && follower.isSpawned() && follower.getNpcId() == expectedFollowerNpcId(command)
						&& follower.getWorldId() == command.worldId() && follower.getInstanceId() == command.instanceId()) {
					return PreflightResult.READY;
				}
			}
			if (player.getController().hasScheduledTask(TaskId.QUEST_FOLLOW)) {
				return PreflightResult.FAILED;
			}
			if (command.action().source() != EscortSource.PLAYER_POSITION_SPAWN) {
				resolveEventNpc(player, command);
			}
			if (command.action().walkerId() != null
					&& (DataManager.WALKER_DATA == null || DataManager.WALKER_DATA.getWalkerTemplate(command.action().walkerId()) == null)) {
				return PreflightResult.FAILED;
			}
			if (command.action().destination() instanceof EscortNpcDestination npc
					&& (DataManager.SPAWNS_DATA2 == null
						|| DataManager.SPAWNS_DATA2.getFirstSpawnByNpcId(player.getWorldId(), npc.npcId()) == null)) {
				return PreflightResult.FAILED;
			}
			if (command.action().destination() instanceof EscortZoneDestination zone
					&& ZoneName.NONE.equals(ZoneName.get(zone.zoneName()).name())) {
				return PreflightResult.FAILED;
			}
			return PreflightResult.READY;
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	private StartResult start(Player player, StartCommand command, QuestEscortCompletionListener completionListener) {
		Npc eventNpc = null;
		Npc follower = null;
		Future<?> task = null;
		String previousWalkerId = null;
		boolean walking = false;
		boolean following = false;
		boolean spawned = command.action().source() != EscortSource.EVENT_NPC;
		try {
			var reservedFollower = command.followerObjectId() > 0
				? GameWorldBootstrapServices.world().findVisibleObject(command.followerObjectId()) : null;
			if (reservedFollower != null && (!(reservedFollower instanceof Npc npc) || !npc.isSpawned()
					|| npc.getNpcId() != expectedFollowerNpcId(command) || npc.getWorldId() != command.worldId()
					|| npc.getInstanceId() != command.instanceId())) {
				throw new IllegalStateException("Reserved escort object id belongs to another world object");
			}
			if (command.action().source() != EscortSource.PLAYER_POSITION_SPAWN
					&& !(command.action().source() == EscortSource.REPLACE_EVENT_NPC_AT_PLAYER_POSITION && reservedFollower != null)) {
				eventNpc = resolveEventNpc(player, command);
			}
			if (spawned) {
				var visible = reservedFollower == null
					? QuestService.spawnQuestNpcWithReservedObjectId(command.worldId(), command.instanceId(), command.action().npcId(),
						command.x(), command.y(), command.z(), command.action().heading(), command.followerObjectId())
					: reservedFollower;
				if (!(visible instanceof Npc npc)) {
					throw new IllegalStateException("Escort follower spawn failed");
				}
				follower = npc;
			} else {
				follower = eventNpc;
			}
			int expectedNpcId = spawned ? command.action().npcId() : command.eventNpcId();
			if (!follower.isSpawned() || follower.getNpcId() != expectedNpcId || follower.getWorldId() != command.worldId()
					|| follower.getInstanceId() != command.instanceId()
					|| GameWorldBootstrapServices.world().findVisibleObject(follower.getObjectId()) != follower) {
				throw new IllegalStateException("Escort follower identity does not match the frozen plan");
			}
			previousWalkerId = command.followerObjectId() > 0 ? command.previousWalkerId() : follower.getSpawn().getWalkerId();
			if (command.action().walkerId() != null) {
				follower.getSpawn().setWalkerId(command.action().walkerId());
			}
			if (command.action().startWalking()) {
				if (!(follower.getAi2() instanceof NpcAI2 ai) || !WalkManager.startWalking(ai)) {
					throw new IllegalStateException("Escort walking failed");
				}
				walking = true;
			}
			if (command.action().sendNpcInfo()) {
				PacketSendUtility.sendPacket(player, new SM_NPC_INFO(follower, player));
			}
			if (command.action().followMe()) {
				follower.getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, player);
				following = true;
			}
			if (command.action().startEmote2()) {
				PacketSendUtility.broadcastPacket(follower,
					new SM_EMOTION(follower, EmotionType.START_EMOTE2, 0, follower.getObjectId()));
			}
			QuestEnv env = new QuestEnv(follower, player, command.questId(), 0);
			task = destinationTask(env, follower, command.action(), completionListener);
			if (task == null) {
				throw new IllegalStateException("Escort task creation failed");
			}
			player.getController().addTask(TaskId.QUEST_FOLLOW, task);
			if (command.action().source() == EscortSource.REPLACE_EVENT_NPC_AT_PLAYER_POSITION && eventNpc != null) {
				eventNpc.getController().onDelete();
				if (GameWorldBootstrapServices.world().findVisibleObject(eventNpc.getObjectId()) != null) {
					throw new IllegalStateException("Escort event NPC replacement failed");
				}
			}
			return new StartResult(ActionResult.APPLIED, follower.getObjectId(), spawned, previousWalkerId);
		} catch (RuntimeException e) {
			rollback(player, follower, task, spawned, previousWalkerId, walking, following);
			return StartResult.failed();
		}
	}

	private static int expectedFollowerNpcId(StartCommand command) {
		return command.action().source() == EscortSource.EVENT_NPC ? command.eventNpcId() : command.action().npcId();
	}

	private static FollowerReservation reserveFollower(Player player, StartCommand command, IntSupplier resourceIds) {
		if (command.action().source() != EscortSource.EVENT_NPC) {
			if (command.action().source() == EscortSource.REPLACE_EVENT_NPC_AT_PLAYER_POSITION) {
				resolveEventNpc(player, command);
			}
			return new FollowerReservation(resourceIds.getAsInt(), null, true);
		}
		Npc follower = resolveEventNpc(player, command);
		return new FollowerReservation(follower.getObjectId(), follower.getSpawn().getWalkerId(), false);
	}

	private static Future<?> destinationTask(QuestEnv env, Npc follower, StartEscortAction action,
			QuestEscortCompletionListener completionListener) {
		return switch (action.destination()) {
			case EscortZoneDestination zone -> QuestTasks.newFollowingToTargetCheckTask(env, follower, ZoneName.get(zone.zoneName()), completionListener);
			case EscortNpcDestination npc -> QuestTasks.newFollowingToTargetCheckTask(env, follower, npc.npcId(), completionListener);
			case EscortCoordinatesDestination coordinates -> QuestTasks.newFollowingToTargetCheckTask(env, follower,
				coordinates.x(), coordinates.y(), coordinates.z(), completionListener);
		};
	}

	static ActionResult cleanup(Player player, CleanupCommand command) {
		try {
			player.getController().cancelTask(TaskId.QUEST_FOLLOW);
			var visible = GameWorldBootstrapServices.world().findVisibleObject(command.followerObjectId());
			if (visible == null) {
				return ActionResult.ALREADY_APPLIED;
			}
			if (!(visible instanceof Npc follower) || follower.getNpcId() != command.followerNpcId()
					|| follower.getWorldId() != command.worldId() || follower.getInstanceId() != command.instanceId()) {
				return ActionResult.FAILED;
			}
			if (command.action().followMe()) {
				follower.getAi2().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, player);
			}
			if (command.action().startWalking() && follower.getAi2() instanceof NpcAI2 ai) {
				WalkManager.stopWalking(ai);
			}
			follower.getSpawn().setWalkerId(command.previousWalkerId());
			if (command.spawnedFollower()) {
				follower.getController().onDelete();
				if (GameWorldBootstrapServices.world().findVisibleObject(command.followerObjectId()) != null) {
					return ActionResult.FAILED;
				}
			}
			return ActionResult.APPLIED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	private QuestEscortCompletionListener completionListener(StartCommand command) {
		QuestEscortCompletionListener typed = new QuestEscortCompletionListener() {
			@Override
			public void onReached(QuestEnv env, Npc follower) {
				long occurredAt = System.currentTimeMillis();
				EscortReachedTargetEvent event = QuestGraphNpcSignalBridge.escortReached(
					eventId("reached", command, follower, occurredAt), occurredAt, command.questId(), playerSnapshot(env.getPlayer()), npcSnapshot(follower));
				ActionResult result = QuestGraphEscortActionAdapter.this.onReached(event);
				if (result != ActionResult.APPLIED && result != ActionResult.ALREADY_APPLIED) {
					throw new IllegalStateException("Escort reached signal or cleanup failed");
				}
			}

			@Override
			public void onLost(QuestEnv env, Npc follower) {
				long occurredAt = System.currentTimeMillis();
				EscortLostTargetEvent event = QuestGraphNpcSignalBridge.escortLost(
					eventId("lost", command, follower, occurredAt), occurredAt, command.questId(), playerSnapshot(env.getPlayer()), npcSnapshot(follower));
				ActionResult result = QuestGraphEscortActionAdapter.this.onLost(event);
				if (result != ActionResult.APPLIED && result != ActionResult.ALREADY_APPLIED) {
					throw new IllegalStateException("Escort lost signal or cleanup failed");
				}
			}
		};
		return QuestEscortCompletionListener.legacyAnd(typed);
	}

	private static String eventId(String outcome, StartCommand command, Npc follower, long occurredAt) {
		return "escort-" + outcome + '-' + command.playerId() + '-' + command.questId() + '-' + follower.getObjectId() + '-' + occurredAt;
	}

	private static PlayerSnapshot playerSnapshot(Player player) {
		return new PlayerSnapshot(player.getObjectId(), player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(), player.getZ());
	}

	private static NpcSnapshot npcSnapshot(Npc npc) {
		return new NpcSnapshot(npc.getNpcId(), npc.getObjectId(), npc.getWorldId(), npc.getInstanceId(), npc.getX(), npc.getY(), npc.getZ());
	}

	private static boolean leaseReleased(Player player, int questId, String resourceKey) {
		var state = player.getQuestGraphStateList().get(questId);
		return state == null || !state.getCleanupLeases().containsKey(resourceKey);
	}

	private static ActionResult validateLiveEscort(Player player, EscortResourceIdentity identity) {
		try {
			if (player.getObjectId() != identity.playerId() || player.getWorldId() != identity.worldId()
					|| player.getInstanceId() != identity.instanceId() || !player.getController().hasScheduledTask(TaskId.QUEST_FOLLOW)) {
				return ActionResult.FAILED;
			}
			var visible = GameWorldBootstrapServices.world().findVisibleObject(identity.objectId());
			return visible instanceof Npc follower && follower.isSpawned() && follower.getNpcId() == identity.npcId()
					&& follower.getWorldId() == identity.worldId() && follower.getInstanceId() == identity.instanceId()
				? ActionResult.APPLIED : ActionResult.FAILED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	private static Npc resolveEventNpc(Player player, StartCommand command) {
		var visible = GameWorldBootstrapServices.world().findVisibleObject(command.eventNpcObjectId());
		if (!(visible instanceof Npc npc) || npc.getNpcId() != command.eventNpcId() || npc.getWorldId() != command.worldId()
				|| npc.getInstanceId() != command.instanceId()) {
			throw new IllegalArgumentException("Escort event NPC does not match the player context");
		}
		return npc;
	}

	private static void rollback(Player player, Npc follower, Future<?> task, boolean spawned, String previousWalkerId,
			boolean walking, boolean following) {
		if (task != null) {
			task.cancel(false);
			player.getController().cancelTask(TaskId.QUEST_FOLLOW);
		}
		if (follower == null) {
			return;
		}
		try {
			if (following) {
				follower.getAi2().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, player);
			}
			if (walking && follower.getAi2() instanceof NpcAI2 ai) {
				WalkManager.stopWalking(ai);
			}
			follower.getSpawn().setWalkerId(previousWalkerId);
			if (spawned) {
				follower.getController().onDelete();
			}
		} catch (RuntimeException ignored) {
		}
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** escort 清理触发原因。 / Reason that triggered escort cleanup. */
	public enum CleanupReason {
		LOGOUT,
		PLAYER_DEATH,
		ABANDON,
		FINISH,
		REACHED_TARGET,
		LOST_TARGET,
		RECOVERY_COMPENSATION,
		MANUAL
	}

	/** 由编译 action 与不可变事件身份构成的启动命令。 / Start command built from the compiled action and immutable event identity. */
	public record StartCommand(int questId, int playerId, StartEscortAction action, int eventNpcId, int eventNpcObjectId,
			int worldId, int instanceId, float x, float y, float z, String idempotencyKey,
			int followerObjectId, String previousWalkerId) {
		/** 校验 owner、event NPC 组合与幂等键。 / Validates owner, event-NPC combination, and idempotency key. */
		public StartCommand {
			Objects.requireNonNull(action, "action");
			if (questId <= 0 || playerId <= 0 || eventNpcId < 0 || eventNpcObjectId < 0 || followerObjectId < 0
					|| (eventNpcId == 0) != (eventNpcObjectId == 0) || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Escort start command is invalid");
			}
			if (worldId < 0 || instanceId < 0 || worldId == 0 && instanceId != 0
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Escort start world context is invalid");
			}
		}
	}

	/** Frozen follower reservation produced before any escort world mutation. */
	record FollowerReservation(int objectId, String previousWalkerId, boolean allocatedObjectId) {
		FollowerReservation {
			if (objectId < 0 || objectId == 0 && (previousWalkerId != null || allocatedObjectId)) {
				throw new IllegalArgumentException("Escort follower reservation is invalid");
			}
		}
	}

	/** 启动端点结果，成功时携带真实 follower 与 cleanup 恢复参数。 / Start result carrying the real follower and cleanup restoration data on success. */
	public record StartResult(ActionResult result, int followerObjectId, boolean spawnedFollower, String previousWalkerId) {
		/** 校验成功结果必须携带真实世界 objectId。 / Validates that successful results carry a real world object id. */
		public StartResult {
			Objects.requireNonNull(result, "result");
			if (followerObjectId < 0
					|| (result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) && followerObjectId == 0) {
				throw new IllegalArgumentException("Escort start result is invalid");
			}
		}

		/** 构造失败结果。 / Creates a failed result. */
		public static StartResult failed() {
			return new StartResult(ActionResult.FAILED, 0, false, null);
		}
	}

	/** 清理指定 player/quest escort lease 的稳定命令。 / Stable command that cleans one player/quest escort lease. */
	public record CleanupCommand(int playerId, int questId, int followerObjectId, int followerNpcId, int worldId, int instanceId,
			boolean spawnedFollower, String previousWalkerId, StartEscortAction action, CleanupReason reason, String idempotencyKey) {
		/** 校验清理 owner、follower 与原因。 / Validates cleanup owner, follower, and reason. */
		public CleanupCommand {
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(reason, "reason");
			if (playerId <= 0 || questId <= 0 || followerObjectId <= 0 || followerNpcId < 0 || worldId < 0 || instanceId < 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Escort cleanup command is invalid");
			}
		}
	}

	/** 玩家当前世界坐标快照，仅在 PREPARED 前冻结一次。 / Player world snapshot frozen once before PREPARED. */
	public record PlayerContext(int playerId, int worldId, int instanceId, float x, float y, float z) {
		public PlayerContext {
			if (playerId <= 0 || worldId <= 0 || instanceId < 0 || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Escort player context is invalid");
			}
		}

		public static PlayerContext from(Player player) {
			return new PlayerContext(player.getObjectId(), player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(), player.getZ());
		}
	}

	private record EscortLease(int questId, int followerObjectId, int followerNpcId, int worldId, int instanceId,
			int eventNpcId, int eventNpcObjectId,
			boolean spawnedFollower, String previousWalkerId, StartEscortAction action, String idempotencyKey,
			EscortResourceIdentity identity) {
		private static EscortLease typed(EscortResourceIdentity identity) {
			return new EscortLease(identity.questId(), identity.objectId(), identity.npcId(), identity.worldId(), identity.instanceId(),
				identity.eventNpcId(), identity.eventNpcObjectId(), identity.spawnedFollower(), identity.previousWalkerId(), identity.action(),
				identity.idempotencyKey(), identity);
		}

		private static EscortLease legacy(StartCommand command, StartResult result) {
			int npcId = result.spawnedFollower() ? command.action().npcId() : command.eventNpcId();
			return new EscortLease(command.questId(), result.followerObjectId(), npcId, command.worldId(), command.instanceId(),
				command.eventNpcId(), command.eventNpcObjectId(), result.spawnedFollower(), result.previousWalkerId(), command.action(),
				command.idempotencyKey(), null);
		}

		private static EscortLease planned(StartCommand command) {
			int npcId = command.action().source() == EscortSource.EVENT_NPC ? command.eventNpcId() : command.action().npcId();
			return new EscortLease(command.questId(), 0, npcId, command.worldId(), command.instanceId(), command.eventNpcId(),
				command.eventNpcObjectId(), command.action().source() != EscortSource.EVENT_NPC, null, command.action(),
				command.idempotencyKey(), null);
		}

		private boolean materialized() {
			return followerObjectId > 0;
		}

		private boolean matches(StartCommand command) {
			return questId == command.questId() && worldId == command.worldId() && instanceId == command.instanceId()
				&& eventNpcId == command.eventNpcId() && eventNpcObjectId == command.eventNpcObjectId()
				&& action.equals(command.action()) && idempotencyKey.equals(command.idempotencyKey());
		}

		private boolean matches(QuestGraphEvent event) {
			return switch (event) {
				case EscortReachedTargetEvent reached -> matches(reached.targetId(), reached.npcId(), reached.npcObjectId(),
					reached.worldId(), reached.instanceId());
				case EscortLostTargetEvent lost -> matches(lost.targetId(), lost.npcId(), lost.npcObjectId(),
					lost.worldId(), lost.instanceId());
				default -> false;
			};
		}

		private boolean matches(int eventQuestId, int eventNpcId, int eventObjectId, int eventWorldId, int eventInstanceId) {
			return eventQuestId == questId && eventNpcId == followerNpcId
				&& eventObjectId == followerObjectId && eventWorldId == worldId && eventInstanceId == instanceId;
		}
	}

	/** 共享同一 JVM 内 adapter 重建前后的 materialized escort 身份。 / Shares materialized escort identities across adapter recreation in one JVM. */
	static final class EscortSessionRegistry {
		private final Map<SessionKey, EscortLease> leases = new HashMap<>();

		private synchronized EscortLease get(int playerId, int questId) {
			return leases.get(new SessionKey(playerId, questId));
		}

		private synchronized boolean hasAny(int playerId) {
			return leases.keySet().stream().anyMatch(key -> key.playerId() == playerId);
		}

		private synchronized EscortLease putIfAbsent(int playerId, int questId, EscortLease lease) {
			return leases.putIfAbsent(new SessionKey(playerId, questId), lease);
		}

		private synchronized boolean reserve(int playerId, int questId, EscortLease reservation) {
			if (hasAny(playerId)) {
				return false;
			}
			leases.put(new SessionKey(playerId, questId), reservation);
			return true;
		}

		private synchronized boolean replace(int playerId, int questId, EscortLease expected, EscortLease materialized) {
			SessionKey key = new SessionKey(playerId, questId);
			if (leases.get(key) != expected) {
				return false;
			}
			leases.put(key, materialized);
			return true;
		}

		private synchronized void remove(int playerId, int questId, EscortLease expected) {
			SessionKey key = new SessionKey(playerId, questId);
			if (leases.get(key) == expected) {
				leases.remove(key);
			}
		}

		private synchronized void remove(EscortResourceIdentity identity) {
			SessionKey key = new SessionKey(identity.playerId(), identity.questId());
			EscortLease current = leases.get(key);
			if (current != null && identity.equals(current.identity())) {
				leases.remove(key);
			}
		}

		private synchronized java.util.List<Integer> questIds(int playerId) {
			return leases.keySet().stream().filter(key -> key.playerId() == playerId).map(SessionKey::questId).sorted().toList();
		}

		private synchronized int size(int playerId) {
			return (int) leases.keySet().stream().filter(key -> key.playerId() == playerId).count();
		}
	}

	private record SessionKey(int playerId, int questId) {
		private SessionKey {
			if (playerId <= 0 || questId <= 0) {
				throw new IllegalArgumentException("Escort session owner is invalid");
			}
		}
	}
}
