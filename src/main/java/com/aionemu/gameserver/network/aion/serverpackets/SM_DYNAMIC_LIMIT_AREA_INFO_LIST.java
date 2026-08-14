package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 当前实例全部动态限制区域的 NoRecall 状态快照。
 * Snapshot of the NoRecall state of all dynamic limit areas in the current instance.
 */
public class SM_DYNAMIC_LIMIT_AREA_INFO_LIST extends AionServerPacket {

	private final Map<String, Boolean> areas;

	/**
	 * 按给定区域状态表构造快照包（保持传入顺序）。
	 * Creates a snapshot packet from the given area state map (preserving insertion order).
	 *
	 * @param areas 区域名称到 NoRecall 启用状态的映射 / map of area names to NoRecall enabled state
	 */
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
