package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 server_variables 的持久化实体。
 * Persistence entity for table server_variables.
 */
@Data
@Table("server_variables")
public class ServerVariablePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private String key;

	private String value;

}
