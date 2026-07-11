package com.aionemu.gameserver.geoEngine.scene;

import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;

/**
 * 用于包围体与射线之间三角形级精确碰撞检测的接口。
 * Interface for triangle-accurate collision between bounding volumes and rays.
 *
 * @author Kirill Vainer
 */
public interface CollisionData {

	/**
	 * 与可碰撞对象进行碰撞检测并写入结果集。
	 * Collides with another collidable and writes hits into the results set.
	 *
	 * @param other 目标可碰撞对象 / target collidable
	 * @param worldMatrix 世界变换矩阵 / world transform matrix
	 * @param worldBound 世界空间包围体 / world-space bounding volume
	 * @param results 碰撞结果收集器 / collision results collector
	 * number of collisions found
	 */
	public int collideWith(Collidable other, Matrix4f worldMatrix, BoundingVolume worldBound, CollisionResults results);
}
