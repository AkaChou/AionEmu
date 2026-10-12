package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_account_rewards")
public class AccountRewardPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)@Column("uniqId")
	private Integer uniqId;

	@Column("accountId")
	private Integer accountId;

	private String added;

	private BigDecimal points;

	private String received;

	private Integer rewarded;

	private BigDecimal luna;

}
