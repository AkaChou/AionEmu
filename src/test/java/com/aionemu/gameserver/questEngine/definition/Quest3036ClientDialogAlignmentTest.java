package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 3036「Let's See What It Does」的三条合同：物件零回页、领奖 owner 唯一、领奖态步号轴（QE-052 + QE-054 + QE-137）。
 * <p>
 * ① **物件零回页**（2026-10-07 实机）：领奖结算曾打在圣物物件 700398 上，`npc-complete
 * finish=SELECTION_DIALOG` 的收尾页 10（questId=0）打到无对话 html 的物件 → 客户端按物件名解析
 * `LF2a_Artifact_Q3036.html` 失败（load fail）。客户端任务书（quest_q3036 的 quest_summary）末行点名的是
 * Atropos（798155），legacy `_3036LetSeeWhatItDoes` 也只在 798155 领奖 → owner 收敛到 Atropos，
 * 物件只保留「用充能发动石 → 推进到领奖行」的零回页边（legacy `useQuestObject(env, 0, 1, true, false)`
 * 只写状态不发页）。
 * <p>
 * ② **领奖态步号轴 = legacy 落盘值 0**（QE-054/QE-056）：该调用的 reward 分支只 `setStatus(REWARD)`、
 * 不写 var0（原版 0x100 状态推进同样不写轴）；任务书行批次（7a7d27809）按「末行索引」把投影抬到 1，
 * 2026-10-07 实机两条 `<p visible>` 全不亮（任务步骤整块空白，与 1123/1361/11006 同型）→ 投影回 0、
 * 自愈边反转为 REWARD/1 -> 0 回滚坏档。
 * <p>
 * Locks quest 3036: the artifact interaction carries no dialog page, Atropos is the sole completion owner,
 * and the reward-state packed step stays on the legacy value 0 (batch-raised saves roll back via
 * REWARD/1 -> 0). Live 2026-10-07: the claim tail sent page 10 to a dialog-less object (load fail) and the
 * batch-raised value blanked the 2-row journal.
 */
class Quest3036ClientDialogAlignmentTest {

	private static final int ARTIFACT = 700398;
	private static final int ATROPOS = 798155;
	private static final int BATCH_ROW = 1;
	private static final int AUTHORITATIVE_ROW = 0;

	/**
	 * 行 0：使用充能发动石推进到领奖行，after-commit 只有状态同步（零回页）。
	 * Row 0: the charged-stone use advances to the reward row with a state sync only — no page at all.
	 */
	@Test
	void artifactUseAdvancesWithoutADialogPage() throws Exception {
		QuestDefinition definition = definition().definition();
		QuestTransition advance = route(definition, "started", "reward",
			new QuestEvent.TalkToNpc(ARTIFACT, QuestDialogAction.USE_OBJECT.id()));

		assertEquals(List.of(), advance.actions(), "the object use only advances the quest state");
		assertEquals(List.of(), advance.conditions(), "no extra gate on the object use");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), advance.afterCommit());
	}

	/**
	 * 物件零回页：以物件为对话目标的所有迁移都不得下发任何页（含通用页 10）。
	 * Zero pages on the object: no transition targeting the artifact may carry a page after-commit.
	 */
	@Test
	void artifactIsNeverTheTargetOfAPage() throws Exception {
		for (QuestTransition transition : definition().definition().transitions()) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.npcId() != ARTIFACT) {
				continue;
			}
			for (AfterCommitAction afterCommit : transition.afterCommit()) {
				assertTrue(!(afterCommit instanceof AfterCommitAction.ShowQuestDialog)
						&& !(afterCommit instanceof AfterCommitAction.ShowQuestSelectionDialog)
						&& !(afterCommit instanceof AfterCommitAction.ShowDialogWindow),
					() -> "the artifact has no client dialog html; page after-commit " + afterCommit
						+ " would fail to load");
			}
		}
	}

	/** 领奖行（Atropos）：行选（31）下发报告页 select_success(10002)，客户端自回 1009 进奖励窗。 */
	@Test
	void atroposRewardRowShowsTheReportConfirmPage() throws Exception {
		QuestDefinition definition = definition().definition();
		QuestTransition report = route(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(ATROPOS, QuestDialogAction.QUEST_SELECT.id()));

		assertEquals(List.of(), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());
	}

	/**
	 * 领奖 owner 唯一 = Atropos：8..23 每个确认动作各一条完成迁移，收尾 = 页 10（questId=0 的
	 * 选择对话页，落在 Atropos 自己的 html 上）。
	 * The sole completion owner: one reward→complete route per confirm action (8..23), each ending on
	 * page 10 — the selection dialog page that Atropos' own dialog html declares.
	 */
	@Test
	void atroposIsTheOnlyCompletionOwnerAndTheTailIsPageTen() throws Exception {
		QuestDefinition definition = definition().definition();
		Set<Integer> owners = new java.util.LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if (!"reward".equals(transition.sourceNode()) || !"complete".equals(transition.targetNode())
					|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
				continue;
			}
			owners.add(talk.npcId());
			assertEquals(ATROPOS, talk.npcId(), "the artifact must not complete quest 3036");
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				transition.afterCommit());
			assertEquals(new QuestAction.CompleteQuest(0),
				transition.actions().getLast(), "completion reward index");
		}
		assertEquals(Set.of(ATROPOS), owners);

		List<Integer> confirmActions = IntStream.rangeClosed(
			QuestDialogAction.SELECTED_QUEST_REWARD1.id(),
			QuestDialogAction.SELECTED_QUEST_NOREWARD.id()).boxed().toList();
		for (int dialogId : confirmActions) {
			route(definition, "reward", "complete", new QuestEvent.TalkToNpc(ATROPOS, dialogId));
		}
	}

	/**
	 * 领奖态步号轴 = legacy 落盘 0：任务书 2 行（行 0 发动、行 1 报告），reward 投影停在行 0 的
	 * 游玩末值；批次误抬的 1 由 REWARD/1 -> 0 自愈边在进世界时回滚（QE-054/QE-056）。
	 * The reward projection stays on the legacy gameplay value 0; batch-raised saves (var0=1) roll back
	 * to 0 on enter-world.
	 */
	@Test
	void rewardProjectionStaysOnTheLegacyValueAndBatchRowsRollBack() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(Map.of("var0", AUTHORITATIVE_ROW), node(definition, "started").projection().variables());
		assertEquals(Map.of("var0", AUTHORITATIVE_ROW), node(definition, "reward").projection().variables());

		List<QuestTransition> rollbacks = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, rollbacks.size(), "quest 3036 keeps exactly one source-less recovery edge");
		QuestTransition rollback = rollbacks.getFirst();
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", BATCH_ROW)), rollback.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", AUTHORITATIVE_ROW)), rollback.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), rollback.afterCommit());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 3036 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest3036ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/3036.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 3036.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
