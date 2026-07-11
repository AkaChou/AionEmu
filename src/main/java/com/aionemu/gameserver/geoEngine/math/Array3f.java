package com.aionemu.gameserver.geoEngine.math;

/**
 * 三个 float 的轻量容器，支持对象池复用。
 * Lightweight three-float container with object-pool reuse.
 */
public class Array3f implements Reusable {

	/** 对象工厂。 / Object factory. */
	private static final ObjectFactory<Object> FACTORY = new ObjectFactory<Object>() {

		public Object create() {
			return new Array3f();
		}
	};

	/** Component a / Component a */
	public float a = 0.0f;
	/** Component b / Component b */
	public float b = 0.0f;
	/** Component c / Component c */
	public float c = 0.0f;

	/**
	 * 将三分量重置为 0。
	 * Resets all three components to 0.
	 */
	public void reset() {
		this.a = 0.0f;
		this.b = 0.0f;
		this.c = 0.0f;
	}

	/**
	 * 从工厂获取实例。
	 * Obtains an instance from the factory.
	 *
	 * pooled instance
	 */
	public static Array3f newInstance() {
		return (Array3f) FACTORY.object();
	}

	/**
	 * 将实例回收到工厂。
	 * Recycles the instance into the factory.
	 *
	 * @param instance 待回收实例 / instance to recycle
	 */
	public static void recycle(Array3f instance) {
		FACTORY.recycle((Object) instance);
	}
}
