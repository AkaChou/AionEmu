package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 玩家配方列表数据访问抽象层。
 * DAO for player craft recipe list persistence.
 *
 * @author lord_rex
 */
public abstract class PlayerRecipesDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerRecipesDAO.class.getName();
	}

	/**
	 * 加载玩家已学会的配方列表。
	 * Loads the recipe list for the player.
	 *
	 * player object id
	 * recipe list
	 */
	public abstract RecipeList load(final int playerId);

	/**
	 * 为玩家添加一条配方。
	 * Adds a recipe for the player.
	 *
	 * player object id
	 * recipe id
	 * @return 是否添加成功 / true if added
	 */
	public abstract boolean addRecipe(final int playerId, final int recipeId);

	/**
	 * 删除玩家的一条配方。
	 * Deletes a recipe from the player.
	 *
	 * player object id
	 * recipe id
	 * @return 是否删除成功 / true if deleted
	 */
	public abstract boolean delRecipe(final int playerId, final int recipeId);

	/** Adds or converges a recipe on the caller-owned transaction. */
	public abstract void addRecipeInTransaction(Connection connection, int playerId, int recipeId) throws SQLException;

	/** Deletes a recipe on the caller-owned transaction; deleting an absent row is successful convergence. */
	public abstract void delRecipeInTransaction(Connection connection, int playerId, int recipeId) throws SQLException;
}
