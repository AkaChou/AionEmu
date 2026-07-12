package com.aionemu.gameserver.geoEngine.math;

import java.util.Random;

/**
 * 快速数学工具类，提供常用数学运算、插值、三角函数与坐标转换。
 * Fast math utility class providing common math operations, interpolation, trig and coordinate conversions.
 */
public final class FastMath {
	/** Double-precision float epsilon / Double-precision float epsilon */
	public static final double DBL_EPSILON = 2.220446049250313E-16;
	/** Single-precision float epsilon / Single-precision float epsilon */
	public static final float FLT_EPSILON = 1.1920929E-7f;
	/** 零值容差。 / Zero-value tolerance. */
	public static final float ZERO_TOLERANCE = 1.0E-4f;
	/** 三分之一。 / One third. */
	public static final float ONE_THIRD = 0.33333334f;
	/** 圆周率 π。 / The constant pi. */
	public static final float PI = (float) Math.PI;
	/** 两倍圆周率 2π。 / Two times pi. */
	public static final float TWO_PI = (float) Math.PI * 2;
	/** 半圆周率 π/2。 / Half of pi. */
	public static final float HALF_PI = 1.5707964f;
	/** 四分之一圆周率 π/4。 / Quarter of pi. */
	public static final float QUARTER_PI = 0.7853982f;
	/** 圆周率倒数 1/π。 / Inverse of pi. */
	public static final float INV_PI = 0.31830987f;
	/** 两倍圆周率倒数 1/(2π)。 / Inverse of two pi. */
	public static final float INV_TWO_PI = 0.15915494f;
	/** 角度转弧度系数。 / Degrees-to-radians conversion factor. */
	public static final float DEG_TO_RAD = (float) Math.PI / 180;
	/** 弧度转角度系数。 / Radians-to-degrees conversion factor. */
	public static final float RAD_TO_DEG = 57.295776f;
	/** 共享随机数生成器。 / Shared random number generator. */
	public static final Random rand = new Random(System.currentTimeMillis());

	/**
	 * 私有构造，禁止实例化。
	 * Private constructor to prevent instantiation.
	 */
	private FastMath() {
	}

	/**
	 * 判断给定整数是否为 2 的幂。
	 * Tests whether the given integer is a power of two.
	 *
	 * @param number 待检测整数 / integer to test
	 * @return 若为 2 的幂则为 true / true if the number is a power of two
	 */
	public static boolean isPowerOfTwo(int number) {
		return number > 0 && (number & number - 1) == 0;
	}

	/**
	 * 返回不小于给定值的最近 2 的幂。
	 * Returns the nearest power of two not less than the given value.
	 *
	 * input value
	 *
	 * @param number
	 * @return 最近的 2 的幂 / nearest power of two
	 */
	public static int nearestPowerOfTwo(int number) {
		return (int) Math.pow(2.0, Math.ceil(Math.log(number) / Math.log(2.0)));
	}

	/**
	 * 线性插值。
	 * Linear interpolation.
	 *
	 * @param scale 插值系数（0~1） / interpolation factor (0~1)
	 * start value
	 * end value
	 * interpolated result
	 */
	public static float interpolateLinear(float scale, float startValue, float endValue) {
		if (startValue == endValue) {
			return startValue;
		}
		if (scale <= 0.0f) {
			return startValue;
		}
		if (scale >= 1.0f) {
			return endValue;
		}
		return (1.0f - scale) * startValue + scale * endValue;
	}

	/**
	 * 对三维向量进行线性插值。
	 * Linearly interpolates between two 3D vectors.
	 *
	 * @param scale 插值系数（0~1） / interpolation factor (0~1)
	 * start vector
	 * end vector
	 * @return 插值后的新向量 / newly interpolated vector
	 */
	public static Vector3f interpolateLinear(float scale, Vector3f startValue, Vector3f endValue) {
		Vector3f res = new Vector3f();
		res.x = FastMath.interpolateLinear(scale, startValue.x, endValue.x);
		res.y = FastMath.interpolateLinear(scale, startValue.y, endValue.y);
		res.z = FastMath.interpolateLinear(scale, startValue.z, endValue.z);
		return res;
	}

	/**
	 * Catmull-Rom 样条标量插值。
	 * Catmull-Rom spline scalar interpolation.
	 *
	 * @param u 插值参数 / interpolation parameter
	 * @param T 张力系数 / tension factor
	 * @param p0 控制点 0 / control point 0
	 * @param p1 控制点 1 / control point 1
	 * @param p2 控制点 2 / control point 2
	 * @param p3 控制点 3 / control point 3
	 * interpolated result
	 */
	public static float interpolateCatmullRom(float u, float T, float p0, float p1, float p2, float p3) {
		double c1 = p1;
		double c2 = -1.0 * (double) T * (double) p0 + (double) (T * p2);
		double c3 = 2.0f * T * p0 + (T - 3.0f) * p1 + (3.0f - 2.0f * T) * p2 + -T * p3;
		double c4 = -T * p0 + (2.0f - T) * p1 + (T - 2.0f) * p2 + T * p3;
		return (float) (((c4 * (double) u + c3) * (double) u + c2) * (double) u + c1);
	}

	/**
	 * 对三维向量进行 Catmull-Rom 样条插值。
	 * Catmull-Rom spline interpolation for 3D vectors.
	 *
	 * @param u 插值参数 / interpolation parameter
	 * @param T 张力系数 / tension factor
	 * @param p0 控制点 0 / control point 0
	 * @param p1 控制点 1 / control point 1
	 * @param p2 控制点 2 / control point 2
	 * @param p3 控制点 3 / control point 3
	 * @return 插值后的新向量 / newly interpolated vector
	 */
	public static Vector3f interpolateCatmullRom(float u, float T, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
		Vector3f res = new Vector3f();
		res.x = FastMath.interpolateCatmullRom(u, T, p0.x, p1.x, p2.x, p3.x);
		res.y = FastMath.interpolateCatmullRom(u, T, p0.y, p1.y, p2.y, p3.y);
		res.z = FastMath.interpolateCatmullRom(u, T, p0.z, p1.z, p2.z, p3.z);
		return res;
	}

	/**
	 * 反余弦，输入超出 [-1, 1] 时进行钳制。
	 * Arc cosine with clamping for values outside [-1, 1].
	 *
	 * input value
	 *
	 * @param fValue
	 * @return 反余弦结果（弧度） / arccosine in radians
	 */
	public static float acos(float fValue) {
		if (-1.0f < fValue) {
			if (fValue < 1.0f) {
				return (float) Math.acos(fValue);
			}
			return 0.0f;
		}
		return (float) Math.PI;
	}

	/**
	 * 反正弦，输入超出 [-1, 1] 时进行钳制。
	 * Arc sine with clamping for values outside [-1, 1].
	 *
	 * input value
	 *
	 * @param fValue
	 * @return 反正弦结果（弧度） / arcsine in radians
	 */
	public static float asin(float fValue) {
		if (-1.0f < fValue) {
			if (fValue < 1.0f) {
				return (float) Math.asin(fValue);
			}
			return 1.5707964f;
		}
		return -1.5707964f;
	}

	/**
	 * 反正切。
	 * Arc tangent.
	 *
	 * input value
	 *
	 * @param fValue
	 * @return 反正切结果（弧度） / arctangent in radians
	 */
	public static float atan(float fValue) {
		return (float) Math.atan(fValue);
	}

	/**
	 * 双参数反正切，返回正确象限。
	 * Two-argument arctangent returning the correct quadrant.
	 *
	 * @param fY Y 分量 / Y component
	 * @param fX X 分量 / X component
	 * @return 反正切结果（弧度） / arctangent in radians
	 */
	public static float atan2(float fY, float fX) {
		return (float) Math.atan2(fY, fX);
	}

	/**
	 * 向上取整。
	 * Ceiling function.
	 *
	 * input value
	 *
	 * @param fValue
	 * @return 不小于输入的最小整数 / smallest integer not less than the input
	 */
	public static float ceil(float fValue) {
		return (float) Math.ceil(fValue);
	}

	/**
	 * 将角度归约到适合 sin 计算的范围。
	 * Reduces an angle into a range suitable for sine computation.
	 *
	 * input radians
	 *
	 * @param radians
	 * @return 归约后的角度 / reduced angle
	 */
	public static float reduceSinAngle(float radians) {
		if (Math.abs(radians %= (float) Math.PI * 2) > (float) Math.PI) {
			radians -= (float) Math.PI * 2;
		}
		if (Math.abs(radians) > 1.5707964f) {
			radians = (float) Math.PI - radians;
		}
		return radians;
	}

	/**
	 * 优化的正弦计算（先归约角度）。
	 * Optimized sine computation (with prior angle reduction).
	 *
	 * input radians
	 * sine value
	 */
	public static float sin2(float fValue) {
		if ((double) Math.abs(fValue = FastMath.reduceSinAngle(fValue)) <= 0.7853981633974483) {
			return (float) Math.sin(fValue);
		}
		return (float) Math.cos(1.5707963267948966 - (double) fValue);
	}

	/**
	 * 优化的余弦计算（基于 sin2）。
	 * Optimized cosine computation (based on sin2).
	 *
	 * input radians
	 * cosine value
	 */
	public static float cos2(float fValue) {
		return FastMath.sin2(fValue + 1.5707964f);
	}

	/**
	 * 余弦。
	 * Cosine.
	 *
	 * @param v 输入弧度 / input radians
	 * cosine value
	 */
	public static float cos(float v) {
		return (float) Math.cos(v);
	}

	/**
	 * 正弦。
	 * Sine.
	 *
	 * @param v 输入弧度 / input radians
	 * sine value
	 */
	public static float sin(float v) {
		return (float) Math.sin(v);
	}

	/**
	 * 自然指数 e^x。
	 * Natural exponential e^x.
	 *
	 * exponent
	 * e raised to the power of fValue
	 */
	public static float exp(float fValue) {
		return (float) Math.exp(fValue);
	}

	/**
	 * 绝对值。
	 * Absolute value.
	 *
	 * input value
	 * absolute value
	 */
	public static float abs(float fValue) {
		if (fValue < 0.0f) {
			return -fValue;
		}
		return fValue;
	}

	/**
	 * 向下取整。
	 * Floor function.
	 *
	 * input value
	 *
	 * @param fValue
	 * @return 不大于输入的最大整数 / largest integer not greater than the input
	 */
	public static float floor(float fValue) {
		return (float) Math.floor(fValue);
	}

	/**
	 * 平方根倒数 1/√x。
	 * Inverse square root 1/√x.
	 *
	 * input value
	 *
	 * @param fValue
	 * @return 平方根倒数 / inverse square root
	 */
	public static float invSqrt(float fValue) {
		return (float) (1.0 / Math.sqrt(fValue));
	}

	/**
	 * 快速近似平方根倒数（Quake 算法）。
	 * Fast approximate inverse square root (Quake algorithm).
	 *
	 * @param x 输入值 / input value
	 * @return 近似平方根倒数 / approximate inverse square root
	 */
	public static float fastInvSqrt(float x) {
		float xhalf = 0.5f * x;
		int i = Float.floatToIntBits(x);
		i = 1597463174 - (i >> 1);
		x = Float.intBitsToFloat(i);
		x *= 1.5f - xhalf * x * x;
		return x;
	}

	/**
	 * 自然对数 ln(x)。
	 * Natural logarithm ln(x).
	 *
	 * input value
	 * natural logarithm
	 */
	public static float log(float fValue) {
		return (float) Math.log(fValue);
	}

	/**
	 * 指定底数的对数。
	 * Logarithm with a given base.
	 *
	 * argument
	 * base
	 * logarithm result
	 */
	public static float log(float value, float base) {
		return (float) (Math.log(value) / Math.log(base));
	}

	/**
	 * 幂运算。
	 * Power function.
	 *
	 * base
	 * exponent
	 * power result
	 */
	public static float pow(float fBase, float fExponent) {
		return (float) Math.pow(fBase, fExponent);
	}

	/**
	 * 平方。
	 * Square.
	 *
	 * input value
	 * squared value
	 */
	public static float sqr(float fValue) {
		return fValue * fValue;
	}

	/**
	 * 平方根。
	 * Square root.
	 *
	 * input value
	 * square root
	 */
	public static float sqrt(float fValue) {
		return (float) Math.sqrt(fValue);
	}

	/**
	 * 正切。
	 * Tangent.
	 *
	 * input radians
	 * tangent value
	 */
	public static float tan(float fValue) {
		return (float) Math.tan(fValue);
	}

	/**
	 * 整数符号函数。
	 * Integer signum function.
	 *
	 * input integer
	 * 1, 0 or -1
	 */
	public static int sign(int iValue) {
		if (iValue > 0) {
			return 1;
		}
		if (iValue < 0) {
			return -1;
		}
		return 0;
	}

	/**
	 * 浮点符号函数。
	 * Floating-point signum function.
	 *
	 * input value
	 * sign (1.0, 0.0 or -1.0)
	 */
	public static float sign(float fValue) {
		return Math.signum(fValue);
	}

	/**
	 * 判断三点的相对方向（逆时针/顺时针/共线）。
	 * clockwise / collinear).
	 *
	 * @param p0 点 0 / point 0
	 * @param p1 点 1 / point 1
	 * @param p2 点 2 / point 2
	 * @return 1 为逆时针，-1 为顺时针，0 为共线 / 1 for CCW, -1 for CW, 0 for collinear
	 */
	public static int counterClockwise(Vector2f p0, Vector2f p1, Vector2f p2) {
		float dx1 = p1.x - p0.x;
		float dy2 = p2.y - p0.y;
		float dy1 = p1.y - p0.y;
		float dx2 = p2.x - p0.x;
		if (dx1 * dy2 > dy1 * dx2) {
			return 1;
		}
		if (dx1 * dy2 < dy1 * dx2) {
			return -1;
		}
		if (dx1 * dx2 < 0.0f || dy1 * dy2 < 0.0f) {
			return -1;
		}
		if (dx1 * dx1 + dy1 * dy1 < dx2 * dx2 + dy2 * dy2) {
			return 1;
		}
		return 0;
	}

	/**
	 * 判断点是否在三角形内部（含边界）。
	 * Tests whether a point lies inside a triangle (including boundary).
	 *
	 * @param t0 三角形顶点 0 / triangle vertex 0
	 * @param t1 三角形顶点 1 / triangle vertex 1
	 * @param t2 三角形顶点 2 / triangle vertex 2
	 * @param p 待测点 / point to test
	 * @return 非 0 表示在内部（或边界），0 表示在外部 / non-zero if inside (or on edge), 0 if outside
	 */
	public static int pointInsideTriangle(Vector2f t0, Vector2f t1, Vector2f t2, Vector2f p) {
		int val1 = FastMath.counterClockwise(t0, t1, p);
		if (val1 == 0) {
			return 1;
		}
		int val2 = FastMath.counterClockwise(t1, t2, p);
		if (val2 == 0) {
			return 1;
		}
		if (val2 != val1) {
			return 0;
		}
		int val3 = FastMath.counterClockwise(t2, t0, p);
		if (val3 == 0) {
			return 1;
		}
		if (val3 != val1) {
			return 0;
		}
		return val3;
	}

	/**
	 * 计算 4×4 矩阵行列式。
	 * Computes the determinant of a 4×4 matrix.
	 *
	 * @param m00 矩阵元素 (0,0) / matrix element (0,0)
	 * @param m01 矩阵元素 (0,1) / matrix element (0,1)
	 * @param m02 矩阵元素 (0,2) / matrix element (0,2)
	 * @param m03 矩阵元素 (0,3) / matrix element (0,3)
	 * @param m10 矩阵元素 (1,0) / matrix element (1,0)
	 * @param m11 矩阵元素 (1,1) / matrix element (1,1)
	 * @param m12 矩阵元素 (1,2) / matrix element (1,2)
	 * @param m13 矩阵元素 (1,3) / matrix element (1,3)
	 * @param m20 矩阵元素 (2,0) / matrix element (2,0)
	 * @param m21 矩阵元素 (2,1) / matrix element (2,1)
	 * @param m22 矩阵元素 (2,2) / matrix element (2,2)
	 * @param m23 矩阵元素 (2,3) / matrix element (2,3)
	 * @param m30 矩阵元素 (3,0) / matrix element (3,0)
	 * @param m31 矩阵元素 (3,1) / matrix element (3,1)
	 * @param m32 矩阵元素 (3,2) / matrix element (3,2)
	 * @param m33 矩阵元素 (3,3) / matrix element (3,3)
	 * determinant value
	 */
	public static float determinant(double m00, double m01, double m02, double m03, double m10, double m11, double m12,
			double m13, double m20, double m21, double m22, double m23, double m30, double m31, double m32,
			double m33) {
		double det01 = m20 * m31 - m21 * m30;
		double det02 = m20 * m32 - m22 * m30;
		double det03 = m20 * m33 - m23 * m30;
		double det12 = m21 * m32 - m22 * m31;
		double det13 = m21 * m33 - m23 * m31;
		double det23 = m22 * m33 - m23 * m32;
		return (float) (m00 * (m11 * det23 - m12 * det13 + m13 * det12)
				- m01 * (m10 * det23 - m12 * det03 + m13 * det02) + m02 * (m10 * det13 - m11 * det03 + m13 * det01)
				- m03 * (m10 * det12 - m11 * det02 + m12 * det01));
	}

	/**
	 * 生成 [0, 1) 范围的随机浮点数。
	 * Generates a random float in the range [0, 1).
	 *
	 * @return 随机浮点数 / random float
	 */
	public static float nextRandomFloat() {
		return rand.nextFloat();
	}

	/**
	 * 生成指定闭区间内的随机整数。
	 * Generates a random integer within the given closed range.
	 *
	 * @param min 最小值（含） / minimum (inclusive)
	 * @param max 最大值（含） / maximum (inclusive)
	 * random integer
	 */
	public static int nextRandomInt(int min, int max) {
		return (int) (FastMath.nextRandomFloat() * (float) (max - min + 1)) + min;
	}

	/**
	 * 生成随机整数。
	 * Generates a random integer.
	 *
	 * random integer
	 */
	public static int nextRandomInt() {
		return rand.nextInt();
	}

	/**
	 * 球坐标转笛卡尔坐标（Y 为上轴）。
	 * Converts spherical coordinates to Cartesian (Y-up).
	 *
	 * @param sphereCoords 球坐标 (半径, 方位角, 仰角) / spherical coords (radius, azimuth, elevation)
	 * @param store 存储结果的向量 / vector to store the result
	 * the store vector
	 */
	public static Vector3f sphericalToCartesian(Vector3f sphereCoords, Vector3f store) {
		store.y = sphereCoords.x * FastMath.sin(sphereCoords.z);
		float a = sphereCoords.x * FastMath.cos(sphereCoords.z);
		store.x = a * FastMath.cos(sphereCoords.y);
		store.z = a * FastMath.sin(sphereCoords.y);
		return store;
	}

	/**
	 * 笛卡尔坐标转球坐标（Y 为上轴）。
	 * Converts Cartesian coordinates to spherical (Y-up).
	 *
	 * @param cartCoords 笛卡尔坐标 / Cartesian coordinates
	 * @param store 存储结果的向量 (半径, 方位角, 仰角) / vector to store (radius, azimuth, elevation)
	 * the store vector
	 */
	public static Vector3f cartesianToSpherical(Vector3f cartCoords, Vector3f store) {
		if (cartCoords.x == 0.0f) {
			cartCoords.x = 1.1920929E-7f;
		}
		store.x = FastMath
				.sqrt(cartCoords.x * cartCoords.x + cartCoords.y * cartCoords.y + cartCoords.z * cartCoords.z);
		store.y = FastMath.atan(cartCoords.z / cartCoords.x);
		if (cartCoords.x < 0.0f) {
			store.y += (float) Math.PI;
		}
		store.z = FastMath.asin(cartCoords.y / store.x);
		return store;
	}

	/**
	 * 球坐标转笛卡尔坐标（Z 为上轴）。
	 * Converts spherical coordinates to Cartesian (Z-up).
	 *
	 * @param sphereCoords 球坐标 (半径, 方位角, 仰角) / spherical coords (radius, azimuth, elevation)
	 * @param store 存储结果的向量 / vector to store the result
	 * the store vector
	 */
	public static Vector3f sphericalToCartesianZ(Vector3f sphereCoords, Vector3f store) {
		store.z = sphereCoords.x * FastMath.sin(sphereCoords.z);
		float a = sphereCoords.x * FastMath.cos(sphereCoords.z);
		store.x = a * FastMath.cos(sphereCoords.y);
		store.y = a * FastMath.sin(sphereCoords.y);
		return store;
	}

	/**
	 * 笛卡尔坐标转球坐标（Z 为上轴）。
	 * Converts Cartesian coordinates to spherical (Z-up).
	 *
	 * @param cartCoords 笛卡尔坐标 / Cartesian coordinates
	 * @param store 存储结果的向量 (半径, 方位角, 仰角) / vector to store (radius, azimuth, elevation)
	 * the store vector
	 */
	public static Vector3f cartesianZToSpherical(Vector3f cartCoords, Vector3f store) {
		if (cartCoords.x == 0.0f) {
			cartCoords.x = 1.1920929E-7f;
		}
		store.x = FastMath
				.sqrt(cartCoords.x * cartCoords.x + cartCoords.y * cartCoords.y + cartCoords.z * cartCoords.z);
		store.z = FastMath.atan(cartCoords.z / cartCoords.x);
		if (cartCoords.x < 0.0f) {
			store.z += (float) Math.PI;
		}
		store.y = FastMath.asin(cartCoords.y / store.x);
		return store;
	}

	/**
	 * 将值归一化到指定区间（循环折返）。
	 * Normalizes a value into the given range (wrapping around).
	 *
	 * input value
	 *
	 * @param min 区间下限 / range minimum
	 * @param max 区间上限 / range maximum
	 * @param max
	 * @return 归一化后的值 / normalized value
	 */
	public static float normalize(float val, float min, float max) {
		if (Float.isInfinite(val) || Float.isNaN(val)) {
			return 0.0f;
		}
		float range = max - min;
		while (val > max) {
			val -= range;
		}
		while (val < min) {
			val += range;
		}
		return val;
	}

	/**
	 * 将 x 的符号设为与 y 相同。
	 * Copies the sign of y onto x.
	 *
	 * @param x 源值 / source value
	 * @param y 提供符号的值 / value providing the sign
	 * @return 带有 y 符号的 x / x with the sign of y
	 */
	public static float copysign(float x, float y) {
		if (y >= 0.0f && x <= 0.0f) {
			return -x;
		}
		if (y < 0.0f && x >= 0.0f) {
			return -x;
		}
		return x;
	}

	/**
	 * 将输入钳制到 [min, max]。
	 * Clamps the input to the range [min, max].
	 *
	 * input value
	 * lower bound
	 * upper bound
	 * @return 钳制后的值 / clamped value
	 */
	public static float clamp(float input, float min, float max) {
		return input < min ? min : (input > max ? max : input);
	}

	/**
	 * 将输入饱和到 [0, 1]。
	 * Saturates the input to the range [0, 1].
	 *
	 * input value
	 *
	 * @param input
	 * @return 饱和后的值 / saturated value
	 */
	public static float saturate(float input) {
		return FastMath.clamp(input, 0.0f, 1.0f);
	}

	/**
	 * 将半精度浮点数（16 位）转换为单精度浮点数。
	 * Converts a half-precision float (16-bit) to a single-precision float.
	 *
	 * @param half 半精度位模式 / half-precision bit pattern
	 * @return 单精度浮点数 / single-precision float
	 */
	public static float convertHalfToFloat(int half) {
		if ((half & 0x7c00) == 0x7c00 && (half & 0x03ff) != 0) {
			return Float.intBitsToFloat(((half & 0x8000) << 16) | 0x7f800000 | ((half & 0x03ff) << 13));
		}
		switch (half) {
		case 0x0000:
			return 0f;
		case 0x8000:
			return -0f;
		case 0x7c00:
			return Float.POSITIVE_INFINITY;
		case 0xfc00:
			return Float.NEGATIVE_INFINITY;
		default:
			return Float.intBitsToFloat(
					((half & 0x8000) << 16) | (((half & 0x7c00) + 0x1C000) << 13) | ((half & 0x03FF) << 13));
		}
	}

	/**
	 * 将单精度浮点数转换为半精度浮点数（16 位）。
	 * Converts a single-precision float to a half-precision float (16-bit).
	 *
	 * @param flt 单精度浮点数 / single-precision float
	 * @return 半精度位模式 / half-precision bit pattern
	 * when the input is NaN。
	 */
	public static short convertFloatToHalf(float flt) {
		if (Float.isNaN(flt)) {
			throw new UnsupportedOperationException("NaN to half conversion not supported!");
		} else if (flt == Float.POSITIVE_INFINITY) {
			return (short) 0x7c00;
		} else if (flt == Float.NEGATIVE_INFINITY) {
			return (short) 0xfc00;
		} else if (flt == 0f) {
			return (short) 0x0000;
		} else if (flt == -0f) {
			return (short) 0x8000;
		} else if (flt > 65504f) {
			// 半精度浮点支持的最大值 / max value supported by half float
			return 0x7bff;
		} else if (flt < -65504f) {
			return (short) (0x7bff | 0x8000);
		} else if (flt > 0f && flt < 5.96046E-8f) {
			return 0x0001;
		} else if (flt < 0f && flt > -5.96046E-8f) {
			return (short) 0x8001;
		}
		int f = Float.floatToIntBits(flt);
		return (short) (((f >> 16) & 0x8000) | ((((f & 0x7f800000) - 0x38000000) >> 13) & 0x7c00)
				| ((f >> 13) & 0x03ff));
	}
}
