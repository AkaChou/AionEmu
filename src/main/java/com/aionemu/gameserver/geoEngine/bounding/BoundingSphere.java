package com.aionemu.gameserver.geoEngine.bounding;


import com.aionemu.boot.i18n.I18n;
import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.UnsupportedCollisionException;
import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Plane;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Triangle;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.utils.BufferUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 包围球，以中心与半径包容一组顶点。
 * Bounding sphere that contains a set of vertices with a center and radius.
 * <br>
 * 典型用法：通过 {@code containAABB} 或 {@code averagePoints} 确定中心与半径；
 * 调用 {@code computeFramePoint} 时会转而调用 {@code containAABB}。
 * Typical usage: define center and radius via {@code containAABB} or {@code averagePoints}.
 * A call to {@code computeFramePoint} in turn calls {@code containAABB}.
 *
 * @author Mark Powell
 * @version $Id: BoundingSphere.java,v 1.59 2007/08/17 10:34:26 rherlitz Exp $
 */
@Slf4j
public class BoundingSphere extends BoundingVolume {

	/** 球半径。 / Sphere radius. */
	float radius;
	/** 半径浮点误差容差系数。 / Floating-point radius epsilon factor. */
	private static final float RADIUS_EPSILON = 1f + 0.00001f;

	/**
	 * 默认构造，创建一个空包围球。
	 * Default constructor instantiating a new BoundingSphere.
	 */
	public BoundingSphere() {
	}

	/**
	 * 以指定半径与中心构造包围球。
	 * Constructs a BoundingSphere with the given radius and center.
	 *
	 * @param r 球半径 / radius of the sphere
	 * @param c 球中心 / center of the sphere
	 */
	public BoundingSphere(float r, Vector3f c) {
		this.center.set(c);
		this.radius = r;
	}

	/**
	 * 返回包围体类型（球体）。
	 * Returns the bounding-volume type (Sphere).
	 *
	 * type enum Sphere
	 */
	@Override
	public Type getType() {
		return Type.Sphere;
	}

	/**
	 * 获取包围球半径。
	 * Returns the radius of the bounding sphere.
	 *
	 * radius of the bounding sphere
	 */
	public float getRadius() {
		return radius;
	}

	/**
	 * 设置包围球半径。
	 * Sets the radius of this bounding sphere.
	 *
	 * new radius of the bounding sphere
	 */
	public void setRadius(float radius) {
		this.radius = radius;
	}

	/**
	 * 根据点集计算包围球，默认使用 Welzl 最小包围球算法。
	 * Computes a bounding sphere from a set of points using the Welzl algorithm by default.
	 *
	 * @param points 待包容的点缓冲 / points to contain
	 */
	@Override
	public void computeFromPoints(FloatBuffer points) {
		calcWelzl(points);
	}

	/**
	 * 根据三角形集合计算包围球，用于 OBBTree 相关计算。
	 * Computes a bounding sphere from a set of triangles; used in OBBTree calculations.
	 *
	 * @param tris 三角形数组 / triangle array
	 * @param start 起始下标（含） / start index (inclusive)
	 * @param end 结束下标（不含） / end index (exclusive)
	 */
	public void computeFromTris(Triangle[] tris, int start, int end) {
		if (end - start <= 0) {
			return;
		}

		Vector3f[] vertList = new Vector3f[(end - start) * 3];

		int count = 0;
		for (int i = start; i < end; i++) {
			vertList[count++] = tris[i].get(0);
			vertList[count++] = tris[i].get(1);
			vertList[count++] = tris[i].get(2);
		}
		averagePoints(vertList);
	}

	//
	// /**
	// * 由三角形集合计算包围体（OBBTree 用）。
	// * <code>computeFromTris</code> creates a new Bounding Box from a given
	// * set of triangles. It is used in OBBTree calculations.
	// *
	// * @param indices
	// * @param mesh
	// * @param start
	// * @param end
	// */
	// public void computeFromTris(int[] indices, Mesh mesh, int start, int end) {
	// if (end - start <= 0) {
	// return;
	// }
	//
	// Vector3f[] vertList = new Vector3f[(end - start) * 3];
	//
	// int count = 0;
	// for (int i = start; i < end; i++) {
	// mesh.getTriangle(indices[i], verts);
	// vertList[count++] = new Vector3f(verts[0]);
	// vertList[count++] = new Vector3f(verts[1]);
	// vertList[count++] = new Vector3f(verts[2]);
	// }
	//
	// averagePoints(vertList);
	// }

	/**
	 * 用 Welzl 算法计算点集的最小包围球。
	 * Calculates a minimum bounding sphere for the set of points via Welzl's algorithm.
	 * 算法源自 flipcode 的 C++ 实现，由 Cep21 翻译为 Java。
	 * Originally found at flipcode (C++) and translated to Java by Cep21.
	 *
	 * @param points 待计算最小包围的点 / points to calculate the minimum bounds from
	 */
	public void calcWelzl(FloatBuffer points) {
		if (center == null) {
			center = new Vector3f();
		}
		FloatBuffer buf = BufferUtils.createFloatBuffer(points.limit());
		points.rewind();
		buf.put(points);
		buf.flip();
		recurseMini(buf, buf.limit() / 3, 0, 0);
	}

	/**
	 * Welzl 递归步骤：逐步纳入边界点，求最小包围球。
	 * Recursive Welzl step that builds a minimum sphere a few points at a time.
	 *
	 * array of points to look through
	 * @param p 使用的点列表大小 / size of the list to be used
	 * @param b 当前已纳入球面的点数 / number of points currently on the sphere boundary
	 * @param ap 点缓冲偏移（模拟 C++ 指针算术） / buffer offset simulating C++ pointer arithmetic
	 */
	private void recurseMini(FloatBuffer points, int p, int b, int ap) {
		Vector3f tempA = Vector3f.newInstance();
		Vector3f tempB = Vector3f.newInstance();
		Vector3f tempC = Vector3f.newInstance();
		Vector3f tempD = Vector3f.newInstance();

		try {
			switch (b) {
			case 0:
				this.radius = 0;
				this.center.set(0, 0, 0);
				break;
			case 1:
				this.radius = 1f - RADIUS_EPSILON;
				BufferUtils.populateFromBuffer(center, points, ap - 1);
				break;
			case 2:
				BufferUtils.populateFromBuffer(tempA, points, ap - 1);
				BufferUtils.populateFromBuffer(tempB, points, ap - 2);
				setSphere(tempA, tempB);
				break;
			case 3:
				BufferUtils.populateFromBuffer(tempA, points, ap - 1);
				BufferUtils.populateFromBuffer(tempB, points, ap - 2);
				BufferUtils.populateFromBuffer(tempC, points, ap - 3);
				setSphere(tempA, tempB, tempC);
				break;
			case 4:
				BufferUtils.populateFromBuffer(tempA, points, ap - 1);
				BufferUtils.populateFromBuffer(tempB, points, ap - 2);
				BufferUtils.populateFromBuffer(tempC, points, ap - 3);
				BufferUtils.populateFromBuffer(tempD, points, ap - 4);
				setSphere(tempA, tempB, tempC, tempD);
				return;
			}
			for (int i = 0; i < p; i++) {
				BufferUtils.populateFromBuffer(tempA, points, i + ap);
				if (tempA.distanceSquared(center) - (radius * radius) > RADIUS_EPSILON - 1f) {
					for (int j = i; j > 0; j--) {
						BufferUtils.populateFromBuffer(tempB, points, j + ap);
						BufferUtils.populateFromBuffer(tempC, points, j - 1 + ap);
						BufferUtils.setInBuffer(tempC, points, j + ap);
						BufferUtils.setInBuffer(tempB, points, j - 1 + ap);
					}
					recurseMini(points, i, b + 1, ap + 1);
				}
			}
		} finally {
			Vector3f.recycle(tempA);
			Vector3f.recycle(tempB);
			Vector3f.recycle(tempC);
			Vector3f.recycle(tempD);
		}
	}

	/**
	 * 由 4 点确定最小包围球（Welzl 算法）。
	 * Calculates the minimum bounding sphere of 4 points (Welzl).
	 *
	 * @param O 球内第 1 点 / 1st point inside the sphere
	 * @param A 球内第 2 点 / 2nd point inside the sphere
	 * @param B 球内第 3 点 / 3rd point inside the sphere
	 * @param C 球内第 4 点 / 4th point inside the sphere
	 * @see #calcWelzl(java.nio.FloatBuffer)
	 */
	private void setSphere(Vector3f O, Vector3f A, Vector3f B, Vector3f C) {
		Vector3f a = A.subtract(O);
		Vector3f b = B.subtract(O);
		Vector3f c = C.subtract(O);

		float Denominator = 2.0f
				* (a.x * (b.y * c.z - c.y * b.z) - b.x * (a.y * c.z - c.y * a.z) + c.x * (a.y * b.z - b.y * a.z));
		if (Denominator == 0) {
			center.set(0, 0, 0);
			radius = 0;
		} else {
			Vector3f o = a.cross(b).multLocal(c.lengthSquared()).addLocal(c.cross(a).multLocal(b.lengthSquared()))
					.addLocal(b.cross(c).multLocal(a.lengthSquared())).divideLocal(Denominator);

			radius = o.length() * RADIUS_EPSILON;
			O.add(o, center);
		}
	}

	/**
	 * 由 3 点确定最小包围球（Welzl 算法）。
	 * Calculates the minimum bounding sphere of 3 points (Welzl).
	 *
	 * @param O 球内第 1 点 / 1st point inside the sphere
	 * @param A 球内第 2 点 / 2nd point inside the sphere
	 * @param B 球内第 3 点 / 3rd point inside the sphere
	 * @see #calcWelzl(java.nio.FloatBuffer)
	 */
	private void setSphere(Vector3f O, Vector3f A, Vector3f B) {
		Vector3f a = A.subtract(O);
		Vector3f b = B.subtract(O);
		Vector3f acrossB = a.cross(b);

		float Denominator = 2.0f * acrossB.dot(acrossB);

		if (Denominator == 0) {
			center.set(0, 0, 0);
			radius = 0;
		} else {

			Vector3f o = acrossB.cross(a).multLocal(b.lengthSquared())
					.addLocal(b.cross(acrossB).multLocal(a.lengthSquared())).divideLocal(Denominator);
			radius = o.length() * RADIUS_EPSILON;
			O.add(o, center);
		}
	}

	/**
	 * 由 2 点确定最小包围球（Welzl 算法）。
	 * Calculates the minimum bounding sphere of 2 points (Welzl).
	 *
	 * @param O 球内第 1 点 / 1st point inside the sphere
	 * @param A 球内第 2 点 / 2nd point inside the sphere
	 * @see #calcWelzl(java.nio.FloatBuffer)
	 */
	private void setSphere(Vector3f O, Vector3f A) {
		radius = FastMath.sqrt(((A.x - O.x) * (A.x - O.x) + (A.y - O.y) * (A.y - O.y) + (A.z - O.z) * (A.z - O.z)) / 4f)
				+ RADIUS_EPSILON - 1f;
		center.interpolate(O, A, .5f);
	}

	/**
	 * 以点的平均位置为球心，取能包容全部点的最小半径。
	 * Sets the sphere center to the average of the points and the radius to the smallest value enclosing them.
	 *
	 * @param points 待包容的点列表 / list of points to contain
	 */
	public void averagePoints(Vector3f[] points) {
		log.info(I18n.get("log.182487772b0f"));
		center = points[0];

		for (int i = 1; i < points.length; i++) {
			center.addLocal(points[i]);
		}

		float quantity = 1.0f / points.length;
		center.multLocal(quantity);

		float maxRadiusSqr = 0;
		for (int i = 0; i < points.length; i++) {
			Vector3f diff = points[i].subtract(center);
			float radiusSqr = diff.lengthSquared();
			if (radiusSqr > maxRadiusSqr) {
				maxRadiusSqr = radiusSqr;
			}
		}

		radius = (float) Math.sqrt(maxRadiusSqr) + RADIUS_EPSILON - 1f;
	}

	/**
	 * 用给定矩阵变换本包围球，结果写入 store（或新建）。
	 * Transforms this sphere by the given matrix into store (or a new instance).
	 *
	 * @param trans 变换矩阵 / transform matrix
	 * @param store 结果存储（可为 null 或非球类型） / destination volume (may be null or non-sphere)
	 * @return 变换后的包围球 / transformed bounding sphere
	 */
	@Override
	public BoundingVolume transform(Matrix4f trans, BoundingVolume store) {
		BoundingSphere sphere;
		if (store == null || store.getType() != BoundingVolume.Type.Sphere) {
			sphere = new BoundingSphere(1, new Vector3f(0, 0, 0));
		} else {
			sphere = (BoundingSphere) store;
		}

		trans.mult(center, sphere.center);
		Vector3f axes = new Vector3f(1, 1, 1);
		trans.mult(axes, axes);
		float ax = getMaxAxis(axes);
		sphere.radius = FastMath.abs(ax * radius) + RADIUS_EPSILON - 1f;
		return sphere;
	}

	/**
	 * 返回缩放向量中绝对值最大的轴分量。
	 * Returns the largest absolute axis component of a scale vector.
	 *
	 * @param scale 缩放向量 / scale vector
	 * @return 最大轴分量 / max axis component
	 */
	private float getMaxAxis(Vector3f scale) {
		float x = FastMath.abs(scale.x);
		float y = FastMath.abs(scale.y);
		float z = FastMath.abs(scale.z);

		if (x >= y) {
			if (x >= z) {
				return x;
			}
			return z;
		}

		if (y >= z) {
			return y;
		}
		return z;
	}

	/**
	 * 判断本包围球相对给定平面所在侧（视锥裁剪常用）。
	 * Determines which side of a plane (typically from a view frustum) this bound lies on.
	 *
	 * @param plane 检测平面 / plane to check against
	 * @return 负侧 / 跨越 / 正侧，或 None（跨越）
	 *         Positive, Negative, or None (straddling)
	 */
	@Override
	public Plane.Side whichSide(Plane plane) {
		float distance = plane.pseudoDistance(center);

		if (distance <= -radius) {
			return Plane.Side.Negative;
		} else if (distance >= radius) {
			return Plane.Side.Positive;
		} else {
			return Plane.Side.None;
		}
	}

	/**
	 * 将本球与另一包围体合并，返回包容两者的新球。
	 * Merges this sphere with another volume and returns a new sphere containing both.
	 *
	 * @param volume 待合并的包围体 / volume to combine with this sphere
	 * @return 合并后的新球；不支持的类型返回 null / new sphere, or null if unsupported
	 */
	@Override
	public BoundingVolume merge(BoundingVolume volume) {
		if (volume == null) {
			return this;
		}

		switch (volume.getType()) {

		case Sphere: {
			BoundingSphere sphere = (BoundingSphere) volume;
			float temp_radius = sphere.getRadius();
			Vector3f temp_center = sphere.center;
			BoundingSphere rVal = new BoundingSphere();
			return merge(temp_radius, temp_center, rVal);
		}

		case AABB: {
			BoundingBox box = (BoundingBox) volume;
			Vector3f radVect = new Vector3f(box.xExtent, box.yExtent, box.zExtent);
			Vector3f temp_center = box.center;
			BoundingSphere rVal = new BoundingSphere();
			return merge(radVect.length(), temp_center, rVal);
		}

		// case OBB: {
		// OrientedBoundingBox box = (OrientedBoundingBox) volume;
		// BoundingSphere rVal = (BoundingSphere) this.clone(null);
		// return rVal.mergeOBB(box);
		// }
		default:
			return null;
		}
	}

	/**
	 * 就地将本球与另一包围体合并，修改自身以包容两者。
	 * Merges this sphere with another volume in place, altering this sphere to contain both.
	 *
	 * @param volume 待合并的包围体 / volume to combine with this sphere
	 * @return 本实例；不支持的类型返回 null / this, or null if unsupported
	 */
	@Override
	public BoundingVolume mergeLocal(BoundingVolume volume) {
		if (volume == null) {
			return this;
		}

		switch (volume.getType()) {

		case Sphere: {
			BoundingSphere sphere = (BoundingSphere) volume;
			float temp_radius = sphere.getRadius();
			Vector3f temp_center = sphere.center;
			return merge(temp_radius, temp_center, this);
		}

		case AABB: {
			BoundingBox box = (BoundingBox) volume;
			Vector3f radVect = Vector3f.newInstance();
			radVect.set(box.xExtent, box.yExtent, box.zExtent);
			Vector3f temp_center = box.center;
			float len = radVect.length();
			Vector3f.recycle(radVect);
			return merge(len, temp_center, this);
		}

		// case OBB: {
		// return mergeOBB((OrientedBoundingBox) volume);
		// }
		default:
			return null;
		}
	}

	// /**
	// * 将此球与给定 OBB 合并。
	// * Merges this sphere with the given OBB.
	// *
	// * @param volume The OBB to merge.
	// * @return This sphere, after merging.
	// */
	// private BoundingSphere mergeOBB(OrientedBoundingBox volume) {
	// // 从 OBB 计算边点。 / compute edge points from the obb
	// if (!volume.correctCorners)
	// volume.computeCorners();
	// _mergeBuf.rewind();
	// for (int i = 0; i < 8; i++) {
	// _mergeBuf.put(volume.vectorStore[i].x);
	// _mergeBuf.put(volume.vectorStore[i].y);
	// _mergeBuf.put(volume.vectorStore[i].z);
	// }
	//
	// // 记住旧半径与中心。 / remember old radius and center
	// float oldRadius = radius;
	// Vector3f oldCenter = _compVect2.set( center );
	//
	// // 由 OBB 点计算新半径与中心。 / compute new radius and center from obb points
	// computeFromPoints(_mergeBuf);
	// Vector3f newCenter = _compVect3.set( center );
	// float newRadius = radius;
	//
	// // 恢复旧中心与半径。 / restore old center and radius
	// center.set( oldCenter );
	// radius = oldRadius;
	//
	// // 合并 OBB 点结果 / merge obb points result
	// merge( newRadius, newCenter, this );
	//
	// return this;
	// }

	/**
	 * 将本球与给定半径/中心的球合并，结果写入 rVal。
	 * Merges this sphere with a sphere of the given radius and center into rVal.
	 *
	 * @param temp_radius 另一球半径 / other sphere radius
	 * @param temp_center 另一球中心 / other sphere center
	 * @param rVal 结果存储 / destination sphere
	 * merge result
	 */
	private BoundingVolume merge(float temp_radius, Vector3f temp_center, BoundingSphere rVal) {
		Vector3f vect1 = Vector3f.newInstance();
		Vector3f diff = temp_center.subtract(center, vect1);
		float lengthSquared = diff.lengthSquared();
		float radiusDiff = temp_radius - radius;

		float fRDiffSqr = radiusDiff * radiusDiff;

		if (fRDiffSqr >= lengthSquared) {
			if (radiusDiff <= 0.0f) {
				Vector3f.recycle(vect1);
				return this;
			}

			Vector3f rCenter = rVal.center;
			if (rCenter == null) {
				rVal.setCenter(rCenter = new Vector3f());
			}
			rCenter.set(temp_center);
			rVal.setRadius(temp_radius);
			Vector3f.recycle(vect1);
			return rVal;
		}

		float length = (float) Math.sqrt(lengthSquared);

		Vector3f rCenter = rVal.center;
		if (rCenter == null) {
			rVal.setCenter(rCenter = new Vector3f());
		}
		if (length > RADIUS_EPSILON) {
			float coeff = (length + radiusDiff) / (2.0f * length);
			rCenter.set(center.addLocal(diff.multLocal(coeff)));
		} else {
			rCenter.set(center);
		}

		rVal.setRadius(0.5f * (length + radius + temp_radius));
		Vector3f.recycle(vect1);
		return rVal;
	}

	/**
	 * 克隆本包围球（数据复制到 store，或新建）。
	 * Clones this BoundingSphere into store, or creates a new instance.
	 *
	 * @param store 结果存储；为 null 或类型不匹配时新建 / destination (new if null or wrong type)
	 * @return 克隆后的包围球 / cloned BoundingSphere
	 */
	@Override
	public BoundingVolume clone(BoundingVolume store) {
		if (store != null && store.getType() == Type.Sphere) {
			BoundingSphere rVal = (BoundingSphere) store;
			if (null == rVal.center) {
				rVal.center = new Vector3f();
			}
			rVal.center.set(center);
			rVal.radius = radius;
			rVal.checkPlane = checkPlane;
			return rVal;
		}
		return new BoundingSphere(radius, (center != null ? (Vector3f) center.clone() : null));
	}

	/**
	 * 返回字符串表示，形式为 "BoundingSphere [Radius: R Center: &lt;Vector&gt;]"。
	 * Returns the string representation: "BoundingSphere [Radius: R Center: &lt;Vector&gt;]".
	 *
	 * @return 字符串表示 / string representation
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [Radius: " + radius + " Center: " + center + "]";
	}

	/**
	 * 与另一包围体相交检测（双分派至对方的 intersectsSphere）。
	 * Intersection test against another volume (double-dispatch via intersectsSphere).
	 *
	 * @param bv 另一包围体 / other bounding volume
	 * whether they intersect
	 */
	@Override
	public boolean intersects(BoundingVolume bv) {
		return bv.intersectsSphere(this);
	}

	/**
	 * 与另一包围球相交检测。
	 * Intersection test against another bounding sphere.
	 *
	 * @param bs 另一包围球 / other bounding sphere
	 * whether they intersect
	 */
	@Override
	public boolean intersectsSphere(BoundingSphere bs) {
		assert Vector3f.isValidVector(center) && Vector3f.isValidVector(bs.center);

		Vector3f vect1 = Vector3f.newInstance();
		Vector3f diff = center.subtract(bs.center, vect1);
		float rsum = getRadius() + bs.getRadius();
		boolean eq = (diff.dot(diff) <= rsum * rsum);
		Vector3f.recycle(vect1);
		return eq;
	}

	/**
	 * 与 AABB 包围盒相交检测。
	 * Intersection test against an axis-aligned bounding box.
	 *
	 * axis-aligned bounding box
	 * whether they intersect
	 */
	@Override
	public boolean intersectsBoundingBox(BoundingBox bb) {
		assert Vector3f.isValidVector(center) && Vector3f.isValidVector(bb.center);

		if (FastMath.abs(bb.center.x - center.x) < getRadius() + bb.xExtent
				&& FastMath.abs(bb.center.y - center.y) < getRadius() + bb.yExtent
				&& FastMath.abs(bb.center.z - center.z) < getRadius() + bb.zExtent) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jme.bounding.BoundingVolume#intersectsOrientedBoundingBox(com.jme.
	 * bounding.OrientedBoundingBox)
	 */
	// public boolean intersectsOrientedBoundingBox(OrientedBoundingBox obb) {
	// return obb.intersectsSphere(this);
	// }

	/**
	 * 与射线是否相交（仅布尔结果）。
	 * Tests whether this sphere intersects a ray (boolean only).
	 *
	 * ray
	 * whether they intersect
	 */
	@Override
	public boolean intersects(Ray ray) {
		assert Vector3f.isValidVector(center);

		Vector3f vect1 = Vector3f.newInstance();
		Vector3f diff = vect1.set(ray.getOrigin()).subtractLocal(center);
		float radiusSquared = getRadius() * getRadius();
		float a = diff.dot(diff) - radiusSquared;
		if (a <= 0.0) {
			// 在球体内 / in sphere
			Vector3f.recycle(vect1);
			return true;
		}

		// 球体外部 / outside sphere
		float b = ray.getDirection().dot(diff);
		if (b >= 0.0) {
			Vector3f.recycle(vect1);
			return false;
		}
		Vector3f.recycle(vect1);
		return b * b >= a;
	}

	/**
	 * 射线与球碰撞，将交点写入 results，返回碰撞点数量。
	 * Collides a ray with this sphere, adding hits to results; returns hit count.
	 *
	 * ray
	 *
	 * @param results 碰撞结果收集器 / collision results collector
	 * @param results 碰撞结果收集器 / collision results collector
	 * @return 碰撞点数量（0/1/2） / number of collision points (0/1/2)
	 */
	public int collideWithRay(Ray ray, CollisionResults results) {
		Vector3f vect1 = Vector3f.newInstance();
		Vector3f diff = vect1.set(ray.getOrigin()).subtractLocal(center);
		float a = diff.dot(diff) - (getRadius() * getRadius());
		float a1, discr, root;
		if (a <= 0.0) {
			// 球体内部 / inside sphere
			a1 = ray.direction.dot(diff);
			discr = (a1 * a1) - a;
			root = FastMath.sqrt(discr);

			float distance = root - a1;
			Vector3f point = new Vector3f(ray.direction).multLocal(distance).addLocal(ray.origin);

			CollisionResult result = new CollisionResult(point, distance);
			results.addCollision(result);
			Vector3f.recycle(vect1);
			return 1;
		}

		a1 = ray.direction.dot(diff);
		if (a1 >= 0.0) {
			Vector3f.recycle(vect1);
			return 0;
		}

		discr = a1 * a1 - a;
		if (discr < 0.0) {
			Vector3f.recycle(vect1);
			return 0;
		} else if (discr >= FastMath.ZERO_TOLERANCE) {
			root = FastMath.sqrt(discr);
			float dist = -a1 - root;
			Vector3f point = new Vector3f(ray.direction).multLocal(dist).addLocal(ray.origin);
			results.addCollision(new CollisionResult(point, dist));

			dist = -a1 + root;
			point = new Vector3f(ray.direction).multLocal(dist).addLocal(ray.origin);
			results.addCollision(new CollisionResult(point, dist));
			Vector3f.recycle(vect1);
			return 2;
		} else {
			float dist = -a1;
			Vector3f point = new Vector3f(ray.direction).multLocal(dist).addLocal(ray.origin);
			results.addCollision(new CollisionResult(point, dist));
			Vector3f.recycle(vect1);
			return 1;
		}
	}

	/**
	 * 与可碰撞对象进行碰撞检测（当前仅支持 Ray）。
	 * Collides with a Collidable (currently only Ray is supported).
	 *
	 * @param other 可碰撞对象 / collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * @return 碰撞点数量 / number of collision points
	 * unsupported collision type。 / unsupported collision type.
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if (other instanceof Ray) {
			Ray ray = (Ray) other;
			return collideWithRay(ray, results);
		} else {
			throw new UnsupportedCollisionException();
		}
	}

	/**
	 * 判断点是否严格位于球内（不含球面）。
	 * Tests whether a point lies strictly inside the sphere (surface excluded).
	 *
	 * point to test
	 *
	 * @param point 检测点 / point to test
	 * @return 是否在球内 / whether the point is inside
	 */
	@Override
	public boolean contains(Vector3f point) {
		return center.distanceSquared(point) < (getRadius() * getRadius());
	}

	/**
	 * 判断点是否与球相交（含球面）。
	 * Tests whether a point intersects the sphere (surface included).
	 *
	 * point to test
	 * whether the point intersects
	 */
	@Override
	public boolean intersects(Vector3f point) {
		return center.distanceSquared(point) <= (getRadius() * getRadius());
	}

	/**
	 * 点到球表面的有符号距离（负值表示在球内）。
	 * Signed distance from a point to the sphere surface (negative if inside).
	 *
	 * point to test
	 *
	 * @param point 检测点 / point to test
	 * @return 有符号距离 / signed distance to the edge
	 */
	@Override
	public float distanceToEdge(Vector3f point) {
		return center.distance(point) - radius;
	}

	/**
	 * 返回球体积 (4/3)πr³。
	 * Returns the sphere volume (4/3)πr³.
	 *
	 * volume
	 */
	@Override
	public float getVolume() {
		return 4 * FastMath.ONE_THIRD * FastMath.PI * radius * radius * radius;
	}
}
