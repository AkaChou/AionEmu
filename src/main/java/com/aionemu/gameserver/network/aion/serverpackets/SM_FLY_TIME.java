package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 飞行时间（FP）同步包：当前与最大飞行点数。
 * Fly time (FP) sync packet: current and max fly points.
 *
 * @author Nemiroff
 */
@AllArgsConstructor
public class SM_FLY_TIME extends AionServerPacket {

	private final int currentFp;
	private final int maxFp;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(currentFp); // current fly time
		writeD(maxFp); // max flytime
	}
}
