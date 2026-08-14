package com.aionemu.gameserver.geoEngine.math;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 三维向量，提供几何运算（jME 风格流式 API，支持对象池复用）。
 * Three-dimensional vector with geometric operations (jME-style fluent API, object-pool reuse).
 */
@Slf4j
public final class Vector3f implements Cloneable, Reusable {

	/** 对象池工厂。 / Object-pool factory. */
	private static final ObjectFactory<Object> FACTORY = new ObjectFactory<Object>() {

		public Object create() {
			return new Vector3f();
		}
	};

	/** 零向量 (0, 0, 0)。 / Zero vector (0, 0, 0). */
	public static final Vector3f ZERO = new Vector3f(0.0f, 0.0f, 0.0f);

	/** NaN vector / NaN vector */
	public static final Vector3f NAN = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

	/** Unit X vector (1, 0, 0) / Unit X vector (1, 0, 0) */
	public static final Vector3f UNIT_X = new Vector3f(1.0f, 0.0f, 0.0f);

	/** Unit Y vector (0, 1, 0) / Unit Y vector (0, 1, 0) */
	public static final Vector3f UNIT_Y = new Vector3f(0.0f, 1.0f, 0.0f);

	/** Unit Z vector (0, 0, 1) / Unit Z vector (0, 0, 1) */
	public static final Vector3f UNIT_Z = new Vector3f(0.0f, 0.0f, 1.0f);

	/** 全 1 向量 (1, 1, 1)。 / All-ones vector (1, 1, 1). */
	public static final Vector3f UNIT_XYZ = new Vector3f(1.0f, 1.0f, 1.0f);

	/** 正无穷向量。 / Positive-infinity vector. */
	public static final Vector3f POSITIVE_INFINITY = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);

	/** 负无穷向量。 / Negative-infinity vector. */
	public static final Vector3f NEGATIVE_INFINITY = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
			Float.NEGATIVE_INFINITY);

	/** X 分量 / X component */
	public float x;

	/** Y 分量 / Y component */
	public float y;

	/** Z component / Z component */
	public float z;

	/**
	 * 构造零向量。
	 * Constructs a zero vector.
	 */
	public Vector3f() {
		this.z = 0.0f;
		this.y = 0.0f;
		this.x = 0.0f;
	}

	/**
	 * 用指定分量构造向量。
	 * Constructs a vector with the given components.
	 *
	 * @param x X 分量 / X component
	 * @param y Y 分量 / Y component
	 * @param z Z 分量 / Z component
	 */
	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * 拷贝构造。
	 * Copy constructor.
	 *
	 * @param copy 源向量 / Source vector
	 */
	public Vector3f(Vector3f copy) {
		this.set(copy);
	}

	/**
	 * 设置分量。
	 * Sets the components.
	 *
	 * @param x X 分量 / X component
	 * @param y Y 分量 / Y component
	 * @param z Z 分量 / Z component
	 * @return 本向量 / This vector
	 */
	public Vector3f set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * 从另一向量拷贝分量。
	 * Copies components from another vector.
	 *
	 * @param vect 源向量 / Source vector
	 * @return 本向量 / This vector
	 */
	public Vector3f set(Vector3f vect) {
		this.x = vect.x;
		this.y = vect.y;
		this.z = vect.z;
		return this;
	}

	/**
	 * 向量加法，返回新向量。
	 * Adds another vector and returns a new vector.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 结果向量；{@code vec} 为 null 时返回 null / Result vector; null if {@code vec} is null
	 */
	public Vector3f add(Vector3f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		return new Vector3f(this.x + vec.x, this.y + vec.y, this.z + vec.z);
	}

	/**
	 * 向量加法，结果写入指定存储向量。
	 * Adds another vector and stores the result.
	 *
	 * @param vec 另一向量 / Other vector
	 * @param result 结果存储 / Result store
	 * @return 结果向量 / Result vector
	 */
	public Vector3f add(Vector3f vec, Vector3f result) {
		result.x = this.x + vec.x;
		result.y = this.y + vec.y;
		result.z = this.z + vec.z;
		return result;
	}

	/**
	 * 就地向量加法。
	 * Adds another vector in place.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 本向量；{@code vec} 为 null 时返回 null / This vector; null if {@code vec} is null
	 */
	public Vector3f addLocal(Vector3f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		this.x += vec.x;
		this.y += vec.y;
		this.z += vec.z;
		return this;
	}

	/**
	 * 加上给定分量，返回新向量。
	 * Adds the given components and returns a new vector.
	 *
	 * @param addX X 增量 / X increment
	 * @param addY Y 增量 / Y increment
	 * @param addZ Z 增量 / Z increment
	 * @return 结果向量 / Result vector
	 */
	public Vector3f add(float addX, float addY, float addZ) {
		return new Vector3f(this.x + addX, this.y + addY, this.z + addZ);
	}

	/**
	 * 就地加上给定分量。
	 * Adds the given components in place.
	 *
	 * @param addX X 增量 / X increment
	 * @param addY Y 增量 / Y increment
	 * @param addZ Z 增量 / Z increment
	 * @return 本向量 / This vector
	 */
	public Vector3f addLocal(float addX, float addY, float addZ) {
		this.x += addX;
		this.y += addY;
		this.z += addZ;
		return this;
	}

	/**
	 * 就地执行 {@code this = this * scalar + add}。
	 * Performs {@code this = this * scalar + add} in place.
	 *
	 * @return 标量 / Scalar
	 * @param add 加数向量 / Addend vector
	 * @param scalar 本向量 / This vector
	 */
	public Vector3f scaleAdd(float scalar, Vector3f add) {
		this.x = this.x * scalar + add.x;
		this.y = this.y * scalar + add.y;
		this.z = this.z * scalar + add.z;
		return this;
	}

	/**
	 * 就地执行 {@code this = mult * scalar + add}。
	 * Performs {@code this = mult * scalar + add} in place.
	 *
	 * @return 标量 / Scalar
	 * @param mult 被乘向量 / Multiplicand vector
	 * @param add 加数向量 / Addend vector
	 * @param scalar 本向量 / This vector
	 */
	public Vector3f scaleAdd(float scalar, Vector3f mult, Vector3f add) {
		this.x = mult.x * scalar + add.x;
		this.y = mult.y * scalar + add.y;
		this.z = mult.z * scalar + add.z;
		return this;
	}

	/**
	 * 点积。
	 * Computes the dot product.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 点积；{@code vec} 为 null 时返回 0 / Dot product; 0 if {@code vec} is null
	 */
	public float dot(Vector3f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bf0985989b35"));
			return 0.0f;
		}
		return this.x * vec.x + this.y * vec.y + this.z * vec.z;
	}

	/**
	 * 叉积，返回新向量。
	 * Cross product returning a new vector.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 叉积结果 / Cross-product result
	 */
	public Vector3f cross(Vector3f v) {
		return this.cross(v, null);
	}

	/**
	 * 叉积，结果写入指定存储向量。
	 * Cross product stored into the given result.
	 *
	 * @param v 另一向量 / Other vector
	 * @param result 结果存储；为 null 时新建 / Result store; created if null
	 * @return 结果向量 / Result vector
	 */
	public Vector3f cross(Vector3f v, Vector3f result) {
		return this.cross(v.x, v.y, v.z, result);
	}

	/**
	 * 与给定分量叉积，结果写入指定存储向量。
	 * Cross product with given components, stored into the result.
	 *
	 * @param otherX 另一 X / Other X
	 * @param otherY 另一 Y / Other Y
	 * @param otherZ 另一 Z / Other Z
	 * @param result 结果存储；为 null 时新建 / Result store; created if null
	 * @return 结果向量 / Result vector
	 */
	public Vector3f cross(float otherX, float otherY, float otherZ, Vector3f result) {
		if (result == null) {
			result = new Vector3f();
		}
		float resX = this.y * otherZ - this.z * otherY;
		float resY = this.z * otherX - this.x * otherZ;
		float resZ = this.x * otherY - this.y * otherX;
		result.set(resX, resY, resZ);
		return result;
	}

	/**
	 * 就地叉积。
	 * Cross product in place.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 本向量 / This vector
	 */
	public Vector3f crossLocal(Vector3f v) {
		return this.crossLocal(v.x, v.y, v.z);
	}

	/**
	 * 与给定分量就地叉积。
	 * Cross product with given components in place.
	 *
	 * @param otherX 另一 X / Other X
	 * @param otherY 另一 Y / Other Y
	 * @param otherZ 另一 Z / Other Z
	 * @return 本向量 / This vector
	 */
	public Vector3f crossLocal(float otherX, float otherY, float otherZ) {
		float tempx = this.y * otherZ - this.z * otherY;
		float tempy = this.z * otherX - this.x * otherZ;
		this.z = this.x * otherY - this.y * otherX;
		this.x = tempx;
		this.y = tempy;
		return this;
	}

	/**
	 * 投影到另一向量上，返回新向量。
	 * Projects onto another vector and returns a new vector.
	 *
	 * @param other 投影方向 / Projection direction
	 * @return 投影结果 / Projection result
	 */
	public Vector3f project(Vector3f other) {
		float n = this.dot(other);
		float d = other.lengthSquared();
		return new Vector3f(other).normalizeLocal().multLocal(n / d);
	}

	/**
	 * 向量长度（模）。
	 * Vector length (magnitude).
	 *
	 * @return 长度 / Length
	 */
	public float length() {
		return FastMath.sqrt(this.lengthSquared());
	}

	/**
	 * 长度平方。
	 * Squared length.
	 *
	 * @return 长度平方 / Squared length
	 */
	public float lengthSquared() {
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}

	/**
	 * 到另一向量的距离平方。
	 * Squared distance to another vector.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 距离平方 / Squared distance
	 */
	public float distanceSquared(Vector3f v) {
		double dx = this.x - v.x;
		double dy = this.y - v.y;
		double dz = this.z - v.z;
		return (float) (dx * dx + dy * dy + dz * dz);
	}

	/**
	 * 到另一向量的欧氏距离。
	 * Euclidean distance to another vector.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 距离 / Distance
	 */
	public float distance(Vector3f v) {
		return FastMath.sqrt(this.distanceSquared(v));
	}

	/**
	 * 标量乘法，返回新向量。
	 * Multiplies by a scalar and returns a new vector.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 结果向量 / Result vector
	 */
	public Vector3f mult(float scalar) {
		return new Vector3f(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	/**
	 * 标量乘法，结果写入指定存储向量。
	 * Multiplies by a scalar and stores the result.
	 *
	 * @return 标量 / Scalar
	 * @param product 结果存储；为 null 时新建 / Result store; created if null
	 * @param scalar 结果向量 / Result vector
	 */
	public Vector3f mult(float scalar, Vector3f product) {
		if (null == product) {
			product = new Vector3f();
		}
		product.x = this.x * scalar;
		product.y = this.y * scalar;
		product.z = this.z * scalar;
		return product;
	}

	/**
	 * 就地标量乘法。
	 * Multiplies by a scalar in place.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 本向量 / This vector
	 */
	public Vector3f multLocal(float scalar) {
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
		return this;
	}

	/**
	 * 就地分量乘法。
	 * Multiplies component-wise in place.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 本向量；{@code vec} 为 null 时返回 null / This vector; null if {@code vec} is null
	 */
	public Vector3f multLocal(Vector3f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		this.x *= vec.x;
		this.y *= vec.y;
		this.z *= vec.z;
		return this;
	}

	/**
	 * 就地与给定分量相乘。
	 * Multiplies by the given components in place.
	 *
	 * @param x X 乘数 / X multiplier
	 * @param y Y 乘数 / Y multiplier
	 * @param z Z 乘数 / Z multiplier
	 * @return 本向量 / This vector
	 */
	public Vector3f multLocal(float x, float y, float z) {
		this.x *= x;
		this.y *= y;
		this.z *= z;
		return this;
	}

	/**
	 * 分量乘法，返回新向量。
	 * Multiplies component-wise and returns a new vector.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 结果向量；{@code vec} 为 null 时返回 null / Result vector; null if {@code vec} is null
	 */
	public Vector3f mult(Vector3f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		return this.mult(vec, null);
	}

	/**
	 * 分量乘法，结果写入指定存储向量。
	 * Multiplies component-wise and stores the result.
	 *
	 * @param vec 另一向量 / Other vector
	 * @param store 结果存储；为 null 时新建 / Result store; created if null
	 * @return 结果向量；{@code vec} 为 null 时返回 null / Result vector; null if {@code vec} is null
	 */
	public Vector3f mult(Vector3f vec, Vector3f store) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		if (store == null) {
			store = new Vector3f();
		}
		return store.set(this.x * vec.x, this.y * vec.y, this.z * vec.z);
	}

	/**
	 * 标量除法，返回新向量。
	 * Divides by a scalar and returns a new vector.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 结果向量 / Result vector
	 */
	public Vector3f divide(float scalar) {
		scalar = 1.0f / scalar;
		return new Vector3f(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	/**
	 * 就地标量除法。
	 * Divides by a scalar in place.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 本向量 / This vector
	 */
	public Vector3f divideLocal(float scalar) {
		scalar = 1.0f / scalar;
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
		return this;
	}

	/**
	 * 分量除法，返回新向量。
	 * Divides component-wise and returns a new vector.
	 *
	 * @param scalar 除数向量 / Divisor vector
	 * @return 结果向量 / Result vector
	 */
	public Vector3f divide(Vector3f scalar) {
		return new Vector3f(this.x / scalar.x, this.y / scalar.y, this.z / scalar.z);
	}

	/**
	 * 就地分量除法。
	 * Divides component-wise in place.
	 *
	 * @param scalar 除数向量 / Divisor vector
	 * @return 本向量 / This vector
	 */
	public Vector3f divideLocal(Vector3f scalar) {
		this.x /= scalar.x;
		this.y /= scalar.y;
		this.z /= scalar.z;
		return this;
	}

	/**
	 * 取反，返回新向量。
	 * Negates and returns a new vector.
	 *
	 * @return 取反后的新向量 / Negated new vector
	 */
	public Vector3f negate() {
		return new Vector3f(-this.x, -this.y, -this.z);
	}

	/**
	 * 就地取反。
	 * Negates in place.
	 *
	 * @return 本向量 / This vector
	 */
	public Vector3f negateLocal() {
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;
		return this;
	}

	/**
	 * 向量减法，返回新向量。
	 * Subtracts another vector and returns a new vector.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 结果向量 / Result vector
	 */
	public Vector3f subtract(Vector3f vec) {
		return new Vector3f(this.x - vec.x, this.y - vec.y, this.z - vec.z);
	}

	/**
	 * 就地向量减法。
	 * Subtracts another vector in place.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 本向量；{@code vec} 为 null 时返回 null / This vector; null if {@code vec} is null
	 */
	public Vector3f subtractLocal(Vector3f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		this.x -= vec.x;
		this.y -= vec.y;
		this.z -= vec.z;
		return this;
	}

	/**
	 * 向量减法，结果写入指定存储向量。
	 * Subtracts another vector and stores the result.
	 *
	 * @param vec 另一向量 / Other vector
	 * @param result 结果存储；为 null 时新建 / Result store; created if null
	 * @return 结果向量 / Result vector
	 */
	public Vector3f subtract(Vector3f vec, Vector3f result) {
		if (result == null) {
			result = new Vector3f();
		}
		result.x = this.x - vec.x;
		result.y = this.y - vec.y;
		result.z = this.z - vec.z;
		return result;
	}

	/**
	 * 减去给定分量，返回新向量。
	 * Subtracts the given components and returns a new vector.
	 *
	 * @param subtractX 要减去的 X / X to subtract
	 * @param subtractY 要减去的 Y / Y to subtract
	 * @param subtractZ 要减去的 Z / Z to subtract
	 * @return 结果向量 / Result vector
	 */
	public Vector3f subtract(float subtractX, float subtractY, float subtractZ) {
		return new Vector3f(this.x - subtractX, this.y - subtractY, this.z - subtractZ);
	}

	/**
	 * 就地减去给定分量。
	 * Subtracts the given components in place.
	 *
	 * @param subtractX 要减去的 X / X to subtract
	 * @param subtractY 要减去的 Y / Y to subtract
	 * @param subtractZ 要减去的 Z / Z to subtract
	 * @return 本向量 / This vector
	 */
	public Vector3f subtractLocal(float subtractX, float subtractY, float subtractZ) {
		this.x -= subtractX;
		this.y -= subtractY;
		this.z -= subtractZ;
		return this;
	}

	/**
	 * 归一化，返回新单位向量。
	 * Returns a new normalized unit vector.
	 *
	 * @return 单位向量 / Unit vector
	 */
	public Vector3f normalize() {
		float length = this.x * this.x + this.y * this.y + this.z * this.z;
		if (length != 1.0f && length != 0.0f) {
			length = 1.0f / FastMath.sqrt(length);
			return new Vector3f(this.x * length, this.y * length, this.z * length);
		}
		return this.clone();
	}

	/**
	 * 就地归一化。
	 * Normalizes in place.
	 *
	 * @return 本向量 / This vector
	 */
	public Vector3f normalizeLocal() {
		float length = this.x * this.x + this.y * this.y + this.z * this.z;
		if (length != 1.0f && length != 0.0f) {
			length = 1.0f / FastMath.sqrt(length);
			this.x *= length;
			this.y *= length;
			this.z *= length;
		}
		return this;
	}

	/**
	 * 就地取各分量与另一向量的最大值。
	 * Sets each component to the max of this and the other vector.
	 *
	 * @param other 另一向量 / Other vector
	 */
	public void maxLocal(Vector3f other) {
		this.x = other.x > this.x ? other.x : this.x;
		this.y = other.y > this.y ? other.y : this.y;
		this.z = other.z > this.z ? other.z : this.z;
	}

	/**
	 * 就地取各分量与另一向量的最小值。
	 * Sets each component to the min of this and the other vector.
	 *
	 * @param other 另一向量 / Other vector
	 */
	public void minLocal(Vector3f other) {
		this.x = other.x < this.x ? other.x : this.x;
		this.y = other.y < this.y ? other.y : this.y;
		this.z = other.z < this.z ? other.z : this.z;
	}

	/**
	 * 将本向量置零。
	 * Sets this vector to zero.
	 *
	 * @return 本向量 / This vector
	 */
	public Vector3f zero() {
		this.z = 0.0f;
		this.y = 0.0f;
		this.x = 0.0f;
		return this;
	}

	/**
	 * 与另一向量的夹角（弧度，基于点积）。
	 * Angle to another vector in radians (via dot product).
	 *
	 * @param otherVector 另一向量 / Other vector
	 * @return 夹角（弧度） / Angle in radians
	 */
	public float angleBetween(Vector3f otherVector) {
		float dotProduct = this.dot(otherVector);
		float angle = FastMath.acos(dotProduct);
		return angle;
	}

	/**
	 * 向目标向量线性插值（就地）。
	 * Linearly interpolates toward a target vector in place.
	 *
	 * @param finalVec 目标向量 / Target vector
	 * @param changeAmnt 插值因子 [0,1] / Interpolation factor [0,1]
	 * @return 本向量 / This vector
	 */
	public Vector3f interpolate(Vector3f finalVec, float changeAmnt) {
		this.x = (1.0f - changeAmnt) * this.x + changeAmnt * finalVec.x;
		this.y = (1.0f - changeAmnt) * this.y + changeAmnt * finalVec.y;
		this.z = (1.0f - changeAmnt) * this.z + changeAmnt * finalVec.z;
		return this;
	}

	/**
	 * 在起止向量之间线性插值，结果写入本向量。
	 * Linearly interpolates between two vectors into this.
	 *
	 * @param beginVec 起始向量 / Start vector
	 * @param finalVec 结束向量 / End vector
	 * @param changeAmnt 插值因子 [0,1] / Interpolation factor [0,1]
	 * @return 本向量 / This vector
	 */
	public Vector3f interpolate(Vector3f beginVec, Vector3f finalVec, float changeAmnt) {
		this.x = (1.0f - changeAmnt) * beginVec.x + changeAmnt * finalVec.x;
		this.y = (1.0f - changeAmnt) * beginVec.y + changeAmnt * finalVec.y;
		this.z = (1.0f - changeAmnt) * beginVec.z + changeAmnt * finalVec.z;
		return this;
	}

	/**
	 * 判断向量是否有效（非 null、非 NaN、非无穷）。
	 * Checks whether a vector is valid (non-null, non-NaN, non-infinite).
	 *
	 * @param vector 待检查向量 / Vector to check
	 * @return 若 valid 则为 true / True if valid
	 */
	public static boolean isValidVector(Vector3f vector) {
		if (vector == null) {
			return false;
		}
		if (Float.isNaN(vector.x) || Float.isNaN(vector.y) || Float.isNaN(vector.z)) {
			return false;
		}
		return !Float.isInfinite(vector.x) && !Float.isInfinite(vector.y) && !Float.isInfinite(vector.z);
	}

	/**
	 * 生成以 w 为法向的正交规范基 {u, v, w}。
	 * Generates an orthonormal basis {u, v, w} with w as the normal.
	 *
	 * @param u 输出基向量 u / Output basis u
	 * @param v 输出基向量 v / Output basis v
	 * @param w 输入法向（将被归一化） / Input normal (normalized in place)
	 */
	public static void generateOrthonormalBasis(Vector3f u, Vector3f v, Vector3f w) {
		w.normalizeLocal();
		Vector3f.generateComplementBasis(u, v, w);
	}

	/**
	 * 给定单位向量 w，生成与之正交的补基 {u, v}。
	 * Given unit vector w, generates the complementary basis {u, v}.
	 *
	 * @param u 输出基向量 u / Output basis u
	 * @param v 输出基向量 v / Output basis v
	 * @param w 单位法向 / Unit normal
	 */
	public static void generateComplementBasis(Vector3f u, Vector3f v, Vector3f w) {
		if (FastMath.abs(w.x) >= FastMath.abs(w.y)) {
			float fInvLength = FastMath.invSqrt(w.x * w.x + w.z * w.z);
			u.x = -w.z * fInvLength;
			u.y = 0.0f;
			u.z = w.x * fInvLength;
			v.x = w.y * u.z;
			v.y = w.z * u.x - w.x * u.z;
			v.z = -w.y * u.x;
		} else {
			float fInvLength = FastMath.invSqrt(w.y * w.y + w.z * w.z);
			u.x = 0.0f;
			u.y = w.z * fInvLength;
			u.z = -w.y * fInvLength;
			v.x = w.y * u.z - w.z * u.y;
			v.y = -w.x * u.z;
			v.z = w.x * u.y;
		}
	}

	/**
	 * 浅克隆。
	 * Shallow clone.
	 *
	 * @return 克隆向量 / Cloned vector
	 */
	public Vector3f clone() {
		try {
			return (Vector3f) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * 转为 float 数组。
	 * Converts to a float array.
	 *
	 * @param floats 目标数组；为 null 时新建 / Target array; created if null
	 * @return 数组（null 输入时含 x,y,z；非 null 时原样返回） / Array (with x,y,z if null input; otherwise returned as-is)
	 */
	public float[] toArray(float[] floats) {
		if (floats == null) {
			floats = new float[] { this.x, this.y, this.z };
		}
		return floats;
	}

	/**
	 * 判断与另一对象是否分量相等。
	 * Whether this equals another object by components.
	 *
	 * @param o 比较对象 / Object to compare
	 * @return 若 equal 则为 true / True if equal
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Vector3f)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		Vector3f comp = (Vector3f) o;
		if (Float.compare(this.x, comp.x) != 0) {
			return false;
		}
		if (Float.compare(this.y, comp.y) != 0) {
			return false;
		}
		return Float.compare(this.z, comp.z) == 0;
	}

	/**
	 * 哈希码。
	 * Hash code.
	 *
	 * @return 哈希值 / Hash value
	 */
	public int hashCode() {
		int hash = 37;
		hash += 37 * hash + Float.floatToIntBits(this.x);
		hash += 37 * hash + Float.floatToIntBits(this.y);
		hash += 37 * hash + Float.floatToIntBits(this.z);
		return hash;
	}

	/**
	 * 字符串表示，形如 {@code (x, y, z)}。
	 * String representation of the form {@code (x, y, z)}.
	 *
	 * @return 字符串表示 / String
	 */
	public String toString() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ")";
	}

	/**
	 * 获取 X 分量。
	 * Gets the X component.
	 *
	 * @return X 分量 / X component
	 */
	public float getX() {
		return this.x;
	}

	/**
	 * 设置 X 分量。
	 * Sets the X component.
	 *
	 * @param x X 分量 / X component
	 * @return 本向量 / This vector
	 */
	public Vector3f setX(float x) {
		this.x = x;
		return this;
	}

	/**
	 * 获取 Y 分量。
	 * Gets the Y component.
	 *
	 * @return Y 分量 / Y component
	 */
	public float getY() {
		return this.y;
	}

	/**
	 * 设置 Y 分量。
	 * Sets the Y component.
	 *
	 * @param y Y 分量 / Y component
	 * @return 本向量 / This vector
	 */
	public Vector3f setY(float y) {
		this.y = y;
		return this;
	}

	/**
	 * 获取 Z 分量。
	 * Gets the Z component.
	 *
	 * @return Z 分量 / Z component
	 */
	public float getZ() {
		return this.z;
	}

	/**
	 * 设置 Z 分量。
	 * Sets the Z component.
	 *
	 * @param z Z 分量 / Z component
	 * @return 本向量 / This vector
	 */
	public Vector3f setZ(float z) {
		this.z = z;
		return this;
	}

	/**
	 * 按索引获取分量（0=x, 1=y, 2=z）。
	 * Gets a component by index (0=x, 1=y, 2=z).
	 *
	 * @param index 分量索引 / Component index
	 * @return 分量值 / Component value
	 * @throws IllegalArgumentException 索引非法 / Invalid index
	 */
	public float get(int index) {
		switch (index) {
		case 0: {
			return this.x;
		}
		case 1: {
			return this.y;
		}
		case 2: {
			return this.z;
		}
		}
		throw new IllegalArgumentException("index must be either 0, 1 or 2");
	}

	/**
	 * 按索引设置分量（0=x, 1=y, 2=z）。
	 * Sets a component by index (0=x, 1=y, 2=z).
	 *
	 * @param index 分量索引 / Component index
	 * @param value 分量值 / Component value
	 * @throws IllegalArgumentException 索引非法 / Invalid index
	 */
	public void set(int index, float value) {
		switch (index) {
		case 0: {
			this.x = value;
			return;
		}
		case 1: {
			this.y = value;
			return;
		}
		case 2: {
			this.z = value;
			return;
		}
		}
		throw new IllegalArgumentException("index must be either 0, 1 or 2");
	}

	/**
	 * 重置为零向量（对象池复用接口）。
	 * Resets to zero (object-pool reuse hook).
	 */
	public void reset() {
		this.z = 0.0f;
		this.y = 0.0f;
		this.x = 0.0f;
	}

	/**
	 * 从对象池获取并置零的新实例。
	 * Obtains a zeroed instance from the object pool.
	 *
	 * @return 池化向量实例 / Pooled vector instance
	 */
	public static Vector3f newInstance() {
		Vector3f vector3f = (Vector3f) FACTORY.object();
		vector3f.z = 0.0f;
		vector3f.y = 0.0f;
		vector3f.x = 0.0f;
		return vector3f;
	}

	/**
	 * 将实例回收到对象池。
	 * Recycles an instance into the object pool.
	 *
	 * @param instance 待回收实例 / Instance to recycle
	 */
	public static void recycle(Vector3f instance) {
		FACTORY.recycle((Object) instance);
	}
}
