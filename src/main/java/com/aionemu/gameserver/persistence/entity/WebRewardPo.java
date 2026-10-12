package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 web_reward 的持久化实体。
 * Persistence entity for table web_reward.
 */
@Data
@Table("web_reward")
public class WebRewardPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer unique;

	private Integer itemOwner;

	private Integer itemId;

	private Long itemCount;

	private Timestamp added;

	private Timestamp received;

	private Integer rewarded;

}
