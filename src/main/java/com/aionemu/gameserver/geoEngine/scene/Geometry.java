package com.aionemu.gameserver.geoEngine.scene;

import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 场景图中的几何体节点，持有网格数据并参与碰撞检测。
 * Scene-graph geometry node that holds mesh data and participates in collision detection.
 */
public class Geometry extends Spatial {

	/** 本节点包含的网格。 / The mesh contained herein. */
	protected Mesh mesh;
	/** 缓存的世界变换矩阵。 / Cached world transform matrix. */
	protected Matrix4f cachedWorldMat = new Matrix4f();

	/**
	 * 仅用于序列化的空构造，请勿在业务代码中使用。
	 * Do not use this constructor. Serialization purposes only.
	 */
	public Geometry() {
	}

	/**
	 * 创建无网格数据的几何体节点。
	 * Creates a geometry node without any mesh data.
	 *
	 * @param name 几何体名称 / name of this geometry
	 */
	public Geometry(String name) {
		super(name);
	}

	/**
	 * 创建带网格数据的几何体节点。
	 * Creates a geometry node with mesh data.
	 *
	 * @param name 几何体名称 / name of this geometry
	 * @param mesh 网格数据 / mesh data for this geometry
	 */
	public Geometry(String name, Mesh mesh) {
		this(name);
		if (mesh == null) {
			throw new NullPointerException();
		}

		this.mesh = mesh;
	}

	/**
	 * 返回网格顶点数。
	 * Returns the mesh vertex count.
	 *
	 * @return 顶点数 / vertex count
	 */
	@Override
	public int getVertexCount() {
		return mesh.getVertexCount();
	}

	/**
	 * 返回网格三角形数。
	 * Returns the mesh triangle count.
	 *
	 * @return 三角形数 / triangle count
	 */
	@Override
	public int getTriangleCount() {
		return mesh.getTriangleCount();
	}

	/**
	 * 设置网格。
	 * Sets the mesh.
	 *
	 * @param mesh 网格 / mesh
	 */
	public void setMesh(Mesh mesh) {

		this.mesh = mesh;
	}

	/**
	 * 获取网格。
	 * Gets the mesh.
	 *
	 * @return 网格 / mesh
	 */
	public Mesh getMesh() {
		return mesh;
	}

	/**
	 * 返回模型空间中的网格包围体。
	 * Returns the bounding volume of the mesh in model space.
	 *
	 * @return 模型空间包围体 / model-space bounding volume
	 */
	public BoundingVolume getModelBound() {
		return mesh.getBound();
	}

	/**
	 * 在网格被修改后更新包围体，并变换到世界空间。
	 * Updates the mesh bounding volume after modification and transforms it into world space.
	 */
	@Override
	public void updateModelBound() {
		mesh.updateBound();
		worldBound = getModelBound().transform(cachedWorldMat, worldBound);
	}

	/**
	 * 返回缓存的世界变换矩阵。
	 * Returns the cached world transform matrix.
	 *
	 * @return 世界矩阵 / world matrix
	 */
	public Matrix4f getWorldMatrix() {
		return cachedWorldMat;
	}

	/**
	 * 设置模型空间包围体到网格。
	 * Sets the model-space bounding volume on the mesh.
	 *
	 * @param modelBound 模型包围体 / model bounding volume
	 */
	@Override
	public void setModelBound(BoundingVolume modelBound) {
		mesh.setBound(modelBound);
	}

	/**
	 * 与可碰撞对象进行碰撞检测（射线先做包围体剔除）。
	 * Collides with another collidable (rays are first culled against the world bound).
	 *
	 * @param other 目标可碰撞对象 / target collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * @return 新增命中次数 / number of collisions added
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if (other instanceof Ray) {
			if (!worldBound.intersects(((Ray) other))) {
				return 0;
			}
		}
		// 注意：网格中的 BIHTree 已检查与网格边界的碰撞 / NOTE: BIHTree in mesh already checks collision with the mesh's bound
		int prevSize = results.size();
		int added = mesh.collideWith(other, cachedWorldMat, worldBound, results);
		int newSize = results.size();
		for (int i = prevSize; i < newSize; i++) {
			results.getCollisionDirect(i).setGeometry(this);
		}
		return added;
	}

	/**
	 * 用旋转、平移与均匀缩放设置世界变换。
	 * Sets the world transform from rotation, translation and uniform scale.
	 *
	 * @param rotation 旋转矩阵 / rotation matrix
	 * @param loc 平移 / translation
	 * @param scale 均匀缩放 / uniform scale
	 */
	@Override
	public void setTransform(Matrix3f rotation, Vector3f loc, float scale) {
		cachedWorldMat.loadIdentity();
		cachedWorldMat.setRotationMatrix(rotation);
		cachedWorldMat.scale(scale);
		cachedWorldMat.setTranslation(loc);
	}

	/**
	 * 用旋转、平移与非均匀缩放设置世界变换。
	 * Sets the world transform from rotation, translation and non-uniform scale.
	 *
	 * @param rotation 旋转矩阵 / rotation matrix
	 * @param loc 平移 / translation
	 * @param scale 各轴缩放 / per-axis scale
	 */
	@Override
	public void setTransform(Matrix3f rotation, Vector3f loc, Vector3f scale) {
		cachedWorldMat.loadIdentity();
		cachedWorldMat.setRotationMatrix(rotation);
		cachedWorldMat.scale(scale);
		cachedWorldMat.setTranslation(loc);
	}

	/**
	 * 返回网格碰撞标志。
	 * Returns the mesh collision flags.
	 *
	 * @return 碰撞标志 / collision flags
	 */
	@Override
	public short getCollisionFlags() {
		return mesh.getCollisionFlags();
	}

	/**
	 * 设置网格碰撞标志。
	 * Sets the mesh collision flags.
	 *
	 * @param flags 碰撞标志 / collision flags
	 */
	@Override
	public void setCollisionFlags(short flags) {
		mesh.setCollisionFlags(flags);
	}
}
