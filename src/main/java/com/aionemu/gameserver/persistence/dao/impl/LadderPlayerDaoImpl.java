package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Function;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.LadderPlayerDao;
import com.aionemu.gameserver.persistence.entity.LadderPlayerPo;
import com.aionemu.gameserver.persistence.mapper.LadderPlayerMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 天梯排行榜数据访问实现。
 * Data access implementation for ladder rankings.
 *
 * @author wanke
 */
@Slf4j
@Service
@Lazy
public class LadderPlayerDaoImpl extends ServiceImpl<LadderPlayerMapper, LadderPlayerPo> implements LadderPlayerDao {

	/** 天梯评分首次入库的基准分。 / Base rating used when a ladder row is first created. */
	private static final int BASE_RATING = 1000;

	/** last_rank 刷新间隔（毫秒）。 / Refresh interval for last_rank, in milliseconds. */
	private static final long RANK_REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L;

	/**
	 * 增加玩家天梯胜利场次。
	 * Adds a ladder win for the player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void addWin(Player player) {
		addPlayerLadderData(player, "wins", 1);
	}

	/**
	 * 增加玩家天梯失败场次。
	 * Adds a ladder loss for the player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void addLoss(Player player) {
		addPlayerLadderData(player, "losses", 1);
	}

	/**
	 * 增加玩家天梯离开（逃跑）场次。
	 * Adds a ladder leave for the player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void addLeave(Player player) {
		addPlayerLadderData(player, "leaves", 1);
	}

	/**
	 * 增加玩家天梯评分。
	 * Adds rating points to the player's ladder score.
	 *
	 * @param player 玩家 / player
	 * @param rating 增加的评分 / rating delta to add
	 */
	@Override
	public void addRating(Player player, int rating) {
		addPlayerLadderData(player, "rating", rating);
	}

	/**
	 * 获取玩家天梯评分；无记录时默认 1000。
	 * Returns the player's ladder rating; defaults to 1000 when unset.
	 *
	 * @param player 玩家 / player
	 * @return 评级值 / rating value
	 */
	@Override
	public int getRating(Player player) {
		int rating = getPlayerLadderData(player, LadderPlayerPo::getRating);
		return rating == 0 ? BASE_RATING : rating;
	}

	/**
	 * 获取玩家当前天梯排名。
	 * Returns the player's current ladder rank.
	 *
	 * @param player 玩家 / player
	 * @return 排名位置 / rank position
	 */
	@Override
	public int getRank(Player player) {
		return getPlayerLadderData(player, LadderPlayerPo::getRank);
	}

	/**
	 * 重新计算并更新全部玩家的天梯排名。
	 * Recalculates and updates ladder ranks for all players with records.
	 */
	@Override
	public void updateRanks() {
		List<LadderPlayerPo> players;
		try {
			players = mapper.selectListByQuery(QueryWrapper.create()
				.where(LadderPlayerPo::getWins).gt(0)
				.or(LadderPlayerPo::getLosses).gt(0)
				.or(LadderPlayerPo::getLeaves).gt(0)
				.orderBy(LadderPlayerPo::getRating, false)
				.orderBy(LadderPlayerPo::getWins, false)
				.orderBy(LadderPlayerPo::getPlayerId, true));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9a75439aa03e"), e);
			return;
		}

		players.sort((o1, o2) -> {
			int result = Integer.compare(intValue(o2.getRating()), intValue(o1.getRating()));
			if (result != 0) {
				return result;
			}
			result = Integer.compare(intValue(o2.getWins()), intValue(o1.getWins()));
			if (result != 0) {
				return result;
			}
			return Integer.compare(o1.getPlayerId(), o2.getPlayerId());
		});

		if (players.isEmpty()) {
			return;
		}

		try {
			int i = 1;
			for (LadderPlayerPo plInfo : players) {
				int playerId = plInfo.getPlayerId();
				Timestamp update = plInfo.getLastUpdate();

				if (update == null || update.getTime() == 0
					|| (System.currentTimeMillis() - update.getTime()) > RANK_REFRESH_INTERVAL_MS) {
					UpdateChain.of(LadderPlayerPo.class)
						.set(LadderPlayerPo::getLastRank, intValue(plInfo.getRank()))
						.set(LadderPlayerPo::getLastUpdate, new Timestamp(System.currentTimeMillis()))
						.where(LadderPlayerPo::getPlayerId).eq(playerId)
						.update();
				}

				UpdateChain.of(LadderPlayerPo.class)
					.set(LadderPlayerPo::getRank, i)
					.where(LadderPlayerPo::getPlayerId).eq(playerId)
					.update();
				i++;
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7f7d50101121"), e);
		}
	}

	/**
	 * 按分数类型累加天梯数据；评分首次入库以 1000 + 增量起算。
	 * Adds ladder data for the given score column; rating starts from 1000 + delta on first insert.
	 *
	 * @param player 玩家 / player
	 * @param column 分数列名（wins/losses/leaves/rating） / score column name
	 * @param value 增量 / delta
	 */
	private void addPlayerLadderData(Player player, String column, int value) {
		int playerId = player.getObjectId();
		try {
			switch (column) {
				case "wins" -> mapper.addWins(playerId, value);
				case "losses" -> mapper.addLosses(playerId, value);
				case "leaves" -> mapper.addLeaves(playerId, value);
				case "rating" -> mapper.addRating(playerId, BASE_RATING + value, value);
				default -> {
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.58ec4d70c9e9", player.getObjectId()), e);
		}
	}

	/**
	 * 读取玩家天梯的单个分数列；无记录时为 0。
	 * Reads one ladder score column for the player; 0 when no row exists.
	 *
	 * @param player 玩家 / player
	 * @param accessor 分数列读取器 / score column accessor
	 * @return 分数值 / score value
	 */
	private int getPlayerLadderData(Player player, Function<LadderPlayerPo, Integer> accessor) {
		int playerId = player.getObjectId();
		try {
			LadderPlayerPo po = mapper.selectOneById(playerId);
			return po == null ? 0 : intValue(accessor.apply(po));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.960d9c9f47e1", playerId), e);
			return 0;
		}
	}

	/**
	 * 空值安全取整：与原 rs.getInt 的 NULL→0 语义一致。
	 * Null-safe int, matching the original rs.getInt NULL→0 semantics.
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
