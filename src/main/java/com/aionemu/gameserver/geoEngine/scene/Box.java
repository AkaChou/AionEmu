package com.aionemu.gameserver.geoEngine.scene;

import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Type;
import com.aionemu.gameserver.geoEngine.utils.BufferUtils;

/**
 * 带实体（填充）面的盒子网格。
 * A box with solid (filled) faces.
 *
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar
 *          2009) $
 */
public class Box extends AbstractBox {

	/** 六个面的三角形索引数据。 / Triangle index data for the six faces. */
	private static final short[] GEOMETRY_INDICES_DATA = { 2, 1, 0, 3, 2, 0, // back
			6, 5, 4, 7, 6, 4, // right
			10, 9, 8, 11, 10, 8, // front
			14, 13, 12, 15, 14, 12, // left
			18, 17, 16, 19, 18, 16, // top
			22, 21, 20, 23, 22, 20 // bottom
	};
	/** 六个面的法线数据。 / Normal data for the six faces. */
	private static final float[] GEOMETRY_NORMALS_DATA = { 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, // back
			1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, // right
			0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, // front
			-1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, // left
			0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, // top
			0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0 // bottom
	};

	/**
	 * 创建中心在原点、各轴半长为给定值的盒子。
	 * Creates a new box centered at the origin with the given per-axis extents.
	 * <p>
	 * 半长 0.5 时即为单位立方体。
	 * An extent of 0.5 yields the unit cube.
	 *
	 * @param x X 轴半长（双向） / size along X in both directions
	 * @param y Y 轴半长（双向） / size along Y in both directions
	 * @param z Z 轴半长（双向） / size along Z in both directions
	 */
	public Box(float x, float y, float z) {
		super();
		updateGeometry(Vector3f.ZERO, x, y, z);
	}

	/**
	 * 创建指定中心与各轴半长的盒子。
	 * Creates a new box with the given center and per-axis extents.
	 * <p>
	 * 半长 0.5 时即为单位立方体。
	 * An extent of 0.5 yields the unit cube.
	 *
	 * @param center 盒子中心 / center of the box
	 * @param x X 轴半长（双向） / size along X in both directions
	 * @param y Y 轴半长（双向） / size along Y in both directions
	 * @param z Z 轴半长（双向） / size along Z in both directions
	 */
	public Box(Vector3f center, float x, float y, float z) {
		super();
		updateGeometry(center, x, y, z);
	}

	/**
	 * 按最小点与最大点构造盒子（定义形状与尺寸，不含朝向/平移）。
	 * Constructs a box from minimum and maximum points (shape and size only; orientation/position are separate).
	 *
	 * @param min 定义盒子的最小点 / minimum point defining the box
	 * @param max 定义盒子的最大点 / maximum point defining the box
	 */
	public Box(Vector3f min, Vector3f max) {
		super();
		updateGeometry(min, max);
	}

	/**
	 * 仅用于序列化的空构造，请勿在业务代码中使用。
	 * Empty constructor for serialization only. Do not use.
	 */
	public Box() {
		super();
	}

	/**
	 * 克隆本盒子（中心与半长相同）。
	 * Creates a clone of this box with the same center and extents.
	 *
	 * @return 克隆的盒子 / cloned box
	 */
	@Override
	public Box clone() {
		return new Box(center.clone(), xExtent, yExtent, zExtent);
	}

	/**
	 * 若尚无索引缓冲则写入固定索引数据。
	 * Writes fixed index data if no index buffer is present yet.
	 */
	@Override
	protected void duUpdateGeometryIndices() {
		if (getBuffer(Type.Index) == null) {
			setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(GEOMETRY_INDICES_DATA));
		}
	}

	/**
	 * 若尚无法线缓冲则写入固定法线数据。
	 * Writes fixed normal data if no normal buffer is present yet.
	 */
	@Override
	protected void duUpdateGeometryNormals() {
		if (getBuffer(Type.Normal) == null) {
			setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(GEOMETRY_NORMALS_DATA));
		}
	}

	/**
	 * 根据当前中心与半长重算顶点并更新包围体。
	 * Recomputes vertices from the current center/extents and updates the bound.
	 */
	@Override
	protected void duUpdateGeometryVertices() {
		FloatBuffer fpb = BufferUtils.createVector3Buffer(24);
		Vector3f[] v = computeVertices();
		fpb.put(new float[] { v[0].x, v[0].y, v[0].z, v[1].x, v[1].y, v[1].z, v[2].x, v[2].y, v[2].z, v[3].x, v[3].y,
				v[3].z, // back
				v[1].x, v[1].y, v[1].z, v[4].x, v[4].y, v[4].z, v[6].x, v[6].y, v[6].z, v[2].x, v[2].y, v[2].z, // right
				v[4].x, v[4].y, v[4].z, v[5].x, v[5].y, v[5].z, v[7].x, v[7].y, v[7].z, v[6].x, v[6].y, v[6].z, // front
				v[5].x, v[5].y, v[5].z, v[0].x, v[0].y, v[0].z, v[3].x, v[3].y, v[3].z, v[7].x, v[7].y, v[7].z, // left
				v[2].x, v[2].y, v[2].z, v[6].x, v[6].y, v[6].z, v[7].x, v[7].y, v[7].z, v[3].x, v[3].y, v[3].z, // top
				v[0].x, v[0].y, v[0].z, v[5].x, v[5].y, v[5].z, v[4].x, v[4].y, v[4].z, v[1].x, v[1].y, v[1].z // bottom
		});
		setBuffer(Type.Position, 3, fpb);
		updateBound();
	}
}
