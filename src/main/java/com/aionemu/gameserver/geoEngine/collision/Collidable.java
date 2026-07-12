package com.aionemu.gameserver.geoEngine.collision;

/**
 * 可碰撞对象接口，geoEngine 中所有参与碰撞检测的空间对象均实现此接口。
 * Interface for collidable objects; all spatials that participate in collision
 * detection in geoEngine implement this.
 *
 * @author Kirill
 */
public interface Collidable {

	/**
	 * 与另一可碰撞对象做碰撞检测，结果写入 {@code results}。
	 * Checks collision with another collidable and appends hits into {@code results}.
	 *
	 * @param other 另一可碰撞对象 / the other collidable
	 * @param results 碰撞结果收集器 / collector for collision hits
	 * @return 检测到的碰撞次数 / how many collisions were found
	 * unsupported collidable pair。
	 */
	public int collideWith(Collidable other, CollisionResults results) throws UnsupportedCollisionException;
}
