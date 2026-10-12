package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 house_scripts 的持久化实体。
 * Persistence entity for table house_scripts.
 */
@Data
@Table("house_scripts")
public class HouseScriptPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer houseId;

	@Id
	private Integer index;

	private String script;

}
