package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.PlayerRegisteredItemPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_registered_items 的映射层。
 * Mapper for table player_registered_items.
 * <p>
 * 单表常规读取走 Flex CRUD；房屋注册表的批量写入由调用方连接执行，故仅保留语句原文。
 * Regular single-table reads use Flex CRUD; the house registry batch writes run on the caller's connection,
 * so only the verbatim statements are kept here.
 * </p>
 */
public interface PlayerRegisteredItemMapper extends BaseMapper<PlayerRegisteredItemPo> {

	/**
	 * 插入注册物品；由调用方连接执行，故保留 {@code ?} 原文。
	 * Inserts a registered item; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `player_registered_items` "
		+ "(`expire_time`, `color`, `color_expires`, `owner_use_count`, `visitor_use_count`, "
		+ "`x`, `y`, `z`, `h`, `area`, `floor`, `player_id`, `item_unique_id`, `item_id`) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 更新注册物品；由调用方连接执行，故保留 {@code ?} 原文。
	 * Updates a registered item; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String UPDATE_QUERY_JDBC = "UPDATE `player_registered_items` SET "
		+ "`expire_time` = ?, `color` = ?, `color_expires` = ?, `owner_use_count` = ?, "
		+ "`visitor_use_count` = ?, `x` = ?, `y` = ?, `z` = ?, `h` = ?, `area` = ?, `floor` = ? "
		+ "WHERE `player_id` = ? AND `item_unique_id` = ? AND `item_id` = ?";

	/**
	 * 按唯一 ID 删除注册物品；由调用方连接执行，故保留 {@code ?} 原文。
	 * Deletes a registered item by unique id; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM `player_registered_items` WHERE `item_unique_id` = ?";
}
