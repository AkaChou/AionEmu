package com.aionemu.gameserver.model.gameobjects.state;

/**
 * 生物视野状态枚举。
 * Creature See State enumeration.
 *
 * @author Sweetkr
 */
public enum CreatureSeeState {
	NORMAL(0), // 正常 / Normal
	SEARCH1(1), // 透视：隐藏 I / See-Through: Hide I
	SEARCH2(2), // 透视：隐藏 II / See-Through: Hide II
	SEARCH5(5), // 未知效果 / no idea :)
	SEARCH10(10);

	private int id;

	private CreatureSeeState(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
