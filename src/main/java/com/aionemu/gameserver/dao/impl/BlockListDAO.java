package com.aionemu.gameserver.dao.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家屏蔽列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of BlockListDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author Ben
 */
@Slf4j
public class BlockListDAO extends com.aionemu.gameserver.dao.BlockListDAO {

	/** 加载黑名单 SQL / Load block list SQL */
	private static final String LOAD_QUERY = "SELECT blocked_player, reason FROM blocks WHERE player=?";
	/** 添加 blocked user sql / Add blocked user SQL */
	private static final String ADD_QUERY = "INSERT INTO blocks (player, blocked_player, reason) VALUES (?, ?, ?)";
	/** 删除被屏蔽用户 SQL / Delete blocked user SQL */
	private static final String DEL_QUERY = "DELETE FROM blocks WHERE player=? AND blocked_player=?";
	/** 更新屏蔽原因 SQL / Update block reason SQL */
	private static final String SET_REASON_QUERY = "UPDATE blocks SET reason=? WHERE player=? AND blocked_player=?";

	private final JdbcClient jdbcClient;

	public BlockListDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	public BlockListDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 添加被屏蔽玩家。
	 * Adds a blocked player.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param objIdToBlock 被屏蔽对象 ID / blocked object id
	 * @param reason 屏蔽原因 / block reason
	 * @return 是否添加成功 / whether add succeeded
	 */
	@Override
	public boolean addBlockedUser(final int playerObjId, final int objIdToBlock, final String reason) {
		try {
			int affected = jdbcClient.sql(ADD_QUERY)
				.param(playerObjId)
				.param(objIdToBlock)
				.param(reason)
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5b7ace3c98ed", playerObjId, objIdToBlock), e);
			return false;
		}
	}

	/**
	 * 移除被屏蔽玩家。
	 * Removes a blocked player.
	 *
	 * @param objIdToDelete 被移除对象 ID / object id to remove
	 * @return 是否删除成功 / whether delete succeeded
	 */
	@Override
	public boolean delBlockedUser(final int playerObjId, final int objIdToDelete) {
		try {
			int affected = jdbcClient.sql(DEL_QUERY)
				.param(playerObjId)
				.param(objIdToDelete)
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4721ed6a731b", playerObjId, objIdToDelete), e);
			return false;
		}
	}

	/**
	 * 加载玩家屏蔽列表。
	 * Loads the block list for a player.
	 * @param player 玩家 / player
	 * @return 屏蔽列表 / block list
	 */
	@Override
	public BlockList load(final Player player) {
		final Map<Integer, BlockedPlayer> list = new HashMap<>();

		try {
			PlayerDAO playerDao = DAOManager.getDAO(PlayerDAO.class);
			jdbcClient.sql(LOAD_QUERY)
				.param(player.getObjectId())
				.query((rs, rowNum) -> {
					int blockedOid = rs.getInt("blocked_player");
					String reason = rs.getString("reason");
					PlayerCommonData pcd = playerDao.loadPlayerCommonData(blockedOid);
					if (pcd == null) {
						log.error(I18n.get("log.53686495edb4", player.getName(), blockedOid));
					} else {
						list.put(blockedOid, new BlockedPlayer(pcd, reason));
					}
					return null;
				})
				.list();
		} catch (Exception e) {
			log.error(I18n.get("log.54a6187d47e3", player.getObjectId()), e);
		}
		return new BlockList(list);
	}

	/**
	 * 设置屏蔽原因。
	 * Sets the block reason.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param blockedPlayerObjId 被屏蔽对象 ID / blocked player object id
	 * @param reason 屏蔽原因 / block reason
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean setReason(final int playerObjId, final int blockedPlayerObjId, final String reason) {
		try {
			int affected = jdbcClient.sql(SET_REASON_QUERY)
				.param(reason)
				.param(playerObjId)
				.param(blockedPlayerObjId)
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.efb77a6431a9", playerObjId, blockedPlayerObjId), e);
			return false;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param databaseName 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
