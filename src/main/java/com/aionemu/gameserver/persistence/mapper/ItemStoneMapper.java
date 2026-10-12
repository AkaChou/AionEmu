package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.ItemStonePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 item_stones 的映射层。
 * Mapper for table item_stones.
 * <p>
 * 单表常规读取走 Flex CRUD；批量增删改由调用方连接执行，故仅保留语句原文。
 * Regular single-table reads use Flex CRUD; the batch writes run on the caller's connection, so only the
 * verbatim statements are kept here.
 * </p>
 */
public interface ItemStoneMapper extends BaseMapper<ItemStonePo> {

	/**
	 * 插入镶嵌石；由调用方连接执行，故保留 {@code ?} 原文。
	 * Inserts an item stone; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `item_stones` "
		+ "(`item_unique_id`, `item_id`, `slot`, `category`, `polishNumber`, `polishCharge`, `proc_count`) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 更新镶嵌石；由调用方连接执行，故保留 {@code ?} 原文。
	 * Updates an item stone; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String UPDATE_QUERY_JDBC = "UPDATE `item_stones` SET "
		+ "`item_id` = ?, `slot` = ?, `polishNumber` = ?, `polishCharge` = ?, `proc_count` = ? "
		+ "WHERE `item_unique_id` = ? AND `category` = ?";

	/**
	 * 删除镶嵌石；由调用方连接执行，故保留 {@code ?} 原文。
	 * Deletes an item stone; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM `item_stones` "
		+ "WHERE `item_unique_id` = ? AND `slot` = ? AND `category` = ?";
}
