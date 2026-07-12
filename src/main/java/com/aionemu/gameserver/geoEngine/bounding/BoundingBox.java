package com.aionemu.gameserver.geoEngine.bounding;

import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.UnsupportedCollisionException;
import com.aionemu.gameserver.geoEngine.math.Array3f;
import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Plane;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Triangle;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.utils.BufferUtils;

//import com.jme.scene.TriMesh;

/**
 * 轴对齐包围盒（AABB），以中心点及沿 x/y/z 轴的半长（extent）描述几何顶点集合的包容体。
 * Axis-aligned bounding box defined by a center and extents along the x, y and z axes.
 * <br>
 * 典型用法是通过 {@link #containAABB} 或平均点集确定中心与半径；
 * {@code computeFramePoint} 内部会调用 {@link #containAABB}。
 * Typical usage determines center/extents via {@link #containAABB} or average points;
 * {@code computeFramePoint} in turn calls {@link #containAABB}.
 *
 * @author Joshua Slack
 * @version $Id: BoundingBox.java,v 1.50 2007/09/22 16:46:35 irrisor Exp $
 */
public class BoundingBox extends BoundingVolume {

	/** 沿 X 轴半范围 / Half-extent along the X axis */
	float xExtent, yExtent, zExtent;

	/**
	 * 默认构造，实例化空包围盒。
	 * Default constructor instantiating an empty bounding box.
	 */
	public BoundingBox() {
	}

	/**
	 * 以给定中心与三轴半长构造包围盒。
	 * Constructs a bounding box with the given center and extents.
	 *
	 * @param c 中心点 / center
	 * @param x X 半长 / X extent
	 * @param y Y 半长 / Y extent
	 * @param z Z 半长 / Z extent
	 */
	public BoundingBox(Vector3f c, float x, float y, float z) {
		this.center.set(c);
		this.xExtent = x;
		this.yExtent = y;
		this.zExtent = z;
	}

	/**
	 * 从另一包围盒拷贝构造。
	 * Copy-constructor from another bounding box.
	 *
	 * source box
	 */
	public BoundingBox(BoundingBox source) {
		this.center.set(source.center);
		this.xExtent = source.xExtent;
		this.yExtent = source.yExtent;
		this.zExtent = source.zExtent;
	}

	/**
	 * 以最小/最大角点构造包围盒。
	 * Constructs a bounding box from min/max corners.
	 *
	 * @param min 最小角点 / minimum corner
	 * @param max 最大角点 / maximum corner
	 */
	public BoundingBox(Vector3f min, Vector3f max) {
		setMinMax(min, max);
	}

	/**
	 * 返回包围体类型（AABB）。
	 * Returns the bounding-volume type (AABB).
	 *
	 * type enum
	 */
	@Override
	public Type getType() {
		return Type.AABB;
	}

	/**
	 * 由点集计算包围盒，默认委托 {@link #containAABB}。
	 * Computes the bounding box from a point set; defaults to {@link #containAABB}.
	 *
	 * @param points 待包容的点缓冲 / points to contain
	 */
	@Override
	public void computeFromPoints(FloatBuffer points) {
		containAABB(points);
	}

	/**
	 * 由三角形数组计算包围盒，用于 OBBTree 相关计算。
	 * Computes the bounding box from a triangle array (used in OBBTree calculations).
	 *
	 * @param tris 三角形数组 / triangle array
	 * @param start 起始下标（含） / start index (inclusive)
	 * @param end 结束下标（不含） / end index (exclusive)
	 */
	public void computeFromTris(Triangle[] tris, int start, int end) {
		if (end - start <= 0) {
			return;
		}
		Vector3f min = Vector3f.newInstance();
		Vector3f max = Vector3f.newInstance();
		min.set(new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
		max.set(new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY));

		Vector3f point;
		for (int i = start; i < end; i++) {
			point = tris[i].get(0);
			checkMinMax(min, max, point);
			point = tris[i].get(1);
			checkMinMax(min, max, point);
			point = tris[i].get(2);
			checkMinMax(min, max, point);
		}

		center.set(min.addLocal(max));
		center.multLocal(0.5f);

		xExtent = max.x - center.x;
		yExtent = max.y - center.y;
		zExtent = max.z - center.z;
		Vector3f.recycle(min);
		Vector3f.recycle(max);
	}

	/**
	 * 由网格索引范围计算包围盒。
	 * Computes the bounding box from mesh triangle indices over a range.
	 *
	 * @param indices 三角形索引数组 / triangle index array
	 * mesh
	 * @param start 起始下标（含） / start index (inclusive)
	 * @param end 结束下标（不含） / end index (exclusive)
	 */
	public void computeFromTris(int[] indices, Mesh mesh, int start, int end) {
		if (end - start <= 0) {
			return;
		}
		Vector3f vect1 = Vector3f.newInstance();
		Vector3f vect2 = Vector3f.newInstance();
		Triangle triangle = Triangle.newInstance();

		Vector3f min = vect1.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		Vector3f max = vect2.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
		Vector3f point;

		for (int i = start; i < end; i++) {
			mesh.getTriangle(indices[i], triangle);
			point = triangle.get(0);
			checkMinMax(min, max, point);
			point = triangle.get(1);
			checkMinMax(min, max, point);
			point = triangle.get(2);
			checkMinMax(min, max, point);
		}

		center.set(min.addLocal(max));
		center.multLocal(0.5f);

		xExtent = max.x - center.x;
		yExtent = max.y - center.y;
		zExtent = max.z - center.z;
		Vector3f.recycle(vect1);
		Vector3f.recycle(vect2);
		Triangle.recycle(triangle);
	}

	/**
	 * 用点更新 min/max 角点（分量级扩展）。
	 * Expands min/max corners component-wise with the given point.
	 *
	 * @param min 当前最小角点 / current minimum
	 * @param max 当前最大角点 / current maximum
	 * @param point 待比较点 / candidate point
	 */
	public static final void checkMinMax(Vector3f min, Vector3f max, Vector3f point) {
		if (point.x < min.x) {
			min.x = point.x;
		}
		if (point.x > max.x) {
			max.x = point.x;
		}
		if (point.y < min.y) {
			min.y = point.y;
		}
		if (point.y > max.y) {
			max.y = point.y;
		}
		if (point.z < min.z) {
			min.z = point.z;
		}
		if (point.z > max.z) {
			max.z = point.z;
		}
	}

	/**
	 * 由点缓冲构建最小体积轴对齐包围盒，中心取盒子中心。
	 * Builds a minimum-volume axis-aligned bounding box of the points, centered at the box center.
	 *
	 * @param points 点列表缓冲 / list of points
	 */
	public void containAABB(FloatBuffer points) {
		if (points == null) {
			return;
		}

		points.rewind();
		if (points.remaining() <= 2) // we need at least a 3 float vector
		{
			return;
		}

		Vector3f vect1 = Vector3f.newInstance();
		BufferUtils.populateFromBuffer(vect1, points, 0);
		float minX = vect1.x, minY = vect1.y, minZ = vect1.z;
		float maxX = vect1.x, maxY = vect1.y, maxZ = vect1.z;

		for (int i = 1, len = points.remaining() / 3; i < len; i++) {
			BufferUtils.populateFromBuffer(vect1, points, i);
			if (vect1.x < minX) {
				minX = vect1.x;
			} else if (vect1.x > maxX) {
				maxX = vect1.x;
			}

			if (vect1.y < minY) {
				minY = vect1.y;
			} else if (vect1.y > maxY) {
				maxY = vect1.y;
			}

			if (vect1.z < minZ) {
				minZ = vect1.z;
			} else if (vect1.z > maxZ) {
				maxZ = vect1.z;
			}
		}
		Vector3f.recycle(vect1);

		center.set(minX + maxX, minY + maxY, minZ + maxZ);
		center.multLocal(0.5f);

		xExtent = maxX - center.x;
		yExtent = maxY - center.y;
		zExtent = maxZ - center.z;
	}

	/**
	 * 用 4x4 矩阵变换本包围盒，结果写入 store（或新建）。
	 * Transforms this bounding box by a 4x4 matrix into store (or a new box).
	 *
	 * @param trans 变换矩阵 / transform matrix
	 * @param store 结果存储；null 或类型不匹配时新建 / result store; created if null or wrong type
	 * transformed AABB
	 */
	@Override
	public BoundingVolume transform(Matrix4f trans, BoundingVolume store) {
		BoundingBox box;
		if (store == null || store.getType() != Type.AABB) {
			box = new BoundingBox();
		} else {
			box = (BoundingBox) store;
		}

		float w = trans.multProj(center, box.center);
		box.center.divideLocal(w);

		Matrix3f transMatrix = Matrix3f.newInstance();
		trans.toRotationMatrix(transMatrix);

		// 将旋转矩阵取正以得到最大 x/y/z 范围 / Make the rotation matrix all positive to get the maximum x/y/z extent
		transMatrix.absoluteLocal();
		Vector3f vect1 = Vector3f.newInstance();
		vect1.set(xExtent, yExtent, zExtent);
		transMatrix.mult(vect1, vect1);

		// 缩放后分配最大旋转。 / Assign the biggest rotations after scales.
		box.xExtent = FastMath.abs(vect1.getX());
		box.yExtent = FastMath.abs(vect1.getY());
		box.zExtent = FastMath.abs(vect1.getZ());
		Vector3f.recycle(vect1);
		Matrix3f.recycle(transMatrix);

		return box;
	}

	/**
	 * 判断本包围盒相对给定平面（通常来自视锥）位于哪一侧。
	 * Determines which side of the given plane (typically from a view frustum) this bound lies on.
	 *
	 * @param plane 待检测平面 / plane to check against
	 * plane side
	 */
	@Override
	public Plane.Side whichSide(Plane plane) {
		float radius = FastMath.abs(xExtent * plane.getNormal().getX())
				+ FastMath.abs(yExtent * plane.getNormal().getY()) + FastMath.abs(zExtent * plane.getNormal().getZ());

		float distance = plane.pseudoDistance(center);

		// 改为 < 与 > 以防浮点精度问题 / changed to < and > to prevent floating point precision problems
		if (distance < -radius) {
			return Plane.Side.Negative;
		} else if (distance > radius) {
			return Plane.Side.Positive;
		} else {
			return Plane.Side.None;
		}
	}

	/**
	 * 合并本包围盒与另一包围体，返回能包容两者的新 AABB。
	 * Merges this box with another volume and returns a new AABB containing both.
	 *
	 * @param volume 待合并的包围体 / volume to merge with
	 * @return 合并后的包围体；不支持的类型返回 null / merged volume, or null if unsupported
	 */
	@Override
	public BoundingVolume merge(BoundingVolume volume) {
		if (volume == null) {
			return this;
		}

		switch (volume.getType()) {
		case AABB: {
			BoundingBox vBox = (BoundingBox) volume;
			return merge(vBox.center, vBox.xExtent, vBox.yExtent, vBox.zExtent,
					new BoundingBox(new Vector3f(0, 0, 0), 0, 0, 0));
		}

		// case OBB: {
		// OrientedBoundingBox box = (OrientedBoundingBox) volume;
		// BoundingBox rVal = (BoundingBox) this.clone(null);
		// return rVal.mergeOBB(box);
		// }
		default:
			return null;
		}
	}

	/**
	 * 就地合并另一包围体，修改自身以包容两者。
	 * Merges another volume into this box in place, expanding to contain both.
	 *
	 * @param volume 待合并的包围体 / volume to merge with
	 * @return 本实例；不支持的类型返回 null / this, or null if unsupported
	 */
	@Override
	public BoundingVolume mergeLocal(BoundingVolume volume) {
		if (volume == null) {
			return this;
		}

		switch (volume.getType()) {
		case AABB: {
			BoundingBox vBox = (BoundingBox) volume;
			return merge(vBox.center, vBox.xExtent, vBox.yExtent, vBox.zExtent, this);
		}
		// case OBB: {
		// return mergeOBB((OrientedBoundingBox) volume);
		// }
		default:
			return null;
		}
	}

	/**
	 * 将此 AABB 与给定 OBB 合并。 / Merges this AABB with the given OBB.
	 */
	// private BoundingBox mergeOBB(OrientedBoundingBox volume) {
	// if (!volume.correctCorners)
	// volume.computeCorners();
	//
	// TempVars vars = TempVars.get();
	// Vector3f min = vars.compVect1.set(center.x - xExtent, center.y - yExtent,
	// center.z - zExtent);
	// Vector3f max = vars.compVect2.set(center.x + xExtent, center.y + yExtent,
	// center.z + zExtent);
	//
	// for (int i = 1; i < volume.vectorStore.length; i++) {
	// Vector3f temp = volume.vectorStore[i];
	// if (temp.x < min.x)
	// min.x = temp.x;
	// else if (temp.x > max.x)
	// max.x = temp.x;
	//
	// if (temp.y < min.y)
	// min.y = temp.y;
	// else if (temp.y > max.y)
	// max.y = temp.y;
	//
	// if (temp.z < min.z)
	// min.z = temp.z;
	// else if (temp.z > max.z)
	// max.z = temp.z;
	// }
	//
	// center.set(min.addLocal(max));
	// center.multLocal(0.5f);
	//
	// xExtent = max.x - center.x;
	// yExtent = max.y - center.y;
	// zExtent = max.z - center.z;
	// return this;
	// }

	/**
	 * 将本包围盒与以中心/半长描述的另一盒子合并，结果写入 rVal。
	 * Merges this box with another defined by center and extents into rVal.
	 *
	 * @param boxCenter 待合并盒子的中心 / center of the box to merge with
	 * @param boxX 待合并盒子的 X 半长 / X extent of the box to merge with
	 * @param boxY 待合并盒子的 Y 半长 / Y extent of the box to merge with
	 * @param boxZ 待合并盒子的 Z 半长 / Z extent of the box to merge with
	 * @param rVal 结果盒子 / resulting merged box
	 * @return 合并结果盒子 / the resulting merged box
	 */
	private BoundingBox merge(Vector3f boxCenter, float boxX, float boxY, float boxZ, BoundingBox rVal) {
		Vector3f vect1 = Vector3f.newInstance();
		Vector3f vect2 = Vector3f.newInstance();

		vect1.x = center.x - xExtent;
		if (vect1.x > boxCenter.x - boxX) {
			vect1.x = boxCenter.x - boxX;
		}
		vect1.y = center.y - yExtent;
		if (vect1.y > boxCenter.y - boxY) {
			vect1.y = boxCenter.y - boxY;
		}
		vect1.z = center.z - zExtent;
		if (vect1.z > boxCenter.z - boxZ) {
			vect1.z = boxCenter.z - boxZ;
		}

		vect2.x = center.x + xExtent;
		if (vect2.x < boxCenter.x + boxX) {
			vect2.x = boxCenter.x + boxX;
		}
		vect2.y = center.y + yExtent;
		if (vect2.y < boxCenter.y + boxY) {
			vect2.y = boxCenter.y + boxY;
		}
		vect2.z = center.z + zExtent;
		if (vect2.z < boxCenter.z + boxZ) {
			vect2.z = boxCenter.z + boxZ;
		}

		center.set(vect2).addLocal(vect1).multLocal(0.5f);

		xExtent = vect2.x - center.x;
		yExtent = vect2.y - center.y;
		zExtent = vect2.z - center.z;

		Vector3f.recycle(vect1);
		Vector3f.recycle(vect2);
		return rVal;
	}

	/**
	 * 克隆本包围盒；若 store 为 AABB 则复用，否则新建。
	 * Clones this bounding box; reuses store when it is an AABB, otherwise creates a new one.
	 *
	 * @param store 结果存储；null 或类型不匹配时新建 / store for the clone; created if null or wrong type
	 * @return 克隆的包围盒 / the cloned bounding box
	 */
	@Override
	public BoundingBox clone(BoundingVolume store) {
		if (store != null && store.getType() == Type.AABB) {
			BoundingBox rVal = (BoundingBox) store;
			rVal.center.set(center);
			rVal.xExtent = xExtent;
			rVal.yExtent = yExtent;
			rVal.zExtent = zExtent;
			rVal.checkPlane = checkPlane;
			return rVal;
		}

		BoundingBox rVal = new BoundingBox(center.clone(), xExtent, yExtent, zExtent);
		return rVal;
	}

	/**
	 * 返回本对象的字符串表示（中心与三轴半长）。
	 * Returns the string representation (center and extents).
	 *
	 * @return 字符串表示 / string representation
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [Center: " + center + "  xExtent: " + xExtent + "  yExtent: " + yExtent
				+ "  zExtent: " + zExtent + "]";
	}

	/**
	 * 检测本包围盒是否与给定包围球相交。
	 * Determines whether this bounding box intersects the given bounding sphere.
	 *
	 * @param bs 包围球 / bounding sphere
	 * whether they intersect
	 */
	@Override
	public boolean intersectsSphere(BoundingSphere bs) {
		return ((FastMath.abs(center.x - bs.center.x) < bs.getRadius() + xExtent)
				&& (FastMath.abs(center.y - bs.center.y) < bs.getRadius() + yExtent)
				&& (FastMath.abs(center.z - bs.center.z) < bs.getRadius() + zExtent));
	}

	/**
	 * 检测本包围盒是否与另一包围体相交（委托对方的 AABB 相交实现）。
	 * Determines whether this box intersects another volume (delegates to the peer AABB test).
	 *
	 * @param bv 另一包围体 / other bounding volume
	 * whether they intersect
	 */
	@Override
	public boolean intersects(BoundingVolume bv) {
		return bv.intersectsBoundingBox(this);
	}

	/**
	 * 检测本包围盒是否与另一 AABB 在任意轴上相交。
	 * Determines whether this box intersects another AABB in any way.
	 *
	 * other AABB
	 * whether they intersect
	 */
	@Override
	public boolean intersectsBoundingBox(BoundingBox bb) {
		assert Vector3f.isValidVector(center) && Vector3f.isValidVector(bb.center);

		if (center.x + xExtent < bb.center.x - bb.xExtent || center.x - xExtent > bb.center.x + bb.xExtent) {
			return false;
		} else if (center.y + yExtent < bb.center.y - bb.yExtent || center.y - yExtent > bb.center.y + bb.yExtent) {
			return false;
		} else if (center.z + zExtent < bb.center.z - bb.zExtent || center.z - zExtent > bb.center.z + bb.zExtent) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 判断此包围体是否与给定包围盒相交。 / determines if this bounding box intersects with a given oriented bounding box. @see com.jme.bounding.BoundingVolume#intersectsOrientedBoundingBox(com.jme.bounding.OrientedBoundingBox)
	 */
	// public boolean intersectsOrientedBoundingBox(OrientedBoundingBox obb) {
	// return obb.intersectsBoundingBox(this);
	// }

	/**
	 * 检测本包围盒是否与射线相交。
	 * Determines whether this bounding box intersects the given ray.
	 *
	 * ray
	 * whether they intersect
	 */
	@Override
	public boolean intersects(Ray ray) {
		// assert Vector3f.isValidVector(center);

		float rhs;

		Vector3f vect1 = Vector3f.newInstance();
		Vector3f vect2 = Vector3f.newInstance();
		Vector3f diff = ray.origin.subtract(getCenter(vect2), vect1);

		final Array3f fWdU = Array3f.newInstance();
		final Array3f fAWdU = Array3f.newInstance();
		final Array3f fDdU = Array3f.newInstance();
		final Array3f fADdU = Array3f.newInstance();
		final Array3f fAWxDdU = Array3f.newInstance();

		fWdU.a = ray.getDirection().dot(Vector3f.UNIT_X);
		fAWdU.a = FastMath.abs(fWdU.a);
		fDdU.a = diff.dot(Vector3f.UNIT_X);
		fADdU.a = FastMath.abs(fDdU.a);
		if (fADdU.a > xExtent && fDdU.a * fWdU.a >= 0.0) {
			Vector3f.recycle(vect1);
			Vector3f.recycle(vect2);
			Array3f.recycle(fWdU);
			Array3f.recycle(fAWdU);
			Array3f.recycle(fDdU);
			Array3f.recycle(fADdU);
			Array3f.recycle(fAWxDdU);
			return false;
		}

		fWdU.b = ray.getDirection().dot(Vector3f.UNIT_Y);
		fAWdU.b = FastMath.abs(fWdU.b);
		fDdU.b = diff.dot(Vector3f.UNIT_Y);
		fADdU.b = FastMath.abs(fDdU.b);
		if (fADdU.b > yExtent && fDdU.b * fWdU.b >= 0.0) {
			Vector3f.recycle(vect1);
			Vector3f.recycle(vect2);
			Array3f.recycle(fWdU);
			Array3f.recycle(fAWdU);
			Array3f.recycle(fDdU);
			Array3f.recycle(fADdU);
			Array3f.recycle(fAWxDdU);
			return false;
		}

		fWdU.c = ray.getDirection().dot(Vector3f.UNIT_Z);
		fAWdU.c = FastMath.abs(fWdU.c);
		fDdU.c = diff.dot(Vector3f.UNIT_Z);
		fADdU.c = FastMath.abs(fDdU.c);
		if (fADdU.c > zExtent && fDdU.c * fWdU.c >= 0.0) {
			Vector3f.recycle(vect1);
			Vector3f.recycle(vect2);
			Array3f.recycle(fWdU);
			Array3f.recycle(fAWdU);
			Array3f.recycle(fDdU);
			Array3f.recycle(fADdU);
			Array3f.recycle(fAWxDdU);
			return false;
		}

		Vector3f wCrossD = ray.getDirection().cross(diff, vect2);

		fAWxDdU.a = FastMath.abs(wCrossD.dot(Vector3f.UNIT_X));
		rhs = yExtent * fAWdU.c + zExtent * fAWdU.b;
		if (fAWxDdU.a > rhs) {
			Vector3f.recycle(vect1);
			Vector3f.recycle(vect2);
			Array3f.recycle(fWdU);
			Array3f.recycle(fAWdU);
			Array3f.recycle(fDdU);
			Array3f.recycle(fADdU);
			Array3f.recycle(fAWxDdU);
			return false;
		}

		fAWxDdU.b = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Y));
		rhs = xExtent * fAWdU.c + zExtent * fAWdU.a;
		if (fAWxDdU.b > rhs) {
			Vector3f.recycle(vect1);
			Vector3f.recycle(vect2);
			Array3f.recycle(fWdU);
			Array3f.recycle(fAWdU);
			Array3f.recycle(fDdU);
			Array3f.recycle(fADdU);
			Array3f.recycle(fAWxDdU);
			return false;
		}

		fAWxDdU.c = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Z));
		rhs = xExtent * fAWdU.b + yExtent * fAWdU.a;
		if (fAWxDdU.c > rhs) {
			Vector3f.recycle(vect1);
			Vector3f.recycle(vect2);
			Array3f.recycle(fWdU);
			Array3f.recycle(fAWdU);
			Array3f.recycle(fDdU);
			Array3f.recycle(fADdU);
			Array3f.recycle(fAWxDdU);
			return false;
		}
		Vector3f.recycle(vect1);
		Vector3f.recycle(vect2);
		Array3f.recycle(fWdU);
		Array3f.recycle(fAWdU);
		Array3f.recycle(fDdU);
		Array3f.recycle(fADdU);
		Array3f.recycle(fAWxDdU);
		return true;
	}

	/**
	 * 与射线求交并将碰撞结果写入 results。
	 * Intersects this box with a ray and records collision results.
	 *
	 * ray
	 *
	 * @param results 碰撞结果收集器 / collision results collector
	 * @param results number of collision points (0 / 1/2)
	 */
	private int collideWithRay(Ray ray, CollisionResults results) {
		Vector3f diff = Vector3f.newInstance().set(ray.origin).subtractLocal(center);
		Vector3f direction = Vector3f.newInstance().set(ray.direction);

		float[] t = { 0f, Float.POSITIVE_INFINITY };

		float saveT0 = t[0], saveT1 = t[1];
		boolean notEntirelyClipped = clip(+direction.x, -diff.x - xExtent, t)
				&& clip(-direction.x, +diff.x - xExtent, t) && clip(+direction.y, -diff.y - yExtent, t)
				&& clip(-direction.y, +diff.y - yExtent, t) && clip(+direction.z, -diff.z - zExtent, t)
				&& clip(-direction.z, +diff.z - zExtent, t);
		Vector3f.recycle(diff);
		Vector3f.recycle(direction);

		if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
			if (t[1] > t[0]) {
				float[] distances = t;
				Vector3f[] points = new Vector3f[] {
						new Vector3f(ray.direction).multLocal(distances[0]).addLocal(ray.origin),
						new Vector3f(ray.direction).multLocal(distances[1]).addLocal(ray.origin) };

				CollisionResult result = new CollisionResult(points[0], distances[0]);
				results.addCollision(result);
				result = new CollisionResult(points[1], distances[1]);
				results.addCollision(result);
				return 2;
			}

			Vector3f point = new Vector3f(ray.direction).multLocal(t[0]).addLocal(ray.origin);
			CollisionResult result = new CollisionResult(point, t[0]);
			results.addCollision(result);
			return 1;
		}
		return 0;
	}

	/**
	 * 与可碰撞对象求交（支持 Ray、Triangle）。
	 * Collides with a collidable (supports Ray and Triangle).
	 *
	 * @param other 可碰撞对象 / collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * collision count
	 * unsupported type。
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if (other instanceof Ray) {
			Ray ray = (Ray) other;
			return collideWithRay(ray, results);
		} else if (other instanceof Triangle) {
			Triangle t = (Triangle) other;
			if (intersects(t.get1(), t.get2(), t.get3())) {
				CollisionResult r = new CollisionResult();
				results.addCollision(r);
				return 1;
			}
			return 0;
		} else {
			throw new UnsupportedCollisionException("With: " + other.getClass().getSimpleName());
		}
	}

	/**
	 * 检测本包围盒是否与由三顶点构成的三角形相交（移植自 Tomas Akenine-Möller 的 tribox3）。
	 * Tests intersection with a triangle given by three vertices (ported from Tomas Akenine-Möller's tribox3).
	 *
	 * @param v1 三角形顶点 1 / triangle vertex 1
	 * @param v2 三角形顶点 2 / triangle vertex 2
	 * @param v3 三角形顶点 3 / triangle vertex 3
	 * whether they intersect
	 */
	public boolean intersects(Vector3f v1, Vector3f v2, Vector3f v3) {
		return Intersection.intersect(this, v1, v2, v3);
	}

	/**
	 * 判断点是否严格位于盒内（不含边界）。
	 * Returns whether the point lies strictly inside the box (boundary excluded).
	 *
	 * point to test
	 *
	 * @param point
	 * @return 是否在内部 / whether inside
	 */
	@Override
	public boolean contains(Vector3f point) {
		return FastMath.abs(center.x - point.x) < xExtent && FastMath.abs(center.y - point.y) < yExtent
				&& FastMath.abs(center.z - point.z) < zExtent;
	}

	/**
	 * 判断点是否与盒子相交（含边界）。
	 * Returns whether the point intersects the box (boundary included).
	 *
	 * point to test
	 * whether they intersect
	 */
	@Override
	public boolean intersects(Vector3f point) {
		return FastMath.abs(center.x - point.x) <= xExtent && FastMath.abs(center.y - point.y) <= yExtent
				&& FastMath.abs(center.z - point.z) <= zExtent;
	}

	/**
	 * 计算点到盒子表面的最短距离；点在内部时为 0。
	 * Distance from the point to the box surface; zero when the point is inside.
	 *
	 * point to test
	 *
	 * @param point
	 * @return 到边缘的距离 / distance to edge
	 */
	@Override
	public float distanceToEdge(Vector3f point) {
		// 在盒子坐标系中计算点的坐标 / compute coordinates of point in box coordinate system
		Vector3f closest = point.subtract(center);

		// 将测试点投影到盒子上 / project test point onto box
		float sqrDistance = 0.0f;
		float delta;

		if (closest.x < -xExtent) {
			delta = closest.x + xExtent;
			sqrDistance += delta * delta;
			closest.x = -xExtent;
		} else if (closest.x > xExtent) {
			delta = closest.x - xExtent;
			sqrDistance += delta * delta;
			closest.x = xExtent;
		}

		if (closest.y < -yExtent) {
			delta = closest.y + yExtent;
			sqrDistance += delta * delta;
			closest.y = -yExtent;
		} else if (closest.y > yExtent) {
			delta = closest.y - yExtent;
			sqrDistance += delta * delta;
			closest.y = yExtent;
		}

		if (closest.z < -zExtent) {
			delta = closest.z + zExtent;
			sqrDistance += delta * delta;
			closest.z = -zExtent;
		} else if (closest.z > zExtent) {
			delta = closest.z - zExtent;
			sqrDistance += delta * delta;
			closest.z = zExtent;
		}

		return FastMath.sqrt(sqrDistance);
	}

	/**
	 * 线段相对测试平面的裁剪；用于射线-AABB 求交。
	 * Clips a line segment against a test plane (used by ray-AABB intersection).
	 *
	 * @param denom 线段分母 / denominator of the line segment
	 * @param numer 线段分子 / numerator of the line segment
	 * @param t 平面参数区间 [t0, t1] / plane parameter interval [t0, t1]
	 * @return 线段是否仍与当前平面相交 / true if the segment still intersects the plane
	 */
	private boolean clip(float denom, float numer, float[] t) {
		// 线段与当前测试相交时返回 true / Return value is 'true' if line segment intersects the current test
		// 平面。否则返回 false，此时线段 / plane. Otherwise 'false' is returned in which case the line segment
		// 被完全裁剪。 / is entirely clipped.
		if (denom > 0.0f) {
			if (numer > denom * t[1]) {
				return false;
			}
			if (numer > denom * t[0]) {
				t[0] = numer / denom;
			}
			return true;
		} else if (denom < 0.0f) {
			if (numer > denom * t[0]) {
				return false;
			}
			if (numer > denom * t[1]) {
				t[1] = numer / denom;
			}
			return true;
		} else {
			return numer <= 0.0;
		}
	}

	/**
	 * 查询三轴半长向量。
	 * Queries the extent vector (x/y/z half-lengths).
	 *
	 * @param store 结果存储；null 时新建 / store for extents; created if null
	 * extent vector
	 */
	public Vector3f getExtent(Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		store.set(xExtent, yExtent, zExtent);
		return store;
	}

	/**
	 * 获取 X 轴半长。
	 * Returns the X-axis half-extent.
	 *
	 * X extent
	 */
	public float getXExtent() {
		return xExtent;
	}

	/**
	 * 获取 Y 轴半长。
	 * Returns the Y-axis half-extent.
	 *
	 * Y extent
	 */
	public float getYExtent() {
		return yExtent;
	}

	/**
	 * 获取 Z 轴半长。
	 * Returns the Z-axis half-extent.
	 *
	 * Z extent
	 */
	public float getZExtent() {
		return zExtent;
	}

	/**
	 * 设置 X 轴半长（不可为负）。
	 * Sets the X-axis half-extent (must be non-negative).
	 *
	 * X extent
	 * if negative
	 */
	public void setXExtent(float xExtent) {
		if (xExtent < 0) {
			throw new IllegalArgumentException();
		}

		this.xExtent = xExtent;
	}

	/**
	 * 设置 Y 轴半长（不可为负）。
	 * Sets the Y-axis half-extent (must be non-negative).
	 *
	 * Y extent
	 * if negative
	 */
	public void setYExtent(float yExtent) {
		if (yExtent < 0) {
			throw new IllegalArgumentException();
		}

		this.yExtent = yExtent;
	}

	/**
	 * 设置 Z 轴半长（不可为负）。
	 * Sets the Z-axis half-extent (must be non-negative).
	 *
	 * Z extent
	 * if negative
	 */
	public void setZExtent(float zExtent) {
		if (zExtent < 0) {
			throw new IllegalArgumentException();
		}

		this.zExtent = zExtent;
	}

	/**
	 * 获取最小角点（中心减半长）。
	 * Returns the minimum corner (center minus extents).
	 *
	 * @param store 结果存储；null 时新建 / store for the result; created if null
	 * minimum corner
	 */
	public Vector3f getMin(Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		store.set(center).subtractLocal(xExtent, yExtent, zExtent);
		return store;
	}

	/**
	 * 获取最大角点（中心加半长）。
	 * Returns the maximum corner (center plus extents).
	 *
	 * @param store 结果存储；null 时新建 / store for the result; created if null
	 * maximum corner
	 */
	public Vector3f getMax(Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		store.set(center).addLocal(xExtent, yExtent, zExtent);
		return store;
	}

	/**
	 * 由最小/最大角点设置中心与三轴半长。
	 * Sets center and extents from minimum and maximum corners.
	 *
	 * @param min 最小角点 / minimum corner
	 * @param max 最大角点 / maximum corner
	 */
	public void setMinMax(Vector3f min, Vector3f max) {
		this.center.set(max).addLocal(min).multLocal(0.5f);
		xExtent = FastMath.abs(max.x - center.x);
		yExtent = FastMath.abs(max.y - center.y);
		zExtent = FastMath.abs(max.z - center.z);
	}

	/**
	 * 返回包围盒体积（8 × xExtent × yExtent × zExtent）。
	 * Returns the box volume (8 × xExtent × yExtent × zExtent).
	 *
	 * volume
	 */
	@Override
	public float getVolume() {
		return (8 * xExtent * yExtent * zExtent);
	}
}
