package com.aionemu.gameserver.controllers.attack;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 玩家专用仇恨列表：只要目标不是自己即可感知，不受敌对阵营限制。
 * Player-specific aggro list that is aware of any creature other than the owner.
 *
 * @author ATracer
 */
public class PlayerAggroList extends AggroList {

	/**
	 * 为指定玩家创建仇恨列表。
	 * Creates an aggro list for the given player-owned creature.
	 *
	 * @param owner 列表所属单位 / list owner
	 */
	public PlayerAggroList(Creature owner) {
		super(owner);
	}

	/**
	 * 判断是否感知该生物：非空且不是自身即可。
	 * Returns whether this list is aware of the creature: non-null and not self.
	 *
	 * @param creature 待判断生物 / creature to check
	 * whether aware
	 */
	@Override
	protected boolean isAware(Creature creature) {
		return creature != null && !creature.getObjectId().equals(owner.getObjectId());
	}
}
