package com.aionemu.gameserver.dao;

import com.aionemu.gameserver.model.house.HouseRegistry;

/**
 * 玩家房屋注册物品数据访问抽象层。
 * DAO for player house registered items persistence.
 */
public abstract class PlayerRegisteredItemsDAO implements IDFactoryAwareDAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerRegisteredItemsDAO.class.getName();
	}

	/**
	 * 加载玩家房屋注册表。
	 * Loads the house registry for the player.
	 *
	 * player object id
	 */
	public abstract void loadRegistry(int playerId);

	/**
	 * 保存玩家房屋注册表。
	 * Stores the house registry for the player.
	 *
	 * @param registry 房屋注册表 / house registry
	 * player object id
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean store(HouseRegistry registry, int playerId);

	/**
	 * 删除玩家全部已注册物品。
	 * Deletes all registered items for the player.
	 *
	 * player object id
	 * @return 是否删除成功 / true if deleted
	 */
	public abstract boolean deletePlayerItems(int playerId);

	/**
	 * 重置玩家房屋注册表。
	 * Resets the house registry for the player.
	 *
	 * player object id
	 */
	public abstract void resetRegistry(int playerId);
}
