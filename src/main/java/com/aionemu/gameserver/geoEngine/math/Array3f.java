/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  object factory
 *  reusable object
 */
package com.aionemu.gameserver.geoEngine.math;

public class Array3f implements Reusable {
	private static final ObjectFactory<Object> FACTORY = new ObjectFactory<Object>() {

		public Object create() {
			return new Array3f();
		}
	};
	public float a = 0.0f;
	public float b = 0.0f;
	public float c = 0.0f;

	public void reset() {
		this.a = 0.0f;
		this.b = 0.0f;
		this.c = 0.0f;
	}

	public static Array3f newInstance() {
		return (Array3f) FACTORY.object();
	}

	public static void recycle(Array3f instance) {
		FACTORY.recycle((Object) instance);
	}
}
