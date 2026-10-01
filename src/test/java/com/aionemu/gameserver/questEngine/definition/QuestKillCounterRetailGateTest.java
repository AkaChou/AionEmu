package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用客户端击杀门控（quest_monster.csv 的 SECTION_1&lt;N）校验**生产**任务的"完成所需击杀数"。
 * 击杀数由 {@link QuestKillCounterSimulator} 经真实 planner 模拟得出，而不是从 XML 形状反推，
 * 因此既能抓住 13758 族那种"客户端 5 杀、XML 要 15 杀"的漂移，也不会被 +1 记账形态误伤。
 * <p>目录是生产视图（真端优先 overlay，见 {@link ProductionQuestDefinitions}）：退役行的击杀台阶
 * 只存在于真端 IR 里，XML-only 目录对它们不可见。
 * <p>Validates the production "kills required" against the client kill gate by simulating kills through
 * the real planner. The simulator is shape-independent: it catches drift without guessing accounting
 * forms. The catalog is the production view (retail-first overlay), because retired rows answer only
 * through the retail IR.
 */
class QuestKillCounterRetailGateTest {
	private static final String CONTRACT_RESOURCE = "/quest/quest-kill-counter-retail-contract.tsv";
	/** 合同快照规模：低于该值说明基线被误删或生成脚本漏了任务。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 414;
	private static final String QUEST_XML_DIR =
		"src/main/resources/aion/data/static_data/quest/definitions/quests";
	private static final String PREVIOUS_SWITCH = System.getProperty("aion.quest.retailDriver");

	@BeforeAll
	static void enableRetailFirstProduction() {
		System.setProperty("aion.quest.retailDriver", "true");
	}

	@AfterAll
	static void restoreSwitch() {
		if (PREVIOUS_SWITCH == null) {
			System.clearProperty("aion.quest.retailDriver");
		} else {
			System.setProperty("aion.quest.retailDriver", PREVIOUS_SWITCH);
		}
	}

	/** 生产视图目录（真端优先 overlay）。 / The production-view catalog (retail-first overlay). */
	private static QuestCatalog catalog() {
		return ProductionQuestDefinitions.catalog();
	}

	@Test
	void contractSnapshotKeepsItsCoverage() {
		Map<Integer, Integer> contract = readings(CONTRACT_RESOURCE, "quest_id", "required_kills");
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"kill-counter contract snapshot must keep its reviewed coverage");
		assertTrue(contract.values().stream().allMatch(gate -> gate > 0), "gates must be positive");
	}

	@Test
	void singleCounterQuestsRequireExactlyTheClientGate() {
		QuestCatalog catalog = catalog();
		Map<Integer, Integer> contract = readings(CONTRACT_RESOURCE, "quest_id", "required_kills");
		List<String> violations = new ArrayList<>();
		int skippedNative = 0;
		for (Map.Entry<Integer, Integer> entry : contract.entrySet()) {
			int questId = entry.getKey();
			int gate = entry.getValue();
			// P7 步 f 重锚：native 行（七家族 + DD 1467 行）退出 typed 目录，其击杀门由原生链路
			// （家族门 + DataDrivenNativeRuntimeGateTest 的行阶梯断言）承担，这里只对拍 XML 保留行。
			// P7 step-f re-anchor: native rows (seven families plus the 1467 DD rows) left the typed
			// catalog; their kill gates are held by the native lane (family gates plus the
			// DataDrivenNativeRuntimeGateTest ladder assertions) — sweep the XML-retained rows only.
			if (nativeLaneOwned(questId)) {
				skippedNative++;
				continue;
			}
			CompiledQuestDefinition compiled = catalog.findExecutable(questId)
				.orElseThrow(() -> new AssertionError("quest " + questId + " is not an executable owner"));
			Set<String> counters = QuestKillCounterSimulator.killCounterFields(compiled);
			if (counters.size() != 1) {
				violations.add("quest " + questId + " is in the single-counter contract but uses " + counters);
				continue;
			}
			int required = QuestKillCounterSimulator.requiredKills(compiled);
			if (required != gate) {
				violations.add("quest " + questId + " must require exactly the client kill gate " + gate
					+ " but simulates " + required);
			}
		}
		assertEquals(List.of(), violations,
			"single-counter quests must reproduce their client kill gate");
		assertTrue(skippedNative > 0, "native-lane skip must actually see rows (口径失效)");
	}

	/** native 车道归属（七家族 handler + P7 步 f 起 DD 运行时路由行）。 / Native-lane ownership. */
	private static boolean nativeLaneOwned(int questId) {
		return com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler.instance().owns(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance().owns(questId);
	}

	/**
	 * 第 2 项：`<kills>` 声明只是展示性狩猎步骤，其 npc 必须真实出现在击杀转换里；
	 * 计数一律以计数器（上面的门禁）为准，禁止再用声明条数当合同。
	 * <p>人口护栏在真端化改造后换了口径：声明只活在 XML 侧——真端元数据没有 kills 源（该轴的分歧在
	 * {@link RetailMetadataEquivalenceGateTest} 注册），所以"审阅过的人口"由机房 XML 目录自证：磁盘上
	 * 含 {@code <kills>} 的保留 XML 集合必须与目录里的声明行集合逐一对应。这比原来的魔法下限
	 * （&ge;90）更强：解析器漏读声明会少行、目录装配漏带声明会多行，两个方向都会红；而退役本来就会
	 * 合法地移出人口，集合对拍随机房目录自动跟随，不需要任何下限数字。
	 * Item 2: kill declarations are display-only and must stay inside the real kill transitions. The
	 * population guard is restated for the retail-first production view: declarations exist only on the
	 * XML side, so the retained corpus attests the population by set equality (no magic floor).
	 */
	@Test
	void killDeclarationsStayInsideKillTransitions() throws IOException {
		QuestCatalog catalog = catalog();
		Set<Integer> declaring = new TreeSet<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			List<QuestKill> declared = compiled.definition().metadata().kills();
			if (declared.isEmpty()) {
				continue;
			}
			declaring.add(compiled.id());
			Set<Integer> hunted = new LinkedHashSet<>();
			for (QuestEvent event : QuestKillCounterSimulator.killEvents(compiled)) {
				if (event instanceof QuestEvent.KillNpc(int npcId)) {
					hunted.add(npcId);
				} else if (event instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
					hunted.addAll(npcIds);
				}
			}
			for (QuestKill kill : declared) {
				for (int npcId : kill.npcIds()) {
					assertTrue(hunted.contains(npcId),
						"quest " + compiled.id() + " declares npc " + npcId
							+ " in <kills> without a matching kill transition");
				}
			}
		}
		assertEquals(declaringQuestIdsInRetainedXml(), declaring,
			"the declaration population must equal the retained XML that declares kills");
	}

	/** 机房 XML 目录里含 {@code <kills>} 的保留文件 id（文本扫描，独立于 XML 编译器）。 /
	 * Retained XML files that declare kills, scanned as text so the check does not reuse the parser. */
	private static Set<Integer> declaringQuestIdsInRetainedXml() throws IOException {
		Set<Integer> declared = new TreeSet<>();
		try (var files = Files.list(Path.of(QUEST_XML_DIR))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
				if (Files.readString(file, StandardCharsets.UTF_8).contains("<kills>")) {
					declared.add(Integer.parseInt(file.getFileName().toString().replace(".xml", "")));
				}
			}
		}
		return declared;
	}

	private static Map<Integer, Integer> readings(String resource, String idColumn, String valueColumn) {
		try (InputStream input = Objects.requireNonNull(
			QuestKillCounterRetailGateTest.class.getResourceAsStream(resource), resource);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			int idIndex = -1;
			int valueIndex = -1;
			Map<Integer, Integer> rows = new LinkedHashMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t");
				if (idIndex < 0) {
					idIndex = indexOf(cells, idColumn);
					valueIndex = indexOf(cells, valueColumn);
					assertTrue(idIndex >= 0, resource + " must declare column " + idColumn);
					assertTrue(valueIndex >= 0, resource + " must declare column " + valueColumn);
					continue;
				}
				rows.put(Integer.parseInt(cells[idIndex]), Integer.parseInt(cells[valueIndex]));
			}
			return rows;
		} catch (Exception e) {
			throw new AssertionError("unable to read " + resource, e);
		}
	}

	private static int indexOf(String[] cells, String name) {
		for (int index = 0; index < cells.length; index++) {
			if (name.equals(cells[index])) {
				return index;
			}
		}
		return -1;
	}
}
