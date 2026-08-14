package com.aionemu.gameserver.model.utils3d;

/**
 * 三维点，用于 3D 工具逻辑。
 * Point 3D for utils 3D logic.
 *
 * @author M@xx modified by Wakizashi
 */
public class Point3D {

	public double x;
	public double y;
	public double z;

	public Point3D() {
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
	}

	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Point3D(float x, float y, float z) {
		this.x = (double) x;
		this.y = (double) y;
		this.z = (double) z;
	}

	/** 计算与另一点的距离 / distance. */
	public double distance(Point3D p) {
		double dx = x - p.x;
		double dy = y - p.y;
		double dz = z - p.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "x=" + x + ", y=" + y + ", z=" + z;
	}

	/** 返回 X 坐标 / Returns the x */
	public double getX() {
		return x;
	}

	/** 返回 Y 坐标 / Returns the y */
	public double getY() {
		return y;
	}

	/** 返回 Z 坐标 / Returns the z */
	public double getZ() {
		return z;
	}
}
