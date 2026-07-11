package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;

class SM_LEGION_TABSTest {

	@Test
	void writesPageEntryCount() {
		List<LegionHistory> history = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			history.add(new LegionHistory(LegionHistoryType.CREATE, "", new Timestamp(0), 0, ""));
		}

		assertHeader(write(new SM_LEGION_TABS(history, 0, 0)), 9, 0, 8);
		assertHeader(write(new SM_LEGION_TABS(history, 1, 0)), 9, 1, 1);
	}

	private static void assertHeader(ByteBuffer buffer, int size, int page, int pageSize) {
		assertEquals(size, buffer.getInt());
		assertEquals(page, buffer.getInt());
		assertEquals(pageSize, buffer.getInt());
	}

	private static ByteBuffer write(SM_LEGION_TABS packet) {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();
		return buffer;
	}
}
