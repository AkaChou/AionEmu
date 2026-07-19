package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class KumukiCaveInstanceTest {
	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KumukiCaveInstance.java");
	private static final Path CONDITIONS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
	private static final Path WAYPOINTS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml");
	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/302330000_Kumuki_Cave.xml");
	private static final Path JSM_PATTERNS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idevent_solo_jsm.xml");
	private static final Path YDY_PATTERNS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idevent_solo_ydy.xml");

	@Test
	void handlerKeepsOnlyTheRetailKeyDoorAndPlayerCleanup() throws Exception {
		String source = Files.readString(HANDLER);
		for (String required : new String[] { "703424", "185000295", "185000296", "186000459", "164002390",
				"16973", "16974", "17619", "17623", "1403686" }) {
			assertTrue(source.contains(required), required);
		}
		for (String forbidden : new String[] { "Future", "onDropRegistered", "onDie(", "spawn(", "sendMovie",
				"SM_PLAY_MOVIE", "GameThreadPoolServices", "sendMsg", "poppySaved", "doors" }) {
			assertFalse(source.contains(forbidden), forbidden);
		}
	}

	@Test
	void retailConditionsOwnBothPagesAndCriticalActors() throws Exception {
		Element world = world302330000();
		Set<String> variables = new HashSet<>();
		for (Element variable : elements(world.getElementsByTagName("variable"))) {
			variables.add(variable.getAttribute("name"));
		}
		assertEquals(Set.of("chase_on", "con_bakaki_die", "con_remove_all", "con_time_attack",
			"idevent_solo_rush", "time_attack_spawn01", "time_attack_spawn02", "time_attack_spawn03",
			"time_attack_spawn04", "tog_spawn_mob"), variables);
		assertEquals(114, world.getElementsByTagName("condition").getLength());
		assertEquals(366, world.getElementsByTagName("slot").getLength());

		Map<String, Integer> initialSlots = new HashMap<>();
		for (Element condition : elements(world.getElementsByTagName("condition"))) {
			if (condition.getAttribute("source").contains("#producer-page-")) {
				initialSlots.put(condition.getAttribute("page_start") + "-" + condition.getAttribute("page_end"),
					condition.getElementsByTagName("npc").getLength());
			}
		}
		assertEquals(Map.of("1-1", 113, "1-2", 29, "2-2", 113), initialSlots);

		for (int npcId : new int[] { 246279, 246280, 246281, 246282, 246305 }) {
			assertNpcPage(world, npcId, 1, 1);
		}
		for (int npcId : new int[] { 246312, 246313, 246314, 246315, 246338 }) {
			assertNpcPage(world, npcId, 2, 2);
		}
		for (int npcId : new int[] { 703425, 703426, 703427, 703428, 835071, 835090, 835091, 835092, 835093 }) {
			assertNpcPage(world, npcId, 1, 2);
		}
		assertNpcExpression(world, 835104, "con_time_attack == 1");
		assertNpcExpression(world, 835057, "con_bakaki_die == 1");
	}

	@Test
	void conditionManagedNpcsHaveNoLegacyStaticSpawnAndAllPathsExist() throws Exception {
		Element world = world302330000();
		Set<Integer> managed = npcIds(world);
		Set<Integer> legacy = new HashSet<>();
		var spawnDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(SPAWNS.toFile());
		for (Element spawn : elements(spawnDocument.getElementsByTagName("spawn"))) {
			legacy.add(Integer.parseInt(spawn.getAttribute("npc_id")));
		}
		Set<Integer> overlap = new HashSet<>(managed);
		overlap.retainAll(legacy);
		assertTrue(overlap.isEmpty(), overlap.toString());
		assertTrue(legacy.containsAll(Set.of(703424, 246294)));

		Set<String> routes = new HashSet<>();
		var waypointDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(WAYPOINTS.toFile());
		for (Element template : elements(waypointDocument.getElementsByTagName("walker_template"))) {
			String route = template.getAttribute("route_id");
			if (route.startsWith("retail:302330000:")) {
				routes.add(route);
			}
		}
		assertEquals(40, routes.size());
		for (Element npc : elements(world.getElementsByTagName("npc"))) {
			String walker = npc.getAttribute("walker");
			assertTrue(walker.isEmpty() || routes.contains(walker), walker);
		}
	}

	@Test
	void retailPatternsOwnPorgusTimerMovieGinsengAndBossChains() throws Exception {
		var jsm = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(JSM_PATTERNS.toFile());
		var ydy = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(YDY_PATTERNS.toFile());

		Element porgus = pattern(jsm.getElementsByTagName("npc_ai_pattern"), "IDEvent_Solo_Porguss_01");
		assertTrue(porgus.getElementsByTagName("give_item_by_user_indicator").getLength() > 0);
		assertTrue(porgus.getElementsByTagName("random_move").getLength() > 0);
		assertTrue(porgus.getElementsByTagName("despawn_by_nameid").getLength() > 0);

		Element timer = pattern(ydy.getElementsByTagName("npc_ai_pattern"), "IDEvent_Solo_NPC_14");
		assertTrue(timer.getElementsByTagName("on_idle_timer").getLength() > 0);
		for (String variable : new String[] { "time_attack_spawn01", "time_attack_spawn02",
				"time_attack_spawn03", "time_attack_spawn04" }) {
			assertTrue(timer.getTextContent().contains(variable), variable);
		}

		Element movie = pattern(jsm.getElementsByTagName("npc_ai_pattern"), "IDEvent_Solo_Condition_03");
		assertEquals("951", movie.getElementsByTagName("cutscene_id").item(0).getTextContent());

		Element ginseng = pattern(ydy.getElementsByTagName("npc_ai_pattern"), "IDEvent_Solo_NPC_03");
		assertTrue(ginseng.getElementsByTagName("use_skill").getLength() > 0);
		assertEquals("3", ginseng.getElementsByTagName("num_to_spawn").item(0).getTextContent());
		assertTrue(ginseng.getElementsByTagName("despawn_self").getLength() > 0);

		for (String boss : new String[] { "IDEvent_Solo_Zone5_Boss", "IDEvent_Solo_Monster_04" }) {
			assertTrue(pattern(jsm.getElementsByTagName("npc_ai_pattern"), boss).getTextContent()
				.contains("con_bakaki_die"), boss);
		}
	}

	private static Element world302330000() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(CONDITIONS.toFile());
		for (Element world : elements(document.getElementsByTagName("world"))) {
			if ("302330000".equals(world.getAttribute("id"))) {
				return world;
			}
		}
		throw new AssertionError("missing world 302330000");
	}

	private static Set<Integer> npcIds(Element root) {
		Set<Integer> ids = new HashSet<>();
		for (Element npc : elements(root.getElementsByTagName("npc"))) {
			ids.add(Integer.parseInt(npc.getAttribute("id")));
		}
		return ids;
	}

	private static void assertNpcPage(Element world, int npcId, int pageStart, int pageEnd) {
		boolean found = false;
		for (Element condition : elements(world.getElementsByTagName("condition"))) {
			if (npcIds(condition).contains(npcId)) {
				found = true;
				assertEquals(pageStart, Integer.parseInt(condition.getAttribute("page_start")), Integer.toString(npcId));
				assertEquals(pageEnd, Integer.parseInt(condition.getAttribute("page_end")), Integer.toString(npcId));
			}
		}
		assertTrue(found, Integer.toString(npcId));
	}

	private static void assertNpcExpression(Element world, int npcId, String expression) {
		for (Element condition : elements(world.getElementsByTagName("condition"))) {
			if (npcIds(condition).contains(npcId) && expression.equals(condition.getAttribute("expression"))) {
				return;
			}
		}
		throw new AssertionError(npcId + " missing expression " + expression);
	}

	private static Element pattern(NodeList patterns, String name) {
		for (Element pattern : elements(patterns)) {
			NodeList names = pattern.getElementsByTagName("name");
			if (names.getLength() > 0 && name.equals(names.item(0).getTextContent())) {
				return pattern;
			}
		}
		throw new AssertionError("missing pattern " + name);
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
