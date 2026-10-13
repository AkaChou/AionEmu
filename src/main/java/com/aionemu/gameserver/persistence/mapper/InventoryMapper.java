package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 inventory 的映射层。
 * Mapper for table inventory.
 * <p>
 * 单表常规读写走 Flex CRUD；此处仅保留跨表查询与「调用方连接」执行的批量语句原文。
 * Regular single-table reads and writes use Flex CRUD; only cross-table lookups and the batch statements
 * executed on the caller's connection are kept here.
 * </p>
 */
public interface InventoryMapper extends BaseMapper<InventoryPo> {

	/**
	 * 按玩家 ID 查询账号 ID（跨表，SQL 原文保留）。
	 * Selects the account id of a player (cross-table, SQL kept verbatim).
	 * @param playerId 玩家对象 ID / player object id
	 * @return 账号 ID；无记录时为 null / account id, or null when absent
	 */
	@Select("SELECT `account_id` FROM `players` WHERE `id` = #{playerId}")
	Integer selectAccountId(@Param("playerId") int playerId);

	/**
	 * 按玩家 ID 查询军团 ID（跨表，SQL 原文保留）。
	 * Selects the legion id of a player (cross-table, SQL kept verbatim).
	 * @param playerId 玩家对象 ID / player object id
	 * @return 军团 ID；无记录时为 null / legion id, or null when absent
	 */
	@Select("SELECT `legion_id` FROM `legion_members` WHERE `player_id` = #{playerId}")
	Integer selectLegionId(@Param("playerId") int playerId);

}
