package com.aionemu.gameserver.geoEngine.scene;

import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 八顶点轴对齐盒子基类。
 * An eight-sided axis-aligned box base class.
 * <p>
 * 由中心点与各轴半长（extent）定义，并据此计算八个顶点。
 * A {@code Box} is defined by a center and per-axis extents; the eight vertices are computed to form an axis-aligned box.
 * <p>
 * 本类不控制几何数据如何生成，具体实现见 {@link Box}。
 * This class does not control how geometry data is generated; see {@link Box} for that.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar
 *          2009) $
 */
public abstract class AbstractBox extends Mesh {

	/** 盒子中心。 / Box center. */
	public final Vector3f center = new Vector3f(0f, 0f, 0f);
	/** X/Y/Z 轴半长。 / Half-extents along X/Y/Z. */
	public float xExtent, yExtent, zExtent;

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public AbstractBox() {
		super();
	}

	/**
	 * 计算表示盒子 8 个顶点的向量数组。
	 * Computes the array of vectors representing the 8 vertices of the box.
	 *
	 * @return 新创建的顶点向量数组 / a newly created array of vertex vectors
	 */
	protected final Vector3f[] computeVertices() {
		Vector3f[] axes = { Vector3f.UNIT_X.mult(xExtent), Vector3f.UNIT_Y.mult(yExtent),
				Vector3f.UNIT_Z.mult(zExtent) };
		return new Vector3f[] { center.subtract(axes[0]).subtractLocal(axes[1]).subtractLocal(axes[2]),
				center.add(axes[0]).subtractLocal(axes[1]).subtractLocal(axes[2]),
				center.add(axes[0]).addLocal(axes[1]).subtractLocal(axes[2]),
				center.subtract(axes[0]).addLocal(axes[1]).subtractLocal(axes[2]),
				center.add(axes[0]).subtractLocal(axes[1]).addLocal(axes[2]),
				center.subtract(axes[0]).subtractLocal(axes[1]).addLocal(axes[2]),
				center.add(axes[0]).addLocal(axes[1]).addLocal(axes[2]),
				center.subtract(axes[0]).addLocal(axes[1]).addLocal(axes[2]) };
	}

	/**
	 * 将顶点索引写入定义盒子几何的索引列表。
	 * Converts indices into the list of vertices that define the box's geometry.
	 */
	protected abstract void duUpdateGeometryIndices();

	/**
	 * 更新盒子各面的法线。
	 * Updates the normals of each of the box's planes.
	 */
	protected abstract void duUpdateGeometryNormals();

	/**
	 * 根据最小/最大点更新定义盒子的顶点位置。
	 * Updates the positions of the vertices that define the box from min/max extents.
	 */
	protected abstract void duUpdateGeometryVertices();

	/**
	 * 获取盒子中心点。
	 * Gets the center point of this box.
	 *
	 * @return 盒子中心点 / center point
	 */
	public final Vector3f getCenter() {
		return center;
	}

	/**
	 * 获取 X 轴半长。
	 * Gets the x-axis size (extent) of this box.
	 *
	 * @return X 轴半长 / x extent
	 */
	public final float getXExtent() {
		return xExtent;
	}

	/**
	 * 获取 Y 轴半长。
	 * Gets the y-axis size (extent) of this box.
	 *
	 * @return Y 轴半长 / y extent
	 */
	public final float getYExtent() {
		return yExtent;
	}

	/**
	 * 获取 Z 轴半长。
	 * Gets the z-axis size (extent) of this box.
	 *
	 * @return Z 轴半长 / z extent
	 */
	public final float getZExtent() {
		return zExtent;
	}

	/**
	 * 在直接修改属性后重建盒子几何。
	 * Rebuilds the box after a property has been directly altered.
	 * <p>
	 * 例如直接改写 extent 后需调用本方法以刷新几何。
	 * For example, after mutating extents directly, call this method to refresh geometry.
	 */
	public final void updateGeometry() {
		duUpdateGeometryVertices();
		duUpdateGeometryNormals();
		duUpdateGeometryIndices();
	}

	/**
	 * 按中心与各轴半长重建盒子。
	 * Rebuilds this box from a center and per-axis extents.
	 * <p>
	 * 实际边长为半长的两倍，因盒子从中心向两侧延伸。
	 * Note that actual side lengths are twice the given extents because the box extends both ways from the center.
	 *
	 * @param center 盒子中心 / center of the box
	 * @param x X 方向半长 / x extent in each direction
	 * @param y Y 方向半长 / y extent in each direction
	 * @param z Z 方向半长 / z extent in each direction
	 */
	public final void updateGeometry(Vector3f center, float x, float y, float z) {
		if (center != null) {
			this.center.set(center);
		}
		this.xExtent = x;
		this.yExtent = y;
		this.zExtent = z;
		updateGeometry();
	}

	/**
	 * 按最小点与最大点重建盒子。
	 * Rebuilds this box from a minimum and maximum corner.
	 * <p>
	 * 盒子更新为以 {@code minPoint} 与 {@code maxPoint} 为对角，其余顶点由二者推导。
	 * The box is updated so opposite corners are {@code minPoint} and {@code maxPoint}; other corners are derived from them.
	 *
	 * @param minPoint 新的最小点 / new minimum point of the box
	 * @param maxPoint 新的最大点 / new maximum point of the box
	 */
	public final void updateGeometry(Vector3f minPoint, Vector3f maxPoint) {
		center.set(maxPoint).addLocal(minPoint).multLocal(0.5f);
		float x = maxPoint.x - center.x;
		float y = maxPoint.y - center.y;
		float z = maxPoint.z - center.z;
		updateGeometry(center, x, y, z);
	}
}
