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
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;

import lombok.extern.slf4j.Slf4j;

/**
 * 引导（Guide）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of GuideDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author xTz
 */
@Slf4j
@Repository
public class GuideDAO extends com.aionemu.gameserver.dao.GuideDAO {

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

	private final JdbcClient jdbcClient;

	public GuideDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public GuideDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
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

	/**
	 * 删除引导。
	 * Deletes a guide.
	 * @param guide_id 指南 ID / guide id
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean deleteGuide(int guide_id) {
		try {
			return jdbcClient.sql(DELETE_QUERY)
				.param(guide_id)
				.update() > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.257617627810", guide_id), e);
			return false;
		}
	}

	/**
	 * 加载玩家全部引导。
	 * Loads all guides of a player.
	 * @param playerId 玩家 ID / player id
	 * @return 指南列表 / guide list
	 */
	@Override
	public List<Guide> loadGuides(int playerId) {
		List<Guide> guides = new ArrayList<>();
		try {
			guides = jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.query((rset, rowNum) -> new Guide(
					rset.getInt("guide_id"),
					rset.getInt("player_id"),
					rset.getString("title")
				))
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.040d5371000d", playerId, " from DB"), e);
		}
		return guides;
	}

	/**
	 * 加载单条引导。
	 * Loads a single guide.
	 * @param player_id 玩家 ID / player id
	 * @param guide_id 指南 ID / guide id
	 * @return 指南 / guide
	 */
	@Override
	public Guide loadGuide(int player_id, int guide_id) {
		try {
			return jdbcClient.sql(SELECT_GUIDE_QUERY)
				.param(guide_id)
				.param(player_id)
				.query((rset, rowNum) -> new Guide(
					guide_id,
					player_id,
					rset.getString("title")
				))
				.optional()
				.orElse(null);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5857d08149b7", player_id, " from DB"), e);
			return null;
		}
	}

	/**
	 * 保存引导。
	 * Saves a guide.
	 * @param guide_id 指南 ID / guide id
	 * @param player 玩家 / player
	 * @param title 称号 / title
	 */
	@Override
	public void saveGuide(int guide_id, Player player, String title) {
		try {
			jdbcClient.sql(INSERT_QUERY)
				.param(guide_id)
				.param(title)
				.param(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9ad0924840e7", player), e);
		}
	}

	/**
	 * 获取已使用的引导 ID 数组。
	 * Gets the array of used guide ids.
	 * @return 已用 ID 数组 / used id array
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<Integer> ids = jdbcClient.sql(SELECT_USED_IDS_QUERY)
				.query((rs, rowNum) -> rs.getInt("guide_id"))
				.list();
			int[] result = new int[ids.size()];
			for (int i = 0; i < ids.size(); i++) {
				result[i] = ids.get(i);
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4c2e4d8e0a12"), e);
			return new int[0];
		}
	}
}
