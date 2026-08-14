package com.aionemu.gameserver.geoEngine.collision.bih;

import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * BIH 树中使用的三角形，缓存三顶点与质心，并提供法线与轴向极值查询。
 * Triangle used by the BIH tree, caching three vertices and centroid with
 * normal and per-axis extreme queries.
 */
public final class BIHTriangle {

	/** 顶点 A / Vertex A */
	private final Vector3f pointa = new Vector3f();
	/** 顶点 B / Vertex B */
	private final Vector3f pointb = new Vector3f();
	/** 顶点 C / Vertex C */
	private final Vector3f pointc = new Vector3f();
	/** 质心。 / Centroid. */
	private final Vector3f center = new Vector3f();

	/**
	 * 以三顶点构造三角形并计算质心。
	 * Constructs a triangle from three vertices and computes the centroid.
	 *
	 * @param p1 顶点 1 / vertex 1
	 * @param p2 顶点 2 / vertex 2
	 * @param p3 顶点 3 / vertex 3
	 */
	public BIHTriangle(Vector3f p1, Vector3f p2, Vector3f p3) {
		pointa.set(p1);
		pointb.set(p2);
		pointc.set(p3);
		center.set(pointa);
		center.addLocal(pointb).addLocal(pointc).multLocal(FastMath.ONE_THIRD);
	}

	/**
	 * 返回顶点 A。
	 * Returns vertex A.
	 *
	 * @return 顶点 A / vertex A
	 */
	public Vector3f get1() {
		return pointa;
	}

	/**
	 * 返回顶点 B。
	 * Returns vertex B.
	 *
	 * @return 顶点 B / vertex B
	 */
	public Vector3f get2() {
		return pointb;
	}

	/**
	 * 返回顶点 C。
	 * Returns vertex C.
	 *
	 * @return 顶点 C / vertex C
	 */
	public Vector3f get3() {
		return pointc;
	}

	/**
	 * 返回质心。
	 * Returns the centroid.
	 *
	 * @return 质心 / centroid
	 */
	public Vector3f getCenter() {
		return center;
	}

	/**
	 * 计算并返回单位法线（每次新建向量）。
	 * Computes and returns the unit normal (allocates a new vector each call).
	 *
	 * @return 单位法线 / unit normal
	 */
	public Vector3f getNormal() {
		Vector3f normal = new Vector3f(pointb);
		normal.subtractLocal(pointa).crossLocal(pointc.x - pointa.x, pointc.y - pointa.y, pointc.z - pointa.z);
		normal.normalizeLocal();
		return normal;
	}

	/**
	 * 返回指定轴上的最小（left）或最大（right）顶点分量。
	 * Returns the minimum ({@code left}) or maximum (right) vertex component on the given axis.
	 *
	 * @param axis 轴索引 0/1/2 对应 x/y/z / axis index 0/1/2 for x/y/z
	 * @param left {@code true} 取最小，否则取最大 / {@code true} for min, else max
	 * @return 极值分量 / extreme component
	 */
	public float getExtreme(int axis, boolean left) {
		float v1, v2, v3;
		switch (axis) {
		case 0:
			v1 = pointa.x;
			v2 = pointb.x;
			v3 = pointc.x;
			break;
		case 1:
			v1 = pointa.y;
			v2 = pointb.y;
			v3 = pointc.y;
			break;
		case 2:
			v1 = pointa.z;
			v2 = pointb.z;
			v3 = pointc.z;
			break;
		default:
			assert false;
			return 0;
		}
		if (left) {
			if (v1 < v2) {
				if (v1 < v3) {
					return v1;
				} else {
					return v3;
				}
			} else {
				if (v2 < v3) {
					return v2;
				} else {
					return v3;
				}
			}
		} else {
			if (v1 > v2) {
				if (v1 > v3) {
					return v1;
				} else {
					return v3;
				}
			} else {
				if (v2 > v3) {
					return v2;
				} else {
					return v3;
				}
			}
		}
	}
}
