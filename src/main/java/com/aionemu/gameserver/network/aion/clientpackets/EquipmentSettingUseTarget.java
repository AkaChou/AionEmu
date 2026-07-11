package com.aionemu.gameserver.network.aion.clientpackets;

/**
 * 装备方案应用目标：穿戴、卸下与切手接口。
 * Equipment preset apply target: equip, unequip and switch-hands API.
 */
interface EquipmentSettingUseTarget {

	/**
	 * 穿戴物品到槽位。
	 * Equips an item into a slot.
	 */
	boolean equipItem(int itemObjectId, long slot);

	/**
	 * 从槽位卸下物品。
	 * Unequips an item from a slot.
	 */
	boolean unEquipItem(int itemObjectId, long slot);

	/**
	 * 是否允许切换主副手。
	 * Whether hand-switch is allowed.
	 */
	boolean canSwitchHands();

	/**
	 * 切换主副手。
	 * Switches main/off hands.
	 */
	boolean switchHands();

	/**
	 * 查询物品当前装备槽位。
	 * Returns currently equipped slot for an item.
	 */
	long getEquippedSlot(int itemObjectId);
}
