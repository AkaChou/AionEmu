package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.e2e.client.VirtualClientState;
import com.aionemu.gameserver.questEngine.e2e.QuestE2eTransitionMatch;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestMembershipPermission;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.definition.QuestPvpKillFacts;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.world.VirtualClock;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 通过正式 QuestProductionDispatcher/QuestExecutionCoordinator 执行一个独立任务场景，并以真实 typed
 * after-commit 组合连接内存世界、状态、协议、生成、AI、传送和电影端口。
 * Executes one isolated quest scenario through the formal QuestProductionDispatcher/QuestExecutionCoordinator and
 * connects the real typed after-commit composition to in-memory state, protocol, spawn, AI, teleport, and movie ports.
 */
public final class QuestE2eRuntime implements QuestHeadlessClient.ActionBridge, AutoCloseable {
	private static final int PLAYER_ID = QuestE2eWorldFixture.PLAYER_ID;
	private final CompiledQuestDefinition definition;
	private final VirtualClientState state;
	private final QuestTrace trace = new QuestTrace();
	private final List<QuestAuditEvent> auditEvents = new ArrayList<>();
	private final QuestE2eWorldFixture world;
	private final ScenarioFacts facts = new ScenarioFacts();
	private final InMemoryActionPort actionPort = new InMemoryActionPort();
	private final InMemoryStatePort statePort = new InMemoryStatePort();
	private final QuestProductionDispatcher.ConnectionProvider connections = this::connection;
	private final QuestEventIndex eventIndex;
	private final QuestRuntimeMetricsCollector metrics = new QuestRuntimeMetricsCollector();
	private final QuestProductionDispatcher dispatcher;
	private boolean failCommit;
	private boolean unsupportedFacts;
	private QuestTransition preparedTransition;
	private QuestTransition matchedTransition;
	private List<QuestTransition> matchedTransitionCandidates = List.of();
	private QuestRouteResult matchedRouteResult = QuestRouteResult.UNKNOWN;
	private int routeCandidateCount;
	private List<QuestAction> committedTransactionActions = List.of();

	public QuestE2eRuntime(CompiledQuestDefinition definition) throws Exception {
		this.definition = java.util.Objects.requireNonNull(definition, "definition");
		state = new VirtualClientState(definition.id());
		world = new QuestE2eWorldFixture(state, trace);
		QuestMetadata metadata = definition.definition().metadata();
		PlayerQuestDialogPort dialogPort = new PlayerQuestDialogPort(id -> id == PLAYER_ID ? world.player() : null);
		PlayerQuestStateSyncPort stateSync = new PlayerQuestStateSyncPort(
			id -> id == PLAYER_ID ? world.player() : null,
			id -> id == definition.id() ? metadata : null,
			id -> id == state.currentObjectId() ? world.player().getTarget() : null,
			ignored -> { }, ignored -> { }, ignored -> { });
		PlayerQuestEffectPort factionEffectPort = new PlayerQuestEffectPort(
			id -> id == PLAYER_ID ? world.player() : null,
			new PlayerQuestEffectPort.EffectOperations() {
				@Override public void apply(Player player, int skillId, int durationMillis) {
					world.applyEffect(skillId, durationMillis);
				}
				@Override public void remove(Player player, int effectId) {
					world.removeEffect(effectId);
				}
			}, id -> id == definition.id() ? metadata : null);
		PlayerQuestSystemMessagePort playerSystemMessagePort = new PlayerQuestSystemMessagePort(
			id -> id == PLAYER_ID ? world.player() : null,
			id -> id == definition.id() ? metadata : null);
		TypedQuestAfterCommitPort afterCommit = new TypedQuestAfterCommitPort(dialogPort, world.teleportPort(),
			world.moviePort(), world.spawnPort(), world.aiPort(), timerPort(), stateSync,
			(statsSnapshot, plan) -> {
				trace.add("AFTER_COMMIT", "refresh-player-stats");
				return true;
			}, effectPort(factionEffectPort), npcPort(), systemMessagePort(playerSystemMessagePort));
		afterCommit.withBroadcastPort(broadcastPort());
		ImmutableQuestCatalog catalog = new ImmutableQuestCatalog(List.of(definition));
		eventIndex = new QuestEventIndex(catalog);
		dispatcher = new QuestProductionDispatcher(catalog,
			new QuestExecutionCoordinator(new PlayerSerialExecutor()), this::snapshot,
			actionPort, statePort, (action, snapshot, plan) -> executeAfterCommit(afterCommit, action, snapshot, plan),
			connections, auditEvents::add, metrics);
	}

	/** 返回正式 dispatcher，供协议回环安装到 QuestEngine。 / Returns the formal dispatcher for protocol-loop installation into QuestEngine. */
	public QuestProductionDispatcher dispatcher() { return dispatcher; }
	/** 返回本场景的客户端状态。 / Returns the client state for this scenario. */
	public VirtualClientState state() { return state; }
	/** 返回本场景的有序轨迹。 / Returns the ordered trace for this scenario. */
	public QuestTrace trace() { return trace; }
	/** 返回生产 dispatcher 记录的结构化失败事件。 / Returns structured failure events recorded by the production dispatcher. */
	public List<QuestAuditEvent> auditEvents() { return List.copyOf(auditEvents); }
	/** 返回真实内存世界夹具。 / Returns the real-port in-memory world fixture. */
	public QuestE2eWorldFixture world() { return world; }
	/** 返回当前任务相关背包事实快照。 / Returns a snapshot of the current quest-relevant inventory facts. */
	public Map<Integer, Integer> inventorySnapshot() { return Map.copyOf(facts.inventory); }
	/** 返回当前路由指标快照，供真实 CM 回环识别本次分发结论。 / Returns the current routing metrics snapshot so the real CM loop can identify this dispatch outcome. */
	public QuestRuntimeMetricsCollector.Snapshot metricsSnapshot() { return metrics.snapshot(); }
	/**
	 * 从协议调用前后的指标差异中返回结论性 route 结果。
	 * Returns the conclusive route result from the metrics delta around one protocol call.
	 */
	public QuestRouteResult conclusiveResultSince(QuestRuntimeMetricsCollector.Snapshot before) {
		QuestRuntimeMetricsCollector.Snapshot after = metrics.snapshot();
		for (QuestRouteResult result : List.of(QuestRouteResult.FAILED, QuestRouteResult.BLOCKED,
				QuestRouteResult.HANDLED)) {
			if (after.outcomeCount(result) > before.outcomeCount(result)) {
				return result;
			}
		}
		return QuestRouteResult.UNKNOWN;
	}
	/** 当前场景是否引用了无法确定性捕获的事实。 / Whether this scenario references facts that cannot be captured deterministically. */
	public boolean unsupportedFacts() { return unsupportedFacts; }
	/** 返回提交后对话包应使用的权威目标 objectId；无目标对话返回零。 / Returns the authoritative target object ID for post-commit dialog packets, or zero for targetless dialogs. */
	public int expectedDialogTargetObjectId() { return facts.targetless ? 0 : facts.interactionObjectId(); }
	/** 返回最后一次分发实际命中的 transition；没有结论性 route 时返回 null。 / Returns the transition conclusively selected by the last dispatch, or null when no route concluded. */
	public QuestTransition matchedTransition() { return matchedTransition; }
	/** 返回最后一次真实协议请求可归因到的 transition 候选。 / Returns transition candidates attributable to the last real protocol request. */
	public List<QuestTransition> matchedTransitionCandidates() { return matchedTransitionCandidates; }
	/** 返回当前准备的目标 transition；未调用 prepare 时返回 null。 / Returns the currently prepared target transition, or null before prepare. */
	public QuestTransition preparedTransition() { return preparedTransition; }
	/** 返回实际命中 route 的结论。 / Returns the conclusive result of the selected route. */
	public QuestRouteResult matchedRouteResult() { return matchedRouteResult; }
	/** 返回最后一次分发的候选 route 数。 / Returns the number of route candidates in the last dispatch. */
	public int routeCandidateCount() { return routeCandidateCount; }
	/**
	 * 返回最后一次请求已提交的完整事务动作。
	 * Returns the complete transactional actions committed by the last request.
	 */
	public List<QuestAction> committedTransactionActions() { return committedTransactionActions; }
	/** 将最后一次实际 route 与准备的目标 transition 对照归因。 / Attributes the last selected route relative to the prepared target transition. */
	public QuestE2eTransitionMatch transitionMatch() {
		if (unsupportedFacts) return QuestE2eTransitionMatch.UNSUPPORTED_SCENARIO_FACTS;
		if (matchedTransition == null) return QuestE2eTransitionMatch.NO_TRANSITION_MATCHED;
		return matchedTransition == preparedTransition
			? QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED
			: QuestE2eTransitionMatch.ALTERNATE_TRANSITION_MATCHED;
	}
	/** 让下一次事务 commit 失败，用于验证原子回滚。 / Makes the next transaction commit fail to verify atomic rollback. */
	public void failNextCommit() { failCommit = true; }
	/**
	 * 用指定计数替换场景中已捕获的装备物品事实。
	 * Replaces the scenario's captured equipped-item facts with the supplied counts.
	 *
	 * @param equippedItems 物品模板 ID 到已装备数量 / item-template ids to equipped counts
	 */
	public void replaceEquippedItemFacts(Map<Integer, Integer> equippedItems) {
		facts.equipmentCaptured = true;
		facts.equippedItems.clear();
		facts.equippedItems.putAll(java.util.Objects.requireNonNull(equippedItems, "equippedItems"));
	}
	/**
	 * 替换整条持续 Journey 使用的玩家职业事实。
	 * Replaces the player-class fact used by the entire persistent journey.
	 *
	 * @param playerClass 规划器选定的具体玩家职业 / concrete player class selected by the planner
	 */
	public void replacePlayerClassFacts(PlayerClass playerClass) {
		facts.playerClass = java.util.Objects.requireNonNull(playerClass, "playerClass");
		world.playerFacts(facts.race, facts.playerClass);
		state.playerFacts(state.level(), facts.race, facts.playerClass);
	}
	/**
	 * 种入规划路径开始前已经由前置任务或世界行为提供的背包事实。
	 * Seeds inventory facts already supplied by prerequisite quests or world behavior before the planned path starts.
	 *
	 * @param initialInventory 物品模板 ID 到最小初始数量 / item-template ids to minimum initial counts
	 */
	public void seedInitialInventoryFacts(Map<Integer, Integer> initialInventory) {
		for (Map.Entry<Integer, Integer> entry : Map.copyOf(initialInventory).entrySet()) {
			if (entry.getKey() <= 0 || entry.getValue() <= 0) {
				throw new IllegalArgumentException("initial inventory ids and counts must be positive");
			}
			facts.inventory.merge(entry.getKey(), entry.getValue(), Math::max);
			state.setItemCount(entry.getKey(), facts.inventory.get(entry.getKey()));
		}
	}
	/**
	 * 应用一次由生产 metadata 明确声明且概率为 100% 的交互物掉落；它是世界掉落事实，不伪装成任务 transition。
	 * Applies one deterministic (100%) interaction-object drop declared by production metadata; this remains a
	 * world-drop fact and is never presented as a quest transition.
	 *
	 * @param npcId 产生掉落的交互物模板 ID / interaction-object template id producing the drop
	 * @param itemId 已获得的任务物品模板 ID / acquired quest-item template id
	 */
	public void receiveDeterministicMetadataDrop(int npcId, int itemId) {
		QuestDrop drop = definition.definition().metadata().drops().stream()
			.filter(candidate -> candidate.npcId() == npcId && candidate.itemId() == itemId)
			.filter(candidate -> candidate.chance() == 100)
			.findFirst().orElseThrow(() -> new IllegalArgumentException(
				"production metadata has no deterministic quest drop for npc=" + npcId + " item=" + itemId));
		facts.inventory.merge(drop.itemId(), 1, Integer::sum);
		state.setItemCount(drop.itemId(), facts.inventory.get(drop.itemId()));
		trace.add("WORLD", "metadata-drop:" + drop.npcId() + ":" + drop.itemId());
	}
	/**
	 * 种入规划路径中不会由任务状态迁移改变的角色、资格和能力条件。
	 * Seeds character, eligibility, and capability conditions that quest-state transitions do not mutate.
	 *
	 * @param transitions 已选择路径上的生产 transition / production transitions on the selected path
	 */
	public void seedPersistentJourneyConditions(List<QuestTransition> transitions) {
		for (QuestTransition transition : List.copyOf(transitions)) {
			for (QuestCondition condition : transition.conditions()) {
				switch (condition) {
					case QuestCondition.StatusIs ignored -> { }
					case QuestCondition.HasItem ignored -> { }
					case QuestCondition.QuestVariableIs ignored -> { }
					case QuestCondition.VariableAtLeast ignored -> { }
					case QuestCondition.VariableBelow ignored -> { }
					case QuestCondition.VariableSumIs ignored -> { }
					case QuestCondition.VariableSumBelow ignored -> { }
					case QuestCondition.WorldIs ignored -> { }
					case QuestCondition.WorldNpcIs ignored -> { }
					case QuestCondition.ZoneIs ignored -> { }
					case QuestCondition.NpcHpBelowPercent ignored -> { }
					default -> applyCondition(condition);
				}
			}
		}
		finishConditionFacts();
		world.playerFacts(facts.race, facts.playerClass);
		state.playerFacts(state.level(), facts.race, facts.playerClass);
	}
	/**
	 * 在单步请求前物化该 transition 所需的动态世界事实，但不重新投影任务状态。
	 * Materializes dynamic world facts required by one transition before its request without re-projecting quest state.
	 *
	 * @param transition 即将执行的生产 transition / production transition about to execute
	 */
	public void seedStepConditions(QuestTransition transition) {
		for (QuestCondition condition : java.util.Objects.requireNonNull(transition, "transition").conditions()) {
			switch (condition) {
				case QuestCondition.WorldIs ignored -> applyCondition(condition);
				case QuestCondition.WorldNpcIs ignored -> applyCondition(condition);
				case QuestCondition.ZoneIs ignored -> applyCondition(condition);
				case QuestCondition.NpcHpBelowPercent ignored -> applyCondition(condition);
				default -> { }
			}
		}
	}
	/** 清除装备事实捕获状态，用于验证生产条件求值 fail closed。 / Clears captured equipment facts to verify fail-closed production evaluation. */
	public void clearCapturedEquipmentFacts() {
		facts.equipmentCaptured = false;
		facts.equippedItems.clear();
		facts.itemSetParts.clear();
	}

	/** 准备一个以指定 transition 为目标的最小满足/不满足事实场景。 / Prepares the minimal satisfying/non-satisfying fact scenario for one transition. */
	public void prepare(QuestTransition transition) {
		preparedTransition = java.util.Objects.requireNonNull(transition, "transition");
		unsupportedFacts = false;
		matchedTransition = null;
		matchedTransitionCandidates = List.of();
		matchedRouteResult = QuestRouteResult.UNKNOWN;
		routeCandidateCount = 0;
		committedTransactionActions = List.of();
		auditEvents.clear();
		NodeProjection source = sourceProjection(transition);
		if (source == null) {
			unsupportedFacts = true;
			return;
		}
		state.project(source.status(), definition.definition().progressLayout().pack(source.variables()));
		facts.reset();
		seedNpcFaction(source.status());
		seedMetadataStartConditions(transition, source.status());
		seedEventAuthority(transition.event());
		for (QuestCondition condition : transition.conditions()) {
			applyCondition(condition);
		}
		finishConditionFacts();
		for (AfterCommitAction action : transition.afterCommit()) {
			seedAfterCommit(action);
		}
		for (QuestAction action : transition.actions()) {
			if (action instanceof QuestAction.RemoveItem remove && !remove.removeAll()) {
				facts.inventory.putIfAbsent(remove.itemId(), remove.count());
			}
			if (action instanceof QuestAction.UnequipItem unequip) {
				facts.equipmentCaptured = true;
				facts.equippedItems.putIfAbsent(unequip.itemId(), 1);
			}
		}
		facts.completed.addAll(definition.definition().metadata().prerequisites());
		world.playerFacts(facts.race, facts.playerClass);
		state.playerFacts(65, facts.race, facts.playerClass);
	}

	/**
	 * 为一次持续会话请求重建事件权威并清空仅属于上一次路由的归因数据，
	 * 不改变任务、背包或世界进度。
	 * Re-establishes event authority for one continuous-session request and clears attribution owned only by the
	 * previous route without changing quest, inventory, or world progress.
	 *
	 * @param request 即将进入任务运行时的客户端请求 / client request about to enter the quest runtime
	 */
	public void beginRequest(ClientActionRequest request) {
		java.util.Objects.requireNonNull(request, "request");
		if (request.questId() != definition.id()) {
			throw new IllegalArgumentException("request quest does not match runtime definition");
		}
		matchedTransition = null;
		matchedTransitionCandidates = List.of();
		matchedRouteResult = QuestRouteResult.UNKNOWN;
		routeCandidateCount = 0;
		committedTransactionActions = List.of();
		auditEvents.clear();
		switch (request.event()) {
			case QuestEvent.TalkToNpc talk -> {
				facts.targetless = false;
				facts.itemObjectId = 0;
				int objectId = request.objectId() > 0 ? request.objectId() : talk.interactionObjectId();
				if (objectId > 0) {
					state.interactWith(talk.npcId(), objectId);
				}
			}
			case QuestEvent.UseItem use -> {
				facts.targetless = false;
				facts.itemObjectId = request.itemObjectId() > 0 ? request.itemObjectId()
					: use.itemObjectId() > 0 ? use.itemObjectId() : facts.itemObjectId;
				facts.inventory.putIfAbsent(use.itemId(), 1);
			}
			case QuestEvent.ItemPlay itemPlay -> {
				facts.targetless = false;
				facts.itemObjectId = request.itemObjectId() > 0 ? request.itemObjectId() : facts.itemObjectId;
				facts.inventory.putIfAbsent(itemPlay.itemId(), 1);
			}
			case QuestEvent.GetItem get -> {
				facts.targetless = true;
				facts.itemObjectId = 0;
				facts.inventory.merge(get.itemId(), 1, Math::max);
			}
			case QuestEvent.CollectItem collect -> {
				facts.targetless = true;
				facts.itemObjectId = 0;
				facts.inventory.merge(collect.itemId(), collect.count(), Math::max);
			}
			default -> {
				facts.targetless = true;
				facts.itemObjectId = 0;
			}
		}
	}

	private NodeProjection sourceProjection(QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.sourceNode()))
				.map(com.aionemu.gameserver.questEngine.definition.QuestNode::projection)
				.findFirst().orElse(null);
		}
		Set<QuestStatus> requiredStatuses = transition.conditions().stream()
			.filter(QuestCondition.StatusIs.class::isInstance)
			.map(QuestCondition.StatusIs.class::cast)
			.map(QuestCondition.StatusIs::status)
			.collect(java.util.stream.Collectors.toSet());
		if (requiredStatuses.size() > 1) {
			return null;
		}
		return definition.definition().nodes().stream()
			.filter(node -> requiredStatuses.isEmpty() || requiredStatuses.contains(node.projection().status()))
			.map(com.aionemu.gameserver.questEngine.definition.QuestNode::projection)
			.findFirst().orElse(null);
	}

	/**
	 * 按生产 {@link PlayerQuestEventPort} 的协议边界构造事件权威信息；只有 NPC 对话拥有对话目标，物品入口保留
	 * CM_USE_ITEM 携带的物品 objectId，其余事件发送任务页面时必须使用 targetless objectId 0。
	 * Seeds event authority using the production {@link PlayerQuestEventPort} protocol boundary: only NPC dialog owns
	 * a dialog target, item ingress retains the item object ID carried by CM_USE_ITEM, and all remaining events must use
	 * targetless object ID 0 when sending quest pages.
	 */
	private void seedEventAuthority(QuestEvent event) {
		switch (event) {
			case QuestEvent.TalkToNpc talk -> world.seedInteractionNpc(talk.npcId(), 900_000 + talk.npcId());
			case QuestEvent.UseItem use ->
				facts.itemObjectId = use.itemObjectId() == 0 ? 800_000 + use.itemId() : use.itemObjectId();
			case QuestEvent.ItemPlay itemPlay -> facts.itemObjectId = 800_000 + itemPlay.itemId();
			case QuestEvent.AttackNpc attack -> {
				world.seedInteractionNpc(attack.npcId(), 900_000 + attack.npcId());
				facts.targetless = true;
			}
			default -> facts.targetless = true;
		}
	}

	/**
	 * 为跨越未接取节点的场景选择一个最小满足的元数据起始条件组。
	 * Seeds one minimal satisfying metadata start-condition group for a transition crossing the unaccepted node.
	 *
	 * <p>生产 planner 会独立评估这些条件；测试场景不能只复制 transition 自身的条件，否则真实可接取
	 * 任务会被错误降级为 {@code NO_MATCH}。</p>
	 * <p>The production planner evaluates these conditions independently; a test scenario must not copy only
	 * transition-local conditions or a genuinely eligible quest is incorrectly reduced to {@code NO_MATCH}.</p>
	 */
	private void seedMetadataStartConditions(QuestTransition transition, QuestStatus sourceStatus) {
		if (sourceStatus != QuestStatus.NONE) {
			return;
		}
		List<com.aionemu.gameserver.questEngine.definition.QuestStartConditionGroup> groups =
			definition.definition().metadata().startConditionGroups();
		if (groups.isEmpty()) {
			facts.completed.addAll(definition.definition().metadata().prerequisites());
			return;
		}
		// OR 组只需选择一组满足；组内条件按 AND 语义全部种入事实。
		// For OR groups, satisfy one group; every condition in that group has AND semantics.
		for (var condition : groups.getFirst().conditions()) {
			switch (condition.type()) {
				case "finished" -> facts.completed.add(condition.questId());
				case "acquired" -> facts.active.add(condition.questId());
				case "equipped" -> {
					facts.equipmentCaptured = true;
					facts.equippedItems.put(condition.questId(), 1);
				}
				case "unfinished", "noacquired" -> {
					facts.completed.remove(condition.questId());
					facts.active.remove(condition.questId());
				}
				default -> unsupportedFacts = true;
			}
		}
	}

	@Override
	public QuestHeadlessClient.DispatchOutcome dispatch(ClientActionRequest request) {
		beginRequest(request);
		trace.add("ROUTER", request.event().type());
		QuestStatus beforeStatus = state.status();
		int beforeVars = state.packedVariables();
		QuestEventRouter.DispatchResult result = null;
		RuntimeException failure = null;
		try {
			QuestDispatchContract contract = request.kind() == ClientActionRequest.Kind.USE_ITEM
				|| request.kind() == ClientActionRequest.Kind.ITEM_PLAY
				? QuestDispatchContract.FIRST_NON_UNKNOWN : QuestDispatchContract.EXCLUSIVE;
			List<QuestEventIndex.Route> routes = eventIndex.routesFor(request.event(), definition.id());
			routeCandidateCount = routes.size();
			result = dispatcher.dispatch(request.event(), PLAYER_ID, definition.id(), contract);
			recordMatchedTransition(routes, result);
			failure = result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
				.filter(java.util.Objects::nonNull).findFirst().orElse(null);
		} catch (RuntimeException exception) {
			failure = exception;
		}
		List<ServerPacketObservation> packets = world.drainPackets();
		boolean changed = beforeStatus != state.status() || beforeVars != state.packedVariables();
		boolean handled = result != null && result.claimed();
		boolean failed = result != null && result.failed() || failure != null;
		return new QuestHeadlessClient.DispatchOutcome(handled, failed, changed, failure, packets);
	}

	private void recordMatchedTransition(List<QuestEventIndex.Route> routes, QuestEventRouter.DispatchResult result) {
		int attempted = Math.min(routes.size(), result.owners().size());
		for (int index = 0; index < attempted; index++) {
			QuestRouteResult routeResult = result.owners().get(index).result();
			if (routeResult == QuestRouteResult.HANDLED || routeResult == QuestRouteResult.BLOCKED
					|| routeResult == QuestRouteResult.FAILED) {
				matchedTransition = routes.get(index).transition();
				matchedTransitionCandidates = List.of(matchedTransition);
				matchedRouteResult = routeResult;
				return;
			}
		}
	}

	/**
	 * 在真实 CM 请求改变状态和背包前，按完整生产计划可行性筛选可归因 transition。
	 * Filters attributable transitions by full production-plan feasibility before a real CM request mutates state
	 * or inventory.
	 */
	public List<QuestTransition> attributableTransitions(ClientActionRequest request, QuestStatus status,
			int packedVariables) {
		java.util.Objects.requireNonNull(request, "request");
		java.util.Objects.requireNonNull(status, "status");
		QuestSnapshot before = snapshot(request.event(), status, packedVariables);
		return eventIndex.routesFor(request.event(), definition.id()).stream()
			.map(QuestEventIndex.Route::transition)
			.filter(transition -> QuestEvent.matches(transition.event(), request.event()))
			.filter(transition -> QuestMutationPlanner.plan(definition, before, request.event(), transition).isPresent())
			.findFirst().stream()
			.toList();
	}

	/**
	 * 根据协议执行前后的规范状态，为绕过测试桥并直接进入 QuestEngine 的真实 CM 请求恢复 route 归因。
	 * Restores route attribution for real CM requests that enter QuestEngine directly instead of the test bridge,
	 * using canonical state before and after protocol execution.
	 */
	public void attributeProtocolResult(ClientActionRequest request, QuestStatus beforeStatus, int beforePackedVariables,
			QuestRouteResult routeResult, List<QuestTransition> attributableBefore) {
		java.util.Objects.requireNonNull(request, "request");
		java.util.Objects.requireNonNull(beforeStatus, "beforeStatus");
		java.util.Objects.requireNonNull(routeResult, "routeResult");
		java.util.Objects.requireNonNull(attributableBefore, "attributableBefore");
		List<QuestEventIndex.Route> routes = eventIndex.routesFor(request.event(), definition.id());
		routeCandidateCount = routes.size();
		List<QuestTransition> sourceCandidates = List.copyOf(attributableBefore);
		List<QuestTransition> attributed = switch (routeResult) {
			case HANDLED, BLOCKED -> sourceCandidates.stream()
				.filter(transition -> transitionResultMatches(transition, beforePackedVariables,
					state.status(), state.packedVariables()))
				.toList();
			default -> sourceCandidates;
		};
		matchedTransitionCandidates = List.copyOf(attributed);
		matchedTransition = attributed.size() == 1 ? attributed.getFirst() : null;
		matchedRouteResult = routeResult;
	}

	private boolean transitionResultMatches(QuestTransition transition, int beforePackedVariables,
			QuestStatus status, int packedVariables) {
		NodeProjection projection = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.targetNode()))
			.map(com.aionemu.gameserver.questEngine.definition.QuestNode::projection)
			.findFirst().orElse(null);
		if (projection == null) return false;
		QuestStatus expectedStatus = projection.status();
		Map<String, Integer> expectedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(beforePackedVariables));
		Set<String> actionTouchedFields = new HashSet<>();
		for (QuestAction action : transition.actions()) {
			if (action instanceof QuestAction.SetStatus setStatus) {
				expectedStatus = setStatus.status();
			} else if (action instanceof QuestAction.SetVariable set) {
				expectedVariables.put(set.field(), set.value());
				actionTouchedFields.add(set.field());
			} else if (action instanceof QuestAction.IncrementVariable increment) {
				expectedVariables.merge(increment.field(), increment.delta(), Integer::sum);
				actionTouchedFields.add(increment.field());
			}
		}
		projection.variables().forEach((field, value) -> {
			if (!actionTouchedFields.contains(field)) expectedVariables.put(field, value);
		});
		return expectedStatus == status
			&& definition.definition().progressLayout().pack(expectedVariables) == packedVariables;
	}

	/** 直接执行一个内存世界事件，供审计器不经过页面点击地走正式 dispatcher。 / Dispatches one world event through the formal dispatcher for audit scenarios. */
	public QuestHeadlessClient.DispatchOutcome dispatchWorld(QuestEvent event) {
		return dispatch(ClientActionRequest.world(definition.id(), event));
	}

	/** 将定义事件具体化为生产入口实际接收的权威运行时事件并分发。 / Materializes a definition event into the authoritative runtime event received by production ingress and dispatches it. */
	public QuestHeadlessClient.DispatchOutcome dispatchPrepared() {
		if (preparedTransition == null) {
			throw new IllegalStateException("no transition has been prepared");
		}
		return dispatchWorld(materializeEvent(preparedTransition));
	}

	/**
	 * 将生产 XML transition 的声明事件具体化为运行时入口会收到的权威事件。
	 * Materializes a production-XML transition event into the authoritative event received by runtime ingress.
	 *
	 * @param transition 生产目录编译出的 transition / transition compiled from the production directory
	 * @return 可直接分发的运行时事件 / runtime event ready for dispatch
	 */
	public QuestEvent materializeEvent(QuestTransition transition) {
		java.util.Objects.requireNonNull(transition, "transition");
		return switch (transition.event()) {
			case QuestEvent.KillNpcSet kills -> new QuestEvent.KillNpc(
				kills.npcIds().stream().min(Integer::compareTo).orElseThrow());
			case QuestEvent.AttackNpc attack -> new QuestEvent.AttackNpc(attack.npcId(),
				new QuestNpcAttackFacts(PLAYER_ID, state.currentObjectId(), attack.npcId(),
					facts.npcCurrentHp, facts.npcMaxHp,
					state.worldId(), state.instanceId()));
			case QuestEvent.KillRanked ranked -> {
				facts.pvpFacts = pvpFacts(ranked.rankId(), state.worldId());
				yield new QuestEvent.KillRanked(ranked.rankId(), facts.pvpFacts);
			}
			case QuestEvent.KillInWorld kill -> {
				int worldId = kill.worldId() == 0 ? state.worldId() : kill.worldId();
				facts.pvpFacts = pvpFacts(1, worldId);
				yield new QuestEvent.KillInWorld(worldId, facts.pvpFacts);
			}
			default -> transition.event();
		};
	}

	private QuestSnapshot snapshot(Connection ignored, int playerId, int questId, QuestEvent event) {
		return snapshot(event, state.status(), state.packedVariables());
	}

	private QuestSnapshot snapshot(QuestEvent event, QuestStatus status, int packedVariables) {
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, definition.id(), status, packedVariables, facts.inventory,
			facts.currencies, true, true, facts.interactionObjectId(), facts.targetObjectId(), state.worldId(),
			state.instanceId(), state.x(), state.y(), state.z(), state.heading())
			.withStartEligibility(facts.startEligibility)
			.withStartingClass(PlayerClass.getStartingClassFor(facts.playerClass))
			.withPlayerClass(facts.playerClass)
			.withGender(facts.gender)
			.withWorldFacts(new QuestWorldFacts(facts.worldNpcIds, facts.zoneNames))
			.withTeamFacts(new QuestTeamFacts(facts.inGroup, false))
			.withCompleteCount(facts.completeCount)
			.withCompletedQuestIds(facts.completed)
			.withActiveQuestIds(facts.active)
			.withRace(facts.race)
			.withEventActive(facts.eventActive)
			.withEventActivities(facts.eventActivities);
		if (facts.equipmentCaptured) {
			snapshot = snapshot.withEquipmentFacts(new QuestEquipmentFacts(
				facts.itemSetParts, facts.equippedItems));
		}
		if (facts.membershipCaptured) {
			snapshot = snapshot.withMembershipFacts(new QuestMembershipFacts(facts.membershipPermissions));
		}
		if (facts.maxDp != null) {
			snapshot = snapshot.withMaxDp(facts.maxDp);
		}
		if (facts.craftCaptured) {
			snapshot = snapshot.withCraftFacts(new QuestCraftSnapshot(facts.knownRecipes,
				facts.craftingSkillLevels, 1600, 2, 1));
		}
		if (facts.pvpFacts != null) {
			snapshot = snapshot.withPvpFacts(facts.pvpFacts);
		}
		return facts.targetless ? snapshot.withTargetlessDialog() : snapshot;
	}

	private void executeAfterCommit(TypedQuestAfterCommitPort afterCommit, AfterCommitAction action,
			QuestSnapshot snapshot, QuestMutationPlan plan) {
		trace.add("AFTER_COMMIT", action.getClass().getSimpleName());
		if (action instanceof AfterCommitAction.PlayMovieRandom random && !random.movieIds().isEmpty()) {
			afterCommit.execute(new AfterCommitAction.PlayMovie(random.movieIds().getFirst(),
				com.aionemu.gameserver.questEngine.definition.QuestMovieType.CUTSCENE), snapshot, plan);
			return;
		}
		if (action instanceof AfterCommitAction.TeleportPlayer teleport
			&& teleport.instanceTarget() instanceof QuestInstanceTarget.NextAvailable) {
			// 副本分配依赖真实 InstanceService；内存世界用固定的当前夹具实例保持确定性。
			// Instance allocation depends on the real InstanceService; the in-memory world uses its deterministic fixture instance.
			action = new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.fixed(state.instanceId()),
				teleport.worldId(), teleport.x(), teleport.y(), teleport.z(), teleport.heading());
		}
		afterCommit.execute(action, snapshot, plan);
	}

	private void applyCondition(QuestCondition condition) {
		switch (condition) {
			case QuestCondition.StatusIs ignored -> { }
			case QuestCondition.StartEligible ignored -> facts.startEligibility = QuestStartEligibility.allowed();
			case QuestCondition.HasItem item -> setItem(item.itemId(), item.count(), item.expected());
			case QuestCondition.CurrencyAtLeast currency -> facts.currencies.put(currency.kind(), currency.amount());
			case QuestCondition.CurrencyBelow currency -> facts.currencies.put(currency.kind(), 0L);
			case QuestCondition.PlayerRaceIs race -> facts.race = race.race();
			case QuestCondition.PlayerClassIs playerClass -> facts.playerClass = playerClass.startingClass();
			case QuestCondition.AdvancedClassIs playerClass -> facts.playerClass = playerClass.playerClass();
			case QuestCondition.PlayerInGroup group -> facts.inGroup = group.expected();
			case QuestCondition.GenderIs gender -> facts.gender = gender.gender();
			case QuestCondition.WorldIs world -> {
				if (world.expected()) {
					facts.worldId = world.worldId();
					state.moveTo(world.worldId(), state.instanceId(), state.x(), state.y(), state.z(), state.heading());
				} else {
					int otherWorldId = world.worldId() == 110010000 ? 120010000 : 110010000;
					facts.worldId = otherWorldId;
					state.moveTo(otherWorldId, state.instanceId(), state.x(), state.y(), state.z(), state.heading());
				}
			}
			case QuestCondition.ZoneIs zone -> {
				if (zone.expected()) facts.zoneNames.add(zone.zone()); else facts.zoneNames.remove(zone.zone());
			}
			case QuestCondition.WorldNpcIs npc -> {
				if (npc.expected()) facts.worldNpcIds.add(npc.npcId()); else facts.worldNpcIds.remove(npc.npcId());
			}
			case QuestCondition.QuestsFinished quests -> facts.completed.addAll(quests.questIds());
			case QuestCondition.UnfinishedQuest quests -> facts.completed.removeAll(quests.questIds());
			case QuestCondition.NoAcquiredQuest quests -> {
				facts.completed.removeAll(quests.questIds());
				facts.active.removeAll(quests.questIds());
			}
			case QuestCondition.AcquiredQuest quests -> facts.active.addAll(quests.questIds());
			case QuestCondition.EventActive event -> {
				if (event.expected()) {
					if (event.questId() == 0) facts.eventActive = true;
					else facts.eventActivities.put(event.questId(), true);
				} else if (event.questId() == 0) {
					facts.eventActive = false;
				} else {
					facts.eventActivities.put(event.questId(), false);
				}
			}
			case QuestCondition.EquipmentSetEquipped equipment -> setEquipmentSet(equipment);
			case QuestCondition.EquippedItem item -> setEquippedItem(item);
			case QuestCondition.MembershipPermission permission -> {
				facts.membershipCaptured = true;
				if (permission.expected()) facts.membershipPermissions.add(permission.permission());
				else facts.membershipPermissions.remove(permission.permission());
			}
			case QuestCondition.DpAtMax ignored -> facts.dpAtMax = true;
			case QuestCondition.CompleteCountIs count ->
				facts.completeCount = count.expected() ? count.value() : count.value() == 0 ? 1 : 0;
			case QuestCondition.NpcHpBelowPercent hp -> {
				if (hp.percent() == 0) {
					unsupportedFacts = true;
				} else {
					facts.npcCurrentHp = Math.min(facts.npcCurrentHp, hp.percent() - 1);
				}
			}
			case QuestCondition.RecipeKnown recipe -> {
				facts.craftCaptured = true;
				if (recipe.expected()) facts.knownRecipes.add(recipe.recipeId());
				else facts.knownRecipes.remove(recipe.recipeId());
			}
			case QuestCondition.CanGrantCraftSkill skill -> {
				facts.craftCaptured = true;
				facts.craftingSkillLevels.merge(skill.skillId(), Math.max(0, skill.targetLevel() - 1), Math::max);
			}
			case QuestCondition.PvpVictimLevelDelta level -> {
				facts.minimumPvpDelta = facts.minimumPvpDelta == null ? level.minimumRecipientDelta()
					: Math.max(facts.minimumPvpDelta, level.minimumRecipientDelta());
				facts.maximumPvpDelta = facts.maximumPvpDelta == null ? level.maximumRecipientDelta()
					: Math.min(facts.maximumPvpDelta, level.maximumRecipientDelta());
			}
			case QuestCondition.PvpRecipientInZone zone -> facts.pvpRecipientZones.add(zone.zone());
			case QuestCondition.QuestVariableIs variable -> setVariable(variable.field(), variable.value());
			case QuestCondition.VariableAtLeast variable -> setVariableAtLeast(variable.field(), variable.value());
			case QuestCondition.VariableBelow variable -> setVariableBelow(variable.field(), variable.value());
			case QuestCondition.VariableSumIs sum -> setVariableSum(sum.fields(), sum.value(), false);
			case QuestCondition.VariableSumBelow sum -> setVariableSum(sum.fields(), sum.value(), true);
		}
	}

	private void finishConditionFacts() {
		if (facts.dpAtMax) {
			long currentDp = facts.currencies.computeIfAbsent(QuestRewardKind.DP, ignored -> 1_000L);
			if (currentDp > Integer.MAX_VALUE) {
				unsupportedFacts = true;
			} else {
				facts.maxDp = (int) currentDp;
			}
		}
		if (facts.minimumPvpDelta != null && facts.maximumPvpDelta != null
				&& facts.minimumPvpDelta > facts.maximumPvpDelta) {
			unsupportedFacts = true;
		}
	}

	private void setEquipmentSet(QuestCondition.EquipmentSetEquipped equipment) {
		facts.equipmentCaptured = true;
		if (equipment.expected()) {
			facts.itemSetParts.put(equipment.setIds().stream().min(Integer::compareTo).orElseThrow(), equipment.count());
			return;
		}
		int nonMatchingCount = equipment.count() == 0 ? 1 : 0;
		for (int setId : equipment.setIds()) {
			facts.itemSetParts.put(setId, nonMatchingCount);
		}
	}

	private void setEquippedItem(QuestCondition.EquippedItem item) {
		facts.equipmentCaptured = true;
		facts.equippedItems.put(item.itemId(), item.expected() ? item.count() : item.count() - 1);
	}

	private QuestPvpKillFacts pvpFacts(int victimRankId, int worldId) {
		int minimum = facts.minimumPvpDelta == null ? 0 : facts.minimumPvpDelta;
		int maximum = facts.maximumPvpDelta == null ? 0 : facts.maximumPvpDelta;
		int delta = minimum <= 0 && maximum >= 0 ? 0 : minimum > 0 ? minimum : maximum;
		long recipientLevel = 1L + Math.max(delta, 0);
		long victimLevel = 1L + Math.max(-delta, 0);
		if (recipientLevel > Integer.MAX_VALUE || victimLevel > Integer.MAX_VALUE) {
			throw new IllegalStateException("PvP level delta cannot be represented by positive integer levels");
		}
		return new QuestPvpKillFacts(PLAYER_ID, PLAYER_ID, PLAYER_ID + 1,
			(int) recipientLevel, (int) victimLevel, victimRankId, worldId,
			QuestPvpCreditSource.SOLO, facts.pvpRecipientZones);
	}

	private void setVariable(String field, int value) {
		Map<String, Integer> variables = new LinkedHashMap<>(definition.definition().progressLayout().unpack(state.packedVariables()));
		variables.put(field, value);
		try {
			state.project(state.status(), definition.definition().progressLayout().pack(variables));
		} catch (IllegalArgumentException invalidScenario) {
			unsupportedFacts = true;
		}
	}

	private void setVariableAtLeast(String field, int value) {
		int current = definition.definition().progressLayout().unpack(state.packedVariables()).getOrDefault(field, 0);
		setVariable(field, Math.max(current, value));
	}

	private void setVariableBelow(String field, int value) {
		var bitField = definition.definition().progressLayout().field(field);
		if (bitField == null) {
			unsupportedFacts = true;
			return;
		}
		if (value <= bitField.minValue()) {
			// compact counter 在 required=1 时会保留一个严格低于字段最小值的不可执行 continuing 分支。
			// A compact counter with required=1 retains an unreachable continuing branch below the field minimum.
			// 事实已经完整捕获；保留最小值，让正式 dispatcher 归因到可执行 sibling route。
			// Facts are fully captured; retain the minimum so the formal dispatcher attributes the executable sibling route.
			setVariable(field, bitField.minValue());
			return;
		}
		int current = definition.definition().progressLayout().unpack(state.packedVariables()).getOrDefault(field, 0);
		setVariable(field, current < value ? current : value - 1);
	}

	private void setVariableSum(List<String> fields, int value, boolean strictlyBelow) {
		Map<String, Integer> currentVariables = definition.definition().progressLayout().unpack(state.packedVariables());
		int currentSum = fields.stream().mapToInt(field -> currentVariables.getOrDefault(field, 0)).sum();
		if ((!strictlyBelow && currentSum == value) || (strictlyBelow && currentSum < value)) {
			return;
		}
		int target = strictlyBelow ? value - 1 : value;
		if (target < 0) {
			unsupportedFacts = true;
			return;
		}
		Map<String, Integer> variables = new LinkedHashMap<>(currentVariables);
		int remaining = target;
		for (int index = 0; index < fields.size(); index++) {
			String field = fields.get(index);
			var bitField = definition.definition().progressLayout().field(field);
			if (bitField == null) {
				unsupportedFacts = true;
				return;
			}
			int minimumRemaining = fields.subList(index + 1, fields.size()).stream()
				.map(definition.definition().progressLayout()::field)
				.filter(java.util.Objects::nonNull)
				.mapToInt(com.aionemu.gameserver.questEngine.definition.BitField::minValue)
				.sum();
			int assigned = Math.min(remaining - minimumRemaining, bitField.maxValue());
			if (assigned < bitField.minValue()) {
				unsupportedFacts = true;
				return;
			}
			variables.put(field, assigned);
			remaining -= assigned;
		}
		if (remaining != 0) {
			unsupportedFacts = true;
			return;
		}
		try {
			state.project(state.status(), definition.definition().progressLayout().pack(variables));
		} catch (IllegalArgumentException invalidScenario) {
			unsupportedFacts = true;
		}
	}

	private void setItem(int itemId, int count, boolean expected) {
		if (expected) facts.inventory.put(itemId, count); else facts.inventory.remove(itemId);
	}

	private void seedAfterCommit(AfterCommitAction action) {
		if (action instanceof AfterCommitAction.StartFollow follow) world.seedSlot(follow.slot(), 204830);
		if (action instanceof AfterCommitAction.StopFollow follow) world.seedSlot(follow.slot(), 204830);
		if (action instanceof AfterCommitAction.AttackTarget attack) world.seedSlot(attack.slot(), 204830);
		if (action instanceof AfterCommitAction.AttackNpcTemplate attack) {
			world.seedSlot(attack.slot(), 204830);
			world.seedInteractionNpc(attack.templateId(), 910_000 + attack.templateId());
		}
		if (action instanceof AfterCommitAction.StartFollowCurrentTargetToNpc follow) {
			world.seedWorldNpc(follow.npcId());
		}
		if (action instanceof AfterCommitAction.RemoveEffect remove) world.seedEffect(remove.effectId());
	}

	private void seedNpcFaction(QuestStatus sourceStatus) {
		QuestMetadata metadata = definition.definition().metadata();
		if (metadata.npcFactionId() == 0) {
			return;
		}
		ENpcFactionQuestState factionState = sourceStatus == QuestStatus.NONE
			? ENpcFactionQuestState.NOTING : ENpcFactionQuestState.START;
		world.seedNpcFaction(metadata.npcFactionId(), definition.id(), "MENTOR".equals(metadata.mentorType()),
			factionState);
	}

	private QuestTimerPort timerPort() {
		return new QuestTimerPort() {
			@Override public boolean startQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
					com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy policy) {
				trace.add("CLOCK", "timer:" + seconds); return true;
			}
			@Override public boolean startInvisibleTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
					com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy policy) { trace.add("CLOCK", "invisible:" + seconds); return true; }
			@Override public boolean cancelQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan,
					com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy.Identity identity) { trace.add("CLOCK", "cancel"); return true; }
		};
	}

	private QuestEffectPort effectPort(PlayerQuestEffectPort factionEffectPort) {
		return new QuestEffectPort() {
			@Override public boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId) { trace.add("WORLD", "morph:" + ascensionId); return true; }
			@Override public boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan, int flightTeleportId) { trace.add("WORLD", "flight:" + flightTeleportId); return true; }
			@Override public boolean setPlayerClass(QuestSnapshot snapshot, QuestMutationPlan plan, PlayerClass playerClass) {
				return world.changePlayerClass(playerClass);
			}
			@Override public boolean startNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
				boolean changed = factionEffectPort.startNpcFactionQuest(snapshot, plan, npcFactionId);
				if (changed) trace.add("WORLD", "npc-faction-start:" + npcFactionId + ":" + world.npcFactionState(npcFactionId));
				return changed;
			}
			@Override public boolean completeNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
				boolean changed = factionEffectPort.completeNpcFactionQuest(snapshot, plan, npcFactionId);
				if (changed) trace.add("WORLD", "npc-faction-complete:" + npcFactionId + ":" + world.npcFactionState(npcFactionId));
				return changed;
			}
			@Override public boolean abortNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
				boolean changed = factionEffectPort.abortNpcFactionQuest(snapshot, plan, npcFactionId);
				if (changed) trace.add("WORLD", "npc-faction-abort:" + npcFactionId + ":" + world.npcFactionState(npcFactionId));
				return changed;
			}
			@Override public boolean playerEmotion(QuestSnapshot snapshot, QuestMutationPlan plan,
					com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion emotion) {
				boolean sent = factionEffectPort.playerEmotion(snapshot, plan, emotion);
				if (sent) trace.add("PACKET", "player-emotion:" + emotion + ":" + snapshot.interactionObjectId());
				return sent;
			}
			@Override public boolean applyEffect(QuestSnapshot snapshot, QuestMutationPlan plan, int skillId,
					int durationMillis) {
				return factionEffectPort.applyEffect(snapshot, plan, skillId, durationMillis);
			}
			@Override public boolean removeEffect(QuestSnapshot snapshot, QuestMutationPlan plan, int effectId) {
				return factionEffectPort.removeEffect(snapshot, plan, effectId);
			}
		};
	}

	private QuestNpcPort npcPort() {
		return new QuestNpcPort() {
			@Override public boolean deleteInteractionNpc(QuestSnapshot snapshot, QuestMutationPlan plan, boolean scheduleRespawn) { trace.add("WORLD", "delete-interaction"); return true; }
			@Override public boolean deleteWorldNpcs(QuestSnapshot snapshot, QuestMutationPlan plan) { trace.add("WORLD", "delete-world"); return true; }
			@Override public boolean addNpcAggro(QuestSnapshot snapshot, QuestMutationPlan plan, int npcTemplateId, int damage) { trace.add("AI", "aggro:" + npcTemplateId); return true; }
		};
	}

	private QuestSystemMessagePort systemMessagePort(PlayerQuestSystemMessagePort playerSystemMessagePort) {
		return new QuestSystemMessagePort() {
			@Override public boolean send(QuestSnapshot snapshot, QuestMutationPlan plan,
					com.aionemu.gameserver.questEngine.definition.QuestSystemMessage message) {
				trace.add("PACKET", "system:" + message);
				return playerSystemMessagePort.send(snapshot, plan, message);
			}
			@Override public boolean send(QuestSnapshot snapshot, QuestMutationPlan plan,
					com.aionemu.gameserver.questEngine.definition.QuestSystemMessagePacket message) {
				trace.add("PACKET", "system-packet:" + message.messageId());
				return playerSystemMessagePort.send(snapshot, plan, message);
			}
		};
	}

	private QuestBroadcastPort broadcastPort() {
		return new QuestBroadcastPort() {
			@Override public boolean broadcastZoneMissionEnd(QuestSnapshot snapshot, QuestMutationPlan plan, int[] questIds) { trace.add("BROADCAST", java.util.Arrays.toString(questIds)); return true; }
			@Override public boolean scheduleEventQuestRefresh(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds, int[] questIds) { trace.add("CLOCK", "refresh:" + seconds); return true; }
		};
	}

	private Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit" -> { trace.add("TRANSACTION", "setAutoCommit:" + args[0]); yield null; }
				case "commit" -> { trace.add("TRANSACTION", "commit"); if (failCommit) { failCommit = false; throw new SQLException("deterministic commit failure"); } yield null; }
				case "rollback" -> { trace.add("TRANSACTION", "rollback"); yield null; }
				case "close" -> { trace.add("TRANSACTION", "close"); yield null; }
				case "toString" -> "quest-e2e-transaction";
				default -> primitiveDefault(method.getReturnType());
			});
	}

	private static Object primitiveDefault(Class<?> type) {
		if (!type.isPrimitive()) return null;
		if (type == boolean.class) return false;
		if (type == byte.class) return (byte) 0;
		if (type == short.class) return (short) 0;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0F;
		if (type == double.class) return 0D;
		if (type == char.class) return '\0';
		return null;
	}

	@Override
	public void close() {
		world.close();
	}

	private final class InMemoryActionPort implements QuestActionPort {
		private Map<Integer, Integer> before;
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) throws SQLException {
			trace.add("TRANSACTION", "preflight");
			before = new LinkedHashMap<>(facts.inventory);
			for (QuestAction action : actions) {
				if (action instanceof QuestAction.RemoveItem remove && !remove.removeAll()
						&& facts.inventory.getOrDefault(remove.itemId(), 0) < remove.count()) {
					throw new SQLException("insufficient item " + remove.itemId());
				}
			}
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) throws SQLException {
			trace.add("TRANSACTION", "actions");
			List<QuestAction> appliedActions = List.copyOf(actions);
			for (QuestAction action : actions) {
				switch (action) {
					case QuestAction.RemoveItem remove -> { if (remove.removeAll()) facts.inventory.remove(remove.itemId()); else facts.inventory.computeIfPresent(remove.itemId(), (id, count) -> count <= remove.count() ? null : count - remove.count()); }
					case QuestAction.GiveItem give -> facts.inventory.merge(give.itemId(), give.count(), Integer::sum);
					default -> { }
				}
			}
			return QuestTransactionParticipant.of(() -> {
				committedTransactionActions = appliedActions;
				trace.add("TRANSACTION", "actions-commit");
			},
				() -> { facts.inventory.clear(); facts.inventory.putAll(before); trace.add("TRANSACTION", "actions-rollback"); });
		}
	}

	private final class InMemoryStatePort implements QuestStatePort {
		private QuestMutationPlan pending;
		@Override public void apply(Connection connection, int playerId, QuestMutationPlan plan) { pending = plan; trace.add("STATE", "apply:" + plan.nextStatus()); }
		@Override public void publish(int playerId, QuestMutationPlan plan) {
			committedTransactionActions = plan.requiredActions();
			state.project(plan.nextStatus(), plan.nextPackedVariables());
			QuestState questState = world.player().getQuestStateList().getQuestState(plan.questId());
			if (questState == null) {
				world.player().getQuestStateList().addQuest(plan.questId(), new QuestState(plan.questId(), plan.nextStatus(), plan.nextPackedVariables(), 0, null, null, null));
			} else {
				questState.setQuestVar(plan.nextPackedVariables());
				questState.setStatus(plan.nextStatus());
			}
			trace.add("STATE", "publish:" + plan.nextStatus());
		}
		@Override public void rollback(int playerId, QuestMutationPlan plan) { pending = null; trace.add("STATE", "rollback"); }
	}

	private final class ScenarioFacts {
		private final Map<Integer, Integer> inventory = new LinkedHashMap<>();
		private final Map<QuestRewardKind, Long> currencies = new LinkedHashMap<>();
		private final Map<Integer, Integer> itemSetParts = new LinkedHashMap<>();
		private final Map<Integer, Integer> equippedItems = new LinkedHashMap<>();
		private final Set<QuestMembershipPermission> membershipPermissions = new HashSet<>();
		private final Set<Integer> knownRecipes = new HashSet<>();
		private final Map<Integer, Integer> craftingSkillLevels = new LinkedHashMap<>();
		private final Set<String> pvpRecipientZones = new HashSet<>();
		private final Set<Integer> completed = new HashSet<>();
		private final Set<Integer> active = new HashSet<>();
		private final Set<Integer> worldNpcIds = new HashSet<>();
		private final Set<String> zoneNames = new HashSet<>();
		private final Map<Integer, Boolean> eventActivities = new HashMap<>();
		private QuestStartEligibility startEligibility = QuestStartEligibility.allowed();
		private QuestPvpKillFacts pvpFacts;
		private Race race = Race.ELYOS;
		private PlayerClass playerClass = PlayerClass.GLADIATOR;
		private Gender gender = Gender.MALE;
		private boolean inGroup;
		private boolean equipmentCaptured;
		private boolean membershipCaptured;
		private boolean craftCaptured;
		private boolean dpAtMax;
		private boolean targetless;
		private int itemObjectId;
		private int worldId = 110010000;
		private int completeCount;
		private int npcCurrentHp = 100;
		private int npcMaxHp = 100;
		private Integer maxDp;
		private Integer minimumPvpDelta;
		private Integer maximumPvpDelta;
		private Boolean eventActive = true;

		private void reset() {
			inventory.clear(); currencies.clear(); itemSetParts.clear(); equippedItems.clear(); membershipPermissions.clear();
			knownRecipes.clear(); craftingSkillLevels.clear(); pvpRecipientZones.clear(); completed.clear(); active.clear();
			worldNpcIds.clear(); zoneNames.clear(); eventActivities.clear();
			startEligibility = QuestStartEligibility.allowed(); pvpFacts = null; race = Race.ELYOS;
			playerClass = PlayerClass.GLADIATOR; gender = Gender.MALE; inGroup = false; equipmentCaptured = false;
			membershipCaptured = false; craftCaptured = false; dpAtMax = false; targetless = false; itemObjectId = 0;
			worldId = 110010000; completeCount = 0; npcCurrentHp = 100; npcMaxHp = 100; maxDp = null;
			minimumPvpDelta = null; maximumPvpDelta = null; eventActive = true;
		}
		private int interactionObjectId() { return itemObjectId > 0 ? itemObjectId : state.currentObjectId(); }
		private int targetObjectId() { return interactionObjectId(); }
	}
}
