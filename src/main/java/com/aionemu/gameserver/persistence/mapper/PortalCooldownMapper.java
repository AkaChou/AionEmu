package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PortalCooldownPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 portal_cooldowns 的映射层。
 * Mapper for table portal_cooldowns.
 */
public interface PortalCooldownMapper extends BaseMapper<PortalCooldownPo> {

	/**
	 * 插入或更新传送门冷却（MySQL 方言 ON DUPLICATE KEY UPDATE，Flex CRUD 无法表达）。
	 * Inserts or updates a portal cooldown (MySQL ON DUPLICATE KEY UPDATE, not expressible by Flex CRUD).
	 * @param playerId 玩家对象 ID / player object id
	 * @param worldId 世界 ID / world id
	 * @param reuseTime 复用时点 / reuse timestamp
	 * @param entryCount 进入次数 / entry count
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `portal_cooldowns` (`player_id`, `world_id`, `reuse_time`, `entry_count`) "
		+ "VALUES (#{playerId}, #{worldId}, #{reuseTime}, #{entryCount}) "
		+ "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`), `entry_count` = VALUES(`entry_count`)")
	int upsert(@Param("playerId") int playerId, @Param("worldId") int worldId, @Param("reuseTime") long reuseTime,
		@Param("entryCount") int entryCount);
}
