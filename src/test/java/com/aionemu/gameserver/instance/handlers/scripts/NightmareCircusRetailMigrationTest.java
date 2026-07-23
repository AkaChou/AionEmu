package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class NightmareCircusRetailMigrationTest {

	@Test
	void worldVersionOwnsTheCircusFlow() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "301200000");
		assertEquals(4, count(world, "<variable "));
		assertEquals(124, count(world, "<condition "));
		assertEquals(124, count(world, "<slot>"));
		assertEquals(222, count(world, "<npc id="));
		for (String required : new String[] { "condition_s1", "condition_s2", "condition_s3", "condition_talk",
				"<npc id=\"233450\"", "<npc id=\"233453\"", "<npc id=\"233455\"",
				"<npc id=\"233467\"", "<npc id=\"831796\"" }) {
			assertTrue(world.contains(required), required);
		}
		for (String partyVersionNpc : new String[] { "233144", "233147", "233149", "233153", "233161" }) {
			assertFalse(world.contains("<npc id=\"" + partyVersionNpc + "\""), partyVersionNpc);
		}
	}

	@Test
	void staticProducersAndMinimalHandlerKeepOnlyUnmodeledCleanup() throws Exception {
		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301200000_The_Nightmare_Circus.xml"));
		assertEquals(2, count(spawns, "<spawn npc_id="));
		assertTrue(spawns.contains("npc_id=\"831740\""));
		assertTrue(spawns.contains("npc_id=\"831747\""));
		for (String staleNpc : new String[] { "831573", "831720", "831721", "831722", "831723" }) {
			assertFalse(spawns.contains("npc_id=\"" + staleNpc + "\""), staleNpc);
		}

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/NightmareCircusInstance.java"));
		for (String effect : new String[] { "21469", "21470", "21471", "21472" }) {
			assertTrue(handler.contains("removeEffect(" + effect + ")"), effect);
		}
		for (String legacy : new String[] { "GameThreadPoolServices", "Future", "onDropRegistered", "onDie(",
				"spawn(", "onInstanceCreate", "onEnterInstance", "onReviveEvent", "sendMovie" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"301200000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail condition/static spawns own the circus flow"));
		assertTrue(ownership.contains(
			"handler only removes effects 21469/21470/21471/21472 on logout/leave"));
	}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		int end = xml.indexOf("</world>", start);
		return xml.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
