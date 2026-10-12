package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 abyss_landing 的持久化实体。
 * Persistence entity for table abyss_landing.
 */
@Data
@Table("abyss_landing")
public class AbyssLandingPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private Integer level;

	private Integer points;

	private Integer siege;

	private Integer commander;

	private Integer artefact;

	private Integer base;

	private Integer monuments;

	private Integer quest;

	private Integer facility;

	private String race;

	private Timestamp levelUpDate;

}
