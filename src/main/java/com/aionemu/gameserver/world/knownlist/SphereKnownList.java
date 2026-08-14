package com.aionemu.gameserver.world.knownlist;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 基于固定球体半径做反向距离判定的玩家感知已知列表。
 * Player-aware known list that uses a fixed sphere radius for reverse range checks.
 *
 * @author ATracer
 */
public class SphereKnownList extends PlayerAwareKnownList {

	/**
	 * 球体判定半径。
	 * Sphere check radius.
	 */
	private final float radius;

	/**
	 * 创建球体半径已知列表。
	 * Creates a sphere-radius known list.
	 *
	 * @param owner 列表所有者 / list owner
	 * @param radius 球体判定半径 / sphere radius
	 */
	public SphereKnownList(VisibleObject owner, float radius) {
		super(owner);
		this.radius = radius;
	}

	/**
	 * {@inheritDoc}
	 *
	 * 使用本列表的球体半径做 3D 距离判定。
	 * Uses this list's sphere radius for a 3D distance check.
	 */
	@Override
	protected boolean checkReversedObjectInRange(VisibleObject newObject) {
		return MathUtil.isIn3dRange(owner, newObject, radius);
	}
}
