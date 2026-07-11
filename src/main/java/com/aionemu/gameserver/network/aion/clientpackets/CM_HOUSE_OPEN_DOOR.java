package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HousePermissions;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 打开/进入房屋门（或离开房屋）的客户端包。
 * Client packet for opening/entering a house door (or leaving a house).
 *
 * @author Wartraxx
 */
public class CM_HOUSE_OPEN_DOOR extends AionClientPacket {
	int address;
	boolean leave = false;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOUSE_OPEN_DOOR(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 读取房屋地址与离开标志。
	 * Reads the house address and leave flag.
	 */
	@Override
	protected void readImpl() {
		address = readD();
		if (readC() != 0) {
			leave = true;
		}
	}
	/**
	 * 校验权限后进入房屋或离开房屋。
	 * Enters or leaves the house after permission checks.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		if (player.getAccessLevel() >= 3 && HousingConfig.ENABLE_SHOW_HOUSE_DOORID) {
			PacketSendUtility.sendMessage(player, "House Door Id: " + address);
		}
		House house = GameHousingServices.housingService().getHouseByAddress(address);
		if (house == null) {
			return;
		}
		if (leave) {
			if (house.getAddress().getExitMapId() != null) {
				TeleportService2.teleportTo(player, house.getAddress().getExitMapId(), house.getAddress().getExitX(),
						house.getAddress().getExitY(), house.getAddress().getExitZ(), (byte) 0,
						TeleportAnimation.BEAM_ANIMATION);
			} else {
				if (GeoDataConfig.GEO_ENABLE) {
					Npc sign = house.getCurrentSign();
					byte flags = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
					Vector3f colSign = GameWorldServices.geoService().getClosestCollision(sign, player.getX(), player.getY(),
							player.getZ() + 2, false, flags);
					Vector3f colWall = GameWorldServices.geoService().getClosestCollision(player, colSign.getX(),
							colSign.getY(), colSign.getZ(), true, flags);
					double radian = Math
							.toRadians(MathUtil.calculateAngleFrom(player.getX(), player.getY(), colWall.x, colWall.y));
					float x = (float) (Math.cos(radian) * 3.0D);
					float y = (float) (Math.sin(radian) * 3.0D);
					TeleportService2.teleportTo(player, house.getWorldId(), colWall.getX() + x, colWall.getY() + y,
							player.getZ(), (byte) 0, TeleportAnimation.BEAM_ANIMATION);
				} else {
					double radian = Math.toRadians(MathUtil.convertHeadingToDegree(player.getHeading()));
					float x = (float) (Math.cos(radian) * 6);
					float y = (float) (Math.sin(radian) * 6);
					TeleportService2.teleportTo(player, house.getWorldId(), player.getX() + x, player.getY() + y,
							player.getZ(), (byte) 0, TeleportAnimation.BEAM_ANIMATION);
				}
			}
		} else {
			if (house.getOwnerId() != player.getObjectId()) {
				boolean allowed = false;
				if (house.getDoorState() == HousePermissions.DOOR_OPENED_FRIENDS) {
					allowed = player.getFriendList().getFriend(house.getOwnerId()) != null
							|| (player.getLegion() != null && player.getLegion().isMember(house.getOwnerId()));
				}
				if (!allowed) {
					if (player.getAccessLevel() < HousingConfig.ENTER_HOUSE_ACCESSLEVEL) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_ENTER_NO_RIGHT2);
						return;
					}
				}
			}
			double radian = Math.toRadians(MathUtil.convertHeadingToDegree(player.getHeading()));
			float x = (float) (Math.cos(radian) * 6);
			float y = (float) (Math.sin(radian) * 6);
			TeleportService2.teleportTo(player, house.getWorldId(), player.getX() + x, player.getY() + y,
					house.getAddress().getZ(), (byte) 0, TeleportAnimation.BEAM_ANIMATION);
		}
	}
}
