package com.aionemu.loginserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

@Data
@Table("ls_player_transfers")
public class PlayerTransferPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer sourceServer;

	private Integer targetServer;

	private Integer sourceAccountId;

	private Integer targetAccountId;

	private Integer playerId;

	private Integer status;

	private String timeAdded;

	private String timePerformed;

	private String timeDone;

	private String comment;

}
