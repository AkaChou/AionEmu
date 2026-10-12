package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_effects 的持久化实体。
 * Persistence entity for table player_effects.
 */
@Data
@Table("player_effects")
public class PlayerEffectPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer skillId;

	private Integer skillLvl;

	private Integer currentTime;

	private Long endTime;

}
