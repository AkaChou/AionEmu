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

	/** 本批次补前置的任务（真端 finished_quest_cond1），锁定精确取值，禁止回退。
	 * P8 重锚：仅保留 XML 保留行的键（其余 22 键随 native 行退出 typed 目录——native 行前置面 =
	 * 显示元数据/链式接取窗，由 native handler 承担）。
	 * P8 re-anchor: only XML-retained keys remain (the other 22 keys left the typed catalog with
	 * their native rows — the native prerequisite face is display metadata / the chain window). */
	private static final Map<Integer, Integer> PREREQ_BATCH = Map.ofEntries(
		Map.entry(2533, 2532), Map.entry(3050, 3049), Map.entry(21080, 21065));


	@Test
	void everyPortedRetailPrerequisiteBranchIsExpressedByTheCatalog() throws Exception {
		Map<Integer, List<List<Integer>>> contract = loadContract();
		QuestCatalog catalog = com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.overlay(
			QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader()));
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
		// P8 重锚：native 行（七族 ∨ DD 1467 行）退出 typed 目录，前置分支扫描域 = XML 保留行
		// （实测 106）；native 行的前置面 = 显示元数据（§10.3-#18 裁定）与链式接取窗（native handler）。
		// P8 re-anchor: native rows left the typed catalog — the branch sweep domain is the XML-retained
		// rows (observed 106); native prerequisite faces are display metadata and the native chain window.
		assertTrue(checkedBranches > 100, "contract coverage too small: " + checkedBranches);
		assertTrue(skippedBranches > 0, "unported-branch skip path must stay exercised");
	}

	@Test
	void batchPrerequisitesMatchTheRetailConditionExactly() throws Exception {
		QuestCatalog catalog = com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.overlay(
			QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader()));
		for (Map.Entry<Integer, Integer> expected : PREREQ_BATCH.entrySet()) {
			QuestCatalogEntry entry = catalog.findEntry(expected.getKey())
				.orElseThrow(() -> new AssertionError("quest " + expected.getKey() + " missing from catalog"));
			assertEquals(Set.of(expected.getValue()), effectiveFinishedConditions(entry.metadata()),
				() -> "quest " + expected.getKey() + " prerequisites");
		}
		// P8：2641 漂移锚随 native 行退出 typed 目录——其链式前置（2619）由 SimpleTalkHandler
		// 的 con_quest 链窗口承担（族门覆盖），此处不再读目录元数据。
		// P8: the 2641 drift anchor left the typed catalog with its native row — the chained
		// prerequisite (2619) is held by SimpleTalkHandler's chain window (family gate).
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
