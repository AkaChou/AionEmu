package com.aionemu.gameserver.geoEngine.math;

/**
 * 三角形：三个顶点，支持懒计算中心/法线，并实现对象池复用。
 * Triangle with three vertices, lazy center/normal, and object-pool reuse.
 */
public class Triangle extends AbstractTriangle implements Reusable {

	/** 对象工厂。 / Object factory. */
	private static final ObjectFactory<Object> FACTORY = new ObjectFactory<Object>() {

		public Object create() {
			return new Triangle();
		}
	};
	/** 顶点 A / Vertex A */
	private Vector3f pointa = new Vector3f();
	/** 顶点 B / Vertex B */
	private Vector3f pointb = new Vector3f();
	/** 顶点 C / Vertex C */
	private Vector3f pointc = new Vector3f();
	/** 懒计算的中心（质心）。 / Lazily computed center (centroid). */
	private transient Vector3f center;
	/** 懒计算的单位法线。 / Lazily computed unit normal. */
	private transient Vector3f normal;
	/** 投影值（外部用途）。 / Projection value (external use). */
	private float projection;
	/** 索引（外部用途）。 / Index (external use). */
	private int index;

	/**
	 * 构造空三角形（顶点为零）。
	 * Constructs an empty triangle (zero vertices).
	 */
	public Triangle() {
	}

	/**
	 * 按三个顶点构造三角形（拷贝分量）。
	 * Constructs a triangle from three vertices (copies components).
	 *
	 * @param p1 顶点 1 / vertex 1
	 * @param p2 顶点 2 / vertex 2
	 * @param p3 顶点 3 / vertex 3
	 */
	public Triangle(Vector3f p1, Vector3f p2, Vector3f p3) {
		this.pointa.set(p1);
		this.pointb.set(p2);
		this.pointc.set(p3);
	}

	/**
	 * 按索引返回顶点（0=A, 1=B, 2=C；其它返回 null）。
	 * Returns a vertex by index (0=A, 1=B, 2=C; null otherwise).
	 *
	 * @param i 顶点索引 / vertex index
	 * vertex, or null
	 */
	public Vector3f get(int i) {
		switch (i) {
		case 0: {
			return this.pointa;
		}
		case 1: {
			return this.pointb;
		}
		case 2: {
			return this.pointc;
		}
		}
		return null;
	}

	/**
	 * 返回第一个顶点。
	 * Returns the first vertex.
	 *
	 * vertex A
	 */
	@Override
	public Vector3f get1() {
		return this.pointa;
	}

	/**
	 * 返回第二个顶点。
	 * Returns the second vertex.
	 *
	 * vertex B
	 */
	@Override
	public Vector3f get2() {
		return this.pointb;
	}

	/**
	 * 返回第三个顶点。
	 * Returns the third vertex.
	 *
	 * vertex C
	 */
	@Override
	public Vector3f get3() {
		return this.pointc;
	}

	/**
	 * 按索引拷贝设置顶点（0=A, 1=B, 2=C）。
	 * Copies and sets a vertex by index (0=A, 1=B, 2=C).
	 *
	 * @param i 顶点索引 / vertex index
	 * new vertex
	 */
	public void set(int i, Vector3f point) {
		switch (i) {
		case 0: {
			this.pointa.set(point);
			break;
		}
		case 1: {
			this.pointb.set(point);
			break;
		}
		case 2: {
			this.pointc.set(point);
		}
		}
	}

	/**
	 * 按索引与分量设置顶点。
	 * Sets a vertex by index and components.
	 *
	 * @param i 顶点索引 / vertex index
	 * @param x X 分量 / X component
	 * @param y Y 分量 / Y component
	 * @param z Z 分量 / Z component
	 */
	public void set(int i, float x, float y, float z) {
		switch (i) {
		case 0: {
			this.pointa.set(x, y, z);
			break;
		}
		case 1: {
			this.pointb.set(x, y, z);
			break;
		}
		case 2: {
			this.pointc.set(x, y, z);
		}
		}
	}

	/**
	 * 拷贝设置第一个顶点。
	 * Copies and sets the first vertex.
	 *
	 * @param v 新顶点 / new vertex
	 */
	public void set1(Vector3f v) {
		this.pointa.set(v);
	}

	/**
	 * 拷贝设置第二个顶点。
	 * Copies and sets the second vertex.
	 *
	 * @param v 新顶点 / new vertex
	 */
	public void set2(Vector3f v) {
		this.pointb.set(v);
	}

	/**
	 * 拷贝设置第三个顶点。
	 * Copies and sets the third vertex.
	 *
	 * @param v 新顶点 / new vertex
	 */
	public void set3(Vector3f v) {
		this.pointc.set(v);
	}

	/**
	 * 拷贝设置三个顶点。
	 * Copies and sets all three vertices.
	 *
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @param v3 顶点 3 / vertex 3
	 */
	@Override
	public void set(Vector3f v1, Vector3f v2, Vector3f v3) {
		this.pointa.set(v1);
		this.pointb.set(v2);
		this.pointc.set(v3);
	}

	/**
	 * 根据当前顶点重算质心（懒分配 center）。
	 * Recomputes the centroid from current vertices (lazily allocates center).
	 */
	public void calculateCenter() {
		if (this.center == null) {
			this.center = new Vector3f(this.pointa);
		} else {
			this.center.set(this.pointa);
		}
		this.center.addLocal(this.pointb).addLocal(this.pointc).multLocal(0.33333334f);
	}

	/**
	 * 根据当前顶点重算单位法线（懒分配 normal）。
	 * Recomputes the unit normal from current vertices (lazily allocates normal).
	 */
	public void calculateNormal() {
		if (this.normal == null) {
			this.normal = new Vector3f(this.pointb);
		} else {
			this.normal.set(this.pointb);
		}
		this.normal.subtractLocal(this.pointa).crossLocal(this.pointc.x - this.pointa.x, this.pointc.y - this.pointa.y,
				this.pointc.z - this.pointa.z);
		this.normal.normalizeLocal();
	}

	/**
	 * 返回质心；若尚未计算则先计算。
	 * Returns the centroid; computes it first if absent.
	 *
	 * centroid
	 */
	public Vector3f getCenter() {
		if (this.center == null) {
			this.calculateCenter();
		}
		return this.center;
	}

	/**
	 * 直接设置质心引用（不从顶点推导）。
	 * Sets the centroid reference directly (not derived from vertices).
	 *
	 * centroid
	 */
	public void setCenter(Vector3f center) {
		this.center = center;
	}

	/**
	 * 返回单位法线；若尚未计算则先计算。
	 * Returns the unit normal; computes it first if absent.
	 *
	 * unit normal
	 */
	public Vector3f getNormal() {
		if (this.normal == null) {
			this.calculateNormal();
		}
		return this.normal;
	}

	/**
	 * 直接设置法线引用（不从顶点推导）。
	 * Sets the normal reference directly (not derived from vertices).
	 *
	 * normal
	 */
	public void setNormal(Vector3f normal) {
		this.normal = normal;
	}

	/**
	 * 返回投影值。
	 * Returns the projection value.
	 *
	 * projection
	 */
	public float getProjection() {
		return this.projection;
	}

	/**
	 * 设置投影值。
	 * Sets the projection value.
	 *
	 * projection
	 */
	public void setProjection(float projection) {
		this.projection = projection;
	}

	/**
	 * 返回索引。
	 * Returns the index.
	 *
	 * index
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * 设置索引。
	 * Sets the index.
	 *
	 * index
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * 由三点计算单位法线，结果写入 store。
	 * Computes a unit normal from three points into store.
	 *
	 * @param v1 点 1 / point 1
	 * @param v2 点 2 / point 2
	 * @param v3 点 3 / point 3
	 * @param store 结果存储（null 则新建） / result storage (allocated if null)
	 * @return 归一化后的 store / normalized store
	 */
	public static Vector3f computeTriangleNormal(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f store) {
		if (store == null) {
			store = new Vector3f(v2);
		} else {
			store.set(v2);
		}
		store.subtractLocal(v1).crossLocal(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
		return store.normalizeLocal();
	}

	/**
	 * 返回运行时类标签。
	 * Returns the runtime class tag.
	 *
	 * class object
	 */
	public Class<? extends Triangle> getClassTag() {
		return this.getClass();
	}

	/**
	 * 深拷贝本三角形（三顶点独立克隆；center/normal 不克隆）。
	 * Deep-clones this triangle (vertices cloned; center/normal not cloned).
	 *
	 * clone
	 */
	public Triangle clone() {
		try {
			Triangle t = (Triangle) super.clone();
			t.pointa = this.pointa.clone();
			t.pointb = this.pointb.clone();
			t.pointc = this.pointc.clone();
			return t;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * 重置到可复用初始状态（顶点清零，center/normal 置 null）。
	 * Resets to a reusable initial state (vertices zeroed; center/normal nulled).
	 */
	public void reset() {
		this.pointa.reset();
		this.pointb.reset();
		this.pointc.reset();
		this.center = null;
		this.normal = null;
		this.projection = 0.0f;
		this.index = 0;
	}

	/**
	 * 从工厂获取实例。
	 * Obtains an instance from the factory.
	 *
	 * pooled instance
	 */
	public static Triangle newInstance() {
		return (Triangle) FACTORY.object();
	}

	/**
	 * 将实例回收到工厂。
	 * Recycles the instance into the factory.
	 *
	 * @param instance 待回收实例 / instance to recycle
	 */
	public static void recycle(Triangle instance) {
		FACTORY.recycle((Object) instance);
	}
}
