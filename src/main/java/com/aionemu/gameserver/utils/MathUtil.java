package com.aionemu.gameserver.utils;

import java.awt.Point;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

import com.aionemu.gameserver.controllers.movement.NpcMoveController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.templates.zone.Point2D;

/**
 * 游戏数学工具：距离、范围判定、朝向/角度与几何采样。
 * Game math utilities: distance, range checks, heading/angle, and geometric sampling.
 */
public class MathUtil {

	/**
	 * 计算两点（2D 模板点）的欧氏距离。
	 * Compute Euclidean distance between two 2D template points.
	 *
	 * Point 1
	 * Point 2
	 * Distance
	 */
	public static double getDistance(Point2D point1, Point2D point2) {
		return getDistance(point1.getX(), point1.getY(), point2.getX(), point2.getY());
	}

	/**
	 * 计算二维坐标欧氏距离。
	 * Compute 2D Euclidean distance.
	 *
	 * @param x1 点 1 的 X / X of point 1
	 * @param y1 点 1 的 Y / Y of point 1
	 * @param x2 点 2 的 X / X of point 2
	 * @param y2 点 2 的 Y / Y of point 2
	 * Distance
	 */
	public static double getDistance(float x1, float y1, float x2, float y2) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * 计算两点（3D）的欧氏距离；任一点为 null 时返回 0。
	 * Compute 3D Euclidean distance; returns 0 if either point is null.
	 *
	 * Point 1
	 * Point 2
	 * Distance
	 */
	public static double getDistance(Point3D point1, Point3D point2) {
		if (point1 == null || point2 == null) {
			return 0;
		}
		return getDistance(point1.getX(), point1.getY(), point1.getZ(), point2.getX(), point2.getY(), point2.getZ());
	}

	/**
	 * 计算三维坐标欧氏距离。
	 * Compute 3D Euclidean distance.
	 *
	 * @param x1 点 1 的 X / X of point 1
	 * @param y1 点 1 的 Y / Y of point 1
	 * @param z1 点 1 的 Z / Z of point 1
	 * @param x2 点 2 的 X / X of point 2
	 * @param y2 点 2 的 Y / Y of point 2
	 * @param z2 点 2 的 Z / Z of point 2
	 * Distance
	 */
	public static double getDistance(float x1, float y1, float z1, float x2, float y2, float z2) {
		float dx = x1 - x2;
		float dy = y1 - y2;
		float dz = z1 - z2;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * 计算可见对象到目标坐标的 3D 距离。
	 * Compute 3D distance from a visible object to a target position.
	 *
	 * Visible object
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * Distance
	 */
	public static double getDistance(VisibleObject object, float x, float y, float z) {
		return getDistance(object.getX(), object.getY(), object.getZ(), x, y, z);
	}

	/**
	 * 计算两个可见对象之间的 3D 距离。
	 * Compute 3D distance between two visible objects.
	 *
	 * Object 1
	 * Object 2
	 * Distance
	 */
	public static double getDistance(VisibleObject object, VisibleObject object2) {
		return getDistance(object.getX(), object.getY(), object.getZ(), object2.getX(), object2.getY(), object2.getZ());
	}

	/**
	 * 获取点到线段最近点（AWT Point 版本）。
	 * Closest point on a segment to a given point (AWT Point version).
	 *
	 * @param ss 线段起点 / Segment start
	 * @param se 线段终点 / Segment end
	 * @param p 查询点 / Query point
	 * Closest point
	 */
	public static Point2D getClosestPointOnSegment(Point ss, Point se, Point p) {
		return getClosestPointOnSegment(ss.x, ss.y, se.x, se.y, p.x, p.y);
	}

	/**
	 * 获取点到线段最近点（浮点坐标）。
	 * Closest point on a segment to a given point (float coordinates).
	 *
	 * @param sx1 线段起点 X / Segment start X
	 * @param sy1 线段起点 Y / Segment start Y
	 * @param sx2 线段终点 X / Segment end X
	 * @param sy2 线段终点 Y / Segment end Y
	 * @param px 查询点 X / Query point X
	 * @param py 查询点 Y / Query point Y
	 * Closest point
	 * When the segment is degenerate。
	 */
	public static Point2D getClosestPointOnSegment(float sx1, float sy1, float sx2, float sy2, float px, float py) {
		double xDelta = sx2 - sx1;
		double yDelta = sy2 - sy1;
		if ((xDelta == 0) && (yDelta == 0)) {
			throw new IllegalArgumentException("Segment start equals segment end");
		}
		double u = ((px - sx1) * xDelta + (py - sy1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		final Point2D closestPoint;
		if (u < 0) {
			closestPoint = new Point2D(sx1, sy1);
		} else if (u > 1) {
			closestPoint = new Point2D(sx2, sy2);
		} else {
			closestPoint = new Point2D((float) (sx1 + u * xDelta), (float) (sy1 + u * yDelta));
		}
		return closestPoint;
	}

	/**
	 * 点到线段的距离（AWT Point 版本）。
	 * Distance from a point to a segment (AWT Point version).
	 *
	 * @param ss 线段起点 / Segment start
	 * @param se 线段终点 / Segment end
	 * @param p 查询点 / Query point
	 * Distance
	 */
	public static double getDistanceToSegment(Point ss, Point se, Point p) {
		return getDistanceToSegment(ss.x, ss.y, se.x, se.y, p.x, p.y);
	}

	/**
	 * 点到线段的距离（整型坐标）。
	 * Distance from a point to a segment (integer coordinates).
	 *
	 * @param sx1 线段起点 X / Segment start X
	 * @param sy1 线段起点 Y / Segment start Y
	 * @param sx2 线段终点 X / Segment end X
	 * @param sy2 线段终点 Y / Segment end Y
	 * @param px 查询点 X / Query point X
	 * @param py 查询点 Y / Query point Y
	 * Distance
	 */
	public static double getDistanceToSegment(int sx1, int sy1, int sx2, int sy2, int px, int py) {
		Point2D closestPoint = getClosestPointOnSegment(sx1, sy1, sx2, sy2, px, py);
		return getDistance(closestPoint.getX(), closestPoint.getY(), px, py);
	}

	/**
	 * 判断两对象是否在同一世界/实例且 2D 距离小于 range。
	 * Whether two objects share world/instance and are within 2D range.
	 *
	 * Object 1
	 * Object 2
	 * @param range 范围半径 / Range radius
	 * @return 在范围内返回 true / True if in range
	 */
	public static boolean isInRange(VisibleObject object1, VisibleObject object2, float range) {
		if (object1.getWorldId() != object2.getWorldId() || object1.getInstanceId() != object2.getInstanceId()) {
			return false;
		}
		float dx = (object2.getX() - object1.getX());
		float dy = (object2.getY() - object1.getY());
		return dx * dx + dy * dy < range * range;
	}

	/**
	 * 判断两对象是否在同一世界/实例且 3D 距离小于 range。
	 * Whether two objects share world/instance and are within 3D range.
	 *
	 * Object 1
	 * Object 2
	 * @param range 范围半径 / Range radius
	 * @return 在范围内返回 true / True if in range
	 */
	public static boolean isIn3dRange(VisibleObject object1, VisibleObject object2, float range) {
		if (object1.getWorldId() != object2.getWorldId() || object1.getInstanceId() != object2.getInstanceId()) {
			return false;
		}
		float dx = (object2.getX() - object1.getX());
		float dy = (object2.getY() - object1.getY());
		float dz = (object2.getZ() - object1.getZ());
		return dx * dx + dy * dy + dz * dz < range * range;
	}

	/**
	 * 判断两对象 3D 距离是否落在 [minRange, maxRange) 区间。
	 * Whether 3D distance between two objects is in [minRange, maxRange).
	 *
	 * Object 1
	 * Object 2
	 * Minimum range
	 * Maximum range
	 *
	 * @return 在区间内返回 true / True if within the band
	 */
	public static boolean isIn3dRangeLimited(VisibleObject object1, VisibleObject object2, float minRange,
			float maxRange) {
		if (object1.getWorldId() != object2.getWorldId() || object1.getInstanceId() != object2.getInstanceId()) {
			return false;
		}
		float dx = (object2.getX() - object1.getX());
		float dy = (object2.getY() - object1.getY());
		float dz = (object2.getZ() - object1.getZ());
		return dx * dx + dy * dy + dz * dz > minRange * minRange && dx * dx + dy * dy + dz * dz < maxRange * maxRange;
	}

	/**
	 * 判断两组 3D 坐标是否在给定半径内。
	 * Whether two 3D positions are within the given radius.
	 *
	 * Object 1 X
	 * Object 1 Y
	 * Object 1 Z
	 * Object 2 X
	 * Object 2 Y
	 * Object 2 Z
	 * @param range 范围半径 / Range radius
	 * @return 在范围内返回 true / True if in range
	 */
	public static boolean isIn3dRange(final float obj1X, final float obj1Y, final float obj1Z, final float obj2X,
			final float obj2Y, final float obj2Z, float range) {
		float dx = (obj2X - obj1X);
		float dy = (obj2Y - obj1Y);
		float dz = (obj2Z - obj1Z);
		return dx * dx + dy * dy + dz * dz < range * range;
	}

	/**
	 * 判断可见对象是否在以中心点为球心的球体内。
	 * Whether a visible object lies inside a sphere.
	 *
	 * Object
	 * Sphere center X
	 * Sphere center Y
	 * Sphere center Z
	 * Radius
	 *
	 * @return 在球内返回 true / True if inside the sphere
	 */
	public static boolean isInSphere(final VisibleObject obj, final float centerX, final float centerY,
			final float centerZ, final float radius) {
		float dx = (obj.getX() - centerX);
		float dy = (obj.getY() - centerY);
		float dz = (obj.getZ() - centerZ);
		return dx * dx + dy * dy + dz * dz < radius * radius;
	}

	/**
	 * 计算从 (obj1X, obj1Y) 指向 (obj2X, obj2Y) 的角度（度，0–360）。
	 * Angle in degrees from (obj1X, obj1Y) toward (obj2X, obj2Y), range 0–360.
	 *
	 * Origin X
	 * Origin Y
	 * Target X
	 * Target Y
	 * Angle in degrees
	 */
	public final static float calculateAngleFrom(float obj1X, float obj1Y, float obj2X, float obj2Y) {
		float angleTarget = (float) Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0) {
			angleTarget = 360 + angleTarget;
		}
		return angleTarget;
	}

	/**
	 * 计算从 obj1 指向 obj2 的角度（度）。
	 * Angle in degrees from obj1 toward obj2.
	 *
	 * @param obj1 起点对象 / Origin object
	 * @param obj2 终点对象 / Target object
	 * Angle in degrees
	 */
	public static float calculateAngleFrom(VisibleObject obj1, VisibleObject obj2) {
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}

	/**
	 * 将客户端 Heading（0–120）转换为角度（度）。
	 * Convert client heading (0–120) to degrees.
	 *
	 * @param clientHeading 客户端朝向 / Client heading
	 * Degrees
	 */
	public final static float convertHeadingToDegree(byte clientHeading) {
		float degree = clientHeading * 3;
		return degree;
	}

	/**
	 * 计算从 obj1 到 obj2 的估计 Heading 值。
	 * Estimate heading from obj1 toward obj2.
	 * 使用菱形角度算法优化计算性能。
	 * Uses diamond-angle approximation for performance.
	 *
	 * @param obj1 起点对象 / Origin object
	 * @param obj2 终点对象 / Target object
	 * Estimated heading (0–120)
	 */
	public final static byte estimateHeadingFrom(VisibleObject obj1, VisibleObject obj2) {
		return (byte) (diamondAngle(obj2.getX() - obj1.getX(), obj2.getY() - obj1.getY()) * 30F);
	}

	/**
	 * 根据坐标差值估计 Heading 值。
	 * Estimate heading from coordinate deltas.
	 * 使用菱形角度算法优化计算性能。
	 * Uses diamond-angle approximation for performance.
	 *
	 * X delta
	 * Y delta
	 * Estimated heading (0–120)
	 */
	public final static byte estimateHeadingFrom(float deltaX, float deltaY) {
		return (byte) (diamondAngle(deltaX, deltaY) * 30F);
	}

	/**
	 * 菱形角度算法（Diamond Angle）。
	 * Diamond-angle algorithm.
	 * 用于高效近似 atan2，避免耗时三角函数；结果与标准 atan2 一致量级。
	 * Efficient atan2 approximation without expensive trig; consistent with atan2 scale.
	 *
	 * @param x X 坐标差值 / X delta
	 * @param y Y 坐标差值 / Y delta
	 * @return 角度值 (0–3 范围) / Angle value in range 0–3
	 */
	public final static float diamondAngle(float x, float y) {
		if (y >= 0) {
			if (y == 0 && x == 0) return 0;
			return (x >= 0 ? y / (x + y) : 1 - x / (-x + y));
		}
		return (x < 0 ? 2 - y / (-x - y) : 3 + x / (x - y));
	}

	/**
	 * 将角度（度）转换为客户端 Heading。
	 * Convert degrees to client heading.
	 *
	 * Angle in degrees
	 * Heading value
	 */
	public final static byte convertDegreeToHeading(float angle) {
		return (byte) (angle / 3);
	}

	/**
	 * 判断对象是否接近目标坐标（含移动检测偏移）。
	 * Whether an object is near a target position (includes move-check offset).
	 *
	 * Object
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * Extra offset
	 *
	 * @return 若 near 则为 true / True if near
	 */
	public final static boolean isNearCoordinates(VisibleObject obj, float x, float y, float z, float offset) {
		return getDistance(obj.getX(), obj.getY(), obj.getZ(), x, y, z) < offset + NpcMoveController.MOVE_CHECK_OFFSET;
	}

	/**
	 * 判断两对象是否接近（含移动检测偏移）。
	 * Whether two objects are near each other (includes move-check offset).
	 *
	 * Object 1
	 * Object 2
	 * Extra offset
	 *
	 * @return 若 near 则为 true / True if near
	 */
	public final static boolean isNearCoordinates(VisibleObject obj, VisibleObject obj2, int offset) {
		return getDistance(obj.getX(), obj.getY(), obj.getZ(), obj2.getX(), obj2.getY(), obj2.getZ()) < offset
				+ NpcMoveController.MOVE_CHECK_OFFSET;
	}

	/**
	 * 判断是否在攻击范围内（含碰撞半径与移动补偿）。
	 * Whether targets are within attack range (collision radii and move compensation).
	 *
	 * Creature 1
	 * Creature 2
	 * @param range 攻击范围 / Attack range
	 * @return 在攻击范围内返回 true / True if in attack range
	 */
	public final static boolean isInAttackRange(Creature object1, Creature object2, float range) {
		if (object1 == null || object2 == null) {
			return false;
		}
		if (object1.getWorldId() != object2.getWorldId() || object1.getInstanceId() != object2.getInstanceId()) {
			return false;
		}
		float offset = object1.getObjectTemplate().getBoundRadius().getCollision()
				+ object2.getObjectTemplate().getBoundRadius().getCollision();
		// 修复：移动补偿应该累加，而不是覆盖碰撞半径
		if (object1.getMoveController().isInMove()) {
			offset += 3f;
		}
		if (object2.getMoveController().isInMove()) {
			offset += 3f;
		}
		return ((getDistance(object1, object2) - offset) <= range);
	}

	/**
	 * 判断 obj2 是否位于 obj1 朝向前/后的攻击圆柱体内。
	 * Whether obj2 is inside the attack cylinder in front of or behind obj1.
	 *
	 * Source object
	 * @param obj2 目标对象 / Target object
	 * Cylinder length
	 * @param radius 圆柱半径（平方比较侧） / Cylinder radius (squared-side compare)
	 * @param isFront true 为前方，false 为后方 / true front, false rear
	 * @return 在圆柱内返回 true / True if inside the cylinder
	 */
	public final static boolean isInsideAttackCylinder(VisibleObject obj1, VisibleObject obj2, int length, int radius,
			boolean isFront) {
		double radian = Math.toRadians(convertHeadingToDegree(obj1.getHeading()));
		int direction = isFront ? 0 : 1;
		float dx = (float) (Math.cos(Math.PI * direction + radian) * length);
		float dy = (float) (Math.sin(Math.PI * direction + radian) * length);
		float tdx = obj2.getX() - obj1.getX();
		float tdy = obj2.getY() - obj1.getY();
		float tdz = obj2.getZ() - obj1.getZ();
		float lengthSqr = length * length;
		float dot = tdx * dx + tdy * dy;
		if (dot < 0.0f || dot > lengthSqr) {
			return false;
		}
		return (tdx * tdx + tdy * tdy + tdz * tdz) - dot * dot / lengthSqr <= radius;
	}

	/**
	 * 在圆内随机采样一个 2D 点。
	 * Sample a random 2D point inside a circle.
	 *
	 * Center X
	 * Center Y
	 * Radius
	 * Sampled point
	 */
	public final static Point get2DPointInsideCircle(float CenterX, float CenterY, int Radius) {
		double X = Math.random() * 2 - 1;
		double YMin = -Math.sqrt(1 - X * X);
		double YMax = Math.sqrt(1 - X * X);
		double Y = Math.random() * (YMax - YMin) + YMin;
		double finalX = X * Radius + CenterX;
		double finalY = Y * Radius + CenterY;
		return new Point((int) finalX, (int) finalY);
	}

	/**
	 * 按给定角度在圆周上取点。
	 * Point on circle circumference for a given angle in degrees.
	 *
	 * Center X
	 * Center Y
	 * Radius
	 * Angle in degrees
	 * Point on circumference
	 */
	public final static Point get2DPointOnCircleCircumference(float CenterX, float CenterY, int Radius,
			float angleInDegrees) {
		float finalX = (float) (Radius * Math.cos(angleInDegrees * Math.PI / 180F)) + CenterX;
		float finalY = (float) (Radius * Math.sin(angleInDegrees * Math.PI / 180F)) + CenterY;
		return new Point((int) finalX, (int) finalY);
	}

	/**
	 * 沿中心指向终点的方向，在给定半径圆周上取点。
	 * Point on circumference along the direction from center toward end point.
	 *
	 * Center point
	 * @param EndPoint 方向参考终点 / Direction end point
	 * Radius
	 * Point on circumference
	 */
	public final static Point get2DPointOnCircleCircumference(Point CenterPoint, Point EndPoint, int Radius) {
		double AngleinXAxis = getAngle(CenterPoint, EndPoint);
		float finalX = (float) (Radius * Math.cos(AngleinXAxis * Math.PI / 180F)) + CenterPoint.x;
		float finalY = (float) (Radius * Math.sin(AngleinXAxis * Math.PI / 180F)) + CenterPoint.y;
		return new Point((int) finalX, (int) finalY);
	}

	/**
	 * 计算从 P1 到 P2 的角度（度）。
	 * Angle in degrees from P1 to P2.
	 *
	 * @param P1 起点 / Origin
	 * @param P2 终点 / Target
	 * Angle in degrees
	 */
	public final static double getAngle(Point P1, Point P2) {
		float dx = P2.x - P1.x;
		float dy = P2.y - P1.y;
		double angle = Math.atan2(dx, dy) * 180 / Math.PI;
		return angle;
	}

	/**
	 * 圆上最接近给定点的点（投影到圆周）。
	 * Point on the circle closest to the given point (radial projection).
	 *
	 * Center
	 * Radius
	 * Reference point
	 * @return 圆周上最近点 / Closest point on circumference
	 */
	public final static Point get2DPointInsideCircleClosestTo(Point Center, int Radius, Point GivenPoint) {
		double vX = GivenPoint.x - Center.x;
		double vY = GivenPoint.y - Center.y;
		double magV = Math.sqrt(vX * vX + vY * vY);
		double aX = Center.x + vX / magV * Radius;
		double aY = Center.y + vY / magV * Radius;
		return new Point((int) aX, (int) aY);
	}

	/**
	 * 在圆环（annulus）内随机采样一个 2D 点。
	 * Sample a random 2D point inside an annulus.
	 *
	 * Center
	 * Outer radius
	 * Inner radius
	 * Sampled point
	 */
	public final static Point get2DPointInsideAnnulus(Point Center, int Radius1, int Radius2) {
		double theta = 360 * Math.random();
		double dist = Math.sqrt(Math.random() * (Radius1 * Radius1 - Radius2 * Radius2) + Radius2 * Radius2);
		double X = dist * Math.cos(theta) + Center.x;
		double Y = dist * Math.sin(theta) + Center.y;
		return new Point((int) X, (int) Y);
	}

	/**
	 * 判断对象是否位于 3D 圆环壳内（在大球内且不在小球内）。
	 * Whether an object lies in a 3D annular shell (inside outer, outside inner sphere).
	 *
	 * Object
	 * Center
	 * Outer radius
	 * Inner radius
	 *
	 * @return 在环壳内返回 true / True if inside the annulus
	 */
	public static boolean isInAnnulus(final VisibleObject obj, Point3D Center, float Radius1, float Radius2) {
		if (!isInSphere(obj, Center.getX(), Center.getY(), Center.getZ(), Radius2)) {
			if (isInSphere(obj, Center.getX(), Center.getY(), Center.getZ(), Radius1)) {
				return true;
			}
		}
		return false;
	}

	/** BigDecimal 常量 2 / BigDecimal constant two */
	static final BigDecimal TWO = new BigDecimal(2);

	/** 根号 10 近似值 / Approximate square root of 10 */
	static final double SQRT_10 = 3.162277660168379332;

	/**
	 * 高精度 BigDecimal 平方根（牛顿迭代）。
	 * High-precision BigDecimal square root (Newton iteration).
	 *
	 * Value to take the square root of
	 * @param rootMC 结果精度上下文 / Math context for the result
	 * Square root
	 * When the value is negative
	 * When precision is 0
	 */
	public static BigDecimal bigSqrt(BigDecimal squarD, MathContext rootMC) {
		int sign = squarD.signum();
		if (sign == -1) {
			throw new ArithmeticException("\nSquare root of a negative number: " + squarD);
		} else if (sign == 0) {
			return squarD.round(rootMC);
		}
		int prec = rootMC.getPrecision();
		if (prec == 0) {
			throw new IllegalArgumentException("\nMost roots won't have infinite precision = 0");
		}
		int BITS = 62;
		int nInit = 16;
		MathContext nMC = new MathContext(18, RoundingMode.HALF_DOWN);
		BigDecimal x = null, e = null;
		BigDecimal v = null, g = null;
		BigInteger bi = squarD.unscaledValue();
		int biLen = bi.bitLength();
		int shift = Math.max(0, biLen - BITS + (biLen % 2 == 0 ? 0 : 1));
		bi = bi.shiftRight(shift);
		double root = Math.sqrt(bi.doubleValue());
		BigDecimal halfBack = new BigDecimal(BigInteger.ONE.shiftLeft(shift / 2));
		int scale = squarD.scale();
		if (scale % 2 == 1) {
			root *= SQRT_10;
		}
		scale = (int) Math.floor(scale / 2.);
		x = new BigDecimal(root, nMC);
		x = x.multiply(halfBack, nMC);
		if (scale != 0) {
			x = x.movePointLeft(scale);
		}
		if (prec < nInit) {
			return x.round(rootMC);
		}
		v = BigDecimal.ONE.divide(TWO.multiply(x), nMC);
		ArrayList<Integer> nPrecs = new ArrayList<Integer>();
		assert nInit > 3 : "Never ending loop!";
		for (int m = prec + 1; m > nInit; m = m / 2 + (m > 100 ? 1 : 2)) {
			nPrecs.add(m);
		}
		for (int i = nPrecs.size() - 1; i > -1; i--) {
			nMC = new MathContext(nPrecs.get(i), (i % 2 == 1) ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN);
			e = squarD.subtract(x.multiply(x, nMC), nMC);
			if (i != 0) {
				x = x.add(e.multiply(v, nMC));
			} else {
				x = x.add(e.multiply(v, rootMC), rootMC);
				break;
			}
			g = BigDecimal.ONE.subtract(TWO.multiply(x).multiply(v, nMC));
			v = v.add(g.multiply(v, nMC));
		}
		return x;
	}
}
