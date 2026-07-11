package com.aionemu.gameserver.model.utils3d;

/**
 * 点3D，用于工具3d 相关逻辑。
 * Point 3 D for utils 3 d logic.
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

	/** 距离 / distance. */
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

	/** 返回 x / Returns the x */
	public double getX() {
		return x;
	}

	/** 返回 y / Returns the y */
	public double getY() {
		return y;
	}

	/** 返回 z / Returns the z */
	public double getZ() {
		return z;
	}
}
