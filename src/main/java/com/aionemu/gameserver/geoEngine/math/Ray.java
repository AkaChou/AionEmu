package com.aionemu.gameserver.geoEngine.math;

import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.UnsupportedCollisionException;

/**
 * 射线：原点加方向，支持与三角形/平面/包围体求交，并实现 {@link Collidable}。
 * Ray defined by origin and direction; supports triangle/plane/BV intersection and {@link Collidable}.
 */
public final class Ray implements Cloneable, Collidable {

	/** 射线原点。 / Ray origin. */
	public Vector3f origin;
	/** 射线方向（通常为单位向量）。 / Ray direction (typically unit length). */
	public Vector3f direction;
	/** 射线最大长度限制，默认正无穷。 / Maximum ray length limit; default positive infinity. */
	public float limit = Float.POSITIVE_INFINITY;

	/**
	 * 构造原点与方向均为零的射线。
	 * Constructs a ray with zero origin and direction.
	 */
	public Ray() {
		this.origin = new Vector3f();
		this.direction = new Vector3f();
	}

	/**
	 * 按原点与方向构造射线（直接持有引用）。
	 * Constructs a ray from origin and direction (holds references directly).
	 *
	 * origin
	 * direction
	 */
	public Ray(Vector3f origin, Vector3f direction) {
		this.origin = origin;
		this.direction = direction;
	}

	/**
	 * 射线与三角形求交，交点写入 loc（世界坐标）。
	 * Intersects this ray with a triangle; stores world-space hit into loc.
	 *
	 * @param t 三角形 / triangle
	 * @param loc 交点存储 / hit-point storage
	 * whether they intersect
	 */
	public boolean intersectWhere(Triangle t, Vector3f loc) {
		return this.intersectWhere(t.get(0), t.get(1), t.get(2), loc);
	}

	/**
	 * 射线与三点三角形求交，交点写入 loc（世界坐标）。
	 * Intersects this ray with a three-point triangle; stores world-space hit into loc.
	 *
	 * @param v0 顶点 0 / vertex 0
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @param loc 交点存储 / hit-point storage
	 * whether they intersect
	 */
	public boolean intersectWhere(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f loc) {
		return this.intersects(v0, v1, v2, loc, false, false);
	}

	/**
	 * 射线与三角形求交，loc 写入平面坐标 (t, w1, w2)。
	 * Intersects this ray with a triangle; stores planar coords (t, w1, w2) into loc.
	 *
	 * @param t 三角形 / triangle
	 * @param loc 平面坐标存储 / planar-coord storage
	 * whether they intersect
	 */
	public boolean intersectWherePlanar(Triangle t, Vector3f loc) {
		return this.intersectWherePlanar(t.get(0), t.get(1), t.get(2), loc);
	}

	/**
	 * 射线与三点三角形求交，loc 写入平面坐标 (t, w1, w2)。
	 * Intersects this ray with a three-point triangle; stores planar coords (t, w1, w2) into loc.
	 *
	 * @param v0 顶点 0 / vertex 0
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @param loc 平面坐标存储 / planar-coord storage
	 * whether they intersect
	 */
	public boolean intersectWherePlanar(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f loc) {
		return this.intersects(v0, v1, v2, loc, true, false);
	}

	/**
	 * Möller–Trumbore 风格射线-三角形/四边形求交。
	 * Möller–Trumbore-style ray–triangle/quad intersection.
	 *
	 * @param v0 顶点 0 / vertex 0
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @param store 结果存储；null 仅测试 / result storage; null tests only
	 * @param doPlanar true 时 store 写 (t,w1,w2)，否则写世界坐标 / planar vs world coords
	 * @param quad true 时按四边形重心条件判定 / use quad barycentric condition when true
	 * whether they intersect
	 */
	private boolean intersects(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f store, boolean doPlanar, boolean quad) {
		float diffDotNorm;
		float dirDotEdge1xDiff;
		float sign;
		Vector3f edge2;
		Vector3f tempVa = Vector3f.newInstance();
		Vector3f tempVb = Vector3f.newInstance();
		Vector3f tempVc = Vector3f.newInstance();
		Vector3f tempVd = Vector3f.newInstance();
		Vector3f diff = this.origin.subtract(v0, tempVa);
		Vector3f edge1 = v1.subtract(v0, tempVb);
		Vector3f norm = edge1.cross(edge2 = v2.subtract(v0, tempVc), tempVd);
		float dirDotNorm = this.direction.dot(norm);
		if (dirDotNorm > 1.1920929E-7f) {
			sign = 1.0f;
		} else if (dirDotNorm < -1.1920929E-7f) {
			sign = -1.0f;
			dirDotNorm = -dirDotNorm;
		} else {
			return false;
		}
		float dirDotDiffxEdge2 = sign * this.direction.dot(diff.cross(edge2, edge2));
		if (dirDotDiffxEdge2 >= 0.0f && (dirDotEdge1xDiff = sign * this.direction.dot(edge1.crossLocal(diff))) >= 0.0f
				&& (!quad ? dirDotDiffxEdge2 + dirDotEdge1xDiff <= dirDotNorm : dirDotEdge1xDiff <= dirDotNorm)
				&& (diffDotNorm = -sign * diff.dot(norm)) >= 0.0f) {
			Vector3f.recycle(tempVa);
			Vector3f.recycle(tempVb);
			Vector3f.recycle(tempVc);
			Vector3f.recycle(tempVd);
			if (store == null) {
				return true;
			}
			float inv = 1.0f / dirDotNorm;
			float t = diffDotNorm * inv;
			if (!doPlanar) {
				store.set(this.origin).addLocal(this.direction.x * t, this.direction.y * t, this.direction.z * t);
			} else {
				float w1 = dirDotDiffxEdge2 * inv;
				float w2 = dirDotEdge1xDiff * inv;
				store.set(t, w1, w2);
			}
			return true;
		}
		Vector3f.recycle(tempVa);
		Vector3f.recycle(tempVb);
		Vector3f.recycle(tempVc);
		Vector3f.recycle(tempVd);
		return false;
	}

	/**
	 * 射线与三点三角形求交，返回参数 t；未命中返回正无穷。
	 * Intersects this ray with a three-point triangle; returns parameter t, or +∞ on miss.
	 *
	 * @param v0 顶点 0 / vertex 0
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @return 交点参数 t，或 {@link Float#POSITIVE_INFINITY} / hit parameter t, or +∞
	 */
	public float intersects(Vector3f v0, Vector3f v1, Vector3f v2) {
		float diffDotNorm;
		float dirDotEdge1xDiff;
		float sign;
		float edge1X = v1.x - v0.x;
		float edge1Y = v1.y - v0.y;
		float edge1Z = v1.z - v0.z;
		float edge2X = v2.x - v0.x;
		float edge2Y = v2.y - v0.y;
		float edge2Z = v2.z - v0.z;
		float normX = edge1Y * edge2Z - edge1Z * edge2Y;
		float normY = edge1Z * edge2X - edge1X * edge2Z;
		float normZ = edge1X * edge2Y - edge1Y * edge2X;
		float dirDotNorm = this.direction.x * normX + this.direction.y * normY + this.direction.z * normZ;
		float diffX = this.origin.x - v0.x;
		float diffY = this.origin.y - v0.y;
		float diffZ = this.origin.z - v0.z;
		if (dirDotNorm > 1.1920929E-7f) {
			sign = 1.0f;
		} else if (dirDotNorm < -1.1920929E-7f) {
			sign = -1.0f;
			dirDotNorm = -dirDotNorm;
		} else {
			return Float.POSITIVE_INFINITY;
		}
		float diffEdge2X = diffY * edge2Z - diffZ * edge2Y;
		float diffEdge2Y = diffZ * edge2X - diffX * edge2Z;
		float diffEdge2Z = diffX * edge2Y - diffY * edge2X;
		float dirDotDiffxEdge2 = sign
				* (this.direction.x * diffEdge2X + this.direction.y * diffEdge2Y + this.direction.z * diffEdge2Z);
		if (dirDotDiffxEdge2 >= 0.0f
				&& (dirDotEdge1xDiff = sign * (this.direction.x * (diffEdge2X = edge1Y * diffZ - edge1Z * diffY)
						+ this.direction.y * (diffEdge2Y = edge1Z * diffX - edge1X * diffZ)
						+ this.direction.z * (diffEdge2Z = edge1X * diffY - edge1Y * diffX))) >= 0.0f
				&& dirDotDiffxEdge2 + dirDotEdge1xDiff <= dirDotNorm
				&& (diffDotNorm = -sign * (diffX * normX + diffY * normY + diffZ * normZ)) >= 0.0f) {
			float inv = 1.0f / dirDotNorm;
			float t = diffDotNorm * inv;
			return t;
		}
		return Float.POSITIVE_INFINITY;
	}

	/**
	 * 射线与由三点定义的四边形求交，loc 写入平面坐标 (t, w1, w2)。
	 * Intersects this ray with a quad defined by three points; stores planar coords into loc.
	 *
	 * @param v0 顶点 0 / vertex 0
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @param loc 平面坐标存储 / planar-coord storage
	 * whether they intersect
	 */
	public boolean intersectWherePlanarQuad(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f loc) {
		return this.intersects(v0, v1, v2, loc, true, true);
	}

	/**
	 * 射线与平面求交（仅正向半射线，ratio ≥ epsilon）。
	 * Intersects this ray with a plane (forward half-ray only, ratio ≥ epsilon).
	 *
	 * @param p 平面 / plane
	 * @param loc 交点存储 / hit-point storage
	 * whether they intersect
	 */
	public boolean intersectsWherePlane(Plane p, Vector3f loc) {
		float denominator = p.getNormal().dot(this.direction);
		if (denominator > -1.1920929E-7f && denominator < 1.1920929E-7f) {
			return false;
		}
		float numerator = -(p.getNormal().dot(this.origin) - p.getConstant());
		float ratio = numerator / denominator;
		if (ratio < 1.1920929E-7f) {
			return false;
		}
		loc.set(this.direction).multLocal(ratio).addLocal(this.origin);
		return true;
	}

	/**
	 * 与另一可碰撞体做碰撞检测（包围体转发；三角形求交写结果）。
	 * Collides with another collidable (BV dispatch; triangle hits are recorded).
	 *
	 * @param other 另一可碰撞体 / other collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * hit count
	 * unsupported collision type。 / unsupported collision type.
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if (other instanceof BoundingVolume) {
			BoundingVolume bv = (BoundingVolume) other;
			return bv.collideWith(this, results);
		}
		if (other instanceof AbstractTriangle) {
			AbstractTriangle tri = (AbstractTriangle) other;
			float d = this.intersects(tri.get1(), tri.get2(), tri.get3());
			if (Float.isInfinite(d) || Float.isNaN(d)) {
				return 0;
			}
			Vector3f point = new Vector3f(this.direction).multLocal(d).addLocal(this.origin);
			results.addCollision(new CollisionResult(point, d));
			return 1;
		}
		throw new UnsupportedCollisionException();
	}

	/**
	 * 点到本射线（半射线，参数 ≥ 0）的最短距离平方。
	 * Squared distance from a point to this half-ray (parameter ≥ 0).
	 *
	 * query point
	 * squared distance
	 */
	public float distanceSquared(Vector3f point) {
		Vector3f tempVa = Vector3f.newInstance();
		Vector3f tempVb = Vector3f.newInstance();
		point.subtract(this.origin, tempVa);
		float rayParam = this.direction.dot(tempVa);
		if (rayParam > 0.0f) {
			this.origin.add(this.direction.mult(rayParam, tempVb), tempVb);
		} else {
			tempVb.set(this.origin);
			rayParam = 0.0f;
		}
		tempVb.subtract(point, tempVa);
		float len = tempVa.lengthSquared();
		Vector3f.recycle(tempVa);
		Vector3f.recycle(tempVb);
		return len;
	}

	/**
	 * 返回射线原点（内部引用）。
	 * Returns the ray origin (internal reference).
	 *
	 * origin
	 */
	public Vector3f getOrigin() {
		return this.origin;
	}

	/**
	 * 拷贝设置射线原点。
	 * Copies and sets the ray origin.
	 *
	 * new origin
	 */
	public void setOrigin(Vector3f origin) {
		this.origin.set(origin);
	}

	/**
	 * 返回射线最大长度限制。
	 * Returns the maximum ray length limit.
	 *
	 * length limit
	 */
	public float getLimit() {
		return this.limit;
	}

	/**
	 * 设置射线最大长度限制。
	 * Sets the maximum ray length limit.
	 *
	 * @param limit 长度限制 / length limit
	 */
	public void setLimit(float limit) {
		this.limit = limit;
	}

	/**
	 * 返回射线方向（内部引用）。
	 * Returns the ray direction (internal reference).
	 *
	 * direction
	 */
	public Vector3f getDirection() {
		return this.direction;
	}

	/**
	 * 拷贝设置射线方向。
	 * Copies and sets the ray direction.
	 *
	 * new direction
	 */
	public void setDirection(Vector3f direction) {
		this.direction.set(direction);
	}

	/**
	 * 从另一射线拷贝原点与方向。
	 * Copies origin and direction from another ray.
	 *
	 * source ray
	 */
	public void set(Ray source) {
		this.origin.set(source.getOrigin());
		this.direction.set(source.getDirection());
	}

	/**
	 * 返回可读字符串表示。
	 * Returns a human-readable string representation.
	 *
	 * @return 描述字符串 / description string
	 */
	public String toString() {
		return this.getClass().getSimpleName() + " [Origin: " + this.origin + ", Direction: " + this.direction + "]";
	}

	/**
	 * 返回运行时类标签。
	 * Returns the runtime class tag.
	 *
	 * class object
	 */
	public Class<? extends Ray> getClassTag() {
		return this.getClass();
	}

	/**
	 * 深拷贝本射线（原点与方向独立克隆）。
	 * Deep-clones this ray (origin and direction cloned independently).
	 *
	 * clone
	 */
	public Ray clone() {
		try {
			Ray r = (Ray) super.clone();
			r.direction = this.direction.clone();
			r.origin = this.origin.clone();
			return r;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
