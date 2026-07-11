package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.world.World;

/**
 * 可见对象控制器基类，管理对象的生成与消失。
 * Base controller for visible objects, managing spawn and despawn.
 *
 * @author ATracer
 * @param <T> 所有者可见对象类型 / owner visible object type
 */
public abstract class VisibleObjectController<T extends VisibleObject> {

	/** 控制器所有者对象。 / Owner object of this controller. */
	private T owner;

	/**
	 * 设置所有者对象。
	 * Sets the owner object.
	 *
	 * owner
	 */
	public void setOwner(T owner) {
		this.owner = owner;
	}

	/**
	 * 获取所有者对象。
	 * Gets the owner object.
	 *
	 * owner
	 */
	public T getOwner() {
		return owner;
	}

	/**
	 * 当另一个可见对象进入本对象视野时回调。
	 * Callback when another visible object enters this object's sight.
	 *
	 * @param object 进入视野的对象 / the object entering sight
	 */
	public void see(VisibleObject object) {
	}

	/**
	 * 当另一个可见对象离开本对象视野时回调。
	 * Callback when another visible object leaves this object's sight.
	 *
	 * @param object 离开视野的对象 / the object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	public void notSee(VisibleObject object, boolean isOutOfRange) {
	}

	/**
	 * 从世界中删除所有者对象。
	 * Deletes the owner object from the world.
	 */
	public void delete() {
		if (getOwner().isSpawned()) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(getOwner());
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().removeObject(getOwner());
	}

	/**
	 * 生成前钩子，处理可放置静态物件的地理服务注册。
	 * Pre-spawn hook that registers placeable static objects with the geo service.
	 */
	public void onBeforeSpawn() {
		if (getOwner().getSpawn() != null && getOwner().getSpawn().getStaticId() > 0) {
			GameWorldServices.geoService().spawnPlaceableObject(getOwner().getWorldId(), getOwner().getInstanceId(),
					getOwner().getSpawn().getStaticId());
		}
	}

	/**
	 * 生成后钩子。
	 * Post-spawn hook.
	 */
	public void onAfterSpawn() {

	}

	/**
	 * 消失时钩子，取消地理服务中的可放置静态物件。
	 * Despawn hook that unregisters placeable static objects from the geo service.
	 */
	public void onDespawn() {
		if (getOwner().getSpawn() != null && getOwner().getSpawn().getStaticId() > 0) {
			GameWorldServices.geoService().despawnPlaceableObject(getOwner().getWorldId(), getOwner().getInstanceId(),
					getOwner().getSpawn().getStaticId());
		}
	}

	/**
	 * 删除时钩子：若对象仍在世界中则先消失再删除。
	 * Delete hook: despawns then deletes the object if it is still in the world.
	 */
	public void onDelete() {
		if (getOwner().isInWorld()) {
			this.onDespawn();
			this.delete();
		}
	}
}
