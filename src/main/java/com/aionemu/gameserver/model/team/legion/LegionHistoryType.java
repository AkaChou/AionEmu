package com.aionemu.gameserver.model.team.legion;

/**
 * 军团历史类型枚举。
 * Legion History Type enumeration.
 *
 * @author Simple
 */
public enum LegionHistoryType {
	/** 创建 / Create. */
	CREATE(0), // 无参数 / No parameters
	/** 加入 / Join. */
	JOIN(1), // 参数：名称 / Parameter: name
	/** 踢出 / Kick. */
	KICK(2), // 参数：名称 / Parameter: name
	/** 升级 / Level Up. */
	LEVEL_UP(3), // 参数：军团等级 / Parameter: legion level
	/** 已任命 / Appointed. */
	APPOINTED(4), // 参数：军团等级 / Parameter: legion level
	/** 徽章登记 / Emblem Register. */
	EMBLEM_REGISTER(5), // 无参数 / No parameters
	/** 徽章修改 / Emblem Modified. */
	EMBLEM_MODIFIED(6), // 无参数 / No parameters
	/** 物品存入 / Item Deposit. */
	ITEM_DEPOSIT(15), // 参数：名称 / Parameter: name
	/** 物品取出 / Item Withdraw. */
	ITEM_WITHDRAW(16), // 参数：名称 / Parameter: name
	/** 基纳存入 / Kinah Deposit. */
	KINAH_DEPOSIT(17), // 参数：名称 / Parameter: name
	/** 基纳取出 / Kinah Withdraw. */
	KINAH_WITHDRAW(18); // 参数：名称 / Parameter: name

	private byte historyType;

	private LegionHistoryType(int historyType) {
		this.historyType = (byte) historyType;
	}

	/**
	 * 返回客户端使用的历史类型 ID。
	 * Returns client-side id for this.
	 *
	 * @return 历史类型 ID / history type id
	 */
	public byte getHistoryId() {
		return this.historyType;
	}
}
