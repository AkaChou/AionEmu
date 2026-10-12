package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.PlayerSkillPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_skills 的映射层。
 * Mapper for table player_skills.
 */
public interface PlayerSkillMapper extends BaseMapper<PlayerSkillPo> {

	/**
	 * 插入技能或更新技能等级（原生 JDBC 常量）。
	 * Insert a skill or update its level (raw JDBC constant).
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `player_skills` (`player_id`, `skill_id`, `skill_level`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `skill_level` = VALUES(`skill_level`)";

	/**
	 * 幂等任务授予技能：重试时绝不降级已独立提升的技能等级（原生 JDBC 常量）。
	 * Idempotent quest grant: retries never downgrade a skill that advanced independently (raw JDBC constant).
	 */
	String GRANT_QUERY_JDBC = "INSERT INTO `player_skills` (`player_id`, `skill_id`, `skill_level`) "
		+ "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `skill_level` = GREATEST(`skill_level`, VALUES(`skill_level`))";

	/**
	 * 更新技能等级（原生 JDBC 常量）。
	 * Update a skill level (raw JDBC constant).
	 */
	String UPDATE_QUERY_JDBC = "UPDATE `player_skills` SET skill_level = ? WHERE player_id = ? AND skill_id = ?";

	/**
	 * 更新技能皮肤信息（原生 JDBC 常量）。
	 * Update skill skin info (raw JDBC constant).
	 */
	String UPDATE_SKIN_QUERY_JDBC = "UPDATE `player_skills` SET skin_id = ?, skin_active_date = ?, "
		+ "skin_expire_time = ?, skin_activated = ? WHERE player_id = ? AND skill_id = ?";

	/**
	 * 删除技能（原生 JDBC 常量）。
	 * Delete a skill (raw JDBC constant).
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM `player_skills` WHERE `player_id` = ? AND skill_id = ?";
}
