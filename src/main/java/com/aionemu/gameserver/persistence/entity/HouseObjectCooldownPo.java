package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 house_object_cooldowns 的持久化实体。
 * Persistence entity for table house_object_cooldowns.
 */
@Data
@Table("house_object_cooldowns")
public class HouseObjectCooldownPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer objectId;

	private Long reuseTime;

}
