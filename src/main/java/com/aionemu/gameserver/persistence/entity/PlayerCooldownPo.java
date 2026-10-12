package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_cooldowns 的持久化实体。
 * Persistence entity for table player_cooldowns.
 */
@Data
@Table("player_cooldowns")
public class PlayerCooldownPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer cooldownId;

	private Long reuseDelay;

}
