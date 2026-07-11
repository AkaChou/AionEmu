package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.housing.PlaceableHouseObject;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_HOUSE_OBJECT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OBJECT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可放置房屋物件控制器，处理可见性同步与对话请求。
 * Placeable house-object controller handling visibility sync and dialog requests.
 *
 * @param <T> 可放置房屋物件模板类型 / placeable house object template type
 */
public class PlaceableObjectController<T extends PlaceableHouseObject> extends VisibleObjectController<HouseObject<T>> {

	/** 观察该物件的玩家映射。 / Map of players observing this object. */
	Map<Integer, ActionObserver> observed = new ConcurrentHashMap<Integer, ActionObserver>();

	/**
	 * 玩家进入范围时发送房屋物件包并注册移动观察者。
	 * Sends the house-object packet and registers a move observer when a player enters range.
	 *
	 * @param object 进入视野的可见对象 / the visible object entering sight
	 */
	public void see(VisibleObject object) {
		Player p = (Player) object;
		ActionObserver observer = new ActionObserver(ObserverType.MOVE);
		p.getObserveController().addObserver(observer);
		observed.put(p.getObjectId(), observer);
		PacketSendUtility.sendPacket(p, new SM_HOUSE_OBJECT(getOwner()));
	}

	/**
	 * 玩家离开范围时移除观察者并发送删除包。
	 * Removes the observer and sends a delete packet when a player leaves range.
	 *
	 * @param object 离开视野的可见对象 / the visible object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		Player p = (Player) object;
		ActionObserver observer = (ActionObserver) observed.remove(p.getObjectId());
		if (isOutOfRange) {
			observer.moved();
			PacketSendUtility.sendPacket(p, new SM_DELETE_HOUSE_OBJECT((getOwner()).getObjectId()));
		}
		p.getObserveController().removeObserver(observer);
	}

	/**
	 * 物件消失时的回调。
	 * Callback when the object despawns.
	 */
	public void onDespawn() {
		getOwner().onDespawn();
	}

	/**
	 * 从世界中删除该房屋物件。
	 * Deletes this house object from the world.
	 */
	public void delete() {
		if (getOwner().isSpawned()) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(getOwner(), false);
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().removeObject(getOwner());
	}

	/**
	 * 处理玩家对房屋物件的对话请求。
	 * Handles a player's dialog request against the house object.
	 *
	 * @param player 发起请求的玩家 / the requesting player
	 */
	public void onDialogRequest(Player player) {
		if (!MathUtil.isInRange(getOwner(), player, getOwner().getObjectTemplate().getTalkingDistance() + 2)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_TOO_FAR_TO_USE);
			return;
		}
		getOwner().onDialogRequest(player);
	}
}
