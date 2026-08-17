package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * 将任务完成产生的高阶守护者晋升与任务状态一起持久化，
 * 并仅在提交成功后发布在线角色升级。
 * Persists quest-completion ArchDaeva promotion with quest state and publishes the live-player upgrade only after
 * commit.
 */
public final class PlayerQuestProgressionPort implements QuestProgressionPort {
	private static final int ARCHDAEVA_MIN_LEVEL = 65;
	private static final int ARCHDAEVA_LEVEL = 66;

	private final QuestPlayerPort players;
	private final ArchDaevaPromotionStore promotionStore;
	private final LongSupplier level66Exp;
	private final Consumer<Player> livePromoter;

	/**
	 * 创建生产晋升端口，使用角色 DAO 和权威经验表。
	 * Creates the production promotion port using the player DAO and authoritative EXP table.
	 *
	 * @param players 在线玩家查找端口 / online-player lookup port
	 * @param playerDao 角色持久化 DAO / player persistence DAO
	 */
	public PlayerQuestProgressionPort(QuestPlayerPort players, PlayerDAO playerDao) {
		this(players, playerDao::promoteArchDaevaInTransaction,
			() -> DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(ARCHDAEVA_LEVEL),
			player -> player.getCommonData().setArchDaeva());
	}

	PlayerQuestProgressionPort(QuestPlayerPort players, ArchDaevaPromotionStore promotionStore,
			LongSupplier level66Exp, Consumer<Player> livePromoter) {
		this.players = Objects.requireNonNull(players, "players");
		this.promotionStore = Objects.requireNonNull(promotionStore, "promotionStore");
		this.level66Exp = Objects.requireNonNull(level66Exp, "level66Exp");
		this.livePromoter = Objects.requireNonNull(livePromoter, "livePromoter");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.PromoteArchDaeva> promotions) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		validatePromotions(promotions);
		if (promotions.isEmpty()) {
			return;
		}
		Player player = requirePlayer(snapshot.playerId());
		validatePlayerLevel(player, snapshot.playerId());
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.PromoteArchDaeva> promotions) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		validatePromotions(promotions);
		if (promotions.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		Player player = requirePlayer(snapshot.playerId());
		validatePlayerLevel(player, snapshot.playerId());
		long minimumExp = level66Exp.getAsLong();
		if (minimumExp <= 0) {
			throw new SQLException("invalid level 66 start EXP: " + minimumExp);
		}
		promotionStore.promote(connection, snapshot.playerId(), minimumExp);
		return QuestTransactionParticipant.of(() -> livePromoter.accept(player), () -> { });
	}

	private Player requirePlayer(int playerId) throws SQLException {
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		return player;
	}

	private static void validatePlayerLevel(Player player, int playerId) throws SQLException {
		if (!player.isArchDaeva() && player.getLevel() < ARCHDAEVA_MIN_LEVEL) {
			throw new SQLException("player is below the ArchDaeva promotion level: " + playerId);
		}
	}

	private static void validatePromotions(List<QuestAction.PromoteArchDaeva> promotions) throws SQLException {
		Objects.requireNonNull(promotions, "promotions");
		if (promotions.size() > 1) {
			throw new SQLException("a quest transition may promote ArchDaeva only once");
		}
	}

	/**
	 * 调用方事务内写入晋升结果的函数式存储边界。
	 * Functional persistence boundary for writing a promotion result in the caller-owned transaction.
	 */
	@FunctionalInterface
	interface ArchDaevaPromotionStore {
		/**
		 * 在事务中写入晋升最低经验和高阶守护者标记。
		 * Writes minimum promotion EXP and the ArchDaeva flag in the transaction.
		 *
		 * @param connection 调用方拥有的 JDBC 连接 / JDBC connection owned by the caller
		 * @param playerId 玩家对象 ID / player object ID
		 * @param minimumExp 晋升所需的最低经验 / minimum EXP required for promotion
		 * @throws SQLException 持久化失败 / if persistence fails
		 */
		void promote(Connection connection, int playerId, long minimumExp) throws SQLException;
	}
}
