package com.aionemu.gameserver.model.gameobjects.state;

/**
 * 生物状态枚举。
 * Creature State enumeration.
 *
 * @author ATracer, Sweetkr
 */
public enum CreatureState {
	/** 激活 / Active. */
	ACTIVE(1), FLYING(2), FLIGHT_TELEPORT(2), RESTING(4), DEAD(7), CHAIR(6), FLOATING_CORPSE(8), PRIVATE_SHOP(10),
	/** 拾取中 / Looting. */
	LOOTING(12), WEAPON_EQUIPPED(32), WALKING(64), NPC_IDLE(64), POWERSHARD(128), TREATMENT(256), GLIDING(512);

	/**
	 * 站立、路径飞行、自由飞行、骑乘、坐下、坐椅、死亡、飞行死亡、个人商店、拾取、飞行拾取、默认。 / Standing, path flying, free flying, riding, sitting, sitting on chair, dead, fly dead, private shop, looting, fly looting, default
	 */

	private int id;

	private CreatureState(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
