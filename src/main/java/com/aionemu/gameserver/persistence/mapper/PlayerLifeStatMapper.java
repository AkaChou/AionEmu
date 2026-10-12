package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerLifeStatPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_life_stats 的映射层。
 * Mapper for table player_life_stats.
 */
public interface PlayerLifeStatMapper extends BaseMapper<PlayerLifeStatPo> {

	/**
	 * 插入或更新玩家生命状态（MySQL upsert）。
	 * Inserts or updates player life stats (MySQL upsert).
	 * @param playerId 玩家对象 ID / player object id
	 * @param hp 当前 HP / current hp
	 * @param mp 当前 MP / current mp
	 * @param fp 当前 FP / current fp
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_life_stats` (`player_id`, `hp`, `mp`, `fp`) VALUES (#{playerId}, #{hp}, #{mp}, #{fp}) "
		+ "ON DUPLICATE KEY UPDATE `hp` = VALUES(`hp`), `mp` = VALUES(`mp`), `fp` = VALUES(`fp`)")
	int upsertLifeStat(@Param("playerId") int playerId, @Param("hp") int hp, @Param("mp") int mp, @Param("fp") int fp);
}
