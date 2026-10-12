package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 portal_cooldowns 的持久化实体。
 * Persistence entity for table portal_cooldowns.
 */
@Data
@Table("portal_cooldowns")
public class PortalCooldownPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer worldId;

	private Long reuseTime;

	private Integer entryCount;

}
