package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;
import java.util.Map.Entry;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 附近任务列表服务端包。
 * Server packet that delivers the list of nearby quests to the client.
 * <p>
 * 条目格式与原版一致（原版 {@code User::_UpdateQuestAcquireCondition}，opcode 127）：每条 4 字节，
 * 平条目写 {@code questId}，软标记条目（原版 {@code CanAcquireQuest == 1}：只差 1 级）写
 * {@code questId | 0x20000}；映射 value &gt; 0 表示软标记。
 * <p>
 * Entries match the retail wire format (opcode 127): 4 bytes each, {@code questId} for a plain entry
 * and {@code questId | 0x20000} for a soft-marked one-level-short entry. A map value above zero
 * carries the soft marker.
 */
@AllArgsConstructor
public class SM_NEARBY_QUESTS extends AionServerPacket {
	/** 原版软标记位（{@code CanAcquireQuest == 1}）。 / The retail soft marker bit. */
	private static final int LEVEL_SOON_MARKER = 0x20000;

	private final Map<Integer, Integer> nearbyQuestList;

	@Override
	protected void writeImpl(AionConnection con) {
		if (nearbyQuestList == null || con.getActivePlayer() == null) {
			return;
		}
		writeC(0);
		writeH(-nearbyQuestList.size() & 0xFFFF);
		for (Entry<Integer, Integer> nearbyQuest : nearbyQuestList.entrySet()) {
			int questId = nearbyQuest.getKey();
			writeD(nearbyQuest.getValue() > 0 ? (questId | LEVEL_SOON_MARKER) : questId);
		}
	}
}
