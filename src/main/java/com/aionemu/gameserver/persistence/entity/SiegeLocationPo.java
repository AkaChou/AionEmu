package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 siege_locations 的持久化实体。
 * Persistence entity for table siege_locations.
 */
@Data
@Table("siege_locations")
public class SiegeLocationPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private String race;

	private Integer legionId;

	private Integer occupyCount;

}
