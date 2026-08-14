package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.services.item.ItemService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/** 类型化任务必需与提交后能力的生产对象图。 / Production object graph for typed quest required and after-commit capabilities. */
public final class QuestRuntimeComposition {
	private final QuestAfterCommitPort afterCommitPort;
	private final QuestEventPort eventPort;
	private final QuestActionPort actionPort;
	private final QuestStatePort statePort;
	private final QuestPvpEventPort pvpEventPort;
	private final QuestProximityEventPort proximityEventPort;
	private final QuestAiPerceptionEventPort aiPerceptionEventPort;
	private final QuestHousingEventPort housingEventPort;
	private final QuestMovementEventPort movementEventPort;
	private final QuestPvpInstanceEventPort pvpInstanceEventPort;
	private final QuestSkillEventPort skillEventPort;
	private final QuestRecoveryEventPort recoveryEventPort;

	private QuestRuntimeComposition(QuestAfterCommitPort afterCommitPort, QuestEventPort eventPort,
		QuestActionPort actionPort, QuestStatePort statePort,
		QuestPvpEventPort pvpEventPort, QuestProximityEventPort proximityEventPort,
		QuestAiPerceptionEventPort aiPerceptionEventPort, QuestHousingEventPort housingEventPort,
		QuestMovementEventPort movementEventPort, QuestPvpInstanceEventPort pvpInstanceEventPort,
		QuestSkillEventPort skillEventPort, QuestRecoveryEventPort recoveryEventPort) {
		this.afterCommitPort = Objects.requireNonNull(afterCommitPort, "afterCommitPort");
		this.eventPort = Objects.requireNonNull(eventPort, "eventPort");
		this.actionPort = Objects.requireNonNull(actionPort, "actionPort");
		this.statePort = Objects.requireNonNull(statePort, "statePort");
		this.pvpEventPort = Objects.requireNonNull(pvpEventPort, "pvpEventPort");
		this.proximityEventPort = Objects.requireNonNull(proximityEventPort, "proximityEventPort");
		this.aiPerceptionEventPort = Objects.requireNonNull(aiPerceptionEventPort, "aiPerceptionEventPort");
		this.housingEventPort = Objects.requireNonNull(housingEventPort, "housingEventPort");
		this.movementEventPort = Objects.requireNonNull(movementEventPort, "movementEventPort");
		this.pvpInstanceEventPort = Objects.requireNonNull(pvpInstanceEventPort, "pvpInstanceEventPort");
		this.skillEventPort = Objects.requireNonNull(skillEventPort, "skillEventPort");
		this.recoveryEventPort = Objects.requireNonNull(recoveryEventPort, "recoveryEventPort");
	}

	public static QuestRuntimeComposition production() {
		return production(questId -> com.aionemu.gameserver.lifecycle.GameEngineServices.questEngine()
			.questCatalog().findMetadata(questId).orElse(null));
	}

	/** 构建元数据读取钉在一个不可变目录快照上的生产对象图。 / Builds a production graph whose metadata reads are pinned to one immutable catalog snapshot. */
	public static QuestRuntimeComposition production(QuestCatalog catalog) {
		Objects.requireNonNull(catalog, "catalog");
		return production(questId -> catalog.findMetadata(questId).orElse(null));
	}

	private static QuestRuntimeComposition production(IntFunction<QuestMetadata> metadata) {
		QuestPlayerPort players = playerId -> GameWorldBootstrapServices.world().findPlayer(playerId);
		QuestSpawnRegistry spawns = QuestSpawnRegistry.global();
		return new QuestRuntimeComposition(TypedQuestAfterCommitPort.fullyComposed(
			new PlayerQuestDialogPort(players),
			new PlayerQuestTeleportPort(players),
			new PlayerQuestMoviePort(players),
			new PlayerQuestSpawnPort(players, spawns),
			new PlayerQuestAiPort(players, spawns),
			new PlayerQuestTimerPort(players), new PlayerQuestStateSyncPort(players, metadata),
			new PlayerQuestStatsPort(players), new PlayerQuestEffectPort(players, metadata),
			new PlayerQuestNpcPort(GameWorldBootstrapServices::world),
			new PlayerQuestSystemMessagePort(players, metadata)),
			new PlayerQuestEventPort(players, new PlayerQuestStartEligibilityPort(players, metadata)),
			new LazyProductionActionPort(players), new LazyProductionStatePort(players, metadata),
			new PlayerQuestPvpEventPort(), new PlayerQuestProximityEventPort(),
			new PlayerQuestAiPerceptionEventPort(), new PlayerQuestHousingEventPort(),
			new PlayerQuestMovementEventPort(), new PlayerQuestPvpInstanceEventPort(),
			new PlayerQuestSkillEventPort(), new PlayerQuestRecoveryEventPort());
	}

	public QuestAfterCommitPort afterCommitPort() {
		return afterCommitPort;
	}

	/** 注入跨任务广播 port（生产侧在 dispatcher 构造后调用，打破循环依赖）。 / Injects the cross-quest broadcast port (called after dispatcher construction to break the composition cycle). */
	public void installBroadcastPort(QuestBroadcastPort broadcastPort) {
		if (afterCommitPort instanceof TypedQuestAfterCommitPort typed) {
			typed.withBroadcastPort(broadcastPort);
		}
	}

	public QuestEventPort eventPort() {
		return eventPort;
	}

	public QuestActionPort actionPort() {
		return actionPort;
	}

	public QuestStatePort statePort() {
		return statePort;
	}

	public QuestPvpEventPort pvpEventPort() {
		return pvpEventPort;
	}

	public QuestProximityEventPort proximityEventPort() {
		return proximityEventPort;
	}

	public QuestAiPerceptionEventPort aiPerceptionEventPort() {
		return aiPerceptionEventPort;
	}

	public QuestHousingEventPort housingEventPort() {
		return housingEventPort;
	}

	public QuestMovementEventPort movementEventPort() {
		return movementEventPort;
	}

	public QuestPvpInstanceEventPort pvpInstanceEventPort() {
		return pvpInstanceEventPort;
	}

	public QuestSkillEventPort skillEventPort() {
		return skillEventPort;
	}

	public QuestRecoveryEventPort recoveryEventPort() {
		return recoveryEventPort;
	}

	public void cleanupAll() {
		QuestRuntimeResources.cleanupAll();
	}

	/** DAOManager 由 bootstrap 初始化，之后 QuestEngine 才能在聚焦测试中构造。 / DAOManager is initialized by bootstrap after QuestEngine can be constructed in focused tests. */
	private static final class LazyProductionActionPort implements QuestActionPort {
		private final QuestPlayerPort players;
		private volatile QuestActionPort delegate;

		private LazyProductionActionPort(QuestPlayerPort players) {
			this.players = players;
		}

		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
				throws SQLException {
			delegate().preflight(connection, snapshot, actions);
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction> actions) throws SQLException {
			return delegate().apply(connection, snapshot, actions);
		}

		private QuestActionPort delegate() {
			QuestActionPort current = delegate;
			if (current != null) {
				return current;
			}
			synchronized (this) {
				if (delegate == null) {
					InventoryDAO inventoryDao = DAOManager.getDAO(InventoryDAO.class);
					PlayerDAO playerDao = DAOManager.getDAO(PlayerDAO.class);
					delegate = new CompositeQuestActionPort(
						new PlayerQuestInventoryPort(players, inventoryDao),
						new PlayerQuestCurrencyPort(players, inventoryDao,
							DAOManager.getDAO(AbyssRankDAO.class), playerDao),
						new PlayerQuestRewardPort(players, inventoryDao, playerDao, ItemService::addQuestItems,
							DAOManager.getDAO(PlayerTitleListDAO.class)),
						new PlayerQuestCraftPort(players, DAOManager.getDAO(PlayerRecipesDAO.class),
							DAOManager.getDAO(PlayerSkillListDAO.class)),
						new PlayerQuestEquipmentPort(players, inventoryDao,
							DAOManager.getDAO(com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO.class)));
				}
				return delegate;
			}
		}
	}

	private static final class LazyProductionStatePort implements QuestStatePort {
		private final QuestPlayerPort players;
		private final IntFunction<QuestMetadata> metadata;
		private volatile QuestStatePort delegate;

		private LazyProductionStatePort(QuestPlayerPort players, IntFunction<QuestMetadata> metadata) {
			this.players = players;
			this.metadata = metadata;
		}

		@Override
		public void apply(Connection connection, int playerId, QuestMutationPlan plan) throws SQLException {
			delegate().apply(connection, playerId, plan);
		}

		@Override
		public void publish(int playerId, QuestMutationPlan plan) {
			delegate().publish(playerId, plan);
		}

		@Override
		public void rollback(int playerId, QuestMutationPlan plan) {
			delegate().rollback(playerId, plan);
		}

		private QuestStatePort delegate() {
			QuestStatePort current = delegate;
			if (current != null) {
				return current;
			}
			synchronized (this) {
				if (delegate == null) {
					delegate = new PlayerQuestStatePort(players, DAOManager.getDAO(PlayerQuestListDAO.class), metadata);
				}
				return delegate;
			}
		}
	}
}
