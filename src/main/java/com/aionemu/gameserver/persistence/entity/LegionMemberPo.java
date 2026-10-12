package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 legion_members 的持久化实体。
 * Persistence entity for table legion_members.
 */
@Data
@Table("legion_members")
public class LegionMemberPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer legionId;

	@Id
	private Integer playerId;

	private String nickname;

	private String rank;

	private String selfintro;

	private Integer challengeScore;

}
