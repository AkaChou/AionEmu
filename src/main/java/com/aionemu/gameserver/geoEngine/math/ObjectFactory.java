package com.aionemu.gameserver.geoEngine.math;

/**
 * 简单对象工厂，支持创建与回收（回收时若实现 {@link Reusable} 则重置）。
 * Simple object factory supporting create and recycle (resets if {@link Reusable}).
 *
 * @param <T> 产物类型 / product type
 */
abstract class ObjectFactory<T> {

	/**
	 * 获取（创建）一个实例。
	 * Obtains (creates) an instance.
	 *
	 * new instance
	 */
	public T object() {
		return create();
	}

	/**
	 * 回收实例；若实现 {@link Reusable} 则先 {@link Reusable#reset()}。
	 * Recycles an instance; resets first if it implements {@link Reusable}.
	 *
	 * @param object 待回收对象 / object to recycle
	 */
	public void recycle(T object) {
		if (object instanceof Reusable reusable) {
			reusable.reset();
		}
	}

	/**
	 * 创建新实例。
	 * Creates a new instance.
	 *
	 * new instance
	 */
	protected abstract T create();
}
