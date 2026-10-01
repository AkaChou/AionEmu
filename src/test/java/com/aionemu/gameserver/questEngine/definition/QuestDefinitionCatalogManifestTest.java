package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCatalogManifestTest {
	private static final Set<Integer> SELECTABLE_REWARD_REPAIR_QUESTS = Set.of(2002, 10521, 10530, 20521, 28602);

	@TempDir
	Path tempDirectory;

	@Test
	void externalProductionCatalogCompiles() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		assertFalse(catalog.executables().isEmpty());
		assertTrue(catalog.entries().size() > catalog.executables().size());
		assertTrue(catalog.entries().stream().allMatch(entry -> catalog.findMetadata(entry.id()).isPresent()));
		// P3b 后 11036/11143（SimpleUseItem）、P5-2 后 16977/15517（DataDriven 交付型）、
		// enterworld 批后 10010、链式接取批后 10011 已由真端驱动退役；XML 目录抽检只保留仍在库的
		// can-act 任务 14153（交互物 700282），并反向锁死 10011 的退役可见性。10011 遗留壳的
		// can-act 自环（731785/731786/731787，均是 ai=quest_use_item 的对话 NPC）在真端表与
		// quest.xml 元数据里都没有对应声明（真端链 = 4×talk + EA + 2×hunt + EA + talk，无掉落、
		// 无 collect 步）；交互物 AI 的放行由 talk 路由满足（QuestItemNpcAI2.canStartInteraction），
		// 掉落归属过滤只对带掉落的行有意义，故真端形状无需这些自环。
		// After P3b, P5-2, the enterworld batch and the chain-acquire batch, 10010/10011 are
		// retail-driven; the XML-catalog spot check keeps the still-live 14153 and asserts 10011's
		// retirement is visible. The legacy 10011 can-act self loops (731785/731786/731787, all
		// quest_use_item dialog npcs) have no counterpart in the retail row or its quest.xml metadata
		// (the retail chain is 4 talks, EA, two hunts, EA, talk — no drops, no collect step); the
		// interaction AI is admitted by the talk route alone and drop-owner filtering only matters
		// for rows with drops, so the retail shape needs no such loops.
		assertTrue(catalog.findExecutable(14153).orElseThrow().definition().transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == 700282));
		assertTrue(catalog.findExecutable(10011).isEmpty(),
			"10011 must be retired from the XML catalog (retail-driven since the chain-acquire batch)");
		assertRepeatStartDialogs(catalog);
	}

	@Test
	void rewardSelectionTransitionsRespondInTheSameInteraction() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		int checked = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Map<String, QuestStatus> statuses = definition.nodes().stream().collect(Collectors.toMap(
				QuestNode::label, node -> node.projection().status()));
			for (QuestTransition transition : definition.transitions()) {
				if (statuses.get(transition.sourceNode()) == QuestStatus.REWARD
						|| statuses.get(transition.targetNode()) != QuestStatus.REWARD
						|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)
						|| talk.dialogId() == null
						|| talk.dialogId() != QuestDialogAction.SELECT_QUEST_REWARD.id()) {
					continue;
				}
				checked++;
				assertTrue(transition.afterCommit().stream().anyMatch(
					QuestDefinitionCatalogManifestTest::isDialogResponse),
					"reward selection must respond immediately: quest=" + compiled.id()
						+ " source=" + transition.sourceNode() + " target=" + transition.targetNode()
						+ " npc=" + talk.npcId());
			}
		}
		assertTrue(checked > 0, "reward dialog audit must inspect production routes");
	}

	@Test
	void completionRoutesNeverEmitUnsupportedSelectableRewardActions() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		int checked = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Map<String, QuestStatus> statuses = definition.nodes().stream().collect(Collectors.toMap(
				QuestNode::label, node -> node.projection().status()));
			for (QuestTransition transition : definition.transitions()) {
				if (statuses.get(transition.targetNode()) != QuestStatus.COMPLETE) {
					continue;
				}
				checked++;
				assertFalse(transition.actions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward reward
					&& reward.rewardKind() == QuestRewardKind.SELECTABLE_ITEM),
					"completion must lower selectable rewards to ITEM: quest=" + compiled.id()
						+ " source=" + transition.sourceNode() + " target=" + transition.targetNode());
			}
		}
		assertTrue(checked > 0, "completion reward audit must inspect production routes");
	}

	@Test
	void repairedSelectableRewardQuestsDeliverEveryChoiceAsAConcreteItem() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		for (int questId : SELECTABLE_REWARD_REPAIR_QUESTS) {
			CompiledQuestDefinition compiled = catalog.findExecutable(questId).orElseThrow();
			QuestDefinition definition = compiled.definition();
			Set<Integer> declared = definition.metadata().rewards().stream()
				.filter(reward -> reward.kind().equals("SELECTABLE_ITEM"))
				.map(QuestReward::id)
				.collect(Collectors.toSet());
			Map<String, QuestStatus> statuses = definition.nodes().stream().collect(Collectors.toMap(
				QuestNode::label, node -> node.projection().status()));
			Set<Integer> concrete = definition.transitions().stream()
				.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.COMPLETE)
				.flatMap(transition -> transition.actions().stream())
				.filter(QuestAction.GrantReward.class::isInstance)
				.map(QuestAction.GrantReward.class::cast)
				.filter(reward -> reward.rewardKind() == QuestRewardKind.ITEM && declared.contains(reward.id()))
				.map(QuestAction.GrantReward::id)
				.collect(Collectors.toSet());
			assertEquals(declared, concrete, "every selectable reward must have a concrete completion route: quest=" + questId);
		}
	}

	@Test
	void quest2002KeepsObjectDialogAndRewardPreviewOnSeparateActions() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		QuestDefinition definition = catalog.findExecutable(2002).orElseThrow().definition();

		List<QuestTransition> rewardRoutes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward"))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203516)
			.toList();
		assertEquals(1, rewardRoutes.stream().filter(transition -> dialogAction(transition) == -1).count());
		assertEquals(1, rewardRoutes.stream().filter(transition -> dialogAction(transition) == 31).count());
		assertEquals(1, rewardRoutes.stream().filter(transition -> dialogAction(transition) == 10007).count());
		assertEquals(1, rewardRoutes.stream().filter(transition -> dialogAction(transition) == 1009).count());

		QuestTransition objectDialog = rewardRoutes.stream()
			.filter(transition -> dialogAction(transition) == -1)
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(3398)), objectDialog.afterCommit());
		assertQuestPage(rewardRoutes, 31, 3398);
		assertPreviewPage(rewardRoutes, 10007);
		assertPreviewPage(rewardRoutes, 1009);
	}

	@Test
	void quest26930UsesTheSimpleItemCheckForCollectionTurnIn() {
		// P3 重锚（计划 §8.9）：26930 自 SimpleTalk 切换批起由 native 车道直驱，typed 定义退出生产视图；
		// 旧断言（overlay 里的 started→reward 边 + HasItem/RemoveItem 动作）属 IR 形状，改锚真端表行 +
		// quest.xml 收集通道 + 族级奖励窗页。
		// P3 re-anchor (plan §8.9): quest 26930 is native-lane driven since the SimpleTalk switch batch, so
		// the IR-shape assertions are replaced by the retail row, the quest.xml collect channel and the
		// family reward-window page.
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals(804627, handler.acquireNpc(26930), "真端 acquired_npc_name = LDF4_Advance_Ekthe_E");
		assertEquals(804627, handler.rewardNpc(26930), "真端 reward_npc_name = LDF4_Advance_Ekthe_E");
		assertEquals(0, handler.relayCount(26930), "26930 是单步行");
		assertTrue(handler.requireRow(26930).itemCheck(), "26930 真端行必须声明 item_check");
		// 交付门 = quest.xml collect_item1 声明的 186000257×10（整组门，不做子集放行）。
		// The hand-in gate is the whole quest.xml collect_item1 group, 186000257 x10.
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(186000257, 10)), handler.workItems(26930));
		assertFalse(handler.unresolvedGate(26930), "可解门不得 fail-closed");
		assertEquals(5, SimpleTalkHandler.PAGE_REWARD_WINDOW, "领取走族级奖励窗页 5");
	}

	@Test
	void emptyDuplicateAndMigrationAnnotatedCatalogsFailClosed() {
		assertEquals("INVALID_PRODUCTION_CATALOG", error("<quest-definition-catalog version=\"2\"/>").code());
		assertEquals("DUPLICATE_CATALOG_OWNER", error("<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"1\" resource=\"one.xml\" mode=\"EXECUTABLE\"/>"
			+ "<definition id=\"1\" resource=\"two.xml\" mode=\"METADATA_ONLY\"/>"
			+ "</quest-definition-catalog>").code());
		assertEquals("INVALID_PRODUCTION_CATALOG", error("<quest-definition-catalog version=\"2\" "
			+ "ownership=\"CURRENT\"><definition id=\"1\" resource=\"one.xml\" mode=\"EXECUTABLE\"/>"
			+ "</quest-definition-catalog>").code());
		assertEquals("INVALID_CATALOG_VERSION", error("<quest-definition-catalog version=\"1\">"
			+ "<definition id=\"1\" resource=\"one.xml\" mode=\"EXECUTABLE\"/>"
			+ "</quest-definition-catalog>").code());
	}

	@Test
	void missingResourceAndDefinitionIdMismatchFailClosed() {
		String missing = "<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"1\" resource=\"missing.xml\" mode=\"EXECUTABLE\"/></quest-definition-catalog>";
		assertEquals("CATALOG_RESOURCE_MISSING", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.compile(bytes(missing), getClass().getClassLoader())).code());

		String mismatch = "<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"2\" resource=\"quest-definition-fixtures/one.xml\" mode=\"EXECUTABLE\"/>"
			+ "</quest-definition-catalog>";
		assertEquals("CATALOG_ID_MISMATCH", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.compile(bytes(mismatch), getClass().getClassLoader())).code());
	}

	@Test
	void metadataOnlyEntriesExposeMetadataButNeverExecutionRoutes() {
		String manifest = "<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"1\" resource=\"quest-definition-fixtures/one.xml\" mode=\"EXECUTABLE\"/>"
			+ "<definition id=\"990002\" resource=\"quest-definition-fixtures/metadata-only.xml\" mode=\"METADATA_ONLY\"/>"
			+ "</quest-definition-catalog>";
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(bytes(manifest), getClass().getClassLoader());

		assertEquals(2, catalog.entries().size());
		assertEquals(List.of(1), catalog.executables().stream().map(CompiledQuestDefinition::id).toList());
		assertEquals(QuestCatalogEntryMode.METADATA_ONLY, catalog.findEntry(990002).orElseThrow().mode());
		assertEquals("metadata-only", catalog.findMetadata(990002).orElseThrow().name());
		assertTrue(catalog.findExecutable(990002).isEmpty());
	}

	@Test
	void externalGameDataDirectoryCompilesWithoutPackagedQuestResources() throws Exception {
		Path quests = Files.createDirectories(tempDirectory.resolve("quests"));
		copyResource("/aion/data/static_data/quest/definitions/quest_definition.xsd",
			tempDirectory.resolve("quest_definition.xsd"));
		copyResource("/aion/data/static_data/quest/definitions/quest_definition_catalog.xsd",
			tempDirectory.resolve("quest_definition_catalog.xsd"));
		copyResource("/quest-definition-fixtures/one.xml", quests.resolve("1.xml"));
		copyResource("/quest-definition-fixtures/metadata-only.xml", quests.resolve("990002.xml"));
		Files.writeString(tempDirectory.resolve("quest_definition_catalog.xml"), """
			<quest-definition-catalog version="2">
			  <definition id="1" resource="aion/data/static_data/quest/definitions/quests/1.xml" mode="EXECUTABLE"/>
			  <definition id="990002" resource="aion/data/static_data/quest/definitions/quests/990002.xml" mode="METADATA_ONLY"/>
			</quest-definition-catalog>
			""");

		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(tempDirectory);

		assertEquals(List.of(1), catalog.executables().stream().map(CompiledQuestDefinition::id).toList());
		assertEquals("metadata-only", catalog.findMetadata(990002).orElseThrow().name());
	}

	private void copyResource(String resource, Path target) throws Exception {
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			Files.copy(java.util.Objects.requireNonNull(input, resource), target);
		}
	}

	private static QuestCompilationException error(String xml) {
		return assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.load(bytes(xml)));
	}

	private static ByteArrayInputStream bytes(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	private static boolean isDialogResponse(AfterCommitAction action) {
		return action instanceof AfterCommitAction.ShowQuestDialog
			|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
			|| action instanceof AfterCommitAction.ShowDialogWindow
			|| action instanceof AfterCommitAction.CloseDialog;
	}

	private static int dialogAction(QuestTransition transition) {
		return ((QuestEvent.TalkToNpc) transition.event()).dialogId();
	}

	private static void assertPreviewPage(List<QuestTransition> routes, int action) {
		assertQuestPage(routes, action, 5);
	}

	private static void assertQuestPage(List<QuestTransition> routes, int action, int page) {
		QuestTransition route = routes.stream()
			.filter(transition -> dialogAction(transition) == action)
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page)), route.afterCommit());
	}

	private static void assertRepeatStartDialogs(QuestCatalog catalog) {
		int checked = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			if (definition.metadata().repeatPolicy().maxRepeatCount() <= 1) {
				continue;
			}
			Map<String, QuestStatus> statuses = new HashMap<>();
			for (QuestNode node : definition.nodes()) {
				statuses.put(node.label(), node.projection().status());
			}
			Set<Integer> startNpcs = definition.transitions().stream()
				.filter(transition -> statuses.get(transition.sourceNode()) == QuestStatus.NONE)
				.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.START)
				.filter(transition -> transition.conditions().contains(new QuestCondition.StartEligible()))
				.map(QuestTransition::event)
				.filter(QuestEvent.TalkToNpc.class::isInstance)
				.map(QuestEvent.TalkToNpc.class::cast)
				.filter(talk -> talk.dialogId() != null && (talk.dialogId() == 1002 || talk.dialogId() == 20000))
				.map(QuestEvent.TalkToNpc::npcId)
				.collect(Collectors.toSet());
			List<String> completeNodes = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.COMPLETE)
				.map(QuestNode::label).toList();
			for (QuestTransition opening : definition.transitions()) {
				if (statuses.get(opening.sourceNode()) != QuestStatus.NONE
						|| statuses.get(opening.targetNode()) != QuestStatus.NONE
						|| !(opening.event() instanceof QuestEvent.TalkToNpc talk)
						|| !startNpcs.contains(talk.npcId()) || talk.dialogId() == null
						|| !opening.actions().isEmpty()
						|| !opening.afterCommit().stream().allMatch(action ->
							action instanceof AfterCommitAction.ShowQuestDialog
								|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
								|| action instanceof AfterCommitAction.ShowDialogWindow
								|| action instanceof AfterCommitAction.CloseDialog)) {
					continue;
				}
				for (String complete : completeNodes) {
					checked++;
					assertTrue(definition.transitions().stream().anyMatch(transition ->
						complete.equals(transition.sourceNode())
							&& complete.equals(transition.targetNode())
							&& transition.event().equals(opening.event())
							&& transition.conditions().contains(new QuestCondition.StartEligible())
							&& transition.afterCommit().equals(opening.afterCommit())),
						"missing repeat dialog route: quest=" + compiled.id() + " source=" + complete
							+ " npc=" + talk.npcId() + " dialog=" + talk.dialogId());
				}
			}
		}
		assertTrue(checked > 0, "repeat dialog audit must inspect production routes");
	}
}
