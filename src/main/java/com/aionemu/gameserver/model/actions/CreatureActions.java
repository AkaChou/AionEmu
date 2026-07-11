package com.aionemu.gameserver.model.actions;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 生物 Actions，用于 actions 相关逻辑。
 * Creature Actions for actions logic.
 */

public class CreatureActions {
	/** 获取名称。 / Returns the name. */
	public static String getName(Creature creature) {
		return creature.getName();
	}

	/**
	 * @param creature 是否已经 dead / 是否已经 dead。 / Whether already dead / Whether already dead
	 */
	public static boolean isAlreadyDead(Creature creature) {
		return creature.getLifeStats().isAlreadyDead();
	}

	/** 删除。 / Delete. */
	public static void delete(Creature creature) {
		if (creature != null) {
			creature.getController().onDelete();
		}
	}
}
