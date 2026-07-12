package com.aionemu.gameserver.geoEngine.scene;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.bounding.Intersection;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.UnsupportedCollisionException;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Triangle;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 导航网格中的单个三角形节点，是 {@link Spatial} 的简单扩展。
 * A single triangular node of a navigation mesh; a simple extension of {@link Spatial}.
 * <p>
 * 仅支持三角形节点；假定给定节点的平面均不与竖直 Z 轴平行。
 * Only triangular nodes are supported. It is assumed that none of the nodes given to this class have a plane parallel to the (vertical) Z-axis.
 * <p>
 * 维护与邻接节点的引用供后续寻路使用，并用 incenter 估计与其他点的距离、用 inRad 作为离开本节点的路径长度。
 * Neighbor references are kept for pathfinding; an incenter estimates distance to other points, and inRad acts as path length off this node.
 *
 * @author Yon (Aion Reconstruction Project)
 */
public class NavGeometry extends Spatial {

	/** 边 1 邻接三角形。 / Neighbor across edge 1. */
	private NavGeometry edge1;
	/** 边 2 邻接三角形。 / Neighbor across edge 2. */
	private NavGeometry edge2;
	/** 边 3 邻接三角形。 / Neighbor across edge 3. */
	private NavGeometry edge3;
	/**
	 * 紧凑几何数据：前 9 项为三顶点 (x,y,z)×3，接着 3 项为内心，最后一项为内切圆半径。
	 * Compact geometry data: first 9 entries are the three vertices (x,y,z)×3, next three are the incenter, last is the incircle radius.
	 */
	final private float[] data;
//	final public float inRad;
//	final public float[] incenter;

	/**
	 * 以名称与 9 个 float 顶点构造导航三角形，并计算内心与内切圆半径。
	 * Constructs a nav triangle from a name and 9 vertex floats, computing incenter and inradius.
	 *
	 * @param name 节点名称 / node name
	 * @param verts 长度必须为 9 的顶点数组 / vertex array of length 9
	 */
	public NavGeometry(String name, float[] verts) {
		assert verts.length == 9:"NavGeometry does not support non-triangle nodes!";
		this.data = new float[13];
		System.arraycopy(verts, 0, this.data, 0, verts.length);

		float[] p1 = getVertex(0);
		float[] p2 = getVertex(1);
		float[] p3 = getVertex(2);
		float[] edge1 = {p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]};
		float[] edge2 = {p3[0] - p2[0], p3[1] - p2[1], p3[2] - p2[2]};
		float[] edge3 = {p1[0] - p3[0], p1[1] - p3[1], p1[2] - p3[2]};
		float edge1Len = (float) Math.sqrt(sumOfSquaredComps(edge1));
		float edge2Len = (float) Math.sqrt(sumOfSquaredComps(edge2));
		float edge3Len = (float) Math.sqrt(sumOfSquaredComps(edge3));
		float lenSum = edge1Len + edge2Len + edge3Len;
		float[] incenter = new float[] {((edge2Len*p1[0]) + (edge3Len*p2[0]) + (edge1Len*p3[0]))/lenSum,
		                        ((edge2Len*p1[1]) + (edge3Len*p2[1]) + (edge1Len*p3[1]))/lenSum,
		                        ((edge2Len*p1[2]) + (edge3Len*p2[2]) + (edge1Len*p3[2]))/lenSum};
		System.arraycopy(incenter, 0, this.data, 9, incenter.length);
		float inRad = ((float) Math.sqrt(lenSum*(lenSum - edge1Len)*(lenSum - edge2Len)*(lenSum - edge3Len)))/lenSum;
		this.data[12] = inRad;
	}

	/** 向量分量平方和。 / Sum of squared vector components. */
	private float sumOfSquaredComps(float[] vec) {
		return (vec[0]*vec[0]) + (vec[1]*vec[1]) + (vec[2]*vec[2]);
	}

	/**
	 * 设置边 1 的邻接三角形。
	 * Sets the neighbor across edge 1.
	 *
	 * neighboring node
	 */
	public void setEdge1(NavGeometry connection) {
		edge1 = connection;
	}

	/**
	 * 设置边 2 的邻接三角形。
	 * Sets the neighbor across edge 2.
	 *
	 * neighboring node
	 */
	public void setEdge2(NavGeometry connection) {
		edge2 = connection;
	}

	/**
	 * 设置边 3 的邻接三角形。
	 * Sets the neighbor across edge 3.
	 *
	 * neighboring node
	 */
	public void setEdge3(NavGeometry connection) {
		edge3 = connection;
	}

	/**
	 * 返回边 1 的邻接三角形。
	 * Returns the neighbor across edge 1.
	 *
	 * neighboring node
	 */
	public NavGeometry getEdge1() {
		return edge1;
	}

	/**
	 * 返回边 2 的邻接三角形。
	 * Returns the neighbor across edge 2.
	 *
	 * neighboring node
	 */
	public NavGeometry getEdge2() {
		return edge2;
	}

	/**
	 * 返回边 3 的邻接三角形。
	 * Returns the neighbor across edge 3.
	 *
	 * neighboring node
	 */
	public NavGeometry getEdge3() {
		return edge3;
	}

	/**
	 * 返回与给定三角形匹配的边编号（1–3）；无匹配返回 0。
	 * Returns the edge number (1–3) matching the given triangle; 0 if none.
	 *
	 * @param tri 邻接三角形 / neighboring triangle
	 * edge number or 0
	 */
	public byte getEdgeMatching(NavGeometry tri) {
		if (edge1 == tri) return 1;
		if (edge2 == tri) return 2;
		if (edge3 == tri) return 3;
		return 0;
	}

	/**
	 * 计算给定点到本三角形表面上的真实最近点。
	 * Computes the true closest point on this triangle surface to the given point.
	 * <p>
	 * 采用 Ericson《Real-Time Collision Detection》风格的重心坐标区域判定：
	 * 先判断点是否落在三个顶点/三条边的维诺区域，否则投影到三角形内部。
	 * Real-Time Collision Detection style barycentric region tests:
	 * vertex and edge Voronoi regions are handled first, otherwise the point is projected inside the triangle.
	 *
	 * @param x 查询点 X / query point X
	 * @param y 查询点 Y / query point Y
	 * @param z 查询点 Z / query point Z
	 * closest point as [x, y, z]
	 */
	public float[] getClosestPoint(float x, float y, float z) {
		float abX = data[3] - data[0], abY = data[4] - data[1], abZ = data[5] - data[2];
		float acX = data[6] - data[0], acY = data[7] - data[1], acZ = data[8] - data[2];
		float apX = x - data[0], apY = y - data[1], apZ = z - data[2];
		float d1 = abX * apX + abY * apY + abZ * apZ;
		float d2 = acX * apX + acY * apY + acZ * apZ;
		if (d1 <= 0 && d2 <= 0) return getVertex(0);

		float bpX = x - data[3], bpY = y - data[4], bpZ = z - data[5];
		float d3 = abX * bpX + abY * bpY + abZ * bpZ;
		float d4 = acX * bpX + acY * bpY + acZ * bpZ;
		if (d3 >= 0 && d4 <= d3) return getVertex(1);

		float vc = d1 * d4 - d3 * d2;
		if (vc <= 0 && d1 >= 0 && d3 <= 0) {
			float v = d1 / (d1 - d3);
			return new float[] {data[0] + v * abX, data[1] + v * abY, data[2] + v * abZ};
		}

		float cpX = x - data[6], cpY = y - data[7], cpZ = z - data[8];
		float d5 = abX * cpX + abY * cpY + abZ * cpZ;
		float d6 = acX * cpX + acY * cpY + acZ * cpZ;
		if (d6 >= 0 && d5 <= d6) return getVertex(2);

		float vb = d5 * d2 - d1 * d6;
		if (vb <= 0 && d2 >= 0 && d6 <= 0) {
			float w = d2 / (d2 - d6);
			return new float[] {data[0] + w * acX, data[1] + w * acY, data[2] + w * acZ};
		}

		float va = d3 * d6 - d5 * d4;
		if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0) {
			float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
			return new float[] {data[3] + w * (data[6] - data[3]), data[4] + w * (data[7] - data[4]),
					data[5] + w * (data[8] - data[5])};
		}

		float denominator = 1 / (va + vb + vc);
		float v = vb * denominator;
		float w = vc * denominator;
		return new float[] {data[0] + abX * v + acX * w, data[1] + abY * v + acY * w,
				data[2] + abZ * v + acZ * w};
	}

	/**
	 * 返回第 i 个顶点的 [x, y, z] 拷贝（i ∈ {0,1,2}）。
	 * Returns a copy of vertex i as [x, y, z] (i ∈ {0,1,2}).
	 *
	 * @param i 顶点索引 / vertex index
	 * vertex coordinates
	 */
	public float[] getVertex(int i) {
		return new float[] {data[i*3], data[i*3 + 1], data[i*3 + 2]};
	}

	/**
	 * 返回指定边的两个端点坐标。
	 * Returns the two endpoints of the given edge.
	 *
	 * edge number (1–3)
	 *
	 * @param edge
	 * @return 两端点，或无效边时为 null / endpoints, or null for an invalid edge
	 */
	public float[][] getEndpoints(byte edge) {
		float[][] ret = new float[2][];
		switch (edge) {
		case 1:
			ret[0] = new float[] {data[0], data[1], data[2]};
			ret[1] = new float[] {data[3], data[4], data[5]};
			break;
		case 2:
			ret[0] = new float[] {data[3], data[4], data[5]};
			ret[1] = new float[] {data[6], data[7], data[8]};
			break;
		case 3:
			ret[0] = new float[] {data[6], data[7], data[8]};
			ret[1] = new float[] {data[0], data[1], data[2]};
			break;
		default:
			assert false:"NavGeometry: Unknown edge: " + edge;
			return null;
		}
		return ret;
	}

	/**
	 * 返回内切圆半径。
	 * Returns the incircle radius.
	 *
	 * @return 内切圆半径 / inradius
	 */
	public float getInRad() {
		return data[12];
	}

	/**
	 * 以内心到给定点的曼哈顿距离作为优先级（越小越优先）。
	 * Priority as Manhattan distance from the incenter to the given point (smaller is better).
	 *
	 * @param x 查询点 X / query X
	 * @param y 查询点 Y / query Y
	 * @param z 查询点 Z / query Z
	 * priority value
	 */
	public float getPriority(float x, float y, float z) {
		float[] incenter = new float[] {data[9], data[10], data[11]};
		float dx = Math.abs(incenter[0] - x);
		float dy = Math.abs(incenter[1] - y);
		float dz = Math.abs(incenter[2] - z);
		return dx + dy + dz;
	}

	/**
	 * 在 XY 平面判断从第三顶点出发的方向是否朝向指定边。
	 * On the XY plane, tests whether a direction from the third vertex points toward the given edge.
	 *
	 * edge number (1–3)
	 *
	 * @param vec 目标点（用 x,y） / destination point (x,y used)
	 * @param vec
	 * @return 朝向该边则为 true / true if toward the edge
	 */
	public boolean isTowardsEdge(byte edge, float[] vec) {
		float[] p0 = new float[] {data[0], data[1]};
		float[] p1 = new float[] {data[3], data[4]};
		float[] p2 = new float[] {data[6], data[7]};
		float[] vec1;
		float[] vec2;
		float[] vec3;
		switch (edge) {
		case 1:
			// 边 1 是点 0 与 1 / Edge 1 is point 0 and 1
			vec1 = new float[] {p0[0] - p2[0], p0[1] - p2[1]};
			vec2 = new float[] {p1[0] - p2[0], p1[1] - p2[1]};
			vec3 = new float[] {vec[0] - p2[0], vec[1] - p2[1]};
			break;
		case 2:
			// 边 2 是点 1 与 2 / Edge 2 is point 1 and 2
			vec1 = new float[] {p1[0] - p0[0], p1[1] - p0[1]};
			vec2 = new float[] {p2[0] - p0[0], p2[1] - p0[1]};
			vec3 = new float[] {vec[0] - p0[0], vec[1] - p0[1]};
			break;
		case 3:
			// 边 3 是点 2 与 0 / Edge 3 is point 2 and 0
			vec1 = new float[] {p2[0] - p1[0], p2[1] - p1[1]};
			vec2 = new float[] {p0[0] - p1[0], p0[1] - p1[1]};
			vec3 = new float[] {vec[0] - p1[0], vec[1] - p1[1]};
			break;
		default:
			return false;
		}
		boolean positive = crossZ(vec1, vec2) > 0;
		if (compareCross(crossZ(vec1, vec3), positive) && compareCross(crossZ(vec3, vec2), positive)) {
			return true; //vec3 is between vec1 and vec2
		}
		return false;
	}

	/**
	 * 从起点到指定边两端点构造漏斗，判断方向向量是否穿过该边。
	 * Builds a funnel from a start point to the edge endpoints and tests whether the direction passes through the edge.
	 * <p>
	 * 非法边号时按边 3 处理。
	 * Invalid edge numbers are treated as edge 3.
	 *
	 * @param edge 边编号（有效 1–3） / edge number (valid range 1–3)
	 * @param dir 从漏斗起点指向目标的方向向量 / direction from funnel start to destination
	 * @param x 漏斗起点 X / funnel start X
	 * @param y 漏斗起点 Y / funnel start Y
	 * @return 方向穿过该边则为 true / true if the direction passes through the edge
	 */
	public boolean isFunnelTowardsEdge(byte edge, float[] dir, float x, float y) {
		float[][] endpoints;
		if (edge == 1) {
			endpoints = getEndpoints((byte) 1);
		} else if (edge == 2) {
			endpoints = getEndpoints((byte) 2);
		} else {
			endpoints = getEndpoints((byte) 3);
		}

		float[] vec1 = new float[] {endpoints[0][0] - x, endpoints[0][1] - y};
		float[] vec2 = new float[] {endpoints[1][0] - x, endpoints[1][1] - y};
		boolean positive = crossZ(vec1, vec2) > 0;
		if (compareCross(crossZ(vec1, dir), positive) && compareCross(crossZ(dir, vec2), positive)) {
			return true; //dir is between vec1 and vec2
		}
		return false;
	}

	/** Z component of 2D cross product / Z component of 2D cross product */
	private static float crossZ(float[] vec1, float[] vec2/*, float x1, float y1, float x2, float y2*/) {
		return ((vec1[0] * vec2[1]) - (vec1[1] * vec2[0]));
//		return ((x1 * y2) - (y1 * x2));
	}

	/** 比较叉积符号是否与期望一致（含等于 0）。 / Whether the cross-product sign matches the expected polarity (including zero). */
	private static boolean compareCross(float crossZ, boolean positive) {
		if (positive) {
			return crossZ >= 0;
		} else {
			return crossZ <= 0;
		}
	}

	/**
	 * 与射线或 AABB 做碰撞检测。
	 * Collides against a ray or AABB.
	 *
	 * @param other 目标可碰撞对象 / target collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * number of collisions
	 * for unsupported types。
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) throws UnsupportedCollisionException {
//		if ((results.getIntentions() & (getCollisionFlags() >> 8)) == 0) return 0; //This is assumed

		if (other instanceof Ray) {
			if (!worldBound.intersects(((Ray) other))) {
				return 0;
			}
			Vector3f intersection = Vector3f.newInstance();
			Vector3f p1 = Vector3f.newInstance().set(data[0], data[1], data[2]);
			Vector3f p2 = Vector3f.newInstance().set(data[3], data[4], data[5]);
			Vector3f p3 = Vector3f.newInstance().set(data[6], data[7], data[8]);
			Triangle tri = Triangle.newInstance();
			tri.set(p1, p2, p3);
			if (((Ray) other).intersectWhere(tri, intersection)) {
				Vector3f displacement = intersection.subtract(((Ray) other).getOrigin());
				float distance = displacement.length();
				if (distance > ((Ray) other).limit) {
					Triangle.recycle(tri);
					Vector3f.recycle(p1);
					Vector3f.recycle(p2);
					Vector3f.recycle(p3);
					Vector3f.recycle(displacement);
					return 0;
				}

				CollisionResult res = new CollisionResult();
				res.setContactPoint(intersection);
				res.setGeometry(this);
				res.setDistance(distance);
				results.addCollision(res);

				Triangle.recycle(tri);
				Vector3f.recycle(p1);
				Vector3f.recycle(p2);
				Vector3f.recycle(p3);
				Vector3f.recycle(displacement);
				return 1;
			}
			Triangle.recycle(tri);
			Vector3f.recycle(p1);
			Vector3f.recycle(p2);
			Vector3f.recycle(p3);
			Vector3f.recycle(intersection);
			return 0;
		} else if (other instanceof BoundingBox) {
			if (worldBound.intersects((BoundingBox) other)) {
				Vector3f p1 = Vector3f.newInstance().set(data[0], data[1], data[2]);
				Vector3f p2 = Vector3f.newInstance().set(data[3], data[4], data[5]);
				Vector3f p3 = Vector3f.newInstance().set(data[6], data[7], data[8]);
				if (Intersection.intersect(((BoundingBox) other), p1, p2, p3)) {
					CollisionResult res = new CollisionResult();
					res.setGeometry(this);
					res.setDistance(worldBound.getCenter().distance(((BoundingBox) other).getCenter()));
					results.addCollision(res);
					Vector3f.recycle(p1);
					Vector3f.recycle(p2);
					Vector3f.recycle(p3);
					return 1;
				}
				Vector3f.recycle(p1);
				Vector3f.recycle(p2);
				Vector3f.recycle(p3);
			}
			return 0;
		} else {
			throw new UnsupportedCollisionException();
		}
	}

	/**
	 * 由三顶点最小/最大点更新世界 AABB。
	 * Updates the world AABB from the three vertices' min/max.
	 */
	@Override
	public void updateModelBound() {
		Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		float[] vert1 = getVertex(0);
		float[] vert2 = getVertex(1);
		float[] vert3 = getVertex(2);
		min.setX(Math.min(vert1[0], Math.min(vert2[0], vert3[0])));
		min.setY(Math.min(vert1[1], Math.min(vert2[1], vert3[1])));
		min.setZ(Math.min(vert1[2], Math.min(vert2[2], vert3[2])));

		max.setX(Math.max(vert1[0], Math.max(vert2[0], vert3[0])));
		max.setY(Math.max(vert1[1], Math.max(vert2[1], vert3[1])));
		max.setZ(Math.max(vert1[2], Math.max(vert2[2], vert3[2])));

		if (worldBound instanceof BoundingBox) {
			((BoundingBox) worldBound).setMinMax(min, max);
		} else {
			worldBound = new BoundingBox(min, max);
		}
	}

	/**
	 * 直接设置世界包围体。
	 * Sets the world bound directly.
	 *
	 * bounding volume
	 */
	@Override
	public void setModelBound(BoundingVolume modelBound) {
		this.worldBound = modelBound;
	}

	/**
	 * 导航三角形恒为 3 顶点。
	 * A nav triangle always has 3 vertices.
	 *
	 * @return 3
	 */
	@Override
	public int getVertexCount() {
		return 3;
	}

	/**
	 * 导航三角形恒为 1 个三角形。
	 * A nav triangle always has 1 triangle.
	 *
	 * @return 1
	 */
	@Override
	public int getTriangleCount() {
		return 1;
	}

	/**
	 * 固定碰撞标志 0x100。
	 * Fixed collision flags 0x100.
	 *
	 * collision flags
	 */
	@Override
	public short getCollisionFlags() {
		return 0x100;
	}

	/**
	 * 忽略设置（标志固定）。
	 * No-op; flags are fixed.
	 *
	 * ignored
	 */
	@Override
	public void setCollisionFlags(short flags) {
		return;
	}

	/**
	 * 不支持变换。
	 * Transform is not supported.
	 *
	 * rotation
	 * translation
	 * scale
	 * always thrown
	 */
	@Override
	public void setTransform(Matrix3f rotation, Vector3f loc, float scale) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 以内心坐标加父类信息的字符串表示。
	 * String form with incenter coordinates plus superclass info.
	 *
	 * @return 描述字符串 / descriptive string
	 */
	@Override
	public String toString() {
		float[] incenter = new float[] {data[9], data[10], data[11]};
		return "(" + incenter[0] + ", " + incenter[1] + ", " + incenter[2] + ") " + super.toString();
	}

}
