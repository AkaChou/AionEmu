package com.aionemu.gameserver.geoEngine.math;

abstract class ObjectFactory<T> {

	public T object() {
		return create();
	}

	public void recycle(T object) {
		if (object instanceof Reusable reusable) {
			reusable.reset();
		}
	}

	protected abstract T create();
}
