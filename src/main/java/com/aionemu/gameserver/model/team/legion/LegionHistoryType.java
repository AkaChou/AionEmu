package com.aionemu.gameserver.model.team.legion;

/**
 * 军团 History 类型枚举。
 * Legion History Type enumeration.
 *
 * @author Simple
 */
public enum LegionHistoryType {
	/** 创建 / Create. */
	CREATE(0), // No parameters
	/** 加入 / Join. */
	JOIN(1), // Parameter: name
	/** 踢出 / Kick. */
	KICK(2), // Parameter: name
	/** 等级 / Level Up */
	LEVEL_UP(3), // Parameter: legion level
	/** 已任命 / Appointed. */
	APPOINTED(4), // Parameter: legion level
	/** Emblem 登记 / Emblem Register */
	EMBLEM_REGISTER(5), // No parameters
	/** Emblem Modified / Emblem Modified */
	EMBLEM_MODIFIED(6), // No parameters
	/** 物品存入 / Item Deposit*/
	ITEM_DEPOSIT(15), // Parameter: name
	/** 物品取出 / Item Withdraw */
	ITEM_WITHDRAW(16), // Parameter: name
	/** 基纳存入 / Kinah Deposit*/
	KINAH_DEPOSIT(17), // Parameter: name
	/** Kinah 取出 / Kinah Withdraw */
	KINAH_WITHDRAW(18); // Parameter: name

	private byte historyType;

	private LegionHistoryType(int historyType) {
		this.historyType = (byte) historyType;
	}

	/**
	 * 返回 client - sideid 用于此。 / Returns client-side id for this
	 *
	 * @return byte
	 */
	public byte getHistoryId() {
		return this.historyType;
	}
}
