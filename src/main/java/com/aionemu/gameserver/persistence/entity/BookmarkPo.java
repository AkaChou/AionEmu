package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 bookmark 的持久化实体。
 * Persistence entity for table bookmark.
 */
@Data
@Table("bookmark")
public class BookmarkPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private String name;

	private Integer charId;

	private Float x;

	private Float y;

	private Float z;

	private Integer worldId;

}
