package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerAtreianBestiaryPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_atreian_bestiary 的映射层。
 * Mapper for table player_atreian_bestiary.
 */
public interface PlayerAtreianBestiaryMapper extends BaseMapper<PlayerAtreianBestiaryPo> {

	/**
	 * 插入或更新图鉴条目（MySQL upsert 方言）。
	 * Inserts or updates a bestiary entry (MySQL upsert dialect).
	 * @param playerId 玩家对象 ID / player object id
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @param killCount 击杀数 / kill count
	 * @param level 等级 / level
	 * @param claimReward 领奖标记 / claim reward flag
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_atreian_bestiary` (`player_id`, `id`, `kill_count`, `level`, `claim_reward`) "
		+ "VALUES (#{playerId}, #{id}, #{killCount}, #{level}, #{claimReward}) "
		+ "ON DUPLICATE KEY UPDATE `id` = VALUES(`id`), `kill_count` = VALUES(`kill_count`), "
		+ "`level` = VALUES(`level`), `claim_reward` = VALUES(`claim_reward`)")
	int upsert(@Param("playerId") int playerId, @Param("id") int id, @Param("killCount") int killCount,
		@Param("level") int level, @Param("claimReward") int claimReward);
}
