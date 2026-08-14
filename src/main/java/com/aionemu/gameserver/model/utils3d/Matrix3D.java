package com.aionemu.gameserver.model.utils3d;

/**
 * 3x3 矩阵，用于 3D 工具逻辑。
 * Matrix 3D for utils 3D logic.
 *
 * @author M@xx
 */
public class Matrix3D {

	/** 单位矩阵 / identity matrix */
	public static final double[][] IDENTITY = new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };

	private double[][] data;

	public Matrix3D() {
		this.data = new double[3][3];
	}

	public Matrix3D(double[][] data) {
		this();
		if (data.length != 3) {
			throw new RuntimeException("Invalid matrix dimensions");
		}

		for (int i = 0; i < 3; i++) {
			if (data[i].length != 3) {
				throw new RuntimeException("Invalid matrix dimensions");
			}
			System.arraycopy(data[i], 0, this.data[i], 0, 3);
		}
	}

	/** 替换指定列并返回新矩阵。 / Replaces a column and returns a new matrix. */
	public Matrix3D replaceColumn(int i, double[] newColumn) {
		if (i > 3 || i < 0) {
			throw new RuntimeException("Invalid column index " + i);
		}
		if (newColumn.length > 3) {
			throw new RuntimeException("Invalid column dimension");
		}
		Matrix3D B = new Matrix3D(data);
		for (int j = 0; j < 3; j++) {
			B.data[j][i] = newColumn[j];
		}
		return B;
	}

	/** 矩阵相乘 / multiply. */
	public Matrix3D multiply(Matrix3D B) {
		Matrix3D C = new Matrix3D();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 3; k++) {
					C.data[i][j] += (data[i][k] * B.data[k][j]);
				}
			}
		}
		return C;
	}

	/** 与标量相乘 / multiply by a scalar. */
	public Matrix3D multiply(double b) {
		Matrix3D C = new Matrix3D();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				C.data[i][j] = b * data[i][j];
			}
		}
		return C;
	}

	/** 计算行列式。 / Computes the determinant. */
	public double determinant() {
		double aei = data[0][0] * data[1][1] * data[2][2];
		double bfg = data[0][1] * data[1][2] * data[2][0];
		double cdh = data[0][2] * data[1][0] * data[2][1];
		double afh = data[0][0] * data[1][2] * data[2][1];
		double bdi = data[0][1] * data[1][0] * data[2][2];
		double ceg = data[0][2] * data[1][1] * data[2][0];
		return (aei + bfg + cdh - afh - bdi - ceg);
	}

	/** 计算逆矩阵。 / Computes the inverse matrix. */
	public Matrix3D inverse() {
		if (Math.abs(determinant()) <= Double.MIN_VALUE) {
			throw new RuntimeException("Matrix not inversible");
		}
		return adjugate().multiply(1 / determinant());
	}

	/** 计算伴随矩阵。 / Computes the adjugate matrix. */
	public Matrix3D adjugate() {
		Matrix3D adj = new Matrix3D();
		adj.data[0][0] = data[1][1] * data[2][2] - data[1][2] * data[2][1];
		adj.data[0][1] = -(data[0][1] * data[2][2] - data[0][2] * data[2][1]);
		adj.data[0][2] = data[0][1] * data[1][2] - data[0][2] * data[1][1];
		adj.data[1][0] = -(data[1][0] * data[2][2] - data[1][2] * data[2][0]);
		adj.data[1][1] = data[0][0] * data[2][2] - data[0][2] * data[2][0];
		adj.data[1][2] = -(data[0][0] * data[1][2] - data[0][2] * data[1][0]);
		adj.data[2][0] = data[1][0] * data[2][1] - data[1][1] * data[2][0];
		adj.data[2][1] = -(data[0][0] * data[2][1] - data[0][1] * data[2][0]);
		adj.data[2][2] = data[0][0] * data[1][1] - data[0][1] * data[1][0];
		return adj;
	}

	/** 矩阵与向量相乘 / multiply by a vector. */
	public double[] multiply(double[] v) {
		if (v.length != 3) {
			throw new RuntimeException("Vector dimensions invalid");
		}

		double[] result = new double[] { data[0][0] * v[0] + data[0][1] * v[1] + data[0][2] * v[2],
				data[1][0] * v[0] + data[1][1] * v[1] + data[1][2] * v[2],
				data[2][0] * v[0] + data[2][1] * v[1] + data[2][2] * v[2] };

		return result;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < 3; i++) {
			s += "[ ";
			for (int j = 0; j < 3; j++) {
				s += String.format("%+4.4f ", data[i][j]);
			}
			s += "]\n";
		}
		return s;
	}
}
