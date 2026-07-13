package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/** 当前实例全部动态限制区域的 NoRecall 状态快照。 */
public class SM_DYNAMIC_LIMIT_AREA_INFO_LIST extends AionServerPacket {

	private final Map<String, Boolean> areas;

	public SM_DYNAMIC_LIMIT_AREA_INFO_LIST(Map<String, Boolean> areas) {
		this.areas = new LinkedHashMap<>(areas);
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(areas.size());
		areas.forEach((name, enabled) -> {
			writeS(name);
			writeD(enabled ? 1 : 0);
		});
	}
}
