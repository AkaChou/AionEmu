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
		"src/main/resources/aion/data/static_data/quest_definition/quests");
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
		quest(14252, variant(213775, 236924), variant(213780, 236929)),
		quest(24252, variant(213775, 236924), variant(213780, 236929)),
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
				QuestDrop baseDrop = singleDrop(metadata, variant.baseNpcId());
				QuestDrop liveDrop = singleDrop(metadata, variant.liveNpcId());

				assertEquals(baseDrop.itemId(), liveDrop.itemId(), () -> "item mismatch for quest "
					+ questVariants.questId() + " npc " + variant.liveNpcId());
				assertEquals(baseDrop.chance(), liveDrop.chance(), () -> "chance mismatch for quest "
					+ questVariants.questId() + " npc " + variant.liveNpcId());
				assertEquals(baseDrop.eachMember(), liveDrop.eachMember(), () -> "member scope mismatch for quest "
					+ questVariants.questId() + " npc " + variant.liveNpcId());
				assertEquals(baseDrop.collectingStep(), liveDrop.collectingStep(),
					() -> "collecting step mismatch for quest " + questVariants.questId()
						+ " npc " + variant.liveNpcId());
				assertEquals(baseDrop.scope(), liveDrop.scope(), () -> "drop scope mismatch for quest "
					+ questVariants.questId() + " npc " + variant.liveNpcId());
			}
		}
	}

	@Test
	void everyQuestReferenceToARetiredDraupnirNpcAlsoIncludesItsLiveVariant() throws Exception {
		try (var paths = Files.list(QUEST_DATA)) {
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

	@Test
	void acceptsTheCurrentAkhalRouteForBothFactionQuests() throws Exception {
		for (int questId : List.of(14252, 24252)) {
			assertFalse(killRoutes(load(questId), 237275).isEmpty(),
				() -> "quest " + questId + " must accept the live Akhal npc 237275");
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

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
			QuestDraupnirNpcVariantContractTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
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
