package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务档位 1 数值奖励（EXP/GOLD/AP/GP）真端合同门禁。
 * <p>
 * 以 Aion 5.8 真端 quest.xml 的档位 1 数值快照
 * ({@code /quest/quest-reward-value-retail-contract.tsv}) 为权威：
 * <ul>
 * <li>真端字段存在（含 0=真端明确无此奖励）即权威，生产对应 kind 数值必须一致；</li>
 * <li>真端字段缺失（RETAIL_UNSET）= 真端未配置，生产侧奖励属服务端设计，跳过比对；</li>
 * <li>生产读取口径：档位 1 = 平铺 &lt;rewards&gt;，多档任务取第一个 &lt;group&gt;
 * （其余档位对应真端 _2/_3 字段，不在本门禁范围）。</li>
 * </ul>
 * 例外（逐条列明，禁止通配豁免）：
 * <ul>
 * <li>AP 服务端 4 倍版本倍率族（11279~11286、21281~21288、18849、18850、28849、28850，
 * 生产 = 真端 × 4，5.8 欧比斯点数版本倍率）。</li>
 * </ul>
 * 基线由 .agents/summary/quest-systemic-goal/build_reward_value_contract_tsv.py 再算。
 */
class QuestRewardValueGateTest {

	private static final String CONTRACT_RESOURCE =
		"/quest/quest-reward-value-retail-contract.tsv";

	private static final String RETAIL_UNSET = "RETAIL_UNSET";

	/** AP 服务端 4 倍版本倍率族（生产 = 真端精确 × 4）。 */
	private static final Set<Integer> AP_SERVER_QUADRUPLE = Set.of(
		11279, 11280, 11281, 11282, 11283, 11284, 11285, 11286,
		21281, 21282, 21283, 21284, 21285, 21286, 21287, 21288,
		18849, 18850, 28849, 28850);

	/**
	 * 真端 EXP=0 而生产保留 1 点经验占位的任务：删除会位移 npc-complete 的
	 * fixed-reward-indices 合同（1 点经验对玩家无可感知差异），按索引合同保留。
	 */
	private static final Set<Integer> PLACEHOLDER_EXP_KEPT = Set.of(80989, 80990);

	private static Map<Integer, RewardValues> contract;
	private static Map<Integer, QuestMetadata> production;

	record RewardValues(Long exp, Long gold, Long ap, Long gp) {
	}

	@BeforeAll
	static void loadFixtures() throws Exception {
		contract = new HashMap<>();
		try (BufferedReader reader = open(CONTRACT_RESOURCE)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("quest_id\t")) {
					continue;
				}
				String[] cols = line.split("\t", -1);
				assertEquals(5, cols.length, "contract row must have 5 columns: " + line);
				contract.put(Integer.parseInt(cols[0]), new RewardValues(
					valueOrNull(cols[1]), valueOrNull(cols[2]), valueOrNull(cols[3]),
					valueOrNull(cols[4])));
			}
		}
		assertFalse(contract.isEmpty(), "reward contract must not be empty");

		production = new HashMap<>();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
			QuestRewardValueGateTest.class.getClassLoader());
		for (CompiledQuestDefinition compiled : catalog.all()) {
			production.put(compiled.id(), compiled.definition().metadata());
		}
		for (int qid : QuestRetailStartMetadataGateTest.metadataOnlyQuestIds()) {
			production.putIfAbsent(qid, QuestRetailStartMetadataGateTest.parseQuestMetadata(qid));
		}
		assertFalse(production.isEmpty(), "production catalog must not be empty");
	}

	private static Long valueOrNull(String value) {
		return RETAIL_UNSET.equals(value) ? null : Long.parseLong(value);
	}

	private static BufferedReader open(String resource) {
		InputStream input = QuestRewardValueGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, resource + " must exist on the test classpath");
		return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}

	/** 档位 1 数值：平铺 <rewards>，多档任务取第一个 <group>。 */
	private static Map<String, Long> tierOneValues(QuestMetadata metadata) {
		Map<String, Long> values = new HashMap<>();
		List<QuestRewardGroup> groups = metadata.rewardGroups();
		List<QuestReward> rewards = groups.isEmpty() ? metadata.rewards()
			: groups.get(0).rewards();
		for (QuestReward reward : rewards) {
			values.putIfAbsent(reward.kind(), reward.amount());
		}
		return values;
	}

	/** 档位 1 的 EXP/GOLD/AP/GP 必须与真端一致（缺失字段与 4 倍 AP 族除外）。 */
	@Test
	void tierOneNumericRewardsMatchTheRetailContract() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RewardValues> entry : contract.entrySet()) {
			int qid = entry.getKey();
			QuestMetadata metadata = production.get(qid);
			if (metadata == null) {
				problems.add("contract quest " + qid + " missing from production catalog");
				continue;
			}
			Map<String, Long> values = tierOneValues(metadata);
			RewardValues retail = entry.getValue();
			if (PLACEHOLDER_EXP_KEPT.contains(qid)
				&& Long.valueOf(0L).equals(retail.exp())
				&& values.getOrDefault("EXP", 0L) == 1L) {
				// 1 点经验占位按 npc-complete 索引合同保留（见例外清单）
				retail = new RewardValues(null, retail.gold(), retail.ap(), retail.gp());
			}
			check(qid, values, retail.exp(), "EXP", false, problems);
			check(qid, values, retail.gold(), "GOLD", false, problems);
			check(qid, values, retail.ap(), "AP", AP_SERVER_QUADRUPLE.contains(qid), problems);
			check(qid, values, retail.gp(), "GP", false, problems);
		}
		assertTrue(problems.isEmpty(), () -> "reward value mismatches: " + problems);
	}

	private static void check(int qid, Map<String, Long> values, Long retailValue,
			String kind, boolean allowQuadruple, List<String> problems) {
		if (retailValue == null) {
			return;
		}
		long actual = values.getOrDefault(kind, 0L);
		if (actual == retailValue) {
			return;
		}
		if (allowQuadruple && retailValue != 0 && actual == retailValue * 4) {
			return;
		}
		problems.add("quest " + qid + " " + kind + "=" + actual
			+ " but retail=" + retailValue);
	}
}
