package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.CraftCooldownPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 craft_cooldowns 的映射层。
 * Mapper for table craft_cooldowns.
 */
public interface CraftCooldownMapper extends BaseMapper<CraftCooldownPo> {

	/**
	 * 插入或更新制作冷却（MySQL 方言 ON DUPLICATE KEY UPDATE，Flex CRUD 无法表达）。
	 * Inserts or updates a craft cooldown (MySQL ON DUPLICATE KEY UPDATE, not expressible by Flex CRUD).
	 * @param playerId 玩家对象 ID / player object id
	 * @param delayId 冷却延迟 ID / cooldown delay id
	 * @param reuseTime 复用时点 / reuse timestamp
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `craft_cooldowns` (`player_id`, `delay_id`, `reuse_time`) "
		+ "VALUES (#{playerId}, #{delayId}, #{reuseTime}) "
		+ "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)")
	int upsert(@Param("playerId") int playerId, @Param("delayId") int delayId, @Param("reuseTime") long reuseTime);
}
