package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.controllers.observer.RoadObserver;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.road.Road;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 道路控制器，管理玩家进入/离开道路区域时的观察者。
 * Road controller that manages observers when players enter or leave a road area.
 *
 * @author SheppeR
 */
public class RoadController extends VisibleObjectController<Road> {

	/** 当前观察该道路的玩家观察者映射。 / Map of observers for players currently observing this road. */
	Map<Integer, RoadObserver> observed = new ConcurrentHashMap<Integer, RoadObserver>();

	/**
	 * 玩家进入道路可视范围时注册观察者。
	 * Registers an observer when a player enters the road's visibility range.
	 *
	 * @param object 进入视野的可见对象 / the visible object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		Player p = (Player) object;
		RoadObserver observer = new RoadObserver(getOwner(), p);
		p.getObserveController().addObserver(observer);
		observed.put(p.getObjectId(), observer);
	}

	/**
	 * 玩家离开道路可视范围时移除观察者。
	 * Removes the observer when a player leaves the road's visibility range.
	 *
	 * @param object 离开视野的可见对象 / the visible object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		Player p = (Player) object;
		RoadObserver observer = observed.remove(p.getObjectId());
		if (isOutOfRange) {
			observer.moved();
		}
		p.getObserveController().removeObserver(observer);
	}
}
