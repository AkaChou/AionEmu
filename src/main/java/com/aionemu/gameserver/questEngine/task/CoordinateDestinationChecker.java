package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 坐标目的地检查器：跟随生物进入指定 XYZ 附近（半径 10）即判定到达。
 * Coordinate destination checker: success when the follower is within range 10 of the given XYZ.
 */
final class CoordinateDestinationChecker extends DestinationChecker {

	/** 目标 X。 Target X. */
	private final float x;
	/** 目标 Y。 Target Y. */
	private final float y;
	/** 目标 Z。 Target Z. */
	private final float z;

	/**
	 * 构造坐标目的地检查器。
	 * Constructs a coordinate destination checker.
	 *
	 * Follower creature
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 */
	CoordinateDestinationChecker(Creature follower, float x, float y, float z) {
		this.follower = follower;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * 判断跟随者是否接近目标坐标。
	 * Returns whether the follower is near the target coordinates.
	 *
	 * @return true 表示在半径 10 内 / true if within range 10
	 */
	@Override
	boolean check() {
		return MathUtil.isNearCoordinates(follower, x, y, z, 10);
	}
}
