package com.aionemu.gameserver.instance.handlers.scripts.luna;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ContaminatedUnderpathRetailMigrationTest {

	@Test
	void retailDataOwnsLunaActorsWhileHandlerOwnsPersistentLifecycle() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"301630000\"", "</world>");
		for (String variable : new String[] { "idluna_def_phase_1_1", "idluna_def_phase_2_1",
				"idluna_def_phase_3_1", "idluna_def_phase_4", "exp_spawn_07" }) {
			assertTrue(world.contains("name=\"" + variable + "\""), variable);
		}
		assertTrue(world.contains("npc id=\"245545\""));
		assertTrue(world.contains("npc id=\"245575\""));
		assertTrue(world.contains("IDLUNA_DEF_PHASE_4==1"));
		assertTrue(world.contains("retail:301630000:npcpath4_phase_01_wave1"));
		assertTrue(world.contains("retail:301630000:npcpath1_phase_01_wave2"));

		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idluna_def_ktw.xml"));
		assertTrue(patterns.contains("<name>IDLUNA_DEF_PHASE_1_1</name>"));
		assertTrue(patterns.contains("<string>IDLUNA_DEF_PHASE_1_1</string>"));
		assertTrue(patterns.contains("IDLuna_def_boss_rewardbox_03"));

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/luna/ContaminatedUnderpathInstance.java"));
		assertTrue(handler.contains("RetailConditionSpawnEngine.setVariable(instance, \"IDLUNA_DEF_PHASE_1_1\", 1, 0)"));
		assertTrue(handler.contains("scheduleDeadline(\"prepare\""));
		assertTrue(handler.contains("scheduleDeadline(\"expire\""));
		assertTrue(handler.contains("scheduleDeadline(\"settle\""));
		assertFalse(handler.contains("spawn("));

		String coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"301630000\"")).findFirst().orElseThrow();
		assertTrue(coverage.contains("retail condition spawns and Pattern own Luna phase waves"));
	}

	@Test
	void lunaHandlersQueueEveryPersistentParticipant() throws Exception {
		for (String[] handler : new String[][] {
				{ "luna/ContaminatedUnderpathInstance.java", "luna" },
				{ "luna/SecretMunitionsFactoryInstance.java", "luna" },
				{ "event/Event_ContaminatedUnderpathInstance.java", "timeattack" } }) {
			String source = Files.readString(Path.of(
					"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + handler[0]));
			assertTrue(source.contains("runtimeState().put(playerKey(playerId), true)"), handler[0]);
			assertTrue(source.contains("runtimeState().snapshot(STATE + \"player.\")"), handler[0]);
			assertTrue(source.contains("InstanceSettlementService.queue(instance, playerId, \"" + handler[1]
					+ "\", plan)"), handler[0]);
			assertTrue(source.contains("if (runtimeState().getBoolean(STATE + \"completed\", false)) {\n\t\t\tsettlePlayers();"), handler[0]);
		}
	}

	private static String block(String value, String startMarker, String endMarker) {
		int start = value.indexOf(startMarker);
		int end = value.indexOf(endMarker, start);
		return value.substring(start, end + endMarker.length());
	}
}
