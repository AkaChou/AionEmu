package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_transform 的持久化实体。
 * Persistence entity for table player_transform.
 */
@Data
@Table("player_transform")
public class PlayerTransformPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer playerId;

	private Integer panelId;

	private Integer itemId;

}
