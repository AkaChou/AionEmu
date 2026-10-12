package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerPunishmentPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_punishments 的映射层。
 * Mapper for table player_punishments.
 */
public interface PlayerPunishmentMapper extends BaseMapper<PlayerPunishmentPo> {

	/**
	 * 替换玩家惩罚记录（MySQL REPLACE INTO，按表列序写入）。
	 * Replaces a player punishment record (MySQL REPLACE INTO, positional by table column order).
	 * @param playerId 玩家对象 ID / player object id
	 * @param punishmentType 惩罚类型 / punishment type
	 * @param startTime 开始时间戳 / start timestamp
	 * @param duration 持续秒数 / duration in seconds
	 * @param reason 惩罚原因 / punishment reason
	 * @return 影响行数 / affected rows
	 */
	@Insert("REPLACE INTO `player_punishments` VALUES (#{playerId},#{punishmentType},#{startTime},#{duration},#{reason})")
	int replacePunishment(@Param("playerId") int playerId, @Param("punishmentType") String punishmentType,
		@Param("startTime") long startTime, @Param("duration") long duration, @Param("reason") String reason);
}
