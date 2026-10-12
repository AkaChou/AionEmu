package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerEmotionPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_emotions 的映射层。
 * Mapper for table player_emotions.
 */
public interface PlayerEmotionMapper extends BaseMapper<PlayerEmotionPo> {

	/**
	 * 插入或更新单个玩家表情（MySQL upsert）。
	 * Inserts or updates a single player emotion (MySQL upsert).
	 * @param playerId 玩家对象 ID / player object id
	 * @param emotion 表情 ID / emotion id
	 * @param remaining 过期时间戳 / expiration timestamp
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_emotions` (`player_id`, `emotion`, `remaining`) "
		+ "VALUES (#{playerId}, #{emotion}, #{remaining}) "
		+ "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)")
	int upsertEmotion(@Param("playerId") int playerId, @Param("emotion") int emotion,
		@Param("remaining") int remaining);
}
