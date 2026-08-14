package com.aionemu.gameserver.geoEngine.math;


import com.aionemu.boot.i18n.I18n;
import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.utils.BufferUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 4×4 矩阵，用于仿射/投影变换。
 * 4×4 matrix for affine/projective transforms.
 */
@Slf4j
public final class Matrix4f implements Cloneable {
	/** 第 0 行 0 列元素。 / Element at row 0, column 0. */
	public float m00;
	/** 第 0 行 1 列元素。 / Element at row 0, column 1. */
	public float m01;
	/** 第 0 行 2 列元素。 / Element at row 0, column 2. */
	public float m02;
	/** 第 0 行 3 列元素。 / Element at row 0, column 3. */
	public float m03;
	/** 第 1 行 0 列元素。 / Element at row 1, column 0. */
	public float m10;
	/** 第 1 行 1 列元素。 / Element at row 1, column 1. */
	public float m11;
	/** 第 1 行 2 列元素。 / Element at row 1, column 2. */
	public float m12;
	/** 第 1 行 3 列元素。 / Element at row 1, column 3. */
	public float m13;
	/** 第 2 行 0 列元素。 / Element at row 2, column 0. */
	public float m20;
	/** 第 2 行 1 列元素。 / Element at row 2, column 1. */
	public float m21;
	/** 第 2 行 2 列元素。 / Element at row 2, column 2. */
	public float m22;
	/** 第 2 行 3 列元素。 / Element at row 2, column 3. */
	public float m23;
	/** 第 3 行 0 列元素。 / Element at row 3, column 0. */
	public float m30;
	/** 第 3 行 1 列元素。 / Element at row 3, column 1. */
	public float m31;
	/** 第 3 行 2 列元素。 / Element at row 3, column 2. */
	public float m32;
	/** 第 3 行 3 列元素。 / Element at row 3, column 3. */
	public float m33;
	/** 单位矩阵常量。 / Identity matrix constant. */
	public static final Matrix4f IDENTITY = new Matrix4f();

	/**
	 * 构造单位矩阵。
	 * Constructs an identity matrix.
	 */
	public Matrix4f() {
		this.loadIdentity();
	}

	/**
	 * 按 16 个元素构造矩阵（行优先 m00..m33）。
	 * Constructs a matrix from 16 elements (row-major m00..m33).
	 *
	 * @param m00 第 0 行 0 列 / row 0 col 0
	 * @param m01 第 0 行 1 列 / row 0 col 1
	 * @param m02 第 0 行 2 列 / row 0 col 2
	 * @param m03 第 0 行 3 列 / row 0 col 3
	 * @param m10 第 1 行 0 列 / row 1 col 0
	 * @param m11 第 1 行 1 列 / row 1 col 1
	 * @param m12 第 1 行 2 列 / row 1 col 2
	 * @param m13 第 1 行 3 列 / row 1 col 3
	 * @param m20 第 2 行 0 列 / row 2 col 0
	 * @param m21 第 2 行 1 列 / row 2 col 1
	 * @param m22 第 2 行 2 列 / row 2 col 2
	 * @param m23 第 2 行 3 列 / row 2 col 3
	 * @param m30 第 3 行 0 列 / row 3 col 0
	 * @param m31 第 3 行 1 列 / row 3 col 1
	 * @param m32 第 3 行 2 列 / row 3 col 2
	 * @param m33 第 3 行 3 列 / row 3 col 3
	 */
	public Matrix4f(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20,
			float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;
		this.m30 = m30;
		this.m31 = m31;
		this.m32 = m32;
		this.m33 = m33;
	}

	/**
	 * 从 float 数组构造矩阵（列优先）。
	 * Constructs a matrix from a float array (column-major).
	 *
	 * @param array 长度 16 的元素数组 / element array of length 16
	 */
	public Matrix4f(float[] array) {
		this.set(array, false);
	}

	/**
	 * 拷贝构造。
	 * Copy constructor.
	 *
	 * @param mat 源矩阵 / source matrix
	 */
	public Matrix4f(Matrix4f mat) {
		this.copy(mat);
	}

	/**
	 * 从另一矩阵拷贝元素；null 时加载单位矩阵。
	 * Copies elements from another matrix; loads identity if null.
	 *
	 * @param matrix 源矩阵 / source matrix
	 */
	public void copy(Matrix4f matrix) {
		if (null == matrix) {
			this.loadIdentity();
		} else {
			this.m00 = matrix.m00;
			this.m01 = matrix.m01;
			this.m02 = matrix.m02;
			this.m03 = matrix.m03;
			this.m10 = matrix.m10;
			this.m11 = matrix.m11;
			this.m12 = matrix.m12;
			this.m13 = matrix.m13;
			this.m20 = matrix.m20;
			this.m21 = matrix.m21;
			this.m22 = matrix.m22;
			this.m23 = matrix.m23;
			this.m30 = matrix.m30;
			this.m31 = matrix.m31;
			this.m32 = matrix.m32;
			this.m33 = matrix.m33;
		}
	}

	/**
	 * 将矩阵元素写入 float 数组（行优先）。
	 * Writes matrix elements into a float array (row-major).
	 *
	 * @param matrix 目标数组，长度须为 16 / destination array of length 16
	 */
	public void get(float[] matrix) {
		this.get(matrix, true);
	}

	/**
	 * 将矩阵元素写入 float 数组。
	 * Writes matrix elements into a float array.
	 *
	 * @param matrix 目标数组，长度须为 16 / destination array of length 16
	 * @param rowMajor true 为行优先，false 为列优先 / true for row-major, false for column-major
	 */
	public void get(float[] matrix, boolean rowMajor) {
		if (matrix.length != 16) {
			throw new IllegalArgumentException("Array must be of size 16.");
		}
		if (rowMajor) {
			matrix[0] = this.m00;
			matrix[1] = this.m01;
			matrix[2] = this.m02;
			matrix[3] = this.m03;
			matrix[4] = this.m10;
			matrix[5] = this.m11;
			matrix[6] = this.m12;
			matrix[7] = this.m13;
			matrix[8] = this.m20;
			matrix[9] = this.m21;
			matrix[10] = this.m22;
			matrix[11] = this.m23;
			matrix[12] = this.m30;
			matrix[13] = this.m31;
			matrix[14] = this.m32;
			matrix[15] = this.m33;
		} else {
			matrix[0] = this.m00;
			matrix[4] = this.m01;
			matrix[8] = this.m02;
			matrix[12] = this.m03;
			matrix[1] = this.m10;
			matrix[5] = this.m11;
			matrix[9] = this.m12;
			matrix[13] = this.m13;
			matrix[2] = this.m20;
			matrix[6] = this.m21;
			matrix[10] = this.m22;
			matrix[14] = this.m23;
			matrix[3] = this.m30;
			matrix[7] = this.m31;
			matrix[11] = this.m32;
			matrix[15] = this.m33;
		}
	}

	/**
	 * 获取指定行列的元素。
	 * Returns the element at the given row and column.
	 *
	 * @param i 行索引（0–3） / row index (0–3)
	 * @param j 列索引（0–3） / column index (0–3)
	 * @return 矩阵元素 / matrix element
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
			case 3: {
				return this.m03;
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
			case 3: {
				return this.m13;
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
			case 3: {
				return this.m23;
			}
			}
		}
		case 3: {
			switch (j) {
			case 0: {
				return this.m30;
			}
			case 1: {
				return this.m31;
			}
			case 2: {
				return this.m32;
			}
			case 3: {
				return this.m33;
			}
			}
		}
		}
		log.warn(I18n.get("log.6ed00c7cd279"));
		throw new IllegalArgumentException("Invalid indices into matrix.");
	}

	/**
	 * 获取指定列，分配新数组。
	 * Returns the specified column, allocating a new array.
	 *
	 * @param i 列索引（0–3） / column index (0–3)
	 * @return 列向量（长度 4） / column vector of length 4
	 */
	public float[] getColumn(int i) {
		return this.getColumn(i, null);
	}

	/**
	 * 获取指定列，写入 store。
	 * Returns the specified column into store.
	 *
	 * @param i 列索引（0–3） / column index (0–3)
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return 列向量 / column vector
	 */
	public float[] getColumn(int i, float[] store) {
		if (store == null) {
			store = new float[4];
		}
		switch (i) {
		case 0: {
			store[0] = this.m00;
			store[1] = this.m10;
			store[2] = this.m20;
			store[3] = this.m30;
			break;
		}
		case 1: {
			store[0] = this.m01;
			store[1] = this.m11;
			store[2] = this.m21;
			store[3] = this.m31;
			break;
		}
		case 2: {
			store[0] = this.m02;
			store[1] = this.m12;
			store[2] = this.m22;
			store[3] = this.m32;
			break;
		}
		case 3: {
			store[0] = this.m03;
			store[1] = this.m13;
			store[2] = this.m23;
			store[3] = this.m33;
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
	 * 设置指定列。
	 * Sets the specified column.
	 *
	 * @param i 列索引（0–3） / column index (0–3)
	 * @param column 列向量（长度 4；null 时警告并返回） / column vector of length 4 (null warns and returns)
	 */
	public void setColumn(int i, float[] column) {
		if (column == null) {
			log.warn(I18n.get("log.df539499d2b4"));
			return;
		}
		switch (i) {
		case 0: {
			this.m00 = column[0];
			this.m10 = column[1];
			this.m20 = column[2];
			this.m30 = column[3];
			break;
		}
		case 1: {
			this.m01 = column[0];
			this.m11 = column[1];
			this.m21 = column[2];
			this.m31 = column[3];
			break;
		}
		case 2: {
			this.m02 = column[0];
			this.m12 = column[1];
			this.m22 = column[2];
			this.m32 = column[3];
			break;
		}
		case 3: {
			this.m03 = column[0];
			this.m13 = column[1];
			this.m23 = column[2];
			this.m33 = column[3];
			break;
		}
		default: {
			log.warn(I18n.get("log.885d8d676fff"));
			throw new IllegalArgumentException("Invalid column index. " + i);
		}
		}
	}

	/**
	 * 设置指定行列的元素。
	 * Sets the element at the given row and column.
	 *
	 * @param i 行索引（0–3） / row index (0–3)
	 * @param j 列索引（0–3） / column index (0–3)
	 * @param value 新值 / new value
	 */
	public void set(int i, int j, float value) {
		switch (i) {
		case 0: {
			switch (j) {
			case 0: {
				this.m00 = value;
				return;
			}
			case 1: {
				this.m01 = value;
				return;
			}
			case 2: {
				this.m02 = value;
				return;
			}
			case 3: {
				this.m03 = value;
				return;
			}
			}
		}
		case 1: {
			switch (j) {
			case 0: {
				this.m10 = value;
				return;
			}
			case 1: {
				this.m11 = value;
				return;
			}
			case 2: {
				this.m12 = value;
				return;
			}
			case 3: {
				this.m13 = value;
				return;
			}
			}
		}
		case 2: {
			switch (j) {
			case 0: {
				this.m20 = value;
				return;
			}
			case 1: {
				this.m21 = value;
				return;
			}
			case 2: {
				this.m22 = value;
				return;
			}
			case 3: {
				this.m23 = value;
				return;
			}
			}
		}
		case 3: {
			switch (j) {
			case 0: {
				this.m30 = value;
				return;
			}
			case 1: {
				this.m31 = value;
				return;
			}
			case 2: {
				this.m32 = value;
				return;
			}
			case 3: {
				this.m33 = value;
				return;
			}
			}
		}
		}
		log.warn(I18n.get("log.6ed00c7cd279"));
		throw new IllegalArgumentException("Invalid indices into matrix.");
	}

	/**
	 * 从 4×4 二维数组设置矩阵。
	 * Sets the matrix from a 4×4 two-dimensional array.
	 *
	 * @param matrix 4×4 源数组 / 4×4 source array
	 */
	public void set(float[][] matrix) {
		if (matrix.length != 4 || matrix[0].length != 4) {
			throw new IllegalArgumentException("Array must be of size 16.");
		}
		this.m00 = matrix[0][0];
		this.m01 = matrix[0][1];
		this.m02 = matrix[0][2];
		this.m03 = matrix[0][3];
		this.m10 = matrix[1][0];
		this.m11 = matrix[1][1];
		this.m12 = matrix[1][2];
		this.m13 = matrix[1][3];
		this.m20 = matrix[2][0];
		this.m21 = matrix[2][1];
		this.m22 = matrix[2][2];
		this.m23 = matrix[2][3];
		this.m30 = matrix[3][0];
		this.m31 = matrix[3][1];
		this.m32 = matrix[3][2];
		this.m33 = matrix[3][3];
	}

	/**
	 * 从另一矩阵设置全部元素，返回自身。
	 * Sets all elements from another matrix and returns this.
	 *
	 * @param matrix 源矩阵 / source matrix
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f set(Matrix4f matrix) {
		this.m00 = matrix.m00;
		this.m01 = matrix.m01;
		this.m02 = matrix.m02;
		this.m03 = matrix.m03;
		this.m10 = matrix.m10;
		this.m11 = matrix.m11;
		this.m12 = matrix.m12;
		this.m13 = matrix.m13;
		this.m20 = matrix.m20;
		this.m21 = matrix.m21;
		this.m22 = matrix.m22;
		this.m23 = matrix.m23;
		this.m30 = matrix.m30;
		this.m31 = matrix.m31;
		this.m32 = matrix.m32;
		this.m33 = matrix.m33;
		return this;
	}

	/**
	 * 从 float 数组设置矩阵（行优先）。
	 * Sets the matrix from a float array (row-major).
	 *
	 * @param matrix 长度 16 的元素数组 / element array of length 16
	 */
	public void set(float[] matrix) {
		this.set(matrix, true);
	}

	/**
	 * 从 float 数组设置矩阵。
	 * Sets the matrix from a float array.
	 *
	 * @param matrix 长度 16 的元素数组 / element array of length 16
	 * @param rowMajor true 为行优先，false 为列优先 / true for row-major, false for column-major
	 */
	public void set(float[] matrix, boolean rowMajor) {
		if (matrix.length != 16) {
			throw new IllegalArgumentException("Array must be of size 16.");
		}
		if (rowMajor) {
			this.m00 = matrix[0];
			this.m01 = matrix[1];
			this.m02 = matrix[2];
			this.m03 = matrix[3];
			this.m10 = matrix[4];
			this.m11 = matrix[5];
			this.m12 = matrix[6];
			this.m13 = matrix[7];
			this.m20 = matrix[8];
			this.m21 = matrix[9];
			this.m22 = matrix[10];
			this.m23 = matrix[11];
			this.m30 = matrix[12];
			this.m31 = matrix[13];
			this.m32 = matrix[14];
			this.m33 = matrix[15];
		} else {
			this.m00 = matrix[0];
			this.m01 = matrix[4];
			this.m02 = matrix[8];
			this.m03 = matrix[12];
			this.m10 = matrix[1];
			this.m11 = matrix[5];
			this.m12 = matrix[9];
			this.m13 = matrix[13];
			this.m20 = matrix[2];
			this.m21 = matrix[6];
			this.m22 = matrix[10];
			this.m23 = matrix[14];
			this.m30 = matrix[3];
			this.m31 = matrix[7];
			this.m32 = matrix[11];
			this.m33 = matrix[15];
		}
	}

	/**
	 * 返回转置矩阵（新实例；内部以行优先数组构造，实际未做转置）。
	 * Returns a transposed matrix (new instance; built from row-major array without actual transposition).
	 *
	 * @return 新矩阵 / new matrix
	 */
	public Matrix4f transpose() {
		float[] tmp = new float[16];
		this.get(tmp, true);
		Matrix4f mat = new Matrix4f(tmp);
		return mat;
	}

	/**
	 * 原地转置本矩阵。
	 * Transposes this matrix in place.
	 *
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f transposeLocal() {
		float tmp = this.m01;
		this.m01 = this.m10;
		this.m10 = tmp;
		tmp = this.m02;
		this.m02 = this.m20;
		this.m20 = tmp;
		tmp = this.m03;
		this.m03 = this.m30;
		this.m30 = tmp;
		tmp = this.m12;
		this.m12 = this.m21;
		this.m21 = tmp;
		tmp = this.m13;
		this.m13 = this.m31;
		this.m31 = tmp;
		tmp = this.m23;
		this.m23 = this.m32;
		this.m32 = tmp;
		return this;
	}

	/**
	 * 将矩阵写入新的 FloatBuffer（行优先）。
	 * Writes the matrix into a new FloatBuffer (row-major).
	 *
	 * @return 已 rewind 的缓冲区 / rewound buffer
	 */
	public FloatBuffer toFloatBuffer() {
		return this.toFloatBuffer(false);
	}

	/**
	 * 将矩阵写入新的 FloatBuffer。
	 * Writes the matrix into a new FloatBuffer.
	 *
	 * @param columnMajor true 为列优先，false 为行优先 / true for column-major, false for row-major
	 * @return 已 rewind 的缓冲区 / rewound buffer
	 */
	public FloatBuffer toFloatBuffer(boolean columnMajor) {
		FloatBuffer fb = BufferUtils.createFloatBuffer(16);
		this.fillFloatBuffer(fb, columnMajor);
		fb.rewind();
		return fb;
	}

	/**
	 * 将矩阵填充到已有 FloatBuffer（行优先）。
	 * Fills an existing FloatBuffer with matrix elements (row-major).
	 *
	 * @param fb 目标缓冲区 / destination buffer
	 * @return 目标缓冲区 / destination buffer
	 */
	public FloatBuffer fillFloatBuffer(FloatBuffer fb) {
		return this.fillFloatBuffer(fb, false);
	}

	/**
	 * 将矩阵填充到已有 FloatBuffer。
	 * Fills an existing FloatBuffer with matrix elements.
	 *
	 * @param fb 目标缓冲区 / destination buffer
	 * @param columnMajor true 为列优先，false 为行优先 / true for column-major, false for row-major
	 * @return 目标缓冲区 / destination buffer
	 */
	public FloatBuffer fillFloatBuffer(FloatBuffer fb, boolean columnMajor) {
		if (columnMajor) {
			fb.put(this.m00).put(this.m10).put(this.m20).put(this.m30);
			fb.put(this.m01).put(this.m11).put(this.m21).put(this.m31);
			fb.put(this.m02).put(this.m12).put(this.m22).put(this.m32);
			fb.put(this.m03).put(this.m13).put(this.m23).put(this.m33);
		} else {
			fb.put(this.m00).put(this.m01).put(this.m02).put(this.m03);
			fb.put(this.m10).put(this.m11).put(this.m12).put(this.m13);
			fb.put(this.m20).put(this.m21).put(this.m22).put(this.m23);
			fb.put(this.m30).put(this.m31).put(this.m32).put(this.m33);
		}
		return fb;
	}

	/**
	 * 将矩阵填充到 float 数组。
	 * Fills a float array with matrix elements.
	 *
	 * @param f 目标数组 / destination array
	 * @param columnMajor true 为列优先，false 为行优先 / true for column-major, false for row-major
	 */
	public void fillFloatArray(float[] f, boolean columnMajor) {
		if (columnMajor) {
			f[0] = this.m00;
			f[1] = this.m10;
			f[2] = this.m20;
			f[3] = this.m30;
			f[4] = this.m01;
			f[5] = this.m11;
			f[6] = this.m21;
			f[7] = this.m31;
			f[8] = this.m02;
			f[9] = this.m12;
			f[10] = this.m22;
			f[11] = this.m32;
			f[12] = this.m03;
			f[13] = this.m13;
			f[14] = this.m23;
			f[15] = this.m33;
		} else {
			f[0] = this.m00;
			f[1] = this.m01;
			f[2] = this.m02;
			f[3] = this.m03;
			f[4] = this.m10;
			f[5] = this.m11;
			f[6] = this.m12;
			f[7] = this.m13;
			f[8] = this.m20;
			f[9] = this.m21;
			f[10] = this.m22;
			f[11] = this.m23;
			f[12] = this.m30;
			f[13] = this.m31;
			f[14] = this.m32;
			f[15] = this.m33;
		}
	}

	/**
	 * 从 FloatBuffer 读取矩阵（行优先）。
	 * Reads the matrix from a FloatBuffer (row-major).
	 *
	 * @param fb 源缓冲区 / source buffer
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f readFloatBuffer(FloatBuffer fb) {
		return this.readFloatBuffer(fb, false);
	}

	/**
	 * 从 FloatBuffer 读取矩阵。
	 * Reads the matrix from a FloatBuffer.
	 *
	 * @param fb 源缓冲区 / source buffer
	 * @param columnMajor true 为列优先，false 为行优先 / true for column-major, false for row-major
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f readFloatBuffer(FloatBuffer fb, boolean columnMajor) {
		if (columnMajor) {
			this.m00 = fb.get();
			this.m10 = fb.get();
			this.m20 = fb.get();
			this.m30 = fb.get();
			this.m01 = fb.get();
			this.m11 = fb.get();
			this.m21 = fb.get();
			this.m31 = fb.get();
			this.m02 = fb.get();
			this.m12 = fb.get();
			this.m22 = fb.get();
			this.m32 = fb.get();
			this.m03 = fb.get();
			this.m13 = fb.get();
			this.m23 = fb.get();
			this.m33 = fb.get();
		} else {
			this.m00 = fb.get();
			this.m01 = fb.get();
			this.m02 = fb.get();
			this.m03 = fb.get();
			this.m10 = fb.get();
			this.m11 = fb.get();
			this.m12 = fb.get();
			this.m13 = fb.get();
			this.m20 = fb.get();
			this.m21 = fb.get();
			this.m22 = fb.get();
			this.m23 = fb.get();
			this.m30 = fb.get();
			this.m31 = fb.get();
			this.m32 = fb.get();
			this.m33 = fb.get();
		}
		return this;
	}

	/**
	 * 将矩阵设为单位矩阵。
	 * Loads the identity matrix.
	 */
	public void loadIdentity() {
		this.m03 = 0.0f;
		this.m02 = 0.0f;
		this.m01 = 0.0f;
		this.m13 = 0.0f;
		this.m12 = 0.0f;
		this.m10 = 0.0f;
		this.m23 = 0.0f;
		this.m21 = 0.0f;
		this.m20 = 0.0f;
		this.m32 = 0.0f;
		this.m31 = 0.0f;
		this.m30 = 0.0f;
		this.m33 = 1.0f;
		this.m22 = 1.0f;
		this.m11 = 1.0f;
		this.m00 = 1.0f;
	}

	/**
	 * 根据视锥参数构建投影矩阵。
	 * Builds a projection matrix from frustum parameters.
	 *
	 * @param near 近裁剪面 / near clip plane
	 * @param far 远裁剪面 / far clip plane
	 * @param right 左边界 / left bound
	 * @param near 右边界 / right bound
	 * @param far 上边界 / top bound
	 * @param left 下边界 / bottom bound
	 * @param parallel true 为正交投影，false 为透视投影 / true for orthographic, false for perspective
	 */
	public void fromFrustum(float near, float far, float left, float right, float top, float bottom, boolean parallel) {
		this.loadIdentity();
		if (parallel) {
			this.m00 = 2.0f / (right - left);
			this.m11 = 2.0f / (top - bottom);
			this.m22 = -2.0f / (far - near);
			this.m33 = 1.0f;
			this.m03 = -(right + left) / (right - left);
			this.m13 = -(top + bottom) / (top - bottom);
			this.m23 = -(far + near) / (far - near);
		} else {
			this.m00 = 2.0f * near / (right - left);
			this.m11 = 2.0f * near / (top - bottom);
			this.m32 = -1.0f;
			this.m33 = -0.0f;
			this.m02 = (right + left) / (right - left);
			this.m12 = (top + bottom) / (top - bottom);
			this.m22 = -(far + near) / (far - near);
			this.m23 = -(2.0f * far * near) / (far - near);
		}
	}

	/**
	 * 从任意轴与旋转角构建旋转矩阵（会先归一化轴）。
	 * Builds a rotation matrix from an axis and angle (axis is normalized first).
	 *
	 * @param angle 旋转角（弧度） / rotation angle in radians
	 * @param axis 旋转轴 / rotation axis
	 */
	public void fromAngleAxis(float angle, Vector3f axis) {
		Vector3f normAxis = axis.normalize();
		this.fromAngleNormalAxis(angle, normAxis);
	}

	/**
	 * 从单位轴与旋转角构建旋转矩阵。
	 * Builds a rotation matrix from a unit axis and angle.
	 *
	 * @param angle 旋转角（弧度） / rotation angle in radians
	 * @param axis 已归一化的旋转轴 / normalized rotation axis
	 */
	public void fromAngleNormalAxis(float angle, Vector3f axis) {
		this.zero();
		this.m33 = 1.0f;
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
	 * 原地将矩阵各元素乘以标量。
	 * Multiplies each matrix element by a scalar in place.
	 *
	 * @param scalar 标量 / scalar
	 */
	public void multLocal(float scalar) {
		this.m00 *= scalar;
		this.m01 *= scalar;
		this.m02 *= scalar;
		this.m03 *= scalar;
		this.m10 *= scalar;
		this.m11 *= scalar;
		this.m12 *= scalar;
		this.m13 *= scalar;
		this.m20 *= scalar;
		this.m21 *= scalar;
		this.m22 *= scalar;
		this.m23 *= scalar;
		this.m30 *= scalar;
		this.m31 *= scalar;
		this.m32 *= scalar;
		this.m33 *= scalar;
	}

	/**
	 * 返回本矩阵乘以标量的新矩阵。
	 * Returns a new matrix equal to this times a scalar.
	 *
	 * @param scalar 标量 / scalar
	 * @return 新矩阵 / new matrix
	 */
	public Matrix4f mult(float scalar) {
		Matrix4f out = new Matrix4f();
		out.set(this);
		out.multLocal(scalar);
		return out;
	}

	/**
	 * 将本矩阵乘以标量的结果写入 store。
	 * Stores this matrix times a scalar into store.
	 *
	 * @return 标量 / scalar
	 * @param store 结果存储 / result storage
	 * @param scalar store 自身（链式调用） / store itself
	 */
	public Matrix4f mult(float scalar, Matrix4f store) {
		store.set(this);
		store.multLocal(scalar);
		return store;
	}

	/**
	 * 返回本矩阵右乘另一矩阵的新矩阵。
	 * Returns a new matrix equal to this times another matrix.
	 *
	 * @param in2 右乘矩阵 / right-hand matrix
	 * @return 乘积矩阵 / product matrix
	 */
	public Matrix4f mult(Matrix4f in2) {
		return this.mult(in2, null);
	}

	/**
	 * 将本矩阵右乘另一矩阵的结果写入 store。
	 * Stores this matrix times another matrix into store.
	 *
	 * @param in2 右乘矩阵 / right-hand matrix
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身（链式调用） / store itself
	 */
	public Matrix4f mult(Matrix4f in2, Matrix4f store) {
		if (store == null) {
			store = new Matrix4f();
		}
		float temp00 = this.m00 * in2.m00 + this.m01 * in2.m10 + this.m02 * in2.m20 + this.m03 * in2.m30;
		float temp01 = this.m00 * in2.m01 + this.m01 * in2.m11 + this.m02 * in2.m21 + this.m03 * in2.m31;
		float temp02 = this.m00 * in2.m02 + this.m01 * in2.m12 + this.m02 * in2.m22 + this.m03 * in2.m32;
		float temp03 = this.m00 * in2.m03 + this.m01 * in2.m13 + this.m02 * in2.m23 + this.m03 * in2.m33;
		float temp10 = this.m10 * in2.m00 + this.m11 * in2.m10 + this.m12 * in2.m20 + this.m13 * in2.m30;
		float temp11 = this.m10 * in2.m01 + this.m11 * in2.m11 + this.m12 * in2.m21 + this.m13 * in2.m31;
		float temp12 = this.m10 * in2.m02 + this.m11 * in2.m12 + this.m12 * in2.m22 + this.m13 * in2.m32;
		float temp13 = this.m10 * in2.m03 + this.m11 * in2.m13 + this.m12 * in2.m23 + this.m13 * in2.m33;
		float temp20 = this.m20 * in2.m00 + this.m21 * in2.m10 + this.m22 * in2.m20 + this.m23 * in2.m30;
		float temp21 = this.m20 * in2.m01 + this.m21 * in2.m11 + this.m22 * in2.m21 + this.m23 * in2.m31;
		float temp22 = this.m20 * in2.m02 + this.m21 * in2.m12 + this.m22 * in2.m22 + this.m23 * in2.m32;
		float temp23 = this.m20 * in2.m03 + this.m21 * in2.m13 + this.m22 * in2.m23 + this.m23 * in2.m33;
		float temp30 = this.m30 * in2.m00 + this.m31 * in2.m10 + this.m32 * in2.m20 + this.m33 * in2.m30;
		float temp31 = this.m30 * in2.m01 + this.m31 * in2.m11 + this.m32 * in2.m21 + this.m33 * in2.m31;
		float temp32 = this.m30 * in2.m02 + this.m31 * in2.m12 + this.m32 * in2.m22 + this.m33 * in2.m32;
		float temp33 = this.m30 * in2.m03 + this.m31 * in2.m13 + this.m32 * in2.m23 + this.m33 * in2.m33;
		store.m00 = temp00;
		store.m01 = temp01;
		store.m02 = temp02;
		store.m03 = temp03;
		store.m10 = temp10;
		store.m11 = temp11;
		store.m12 = temp12;
		store.m13 = temp13;
		store.m20 = temp20;
		store.m21 = temp21;
		store.m22 = temp22;
		store.m23 = temp23;
		store.m30 = temp30;
		store.m31 = temp31;
		store.m32 = temp32;
		store.m33 = temp33;
		return store;
	}

	/**
	 * 原地右乘另一矩阵。
	 * Multiplies this matrix by another matrix in place.
	 *
	 * @param in2 右乘矩阵 / right-hand matrix
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f multLocal(Matrix4f in2) {
		return this.mult(in2, this);
	}

	/**
	 * 用本矩阵变换三维向量（含平移），分配新向量。
	 * Transforms a 3D vector by this matrix (including translation), allocating a new vector.
	 *
	 * @param vec 输入向量 / input vector
	 * @return 变换后的向量 / transformed vector
	 */
	public Vector3f mult(Vector3f vec) {
		return this.mult(vec, null);
	}

	/**
	 * 用本矩阵变换三维向量（含平移），结果写入 store。
	 * Transforms a 3D vector by this matrix (including translation) into store.
	 *
	 * @param vec 输入向量 / input vector
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身（链式调用） / store itself
	 */
	public Vector3f mult(Vector3f vec, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		store.x = this.m00 * vx + this.m01 * vy + this.m02 * vz + this.m03;
		store.y = this.m10 * vx + this.m11 * vy + this.m12 * vz + this.m13;
		store.z = this.m20 * vx + this.m21 * vy + this.m22 * vz + this.m23;
		return store;
	}

	/**
	 * 用本矩阵的 3×3 旋转部分变换法线向量（不含平移）。
	 * Transforms a normal vector by the 3×3 rotation part (no translation).
	 *
	 * @param vec 输入法线 / input normal
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身（链式调用） / store itself
	 */
	public Vector3f multNormal(Vector3f vec, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		store.x = this.m00 * vx + this.m01 * vy + this.m02 * vz;
		store.y = this.m10 * vx + this.m11 * vy + this.m12 * vz;
		store.z = this.m20 * vx + this.m21 * vy + this.m22 * vz;
		return store;
	}

	/**
	 * 用本矩阵转置的 3×3 部分变换法线向量（行向量乘法）。
	 * Transforms a normal by the transposed 3×3 part (row-vector multiply).
	 *
	 * @param vec 输入法线 / input normal
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身（链式调用） / store itself
	 */
	public Vector3f multNormalAcross(Vector3f vec, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		store.x = this.m00 * vx + this.m10 * vy + this.m20 * vz;
		store.y = this.m01 * vx + this.m11 * vy + this.m21 * vz;
		store.z = this.m02 * vx + this.m12 * vy + this.m22 * vz;
		return store;
	}

	/**
	 * 用本矩阵做投影变换，三维结果写入 store，返回 w 分量。
	 * Applies a projective transform; stores XYZ in store and returns the W component.
	 *
	 * @param vec 输入向量 / input vector
	 * @param store 三维结果存储 / 3D result storage
	 * @return 齐次 W 分量 / homogeneous W component
	 */
	public float multProj(Vector3f vec, Vector3f store) {
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		store.x = this.m00 * vx + this.m01 * vy + this.m02 * vz + this.m03;
		store.y = this.m10 * vx + this.m11 * vy + this.m12 * vz + this.m13;
		store.z = this.m20 * vx + this.m21 * vy + this.m22 * vz + this.m23;
		return this.m30 * vx + this.m31 * vy + this.m32 * vz + this.m33;
	}

	/**
	 * 用本矩阵转置变换三维向量（含平移，行向量乘法）。
	 * Transforms a 3D vector by the transpose (including translation, row-vector multiply).
	 *
	 * @param vec 输入向量（null 时记录日志并返回 null） / input vector (null logs and returns null)
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身，或 null（新建时） / store itself, or null
	 */
	public Vector3f multAcross(Vector3f vec, Vector3f store) {
		if (null == vec) {
			log.info(I18n.get("log.badf0b9e5c44"));
			return null;
		}
		if (store == null) {
			store = new Vector3f();
		}
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		store.x = this.m00 * vx + this.m10 * vy + this.m20 * vz + this.m30 * 1.0f;
		store.y = this.m01 * vx + this.m11 * vy + this.m21 * vz + this.m31 * 1.0f;
		store.z = this.m02 * vx + this.m12 * vy + this.m22 * vz + this.m32 * 1.0f;
		return store;
	}

	/**
	 * 用本矩阵原地变换四维向量。
	 * Transforms a 4D vector by this matrix in place.
	 *
	 * @param vec4f 长度 4 的向量（无效时警告并返回 null） / vector of length 4 (invalid warns and returns null)
	 * @return 变换后的 vec4f，或 null / transformed vec4f, or null
	 */
	public float[] mult(float[] vec4f) {
		if (null == vec4f || vec4f.length != 4) {
			log.warn(I18n.get("log.0de89fa45f4c"));
			return null;
		}
		float x = vec4f[0];
		float y = vec4f[1];
		float z = vec4f[2];
		float w = vec4f[3];
		vec4f[0] = this.m00 * x + this.m01 * y + this.m02 * z + this.m03 * w;
		vec4f[1] = this.m10 * x + this.m11 * y + this.m12 * z + this.m13 * w;
		vec4f[2] = this.m20 * x + this.m21 * y + this.m22 * z + this.m23 * w;
		vec4f[3] = this.m30 * x + this.m31 * y + this.m32 * z + this.m33 * w;
		return vec4f;
	}

	/**
	 * 用本矩阵转置原地变换四维向量（行向量乘法）。
	 * Transforms a 4D vector by the transpose in place (row-vector multiply).
	 *
	 * @param vec4f 长度 4 的向量（无效时警告并返回 null） / vector of length 4 (invalid warns and returns null)
	 * @return 变换后的 vec4f，或 null / transformed vec4f, or null
	 */
	public float[] multAcross(float[] vec4f) {
		if (null == vec4f || vec4f.length != 4) {
			log.warn(I18n.get("log.0de89fa45f4c"));
			return null;
		}
		float x = vec4f[0];
		float y = vec4f[1];
		float z = vec4f[2];
		float w = vec4f[3];
		vec4f[0] = this.m00 * x + this.m10 * y + this.m20 * z + this.m30 * w;
		vec4f[1] = this.m01 * x + this.m11 * y + this.m21 * z + this.m31 * w;
		vec4f[2] = this.m02 * x + this.m12 * y + this.m22 * z + this.m32 * w;
		vec4f[3] = this.m03 * x + this.m13 * y + this.m23 * z + this.m33 * w;
		return vec4f;
	}

	/**
	 * 返回本矩阵的逆矩阵（新实例）。
	 * Returns the inverse of this matrix (new instance).
	 *
	 * @return 逆矩阵 / inverse matrix
	 * @throws ArithmeticException 矩阵不可逆 / if the matrix is singular
	 */
	public Matrix4f invert() {
		return this.invert(null);
	}

	/**
	 * 将本矩阵的逆写入 store。
	 * Stores the inverse of this matrix into store.
	 *
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身（链式调用） / store itself
	 * @throws ArithmeticException 矩阵不可逆 / if the matrix is singular
	 */
	public Matrix4f invert(Matrix4f store) {
		float fB0;
		float fA5;
		float fB1;
		float fA4;
		float fB2;
		float fA3;
		float fB3;
		float fA2;
		float fB4;
		float fA1;
		float fB5;
		float fA0;
		float fDet;
		if (store == null) {
			store = new Matrix4f();
		}
		if (FastMath.abs(fDet = (fA0 = this.m00 * this.m11 - this.m01 * this.m10)
				* (fB5 = this.m22 * this.m33 - this.m23 * this.m32)
				- (fA1 = this.m00 * this.m12 - this.m02 * this.m10) * (fB4 = this.m21 * this.m33 - this.m23 * this.m31)
				+ (fA2 = this.m00 * this.m13 - this.m03 * this.m10) * (fB3 = this.m21 * this.m32 - this.m22 * this.m31)
				+ (fA3 = this.m01 * this.m12 - this.m02 * this.m11) * (fB2 = this.m20 * this.m33 - this.m23 * this.m30)
				- (fA4 = this.m01 * this.m13 - this.m03 * this.m11) * (fB1 = this.m20 * this.m32 - this.m22 * this.m30)
				+ (fA5 = this.m02 * this.m13 - this.m03 * this.m12)
						* (fB0 = this.m20 * this.m31 - this.m21 * this.m30)) <= 0.0f) {
			throw new ArithmeticException("This matrix cannot be inverted");
		}
		store.m00 = this.m11 * fB5 - this.m12 * fB4 + this.m13 * fB3;
		store.m10 = -this.m10 * fB5 + this.m12 * fB2 - this.m13 * fB1;
		store.m20 = this.m10 * fB4 - this.m11 * fB2 + this.m13 * fB0;
		store.m30 = -this.m10 * fB3 + this.m11 * fB1 - this.m12 * fB0;
		store.m01 = -this.m01 * fB5 + this.m02 * fB4 - this.m03 * fB3;
		store.m11 = this.m00 * fB5 - this.m02 * fB2 + this.m03 * fB1;
		store.m21 = -this.m00 * fB4 + this.m01 * fB2 - this.m03 * fB0;
		store.m31 = this.m00 * fB3 - this.m01 * fB1 + this.m02 * fB0;
		store.m02 = this.m31 * fA5 - this.m32 * fA4 + this.m33 * fA3;
		store.m12 = -this.m30 * fA5 + this.m32 * fA2 - this.m33 * fA1;
		store.m22 = this.m30 * fA4 - this.m31 * fA2 + this.m33 * fA0;
		store.m32 = -this.m30 * fA3 + this.m31 * fA1 - this.m32 * fA0;
		store.m03 = -this.m21 * fA5 + this.m22 * fA4 - this.m23 * fA3;
		store.m13 = this.m20 * fA5 - this.m22 * fA2 + this.m23 * fA1;
		store.m23 = -this.m20 * fA4 + this.m21 * fA2 - this.m23 * fA0;
		store.m33 = this.m20 * fA3 - this.m21 * fA1 + this.m22 * fA0;
		float fInvDet = 1.0f / fDet;
		store.multLocal(fInvDet);
		return store;
	}

	/**
	 * 原地求逆；若不可逆则置为零矩阵。
	 * Inverts this matrix in place; zeros the matrix if singular.
	 *
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f invertLocal() {
		float fA0 = this.m00 * this.m11 - this.m01 * this.m10;
		float fB5 = this.m22 * this.m33 - this.m23 * this.m32;
		float fA1 = this.m00 * this.m12 - this.m02 * this.m10;
		float fB4 = this.m21 * this.m33 - this.m23 * this.m31;
		float fA2 = this.m00 * this.m13 - this.m03 * this.m10;
		float fB3 = this.m21 * this.m32 - this.m22 * this.m31;
		float fA3 = this.m01 * this.m12 - this.m02 * this.m11;
		float fB2 = this.m20 * this.m33 - this.m23 * this.m30;
		float fA4 = this.m01 * this.m13 - this.m03 * this.m11;
		float fB1 = this.m20 * this.m32 - this.m22 * this.m30;
		float fA5 = this.m02 * this.m13 - this.m03 * this.m12;
		float fB0 = this.m20 * this.m31 - this.m21 * this.m30;
		float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;
		if (FastMath.abs(fDet) <= 0.0f) {
			return this.zero();
		}
		float f00 = this.m11 * fB5 - this.m12 * fB4 + this.m13 * fB3;
		float f10 = -this.m10 * fB5 + this.m12 * fB2 - this.m13 * fB1;
		float f20 = this.m10 * fB4 - this.m11 * fB2 + this.m13 * fB0;
		float f30 = -this.m10 * fB3 + this.m11 * fB1 - this.m12 * fB0;
		float f01 = -this.m01 * fB5 + this.m02 * fB4 - this.m03 * fB3;
		float f11 = this.m00 * fB5 - this.m02 * fB2 + this.m03 * fB1;
		float f21 = -this.m00 * fB4 + this.m01 * fB2 - this.m03 * fB0;
		float f31 = this.m00 * fB3 - this.m01 * fB1 + this.m02 * fB0;
		float f02 = this.m31 * fA5 - this.m32 * fA4 + this.m33 * fA3;
		float f12 = -this.m30 * fA5 + this.m32 * fA2 - this.m33 * fA1;
		float f22 = this.m30 * fA4 - this.m31 * fA2 + this.m33 * fA0;
		float f32 = -this.m30 * fA3 + this.m31 * fA1 - this.m32 * fA0;
		float f03 = -this.m21 * fA5 + this.m22 * fA4 - this.m23 * fA3;
		float f13 = this.m20 * fA5 - this.m22 * fA2 + this.m23 * fA1;
		float f23 = -this.m20 * fA4 + this.m21 * fA2 - this.m23 * fA0;
		float f33 = this.m20 * fA3 - this.m21 * fA1 + this.m22 * fA0;
		this.m00 = f00;
		this.m01 = f01;
		this.m02 = f02;
		this.m03 = f03;
		this.m10 = f10;
		this.m11 = f11;
		this.m12 = f12;
		this.m13 = f13;
		this.m20 = f20;
		this.m21 = f21;
		this.m22 = f22;
		this.m23 = f23;
		this.m30 = f30;
		this.m31 = f31;
		this.m32 = f32;
		this.m33 = f33;
		float fInvDet = 1.0f / fDet;
		this.multLocal(fInvDet);
		return this;
	}

	/**
	 * 返回伴随矩阵（新实例）。
	 * Returns the adjoint matrix (new instance).
	 *
	 * @return 伴随矩阵 / adjoint matrix
	 */
	public Matrix4f adjoint() {
		return this.adjoint(null);
	}

	/**
	 * 将伴随矩阵写入 store。
	 * Stores the adjoint matrix into store.
	 *
	 * @param store 结果存储（null 时分配） / result storage (allocated if null)
	 * @return store 自身（链式调用） / store itself
	 */
	public Matrix4f adjoint(Matrix4f store) {
		if (store == null) {
			store = new Matrix4f();
		}
		float fA0 = this.m00 * this.m11 - this.m01 * this.m10;
		float fA1 = this.m00 * this.m12 - this.m02 * this.m10;
		float fA2 = this.m00 * this.m13 - this.m03 * this.m10;
		float fA3 = this.m01 * this.m12 - this.m02 * this.m11;
		float fA4 = this.m01 * this.m13 - this.m03 * this.m11;
		float fA5 = this.m02 * this.m13 - this.m03 * this.m12;
		float fB0 = this.m20 * this.m31 - this.m21 * this.m30;
		float fB1 = this.m20 * this.m32 - this.m22 * this.m30;
		float fB2 = this.m20 * this.m33 - this.m23 * this.m30;
		float fB3 = this.m21 * this.m32 - this.m22 * this.m31;
		float fB4 = this.m21 * this.m33 - this.m23 * this.m31;
		float fB5 = this.m22 * this.m33 - this.m23 * this.m32;
		store.m00 = this.m11 * fB5 - this.m12 * fB4 + this.m13 * fB3;
		store.m10 = -this.m10 * fB5 + this.m12 * fB2 - this.m13 * fB1;
		store.m20 = this.m10 * fB4 - this.m11 * fB2 + this.m13 * fB0;
		store.m30 = -this.m10 * fB3 + this.m11 * fB1 - this.m12 * fB0;
		store.m01 = -this.m01 * fB5 + this.m02 * fB4 - this.m03 * fB3;
		store.m11 = this.m00 * fB5 - this.m02 * fB2 + this.m03 * fB1;
		store.m21 = -this.m00 * fB4 + this.m01 * fB2 - this.m03 * fB0;
		store.m31 = this.m00 * fB3 - this.m01 * fB1 + this.m02 * fB0;
		store.m02 = this.m31 * fA5 - this.m32 * fA4 + this.m33 * fA3;
		store.m12 = -this.m30 * fA5 + this.m32 * fA2 - this.m33 * fA1;
		store.m22 = this.m30 * fA4 - this.m31 * fA2 + this.m33 * fA0;
		store.m32 = -this.m30 * fA3 + this.m31 * fA1 - this.m32 * fA0;
		store.m03 = -this.m21 * fA5 + this.m22 * fA4 - this.m23 * fA3;
		store.m13 = this.m20 * fA5 - this.m22 * fA2 + this.m23 * fA1;
		store.m23 = -this.m20 * fA4 + this.m21 * fA2 - this.m23 * fA0;
		store.m33 = this.m20 * fA3 - this.m21 * fA1 + this.m22 * fA0;
		return store;
	}

	/**
	 * 计算矩阵行列式。
	 * Computes the matrix determinant.
	 *
	 * @return 行列式值 / determinant value
	 */
	public float determinant() {
		float fA0 = this.m00 * this.m11 - this.m01 * this.m10;
		float fA1 = this.m00 * this.m12 - this.m02 * this.m10;
		float fA2 = this.m00 * this.m13 - this.m03 * this.m10;
		float fA3 = this.m01 * this.m12 - this.m02 * this.m11;
		float fA4 = this.m01 * this.m13 - this.m03 * this.m11;
		float fA5 = this.m02 * this.m13 - this.m03 * this.m12;
		float fB0 = this.m20 * this.m31 - this.m21 * this.m30;
		float fB1 = this.m20 * this.m32 - this.m22 * this.m30;
		float fB2 = this.m20 * this.m33 - this.m23 * this.m30;
		float fB3 = this.m21 * this.m32 - this.m22 * this.m31;
		float fB4 = this.m21 * this.m33 - this.m23 * this.m31;
		float fB5 = this.m22 * this.m33 - this.m23 * this.m32;
		float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;
		return fDet;
	}

	/**
	 * 将矩阵全部元素置零。
	 * Zeros all matrix elements.
	 *
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f zero() {
		this.m03 = 0.0f;
		this.m02 = 0.0f;
		this.m01 = 0.0f;
		this.m00 = 0.0f;
		this.m13 = 0.0f;
		this.m12 = 0.0f;
		this.m11 = 0.0f;
		this.m10 = 0.0f;
		this.m23 = 0.0f;
		this.m22 = 0.0f;
		this.m21 = 0.0f;
		this.m20 = 0.0f;
		this.m33 = 0.0f;
		this.m32 = 0.0f;
		this.m31 = 0.0f;
		this.m30 = 0.0f;
		return this;
	}

	/**
	 * 返回本矩阵与另一矩阵的元素和（新实例）。
	 * Returns the element-wise sum of this and another matrix (new instance).
	 *
	 * @param mat 另一矩阵 / other matrix
	 * @return 和矩阵 / sum matrix
	 */
	public Matrix4f add(Matrix4f mat) {
		Matrix4f result = new Matrix4f();
		result.m00 = this.m00 + mat.m00;
		result.m01 = this.m01 + mat.m01;
		result.m02 = this.m02 + mat.m02;
		result.m03 = this.m03 + mat.m03;
		result.m10 = this.m10 + mat.m10;
		result.m11 = this.m11 + mat.m11;
		result.m12 = this.m12 + mat.m12;
		result.m13 = this.m13 + mat.m13;
		result.m20 = this.m20 + mat.m20;
		result.m21 = this.m21 + mat.m21;
		result.m22 = this.m22 + mat.m22;
		result.m23 = this.m23 + mat.m23;
		result.m30 = this.m30 + mat.m30;
		result.m31 = this.m31 + mat.m31;
		result.m32 = this.m32 + mat.m32;
		result.m33 = this.m33 + mat.m33;
		return result;
	}

	/**
	 * 原地加上另一矩阵。
	 * Adds another matrix to this one in place.
	 *
	 * @param mat 另一矩阵 / other matrix
	 */
	public void addLocal(Matrix4f mat) {
		this.m00 += mat.m00;
		this.m01 += mat.m01;
		this.m02 += mat.m02;
		this.m03 += mat.m03;
		this.m10 += mat.m10;
		this.m11 += mat.m11;
		this.m12 += mat.m12;
		this.m13 += mat.m13;
		this.m20 += mat.m20;
		this.m21 += mat.m21;
		this.m22 += mat.m22;
		this.m23 += mat.m23;
		this.m30 += mat.m30;
		this.m31 += mat.m31;
		this.m32 += mat.m32;
		this.m33 += mat.m33;
	}

	/**
	 * 提取平移分量，分配新向量。
	 * Extracts the translation component, allocating a new vector.
	 *
	 * @return 平移向量 / translation vector
	 */
	public Vector3f toTranslationVector() {
		return new Vector3f(this.m03, this.m13, this.m23);
	}

	/**
	 * 提取平移分量到已有向量。
	 * Extracts the translation component into an existing vector.
	 *
	 * @param vector 目标向量 / destination vector
	 */
	public void toTranslationVector(Vector3f vector) {
		vector.set(this.m03, this.m13, this.m23);
	}

	/**
	 * 提取左上 3×3 旋转矩阵（新实例）。
	 * Extracts the upper-left 3×3 rotation matrix (new instance).
	 *
	 * @return 旋转矩阵 / rotation matrix
	 */
	public Matrix3f toRotationMatrix() {
		return new Matrix3f(this.m00, this.m01, this.m02, this.m10, this.m11, this.m12, this.m20, this.m21, this.m22);
	}

	/**
	 * 提取左上 3×3 旋转矩阵到已有矩阵。
	 * Extracts the upper-left 3×3 rotation matrix into an existing matrix.
	 *
	 * @param mat 目标矩阵 / destination matrix
	 */
	public void toRotationMatrix(Matrix3f mat) {
		mat.m00 = this.m00;
		mat.m01 = this.m01;
		mat.m02 = this.m02;
		mat.m10 = this.m10;
		mat.m11 = this.m11;
		mat.m12 = this.m12;
		mat.m20 = this.m20;
		mat.m21 = this.m21;
		mat.m22 = this.m22;
	}

	/**
	 * 用给定 3×3 矩阵设置本矩阵的旋转部分。
	 * Sets the rotation part of this matrix from a 3×3 matrix.
	 *
	 * @param mat 源旋转矩阵 / source rotation matrix
	 */
	public void setRotationMatrix(Matrix3f mat) {
		this.m00 = mat.m00;
		this.m01 = mat.m01;
		this.m02 = mat.m02;
		this.m10 = mat.m10;
		this.m11 = mat.m11;
		this.m12 = mat.m12;
		this.m20 = mat.m20;
		this.m21 = mat.m21;
		this.m22 = mat.m22;
	}

	/**
	 * 按分量缩放对角线上的缩放因子（乘到 m00/m11/m22）。
	 * Scales the diagonal scale factors component-wise (multiplies m00/m11/m22).
	 *
	 * @param x X 缩放 / X scale
	 * @param y Y 缩放 / Y scale
	 * @param z Z 缩放 / Z scale
	 */
	public void setScale(float x, float y, float z) {
		this.m00 *= x;
		this.m11 *= y;
		this.m22 *= z;
	}

	/**
	 * 按向量缩放对角线上的缩放因子。
	 * Scales the diagonal scale factors by a vector.
	 *
	 * @param scale 缩放向量 / scale vector
	 */
	public void setScale(Vector3f scale) {
		this.m00 *= scale.x;
		this.m11 *= scale.y;
		this.m22 *= scale.z;
	}

	/**
	 * 从 float 数组设置平移分量。
	 * Sets the translation component from a float array.
	 *
	 * @param translation 长度 3 的平移数组 / translation array of length 3
	 */
	public void setTranslation(float[] translation) {
		if (translation.length != 3) {
			throw new IllegalArgumentException("Translation size must be 3.");
		}
		this.m03 = translation[0];
		this.m13 = translation[1];
		this.m23 = translation[2];
	}

	/**
	 * 按分量设置平移。
	 * Sets the translation by components.
	 *
	 * @param x X 平移 / X translation
	 * @param y Y 平移 / Y translation
	 * @param z Z 平移 / Z translation
	 */
	public void setTranslation(float x, float y, float z) {
		this.m03 = x;
		this.m13 = y;
		this.m23 = z;
	}

	/**
	 * 从向量设置平移。
	 * Sets the translation from a vector.
	 *
	 * @param translation 平移向量 / translation vector
	 */
	public void setTranslation(Vector3f translation) {
		this.m03 = translation.x;
		this.m13 = translation.y;
		this.m23 = translation.z;
	}

	/**
	 * 从 float 数组设置反向平移（取负）。
	 * Sets the inverse translation from a float array (negated).
	 *
	 * @param translation 长度 3 的平移数组 / translation array of length 3
	 */
	public void setInverseTranslation(float[] translation) {
		if (translation.length != 3) {
			throw new IllegalArgumentException("Translation size must be 3.");
		}
		this.m03 = -translation[0];
		this.m13 = -translation[1];
		this.m23 = -translation[2];
	}

	/**
	 * 按欧拉角（度，顺序 Z-Y-X）设置旋转部分。
	 * Sets the rotation part from Euler angles in degrees (order Z-Y-X).
	 *
	 * @param angles 欧拉角（度，x=roll, y=pitch, z=yaw） / Euler angles in degrees: x=roll, y=pitch, z=yaw
	 */
	public void angleRotation(Vector3f angles) {
		float angle = angles.z * ((float) Math.PI / 180);
		float sy = FastMath.sin(angle);
		float cy = FastMath.cos(angle);
		angle = angles.y * ((float) Math.PI / 180);
		float sp = FastMath.sin(angle);
		float cp = FastMath.cos(angle);
		angle = angles.x * ((float) Math.PI / 180);
		float sr = FastMath.sin(angle);
		float cr = FastMath.cos(angle);
		this.m00 = cp * cy;
		this.m10 = cp * sy;
		this.m20 = -sp;
		this.m01 = sr * sp * cy + cr * -sy;
		this.m11 = sr * sp * sy + cr * cy;
		this.m21 = sr * cp;
		this.m02 = cr * sp * cy + -sr * -sy;
		this.m12 = cr * sp * sy + -sr * cy;
		this.m22 = cr * cp;
		this.m03 = 0.0f;
		this.m13 = 0.0f;
		this.m23 = 0.0f;
	}

	/**
	 * 按反向欧拉角（弧度）设置旋转部分。
	 * Sets the rotation part from inverse Euler angles in radians.
	 *
	 * @param angles 长度 3 的角度数组（弧度） / angle array of length 3 in radians
	 */
	public void setInverseRotationRadians(float[] angles) {
		if (angles.length != 3) {
			throw new IllegalArgumentException("Angles must be of size 3.");
		}
		double cr = FastMath.cos(angles[0]);
		double sr = FastMath.sin(angles[0]);
		double cp = FastMath.cos(angles[1]);
		double sp = FastMath.sin(angles[1]);
		double cy = FastMath.cos(angles[2]);
		double sy = FastMath.sin(angles[2]);
		this.m00 = (float) (cp * cy);
		this.m10 = (float) (cp * sy);
		this.m20 = (float) (-sp);
		double srsp = sr * sp;
		double crsp = cr * sp;
		this.m01 = (float) (srsp * cy - cr * sy);
		this.m11 = (float) (srsp * sy + cr * cy);
		this.m21 = (float) (sr * cp);
		this.m02 = (float) (crsp * cy + sr * sy);
		this.m12 = (float) (crsp * sy - sr * cy);
		this.m22 = (float) (cr * cp);
	}

	/**
	 * 按反向欧拉角（度）设置旋转部分。
	 * Sets the rotation part from inverse Euler angles in degrees.
	 *
	 * @param angles 长度 3 的角度数组（度） / angle array of length 3 in degrees
	 */
	public void setInverseRotationDegrees(float[] angles) {
		if (angles.length != 3) {
			throw new IllegalArgumentException("Angles must be of size 3.");
		}
		float[] vec = new float[] { angles[0] * 57.295776f, angles[1] * 57.295776f, angles[2] * 57.295776f };
		this.setInverseRotationRadians(vec);
	}

	/**
	 * 对 float 数组原地做反向平移。
	 * Applies inverse translation to a float array in place.
	 *
	 * @param vec 长度 3 的向量 / vector of length 3
	 */
	public void inverseTranslateVect(float[] vec) {
		if (vec.length != 3) {
			throw new IllegalArgumentException("vec must be of size 3.");
		}
		vec[0] = vec[0] - this.m03;
		vec[1] = vec[1] - this.m13;
		vec[2] = vec[2] - this.m23;
	}

	/**
	 * 对向量原地做反向平移。
	 * Applies inverse translation to a vector in place.
	 *
	 * @param data 目标向量 / target vector
	 */
	public void inverseTranslateVect(Vector3f data) {
		data.x -= this.m03;
		data.y -= this.m13;
		data.z -= this.m23;
	}

	/**
	 * 对向量原地做正向平移。
	 * Applies forward translation to a vector in place.
	 *
	 * @param data 目标向量 / target vector
	 */
	public void translateVect(Vector3f data) {
		data.x += this.m03;
		data.y += this.m13;
		data.z += this.m23;
	}

	/**
	 * 用旋转部分的转置原地旋转向量（反向旋转）。
	 * Rotates a vector in place by the transpose of the rotation part (inverse rotation).
	 *
	 * @param vec 目标向量 / target vector
	 */
	public void inverseRotateVect(Vector3f vec) {
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		vec.x = vx * this.m00 + vy * this.m10 + vz * this.m20;
		vec.y = vx * this.m01 + vy * this.m11 + vz * this.m21;
		vec.z = vx * this.m02 + vy * this.m12 + vz * this.m22;
	}

	/**
	 * 用旋转部分原地旋转向量。
	 * Rotates a vector in place by the rotation part.
	 *
	 * @param vec 目标向量 / target vector
	 */
	public void rotateVect(Vector3f vec) {
		float vx = vec.x;
		float vy = vec.y;
		float vz = vec.z;
		vec.x = vx * this.m00 + vy * this.m01 + vz * this.m02;
		vec.y = vx * this.m10 + vy * this.m11 + vz * this.m12;
		vec.z = vx * this.m20 + vy * this.m21 + vz * this.m22;
	}

	/**
	 * 返回矩阵的可读字符串表示。
	 * Returns a human-readable string representation of the matrix.
	 *
	 * @return 格式化字符串 / formatted string
	 */
	public String toString() {
		StringBuilder result = new StringBuilder("Matrix4f\n[\n");
		result.append(" ");
		result.append(this.m00);
		result.append("  ");
		result.append(this.m01);
		result.append("  ");
		result.append(this.m02);
		result.append("  ");
		result.append(this.m03);
		result.append(" \n");
		result.append(" ");
		result.append(this.m10);
		result.append("  ");
		result.append(this.m11);
		result.append("  ");
		result.append(this.m12);
		result.append("  ");
		result.append(this.m13);
		result.append(" \n");
		result.append(" ");
		result.append(this.m20);
		result.append("  ");
		result.append(this.m21);
		result.append("  ");
		result.append(this.m22);
		result.append("  ");
		result.append(this.m23);
		result.append(" \n");
		result.append(" ");
		result.append(this.m30);
		result.append("  ");
		result.append(this.m31);
		result.append("  ");
		result.append(this.m32);
		result.append("  ");
		result.append(this.m33);
		result.append(" \n]");
		return result.toString();
	}

	/**
	 * 计算矩阵哈希码。
	 * Computes the matrix hash code.
	 *
	 * @return 哈希值 / hash value
	 */
	public int hashCode() {
		int hash = 37;
		hash = 37 * hash + Float.floatToIntBits(this.m00);
		hash = 37 * hash + Float.floatToIntBits(this.m01);
		hash = 37 * hash + Float.floatToIntBits(this.m02);
		hash = 37 * hash + Float.floatToIntBits(this.m03);
		hash = 37 * hash + Float.floatToIntBits(this.m10);
		hash = 37 * hash + Float.floatToIntBits(this.m11);
		hash = 37 * hash + Float.floatToIntBits(this.m12);
		hash = 37 * hash + Float.floatToIntBits(this.m13);
		hash = 37 * hash + Float.floatToIntBits(this.m20);
		hash = 37 * hash + Float.floatToIntBits(this.m21);
		hash = 37 * hash + Float.floatToIntBits(this.m22);
		hash = 37 * hash + Float.floatToIntBits(this.m23);
		hash = 37 * hash + Float.floatToIntBits(this.m30);
		hash = 37 * hash + Float.floatToIntBits(this.m31);
		hash = 37 * hash + Float.floatToIntBits(this.m32);
		hash = 37 * hash + Float.floatToIntBits(this.m33);
		return hash;
	}

	/**
	 * 逐元素比较是否相等。
	 * Compares equality element-wise.
	 *
	 * @param o 比较对象 / object to compare
	 * @return 全部元素相等则为 true / true if all elements are equal
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Matrix4f) || o == null) {
			return false;
		}
		if (this == o) {
			return true;
		}
		Matrix4f comp = (Matrix4f) o;
		if (Float.compare(this.m00, comp.m00) != 0) {
			return false;
		}
		if (Float.compare(this.m01, comp.m01) != 0) {
			return false;
		}
		if (Float.compare(this.m02, comp.m02) != 0) {
			return false;
		}
		if (Float.compare(this.m03, comp.m03) != 0) {
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
		if (Float.compare(this.m13, comp.m13) != 0) {
			return false;
		}
		if (Float.compare(this.m20, comp.m20) != 0) {
			return false;
		}
		if (Float.compare(this.m21, comp.m21) != 0) {
			return false;
		}
		if (Float.compare(this.m22, comp.m22) != 0) {
			return false;
		}
		if (Float.compare(this.m23, comp.m23) != 0) {
			return false;
		}
		if (Float.compare(this.m30, comp.m30) != 0) {
			return false;
		}
		if (Float.compare(this.m31, comp.m31) != 0) {
			return false;
		}
		if (Float.compare(this.m32, comp.m32) != 0) {
			return false;
		}
		return Float.compare(this.m33, comp.m33) == 0;
	}

	/**
	 * 返回运行时类标签。
	 * Returns the runtime class tag.
	 *
	 * @return 类对象 / class object
	 */
	public Class<? extends Matrix4f> getClassTag() {
		return this.getClass();
	}

	/**
	 * 判断是否精确为单位矩阵。
	 * Tests whether this is exactly the identity matrix.
	 *
	 * @return 若为单位矩阵则为 true / true if identity
	 */
	public boolean isIdentity() {
		return this.m00 == 1.0f && this.m01 == 0.0f && this.m02 == 0.0f && this.m03 == 0.0f && this.m10 == 0.0f
				&& this.m11 == 1.0f && this.m12 == 0.0f && this.m13 == 0.0f && this.m20 == 0.0f && this.m21 == 0.0f
				&& this.m22 == 1.0f && this.m23 == 0.0f && this.m30 == 0.0f && this.m31 == 0.0f && this.m32 == 0.0f
				&& this.m33 == 1.0f;
	}

	/**
	 * 按向量对各列分别缩放（列 0×X、列 1×Y、列 2×Z）。
	 * Scales each column by the corresponding vector component (col0×X, col1×Y, col2×Z).
	 *
	 * @param scale 缩放向量 / scale vector
	 */
	public void scale(Vector3f scale) {
		this.m00 *= scale.getX();
		this.m10 *= scale.getX();
		this.m20 *= scale.getX();
		this.m30 *= scale.getX();
		this.m01 *= scale.getY();
		this.m11 *= scale.getY();
		this.m21 *= scale.getY();
		this.m31 *= scale.getY();
		this.m02 *= scale.getZ();
		this.m12 *= scale.getZ();
		this.m22 *= scale.getZ();
		this.m32 *= scale.getZ();
	}

	/**
	 * 按统一标量缩放前三列。
	 * Scales the first three columns by a uniform scalar.
	 *
	 * @param scale 缩放因子 / scale factor
	 */
	public void scale(float scale) {
		this.m00 *= scale;
		this.m10 *= scale;
		this.m20 *= scale;
		this.m30 *= scale;
		this.m01 *= scale;
		this.m11 *= scale;
		this.m21 *= scale;
		this.m31 *= scale;
		this.m02 *= scale;
		this.m12 *= scale;
		this.m22 *= scale;
		this.m32 *= scale;
	}

	/**
	 * 在容差 1e-4 内判断矩阵是否为单位矩阵。
	 * Tests whether a matrix is identity within a 1e-4 tolerance.
	 *
	 * @param mat 待检测矩阵 / matrix to test
	 * @return 若近似单位矩阵则为 true / true if approximately identity
	 */
	static boolean equalIdentity(Matrix4f mat) {
		if ((double) Math.abs(mat.m00 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m11 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m22 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m33 - 1.0f) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m01) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m02) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m03) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m10) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m12) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m13) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m20) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m21) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m23) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m30) > 1.0E-4) {
			return false;
		}
		if ((double) Math.abs(mat.m31) > 1.0E-4) {
			return false;
		}
		return !((double) Math.abs(mat.m32) > 1.0E-4);
	}

	/**
	 * 浅克隆本矩阵。
	 * Shallow-clones this matrix.
	 *
	 * @return 克隆实例 / clone instance
	 */
	public Matrix4f clone() {
		try {
			return (Matrix4f) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
