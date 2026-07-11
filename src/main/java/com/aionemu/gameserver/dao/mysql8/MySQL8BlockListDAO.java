package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 玩家屏蔽列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of BlockListDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Ben
 */
@Slf4j
public class MySQL8BlockListDAO extends BlockListDAO {

	/** 加载黑名单 SQL / Load block list SQL */
	private static final String LOAD_QUERY = "SELECT blocked_player, reason FROM blocks WHERE player=?";
	/** 添加 blocked user sql / Add blocked user SQL */
	private static final String ADD_QUERY = "INSERT INTO blocks (player, blocked_player, reason) VALUES (?, ?, ?)";
	/** 删除被屏蔽用户 SQL / Delete blocked user SQL */
	private static final String DEL_QUERY = "DELETE FROM blocks WHERE player=? AND blocked_player=?";
	/** 更新屏蔽原因 SQL / Update block reason SQL */
	private static final String SET_REASON_QUERY = "UPDATE blocks SET reason=? WHERE player=? AND blocked_player=?";

	/**
	 * 添加被屏蔽玩家。
	 * Adds a blocked player.
	 *
	 * player object id
	 * @param objIdToBlock 被屏蔽对象 ID / blocked object id
	 * block reason
	 *
	 * @return 是否添加成功 / whether add succeeded
	 */
	@Override
	public boolean addBlockedUser(final int playerObjId, final int objIdToBlock, final String reason) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(ADD_QUERY)) {

			stmt.setInt(1, playerObjId);
			stmt.setInt(2, objIdToBlock);
			stmt.setString(3, reason);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.5b7ace3c98ed", playerObjId, objIdToBlock, e));
			return false;
		}
	}

	/**
	 * 移除被屏蔽玩家。
	 * Removes a blocked player.
	 *
	 * player object id
	 *
	 * @param objIdToDelete 被移除对象 ID / object id to remove
	 * @param objIdToDelete @return 是否删除成功 / whether delete succeeded
	 */
	@Override
	public boolean delBlockedUser(final int playerObjId, final int objIdToDelete) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DEL_QUERY)) {

			stmt.setInt(1, playerObjId);
			stmt.setInt(2, objIdToDelete);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.4721ed6a731b", playerObjId, objIdToDelete, e));
			return false;
		}
	}

	/**
	 * 加载玩家屏蔽列表。
	 * Loads the block list for a player.
	 *
	 * 玩家 / player
	 * block list
	 */
	@Override
	public BlockList load(final Player player) {
		final Map<Integer, BlockedPlayer> list = new HashMap<Integer, BlockedPlayer>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				PlayerDAO playerDao = DAOManager.getDAO(PlayerDAO.class);
				while (rset.next()) {
					int blockedOid = rset.getInt("blocked_player");
					PlayerCommonData pcd = playerDao.loadPlayerCommonData(blockedOid);
					if (pcd == null) {
						log.error(I18n.get("log.53686495edb4", player.getName(), blockedOid));
					} else {
						list.put(blockedOid, new BlockedPlayer(pcd, rset.getString("reason")));
					}
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.54a6187d47e3", player.getObjectId(), e));
		}
		return new BlockList(list);
	}

	/**
	 * 设置屏蔽原因。
	 * Sets the block reason.
	 *
	 * player object id
	 * @param blockedPlayerObjId 被屏蔽对象 ID / blocked player object id
	 * block reason
	 *
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean setReason(final int playerObjId, final int blockedPlayerObjId, final String reason) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SET_REASON_QUERY)) {

			stmt.setString(1, reason);
			stmt.setInt(2, playerObjId);
			stmt.setInt(3, blockedPlayerObjId);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.efb77a6431a9", playerObjId, blockedPlayerObjId, e));
			return false;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
