package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用客户端击杀门控（quest_monster.csv 的 SECTION_1&lt;N）校验生产任务"完成所需击杀数"。
 * 击杀数由 {@link QuestKillCounterSimulator} 经真实 planner 模拟得出，而不是从 XML 形状反推，
 * 因此既能抓住 13758 族那种"客户端 5 杀、XML 要 15 杀"的漂移，也不会被 +1 记账形态误伤。
 * <p>Validates the production "kills required" against the client kill gate by simulating kills through
 * the real planner. The simulator is shape-independent: it catches drift without guessing accounting forms.
 */
class QuestKillCounterRetailGateTest {
	private static final String CONTRACT_RESOURCE = "/quest/quest-kill-counter-retail-contract.tsv";
	/** 合同快照规模：低于该值说明基线被误删或生成脚本漏了任务。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 414;

	private static QuestCatalog catalog() {
		return QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
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
		for (Map.Entry<Integer, Integer> entry : contract.entrySet()) {
			int questId = entry.getKey();
			int gate = entry.getValue();
			CompiledQuestDefinition compiled = catalog.findExecutable(questId)
				.orElseThrow(() -> new AssertionError("quest " + questId + " is not an executable owner"));
			Set<String> counters = QuestKillCounterSimulator.killCounterFields(compiled);
			assertEquals(1, counters.size(),
				"quest " + questId + " is in the single-counter contract but uses " + counters);
			int required = QuestKillCounterSimulator.requiredKills(compiled);
			assertEquals(gate, required,
				"quest " + questId + " must require exactly the client kill gate");
		}
	}

	/**
	 * 反向对照：把 13765 恢复成修复前的漂移形态（累加门槛与收口门槛同为 14、终值 15），
	 * 模拟器必须复现"多杀 10 只"。这条证明门禁不是空转。
	 * Negative control: reintroduce the exact pre-fix drift of the 13758 family and require the
	 * simulator to reproduce the overkill.
	 */
	@Test
	void simulatorReproducesTheFixedOverkillDrift() {
		CompiledQuestDefinition original = catalog().findExecutable(13765).orElseThrow();
		assertEquals(5, QuestKillCounterSimulator.requiredKills(original), "13765 requires five kills");
		QuestTransition accumulate = killRoute(original, "started", "started");
		QuestTransition finish = killRoute(original, "started", "reward");
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 4)), accumulate.conditions());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 4)), finish.conditions());

		CompiledQuestDefinition drifted = replaceTransition(original, finish,
			new QuestTransition(finish.event(), List.of(new QuestCondition.VariableAtLeast("var1", 5)),
				finish.actions(), finish.targetNode(), finish.afterCommit(), finish.priority(),
				finish.sourceNode()));
		drifted = replaceTransition(drifted, accumulate,
			new QuestTransition(accumulate.event(), List.of(new QuestCondition.VariableBelow("var1", 5)),
				accumulate.actions(), accumulate.targetNode(), accumulate.afterCommit(),
				accumulate.priority(), accumulate.sourceNode()));
		assertEquals(6, QuestKillCounterSimulator.requiredKills(drifted),
			"simulator must report one extra kill when the counting gate drifts by one");
	}

	private static QuestTransition killRoute(CompiledQuestDefinition compiled, String source, String target) {
		return compiled.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet)
			.filter(transition -> source.equals(transition.sourceNode()) && target.equals(transition.targetNode()))
			.findFirst().orElseThrow();
	}

	/**
	 * 第 2 项：`<kills>` 声明只是展示性狩猎步骤，其 npc 必须真实出现在击杀转换里；
	 * 计数一律以 var1 计数器（上面的门禁）为准，禁止再用声明条数当合同。
	 * Item 2: kill declarations are display-only and must stay inside the real kill transitions.
	 */
	@Test
	void killDeclarationsStayInsideKillTransitions() {
		QuestCatalog catalog = catalog();
		int declaring = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			List<QuestKill> declared = compiled.definition().metadata().kills();
			if (declared.isEmpty()) {
				continue;
			}
			declaring++;
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
		assertTrue(declaring >= 90,
			"kill declarations are display-only; their reviewed population must not silently shrink ("
				+ declaring + " declaring quests)");
	}

	private static CompiledQuestDefinition replaceTransition(CompiledQuestDefinition compiled,
			QuestTransition original, QuestTransition replacement) {
		QuestDefinition definition = compiled.definition();
		List<QuestTransition> transitions = definition.transitions().stream()
			.map(transition -> transition.equals(original) ? replacement : transition).toList();
		return QuestDefinitionCompiler.compile(new QuestDefinition(definition.id(), definition.version(),
			definition.metadata(), definition.progressLayout(), definition.nodes(), transitions));
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
