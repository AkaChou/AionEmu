package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 争议之地状态包：同步相关世界是否处于活跃争议状态。
 * Dispute-land status packet: whether listed worlds are in active dispute.
 *
 * @author Rinzler
 */
public class SM_DISPUTE_LAND extends AionServerPacket {
	boolean active;
	List<Integer> worlds;

	public SM_DISPUTE_LAND(List<Integer> worlds, boolean active) {
		this.worlds = worlds;
		this.active = active;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(worlds.size());
		for (int world : worlds) {
			writeD(active ? 0x02 : 0x01);
			writeD(world);
			writeQ(0x00);
			writeQ(0x00);
			writeQ(0x00);
			writeQ(0x00);
		}
	}
}
