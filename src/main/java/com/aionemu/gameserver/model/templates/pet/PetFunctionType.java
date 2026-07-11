package com.aionemu.gameserver.model.templates.pet;

/**
 * 宠物函数类型枚举。
 * Pet Function Type enumeration.
 *
 * @author Rinzler Formula: dataBitCount*2^5 OR id
 */
public enum PetFunctionType {
	/** 仓库。 / Warehouse. */
	WAREHOUSE(0, true), FOOD(1, 64), DOPING(2, 256), LOOT(3, 8), APPEARANCE(1), NONE(4, true), CHEER(5), // need test
	/** 商品 / Merchand. */
	MERCHAND(6), // need test
	/** 包 / Bag. */
	BAG(-1), WING(-2);

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
