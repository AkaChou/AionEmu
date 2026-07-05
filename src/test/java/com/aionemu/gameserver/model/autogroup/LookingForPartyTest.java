package com.aionemu.gameserver.model.autogroup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LookingForPartyTest {

	@Test
	void searchInstancesUseLockProtectedIteration() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/model/autogroup/LookingForParty.java"));

		assertTrue(source.contains("Iterator<SearchInstance>"));
		assertFalse(source.contains("searchInstances.remove(si);"));
		assertTrue(source.contains("public List<SearchInstance> getSearchInstances() {\n\t\tsuper.readLock();"));
		assertTrue(source.contains("public boolean isRegistredInstance(int instanceMaskId) {\n\t\tsuper.readLock();"));
	}
}
