package com.aionemu.gameserver.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class QuestStatePersistenceSafetyTest {

	@Test
	void adminQuestInitializationLeavesOptionalTimestampsUnset() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/commands/admin/Quest.java"));

		assertFalse(source.contains("new Timestamp(0)"),
			"admin quest initialization must not use the Unix epoch as a timestamp sentinel");
		assertTrue(source.contains("new QuestState(questId, questStatus, 0, 0, null, 0, null)"),
			"unset repeat and completion times must be persisted as SQL NULL");
	}
}
