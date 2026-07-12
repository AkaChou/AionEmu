package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

/**
 * 玩家配方 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerRecipesDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author lord_rex
 */
@Slf4j
public class PlayerRecipesDAO extends com.aionemu.gameserver.dao.PlayerRecipesDAO {


	/** 查询配方 SQL / Select recipes SQL*/
	private static final String SELECT_QUERY = "SELECT `recipe_id` FROM player_recipes WHERE `player_id`=?";
	/** 添加配方 SQL / Add recipe SQL*/
	private static final String ADD_QUERY = "INSERT INTO player_recipes (`player_id`, `recipe_id`) VALUES (?, ?)";
	/** 删除配方 SQL / Delete recipe SQL*/
	private static final String DELETE_QUERY = "DELETE FROM player_recipes WHERE `player_id`=? AND `recipe_id`=?";

	/**
	 * 加载玩家配方列表。
	 * Loads a player's recipe list.
	 *
	 * player id
	 * recipe list
	 */
	@Override
	public RecipeList load(final int playerId) {
		final HashSet<Integer> recipeList = new HashSet<Integer>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {

			ps.setInt(1, playerId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					recipeList.add(rs.getInt("recipe_id"));
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.5a3357334006", playerId, e));
		}
		return new RecipeList(recipeList);
	}

	/**
	 * 为玩家添加配方。
	 * Adds a recipe for the player.
	 *
	 * player id
	 * recipe id
	 *
	 * @return 是否添加成功 / whether the insert succeeded
	 */
	@Override
	public boolean addRecipe(final int playerId, final int recipeId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(ADD_QUERY)) {

			ps.setInt(1, playerId);
			ps.setInt(2, recipeId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.23c5276a9d26", playerId, recipeId, e));
			return false;
		}
	}

	/**
	 * 删除玩家配方。
	 * Deletes a recipe from the player.
	 *
	 * player id
	 * recipe id
	 *
	 * @return 是否删除成功 / whether the delete succeeded
	 */
	@Override
	public boolean delRecipe(final int playerId, final int recipeId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {

			ps.setInt(1, playerId);
			ps.setInt(2, recipeId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.05178d165c29", playerId, recipeId, e));
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
