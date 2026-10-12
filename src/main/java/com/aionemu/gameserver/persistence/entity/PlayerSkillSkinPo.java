package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_skill_skins 的持久化实体。
 * Persistence entity for table player_skill_skins.
 */
@Data
@Table("player_skill_skins")
public class PlayerSkillSkinPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer playerId;

	private Integer skinId;

	/**
	 * DDL 为 bigint，原实现以 rs.getInt 读取，故此处保持 Integer。
	 * DDL type is bigint; the legacy code read it via rs.getInt, hence Integer is kept here.
	 */
	private Integer remaining;

	private Integer active;

}
