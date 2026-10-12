package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.io.Serial;

import lombok.Data;

/**
 * 表 users 的持久化实体。
 * Persistence entity for table users.
 */
@Data
@Table("users")
public class UserPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Long id;

	private String account;

	private String ip;

	private LocalDateTime date;

}
