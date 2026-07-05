package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AutoGroupServiceTest {

	@Test
	void unregisterInstanceRemovesSearcherByEntryKey() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/AutoGroupService.java"));

		assertTrue(source.contains("for (Map.Entry<Integer, LookingForParty> entry : searchers.entrySet())"));
		assertFalse(source.contains("searchers.values().remove(lfp)"));
	}
}
