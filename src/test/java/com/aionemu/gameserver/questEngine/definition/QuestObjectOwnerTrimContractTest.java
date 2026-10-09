package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物件 owner 同型批次（2026-10-08，QE-052 机型变体；代表先例 3036）：
 * 任务目标物件（`ai=quest_use_item`，客户端无对话 html）不得成为「以物件为目标的发页对象」，
 * 更不得登记 `npc-complete`——`finish=SELECTION_DIALOG` 的收尾页 10 以 questId=0 解析物件自身
 * html 名称（如 `LF2a_Artifact_Q3036.html`），客户端直接 load fail；在物件上打开的奖励窗/
 * 报告页也会把领奖入口从「任务书领奖行 NPC」挪到物件（owner 不再唯一）。
 * <p>
 * 本批逐件取证（legacy handler @ 7e9f0316c^ + 客户端任务书/按钮契约 + 原版零发页函数）后，
 * 把物件收敛为「零回页推进 / 纯采集掉落」，领奖 owner 唯一 = 任务书领奖行点名的 NPC：
 * <ul>
 *   <li>1582：墓碑物件 700196 零回页推进 var0 0→1；Trou 204560 单步阶梯（SETPRO2）；领奖 = Nerison 204573。</li>
 *   <li>2232：蜂巢 700061 纯采集掉落；领奖 = Gilungk 203613。</li>
 *   <li>2237：肥料袋 700145 纯采集掉落；领奖 = 832822（保留清单已裁定交付人分歧）。</li>
 *   <li>2307：汤锅 700247 零回页推进 var0 0→1；Spedor 204336 三支选择（零回页进 REWARD）；领奖 = Fathir 204378。</li>
 *   <li>2664：药缸 700324 阶梯计数 0..4（零回页）；领奖 = Dewi 204777；reward 轴 = legacy 落盘 4。</li>
 *   <li>4004：土堆 700340 阶梯计数 0..4（零回页）；领奖 = Randet 205128；reward 轴 = legacy 落盘 4。</li>
 *   <li>4012：FOBJ 700342 纯采集掉落；领奖 = Scarecrow_Virhu 730104；reward 轴 = legacy 落盘 0。</li>
 *   <li>30211/30213/30311：符文宝珠 730275 的对话由 RiftOrbAI2 承担（原版注册面挂 riftorb AI，legacy
 *       注册与对话块整体注释禁用，与姊妹任务 30313 同形）；宝珠的整体 NPC 块（NPC_START/NPC_REPORT/
 *       领取路由/npc-complete）全部移除，领奖 owner 回到任务书领奖行 NPC；reward 轴 = 引擎外写入方
 *       RiftOrbAI2 落盘的领奖行 1（QE-046 基线 external-reward-advance-baseline.tsv 锁定）。
 *       注：30213 的接取 NPC 798941 在既有形状里带一份与 798926 相同的领取面，按姊妹任务 30313
 *       先例保留（legacy 的 REWARD 分支只注册 798926，属未收口项，见 summary 文档）。</li>
 * </ul>
 * Locks the object-owner sweep (2026-10-08, QE-052 machine variant, 3036 is the reference case): an
 * interaction object may never be the target of a page send or a completion owner; each quest's claim
 * lives on the NPC named by the client journal's reward row, and the reward projection stays on the
 * persisted value left by the legacy handler or by the engine-external writer.
 */
class QuestObjectOwnerTrimContractTest {

	/**
	 * 逐件合同：questId / 物件（不得领奖、不得被发页）/ 领奖 owner（任务书领奖行 NPC）/ reward 轴权威值。
	 * Per quest: the objects (never claim, never page targets), the sole completion owner, and the
	 * authoritative reward-axis value.
	 */
	private record TrimCase(int questId, Set<Integer> objects, Set<Integer> completionOwners,
			int rewardVar0) {
	}

	private static final List<TrimCase> CASES = List.of(
		new TrimCase(1582, Set.of(700196), Set.of(204573), 2),
		new TrimCase(2232, Set.of(700061), Set.of(203613), 1),
		new TrimCase(2237, Set.of(700145), Set.of(832822), 0),
		new TrimCase(2307, Set.of(700247), Set.of(204378), 1),
		new TrimCase(2664, Set.of(700324), Set.of(204777), 4),
		new TrimCase(4004, Set.of(700340), Set.of(205128), 4),
		new TrimCase(4012, Set.of(700342), Set.of(730104), 0),
		new TrimCase(30211, Set.of(730275), Set.of(798941), 0),
		new TrimCase(30213, Set.of(730275), Set.of(798926, 798941), 0),
		new TrimCase(30311, Set.of(730275), Set.of(799322), 0));

	/** 物件不得成为领奖 owner：reward→complete 的路由集合必须与物件不相交。 */
	@Test
	void objectsAreNeverCompletionOwners() throws Exception {
		for (TrimCase trim : CASES) {
			QuestDefinition definition = definition(trim.questId()).definition();
			for (QuestTransition transition : definition.transitions()) {
				if (!"reward".equals(transition.sourceNode())
						|| !"complete".equals(transition.targetNode())
						|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
					continue;
				}
				assertTrue(!trim.objects().contains(talk.npcId()),
					() -> "quest " + trim.questId() + " object " + talk.npcId()
						+ " must not own completion (SELECT_QUEST tail page 10 resolves the object's own"
						+ " html name, which does not exist -> client load fail)");
			}
		}
	}

	/** 物件零回页：以物件为目标的任何迁移都不得带发页 after-commit（含收尾页 10 与奖励窗）。 */
	@Test
	void objectsAreNeverPageTargets() throws Exception {
		for (TrimCase trim : CASES) {
			QuestDefinition definition = definition(trim.questId()).definition();
			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
						|| !trim.objects().contains(talk.npcId())) {
					continue;
				}
				for (AfterCommitAction afterCommit : transition.afterCommit()) {
					assertTrue(!(afterCommit instanceof AfterCommitAction.ShowQuestDialog)
							&& !(afterCommit instanceof AfterCommitAction.ShowQuestSelectionDialog)
							&& !(afterCommit instanceof AfterCommitAction.ShowDialogWindow),
						() -> "quest " + trim.questId() + " sends page " + afterCommit + " to object "
							+ talk.npcId()
							+ "; pages and reward windows must stay on the journal reward-row NPC");
				}
			}
		}
	}

	/** 领奖 owner 唯一 = 任务书领奖行 NPC，且收尾页 10 落在该 NPC 自己的 html 上。 */
	@Test
	void completionOwnersAreTheJournalRewardRowNpc() throws Exception {
		for (TrimCase trim : CASES) {
			QuestDefinition definition = definition(trim.questId()).definition();
			Set<Integer> owners = new LinkedHashSet<>();
			for (QuestTransition transition : definition.transitions()) {
				if (!"reward".equals(transition.sourceNode())
						|| !"complete".equals(transition.targetNode())
						|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
					continue;
				}
				owners.add(talk.npcId());
				assertEquals(List.of(
					new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
					transition.afterCommit(),
					() -> "quest " + trim.questId() + " completion tail must be the selection page");
			}
			assertEquals(trim.completionOwners(), owners,
				() -> "quest " + trim.questId() + " completion owner set");
		}
	}

	/** 领奖态步号轴 = 落盘值（QE-054 legacy 值；QE-046 引擎外写入方值与基线 TSV 对齐，不按行号机械抬升）。 */
	@Test
	void rewardProjectionMatchesTheLegacyPersistedValue() throws Exception {
		for (TrimCase trim : CASES) {
			QuestDefinition definition = definition(trim.questId()).definition();
			QuestNode reward = definition.nodes().stream()
				.filter(candidate -> "reward".equals(candidate.label())).findFirst().orElseThrow();
			assertEquals(QuestStatus.REWARD, reward.projection().status());
			assertEquals(Map.of("var0", trim.rewardVar0()), reward.projection().variables(),
				() -> "quest " + trim.questId() + " reward axis must match the persisted writer value");
		}
	}

	/** 物件推进边存在且零回页：每个物件的 USE_OBJECT 迁移至少一条（纯采集掉落 / AI 接管对话的任务无路由）。 */
	@Test
	void objectRoutesAreOutgoingTalkRoutesOnly() throws Exception {
		// 纯采集掉落（无推进边）与对话整体由 AI 接管（riftorb AI）的任务：XML 里不得留物件路由。
		// Pure-collection objects and objects whose dialog is owned by an AI (riftorb): no XML routes.
		Set<Integer> withoutXmlDialogFace = Set.of(2232, 2237, 4012, 30211, 30213, 30311);
		for (TrimCase trim : CASES) {
			QuestDefinition definition = definition(trim.questId()).definition();
			List<QuestTransition> objectRoutes = definition.transitions().stream()
				.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& trim.objects().contains(talk.npcId()))
				.toList();
			if (withoutXmlDialogFace.contains(trim.questId())) {
				assertEquals(List.of(), objectRoutes,
					() -> "quest " + trim.questId()
						+ ": the object keeps no XML dialog routes (collection drop / AI-owned dialog)");
				continue;
			}
			assertTrue(!objectRoutes.isEmpty(),
				() -> "quest " + trim.questId() + " keeps the object advance route");
			for (QuestTransition route : objectRoutes) {
				QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) route.event();
				assertEquals(QuestDialogAction.USE_OBJECT.id(), talk.dialogId(),
					() -> "quest " + trim.questId() + " object routes may only be USE_OBJECT advances");
			}
		}
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestObjectOwnerTrimContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
