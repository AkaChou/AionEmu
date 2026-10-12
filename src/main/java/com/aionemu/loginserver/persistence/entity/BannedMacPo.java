package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_banned_mac")
public class BannedMacPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)@Column("uniId")
	private Integer uniId;

	private String address;

	private Timestamp time;

	private String details;

}
