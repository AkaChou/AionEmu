package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步城镇等级列表的服务端包。
 * Server packet that syncs the town level list.
 */
@AllArgsConstructor
public class SM_TOWNS_LIST extends AionServerPacket {
	private final Map<Integer, Town> towns;

	protected void writeImpl(AionConnection con) {
		writeH(towns.size());
		for (Town town : towns.values()) {
			writeD(town.getId());
			writeD(town.getLevel());
			writeD((int) (town.getLevelUpDate().getTime() / 1000));
		}
	}
}
