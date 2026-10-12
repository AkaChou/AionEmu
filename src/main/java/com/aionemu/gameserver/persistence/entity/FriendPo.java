package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 friends 的持久化实体。
 * Persistence entity for table friends.
 */
@Data
@Table("friends")
public class FriendPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer player;

	@Id
	private Integer friend;

}
