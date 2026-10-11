package com.aionemu.gameserver.model.geometry;

import java.io.Serializable;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 三维点，用于几何相关逻辑。
 * Point 3D for geometry logic.
 * @author SoulKeeper
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
	 * 由二维点和 Z 坐标创建新的三维点。
	 * Creates new 3D point from 2D point and z coord.
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
	 * 检查该点是否与另一个点相等。
	 * Checks if this point is equal to another point.
	 * @param o 要比较的点 / point to compare with
	 * @return 相等返回 true / true if equal
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Point3D point3D)) {
			return false;
		}
		return x == point3D.x && y == point3D.y && z == point3D.z;
	}

	/**
	 * 返回点的哈希码：result = x; result = 31 * result + y; result = 31 * result + z。
	 * Returns point's hashcode: result = x; result = 31 * result + y; result = 31 * result + z.
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
	 * 克隆本点（浅拷贝三个 float 字段，等价于复制构造器）。
	 * Clones this point (shallow copy of the three float fields, equivalent to the copy constructor).
	 * @return 本点的副本 / copy of this point
	 */
	@Override
	public Point3D clone() {
		try {
			return (Point3D) super.clone();
		} catch (CloneNotSupportedException e) {
			// 不可达：Point3D 实现了 Cloneable / Unreachable: Point3D implements Cloneable
		 throw new AssertionError(e);
		}
	}

	/**
	 * 本点的格式化字符串表示。
	 * Formatted string representation of this point.
	 */
	@Override
	public String toString() {
		return "Point3D" + "{x=" + x + ", y=" + y + ", z=" + z + '}';
	}
}
