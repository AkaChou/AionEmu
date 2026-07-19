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
		assertMigrated("DraupnirCaveInstance", "scheduleDeadline(\"gate_raid_2\"", "draupnir.adjutants");
		assertMigrated("crucible/CrucibleChallengeInstance", "scheduleDeadline(\"bonus_spawn\"",
				"crucible.bonus_spawning_done");
		assertMigrated("LinkgateFoundryInstance", "scheduleDeadline(\"expire\"",
				"linkgate.expire_deadline");
		assertMigrated("DrakenseerLairInstance", "scheduleDeadline(\"expire\"",
				"drakenseer.enhancers");
		assertMigrated("RightWingChamberInstance", "scheduleDeadline(\"chests\"",
				"rightwing.exit_deadline");
		assertMigrated("LeftWingChamberInstance", "scheduleDeadline(\"chest\"",
				"leftwing.next_deadline");
		assertMigrated("TheHexwayInstance", "scheduleDeadline(\"chest\"",
				"hexway.next_deadline");
		assertMigrated("LowerUdasTempleInstance", "scheduleDeadline(\"chest\"",
				"lower_udas.next_deadline");
		assertMigrated("AbyssStoreroomInstance", "scheduleDeadline(\"barrier_\"",
				"storeroom.next_deadline");
		assertMigrated("SealedArgentManorInstance", "scheduleDeadline(\"expire\"",
				"sealed.resistance_skill");
		assertSourceExcludes("SealedArgentManorInstance", "GameThreadPoolServices");
		assertMigrated("SmolderingFireTempleInstance", "scheduleDeadline(\"expire\"",
				"smolder.kill.");
		assertSourceExcludes("SmolderingFireTempleInstance", "GameThreadPoolServices");
		assertNoFuture("DivineTowerInstanceL");
		assertNoFuture("DivineTowerInstanceD");
		assertNoFuture("GraveOfSteelStoreroomInstance");
		assertNoFuture("IsleOfRootsStoreroomInstance");
		assertNoFuture("TwilightBattlefieldStoreroomInstance");
	}

	private static void assertMigrated(String className, String deadline, String state) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + className + ".java"));
		assertTrue(source.contains(deadline));
		assertTrue(source.contains(state));
		assertFalse(source.contains("Future<?>"));
	}

	private static void assertNoFuture(String className) throws Exception {
		assertSourceExcludes(className, "Future<?>");
	}

	private static void assertSourceExcludes(String className, String forbidden) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + className + ".java"));
		assertFalse(source.contains(forbidden));
	}
}
