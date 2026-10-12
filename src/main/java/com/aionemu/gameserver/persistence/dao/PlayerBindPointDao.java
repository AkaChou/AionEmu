package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerBindPointPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家绑定点（回城点）数据访问层。
 * Data access layer for player bind points.
 * @author evilset
 */
public interface PlayerBindPointDao extends IService<PlayerBindPointPo> {

	/**
	 * 加载玩家绑定点。
	 * Loads the player's bind point.
	 * @param player 玩家 / player
	 */
	void loadBindPoint(Player player);

	/**
	 * 插入玩家绑定点记录。
	 * Inserts a player bind-point record.
	 * @param player 玩家 / player
	 * @return 若成功则为 true / true if successful
	 */
	boolean insertBindPoint(Player player);

	/**
	 * 更新玩家绑定点记录。
	 * Updates a player bind-point record.
	 * @param player 玩家 / player
	 * @return 若成功则为 true / true if successful
	 */
	boolean updateBindPoint(Player player);

	/**
	 * 持久化玩家绑定点（插入或更新）。
	 * Persists the player's bind point (insert or update).
	 * @param player 玩家 / player
	 * @return 若成功则为 true / true if successful
	 */
	boolean store(Player player);
}
