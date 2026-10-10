package com.aionemu.gameserver.geoEngine.math;

import com.aionemu.boot.i18n.I18n;

import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;

/**
 * 3×3 矩阵，用于旋转与线性变换；支持对象池复用。
 * 3×3 matrix for rotation and linear transforms; supports object-pool reuse.
 */
@Slf4j
@AllArgsConstructor
public final class Matrix3f implements Cloneable, Reusable {
	/** 对象工厂，用于矩阵实例池化。 / Object factory for pooling matrix instances. */
	private static final ObjectFactory<Object> FACTORY = new ObjectFactory<>() {

		public Object create() {
			return new Matrix3f();
		}
	};
	/** 第 0 行 0 列元素。 / Element at row 0, column 0. */
	float m00;
	/** 第 0 行 1 列元素。 / Element at row 0, column 1. */
	float m01;
	/** 第 0 行 2 列元素。 / Element at row 0, column 2. */
	float m02;
	/** 第 1 行 0 列元素。 / Element at row 1, column 0. */
	float m10;
	/** 第 1 行 1 列元素。 / Element at row 1, column 1. */
	float m11;
	/** 第 1 行 2 列元素。 / Element at row 1, column 2. */
	float m12;
	/** 第 2 行 0 列元素。 / Element at row 2, column 0. */
	float m20;
	/** 第 2 行 1 列元素。 / Element at row 2, column 1. */
	float m21;
	/** 第 2 行 2 列元素。 / Element at row 2, column 2. */
	float m22;

	/**
	 * 构造单位矩阵。
	 * Constructs an identity matrix.
	 */
	public Matrix3f() {
		this.loadIdentity();
	}

	/**
	 * 就地将每个元素取绝对值。
	 * Sets each element to its absolute value in place.
	 */
	public void absoluteLocal() {
		this.m00 = FastMath.abs(this.m00);
		this.m01 = FastMath.abs(this.m01);
		this.m02 = FastMath.abs(this.m02);
		this.m10 = FastMath.abs(this.m10);
		this.m11 = FastMath.abs(this.m11);
		this.m12 = FastMath.abs(this.m12);
		this.m20 = FastMath.abs(this.m20);
		this.m21 = FastMath.abs(this.m21);
		this.m22 = FastMath.abs(this.m22);
	}

	/**
	 * 将本矩阵设为与给定矩阵相同；若参数为 null 则设为单位矩阵。
	 * Sets this matrix equal to the given matrix; loads identity if null.
	 * @param matrix 源矩阵，可为 null / source matrix, may be null
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f set(Matrix3f matrix) {
		if (null == matrix) {
			this.loadIdentity();
		} else {
			this.m00 = matrix.m00;
			this.m01 = matrix.m01;
			this.m02 = matrix.m02;
			this.m10 = matrix.m10;
			this.m11 = matrix.m11;
			this.m12 = matrix.m12;
			this.m20 = matrix.m20;
			this.m21 = matrix.m21;
			this.m22 = matrix.m22;
		}
		return this;
	}

	/**
	 * 获取指定行列处的元素。
	 * Gets the element at the given row and column.
	 * @param i 行索引（0–2） / row index (0–2)
	 * @param j 列索引（0–2） / column index (0–2)
	 * @return 该位置的元素值 / element value at that position
	 * @throws IllegalArgumentException 索引越界时 / if indices are out of range.
	 */
	public float get(int i, int j) {
		switch (i) {
		case 0: {
			switch (j) {
			case 0: {
				return this.m00;
			}
			case 1: {
				return this.m01;
			}
			case 2: {
				return this.m02;
			}
			}
		}
		case 1: {
			switch (j) {
			case 0: {
				return this.m10;
			}
			case 1: {
				return this.m11;
			}
			case 2: {
				return this.m12;
			}
			}
		}
		case 2: {
			switch (j) {
			case 0: {
				return this.m20;
			}
			case 1: {
				return this.m21;
			}
			case 2: {
				return this.m22;
			}
			}
		}
		}
		log.warn(I18n.get("log.6ed00c7cd279"));
		throw new IllegalArgumentException("Invalid indices into matrix.");
	}

	/**
	 * 将矩阵元素写入浮点数组。
	 * Writes matrix elements into a float array.
	 * <p>
	 * 支持长度为 9 或 16 的数组；16 时仅填充左上 3×3 区域。
	 * Supports length 9 or 16; for 16 only the upper-left 3×3 is filled.
	 * @param data 目标数组，长度须为 9 或 16 / destination array of length 9 or 16
	 * @param rowMajor true 为行主序，false 为列主序 / true for row-major, false for column-major
	 * @throws IllegalArgumentException 数组长度非法时 / if array length is invalid.
	 */
	public void get(float[] data, boolean rowMajor) {
		if (data.length == 9) {
			if (rowMajor) {
				data[0] = this.m00;
				data[1] = this.m01;
				data[2] = this.m02;
				data[3] = this.m10;
				data[4] = this.m11;
				data[5] = this.m12;
				data[6] = this.m20;
				data[7] = this.m21;
				data[8] = this.m22;
			} else {
				data[0] = this.m00;
				data[1] = this.m10;
				data[2] = this.m20;
				data[3] = this.m01;
				data[4] = this.m11;
				data[5] = this.m21;
				data[6] = this.m02;
				data[7] = this.m12;
				data[8] = this.m22;
			}
		} else if (data.length == 16) {
			if (rowMajor) {
				data[0] = this.m00;
				data[1] = this.m01;
				data[2] = this.m02;
				data[4] = this.m10;
				data[5] = this.m11;
				data[6] = this.m12;
				data[8] = this.m20;
				data[9] = this.m21;
				data[10] = this.m22;
			} else {
				data[0] = this.m00;
				data[1] = this.m10;
				data[2] = this.m20;
				data[4] = this.m01;
				data[5] = this.m11;
				data[6] = this.m21;
				data[8] = this.m02;
				data[9] = this.m12;
				data[10] = this.m22;
			}
		} else {
			throw new IndexOutOfBoundsException("Array size must be 9 or 16 in Matrix3f.get().");
		}
	}

	/**
	 * 设置指定行列处的元素。
	 * Sets the element at the given row and column.
	 * @param i 行索引（0–2） / row index (0–2)
	 * @param j 列索引（0–2） / column index (0–2)
	 * @param value 新值 / new value
	 * @return 本矩阵（链式调用） / this matrix
	 * @throws IllegalArgumentException 索引越界时 / if indices are out of range.
	 */
	public Matrix3f set(int i, int j, float value) {
		switch (i) {
		case 0: {
			switch (j) {
			case 0: {
				this.m00 = value;
				return this;
			}
			case 1: {
				this.m01 = value;
				return this;
			}
			case 2: {
				this.m02 = value;
				return this;
			}
			}
		}
		case 1: {
			switch (j) {
			case 0: {
				this.m10 = value;
				return this;
			}
			case 1: {
				this.m11 = value;
				return this;
			}
			case 2: {
				this.m12 = value;
				return this;
			}
			}
		}
		case 2: {
			switch (j) {
			case 0: {
				this.m20 = value;
				return this;
			}
			case 1: {
				this.m21 = value;
				return this;
			}
			case 2: {
				this.m22 = value;
				return this;
			}
			}
		}
		}
		log.warn(I18n.get("log.6ed00c7cd279"));
		throw new IllegalArgumentException("Invalid indices into matrix.");
	}

	/**
	 * 从 3×3 二维数组设置矩阵元素（行主序）。
	 * Sets matrix elements from a 3×3 two-dimensional array (row-major).
	 * @param matrix 3×3 数组 / 3×3 array
	 * @return 本矩阵（链式调用） / this matrix
	 * @throws IllegalArgumentException 尺寸不为 3×3 时 / if dimensions are not 3×3
	 */
	public Matrix3f set(float[][] matrix) {
		if (matrix.length != 3 || matrix[0].length != 3) {
			throw new IllegalArgumentException("Array must be of size 9.");
		}
		this.m00 = matrix[0][0];
		this.m01 = matrix[0][1];
		this.m02 = matrix[0][2];
		this.m10 = matrix[1][0];
		this.m11 = matrix[1][1];
		this.m12 = matrix[1][2];
		this.m20 = matrix[2][0];
		this.m21 = matrix[2][1];
		this.m22 = matrix[2][2];
		return this;
	}

	/**
	 * 从长度为 9 的数组按行主序设置矩阵。
	 * Sets this matrix from a length-9 array in row-major order.
	 * @param matrix 长度 9 的数组 / array of length 9
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f set(float[] matrix) {
		return this.set(matrix, true);
	}

	/**
	 * 从长度为 9 的数组设置矩阵。
	 * Sets this matrix from a length-9 array.
	 * @param matrix 长度 9 的数组 / array of length 9
	 * @param rowMajor true 为行主序，false 为列主序 / true for row-major, false for column-major
	 * @return 本矩阵（链式调用） / this matrix
	 * @throws IllegalArgumentException 数组长度不为 9 时 / if array length is not 9.
	 */
	public Matrix3f set(float[] matrix, boolean rowMajor) {
		if (matrix.length != 9) {
			throw new IllegalArgumentException("Array must be of size 9.");
		}
		if (rowMajor) {
			this.m00 = matrix[0];
			this.m01 = matrix[1];
			this.m02 = matrix[2];
			this.m10 = matrix[3];
			this.m11 = matrix[4];
			this.m12 = matrix[5];
			this.m20 = matrix[6];
			this.m21 = matrix[7];
			this.m22 = matrix[8];
		} else {
			this.m00 = matrix[0];
			this.m01 = matrix[3];
			this.m02 = matrix[6];
			this.m10 = matrix[1];
			this.m11 = matrix[4];
			this.m12 = matrix[7];
			this.m20 = matrix[2];
			this.m21 = matrix[5];
			this.m22 = matrix[8];
		}
		return this;
	}

	/**
	 * 将矩阵设为单位矩阵。
	 * Loads the identity matrix.
	 */
	public void loadIdentity() {
		this.m21 = 0.0f;
		this.m20 = 0.0f;
		this.m12 = 0.0f;
		this.m10 = 0.0f;
		this.m02 = 0.0f;
		this.m01 = 0.0f;
		this.m22 = 1.0f;
		this.m11 = 1.0f;
		this.m00 = 1.0f;
	}

	/**
	 * 判断是否为单位矩阵（精确比较）。
	 * Returns whether this is exactly the identity matrix.
	 * @return 是单位矩阵则为 true / true if identity
	 */
	public boolean isIdentity() {
		return this.m00 == 1.0f && this.m01 == 0.0f && this.m02 == 0.0f && this.m10 == 0.0f && this.m11 == 1.0f
				&& this.m12 == 0.0f && this.m20 == 0.0f && this.m21 == 0.0f && this.m22 == 1.0f;
	}

	/**
	 * 本矩阵右乘 mat，返回新矩阵（不修改自身）。
	 * Right-multiplies this by mat and returns a new matrix (this is unchanged).
	 * @param mat 右乘矩阵 / matrix to multiply on the right
	 * @return 乘积矩阵 / product matrix
	 */
	public Matrix3f mult(Matrix3f mat) {
		return this.mult(mat, null);
	}

	/**
	 * 本矩阵右乘 mat，结果写入 product（可为 null 则新建）。
	 * Right-multiplies this by mat into product (allocates if null).
	 * @param mat 右乘矩阵 / matrix to multiply on the right
	 * @param product 结果存储，可为 null / result storage, may be null
	 * @return 乘积矩阵 / product matrix
	 */
	public Matrix3f mult(Matrix3f mat, Matrix3f product) {
		if (product == null) {
			product = new Matrix3f();
		}
		float temp00 = this.m00 * mat.m00 + this.m01 * mat.m10 + this.m02 * mat.m20;
		float temp01 = this.m00 * mat.m01 + this.m01 * mat.m11 + this.m02 * mat.m21;
		float temp02 = this.m00 * mat.m02 + this.m01 * mat.m12 + this.m02 * mat.m22;
		float temp10 = this.m10 * mat.m00 + this.m11 * mat.m10 + this.m12 * mat.m20;
		float temp11 = this.m10 * mat.m01 + this.m11 * mat.m11 + this.m12 * mat.m21;
		float temp12 = this.m10 * mat.m02 + this.m11 * mat.m12 + this.m12 * mat.m22;
		float temp20 = this.m20 * mat.m00 + this.m21 * mat.m10 + this.m22 * mat.m20;
		float temp21 = this.m20 * mat.m01 + this.m21 * mat.m11 + this.m22 * mat.m21;
		float temp22 = this.m20 * mat.m02 + this.m21 * mat.m12 + this.m22 * mat.m22;
		product.m00 = temp00;
		product.m01 = temp01;
		product.m02 = temp02;
		product.m10 = temp10;
		product.m11 = temp11;
		product.m12 = temp12;
		product.m20 = temp20;
		product.m21 = temp21;
		product.m22 = temp22;
		return product;
	}

	/**
	 * 用本矩阵变换向量，返回新向量。
	 * Transforms the vector by this matrix and returns a new vector.
	 * @param vec 待变换向量 / vector to transform
	 * @return 变换后的向量 / transformed vector
	 */
	public Vector3f mult(Vector3f vec) {
		return this.mult(vec, null);
	}

	/**
	 * 用本矩阵变换向量，结果写入 product（可为 null 则新建）。
	 * Transforms the vector by this matrix into product (allocates if null).
	 * @param vec 待变换向量 / vector to transform
	 * @param product 结果存储，可为 null / result storage, may be null
	 * @return 变换后的向量 / transformed vector
	 */
	public Vector3f mult(Vector3f vec, Vector3f product) {
		if (null == product) {
			product = new Vector3f();
		}
		float x = vec.x;
		float y = vec.y;
		float z = vec.z;
		product.x = this.m00 * x + this.m01 * y + this.m02 * z;
		product.y = this.m10 * x + this.m11 * y + this.m12 * z;
		product.z = this.m20 * x + this.m21 * y + this.m22 * z;
		return product;
	}

	/**
	 * 就地将本矩阵每个元素乘以标量。
	 * Multiplies every element of this matrix by the scalar in place.
	 * @param scale 标量 / scalar
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f multLocal(float scale) {
		this.m00 *= scale;
		this.m01 *= scale;
		this.m02 *= scale;
		this.m10 *= scale;
		this.m11 *= scale;
		this.m12 *= scale;
		this.m20 *= scale;
		this.m21 *= scale;
		this.m22 *= scale;
		return this;
	}

	/**
	 * 就地用本矩阵变换向量。
	 * Transforms the vector by this matrix in place.
	 * @param vec 待变换向量；为 null 时返回 null / vector to transform; returns null if null
	 * @return 变换后的同一向量 / the same vector after transform
	 */
	public Vector3f multLocal(Vector3f vec) {
		if (vec == null) {
			return null;
		}
		float x = vec.x;
		float y = vec.y;
		vec.x = this.m00 * x + this.m01 * y + this.m02 * vec.z;
		vec.y = this.m10 * x + this.m11 * y + this.m12 * vec.z;
		vec.z = this.m20 * x + this.m21 * y + this.m22 * vec.z;
		return vec;
	}

	/**
	 * 就地右乘 mat（this = this * mat）。
	 * Right-multiplies this by mat in place (this = this * mat).
	 * @param mat 右乘矩阵 / matrix to multiply on the right
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f multLocal(Matrix3f mat) {
		return this.mult(mat, this);
	}

	/**
	 * 将本矩阵所有元素置零。
	 * Sets all elements of this matrix to zero.
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f zero() {
		this.m22 = 0.0f;
		this.m21 = 0.0f;
		this.m20 = 0.0f;
		this.m12 = 0.0f;
		this.m11 = 0.0f;
		this.m10 = 0.0f;
		this.m02 = 0.0f;
		this.m01 = 0.0f;
		this.m00 = 0.0f;
		return this;
	}

	/**
	 * 就地将 mat 加到本矩阵（已弃用）。
	 * Adds mat to this matrix in place (deprecated).
	 * @param mat 加数矩阵 / matrix to add
	 * @deprecated 请使用显式加法 API / use an explicit addition API instead
	 */
	@Deprecated
	public void add(Matrix3f mat) {
		this.m00 += mat.m00;
		this.m01 += mat.m01;
		this.m02 += mat.m02;
		this.m10 += mat.m10;
		this.m11 += mat.m11;
		this.m12 += mat.m12;
		this.m20 += mat.m20;
		this.m21 += mat.m21;
		this.m22 += mat.m22;
	}

	/**
	 * 返回矩阵的可读字符串表示。
	 * Returns a human-readable string representation of this matrix.
	 * @return 多行字符串 / multi-line string
	 */
	public String toString() {
		return "Matrix3f\n[\n" + " " +
			this.m00 +
			"  " +
			this.m01 +
			"  " +
			this.m02 +
			" \n" +
			" " +
			this.m10 +
			"  " +
			this.m11 +
			"  " +
			this.m12 +
			" \n" +
			" " +
			this.m20 +
			"  " +
			this.m21 +
			"  " +
			this.m22 +
			" \n]";
	}

	/**
	 * 基于全部元素计算哈希码。
	 * Computes a hash code from all elements.
	 * @return 哈希码 / hash code
	 */
	public int hashCode() {
		int hash = 37;
		hash = 37 * hash + Float.floatToIntBits(this.m00);
		hash = 37 * hash + Float.floatToIntBits(this.m01);
		hash = 37 * hash + Float.floatToIntBits(this.m02);
		hash = 37 * hash + Float.floatToIntBits(this.m10);
		hash = 37 * hash + Float.floatToIntBits(this.m11);
		hash = 37 * hash + Float.floatToIntBits(this.m12);
		hash = 37 * hash + Float.floatToIntBits(this.m20);
		hash = 37 * hash + Float.floatToIntBits(this.m21);
		hash = 37 * hash + Float.floatToIntBits(this.m22);
		return hash;
	}

	/**
	 * 判断与另一对象是否元素完全相等。
	 * Returns whether the other object is a Matrix3f with identical elements.
	 * @param o 比较对象 / object to compare
	 * @return 元素全等则为 true / true if all elements match
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Matrix3f comp)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		if (Float.compare(this.m00, comp.m00) != 0) {
			return false;
		}
		if (Float.compare(this.m01, comp.m01) != 0) {
			return false;
		}
		if (Float.compare(this.m02, comp.m02) != 0) {
			return false;
		}
		if (Float.compare(this.m10, comp.m10) != 0) {
			return false;
		}
		if (Float.compare(this.m11, comp.m11) != 0) {
			return false;
		}
		if (Float.compare(this.m12, comp.m12) != 0) {
			return false;
		}
		if (Float.compare(this.m20, comp.m20) != 0) {
			return false;
		}
		if (Float.compare(this.m21, comp.m21) != 0) {
			return false;
		}
		return Float.compare(this.m22, comp.m22) == 0;
	}

	/**
	 * 按向量分量对矩阵列缩放（第 0 列 × scale.x，第 1 列 × scale.y，第 2 列 × scale.z）。
	 * Scales matrix columns by the vector components (col0 × scale.x, col1 × scale.y, col2 × scale.z).
	 * @param scale 各轴缩放因子 / per-axis scale factors
	 */
	public void scale(Vector3f scale) {
		this.m00 *= scale.x;
		this.m10 *= scale.x;
		this.m20 *= scale.x;
		this.m01 *= scale.y;
		this.m11 *= scale.y;
		this.m21 *= scale.y;
		this.m02 *= scale.z;
		this.m12 *= scale.z;
		this.m22 *= scale.z;
	}

	/**
	 * 浅克隆本矩阵。
	 * Returns a shallow clone of this matrix.
	 * @return 克隆实例 / cloned instance
	 */
	public Matrix3f clone() {
		try {
			return (Matrix3f) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * 重置为可复用状态（加载单位矩阵）。
	 * Resets to a reusable state (loads identity).
	 */
	public void reset() {
		this.loadIdentity();
	}

	/**
	 * 从对象池获取矩阵实例。
	 * Obtains a matrix instance from the object pool.
	 * @return 池化矩阵实例 / pooled matrix instance
	 */
	public static Matrix3f newInstance() {
		return (Matrix3f) FACTORY.object();
	}

	/**
	 * 将矩阵实例归还对象池。
	 * Recycles a matrix instance into the object pool.
	 * @param instance 待回收实例 / instance to recycle
	 */
	public static void recycle(Matrix3f instance) {
		FACTORY.recycle(instance);
	}
}
