package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_pets 的持久化实体。
 * Persistence entity for table player_pets.
 */
@Data
@Table("player_pets")
public class PlayerPetPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer petId;

	private Integer decoration;

	private String name;

	private Integer hungryLevel;

	private Integer feedProgress;

	private Long reuseTime;

	private Timestamp birthday;

	private Long moodStarted;

	private Integer counter;

	private Long moodCdStarted;

	private Long giftCdStarted;

	private String dopings;

	private Timestamp despawnTime;

	private Integer expireTime;

}
