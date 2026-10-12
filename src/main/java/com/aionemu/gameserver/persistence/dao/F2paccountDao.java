package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.F2paccountPo;
import com.mybatisflex.core.service.IService;

/**
 * 免费转付费（F2P）状态数据访问层。
 * Free-to-play (F2P) status data access layer.
 * @author Ranastic (Encom)
 * @author Ace
 */
public interface F2paccountDao extends IService<F2paccountPo> {

	/**
	 * 加载玩家的 F2P 信息。
	 * Loads F2P info for a player.
	 * @param player 玩家 / player
	 */
	void loadF2pInfo(Player player);

	/**
	 * 存储玩家的 F2P 时间。
	 * Stores F2P time for a player.
	 * @param playerId 玩家 ID / player ID
	 * @param time F2P 时间 / F2P time
	 * @return 是否成功 / whether successful
	 */
	boolean storeF2p(int playerId, int time);

	/**
	 * 更新玩家的 F2P 时间。
	 * Updates F2P time for a player.
	 * @param playerId 玩家 ID / player ID
	 * @param time F2P 时间 / F2P time
	 * @return 是否成功 / whether successful
	 */
	boolean updateF2p(int playerId, int time);

	/**
	 * 删除玩家的 F2P 记录。
	 * Deletes F2P record for a player.
	 * @param playerId 玩家 ID / player ID
	 * @return 是否成功 / whether successful
	 */
	boolean deleteF2p(int playerId);
}
