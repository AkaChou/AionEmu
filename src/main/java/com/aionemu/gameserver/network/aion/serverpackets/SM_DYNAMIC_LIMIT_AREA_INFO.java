package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 单个动态限制区域的 NoRecall 状态更新。
 * Updates the NoRecall state of a single dynamic limit area.
 */
@AllArgsConstructor
public class SM_DYNAMIC_LIMIT_AREA_INFO extends AionServerPacket {

	private final String areaName;
	private final boolean enabled;

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(areaName);
		writeD(enabled ? 1 : 0);
	}
}
