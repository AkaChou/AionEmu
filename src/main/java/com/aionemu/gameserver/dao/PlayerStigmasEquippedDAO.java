package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;

/**
 * 玩家已装备刺针（Stigma）数据访问抽象层。
 * DAO for player equipped stigma items persistence.
 */
public abstract class PlayerStigmasEquippedDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerStigmasEquippedDAO.class.getName();
	}

	/**
	 * 加载玩家已装备刺针列表。
	 * Loads the equipped stigma item list for the player.
	 *
	 * player object id
	 * @return 已装备刺针列表 / equipped stigma list
	 */
	public abstract PlayerEquippedStigmaList loadItemsList(int playerId);

	/**
	 * 保存玩家已装备刺针。
	 * Stores equipped stigma items for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeItems(Player player);

}
