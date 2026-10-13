package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerSkillPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_skills 的映射层。
 * Mapper for table player_skills.
 *
 * <p>技能行的写入是 MySQL upsert，MyBatis-Flex 1.11.8 无 SQL 级 upsert
 * （{@code insertOrUpdate} 只按主键在 Java 侧二选一，行不存在时会静默丢数据），故保留注解 SQL。
 * Skill rows are written with a MySQL upsert; MyBatis-Flex 1.11.8 has no SQL-level upsert
 * ({@code insertOrUpdate} branches in Java on the primary key and silently drops the row when it is
 * absent), so the annotated SQL is kept.
 */
public interface PlayerSkillMapper extends BaseMapper<PlayerSkillPo> {

	/**
	 * 插入技能或更新技能等级（MySQL upsert）。
	 * Inserts a skill or updates its level (MySQL upsert).
	 * @param playerId 玩家对象 ID / player object id
	 * @param skillId 技能 ID / skill id
	 * @param skillLevel 技能等级 / skill level
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_skills` (`player_id`, `skill_id`, `skill_level`) VALUES (#{playerId}, #{skillId}, #{skillLevel}) "
		+ "ON DUPLICATE KEY UPDATE `skill_level` = VALUES(`skill_level`)")
	int insertSkill(@Param("playerId") int playerId, @Param("skillId") int skillId,
		@Param("skillLevel") int skillLevel);

	/**
	 * 幂等任务授予技能：重试时绝不降级已独立提升的技能等级。
	 * Idempotent quest grant: retries never downgrade a skill that advanced independently.
	 * @param playerId 玩家对象 ID / player object id
	 * @param skillId 技能 ID / skill id
	 * @param skillLevel 目标等级 / target level
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_skills` (`player_id`, `skill_id`, `skill_level`) VALUES (#{playerId}, #{skillId}, #{skillLevel}) "
		+ "ON DUPLICATE KEY UPDATE `skill_level` = GREATEST(`skill_level`, VALUES(`skill_level`))")
	int grantSkill(@Param("playerId") int playerId, @Param("skillId") int skillId,
		@Param("skillLevel") int skillLevel);
}
