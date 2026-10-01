package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;

/**
 * {@code SM_NEARBY_QUESTS}（真端 opcode 127 = {@code S_UPDATE_ZONE_QUEST}）条目字节面门禁。
 * <p>
 * 真端 {@code MainServer/User::_UpdateQuestAcquireCondition} 每条写 4 字节：{@code CanAcquireQuest == 2}
 * 写 {@code questId}，{@code == 1}（只差 1 级）写 {@code questId | 0x20000}；包首两字段 = 段计数与
 * 「负号收尾」的条目数。真端网关解出的 opcode {@code 0x0181} 按 {@code (op + 0xD5) ^ 0xD5} 反解即 127。
 * <p>
 * Wire-format gate for the nearby-quest packet: the entries must keep the retail four-byte shape for
 * both plain and soft-marked rows, including quest ids above {@code 0xFFFF} (703 such retail table
 * rows exist, e.g. {@code 80010}).
 */
class SMNearbyQuestsPacketTest {

	/** 真端软标记位。 / The retail soft marker bit. */
	private static final int LEVEL_SOON_MARKER = 0x20000;

	@Test
	void writesPlainRetailEntries() {
		assertArrayEquals(new byte[] { 0, (byte) 0xFF, (byte) 0xFF, (byte) 0x88, 0x13, 0, 0 },
			payload(entry(5000, 0)), "平条目 = questId 四字节（真端 2）");
	}

	@Test
	void writesSoftMarkedRetailEntries() {
		byte[] actual = payload(entry(5000, 1));
		assertArrayEquals(new byte[] { 0, (byte) 0xFF, (byte) 0xFF, (byte) 0x88, 0x13, 2, 0 }, actual,
			"软标记条目 = questId | 0x20000（真端 1）");
	}

	@Test
	void writesQuestIdsAboveSixteenBitsWithTheSameMarkerBit() {
		// 真端表 703 行 id > 0xFFFF（SimpleTalk 526 / SimpleHunt 142 / SimpleUseItem 33 / SimpleItemPlay 2）；
		// 真端按四字节写 id | 0x20000。
		// 703 retail table rows carry ids above 0xFFFF; retail writes id | 0x20000 as a four-byte entry.
		assertArrayEquals(new byte[] { 0, (byte) 0xFF, (byte) 0xFF, (byte) 0x8A, 0x38, 3, 0 },
			payload(entry(80010, 1)), "80010 | 0x20000 = 0x3388A");
		assertArrayEquals(new byte[] { 0, (byte) 0xFF, (byte) 0xFF, (byte) 0x8A, 0x38, 1, 0 },
			payload(entry(80010, 0)), "平条目 80010 = 0x1388A");
	}

	private static Map<Integer, Integer> entry(int questId, int flag) {
		Map<Integer, Integer> entries = new LinkedHashMap<>();
		entries.put(questId, flag);
		return entries;
	}

	private static byte[] payload(Map<Integer, Integer> entries) {
		SM_NEARBY_QUESTS packet = new SM_NEARBY_QUESTS(entries);
		ObjenesisStd objenesis = new ObjenesisStd();
		AionConnection connection = objenesis.newInstance(AionConnection.class);
		setField(AionConnection.class, connection, "activePlayer",
			new AtomicReference<>(objenesis.newInstance(Player.class)));
		ByteBuffer buffer = ByteBuffer.allocate(16);
		packet.setBuf(buffer);
		packet.writeImpl(connection);
		buffer.flip();
		byte[] payload = new byte[buffer.remaining()];
		buffer.get(payload);
		return payload;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
