package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全库掉落契约门禁：生产任务的掉落形态不得低于真端 quest.xml 记录的契约。
 * Catalog drop-contract gate: production drops must not fall below the retail quest.xml contract.
 *
 * <p>真端以道具与怪物名描述掉落，离线无法把名称映射回 ID，因此基线 TSV 记录的是名称无关的结构：
 * 每个概率档覆盖了多少只怪（允许生产把同一批怪拆成多行、也允许生产比真端多来源），以及掉落道具种数。
 * 生产少怪物或少道具种数即视为丢失掉落来源，必须修复或写入有证据的豁免清单。</p>
 * Retail names items and monsters, so the baseline stores a name-independent structure: how many monsters
 * feed each drop-chance bucket (production may re-group rows and add extra sources) and how many distinct
 * items drop. Losing a monster or an item relative to retail fails the gate unless the quest is listed in
 * the evidence-backed exception file.
 */
class QuestDropContractGateTest {
	private static final String BASELINE_RESOURCE = "/quest/quest-drop-retail-contract.tsv";
	private static final String EXCEPTION_RESOURCE = "/quest/quest-drop-contract-exceptions.tsv";

	@Test
	void productionDropsKeepTheRetailPerMonsterChanceContract() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Map<Integer, BaselineDrop> baseline = loadBaseline();
		Set<Integer> exceptions = loadExceptions();

		List<String> violations = new ArrayList<>();
		List<String> staleExceptions = new ArrayList<>();
		for (int questId : exceptions) {
			if (!baseline.containsKey(questId)) {
				staleExceptions.add("exception without a baseline row: " + questId);
			}
		}
		for (Map.Entry<Integer, BaselineDrop> entry : baseline.entrySet()) {
			Optional<CompiledQuestDefinition> compiled = catalog.findExecutable(entry.getKey());
			if (compiled.isEmpty() || exceptions.contains(entry.getKey())) {
				// 目录占位任务与已豁免任务不参与形态比对。
				// Catalog placeholders and excused quests do not participate in the shape comparison.
				continue;
			}
			Map<Integer, Integer> chanceHistogram = new TreeMap<>();
			Set<Integer> items = new LinkedHashSet<>();
			for (QuestDrop drop : compiled.get().definition().metadata().drops()) {
				items.add(drop.itemId());
				chanceHistogram.merge(drop.chance(), 1, Integer::sum);
			}
			for (Map.Entry<Integer, Integer> expected : entry.getValue().chances().entrySet()) {
				int actual = chanceHistogram.getOrDefault(expected.getKey(), 0);
				if (actual < expected.getValue()) {
					violations.add("Quest " + entry.getKey() + " chance " + expected.getKey()
						+ " covers " + actual + " monsters but retail requires " + expected.getValue());
				}
			}
			if (items.size() < entry.getValue().items()) {
				violations.add("Quest " + entry.getKey() + " declares " + items.size()
					+ " drop items but retail requires " + entry.getValue().items());
			}
		}

		assertTrue(staleExceptions.isEmpty(), "Drop contract exceptions without a baseline row: " + staleExceptions);
		assertTrue(violations.isEmpty(),
			"Production drops fell below the retail contract: " + violations);
	}

	private static Map<Integer, BaselineDrop> loadBaseline() throws Exception {
		Map<Integer, BaselineDrop> result = new TreeMap<>();
		try (InputStream input = QuestDropContractGateTest.class.getResourceAsStream(BASELINE_RESOURCE)) {
			assertNotNull(input, "missing drop contract baseline " + BASELINE_RESOURCE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] columns = line.split("\t", -1);
				Map<Integer, Integer> chances = new TreeMap<>();
				if (!columns[2].isBlank()) {
					for (String bucket : columns[2].split(",")) {
						String[] parts = bucket.split(":", -1);
						chances.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
					}
				}
				result.put(Integer.parseInt(columns[0]),
					new BaselineDrop(Integer.parseInt(columns[1]), chances));
			}
		}
		return result;
	}

	private static Set<Integer> loadExceptions() throws Exception {
		Set<Integer> result = new LinkedHashSet<>();
		try (InputStream input = QuestDropContractGateTest.class.getResourceAsStream(EXCEPTION_RESOURCE)) {
			assertNotNull(input, "missing drop contract exceptions " + EXCEPTION_RESOURCE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				result.add(Integer.parseInt(line.split("\t", -1)[0]));
			}
		}
		return result;
	}

	private record BaselineDrop(int items, Map<Integer, Integer> chances) {
	}
}
