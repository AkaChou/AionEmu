package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.ItemCooldownPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 item_cooldowns 的映射层。
 * Mapper for table item_cooldowns.
 */
public interface ItemCooldownMapper extends BaseMapper<ItemCooldownPo> {

	/**
	 * 插入或更新单条物品冷却；MySQL 方言 {@code ON DUPLICATE KEY UPDATE} 逐字节保留，仅 {@code ?} 改为占位参数。
	 * Inserts or updates a single item cooldown; the MySQL {@code ON DUPLICATE KEY UPDATE} dialect is kept
	 * verbatim and only {@code ?} becomes a placeholder.
	 * @param playerId 玩家对象 ID / player object id
	 * @param delayId 冷却延迟 ID / cooldown delay id
	 * @param useDelay 使用延迟 / use delay
	 * @param reuseTime 下次可用时间 / next reuse time
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `item_cooldowns` (`player_id`, `delay_id`, `use_delay`, `reuse_time`) VALUES "
		+ "(#{playerId},#{delayId},#{useDelay},#{reuseTime}) "
		+ "ON DUPLICATE KEY UPDATE `use_delay` = VALUES(`use_delay`), `reuse_time` = VALUES(`reuse_time`)")
	int upsert(@Param("playerId") int playerId, @Param("delayId") int delayId, @Param("useDelay") int useDelay,
		@Param("reuseTime") long reuseTime);
}
