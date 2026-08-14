package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;

/**
 * 玩家表情列表数据访问对象。
 * Player emotion list data access object.
 *
 * @author Mr. Poke
 */
public abstract class PlayerEmotionListDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerEmotionListDAO.class.getName();
	}

	/**
	 * 加载玩家表情列表。
	 * Loads the player's emotion list.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadEmotions(Player player);

	/**
	 * 为玩家插入一条表情。
	 * Inserts an emotion for the player.
	 *
	 * @param player 玩家 / player
	 * @param emotion 表情 / emotion
	 */
	public abstract void insertEmotion(Player player, Emotion emotion);

	/**
	 * 删除玩家的指定表情。
	 * Deletes the given emotion for the player.
	 *
	 * @param playerId 玩家 ID / player id
	 * @param emotionId 表情 ID / emotion id
	 */
	public abstract void deleteEmotion(int playerId, int emotionId);
}
