package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家衣柜（时装）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerWardrobeDAO using JdbcClient.
 *
 * @author Ranastic
 */
@Slf4j
@Repository
public class PlayerWardrobeDAO extends com.aionemu.gameserver.dao.PlayerWardrobeDAO {

	/**
	 * 插入或更新衣柜条目 / Insert or update a wardrobe entry
	 */
	private static final String INSERT_OR_UPDATE = "INSERT INTO `player_wardrobe` (`player_id`, `item_id`, `slot`, `reskin_count`) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE `item_id` = VALUES(`item_id`), `slot` = VALUES(`slot`)";
	/**
	 * 查询玩家衣柜全部条目 / Select all wardrobe entries for a player
	 */
	private static final String SELECT_QUERY = "SELECT `item_id`,`slot`,`reskin_count` FROM `player_wardrobe` WHERE `player_id`=?";
	/**
	 * 删除指定衣柜条目 / Delete a specific wardrobe entry
	 */
	private static final String DELETE_QUERY = "DELETE FROM `player_wardrobe` WHERE `player_id`=? AND `item_id`=?";
	/**
	 * 统计衣柜物品数量 / Count wardrobe items
	 */
	private static final String COUNT_QUERY = "SELECT COUNT(*) AS `size` FROM `player_wardrobe` WHERE `player_id`=?";
	/**
	 * 按槽位查询物品 ID / Select item id by slot
	 */
	private static final String SELECT_ITEM_BY_SLOT_QUERY = "SELECT `item_id` FROM `player_wardrobe` WHERE `player_id`=? AND `slot`=?";
	/**
	 * 按槽位查询重染次数 / Select reskin count by slot
	 */
	private static final String SELECT_RESKIN_BY_SLOT_QUERY = "SELECT `reskin_count` FROM `player_wardrobe` WHERE `player_id`=? AND `slot`=?";
	/**
	 * 按槽位更新重染次数 / Update reskin count by slot
	 */
	private static final String UPDATE_RESKIN_QUERY = "UPDATE player_wardrobe SET `reskin_count`=? WHERE `player_id`=? AND `slot`=?";

	private final JdbcClient jdbcClient;

	public PlayerWardrobeDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerWardrobeDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 判断当前数据库是否受本 DAO 支持。
	 * Checks whether the given database is supported by this DAO.
	 *
	 * @param databaseName 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}

	/**
	 * 加载玩家衣柜列表。
	 * Loads the player's wardrobe list.
	 *
	 * @param player 玩家 / player
	 * @return 衣柜列表 / wardrobe list
	 */
	@Override
	public PlayerWardrobeList load(Player player) {
		List<PlayerWardrobeEntry> w = new ArrayList<>();

		try {
			w = jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rset, _) -> {
					int itemId = rset.getInt("item_id");
					int slot = rset.getInt("slot");
					int reskin = rset.getInt("reskin_count");
					return new PlayerWardrobeEntry(itemId, slot, reskin, PersistentState.UPDATED);
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0c81ac00324b", player.getObjectId()), e);
		}
		return new PlayerWardrobeList(w);
	}

	/**
	 * 保存衣柜条目（插入或更新）。
	 * Stores a wardrobe entry (insert or update).
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param itemId   物品 ID / item id
	 * @param slot     槽位 / slot
	 * @param reskin   外观重置次数 / reskin count
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean store(int objectId, int itemId, int slot, int reskin) {
		try {
			jdbcClient.sql(INSERT_OR_UPDATE)
				.param(objectId)
				.param(itemId)
				.param(slot)
				.param(reskin)
				.update();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7fed95e25235", objectId), e);
			return false;
		}
	}

	/**
	 * 删除指定衣柜条目。
	 * Deletes a wardrobe entry.
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param itemId   物品 ID / item id
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean delete(int objectId, int itemId) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(objectId)
				.param(itemId)
				.update();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.06e3e78e5e3b", objectId), e);
			return false;
		}
	}

	/**
	 * 获取玩家衣柜物品数量。
	 * Returns the number of wardrobe items for the player.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 物品数量 / item count
	 */
	@Override
	public int getItemSize(int playerObjId) {
		try {
			Integer size = jdbcClient.sql(COUNT_QUERY)
				.param(playerObjId)
				.query((rs, _) -> rs.getInt("size"))
				.optional()
				.orElse(0);
			return size;
		} catch (DataAccessException e) {
			log.debug("Could not get item size for player {}", playerObjId, e);
			return 0;
		}
	}

	/**
	 * 按槽位获取衣柜物品 ID。
	 * Returns the wardrobe item id for the given slot.
	 *
	 * @param obj  玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 物品 ID，未找到时返回 0 / item id, or 0 if not found
	 */
	@Override
	public int getWardrobeItemBySlot(final int obj, int slot) {
		try {
			Integer itemId = jdbcClient.sql(SELECT_ITEM_BY_SLOT_QUERY)
				.param(obj)
				.param(slot)
				.query((rs, _) -> rs.getInt("item_id"))
				.optional()
				.orElse(0);
			return itemId;
		} catch (DataAccessException e) {
			log.debug("No wardrobe item found for player {}, slot {}", obj, slot);
			return 0;
		}
	}

	/**
	 * 按槽位获取重染次数。
	 * Returns the reskin count for the given slot.
	 *
	 * @param obj  玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 重染次数，未找到时返回 0 / reskin count, or 0 if not found
	 */
	@Override
	public int getReskinCountBySlot(final int obj, int slot) {
		try {
			Integer reskin = jdbcClient.sql(SELECT_RESKIN_BY_SLOT_QUERY)
				.param(obj)
				.param(slot)
				.query((rs, _) -> rs.getInt("reskin_count"))
				.optional()
				.orElse(0);
			return reskin;
		} catch (DataAccessException e) {
			log.debug("No reskin count found for player {}, slot {}", obj, slot);
			return 0;
		}
	}

	/**
	 * 按槽位设置重染次数。
	 * Sets the reskin count for the given slot.
	 *
	 * @param obj          玩家对象 ID / player object id
	 * @param slot         槽位 / slot
	 * @param reskin_count 外观重置次数 / reskin count
	 */
	@Override
	public void setReskinCountBySlot(int obj, int slot, int reskin_count) {
		try {
			jdbcClient.sql(UPDATE_RESKIN_QUERY)
				.param(reskin_count)
				.param(obj)
				.param(slot)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5acbf781683c", obj, slot), e);
		}
	}
}
