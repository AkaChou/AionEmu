package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.NpcObject;
import com.aionemu.gameserver.model.gameobjects.UseableItemObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端生成/同步单个房屋摆放物的服务端包。
 * Server packet that spawns or synchronizes a single house object to the client.
 */
public class SM_HOUSE_OBJECT extends AionServerPacket {
	HouseObject<?> houseObject;

	/**
	 * 构造单个房屋摆放物同步包。
	 * Creates a single house object sync packet.
	 *
	 * @param owner 房屋摆放物 / house object
	 */
	public SM_HOUSE_OBJECT(HouseObject<?> owner) {
		this.houseObject = owner;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		if (player == null) {
			return;
		}
		House house = houseObject.getOwnerHouse();
		int templateId = houseObject.getObjectTemplate().getTemplateId();
		writeD(house.getAddress().getId());
		writeD(house.getOwnerId());
		writeD(houseObject.getObjectId());
		writeD(houseObject.getObjectId());
		writeD(templateId);
		writeF(houseObject.getX());
		writeF(houseObject.getY());
		writeF(houseObject.getZ());
		writeH(houseObject.getRotation());
		writeD(player.getHouseObjectCooldownList().getReuseDelay(houseObject.getObjectId()));
		if (houseObject.getUseSecondsLeft() > 0) {
			writeD(houseObject.getUseSecondsLeft());
		} else {
			writeD(0);
		}
		Integer color = null;
		if (houseObject != null) {
			color = houseObject.getColor();
		}
		if (color != null && color > 0) {
			writeC(1);
			writeC((color & 0xFF0000) >> 16);
			writeC((color & 0xFF00) >> 8);
			writeC((color & 0xFF));
		} else {
			writeC(0);
			writeC(0);
			writeC(0);
			writeC(0);
		}
		writeD(0);
		byte typeId = houseObject.getObjectTemplate().getTypeId();
		writeC(typeId);
		switch (typeId) {
		case 1:
			((UseableItemObject) houseObject).writeUsageData(getBuf());
			break;
		case 7:
			NpcObject npcObj = (NpcObject) houseObject;
			writeD(npcObj.getNpcObjectId());
		default:
			break;
		}
	}
}
