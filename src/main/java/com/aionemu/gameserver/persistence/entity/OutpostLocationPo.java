package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 outpost_location 的持久化实体。
 * Persistence entity for table outpost_location.
 */
@Data
@Table("outpost_location")
public class OutpostLocationPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer id;

	private String race;

}
