package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_account_time")
public class AccountTimePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer accountId;

	private Timestamp lastActive;

	private Timestamp expirationTime;

	private Integer sessionDuration;

	private Integer accumulatedOnline;

	private Integer accumulatedRest;

	private Timestamp penaltyEnd;

}
