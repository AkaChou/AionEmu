package com.aionemu.gameserver.services.events.bg;

import java.util.Collection;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.LadderDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 战场天梯评分域：胜/负场与 Elo 评分变化的记录与批量更新。
 * Battleground ladder domain: win/loss recording, Elo delta and batch ladder updates.
 *
 * <p>本类从 {@link Battleground} 拆出；{@code Battleground} 保留同名 protected 委托方法，
 * 子类（DeathmatchBg / SoloSurvivorBg / TwoTeamBg / TwoTeamSmallBg）的调用方式不变。
 * Split out of {@link Battleground}; the base class keeps protected delegating methods, so subclass
 * call sites (DeathmatchBg / SoloSurvivorBg / TwoTeamBg / TwoTeamSmallBg) stay untouched.</p>
 */
final class BattlegroundLadder {

	/** Elo 评分 K 值 / Elo rating K-value. */
	static final int K_VALUE = 20;

	/**
	 * 读取天梯 DAO。
	 * Resolves the ladder DAO.
	 *
	 * @return 天梯 DAO / ladder DAO
	 */
	static LadderDAO dao() {
		return DAOManager.getDAO(LadderDAO.class);
	}

	/**
	 * 记录玩家胜场与评分变化。
	 * Records a player win and rating change.
	 *
	 * @param player       玩家 / player
	 * @param ratingChange 评分变化量 / rating delta
	 */
	static void playerWinMatch(Player player, int ratingChange) {
		LadderDAO dao = dao();
		dao.addWin(player);
		dao.addRating(player, Math.round(ratingChange / (dao.getRating(player) * 0.0015f)));
	}

	/**
	 * 记录玩家负场与评分变化。
	 * Records a player loss and rating change.
	 *
	 * @param player       玩家 / player
	 * @param ratingChange 评分变化量 / rating delta
	 */
	static void playerLoseMatch(Player player, int ratingChange) {
		LadderDAO dao = dao();
		dao.addLoss(player);
		dao.addRating(player, Math.round(ratingChange * (dao.getRating(player) * 0.0015f)));
	}

	/**
	 * 按胜负双方批量更新天梯。
	 * Batch ladder update for winners and losers.
	 *
	 * @param winner 胜方玩家集合 / winners
	 * @param loser  败方玩家集合 / losers
	 */
	static void performLadderUpdate(Collection<Player> winner, Collection<Player> loser) {
		LadderDAO dao = dao();
		int avgWinnerRating = 0;
		int avgLoserRating = 0;
		for (Player pl : winner) {
			dao.addWin(pl);
			avgWinnerRating += dao.getRating(pl);
		}
		for (Player pl : loser) {
			dao.addLoss(pl);
			avgLoserRating += dao.getRating(pl);
		}
		if (winner.size() > 0) {
			avgWinnerRating = avgWinnerRating / winner.size();
		}
		if (loser.size() > 0) {
			avgLoserRating = avgLoserRating / loser.size();
		}
		int ratingChange = calcRatingChange(avgWinnerRating, avgLoserRating);
		for (Player pl : winner) {
			dao.addRating(pl, +ratingChange);
		}
		for (Player pl : loser) {
			dao.addRating(pl, -ratingChange);
		}
	}

	/**
	 * 计算 Elo 评分变化量。
	 * Calculates the Elo rating change.
	 *
	 * @param ratingA 胜方平均评分 / rating A
	 * @param ratingB 败方平均评分 / rating B
	 * @return 评分变化量 / delta
	 */
	static int calcRatingChange(int ratingA, int ratingB) {
		return (int) Math.round(K_VALUE * (1 / (1 + Math.pow(10, ((float) ratingB - (float) ratingA) / 400))));
	}

	private BattlegroundLadder() {
	}
}
