package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家升级街机（Upgrade Arcade）数据访问抽象层。
 * DAO for player Upgrade Arcade event data persistence.
 *
 * @author Ranastic
 */
public abstract class PlayerUpgradeArcadeDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerUpgradeArcadeDAO.class.getName();
	}

	/**
	 * 加载玩家升级街机数据。
	 * Loads Upgrade Arcade data for the player.
	 *
	 * 玩家 / player
	 */
	public abstract void load(Player player);

	/**
	 * 新增玩家升级街机记录。
	 * Adds an Upgrade Arcade record for the player.
	 *
	 * player object id
	 * @param frenzy_meter 狂热仪表值 / frenzy meter value
	 * upgrade level
	 * @return 是否添加成功 / true if added
	 */
	public abstract boolean addUpgradeArcade(final int playerId, final int frenzy_meter, final int upgrade_lvl);

	/**
	 * 删除玩家升级街机记录。
	 * Deletes an Upgrade Arcade record for the player.
	 *
	 * player object id
	 * @param frenzy_meter 狂热仪表值 / frenzy meter value
	 * upgrade level
	 * @return 是否删除成功 / true if deleted
	 */
	public abstract boolean delUpgradeArcade(final int playerId, final int frenzy_meter, final int upgrade_lvl);

	/**
	 * 保存玩家升级街机数据。
	 * Stores Upgrade Arcade data for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean store(Player player);

	/**
	 * 按对象 ID 设置狂热仪表值。
	 * Sets frenzy meter by player object id.
	 *
	 * @param obj 玩家对象 ID / player object id
	 * @param frenzy_meter 狂热仪表值 / frenzy meter value
	 * @return 是否更新成功 / true if updated
	 */
	public abstract boolean setFrenzyMeterByObjId(final int obj, final int frenzy_meter);

	/**
	 * 按对象 ID 设置升级等级。
	 * Sets upgrade level by player object id.
	 *
	 * @param obj 玩家对象 ID / player object id
	 * upgrade level
	 * @return 是否更新成功 / true if updated
	 */
	public abstract boolean setUpgradeLvlByObjId(final int obj, final int upgrade_lvl);

	/**
	 * 按对象 ID 查询狂热仪表值。
	 * Returns frenzy meter by player object id.
	 *
	 * @param obj 玩家对象 ID / player object id
	 * @return 狂热仪表值 / frenzy meter value
	 */
	public abstract int getFrenzyMeterByObjId(final int obj);

	/**
	 * 按对象 ID 查询升级等级。
	 * Returns upgrade level by player object id.
	 *
	 * @param obj 玩家对象 ID / player object id
	 * upgrade level
	 */
	public abstract int getUpgradeLvlByObjId(final int obj);
}
