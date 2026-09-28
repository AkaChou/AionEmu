package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleTalk wave A 链式行永久门禁（P0c-10f 退役口径，替代临时对拍探针）。三不变量：
 * <ol>
 * <li><b>回放保真</b>：83 行 ADOPT 逐行用生产口径 fixture 编译，合成定义的节点投影必须等于
 *     登记 N 记录（label/status/var0），IR 指纹必须等于冻结值
 *     {@code retail-simple-talk-chain-ir-fingerprints.tsv}
 *     （重冻结：{@code -Dretail.talkChain.fingerprintOut=<path>}）；</li>
 * <li><b>retention 分区</b>：登记行集恰好二分为 ADOPT（RETAIL_TABLE + 已退役）与
 *     KEEP（XML_RETENTION + 未退役），无孤儿行；</li>
 * <li><b>登记形状</b>：每行至少有节点与布局记录，ADOPT 行必须有路由或规范块（残组形状）——
 *     登记表可增长（wave B），但已冻结的指纹集与分区不许静默漂移。</li>
 * </ol>
 * Permanent gate for the wave-A chain rows: frozen retail-side fingerprints, retention partition,
 * and replay fidelity against the registry — replaces the deleted adjudication probe.
 */
class RetailSimpleTalkChainGateTest {

	/** S2 规范段行下限：双块行（G1）实测 203，取 200 防谓词失配导致不变量空转。 */
	private static final int CANONICAL_SEGMENT_ROW_FLOOR = 200;
	/** S2 链式过场行下限：真端 cutsceneid1 ∩ RETAIL_TABLE ∩ 链式 = 4 行（1422/2421/3006/3020）。 */
	private static final int CHAIN_CUTSCENE_ROW_FLOOR = 4;
	/** S3a 接取段接管行下限：有 NPC_START 块、无 NPC_REPORT 块、非系统发放、selectionSources 段内 = 60 行。 */
	private static final int CANONICAL_ACCEPT_ROW_FLOOR = 55;
	/** S3c 交付段接管行下限/上限：A 形翻面行实测 30（下限 28 防谓词失配，上限 40 防切片越界）。 */
	private static final int S3C_DELIVERY_ROW_FLOOR = 28;
	private static final int S3C_DELIVERY_ROW_CEILING = 40;
	/** S3c 对照行下限：有报告页下发但无 reward 入边翻面记录、也无报告块的行（本片不碰，判例 1152——翻面落
	 *  `pepper` 非 `reward`）实测 1。 / Control rows (report page but no reward-bound flip): 1 measured. */
	private static final int S3C_CONTROL_ROW_FLOOR = 1;
	/** S3c 系统发放报告块路径下限：实测 1（35017——systemGrant + REPORT 块 + 无翻面记录）。 */
	private static final int S3C_BLOCK_PATH_ROW_FLOOR = 1;
	/** S3c-D 谓词面下限/上限：实测 50 行（有 reward 入边翻面记录且无报告块）；上限 60 防切片越界。 */
	private static final int S3C_D_ROW_FLOOR = 48;
	private static final int S3C_D_ROW_CEILING = 60;
	/** S3c-D mixed 子面下限：实测 5（4209/21033/21455/30711/30761，中间人翻面逐字保留、只退场报告页）。 */
	private static final int S3C_MIXED_ROW_FLOOR = 4;

	/** S3c-obj：物件哨兵接取者（判例 1323）恰 1 行——下限防谓词失配，上限防切片越界。 */
	private static final int S3C_OBJECT_ROW_FLOOR = 1;

	private static final int S3C_OBJECT_ROW_CEILING = 1;
	/** S3a/S3b 延期行集：越界 selectionSources **且**该源上登记表有 `FINISH_DIALOG` 记录（有载体，不得静默丢）。
	 *  S3b 后为空——判例 28809 的段外源（`s1`/`s2`）经复算为**无载体**（登记表无对应 `FINISH_DIALOG`、
	 *  客户端页动作集亦无 `1008`）⇒ 裁定放行；集合必须逐行命中，登记表漂移即红。 */
	private static final Set<Integer> ACCEPT_DEFERRED_ROWS = Set.of();
	/** S2 策略 A 词表（与合成器同口径的测试侧独立副本）。 */
	private static final Set<String> CHAIN_ACCEPT_RETIRED = Set.of(
		"ASK_QUEST_ACCEPT", "SELECT1", "SELECT1_1", "SELECT1_1_1", "QUEST_ACCEPT_1",
		"QUEST_ACCEPT_SIMPLE", "QUEST_REFUSE_1", "QUEST_REFUSE_2", "QUEST_REFUSE_SIMPLE");
	private static final Set<String> CHAIN_DELIVERY_RETIRED = Set.of(
		"SELECT_QUEST_REWARD", "CHECK_USER_HAS_QUEST_ITEM", "CHECK_USER_HAS_QUEST_ITEM_SIMPLE",
		"SELECT5", "SELECT6");
	private static final Set<String> CHAIN_RETIRED_PAGES = Set.of(
		"SELECT1", "SELECT1_1", "SELECT1_1_1", "SELECT5", "SELECT6");

	private static final String CHAIN_STEPS = "/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv";
	private static final String FINGERPRINTS = "/quest/retail-simple-talk-chain-ir-fingerprints.tsv";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml",
		"npc_template_834290_885645.xml");

	private static RetailSimpleTalkTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static RetailClientDialogExits exits;
	private static RetailClientSummaryRows summaryRows;
	private static RetailClientRewardNpcs clientRewardNpcs;
	private static RetailClientTalkChainSteps chainSteps;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Map<Integer, String> frozenFingerprints;
	private static Map<Integer, String> retentionOwners;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml")) {
			table = RetailSimpleTalkTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		exits = RetailClientDialogExits.defaultExits();
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv")) {
			summaryRows = RetailClientSummaryRows.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv")) {
			clientRewardNpcs = RetailClientRewardNpcs.load(input);
		}
		try (InputStream input = open(CHAIN_STEPS)) {
			chainSteps = RetailClientTalkChainSteps.load(input);
		}
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll("/aion/data/static_data/items/item/",
			listXmlNames("/aion/data/static_data/items/item/")));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		frozenFingerprints = new TreeMap<>();
		for (String line : lines(open(FINGERPRINTS))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			frozenFingerprints.put(Integer.parseInt(parts[0]), parts[1]);
		}
		retentionOwners = new HashMap<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			retentionOwners.put(Integer.parseInt(parts[0]), parts[1]);
		}
	}

	/** 不变量 1：ADOPT 行回放保真（节点投影 = 登记 N 记录，IR 指纹 = 冻结值）。 */
	@Test
	void adoptRowsReplayRegistryAndMatchFrozenFingerprints() throws Exception {
		String freezeOut = System.getProperty("retail.talkChain.fingerprintOut");
		// 重冻结模式：遍历 retention 驱动的全部采纳候选（新采纳行尚无冻结值也可入表）；
		// 校验模式：遍历冻结键集。
		Set<Integer> targets = freezeOut != null
			? chainSteps.questIds().stream()
				.filter(qid -> "RETAIL_TABLE".equals(retentionOwners.get(qid)))
				.collect(java.util.stream.Collectors.toCollection(TreeSet::new))
			: new TreeSet<>(frozenFingerprints.keySet());
		Map<Integer, String> computed = new TreeMap<>();
		List<String> problems = new ArrayList<>();
		for (int questId : targets) {
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty()) {
				problems.add(questId + ": 真端表/quest.xml 输入缺失");
				continue;
			}
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex,
				RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex, randomRewards, nameIds),
				exits, summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			String nodeProblem = nodeProjectionProblem(questId, outcome.definition().definition());
			if (nodeProblem != null) {
				problems.add(nodeProblem);
				continue;
			}
			computed.put(questId, RetailIrFingerprint.fingerprint(outcome.definition().definition()));
		}
		assertTrue(problems.isEmpty(), () -> "链式行回放缺口：" + problems.stream().limit(10).toList());
		if (freezeOut != null) {
			StringBuilder text = new StringBuilder("# SimpleTalk wave A 链式行真端侧冻结 IR 指纹\n"
				+ "# 由 -Dretail.talkChain.fingerprintOut 重算生成；任何变化必须人工核对后入仓\n"
				+ "# quest_id\tir_fingerprint\n");
			for (var e : computed.entrySet()) {
				text.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
			}
			Files.writeString(Path.of(freezeOut), text.toString());
			return;
		}
		assertEquals(frozenFingerprints, computed, () -> "链式 IR 指纹偏离冻结值（如为预期演进，重跑"
			+ " -Dretail.talkChain.fingerprintOut 并人工核对）");
	}

	/**
	 * S2 不变量：双块规范段行（同时具备 NPC_START 与 NPC_REPORT 块）的接取/交付段必须逐行等于规范形，
	 * 且定义内旧页链零残留。门源与合成器同规则、异路径独立重算（含退场记录 HAS_ITEM 载荷兜底承接）。
	 * S2: rows with both blocks carry the canonical accept/delivery segments with zero page-chain
	 * residue — the gate source is recomputed independently from the registry fixtures.
	 */
	@Test
	void canonicalSegmentRowsCarryCanonicalShapesAndZeroPageChainResidue() throws Exception {
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (int questId : new TreeSet<>(chainSteps.questIds())) {
			if (!"RETAIL_TABLE".equals(retentionOwners.get(questId))) {
				continue;
			}
			List<RetailClientTalkChainSteps.BlockRecord> starts = chainSteps.blocks(questId, "NPC_START");
			List<RetailClientTalkChainSteps.BlockRecord> reports = chainSteps.blocks(questId, "NPC_REPORT");
			if (starts.isEmpty() || reports.isEmpty()) {
				continue;
			}
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty()) {
				problems.add(questId + ": 真端表/quest.xml 输入缺失");
				continue;
			}
			var metadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
				randomRewards, nameIds);
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex, metadata, exits,
				summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			checked++;
			QuestDefinition definition = outcome.definition().definition();
			Set<Integer> acceptNpcs = new TreeSet<>();
			starts.forEach(block -> acceptNpcs.add(block.npcId()));
			Set<Integer> reportNpcs = new TreeSet<>();
			reports.forEach(block -> reportNpcs.add(block.npcId()));
			reportNpcs.addAll(rewardNpcs(entry.get()));
			for (RetailClientTalkChainSteps.BlockRecord start : starts) {
				boolean landing = definition.transitions().stream().anyMatch(transition ->
					transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == start.npcId()
						&& isAction(talk, QuestDialogAction.QUEST_SELECT)
						&& "unaccepted".equals(transition.sourceNode())
						&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())));
				if (!landing) {
					problems.add(questId + ": 缺 canonical 接取窗边 npc=" + start.npcId());
				}
				for (QuestDialogAction commit : List.of(QuestDialogAction.QUEST_ACCEPT_1,
						QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
					boolean present = definition.transitions().stream().anyMatch(transition ->
						transition.event() instanceof QuestEvent.TalkToNpc talk
							&& talk.npcId() == start.npcId() && isAction(talk, commit)
							&& "unaccepted".equals(transition.sourceNode())
							&& start.target().equals(transition.targetNode())
							&& transition.conditions().contains(new QuestCondition.StartEligible()));
					if (!present) {
						problems.add(questId + ": 缺 " + commit + " 接取提交边 npc=" + start.npcId());
					}
				}
			}
			for (QuestTransition transition : definition.transitions()) {
				boolean retiredPage = transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT1.id()
							|| page.dialogId() == QuestDialogPage.SELECT1_1.id()
							|| page.dialogId() == QuestDialogPage.SELECT1_1_1.id()
							|| page.dialogId() == QuestDialogPage.SELECT5.id()
							|| page.dialogId() == QuestDialogPage.SELECT6.id()));
				if (retiredPage) {
					problems.add(questId + ": 规范段行仍下发旧页链页 " + transition.sourceNode()
						+ "→" + transition.targetNode());
				}
				if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
						&& talk.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
						&& (acceptNpcs.contains(talk.npcId()) || reportNpcs.contains(talk.npcId()))) {
					problems.add(questId + ": 规范段行仍保留 1007 中转 npc=" + talk.npcId());
				}
			}
			for (RetailClientTalkChainSteps.BlockRecord report : reports) {
				var rewardIds = rewardNpcs(entry.get());
				int reportNpc = rewardIds.size() == 1 ? rewardIds.iterator().next() : report.npcId();
				List<QuestItemRequirement> gate = expectedChainGate(entry.get(), metadata.metadata(),
					reportNpcs, questId);
				List<QuestCondition> hasItems = gate.stream()
					.<QuestCondition>map(item -> new QuestCondition.HasItem(item.itemId(), item.count())).toList();
				List<QuestAction> removeItems = gate.stream()
					.<QuestAction>map(item -> new QuestAction.RemoveItem(item.itemId(), item.count())).toList();
				int windowPage = RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata.metadata(),
					questId);
				List<QuestTransition> delivered = definition.transitions().stream().filter(transition ->
					transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == reportNpc
						&& isAction(talk, QuestDialogAction.QUEST_SELECT)
						&& report.source().equals(transition.sourceNode())
						&& "reward".equals(transition.targetNode())).toList();
				if (delivered.size() != 1) {
					problems.add(questId + ": 交付边 QUEST_SELECT(" + report.source() + "→reward) 数 "
						+ delivered.size() + "（期望 1）npc=" + reportNpc);
					continue;
				}
				QuestTransition delivery = delivered.get(0);
				List<AfterCommitAction> afterCommit = new ArrayList<>(delivery.afterCommit());
				afterCommit.removeIf(action -> action instanceof AfterCommitAction.PlayMovie);
				List<AfterCommitAction> expectedAfter = List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(windowPage));
				if (!delivery.conditions().equals(hasItems) || !delivery.actions().equals(removeItems)
						|| delivery.priority() != null || !afterCommit.equals(expectedAfter)) {
					problems.add(questId + ": 交付边形状不符 npc=" + reportNpc + " 条件="
						+ delivery.conditions() + " 动作=" + delivery.actions() + " 提交后=" + delivery.afterCommit()
						+ "（期望 条件=" + hasItems + " 动作=" + removeItems + " 提交后=" + expectedAfter
						+ " 优先级=" + delivery.priority() + "）");
				}
			}
		}
		final int covered = checked;
		assertTrue(problems.isEmpty(), () -> "链式规范段缺口：" + problems.stream().limit(20).toList()
			+ " (checked=" + covered + ")");
		assertTrue(covered >= CANONICAL_SEGMENT_ROW_FLOOR,
			() -> "规范段行覆盖回退：" + covered + " < " + CANONICAL_SEGMENT_ROW_FLOOR);
	}

	/**
	 * S3a 不变量：仅接取段接管的行（有 NPC_START 块、无 NPC_REPORT 块、非系统发放、selectionSources 段内）
	 * 必须逐行等于 canonical 接取形——{@code QUEST_SELECT(unaccepted)→接取窗} 边、1002/20000 两形提交
	 * （带 StartEligible 落在块 target）、块 NPC 上零页梯残留（select1 族页下发与 1007/1011/1012/1013 动作）；
	 * **交付段逐字保留**（登记记录的报告页下发仍在，与 S2 双块行的零残留成对照）；延期行
	 * （越界 selectionSources，判例 28809）必须保持 legacy 页梯（select1 入口页仍在），不得被静默接管。
	 * S3a: accept-only rows carry the canonical accept segment while their R-driven delivery stays
	 * verbatim; deferred rows keep their legacy ladder.
	 */
	@Test
	void acceptOnlyRowsCarryCanonicalAcceptAndKeepTheirDeliverySegment() throws Exception {
		List<String> problems = new ArrayList<>();
		int checked = 0;
		Set<Integer> deferred = new TreeSet<>();
		for (int questId : new TreeSet<>(chainSteps.questIds())) {
			if (!"RETAIL_TABLE".equals(retentionOwners.get(questId))) {
				continue;
			}
			List<RetailClientTalkChainSteps.BlockRecord> starts = chainSteps.blocks(questId, "NPC_START");
			if (!chainSteps.blocks(questId, "NPC_REPORT").isEmpty()) {
				continue;
			}
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty() || entry.get().grantKind().systemGrant()) {
				continue;
			}
			// S3b：R 驱动接取段（无 NPC_START 块、由登记表提交形合成）同样纳入本不变量；接取 NPC 取
			// `acquired` 解析值（与合成器同源），物件入口行（判例 1323）不在本片。
			Set<Integer> acceptNpcs = new TreeSet<>();
			String acceptTarget = "started";
			boolean rDriven = starts.isEmpty();
			if (rDriven) {
				var acquired = npcIndex.resolveAll(List.of(
					entry.get().acquiredNpc() == null ? "" : entry.get().acquiredNpc())).npcIds();
				int npc = acquired.size() == 1 ? acquired.iterator().next() : -1;
				boolean submitted = chainSteps.routes(questId).stream().anyMatch(route ->
					"unaccepted".equals(route.source()) && "started".equals(route.target())
						&& route.action().startsWith("QUEST_ACCEPT"));
				// 入口必须是客户端可点的 `QUEST_SELECT` 记录（与合成器同判据；判例 1323 的物件入口不在本片）。
				boolean clientEntry = chainSteps.routes(questId).stream().anyMatch(route -> route.npcId() == npc
					&& "unaccepted".equals(route.source()) && "QUEST_SELECT".equals(route.action()));
				if (acquired.size() != 1 || !submitted || !clientEntry) {
					continue;
				}
				acceptNpcs.add(acquired.iterator().next());
			} else {
				starts.forEach(block -> acceptNpcs.add(block.npcId()));
				acceptTarget = starts.getFirst().target();
			}
			var metadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
				randomRewards, nameIds);
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex, metadata, exits,
				summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			QuestDefinition definition = outcome.definition().definition();
			final String acceptTargetRef = acceptTarget;
			if (starts.stream().anyMatch(block -> !acceptSourcesWithinSegment(questId, block))) {
				deferred.add(questId);
				boolean legacyLadder = definition.transitions().stream().anyMatch(transition ->
					transition.event() instanceof QuestEvent.TalkToNpc talk
						&& acceptNpcs.contains(talk.npcId()) && isAction(talk, QuestDialogAction.QUEST_SELECT)
						&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT1.id())));
				if (!legacyLadder) {
					problems.add(questId + ": 延期行必须保持 legacy 接取页梯（select1 入口页未下发）");
				}
				continue;
			}
			checked++;
			for (int acceptNpc : acceptNpcs) {
				boolean landing = definition.transitions().stream().anyMatch(transition ->
					transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == acceptNpc
						&& isAction(talk, QuestDialogAction.QUEST_SELECT)
						&& "unaccepted".equals(transition.sourceNode())
						&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())));
				if (!landing) {
					problems.add(questId + ": 缺 canonical 接取窗边 npc=" + acceptNpc);
				}
				for (QuestDialogAction commit : List.of(QuestDialogAction.QUEST_ACCEPT_1,
						QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
					boolean present = definition.transitions().stream().anyMatch(transition ->
						transition.event() instanceof QuestEvent.TalkToNpc talk
							&& talk.npcId() == acceptNpc && isAction(talk, commit)
							&& "unaccepted".equals(transition.sourceNode())
							&& acceptTargetRef.equals(transition.targetNode())
							&& transition.conditions().contains(new QuestCondition.StartEligible()));
					if (!present) {
						problems.add(questId + ": 缺 " + commit + " 接取提交边 npc=" + acceptNpc);
					}
				}
			}
			for (QuestTransition transition : definition.transitions()) {
				boolean retiredPage = transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT1.id()
							|| page.dialogId() == QuestDialogPage.SELECT1_1.id()
							|| page.dialogId() == QuestDialogPage.SELECT1_1_1.id()));
				if (retiredPage) {
					problems.add(questId + ": 接取段行仍下发旧页梯页 " + transition.sourceNode()
						+ "→" + transition.targetNode());
				}
				if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
						&& acceptNpcs.contains(talk.npcId())
						&& (talk.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
							|| talk.dialogId() == QuestDialogAction.SELECT1.id()
							|| talk.dialogId() == QuestDialogAction.SELECT1_1.id()
							|| talk.dialogId() == QuestDialogAction.SELECT1_1_1.id())) {
					problems.add(questId + ": 接取段行仍保留页梯动作 " + talk.dialogId() + " npc=" + talk.npcId());
				}
			}
			// 交付段逐字保留：登记记录下发的交付页（SELECT5/SELECT6）必须仍在合成定义里下发——
			// 与 S2 双块行的"零残留"互为对照（S3a 不碰交付段）。
			Set<String> registryDeliveryPages = new TreeSet<>();
			for (RetailClientTalkChainSteps.RouteRecord route : chainSteps.routes(questId)) {
				for (String token : route.afterCommits().split(";")) {
					String trimmed = token.trim();
					if (trimmed.startsWith("DIALOG:SHOW_QUEST_PAGE:")) {
						String page = trimmed.substring("DIALOG:SHOW_QUEST_PAGE:".length());
						if ("SELECT5".equals(page) || "SELECT6".equals(page)) {
							registryDeliveryPages.add(page);
						}
					}
				}
			}
			// S3c：交付段已被 A 形接管（无报告块 + 翻面记录）的行，其报告页按 R-REP 页下发判据退场——
			// 本不变量只对**交付段仍为 legacy** 的行成立（对照断言在 S3c 不变量里逐行重算）。
			// Rows whose delivery segment was taken over by S3c are out of this verbatim-preservation sample.
			// S3c-D：交付段报告页按 R-REP 页判据退场的行（有 reward 入边翻面记录且无报告块）同样不在
			// "交付段逐字保留"取样面内——对照断言在 S3c-D 不变量里逐行重算。
			// Rows whose report page retires under S3c-D (R-REP) are out of this verbatim sample too.
			if (deliveryTakenOverByA(entry.get()) || dClassReportRetire(entry.get())) {
				continue;
			}
			for (String page : registryDeliveryPages) {
				int pageId = QuestDialogPage.valueOf(page).id();
				boolean pushed = definition.transitions().stream().anyMatch(transition ->
					transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(pageId)));
				if (!pushed) {
					problems.add(questId + ": 交付段页 " + page + " 被接取段接管误删（S3a 不碰交付段）");
				}
			}
		}
		final int covered = checked;
		assertTrue(problems.isEmpty(), () -> "S3a 接取段缺口：" + problems.stream().limit(20).toList()
			+ " (checked=" + covered + ")");
		assertTrue(covered >= CANONICAL_ACCEPT_ROW_FLOOR,
			() -> "接取段接管行覆盖回退：" + covered + " < " + CANONICAL_ACCEPT_ROW_FLOOR);
		assertEquals(ACCEPT_DEFERRED_ROWS, deferred,
			() -> "延期行集漂移（登记表 selectionSources 变化必须重新裁定）：" + deferred);
	}

	/**
	 * S3c 不变量：A 形交付段接管（实测 30 行）必须逐行换规范交付形——每条真端翻面记录**恰一条**
	 * {@code (源节点, 交付 NPC, 31)} 键的交付边（同键登记记录被层 A 覆盖删除即红），落 `reward` 且带
	 * 档位奖励窗；翻面记录的**载荷逐字承接**（条件侧 `HAS_ITEM`/`VAR_IS`、动作侧 `REMOVE_ITEM`/`SET_VAR`/
	 * `GIVE_ITEM`——静默丢门/丢物/丢阶段写即红）；旧报告页（SELECT5/SELECT6）零下发；reward 态保留奖励窗
	 * 重开载体（G-3）。对照：**竞争翻面行**（D 类中间人 premature 形）交付段逐字保留，报告页必须仍在。
	 * S3c: A-shaped delivery takeover rows carry one canonical gated delivery edge per retail flip record
	 * with the flip payload carried verbatim and zero report-page residue; the D-class control stays legacy.
	 */
	@Test
	void s3cDeliveryRowsCarryCanonicalDeliveryShapesAndZeroResidue() throws Exception {
		List<String> problems = new ArrayList<>();
		Set<Integer> covered = new TreeSet<>();
		int control = 0;
		int blockTakenOver = 0;
		int dClassCovered = 0;
		int mixedCovered = 0;
		for (int questId : new TreeSet<>(chainSteps.questIds())) {
			if (!"RETAIL_TABLE".equals(retentionOwners.get(questId))) {
				continue;
			}
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty()) {
				continue;
			}
			Set<Integer> scope = rewardNpcScope(entry.get());
			Set<String> startStates = startStateLabels(questId);
			List<RetailClientTalkChainSteps.RouteRecord> flips = aShapedFlips(entry.get(), scope, startStates);
			boolean competing = !competingFlips(entry.get(), scope, startStates).isEmpty();
			Set<String> registryReportPages = new TreeSet<>();
			for (RetailClientTalkChainSteps.RouteRecord route : chainSteps.routes(questId)) {
				registryReportPages.addAll(pushedPages(route.afterCommits()));
			}
			registryReportPages.retainAll(Set.of("SELECT5", "SELECT6"));
			// 对照组：有报告页下发但**没有** reward 入边翻面记录的行 ⇒ 交付段必须逐字保留（本片不碰，
			// 判例 1152——翻面落在 `pepper` 非 `reward`）。
			// Control: rows with report pages but no reward-bound flip keep their delivery verbatim.
			if (!dClassReportRetire(entry.get()) && !deliveryTakenOverByA(entry.get())
					&& chainSteps.blocks(questId, "NPC_REPORT").isEmpty()
					&& !registryReportPages.isEmpty()) {
				var controlMetadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
					randomRewards, nameIds);
				var controlOutcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex,
					controlMetadata, exits, summaryRows, clientRewardNpcs, chainSteps);
				if (!controlOutcome.accepted()) {
					problems.add(questId + ": 对照行被拒绝 " + controlOutcome.rejectionCode());
					continue;
				}
				control++;
				for (String page : registryReportPages) {
					int pageId = QuestDialogPage.valueOf(page).id();
					boolean pushed = controlOutcome.definition().definition().transitions().stream()
						.anyMatch(transition -> transition.afterCommit()
							.contains(new AfterCommitAction.ShowQuestDialog(pageId)));
					if (!pushed) {
						problems.add(questId + ": 对照行的报告页 " + page + " 被误删（无 reward 入边不碰）");
					}
				}
				continue;
			}
			// S3c-D 面（(iii) 的**可证安全子集**）：交付 NPC 侧下发报告页（`SELECT5`/`SELECT6`）的记录按
			// R-REP 页下发判据退场；中间人翻面**逐字保留**（「就地加窗」已被 e2e 契约门否决——中间人 owner
			// 无领奖选择腿 ⇒ 加窗会造可见死按钮，判例 3100）。
			// S3c-D: report pages retire by the page criterion; mid flips stay verbatim (windowing rejected
			// on e2e evidence of dead reward-selection buttons).
			if (dClassReportRetire(entry.get()) && !deliveryTakenOverByA(entry.get())) {
				var dMetadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
					randomRewards, nameIds);
				var dOutcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex,
					dMetadata, exits, summaryRows, clientRewardNpcs, chainSteps);
				if (!dOutcome.accepted()) {
					problems.add(questId + ": (iii) 行被拒绝 " + dOutcome.rejectionCode() + " "
						+ dOutcome.detail());
					continue;
				}
				QuestDefinition dDefinition = dOutcome.definition().definition();
				for (QuestTransition transition : dDefinition.transitions()) {
					boolean retiredPage = transition.afterCommit().stream().anyMatch(action2 ->
						action2 instanceof AfterCommitAction.ShowQuestDialog page
							&& (page.dialogId() == QuestDialogPage.SELECT5.id()
								|| page.dialogId() == QuestDialogPage.SELECT6.id()));
					if (retiredPage) {
						problems.add(questId + ": (iii) 行仍下发报告页 " + transition.sourceNode()
							+ "→" + transition.targetNode());
					}
				}
				Set<Integer> dScope = rewardNpcScope(entry.get());
				Set<String> dStartStates = startStateLabels(questId);
				List<RetailClientTalkChainSteps.RouteRecord> dFlips = chainSteps.routes(questId).stream()
					.filter(route -> "reward".equals(route.target()) && dStartStates.contains(route.source())
						&& A_SHAPED_ACTIONS.contains(route.action()))
					.toList();
				for (RetailClientTalkChainSteps.RouteRecord flip : dFlips) {
					QuestDialogAction action = QuestDialogAction.valueOf(flip.action());
					Integer flipPriority = "-".equals(flip.priority()) ? null : Integer.parseInt(flip.priority());
					// 同键可按优先级并存（判例 21455 的 prio 0 翻面与 prio 1 自环），故按优先级消歧。
					// Same-key edges can coexist by priority; disambiguate on it.
					List<QuestTransition> matches = dDefinition.transitions().stream()
						.filter(transition -> Objects.equals(transition.sourceNode(), flip.source()))
						.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
							&& talk.npcId() == flip.npcId() && isAction(talk, action))
						.filter(transition -> Objects.equals(transition.priority(), flipPriority))
						.toList();
					if (matches.size() != 1) {
						problems.add(questId + ": 翻面边 " + flip.source() + ":" + flip.npcId() + ":"
							+ flip.action() + " 数 " + matches.size() + "（期望 1）");
						continue;
					}
					if (!matches.getFirst().afterCommit().equals(expectedRegistryAfter(flip.afterCommits()))) {
						problems.add(questId + ": 翻面边 " + flip.source() + ":" + flip.npcId()
							+ " after 未逐字保留（(iii) 不留驻 ⇒ 加窗已否决）");
					}
				}
				// G-3 对偶（S3c-D 行没有 canonicalDelivery 分支的自动守卫）：reward 态必须保留奖励窗重开载体，
				// 否则 R-REP 退场把载体一起打掉（判例 4970：其 USE_OBJECT 腿下发 SELECT5 ⇒ 载体是
				// `SELECT_QUEST_REWARD reward→reward → 窗1`）。
				Map<String, QuestStatus> dStatus = new HashMap<>();
				dDefinition.nodes().forEach(node -> dStatus.put(node.label(), node.projection().status()));
				boolean dReopenCarrier = dDefinition.transitions().stream()
					.filter(transition -> transition.sourceNode() != null)
					.filter(transition -> dStatus.get(transition.sourceNode()) == QuestStatus.REWARD)
					.flatMap(transition -> transition.afterCommit().stream())
					.anyMatch(action2 -> action2 instanceof AfterCommitAction.ShowQuestDialog page
						&& rewardWindowPages().contains(page.dialogId()));
				if (!dReopenCarrier) {
					problems.add(questId + ": (iii) 行缺 reward 态奖励窗重开载体（R-REP 不得打掉载体）");
				}
				dClassCovered++;
				if (!aShapedFlips(entry.get(), dScope, dStartStates).isEmpty()
						&& !competingFlips(entry.get(), dScope, dStartStates).isEmpty()) {
					mixedCovered++;
				}
				continue;
			}
			// 块路径（S2 形 / 系统发放交付段）行：由 S2 不变量与守卫不变量覆盖；本不变量只锁 A 形面。
			// system-grant rows without a flip record (判例 35017) take the block path — asserted below.
			if (flips.isEmpty() && entry.get().grantKind().systemGrant()
					&& !chainSteps.blocks(questId, "NPC_REPORT").isEmpty()) {
				var blockMetadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
					randomRewards, nameIds);
				var blockOutcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex,
					blockMetadata, exits, summaryRows, clientRewardNpcs, chainSteps);
				if (!blockOutcome.accepted()) {
					problems.add(questId + ": 系统发放报告块行被拒绝 " + blockOutcome.rejectionCode());
					continue;
				}
				blockTakenOver++;
				QuestDefinition blockDefinition = blockOutcome.definition().definition();
				Set<Integer> blockWindows = rewardWindowPages();
				for (RetailClientTalkChainSteps.BlockRecord report
						: chainSteps.blocks(questId, "NPC_REPORT")) {
					List<QuestTransition> gated = blockDefinition.transitions().stream()
						.filter(transition -> Objects.equals(transition.sourceNode(), report.source()))
						.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
							&& isAction(talk, QuestDialogAction.QUEST_SELECT))
						.filter(transition -> "reward".equals(transition.targetNode()))
						.toList();
					if (gated.size() != 1) {
						problems.add(questId + ": 报告块源 " + report.source() + " 的规范交付边 "
							+ gated.size() + " 条（期望 1）");
						continue;
					}
					if (gated.getFirst().afterCommit().stream().noneMatch(action ->
							action instanceof AfterCommitAction.ShowQuestDialog page
								&& blockWindows.contains(page.dialogId()))) {
						problems.add(questId + ": 报告块源 " + report.source() + " 交付边缺档位奖励窗");
					}
				}
				for (QuestTransition transition : blockDefinition.transitions()) {
					boolean retiredPage = transition.afterCommit().stream().anyMatch(action ->
						action instanceof AfterCommitAction.ShowQuestDialog page
							&& (page.dialogId() == QuestDialogPage.SELECT5.id()
								|| page.dialogId() == QuestDialogPage.SELECT6.id()));
					if (retiredPage) {
						problems.add(questId + ": 系统发放交付段仍下发报告页 " + transition.sourceNode()
							+ "→" + transition.targetNode());
					}
				}
				continue;
			}
			if (flips.isEmpty() || competing
					|| !chainSteps.blocks(questId, "NPC_REPORT").isEmpty()) {
				continue;
			}
			var metadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
				randomRewards, nameIds);
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex, metadata, exits,
				summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			covered.add(questId);
			QuestDefinition definition = outcome.definition().definition();
			Set<Integer> windows = rewardWindowPages();
			for (RetailClientTalkChainSteps.RouteRecord flip : flips) {
				List<QuestTransition> keyed = definition.transitions().stream()
					.filter(transition -> Objects.equals(transition.sourceNode(), flip.source()))
					.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == flip.npcId() && isAction(talk, QuestDialogAction.QUEST_SELECT))
					.toList();
				if (keyed.size() != 1) {
					problems.add(questId + ": (源,交付NPC,31) 键交付边 " + keyed.size() + " 条（期望 1，"
						+ "层 A 同键覆盖或登记记录残留）src=" + flip.source() + " npc=" + flip.npcId());
					continue;
				}
				QuestTransition edge = keyed.getFirst();
				if (!"reward".equals(edge.targetNode())) {
					problems.add(questId + ": 交付边未落 reward（target=" + edge.targetNode() + "）");
				}
				if (edge.afterCommit().stream().noneMatch(action ->
						action instanceof AfterCommitAction.ShowQuestDialog page
							&& windows.contains(page.dialogId()))) {
					problems.add(questId + ": 交付边缺档位奖励窗（QE-028 查表）");
				}
				for (String token : flip.conditions().split(";")) {
					String trimmed = token.trim();
					if (trimmed.isEmpty() || trimmed.equals("-") || "START_ELIGIBLE".equals(trimmed)) {
						continue;
					}
					if (!carriedConditionPresent(edge, trimmed)) {
						problems.add(questId + ": 条件载荷未逐字承接 " + trimmed);
					}
				}
				for (String token : flip.actions().split(";")) {
					String trimmed = token.trim();
					if (trimmed.isEmpty() || trimmed.equals("-")) {
						continue;
					}
					if (!carriedActionPresent(edge, trimmed)) {
						problems.add(questId + ": 动作载荷未逐字承接 " + trimmed);
					}
				}
			}
			for (QuestTransition transition : definition.transitions()) {
				boolean retiredPage = transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT5.id()
							|| page.dialogId() == QuestDialogPage.SELECT6.id()));
				if (retiredPage) {
					problems.add(questId + ": 交付段仍下发旧报告页 " + transition.sourceNode()
						+ "→" + transition.targetNode());
				}
			}
			Map<String, QuestStatus> statusByLabel = new HashMap<>();
			definition.nodes().forEach(node -> statusByLabel.put(node.label(), node.projection().status()));
			boolean reopenCarrier = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() != null)
				.filter(transition -> statusByLabel.get(transition.sourceNode()) == QuestStatus.REWARD)
				.flatMap(transition -> transition.afterCommit().stream())
				.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog page
					&& windows.contains(page.dialogId()));
			if (!reopenCarrier) {
				problems.add(questId + ": 缺 reward 态奖励窗重开载体");
			}
		}
		final int checked = covered.size();
		final int controlCovered = control;
		final int blockCovered = blockTakenOver;
		final int dClassRows = dClassCovered;
		final int mixedRows = mixedCovered;
		assertTrue(problems.isEmpty(), () -> "S3c 交付段缺口：" + problems.stream().limit(20).toList()
			+ " (checked=" + checked + " control=" + controlCovered + ")");
		assertTrue(checked >= S3C_DELIVERY_ROW_FLOOR,
			() -> "S3c 交付段接管覆盖面回退：" + checked + " < " + S3C_DELIVERY_ROW_FLOOR + " 行集=" + covered);
		assertTrue(checked <= S3C_DELIVERY_ROW_CEILING,
			() -> "S3c 交付段接管越界（切片边界漂移）：" + checked + " > " + S3C_DELIVERY_ROW_CEILING
				+ " 行集=" + covered);
		assertTrue(controlCovered >= S3C_CONTROL_ROW_FLOOR,
			() -> "S3c 竞争翻面对照面回退：" + controlCovered + " < " + S3C_CONTROL_ROW_FLOOR);
		assertTrue(blockCovered >= S3C_BLOCK_PATH_ROW_FLOOR,
			() -> "S3c 系统发放块路径覆盖回退：" + blockCovered + " < " + S3C_BLOCK_PATH_ROW_FLOOR);
		assertTrue(dClassRows >= S3C_D_ROW_FLOOR,
			() -> "S3c-D (iii) 就地加窗覆盖回退：" + dClassRows + " < " + S3C_D_ROW_FLOOR);
		assertTrue(dClassRows <= S3C_D_ROW_CEILING,
			() -> "S3c-D 切片越界：" + dClassRows + " > " + S3C_D_ROW_CEILING);
		assertTrue(mixedRows >= S3C_MIXED_ROW_FLOOR,
			() -> "S3c-D mixed 子面回退：" + mixedRows + " < " + S3C_MIXED_ROW_FLOOR);
	}

	/**
	 * S3c-obj 不变量（判例 1323；五轴谓词全量普查实测**恰 1 行**）：物件哨兵接取者的接取段按**物件梯**
	 * 合成——入口 `USE_OBJECT` 自环下发入口页 `select1`、中转 `ASK_QUEST_ACCEPT` 下发接取窗（页 4）、
	 * 提交 `QUEST_ACCEPT_1` 落 `started` 且**承载真端 `give_item`**、拒绝 `QUEST_REFUSE_1` 下发拒绝页、
	 * 关窗出口 `CLOSE`。客户端的物件三页（入口页 1011 / 接取窗 4 / 拒绝页 1004）逐页可达；入口自环
	 * **零载荷**（自环无门且可重复 ⇒ 重复用物件叠加任务物品）；边集与该物件登记记录**逐键相等**
	 * （合成多一条 = 发明客户端不会发的按钮，少一条 = 静默丢出口——编译期合同守卫兜底，此处独立重算）。
	 * S3c-obj: the object-sentinel accept ladder keeps the client's three pages, moves the retail
	 * give_item onto the commit edge, and leaves the repeatable entry self loop payload-free.
	 */
	@Test
	void s3cObjectEntryRowsKeepTheClientLadderAndMoveTheGiveToTheCommit() throws Exception {
		List<String> problems = new ArrayList<>();
		Set<Integer> covered = new TreeSet<>();
		for (int questId : new TreeSet<>(chainSteps.questIds())) {
			if (!"RETAIL_TABLE".equals(retentionOwners.get(questId))) {
				continue;
			}
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty() || entry.get().grantKind().systemGrant()
					|| !chainSteps.blocks(questId, "NPC_START").isEmpty()) {
				continue;
			}
			Set<Integer> acquired = npcIndex.resolveAll(List.of(
				entry.get().acquiredNpc() == null ? "" : entry.get().acquiredNpc())).npcIds();
			if (acquired.size() != 1) {
				continue;
			}
			int objectNpc = acquired.iterator().next();
			boolean entryRoute = chainSteps.routes(questId).stream().anyMatch(route -> route.npcId() == objectNpc
				&& "unaccepted".equals(route.source()) && "USE_OBJECT".equals(route.action())
				&& !pushedPages(route.afterCommits()).isEmpty());
			boolean relayRoute = chainSteps.routes(questId).stream().anyMatch(route -> route.npcId() == objectNpc
				&& "unaccepted".equals(route.source()) && "ASK_QUEST_ACCEPT".equals(route.action())
				&& !pushedPages(route.afterCommits()).isEmpty());
			boolean commitRoute = chainSteps.routes(questId).stream().anyMatch(route -> route.npcId() == objectNpc
				&& "unaccepted".equals(route.source()) && "started".equals(route.target())
				&& route.action().startsWith("QUEST_ACCEPT"));
			if (!entryRoute || !relayRoute || !commitRoute) {
				continue;
			}
			var metadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex, randomRewards,
				nameIds);
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex, metadata, exits,
				summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 物件接取行被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			covered.add(questId);
			QuestDefinition definition = outcome.definition().definition();
			List<QuestTransition> objectEdges = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == objectNpc && talk.dialogId() != null)
				.toList();
			Set<String> emittedKeys = new TreeSet<>();
			for (QuestTransition edge : objectEdges) {
				emittedKeys.add(edge.sourceNode() + ":"
					+ ((QuestEvent.TalkToNpc) edge.event()).dialogId());
			}
			Set<String> declaredKeys = new TreeSet<>();
			for (RetailClientTalkChainSteps.RouteRecord route : chainSteps.routes(questId)) {
				if (route.npcId() != objectNpc || !"TALK".equals(route.eventType())) {
					continue;
				}
				QuestDialogAction action;
				try {
					action = QuestDialogAction.valueOf(route.action());
				} catch (IllegalArgumentException e) {
					problems.add(questId + ": 物件梯含非枚举动作 " + route.action());
					continue;
				}
				declaredKeys.add(route.source() + ":" + action.id());
			}
			if (!emittedKeys.equals(declaredKeys)) {
				problems.add(questId + ": 物件梯键集漂移 emitted=" + emittedKeys + " declared=" + declaredKeys);
			}
			// 三页可达：入口页 / 接取窗 / 拒绝页各由**恰一条**物件边下发（多一条即入口页被重复下发）。
			for (QuestDialogPage page : List.of(QuestDialogPage.SELECT1,
					QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW, QuestDialogPage.QUEST_REFUSE_1)) {
				long pushes = objectEdges.stream().filter(edge -> edge.afterCommit()
					.contains(new AfterCommitAction.ShowQuestDialog(page.id()))).count();
				if (pushes != 1) {
					problems.add(questId + ": 物件页 " + page + " 下发边数 " + pushes + "（期望 1）");
				}
			}
			QuestTransition entryEdge = objectEdges.stream()
				.filter(edge -> ((QuestEvent.TalkToNpc) edge.event()).dialogId()
					== QuestDialogAction.USE_OBJECT.id())
				.findFirst().orElse(null);
			QuestTransition commitEdge = objectEdges.stream()
				.filter(edge -> ((QuestEvent.TalkToNpc) edge.event()).dialogId()
					== QuestDialogAction.QUEST_ACCEPT_1.id())
				.findFirst().orElse(null);
			if (entryEdge == null || commitEdge == null) {
				problems.add(questId + ": 物件梯缺入口边或提交边");
				continue;
			}
			if (!entryEdge.actions().isEmpty()) {
				problems.add(questId + ": 物件入口自环仍带载荷 " + entryEdge.actions()
					+ "（可重复用物件 ⇒ 叠加发物）");
			}
			List<QuestAction> expectedGive = expectedObjectAcceptGive(entry.get());
			if (expectedGive.isEmpty()) {
				problems.add(questId + ": 物件接取行无真端 give_item（提交边无物可发，须交裁定）");
			} else if (!commitEdge.actions().equals(expectedGive)) {
				problems.add(questId + ": 提交边载荷 " + commitEdge.actions() + " != 真端 give_item " + expectedGive);
			}
			if (!commitEdge.conditions().equals(List.of(new QuestCondition.StartEligible()))) {
				problems.add(questId + ": 提交边缺 StartEligible 门 " + commitEdge.conditions());
			}
			if (!"started".equals(commitEdge.targetNode())) {
				problems.add(questId + ": 提交边未落接取目标 " + commitEdge.targetNode());
			}
			// 零残留：物件接取行不得同时登记 NPC 形入口（31）——两形并立会让入口动作分家。
			for (QuestTransition transition : definition.transitions()) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == objectNpc
						&& talk.dialogId() != null && "unaccepted".equals(transition.sourceNode())
						&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()) {
					problems.add(questId + ": 物件接取行仍登记 NPC 形入口（31）");
				}
			}
		}
		final int rows = covered.size();
		assertTrue(problems.isEmpty(), () -> "S3c-obj 物件接取缺口：" + problems.stream().limit(20).toList()
			+ " (covered=" + rows + ")");
		assertTrue(rows >= S3C_OBJECT_ROW_FLOOR,
			() -> "物件接取变体覆盖面回退：" + rows + " < " + S3C_OBJECT_ROW_FLOOR + " 行集=" + covered);
		assertTrue(rows <= S3C_OBJECT_ROW_CEILING,
			() -> "物件接取切片越界：" + rows + " > " + S3C_OBJECT_ROW_CEILING + " 行集=" + covered);
	}

	/** S3c-obj：真端 `give_item` 符号 → 接取发物（与合成器同通道：work item 首项 + 符号列计数）。 /
	 * The retail give_item symbol resolved through the work-item channel. */
	private static List<QuestAction> expectedObjectAcceptGive(RetailSimpleTalkTable.Entry entry) {
		String symbol = entry.giveItemSymbol();
		if (symbol == null || symbol.isBlank()) {
			return List.of();
		}
		Integer itemId = RetailQuestWorkItems.first(symbol, entry.questId());
		if (itemId == null) {
			return List.of();
		}
		String[] parts = symbol.trim().split("\\s+");
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		return List.of(new QuestAction.GiveItem(itemId, count));
	}

	/** 承接条件 token 是否**逐字**落在交付边上（fail-closed：未知 token 一律算未承接）。 /
	 * Whether the carried condition token is verbatim present on the edge. */
	private static boolean carriedConditionPresent(QuestTransition edge, String token) {
		if (token.startsWith("HAS_ITEM:")) {
			String[] parts = token.substring("HAS_ITEM:".length()).split(":");
			return edge.conditions().contains(new QuestCondition.HasItem(
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true));
		}
		if (token.startsWith("VAR_IS:")) {
			String[] kv = token.substring("VAR_IS:".length()).split("=");
			return edge.conditions().contains(new QuestCondition.QuestVariableIs(
				kv[0], Integer.parseInt(kv[1])));
		}
		return false;
	}

	/** 承接动作 token 是否**逐字**落在交付边上（fail-closed：未知 token 一律算未承接）。 /
	 * Whether the carried action token is verbatim present on the edge. */
	private static boolean carriedActionPresent(QuestTransition edge, String token) {
		if (token.startsWith("REMOVE_ITEM:")) {
			String[] parts = token.substring("REMOVE_ITEM:".length()).split(":");
			return edge.actions().contains(new QuestAction.RemoveItem(
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
		}
		if (token.startsWith("SET_VAR:")) {
			String[] kv = token.substring("SET_VAR:".length()).split("=");
			return edge.actions().contains(new QuestAction.SetVariable(kv[0], Integer.parseInt(kv[1])));
		}
		if (token.startsWith("GIVE_ITEM:")) {
			String[] parts = token.substring("GIVE_ITEM:".length()).split(":");
			return edge.actions().contains(new QuestAction.GiveItem(
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
		}
		return false;
	}

	/** S3a/S3b：接取块的 selectionSources 是否可接管（段内恒可；段外仅当登记表在该源上**没有**
	 *  {@code FINISH_DIALOG} 记录时放行），与合成器同源。 */
	private static boolean acceptSourcesWithinSegment(int questId, RetailClientTalkChainSteps.BlockRecord start) {
		for (String source : start.extra().split("\\|", -1)[0].trim().split("\\s+")) {
			if (source.isEmpty() || source.equals("-") || source.equals("unaccepted")
					|| source.equals(start.target())) {
				continue;
			}
			boolean served = chainSteps.routes(questId).stream().anyMatch(route -> route.npcId() == start.npcId()
				&& "FINISH_DIALOG".equals(route.action()) && source.equals(route.source()));
			if (served) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 守卫加固片（G-1..G-4）不变量：对已接管段的行独立重算四条性质——G-1 退场载荷（条件侧 + **动作侧**）
	 * 必须被交付边的门覆盖（扣物静默消失即红）；G-2 交付 NPC 上不得有**领奖前置态**（源节点投影 START）的
	 * `1009/39/20002` 中转（REWARD 态同名路由是 QE-083 重开预览语义，允许）；G-3 reward 态必须有奖励窗
	 * 重开载体；G-4 退场的阶段门必须被规范边源节点投影蕴含。下限断言防谓词失配导致不变量空转。
	 * Hardened guard invariants, recomputed independently from the registry fixtures.
	 */
	@Test
	void hardenedGuardsHoldAcrossCanonicalRows() throws Exception {
		List<String> problems = new ArrayList<>();
		int checked = 0;
		int deliveryChecked = 0;
		int stageGateRoutes = 0;
		for (int questId : new TreeSet<>(chainSteps.questIds())) {
			if (!"RETAIL_TABLE".equals(retentionOwners.get(questId))) {
				continue;
			}
			List<RetailClientTalkChainSteps.BlockRecord> starts = chainSteps.blocks(questId, "NPC_START");
			if (starts.isEmpty() || starts.stream().anyMatch(block -> !acceptSourcesWithinSegment(questId, block))) {
				continue;
			}
			List<RetailClientTalkChainSteps.BlockRecord> reports = chainSteps.blocks(questId, "NPC_REPORT");
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty() || entry.get().grantKind().systemGrant()) {
				continue;
			}
			var metadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
				randomRewards, nameIds);
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex, metadata, exits,
				summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			checked++;
			QuestDefinition definition = outcome.definition().definition();
			Map<String, QuestStatus> statusByLabel = new HashMap<>();
			definition.nodes().forEach(node -> statusByLabel.put(node.label(), node.projection().status()));
			Set<String> canonicalSources = new TreeSet<>();
			canonicalSources.add("unaccepted");
			reports.forEach(block -> canonicalSources.add(block.source()));
			Set<Integer> reportNpcs = new TreeSet<>();
			reports.forEach(block -> reportNpcs.add(block.npcId()));
			reportNpcs.addAll(rewardNpcs(entry.get()));
			// G-4：退场的阶段门必须被规范边源节点投影蕴含（否则门消失 ⇒ 领奖提前且审计看不见）。
			for (RetailClientTalkChainSteps.RouteRecord route : retiredRoutesOf(questId, reportNpcs)) {
				for (String token : route.conditions().split(";")) {
					String trimmed = token.trim();
					if (!stageGateToken(trimmed)) {
						continue;
					}
					stageGateRoutes++;
					if (!impliedByCanonicalSource(trimmed, canonicalSources, definition)) {
						problems.add(questId + ": 退场阶段门未被规范边源节点蕴含 " + trimmed);
					}
				}
			}
			if (reports.isEmpty()) {
				continue;
			}
			deliveryChecked++;
			List<QuestTransition> delivered = definition.transitions().stream()
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& isAction(talk, QuestDialogAction.QUEST_SELECT))
				.filter(transition -> reports.stream()
					.anyMatch(block -> block.source().equals(transition.sourceNode())))
				.toList();
			// G-1：退场载荷（条件侧 + 动作侧）必须被交付边的门覆盖——扣物静默消失即红。
			for (QuestItemRequirement item : retiredPayload(reportNpcs, questId)) {
				boolean covered = delivered.stream().anyMatch(transition ->
					transition.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.HasItem has
						&& has.itemId() == item.itemId() && has.count() >= item.count())
						|| transition.actions().stream().anyMatch(action -> action instanceof QuestAction.RemoveItem remove
							&& remove.itemId() == item.itemId() && remove.count() >= item.count()));
				if (!covered) {
					problems.add(questId + ": 退场载荷未被交付边覆盖 item=" + item.itemId() + ":" + item.count());
				}
			}
			// G-2：交付 NPC 上不得有领奖前置态的 1009/39/20002 中转。
			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.dialogId() == null) {
					continue;
				}
				QuestStatus source = transition.sourceNode() == null
					? null : statusByLabel.get(transition.sourceNode());
				if (source == QuestStatus.START && reportNpcs.contains(talk.npcId())
						&& (talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()
							|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
							|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())) {
					problems.add(questId + ": 领奖前置态仍带交付中转 " + transition.sourceNode()
						+ " npc " + talk.npcId() + " dialog " + talk.dialogId());
				}
			}
			// G-3：reward 态必须有奖励窗重开载体（领奖后关窗可重开）。
			Set<Integer> windows = rewardWindowPages();
			boolean reopenCarrier = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() != null)
				.filter(transition -> statusByLabel.get(transition.sourceNode()) == QuestStatus.REWARD)
				.flatMap(transition -> transition.afterCommit().stream())
				.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog page
					&& windows.contains(page.dialogId()));
			if (!reopenCarrier) {
				problems.add(questId + ": 缺 reward 态奖励窗重开载体");
			}
		}
		final int covered = checked;
		final int deliveryCovered = deliveryChecked;
		final int gates = stageGateRoutes;
		assertTrue(problems.isEmpty(), () -> "守卫加固缺口：" + problems.stream().limit(20).toList()
			+ " (checked=" + covered + " delivery=" + deliveryCovered + " stageGates=" + gates + ")");
		assertTrue(covered >= CANONICAL_SEGMENT_ROW_FLOOR,
			() -> "守卫加固覆盖面回退：" + covered + " < " + CANONICAL_SEGMENT_ROW_FLOOR);
		assertTrue(deliveryCovered >= 200,
			() -> "交付段覆盖面回退：" + deliveryCovered + " < 200（谓词失配 = 不变量空转）");
	}

	/**
	 * S2 不变量：链式过场行恰一部电影——movieId = 真端 cutsceneid1、类型 CUTSCENE、落点由触发轴决定：
	 * 1007/1009 触发重挂到规范段的 {@code QUEST_SELECT} 边；页触发（1353/1694 等）留在登记记录的
	 * 同号动作边上（页链未退场，落点不动）。下限断言防谓词失配导致不变量空转。
	 * S2: chain cutscene rows carry exactly one retail movie; the carrier follows the trigger axis.
	 */
	@Test
	void chainCutsceneRowsCarryExactlyOneRetailMovie() throws Exception {
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (int questId : new TreeSet<>(chainSteps.questIds())) {
			if (!"RETAIL_TABLE".equals(retentionOwners.get(questId))) {
				continue;
			}
			var entry = table.find(questId);
			var meta = retailTable.find(questId);
			if (entry.isEmpty() || meta.isEmpty() || !entry.get().cutscene()) {
				continue;
			}
			var metadata = RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex,
				randomRewards, nameIds);
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex, metadata, exits,
				summaryRows, clientRewardNpcs, chainSteps);
			if (!outcome.accepted()) {
				problems.add(questId + ": 被拒绝 " + outcome.rejectionCode());
				continue;
			}
			checked++;
			QuestDefinition definition = outcome.definition().definition();
			List<QuestTransition> carriers = definition.transitions().stream().filter(transition ->
				transition.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.PlayMovie))
				.toList();
			List<AfterCommitAction.PlayMovie> movies = carriers.stream()
				.flatMap(transition -> transition.afterCommit().stream())
				.filter(action -> action instanceof AfterCommitAction.PlayMovie)
				.map(action -> (AfterCommitAction.PlayMovie) action).toList();
			if (movies.size() != 1) {
				problems.add(questId + ": PlayMovie 数 " + movies.size() + "（期望 1）");
				continue;
			}
			AfterCommitAction.PlayMovie movie = movies.get(0);
			if (movie.movieId() != entry.get().cutsceneMovieId()) {
				problems.add(questId + ": movieId=" + movie.movieId() + " != 真端 cutsceneid1="
					+ entry.get().cutsceneMovieId());
			}
			if (movie.type() != QuestMovieType.CUTSCENE) {
				problems.add(questId + ": 影片类型 " + movie.type() + " != CUTSCENE");
			}
			int trigger = entry.get().cutsceneTrigger();
			if (trigger == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					|| trigger == QuestDialogAction.SELECT_QUEST_REWARD.id()) {
				if (!(carriers.get(0).event() instanceof QuestEvent.TalkToNpc carrier)
						|| !isAction(carrier, QuestDialogAction.QUEST_SELECT)) {
					problems.add(questId + ": 触发轴 " + trigger + " 的过场必须挂在规范段 QUEST_SELECT 边上（"
						+ carriers.get(0).event() + "）");
				}
			} else if (!(carriers.get(0).event() instanceof QuestEvent.TalkToNpc carrier)
					|| carrier.dialogId() == null || carrier.dialogId() != trigger) {
				problems.add(questId + ": 页触发 " + trigger + " 的过场必须留在同号动作边上（"
					+ carriers.get(0).event() + "）");
			}
		}
		final int coveredMovies = checked;
		assertTrue(problems.isEmpty(), () -> "链式过场不变量缺口：" + problems.stream().limit(20).toList()
			+ " (checked=" + coveredMovies + ")");
		assertTrue(coveredMovies >= CHAIN_CUTSCENE_ROW_FLOOR,
			() -> "链式过场行覆盖回退：" + coveredMovies + " < " + CHAIN_CUTSCENE_ROW_FLOOR
				+ "（谓词失配 = 不变量空转）");
	}

	/** 不变量 2+3：retention 精确二分 + 登记形状下限。 */
	@Test
	void registryPartitionsIntoAdoptAndKeepWithoutOrphans() {
		Set<Integer> registryIds = new TreeSet<>(chainSteps.questIds());
		Set<Integer> adoptIds = new TreeSet<>(frozenFingerprints.keySet());
		Set<Integer> keepIds = new TreeSet<>();
		List<String> problems = new ArrayList<>();
		for (int questId : registryIds) {
			String owner = retentionOwners.get(questId);
			if (owner == null) {
				problems.add(questId + ": 登记行没有保留清单行（孤儿）");
			} else if ("RETAIL_TABLE".equals(owner)) {
				if (!adoptIds.contains(questId)) {
					problems.add(questId + ": RETAIL_TABLE 但不在冻结指纹里");
				}
			} else if ("XML_RETENTION".equals(owner)) {
				keepIds.add(questId);
			} else {
				problems.add(questId + ": 未知的保留 owner " + owner);
			}
		}
		for (int questId : adoptIds) {
			if (!registryIds.contains(questId)) {
				problems.add(questId + ": 冻结指纹行已不在登记表（孤儿指纹）");
			}
		}
		assertTrue(problems.isEmpty(), () -> "链式登记分区缺口：" + problems.stream().limit(10).toList());
		for (int questId : adoptIds) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> questId + ": ADOPT 行必须已退役（XML 已删）");
			assertTrue(chainSteps.layout(questId).isPresent(), () -> questId + ": 缺布局记录");
			assertFalse(chainSteps.nodes(questId).isEmpty(), () -> questId + ": 缺节点记录");
			// wave B-1 残组行（11069/18210/26990/28210）：XML 只有规范块、无显式路由——
			// 纯规范合成即真端形状，允许零路由，但必须有 NPC_START 块背书。
			boolean routed = !chainSteps.routes(questId).isEmpty();
			boolean canonicalOnly = chainSteps.block(questId, "NPC_START").isPresent();
			assertTrue(routed || canonicalOnly,
				() -> questId + ": ADOPT 行必须有路由或规范块（残组形状）");
		}
		for (int questId : keepIds) {
			assertFalse(RetiredQuestIds.contains(questId),
				() -> questId + ": KEEP 行必须仍由 XML 拥有（未退役）");
		}
	}

	/** 合成定义的节点投影必须逐一等于登记 N 记录（label, status, var0）。 */
	private static String nodeProjectionProblem(int questId, QuestDefinition definition) {
		Map<String, QuestNode> byLabel = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			byLabel.put(node.label(), node);
		}
		List<RetailClientTalkChainSteps.NodeRecord> expected = chainSteps.nodes(questId);
		if (byLabel.size() != expected.size()) {
			return questId + ": 节点数 " + byLabel.size() + " != 登记 " + expected.size();
		}
		for (var record : expected) {
			QuestNode node = byLabel.get(record.label());
			if (node == null) {
				return questId + ": 缺登记节点 " + record.label();
			}
			QuestStatus status = QuestStatus.valueOf(record.status());
			if (node.projection().status() != status) {
				return questId + ": 节点 " + record.label() + " 状态 " + node.projection().status()
					+ " != 登记 " + record.status();
			}
			Integer var0 = node.projection().variables().get("var0");
			if (record.var0() == null) {
				if (var0 != null) {
					return questId + ": 节点 " + record.label() + " 应为 varless（登记 var0='-'）实际 "
						+ var0;
				}
			} else if (var0 == null || var0.intValue() != record.var0()) {
				return questId + ": 节点 " + record.label() + " var0=" + var0 + " != 登记 " + record.var0();
			}
		}
		return null;
	}

	/** 交付 NPC 集：唯一名解析优先（与合成器同规则）。 / Resolved reward NPCs. */
	private static Set<Integer> rewardNpcs(RetailSimpleTalkTable.Entry entry) {
		return rewardNpcScope(entry);
	}

	/** 真端 reward_npc_name 解析集；空集时回落客户端交付 NPC 登记（与合成器同源，S3c 判例 35024/45024）。 /
	 * Retail reward-name resolution, falling back to the client-declared reward NPCs when empty. */
	private static Set<Integer> rewardNpcScope(RetailSimpleTalkTable.Entry entry) {
		Set<Integer> resolved = npcIndex.resolveAll(
			List.of(entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
		return resolved.isEmpty()
			? new TreeSet<>(clientRewardNpcs.rewardNpcs(entry.questId())) : resolved;
	}

	/** S3c：交付段接管判据（A 形翻面记录 + 无竞争翻面）的测试侧独立复算。 / S3c delivery-takeover predicate. */
	private static boolean deliveryTakenOverByA(RetailSimpleTalkTable.Entry entry) {
		if (!chainSteps.blocks(entry.questId(), "NPC_REPORT").isEmpty()) {
			return false;
		}
		Set<Integer> scope = rewardNpcScope(entry);
		Set<String> startStates = startStateLabels(entry.questId());
		return aShapedFlips(entry, scope, startStates).size() > 0
			&& competingFlips(entry, scope, startStates).isEmpty();
	}

	/** S3c-D 面派生（与合成器同源）：非 A 形行里落在交付作用域之外的翻面记录键。 */
	private static Set<String> dClassWindowedFlipKeys(RetailSimpleTalkTable.Entry entry) {
		Set<String> startStates = startStateLabels(entry.questId());
		if (!aShapedFlips(entry, rewardNpcScope(entry), startStates).isEmpty()
				|| !chainSteps.blocks(entry.questId(), "NPC_REPORT").isEmpty()) {
			// A 形行由本不变量主臂覆盖；报告块行（判例 39003/49003）交付段已由块接管 ⇒ 中间人翻面逐字保留。
			return Set.of();
		}
		Set<Integer> scope = rewardNpcScope(entry);
		Set<String> keys = new TreeSet<>();
		for (RetailClientTalkChainSteps.RouteRecord route : chainSteps.routes(entry.questId())) {
			if ("reward".equals(route.target()) && startStates.contains(route.source())
					&& A_SHAPED_ACTIONS.contains(route.action()) && !scope.contains(route.npcId())) {
				keys.add(route.source() + ":" + route.npcId() + ":" + route.action());
			}
		}
		return keys;
	}

	/** S3c-D 报告页退场作用域：有 reward 入边翻面记录（任一作用域）且无 NPC_REPORT 块。 */
	private static boolean dClassReportRetire(RetailSimpleTalkTable.Entry entry) {
		if (!chainSteps.blocks(entry.questId(), "NPC_REPORT").isEmpty()) {
			return false;
		}
		Set<String> startStates = startStateLabels(entry.questId());
		boolean anyFlip = chainSteps.routes(entry.questId()).stream().anyMatch(route ->
			"reward".equals(route.target()) && startStates.contains(route.source())
				&& A_SHAPED_ACTIONS.contains(route.action()));
		return anyFlip || !dClassWindowedFlipKeys(entry).isEmpty();
	}

	/** 登记表 afterCommits 令牌的测试侧解码（只覆盖 `SYNC:*`/`CLOSE`；其余原样失败）。 */
	private static List<AfterCommitAction> expectedRegistryAfter(String afterCommits) {
		List<AfterCommitAction> actions = new ArrayList<>();
		for (String token : afterCommits.split(";")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if ("CLOSE".equals(trimmed)) {
				actions.add(new AfterCommitAction.CloseDialog());
			} else if (trimmed.startsWith("SYNC:")) {
				actions.add(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.valueOf(trimmed.substring("SYNC:".length()))));
			} else if (trimmed.startsWith("DIALOG:SHOW_SELECTION_PAGE:")) {
				actions.add(new AfterCommitAction.ShowQuestSelectionDialog(
					QuestDialogPage.valueOf(trimmed.substring("DIALOG:SHOW_SELECTION_PAGE:".length())).id()));
			} else if (trimmed.startsWith("DIALOG:SHOW_QUEST_PAGE:")) {
				actions.add(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.valueOf(trimmed.substring("DIALOG:SHOW_QUEST_PAGE:".length())).id()));
			} else {
				throw new IllegalStateException("unexpected registry after token: " + trimmed);
			}
		}
		return List.copyOf(actions);
	}

	/** A 形翻面记录：源节点投影 START、落在真端/客户端交付 NPC、动作属交付翻转族。 / A-shaped flip records. */
	private static List<RetailClientTalkChainSteps.RouteRecord> aShapedFlips(RetailSimpleTalkTable.Entry entry,
			Set<Integer> scope, Set<String> startStates) {
		return chainSteps.routes(entry.questId()).stream()
			.filter(route -> flipRoute(route, scope, startStates)).toList();
	}

	/** 竞争翻面：指向 reward 的 START 源翻转记录落在交付 NPC **之外**（D 类中间人 premature 形）。 /
	 * Competing flips: reward-bound START-source flips outside the delivery NPC scope. */
	private static List<RetailClientTalkChainSteps.RouteRecord> competingFlips(RetailSimpleTalkTable.Entry entry,
			Set<Integer> scope, Set<String> startStates) {
		return chainSteps.routes(entry.questId()).stream()
			.filter(route -> "reward".equals(route.target()) && startStates.contains(route.source())
				&& !scope.contains(route.npcId()) && A_SHAPED_ACTIONS.contains(route.action()))
			.toList();
	}

	private static Set<String> startStateLabels(int questId) {
		return chainSteps.nodes(questId).stream()
			.filter(node -> "START".equals(node.status()))
			.map(RetailClientTalkChainSteps.NodeRecord::label)
			.collect(java.util.stream.Collectors.toCollection(TreeSet::new));
	}

	private static boolean flipRoute(RetailClientTalkChainSteps.RouteRecord route, Set<Integer> scope,
			Set<String> startStates) {
		return "reward".equals(route.target()) && startStates.contains(route.source())
			&& scope.contains(route.npcId()) && A_SHAPED_ACTIONS.contains(route.action());
	}
	/** A 形交付翻面动作族（与合成器同口径的测试侧副本）。 / A-shaped flip action family. */
	private static final Set<String> A_SHAPED_ACTIONS = Set.of("SELECT_QUEST_REWARD", "SET_SUCCEED",
		"SETPRO1", "SETPRO2", "SETPRO3");

	private static boolean isAction(QuestEvent.TalkToNpc talk, QuestDialogAction action) {
		return talk.dialogId() != null && talk.dialogId() == action.id();
	}

	/** S2 交付门独立重算：item_check 三形（元数据 / carried work-items / 退场记录载荷）。 */
	private static List<QuestItemRequirement> expectedChainGate(RetailSimpleTalkTable.Entry entry,
			QuestMetadata metadata, Set<Integer> reportNpcs, int questId) {
		if (!entry.itemCheck()) {
			return List.of();
		}
		List<QuestItemRequirement> reportItems = metadata.itemRequirements().isEmpty()
			? carriedWorkItems(metadata, questId) : metadata.itemRequirements();
		return reportItems.isEmpty() ? retiredPayload(reportNpcs, questId) : reportItems;
	}

	/**
	 * carried work-items（与合成器同规则、异路径）：{@code quest_work_items} ∩ 段内授予 − 段内移除
	 * （reward/complete 端的记录不参与——它们的扣物属于领奖/完成段）。
	 * Carried work items: the work-item domain intersected with the grants minus the removals of the
	 * progression segments (reward/complete-side records do not participate).
	 */
	private static List<QuestItemRequirement> carriedWorkItems(QuestMetadata metadata, int questId) {
		Map<Integer, Integer> granted = new TreeMap<>();
		Map<Integer, Integer> removed = new TreeMap<>();
		for (RetailClientTalkChainSteps.BlockRecord start : chainSteps.blocks(questId, "NPC_START")) {
			String[] extra = start.extra().split("\\|");
			if (extra.length > 2) {
				for (String token : extra[2].split(";")) {
					String[] parts = token.trim().split(":");
					if (parts.length == 3 && "GIVE_ITEM".equals(parts[0])) {
						granted.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
					}
				}
			}
		}
		for (RetailClientTalkChainSteps.RouteRecord route : chainSteps.routes(questId)) {
			if ("reward".equals(route.source()) || "reward".equals(route.target())
					|| "complete".equals(route.target())) {
				continue;
			}
			for (String token : route.actions().split(";")) {
				String[] parts = token.trim().split(":");
				if (parts.length != 3) {
					continue;
				}
				if ("GIVE_ITEM".equals(parts[0])) {
					granted.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
				} else if ("REMOVE_ITEM".equals(parts[0])) {
					removed.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
				}
			}
		}
		List<QuestItemRequirement> carried = new ArrayList<>();
		for (QuestItemRequirement item : metadata.questWorkItems()) {
			int count = Math.min(item.count(), granted.getOrDefault(item.itemId(), 0))
				- removed.getOrDefault(item.itemId(), 0);
			if (count > 0) {
				carried.add(new QuestItemRequirement(item.itemId(), count));
			}
		}
		return List.copyOf(carried);
	}

	/** 退场记录集（测试侧独立重算：词表 × 段 NPC 作用域，与合成器同规则）。 / Retired routes. */
	private static List<RetailClientTalkChainSteps.RouteRecord> retiredRoutesOf(int questId,
			Set<Integer> reportNpcs) {
		Set<Integer> acceptNpcs = new TreeSet<>();
		chainSteps.blocks(questId, "NPC_START").forEach(block -> acceptNpcs.add(block.npcId()));
		List<RetailClientTalkChainSteps.RouteRecord> retired = new ArrayList<>();
		for (RetailClientTalkChainSteps.RouteRecord route : chainSteps.routes(questId)) {
			boolean matched = (acceptNpcs.contains(route.npcId())
					&& (CHAIN_ACCEPT_RETIRED.contains(route.action())
						|| ("SETPRO1".equals(route.action()) && "unaccepted".equals(route.source()))))
				|| (reportNpcs.contains(route.npcId())
					&& (CHAIN_DELIVERY_RETIRED.contains(route.action())
						|| !java.util.Collections.disjoint(pushedPages(route.afterCommits()),
							CHAIN_RETIRED_PAGES)));
			if (matched) {
				retired.add(route);
			}
		}
		return retired;
	}

	/**
	 * 退场记录承接的门（测试侧独立重算：词表 × 段 NPC 作用域；**条件侧 + 动作侧**——G-1 后与合成器同轴）。
	 * Payload carried by retired routes, condition side and action side.
	 */
	private static List<QuestItemRequirement> retiredPayload(Set<Integer> reportNpcs, int questId) {
		Map<Integer, Integer> required = new TreeMap<>();
		for (RetailClientTalkChainSteps.RouteRecord route : retiredRoutesOf(questId, reportNpcs)) {
			for (String token : route.conditions().split(";")) {
				String[] parts = token.trim().split(":");
				if (parts.length == 3 && "HAS_ITEM".equals(parts[0])) {
					required.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
				}
			}
			for (String token : route.actions().split(";")) {
				String[] parts = token.trim().split(":");
				if (parts.length == 3 && "REMOVE_ITEM".equals(parts[0])) {
					required.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
				}
			}
		}
		return required.entrySet().stream()
			.map(item -> new QuestItemRequirement(item.getKey(), item.getValue())).toList();
	}

	/** G-4：该条件令牌是否为阶段门（`VAR_IS` / `VAR_AT_LEAST`）。 / Whether the token is a stage gate. */
	private static boolean stageGateToken(String token) {
		return token.startsWith("VAR_IS:") || token.startsWith("VAR_AT_LEAST:");
	}

	/** G-4：该阶段门是否被集合中某个节点的投影蕴含（与合成器同判据，异路径重算）。 /
	 * Whether some canonical source node's projection implies the stage gate. */
	private static boolean impliedByCanonicalSource(String token, Set<String> canonicalSources,
			QuestDefinition definition) {
		boolean atLeast = token.startsWith("VAR_AT_LEAST:");
		String[] kv = token.substring(token.indexOf(':') + 1).split("=");
		if (kv.length != 2) {
			return false;
		}
		int bound = Integer.parseInt(kv[1]);
		for (String source : canonicalSources) {
			Integer actual = definition.nodes().stream()
				.filter(node -> node.label().equals(source))
				.map(node -> node.projection().variables().get(kv[0]))
				.findFirst().orElse(null);
			if (actual != null && (atLeast ? actual >= bound : actual == bound)) {
				return true;
			}
		}
		return false;
	}

	/** G-3：六档奖励窗页 id 集（查表口径，与交付窗选用同源）。 / The six tiered reward window page ids. */
	private static Set<Integer> rewardWindowPages() {
		Set<Integer> pages = new TreeSet<>();
		for (int tier = 0; tier < 6; tier++) {
			QuestDialogPage.rewardWindowForTier(tier).ifPresent(page -> pages.add(page.id()));
		}
		return pages;
	}

	/** 记录下发的任务页常量集（只认 {@code DIALOG:SHOW_QUEST_PAGE:} 通道）。 / Quest page pushes. */
	private static Set<String> pushedPages(String afterCommits) {
		Set<String> pages = new java.util.HashSet<>();
		for (String token : afterCommits.split(";")) {
			String trimmed = token.trim();
			if (trimmed.startsWith("DIALOG:SHOW_QUEST_PAGE:")) {
				pages.add(trimmed.substring("DIALOG:SHOW_QUEST_PAGE:".length()));
			}
		}
		return pages;
	}

	private static Map<String, Integer> randomRewardIds() throws Exception {
		Map<String, Integer> ids = new HashMap<>();
		var document = parse(open("/aion/data/static_data/quest_random_rewards.xml"));
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
		for (String line : lines(open("/aion/data/static_data/quest_retail/quest_name_string_ids.tsv"))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())),
				Integer.parseInt(parts[1]));
		}
		return ids;
	}

	private static List<String> listXmlNames(String dir) throws Exception {
		var url = RetailSimpleTalkChainGateTest.class.getResource(dir);
		assertNotNull(url, "missing dir " + dir);
		java.io.File[] files = new java.io.File(url.toURI()).listFiles((d, name) -> name.endsWith(".xml"));
		assertNotNull(files);
		return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws Exception {
		List<InputStream> inputs = new ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	private static org.w3c.dom.Document parse(InputStream input) throws Exception {
		try (input) {
			var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(input);
		}
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private static InputStream open(String resource) throws Exception {
		InputStream input = RetailSimpleTalkChainGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
