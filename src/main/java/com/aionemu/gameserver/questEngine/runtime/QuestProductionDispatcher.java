package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * 通过中央路由器和事务协调器执行正式 typed owner。
 * Executes live typed owners through the central router and transaction coordinator.
 */
@Slf4j(topic = "QUEST_RUNTIME")
public final class QuestProductionDispatcher {
	@FunctionalInterface
	interface ConnectionProvider {
		Connection open() throws SQLException;
	}

	private final QuestCatalogRegistry catalog;
	private final QuestEventIndex index;
	private final QuestDropIndex dropIndex;
	private final QuestEventRouter router;
	private final QuestExecutionCoordinator coordinator;
	private final QuestEventPort eventPort;
	private final QuestActionPort actionPort;
	private final QuestStatePort statePort;
	private final QuestAfterCommitPort afterCommitPort;
	private final ConnectionProvider connections;
	private final QuestAuditSink auditSink;
	private final QuestRuntimeMetrics metrics;

	private QuestProductionDispatcher(QuestCatalog catalog) {
		this.catalog = registry(catalog);
		this.index = new QuestEventIndex(this.catalog);
		this.dropIndex = new QuestDropIndex(this.catalog);
		this.auditSink = new LocalizedQuestAuditSink();
		this.metrics = new QuestRuntimeMetricsCollector();
		this.router = new QuestEventRouter(index, auditSink, metrics);
		this.coordinator = null;
		this.eventPort = null;
		this.actionPort = null;
		this.statePort = null;
		this.afterCommitPort = null;
		this.connections = null;
	}

	QuestProductionDispatcher(QuestCatalog catalog, QuestExecutionCoordinator coordinator,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort, ConnectionProvider connections,
			QuestAuditSink auditSink, QuestRuntimeMetrics metrics) {
		this.catalog = registry(catalog);
		this.index = new QuestEventIndex(this.catalog);
		this.dropIndex = new QuestDropIndex(this.catalog);
		this.auditSink = Objects.requireNonNull(auditSink, "auditSink");
		this.metrics = Objects.requireNonNull(metrics, "metrics");
		this.router = new QuestEventRouter(index, this.auditSink, this.metrics);
		this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
		this.eventPort = Objects.requireNonNull(eventPort, "eventPort");
		this.actionPort = Objects.requireNonNull(actionPort, "actionPort");
		this.statePort = Objects.requireNonNull(statePort, "statePort");
		this.afterCommitPort = Objects.requireNonNull(afterCommitPort, "afterCommitPort");
		this.connections = Objects.requireNonNull(connections, "connections");
	}

	/** 返回无 owner 的禁用 dispatcher，供引擎初始化使用。 Return an owner-free dispatcher for initialization. */
	public static QuestProductionDispatcher disabled() {
		return new QuestProductionDispatcher(new ImmutableQuestCatalog(List.of()));
	}

	/** 组装生产端口和中央执行链。 Compose production ports and the central execution chain. */
	public static QuestProductionDispatcher production(QuestCatalog catalog, QuestRuntimeComposition composition) {
		Objects.requireNonNull(composition, "composition");
		return new QuestProductionDispatcher(catalog, new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			composition.eventPort(), composition.actionPort(), composition.statePort(),
			composition.afterCommitPort(), DatabaseFactory::getConnection,
			new LocalizedQuestAuditSink(), new QuestRuntimeMetricsCollector());
	}

	private static QuestCatalogRegistry registry(QuestCatalog catalog) {
		Objects.requireNonNull(catalog, "catalog");
		return catalog instanceof QuestCatalogRegistry registry ? registry : new QuestCatalogRegistry(catalog);
	}

	/** 返回此 dispatcher 事件索引使用的精确不可变元数据/owner 快照。 / Returns the exact immutable metadata/owner snapshot used by this dispatcher's event index. */
	public QuestCatalogRegistry catalogRegistry() {
		return catalog;
	}

	/** 从与事件路由相同的不可变快照返回规范任务掉落。 / Returns canonical quest drops from the same immutable snapshot as event routing. */
	public List<QuestCatalogDrop> questDrops(int npcId) {
		return npcId <= 0 ? List.of() : dropIndex.dropsFor(npcId);
	}

	/** 判断 typed catalog 是否拥有任务。 Return whether the typed catalog owns the quest. */
	public boolean owns(int questId) {
		return questId > 0 && catalog.findExecutable(questId).isPresent();
	}

	/** 返回目录是否拥有指定事件键的路由。 / Returns whether the catalog has a route for the supplied event key. */
	public boolean hasRoutes(QuestEvent event) {
		Objects.requireNonNull(event, "event");
		return !index.routesFor(event).isEmpty();
	}

	/** 返回命名的类型化 owner 是否拥有指定事件键的路由。 / Returns whether the named typed owner has a route for the supplied event key. */
	public boolean hasRoutes(QuestEvent event, int questId) {
		Objects.requireNonNull(event, "event");
		if (questId <= 0) {
			return false;
		}
		return !index.routesFor(event, questId).isEmpty();
	}

	/** 返回排序后的正式 owner ID。 Return sorted production owner IDs. */
	public List<Integer> owners() {
		return catalog.executables().stream().map(CompiledQuestDefinition::id).sorted().toList();
	}

	/**
	 * 返回 item-play 入口所需的唯一动画时长。
	 * Returns the unique animation duration required by the item-play entry point.
	 *
	 * @param itemId 物品模板 ID / item template id
	 * @return 播放时长；没有 typed owner 时为空 / duration, or empty without a typed owner
	 */
	public OptionalInt itemPlayAnimationMillis(int itemId) {
		return index.itemPlayAnimationMillis(itemId);
	}

	/**
	 * {@code questId == 0} 时分发全部匹配 owner，否则只分发指定 owner。
	 * 数据库连接按需获取，无关事件不会访问连接池。
	 *
	 * Dispatches all matching owners when {@code questId == 0}, otherwise only the named owner.
	 * A connection is acquired lazily, so unrelated live events do not touch the database pool.
	 */
	public QuestEventRouter.DispatchResult dispatch(QuestEvent event, int playerId, int questId,
			QuestDispatchContract contract) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(contract, "contract");
		if (playerId <= 0) {
			throw new IllegalArgumentException("playerId must be positive");
		}
		if (questId < 0) {
			throw new IllegalArgumentException("questId must not be negative");
		}
		try (LazyConnection connection = new LazyConnection(connections)) {
			QuestRouteHandler handler = route -> execute(connection, playerId, event, route, contract);
			return questId == 0
				? router.dispatch(event, contract, handler)
				: router.dispatchOwner(event, questId, contract, handler);
		}
	}

	/**
	 * 向每个命名 owner 分发一个事件，并报告每个目标是否可路由且无失败。
	 * 路由条件不匹配是成功投递；路由缺失或执行失败则不是。
	 * 即使前面的目标失败，也会尝试每个目标。
	 * Dispatches one event to every named owner and reports whether every target was routable without failure.
	 * A routed condition mismatch is a successful delivery; an absent route or a failed execution is not.
	 * Every target is attempted even after an earlier target fails.
	 */
	public boolean dispatchOwners(QuestEvent event, int playerId, int[] questIds, QuestDispatchContract contract) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(questIds, "questIds");
		Objects.requireNonNull(contract, "contract");
		boolean success = questIds.length > 0;
		for (int questId : questIds) {
			QuestEventRouter.DispatchResult result = dispatch(event, playerId, questId, contract);
			success &= !result.owners().isEmpty() && !result.failed();
		}
		return success;
	}

	/** 通过 owner 的类型化获取路由执行服务器授权、无目标的共享接受。 / Executes a server-authorized, targetless share acceptance through the owner's typed acquisition route. */
	public boolean dispatchSharedQuestAccept(int playerId, int questId, int dialogId) {
		if (playerId <= 0 || questId <= 0 || (dialogId != 1002 && dialogId != 20000)) {
			return false;
		}
		CompiledQuestDefinition definition = catalog.findExecutable(questId).orElse(null);
		if (definition == null || definition.definition().metadata().cannotShare()) {
			return false;
		}
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.definition().nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		List<QuestTransition> candidates = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == dialogId)
			.filter(transition -> startsFromShareableState(transition, statuses))
			.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.START)
			.toList();
		if (candidates.isEmpty()) {
			return false;
		}
		QuestEvent.QuestDialog event = new QuestEvent.QuestDialog(dialogId);
		try (LazyConnection connection = new LazyConnection(connections)) {
			for (QuestTransition transition : candidates) {
				QuestRouteResult result = execute(connection, playerId, event,
					new QuestEventIndex.Route(questId, transition), QuestDispatchContract.EXCLUSIVE, true);
				if (result == QuestRouteResult.HANDLED || result == QuestRouteResult.BLOCKED) {
					return true;
				}
				if (result == QuestRouteResult.FAILED) {
					return false;
				}
			}
		}
		return false;
	}

	private static boolean startsFromShareableState(QuestTransition transition, Map<String, QuestStatus> statuses) {
		if (transition.sourceNode() != null) {
			QuestStatus status = statuses.get(transition.sourceNode());
			return status == QuestStatus.NONE || status == QuestStatus.COMPLETE;
		}
		return transition.conditions().stream().anyMatch(condition ->
			condition instanceof QuestCondition.StatusIs status
				&& (status.status() == QuestStatus.NONE || status.status() == QuestStatus.COMPLETE));
	}

	private QuestRouteResult execute(LazyConnection connection, int playerId, QuestEvent event,
		QuestEventIndex.Route route, QuestDispatchContract contract) {
		return execute(connection, playerId, event, route, contract, false);
	}

	private QuestRouteResult execute(LazyConnection connection, int playerId, QuestEvent event,
		QuestEventIndex.Route route, QuestDispatchContract contract, boolean sharedQuestAccept) {
		// 索引刻意用一个宽键路由一个 NPC 的所有对话。
		// 其他对话的候选是普通不匹配而非执行失败；保持非结论性，让路由器尝试下一个转换。
		// The index deliberately routes all dialogs for one NPC through one broad key.
		// A candidate with another dialog is an ordinary non-match, not an execution
		// failure; keep it non-conclusive so the router can try the next transition.
		if (!sharedQuestAccept && !QuestEvent.matches(route.transition().event(), event)) {
			return QuestRouteResult.UNKNOWN;
		}
		CompiledQuestDefinition definition = catalog.findExecutable(route.questId()).orElseThrow();
		try {
			QuestExecutionResult result = sharedQuestAccept
				? coordinator.executeSharedQuestAccept(connection.get(), playerId, definition,
					(QuestEvent.QuestDialog) event, route.transition(), eventPort, actionPort, statePort, afterCommitPort)
				: coordinator.execute(connection.get(), playerId, definition, event,
					route.transition(), eventPort, actionPort, statePort, afterCommitPort);
			if (!result.afterCommitFailures().isEmpty()) {
				log.warn(I18n.get("log.quest_engine.typed_after_commit_failures",
					definition.id(), result.afterCommitFailures().size()));
				for (RuntimeException failure : result.afterCommitFailures()) {
					try {
						auditSink.record(QuestEventRouter.auditEvent(event, contract, route,
							QuestRouteResult.HANDLED, failure));
					} catch (RuntimeException auditFailure) {
						metrics.onAuditFailure(route.questId(), auditFailure.getClass().getName());
					}
				}
			}
			return switch (result.status()) {
				case COMMITTED -> result.plan().requiredActions().stream()
					.anyMatch(QuestAction.BlockDefaultItemUse.class::isInstance)
					? QuestRouteResult.BLOCKED : QuestRouteResult.HANDLED;
				// 同一 owner 的一条路由可能被重复索引（例如同一物品用于不同任务状态）。
				// FIRST_NON_UNKNOWN 应继续尝试，直到某个 transition 真正匹配。
				// A route can be indexed more than once for one owner (for example
				// one item used at different quest states). Let FIRST_NON_UNKNOWN
				// continue until a transition actually matches.
				case NO_MATCH -> QuestRouteResult.UNKNOWN;
			};
		} catch (RuntimeException failure) {
			throw failure;
		} catch (Exception failure) {
			throw new QuestProductionExecutionException(definition.id(), failure);
		}
	}

	private static final class LazyConnection implements AutoCloseable {
		private final ConnectionProvider provider;
		private Connection connection;

		private LazyConnection(ConnectionProvider provider) {
			this.provider = provider;
		}

		private Connection get() {
			if (connection == null) {
				if (provider == null) {
					throw new IllegalStateException("typed production dispatcher is disabled");
				}
				try {
					connection = provider.open();
				} catch (SQLException failure) {
					throw new QuestProductionExecutionException(0, failure);
				}
			}
			return connection;
		}

		@Override
		public void close() {
			if (connection == null) {
				return;
			}
			try {
				connection.close();
			} catch (SQLException failure) {
				log.warn(I18n.get("log.quest_engine.typed_connection_close_failed"), failure);
			}
		}
	}

	private static final class QuestProductionExecutionException extends RuntimeException {
		private QuestProductionExecutionException(int questId, Throwable cause) {
			super("typed quest execution failed for owner " + questId, cause);
		}
	}
}
