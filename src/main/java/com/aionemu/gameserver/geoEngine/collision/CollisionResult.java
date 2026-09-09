package com.aionemu.gameserver.geoEngine.collision;

import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 单次碰撞命中结果，包含接触点、法线、距离与命中几何体；可按距离排序。
 * A single collision hit carrying contact point, normal, distance and hit geometry;
 * comparable by distance.
 *
 * @author Kirill
 */
@NoArgsConstructor
public class CollisionResult implements Comparable<CollisionResult> {

	/** 命中的空间几何体。 / Hit spatial geometry. */
	@Getter
	@Setter
	private Spatial geometry;
	/** 接触点（世界坐标）。 / Contact point in world space. */
	@Getter
	@Setter
	private Vector3f contactPoint;
	/** 接触法线。 / Contact normal. */
	@Getter
	@Setter
	private Vector3f contactNormal;
	/** 从射线原点到接触点的距离。 / Distance from ray origin to contact point. */
	@Getter
	@Setter
	private float distance;

	/**
	 * 以接触点与距离构造结果。
	 * Constructs a result with contact point and distance.
	 *
	 * @param contactPoint 接触点 / contact point
	 * @param distance 距离 / distance
	 */
	public CollisionResult(Vector3f contactPoint, float distance) {
		this.contactPoint = contactPoint;
		this.distance = distance;
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
}
