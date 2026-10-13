package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashSet;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;
import com.aionemu.gameserver.persistence.dao.PlayerRecipeDao;
import com.aionemu.gameserver.persistence.entity.PlayerRecipePo;
import com.aionemu.gameserver.persistence.mapper.PlayerRecipeMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家配方数据访问实现。
 * Data access implementation for player craft recipe lists.
 * @author lord_rex
 */
@Slf4j
@Service
@Lazy
public class PlayerRecipeDaoImpl extends ServiceImpl<PlayerRecipeMapper, PlayerRecipePo> implements PlayerRecipeDao {

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
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerRecipePo::getPlayerId).eq(playerId))
				.forEach(po -> recipeList.add(po.getRecipeId()));
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
			mapper.addRecipe(playerId, recipeId);
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
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerRecipePo::getPlayerId).eq(playerId)
				.and(PlayerRecipePo::getRecipeId).eq(recipeId));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.05178d165c29", playerId, recipeId), e);
			return false;
		}
	}

	@Override
	public void addRecipeInTransaction(int playerId, int recipeId) {
		mapper.addRecipe(playerId, recipeId);
	}

	@Override
	public void delRecipeInTransaction(int playerId, int recipeId) {
		mapper.deleteByQuery(QueryWrapper.create()
			.where(PlayerRecipePo::getPlayerId).eq(playerId)
			.and(PlayerRecipePo::getRecipeId).eq(recipeId));
	}
}
