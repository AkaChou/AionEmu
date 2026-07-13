package com.aionemu.gameserver.controllers.attack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;

class AggroListTest {

	@Test
	void addHateUsesKnownListSnapshotVisitorForKnownPlayers() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/attack/AggroList.java"));

		assertTrue(source.contains("doOnAllPlayers"));
		assertFalse(source.contains("getKnownPlayers().values()"));
	}

	@Test
	void getListReturnsSnapshotSafeForRemovalDuringIteration() throws ReflectiveOperationException {
		AggroList aggroList = new AggroList(null);
		Map<Integer, AggroInfo> entries = new LinkedHashMap<Integer, AggroInfo>();
		entries.put(1, new AggroInfo(null));
		entries.put(2, new AggroInfo(null));
		entries.put(3, new AggroInfo(null));
		setAggroList(aggroList, entries);

		Collection<AggroInfo> snapshot = aggroList.getList();

		assertDoesNotThrow(() -> {
			for (AggroInfo aggroInfo : snapshot) {
				entries.values().remove(aggroInfo);
			}
		});
	}

	@Test
	void timedHateIsRemovedOnlyFromTheOriginalEntry() throws ReflectiveOperationException {
		AggroList aggroList = new AggroList(null);
		Map<Integer, AggroInfo> entries = new LinkedHashMap<Integer, AggroInfo>();
		AggroInfo original = new AggroInfo(null);
		original.setHate(60);
		long token = original.addVolatileHate(40);
		entries.put(1, original);
		setAggroList(aggroList, entries);

		aggroList.removeTimedHate(1, original, token);
		assertEquals(60, original.getHate());

		AggroInfo replacement = new AggroInfo(null);
		replacement.setHate(200);
		entries.put(1, replacement);
		aggroList.removeTimedHate(1, original, token);
		assertEquals(200, replacement.getHate());
	}

	@Test
	void volatileHateCanBeResetWithoutAffectingPermanentHate() {
		AggroInfo aggroInfo = new AggroInfo(null);
		aggroInfo.setHate(100);
		long first = aggroInfo.addVolatileHate(40);
		long second = aggroInfo.addVolatileHate(20);

		aggroInfo.removeVolatileHate(first);
		assertEquals(120, aggroInfo.getHate());
		aggroInfo.resetVolatileHate();
		assertEquals(100, aggroInfo.getHate());
		aggroInfo.removeVolatileHate(second);
		assertEquals(100, aggroInfo.getHate());
	}

	@Test
	void resetsOnlyVolatileHateOrKeepsMostHatedAtOne() throws ReflectiveOperationException {
		AggroList aggroList = new AggroList(null);
		AggroInfo mostHated = aggroInfo(100, 60);
		AggroInfo other = aggroInfo(80, 40);
		setAggroList(aggroList, new LinkedHashMap<>(Map.of(1, mostHated, 2, other)));

		aggroList.resetHatepoints(true, true);
		assertEquals(160, mostHated.getHate());
		assertEquals(80, other.getHate());

		aggroList.resetHatepoints(true, false);
		assertEquals(1, mostHated.getHate());
		assertEquals(0, other.getHate());
	}

	private static AggroInfo aggroInfo(int permanentHate, int volatileHate) {
		ObjenesisStd objenesis = new ObjenesisStd();
		Npc attacker = objenesis.newInstance(Npc.class);
		attacker.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		AggroInfo info = new AggroInfo(attacker);
		info.setHate(permanentHate);
		info.addVolatileHate(volatileHate);
		return info;
	}

	private static void setAggroList(AggroList target, Map<Integer, AggroInfo> value) throws ReflectiveOperationException {
		Field field = AggroList.class.getDeclaredField("aggroList");
		field.setAccessible(true);
		field.set(target, value);
	}
}
