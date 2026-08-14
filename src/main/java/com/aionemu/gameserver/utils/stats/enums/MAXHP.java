package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业最大生命值系数枚举。
 * Max HP coefficient enum by player class.
 */
public enum MAXHP {
	WARRIOR(1.1688f, 1.1688f, 284), GLADIATOR(1.3393f, 48.246f, 342), TEMPLAR(1.3288f, 51.878f, 281),
	SCOUT(1.0297f, 40.823f, 219), ASSASSIN(1.0488f, 40.38f, 222), RANGER(0.5f, 38.5f, 133), MAGE(0.7554f, 29.457f, 132),
	SORCERER(0.6352f, 24.852f, 112), SPIRIT_MASTER(1, 20.6f, 157), PRIEST(1.0303f, 40.824f, 201),
	CLERIC(0.9277f, 35.988f, 229), CHANTER(0.9277f, 35.988f, 229),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(1.0297f, 40.823f, 219), GUNSLINGER(1.0488f, 40.38f, 222), MUSE(0.7554f, 29.457f, 132),
	SONGWEAVER(1, 20.6f, 157),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(0.9277f, 35.988f, 229);

	/**
	 * 最大生命公式二次项系数 a。
	 * Quadratic coefficient a of the max-HP formula.
	 */
	private float a;

	/**
	 * 最大生命公式一次项系数 b。
	 * Linear coefficient b of the max-HP formula.
	 */
	private float b;

	/**
	 * 最大生命公式常数项 c。
	 * Constant coefficient c of the max-HP formula.
	 */
	private float c;

	private MAXHP(float a, float b, float c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	/**
	 * 按等级计算最大生命值。
	 * Calculate max HP for given level.
	 *
	 * @param level 角色等级 / Character level
	 * @return 最大生命值 / Max HP
	 */
	public int getMaxHpFor(int level) {
		return Math.round(a * (level - 1) * (level - 1) + b * (level - 1) + c);
	}
}
