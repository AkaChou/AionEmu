package com.aionemu.gameserver.geoEngine.collision.bih;

import java.util.Comparator;

import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 按指定轴上三角形质心分量比较的比较器，用于 BIH 建树时的三角形排序。
 * Comparator of {@link BIHTriangle}s by centroid component on a chosen axis,
 * used when sorting triangles during BIH construction.
 */
public class TriangleAxisComparator implements Comparator<BIHTriangle> {

	/** 比较轴 0/1/2 对应 x/y/z。 / Comparison axis 0/1/2 for x/y/z. */
	private final int axis;

	/**
	 * 以比较轴构造。
	 * Constructs with the comparison axis.
	 *
	 * @param axis 轴索引 / axis index
	 */
	public TriangleAxisComparator(int axis) {
		this.axis = axis;
	}

	/**
	 * 按质心在指定轴上的分量比较两三角形。
	 * Compares two triangles by their centroid component on the configured axis.
	 *
	 * @param o1 三角形 1 / triangle 1
	 * @param o2 三角形 2 / triangle 2
	 * @return negative / zero/positive
	 */
	@Override
	public int compare(BIHTriangle o1, BIHTriangle o2) {
		float v1, v2;
		Vector3f c1 = o1.getCenter();
		Vector3f c2 = o2.getCenter();
		switch (axis) {
		case 0:
			v1 = c1.x;
			v2 = c2.x;
			break;
		case 1:
			v1 = c1.y;
			v2 = c2.y;
			break;
		case 2:
			v1 = c1.z;
			v2 = c2.z;
			break;
		default:
			assert false;
			return 0;
		}
		if (v1 > v2) {
			return 1;
		} else if (v1 < v2) {
			return -1;
		} else {
			return 0;
		}
	}
}
