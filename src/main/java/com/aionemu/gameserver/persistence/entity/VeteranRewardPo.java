package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 veteran_rewards 的持久化实体。
 * Persistence entity for table veteran_rewards.
 */
@Data
@Table("veteran_rewards")
public class VeteranRewardPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private String player;

	private Integer type;

	private Integer item;

	private Integer count;

	private Integer kinah;

	private String sender;

	private String title;

	private String message;

}
