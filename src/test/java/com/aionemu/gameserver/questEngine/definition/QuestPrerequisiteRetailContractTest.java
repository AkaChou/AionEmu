package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务前置（prerequisites / start-condition-groups）真端合同门禁。
 * <p>
 * 以 Aion 5.8 真端服务端任务表 quest.xml 的快照
 * ({@code /quest/quest-prerequisite-retail-contract.tsv}) 为权威，逐任务检查
 * {@code finished_quest_condN} 声明的 OR 分支（分支内逗号 = AND）是否被生产目录表达：
 * <ul>
 * <li>分支内所有任务都已移植时，该分支必须被 prerequisites 或 finished 起始条件覆盖；</li>
 * <li>分支内存在未移植任务时跳过该分支（仓库既有策略：不声明指向未移植任务的悬空前置，
 * 移植完成后该分支自动转为强制，门禁会立即要求补齐）；</li>
 * <li>未移植单分支任务保持 fail-open，但一旦依赖任务进入目录就必须补上前置。</li>
 * </ul>
 * 基线由 {@code .agents/summary/retail-template-reconciliation/build_prereq_contract_tsv.py} 从真端数据再算。
 */
class QuestPrerequisiteRetailContractTest {

	private static final String CONTRACT_RESOURCE = "/quest/quest-prerequisite-retail-contract.tsv";

	/** 本批次补前置的任务（真端 finished_quest_cond1），锁定精确取值，禁止回退。 */
	private static final Map<Integer, Integer> PREREQ_BATCH = Map.ofEntries(
		Map.entry(2533, 2532), Map.entry(3050, 3049), Map.entry(15471, 15402),
		Map.entry(15551, 15550), Map.entry(15552, 15551), Map.entry(15553, 15552),
		Map.entry(15554, 15553), Map.entry(15563, 15550), Map.entry(15595, 15550),
		Map.entry(15673, 15550), Map.entry(16823, 16822), Map.entry(16824, 16821),
		Map.entry(16825, 16822), Map.entry(18035, 18036), Map.entry(18821, 18830),
		Map.entry(18993, 18992), Map.entry(21004, 21001), Map.entry(21080, 21065),
		Map.entry(21201, 21200), Map.entry(2641, 2619), Map.entry(26823, 26822),
		Map.entry(28035, 28036), Map.entry(30719, 30708), Map.entry(49004, 49003),
		Map.entry(80343, 80341));

	/** 真端要求、但因依赖任务尚未移植而保持 fail-open 的链（移植后必须补齐前置）。 */
	private static final Map<Integer, Integer> UNPORTED_CHAIN_PENDING = Map.of(
		1870, 1868, 2869, 2868, 2870, 2868);

	@Test
	void everyPortedRetailPrerequisiteBranchIsExpressedByTheCatalog() throws Exception {
		Map<Integer, List<List<Integer>>> contract = loadContract();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Set<Integer> catalogIds = new LinkedHashSet<>();
		catalog.entries().forEach(entry -> catalogIds.add(entry.id()));

		List<String> violations = new ArrayList<>();
		int checkedBranches = 0;
		int skippedBranches = 0;
		for (QuestCatalogEntry entry : catalog.entries()) {
			List<List<Integer>> branches = contract.get(entry.id());
			if (branches == null) {
				continue;
			}
			Set<Integer> effective = effectiveFinishedConditions(entry.metadata());
			for (List<Integer> branch : branches) {
				if (!catalogIds.containsAll(branch)) {
					skippedBranches++;
					continue;
				}
				checkedBranches++;
				if (!effective.containsAll(branch)) {
					violations.add(entry.id() + ": retail branch " + branch
						+ " not covered by " + effective);
				}
			}
		}

		assertTrue(violations.isEmpty(), () -> "missing retail prerequisite branches: " + violations);
		assertTrue(checkedBranches > 1000, "contract coverage too small: " + checkedBranches);
		assertTrue(skippedBranches > 0, "unported-branch skip path must stay exercised");
	}

	@Test
	void batchPrerequisitesMatchTheRetailConditionExactly() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		for (Map.Entry<Integer, Integer> expected : PREREQ_BATCH.entrySet()) {
			QuestCatalogEntry entry = catalog.findEntry(expected.getKey())
				.orElseThrow(() -> new AssertionError("quest " + expected.getKey() + " missing from catalog"));
			assertEquals(Set.of(expected.getValue()), effectiveFinishedConditions(entry.metadata()),
				() -> "quest " + expected.getKey() + " prerequisites");
		}
		// 2641 的迁移漂移（误配 2640）不得回归。
		QuestCatalogEntry drift = catalog.findEntry(2641).orElseThrow();
		assertFalse(effectiveFinishedConditions(drift.metadata()).contains(2640),
			"quest 2641 must not gate on 2640; retail condition is 2619");
	}

	@Test
	void unportedSingleBranchChainsStayOpenUntilTheDependencyIsPorted() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		for (Map.Entry<Integer, Integer> pending : UNPORTED_CHAIN_PENDING.entrySet()) {
			boolean dependencyPorted = catalog.findEntry(pending.getValue()).isPresent();
			Set<Integer> effective = effectiveFinishedConditions(
				catalog.findEntry(pending.getKey()).orElseThrow().metadata());
			if (dependencyPorted) {
				assertTrue(effective.contains(pending.getValue()),
					() -> "quest " + pending.getKey() + " must require ported dependency "
						+ pending.getValue());
			} else {
				assertFalse(effective.contains(pending.getValue()),
					() -> "quest " + pending.getKey() + " must not declare dangling prerequisite "
						+ pending.getValue());
			}
		}
	}

	private static Set<Integer> effectiveFinishedConditions(QuestMetadata metadata) {
		Set<Integer> effective = new LinkedHashSet<>(metadata.prerequisites());
		for (QuestStartCondition condition : metadata.startConditions()) {
			if ("finished".equalsIgnoreCase(condition.type())) {
				effective.add(condition.questId());
			}
		}
		return effective;
	}

	private static Map<Integer, List<List<Integer>>> loadContract() throws Exception {
		Map<Integer, List<List<Integer>>> contract = new HashMap<>();
		try (InputStream input = QuestPrerequisiteRetailContractTest.class
			.getResourceAsStream(CONTRACT_RESOURCE)) {
			assertNotNull(input, "missing retail prerequisite contract");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
				StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isEmpty() || line.startsWith("#") || line.startsWith("quest_id\t")) {
						continue;
					}
					String[] cols = line.split("\t", -1);
					assertEquals(2, cols.length, "contract row must have 2 columns: " + line);
					List<List<Integer>> branches = new ArrayList<>();
					for (String branch : cols[1].split(";", -1)) {
						List<Integer> ids = new ArrayList<>();
						for (String id : branch.split(",", -1)) {
							ids.add(Integer.parseInt(id.trim()));
						}
						branches.add(List.copyOf(ids));
					}
					contract.put(Integer.parseInt(cols[0]), List.copyOf(branches));
				}
			}
		}
		assertFalse(contract.isEmpty(), "retail prerequisite contract must not be empty");
		return contract;
	}
}
