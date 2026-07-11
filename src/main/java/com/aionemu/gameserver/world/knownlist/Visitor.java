package com.aionemu.gameserver.world.knownlist;

/**
 * 已知列表遍历回调。
 * Callback for iterating known-list entries.
 *
 * @param <T> 被访问对象类型 / visited object type
 * @author ATracer
 */
public interface Visitor<T> {

	/**
	 * 访问单个对象。
	 * Visits a single object.
	 *
	 * @param object 被访问对象 / visited object
	 */
	void visit(T object);
}
