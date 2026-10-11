package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家配方 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerRecipesDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author lord_rex
 */
@Slf4j
@Repository
public class PlayerRecipesDAO extends com.aionemu.gameserver.dao.PlayerRecipesDAO {

	/** 查询配方 SQL / Select recipes SQL*/
	private static final String SELECT_QUERY = "SELECT `recipe_id` FROM player_recipes WHERE `player_id`=?";
	/** 添加配方 SQL / Add recipe SQL*/
	private static final String ADD_QUERY = "INSERT INTO player_recipes (`player_id`, `recipe_id`) VALUES (?, ?) "
		+ "ON DUPLICATE KEY UPDATE `recipe_id`=VALUES(`recipe_id`)";
	/** 删除配方 SQL / Delete recipe SQL*/
	private static final String DELETE_QUERY = "DELETE FROM player_recipes WHERE `player_id`=? AND `recipe_id`=?";

	private final JdbcClient jdbcClient;

	public PlayerRecipesDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerRecipesDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载玩家配方列表。
	 * Loads a player's recipe list.
	 * @param playerId 玩家 ID / player id
	 * @return 配方列表 / recipe list
	 */
	@Override
	public RecipeList load(final int playerId) {
		final HashSet<Integer> recipeList = new HashSet<>();

		try {
			recipeList.addAll(jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.query((rs, _) -> rs.getInt("recipe_id"))
				.list());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5a3357334006", playerId), e);
		}
		return new RecipeList(recipeList);
	}

	/**
	 * 为玩家添加配方。
	 * Adds a recipe for the player.
	 * @param playerId 玩家 ID / player id
	 * @param recipeId 配方 ID / recipe id
	 * @return 是否添加成功 / whether the insert succeeded
	 */
	@Override
	public boolean addRecipe(final int playerId, final int recipeId) {
		try {
			jdbcClient.sql(ADD_QUERY)
				.param(playerId)
				.param(recipeId)
				.update();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.23c5276a9d26", playerId, recipeId), e);
			return false;
		}
	}

	/**
	 * 删除玩家配方。
	 * Deletes a recipe from the player.
	 * @param playerId 玩家 ID / player id
	 * @param recipeId 配方 ID / recipe id
	 * @return 是否删除成功 / whether the delete succeeded
	 */
	@Override
	public boolean delRecipe(final int playerId, final int recipeId) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(playerId)
				.param(recipeId)
				.update();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.05178d165c29", playerId, recipeId), e);
			return false;
		}
	}

	@Override
	public void addRecipeInTransaction(Connection connection, int playerId, int recipeId) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(ADD_QUERY)) {
			ps.setInt(1, playerId);
			ps.setInt(2, recipeId);
			ps.executeUpdate();
		}
	}

	@Override
	public void delRecipeInTransaction(Connection connection, int playerId, int recipeId) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(DELETE_QUERY)) {
			ps.setInt(1, playerId);
			ps.setInt(2, recipeId);
			ps.executeUpdate();
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
