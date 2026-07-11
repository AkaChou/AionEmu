package com.aionemu.gameserver.model.gameobjects.state;

/**
 * 生物视野状态枚举。
 * Creature See State enumeration.
 *
 * @author Sweetkr
 */
public enum CreatureSeeState {
	NORMAL(0), // Normal
	SEARCH1(1), // See-Through: Hide I
	SEARCH2(2), // See-Through: Hide II
	SEARCH5(5), // no idea :)
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
