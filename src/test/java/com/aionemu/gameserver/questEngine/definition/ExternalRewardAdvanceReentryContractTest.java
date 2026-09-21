package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定“引擎外推进 REWARD”的任务合同：写入方留下的打包步数、reward 节点投影、领奖态入口页与旧存档自愈
 * 必须始终对齐。
 * Locks the contract of quests advanced to REWARD outside the typed engine: the packed step left by the writer,
 * the reward node projection, the reward-state entry page and the legacy-save recovery must stay aligned.
 *
 * <p>这些任务的 REWARD 不是由 `SELECT_QUEST_REWARD` 事务写入的，而是由客户端包（`CM_CREATIVITY_POINTS`）、
 * 服务（`CoalescenceService`、`MinionService`）或 AI（`RiftOrbAI2`）直接 `setStatus(QuestStatus.REWARD)`。typed
 * 引擎按 (status, packed step) 匹配路由，因此只要写入方的步数与 reward 节点投影不一致，或定义没有注册
 * `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS` 入口页，玩家点任务行后只会看到通用“结束对话”页。
 * The REWARD state of these quests is written by client packets (`CM_CREATIVITY_POINTS`), services
 * (`CoalescenceService`, `MinionService`) or AI (`RiftOrbAI2`) instead of a `SELECT_QUEST_REWARD` transaction.
 * The typed engine matches routes by (status, packed step), so a writer step that differs from the reward
 * projection, or a missing `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS` entry page, leaves the player with the
 * generic end-dialog page after selecting the quest row.</p>
 *
 * <p>基线 `src/test/resources/quest/external-reward-advance-baseline.tsv` 由只读审计脚本
 * `.agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py` 生成；新增引擎外写入方时先重跑
 * 脚本刷新基线，本测试会立刻要求新条目提供同样的入口页与自愈证据。
 * The baseline `src/test/resources/quest/external-reward-advance-baseline.tsv` is produced by the read-only audit
 * script `.agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py`; when a new engine-external
 * writer appears, re-run that script and this test immediately demands the same entry page and recovery evidence.</p>
 */
class ExternalRewardAdvanceReentryContractTest {
	private static final String BASELINE_RESOURCE = "/quest/external-reward-advance-baseline.tsv";
	private static final List<Integer> EXPECTED_QUEST_IDS = List.of(
		10522, 15542, 15545, 20522, 25542, 25545, 30211, 30213, 30311, 30313);

	@Test
	void engineExternalRewardWritersKeepProjectionAndReentryRoutesAligned() throws Exception {
		List<BaselineRow> rows = readBaseline();
		assertEquals(EXPECTED_QUEST_IDS, rows.stream().map(BaselineRow::questId).toList(),
			"引擎外推进 REWARD 的任务清单发生变化时必须同步审计脚本与基线"
				+ " / the engine-external REWARD writer set changed; refresh the audit script and baseline");

		for (BaselineRow row : rows) {
			QuestDefinition definition = definition(row.questId()).definition();
			assertEquals(row.writerStep(), row.rewardProjection(),
				"quest " + row.questId() + " baseline must record the aligned writer step");

			// reward 投影必须等于写入方留下的打包步数，否则该存档匹配不到任何领奖路由。
			// The reward projection must equal the packed step left by the writer, otherwise the save matches
			// no reward route at all.
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", row.writerStep()));

			// 进入 REWARD 的事务不得再改写 var0：打包步数由 reward 节点投影决定。
			// Transactions entering REWARD must not rewrite var0: the reward node projection owns it.
			assertTrue(definition.transitions().stream()
					.filter(transition -> "started".equals(transition.sourceNode()))
					.filter(transition -> "reward".equals(transition.targetNode()))
					.flatMap(transition -> transition.actions().stream())
					.noneMatch(action -> action instanceof QuestAction.SetVariable set
						&& "var0".equals(set.field())),
				"quest " + row.questId() + " must not rewrite the reward packed step");

			// 每个完成 NPC 都必须注册领奖态入口页：31 -> select_success(10002) -> 1009 -> 奖励窗口。
			// Every completion NPC must register the reward-state entry page:
			// 31 -> select_success(10002) -> 1009 -> reward window.
			assertFalse(row.completionNpcIds().isEmpty(),
				"quest " + row.questId() + " has no completion NPC evidence");
			for (int npcId : row.completionNpcIds()) {
				QuestTransition entry = rewardEntryRoute(definition, npcId);
				assertEquals("reward", entry.targetNode(), "quest " + row.questId() + " entry target");
				assertEquals(List.of(), entry.conditions(), "quest " + row.questId() + " entry conditions");
				assertEquals(List.of(), entry.actions(), "quest " + row.questId() + " entry actions");
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
					entry.afterCommit(), "quest " + row.questId() + " entry response");
				assertNull(entry.priority(), "quest " + row.questId() + " entry priority");
			}

			// 旧存档自愈值与写入方遗留的错位步数一一对应，且顺序、条件、响应完全固定。
			// Legacy-save recovery values map one-to-one onto the misplaced steps left by earlier writers, with
			// a fixed order, condition list and response.
			List<QuestTransition> recoveries = unsourcedRewardRecoveries(definition);
			assertEquals(row.staleRewardSteps().size(), recoveries.size(),
				"quest " + row.questId() + " recovery route count");
			for (int index = 0; index < recoveries.size(); index++) {
				QuestTransition recovery = recoveries.get(index);
				assertEquals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", row.staleRewardSteps().get(index))),
					recovery.conditions(), "quest " + row.questId() + " recovery conditions");
				assertEquals(List.of(), recovery.actions(), "quest " + row.questId() + " recovery actions");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
					"quest " + row.questId() + " recovery response");
				assertNull(recovery.priority(), "quest " + row.questId() + " recovery priority");
			}

			assertWriterStillExists(row);
		}
	}

	@Test
	void representativeQuest10522KeepsTheClientVisibleRewardChain() throws Exception {
		QuestDefinition definition = definition(10522).definition();

		// 客户端 quest_q10522.html 的 select_success(10002) 只有 HACTION_SELECT_QUEST_REWARD(1009)，
		// 因此 10002 必须由 31 入口页下发，而 1009 必须由 reward 态预览打开奖励窗口 5。
		// Client quest_q10522.html exposes only HACTION_SELECT_QUEST_REWARD(1009) on select_success(10002), so
		// 10002 must be emitted by the action-31 entry page and 1009 must open reward window 5 from REWARD.
		QuestTransition entry = rewardEntryRoute(definition, 806075);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			entry.afterCommit());
		QuestTransition rewardWindow = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 806075
				&& Objects.equals(talk.dialogId(), QuestDialogAction.SELECT_QUEST_REWARD.id()))
			.findFirst().orElseThrow();
		assertTrue(rewardWindow.afterCommit().contains(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			"1009 must still open reward window 5 / 1009 必须仍然打开奖励窗口 5");
	}

	private static void assertWriterStillExists(BaselineRow row) throws Exception {
		int separator = row.writerEvidence().indexOf('#');
		assertTrue(separator > 0, "writer evidence must be <path>#<method>: " + row.writerEvidence());
		Path writer = Path.of(row.writerEvidence().substring(0, separator));
		String method = row.writerEvidence().substring(separator + 1);
		assertTrue(Files.isRegularFile(writer), "missing engine-external writer " + writer);
		String source = Files.readString(writer, StandardCharsets.UTF_8);
		assertTrue(source.contains(method),
			"writer " + writer + " no longer declares " + method + "; refresh the audit baseline");
		assertTrue(source.contains("QuestStatus.REWARD"),
			"writer " + writer + " no longer advances the quest to REWARD");
	}

	private static QuestTransition rewardEntryRoute(QuestDefinition definition, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "reward".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Objects.equals(talk.dialogId(), QuestDialogAction.QUEST_SELECT.id()))
			.findFirst().orElseThrow(() -> new AssertionError(
				"quest " + definition.id() + " has no reward-state entry page for NPC " + npcId));
	}

	private static List<QuestTransition> unsourcedRewardRecoveries(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode() == null)
			.filter(transition -> "reward".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static List<BaselineRow> readBaseline() throws Exception {
		InputStream input = ExternalRewardAdvanceReentryContractTest.class.getResourceAsStream(BASELINE_RESOURCE);
		assertNotNull(input, "missing baseline resource " + BASELINE_RESOURCE);
		List<BaselineRow> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String record = line;
				String[] fields = record.split("\t", -1);
				assertEquals(6, fields.length, () -> "invalid baseline row: " + record);
				rows.add(new BaselineRow(Integer.parseInt(fields[0]), fields[1], Integer.parseInt(fields[2]),
					Integer.parseInt(fields[3]), ints(fields[4]), ints(fields[5])));
			}
		}
		return List.copyOf(rows);
	}

	private static List<Integer> ints(String field) {
		// 空列在基线 TSV 中写作 "-"，避免生成行尾 tab。
		// Empty columns are written as "-" in the baseline TSV so rows never end with a tab.
		if (field.isBlank() || "-".equals(field.trim())) {
			return List.of();
		}
		List<Integer> values = new ArrayList<>();
		for (String token : field.trim().split("\\s+")) {
			values.add(Integer.parseInt(token));
		}
		return List.copyOf(values);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = ExternalRewardAdvanceReentryContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record BaselineRow(int questId, String writerEvidence, int writerStep, int rewardProjection,
			List<Integer> completionNpcIds, List<Integer> staleRewardSteps) {
	}
}
