package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 23：39713（[Daily] Fresh Powder）绿帽团阵营日任的任务书行号阶梯，并用魔族已对齐的镜像 49713
 * 作横向对照。
 * <p>
 * 族级判据：两侧客户端 {@code quest_q39713.html} / {@code quest_q49713.html} 的 {@code quest_summary}
 * 都是 3 行（行 0「和绿林团南部卡塔拉姆支部对话」、行 1「把净化粉末撒在遗忘沼泽的污染根源上」、
 * 行 2「向绿林团南部卡塔拉姆支部报告」），页与按钮也同形（{@code select2} = HACTION_SETPRO1、
 * {@code select5} = HACTION_SELECT_QUEST_REWARD、{@code ask_quest_accept} = HACTION_FINISH_DIALOG）；
 * 支部成员由客户端字符串 {@code STR_DIC_E_LDF5a_Greenhat_BA} 独立锁定为
 * 800936/800937/800938（LDF5b_Ubarung/Dieroonroon/Argarung_Greenhat）。
 * <p>
 * 魔族侧 49713 已是行号阶梯；天族侧 39713 在迁移时把整条阶梯塌陷成 9 条无守卫的
 * {@code started -> reward} 直跳（npc-item-report ×3 + SET_SUCCEED ×3 + SELECT_QUEST_REWARD ×3），
 * 于是行 1、行 2 没有任何 START/REWARD 状态（客户端那两行永远不亮）、{@code started --QUEST_SELECT}
 * 错开成报告页 select5、reward 投影停在 0 而客户端领奖行是 2（QE-051 行错位），报告路由还重复要求
 * 已经用掉的净化粉末。
 * <p>
 * 本门禁锁：两侧 3 行各有一个 START/REWARD 状态、行 1 由三名支部成员任一位发放本侧粉末并推进一步、
 * 行 2 由 {@code use-item} 消耗粉末后报告、领奖路由只挂在行 2（{@code powder-used -> reward}）且
 * reward 投影 = 2、旧存档（REWARD + var0=0）在进入世界时自愈到 2，并保持两侧物品/接取口径互不污染。
 * <p>
 * Locks batch 23: the three-row journal ladder of the Greenhat faction daily. 49713 is the already-aligned
 * Asmodian baseline; 39713 had collapsed all three rows into nine unguarded {@code started -> reward}
 * jumps, so rows 1 and 2 had no state, the first row opened the wrong client page and the reward
 * projection stayed on row 0. The gate pins both variants' ladder, item hand-out/consumption, the
 * report-row reward route (QE-051), the legacy reward-row self-heal and the per-side item isolation.
 */
class FactionDailyRowLadderContractTest {

	/** 天族 / 魔族镜像 / 各自的任务道具 / 支部三名成员 / 领奖行与旧投影。 */
	private static final int ELYOS = 39713;
	private static final int ASMODIAN = 49713;
	private static final int ELYOS_POWDER = 182215285;
	private static final int ASMODIAN_POWDER = 182215277;
	private static final Set<Integer> BRANCH = Set.of(800936, 800937, 800938);
	private static final int REWARD_ROW = 2;
	private static final int LEGACY_REWARD_PROJECTION = 0;

	@Test
	void everyClientRowOwnsOneLadderState() throws Exception {
		Map<String, Integer> rowProjection = new LinkedHashMap<>();
		rowProjection.put("unaccepted", 0);
		rowProjection.put("started", 0);
		rowProjection.put("powder-received", 1);
		rowProjection.put("powder-used", 2);
		rowProjection.put("reward", REWARD_ROW);
		rowProjection.put("complete", 0);
		for (int questId : List.of(ELYOS, ASMODIAN)) {
			QuestDefinition definition = definition(questId).definition();
			assertEquals(rowProjection.keySet(),
				definition.nodes().stream().map(QuestNode::label).collect(java.util.stream.Collectors.toSet()),
				() -> "quest " + questId + " node shape");
			for (Map.Entry<String, Integer> entry : rowProjection.entrySet()) {
				QuestNode node = node(definition, entry.getKey());
				assertEquals(entry.getValue(), node.projection().variables().get("var0"),
					() -> "quest " + questId + " node " + entry.getKey() + " journal row projection");
			}
			assertEquals(QuestStatus.NONE, node(definition, "unaccepted").projection().status());
			for (String label : List.of("started", "powder-received", "powder-used")) {
				assertEquals(QuestStatus.START, node(definition, label).projection().status(),
					() -> "quest " + questId + " node " + label + " must stay a START row");
			}
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status());
			assertEquals(QuestStatus.COMPLETE, node(definition, "complete").projection().status());

			BitField field = definition.progressLayout().field("var0");
			assertNotNull(field, () -> "quest " + questId + " must declare var0 as the journal row index");
			assertEquals(0, field.offset(), () -> "quest " + questId + " var0 must stay in SECTION_0");
			assertEquals(3, field.maxValue(),
				() -> "quest " + questId + " var0 must be sized like its twin (2 bits, max 3)");

			/* 客户端三行都必须有可见状态，缺口行在游戏里永远不会高亮（QE-051）。 */
			/* All three client rows must own a visible state; a gap row never lights up (QE-051). */
			assertEquals(Set.of(0, 1, REWARD_ROW), visibleRows(definition),
				() -> "quest " + questId + " visible journal rows");
		}
	}

	@Test
	void branchMembersHandOutThePowderOnRowOne() throws Exception {
		for (int questId : List.of(ELYOS, ASMODIAN)) {
			QuestDefinition definition = definition(questId).definition();
			int powder = powder(questId);

			assertEquals(BRANCH, npcIds(definition, "started", "powder-received"),
				() -> "quest " + questId + " row 1 must be reachable from every branch member");
			for (QuestTransition receive : routes(definition, "started", "powder-received")) {
				QuestEvent.TalkToNpc talk = assertInstanceOf(QuestEvent.TalkToNpc.class, receive.event());
				assertEquals(QuestDialogAction.SETPRO1.id(), talk.dialogId(),
					() -> "quest " + questId + " row 1 is opened by the client SETPRO1 button");
				assertEquals(List.of(new QuestAction.GiveItem(powder, 1),
					new QuestAction.SetVariable("var0", 1)), receive.actions(),
					() -> "quest " + questId + " row 1 hands out the powder and advances exactly one row");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()), receive.afterCommit(),
					() -> "quest " + questId + " row 1 must only refresh the journal");
			}

			/* 行 0 的 QUEST_SELECT 必须打开领取粉末的 select2 页，而不是报告页（旧缺陷开的是 select5）。 */
			/* Row 0's QUEST_SELECT must open the powder hand-out page select2, not the report page select5. */
			assertEquals(BRANCH, npcIds(definition, "started", "started"),
				() -> "quest " + questId + " row 0 dialog route owners");
			for (QuestTransition select : routes(definition, "started", "started")) {
				QuestEvent.TalkToNpc talk = assertInstanceOf(QuestEvent.TalkToNpc.class, select.event());
				assertEquals(QuestDialogAction.QUEST_SELECT.id(), talk.dialogId());
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
					select.afterCommit(), () -> "quest " + questId + " row 0 must show select2");
			}
		}
	}

	@Test
	void usingThePowderAdvancesToTheReportRow() throws Exception {
		for (int questId : List.of(ELYOS, ASMODIAN)) {
			QuestDefinition definition = definition(questId).definition();
			int powder = powder(questId);

			QuestTransition use = route(definition, "powder-received", "powder-used");
			assertEquals(1, routes(definition, "powder-received", "powder-used").size(),
				() -> "quest " + questId + " must have exactly one powder-use step");
			QuestEvent.UseItem useItem = assertInstanceOf(QuestEvent.UseItem.class, use.event());
			assertEquals(powder, useItem.itemId(), () -> "quest " + questId + " powder item");
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1),
				new QuestCondition.HasItem(powder, 1, true)), use.conditions(),
				() -> "quest " + questId + " powder use requires row 1 and the item in hand");
			assertEquals(List.of(new QuestAction.RemoveItem(powder, 1),
				new QuestAction.SetVariable("var0", 2)), use.actions(),
				() -> "quest " + questId + " powder use consumes the item and advances exactly one row");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				use.afterCommit(), () -> "quest " + questId + " powder use must only refresh the journal");

			/* 行 2 的报告页自环：玩家在粉已撒完后再次点任务行看到 select5。 */
			/* The report-page self loop of row 2: after the powder is used the journal row offers select5. */
			for (QuestTransition select : routes(definition, "powder-used", "powder-used")) {
				QuestEvent.TalkToNpc talk = assertInstanceOf(QuestEvent.TalkToNpc.class, select.event());
				assertEquals(QuestDialogAction.QUEST_SELECT.id(), talk.dialogId());
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
					select.afterCommit(), () -> "quest " + questId + " row 2 must show select5");
			}
			assertEquals(BRANCH, npcIds(definition, "powder-used", "powder-used"),
				() -> "quest " + questId + " report-row dialog owners");
		}
	}

	@Test
	void rewardRouteLivesOnTheReportRow() throws Exception {
		for (int questId : List.of(ELYOS, ASMODIAN)) {
			QuestDefinition definition = definition(questId).definition();
			/* 旧塌陷：任何 started -> reward 的无守卫直跳都必须保持清零，否则行 1/行 2 又会消失。 */
			/* The legacy collapse: no unguarded started -> reward jump may come back (it kills rows 1 and 2). */
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + questId + " must not jump from row 0 straight to the reward row");
			assertEquals(BRANCH, npcIds(definition, "powder-used", "reward"),
				() -> "quest " + questId + " reward row must be reachable from every branch member");
			for (QuestTransition report : routes(definition, "powder-used", "reward")) {
				assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", REWARD_ROW)),
					report.conditions(),
					() -> "quest " + questId + " reward route may only fire on the report row");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
					report.afterCommit(),
					() -> "quest " + questId + " reward route must refresh the journal and open the window");
			}
			/* 领奖与完成 owner 保持三名支部成员（QE-052）。 */
			/* The reward and completion owners stay on the three branch members (QE-052). */
			assertEquals(BRANCH, npcIds(definition, "reward", "complete"),
				() -> "quest " + questId + " completion owner trim");
			assertEquals(BRANCH, npcIds(definition, "reward", "reward"),
				() -> "quest " + questId + " reward preview owners");
		}
	}

	@Test
	void rowsAdvanceSeriallyAndOutOfOrderEventsProduceNoPlan() throws Exception {
		for (int questId : List.of(ELYOS, ASMODIAN)) {
			CompiledQuestDefinition compiled = definition(questId);
			QuestDefinition definition = compiled.definition();
			int powder = powder(questId);

			QuestTransition receive = firstRoute(definition, "started", "powder-received");
			QuestMutationPlan received = plan(compiled, QuestStatus.START, 0, receive, Map.of());
			assertNotNull(received, () -> "quest " + questId + " row 0 must advance to row 1");
			assertEquals(1, row(compiled, received), () -> "quest " + questId + " row 0 -> row 1");

			QuestTransition use = route(definition, "powder-received", "powder-used");
			QuestMutationPlan used = plan(compiled, QuestStatus.START, 1, use, Map.of(powder, 1));
			assertNotNull(used, () -> "quest " + questId + " row 1 must advance to row 2");
			assertEquals(2, row(compiled, used), () -> "quest " + questId + " row 1 -> row 2");
			assertNull(plan(compiled, QuestStatus.START, 0, use, Map.of(powder, 1)),
				() -> "quest " + questId + " powder use must not fire from row 0");
			assertNull(plan(compiled, QuestStatus.START, 1, use, Map.of()),
				() -> "quest " + questId + " powder use without the item must not plan");

			QuestTransition report = firstRoute(definition, "powder-used", "reward");
			QuestMutationPlan reported = plan(compiled, QuestStatus.START, REWARD_ROW, report, Map.of());
			assertNotNull(reported, () -> "quest " + questId + " row 2 must enter the reward state");
			assertEquals(QuestStatus.REWARD, reported.nextStatus());
			assertEquals(REWARD_ROW, row(compiled, reported),
				() -> "quest " + questId + " reward row comes from the target projection");
			assertNull(plan(compiled, QuestStatus.START, 1, report, Map.of()),
				() -> "quest " + questId + " report must not fire from row 1");
		}
	}

	@Test
	void legacyRewardSavesAreRepairedOnEnterWorld() throws Exception {
		CompiledQuestDefinition compiled = definition(ELYOS);
		List<QuestTransition> recovery = compiled.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterWorld)
			.toList();
		assertEquals(1, recovery.size(), "exactly one enter-world reward recovery route for 39713");
		QuestTransition heal = recovery.getFirst();
		assertEquals(List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", LEGACY_REWARD_PROJECTION)), heal.conditions(),
			"the recovery route must target the legacy reward projection 0");
		assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
			"the recovery route must write the reward row 2");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
			"the recovery route must refresh the journal");

		QuestMutationPlan repaired = plan(compiled, QuestStatus.REWARD, LEGACY_REWARD_PROJECTION, heal,
			Map.of());
		assertNotNull(repaired, "a REWARD save on the legacy projection must be repaired");
		assertEquals(REWARD_ROW, row(compiled, repaired), "repaired reward row");
		assertEquals(QuestStatus.REWARD, repaired.nextStatus(), "repair keeps the reward status");
		assertNull(plan(compiled, QuestStatus.REWARD, REWARD_ROW, heal, Map.of()),
			"an already repaired reward save must not be rewritten");

		/* 魔族镜像本来就把领奖态写在行 2，不需要自愈边；这里显式锁住，防止机械对齐。 */
		/* The Asmodian twin already persisted row 2, so it needs no recovery edge; keep that explicit. */
		assertTrue(definition(ASMODIAN).definition().transitions().stream()
			.noneMatch(candidate -> candidate.sourceNode() == null
				&& "reward".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.EnterWorld),
			"49713 must not grow a spurious legacy recovery edge");
	}

	@Test
	void variantsKeepTheirOwnItemsAndAcceptPaths() throws Exception {
		assertEquals(Set.of(ELYOS_POWDER), itemIds(definition(ELYOS).definition()),
			"39713 may only touch the Elyos powder");
		assertEquals(Set.of(ASMODIAN_POWDER), itemIds(definition(ASMODIAN).definition()),
			"49713 may only touch the Asmodian powder");

		/* 接取口径各自独立：天族走支部 NPC 对话，魔族走无目标 QUEST_ACTION，本批不得互相污染。 */
		/* Accept paths stay per side: Elyos accepts through the branch NPCs, Asmodian through a targetless
		   QUEST_ACTION; this batch must not cross-pollinate them. */
		assertTrue(definition(ELYOS).definition().transitions().stream()
			.noneMatch(candidate -> candidate.event() instanceof QuestEvent.QuestDialog),
			"39713 accepts through branch NPC dialogs, not a targetless quest action");
		assertTrue(definition(ASMODIAN).definition().transitions().stream()
			.anyMatch(candidate -> candidate.event() instanceof QuestEvent.QuestDialog),
			"49713 keeps its targetless quest-action accept path");
	}

	private static int powder(int questId) {
		return questId == ELYOS ? ELYOS_POWDER : ASMODIAN_POWDER;
	}

	private static Set<Integer> visibleRows(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START
				|| node.projection().status() == QuestStatus.REWARD)
			.map(node -> node.projection().variables().get("var0"))
			.filter(java.util.Objects::nonNull)
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<Integer> itemIds(QuestDefinition definition) {
		Set<Integer> ids = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.event() instanceof QuestEvent.UseItem useItem) {
				ids.add(useItem.itemId());
			}
			for (QuestCondition condition : transition.conditions()) {
				if (condition instanceof QuestCondition.HasItem hasItem) {
					ids.add(hasItem.itemId());
				}
			}
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.GiveItem give) {
					ids.add(give.itemId());
				}
				if (action instanceof QuestAction.RemoveItem remove) {
					ids.add(remove.itemId());
				}
			}
		}
		return ids;
	}

	private static Set<Integer> npcIds(QuestDefinition definition, String source, String target) {
		return routes(definition, source, target).stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).npcId())
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.toList();
	}

	/** 一名支部成员一条路由时取第一条；/ First of the per-member routes. */
	private static QuestTransition firstRoute(QuestDefinition definition, String source, String target) {
		return routes(definition, source, target).getFirst();
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target) {
		List<QuestTransition> matches = routes(definition, source, target);
		assertEquals(1, matches.size(), () -> "expected one route " + source + " -> " + target);
		return matches.getFirst();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition compiled, QuestStatus status,
			int currentRow, QuestTransition transition, Map<Integer, Integer> inventory) {
		Map<String, Integer> variables = new LinkedHashMap<>(
			compiled.definition().progressLayout().unpack(0));
		variables.put("var0", currentRow);
		QuestSnapshot snapshot = new QuestSnapshot(7, compiled.definition().id(), status,
			compiled.definition().progressLayout().pack(variables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
		return QuestMutationPlanner.plan(compiled, snapshot, transition.event(), transition).orElse(null);
	}

	private static int row(CompiledQuestDefinition compiled, QuestMutationPlan plan) {
		return compiled.definition().progressLayout().unpack(plan.nextPackedVariables()).get("var0");
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = FactionDailyRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
