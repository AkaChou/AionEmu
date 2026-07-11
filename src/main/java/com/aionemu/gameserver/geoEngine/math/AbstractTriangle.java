package com.aionemu.gameserver.geoEngine.math;

import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;

/**
 * 三角形抽象基类，提供三个顶点访问与碰撞转发。
 * Abstract triangle base providing three vertices and collision dispatch.
 */
public abstract class AbstractTriangle implements Collidable {

	/**
	 * 返回第一个顶点。
	 * Returns the first vertex.
	 *
	 * vertex 1
	 */
	public abstract Vector3f get1();

	/**
	 * 返回第二个顶点。
	 * Returns the second vertex.
	 *
	 * vertex 2
	 */
	public abstract Vector3f get2();

	/**
	 * 返回第三个顶点。
	 * Returns the third vertex.
	 *
	 * vertex 3
	 */
	public abstract Vector3f get3();

	/**
	 * 设置三个顶点。
	 * Sets the three vertices.
	 *
	 * vertex 1
	 * vertex 2
	 * vertex 3
	 */
	public abstract void set(Vector3f var1, Vector3f var2, Vector3f var3);

	/**
	 * 与另一可碰撞体做碰撞检测（委托给对方）。
	 * Collides with another collidable (delegates to the other).
	 *
	 * @param other 另一可碰撞体 / other collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * hit count
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		return other.collideWith(this, results);
	}
}
