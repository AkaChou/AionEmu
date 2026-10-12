package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 skill_motions 的持久化实体。
 * Persistence entity for table skill_motions.
 */
@Data
@Table("skill_motions")
public class SkillMotionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private String motionName;

	@Id
	private Integer skillId;

	@Id
	private Integer attackSpeed;

	@Id
	private String weaponType;

	@Id
	private String offWeaponType;

	private String race;

	@Id
	private String gender;

	private Integer time;

}
