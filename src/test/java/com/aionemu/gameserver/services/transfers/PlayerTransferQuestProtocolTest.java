package com.aionemu.gameserver.services.transfers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PlayerTransferQuestProtocolTest {

	@Test
	void preservesNextRepeatTimeAndPacketAlignment() throws Exception {
		String writer = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/loginserver/serverpackets/SM_PTRANSFER_CONTROL.java"));
		assertOrdered(writer,
				"writeD(qs.getCompleteCount());",
				"writeS(qs.getNextRepeatTime() == null ? null : qs.getNextRepeatTime().toString());",
				"writeD(qs.getReward());");

		String reader = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/transfers/CMT_CHARACTER_INFORMATION.java"));
		assertOrdered(reader,
				"int completeCount = readD();",
				"String nextRepeatTime = readS();",
				"int reward = readD();",
				"nextRepeatTime.isEmpty() ? null : Timestamp.valueOf(nextRepeatTime)");
		assertTrue(reader.contains("readB(8);\n\t\t\t\treadS();\n\t\t\t\treadB(4);"));
	}

	private void assertOrdered(String source, String... statements) {
		int previous = -1;
		for (String statement : statements) {
			int current = source.indexOf(statement, previous + 1);
			assertTrue(current > previous, () -> "Missing or out-of-order statement: " + statement);
			previous = current;
		}
	}
}
