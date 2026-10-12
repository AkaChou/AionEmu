package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigInteger;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_account_vip")
public class AccountVipPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer accountId;

	/** Client VIP stage (1-6) */
	private Integer vipLevel;

	/** VIP progress experience */
	private BigInteger vipExp;

	/** Unix seconds VIP end time */
	private BigInteger expireTime;

}
