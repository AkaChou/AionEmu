package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_life_stats 的持久化实体。
 * Persistence entity for table player_life_stats.
 */
@Data
@Table("player_life_stats")
public class PlayerLifeStatPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer hp;

	private Integer mp;

	private Integer fp;

}
