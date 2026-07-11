package com.aionemu.gameserver.geoEngine.collision;

import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 运动许可监听器，在位移发生前检查是否允许，并按需修正位置与速度。
 * Motion-allowed listener that checks whether a move is permitted and may
 * adjust position and velocity when it is not.
 */
public interface MotionAllowedListener {

	/**
	 * 检查运动是否允许；不允许时就地修改位置与速度向量。
	 * Checks if motion is allowed; modifies position and velocity in place when not.
	 *
	 * @param position 当前位置（可修改） / current position (mutable)
	 * @param velocity 当前速度（可修改） / current velocity (mutable)
	 */
	public void checkMotionAllowed(Vector3f position, Vector3f velocity);
}
