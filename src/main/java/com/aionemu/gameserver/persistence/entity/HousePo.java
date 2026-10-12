package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 houses 的持久化实体。
 * Persistence entity for table houses.
 */
@Data
@Table("houses")
public class HousePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private Integer playerId;

	private Integer buildingId;

	private Integer address;

	private Timestamp acquireTime;

	private Integer settings;

	private String status;

	private Integer feePaid;

	private Timestamp nextPay;

	private Timestamp sellStarted;

	private byte[] signNotice;

}
