package com.aionemu.gameserver.model.geometry;

import java.io.Serializable;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import lombok.Getter;
import lombok.Setter;

/**
 * 三维点，用于几何相关逻辑。
 * Point 3D for geometry logic.
 *
 * @author SoulKeeper
 */
@SuppressWarnings("serial")
@Getter
@Setter
public class Point3D implements Cloneable, Serializable {

	/**
	 * 点的 X 坐标。
	 * X coord of the point.
	 */
	private float x;

	/**
	 * 点的 Y 坐标。
	 * Y coord of the point.
	 */
	private float y;

	/**
	 * 点的 Z 坐标。
	 * Z coord of the point.
	 */
	private float z;

	/**
	 * 创建坐标为 0, 0, 0 的新点。
	 * Creates new point with coords 0, 0, 0.
	 */
	public Point3D() {
	}

	/**
	 * 由二维点和 Z 坐标创建新的三维点。
	 * Creates new 3D point from 2D point and z coord.
	 *
	 * @param point 二维点 / 2D point
	 * @param z     z 坐标 / z coord
	 */
	public Point3D(Point2D point, float z) {
		this(point.getX(), point.getY(), z);
	}

	/**
	 * 克隆另一个三维点。
	 * Clones another 3D point.
	 */
	public Point3D(Point3D point) {
		this(point.getX(), point.getY(), point.getZ());
	}

	/**
	 * 用给定坐标创建新的三维点。
	 * Creates new 3D point with given coords.
	 *
	 * @param x x 坐标 / x coord
	 * @param y y 坐标 / y coord
	 * @param z z 坐标 / z coord
	 */
	public Point3D(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * 检查该点是否与另一个点相等。
	 * Checks if this point is equal to another point.
	 *
	 * @param o 要比较的点 / point to compare with
	 * @return 相等返回 true / true if equal
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Point3D)) {
			return false;
		}
		Point3D point3D = (Point3D) o;
		return x == point3D.x && y == point3D.y && z == point3D.z;
	}

	/**
	 * 返回点的哈希码：result = x; result = 31 * result + y; result = 31 * result + z。
	 * Returns point's hashcode: result = x; result = 31 * result + y; result = 31 * result + z.
	 *
	 * @return 哈希码 / hashcode
	 */
	@Override
	public int hashCode() {
		float result = x;
		result = 31 * result + y;
		result = 31 * result + z;
		return (int) (result * 100);
	}

	/**
	 * @return 本点的副本 / Clones this point, copy of this point
	 * @throws CloneNotSupportedException 永不会抛出 / never thrown
	 */
	@Override
	public Point3D clone() throws CloneNotSupportedException {
		return new Point3D(this);
	}

	/**
	 * 本点的格式化字符串表示。
	 * Formatted string representation of this point.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Point3D");
		sb.append("{x=").append(x);
		sb.append(", y=").append(y);
		sb.append(", z=").append(z);
		sb.append('}');
		return sb.toString();
	}
}
