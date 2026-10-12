package com.aionemu.gameserver.skillengine.model;

import com.aionemu.gameserver.model.templates.item.WeaponType;
import org.jspecify.annotations.NonNull;

/**
 * 武器类型包装：主/副手组合，用于动作时间查表与比较。
 * Weapon type wrapper: main/off-hand pair for motion-time lookup and compare.
 *
 * @param mainHand 获取主手武器类型。
 *                 Gets main-hand weapon type.
 * @param offHand  获取副手武器类型。
 *                 Gets off-hand weapon type.
 * @author kecimis
 */
public record WeaponTypeWrapper(WeaponType mainHand, WeaponType offHand) implements Comparable<WeaponTypeWrapper> {

	/**
	 * 构造武器类型包装；双手持有时归一为单手剑双持键。
	 * Builds wrapper; dual-wield both hands normalize to dual 1H sword key.
	 */
	public WeaponTypeWrapper(WeaponType mainHand, WeaponType offHand) {
		if (mainHand != null && offHand != null) {
			this.mainHand = WeaponType.SWORD_1H;
			this.offHand = WeaponType.SWORD_1H;
		} else {
			this.mainHand = mainHand;
			this.offHand = offHand;
		}
	}

	/**
	 * 相等比较（主/副手）。
	 * Equality by main/off-hand.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WeaponTypeWrapper other = (WeaponTypeWrapper) obj;
		if (mainHand != other.mainHand) {
			return false;
		}
		return offHand == other.offHand;
	}

	/**
	 * 字符串表示。
	 * String representation.
	 */
	@Override
	public @NonNull String toString() {
		return "mainHandType=\"" + (mainHand != null ? mainHand.toString() : "null") + "\"" + " offHandType=\""
			+ (offHand != null ? offHand.toString() : "null");
	}

	/**
	 * 哈希码。
	 * Hash code.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mainHand == null) ? 0 : mainHand.hashCode());
		result = prime * result + ((offHand == null) ? 0 : offHand.hashCode());
		return result;
	}

	/**
	 * 比较武器包装（用于排序/查表）。
	 * Compares weapon wrappers (for sort/lookup).
	 *
	 * @param o 另一个包装 / other wrapper
	 */
	@Override
	public int compareTo(@NonNull WeaponTypeWrapper o) {
		if (mainHand == null || o.mainHand() == null) {
			return 0;
		} else if (offHand != null && o.offHand() != null) {
			return 0;
		} else if (offHand != null) {
			return 1;
		} else if (o.offHand() != null) {
			return -1;
		} else {
			return mainHand.toString().compareTo(o.mainHand().toString());
		}
	}
}
