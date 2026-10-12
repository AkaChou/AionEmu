package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_equipment_setting 的持久化实体。
 * Persistence entity for table player_equipment_setting.
 */
@Data
@Table("player_equipment_setting")
public class PlayerEquipmentSettingPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer slot;

	private String name;

	private Integer display;

	private Integer mHand;

	private Integer sHand;

	private Integer helmet;

	private Integer torso;

	private Integer glove;

	private Integer boots;

	private Integer earringsLeft;

	private Integer earringsRight;

	private Integer ringLeft;

	private Integer ringRight;

	private Integer necklace;

	private Integer shoulder;

	private Integer pants;

	private Integer powershardLeft;

	private Integer powershardRight;

	private Integer wings;

	private Integer waist;

	private Integer mOffHand;

	private Integer sOffHand;

	private Integer plume;

	private Integer bracelet;

}
