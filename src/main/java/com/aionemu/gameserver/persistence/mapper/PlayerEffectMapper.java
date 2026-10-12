package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.PlayerEffectPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_effects 的映射层。
 * Mapper for table player_effects.
 */
public interface PlayerEffectMapper extends BaseMapper<PlayerEffectPo> {

	/**
	 * 插入或更新玩家效果（原生 JDBC 常量）。
	 * Insert or update a player effect (raw JDBC constant).
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `player_effects` (`player_id`, `skill_id`, `skill_lvl`, `current_time`, `end_time`) "
		+ "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
		+ "`skill_lvl` = VALUES(`skill_lvl`), `current_time` = VALUES(`current_time`), `end_time` = VALUES(`end_time`)";

	/**
	 * 删除玩家全部效果（原生 JDBC 常量）。
	 * Delete all effects of a player (raw JDBC constant).
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM `player_effects` WHERE `player_id` = ?";
}
