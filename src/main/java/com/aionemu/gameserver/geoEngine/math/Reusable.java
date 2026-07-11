package com.aionemu.gameserver.geoEngine.math;

/**
 * 可重置对象接口，供对象池回收前清理状态。
 * Marker for objects that can be reset before pool recycle.
 */
interface Reusable {

	/**
	 * 重置到可复用的初始状态。
	 * Resets this instance to a reusable initial state.
	 */
	void reset();
}
