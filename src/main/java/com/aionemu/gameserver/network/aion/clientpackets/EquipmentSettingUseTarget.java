package com.aionemu.gameserver.network.aion.clientpackets;

interface EquipmentSettingUseTarget {

	boolean equipItem(int itemObjectId, long slot);

	boolean unEquipItem(int itemObjectId, long slot);

	boolean switchHands();

	long getEquippedSlot(int itemObjectId);
}
