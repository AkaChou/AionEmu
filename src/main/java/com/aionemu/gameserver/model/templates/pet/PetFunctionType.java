package com.aionemu.gameserver.model.templates.pet;

/**
 * 宠物函数类型枚举。
 * Pet Function Type enumeration.
 *
 * @author Rinzler Formula: dataBitCount*2^5 OR id
 */
public enum PetFunctionType {
	/** 仓库。 / Warehouse. */
	WAREHOUSE(0, true),
	/** 食物。 / Food. */
	FOOD(1, 64),
	/** 兴奋剂。 / Doping. */
	DOPING(2, 256),
	/** 拾取。 / Loot. */
	LOOT(3, 8),
	/** 外观。 / Appearance. */
	APPEARANCE(1),
	/** 无。 / None. */
	NONE(4, true),
	/** 加油。 / Cheer. */
	CHEER(5), // 需要测试。 / need test
	/** 商品。 / Merchand. */
	MERCHAND(6), // 需要测试。 / need test
	/** 背包。 / Bag. */
	BAG(-1),
	/** 翅膀。 / Wing. */
	WING(-2);

	private short id;
	private boolean isPlayerFunc = false;

	PetFunctionType(int id, boolean isPlayerFunc) {
		this(id);
		this.isPlayerFunc = isPlayerFunc;
	}

	PetFunctionType(int id, int dataBitCount) {
		this(dataBitCount << 5 | id);
		this.isPlayerFunc = true;
	}

	PetFunctionType(int id) {
		this.id = (short) (id & 0xFFFF);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 是否为玩家函数。 / Whether player function. */
	public boolean isPlayerFunction() {
		return isPlayerFunc;
	}
}
