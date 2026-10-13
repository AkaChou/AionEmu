package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Column;
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

	/**
	 * 上一期会员等级。遗留 INSERT 不写该列，取值来自 DDL 默认值，且 {@code updateMembership}
	 * 以它为赋值来源（{@code SET membership = old_membership}），故须保留可读。
	 * Previous membership level. The legacy INSERT omitted it, its value came from the DDL default,
	 * and {@code updateMembership} reads it as an assignment source ({@code SET membership =
	 * old_membership}), so it must stay readable.
	 */
	@Column(insertable = false)
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

	/**
	 * 账号余额。遗留 INSERT 不写该列，取值来自 DDL 默认值；领域代码从不读写它。
	 * Account balance. The legacy INSERT omitted it and its value came from the DDL default;
	 * no domain code reads or writes it.
	 */
	@Column(insertable = false)
	private Float balance;

	/**
	 * 露娜点数。遗留 INSERT 不写该列，取值来自 DDL 默认值；后续由 {@code updateLuna} 维护。
	 * Luna points. The legacy INSERT omitted it and its value came from the DDL default; it is
	 * maintained afterwards by {@code updateLuna}.
	 */
	@Column(insertable = false)
	private Integer luna;

	private Integer returnAccount;

	private Timestamp returnEnd;

}
