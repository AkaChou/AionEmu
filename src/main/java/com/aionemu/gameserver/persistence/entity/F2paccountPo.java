package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 f2paccount 的持久化实体。
 * Persistence entity for table f2paccount.
 */
@Data
@Table("f2paccount")
public class F2paccountPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer time;

}
