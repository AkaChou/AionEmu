package com.aionemu.gameserver.model.veteranrewards;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 老兵奖励，用于 veteranrewards 相关逻辑。
 * Veteran Rewards for veteranrewards logic.
 */

@RequiredArgsConstructor
@AllArgsConstructor
public class VeteranRewards {
	private int id;
	/** 获取玩家。 / Returns the player. */
	@Getter
	private final String Player;
	/** 获取类型。 / Returns the type. */
	@Getter
	private final int type;
	/** 获取物品。 / Returns the item. */
	@Getter
	private final int item;
	/** 获取计数。 / Returns the count. */
	@Getter
	private final int count;
	/** 获取基纳。 / Returns the kinah. */
	@Getter
	private final int kinah;
	/** 返回 sender / Returns the sender */
	@Getter
	private final String Sender;
	/** 获取称号。 / Returns the title. */
	@Getter
	private final String Title;
	/** 获取消息。 / Returns the message. */
	@Getter
	private final String Message;

	/** 返回 ID / Returns the id */
	public int getId() {
		if (id != 0) {
			return id;
		} else {
			return -1;
		}
	}
}
