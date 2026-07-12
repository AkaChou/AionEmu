package com.aionemu.gameserver.geoEngine.math;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 空间平面，法线加常数形式（N·X = constant）。
 * Spatial plane in normal-plus-constant form (N·X = constant).
 */
@Slf4j
public class Plane implements Cloneable {

	/** 平面法线。 / Plane normal. */
	protected Vector3f normal;
	/** Plane constant term (N·X = constant) / Plane constant term (N·X = constant) */
	protected float constant;

	/**
	 * 构造零法线平面。
	 * Constructs a plane with a zero normal.
	 */
	public Plane() {
		this.normal = new Vector3f();
	}

	/**
	 * 按法线与常数构造平面。
	 * Constructs a plane from normal and constant.
	 *
	 * @param normal 平面法线（null 时警告并替换为零向量） / plane normal (null warns and becomes zero)
	 * plane constant
	 */
	public Plane(Vector3f normal, float constant) {
		if (normal == null) {
			log.warn(I18n.get("log.24ba7c4ed2c5"));
			normal = new Vector3f();
		}
		this.normal = normal;
		this.constant = constant;
	}

	/**
	 * 设置平面法线（拷贝分量；null 时警告并置为零向量）。
	 * Sets the plane normal (copies components; null warns and becomes zero).
	 *
	 * new normal
	 */
	public void setNormal(Vector3f normal) {
		if (normal == null) {
			log.warn(I18n.get("log.24ba7c4ed2c5"));
			normal = new Vector3f();
		}
		this.normal.set(normal);
	}

	/**
	 * 按分量设置平面法线。
	 * Sets the plane normal by components.
	 *
	 * @param x X 分量 / X component
	 * @param y Y 分量 / Y component
	 * @param z Z 分量 / Z component
	 */
	public void setNormal(float x, float y, float z) {
		if (this.normal == null) {
			log.warn(I18n.get("log.24ba7c4ed2c5"));
			this.normal = new Vector3f();
		}
		this.normal.set(x, y, z);
	}

	/**
	 * 返回平面法线（内部引用）。
	 * Returns the plane normal (internal reference).
	 *
	 * normal
	 */
	public Vector3f getNormal() {
		return this.normal;
	}

	/**
	 * 设置平面常数项。
	 * Sets the plane constant term.
	 *
	 * constant
	 */
	public void setConstant(float constant) {
		this.constant = constant;
	}

	/**
	 * 返回平面常数项。
	 * Returns the plane constant term.
	 *
	 * constant
	 */
	public float getConstant() {
		return this.constant;
	}

	/**
	 * 计算点到平面的最近点，结果写入 store。
	 * Computes the closest point on the plane to the given point into store.
	 *
	 * query point
	 * @param store 结果存储 / result storage
	 * store itself
	 */
	public Vector3f getClosestPoint(Vector3f point, Vector3f store) {
		float t = (this.constant - this.normal.dot(point)) / this.normal.dot(this.normal);
		return store.set(this.normal).multLocal(t).addLocal(point);
	}

	/**
	 * 计算点到平面的最近点（分配新向量）。
	 * Computes the closest point on the plane (allocates a new vector).
	 *
	 * query point
	 * closest point
	 */
	public Vector3f getClosestPoint(Vector3f point) {
		return this.getClosestPoint(point, new Vector3f());
	}

	/**
	 * 将点关于本平面做镜像反射。
	 * Reflects a point across this plane.
	 *
	 * @param point 待反射点 / point to reflect
	 * @param store 结果存储（null 则新建） / result storage (allocated if null)
	 * @return 反射后的点 / reflected point
	 */
	public Vector3f reflect(Vector3f point, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		float d = this.pseudoDistance(point);
		store.set(this.normal).negateLocal().multLocal(d * 2.0f);
		store.addLocal(point);
		return store;
	}

	/**
	 * 计算点到平面的伪距离（有符号，N·P − constant）。
	 * Computes signed pseudo-distance from point to plane (N·P − constant).
	 *
	 * query point
	 *
	 * @param point
	 * @return 有符号伪距离 / signed pseudo-distance
	 */
	public float pseudoDistance(Vector3f point) {
		return this.normal.dot(point) - this.constant;
	}

	/**
	 * 判断点相对平面的侧别。
	 * Determines which side of the plane the point lies on.
	 *
	 * query point
	 * side
	 */
	public Side whichSide(Vector3f point) {
		float dis = this.pseudoDistance(point);
		if (dis < 0.0f) {
			return Side.Negative;
		}
		if (dis > 0.0f) {
			return Side.Positive;
		}
		return Side.None;
	}

	/**
	 * 判断点是否近似落在平面上（epsilon ≈ Float.MIN_NORMAL）。
	 * Tests whether the point lies approximately on the plane (epsilon ≈ Float.MIN_NORMAL).
	 *
	 * query point
	 *
	 * @param point
	 * @return 是否在平面上 / whether on plane
	 */
	public boolean isOnPlane(Vector3f point) {
		float dist = this.pseudoDistance(point);
		return dist < 1.1920929E-7f && dist > -1.1920929E-7f;
	}

	/**
	 * 由三角形三个顶点定义本平面。
	 * Defines this plane from a triangle's three vertices.
	 *
	 * @param t 三角形 / triangle
	 */
	public void setPlanePoints(AbstractTriangle t) {
		this.setPlanePoints(t.get1(), t.get2(), t.get3());
	}

	/**
	 * 由原点与法线定义本平面（constant = N·origin）。
	 * Defines this plane from an origin point and normal (constant = N·origin).
	 *
	 * @param origin 平面上一点 / a point on the plane
	 * normal
	 */
	public void setOriginNormal(Vector3f origin, Vector3f normal) {
		this.normal.set(normal);
		this.constant = normal.x * origin.x + normal.y * origin.y + normal.z * origin.z;
	}

	/**
	 * 由三点定义本平面（法线归一化，constant = N·v1）。
	 * Defines this plane from three points (normal normalized, constant = N·v1).
	 *
	 * @param v1 点 1 / point 1
	 * @param v2 点 2 / point 2
	 * @param v3 点 3 / point 3
	 */
	public void setPlanePoints(Vector3f v1, Vector3f v2, Vector3f v3) {
		this.normal.set(v2).subtractLocal(v1);
		this.normal.crossLocal(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).normalizeLocal();
		this.constant = this.normal.dot(v1);
	}

	/**
	 * 返回可读字符串表示。
	 * Returns a human-readable string representation.
	 *
	 * @return 描述字符串 / description string
	 */
	public String toString() {
		return this.getClass().getSimpleName() + " [Normal: " + this.normal + " - Constant: " + this.constant + "]";
	}

	/**
	 * 返回运行时类标签。
	 * Returns the runtime class tag.
	 *
	 * class object
	 */
	public Class<? extends Plane> getClassTag() {
		return this.getClass();
	}

	/**
	 * 深拷贝本平面（法线独立克隆）。
	 * Deep-clones this plane (normal cloned independently).
	 *
	 * clone
	 */
	public Plane clone() {
		try {
			Plane p = (Plane) super.clone();
			p.normal = this.normal.clone();
			return p;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * 点相对平面的侧别。
	 * Side of a point relative to the plane.
	 */
	public static enum Side {
		/** 落在平面上。 / Lies on the plane. */
		None,
		/** Positive normal side (pseudo-distance &gt; 0) / Positive normal side (pseudo-distance &gt; 0) */
		Positive,
		/** Negative normal side (pseudo-distance &lt; 0) / Negative normal side (pseudo-distance &lt; 0) */
		Negative;

	}
}
