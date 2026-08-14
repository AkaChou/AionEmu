package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 跟随目的地检查器抽象基类，判断跟随生物是否到达目标条件。
 * Abstract base for follow-destination checkers that decide whether a follower has reached its target condition.
 */
abstract class DestinationChecker {

	/** 正在跟随的生物。 Creature currently following. */
	protected Creature follower;

	/**
	 * 检查是否已到达目的地/满足目标条件。
	 * Checks whether the destination is reached / the target condition is met.
	 *
	 * @return 若已到达目的地则为 true / true if destination reached
	 */
	abstract boolean check();
}
