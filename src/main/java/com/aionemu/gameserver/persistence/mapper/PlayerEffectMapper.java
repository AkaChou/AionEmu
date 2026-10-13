package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerEffectPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_effects 的映射层。
 * Mapper for table player_effects.
 */
public interface PlayerEffectMapper extends BaseMapper<PlayerEffectPo> {

	/**
	 * 插入或更新玩家效果（MySQL upsert；Flex 1.11.8 无 SQL 级 upsert，故保留注解 SQL）。
	 * Inserts or updates a player effect (MySQL upsert; MyBatis-Flex 1.11.8 has no SQL-level upsert).
	 * @param playerId 玩家对象 ID / player object id
	 * @param skillId 技能 ID / skill id
	 * @param skillLvl 技能等级 / skill level
	 * @param currentTime 剩余时间 / remaining time
	 * @param endTime 结束时间 / end time
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_effects` (`player_id`, `skill_id`, `skill_lvl`, `current_time`, `end_time`) "
		+ "VALUES (#{playerId}, #{skillId}, #{skillLvl}, #{currentTime}, #{endTime}) ON DUPLICATE KEY UPDATE "
		+ "`skill_lvl` = VALUES(`skill_lvl`), `current_time` = VALUES(`current_time`), `end_time` = VALUES(`end_time`)")
	int upsertEffect(@Param("playerId") int playerId, @Param("skillId") int skillId,
		@Param("skillLvl") int skillLvl, @Param("currentTime") int currentTime, @Param("endTime") long endTime);
}
