package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;

import com.mybatisflex.core.BaseMapper;
import com.aionemu.gameserver.persistence.entity.SkillMotionPo;

/**
 * 表 skill_motions 的映射层。
 * Mapper for table skill_motions.
 * <p>
 * 采集写入是「已存在则忽略」的原子 upsert：主键六列全部是业务值（动作名、技能、攻速、双手武器、性别），
 * 故 Flex 1.11.8 的 {@code insertOrUpdate} 会走 update 分支，行不存在时永不插入（静默丢数据），
 * 且 Flex 无 SQL 级 upsert 可表达，只能逐字保留注解 SQL。
 * The collected write is an atomic "ignore when present" upsert: all six primary-key columns are business
 * values (motion, skill, attack speed, both weapon types, gender), so Flex 1.11.8's {@code insertOrUpdate}
 * takes the update branch and never inserts a missing row (silent data loss), and Flex cannot express a
 * SQL-level upsert — hence the verbatim annotated SQL.
 * </p>
 */
public interface SkillMotionMapper extends BaseMapper<SkillMotionPo> {

	/**
	 * 写入一条动作耗时；同一主键已存在时保持不变（原 {@code ON DUPLICATE KEY UPDATE motion_name = ?}）。
	 * Writes one motion timing; an existing row with the same key is left untouched (the former
	 * {@code ON DUPLICATE KEY UPDATE motion_name = ?}).
	 *
	 * @param po 动作耗时行 / motion timing row
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `skill_motions` (`motion_name`, `weapon_type`, `off_weapon_type`, `skill_id`, "
		+ "`attack_speed`, `race`, `gender`, `time`) VALUES (#{motionName}, #{weaponType}, #{offWeaponType}, "
		+ "#{skillId}, #{attackSpeed}, #{race}, #{gender}, #{time}) "
		+ "ON DUPLICATE KEY UPDATE `motion_name` = #{motionName}")
	int upsert(SkillMotionPo po);
}
