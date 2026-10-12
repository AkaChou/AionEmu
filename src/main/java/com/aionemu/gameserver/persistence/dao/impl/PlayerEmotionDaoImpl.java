package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;
import com.aionemu.gameserver.model.gameobjects.player.emotion.EmotionList;
import com.aionemu.gameserver.persistence.dao.PlayerEmotionDao;
import com.aionemu.gameserver.persistence.entity.PlayerEmotionPo;
import com.aionemu.gameserver.persistence.mapper.PlayerEmotionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家表情列表数据访问实现。
 * Data access implementation for player emotion lists.
 * @author Mr. Poke
 */
@Slf4j
@Service
@Lazy
public class PlayerEmotionDaoImpl extends ServiceImpl<PlayerEmotionMapper, PlayerEmotionPo>
	implements PlayerEmotionDao {

	/**
	 * 加载玩家表情列表到内存，并跳过已过期表情。
	 * Loads the player's emotion list into memory, skipping expired emotions.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadEmotions(Player player) {
		EmotionList emotions = new EmotionList(player);
		int currentTime = (int) (System.currentTimeMillis() / 1000);

		try {
			for (PlayerEmotionPo po : mapper.selectListByQuery(QueryWrapper.create()
				.where(PlayerEmotionPo::getPlayerId).eq(player.getObjectId()))) {
				int emotionId = po.getEmotion();
				int remaining = po.getRemaining();

				// 跳过过期表情 / Skip expired emotions
				if (remaining > 0 && remaining <= currentTime) {
					deleteEmotion(player.getObjectId(), emotionId);
					continue;
				}

				emotions.add(emotionId, remaining, false);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.efdea0fd00c0", player.getObjectId()), e);
		}

		player.setEmotions(emotions);
	}

	/**
	 * 插入或更新单个玩家表情。
	 * Inserts or updates a single player emotion.
	 * @param player 玩家 / player
	 * @param emotion 表情 / emotion
	 */
	@Override
	public void insertEmotion(Player player, Emotion emotion) {
		try {
			mapper.upsertEmotion(player.getObjectId(), emotion.id(), emotion.getExpireTime());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a9e6401899a7", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家的指定表情。
	 * Deletes a specific emotion for the player.
	 * @param playerId 玩家 ID / player id
	 * @param emotionId 表情 ID / emotion id
	 */
	@Override
	public void deleteEmotion(int playerId, int emotionId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerEmotionPo::getPlayerId).eq(playerId)
				.and(PlayerEmotionPo::getEmotion).eq(emotionId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6015c7958d1b", playerId, emotionId), e);
		}
	}
}
