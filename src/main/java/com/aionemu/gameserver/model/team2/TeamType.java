package com.aionemu.gameserver.model.team2;

import lombok.Getter;

/**
 * 团队类型枚举。
 * Team Type enumeration.
 */

@Getter
public enum TeamType {
	/** 队伍。 / Group. */
	GROUP(0x3F, 0),
	/** 自动组队。 / Auto Group. */
	AUTO_GROUP(0x02, 1),
	/** 同盟。 / Alliance. */
	ALLIANCE(0x3F, 0),
	/** 防御同盟。 / Alliance Defence. */
	ALLIANCE_DEFENCE(0x3F, 4),
	/** 进攻同盟。 / Alliance Offence. */
	ALLIANCE_OFFENCE(0x02, 3),
	/** 区域默认。 / In-area Default. */
	IN_AREA_DEFAULT(0x00, 3),
	/** 区域目标 1。 / In-area Target 1. */
	IN_AREA_TARGET_1(0xE2, 4),
	/** 区域目标 2。 / In-area Target 2. */
	IN_AREA_TARGET_2(0xFF, 5),
	/** 区域目标 3。 / In-area Target 3. */
	IN_AREA_TARGET_3(0x22, 6),
	/** 区域目标 4。 / In-area Target 4. */
	IN_AREA_TARGET_4(0x22, 7);

	private int type;
	private int subType;

	private TeamType(int type, int subType) {
		this.type = type;
		this.subType = subType;
	}

	/** 是否为自动团队。 / Whether auto team. */
	public boolean isAutoTeam() {
		return this.getType() == 0x02;
	}

	/**
	 * @return 是否进攻 / Whether offence
	 */
	public boolean isOffence() {
		return this.getSubType() == 3;
	}

	/**
	 * @return 是否防御 / Whether defence
	 */
	public boolean isDefence() {
		return this.getSubType() == 4;
	}

	/** 是否为真实区域控制器创建的自动团队。 / Whether created as an auto team by the retail in-area controller. */
	public boolean isInArea() {
		return switch (this) {
			case IN_AREA_DEFAULT, IN_AREA_TARGET_1, IN_AREA_TARGET_2, IN_AREA_TARGET_3, IN_AREA_TARGET_4 -> true;
			default -> false;
		};
	}

	/** 区域团队保留最后一名成员，空团队仍正常解散。 / In-area teams keep the last member; empty teams still disband normally. */
	public boolean shouldDisband(int onlineMembers) {
		return onlineMembers == 0 || onlineMembers == 1 && !isInArea();
	}
}
