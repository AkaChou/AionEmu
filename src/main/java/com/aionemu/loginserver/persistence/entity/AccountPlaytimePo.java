package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_account_playtime")
public class AccountPlaytimePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Long accountId;

	private Long accumulatedOnline;

	private Timestamp lastupdate;

}
