package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_atreian_bestiary 的持久化实体。
 * Persistence entity for table player_atreian_bestiary.
 */
@Data
@Table("player_atreian_bestiary")
public class PlayerAtreianBestiaryPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer id;

	private Integer killCount;

	private Integer level;

	private Integer claimReward;

}
