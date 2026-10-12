package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 blocks 的持久化实体。
 * Persistence entity for table blocks.
 */
@Data
@Table("blocks")
public class BlockPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer player;

	@Id
	private Integer blockedPlayer;

	private String reason;

}
