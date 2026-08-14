package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;

/**
 * 玩家盗贼公会状态数据访问抽象层。
 * DAO for player thieves guild status persistence.
 *
 * @author Rinzler (Encom)
 */
public abstract class PlayerThievesListDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerThievesListDAO.class.getName();
	}

	/**
	 * 加载玩家盗贼公会状态。
	 * Loads thieves guild status for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @return 盗贼状态列表 / thieves status list
	 */
	public abstract ThievesStatusList loadThieves(int playerId);

	/**
	 * 保存新的盗贼公会记录。
	 * Saves a new thieves guild record.
	 *
	 * @param thieves 窃贼状态 / thieves status
	 * @return 是否保存成功 / true if saved
	 */
	public abstract boolean saveNewThieves(ThievesStatusList thieves);

	/**
	 * 更新盗贼公会状态。
	 * Stores/updates thieves guild status.
	 *
	 * @param thieves 窃贼状态 / thieves status
	 */
	public abstract void storeThieves(ThievesStatusList thieves);
}
