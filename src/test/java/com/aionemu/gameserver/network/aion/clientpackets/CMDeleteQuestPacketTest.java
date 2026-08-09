package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

class CMDeleteQuestPacketTest {

	@Test
	void readsGrowthQuestIdAsFullThirtyTwoBitValue() {
		CM_DELETE_QUEST packet = new CM_DELETE_QUEST(0, State.IN_GAME);
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(80215).flip();
		packet.setBuffer(buffer);

		assertTrue(packet.read());
		assertEquals(80215, packet.questId);
	}
}
