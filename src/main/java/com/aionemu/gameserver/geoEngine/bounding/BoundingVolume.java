package com.aionemu.gameserver.geoEngine.bounding;

import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Plane;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 包围体抽象基类，定义点集的包容与相交检测接口。
 * Abstract base for bounding volumes dealing with containment of a collection of points.
 *
 * @author Mark Powell
 * @version $Id: BoundingVolume.java,v 1.24 2007/09/21 15:45:32 nca Exp $
 */
public abstract class BoundingVolume implements Collidable {

	/**
	 * 包围体类型枚举。
	 * Bounding-volume type enum.
	 */
	public enum Type {

		Sphere, AABB, OBB, Capsule;
	}

	/** 优先检测的裁剪平面索引。 / Index of the clip plane to check first. */
	protected int checkPlane = 0;
	/** 包围体中心点。 / Center of the bounding volume. */
	Vector3f center = new Vector3f();

	/**
	 * 默认构造，中心位于原点。
	 * Default constructor with center at the origin.
	 */
	public BoundingVolume() {
	}

	/**
	 * 以给定中心构造包围体。
	 * Constructs a bounding volume with the given center.
	 *
	 * center point
	 */
	public BoundingVolume(Vector3f center) {
		this.center.set(center);
	}

	/**
	 * 获取应优先检测的裁剪平面索引。
	 * Returns the clip-plane index that should be checked first.
	 *
	 * plane index
	 */
	public int getCheckPlane() {
		return checkPlane;
	}

	/**
	 * 设置渲染时优先检测的平面索引。
	 * Sets the index of the plane that should be first checked during rendering.
	 *
	 * @param value 平面索引 / plane index
	 */
	public final void setCheckPlane(int value) {
		checkPlane = value;
	}

	/**
	 * 返回此包围体的类型。
	 * Returns the type of this bounding volume.
	 *
	 * type enum
	 */
	public abstract Type getType();

	/**
	 * 通过旋转、平移与缩放变换包围体。
	 * Transforms the bounding volume by rotation, translation and scale.
	 *
	 * @param trans 变换矩阵 / transform matrix
	 * @param store 结果存储（可为 null） / destination volume (may be null)
	 * @return 变换后的包围体 / transformed bounding volume
	 */
	public abstract BoundingVolume transform(Matrix4f trans, BoundingVolume store);

	/**
	 * 判断包围体相对平面所在侧（正侧、负侧或跨越）。
	 * Returns which side of a plane the volume lies on (positive, negative, or none/straddling).
	 *
	 * @param plane 检测平面 / plane to test against
	 * side relative to the plane
	 */
	public abstract Plane.Side whichSide(Plane plane);

	/**
	 * 根据点集计算包围体。
	 * Computes a bounding volume that encompasses a collection of points.
	 *
	 * point buffer
	 */
	public abstract void computeFromPoints(FloatBuffer points);

	/**
	 * 合并两个包围体，返回包含两者的新包围体。
	 * Merges two volumes into a new volume containing both.
	 *
	 * @param volume 另一包围体 / the volume to combine
	 * @return 合并后的包围体 / merged bounding volume
	 */
	public abstract BoundingVolume merge(BoundingVolume volume);

	/**
	 * 就地合并两个包围体，结果存于自身。
	 * Merges two volumes in place; the result is stored in this volume.
	 *
	 * @param volume 另一包围体 / the volume to combine
	 * @return this
	 */
	public abstract BoundingVolume mergeLocal(BoundingVolume volume);

	/**
	 * 克隆包围体数据到指定存储（类型不符或为 null 时新建）。
	 * Clones this volume into the given store (creates a new one if null or wrong class).
	 *
	 * @param store 存储目标 / destination store
	 * cloned bounding volume
	 */
	public abstract BoundingVolume clone(BoundingVolume store);

	/**
	 * 获取中心点引用。
	 * Returns the center vector reference.
	 *
	 * center
	 */
	public final Vector3f getCenter() {
		return center;
	}

	/**
	 * 将中心点写入给定向量并返回。
	 * Copies the center into the given store and returns it.
	 *
	 * @param store 目标向量 / destination vector
	 * @return store
	 */
	public final Vector3f getCenter(Vector3f store) {
		store.set(center);
		return store;
	}

	/**
	 * 设置中心点引用。
	 * Sets the center vector reference.
	 *
	 * new center
	 */
	public final void setCenter(Vector3f newCenter) {
		center = newCenter;
	}

	/**
	 * 计算中心到给定点的距离。
	 * Distance from the volume center to the given point.
	 *
	 * target point
	 * distance
	 */
	public final float distanceTo(Vector3f point) {
		return center.distance(point);
	}

	/**
	 * 计算中心到给定点的距离平方。
	 * Squared distance from the volume center to the given point.
	 *
	 * target point
	 * squared distance
	 */
	public final float distanceSquaredTo(Vector3f point) {
		return center.distanceSquared(point);
	}

	/**
	 * 计算最近边到给定点的距离。
	 * Distance from the nearest edge of this volume to the given point.
	 *
	 * target point
	 * @return 到边的距离 / distance to edge
	 */
	public abstract float distanceToEdge(Vector3f point);

	/**
	 * 判断两包围体是否相交（包含、重叠或接触）。
	 * Whether this volume and the other intersect (contain, overlap, or touch).
	 *
	 * @param bv 另一包围体 / other volume
	 * 若 intersecting 则为 true / true if intersecting
	 */
	public abstract boolean intersects(BoundingVolume bv);

	/**
	 * 判断射线是否与本包围体相交。
	 * Whether a ray intersects this bounding volume.
	 *
	 * ray to test
	 * 若 intersecting 则为 true / true if intersecting
	 */
	public abstract boolean intersects(Ray ray);

	/**
	 * 判断与给定包围球是否相交。
	 * Whether this volume intersects the given sphere.
	 *
	 * @param bs 包围球 / bounding sphere
	 * 若 intersecting 则为 true / true if intersecting
	 */
	public abstract boolean intersectsSphere(BoundingSphere bs);

	/**
	 * 判断与给定包围盒是否相交。
	 * Whether this volume intersects the given axis-aligned box.
	 *
	 * @param bb 包围盒 / bounding box
	 * 若 intersecting 则为 true / true if intersecting
	 */
	public abstract boolean intersectsBoundingBox(BoundingBox bb);

	/**
	 * 判断此包围体是否与给定包围盒相交。 / determines if this bounding volume and a given bounding box are intersecting.
	 */
	// public abstract boolean intersectsOrientedBoundingBox(OrientedBoundingBox
	// bb);

	/**
	 * 判断点是否严格包含于包围体内。
	 * Whether the given point is strictly contained inside this volume.
	 *
	 * point to check
	 * 若 contained 则为 true / true if contained
	 */
	public abstract boolean contains(Vector3f point);

	/**
	 * 判断点是否与包围体相交（接触或在内部）。
	 * Whether the given point intersects (touches or is inside) this volume.
	 *
	 * point to check
	 * 若 intersecting 则为 true / true if intersecting
	 */
	public abstract boolean intersects(Vector3f point);

	/**
	 * 返回包围体体积。
	 * Returns the volume of this bounding volume.
	 *
	 * volume
	 */
	public abstract float getVolume();

	/**
	 * 浅克隆，中心向量深拷贝。
	 * Clones this volume with a deep-copied center vector.
	 *
	 * clone
	 */
	@Override
	public BoundingVolume clone() {
		try {
			BoundingVolume clone = (BoundingVolume) super.clone();
			clone.center = center.clone();
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}
}
