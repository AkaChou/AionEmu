package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.controllers.observer.FlyRingObserver;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞行环控制器，管理玩家穿越飞行环时的观察者。
 * Fly ring controller that manages observers when players pass through a fly ring.
 *
 * @author xavier
 */
public class FlyRingController extends VisibleObjectController<FlyRing> {

	/** 当前观察该飞行环的玩家观察者映射。 / Map of observers for players currently observing this fly ring. */
	Map<Integer, FlyRingObserver> observed = new ConcurrentHashMap<Integer, FlyRingObserver>();

	/**
	 * 玩家进入飞行环可视范围时注册观察者。
	 * Registers an observer when a player enters the fly ring's visibility range.
	 *
	 * @param object 进入视野的可见对象 / the visible object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		Player p = (Player) object;
		FlyRingObserver observer = new FlyRingObserver(getOwner(), p);
		p.getObserveController().addObserver(observer);
		observed.put(p.getObjectId(), observer);
	}

	/**
	 * 玩家离开飞行环可视范围时移除观察者。
	 * Removes the observer when a player leaves the fly ring's visibility range.
	 *
	 * @param object 离开视野的可见对象 / the visible object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		Player p = (Player) object;
		FlyRingObserver observer = observed.remove(p.getObjectId());
		if (isOutOfRange) {
			observer.moved();
		}
		p.getObserveController().removeObserver(observer);
	}
}
