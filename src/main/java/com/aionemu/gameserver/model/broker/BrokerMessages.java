package com.aionemu.gameserver.model.broker;

/**
 * 经纪行 Messages 枚举。
 * Broker Messages enumeration.
 *
 * @author kosyachok
 */
public enum BrokerMessages {
	/** 无法登记物品 / Cant Register Item */
	CANT_REGISTER_ITEM(2),
	/** 背包空间不足 / No Space Available */
	NO_SPACE_AVAIABLE(3),
	/** 基纳不足 / Not Enough Kinah */
	NO_ENOUGHT_KINAH(5);

	private int id;

	private BrokerMessages(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
