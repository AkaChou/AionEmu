package com.aionemu.gameserver.model.team2;

import lombok.Getter;

/**
 * 团队类型枚举。
 * Team Type enumeration.
 */

@Getter
public enum TeamType {
	/** 队伍。 / Group. */
	GROUP(0x3F, 0), AUTO_GROUP(0x02, 1), ALLIANCE(0x3F, 0), ALLIANCE_DEFENCE(0x3F, 4), ALLIANCE_OFFENCE(0x02, 3);

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
	 * @return Whether offence / Whether offence
	 */
	public boolean isOffence() {
		return this.getSubType() == 3;
	}

	/**
	 * @return Whether defence / Whether defence
	 */
	public boolean isDefence() {
		return this.getSubType() == 4;
	}
}
