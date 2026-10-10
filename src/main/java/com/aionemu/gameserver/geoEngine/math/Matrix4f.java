package com.aionemu.gameserver.geoEngine.math;

import com.aionemu.boot.i18n.I18n;

import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;

/**
 * 4×4 矩阵，用于仿射/投影变换。
 * 4×4 matrix for affine/projective transforms.
 */
@Slf4j
@AllArgsConstructor
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

	/**
	 * 构造单位矩阵。
	 * Constructs an identity matrix.
	 */
	public Matrix4f() {
		this.loadIdentity();
	}

	/**
	 * 从另一矩阵拷贝元素；null 时加载单位矩阵。
	 * Copies elements from another matrix; loads identity if null.
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
	 * @param matrix 目标数组，长度须为 16 / destination array of length 16
	 */
	public void get(float[] matrix) {
		this.get(matrix, true);
	}

	/**
	 * 将矩阵元素写入 float 数组。
	 * Writes matrix elements into a float array.
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
	 * 设置指定行列的元素。
	 * Sets the element at the given row and column.
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
	 * @param matrix 长度 16 的元素数组 / element array of length 16
	 */
	public void set(float[] matrix) {
		this.set(matrix, true);
	}

	/**
	 * 从 float 数组设置矩阵。
	 * Sets the matrix from a float array.
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
	 * 原地将矩阵各元素乘以标量。
	 * Multiplies each matrix element by a scalar in place.
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
	 * @param in2 右乘矩阵 / right-hand matrix
	 * @return 乘积矩阵 / product matrix
	 */
	public Matrix4f mult(Matrix4f in2) {
		return this.mult(in2, null);
	}

	/**
	 * 将本矩阵右乘另一矩阵的结果写入 store。
	 * Stores this matrix times another matrix into store.
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
	 * @param in2 右乘矩阵 / right-hand matrix
	 * @return 本矩阵（链式调用） / this matrix
	 */
	public Matrix4f multLocal(Matrix4f in2) {
		return this.mult(in2, this);
	}

	/**
	 * 用本矩阵变换三维向量（含平移），分配新向量。
	 * Transforms a 3D vector by this matrix (including translation), allocating a new vector.
	 * @param vec 输入向量 / input vector
	 * @return 变换后的向量 / transformed vector
	 */
	public Vector3f mult(Vector3f vec) {
		return this.mult(vec, null);
	}

	/**
	 * 用本矩阵变换三维向量（含平移），结果写入 store。
	 * Transforms a 3D vector by this matrix (including translation) into store.
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
	 * 用本矩阵做投影变换，三维结果写入 store，返回 w 分量。
	 * Applies a projective transform; stores XYZ in store and returns the W component.
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
	 * 用本矩阵原地变换四维向量。
	 * Transforms a 4D vector by this matrix in place.
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
	 * 返回本矩阵的逆矩阵（新实例）。
	 * Returns the inverse of this matrix (new instance).
	 * @return 逆矩阵 / inverse matrix
	 * @throws ArithmeticException 矩阵不可逆 / if the matrix is singular
	 */
	public Matrix4f invert() {
		return this.invert(null);
	}

	/**
	 * 将本矩阵的逆写入 store。
	 * Stores the inverse of this matrix into store.
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
	 * 将矩阵全部元素置零。
	 * Zeros all matrix elements.
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
	 * 提取左上 3×3 旋转矩阵到已有矩阵。
	 * Extracts the upper-left 3×3 rotation matrix into an existing matrix.
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
	 * 按向量缩放对角线上的缩放因子。
	 * Scales the diagonal scale factors by a vector.
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
	 * 从向量设置平移。
	 * Sets the translation from a vector.
	 * @param translation 平移向量 / translation vector
	 */
	public void setTranslation(Vector3f translation) {
		this.m03 = translation.x;
		this.m13 = translation.y;
		this.m23 = translation.z;
	}

	/**
	 * 返回矩阵的可读字符串表示。
	 * Returns a human-readable string representation of the matrix.
	 * @return 格式化字符串 / formatted string
	 */
	public String toString() {
		return "Matrix4f\n[\n" + " " +
			this.m00 +
			"  " +
			this.m01 +
			"  " +
			this.m02 +
			"  " +
			this.m03 +
			" \n" +
			" " +
			this.m10 +
			"  " +
			this.m11 +
			"  " +
			this.m12 +
			"  " +
			this.m13 +
			" \n" +
			" " +
			this.m20 +
			"  " +
			this.m21 +
			"  " +
			this.m22 +
			"  " +
			this.m23 +
			" \n" +
			" " +
			this.m30 +
			"  " +
			this.m31 +
			"  " +
			this.m32 +
			"  " +
			this.m33 +
			" \n]";
	}

	/**
	 * 计算矩阵哈希码。
	 * Computes the matrix hash code.
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
	 * @param o 比较对象 / object to compare
	 * @return 全部元素相等则为 true / true if all elements are equal
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Matrix4f comp)) {
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
	 * 判断是否精确为单位矩阵。
	 * Tests whether this is exactly the identity matrix.
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
	 * 浅克隆本矩阵。
	 * Shallow-clones this matrix.
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
