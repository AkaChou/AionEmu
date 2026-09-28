package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.definition.QuestXmlFixtures;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleCollectItem 真端驱动门禁（M5-b2 起）。判据是<b>真端语义 + 客户端契约</b>，不是"必须与历史 XML 等价"：
 * XML 自身带历史错误（用户口径 2026-09-23），只有在真端表达不了时才保留。
 * <p>
 * 数据来源三层：真端模板表 {@code Quest_SimpleCollectItem.xml}（接取/对象/报告 NPC）、真端 {@code quest.xml}
 * 元数据（交付物、奖励）、客户端契约登记（{@code quest_client_dialog_exits.tsv} 的 SELECT1_1/SELECT6、
 * {@code quest_client_summary_rows.tsv} 的任务书末行——真端表本身没有这两列）。
 * <p>
 * 断言四件事：
 * <ol>
 * <li><b>家族规模与可驱动下限</b>：真端表 ∩ 生产宇宙的规模、可驱动数量不回退；</li>
 * <li><b>真端语义不变量</b>：接取/SETPRO1/报告页/交付检查对（39 与 20002）/完成区间/交付失败页/续页；</li>
 * <li><b>漂移登记</b>：与历史 XML 的逐任务差异必须与 {@code retail-simple-collect-item-drift.tsv} 一致
 * （登记用于发现静默漂移与追溯 XML 缺陷，不是退役门槛）；</li>
 * <li><b>冻结 IR 指纹</b>：已退役任务用冻结指纹继续守等价证据，退役集合必须与保留清单一致。</li>
 * </ol>
 * Retail-semantics gate for the SimpleCollectItem family.
 */
class RetailSimpleCollectItemGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String ITEM_DIR = "/aion/data/static_data/items/item/";
	private static final String CATALOG = "/aion/data/static_data/quest_definition/quest_definition_catalog.xml";
	private static final String RETENTION = "/aion/data/static_data/quest_retail/retail-xml-retention.tsv";
	/** 与历史 XML 的漂移登记（quest_id, classification）。 / Drift registry versus the legacy XML. */
	private static final String DRIFT_REGISTRY = "/quest/retail-simple-collect-item-drift.tsv";
	/** 冻结 IR 指纹（退役后继续发现静默漂移）。 / Frozen IR fingerprints. */
	private static final String FINGERPRINTS = "/quest/retail-simple-collect-item-ir-fingerprints.tsv";
	/** 冻结的家族规模（真端表 ∩ 生产宇宙）。 / Frozen family size. */
	private static final int FROZEN_FAMILY_SIZE = 178;
	/** 可驱动（accepted）数量下限，防止静默回退。 / Floor for retail-drivable quests. */
	private static final int ACCEPTED_FLOOR = 178;
	/** 已退役子集（M5-b2/M5-b3/M5-b3x/P1a 批次 + P1b 可选奖励与多交付物行）。 / Retired subset frozen size. */
	private static final int FROZEN_RETIRED_SIZE = 175;
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailSimpleCollectItemTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> familyIds;
	private static Map<Integer, String> driftRegistry;
	private static Map<Integer, String> fingerprints;
	private static RetailClientDialogExits clientDialogExits;
	private static RetailClientSummaryRows clientSummaryRows;
	private static RetailClientRewardNpcs clientRewardNpcs;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest_retail/Quest_SimpleCollectItem.xml")) {
			table = RetailSimpleCollectItemTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		clientDialogExits = RetailClientDialogExits.defaultExits();
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv")) {
			clientSummaryRows = RetailClientSummaryRows.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv")) {
			clientRewardNpcs = RetailClientRewardNpcs.load(input);
		}
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll(ITEM_DIR, listXmlNames(ITEM_DIR)));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		familyIds = familyIds();
		driftRegistry = driftRegistry();
		fingerprints = fingerprints();
	}

	@Test
	void familyScopeIsFrozen() {
		assertTrue(familyIds.size() == FROZEN_FAMILY_SIZE,
			() -> "SimpleCollectItem 家族规模漂移：期望 " + FROZEN_FAMILY_SIZE + " 实际 " + familyIds.size());
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
		assertTrue(problems.isEmpty(), () -> "SimpleCollectItem 真端语义缺口："
			+ problems.stream().limit(20).toList() + " (accepted=" + drivable + ")");
		assertTrue(drivable >= ACCEPTED_FLOOR, () -> "SimpleCollectItem 可驱动数量回退："
			+ drivable + " < " + ACCEPTED_FLOOR);
	}

	/**
	 * 与历史 XML 的差异逐条登记（登记是护栏，不是退役门槛）。
	 * <p>
	 * 已退役任务（XML 只在 git 历史里）不可再重算，登记行即冻结证据；仍由 XML 拥有的任务照旧逐条现算对拍。
	 * Per-quest drift registry; retired quests keep their frozen row as the evidence.
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
		String dump = System.getProperty("retail.collect.equivOut");
		if (dump != null) {
			Files.writeString(Path.of(dump), dumpText(classification));
		}
		assertTrue(driftRegistry.size() == FROZEN_FAMILY_SIZE,
			() -> "漂移登记行数漂移：" + driftRegistry.size());
		assertTrue(problems.isEmpty(), () -> "SimpleCollectItem 漂移登记失同步："
			+ problems.stream().limit(20).toList());
	}

	/** 冻结 IR 指纹：XML 删除后仍能发现静默漂移。 / Frozen fingerprints, the post-deletion drift guard. */
	@Test
	void frozenFingerprintsCoverExactlyTheRetiredQuests() throws Exception {
		String freezeOut = System.getProperty("retail.collect.fpOut");
		Map<Integer, String> actual = new TreeMap<>();
		int frozenRows = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				continue;
			}
			QuestDefinition definition = row.outcome().definition().definition();
			String digest = RetailIrFingerprint.fingerprint(definition);
			actual.put(questId, digest);
			if (freezeOut != null && RetiredQuestIds.contains(questId)) {
				frozenRows++;
			}
		}
		if (freezeOut != null) {
			StringBuilder text = new StringBuilder("# SimpleCollectItem 冻结 IR 指纹（XML 删除后继续守等价证据）\n"
				+ "# quest_id\tretail_fingerprint\tnodes\ttransitions\n");
			for (Map.Entry<Integer, String> entry : actual.entrySet()) {
				if (!RetiredQuestIds.contains(entry.getKey())) {
					continue;
				}
				QuestDefinition definition = retailRow(entry.getKey()).outcome().definition().definition();
				text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\t')
					.append(definition.nodes().size()).append('\t').append(definition.transitions().size())
					.append('\n');
			}
			Files.writeString(Path.of(freezeOut), text.toString());
		}
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, String> entry : fingerprints.entrySet()) {
			String observed = actual.get(entry.getKey());
			if (observed == null) {
				problems.add(entry.getKey() + ": 冻结指纹存在但当前不可驱动");
			} else if (!observed.equals(entry.getValue())) {
				problems.add(entry.getKey() + ": 指纹漂移 " + entry.getValue() + " -> " + observed);
			}
		}
		Set<Integer> retiredInFamily = new TreeSet<>(RetiredQuestIds.all());
		retiredInFamily.retainAll(familyIds);
		assertTrue(fingerprints.size() == retiredInFamily.size(),
			() -> "冻结集合与退役集合不一致：frozen=" + fingerprints.size() + " retired=" + retiredInFamily.size());
		assertTrue(problems.isEmpty(), () -> "SimpleCollectItem 冻结指纹失同步："
			+ problems.stream().limit(20).toList());
	}

	/**
	 * 夹具与生产驱动必须产出同一个定义：本类夹具走合成器公共入口，生产走 {@code RetailQuestDriver} 的 overlay
	 * （{@code ProductionQuestDefinitions}）。两者一旦分叉，夹具就会给生产发"假绿"——P0c-5b 就是这样：
	 * 14120 缺真端 {@code talk_npc1} 步骤让服务端启动失败，而族门禁全绿（夹具走的是不带该步骤的重载）。
	 * <p>
	 * 对照前夹具侧同样套用生产 overlay 的接取入口页修复（{@link RetailClientAcceptEntryPage}）：真端表没有
	 * 对话页列，入口页按客户端任务页 HTML 判定，属生产组合层而非合成器；页契约本身由
	 * {@code RetailClientAcceptEntryPageTest} 全 owner 面守。夹具合成器输出与生产 overlay 的其余各轴仍须逐字节一致。
	 * The fixture applies the same accept-entry-page overlay as the production driver before comparing: the
	 * retail tables carry no dialog-page column, so the entry page is resolved against the client task HTML
	 * at the composition layer (its own contract gate is {@code RetailClientAcceptEntryPageTest}); every
	 * other axis of the fixture synthesizer output must still match production byte for byte.
	 * The fixture and the production driver must build the identical definition; a fork between the two
	 * turns the whole family gate into a false green.
	 */
	@Test
	void fixtureMatchesTheProductionDriver() throws Exception {
		Map<Integer, String> owners = retentionOwners();
		List<String> problems = new ArrayList<>();
		int compared = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				continue;
			}
			// 只有保留清单标为 RETAIL_TABLE 的行由生产 overlay 接管；留在 XML 的行（登记差异轴）
			// 生产定义本来就是 XML，不参与本对照。
			// Only RETAIL_TABLE rows are owned by the production overlay; rows kept on XML are excluded.
			if (!"RETAIL_TABLE".equals(owners.get(questId))) {
				continue;
			}
			compared++;
			String fixture = RetailIrFingerprint.fingerprint(RetailClientAcceptEntryPage.repair(
				row.outcome().definition(), QuestDialogContract.loadDefault()).definition());
			String production = RetailIrFingerprint.fingerprint(
				ProductionQuestDefinitions.definition(questId).definition());
			if (!fixture.equals(production)) {
				problems.add(questId + ": 夹具 " + fixture + " != 生产 " + production);
			}
		}
		int total = compared;
		assertTrue(problems.isEmpty(), () -> "SimpleCollectItem 夹具与生产驱动分叉："
			+ problems.stream().limit(20).toList() + " (compared=" + total + ")");
		assertTrue(total >= FROZEN_RETIRED_SIZE,
			() -> "夹具/生产对比数量回退：" + total + " < " + FROZEN_RETIRED_SIZE);
	}

	/** 保留清单必须把可驱动集合标为 RETAIL_TABLE、其余留在 XML。 / Manifest agreement with the driver. */
	@Test
	void retentionManifestMatchesDriverOwnership() throws Exception {
		Map<Integer, String> owners = retentionOwners();
		List<String> problems = new ArrayList<>();
		int retailOwned = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			String owner = owners.get(questId);
			boolean accepted = row.outcome().accepted();
			if (accepted && "RETAIL_TABLE".equals(owner)) {
				retailOwned++;
				continue;
			}
			if (accepted && "XML_RETENTION".equals(owner)) {
				// 已可由真端驱动但仍留在 XML：只允许登记在案的差异轴（评审未过的批次）。
				if (!isRegisteredRetentionGap(questId)) {
					problems.add(questId + ": 可驱动却被留在 XML，且不在批次登记内");
				}
				continue;
			}
			if (!accepted && "RETAIL_TABLE".equals(owner)) {
				problems.add(questId + ": 驱动拒绝却被标为 RETAIL_TABLE（" + row.outcome().rejectionCode() + "）");
			}
		}
		int owned = retailOwned;
		assertTrue(owned >= FROZEN_RETIRED_SIZE,
			() -> "真端驱动拥有的任务数量回退：" + owned + " < " + FROZEN_RETIRED_SIZE);
		assertTrue(problems.isEmpty(), () -> "保留清单与真端驱动不一致："
			+ problems.stream().limit(20).toList());
	}

	// ------------------------------------------------------------------ 语义不变量

	private static boolean isRegisteredRetentionGap(int questId) {
		String classification = driftRegistry.get(questId);
		return classification != null && classification.contains("ROUTE");
	}

	private static void inspect(int questId, RetailRow row, List<String> problems) {
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
			// 领奖投影必须等于客户端任务书末行行号（QE-051）；采集行节点 v{collect_progress} 停在该行；
			// 其余节点留在行 0。
			// The reward projection must equal the last client journal row index; the collect-row node
			// v{collect_progress} sits on its own row; every other node stays on row 0.
			int expected;
			if (node.projection().status() == QuestStatus.REWARD) {
				expected = clientSummaryRows.lastRowIndex(questId);
			} else if (node.label().matches("v\\d+")) {
				expected = Integer.parseInt(node.label().substring(1));
			} else {
				expected = 0;
			}
			if (var0 == null || var0.intValue() != expected) {
				problems.add(questId + ": 节点 " + node.label() + " 投影 var0=" + var0
					+ "，期望 " + expected + "（客户端行数 " + clientRows + "）");
			}
		}
		if (!statuses.equals(Set.of(QuestStatus.NONE, QuestStatus.START, QuestStatus.REWARD,
			QuestStatus.COMPLETE))) {
			problems.add(questId + ": 节点状态集合异常 " + statuses);
		}
		if (row.entry().grantKind().systemGrant()) {
			// 系统发放形状（M5-b3x）：真端无 NPC 接取（普查 43/43 无生命周期三元组、客户端只有委托书页），
			// 定义不得出现任何接取/续页/SETPRO1 路由。
			// System-grant shape: retail has no NPC accept, so accept/continuation/SETPRO1 routes are forbidden.
			if (hasAnyAcceptRoute(definition)) {
				problems.add(questId + ": 系统发放行不得有接取路由");
			}
		} else {
			if (!hasAccept(definition, row.acquiredNpc())) {
				problems.add(questId + ": 缺接取路由 npc=" + row.acquiredNpc());
			}
			if (!hasSetpro(definition, row.acquiredNpc())) {
				problems.add(questId + ": 缺真端 SETPRO1 备选接取路由 npc=" + row.acquiredNpc());
			}
			// P0-2 规范形：SELECT1 入口页删除，SELECT1_1 续页梯随之消失，不再要求登记。
			// Canonical since P0-2: the SELECT1 entry page is gone, so the SELECT1_1 ladder is no
			// longer required.
		}
		if (!hasObjectRoutes(definition, row)) {
			problems.add(questId + ": 缺采集对象路由 " + row.entry().objects());
		}
		// 简报步骤（真端 {@code talk_npc1}，P0-2 规范形）：QUEST_SELECT 一步直达采集行
		// （LEVEL 同步 + 关窗）；select2 页链不再由服务端驱动。
		// The briefing step (canonical since P0-2): QUEST_SELECT lands on the collect row in one step.
		String talkNpcName = row.entry().talkNpc();
		if (talkNpcName != null && !talkNpcName.isBlank()) {
			String collectNode = definition.nodes().stream().map(QuestNode::label)
				.filter(label -> label.matches("v\\d+")).findFirst().orElse("started");
			Set<Integer> talkIds = npcIndex.resolveAll(List.of(talkNpcName)).npcIds();
			if (talkIds.size() != 1) {
				problems.add(questId + ": 简报步骤未解析 talk_npc1=" + talkNpcName + " ids=" + talkIds);
			} else if (!hasCanonicalBriefing(definition, talkIds.iterator().next(), collectNode)) {
				problems.add(questId + ": 缺规范形简报一步路由 " + talkNpcName + " -> " + collectNode);
			}
		}
		for (int rewardNpc : row.rewardNpcs()) {
			if (!hasReport(definition, rewardNpc, row, problems)) {
				problems.add(questId + ": 缺报告/交付检查路由 npc=" + rewardNpc);
			}
			if (!hasComplete(definition, rewardNpc, row)) {
				problems.add(questId + ": 缺完成分支 npc=" + rewardNpc);
			}
			if (row.acquiredNpc() != rewardNpc && !hasReportNpcExit(definition, rewardNpc)) {
				problems.add(questId + ": 缺报告 NPC 关窗出口 npc=" + rewardNpc);
			}
		}
		if (row.rewardNpcs().isEmpty()) {
			problems.add(questId + ": 无交付 NPC 集");
		}
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
	 * 任意 NPC 上的接取路由（系统发放行必须为零；对象 TALK 路由 dialogId=null，不算接取）。
	 * Any NPC accept route; forbidden for system-grant rows (object TALK routes carry no dialog id).
	 */
	private static boolean hasAnyAcceptRoute(QuestDefinition definition) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id()
					|| talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()
					|| talk.dialogId() == QuestDialogAction.SETPRO1.id()
					|| talk.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id())
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START);
	}

	private static boolean hasSetpro(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.SETPRO1)
				&& transition.conditions().contains(new QuestCondition.StartEligible())
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START);
	}

	private static boolean hasReportNpcExit(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.FINISH_DIALOG)
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START
				&& transition.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	/**
	 * 规范形简报（quest-native-dispatch P0-2）：QUEST_SELECT 一步直达采集行，LEVEL 同步 + 关窗；
	 * select2 页链不再由服务端驱动，逐跳路由要求随之退役。
	 * Canonical briefing (quest-native-dispatch P0-2): QUEST_SELECT lands on the collect row in one
	 * step with the journal refresh and close; the per-hop page-chain requirement retires with it.
	 */
	private static boolean hasCanonicalBriefing(QuestDefinition definition, int npcId, String collectNode) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.QUEST_SELECT)
				&& collectNode.equals(transition.targetNode())
				&& transition.afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
				&& transition.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	private static boolean hasObjectRoutes(QuestDefinition definition, RetailRow row) {
		Set<Integer> objects = new LinkedHashSet<>();
		for (String object : row.entry().objects()) {
			Set<Integer> ids = npcIndex.resolveAll(List.of(object)).npcIds();
			if (ids.size() == 1) {
				objects.add(ids.iterator().next());
			}
		}
		for (int objectId : objects) {
			boolean talkRoute = definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == objectId && talk.dialogId() == null);
			boolean canAct = definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.CanAct act
					&& act.templateId() == objectId && "ACTION_ITEM_USE".equals(act.actionType()));
			if (!talkRoute || !canAct) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasReport(QuestDefinition definition, int npcId, RetailRow row, List<String> problems) {
		List<QuestItemRequirement> items = row.metadata().itemRequirements();
		if (items.isEmpty()) {
			problems.add(row.entry().questId() + ": 交付物为空");
			return false;
		}
		List<QuestCondition> hasItems = items.stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = items.stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		// 规范形交付（quest-native-dispatch P0-2）：满段 QUEST_SELECT 带整组 HasItem 门控直翻 REWARD
		// 并下发档位奖励窗；SELECT5 报告页与 39/20002 检查对不再登记。
		// Canonical delivery (quest-native-dispatch P0-2): the full-node QUEST_SELECT gated by the
		// whole hand-in set flips REWARD; the SELECT5 report page and the 39/20002 pairs are gone.
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.QUEST_SELECT)
				&& transition.conditions().equals(hasItems) && transition.actions().equals(removeItems)
				&& statusOf(definition, transition.targetNode()) == QuestStatus.REWARD
				&& transition.afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
	}

	/**
	 * 完成分支：无可选奖励 = 确认区间 8..23 全覆盖；有可选奖励 = 第 k 个可选项绑确认动作 8+k（P1b）。
	 * Completion branches: fixed-only quests confirm on the full 8..23 range; selectable quests bind
	 * confirm action 8+k to the k-th selectable reward (P1b).
	 */
	private static boolean hasComplete(QuestDefinition definition, int npcId, RetailRow row) {
		int first = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
		int last = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
		Set<Integer> confirmIds = new LinkedHashSet<>();
		definition.transitions().forEach(transition -> {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& talk.dialogId() != null && talk.dialogId() >= first && talk.dialogId() <= last
				&& statusOf(definition, transition.targetNode()) == QuestStatus.COMPLETE) {
				confirmIds.add(talk.dialogId());
			}
		});
		int selectables = row.selectableCount();
		Set<Integer> expected = new LinkedHashSet<>();
		int bound = selectables == 0 ? last : first + selectables - 1;
		for (int id = first; id <= bound; id++) {
			expected.add(id);
		}
		return confirmIds.equals(expected);
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
			if (fromXml == null) {
				classification.put(questId, "NO_XML");
				continue;
			}
			String retailText = RetailIrFingerprint.canonicalText(row.outcome().definition().definition());
			String xmlText = RetailIrFingerprint.canonicalText(fromXml.definition());
			classification.put(questId, retailText.equals(xmlText) ? "EQUIVALENT"
				: "DIFF:" + axisText(retailText, xmlText));
		}
		return classification;
	}

	/** 差异归轴：两侧独有的规范化 IR 行按已知轴分类。 / Classifies differing IR lines into known axes. */
	private static String axisText(String retailText, String xmlText) {
		Set<String> retail = new HashSet<>(List.of(retailText.split("\n")));
		Set<String> xml = new HashSet<>(List.of(xmlText.split("\n")));
		Set<String> axes = new TreeSet<>();
		for (String line : xml) {
			if (!retail.contains(line)) {
				axes.add(axisOf(line, false));
			}
		}
		for (String line : retail) {
			if (!xml.contains(line)) {
				axes.add(axisOf(line, true));
			}
		}
		return String.join(" ", axes);
	}

	private static String axisOf(String line, boolean retailOnly) {
		String prefix = retailOnly ? "RETAIL_EXTRA:" : "XML_EXTRA:";
		if (line.contains("BitField[name=var0")) {
			return prefix + "VAR0_FIELD";
		}
		if (line.matches("N\\tREWARD/\\d+") || line.contains("REWARD/0") || line.contains("REWARD/1")
			|| line.contains("REWARD/2")) {
			return prefix + "REWARD_ROW";
		}
		for (String token : List.of("1012", "1013", "10255", "1008", "1009", "39", "20002", "10")) {
			if (line.contains("dialogId=" + token)) {
				return prefix + "DIALOG_" + token;
			}
		}
		if (line.startsWith("T\t")) {
			return prefix + "ROUTE";
		}
		return prefix + "OTHER";
	}

	private static String dumpText(Map<Integer, String> classification) {
		Map<String, Integer> histogram = new TreeMap<>();
		classification.values().forEach(kind -> histogram.merge(kind, 1, Integer::sum));
		StringBuilder text = new StringBuilder("# SimpleCollectItem 漂移登记（禁止手改；重算：-Dretail.collect.equivOut=<path>）\n");
		histogram.forEach((kind, count) -> text.append("# ").append(kind).append('\t').append(count).append('\n'));
		classification.forEach((questId, kind) -> text.append(questId).append('\t').append(kind).append('\n'));
		return text.toString();
	}

	/** 单任务真端编译上下文（表行 + 元数据 + 结果）。 / Retail compile context for one quest row. */
	private record RetailRow(RetailSimpleCollectItemTable.Entry entry, QuestMetadata metadata, int acquiredNpc,
			List<Integer> rewardNpcs, int selectableCount,
			RetailSimpleCollectItemDefinitionCompiler.Outcome outcome) {
	}

	private static RetailRow retailRow(int questId) {
		RetailSimpleCollectItemTable.Entry entry = table.find(questId).orElseThrow();
		var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
			itemIndex, randomRewards, nameIds);
		var outcome = RetailSimpleCollectItemDefinitionCompiler.compile(entry, npcIndex, metadata,
			clientDialogExits, clientSummaryRows, clientRewardNpcs);
		Set<Integer> resolved = npcIndex.resolveAll(
			List.of(entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
		List<Integer> rewardNpcs = resolved.size() == 1 ? List.of(resolved.iterator().next())
			: clientRewardNpcs.rewardNpcs(questId);
		List<QuestReward> rewards = metadata.metadata().rewardGroups().isEmpty()
			? List.of() : metadata.metadata().rewardGroups().get(0).rewards();
		int selectables = (int) rewards.stream()
			.filter(reward -> QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM)
			.count();
		return new RetailRow(entry, metadata.metadata(), resolveOrMinusOne(entry.acquiredNpc()), rewardNpcs,
			selectables, outcome);
	}

	/** 唯一 NPC id；空集/多解返回 -1（拒绝码已覆盖这些行）。 / Unique npc id, -1 when unresolved. */
	private static int resolveOrMinusOne(String name) {
		Set<Integer> ids = npcIndex.resolveAll(List.of(name == null ? "" : name)).npcIds();
		return ids.size() == 1 ? ids.iterator().next() : -1;
	}

	/** 真端表 ∩ 生产宇宙（catalog ∪ 退役清单）。 / Retail table intersected with the production universe. */
	private static Set<Integer> familyIds() throws Exception {
		String text = new String(open(CATALOG).readAllBytes(), StandardCharsets.UTF_8);
		Set<Integer> catalog = new HashSet<>();
		var matcher = java.util.regex.Pattern.compile("<definition id=\"(\\d+)\"").matcher(text);
		while (matcher.find()) {
			catalog.add(Integer.parseInt(matcher.group(1)));
		}
		catalog.addAll(RetiredQuestIds.all());
		Set<Integer> ids = new TreeSet<>(table.questIds());
		ids.retainAll(catalog);
		return ids;
	}

	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new HashMap<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			owners.put(Integer.parseInt(parts[0]), parts[1]);
		}
		return owners;
	}

	private static Map<Integer, String> driftRegistry() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		try (InputStream input = open(DRIFT_REGISTRY)) {
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				rows.put(Integer.parseInt(parts[0]), parts[1]);
			}
		}
		return rows;
	}

	private static Map<Integer, String> fingerprints() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		try (InputStream input = open(FINGERPRINTS)) {
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				rows.put(Integer.parseInt(parts[0]), parts[1]);
			}
		}
		return rows;
	}

	private static InputStream open(String resource) {
		InputStream input = RetailSimpleCollectItemGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}

	private static List<InputStream> openAll(String dir, List<String> names) {
		List<InputStream> streams = new ArrayList<>();
		for (String name : names) {
			streams.add(open(dir + name));
		}
		return streams;
	}

	private static List<String> listXmlNames(String dir) throws Exception {
		var url = RetailSimpleCollectItemGateTest.class.getResource(dir);
		assertNotNull(url, "missing dir " + dir);
		java.io.File[] files = new java.io.File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
		assertNotNull(files);
		return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		}
	}

	private static Map<Integer, Integer> nameIds() throws Exception {
		Map<Integer, Integer> ids = new HashMap<>();
		for (String line : lines(open("/aion/data/static_data/quest_retail/quest_name_string_ids.tsv"))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())), Integer.parseInt(parts[1]));
		}
		return ids;
	}

	private static Map<String, Integer> randomRewardIds() throws Exception {
		Map<String, Integer> ids = new HashMap<>();
		var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(open("/aion/data/static_data/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
	}
}
