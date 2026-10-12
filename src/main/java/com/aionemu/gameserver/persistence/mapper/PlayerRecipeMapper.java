package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerRecipePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_recipes 的映射层。
 * Mapper for table player_recipes.
 */
public interface PlayerRecipeMapper extends BaseMapper<PlayerRecipePo> {

	/**
	 * 事务内使用的原始 JDBC 语句（? 占位，供调用方连接执行）。
	 * Raw JDBC statements for caller-owned transactions (? placeholders).
	 */
	String ADD_RECIPE_JDBC = "INSERT INTO player_recipes (`player_id`, `recipe_id`) VALUES (?, ?) "
		+ "ON DUPLICATE KEY UPDATE `recipe_id`=VALUES(`recipe_id`)";
	/** 事务内使用的删除原始 JDBC 语句 / raw JDBC delete statement for caller-owned transactions. */
	String DELETE_RECIPE_JDBC = "DELETE FROM player_recipes WHERE `player_id`=? AND `recipe_id`=?";

	/**
	 * 添加配方（MySQL upsert，重复学习收敛为同一行）。
	 * Adds a recipe (MySQL upsert, re-learning converges on the same row).
	 * @param playerId 玩家对象 ID / player object id
	 * @param recipeId 配方 ID / recipe id
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO player_recipes (`player_id`, `recipe_id`) VALUES (#{playerId}, #{recipeId}) "
		+ "ON DUPLICATE KEY UPDATE `recipe_id`=VALUES(`recipe_id`)")
	int addRecipe(@Param("playerId") int playerId, @Param("recipeId") int recipeId);
}
