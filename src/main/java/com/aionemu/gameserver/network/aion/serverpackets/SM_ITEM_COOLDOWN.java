package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步物品冷却时间的服务端包。
 * Server packet that synchronizes item cooldown timers to the client.
 *
 * @author ATracer
 */
@AllArgsConstructor
public class SM_ITEM_COOLDOWN extends AionServerPacket {

	private final Map<Integer, ItemCooldown> cooldowns;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(cooldowns.size());
		long currentTime = System.currentTimeMillis();
		for (Map.Entry<Integer, ItemCooldown> entry : cooldowns.entrySet()) {
			writeH(entry.getKey());
			int left = (int) ((entry.getValue().getReuseTime() - currentTime) / 1000);
			writeD(left > 0 ? left : 0);
			writeD(entry.getValue().getUseDelay());
		}
	}
}
