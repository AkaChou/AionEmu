package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 players 的持久化实体。
 * Persistence entity for table players.
 */
@Data
@Table("players")
public class PlayerPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private String name;

	private Integer accountId;

	private String accountName;

	private Long exp;

	private Long recoverexp;

	private Float x;

	private Float y;

	private Float z;

	/** DDL 为 int，原实现以 rs.getByte 读取。 / DDL int; the legacy code read it with rs.getByte. */
	private Byte heading;

	private Integer worldId;

	private String gender;

	private String race;

	private String playerClass;

	private Timestamp creationDate;

	private Timestamp deletionDate;

	private Timestamp lastOnline;

	private Integer questExpands;

	private Integer advencedStigmaSlotSize;

	private Integer warehouseSize;

	private Integer mailboxLetters;

	private Integer bindPoint;

	private Integer titleId;

	private Integer bonusTitleId;

	/** DDL 为 tinyint(1)，原实现以 rs.getBoolean 读取。 / DDL tinyint(1); the legacy code read it with rs.getBoolean. */
	private Boolean online;

	private String note;

	private Integer npcExpands;

	private Integer worldOwner;

	private Integer dp;

	private Integer soulSickness;

	private Long reposteEnergy;

	private Integer mentorFlagTime;

	/** DDL 为 decimal(20,0)，原实现以 rs.getLong 读取。 / DDL decimal(20,0); the legacy code read it with rs.getLong. */
	private Long lastTransferTime;

	private Integer stamps;

	private Timestamp lastStamp;

	private Integer rewardedPass;

	/** DDL 为 bigint，原实现以 rs.getInt 读取。 / DDL bigint; the legacy code read it with rs.getInt. */
	private Integer passportTime;

	@Column("is_archdaeva")
	private Boolean archdaeva;

	private Integer creativityPoint;

	private Long auraOfGrowth;

	private Integer joinLegionId;

	private String joinState;

	private Long berdinStar;

	private Integer lunaConsume;

	private Integer muniKeys;

	private Integer lunaConsumeCount;

	private Integer wardrobeSlot;

	private Long abyssFavor;

	private Integer lunaPoints;

	private Integer frenzyPoints;

	private Integer frenzyCount;

	private Integer tocFloor;

	private Integer stoneCp;

	private Integer goldenDice;

	private Integer sweepReset;

	private Integer minionSkillPoints;

	private Timestamp minionFunctionTime;

}
