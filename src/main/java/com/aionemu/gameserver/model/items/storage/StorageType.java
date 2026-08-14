package com.aionemu.gameserver.model.items.storage;

/**
 * 仓库类型枚举。
 * Storage Type enumeration.
 */

public enum StorageType {
	// 背包与仓库。 / Cube & Warehouse.
	/** 魔立方。 / Cube. */
	CUBE(0, 27, 9, 162), // 4.9
	/** 普通仓库。 / Regular Warehouse. */
	REGULAR_WAREHOUSE(1, 112, 8), ACCOUNT_WAREHOUSE(2, 16, 8), LEGION_WAREHOUSE(3, 80, 8),

	// 宠物袋。 / Pet's Bag.
	/** 宠物袋 6。 / Pet Bag 6. */
	PET_BAG_6(32, 6, 6), PET_BAG_12(33, 12, 6), PET_BAG_18(34, 18, 6), PET_BAG_22(45, 22, 6), // 5.8
	/** 宠物袋 24。 / Pet Bag 24. */
	PET_BAG_24(35, 24, 6), PET_BAG_28(44, 28, 6), // 5.1
	/** 宠物袋 30。 / Pet Bag 30. */
	PET_BAG_30(40, 30, 6),

	// 现金宠物袋。 / Cash Pet's Bag.
	/** 现金宠物袋 12。 / Cash Pet Bag 12. */
	CASH_PET_BAG_12(36, 12, 6), CASH_PET_BAG_18(37, 18, 6), CASH_PET_BAG_30(38, 30, 6), CASH_PET_BAG_24(39, 24, 6),
	/** 现金宠物袋 26。 / Cash Pet Bag 26. */
	CASH_PET_BAG_26(41, 26, 6), CASH_PET_BAG_32(42, 32, 6), CASH_PET_BAG_34(43, 34, 6),

	// 房屋。 / Housing.
	/** 房屋仓库 01。 / House Storage 01. */
	HOUSE_STORAGE_01(60, 9, 9), HOUSE_STORAGE_02(61, 9, 9), HOUSE_STORAGE_03(62, 9, 9), HOUSE_STORAGE_04(63, 9, 9),
	/** 房屋仓库 05。 / House Storage 05. */
	HOUSE_STORAGE_05(64, 9, 9), HOUSE_STORAGE_06(65, 9, 9), HOUSE_STORAGE_07(66, 9, 9), HOUSE_STORAGE_08(67, 9, 9),
	/** 房屋仓库 09。 / House Storage 09. */
	HOUSE_STORAGE_09(68, 18, 9), HOUSE_STORAGE_10(69, 18, 9), HOUSE_STORAGE_11(70, 18, 9), HOUSE_STORAGE_12(71, 18, 9),
	/** 房屋仓库 14。 / House Storage 14. */
	HOUSE_STORAGE_14(73, 18, 9), HOUSE_STORAGE_16(75, 27, 9), HOUSE_STORAGE_18(77, 27, 9), HOUSE_STORAGE_20(79, 0, 0),

	// 其他。 / Other.
	/** 经纪行。 / Broker. */
	BROKER(126), MAILBOX(127);

	// 宠物。 / Pet's.
	public static final int PET_BAG_MIN = 32;
	public static final int PET_BAG_MAX = 45;

	// 房屋。 / Housing.
	public static final int HOUSE_WH_MIN = 60;
	public static final int HOUSE_WH_MAX = 79;

	private int id;
	private int limit;
	private int length;
	private int specialLimit;

	private StorageType(int id, int limit, int length, int specialLimit) {
		this(id, limit, length);
		this.specialLimit = specialLimit;
	}

	private StorageType(int id, int limit, int length) {
		this(id);
		this.limit = limit;
		this.length = length;
	}

	private StorageType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取限制。 / Returns the limit. */
	public int getLimit() {
		return limit;
	}

	/** 返回 length / Returns the length */
	public int getLength() {
		return length;
	}

	/** 返回 special limit / Returns the special limit */
	public int getSpecialLimit() {
		return specialLimit;
	}

	/** 返回按 ID 的仓库类型 / Returns the storage type by id */
	public static StorageType getStorageTypeById(int id) {
		for (StorageType st : values()) {
			if (st.id == id) {
				return st;
			}
		}
		return null;
	}

	/** 返回仓库 ID / Returns the storage id */
	public static int getStorageId(int limit, int length) {
		for (StorageType st : values()) {
			if (st.limit == limit && st.length == length) {
				return st.id;
			}
		}
		return -1;
	}
}
