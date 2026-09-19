package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接取携带道具门禁：生产 {@code inventory-items} 必须能回指真端 {@code inventory_item_name}。
 * <p>
 * Aion 5.8 的 {@code check_item} 是任务中段检查/消耗物，不是接取前置；私有
 * {@code quest_data.xml} 迁移曾把两者都写进 {@code inventory_items}，导致 30721
 * 的镇静剂在接取阶段被当成必需物而拒绝接取。普通检查物只能保留在 transition
 * 的 {@code item-play}/{@code has-item} 合同中，不能进入接取资格门控。
 * <p>
 * Gate: production {@code inventory-items} must be backed by the same quest's
 * retail {@code inventory_item_name}; retail {@code check_item} alone is not an
 * accept-time requirement.
 */
class QuestInventoryStartItemGateTest {

	private static final String CONTRACT_RESOURCE =
		"/quest/quest-inventory-start-item-retail-contract.tsv";
	private static final int SEDATIVE_ITEM_ID = 182215698;

	@Test
	void productionInventoryItemsAreBackedByRetailInventoryItemNames() throws Exception {
		Map<Integer, Set<Integer>> retail = loadRetailContract();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> violations = new ArrayList<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			Set<Integer> allowed = retail.getOrDefault(compiled.id(), Set.of());
			for (QuestItemRequirement requirement : compiled.definition().metadata().inventoryItems()) {
				if (!allowed.contains(requirement.itemId())) {
					violations.add("quest " + compiled.id() + " gates accept on item "
						+ requirement.itemId() + " which is not a retail inventory_item_name");
				}
			}
		}
		assertTrue(violations.isEmpty(),
			() -> "inventory-items must be accept-time retail inventory_item_name items: " + violations);
	}

	@Test
	void quest30721DoesNotGateAcceptanceOnItsMidQuestSedative() throws Exception {
		QuestDefinition definition = load(30721);
		assertTrue(definition.metadata().inventoryItems().isEmpty(),
			"Sedative is granted by 804868 after accept, not carried into accept");

		QuestTransition accept = definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode())
				&& "s0".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 804704
				&& Integer.valueOf(QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(accept.conditions().contains(new QuestCondition.StartEligible()),
			"30721 accept route must remain start-eligible gated");
		assertTrue(accept.actions().isEmpty(),
			"30721 must not grant Sedative at accept");

		QuestTransition grant = definition.transitions().stream()
			.filter(transition -> "s1".equals(transition.sourceNode())
				&& "s2".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		assertTrue(grant.actions().contains(
			new QuestAction.GiveItem(SEDATIVE_ITEM_ID, 1)),
			"30721 gives Sedative at the 804868 step");

		QuestTransition consume = definition.transitions().stream()
			.filter(transition -> "s2".equals(transition.sourceNode())
				&& "s3".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(new QuestEvent.ItemPlay(SEDATIVE_ITEM_ID, 3000), consume.event(),
			"30721 consumes Sedative through item-play");
		assertTrue(consume.actions().contains(
			new QuestAction.RemoveItem(SEDATIVE_ITEM_ID, 1)),
			"30721 removes the consumed Sedative");
	}

	private static Map<Integer, Set<Integer>> loadRetailContract() throws Exception {
		Map<Integer, Set<Integer>> result = new HashMap<>();
		try (InputStream input = QuestInventoryStartItemGateTest.class
				.getResourceAsStream(CONTRACT_RESOURCE)) {
			assertNotNull(input, CONTRACT_RESOURCE + " must exist on the test classpath");
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#")) {
						continue;
					}
					String[] columns = line.split("\t", -1);
					assertEquals(2, columns.length, "contract row must have 2 columns: " + line);
					Set<Integer> itemIds = new TreeSet<>();
					if (!columns[1].isBlank()) {
						for (String itemId : columns[1].split(",")) {
							itemIds.add(Integer.parseInt(itemId));
						}
					}
					result.put(Integer.parseInt(columns[0]), Set.copyOf(itemIds));
				}
			}
		}
		assertTrue(!result.isEmpty(), "retail inventory-item contract must not be empty");
		return result;
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestInventoryStartItemGateTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, "quest " + questId + " definition must exist");
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
