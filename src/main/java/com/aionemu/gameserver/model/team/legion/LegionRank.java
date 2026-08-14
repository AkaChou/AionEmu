package com.aionemu.gameserver.model.team.legion;

/**
 * 军团军阶枚举。
 * Legion Rank enumeration.
 *
 * @author Simple
 */
public enum LegionRank {
	/** 军团长 / Brigade general. */
	BRIGADE_GENERAL(0),
	/** 副官 / Deputy. */
	DEPUTY(1),
	/** 百夫长 / Centurion. */
	CENTURION(2),
	/** 军团兵 / Legionary. */
	LEGIONARY(3),
	/** 志愿兵 / Volunteer. */
	VOLUNTEER(4);

	private byte rank;

	private LegionRank(int rank) {
		this.rank = (byte) rank;
	}

	/**
	 * 返回客户端使用的军阶 ID。
	 * Returns client-side id for this.
	 *
	 * @return 军阶 ID / rank id
	 */
	public byte getRankId() {
		return this.rank;
	}
}
