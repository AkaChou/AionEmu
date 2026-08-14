package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 目标生物目的地检查器：跟随生物进入目标生物 3D 距离 10 内即判定到达。
 * Target-creature destination checker: success when the follower is within 3D range 10 of the target.
 */
final class TargetDestinationChecker extends DestinationChecker {

	/** 目标生物。 Target creature. */
	private final Creature target;

	/**
	 * 构造目标生物目的地检查器。
	 * Constructs a target-creature destination checker.
	 *
	 * @param follower 跟随生物 / follower creature
	 * @param target 目标生物 / target creature
	 */
	TargetDestinationChecker(Creature follower, Creature target) {
		this.follower = follower;
		this.target = target;
	}

	/**
	 * 判断跟随者是否接近目标生物。
	 * Returns whether the follower is near the target creature.
	 *
	 * @return 3D 距离 ≤ 10 时为 true / true if 3D distance ≤ 10
	 */
	@Override
	boolean check() {
		return MathUtil.isIn3dRange(target, follower, 10);
	}
}
