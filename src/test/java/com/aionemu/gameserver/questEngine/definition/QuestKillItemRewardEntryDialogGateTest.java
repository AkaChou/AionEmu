package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 门禁：击杀 / 用物事件把任务推进到 REWARD 时不得下发对话页。
 * Gate: kill / item events that advance a quest into REWARD must not push a dialog page.
 * <p>真端取证（ScriptDLL64.c 反编译源）：击杀处理器 {@code FUN_180ed93d0}/{@code FUN_180ed9410}
 * 经通用计数口 {@code FUN_180caa850}（3031，0xbd7；仅 0xf0 位域累加 + 0x120 计数通报）、
 * {@code FUN_180ed9330}（3056，0xbf0；0x100 推进 + 0x110 通报）、{@code FUN_180edded0}
 * （2001，0x7d1；变量阶梯 3..8 + 0x100 收尾 + 0x110 通报），用物处理器 {@code FUN_180f03f20}
 * （11006，0x2afe）、{@code FUN_180f00ac0}/{@code FUN_180f00b30}/{@code FUN_180f00ba0}
 * （11031–11033，0x2b17–0x2b19）一律只做推进 + 通告（0x2f8 / 0x2b8），没有任何 0x188 发页调用。
 * 这 11 条边在 2026-10-07 前带着「回选择页 10（SELECT_QUEST）」或「奖励窗（页 5）」的翻译夸大尾随——
 * 击杀 / 用物没有对话对象（targetObj=0），客户端收到 {@code SM_DIALOG_WINDOW(0, page)} 即 load fail
 * （实机报障：3031 完成击杀后弹出 load fail 对话）。</p>
 * <p>Retail forensics (decompiled ScriptDLL64.c): the kill handlers {@code FUN_180ed93d0}/{@code FUN_180ed9410}
 * through the generic counter port {@code FUN_180caa850} (3031, 0xbd7; only a 0xf0 bit-field increment plus a
 * 0x120 counter notify), {@code FUN_180ed9330} (3056, 0xbf0; 0x100 advance + 0x110 notify) and
 * {@code FUN_180edded0} (2001, 0x7d1; variable ladder 3..8 + 0x100 close + 0x110 notify), and the item-use
 * handlers {@code FUN_180f03f20} (11006, 0x2afe) and {@code FUN_180f00ac0}/{@code FUN_180f00b30}/
 * {@code FUN_180f00ba0} (11031–11033, 0x2b17–0x2b19) all perform only an advance plus a notice (0x2f8 / 0x2b8),
 * with no 0x188 page call anywhere. Before 2026-10-07 those 11 edges trailed a translation-exaggerated
 * "back to selection page 10 (SELECT_QUEST)" or "reward window (page 5)": a kill or item use has no dialog
 * object (targetObj=0), so {@code SM_DIALOG_WINDOW(0, page)} makes the client show a load-fail dialog
 * (live report: quest 3031 pops a load-fail dialog once the kills complete).</p>
 */
class QuestKillItemRewardEntryDialogGateTest {
	/**
	 * 逐任务镜像契约（本门禁的取证样本）：REWARD 入口边的 source 节点。
	 * Mirrored per-quest contracts (this gate's forensics samples): the source node of each REWARD entry edge.
	 */
	private static final List<RewardEntrySample> SAMPLES = List.of(
		new RewardEntrySample(3031, new QuestEvent.KillNpc(214219), "started"),
		new RewardEntrySample(3031, new QuestEvent.KillNpc(214220), "started"),
		new RewardEntrySample(3031, new QuestEvent.KillNpc(214222), "started"),
		new RewardEntrySample(3031, new QuestEvent.KillNpc(214223), "started"),
		new RewardEntrySample(3056, new QuestEvent.KillNpc(214578), "v1"),
		new RewardEntrySample(2001, new QuestEvent.KillNpc(210369), "k6"),
		new RewardEntrySample(2001, new QuestEvent.KillNpc(210368), "k6"),
		new RewardEntrySample(11006, new QuestEvent.UseItem(182206705), "v2"),
		new RewardEntrySample(11031, new QuestEvent.UseItem(182206724), "v2"),
		new RewardEntrySample(11032, new QuestEvent.UseItem(182206726), "v2"),
		new RewardEntrySample(11033, new QuestEvent.UseItem(182206728), "v2"));

	@TestFactory
	Stream<DynamicTest> sampledRewardEntriesCarryTheSyncOnlyAfterCommit() {
		return SAMPLES.stream().map(sample -> DynamicTest.dynamicTest(
			"quest " + sample.questId() + " " + sample.event(), () -> assertSample(sample)));
	}

	/**
	 * 3031 的完成击杀仍必须进入 REWARD：删除发页不许动状态推进与计数写回。
	 * Quest 3031's completing kill must still enter REWARD: removing the page must not touch the state
	 * advance or the counter write-back.
	 */
	@Test
	void completingKillOfQuest3031StillEntersReward() throws Exception {
		CompiledQuestDefinition compiled = load(3031);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		QuestEvent kill = new QuestEvent.KillNpc(214219);
		QuestTransition transition = transition(definition, "started", "reward", kill);

		QuestSnapshot before = new QuestSnapshot(7, 3031, QuestStatus.START,
			layout.pack(Map.of("var1", 14, "var2", 12)), Map.of());
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, before, kill, transition).orElseThrow(
			() -> new AssertionError("quest 3031 completing kill must still match the started -> reward edge"));
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(Map.of("var1", 15, "var2", 12), layout.unpack(plan.nextPackedVariables()));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), transition.afterCommit());
	}

	/**
	 * 生产目录全量：任何击杀 / 用物事件进入 REWARD 的边都不得带对话页收尾。
	 * Whole production catalog: no kill / item edge into REWARD may carry a dialog-page after-commit.
	 */
	@Test
	void noKillOrItemRewardEntryInTheProductionCatalogCarriesADialogPage() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> defects = new ArrayList<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Map<String, QuestStatus> statuses = definition.nodes().stream()
				.collect(Collectors.toMap(QuestNode::label, node -> node.projection().status()));
			for (QuestTransition transition : definition.transitions()) {
				if (!isKillOrItemEvent(transition.event())) {
					continue;
				}
				if (statuses.get(transition.targetNode()) != QuestStatus.REWARD) {
					continue;
				}
				if (transition.afterCommit().stream().anyMatch(QuestKillItemRewardEntryDialogGateTest::isDialogPage)) {
					defects.add(compiled.id() + " " + transition.sourceNode() + "->"
						+ transition.targetNode() + " " + transition.event());
				}
			}
		}
		assertEquals(List.of(), defects,
			"a kill/item REWARD entry has no dialog object, so a dialog page makes the client show load fail;"
				+ " retail kill/item handlers send no 0x188 page (see the class evidence)");
	}

	private static void assertSample(RewardEntrySample sample) {
		QuestDefinition definition = load(sample.questId()).definition();
		QuestTransition entry = transition(definition, sample.sourceNode(), "reward", sample.event());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), entry.afterCommit());
		assertTrue(entry.afterCommit().stream().noneMatch(QuestKillItemRewardEntryDialogGateTest::isDialogPage));
	}

	/** 击杀 / 用物事件族：真端这些处理器族已逐一体检为发页静默。 Kill/item event families, each verified silent in retail. */
	private static boolean isKillOrItemEvent(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc
			|| event instanceof QuestEvent.KillNpcSet
			|| event instanceof QuestEvent.UseItem
			|| event instanceof QuestEvent.ItemPlay;
	}

	private static boolean isDialogPage(AfterCommitAction action) {
		return action instanceof AfterCommitAction.ShowQuestDialog
			|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
			|| action instanceof AfterCommitAction.ShowDialogWindow;
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest/definitions/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestKillItemRewardEntryDialogGateTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}

	/** 取证样本：任务 + 事件 + 事件的 source 节点。 One forensics sample: quest + event + the event's source node. */
	private record RewardEntrySample(int questId, QuestEvent event, String sourceNode) {
	}
}
