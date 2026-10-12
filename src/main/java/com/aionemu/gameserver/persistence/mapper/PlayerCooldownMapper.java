package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerCooldownPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_cooldowns 的映射层。
 * Mapper for table player_cooldowns.
 */
public interface PlayerCooldownMapper extends BaseMapper<PlayerCooldownPo> {

	/**
	 * 插入或更新技能冷却（MySQL 方言 ON DUPLICATE KEY UPDATE，Flex CRUD 无法表达）。
	 * Inserts or updates a skill cooldown (MySQL ON DUPLICATE KEY UPDATE, not expressible by Flex CRUD).
	 * @param playerId 玩家对象 ID / player object id
	 * @param cooldownId 冷却 ID / cooldown id
	 * @param reuseDelay 复用延迟 / reuse delay
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_cooldowns` (`player_id`, `cooldown_id`, `reuse_delay`) "
		+ "VALUES (#{playerId}, #{cooldownId}, #{reuseDelay}) "
		+ "ON DUPLICATE KEY UPDATE `reuse_delay` = VALUES(`reuse_delay`)")
	int upsert(@Param("playerId") int playerId, @Param("cooldownId") int cooldownId,
		@Param("reuseDelay") long reuseDelay);
}
