package com.aionemu.gameserver.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.aionemu.gameserver.model.gameobjects.player.RecipeList;
import com.aionemu.gameserver.persistence.entity.PlayerRecipePo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家配方列表数据访问层。
 * Data access layer for player craft recipe lists.
 * @author lord_rex
 */
public interface PlayerRecipeDao extends IService<PlayerRecipePo> {

	/**
	 * 加载玩家已学会的配方列表。
	 * Loads the recipe list for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 配方列表 / recipe list
	 */
	RecipeList load(int playerId);

	/**
	 * 为玩家添加一条配方。
	 * Adds a recipe for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param recipeId 配方 ID / recipe id
	 * @return 是否添加成功 / true if added
	 */
	boolean addRecipe(int playerId, int recipeId);

	/**
	 * 删除玩家的一条配方。
	 * Deletes a recipe from the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param recipeId 配方 ID / recipe id
	 * @return 是否删除成功 / true if deleted
	 */
	boolean delRecipe(int playerId, int recipeId);

	/** Adds or converges a recipe on the caller-owned transaction. */
	void addRecipeInTransaction(Connection connection, int playerId, int recipeId) throws SQLException;

	/** Deletes a recipe on the caller-owned transaction; deleting an absent row is successful convergence. */
	void delRecipeInTransaction(Connection connection, int playerId, int recipeId) throws SQLException;
}
