package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class InstanceHandlerRecoveryMigrationTest {

	@Test
	void migratedHandlersUsePersistentDeadlinesAndState() throws Exception {
		assertMigrated("AdmaStrongholdInstance", "scheduleDeadline(\"pot\"", "adma.complete");
		assertMigrated("PadmarashkaCaveInstance", "scheduleDeadline(\"expire\"", "padma.protectors");
		assertMigrated("CradleOfEternityInstance", "scheduleDeadline(\"start\"", "cradle.covetous_complete");
		assertMigrated("TransidiumAnnexInstance", "scheduleDeadline(\"start\"", "transidium.hangar_barricade");
		assertMigrated("TheobomosLabInstance", "scheduleDeadline(\"stone\"", "theobomos.ifrit_deadline");
	}

	private static void assertMigrated(String className, String deadline, String state) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + className + ".java"));
		assertTrue(source.contains(deadline));
		assertTrue(source.contains(state));
		assertFalse(source.contains("Future<?>"));
	}
}
