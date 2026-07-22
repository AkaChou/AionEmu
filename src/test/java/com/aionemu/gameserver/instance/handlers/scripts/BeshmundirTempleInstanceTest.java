package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class BeshmundirTempleInstanceTest {
	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/BeshmundirTempleInstance.java");
	private static final Path CONDITIONS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/300170000_Beshmundir_Temple.xml");

	@Test
	void summonsRespondentOnlyForActiveQuestWithOil() {
		QuestState active = new QuestState(30208, QuestStatus.START, 0, 0, null, null, null);
		QuestState completed = new QuestState(30208, QuestStatus.COMPLETE, 0, 1, null, null, null);

		assertTrue(BeshmundirTempleInstance.canSummonRespondent(active, 1));
		assertFalse(BeshmundirTempleInstance.canSummonRespondent(active, 0));
		assertFalse(BeshmundirTempleInstance.canSummonRespondent(completed, 1));
		assertFalse(BeshmundirTempleInstance.canSummonRespondent(null, 1));
	}

	@Test
	void handlerKeepsOnlyInteractionsMissingFromRetailData() throws Exception {
		String source = Files.readString(HANDLER);
		assertTrue(source.contains("case 730274 ->"));
		assertTrue(source.contains("case 730290 ->"));
		for (String forbidden : new String[] { "Future<?>", "onDropRegistered", "onDie(", "GameThreadPoolServices",
				"macunbelloSoul", "warriorMonument", "sendMovie(", "sendMsgByRace" }) {
			assertFalse(source.contains(forbidden), forbidden);
		}
	}

	@Test
	void retailConditionsOwnDifficultySpecificBossesAndProducers() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(CONDITIONS.toFile());
		Element world = null;
		for (Element candidate : elements(document.getElementsByTagName("world"))) {
			if ("300170000".equals(candidate.getAttribute("id"))) {
				world = candidate;
				break;
			}
		}
		assertTrue(world != null);
		assertEquals(83, world.getElementsByTagName("condition").getLength());
		Set<String> variables = new HashSet<>();
		for (Element variable : elements(world.getElementsByTagName("variable"))) {
			variables.add(variable.getAttribute("name"));
		}
		assertTrue(variables.containsAll(Set.of("debufflich", "doorwall_spawn", "idct_spectern_spawn",
			"idct_specterh_spawn")));

		Map<Integer, Map<Integer, Integer>> producerCounts = new HashMap<>();
		Map<Integer, String> walkers = new HashMap<>();
		for (Element condition : elements(world.getElementsByTagName("condition"))) {
			int page = Integer.parseInt(condition.getAttribute("page_start"));
			String expression = condition.getAttribute("expression");
			Set<Integer> ids = new HashSet<>();
			for (Element npc : elements(condition.getElementsByTagName("npc"))) {
				int npcId = Integer.parseInt(npc.getAttribute("id"));
				ids.add(npcId);
				if (!npc.getAttribute("walker").isBlank()) {
					walkers.put(npcId, npc.getAttribute("walker"));
				}
				if ("1".equals(expression)) {
					producerCounts.computeIfAbsent(page, ignored -> new HashMap<>()).merge(npcId, 1, Integer::sum);
				}
			}
			if (ids.contains(216239)) {
				assertEquals(1, page);
				assertTrue(expression.contains("IDCT_SpecterN_Spawn >= 10"));
			}
			if (ids.contains(216158)) {
				assertEquals(2, page);
				assertTrue(expression.contains("IDCT_SpecterH_Spawn >= 10"));
			}
		}
		assertEquals(15, producerCounts.get(1).get(216739));
		assertEquals(15, producerCounts.get(2).get(216740));
		assertTrue(producerCounts.get(1).keySet().containsAll(Set.of(216587, 216588, 216589)));
		assertTrue(producerCounts.get(2).keySet().containsAll(Set.of(216583, 216584, 216585)));
		assertEquals("retail:300170000:path_12", walkers.get(216161));
		assertEquals("retail:300170000:hugeslime_path", walkers.get(216163));
		assertEquals("retail:300170000:path_8", walkers.get(216247));
		assertEquals("retail:300170000:path_6", walkers.get(216248));
	}

	@Test
	void conditionManagedNpcsHaveNoLegacyStaticSpawn() throws Exception {
		Set<Integer> managed = new HashSet<>();
		var conditionDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(CONDITIONS.toFile());
		for (Element world : elements(conditionDocument.getElementsByTagName("world"))) {
			if ("300170000".equals(world.getAttribute("id"))) {
				for (Element npc : elements(world.getElementsByTagName("npc"))) {
					managed.add(Integer.parseInt(npc.getAttribute("id")));
				}
			}
		}
		Set<Integer> legacy = new HashSet<>();
		var spawnDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(SPAWNS.toFile());
		for (Element spawn : elements(spawnDocument.getElementsByTagName("spawn"))) {
			legacy.add(Integer.parseInt(spawn.getAttribute("npc_id")));
		}
		managed.retainAll(legacy);
		assertTrue(managed.isEmpty(), managed.toString());
		assertTrue(legacy.contains(216586));
	}

	private static Iterable<Element> elements(NodeList nodes) {
		return () -> new java.util.Iterator<>() {
			private int index;

			@Override
			public boolean hasNext() {
				return index < nodes.getLength();
			}

			@Override
			public Element next() {
				return (Element) nodes.item(index++);
			}
		};
	}
}
