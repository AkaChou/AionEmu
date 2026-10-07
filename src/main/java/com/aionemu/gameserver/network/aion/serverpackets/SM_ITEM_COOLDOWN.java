package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步物品冷却时间的服务端包。
 * Server packet that synchronizes item cooldown timers to the client.
 * @author ATracer
 */
@AllArgsConstructor
public class SM_ITEM_COOLDOWN extends AionServerPacket {

	private final Map<Integer, ItemCooldown> cooldowns;

	/**
	 * 构造物品冷却「整表载入」包：{@code null} 冷却表按空表处理，等价于让客户端不保留任何物品冷却。
	 * Creates a whole-table item-cooldown "load" packet: a {@code null} table is treated as empty,
	 * which tells the client to keep no item cooldown at all.
	 * <p>物品冷却的计时器由客户端持有，本包是服务端唯一能改写它的手段；因此清空冷却时，即使服务端
	 * 侧的表为空也必须下发，否则客户端本地预测的冷却扫描不会被清掉。
	 * The client owns the item-cooldown timers and this packet is the only way the server can rewrite
	 * them; a clear must therefore be sent even when the server-side table ends up empty, otherwise the
	 * client's locally predicted sweep is never cleared.</p>
	 * @param cooldowns 玩家物品冷却表（可为 null） / the player's item-cooldown table (may be null)
	 * @return 整表载入包 / whole-table load packet
	 */
	public static SM_ITEM_COOLDOWN load(Map<Integer, ItemCooldown> cooldowns) {
		return new SM_ITEM_COOLDOWN(cooldowns == null ? Map.of() : cooldowns);
	}

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
