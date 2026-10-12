package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 petitions 的持久化实体。
 * Persistence entity for table petitions.
 */
@Data
@Table("petitions")
public class PetitionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 请愿 ID；DDL 为 bigint，原实现以 {@code rs.getInt} 读取，模型亦为 int。
	 * Petition id; DDL is bigint, while the legacy implementation read it via {@code rs.getInt}
	 * and the {@code Petition} model type is int.
	 */
	@Id
	private Integer id;

	private Integer playerId;

	private Integer type;

	private String title;

	private String message;

	private String addData;

	private Long time;

	private String status;

}
