package com.aionemu.gameserver.quest.handlers.archdaeva;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SanctuaryDungeonReentryTest {

	private static final List<Path> QUEST_SOURCES = List.of(
			Path.of("src/main/java/com/aionemu/gameserver/quest/handlers/archdaeva/_10520Covert_Communiques.java"),
			Path.of("src/main/java/com/aionemu/gameserver/quest/handlers/archdaeva/_20520Lost_Destiny.java"));

	@Test
	void completedMovieStepCanReenterFromTeleporterWorldOrTriggerZone() throws IOException {
		for (Path sourcePath : QUEST_SOURCES) {
			String source = Files.readString(sourcePath);
			String dialog = methodBody(source, "public boolean onDialogEvent(QuestEnv env)");
			String enterWorld = methodBody(source, "public boolean onEnterWorldEvent(QuestEnv env)");
			String enterZone = methodBody(source, "public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName)");

			assertTrue(dialog.contains("env.getDialog() == QuestDialog.START_DIALOG && isSanctuaryDungeonReentryPending(qs)"),
					() -> sourcePath + " must allow reentry from the capital teleporter");
			assertTrue(enterWorld.contains("isSanctuaryDungeonReentryPending(qs)"),
					() -> sourcePath + " must reenter after traveling back to the quest world");
			assertTrue(enterZone.contains("else if (isSanctuaryDungeonReentryPending(qs))"),
					() -> sourcePath + " must allow reentry from the original trigger zone");
			assertTrue(source.contains("questState.getStatus() == QuestStatus.REWARD"),
					() -> sourcePath + " must recover players who left after setting the reward state");
		}
	}

	@Test
	void reentryReusesTheRegisteredInstanceBeforeCreatingOne() throws IOException {
		for (Path sourcePath : QUEST_SOURCES) {
			String source = Files.readString(sourcePath);
			String enterDungeon = methodBody(source, "private void enterSanctuaryDungeon(Player player)");
			int registeredLookup = enterDungeon.indexOf("InstanceService.getRegisteredInstance(");
			int missingGuard = enterDungeon.indexOf("if (sanctuaryDungeon == null)");
			int createInstance = enterDungeon.indexOf("InstanceService.getNextAvailableInstance(");
			int teleport = enterDungeon.indexOf("TeleportService2.teleportTo(");

			assertTrue(registeredLookup >= 0, () -> sourcePath + " must look up the previous instance");
			assertTrue(missingGuard > registeredLookup, () -> sourcePath + " must create only when lookup fails");
			assertTrue(createInstance > missingGuard, () -> sourcePath + " must create a replacement instance");
			assertTrue(teleport > createInstance, () -> sourcePath + " must teleport after resolving the instance");
		}
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
