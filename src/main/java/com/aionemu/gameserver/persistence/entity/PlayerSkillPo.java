package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_skills 的持久化实体。
 * Persistence entity for table player_skills.
 */
@Data
@Table("player_skills")
public class PlayerSkillPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer skillId;

	private Integer skillLevel;

	private Integer skinId;

	private Timestamp skinActiveDate;

	private Integer skinExpireTime;

	/**
	 * DDL 为 bigint，原实现以 rs.getBoolean 读取，故此处保持 Boolean。
	 * DDL type is bigint; the legacy code read it via rs.getBoolean, hence Boolean is kept here.
	 */
	private Boolean skinActivated;

}
