package com.aionemu.gameserver.services.toypet;

/**
 * 宠物饥饿等级。
 * Pet hunger level.
 *
 * @author Rolandas
 */
public enum PetHungryLevel {
	/** 饥饿 / Hungry */
	HUNGRY(0),
	/** 满足 / Content */
	CONTENT(1),
	/** 半饱 / Semi-full */
	SEMIFULL(2),
	/** 吃饱 / Full */
	FULL(3);

	private byte value;

	PetHungryLevel(int value) {
		this.value = (byte) value;
	}

	/**
	 * 返回等级对应的数值。
	 * Returns the numeric value of this level.
	 *
	 * Level value
	 */
	public byte getValue() {
		return value;
	}

	/**
	 * 返回下一饥饿等级；已满时回到饥饿。
	 * Returns the next hunger level; wraps from full back to hungry.
	 *
	 * Next level
	 */
	public PetHungryLevel getNextValue() {
		byte levelValue = value;
		switch (levelValue) {
		case 0:
			return CONTENT;
		case 1:
			return SEMIFULL;
		case 2:
			return FULL;
		case 3:
			return HUNGRY;
		default:
			return HUNGRY;
		}
	}

	/**
	 * 按数值解析饥饿等级。
	 * Resolve hunger level by numeric id.
	 *
	 * @param value 等级数值 / Level value
	 * @return 对应枚举常量 / Matching enum constant
	 */
	public static PetHungryLevel fromId(int value) {
		return PetHungryLevel.values()[value];
	}
}
