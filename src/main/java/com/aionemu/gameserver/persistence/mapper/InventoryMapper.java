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

	/**
	 * 插入物品记录；由调用方连接执行，故保留 {@code ?} 原文。
	 * Inserts an inventory row; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `inventory` (`item_unique_id`, `item_id`, `item_count`, `item_color`, "
		+ "`color_expires`, `item_creator`, `expire_time`, `activation_count`, `item_owner`, "
		+ "`is_equiped`, is_soul_bound, `slot`, `item_location`, `enchant`, `enchant_bonus`, "
		+ "`item_skin`, `fusioned_item`, `optional_socket`, `optional_fusion_socket`, `charge`, "
		+ "`rnd_bonus`, `rnd_count`, `wrappable_count`, `is_packed`, `tempering_level`, "
		+ "`is_topped`, `strengthen_skill`, `skin_skill`, `luna_reskin`, `reduction_level`, "
		+ "`is_seal`, `isEnhance`, `enhanceSkillId`, `enhanceSkillEnchant`) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 更新物品记录；由调用方连接执行，故保留 {@code ?} 原文。
	 * Updates an inventory row; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String UPDATE_QUERY_JDBC = "UPDATE inventory SET item_count = ?, item_color = ?, color_expires = ?, "
		+ "item_creator = ?, expire_time = ?, activation_count = ?, item_owner = ?, "
		+ "is_equiped = ?, is_soul_bound = ?, slot = ?, item_location = ?, enchant = ?, "
		+ "enchant_bonus = ?, item_skin = ?, fusioned_item = ?, optional_socket = ?, "
		+ "optional_fusion_socket = ?, charge = ?, rnd_bonus = ?, rnd_count = ?, "
		+ "wrappable_count = ?, is_packed = ?, tempering_level = ?, is_topped = ?, "
		+ "strengthen_skill = ?, skin_skill = ?, luna_reskin = ?, reduction_level = ?, "
		+ "is_seal = ?, isEnhance = ?, enhanceSkillId = ?, enhanceSkillEnchant = ? "
		+ "WHERE item_unique_id = ?";

	/**
	 * 按唯一 ID 删除物品记录；由调用方连接执行，故保留 {@code ?} 原文。
	 * Deletes an inventory row by unique id; executed on the caller's connection, hence the verbatim {@code ?}.
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM inventory WHERE item_unique_id = ?";
}
