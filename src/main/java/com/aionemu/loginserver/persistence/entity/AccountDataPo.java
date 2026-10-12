package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_account_data")
public class AccountDataPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private String name;

	private String password;

	private Integer activated;

	private Integer accessLevel;

	private Integer membership;

	private Integer oldMembership;

	private Integer lastServer;

	private String lastIp;

	private String lastMac;

	private String ipForce;

	private Date expire;

	private Long toll;

	private String email;

	private String question;

	private String answer;

	private Float balance;

	private Integer luna;

	private Integer returnAccount;

	private Timestamp returnEnd;

}
