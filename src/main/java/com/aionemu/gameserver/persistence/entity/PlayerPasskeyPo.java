package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_passkey 的持久化实体。
 * Persistence entity for table player_passkey.
 */
@Data
@Table("player_passkey")
public class PlayerPasskeyPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer accountId;

	@Id
	private String passkey;

}
