package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_emotions 的持久化实体。
 * Persistence entity for table player_emotions.
 */
@Data
@Table("player_emotions")
public class PlayerEmotionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer emotion;

	private Integer remaining;

}
