package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Announcement;

import lombok.extern.slf4j.Slf4j;

/**
 * 公告 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of AnnouncementsDAO using JdbcClient.
 * @author Divinity
 */
@Slf4j
@Repository
public class AnnouncementsDAO extends com.aionemu.gameserver.dao.AnnouncementsDAO {

	/** 查询公告 SQL / Select announcements SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM announcements ORDER BY id";
	/** 插入公告 SQL / Insert announcement SQL*/
	private static final String INSERT_QUERY = "INSERT INTO announcements (announce, faction, type, delay) VALUES (?, ?, ?, ?)";
	/** 删除公告 SQL / Delete announcement SQL*/
	private static final String DELETE_QUERY = "DELETE FROM announcements WHERE id = ?";

	private final JdbcClient jdbcClient;

	public AnnouncementsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public AnnouncementsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载全部公告。
	 * Loads all announcements.
	 * @return 公告集合 / announcement set
	 */
	@Override
	public Set<Announcement> getAnnouncements() {
		final Set<Announcement> result = new HashSet<>();
		try {
			jdbcClient.sql(SELECT_QUERY)
				.query((rs, rowNum) -> new Announcement(
					rs.getInt("id"),
					rs.getString("announce"),
					rs.getString("faction"),
					rs.getString("type"),
					rs.getInt("delay")
				))
				.list()
				.forEach(result::add);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3e42f415d09f"), e);
		}
		return result;
	}

	/**
	 * 添加公告。
	 * Adds an announcement.
	 * @param announce 公告对象 / announcement object
	 */
	@Override
	public void addAnnouncement(final Announcement announce) {
		try {
			jdbcClient.sql(INSERT_QUERY)
				.param(announce.getAnnounce())
				.param(announce.getFaction())
				.param(announce.getType())
				.param(announce.getDelay())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b3b62f944143"), e);
		}
	}

	/**
	 * 按 ID 删除公告。
	 * Deletes an announcement by id.
	 * @param idAnnounce 公告 ID / announcement id
	 * @return 是否删除成功 / whether the delete succeeded
	 */
	@Override
	public boolean delAnnouncement(final int idAnnounce) {
		try {
			int affected = jdbcClient.sql(DELETE_QUERY)
				.param(idAnnounce)
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7f5bab6b7889", idAnnounce), e);
			return false;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param database 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
