package com.aionemu.gameserver.geoEngine.math;

/**
 * 3×3 对称矩阵的特征值与特征向量分解器。
 * Eigenvalue and eigenvector decomposer for 3×3 symmetric matrices.
 */
public class Eigen3f {
	/** 特征值数组。 / Eigenvalue array. */
	float[] eigenValues = new float[3];
	/** 特征向量数组。 / Eigenvector array. */
	Vector3f[] eigenVectors = new Vector3f[3];
	/** 三分之一（双精度）。 / One third (double precision). */
	static final double ONE_THIRD_DOUBLE = 0.3333333333333333;
	/** √3（双精度）。 / Square root of three (double precision). */
	static final double ROOT_THREE_DOUBLE = Math.sqrt(3.0);

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public Eigen3f() {
	}

	/**
	 * 使用给定矩阵立即计算特征分解。
	 * Immediately computes the eigen decomposition for the given matrix.
	 *
	 * @param data 输入 3×3 矩阵 / input 3×3 matrix
	 */
	public Eigen3f(Matrix3f data) {
		this.calculateEigen(data);
	}

	/**
	 * 计算给定 3×3 对称矩阵的特征值与特征向量。
	 * Computes the eigenvalues and eigenvectors of the given 3×3 symmetric matrix.
	 *
	 * @param data 输入 3×3 矩阵 / input 3×3 matrix
	 */
	public void calculateEigen(Matrix3f data) {
		this.eigenVectors[0] = new Vector3f();
		this.eigenVectors[1] = new Vector3f();
		this.eigenVectors[2] = new Vector3f();
		Matrix3f scaledData = new Matrix3f(data);
		float maxMagnitude = this.scaleMatrix(scaledData);
		double[] roots = new double[3];
		this.computeRoots(scaledData, roots);
		this.eigenValues[0] = (float) roots[0];
		this.eigenValues[1] = (float) roots[1];
		this.eigenValues[2] = (float) roots[2];
		float[] maxValues = new float[3];
		Vector3f[] maxRows = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f() };
		for (int i = 0; i < 3; ++i) {
			Matrix3f tempMatrix = new Matrix3f(scaledData);
			tempMatrix.m00 -= this.eigenValues[i];
			tempMatrix.m11 -= this.eigenValues[i];
			tempMatrix.m22 -= this.eigenValues[i];
			float[] val = new float[] { maxValues[i] };
			if (!this.positiveRank(tempMatrix, val, maxRows[i])) {
				if (maxMagnitude > 1.0f) {
					int j = 0;
					while (j < 3) {
						int n = j++;
						this.eigenValues[n] = this.eigenValues[n] * maxMagnitude;
					}
				}
				this.eigenVectors[0].set(Vector3f.UNIT_X);
				this.eigenVectors[1].set(Vector3f.UNIT_Y);
				this.eigenVectors[2].set(Vector3f.UNIT_Z);
				return;
			}
			maxValues[i] = val[0];
		}
		float maxCompare = maxValues[0];
		int i = 0;
		if (maxValues[1] > maxCompare) {
			maxCompare = maxValues[1];
			i = 1;
		}
		if (maxValues[2] > maxCompare) {
			i = 2;
		}
		switch (i) {
		case 0: {
			maxRows[0].normalizeLocal();
			this.computeVectors(scaledData, maxRows[0], 1, 2, 0);
			break;
		}
		case 1: {
			maxRows[1].normalizeLocal();
			this.computeVectors(scaledData, maxRows[1], 2, 0, 1);
			break;
		}
		case 2: {
			maxRows[2].normalizeLocal();
			this.computeVectors(scaledData, maxRows[2], 0, 1, 2);
		}
		}
		if (maxMagnitude > 1.0f) {
			i = 0;
			while (i < 3) {
				int n = i++;
				this.eigenValues[n] = this.eigenValues[n] * maxMagnitude;
			}
		}
	}

	/**
	 * 按最大元素幅值缩放矩阵，提高数值稳定性。
	 * Scales the matrix by its max element magnitude for numerical stability.
	 *
	 * @param mat 待缩放矩阵（原地修改） / matrix to scale (modified in place)
	 * @return 缩放前的最大幅值 / max magnitude before scaling
	 */
	private float scaleMatrix(Matrix3f mat) {
		float max = FastMath.abs(mat.m00);
		float abs = FastMath.abs(mat.m01);
		if (abs > max) {
			max = abs;
		}
		if ((abs = FastMath.abs(mat.m02)) > max) {
			max = abs;
		}
		if ((abs = FastMath.abs(mat.m11)) > max) {
			max = abs;
		}
		if ((abs = FastMath.abs(mat.m12)) > max) {
			max = abs;
		}
		if ((abs = FastMath.abs(mat.m22)) > max) {
			max = abs;
		}
		if (max > 1.0f) {
			float fInvMax = 1.0f / max;
			mat.multLocal(fInvMax);
		}
		return max;
	}

	/**
	 * 根据最大行向量计算全部特征向量。
	 * Computes all eigenvectors from the dominant row vector.
	 *
	 * @param mat 缩放后的矩阵 / scaled matrix
	 * @param vect 主导行向量 / dominant row vector
	 * @param index1 第一特征向量索引 / first eigenvector index
	 * @param index2 第二特征向量索引 / second eigenvector index
	 * @param index3 第三特征向量索引 / third eigenvector index
	 */
	private void computeVectors(Matrix3f mat, Vector3f vect, int index1, int index2, int index3) {
		float invLength;
		Vector3f vectorU = new Vector3f();
		Vector3f vectorV = new Vector3f();
		Vector3f.generateComplementBasis(vectorU, vectorV, vect);
		Vector3f tempVect = mat.mult(vectorU);
		float p00 = this.eigenValues[index3] - vectorU.dot(tempVect);
		float p01 = vectorV.dot(tempVect);
		float p11 = this.eigenValues[index3] - vectorV.dot(mat.mult(vectorV));
		float max = FastMath.abs(p00);
		boolean row = false;
		float fAbs = FastMath.abs(p01);
		if (fAbs > max) {
			max = fAbs;
		}
		if ((fAbs = FastMath.abs(p11)) > max) {
			max = fAbs;
			row = true;
		}
		if (max >= 1.0E-4f) {
			if (!row) {
				invLength = FastMath.invSqrt(p00 * p00 + p01 * p01);
				vectorU.mult(p01 *= invLength, this.eigenVectors[index3]).addLocal(vectorV.mult(p00 *= invLength));
			} else {
				invLength = FastMath.invSqrt(p11 * p11 + p01 * p01);
				vectorU.mult(p11 *= invLength, this.eigenVectors[index3]).addLocal(vectorV.mult(p01 *= invLength));
			}
		} else {
			this.eigenVectors[index3] = !row ? vectorV : vectorU;
		}
		Vector3f vectorS = vect.cross(this.eigenVectors[index3]);
		mat.mult(vect, tempVect);
		p00 = this.eigenValues[index1] - vect.dot(tempVect);
		p01 = vectorS.dot(tempVect);
		p11 = this.eigenValues[index1] - vectorS.dot(mat.mult(vectorS));
		max = FastMath.abs(p00);
		row = false;
		fAbs = FastMath.abs(p01);
		if (fAbs > max) {
			max = fAbs;
		}
		if ((fAbs = FastMath.abs(p11)) > max) {
			max = fAbs;
			row = true;
		}
		if (max >= 1.0E-4f) {
			if (!row) {
				invLength = FastMath.invSqrt(p00 * p00 + p01 * p01);
				this.eigenVectors[index1] = vect.mult(p01 *= invLength).add(vectorS.mult(p00 *= invLength));
			} else {
				invLength = FastMath.invSqrt(p11 * p11 + p01 * p01);
				this.eigenVectors[index1] = vect.mult(p11 *= invLength).add(vectorS.mult(p01 *= invLength));
			}
		} else if (!row) {
			this.eigenVectors[index1].set(vectorS);
		} else {
			this.eigenVectors[index1].set(vect);
		}
		this.eigenVectors[index3].cross(this.eigenVectors[index1], this.eigenVectors[index2]);
	}

	/**
	 * 判断矩阵是否具有正秩，并记录最大行。
	 * Tests whether the matrix has positive rank and records the dominant row.
	 *
	 * @param matrix 输入矩阵 / input matrix
	 * @param maxMagnitudeStore 最大幅值输出 [0] / max magnitude output [0]
	 * @param maxRowStore 最大行向量输出 / dominant row vector output
	 * @return 若最大幅值 ≥ 容差则为 true / true if max magnitude ≥ tolerance
	 */
	private boolean positiveRank(Matrix3f matrix, float[] maxMagnitudeStore, Vector3f maxRowStore) {
		maxMagnitudeStore[0] = -1.0f;
		int iMaxRow = -1;
		for (int iRow = 0; iRow < 3; ++iRow) {
			for (int iCol = iRow; iCol < 3; ++iCol) {
				float fAbs = FastMath.abs(matrix.get(iRow, iCol));
				if (!(fAbs > maxMagnitudeStore[0]))
					continue;
				maxMagnitudeStore[0] = fAbs;
				iMaxRow = iRow;
			}
		}
		maxRowStore.set(matrix.getRow(iMaxRow));
		return maxMagnitudeStore[0] >= 1.0E-4f;
	}

	/**
	 * 计算特征多项式的三个实根（升序）。
	 * Computes the three real roots of the characteristic polynomial (ascending order).
	 *
	 * @param mat 输入矩阵 / input matrix
	 * @param rootsStore 根存储数组（长度 3） / root storage array (length 3)
	 */
	private void computeRoots(Matrix3f mat, double[] rootsStore) {
		double mbDiv2;
		double q;
		double a = mat.m00;
		double b = mat.m01;
		double c = mat.m02;
		double d = mat.m11;
		double e = mat.m12;
		double f = mat.m22;
		double char0 = a * d * f + 2.0 * b * c * e - a * e * e - d * c * c - f * b * b;
		double char1 = a * d - b * b + a * f - c * c + d * f - e * e;
		double char2 = a + d + f;
		double char2Div3 = char2 * 0.3333333333333333;
		double abcDiv3 = (char1 - char2 * char2Div3) * 0.3333333333333333;
		if (abcDiv3 > 0.0) {
			abcDiv3 = 0.0;
		}
		if ((q = (mbDiv2 = 0.5 * (char0 + char2Div3 * (2.0 * char2Div3 * char2Div3 - char1))) * mbDiv2
				+ abcDiv3 * abcDiv3 * abcDiv3) > 0.0) {
			q = 0.0;
		}
		double magnitude = Math.sqrt(-abcDiv3);
		double angle = Math.atan2(Math.sqrt(-q), mbDiv2) * 0.3333333333333333;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double root0 = char2Div3 + 2.0 * magnitude * cos;
		double root1 = char2Div3 - magnitude * (cos + ROOT_THREE_DOUBLE * sin);
		double root2 = char2Div3 - magnitude * (cos - ROOT_THREE_DOUBLE * sin);
		if (root1 >= root0) {
			rootsStore[0] = root0;
			rootsStore[1] = root1;
		} else {
			rootsStore[0] = root1;
			rootsStore[1] = root0;
		}
		if (root2 >= rootsStore[1]) {
			rootsStore[2] = root2;
		} else {
			rootsStore[2] = rootsStore[1];
			if (root2 >= rootsStore[0]) {
				rootsStore[1] = root2;
			} else {
				rootsStore[1] = rootsStore[0];
				rootsStore[0] = root2;
			}
		}
	}

	/**
	 * 获取指定索引的特征值。
	 * Returns the eigenvalue at the given index.
	 *
	 * @param i 索引（0~2） / index (0~2)
	 * @return 特征值 / eigenvalue
	 */
	public float getEigenValue(int i) {
		return this.eigenValues[i];
	}

	/**
	 * 获取指定索引的特征向量。
	 * Returns the eigenvector at the given index.
	 *
	 * @param i 索引（0~2） / index (0~2)
	 * @return 特征向量 / eigenvector
	 */
	public Vector3f getEigenVector(int i) {
		return this.eigenVectors[i];
	}

	/**
	 * 获取全部特征值数组。
	 * Returns the full eigenvalue array.
	 *
	 * @return 特征值数组 / eigenvalue array
	 */
	public float[] getEigenValues() {
		return this.eigenValues;
	}

	/**
	 * 获取全部特征向量数组。
	 * Returns the full eigenvector array.
	 *
	 * @return 特征向量数组 / eigenvector array
	 */
	public Vector3f[] getEigenVectors() {
		return this.eigenVectors;
	}
}
