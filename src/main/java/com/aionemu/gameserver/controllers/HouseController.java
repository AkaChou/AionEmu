package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.List;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_HOUSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_RENDER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房屋控制器，管理房屋渲染、外观更新与访客踢出。
 * House controller managing house rendering, appearance updates and visitor kicks.
 */
public class HouseController extends VisibleObjectController<House> {

	/** 正在观察该房屋的玩家映射。 / Map of players currently observing this house. */
	Map<Integer, ActionObserver> observed = new ConcurrentHashMap<Integer, ActionObserver>();

	/**
	 * 玩家进入房屋范围时发送渲染包并生成室内物件。
	 * Sends render packets and spawns house objects when a player enters range.
	 *
	 * @param object 进入视野的可见对象 / the visible object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		Player p = (Player) object;
		ActionObserver observer = new ActionObserver(ObserverType.MOVE);
		p.getObserveController().addObserver(observer);
		observed.put(p.getObjectId(), observer);
		AionServerPacket packet;
		if (getOwner().isInInstance()) {
			packet = new SM_HOUSE_UPDATE(getOwner());
		} else {
			packet = new SM_HOUSE_RENDER(getOwner());
		}
		PacketSendUtility.sendPacket(p, packet);
		spawnObjects();
	}

	/**
	 * 玩家离开房屋范围时移除观察者并可发送删除包。
	 * Removes the observer and may send a delete packet when a player leaves range.
	 *
	 * @param object 离开视野的可见对象 / the visible object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		Player p = (Player) object;
		ActionObserver observer = observed.remove(p.getObjectId());
		if (isOutOfRange) {
			observer.moved();
			if (!getOwner().isInInstance()) {
				PacketSendUtility.sendPacket(p, new SM_DELETE_HOUSE(getOwner().getAddress().getId()));
			}
		}
		p.getObserveController().removeObserver(observer);
	}

	/**
	 * 生成房屋登记处中所有已放置物件。
	 * Spawns all placeable objects registered on this house.
	 */
	public void spawnObjects() {
		if (getOwner().getRegistry() != null) {
			for (HouseObject<?> obj : getOwner().getRegistry().getSpawnedObjects()) {
				obj.spawn();
			}
		}
	}

	/**
	 * 生成后同步房屋门状态到地理服务。
	 * After spawn, syncs the house door state to the geo service.
	 */
	@Override
	public void onAfterSpawn() {
		super.onAfterSpawn();
		GameWorldServices.geoService().setHouseDoorState(getOwner().getWorldId(), getOwner().getInstanceId(),
				getOwner().getAddress().getId(), getOwner().getDoorState().isDoorOpen());
	}

	/**
	 * 向所有观察者异步推送房屋外观更新。
	 * Asynchronously pushes a house appearance update to all observers.
	 */
	public void updateAppearance() {
		GameThreadPoolServices.threadPoolManager().execute(new Runnable() {
			@Override
			public void run() {
				for (int playerId : observed.keySet()) {
					Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
					if (player == null) {
						continue;
					}
					PacketSendUtility.sendPacket(player, new SM_HOUSE_UPDATE(getOwner()));
				}
			}
		});
	}

	/**
	 * 向所有观察者异步广播完整房屋渲染包。
	 * Asynchronously broadcasts the full house render packet to all observers.
	 */
	public void broadcastAppearance() {
		GameThreadPoolServices.threadPoolManager().execute(new Runnable() {
			@Override
			public void run() {
				for (int playerId : observed.keySet()) {
					Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
					if (player == null) {
						continue;
					}
					PacketSendUtility.sendPacket(player, new SM_HOUSE_RENDER(getOwner()));
				}
			}
		});
	}

	/**
	 * 将访客踢出房屋区域。
	 * Kicks visitors out of the house zone.
	 *
	 * @param kicker 发起踢出的玩家，可为 null / player who initiated the kick, may be null
	 * @param kickFriends 是否连同好友一并踢出 / whether friends are also kicked
	 * @param onSettingsChange 是否因权限设置变更触发 / whether triggered by a settings change
	 */
	public void kickVisitors(Player kicker, boolean kickFriends, boolean onSettingsChange) {
		List<ZoneInfo> zoneInfo = DataManager.ZONE_DATA.getZones().get(getOwner().getWorldId());
		for (ZoneInfo info : zoneInfo) {
			if (info.getZoneTemplate().getName().name().equals(getOwner().getName())) {
				for (Integer objId : this.observed.keySet()) {
					if (objId == getOwner().getOwnerId()) {
						continue;
					}
					if (!kickFriends && kicker != null && kicker.getFriendList().getFriend(objId) != null) {
						continue;
					}
					Player visitor = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
					if (visitor != null) {
						if (visitor.isInsideZone(info.getZoneTemplate().getName())) {
							moveOutside(visitor, onSettingsChange);
						}
					}
				}
				break;
			}
		}
		if (kicker != null) {
			if (!kickFriends) {
				PacketSendUtility.sendPacket(kicker, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_ORDER_OUT_WITHOUT_FRIENDS);
			} else {
				PacketSendUtility.sendPacket(kicker, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_ORDER_OUT_ALL);
			}
		}
	}

	/**
	 * 将玩家传送到房屋外部并发送系统消息。
	 * Teleports a player outside the house and sends a system message.
	 *
	 * target player
	 * @param onSettingsChange 是否因设置变更 / whether due to a settings change
	 */
	private void moveOutside(Player player, boolean onSettingsChange) {
		if (getOwner().getHouseType() == HouseType.STUDIO) {
			float x = getOwner().getAddress().getExitX();
			float y = getOwner().getAddress().getExitY();
			float z = getOwner().getAddress().getExitZ();
			TeleportService2.teleportTo(player, getOwner().getAddress().getExitMapId(), 1, x, y, z, player.getHeading(),
					TeleportAnimation.BEAM_ANIMATION);
		} else {
			Npc sign = getOwner().getCurrentSign();
			double radian = Math.toRadians(MathUtil.convertHeadingToDegree(sign.getHeading()));
			float x = (float) (sign.getX() + (8 * Math.cos(radian)));
			float y = (float) (sign.getY() + (8 * Math.sin(radian)));
			TeleportService2.teleportTo(player, getOwner().getWorldId(), 1, x, y, player.getZ() + 1,
					player.getHeading(), TeleportAnimation.BEAM_ANIMATION);
		}
		if (onSettingsChange) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CHANGE_OWNER);
		} else {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_REQUEST_OUT);
		}
	}
}
