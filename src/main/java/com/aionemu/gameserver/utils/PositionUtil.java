package com.aionemu.gameserver.utils;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * 可见对象相对方位与包围盒相关计算工具。
 * Utilities for relative facing and bounding calculations between visible objects.
 */
public class PositionUtil {
	/** 判定背后/正面时允许的最大角度差 / Max allowed angle difference for behind/front checks */
	private static final float MAX_ANGLE_DIFF = 90f;

	/**
	 * 判断 object1 是否位于 object2 的背后。
	 * Checks whether object1 is behind object2.
	 *
	 * Observer
	 * Target
	 *
	 * @return 是否在背后 / Whether behind the target
	 */
	public static boolean isBehindTarget(VisibleObject object1, VisibleObject object2) {
		float angleObject1 = MathUtil.calculateAngleFrom(object1, object2);
		float angleObject2 = MathUtil.convertHeadingToDegree(object2.getHeading());
		float angleDiff = angleObject1 - angleObject2;
		if (angleDiff <= -360 + MAX_ANGLE_DIFF) {
			angleDiff += 360;
		}
		if (angleDiff >= 360 - MAX_ANGLE_DIFF) {
			angleDiff -= 360;
		}
		return Math.abs(angleDiff) <= MAX_ANGLE_DIFF;
	}

	/**
	 * 判断 object1 是否位于 object2 的正前方。
	 * Checks whether object1 is in front of object2.
	 *
	 * Observer
	 * Target
	 *
	 * @return 是否在正前方 / Whether in front of the target
	 */
	public static boolean isInFrontOfTarget(VisibleObject object1, VisibleObject object2) {
		float angleObject2 = MathUtil.calculateAngleFrom(object2, object1);
		float angleObject1 = MathUtil.convertHeadingToDegree(object2.getHeading());
		float angleDiff = angleObject1 - angleObject2;
		if (angleDiff <= -360 + MAX_ANGLE_DIFF) {
			angleDiff += 360;
		}
		if (angleDiff >= 360 - MAX_ANGLE_DIFF) {
			angleDiff -= 360;
		}
		return Math.abs(angleDiff) <= MAX_ANGLE_DIFF;
	}

	/**
	 * 通过朝向射线判断 object2 是否在 object1 的背后半平面内。
	 * Uses a heading ray to check whether object2 lies in the half-plane behind object1.
	 *
	 * Reference object
	 *
	 * @param object2 待判定对象 / Object to test
	 * @param object2 @return 是否在背后半平面 / Whether in the behind half-plane
	 */
	public static boolean isBehind(VisibleObject object1, VisibleObject object2) {
		float angle = MathUtil.convertHeadingToDegree(object1.getHeading()) + 90;
		if (angle >= 360) {
			angle -= 360;
		}
		double radian = Math.toRadians(angle);
		float x0 = object1.getX();
		float y0 = object1.getY();
		float x1 = (float) (Math.cos(radian) * 5) + x0;
		float y1 = (float) (Math.sin(radian) * 5) + y0;
		float xA = object2.getX();
		float yA = object2.getY();
		float temp = (x1 - x0) * (yA - y0) - (y1 - y0) * (xA - x0);
		return temp > 0;
	}

	/**
	 * 计算 object1 朝向到 object2 的相对角度差。
	 * Calculates the relative angle from object1's facing to object2.
	 *
	 * Reference object
	 * Target object
	 * Angle difference in degrees
	 */
	public static float getAngleToTarget(VisibleObject object1, VisibleObject object2) {
		float angleObject1 = MathUtil.convertHeadingToDegree(object1.getHeading()) - 180;
		if (angleObject1 < 0) {
			angleObject1 += 360;
		}
		float angleObject2 = MathUtil.calculateAngleFrom(object1, object2);
		float angleDiff = angleObject1 - angleObject2 - 180;
		if (angleDiff < 0) {
			angleDiff += 360;
		}
		return angleDiff;
	}

	/**
	 * 计算两对象在相对方向上的包围半径修正量。
	 * Calculates the directional bound correction between two objects.
	 *
	 * Object 1
	 * Object 2
	 * @param inverseTarget 是否反转目标方向 / Whether to invert the target direction
	 * @return 方向包围修正量 / Directional bound correction
	 */
	public static float getDirectionalBound(VisibleObject object1, VisibleObject object2, boolean inverseTarget) {
		float angle = 90 - (inverseTarget ? getAngleToTarget(object2, object1) : getAngleToTarget(object1, object2));
		if (angle < 0) {
			angle += 360;
		}
		double radians = Math.toRadians(angle);
		float x1 = (float) (object1.getX()
				+ object1.getObjectTemplate().getBoundRadius().getSide() * Math.cos(radians));
		float y1 = (float) (object1.getY()
				+ object1.getObjectTemplate().getBoundRadius().getFront() * Math.sin(radians));
		float x2 = (float) (object2.getX()
				+ object2.getObjectTemplate().getBoundRadius().getSide() * Math.cos(Math.PI + radians));
		float y2 = (float) (object2.getY()
				+ object2.getObjectTemplate().getBoundRadius().getFront() * Math.sin(Math.PI + radians));
		float bound1 = (float) MathUtil.getDistance(object1.getX(), object1.getY(), x1, y1);
		float bound2 = (float) MathUtil.getDistance(object2.getX(), object2.getY(), x2, y2);
		return bound1 - bound2;
	}

	/**
	 * 计算两对象方向包围修正量（不反转目标）。
	 * Calculates the directional bound correction without inverting the target.
	 *
	 * Object 1
	 * Object 2
	 * @return 方向包围修正量 / Directional bound correction
	 */
	public static float getDirectionalBound(VisibleObject object1, VisibleObject object2) {
		return getDirectionalBound(object1, object2, false);
	}

	/**
	 * 计算从 fromObject 指向 object 的远离朝向。
	 * Calculates the heading to move away from fromObject toward object.
	 *
	 * Source object
	 * Target object
	 * Heading byte
	 */
	public static byte getMoveAwayHeading(VisibleObject fromObject, VisibleObject object) {
		float angle = MathUtil.calculateAngleFrom(fromObject, object);
		byte heading = MathUtil.convertDegreeToHeading(angle);
		return heading;
	}
}
