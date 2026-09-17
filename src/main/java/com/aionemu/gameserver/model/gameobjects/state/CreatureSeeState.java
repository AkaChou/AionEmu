package com.aionemu.gameserver.model.gameobjects.state;

import lombok.Getter;

/**
 * 生物视野状态枚举。
 * Creature See State enumeration.
 *
 * @author Sweetkr
 */
@Getter
public enum CreatureSeeState {
	NORMAL(0), // 正常 / Normal
	SEARCH1(1), // 透视：隐藏 I / See-Through: Hide I
	SEARCH2(2), // 透视：隐藏 II / See-Through: Hide II
	SEARCH5(5), // 未知效果 / no idea :)
	SEARCH10(10);

	/**
	 * @return the id
	 */
	private final int id;

	CreatureSeeState(int id) {
		this.id = id;
	}
}
