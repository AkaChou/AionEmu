package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 免费转付费（F2P）状态数据访问对象。
 * Free-to-play (F2P) status data access object.
 *
 * @author Ranastic (Encom)
 * @author Ace
 */
public abstract class F2pDAO implements DAO {
	/**
	 * 加载玩家的 F2P 信息。
	 * Loads F2P info for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void loadF2pInfo(Player player);

	/**
	 * 存储玩家的 F2P 时间。
	 * Stores F2P time for a player.
	 *
	 * player ID
	 * F2P time
	 * whether successful
	 */
	public abstract boolean storeF2p(int playerId, int time);

	/**
	 * 更新玩家的 F2P 时间。
	 * Updates F2P time for a player.
	 *
	 * player ID
	 * F2P time
	 * whether successful
	 */
	public abstract boolean updateF2p(int playerId, int time);

	/**
	 * 删除玩家的 F2P 记录。
	 * Deletes F2P record for a player.
	 *
	 * player ID
	 * whether successful
	 */
	public abstract boolean deleteF2p(int playerId);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	public String getClassName() {
		return F2pDAO.class.getName();
	}
}
