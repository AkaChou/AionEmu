package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_svstats")
public class SvstatPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer server;

	private Integer status;

	private Integer current;

	private Integer max;

	private LocalDateTime lastUpdate;

}
