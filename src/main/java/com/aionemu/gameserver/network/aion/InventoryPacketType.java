package com.aionemu.gameserver.network.aion;

/**
 * 物品相关包场景类型：仓库、背包、邮件/回购、个人商店、武器切换等。
 * Inventory-related packet scene types: warehouse, inventory, mail/repurchase, private store, weapon switch.
 *
 * @author ATracer
 */
public enum InventoryPacketType {

	WAREHOUSE(false, false, false), INVENTORY(true, false, false), MAIL_REPURCHASE(false, true, false),
	PRIVATE_STORE(false, false, true), WEAPON_SWITCH(true, false, false, true);

	private boolean isInventory;
	private boolean isMailOrRepurchase;
	private boolean isPrivateStore;
	private boolean isWeaponSwitch;

	private InventoryPacketType(boolean isInventory, boolean isMail, boolean isPrivateStore) {
		this(isInventory, isMail, isPrivateStore, false);
	}

	private InventoryPacketType(boolean isInventory, boolean isMail, boolean isPrivateStore, boolean isWeaponSwitch) {
		this.isInventory = isInventory;
		this.isMailOrRepurchase = isMail;
		this.isPrivateStore = isPrivateStore;
		this.isWeaponSwitch = isWeaponSwitch;
	}

	/**
	 * 是否为背包场景。
	 * Whether this is an inventory scene.
	 *
	 * @return 若 inventory 则为 true / true if inventory
	 */
	public final boolean isInventory() {
		return isInventory;
	}

	/**
	 * 是否为邮件场景。
	 * Whether this is a mail scene.
	 *
	 * @return 若 mail 则为 true / true if mail
	 */
	public final boolean isMail() {
		return isMailOrRepurchase;
	}

	/**
	 * 是否为回购场景。
	 * Whether this is a repurchase scene.
	 *
	 * @return 若 repurchase 则为 true / true if repurchase
	 */
	public final boolean isRepurchase() {
		return isMailOrRepurchase;
	}

	/**
	 * 是否为个人商店场景。
	 * Whether this is a private store scene.
	 *
	 * @return 是否个人商店 / true if private store
	 */
	public final boolean isPrivateStore() {
		return isPrivateStore;
	}

	/**
	 * 是否为武器切换场景。
	 * Whether this is a weapon-switch scene.
	 *
	 * @return 是否武器切换 / true if weapon switch
	 */
	public final boolean isWeaponSwitch() {
		return isWeaponSwitch;
	}
}
