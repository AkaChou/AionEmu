package com.aionemu.gameserver.services.toypet;

import lombok.Getter;

/**
 * 宠物饥饿等级。
 * Pet hunger level.
 * @author Rolandas
 */
@Getter
public enum PetHungryLevel {
	/** 饥饿 / Hungry */
	HUNGRY(0),
	/** 满足 / Content */
	CONTENT(1),
	/** 半饱 / Semi-full */
	SEMIFULL(2),
	/** 吃饱 / Full */
	FULL(3);

	/**
	 * 返回等级对应的数值。
	 * Returns the numeric value of this level.
	 * Level value
	 */
	private final byte value;

	PetHungryLevel(int value) {
		this.value = (byte) value;
	}

	/**
	 * 返回下一饥饿等级；已满时回到饥饿。
	 * Returns the next hunger level; wraps from full back to hungry.
	 * Next level
	 */
	public PetHungryLevel getNextValue() {
		byte levelValue = value;
        return switch (levelValue) {
            case 0 -> CONTENT;
            case 1 -> SEMIFULL;
            case 2 -> FULL;
            case 3 -> HUNGRY;
            default -> HUNGRY;
        };
	}

	/**
	 * 按数值解析饥饿等级。
	 * Resolve hunger level by numeric id.
	 * @param value 等级数值 / Level value
	 * @return 对应枚举常量 / Matching enum constant
	 */
	public static PetHungryLevel fromId(int value) {
		return PetHungryLevel.values()[value];
	}
}
