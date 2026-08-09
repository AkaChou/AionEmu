package com.aionemu.gameserver.commands.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class QuestReloadAtomicityTest {

	private static final Path SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/commands/admin/Reload.java");

	@Test
	void questCatalogIsPreparedBeforeTheLiveEngineIsCleared() throws Exception {
		String source = Files.readString(SOURCE);
		String execute = methodBody(source, "public void execute(Player admin, String... params)");
		String reload = methodBody(source, "private void reloadQuests(List<QuestTemplate> quests, List<XMLQuest> scripts,");

		int prepare = execute.indexOf("questEngine.prepareProductionDefinitions()");
		int conflictValidation = execute.indexOf("questEngine.validateLegacyOwnerConflicts", prepare);
		int invokeReload = execute.indexOf("reloadQuests(", conflictValidation);
		assertTrue(prepare >= 0);
		assertTrue(conflictValidation > prepare);
		assertTrue(invokeReload > conflictValidation);

		int previous = reload.indexOf("questEngine.currentProductionDefinitions()");
		int shutdown = reload.indexOf("questEngine.shutdown()", previous);
		int installPrepared = reload.indexOf("questEngine.load(null, prepared)", shutdown);
		int restorePrevious = reload.indexOf("questEngine.load(null, previous)", installPrepared);
		assertTrue(previous >= 0);
		assertTrue(shutdown > previous);
		assertTrue(installPrepared > shutdown);
		assertTrue(restorePrevious > installPrepared);
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, signature + " must have a method body");
		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}
}
