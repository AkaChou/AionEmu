package com.aionemu.gameserver.model.team2;

import lombok.Getter;

/**
 * 团队类型枚举。
 * Team Type enumeration.
 */

@Getter
public enum TeamType {
	/** 队伍。 / Group. */
	GROUP(0x3F, 0), AUTO_GROUP(0x02, 1), ALLIANCE(0x3F, 0), ALLIANCE_DEFENCE(0x3F, 4), ALLIANCE_OFFENCE(0x02, 3),
	IN_AREA_DEFAULT(0x00, 3), IN_AREA_TARGET_1(0xE2, 4), IN_AREA_TARGET_2(0xFF, 5),
	IN_AREA_TARGET_3(0x22, 6), IN_AREA_TARGET_4(0x22, 7);

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
	 * @return Whether offence
	 */
	public boolean isOffence() {
		return this.getSubType() == 3;
	}

	/**
	 * @return Whether defence
	 */
	public boolean isDefence() {
		return this.getSubType() == 4;
	}

	/** 是否为真端区域控制器创建的自动团队。 */
	public boolean isInArea() {
		return switch (this) {
			case IN_AREA_DEFAULT, IN_AREA_TARGET_1, IN_AREA_TARGET_2, IN_AREA_TARGET_3, IN_AREA_TARGET_4 -> true;
			default -> false;
		};
	}

	/** 区域团队保留最后一名成员，空团队仍正常解散。 */
	public boolean shouldDisband(int onlineMembers) {
		return onlineMembers == 0 || onlineMembers == 1 && !isInArea();
	}
}
