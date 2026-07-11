package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 引导（Guide）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of GuideDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author xTz
 */
@Slf4j
public class MySQL8GuideDAO extends GuideDAO {


	/** 删除引导 SQL / Delete guide SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `guides` WHERE `guide_id`=?";
	/** 查询玩家引导 SQL / Select player guides SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM `guides` WHERE `player_id`=?";
	/** 查询单条引导 SQL / Select single guide SQL*/
	private static final String SELECT_GUIDE_QUERY = "SELECT * FROM `guides` WHERE `guide_id`=? AND `player_id`=?";
	/** 插入引导 SQL / Insert guide SQL*/
	private static final String INSERT_QUERY = "INSERT INTO guides(guide_id, title, player_id) VALUES (?, ?, ?)";
	/** 查询已用引导 ID SQL / Select used guide ids SQL*/
	private static final String SELECT_USED_IDS_QUERY = "SELECT guide_id FROM guides";

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param arg0 数据库名 / database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}

	/**
	 * 删除引导。
	 * Deletes a guide.
	 *
	 * guide id
	 * whether successful
	 */
	@Override
	public boolean deleteGuide(int guide_id) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

			stmt.setInt(1, guide_id);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.257617627810", guide_id, e));
			return false;
		}
	}

	/**
	 * 加载玩家全部引导。
	 * Loads all guides of a player.
	 *
	 * player id
	 * guide list
	 */
	@Override
	public List<Guide> loadGuides(int playerId) {
		final List<Guide> guides = new ArrayList<Guide>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, playerId);

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int guide_id = rset.getInt("guide_id");
					int player_id = rset.getInt("player_id");
					String title = rset.getString("title");
					Guide guide = new Guide(guide_id, player_id, title);
					guides.add(guide);
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.040d5371000d", playerId, " from DB", e));
		}
		return guides;
	}

	/**
	 * 加载单条引导。
	 * Loads a single guide.
	 *
	 * player id
	 * guide id
	 * guide
	 */
	@Override
	public Guide loadGuide(int player_id, int guide_id) {
		Guide guide = null;

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_GUIDE_QUERY)) {

			stmt.setInt(1, guide_id);
			stmt.setInt(2, player_id);

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					String title = rset.getString("title");
					guide = new Guide(guide_id, player_id, title);
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.5857d08149b7", player_id, " from DB", e));
		}
		return guide;
	}

	/**
	 * 保存引导。
	 * Saves a guide.
	 *
	 * guide id
	 * 玩家 / player
	 * title
	 */
	@Override
	public void saveGuide(int guide_id, Player player, String title) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			stmt.setInt(1, guide_id);
			stmt.setString(2, title);
			stmt.setInt(3, player.getObjectId());
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error(I18n.get("log.9ad0924840e7", player, e));
		}
	}

	/**
	 * 获取已使用的引导 ID 数组。
	 * Gets the array of used guide ids.
	 *
	 * used id array
	 */
	@Override
	public int[] getUsedIDs() {
		List<Integer> ids = new ArrayList<Integer>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			 ResultSet rs = statement.executeQuery()) {

			while (rs.next()) {
				ids.add(rs.getInt("guide_id"));
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.4c2e4d8e0a12", e));
			return new int[0];
		}

		int[] result = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			result[i] = ids.get(i);
		}
		return result;
	}
}
