package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;
import com.aionemu.gameserver.persistence.entity.PlayerEmotionPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家表情列表数据访问层。
 * Data access layer for player emotion lists.
 * @author Mr. Poke
 */
public interface PlayerEmotionDao extends IService<PlayerEmotionPo> {

	/**
	 * 加载玩家表情列表。
	 * Loads the player's emotion list.
	 * @param player 玩家 / player
	 */
	void loadEmotions(Player player);

	/**
	 * 为玩家插入一条表情。
	 * Inserts an emotion for the player.
	 * @param player 玩家 / player
	 * @param emotion 表情 / emotion
	 */
	void insertEmotion(Player player, Emotion emotion);

	/**
	 * 删除玩家的指定表情。
	 * Deletes the given emotion for the player.
	 * @param playerId 玩家 ID / player id
	 * @param emotionId 表情 ID / emotion id
	 */
	void deleteEmotion(int playerId, int emotionId);
}
