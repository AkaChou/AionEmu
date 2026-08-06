package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SM_QUEST_ACTIONTest {

	@Test
	void addQuestUsesTheClientListInsertionAction() {
		SM_QUEST_ACTION packet = SM_QUEST_ACTION.addQuest(0x12345678, QuestStatus.START, 0x23456789);
		ByteBuffer buffer = ByteBuffer.allocate(14);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		assertArrayEquals(new byte[] { 1, 0x78, 0x56, 0x34, 0x12, 3, 0,
			(byte) 0x89, 0x67, 0x45, 0x23, 0, 0, 0 }, buffer.array());
	}

	@Test
	void updateQuestUsesTheExistingQuestUpdateAction() {
		SM_QUEST_ACTION packet = SM_QUEST_ACTION.updateQuest(0x12345678, QuestStatus.REWARD, 0x23456789);
		ByteBuffer buffer = ByteBuffer.allocate(13);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		assertArrayEquals(new byte[] { 2, 0x78, 0x56, 0x34, 0x12, 4, 0,
			(byte) 0x89, 0x67, 0x45, 0x23, 0, 0 }, buffer.array());
	}
}
