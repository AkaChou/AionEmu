package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.QuestXmlFixtures;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleTalk 真端驱动门禁（M3-b 起）。判据是 <b>真端语义</b>，不是与历史 quest-definition XML 的 IR 等价：
 * 2026-09-23 口径确认 XML 自身带大量历史错误（例如真端没有进度域的单步任务被写成 REWARD/var0=1，
 * 而客户端任务书只有 1 行），因此 XML 只能在"真端无法表达"时保留，不能当审计金标准。
 * <p>
 * 本门禁断言三件事：
 * <ol>
 * <li><b>家族规模冻结</b>：真端 {@code Quest_SimpleTalk.xml} ∩ 生产宇宙（catalog ∪ 退役 fixture）= 2223；</li>
 * <li><b>可驱动集合的语义不变量</b>：单一步长 0 投影、接取落在 {@code acquired_npc_name}、
 * 交付落在 {@code reward_npc_name}（规范形三态：空门 / {@code collect_item*} 整组门 / 工作物品门）、
 * 完成分支齐备；交付门控的条件与动作必须与真端轴一致，且不得残留旧页链路由
 * （SELECT5 报告页、39/20002 检查对、prio-1 回页、select6 失败页）；可驱动数量不许回退；</li>
 * <li><b>过场不变量</b>：已受理的过场单步行定义内 {@code PlayMovie} 恰好 1 个且 movieId 等于真端
 * {@code cutsceneid1}（canonical 形没有 1007/1009 路由，重挂失配会静默丢电影且不改变受理计数）；</li>
 * <li><b>漂移登记</b>：与历史 XML 的逐任务分类（等价 / 节点投影差 / 路由差 / 稳定拒绝码）必须与
 * {@code retail-simple-talk-drift.tsv} 一致——登记用于发现静默漂移和追溯历史 XML 缺陷，
 * 不是退役门槛（退役门槛 = 保留清单 owner=RETAIL_TABLE + 本门禁的语义不变量）。</li>
 * </ol>
 * Retail-semantics gate for the SimpleTalk family; XML equivalence is a drift registry, not the criterion.
 * The invariants cover the canonical delivery gate shapes, the retirement of the page-chain routes, and
 * a per-row cutscene invariant (exactly one PlayMovie carrying the retail cutsceneid1).
 * 排查模式：{@code -Dretail.talk.equivOut=<path>} 导出逐任务分类。
 */
class RetailSimpleTalkGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String CATALOG = "/aion/data/static_data/quest/definitions/quest_definition_catalog.xml";
	/** 与历史 XML 的漂移登记（quest_id, classification）。 / Drift registry versus the legacy XML. */
	private static final String DRIFT_REGISTRY = "/quest/retail-simple-talk-drift.tsv";
	/** 冻结的家族规模（真端表 ∩ 生产宇宙）。 / Frozen family size. */
	private static final int FROZEN_FAMILY_SIZE = 2223;
	/** 可驱动（accepted）数量下限，防止静默回退（1598 + P0c-2 系统发放批 40）。 */
	/** Floor for retail-drivable quests; raised by the P0c-2 system-grant batch. */
	private static final int ACCEPTED_FLOOR = 1638;
	/**
	 * 过场行覆盖下限：真端 ADOPT 集 13 行（28 行 craft KEEP_XML 行的 canonical 形状同轴，实测受理 41）。
	 * Floor for cutscene-row coverage; the frozen ADOPT set is 13 rows.
	 */
	private static final int CUTSCENE_ROW_FLOOR = 13;
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailSimpleTalkTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> familyIds;
	private static Map<Integer, String> driftRegistry;
	private static RetailClientDialogExits clientDialogExits;
	private static RetailClientSummaryRows clientSummaryRows;
	private static RetailClientRewardNpcs clientRewardNpcs;
	private static RetailQuestUseItemNpcs interactionObjects;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest/retail/Quest_SimpleTalk.xml")) {
			table = RetailSimpleTalkTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest/retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		clientDialogExits = RetailClientDialogExits.defaultExits();
		clientSummaryRows = RetailClientSummaryRows.defaultSummaryRows();
		clientRewardNpcs = RetailClientRewardNpcs.defaultRewardNpcs();
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll("/aion/data/static_data/items/item/",
			listXmlNames("/aion/data/static_data/items/item/")));
		interactionObjects = RetailQuestUseItemNpcs.fromIds(npcIndex.questUseItemNpcIds());
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		familyIds = familyIds();
		driftRegistry = driftRegistry();
	}

	@Test
	void familyScopeIsFrozen() {
		assertTrue(familyIds.size() == FROZEN_FAMILY_SIZE,
			() -> "SimpleTalk 家族规模漂移：期望 " + FROZEN_FAMILY_SIZE + " 实际 " + familyIds.size());
	}

	/** 可驱动集合必须满足真端语义（与 XML 是否等价无关）。 / Retail semantics of every drivable row. */
	@Test
	void acceptedDefinitionsCarryRetailSemantics() {
		List<String> problems = new ArrayList<>();
		int accepted = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				continue;
			}
			accepted++;
			inspect(questId, row, problems);
		}
		int drivable = accepted;
		assertTrue(problems.isEmpty(), () -> "SimpleTalk 真端语义缺口："
			+ problems.stream().limit(20).toList() + " (accepted=" + drivable + ")");
		assertTrue(drivable >= ACCEPTED_FLOOR, () -> "SimpleTalk 可驱动数量回退："
			+ drivable + " < " + ACCEPTED_FLOOR);
	}

	/**
	 * S1 过场不变量（本片唯一语义新增点的守卫）：canonical 形没有 1007/1009 路由，过场重挂一旦失配
	 * 会静默丢电影（旧 {@code attachMovie} 零匹配时原样返回），而受理计数与形状断言都不会变。
	 * <p>
	 * 逐行断言：已受理且声明 {@code cutsceneid1} 且 {@code cs1_haction} ∈ {1007, 1009} 的<b>单步行</b>，
	 * 定义内 {@code PlayMovie} 恰好 1 个、movieId = 真端 {@code cutsceneid1}、类型 = {@code CUTSCENE}。
	 * <b>禁止</b>改写成"全家族电影总数守恒"——别的行会补数，那种弱断言拦不住单行静默丢失。
	 * 链面过场统一由规范模型驱动，逐行守护（触发轴决定落点：1007/1009 重挂到规范段 QUEST_SELECT 边，
	 * 中转页触发留在阶段同号动作边）。
	 * Per-row cutscene invariant for accepted single-step rows: exactly one PlayMovie carrying the retail
	 * cutsceneid1 with the CUTSCENE type. A whole-family movie total would be a weak assertion because
	 * other rows can compensate for a silently dropped one; chain-row movies are covered per-row by the
	 * chain gate (S2).
	 */
	@Test
	void acceptedCutsceneRowsCarryExactlyOneRetailMovie() {
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted() || !row.entry().cutscene()) {
				continue;
			}
			int trigger = row.entry().cutsceneTrigger();
			if (trigger != QuestDialogAction.SELECT_QUEST_REWARD.id()
					&& trigger != QuestDialogAction.ASK_QUEST_ACCEPT.id()
					&& trigger != 1353 && trigger != 1694) {
				continue;
			}
			checked++;
			List<QuestTransition> carriers = row.outcome().definition().definition().transitions().stream()
				.filter(transition -> transition.afterCommit().stream()
					.anyMatch(action -> action instanceof AfterCommitAction.PlayMovie))
				.toList();
			List<AfterCommitAction.PlayMovie> movies = carriers.stream()
				.flatMap(transition -> transition.afterCommit().stream())
				.filter(action -> action instanceof AfterCommitAction.PlayMovie)
				.map(action -> (AfterCommitAction.PlayMovie) action)
				.toList();
			if (movies.size() != 1) {
				problems.add(questId + ": PlayMovie 数 " + movies.size() + "（过场重挂失配，期望 1）");
				continue;
			}
			// 落点守卫：重挂边必须是 QUEST_SELECT（接取窗页 4 / 交付窗页），页链按钮不再承载过场。
			// Landing guard: the re-hung edge must be QUEST_SELECT; page-chain buttons no longer carry it.
			if (trigger == QuestDialogAction.SELECT_QUEST_REWARD.id()
					|| trigger == QuestDialogAction.ASK_QUEST_ACCEPT.id()) {
				if (!(carriers.get(0).event() instanceof QuestEvent.TalkToNpc carrier)
						|| !isAction(carrier, QuestDialogAction.QUEST_SELECT)) {
					problems.add(questId + ": 过场未挂在 QUEST_SELECT 边上（" + carriers.get(0).event() + "）");
				}
			}
			AfterCommitAction.PlayMovie movie = movies.get(0);
			if (movie.movieId() != row.entry().cutsceneMovieId()) {
				problems.add(questId + ": 过场 movieId=" + movie.movieId()
					+ " != 真端 cutsceneid1=" + row.entry().cutsceneMovieId());
			}
			if (movie.type() != QuestMovieType.CUTSCENE) {
				problems.add(questId + ": 过场影片类型 " + movie.type() + " != CUTSCENE");
			}
		}
		int covered = checked;
		assertTrue(problems.isEmpty(), () -> "SimpleTalk 过场不变量缺口："
			+ problems.stream().limit(20).toList() + " (checked=" + covered + ")");
		assertTrue(covered >= CUTSCENE_ROW_FLOOR, () -> "SimpleTalk 过场行覆盖回退：" + covered
			+ " < " + CUTSCENE_ROW_FLOOR + "（谓词失配 = 不变量空转）");
	}

	/**
	 * 与历史 XML 的差异逐条登记（登记是护栏，不是退役门槛）。
	 * <p>
	 * 已退役任务（XML 只在 git 历史里）不可再重算，登记行即冻结证据：只核对"有登记"；
	 * 仍由 XML 拥有的任务照旧逐条现算对拍。
	 * Retired quests cannot be re-classified (their XML lives in git history), so the registry row is
	 * the frozen evidence; XML-owned quests are still re-derived and compared row by row.
	 */
	@Test
	void driftVersusLegacyXmlIsRegistered() throws Exception {
		Map<Integer, String> classification = classify();
		List<String> problems = new ArrayList<>();
		for (int questId : new TreeSet<>(RetiredQuestIds.all())) {
			if (familyIds.contains(questId) && !driftRegistry.containsKey(questId)) {
				problems.add(questId + ": 已退役但没有漂移登记行（冻结证据缺失）");
			}
		}
		for (int questId : new TreeSet<>(driftRegistry.keySet())) {
			if (RetiredQuestIds.contains(questId)) {
				continue;
			}
			String expected = driftRegistry.get(questId);
			String actual = classification.get(questId);
			if (!expected.equals(actual)) {
				problems.add(questId + ": 登记=" + expected + " 实际=" + actual);
			}
		}
		for (int questId : new TreeSet<>(classification.keySet())) {
			if (!driftRegistry.containsKey(questId)) {
				problems.add(questId + ": 未登记（" + classification.get(questId) + "）");
			}
		}
		String dump = System.getProperty("retail.talk.equivOut");
		if (dump != null) {
			Files.writeString(Path.of(dump), dumpText(classification));
		}
		assertTrue(driftRegistry.size() == FROZEN_FAMILY_SIZE,
			() -> "漂移登记行数漂移：" + driftRegistry.size());
		assertTrue(problems.isEmpty(), () -> "SimpleTalk 漂移登记失同步："
			+ problems.stream().limit(20).toList());
	}

	// ------------------------------------------------------------------ 语义不变量

	private static void inspect(int questId, RetailRow row, List<String> problems) {
		if (!row.entry().singleStep()) {
			inspectCanonicalChain(questId, row, problems);
			return;
		}
		QuestDefinition definition = row.outcome().definition().definition();
		if (definition.progressLayout().fields().size() != 1
			|| !"var0".equals(definition.progressLayout().fields().get(0).name())) {
			problems.add(questId + ": 进度域应为单一 var0，实际 " + definition.progressLayout().fields());
		}
		int clientRows = clientSummaryRows.rows(questId);
		if (clientRows <= 0) {
			problems.add(questId + ": 客户端任务书行数未登记（REWARD 投影无来源）");
		}
		Set<QuestStatus> statuses = new LinkedHashSet<>();
		for (QuestNode node : definition.nodes()) {
			statuses.add(node.projection().status());
			Integer var0 = node.projection().variables().get("var0");
			// 领奖投影必须等于客户端任务书末行行号（QE-051）；其余节点留在行 0。
			// The reward projection must equal the last client journal row index.
			int expected = node.projection().status() == QuestStatus.REWARD
				? clientSummaryRows.lastRowIndex(questId) : 0;
			if (var0 == null || var0.intValue() != expected) {
				problems.add(questId + ": 节点 " + node.label() + " 投影 var0=" + var0
					+ "，期望 " + expected + "（客户端行数 " + clientRows + "）");
			}
		}
		if (!statuses.equals(Set.of(QuestStatus.NONE, QuestStatus.START, QuestStatus.REWARD,
			QuestStatus.COMPLETE))) {
			problems.add(questId + ": 节点状态集合异常 " + statuses);
		}
		if (row.entry().grantKind() == RetailGrantKind.CHALLENGE_TASK) {
			// P0c-12：挑战任务哨兵走对话接取（受理 = 交付 shugo 本人，判例 17100），非系统发放形状。
			// P0c-12: challenge-task sentinels take the dialog-accept shape at the hand-in shugo.
			if (!hasAccept(definition, row.acquiredNpc())) {
				problems.add(questId + ": 缺接取路由 npc=" + row.acquiredNpc());
			}
		} else if (row.entry().grantKind().systemGrant()) {
			// 系统发放形状（P0c-2）：真端没有 NPC 接取（客户端只有委托书页），定义不得出现接取/续页路由，
			// 但必须有一条 SystemGrant 边，否则发放子系统分配后无法发放。
			// System-grant shape: accept routes are forbidden, the SystemGrant edge is mandatory.
			if (hasAnyAcceptRoute(definition)) {
				problems.add(questId + ": 系统发放行不得有接取路由");
			}
			if (!hasSystemGrant(definition)) {
				problems.add(questId + ": 系统发放行缺 SystemGrant 边");
			}
		} else {
			if (!hasAccept(definition, row.acquiredNpc())) {
				problems.add(questId + ": 缺接取路由 npc=" + row.acquiredNpc());
			}
			// S1：SELECT1_1/SELECT1_1_1 续页路由随页链退场——canonical 接取窗（页 4）直发，
			// 客户端出口登记表的续页轴不再由服务端驱动（与采集族同口径）。
			// S1: the SELECT1_1 continuation retires with the page chain; the canonical ask window
			// (page 4) is direct, so the client exit registry no longer drives a continuation route.
		}
		for (int rewardNpc : row.rewardNpcs()) {
			if (!hasCanonicalDelivery(definition, rewardNpc, row, problems)) {
				problems.add(questId + ": 交付路由不合规范形 npc=" + rewardNpc
					+ " itemCheck=" + row.entry().itemCheck());
			}
			if (!hasComplete(definition, rewardNpc)) {
				problems.add(questId + ": 缺完成分支 npc=" + rewardNpc);
			}
			boolean systemGrantShape = row.entry().grantKind().systemGrant()
				&& row.entry().grantKind() != RetailGrantKind.CHALLENGE_TASK;
			if (systemGrantShape && !hasReportNpcExit(definition, rewardNpc)) {
				problems.add(questId + ": 系统发放行缺报告 NPC 关窗出口 npc=" + rewardNpc);
			}
		}
		if (row.rewardNpcs().isEmpty()) {
			problems.add(questId + ": 无交付 NPC 集");
		}
	}

	private static void inspectCanonicalChain(int questId, RetailRow row, List<String> problems) {
		QuestDefinition definition = row.outcome().definition().definition();
		if (definition.progressLayout().fields().size() != 1
			|| !"var0".equals(definition.progressLayout().fields().get(0).name())) {
			problems.add(questId + ": 进度域应为单一 var0，实际 " + definition.progressLayout().fields());
		}
		int clientRows = clientSummaryRows.rows(questId);
		if (clientRows <= 0) {
			problems.add(questId + ": 客户端任务书行数未登记（REWARD 投影无来源）");
		}
		Set<QuestStatus> statuses = new LinkedHashSet<>();
		int m = row.entry().talkNpcs().size();
		int expectedRewardRow = clientSummaryRows.rows(questId) > 0
			? clientSummaryRows.lastRowIndex(questId) : m;
		for (QuestNode node : definition.nodes()) {
			statuses.add(node.projection().status());
			Integer var0 = node.projection().variables().get("var0");
			int expected = switch (node.label()) {
				case "unaccepted", "started", "complete" -> 0;
				case "reward" -> expectedRewardRow;
				default -> {
					if (node.label().startsWith("step")) {
						yield Integer.parseInt(node.label().substring("step".length()));
					}
					yield -1;
				}
			};
			if (var0 == null || var0.intValue() != expected) {
				problems.add(questId + ": 节点 " + node.label() + " 投影 var0=" + var0
					+ "，期望 " + expected);
			}
		}
		if (!statuses.equals(Set.of(QuestStatus.NONE, QuestStatus.START, QuestStatus.REWARD,
			QuestStatus.COMPLETE))) {
			problems.add(questId + ": 节点状态集合异常 " + statuses);
		}
		if (row.entry().grantKind().systemGrant()) {
			if (!hasSystemGrant(definition)) {
				problems.add(questId + ": 系统发放行缺 SystemGrant 边");
			}
		} else {
			if (!hasAccept(definition, row.acquiredNpc())) {
				problems.add(questId + ": 缺接取路由 npc=" + row.acquiredNpc());
			}
		}
		for (int rewardNpc : row.rewardNpcs()) {
			if (!hasCanonicalDeliveryFromStep(definition, rewardNpc, "step" + m)) {
				problems.add(questId + ": 交付路由不合规范形 npc=" + rewardNpc);
			}
			if (!hasComplete(definition, rewardNpc)) {
				problems.add(questId + ": 缺完成分支 npc=" + rewardNpc);
			}
		}
	}

	private static boolean hasCanonicalDeliveryFromStep(QuestDefinition definition, int npcId, String sourceNode) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.QUEST_SELECT)
				&& sourceNode.equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode()));
	}

	/** 任一接取/续页路由（系统发放行都不允许有）。 / Any accept or continuation route. */
	private static boolean hasAnyAcceptRoute(QuestDefinition definition) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id()
					|| talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()
					|| talk.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					|| talk.dialogId() == QuestDialogAction.SELECT1_1.id()
					|| talk.dialogId() == QuestDialogAction.SELECT1_1_1.id())
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START);
	}

	/** 系统发放边（NONE → START，带 StartEligible 条件）。 / The mandatory system-grant edge. */
	private static boolean hasSystemGrant(QuestDefinition definition) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.SystemGrant
				&& transition.conditions().contains(new QuestCondition.StartEligible())
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START);
	}

	/** 报告 NPC 的关窗出口（交付检查失败页的按钮）。 / Close-dialog exit of the report NPC. */
	private static boolean hasReportNpcExit(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.FINISH_DIALOG)
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START
				&& transition.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	private static boolean hasAccept(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& (isAction(talk, QuestDialogAction.QUEST_ACCEPT_1)
					|| isAction(talk, QuestDialogAction.QUEST_ACCEPT_SIMPLE))
				&& transition.conditions().contains(new QuestCondition.StartEligible())
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START);
	}

	/**
	 * 规范形交付三态（S1，与采集族同口径）：{@code QUEST_SELECT}(started→reward) 带门直翻领奖并下发
	 * 档位奖励窗——空门（无 {@code item_check}）、collect 整组门（真端 {@code collect_item*}）、
	 * 工作物品门（{@code item_check} 无 collect_item，门 = 接取发放的 {@code give_item} 同物）。
	 * 报告页（SELECT5）、39/20002 检查对、prio-1 回页与 select6 失败页整类退场：未集齐时零路由。
	 * Canonical delivery (three gate shapes): the started→reward QUEST_SELECT flips reward and shows the
	 * tier window; the SELECT5 report page, the 39/20002 check pairs, the priority-1 fallback and the
	 * select6 failure page are gone, so an incomplete hand-in has no route at all.
	 */
	private static boolean hasCanonicalDelivery(QuestDefinition definition, int npcId, RetailRow row,
			List<String> problems) {
		int questId = row.entry().questId();
		List<QuestCondition> hasItems = new ArrayList<>();
		List<QuestAction> removeItems = new ArrayList<>();
		if (row.entry().itemCheck()) {
			List<QuestItemRequirement> gate = row.metadata().itemRequirements().isEmpty()
				? workItemGate(row) : row.metadata().itemRequirements();
			if (gate == null) {
				problems.add(questId + ": item_check 行的工作物品门解析不到（give_item 符号不在 work_items 通道）");
				return false;
			}
			for (QuestItemRequirement item : gate) {
				hasItems.add(new QuestCondition.HasItem(item.itemId(), item.count()));
				removeItems.add(new QuestAction.RemoveItem(item.itemId(), item.count()));
			}
		}
		// 交付窗页取单一真源助手（零奖励组兜底固定窗 1）；单步面 completeFlow 拒绝多奖励档。
		// The delivery window page comes from the shared helper; single-step rows never carry ≥2 tiers.
		int windowPage = RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(row.metadata(), questId);
		List<QuestTransition> delivered = definition.transitions().stream().filter(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.QUEST_SELECT)
				&& "started".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())).toList();
		if (delivered.size() != 1) {
			problems.add(questId + ": 交付路由 QUEST_SELECT(started→reward) 数 " + delivered.size()
				+ "（期望 1）npc=" + npcId);
			return false;
		}
		QuestTransition delivery = delivered.get(0);
		// 过场重挂会在开窗前插入 PlayMovie（S1 唯一语义新增点，逐行不变量另行断言），此处先归一化。
		// The cutscene hop inserts PlayMovie before the window; the shape check normalizes it away.
		List<AfterCommitAction> afterCommit = new ArrayList<>(delivery.afterCommit());
		afterCommit.removeIf(action -> action instanceof AfterCommitAction.PlayMovie);
		List<AfterCommitAction> expectedAfterCommit = List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(windowPage));
		boolean shaped = delivery.conditions().equals(hasItems) && delivery.actions().equals(removeItems)
			&& delivery.priority() == null && afterCommit.equals(expectedAfterCommit);
		if (!shaped) {
			problems.add(questId + ": 交付边形状不符 npc=" + npcId + " 条件=" + delivery.conditions()
				+ " 动作=" + delivery.actions() + " 提交后=" + delivery.afterCommit()
				+ "（期望 条件=" + hasItems + " 动作=" + removeItems + " 提交后=" + expectedAfterCommit + "）");
		}
		if (hasLegacyDeliveryResidue(definition, npcId, hasItems)) {
			problems.add(questId + ": 交付段残留旧页链路由 npc=" + npcId
				+ " itemCheck=" + row.entry().itemCheck());
			return false;
		}
		return shaped;
	}

	/**
	 * 工作物品门的独立重算（与合成器同规则、异路径）：{@code item_check} 无 collect_item 的行，
	 * 门 = {@code give_item} 符号经 quest_data work_items 通道解析出的同物（数量取符号列，缺省 1），
	 * 且该物必须落在真端的 work-items 命名域里（判例 3204：quest_3204a ↔ 182209085）。
	 * Independent recomputation of the work-item gate: the give_item symbol resolves through the
	 * quest_data work-items channel and must sit in the retail work-items domain; null when the row
	 * offers no gate source (such rows do not compile).
	 */
	private static List<QuestItemRequirement> workItemGate(RetailRow row) {
		String symbol = row.entry().giveItemSymbol();
		Integer itemId = symbol == null ? null : RetailQuestWorkItems.first(symbol, row.entry().questId());
		if (itemId == null || row.metadata().questWorkItems().stream()
				.noneMatch(item -> item.itemId() == itemId)) {
			return null;
		}
		String[] parts = symbol.trim().split("\\s+");
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		return List.of(new QuestItemRequirement(itemId, count));
	}

	/**
	 * 失败侧零路由：旧交付页链的残留判据——{@code started} 源上的 1009/39/20002 路由（成功直翻与
	 * prio-1 回页）、交付 NPC 上的非空 priority、有门行的无门直翻领奖，以及定义内下发的
	 * SELECT5/SELECT6 页（完成流的 1009 预览路由源节点是 {@code reward}，不算残留）。
	 * Zero failure-side residue: no started-source 1009/39/20002 route, no non-null priority at the delivery
	 * npc, no ungated reward flip for gated rows, and no SELECT5/SELECT6 page push in the definition (the
	 * completion-flow 1009 preview route sources from {@code reward} and is not residue).
	 */
	private static boolean hasLegacyDeliveryResidue(QuestDefinition definition, int npcId,
			List<QuestCondition> gate) {
		boolean legacyStartedRoute = definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& "started".equals(transition.sourceNode())
				&& (isAction(talk, QuestDialogAction.SELECT_QUEST_REWARD)
					|| isAction(talk, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM)
					|| isAction(talk, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE)));
		// 优先级判据只查交付段（started→reward）：完成流（reward→complete）的类/槽位路由**本来就带优先级**
		// （rewardWindowAutoFlow 的 class×slot 索引 0..33），那不是旧形残留（判例 1690/2657/80290 等 10 行）。
		// The priority criterion is scoped to the delivery segment: the completion flow's class/slot routes
		// legitimately carry priorities (the rewardWindowAutoFlow class×slot index) and are not residue.
		boolean priorityRoute = definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& "started".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
				&& transition.priority() != null);
		boolean ungatedReward = !gate.isEmpty() && definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& "started".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
				&& transition.conditions().isEmpty() && transition.actions().isEmpty());
		boolean pageChain = definition.transitions().stream().anyMatch(transition ->
			transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id())));
		return legacyStartedRoute || priorityRoute || ungatedReward || pageChain;
	}

	private static boolean hasComplete(QuestDefinition definition, int npcId) {
		int first = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
		int last = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
		long count = definition.transitions().stream().filter(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() != null
				&& talk.dialogId() >= first && talk.dialogId() <= last
				&& statusOf(definition, transition.targetNode()) == QuestStatus.COMPLETE).count();
		return count == last - first + 1;
	}

	private static QuestStatus statusOf(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(node -> node.label().equals(label))
			.map(node -> node.projection().status()).findFirst().orElse(null);
	}

	private static boolean isAction(QuestEvent.TalkToNpc talk, QuestDialogAction action) {
		return talk.dialogId() != null && talk.dialogId() == action.id();
	}

	// ------------------------------------------------------------------ 分类与装载

	/** 逐任务分类（真端合成定义 vs 历史 XML，登记用）。 / Per-quest classification versus the legacy XML. */
	private static Map<Integer, String> classify() throws Exception {
		Map<Integer, String> classification = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				classification.put(questId, "REJECTED:" + row.outcome().rejectionCode());
				continue;
			}
			if (RetiredQuestIds.contains(questId)) {
				// 已退役：XML 只在 git 历史里，不可重算；分类由冻结登记行承担。
				// Retired: the XML lives in git history, so the frozen registry row is the evidence.
				classification.put(questId, driftRegistry.get(questId));
				continue;
			}
			CompiledQuestDefinition fromXml = QuestXmlFixtures.compile(questId);
			String problem = RetailSimpleHuntEquivalenceGateTest.irEquivalenceProblem(
				fromXml.definition(), row.outcome().definition().definition());
			if (problem == null) {
				classification.put(questId, "EQUIVALENT");
			} else {
				classification.put(questId, "DIFF:" + (problem.startsWith("node sets differ")
					? "NODE_PROJECTION" : "TRANSITION_SET"));
			}
		}
		return classification;
	}

	private static String dumpText(Map<Integer, String> classification) {
		Map<String, Integer> histogram = new TreeMap<>();
		classification.values().forEach(kind -> histogram.merge(kind, 1, Integer::sum));
		StringBuilder text = new StringBuilder("# SimpleTalk 漂移登记导出（登记不是退役门槛）\n");
		histogram.forEach((kind, count) -> text.append("# ").append(kind).append('\t').append(count).append('\n'));
		classification.forEach((questId, kind) -> text.append(questId).append('\t').append(kind).append('\n'));
		return text.toString();
	}

	/** 单任务真端编译上下文（表行 + 元数据 + 结果）。 / Retail compile context for one quest row. */
	private record RetailRow(RetailSimpleTalkTable.Entry entry, QuestMetadata metadata, int acquiredNpc,
			List<Integer> rewardNpcs, RetailSimpleTalkDefinitionCompiler.Outcome outcome) {
	}

	private static RetailRow retailRow(int questId) {
		RetailSimpleTalkTable.Entry entry = table.find(questId).orElseThrow();
		var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
			itemIndex, randomRewards, nameIds);
		var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry, npcIndex, itemIndex, metadata,
			clientDialogExits, clientSummaryRows, clientRewardNpcs, interactionObjects);
		// 交付 NPC 集：唯一名解析优先，复合势力引用取客户端任务书 dic 链登记（与合成器同口径）。
		// Hand-in NPC set: unique name first, composite faction references via the client registry.
		Set<Integer> resolvedReward = npcIndex.resolveAll(List.of(
			entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
		List<Integer> rewardNpcs = resolvedReward.size() == 1
			? List.of(resolvedReward.iterator().next()) : clientRewardNpcs.rewardNpcs(entry.questId());
		// P0c-12：挑战任务哨兵受理 NPC = 交付 shugo（与编译器同口径）。
		String acquireName = entry.grantKind() == RetailGrantKind.CHALLENGE_TASK
			? entry.rewardNpc() : entry.acquiredNpc();
		return new RetailRow(entry, metadata.metadata(), resolveOrMinusOne(acquireName),
			rewardNpcs, outcome);
	}

	/** 唯一 NPC id；空集/多解返回 -1（拒绝码已覆盖这些行）。 / Unique npc id, -1 when unresolved. */
	private static int resolveOrMinusOne(String name) {
		Set<Integer> ids = npcIndex.resolveAll(List.of(name == null ? "" : name)).npcIds();
		return ids.size() == 1 ? ids.iterator().next() : -1;
	}

	/** 真端表 ∩ 生产宇宙（catalog ∪ 退役 fixture）。 / Retail table intersected with the production universe. */
	private static Set<Integer> familyIds() throws Exception {
		String text = new String(open(CATALOG).readAllBytes(), StandardCharsets.UTF_8);
		Set<Integer> catalog = new HashSet<>();
		var matcher = java.util.regex.Pattern.compile("<definition id=\"(\\d+)\"").matcher(text);
		while (matcher.find()) {
			catalog.add(Integer.parseInt(matcher.group(1)));
		}
		catalog.addAll(retiredFixtureIds());
		Set<Integer> ids = new TreeSet<>(table.questIds());
		ids.retainAll(catalog);
		return ids;
	}

	/** 已退役任务 id（保留清单 owner=RETAIL_TABLE）。 / Retired quest ids from the retention manifest. */
	private static Set<Integer> retiredFixtureIds() {
		return com.aionemu.gameserver.questEngine.definition.RetiredQuestIds.all();
	}

	private static Map<Integer, String> driftRegistry() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		InputStream input = RetailSimpleTalkGateTest.class.getResourceAsStream(DRIFT_REGISTRY);
		assertNotNull(input, "missing resource " + DRIFT_REGISTRY);
		for (String line : lines(input)) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			rows.put(Integer.parseInt(parts[0]), parts[1]);
		}
		return rows;
	}

	private static Map<String, Integer> randomRewardIds() throws Exception {
		Map<String, Integer> ids = new HashMap<>();
		var document = parse(open("/aion/data/static_data/quest/legacy/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
	}

	private static Map<Integer, Integer> nameIds() throws Exception {
		Map<Integer, Integer> ids = new HashMap<>();
		for (String line : lines(open("/aion/data/static_data/quest/retail/quest_name_string_ids.tsv"))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())), Integer.parseInt(parts[1]));
		}
		return ids;
	}

	private static List<String> listXmlNames(String dir) throws Exception {
		var url = RetailSimpleTalkGateTest.class.getResource(dir);
		assertNotNull(url, "missing dir " + dir);
		java.io.File[] files = new java.io.File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
		assertNotNull(files);
		return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
	}

	private static org.w3c.dom.Document parse(InputStream input) throws Exception {
		try (input) {
			var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(input);
		}
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws Exception {
		List<InputStream> inputs = new java.util.ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private static InputStream open(String resource) throws Exception {
		InputStream input = RetailSimpleTalkGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
