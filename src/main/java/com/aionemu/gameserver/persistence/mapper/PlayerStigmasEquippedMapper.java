package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.PlayerStigmasEquippedPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_stigmas_equipped 的映射层。
 * Mapper for table player_stigmas_equipped.
 */
public interface PlayerStigmasEquippedMapper extends BaseMapper<PlayerStigmasEquippedPo> {

	/**
	 * 插入已装备刺针（原生 JDBC 常量，供调用方事务复用）。
	 * Insert an equipped stigma (raw JDBC constant for caller-owned transactions).
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `player_stigmas_equipped` (`player_id`, `item_id`, `item_name`) VALUES (?,?,?)";

	/**
	 * 更新已装备刺针（原生 JDBC 常量）。
	 * Update an equipped stigma (raw JDBC constant).
	 */
	String UPDATE_QUERY_JDBC = "UPDATE `player_stigmas_equipped` SET item_id=?, item_name=? WHERE player_id=? AND item_id=?";

	/**
	 * 删除已装备刺针（原生 JDBC 常量）。
	 * Delete an equipped stigma (raw JDBC constant).
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM `player_stigmas_equipped` WHERE `player_id`=? AND item_id=? AND item_name=?";
}
