package com.aionemu.gameserver.geoEngine.math;

import com.aionemu.boot.i18n.I18n;
import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.utils.BufferUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 3×3 矩阵，用于旋转与线性变换；支持对象池复用。
 * 3×3 matrix for rotation and linear transforms; supports object-pool reuse.
 */
@Slf4j
public final class Matrix3f implements Cloneable, Reusable {
	/** 对象工厂，用于矩阵实例池化。 / Object factory for pooling matrix instances. */
	private static final ObjectFactory<Object> FACTORY = new ObjectFactory<Object>() {

		public Object create() {
			return new Matrix3f();
		}
	};
	/** 第 0 行 0 列元素。 / Element at row 0, column 0. */
	protected float m00;
	/** 第 0 行 1 列元素。 / Element at row 0, column 1. */
	protected float m01;
	/** 第 0 行 2 列元素。 / Element at row 0, column 2. */
	protected float m02;
	/** 第 1 行 0 列元素。 / Element at row 1, column 0. */
	protected float m10;
	/** 第 1 行 1 列元素。 / Element at row 1, column 1. */
	protected float m11;
	/** 第 1 行 2 列元素。 / Element at row 1, column 2. */
	protected float m12;
	/** 第 2 行 0 列元素。 / Element at row 2, column 0. */
	protected float m20;
	/** 第 2 行 1 列元素。 / Element at row 2, column 1. */
	protected float m21;
	/** 第 2 行 2 列元素。 / Element at row 2, column 2. */
	protected float m22;

	/**
	 * 构造单位矩阵。
	 * Constructs an identity matrix.
	 */
	public Matrix3f() {
		this.loadIdentity();
	}

	/**
	 * 按给定 9 个元素构造矩阵（行主序）。
	 * Constructs a matrix from the given 9 elements (row-major).
	 *
	 * @param m00 第 0 行 0 列 / row 0 col 0
	 * @param m01 第 0 行 1 列 / row 0 col 1
	 * @param m02 第 0 行 2 列 / row 0 col 2
	 * @param m10 第 1 行 0 列 / row 1 col 0
	 * @param m11 第 1 行 1 列 / row 1 col 1
	 * @param m12 第 1 行 2 列 / row 1 col 2
	 * @param m20 第 2 行 0 列 / row 2 col 0
	 * @param m21 第 2 行 1 列 / row 2 col 1
	 * @param m22 第 2 行 2 列 / row 2 col 2
	 */
	public Matrix3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}

	/**
	 * 复制构造，从另一矩阵复制全部元素。
	 * Copy constructor; copies all elements from another matrix.
	 *
	 * @param mat 源矩阵 / source matrix
	 */
	public Matrix3f(Matrix3f mat) {
		this.set(mat);
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
	 *
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
	 *
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
	 *
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
	 * 获取指定列向量（新建存储）。
	 * Gets the specified column vector (allocates storage).
	 *
	 * @param i 列索引（0–2） / column index (0–2)
	 * @return 列向量 / the column vector
	 */
	public Vector3f getColumn(int i) {
		return this.getColumn(i, null);
	}

	/**
	 * 获取指定列向量，结果写入 store（可为 null 则新建）。
	 * Gets the specified column vector into store (allocates if null).
	 *
	 * @param i 列索引（0–2） / column index (0–2)
	 * @param store 结果存储，可为 null / result storage, may be null
	 * @return 列向量（store 或新建） / the column vector (store or new)
	 * @throws IllegalArgumentException 列索引非法时 / if column index is invalid.
	 */
	public Vector3f getColumn(int i, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		switch (i) {
		case 0: {
			store.x = this.m00;
			store.y = this.m10;
			store.z = this.m20;
			break;
		}
		case 1: {
			store.x = this.m01;
			store.y = this.m11;
			store.z = this.m21;
			break;
		}
		case 2: {
			store.x = this.m02;
			store.y = this.m12;
			store.z = this.m22;
			break;
		}
		default: {
			log.warn(I18n.get("log.885d8d676fff"));
			throw new IllegalArgumentException("Invalid column index. " + i);
		}
		}
		return store;
	}

	/**
	 * 获取指定行向量（新建存储）。
	 * Gets the specified row vector (allocates storage).
	 *
	 * @param i 行索引（0–2） / row index (0–2)
	 * @return 行向量 / the row vector
	 */
	public Vector3f getRow(int i) {
		return this.getRow(i, null);
	}

	/**
	 * 获取指定行向量，结果写入 store（可为 null 则新建）。
	 * Gets the specified row vector into store (allocates if null).
	 *
	 * @param i 行索引（0–2） / row index (0–2)
	 * @param store 结果存储，可为 null / result storage, may be null
	 * @return 行向量（store 或新建） / the row vector (store or new)
	 * @throws IllegalArgumentException 行索引非法时 / if row index is invalid.
	 */
	public Vector3f getRow(int i, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		switch (i) {
		case 0: {
			store.x = this.m00;
			store.y = this.m01;
			store.z = this.m02;
			break;
		}
		case 1: {
			store.x = this.m10;
			store.y = this.m11;
			store.z = this.m12;
			break;
		}
		case 2: {
			store.x = this.m20;
			store.y = this.m21;
			store.z = this.m22;
			break;
		}
		default: {
			log.warn(I18n.get("log.c9cf152b9e07"));
			throw new IllegalArgumentException("Invalid row index. " + i);
		}
		}
		return store;
	}

	/**
	 * 将矩阵以行主序写入新的 FloatBuffer。
	 * Writes this matrix in row-major order into a new FloatBuffer.
	 *
	 * @return 含 9 个浮点的缓冲区（position 已 rewind） / buffer of 9 floats (rewound)
	 */
	public FloatBuffer toFloatBuffer() {
		FloatBuffer fb = BufferUtils.createFloatBuffer(9);
		fb.put(this.m00).put(this.m01).put(this.m02);
		fb.put(this.m10).put(this.m11).put(this.m12);
		fb.put(this.m20).put(this.m21).put(this.m22);
		fb.rewind();
		return fb;
	}

	/**
	 * 将矩阵元素追加写入给定 FloatBuffer。
	 * Appends matrix elements into the given FloatBuffer.
	 *
	 * @param fb 目标缓冲区 / destination buffer
	 * @param columnMajor true 为列主序，false 为行主序 / true for column-major, false for row-major
	 * @return 传入的缓冲区 / the given buffer
	 */
	public FloatBuffer fillFloatBuffer(FloatBuffer fb, boolean columnMajor) {
		if (columnMajor) {
			fb.put(this.m00).put(this.m10).put(this.m20);
			fb.put(this.m01).put(this.m11).put(this.m21);
			fb.put(this.m02).put(this.m12).put(this.m22);
		} else {
			fb.put(this.m00).put(this.m01).put(this.m02);
			fb.put(this.m10).put(this.m11).put(this.m12);
			fb.put(this.m20).put(this.m21).put(this.m22);
		}
		return fb;
	}

	/**
	 * 设置指定列为给定向量。
	 * Sets the specified column from the given vector.
	 *
	 * @param i 列索引（0–2） / column index (0–2)
	 * @param column 列向量；为 null 时忽略 / column vector; ignored if null
	 * @return 本矩阵（链式调用） / this matrix
	 * @throws IllegalArgumentException 列索引非法时 / if column index is invalid
	 */
	public Matrix3f setColumn(int i, Vector3f column) {
		if (column == null) {
			log.warn(I18n.get("log.df539499d2b4"));
			return this;
		}
		switch (i) {
		case 0: {
			this.m00 = column.x;
			this.m10 = column.y;
			this.m20 = column.z;
			break;
		}
		case 1: {
			this.m01 = column.x;
			this.m11 = column.y;
			this.m21 = column.z;
			break;
		}
		case 2: {
			this.m02 = column.x;
			this.m12 = column.y;
			this.m22 = column.z;
			break;
		}
		default: {
			log.warn(I18n.get("log.885d8d676fff"));
			throw new IllegalArgumentException("Invalid column index. " + i);
		}
		}
		return this;
	}

	/**
	 * 设置指定行为给定向量。
	 * Sets the specified row from the given vector.
	 *
	 * @param i 行索引（0–2） / row index (0–2)
	 * @param row 行向量；为 null 时忽略 / row vector; ignored if null
	 * @return 本矩阵（链式调用） / this matrix
	 * @throws IllegalArgumentException 行索引非法时 / if row index is invalid
	 */
	public Matrix3f setRow(int i, Vector3f row) {
		if (row == null) {
			log.warn(I18n.get("log.78c7a0feb6f0"));
			return this;
		}
		switch (i) {
		case 0: {
			this.m00 = row.x;
			this.m01 = row.y;
			this.m02 = row.z;
			break;
		}
		case 1: {
			this.m10 = row.x;
			this.m11 = row.y;
			this.m12 = row.z;
			break;
		}
		case 2: {
			this.m20 = row.x;
			this.m21 = row.y;
			this.m22 = row.z;
			break;
		}
		default: {
			log.warn(I18n.get("log.c9cf152b9e07"));
			throw new IllegalArgumentException("Invalid row index. " + i);
		}
		}
		return this;
	}

	/**
	 * 设置指定行列处的元素。
	 * Sets the element at the given row and column.
	 *
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
	 *
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
	 * 由三个正交基轴构造旋转矩阵（列分别为 u、v、w）。
	 * Builds a rotation matrix from three orthonormal axes (columns u, v, w).
	 *
	 * @param uAxis U 轴 / U axis
	 * @param vAxis V 轴 / V axis
	 * @param wAxis W 轴 / W axis
	 */
	public void fromAxes(Vector3f uAxis, Vector3f vAxis, Vector3f wAxis) {
		this.m00 = uAxis.x;
		this.m10 = uAxis.y;
		this.m20 = uAxis.z;
		this.m01 = vAxis.x;
		this.m11 = vAxis.y;
		this.m21 = vAxis.z;
		this.m02 = wAxis.x;
		this.m12 = wAxis.y;
		this.m22 = wAxis.z;
	}

	/**
	 * 从长度为 9 的数组按行主序设置矩阵。
	 * Sets this matrix from a length-9 array in row-major order.
	 *
	 * @param matrix 长度 9 的数组 / array of length 9
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f set(float[] matrix) {
		return this.set(matrix, true);
	}

	/**
	 * 从长度为 9 的数组设置矩阵。
	 * Sets this matrix from a length-9 array.
	 *
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
	 *
	 * @return 是单位矩阵则为 true / true if identity
	 */
	public boolean isIdentity() {
		return this.m00 == 1.0f && this.m01 == 0.0f && this.m02 == 0.0f && this.m10 == 0.0f && this.m11 == 1.0f
				&& this.m12 == 0.0f && this.m20 == 0.0f && this.m21 == 0.0f && this.m22 == 1.0f;
	}

	/**
	 * 由旋转角与轴构造旋转矩阵（轴会先归一化）。
	 * Builds a rotation matrix from an angle and axis (axis is normalized first).
	 *
	 * @param angle 旋转角（弧度） / rotation angle in radians
	 * @param axis 旋转轴 / rotation axis
	 */
	public void fromAngleAxis(float angle, Vector3f axis) {
		Vector3f normAxis = axis.normalize();
		this.fromAngleNormalAxis(angle, normAxis);
	}

	/**
	 * 由旋转角与已归一化轴构造旋转矩阵（Rodrigues 公式）。
	 * Builds a rotation matrix from an angle and a unit axis (Rodrigues' formula).
	 *
	 * @param angle 旋转角（弧度） / rotation angle in radians
	 * @param axis 已归一化的旋转轴 / normalized rotation axis
	 */
	public void fromAngleNormalAxis(float angle, Vector3f axis) {
		float fCos = FastMath.cos(angle);
		float fSin = FastMath.sin(angle);
		float fOneMinusCos = 1.0f - fCos;
		float fX2 = axis.x * axis.x;
		float fY2 = axis.y * axis.y;
		float fZ2 = axis.z * axis.z;
		float fXYM = axis.x * axis.y * fOneMinusCos;
		float fXZM = axis.x * axis.z * fOneMinusCos;
		float fYZM = axis.y * axis.z * fOneMinusCos;
		float fXSin = axis.x * fSin;
		float fYSin = axis.y * fSin;
		float fZSin = axis.z * fSin;
		this.m00 = fX2 * fOneMinusCos + fCos;
		this.m01 = fXYM - fZSin;
		this.m02 = fXZM + fYSin;
		this.m10 = fXYM + fZSin;
		this.m11 = fY2 * fOneMinusCos + fCos;
		this.m12 = fYZM - fXSin;
		this.m20 = fXZM - fYSin;
		this.m21 = fYZM + fXSin;
		this.m22 = fZ2 * fOneMinusCos + fCos;
	}

	/**
	 * 本矩阵右乘 mat，返回新矩阵（不修改自身）。
	 * Right-multiplies this by mat and returns a new matrix (this is unchanged).
	 *
	 * @param mat 右乘矩阵 / matrix to multiply on the right
	 * @return 乘积矩阵 / product matrix
	 */
	public Matrix3f mult(Matrix3f mat) {
		return this.mult(mat, null);
	}

	/**
	 * 本矩阵右乘 mat，结果写入 product（可为 null 则新建）。
	 * Right-multiplies this by mat into product (allocates if null).
	 *
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
	 *
	 * @param vec 待变换向量 / vector to transform
	 * @return 变换后的向量 / transformed vector
	 */
	public Vector3f mult(Vector3f vec) {
		return this.mult(vec, null);
	}

	/**
	 * 用本矩阵变换向量，结果写入 product（可为 null 则新建）。
	 * Transforms the vector by this matrix into product (allocates if null).
	 *
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
	 *
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
	 *
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
	 *
	 * @param mat 右乘矩阵 / matrix to multiply on the right
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f multLocal(Matrix3f mat) {
		return this.mult(mat, this);
	}

	/**
	 * 就地转置本矩阵。
	 * Transposes this matrix in place.
	 *
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f transposeLocal() {
		float tmp = this.m01;
		this.m01 = this.m10;
		this.m10 = tmp;
		tmp = this.m02;
		this.m02 = this.m20;
		this.m20 = tmp;
		tmp = this.m12;
		this.m12 = this.m21;
		this.m21 = tmp;
		return this;
	}

	/**
	 * 求逆矩阵，返回新矩阵（不修改自身）；奇异时返回零矩阵。
	 * Returns the inverse as a new matrix (this unchanged); returns zero if singular.
	 *
	 * @return 逆矩阵 / inverse matrix
	 */
	public Matrix3f invert() {
		return this.invert(null);
	}

	/**
	 * 求逆矩阵，结果写入 store（可为 null 则新建）；奇异时 store 置零。
	 * Inverts into store (allocates if null); zeros store if singular.
	 *
	 * @param store 结果存储，可为 null / result storage, may be null
	 * @return 逆矩阵 / inverse matrix
	 */
	public Matrix3f invert(Matrix3f store) {
		float det;
		if (store == null) {
			store = new Matrix3f();
		}
		if (FastMath.abs(det = this.determinant()) <= 1.1920929E-7f) {
			return store.zero();
		}
		store.m00 = this.m11 * this.m22 - this.m12 * this.m21;
		store.m01 = this.m02 * this.m21 - this.m01 * this.m22;
		store.m02 = this.m01 * this.m12 - this.m02 * this.m11;
		store.m10 = this.m12 * this.m20 - this.m10 * this.m22;
		store.m11 = this.m00 * this.m22 - this.m02 * this.m20;
		store.m12 = this.m02 * this.m10 - this.m00 * this.m12;
		store.m20 = this.m10 * this.m21 - this.m11 * this.m20;
		store.m21 = this.m01 * this.m20 - this.m00 * this.m21;
		store.m22 = this.m00 * this.m11 - this.m01 * this.m10;
		store.multLocal(1.0f / det);
		return store;
	}

	/**
	 * 就地求逆；奇异时置为零矩阵。
	 * Inverts this matrix in place; zeros if singular.
	 *
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f invertLocal() {
		float det = this.determinant();
		if (FastMath.abs(det) <= 1.1920929E-7f) {
			return this.zero();
		}
		float f00 = this.m11 * this.m22 - this.m12 * this.m21;
		float f01 = this.m02 * this.m21 - this.m01 * this.m22;
		float f02 = this.m01 * this.m12 - this.m02 * this.m11;
		float f10 = this.m12 * this.m20 - this.m10 * this.m22;
		float f11 = this.m00 * this.m22 - this.m02 * this.m20;
		float f12 = this.m02 * this.m10 - this.m00 * this.m12;
		float f20 = this.m10 * this.m21 - this.m11 * this.m20;
		float f21 = this.m01 * this.m20 - this.m00 * this.m21;
		float f22 = this.m00 * this.m11 - this.m01 * this.m10;
		this.m00 = f00;
		this.m01 = f01;
		this.m02 = f02;
		this.m10 = f10;
		this.m11 = f11;
		this.m12 = f12;
		this.m20 = f20;
		this.m21 = f21;
		this.m22 = f22;
		this.multLocal(1.0f / det);
		return this;
	}

	/**
	 * 求伴随矩阵，返回新矩阵。
	 * Returns the adjugate (classical adjoint) as a new matrix.
	 *
	 * @return 伴随矩阵 / adjugate matrix
	 */
	public Matrix3f adjoint() {
		return this.adjoint(null);
	}

	/**
	 * 求伴随矩阵，结果写入 store（可为 null 则新建）。
	 * Computes the adjugate into store (allocates if null).
	 *
	 * @param store 结果存储，可为 null / result storage, may be null
	 * @return 伴随矩阵 / adjugate matrix
	 */
	public Matrix3f adjoint(Matrix3f store) {
		if (store == null) {
			store = new Matrix3f();
		}
		store.m00 = this.m11 * this.m22 - this.m12 * this.m21;
		store.m01 = this.m02 * this.m21 - this.m01 * this.m22;
		store.m02 = this.m01 * this.m12 - this.m02 * this.m11;
		store.m10 = this.m12 * this.m20 - this.m10 * this.m22;
		store.m11 = this.m00 * this.m22 - this.m02 * this.m20;
		store.m12 = this.m02 * this.m10 - this.m00 * this.m12;
		store.m20 = this.m10 * this.m21 - this.m11 * this.m20;
		store.m21 = this.m01 * this.m20 - this.m00 * this.m21;
		store.m22 = this.m00 * this.m11 - this.m01 * this.m10;
		return store;
	}

	/**
	 * 计算行列式。
	 * Computes the determinant.
	 *
	 * @return 行列式值 / determinant value
	 */
	public float determinant() {
		float fCo00 = this.m11 * this.m22 - this.m12 * this.m21;
		float fCo10 = this.m12 * this.m20 - this.m10 * this.m22;
		float fCo20 = this.m10 * this.m21 - this.m11 * this.m20;
		float fDet = this.m00 * fCo00 + this.m01 * fCo10 + this.m02 * fCo20;
		return fDet;
	}

	/**
	 * 将本矩阵所有元素置零。
	 * Sets all elements of this matrix to zero.
	 *
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
	 *
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
	 * 转置本矩阵（就地，与 {@link #transposeLocal()} 相同）。
	 * Transposes this matrix in place (same as {@link #transposeLocal()}).
	 *
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix3f transpose() {
		return this.transposeLocal();
	}

	/**
	 * 返回转置后的新矩阵（不修改自身）。
	 * Returns a new transposed matrix (this is unchanged).
	 *
	 * @return 转置矩阵 / transposed matrix
	 */
	public Matrix3f transposeNew() {
		Matrix3f ret = new Matrix3f(this.m00, this.m10, this.m20, this.m01, this.m11, this.m21, this.m02, this.m12,
				this.m22);
		return ret;
	}

	/**
	 * 返回矩阵的可读字符串表示。
	 * Returns a human-readable string representation of this matrix.
	 *
	 * @return 多行字符串 / multi-line string
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("Matrix3f\n[\n");
		result.append(" ");
		result.append(this.m00);
		result.append("  ");
		result.append(this.m01);
		result.append("  ");
		result.append(this.m02);
		result.append(" \n");
		result.append(" ");
		result.append(this.m10);
		result.append("  ");
		result.append(this.m11);
		result.append("  ");
		result.append(this.m12);
		result.append(" \n");
		result.append(" ");
		result.append(this.m20);
		result.append("  ");
		result.append(this.m21);
		result.append("  ");
		result.append(this.m22);
		result.append(" \n]");
		return result.toString();
	}

	/**
	 * 基于全部元素计算哈希码。
	 * Computes a hash code from all elements.
	 *
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
	 *
	 * @param o 比较对象 / object to compare
	 * @return 元素全等则为 true / true if all elements match
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Matrix3f) || o == null) {
			return false;
		}
		if (this == o) {
			return true;
		}
		Matrix3f comp = (Matrix3f) o;
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
	 * 返回运行时类型标签（本类或其子类）。
	 * Returns the runtime class tag (this class or a subclass).
	 *
	 * @return 类对象 / class object
	 */
	public Class<? extends Matrix3f> getClassTag() {
		return this.getClass();
	}

	/**
	 * 由起始向量与目标向量构造将 start 旋转到 end 的旋转矩阵。
	 * Builds a rotation matrix that rotates start onto end.
	 *
	 * @param start 起始方向 / start direction
	 * @param end 目标方向 / end direction
	 */
	public void fromStartEndVectors(Vector3f start, Vector3f end) {
		float f;
		Vector3f v = new Vector3f();
		start.cross(end, v);
		float e = start.dot(end);
		float f2 = f = e < 0.0f ? -e : e;
		if (f > 0.9999f) {
			Vector3f u = new Vector3f();
			Vector3f x = new Vector3f();
			x.x = (double) start.x > 0.0 ? start.x : -start.x;
			x.y = (double) start.y > 0.0 ? start.y : -start.y;
			float f3 = x.z = (double) start.z > 0.0 ? start.z : -start.z;
			if (x.x < x.y) {
				if (x.x < x.z) {
					x.x = 1.0f;
					x.z = 0.0f;
					x.y = 0.0f;
				} else {
					x.z = 1.0f;
					x.y = 0.0f;
					x.x = 0.0f;
				}
			} else if (x.y < x.z) {
				x.y = 1.0f;
				x.z = 0.0f;
				x.x = 0.0f;
			} else {
				x.z = 1.0f;
				x.y = 0.0f;
				x.x = 0.0f;
			}
			u.x = x.x - start.x;
			u.y = x.y - start.y;
			u.z = x.z - start.z;
			v.x = x.x - end.x;
			v.y = x.y - end.y;
			v.z = x.z - end.z;
			float c1 = 2.0f / u.dot(u);
			float c2 = 2.0f / v.dot(v);
			float c3 = c1 * c2 * u.dot(v);
			for (int i = 0; i < 3; ++i) {
				float val;
				for (int j = 0; j < 3; ++j) {
					val = -c1 * u.get(i) * u.get(j) - c2 * v.get(i) * v.get(j) + c3 * v.get(i) * u.get(j);
					this.set(i, j, val);
				}
				val = this.get(i, i);
				this.set(i, i, val + 1.0f);
			}
		} else {
			float h = 1.0f / (1.0f + e);
			float hvx = h * v.x;
			float hvz = h * v.z;
			float hvxy = hvx * v.y;
			float hvxz = hvx * v.z;
			float hvyz = hvz * v.y;
			this.set(0, 0, e + hvx * v.x);
			this.set(0, 1, hvxy - v.z);
			this.set(0, 2, hvxz + v.y);
			this.set(1, 0, hvxy + v.z);
			this.set(1, 1, e + h * v.y * v.y);
			this.set(1, 2, hvyz - v.x);
			this.set(2, 0, hvxz - v.y);
			this.set(2, 1, hvyz + v.x);
			this.set(2, 2, e + hvz * v.z);
		}
	}

	/**
	 * 按向量分量对矩阵列缩放（第 0 列 × scale.x，第 1 列 × scale.y，第 2 列 × scale.z）。
	 * Scales matrix columns by the vector components (col0 × scale.x, col1 × scale.y, col2 × scale.z).
	 *
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
	 * 容差判断矩阵是否近似为单位矩阵。
	 * Returns whether the matrix is approximately the identity within tolerance.
	 *
	 * @param mat 待检测矩阵 / matrix to test
	 * @return 近似单位矩阵则为 true / true if approximately identity
	 */
	static final boolean equalIdentity(Matrix3f mat) {
		if ((double) Math.abs(mat.m00 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m11 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m22 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m01) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m02) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m10) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m12) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m20) > 1.0E-4) {
			return false;
		}
		return !((double) Math.abs(mat.m21) > 1.0E-4);
	}

	/**
	 * 浅克隆本矩阵。
	 * Returns a shallow clone of this matrix.
	 *
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
	 *
	 * @return 池化矩阵实例 / pooled matrix instance
	 */
	public static Matrix3f newInstance() {
		return (Matrix3f) FACTORY.object();
	}

	/**
	 * 将矩阵实例归还对象池。
	 * Recycles a matrix instance into the object pool.
	 *
	 * @param instance 待回收实例 / instance to recycle
	 */
	public static void recycle(Matrix3f instance) {
		FACTORY.recycle((Object) instance);
	}
}
