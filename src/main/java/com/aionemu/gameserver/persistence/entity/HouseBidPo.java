package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 house_bids 的持久化实体。
 * Persistence entity for table house_bids.
 */
@Data
@Table("house_bids")
public class HouseBidPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer houseId;

	@Id
	private Long bid;

	private Timestamp bidTime;

}
