package com.aionemu.gameserver.geoEngine.collision;

import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Spatial;

/**
 * 单次碰撞命中结果，包含接触点、法线、距离与命中几何体；可按距离排序。
 * A single collision hit carrying contact point, normal, distance and hit geometry;
 * comparable by distance.
 *
 * @author Kirill
 */
public class CollisionResult implements Comparable<CollisionResult> {

	/** 命中的空间几何体。 / Hit spatial geometry. */
	private Spatial geometry;
	/** 接触点（世界坐标）。 / Contact point in world space. */
	private Vector3f contactPoint;
	/** 接触法线。 / Contact normal. */
	private Vector3f contactNormal;
	/** 从射线原点到接触点的距离。 / Distance from ray origin to contact point. */
	private float distance;

	/**
	 * 以接触点与距离构造结果。
	 * Constructs a result with contact point and distance.
	 *
	 * contact point
	 * distance
	 */
	public CollisionResult(Vector3f contactPoint, float distance) {
		this.contactPoint = contactPoint;
		this.distance = distance;
	}

	/**
	 * 空构造，供后续 setter 填充。
	 * Empty constructor for later setter population.
	 */
	public CollisionResult() {
	}

	/**
	 * 设置接触点。
	 * Sets the contact point.
	 *
	 * contact point
	 */
	public void setContactPoint(Vector3f point) {
		this.contactPoint = point;
	}

	/**
	 * 设置距离。
	 * Sets the distance.
	 *
	 * distance
	 */
	public void setDistance(float dist) {
		this.distance = dist;
	}

	/**
	 * 按距离比较，近者优先。
	 * Compares by distance; nearer hits come first.
	 *
	 * @param other 另一结果 / other result
	 * @return 负 / 零/正 表示 近/等/远 / negative/zero/positive for nearer/equal/farther
	 */
	@Override
	public int compareTo(CollisionResult other) {
		if (distance < other.distance) {
			return -1;
		} else if (distance > other.distance) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * 设置接触法线。
	 * Sets the contact normal.
	 *
	 * normal
	 */
	public void setContactNormal(Vector3f norm) {
		this.contactNormal = norm;
	}

	/**
	 * 设置命中几何体。
	 * Sets the hit geometry.
	 *
	 * @param geom 空间对象 / spatial
	 */
	public void setGeometry(Spatial geom) {
		this.geometry = geom;
	}

	/**
	 * 返回接触法线。
	 * Returns the contact normal.
	 *
	 * normal
	 */
	public Vector3f getContactNormal() {
		return contactNormal;
	}

	/**
	 * 返回接触点。
	 * Returns the contact point.
	 *
	 * contact point
	 */
	public Vector3f getContactPoint() {
		return contactPoint;
	}

	/**
	 * 返回命中几何体。
	 * Returns the hit geometry.
	 *
	 * spatial
	 */
	public Spatial getGeometry() {
		return geometry;
	}

	/**
	 * 返回距离。
	 * Returns the distance.
	 *
	 * distance
	 */
	public float getDistance() {
		return distance;
	}
}
