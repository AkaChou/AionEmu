package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 legion_emblems 的持久化实体。
 * Persistence entity for table legion_emblems.
 */
@Data
@Table("legion_emblems")
public class LegionEmblemPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer legionId;

	private Integer emblemId;

	private Integer colorR;

	private Integer colorG;

	private Integer colorB;

	private String emblemType;

	private byte[] emblemData;

}
