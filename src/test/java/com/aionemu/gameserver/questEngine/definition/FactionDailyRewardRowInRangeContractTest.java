package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 36500/46500 阵营日任族（17 个，QE-054 遗留问题 B）的领奖行在客户端行数之内。
 * <p>
 * 判据：客户端任务书按 {@code row[axis]} 显示（4338/10525 已验收校准），领奖投影越出
 * {@code quest_summary} 行数即整块空白（1361 实机现象）。该族客户端 2 行（0 击杀 / 1 报告），
 * 16 个成员投影 1、镜像对称；36512 曾投影 2（越界，镜像 46512 为 1）——本门禁锁修复后的
 * 1、击杀边写入值、以及旧存档 {@code REWARD/var0=2 -> 1} 自愈边。
 * <p>
 * Locks the reward row of the 36500/46500 faction-daily family (17 quests, QE-054 residual B)
 * within the client journal rows. The client renders {@code row[axis]} (calibrated on the accepted
 * 4338/10525); an axis beyond the {@code quest_summary} row count blanks the whole journal (live
 * 1361). The family's clients declare two rows (0 kill / 1 report); sixteen members project 1 with
 * mirrored pairs, while 36512 projected the out-of-range 2 (mirror 46512 uses 1). This gate pins the
 * fixed 1, the kill-edge write, and the legacy {@code REWARD/var0=2 -> 1} self-heal edge.
 */
class FactionDailyRewardRowInRangeContractTest {

	/** 全族 17 个（天族 365xx / 魔族 465xx 镜像）。 / The 17 family members (Elyos 365xx / Asmodian 465xx). */
	private static final List<Integer> FAMILY = List.of(36500, 36504, 36505, 36506, 36510, 36511, 36512, 36516,
		46500, 46504, 46505, 46506, 46510, 46511, 46512, 46516, 46517);

	/** 镜像对（同形任务，领奖行必须一致）。 / Mirror pairs whose reward rows must agree. */
	private static final Map<Integer, Integer> MIRRORS = Map.of(36500, 46500, 36504, 46504, 36505, 46505,
		36506, 46506, 36510, 46510, 36511, 46511, 36512, 46512, 36516, 46516);

	/** 36512 旧投影（越界值），自愈边必须把它纠回 1。 / 36512's old out-of-range projection, healed to 1. */
	private static final int Q36512_LEGACY_REWARD_ROW = 2;

	@Test
	void everyFamilyMemberKeepsTheRewardRowInsideTheClientJournal() throws Exception {
		Map<Integer, Integer> clientRows = clientSummaryRows();
		for (int questId : FAMILY) {
			Integer rows = clientRows.get(questId);
			assertTrue(rows != null && rows > 0,
				() -> "quest " + questId + " missing from quest_client_summary_rows.tsv");
			int rewardRow = rewardRow(definition(questId).definition());
			assertTrue(rewardRow >= 0 && rewardRow <= rows - 1,
				() -> "quest " + questId + " reward row " + rewardRow + " escapes the client journal ("
					+ rows + " rows) and would blank it");
		}
	}

	@Test
	void mirroredPairsKeepTheSameRewardRow() throws Exception {
		for (var entry : MIRRORS.entrySet()) {
			int elyosRow = rewardRow(definition(entry.getKey()).definition());
			int asmodianRow = rewardRow(definition(entry.getValue()).definition());
			assertEquals(elyosRow, asmodianRow,
				() -> "quest " + entry.getKey() + " and mirror " + entry.getValue() + " reward rows diverged");
		}
	}

	@Test
	void quest36512KeepsTheReportRowAndHealsTheLegacyOutOfRangeSave() throws Exception {
		QuestDefinition definition = definition(36512).definition();
		assertEquals(1, rewardRow(definition));

		/* 击杀推进边（s1 -> reward）与物品使用边（started -> s1）都必须把轴写 1（报告行）。 */
		/* Both the kill advance (s1 -> reward) and the item-use edge (started -> s1) write axis 1. */
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), actions(definition, "s1", "reward"));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), actions(definition, "started", "s1"));

		/* 旧存档自愈：REWARD/var0=2（越界旧投影）进入世界时纠回 1。 */
		/* Legacy heal: REWARD/var0=2 (the old out-of-range projection) is repaired to 1 on enter-world. */
		List<QuestTransition> recovery = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, recovery.size(), "quest 36512 reward recovery route");
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", Q36512_LEGACY_REWARD_ROW)),
			recovery.getFirst().conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), recovery.getFirst().actions());
	}

	private static List<QuestAction> actions(QuestDefinition definition, String source, String target) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.toList();
		assertEquals(1, matches.size(),
			() -> "quest " + definition.id() + " route " + source + " -> " + target);
		return matches.getFirst().actions();
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables().get("var0");
	}

	/** 客户端任务书行数登记（quest_id → quest_summary 行数）。 / The client journal row registry. */
	private static Map<Integer, Integer> clientSummaryRows() throws IOException {
		Map<Integer, Integer> rows = new LinkedHashMap<>();
		try (InputStream input = FactionDailyRewardRowInRangeContractTest.class
				.getResourceAsStream("/quest/quest_client_summary_rows.tsv")) {
			if (input == null) {
				throw new IllegalStateException("missing quest_client_summary_rows.tsv");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t");
				rows.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			}
		}
		return rows;
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = FactionDailyRewardRowInRangeContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
