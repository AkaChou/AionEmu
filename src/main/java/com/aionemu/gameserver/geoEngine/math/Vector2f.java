package com.aionemu.gameserver.geoEngine.math;


import com.aionemu.boot.i18n.I18n;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import lombok.extern.slf4j.Slf4j;

/**
 * 二维向量，提供几何运算（jME 风格流式 API）。
 * Two-dimensional vector with geometric operations (jME-style fluent API).
 */
@Slf4j
public final class Vector2f implements Cloneable {

	/** 零向量 (0, 0)。 / Zero vector (0, 0). */
	public static final Vector2f ZERO = new Vector2f(0.0f, 0.0f);

	/** Unit XY vector (1, 1) / Unit XY vector (1, 1) */
	public static final Vector2f UNIT_XY = new Vector2f(1.0f, 1.0f);

	/** X 分量 / X component */
	public float x;

	/** Y 分量 / Y component */
	public float y;

	/**
	 * 用指定分量构造向量。
	 * Constructs a vector with the given components.
	 *
	 * @param x X 分量 / X component
	 * @param y Y 分量 / Y component
	 */
	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * 构造零向量。
	 * Constructs a zero vector.
	 */
	public Vector2f() {
		this.y = 0.0f;
		this.x = 0.0f;
	}

	/**
	 * 拷贝构造。
	 * Copy constructor.
	 *
	 * @param vector2f 源向量 / Source vector
	 */
	public Vector2f(Vector2f vector2f) {
		this.x = vector2f.x;
		this.y = vector2f.y;
	}

	/**
	 * 设置分量。
	 * Sets the components.
	 *
	 * @param x X 分量 / X component
	 * @param y Y 分量 / Y component
	 * @return 本向量 / This vector
	 */
	public Vector2f set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * 从另一向量拷贝分量。
	 * Copies components from another vector.
	 *
	 * @param vec 源向量 / Source vector
	 * @return 本向量 / This vector
	 */
	public Vector2f set(Vector2f vec) {
		this.x = vec.x;
		this.y = vec.y;
		return this;
	}

	/**
	 * 向量加法，返回新向量。
	 * Adds another vector and returns a new vector.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 结果向量；{@code vec} 为 null 时返回 null / Result vector; null if {@code vec} is null
	 */
	public Vector2f add(Vector2f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		return new Vector2f(this.x + vec.x, this.y + vec.y);
	}

	/**
	 * 就地向量加法。
	 * Adds another vector in place.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 本向量；{@code vec} 为 null 时返回 null / This vector; null if {@code vec} is null
	 */
	public Vector2f addLocal(Vector2f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		this.x += vec.x;
		this.y += vec.y;
		return this;
	}

	/**
	 * 就地加上给定分量。
	 * Adds the given components in place.
	 *
	 * @param addX X 增量 / X increment
	 * @param addY Y 增量 / Y increment
	 * @return 本向量 / This vector
	 */
	public Vector2f addLocal(float addX, float addY) {
		this.x += addX;
		this.y += addY;
		return this;
	}

	/**
	 * 向量加法，结果写入指定存储向量。
	 * Adds another vector and stores the result.
	 *
	 * @param vec 另一向量 / Other vector
	 * @param result 结果存储；为 null 时新建 / Result store; created if null
	 * @return 结果向量；{@code vec} 为 null 时返回 null / Result vector; null if {@code vec} is null
	 */
	public Vector2f add(Vector2f vec, Vector2f result) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		if (result == null) {
			result = new Vector2f();
		}
		result.x = this.x + vec.x;
		result.y = this.y + vec.y;
		return result;
	}

	/**
	 * 点积。
	 * Computes the dot product.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 点积；{@code vec} 为 null 时返回 0 / Dot product; 0 if {@code vec} is null
	 */
	public float dot(Vector2f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bf0985989b35"));
			return 0.0f;
		}
		return this.x * vec.x + this.y * vec.y;
	}

	/**
	 * 二维叉积（结果为 Z 轴三维向量）。
	 * 2D cross product (result is a 3D vector along Z).
	 *
	 * @param v 另一向量 / Other vector
	 * @return 叉积结果 / Cross-product result
	 */
	public Vector3f cross(Vector2f v) {
		return new Vector3f(0.0f, 0.0f, this.determinant(v));
	}

	/**
	 * 二维行列式（有向面积）：x·vy − y·vx。
	 * 2D determinant (signed area): x·vy − y·vx.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 行列式值 / Determinant value
	 */
	public float determinant(Vector2f v) {
		return this.x * v.y - this.y * v.x;
	}

	/**
	 * 向目标向量线性插值（就地）。
	 * Linearly interpolates toward a target vector in place.
	 *
	 * @param finalVec 目标向量 / Target vector
	 * @param changeAmnt 插值因子 [0,1] / Interpolation factor [0,1]
	 * @return 本向量 / This vector
	 */
	public Vector2f interpolate(Vector2f finalVec, float changeAmnt) {
		this.x = (1.0f - changeAmnt) * this.x + changeAmnt * finalVec.x;
		this.y = (1.0f - changeAmnt) * this.y + changeAmnt * finalVec.y;
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
	public Vector2f interpolate(Vector2f beginVec, Vector2f finalVec, float changeAmnt) {
		this.x = (1.0f - changeAmnt) * beginVec.x + changeAmnt * finalVec.x;
		this.y = (1.0f - changeAmnt) * beginVec.y + changeAmnt * finalVec.y;
		return this;
	}

	/**
	 * 判断向量是否有效（非 null、非 NaN、非无穷）。
	 * Checks whether a vector is valid (non-null, non-NaN, non-infinite).
	 *
	 * @param vector 待检查向量 / Vector to check
	 * @return 若 valid 则为 true / True if valid
	 */
	public static boolean isValidVector(Vector2f vector) {
		if (vector == null) {
			return false;
		}
		if (Float.isNaN(vector.x) || Float.isNaN(vector.y)) {
			return false;
		}
		return !Float.isInfinite(vector.x) && !Float.isInfinite(vector.y);
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
		return this.x * this.x + this.y * this.y;
	}

	/**
	 * 到另一向量的距离平方。
	 * Squared distance to another vector.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 距离平方 / Squared distance
	 */
	public float distanceSquared(Vector2f v) {
		double dx = this.x - v.x;
		double dy = this.y - v.y;
		return (float) (dx * dx + dy * dy);
	}

	/**
	 * 到指定坐标的距离平方。
	 * Squared distance to the given coordinates.
	 *
	 * @param otherX 目标 X / Target X
	 * @param otherY 目标 Y / Target Y
	 * @return 距离平方 / Squared distance
	 */
	public float distanceSquared(float otherX, float otherY) {
		double dx = this.x - otherX;
		double dy = this.y - otherY;
		return (float) (dx * dx + dy * dy);
	}

	/**
	 * 到另一向量的欧氏距离。
	 * Euclidean distance to another vector.
	 *
	 * @param v 另一向量 / Other vector
	 * @return 距离 / Distance
	 */
	public float distance(Vector2f v) {
		return FastMath.sqrt(this.distanceSquared(v));
	}

	/**
	 * 标量乘法，返回新向量。
	 * Multiplies by a scalar and returns a new vector.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 结果向量 / Result vector
	 */
	public Vector2f mult(float scalar) {
		return new Vector2f(this.x * scalar, this.y * scalar);
	}

	/**
	 * 就地标量乘法。
	 * Multiplies by a scalar in place.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 本向量 / This vector
	 */
	public Vector2f multLocal(float scalar) {
		this.x *= scalar;
		this.y *= scalar;
		return this;
	}

	/**
	 * 就地分量乘法。
	 * Multiplies component-wise in place.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 本向量；{@code vec} 为 null 时返回 null / This vector; null if {@code vec} is null
	 */
	public Vector2f multLocal(Vector2f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		this.x *= vec.x;
		this.y *= vec.y;
		return this;
	}

	/**
	 * 标量乘法，结果写入指定存储向量。
	 * Multiplies by a scalar and stores the result.
	 *
	 * @return 标量 / Scalar
	 * @param product 结果存储；为 null 时新建 / Result store; created if null
	 * @param scalar 结果向量 / Result vector
	 */
	public Vector2f mult(float scalar, Vector2f product) {
		if (null == product) {
			product = new Vector2f();
		}
		product.x = this.x * scalar;
		product.y = this.y * scalar;
		return product;
	}

	/**
	 * 标量除法，返回新向量。
	 * Divides by a scalar and returns a new vector.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 结果向量 / Result vector
	 */
	public Vector2f divide(float scalar) {
		return new Vector2f(this.x / scalar, this.y / scalar);
	}

	/**
	 * 就地标量除法。
	 * Divides by a scalar in place.
	 *
	 * @param scalar 标量 / Scalar
	 * @return 本向量 / This vector
	 */
	public Vector2f divideLocal(float scalar) {
		this.x /= scalar;
		this.y /= scalar;
		return this;
	}

	/**
	 * 取反，返回新向量。
	 * Negates and returns a new vector.
	 *
	 * @return 取反后的新向量 / Negated new vector
	 */
	public Vector2f negate() {
		return new Vector2f(-this.x, -this.y);
	}

	/**
	 * 就地取反。
	 * Negates in place.
	 *
	 * @return 本向量 / This vector
	 */
	public Vector2f negateLocal() {
		this.x = -this.x;
		this.y = -this.y;
		return this;
	}

	/**
	 * 向量减法，返回新向量。
	 * Subtracts another vector and returns a new vector.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 结果向量 / Result vector
	 */
	public Vector2f subtract(Vector2f vec) {
		return this.subtract(vec, null);
	}

	/**
	 * 向量减法，结果写入指定存储向量。
	 * Subtracts another vector and stores the result.
	 *
	 * @param vec 另一向量 / Other vector
	 * @param store 结果存储；为 null 时新建 / Result store; created if null
	 * @return 结果向量 / Result vector
	 */
	public Vector2f subtract(Vector2f vec, Vector2f store) {
		if (store == null) {
			store = new Vector2f();
		}
		store.x = this.x - vec.x;
		store.y = this.y - vec.y;
		return store;
	}

	/**
	 * 减去给定分量，返回新向量。
	 * Subtracts the given components and returns a new vector.
	 *
	 * @param valX 要减去的 X / X to subtract
	 * @param valY 要减去的 Y / Y to subtract
	 * @return 结果向量 / Result vector
	 */
	public Vector2f subtract(float valX, float valY) {
		return new Vector2f(this.x - valX, this.y - valY);
	}

	/**
	 * 就地向量减法。
	 * Subtracts another vector in place.
	 *
	 * @param vec 另一向量 / Other vector
	 * @return 本向量；{@code vec} 为 null 时返回 null / This vector; null if {@code vec} is null
	 */
	public Vector2f subtractLocal(Vector2f vec) {
		if (null == vec) {
			log.warn(I18n.get("log.bd114ba746e6"));
			return null;
		}
		this.x -= vec.x;
		this.y -= vec.y;
		return this;
	}

	/**
	 * 就地减去给定分量。
	 * Subtracts the given components in place.
	 *
	 * @param valX 要减去的 X / X to subtract
	 * @param valY 要减去的 Y / Y to subtract
	 * @return 本向量 / This vector
	 */
	public Vector2f subtractLocal(float valX, float valY) {
		this.x -= valX;
		this.y -= valY;
		return this;
	}

	/**
	 * 归一化，返回新单位向量。
	 * Returns a new normalized unit vector.
	 *
	 * @return 单位向量 / Unit vector
	 */
	public Vector2f normalize() {
		float length = this.length();
		if (length != 0.0f) {
			return this.divide(length);
		}
		return this.divide(1.0f);
	}

	/**
	 * 就地归一化。
	 * Normalizes in place.
	 *
	 * @return 本向量 / This vector
	 */
	public Vector2f normalizeLocal() {
		float length = this.length();
		if (length != 0.0f) {
			return this.divideLocal(length);
		}
		return this.divideLocal(1.0f);
	}

	/**
	 * 与另一向量的最小夹角（弧度，基于点积）。
	 * Smallest angle to another vector in radians (via dot product).
	 *
	 * @param otherVector 另一向量 / Other vector
	 * @return 夹角（弧度） / Angle in radians
	 */
	public float smallestAngleBetween(Vector2f otherVector) {
		float dotProduct = this.dot(otherVector);
		float angle = FastMath.acos(dotProduct);
		return angle;
	}

	/**
	 * 与另一向量的有向夹角（弧度，基于 atan2）。
	 * Signed angle to another vector in radians (via atan2).
	 *
	 * @param otherVector 另一向量 / other vector
	 * @return 有向夹角（弧度） / Signed angle in radians
	 */
	public float angleBetween(Vector2f otherVector) {
		float angle = FastMath.atan2(otherVector.y, otherVector.x) - FastMath.atan2(this.y, this.x);
		return angle;
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
	public Vector2f setX(float x) {
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
	public Vector2f setY(float y) {
		this.y = y;
		return this;
	}

	/**
	 * 获取极角（弧度，取负 atan2）。
	 * Gets the polar angle in radians (negated atan2).
	 *
	 * @return 夹角（弧度） / Angle in radians
	 */
	public float getAngle() {
		return -FastMath.atan2(this.y, this.x);
	}

	/**
	 * 将本向量置零。
	 * Sets this vector to zero.
	 *
	 * @return 本向量 / This vector
	 */
	public Vector2f zero() {
		this.y = 0.0f;
		this.x = 0.0f;
		return this;
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
		return hash;
	}

	/**
	 * 浅克隆。
	 * Shallow clone.
	 *
	 * @return 克隆向量 / Cloned vector
	 */
	public Vector2f clone() {
		try {
			return (Vector2f) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * 转为 float 数组。
	 * Converts to a float array.
	 *
	 * @param floats 目标数组；为 null 时新建 / Target array; created if null
	 * @return 数组（null 输入时含 x,y；非 null 时原样返回） / Array (with x,y if null input; otherwise returned as-is)
	 */
	public float[] toArray(float[] floats) {
		if (floats == null) {
			floats = new float[] { this.x, this.y };
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
		if (!(o instanceof Vector2f)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		Vector2f comp = (Vector2f) o;
		if (Float.compare(this.x, comp.x) != 0) {
			return false;
		}
		return Float.compare(this.y, comp.y) == 0;
	}

	/**
	 * 字符串表示，形如 {@code (x, y)}。
	 * String representation of the form {@code (x, y)}.
	 *
	 * @return 字符串表示 / String
	 */
	public String toString() {
		return "(" + this.x + ", " + this.y + ")";
	}

	/**
	 * 从外部输入读取分量。
	 * Reads components from an external input.
	 *
	 * @param in 输入流 / Input stream
	 * @throws IOException I/O 错误 / I/O error
	 * @throws ClassNotFoundException 类未找到 / Class not found
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.x = in.readFloat();
		this.y = in.readFloat();
	}

	/**
	 * 将分量写入外部输出。
	 * Writes components to an external output.
	 *
	 * @param out 输出流 / output stream
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeFloat(this.x);
		out.writeFloat(this.y);
	}

	/**
	 * 返回运行时类标记。
	 * Returns the runtime class tag.
	 *
	 * @return 类对象 / Class object
	 */
	public Class<? extends Vector2f> getClassTag() {
		return this.getClass();
	}

	/**
	 * 绕原点旋转（就地）。
	 * Rotates around the origin in place.
	 *
	 * @param angle 角度（弧度） / Angle in radians
	 * @param cw 是否顺时针 / Whether clockwise
	 */
	public void rotateAroundOrigin(float angle, boolean cw) {
		if (cw) {
			angle = -angle;
		}
		float newX = FastMath.cos(angle) * this.x - FastMath.sin(angle) * this.y;
		float newY = FastMath.sin(angle) * this.x + FastMath.cos(angle) * this.y;
		this.x = newX;
		this.y = newY;
	}
}
