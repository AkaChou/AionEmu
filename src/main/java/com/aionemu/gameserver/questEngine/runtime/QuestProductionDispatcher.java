package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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

	private final QuestCatalog catalog;
	private final QuestEventIndex index;
	private final QuestEventRouter router;
	private final QuestExecutionCoordinator coordinator;
	private final QuestEventPort eventPort;
	private final QuestActionPort actionPort;
	private final QuestStatePort statePort;
	private final QuestAfterCommitPort afterCommitPort;
	private final ConnectionProvider connections;

	private QuestProductionDispatcher(QuestCatalog catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.index = new QuestEventIndex(catalog);
		this.router = new QuestEventRouter(index, new LocalizedQuestAuditSink(), new QuestRuntimeMetricsCollector());
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
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.index = new QuestEventIndex(catalog);
		this.router = new QuestEventRouter(index, auditSink, metrics);
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

	/** 判断 typed catalog 是否拥有任务。 Return whether the typed catalog owns the quest. */
	public boolean owns(int questId) {
		return questId > 0 && catalog.find(questId).isPresent();
	}

	/** 返回排序后的正式 owner ID。 Return sorted production owner IDs. */
	public List<Integer> owners() {
		return catalog.all().stream().map(CompiledQuestDefinition::id).sorted().toList();
	}

	/**
	 * 返回 item-play 入口所需的唯一动画时长。
	 * Returns the unique animation duration required by the item-play entry point.
	 *
	 * @param itemId 物品模板 ID / item template id
	 * 播放时长；没有 typed owner 时为空 / duration, or empty without a typed owner
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
			QuestRouteHandler handler = route -> execute(connection, playerId, event, route);
			return questId == 0
				? router.dispatch(event, contract, handler)
				: router.dispatchOwner(event, questId, contract, handler);
		}
	}

	private QuestRouteResult execute(LazyConnection connection, int playerId, QuestEvent event,
		QuestEventIndex.Route route) {
		// The index deliberately routes all dialogs for one NPC through one broad key.
		// A candidate with another dialog is an ordinary non-match, not an execution
		// failure; keep it non-conclusive so the router can try the next transition.
		if (!QuestEvent.matches(route.transition().event(), event)) {
			return QuestRouteResult.UNKNOWN;
		}
		CompiledQuestDefinition definition = catalog.find(route.questId()).orElseThrow();
		try {
			QuestExecutionResult result = coordinator.execute(connection.get(), playerId, definition, event,
				route.transition(), eventPort, actionPort, statePort, afterCommitPort);
			if (!result.afterCommitFailures().isEmpty()) {
				log.warn(I18n.get("log.quest_engine.typed_after_commit_failures",
					definition.id(), result.afterCommitFailures().size()));
			}
			return switch (result.status()) {
				case COMMITTED -> QuestRouteResult.HANDLED;
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
