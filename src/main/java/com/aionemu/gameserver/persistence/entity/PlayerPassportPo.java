package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_passports 的持久化实体。
 * Persistence entity for table player_passports.
 */
@Data
@Table("player_passports")
public class PlayerPassportPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer accountId;

	private Integer passportId;

	private Integer stamps;

	private Timestamp lastStamp;

	private Integer rewarded;

}
