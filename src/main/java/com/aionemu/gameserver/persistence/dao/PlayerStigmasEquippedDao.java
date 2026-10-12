package com.aionemu.gameserver.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;
import com.aionemu.gameserver.persistence.entity.PlayerStigmasEquippedPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家已装备刺针（Stigma）数据访问层。
 * Data access layer for player equipped stigma items.
 */
public interface PlayerStigmasEquippedDao extends IService<PlayerStigmasEquippedPo> {

	/**
	 * 加载玩家已装备刺针列表。
	 * Loads the equipped stigma item list for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 已装备刺针列表 / equipped stigma list
	 */
	PlayerEquippedStigmaList loadItemsList(int playerId);

	/**
	 * 保存玩家已装备刺针。
	 * Stores equipped stigma items for the player.
	 * @param player 玩家 / player
	 */
	void storeItems(Player player);

	/**
	 * 在调用方事务连接上持久化待处理的刺针列表变更。
	 * Persists pending stigma-list changes on a caller-owned transaction.
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param player 玩家 / player
	 * @throws SQLException 写入失败时 / on write failure
	 */
	void storeItemsInTransaction(Connection connection, Player player) throws SQLException;

	/**
	 * 在调用方事务提交后发布已持久化的条目状态。
	 * Publishes persisted entry states after the caller-owned transaction commits.
	 * @param player 玩家 / player
	 */
	void markStored(Player player);
}
