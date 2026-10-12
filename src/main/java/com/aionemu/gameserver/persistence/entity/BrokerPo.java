package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 broker 的持久化实体。
 * Persistence entity for table broker.
 */
@Data
@Table("broker")
public class BrokerPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer itemPointer;

	private Integer itemId;

	private Long itemCount;

	private String itemCreator;

	private String seller;

	private Long price;

	private String brokerRace;

	private Timestamp expireTime;

	private Timestamp settleTime;

	private Integer sellerId;

	@Column("is_sold")
	private Boolean sold;

	@Column("is_settled")
	private Boolean settled;

	@Column("is_splitsell")
	private Boolean splitsell;

}
