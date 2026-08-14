package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 单个动态限制区域的 NoRecall 状态更新。
 * Updates the NoRecall state of a single dynamic limit area.
 */
public class SM_DYNAMIC_LIMIT_AREA_INFO extends AionServerPacket {

	private final String areaName;
	private final boolean enabled;

	/**
	 * 构造指定区域的 NoRecall 状态更新包。
	 * Creates a NoRecall state update packet for the given area.
	 *
	 * @param areaName 区域名称 / area name
	 * @param enabled 是否启用 NoRecall 限制 / whether the NoRecall restriction is enabled
	 */
	public SM_DYNAMIC_LIMIT_AREA_INFO(String areaName, boolean enabled) {
		this.areaName = areaName;
		this.enabled = enabled;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(areaName);
		writeD(enabled ? 1 : 0);
	}
}
