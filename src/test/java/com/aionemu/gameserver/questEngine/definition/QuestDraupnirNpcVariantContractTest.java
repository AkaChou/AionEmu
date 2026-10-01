package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDraupnirNpcVariantContractTest {
	private static final Path SPAWN_DATA = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/320080000_Draupnir_Cave.xml");
	private static final Path INSTANCE_HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DraupnirCaveInstance.java");
	private static final Path QUEST_DATA = Path.of(
		"src/main/resources/aion/data/static_data/quest/definitions/quests");
	private static final Map<Integer, Integer> RETIRED_NPC_VARIANTS = Map.of(
		213775, 236924,
		213778, 237265,
		213780, 236929,
		213802, 237267);

	private static final List<QuestVariants> QUEST_VARIANTS = List.of(
		quest(3532, variant(213780, 236929)),
		quest(17001, variant(213780, 236929)),
		quest(27001, variant(213780, 236929)),
		quest(4525, variant(213780, 236929)),
		// 真端 DataDriven 行把 14252/24252 的段 2 目标改为 IDDF3_DrakanFiBossD_50_Ah_SP_2（237263，
		// 独立 name_id 337916）：213780/236929（name_id 315799）不再是这两条顺序链的击杀目标；段 1 的
		// 显示名族（315794）仍同时覆盖退役 213775 与实刷 236924，变体对保留。
		// The retail DataDriven rows switched stage 2 of 14252/24252 to IDDF3_DrakanFiBossD_50_Ah_SP_2
		// (237263, its own name_id), so 213780/236929 are no longer kill targets of these sequential
		// chains; the stage-1 display family still covers both 213775 and the live 236924.
		quest(14252, variant(213775, 236924)),
		quest(24252, variant(213775, 236924)),
		quest(80215, variant(213778, 237265), variant(213802, 237267), variant(213780, 236929)),
		quest(80224, variant(213778, 237265), variant(213802, 237267), variant(213780, 236929)),
		quest(80734, variant(213780, 236929)),
		quest(80805, variant(213780, 236929)),
		quest(80399, variant(213802, 237267)),
		quest(80400, variant(213778, 237265)),
		quest(80415, variant(213802, 237267)),
		quest(80416, variant(213778, 237265)),
		quest(80434, variant(213802, 237267)),
		quest(80435, variant(213778, 237265)),
		quest(80450, variant(213802, 237267)),
		quest(80451, variant(213778, 237265)),
		quest(19676, variant(213780, 236929)),
		quest(29676, variant(213780, 236929)));

	private static final List<QuestVariants> DROP_VARIANTS = List.of(
		quest(2631, variant(213775, 236924)));

	@Test
	void liveNpcVariantsPreserveEachBaseKillContract() throws Exception {
		for (QuestVariants questVariants : QUEST_VARIANTS) {
			CompiledQuestDefinition definition = load(questVariants.questId());
			for (NpcVariant variant : questVariants.variants()) {
				List<QuestTransition> baseRoutes = killRoutes(definition, variant.baseNpcId());
				List<QuestTransition> liveRoutes = killRoutes(definition, variant.liveNpcId());

				assertFalse(baseRoutes.isEmpty(), () -> "missing base route " + questVariants.questId()
					+ " npc " + variant.baseNpcId());
				assertFalse(liveRoutes.isEmpty(), () -> "missing live route " + questVariants.questId()
					+ " npc " + variant.liveNpcId());
				for (QuestTransition baseRoute : baseRoutes) {
					QuestTransition liveRoute = liveRoutes.stream()
						.filter(candidate -> sameContractExceptEvent(baseRoute, candidate))
						.findFirst()
						.orElseThrow(() -> new AssertionError("variant route mismatch for quest "
							+ questVariants.questId() + ": " + baseRoute + " -> " + variant.liveNpcId()));
					assertEquivalentKillPlans(definition, baseRoute, liveRoute, variant);
				}
			}
		}
	}

	@Test
	void liveNpcVariantsPreserveQuestDropContracts() throws Exception {
		for (QuestVariants questVariants : DROP_VARIANTS) {
			QuestMetadata metadata = load(questVariants.questId()).definition().metadata();
			for (NpcVariant variant : questVariants.variants()) {
				// 掉落死 id 修复（生产驱动）：真端模板 id 213775 在本服世界无实刷，掉落契约整体
				// 移到实刷变体 236924；退役 id 不再携带死数据（真端行 item 182204478 / 100%）。
				// Dead-id drop repair (production driver): the retail template id 213775 has no live
				// spawn in this world, so the drop contract moves wholesale to the live variant 236924
				// and the retired id carries no dead data (retail row: item 182204478 at 100%).
				assertTrue(metadata.drops().stream().noneMatch(drop -> drop.npcId() == variant.baseNpcId()),
					() -> "retired npc " + variant.baseNpcId() + " must not carry a quest drop for quest "
						+ questVariants.questId());
				QuestDrop liveDrop = singleDrop(metadata, variant.liveNpcId());
				assertEquals(182204478, liveDrop.itemId(), () -> "item mismatch for quest "
					+ questVariants.questId() + " npc " + variant.liveNpcId());
				assertEquals(100, liveDrop.chance(), () -> "chance mismatch for quest "
					+ questVariants.questId() + " npc " + variant.liveNpcId());
			}
		}
	}

	@Test
	void everyQuestReferenceToARetiredDraupnirNpcAlsoIncludesItsLiveVariant() throws Exception {
		// 退役任务的 XML 不再进仓：本扫描只覆盖仍由 XML 拥有的生产任务；
		// 真端驱动任务的 NPC id 由 RetailNpcNameIndex 的同名族闭包给出（见 RetailSimpleHunt* 门禁）。
		// Retired XMLs are gone; retail-driven quests resolve npc ids through the name-family closure.
		for (Path dir : List.of(QUEST_DATA)) {
			try (var paths = Files.list(dir)) {
				for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".xml")).toList()) {
					String xml = Files.readString(path);
					for (Map.Entry<Integer, Integer> variant : RETIRED_NPC_VARIANTS.entrySet()) {
						if (containsNpcId(xml, variant.getKey())) {
							assertTrue(containsNpcId(xml, variant.getValue()), () -> path.getFileName()
								+ " references retired NPC " + variant.getKey()
								+ " without live variant " + variant.getValue());
						}
					}
				}
			}
		}
	}

	@Test
	void keepsOnlyAuthoritativeStaticDraupnirVariantsAndGeneratedTargets() throws Exception {
		Set<Integer> trackedNpcIds = Set.of(213775, 213776, 213778, 213779, 213780, 213802,
			236924, 236925, 236928, 236929, 237263, 237264, 237265, 237266, 237267, 237275);
		Set<Integer> liveStaticNpcIds = staticSpawnIdsWithSpots();

		assertEquals(Set.of(213776, 213779, 236924, 237265, 237267), liveStaticNpcIds.stream()
			.filter(trackedNpcIds::contains).collect(Collectors.toSet()));

		String handler = Files.readString(INSTANCE_HANDLER);
		assertTrue(Pattern.compile("(?s).*\\bcase 237265:.*").matcher(handler).matches());
		assertTrue(Pattern.compile("(?s).*\\bcase 237267:.*").matcher(handler).matches());
		assertTrue(Pattern.compile("case 213780: //Commander Bakarma\\.\\s*\\n\\s*case 236929: //Commander Bakarma\\.")
			.matcher(handler).find());
		assertTrue(Pattern.compile("\\bspawn\\(236929\\s*,").matcher(handler).find());
		assertTrue(Pattern.compile("\\bspawn\\(237275\\s*,").matcher(handler).find());
	}

	private static QuestDrop singleDrop(QuestMetadata metadata, int npcId) {
		List<QuestDrop> drops = metadata.drops().stream()
			.filter(drop -> drop.npcId() == npcId)
			.toList();
		assertEquals(1, drops.size(), () -> "expected one quest drop for npc " + npcId);
		return drops.getFirst();
	}

	private static boolean containsNpcId(String xml, int npcId) {
		return Pattern.compile("(?<!\\d)" + npcId + "(?!\\d)").matcher(xml).find();
	}

	private static Set<Integer> staticSpawnIdsWithSpots() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList spawns;
		try (InputStream input = Files.newInputStream(SPAWN_DATA)) {
			spawns = factory.newDocumentBuilder().parse(input).getElementsByTagName("spawn");
		}

		Set<Integer> result = new HashSet<>();
		for (int index = 0; index < spawns.getLength(); index++) {
			Element spawn = (Element) spawns.item(index);
			if (spawn.getElementsByTagName("spot").getLength() > 0) {
				result.add(Integer.parseInt(spawn.getAttribute("npc_id")));
			}
		}
		return result;
	}

	private static List<QuestTransition> killRoutes(CompiledQuestDefinition definition, int npcId) {
		return definition.transitionsFor("KILL_NPC").stream()
			.filter(transition -> QuestEvent.matches(transition.event(), new QuestEvent.KillNpc(npcId)))
			.toList();
	}

	private static boolean sameContractExceptEvent(QuestTransition base, QuestTransition variant) {
		return Objects.equals(base.sourceNode(), variant.sourceNode())
			&& base.targetNode().equals(variant.targetNode())
			&& base.conditions().equals(variant.conditions())
			&& base.actions().equals(variant.actions())
			&& base.afterCommit().equals(variant.afterCommit())
			&& Objects.equals(base.priority(), variant.priority());
	}

	private static void assertEquivalentKillPlans(CompiledQuestDefinition definition,
			QuestTransition baseRoute, QuestTransition variantRoute, NpcVariant variant) {
		QuestMutationPlan basePlan = plan(definition, baseRoute, variant.baseNpcId());
		QuestMutationPlan variantPlan = plan(definition, variantRoute, variant.liveNpcId());
		assertEquals(basePlan.nextStatus(), variantPlan.nextStatus(), () -> "status mismatch for quest "
			+ definition.id() + " npc " + variant.liveNpcId());
		assertEquals(basePlan.nextPackedVariables(), variantPlan.nextPackedVariables(),
			() -> "variable projection mismatch for quest " + definition.id()
				+ " npc " + variant.liveNpcId());
		assertEquals(basePlan.requiredActions(), variantPlan.requiredActions(),
			() -> "action mismatch for quest " + definition.id() + " npc " + variant.liveNpcId());
		assertEquals(basePlan.afterCommit(), variantPlan.afterCommit(),
			() -> "after-commit mismatch for quest " + definition.id() + " npc " + variant.liveNpcId());
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition definition,
			QuestTransition transition, int npcId) {
		QuestNode source = node(definition, transition.sourceNode());
		QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), source.projection().status(),
			definition.definition().progressLayout().pack(source.projection().variables()), Map.of());
		return QuestMutationPlanner.plan(definition, snapshot, new QuestEvent.KillNpc(npcId), transition)
			.orElseThrow(() -> new AssertionError("unplannable route for quest " + definition.id()
				+ " npc " + npcId + ": " + transition));
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(node -> node.label().equals(label))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing node " + label + " in quest " + definition.id()));
	}

	/**
	 * 生产定义：XML 目录 + 真端 overlay；退役任务由真端驱动提供（旧 XML 只在 git 历史里）。
	 * Production definition via the production view; retired quests come from the retail driver.
	 */
	private static CompiledQuestDefinition load(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}

	private static QuestVariants quest(int questId, NpcVariant... variants) {
		return new QuestVariants(questId, List.of(variants));
	}

	private static NpcVariant variant(int baseNpcId, int liveNpcId) {
		return new NpcVariant(baseNpcId, liveNpcId);
	}

	private record QuestVariants(int questId, List<NpcVariant> variants) {
	}

	private record NpcVariant(int baseNpcId, int liveNpcId) {
	}
}
