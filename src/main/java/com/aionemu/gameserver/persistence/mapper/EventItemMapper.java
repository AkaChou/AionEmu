package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.EventItemPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 event_items 的映射层。
 * Mapper for table event_items.
 */
public interface EventItemMapper extends BaseMapper<EventItemPo> {

	/**
	 * 插入或更新活动物品当日计数；MySQL 方言 {@code ON DUPLICATE KEY UPDATE} 逐字节保留，仅 {@code ?} 改为占位参数。
	 * Inserts or updates an event item daily count; the MySQL {@code ON DUPLICATE KEY UPDATE} dialect is
	 * kept verbatim and only {@code ?} becomes a placeholder.
	 * @param playerId 玩家对象 ID / player object id
	 * @param itemId 物品模板 ID / item template id
	 * @param counts 当日计数 / daily count
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `event_items` (`player_id`, `item_id`, `counts`) VALUES (#{playerId},#{itemId},#{counts}) "
		+ "ON DUPLICATE KEY UPDATE `counts` = VALUES(`counts`)")
	int upsert(@Param("playerId") int playerId, @Param("itemId") int itemId, @Param("counts") int counts);
}
