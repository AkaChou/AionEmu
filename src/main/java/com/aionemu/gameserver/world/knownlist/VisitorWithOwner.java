package com.aionemu.gameserver.world.knownlist;

/**
 * 携带所有者上下文的已知列表遍历回调。
 * Known-list visitor callback that also receives the owner context.
 *
 * @param <T> 被访问对象类型 / visited object type
 * @param <V> 所有者类型 / owner type
 * @author ATracer
 */
public interface VisitorWithOwner<T, V> {

	/**
	 * 访问单个对象及其所有者。
	 * Visits a single object together with its owner.
	 *
	 * @param object 被访问对象 / visited object
	 * @param owner 所有者 / owner
	 */
	void visit(T object, V owner);
}
