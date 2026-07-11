package com.aionemu.gameserver.dao;

import java.sql.Timestamp;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 天梯（Ladder）排位数据访问对象。
 * Ladder ranking data access object.
 *
 * @author wanke
 */
public abstract class LadderDAO implements DAO {
	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public final String getClassName() {
		return LadderDAO.class.getName();
	}

	/**
	 * 玩家天梯数据传输对象。
	 * Player ladder data transfer object.
	 */
	public static class PlayerLadderData {
		/** 玩家。 / Player. */
		private Player player;
		/** 评分。 / Rating. */
		private int rating;
		/** 排名。 / Rank. */
		private int rank;
		/** 胜场。 / Wins. */
		private int wins;
		/** 负场。 / Losses. */
		private int losses;
		/** 离开次数。 / Leaves. */
		private int leaves;
		/** 最后更新时间。 / Last update time. */
		private Timestamp lastUpdate;

		/**
		 * 构造玩家天梯数据。
		 * Constructs player ladder data.
		 *
		 * 玩家 / player
		 * rating
		 * rank
		 * wins
		 * losses
		 * leaves
		 * @param lastUpdate 最后更新时间 / last update time
		 */
		public PlayerLadderData(Player player, int rating, int rank, int wins, int losses, int leaves,
				Timestamp lastUpdate) {
			this.player = player;
			this.rating = rating;
			this.rank = rank;
			this.wins = wins;
			this.losses = losses;
			this.leaves = leaves;
			this.lastUpdate = lastUpdate;
		}

		/**
		 * 设置评分。 / Sets the rating.
		 *
		 * rating
		 */
		public void setRating(int rating) {
			this.rating = rating;
		}

		/**
		 * 获取评分。 / Gets the rating.
		 *
		 * rating
		 */
		public int getRating() {
			return rating;
		}

		/**
		 * 设置排名。 / Sets the rank.
		 *
		 * rank
		 */
		public void setRank(int rank) {
			this.rank = rank;
		}

		/**
		 * 获取排名。 / Gets the rank.
		 *
		 * rank
		 */
		public int getRank() {
			return rank;
		}

		/**
		 * 设置胜场。 / Sets wins.
		 *
		 * wins
		 */
		public void setWins(int wins) {
			this.wins = wins;
		}

		/**
		 * 获取胜场。 / Gets wins.
		 *
		 * wins
		 */
		public int getWins() {
			return wins;
		}

		/**
		 * 设置负场。 / Sets losses.
		 *
		 * losses
		 */
		public void setLosses(int losses) {
			this.losses = losses;
		}

		/**
		 * 获取负场。 / Gets losses.
		 *
		 * losses
		 */
		public int getLosses() {
			return losses;
		}

		/**
		 * 设置离开次数。 / Sets leaves.
		 *
		 * leaves
		 */
		public void setLeaves(int leaves) {
			this.leaves = leaves;
		}

		/**
		 * 获取离开次数。 / Gets leaves.
		 *
		 * leaves
		 */
		public int getLeaves() {
			return leaves;
		}

		/**
		 * 设置最后更新时间。 / Sets the last update time.
		 *
		 * @param lastUpdate 最后更新时间 / last update time
		 */
		public void setLastUpdate(Timestamp lastUpdate) {
			this.lastUpdate = lastUpdate;
		}

		/**
		 * 获取最后更新时间。 / Gets the last update time.
		 *
		 * @return 最后更新时间 / last update time
		 */
		public Timestamp getLastUpdate() {
			return lastUpdate;
		}

		/**
		 * 设置玩家。 / Sets the player.
		 *
		 * 玩家 / player
		 */
		public void setPlayer(Player player) {
			this.player = player;
		}

		/**
		 * 获取玩家。 / Gets the player.
		 *
		 * 玩家 / player
		 */
		public Player getPlayer() {
			return player;
		}
	}

	/**
	 * 刷新天梯排名。
	 * Updates ladder ranks.
	 */
	public abstract void updateRanks();

	/**
	 * 获取玩家排名。
	 * Gets a player's rank.
	 *
	 * 玩家 / player
	 * rank
	 */
	public abstract int getRank(Player player);

	/**
	 * 为玩家增加一场胜利。
	 * Adds a win for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void addWin(Player player);

	/**
	 * 为玩家增加一场失败。
	 * Adds a loss for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void addLoss(Player player);

	/**
	 * 为玩家增加一次离开。
	 * Adds a leave for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void addLeave(Player player);

	/**
	 * 为玩家增加评分。
	 * Adds rating for a player.
	 *
	 * 玩家 / player
	 * rating delta
	 */
	public abstract void addRating(Player player, int rating);

	/**
	 * 获取玩家胜场数。
	 * Gets wins for a player.
	 *
	 * 玩家 / player
	 * wins
	 */
	public abstract int getWins(Player player);

	/**
	 * 获取玩家负场数。
	 * Gets losses for a player.
	 *
	 * 玩家 / player
	 * losses
	 */
	public abstract int getLosses(Player player);

	/**
	 * 获取玩家离开次数。
	 * Gets leaves for a player.
	 *
	 * 玩家 / player
	 * leaves
	 */
	public abstract int getLeaves(Player player);

	/**
	 * 获取玩家评分。
	 * Gets rating for a player.
	 *
	 * 玩家 / player
	 * rating
	 */
	public abstract int getRating(Player player);

	/**
	 * 设置玩家胜场数。
	 * Sets wins for a player.
	 *
	 * 玩家 / player
	 * wins
	 */
	public abstract void setWins(Player player, int wins);

	/**
	 * 设置玩家负场数。
	 * Sets losses for a player.
	 *
	 * 玩家 / player
	 * losses
	 */
	public abstract void setLosses(Player player, int losses);

	/**
	 * 设置玩家离开次数。
	 * Sets leaves for a player.
	 *
	 * 玩家 / player
	 * leaves
	 */
	public abstract void setLeaves(Player player, int leaves);

	/**
	 * 设置玩家评分。
	 * Sets rating for a player.
	 *
	 * 玩家 / player
	 * rating
	 */
	public abstract void setRating(Player player, int rating);

	/**
	 * 获取玩家的天梯数据。
	 * Gets ladder data for a player.
	 *
	 * 玩家 / player
	 * ladder data
	 */
	public abstract PlayerLadderData getPlayerLadderData(Player player);
}
