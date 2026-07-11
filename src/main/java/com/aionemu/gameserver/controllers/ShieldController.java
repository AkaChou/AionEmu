package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.shield.Shield;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 要塞护盾控制器，为敌对阵营玩家注册护盾伤害观察者。
 * Fortress shield controller that registers shield-damage observers for enemy-race players.
 */
public class ShieldController extends VisibleObjectController<Shield> {

	/** 当前受护盾观察的玩家映射。 / Map of players currently observed by the shield. */
	Map<Integer, ActionObserver> observed = new ConcurrentHashMap<Integer, ActionObserver>();

	/**
	 * 敌对玩家进入护盾范围时注册护盾观察者。
	 * Registers a shield observer when an enemy player enters the shield range.
	 *
	 * @param object 进入视野的可见对象 / the visible object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		FortressLocation loc = GameFeatureServices.siegeService().getFortress(getOwner().getId());
		Player player = (Player) object;
		if (loc.isUnderShield()) {
			if (loc.getRace() != SiegeRace.getByRace(player.getRace())) {
				ActionObserver observer = GameFeatureServices.shieldService().createShieldObserver(loc.getLocationId(), player);
				if (observer != null) {
					player.getObserveController().addObserver(observer);
					observed.put(player.getObjectId(), observer);
				}
			}
		}
	}

	/**
	 * 敌对玩家离开护盾范围时移除护盾观察者。
	 * Removes the shield observer when an enemy player leaves the shield range.
	 *
	 * @param object 离开视野的可见对象 / the visible object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		FortressLocation loc = GameFeatureServices.siegeService().getFortress(getOwner().getId());
		Player player = (Player) object;
		if (loc.isUnderShield()) {
			if (loc.getRace() != SiegeRace.getByRace(player.getRace())) {
				ActionObserver observer = observed.remove(player.getObjectId());
				if (observer != null) {
					if (isOutOfRange)
						observer.moved();
					player.getObserveController().removeObserver(observer);
				}
			}
		}
	}

	/**
	 * 禁用护盾并清理所有已注册的玩家观察者。
	 * Disables the shield and clears all registered player observers.
	 */
	public void disable() {
		for (Integer playerId : observed.keySet().toArray(Integer[]::new)) {
			ActionObserver observer = observed.remove(playerId);
			Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
			if (player != null) {
				player.getObserveController().removeObserver(observer);
			}
		}
	}
}
