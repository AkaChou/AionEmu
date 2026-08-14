package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 玩家已装备刺针（Stigma）数据访问抽象层。
 * DAO for player equipped stigma items persistence.
 */
public abstract class PlayerStigmasEquippedDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerStigmasEquippedDAO.class.getName();
	}

	/**
	 * 加载玩家已装备刺针列表。
	 * Loads the equipped stigma item list for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @return 已装备刺针列表 / equipped stigma list
	 */
	public abstract PlayerEquippedStigmaList loadItemsList(int playerId);

	/**
	 * 保存玩家已装备刺针。
	 * Stores equipped stigma items for the player.
	 *
	 * @param player 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeItems(Player player);

	/** Persists pending stigma-list changes on a caller-owned transaction. */
	public abstract void storeItemsInTransaction(Connection connection, Player player) throws SQLException;

	/** Publishes persisted entry states after the caller-owned transaction commits. */
	public void markStored(Player player) {
		player.getEquipedStigmaList().markStored();
	}

}
