package com.aionemu.gameserver.model.team.legion;

/**
 * 军团军阶枚举。
 * Legion Rank enumeration.
 *
 * @author Simple
 */
public enum LegionRank {
	/** All Legion Ranks  / All Legion Ranks * */
	BRIGADE_GENERAL(0), DEPUTY(1), CENTURION(2), LEGIONARY(3), VOLUNTEER(4);

	private byte rank;

	private LegionRank(int rank) {
		this.rank = (byte) rank;
	}

	/**
	 * 返回 client - sideid 用于此。 / Returns client-side id for this
	 *
	 * @return byte
	 */
	public byte getRankId() {
		return this.rank;
	}
}
