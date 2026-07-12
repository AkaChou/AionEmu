package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Announcement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * 公告 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of AnnouncementsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Divinity
 */
@Slf4j
public class AnnouncementsDAO extends com.aionemu.gameserver.dao.AnnouncementsDAO {


	/** 查询公告 SQL / Select announcements SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM announcements ORDER BY id";
	/** 插入公告 SQL / Insert announcement SQL*/
	private static final String INSERT_QUERY = "INSERT INTO announcements (announce, faction, type, delay) VALUES (?, ?, ?, ?)";
	/** 删除公告 SQL / Delete announcement SQL*/
	private static final String DELETE_QUERY = "DELETE FROM announcements WHERE id = ?";

	/**
	 * 加载全部公告。
	 * Loads all announcements.
	 *
	 * announcement set
	 */
	@Override
	public Set<Announcement> getAnnouncements() {
		final Set<Announcement> result = new HashSet<Announcement>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
			 ResultSet resultSet = stmt.executeQuery()) {

			while (resultSet.next()) {
				result.add(new Announcement(
					resultSet.getInt("id"),
					resultSet.getString("announce"),
					resultSet.getString("faction"),
					resultSet.getString("type"),
					resultSet.getInt("delay")
				));
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.3e42f415d09f", e));
		}
		return result;
	}

	/**
	 * 添加公告。
	 * Adds an announcement.
	 *
	 * announcement object
	 */
	@Override
	public void addAnnouncement(final Announcement announce) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			stmt.setString(1, announce.getAnnounce());
			stmt.setString(2, announce.getFaction());
			stmt.setString(3, announce.getType());
			stmt.setInt(4, announce.getDelay());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.b3b62f944143", e));
		}
	}

	/**
	 * 按 ID 删除公告。
	 * Deletes an announcement by id.
	 *
	 * announcement id
	 *
	 * @param idAnnounce
	 * @return 是否删除成功 / whether the delete succeeded
	 */
	@Override
	public boolean delAnnouncement(final int idAnnounce) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

			stmt.setInt(1, idAnnounce);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.7f5bab6b7889", idAnnounce, e));
			return false;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param s 数据库名 / database name
	 * @param i 主版本 / major version
	 * @param i1 次版本 / minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String s, int i, int i1) {
		return DAOUtils.supports(s, i, i1);
	}
}
