package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 锁定任务 26800 的 Aion 5.8 客户端页面、跨地图阶段和最终领奖 owner。
 * 领奖态 packed step 由批次 28 按 QE-054 校正为 legacy 落盘值 2（客户端 quest_summary 末行索引），
 * 旧投影遗留的 REWARD/var0=3 由无 source 的 enter-world 自愈边纠正。
 * Locks quest 26800 to the Aion 5.8 client pages, cross-map stages, and final reward owner. Batch 28 pins
 * the reward step to the legacy-persisted 2 (QE-054) and repairs stale REWARD/var0=3 saves on enter-world.
 */
class Quest26800ClientDialogAlignmentTest {
	private static final int START_NPC = 806079;
	private static final int HANDOFF_NPC = 806233;
	private static final int REWARD_NPC = 806149;
	private static final int QUEST_20527_FRAGMENT = 731711;
	private static final int TOWER_PORTAL = 806082;
	private static final int ARCHIVES_PORTAL = 806029;
	private static final Path PORTAL_TEMPLATES = Path.of(
		"src/main/resources/aion/data/static_data/portals/portal_template2.xml");
	private static final Path PORTAL_LOCATIONS = Path.of(
		"src/main/resources/aion/data/static_data/portals/portal_loc.xml");

	@Test
	void acceptsThroughTheRetailAskWindow() throws Exception {
		QuestDefinition definition = definition(26800);

		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "s1", QuestStatus.START, 1);
		assertNode(definition, "s2", QuestStatus.START, 2);
		// legacy changeQuestStep(env, 2, 3, true) 只置 REWARD、step 停在 2，与客户端末行索引一致。
		assertNode(definition, "reward", QuestStatus.REWARD, 2);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		// 接取段 = 真端原生相位 A：QUEST_SELECT 自环下发**客户端任务页声明的入口页**（本任务页只有
		// select_none(4762)，没有 ask_quest_accept(4) ⇒ 发 4762），客户端原生控件 1002/20000 回传建档；
		// 旧形的 SELECT_NONE_1 续页（动作 4763）与 ASK_QUEST_ACCEPT(1007) 中转一律无路由。
		// The accept segment is native lifecycle phase A: the QUEST_SELECT self-loop emits the page the
		// client task HTML declares (this page declares select_none(4762) but no ask_quest_accept(4), so
		// 4762 is emitted) and the native controls 1002/20000 commit; the SELECT_NONE_1 continuation and
		// the ASK_QUEST_ACCEPT(1007) hop carry no route.
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT,
			ClientAcceptEntryPageAssertions.expectedEntryPage(26800));
		List<Integer> retiredAcceptActions = List.of(QuestDialogPage.SELECT_NONE_1.id(),
			QuestDialogAction.ASK_QUEST_ACCEPT.id());
		assertTrue(routes(definition, "unaccepted", START_NPC).stream()
			.noneMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && retiredAcceptActions.contains(talk.dialogId())),
			"quest 26800 owns no select_none letter chain after the canonical accept");

		QuestTransition askWindowCommit = talk(definition, "unaccepted", "started", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals(List.of(new QuestCondition.StartEligible()), askWindowCommit.conditions());
		assertEquals(List.of(), askWindowCommit.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			askWindowCommit.afterCommit());

		QuestTransition accept = talk(definition, "unaccepted", "started", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
	}

	@Test
	void advancesThroughTheTowerAndEnfitentaOnlyAtTheAuthoritativeStages() throws Exception {
		QuestDefinition definition = definition(26800);

		QuestTransition towerArrival = transition(definition, "started", "s1",
			new QuestEvent.EnterZone("DF_TOWER_SENSORY_AREA_Q26800_220120000"));
		// 真端把三段行门写进节点投影（引擎按投影匹配源行），条件下不再有显式 var0 断言。
		// The retail ladder writes the row guards into the node projections (the engine matches source
		// rows through them), so the conditions carry no explicit var0 check.
		assertEquals(Map.of("var0", 0), nodeVariables(definition, "started"));
		assertEquals(List.of(), towerArrival.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), towerArrival.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			towerArrival.afterCommit());

		// 客户端链登记（quest_client_talk_chain_pages.tsv）给 26800 第 1 行只登记单页
		// `talk:1352:10255`：事件页 SELECT2(1352) 同时是推进页，遗留 XML 多出的 SELECT2_1(1353)
		// 路由随退役退场（16800 同形）。
		// The client chain registry declares a single page for 26800's row 1 (`talk:1352:10255`): SELECT2
		// (1352) is both the event and the advance page, so the extra SELECT2_1 (1353) route of the legacy
		// XML retired with it (the 16800 shape).
		assertPage(definition, "s1", HANDOFF_NPC, QuestDialogAction.SELECT2,
			QuestDialogPage.SELECT2);
		assertTrue(routes(definition, "s1", HANDOFF_NPC).stream()
			.noneMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogPage.SELECT2_1.id()),
			"quest 26800 row 1 owns no SELECT2_1 page route");

		QuestTransition handoff = talk(definition, "s1", "s2", HANDOFF_NPC,
			QuestDialogAction.SET_SUCCEED);
		assertEquals(Map.of("var0", 1), nodeVariables(definition, "s1"));
		assertEquals(List.of(), handoff.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), handoff.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), handoff.afterCommit());
	}

	@Test
	void entersRewardBeforeMovieAndKeepsFereganAsTheOnlyRewardOwner() throws Exception {
		QuestDefinition definition = definition(26800);

		QuestTransition archivesArrival = transition(definition, "s2", "reward",
			new QuestEvent.EnterZone("IDETERNITY_01_Q16800_301540000"));
		assertEquals(Map.of("var0", 2), nodeVariables(definition, "s2"));
		assertEquals(List.of(), archivesArrival.conditions());
		// 真端显式回写 target 行（= reward 投影 var0=2），packed step 仍是 legacy 落盘值 2。
		// The retail edge writes the target row back (= the reward projection var0=2); the packed step
		// still lands on the legacy persisted value 2.
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), archivesArrival.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.PlayMovie(932)), archivesArrival.afterCommit());

		assertPage(definition, "reward", REWARD_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		QuestTransition preview = talk(definition, "reward", "reward", REWARD_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", REWARD_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(), completion.conditions());
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 155160, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 8868125, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());

		assertTrue(routes(definition, "reward", START_NPC).isEmpty());
		assertTrue(routes(definition, "reward", HANDOFF_NPC).isEmpty());
	}

	@Test
	void keepsTheQuest20527FragmentSeparateFromTheTwoWorldPortals() throws Exception {
		QuestDefinition quest26800 = definition(26800);
		QuestDefinition quest20527 = definition(20527);

		assertFalse(hasNpcEvent(quest26800, QUEST_20527_FRAGMENT));
		assertTrue(quest20527.transitions().stream()
			.anyMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(QUEST_20527_FRAGMENT, QuestDialogAction.USE_OBJECT.id()))));

		Document templates = document(PORTAL_TEMPLATES);
		Element tower = element(templates,
			"/portal_templates2/portal_dialog[@npc_id='" + TOWER_PORTAL + "']/portal_path");
		assertEquals("104", tower.getAttribute("dialog"));
		assertEquals("2201200", tower.getAttribute("loc_id"));

		Element archives = element(templates,
			"/portal_templates2/portal_dialog[@npc_id='" + ARCHIVES_PORTAL + "']/portal_path");
		assertEquals("10000", archives.getAttribute("dialog"));
		assertEquals("3015400", archives.getAttribute("loc_id"));
		assertEquals("true", archives.getAttribute("instance"));
		assertEquals("ASMODIANS", archives.getAttribute("race"));

		Document locations = document(PORTAL_LOCATIONS);
		assertEquals("220120000", element(locations,
			"/portal_locs/portal_loc[@loc_id='2201200']").getAttribute("world_id"));
		assertEquals("301540000", element(locations,
			"/portal_locs/portal_loc[@loc_id='3015400']").getAttribute("world_id"));
	}

	private static boolean hasNpcEvent(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition -> switch (transition.event()) {
			case QuestEvent.TalkToNpc talk -> talk.npcId() == npcId;
			case QuestEvent.CanAct canAct -> canAct.templateId() == npcId;
			default -> false;
		});
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertPage(definition, source, npcId, action, page.id());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, int pageId) {
		QuestTransition transition = talk(definition, source, source, npcId, action);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 26800 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(new NodeProjection(status, Map.of("var0", var0)), node.projection());
	}

	private static Document document(Path path) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(path.toFile());
	}

	private static Element element(Document document, String expression) throws Exception {
		NodeList matches = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
			expression, document, XPathConstants.NODESET);
		assertEquals(1, matches.getLength(), expression);
		return (Element) matches.item(0);
	}

	/** 节点投影（引擎按它匹配源行——显式 var0 条件在真端形里由投影承担）。 / Node projection. */
	private static Map<String, Integer> nodeVariables(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow()
			.projection().variables();
	}

	private static QuestDefinition definition(int questId) throws Exception {
		// 26800 已由真端驱动退役（wave4 enterarea 区名解析）：退役任务的 XML 只在 git 历史里，
		// 统一取生产视图（XML 目录 + 真端 overlay）——未退役任务与直接编译 XML 等价（20527 仍走 XML）。
		// 26800 is retail-driven now (wave4 enterarea zone resolution), so its XML lives only in git
		// history; the production view (XML directory plus retail overlay) is the single source (20527
		// still compiles from its XML).
		return ProductionQuestDefinitions.definition(questId).definition();
	}
}
