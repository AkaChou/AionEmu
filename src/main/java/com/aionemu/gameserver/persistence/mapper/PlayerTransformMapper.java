package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerTransformPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_transform 的映射层。
 * Mapper for table player_transform.
 */
public interface PlayerTransformMapper extends BaseMapper<PlayerTransformPo> {

	/**
	 * 插入或更新变身面板数据（MySQL upsert 方言）。
	 * Inserts or updates transform panel data (MySQL upsert dialect).
	 * @param playerId 玩家对象 ID / player object id
	 * @param panelId 面板 ID / panel id
	 * @param itemId 物品 ID / item id
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_transform` (`player_id`, `panel_id`, `item_id`) "
		+ "VALUES (#{playerId}, #{panelId}, #{itemId}) "
		+ "ON DUPLICATE KEY UPDATE `panel_id` = VALUES(`panel_id`), `item_id` = VALUES(`item_id`)")
	int upsert(@Param("playerId") int playerId, @Param("panelId") int panelId, @Param("itemId") int itemId);
}
