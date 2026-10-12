package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 announcements 的持久化实体。
 * Persistence entity for table announcements.
 */
@Data
@Table("announcements")
public class AnnouncementPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private String announce;

	private String faction;

	private String type;

	private Integer delay;

}
