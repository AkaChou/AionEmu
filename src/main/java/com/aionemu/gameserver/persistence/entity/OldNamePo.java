package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 old_names 的持久化实体。
 * Persistence entity for table old_names.
 */
@Data
@Table("old_names")
public class OldNamePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Long id;

	private Integer playerId;

	private String oldName;

	private String newName;

}
