package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.LadderPlayerPo;
import com.mybatisflex.core.service.IService;

/**
 * 天梯（Ladder）排位数据访问对象。
 * Ladder ranking data access object.
 *
 * @author wanke
 */
public interface LadderPlayerDao extends IService<LadderPlayerPo> {

	/**
	 * 刷新天梯排名。
	 * Updates ladder ranks.
	 */
	void updateRanks();

	/**
	 * 获取玩家排名。
	 * Gets a player's rank.
	 *
	 * @param player 玩家 / player
	 * @return 段位 / rank
	 */
	int getRank(Player player);

	/**
	 * 为玩家增加一场胜利。
	 * Adds a win for a player.
	 *
	 * @param player 玩家 / player
	 */
	void addWin(Player player);

	/**
	 * 为玩家增加一场失败。
	 * Adds a loss for a player.
	 *
	 * @param player 玩家 / player
	 */
	void addLoss(Player player);

	/**
	 * 为玩家增加一次离开。
	 * Adds a leave for a player.
	 *
	 * @param player 玩家 / player
	 */
	void addLeave(Player player);

	/**
	 * 为玩家增加评分。
	 * Adds rating for a player.
	 *
	 * @param player 玩家 / player
	 * @param rating 评分增量 / rating delta
	 */
	void addRating(Player player, int rating);

	/**
	 * 获取玩家评分。
	 * Gets rating for a player.
	 *
	 * @param player 玩家 / player
	 * @return 评分 / rating
	 */
	int getRating(Player player);
}
