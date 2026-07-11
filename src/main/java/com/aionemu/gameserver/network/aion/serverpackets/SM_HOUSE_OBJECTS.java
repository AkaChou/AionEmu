package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步玩家房屋中已生成摆放物列表的服务端包。
 * Server packet that synchronizes the player's spawned house object list to the client.
 */
public class SM_HOUSE_OBJECTS extends AionServerPacket {
	Player player;

	/**
	 * 构造房屋已生成摆放物列表包。
	 * Creates a spawned house objects list packet.
	 *
	 * 玩家 / player
	 */
	public SM_HOUSE_OBJECTS(Player player) {
		this.player = player;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		List<HouseObject<?>> objects = player.getHouseRegistry().getSpawnedObjects();
		writeH(objects.size());
		for (HouseObject<?> obj : objects) {
			writeD(obj.getObjectTemplate().getTemplateId());
			writeF(obj.getX());
			writeF(obj.getY());
			writeF(obj.getZ());
		}
	}
}
