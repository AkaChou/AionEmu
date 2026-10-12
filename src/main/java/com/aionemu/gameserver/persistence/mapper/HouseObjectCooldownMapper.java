package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.HouseObjectCooldownPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 house_object_cooldowns 的映射层。
 * Mapper for table house_object_cooldowns.
 */
public interface HouseObjectCooldownMapper extends BaseMapper<HouseObjectCooldownPo> {

	/**
	 * 插入或更新房屋物件冷却（MySQL 方言 ON DUPLICATE KEY UPDATE，Flex CRUD 无法表达）。
	 * Inserts or updates a house object cooldown (MySQL ON DUPLICATE KEY UPDATE, not expressible by Flex CRUD).
	 * @param playerId 玩家对象 ID / player object id
	 * @param objectId 物件 ID / object id
	 * @param reuseTime 复用时点 / reuse timestamp
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `house_object_cooldowns` (`player_id`, `object_id`, `reuse_time`) "
		+ "VALUES (#{playerId}, #{objectId}, #{reuseTime}) "
		+ "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)")
	int upsert(@Param("playerId") int playerId, @Param("objectId") int objectId, @Param("reuseTime") long reuseTime);
}
