package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 30：真端“过场/影片播放用隐藏任务”族的服务端边界。
 * <p>
 * 真端 58Server `Map/XML/quest.xml` 的 `dev_name` 直接点名了这批任务的用途：
 * `18744/28744` = 「타메스 컷신 재생용(천)」（泰梅斯过场播放用），`16984/26984` =
 * 「룬의 안식처 컷신 재생용 히든 퀘스트 (천)」（符文圣所过场播放用隐藏任务），`20015` =
 * 「[5.5 업데이트 인트로 영상 재생용 히든 퀘스트] …」。它们的客户端 quest_summary 固定渲染若干空 `<step>` 槽（18744/28744 为 4 槽，step0 只挂 `[%collectitem]` 占位），
 * 因此服务端实现只能是“进入世界 → 播放过场 → 过场结束即完成”，不得按行号口径补任务书行。
 * <p>
 * 本批补齐了证据链完整的 `18744/28744`（拉科兰遗迹 300610000：等级 >= 60 + 阵营 + 未完成自动接取，
 * 播放过场 912 = 客户端 CutScenes.xml 的 CS_ID_132，过场结束即完成）。过场 912 属于 CutScenes.xml
 * （`cs_id_132.seq`，文本为拉科兰遗迹开场：精神支配实验 / 三岔路），而 CutSceneMovies.xml 只有 id 1..37，
 * 所以包类型必须是 {@code CUTSCENE}(0)；迁移前 handler 写的 `SM_PLAY_MOVIE(1, 912)` 落在影片表之外，
 * 不能照抄。其余 8 个（16984/26984 无过场 id 与触发世界证据、20015 无行为证据、18706/28706 是客户端
 * 999 级占位、3959/4963 是真端前置被取消的禁用占位、29706 在客户端 quest.xml 与真端 quest.xml 中都不存在）
 * 保持隔离，仅登记证据（审计脚本 CUTSCENE_HIDDEN_QUEST_REGISTRY 与 docs/QUEST_CATALOG 系列文档）。
 * <p>
 * Batch 30 contract for the retail "cutscene playback hidden quest" family: the journal has blank <step> slots, so
 * the definition is enter-world + play cutscene + complete on movie end, with no reward row. 18744/28744 are the
 * evidence-complete pair (Raksang Ruins 300610000, level 60 gate, race, cutscene 912 from CutScenes.xml → packet
 * type CUTSCENE); the remaining eight stay isolated until their own evidence exists.
 */
class CutsceneHiddenQuestFamilyContractTest {

	private static final Path DEFINITION_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition");
	private static final Path QUEST_DIRECTORY = DEFINITION_DIRECTORY.resolve("quests");
	private static final int RAKSANG_RUINS = 300610000;
	private static final int RAKSANG_INTRO_CUTSCENE = 912;
	private static final Map<Integer, String> RAKSANG_INTRO_QUESTS = Map.of(
		18744, "ELYOS",
		28744, "ASMODIANS");
	private static final List<Integer> METADATA_ONLY_CUTSCENE_QUESTS = List.of(16984, 26984);
	/* 客户端 quest.xml 缺失（29706）或真端 quest.xml 明确禁用/999 占位（其余）的 id：
	 * 不得出现在生产目录里，也不得被重新打包。 / Must stay unregistered and unpackaged. */
	private static final List<Integer> ISOLATED_CLIENT_ONLY_QUESTS = List.of(3959, 4963, 18706, 28706, 20015, 29706);

	@Test
	void raksangIntroHiddenQuestsAutoStartOnEnterWorldAndFinishOnTheCutscene() throws Exception {
		for (Map.Entry<Integer, String> entry : RAKSANG_INTRO_QUESTS.entrySet()) {
			int questId = entry.getKey();
			QuestDefinition definition = definition(questId).definition();
			assertEquals(60, definition.metadata().minLevel(), () -> "quest " + questId + " minimum level");
			assertEquals(Integer.MAX_VALUE, definition.metadata().maxLevel(),
				() -> "quest " + questId + " is uncapped in retail (maxlevel_permitted=0)");
			assertEquals(Set.of(entry.getValue()), definition.metadata().permittedRaces(),
				() -> "quest " + questId + " race gate");
			assertTrue(definition.metadata().rewards().isEmpty(),
				() -> "quest " + questId + " grants nothing in retail (reward_exp1=0 / reward_gold1=0)");
			assertEquals(Set.of("unaccepted", "started", "complete"),
				definition.nodes().stream().map(QuestNode::label).collect(Collectors.toSet()),
				() -> "quest " + questId + " keeps the blank-journal ladder");
			assertTrue(definition.nodes().stream()
					.noneMatch(node -> node.projection().status() == QuestStatus.REWARD),
				() -> "quest " + questId + " has no reward row to light up");

			List<QuestTransition> accepts = routes(definition, "unaccepted", "started");
			assertEquals(1, accepts.size(), () -> "quest " + questId + " auto-start route");
			QuestTransition accept = accepts.getFirst();
			assertEquals(new QuestEvent.EnterWorld(), accept.event(),
				() -> "quest " + questId + " starts on entering the instance world");
			assertTrue(accept.conditions().contains(new QuestCondition.WorldIs(RAKSANG_RUINS, true)),
				() -> "quest " + questId + " is gated to Raksang Ruins");
			assertTrue(accept.conditions().contains(new QuestCondition.StartEligible()),
				() -> "quest " + questId + " authorises the metadata start gate (level 60 + race + not completed)");
			assertTrue(accept.afterCommit().contains(
					new AfterCommitAction.PlayMovie(RAKSANG_INTRO_CUTSCENE, QuestMovieType.CUTSCENE)),
				() -> "quest " + questId + " plays cutscene 912 through the CutScenes packet family");
			assertTrue(accept.afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
				() -> "quest " + questId + " refreshes visibility after the auto-start");

			List<QuestTransition> replays = routes(definition, "started", "started");
			assertEquals(1, replays.size(), () -> "quest " + questId + " replay route");
			assertTrue(replays.getFirst().conditions().contains(new QuestCondition.WorldIs(RAKSANG_RUINS, true)),
				() -> "quest " + questId + " replays only inside Raksang Ruins");
			assertTrue(replays.getFirst().afterCommit().contains(
					new AfterCommitAction.PlayMovie(RAKSANG_INTRO_CUTSCENE, QuestMovieType.CUTSCENE)),
				() -> "quest " + questId + " replays cutscene 912");
			assertTrue(replays.getFirst().afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				() -> "quest " + questId + " replay stays packet-only");

			List<QuestTransition> finishes = routes(definition, "started", "complete");
			assertEquals(1, finishes.size(), () -> "quest " + questId + " completion route");
			assertEquals(new QuestEvent.MovieEnd(RAKSANG_INTRO_CUTSCENE), finishes.getFirst().event(),
				() -> "quest " + questId + " completes when the intro cutscene ends");
			assertTrue(finishes.getFirst().actions().contains(new QuestAction.CompleteQuest(0)),
				() -> "quest " + questId + " completes without a reward row");
			assertTrue(finishes.getFirst().afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION)),
				() -> "quest " + questId + " notifies completion");
		}
	}

	@Test
	void hiddenCutsceneQuestsMustNotGrowJournalRowsOrDialogRoutes() throws Exception {
		for (int questId : RAKSANG_INTRO_QUESTS.keySet()) {
			QuestDefinition definition = definition(questId).definition();
			assertTrue(definition.transitions().stream()
					.noneMatch(route -> route.event() instanceof QuestEvent.TalkToNpc),
				() -> "quest " + questId + " is cutscene-driven and owns no dialog route");
			assertTrue(definition.nodes().stream().noneMatch(node -> node.label().matches("s\\d+")),
				() -> "quest " + questId + " must not grow journal row nodes");
		}
	}

	@Test
	void metadataOnlyCutsceneQuestsStayStaticUntilTheirOwnEvidenceExists() throws Exception {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(DEFINITION_DIRECTORY);
		for (int questId : METADATA_ONLY_CUTSCENE_QUESTS) {
			QuestCatalogEntry entry = catalog.findEntry(questId)
				.orElseThrow(() -> new AssertionError("missing catalog entry for " + questId));
			assertEquals(QuestCatalogEntryMode.METADATA_ONLY, entry.mode(),
				() -> "quest " + questId + " must stay metadata-only until its cutscene id and trigger world are proven");
			QuestDefinition definition = metadataOnlyDefinition(questId);
			assertTrue(definition.nodes().isEmpty(), () -> "quest " + questId + " keeps no nodes");
			assertTrue(definition.transitions().isEmpty(), () -> "quest " + questId + " keeps no routes");
		}
	}

	@Test
	void isolatedClientOnlyQuestsStayOutOfTheProductionCatalog() throws Exception {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(DEFINITION_DIRECTORY);
		for (int questId : ISOLATED_CLIENT_ONLY_QUESTS) {
			assertTrue(catalog.findEntry(questId).isEmpty(),
				() -> "isolated client-only quest " + questId + " must not be registered");
			Path definition = QUEST_DIRECTORY.resolve(questId + ".xml");
			assertFalse(Files.exists(definition),
				() -> "isolated client-only quest definition remains packaged: " + definition);
		}
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> target.equals(route.targetNode()))
			.toList();
	}

	/* 仅元数据条目没有节点，必须走不要求可执行节点的解析入口。 */
	/* Metadata-only entries carry no nodes, so they parse through the metadata-only entry point. */
	private static QuestDefinition metadataOnlyDefinition(int questId) throws IOException {
		try (InputStream input = CutsceneHiddenQuestFamilyContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.parse(input);
		}
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = CutsceneHiddenQuestFamilyContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
