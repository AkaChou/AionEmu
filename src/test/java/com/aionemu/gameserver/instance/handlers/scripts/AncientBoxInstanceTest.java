package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

class AncientBoxInstanceTest {

	private static final Set<Integer> ANCIENT_BOX_IDS = Set.of(702700, 702701);
	private static final Path NPC_TEMPLATES = Path.of(
		"src/main/resources/aion/data/static_data/npcs/npc_template.xml");
	private static final Path CHEST_TEMPLATES = Path.of(
		"src/main/resources/aion/data/static_data/chests/chest_templates.xml");
	private static final Path ACTION_ITEM_AI = Path.of(
		"src/main/java/com/aionemu/gameserver/ai/ActionItemNpcAI2.java");
	private static final Path CHEST_AI = Path.of(
		"src/main/java/com/aionemu/gameserver/ai/ChestAI2.java");
	private static final Path INSTANCE = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SealedDanuarMysticariumInstance.java");

	@Test
	void ancientBoxesAreKeylessThreeSecondChests() throws Exception {
		Map<Integer, NpcInteraction> interactions = readNpcInteractions();
		Map<Integer, Integer> keyItems = readChestKeyItems();

		assertEquals(ANCIENT_BOX_IDS, interactions.keySet());
		assertEquals(ANCIENT_BOX_IDS, keyItems.keySet());
		for (int npcId : ANCIENT_BOX_IDS) {
			assertEquals("chest", interactions.get(npcId).ai());
			assertEquals(3, interactions.get(npcId).delaySeconds());
			assertEquals(0, keyItems.get(npcId));
		}
	}

	@Test
	void actionItemUseCanBeInterruptedBeforeTheChestOpens() throws Exception {
		String source = Files.readString(ACTION_ITEM_AI);
		String start = methodBody(source, "protected void handleUseItemStart(final Player player)");

		int observer = start.indexOf("new ItemUseObserver()");
		int cancelTask = start.indexOf("cancelTask(TaskId.ACTION_ITEM_NPC)", observer);
		int cancelProgress = start.indexOf("cancelBarAnimation", cancelTask);
		int schedule = start.indexOf("addTask(TaskId.ACTION_ITEM_NPC", observer);
		int finish = start.indexOf("handleUseItemFinish(player)", schedule);

		assertTrue(observer >= 0);
		assertTrue(cancelTask > observer);
		assertTrue(cancelProgress > cancelTask);
		assertTrue(schedule > cancelProgress);
		assertTrue(finish > schedule);
	}

	@Test
	void chestDiesBeforeItsDropIsRegisteredAndTheLootWindowOpens() throws Exception {
		String source = Files.readString(CHEST_AI);
		String finish = methodBody(source, "protected void handleUseItemFinish(Player player)");

		int analyze = finish.indexOf("analyzeOpening(player)");
		int death = finish.indexOf("AI2Actions.dieSilently(this, player)", analyze);
		int register = finish.indexOf("registerDrop(getOwner(), player, maxLevel(players), players)", death);
		int lootWindow = finish.indexOf("requestDropList(player, getObjectId())", register);

		assertTrue(analyze >= 0);
		assertTrue(death > analyze);
		assertTrue(register > death);
		assertTrue(lootWindow > register);
	}

	@Test
	void instanceDropHookAcceptsBothAncientBoxTemplates() throws Exception {
		String source = Files.readString(INSTANCE);
		String onDropRegistered = methodBody(source, "public void onDropRegistered(Npc npc)");

		assertTrue(onDropRegistered.contains("case 702700:"));
		assertTrue(onDropRegistered.contains("case 702701:"));
	}

	private static Map<Integer, NpcInteraction> readNpcInteractions() throws Exception {
		Map<Integer, NpcInteraction> interactions = new HashMap<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		try (InputStream input = Files.newInputStream(NPC_TEMPLATES)) {
			XMLStreamReader reader = factory.createXMLStreamReader(input);
			int npcId = 0;
			String ai = null;
			int delay = 0;
			while (reader.hasNext() && interactions.size() < ANCIENT_BOX_IDS.size()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("npc_template")) {
					npcId = Integer.parseInt(reader.getAttributeValue(null, "npc_id"));
					ai = reader.getAttributeValue(null, "ai");
					delay = 0;
				} else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("talk_info")
						&& ANCIENT_BOX_IDS.contains(npcId)) {
					delay = Integer.parseInt(reader.getAttributeValue(null, "delay"));
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("npc_template")
						&& ANCIENT_BOX_IDS.contains(npcId)) {
					interactions.put(npcId, new NpcInteraction(ai, delay));
				}
			}
			reader.close();
		}
		return interactions;
	}

	private static Map<Integer, Integer> readChestKeyItems() throws Exception {
		Map<Integer, Integer> keyItems = new HashMap<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		try (InputStream input = Files.newInputStream(CHEST_TEMPLATES)) {
			XMLStreamReader reader = factory.createXMLStreamReader(input);
			int npcId = 0;
			while (reader.hasNext() && keyItems.size() < ANCIENT_BOX_IDS.size()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("chest")) {
					npcId = Integer.parseInt(reader.getAttributeValue(null, "npcid"));
				} else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("keyitem")
						&& ANCIENT_BOX_IDS.contains(npcId)) {
					keyItems.put(npcId, Integer.parseInt(reader.getAttributeValue(null, "itemid")));
				}
			}
			reader.close();
		}
		return keyItems;
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

	private record NpcInteraction(String ai, int delaySeconds) {
	}
}
