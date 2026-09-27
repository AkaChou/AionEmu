package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestEventIndex;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 五个阿斯莫升级型职业奖励任务的无目标奖励确认合同。
 * Targetless reward-confirmation contract for the five Asmodian level-up class-reward quests.
 */
class Quest23830To23834TargetlessRewardTest {
	private static final List<PlayerClass> CLASSES = List.of(
		PlayerClass.GLADIATOR, PlayerClass.TEMPLAR, PlayerClass.RANGER, PlayerClass.ASSASSIN,
		PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER, PlayerClass.CLERIC, PlayerClass.CHANTER,
		PlayerClass.GUNSLINGER, PlayerClass.SONGWEAVER, PlayerClass.AETHERTECH);

	private static final List<Spec> SPECS = List.of(
		new Spec(23830, 182216123, 46544, false,
			List.of(140001109, 140001130, 140001168, 140001145, 140001189, 140001202,
				140001236, 140001221, 140001253, 140001289, 140001271)),
		new Spec(23831, 182216124, 337255, false,
			List.of(140001111, 140001128, 140001171, 140001142, 140001186, 140001203,
				140001240, 140001220, 140001254, 140001290, 140001275)),
		new Spec(23832, 182216125, 555019, true,
			List.of(140001105, 140001123, 140001154, 140001136, 140001177, 140001196,
				140001231, 140001216, 140001249, 140001284, 140001264)),
		new Spec(23833, 182216126, 731094, false,
			List.of(140001103, 140001124, 140001156, 140001137, 140001176, 140001198,
				140001228, 140001214, 140001247, 140001282, 140001265)),
		new Spec(23834, 182216127, 1005193, true,
			List.of(140001118, 140001135, 140001173, 140001151, 140001192, 140001210,
				140001245, 140001227, 140001262, 140001296, 140001279)));

	@Test
	void compilesNormalAndRealtimeTargetlessRewardsWithClassRewardsAndCloseOrder() throws Exception {
		for (Spec spec : SPECS) {
			CompiledQuestDefinition compiled = load(spec.questId());
			QuestEventIndex eventIndex = new QuestEventIndex(new ImmutableQuestCatalog(List.of(compiled)));
			for (int classIndex = 0; classIndex < CLASSES.size(); classIndex++) {
				PlayerClass playerClass = CLASSES.get(classIndex);
				int rewardId = spec.rewardIds().get(classIndex);
				// DD canonical：对话页确认通道（8）与奖励窗自动确认通道（110）都按职业梯展开，
				// 职业路由优先级 = 职业序号（与遗留 priority=0..10 形对齐）。
				// DD canonical: both the talk-page confirm channel (8) and the reward-window
				// auto-confirm channel (110) expand per class ladder; class-route priority = class
				// index (aligned with the legacy priority 0..10 shape).
				assertTargetlessRoute(compiled, eventIndex, spec, playerClass, rewardId,
					QuestDialogAction.SELECTED_QUEST_REWARD1.id(), classIndex);
				assertTargetlessRoute(compiled, eventIndex, spec, playerClass, rewardId,
					QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id(), classIndex);

				QuestTransition npc = route(compiled, new QuestEvent.TalkToNpc(204061, 8), playerClass,
					classIndex);
				assertEquals("reward", npc.sourceNode());
				assertEquals("complete", npc.targetNode());
				// 动作两形等价（work item 清理定义尾追加或 planner 追加）。
				// Both cleanup placements are equivalent (definition tail or planner appended).
				List<QuestAction> expected = expectedActions(spec, rewardId);
				List<QuestAction> withoutCleanup = expected.stream()
					.filter(action -> !(action instanceof QuestAction.RemoveItem))
					.toList();
				assertTrue(npc.actions().equals(expected) || npc.actions().equals(withoutCleanup),
					() -> "unexpected completion actions " + npc.actions());
				assertEquals(List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
					npc.afterCommit());
			}
		}
	}

	private static void assertTargetlessRoute(CompiledQuestDefinition compiled, QuestEventIndex index,
			Spec spec, PlayerClass playerClass, int rewardId, int dialogId, Integer priority) {
		// DD 形按 NPC 键注册路由（遗留形用 npc 无关的 QuestDialog 键）：交付 NPC 从产物
		// reward→complete 路由自推。
		// DD routes register under the npc key (the legacy shape used the npc-agnostic QuestDialog
		// key); the reward npc derives from the compiled reward→complete routes.
		int rewardNpc = compiled.definition().transitions().stream()
			.filter(t -> "reward".equals(t.sourceNode()) && "complete".equals(t.targetNode()))
			.map(t -> t.event())
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.mapToInt(QuestEvent.TalkToNpc::npcId)
			.findFirst().orElseThrow();
		QuestEvent event = new QuestEvent.TalkToNpc(rewardNpc, dialogId);
		assertTrue(index.routesFor(event, spec.questId()).stream()
			.anyMatch(candidate -> candidate.transition().conditions()
				.contains(new QuestCondition.AdvancedClassIs(playerClass))));
		QuestTransition targetless = route(compiled, event, playerClass, priority);

		assertEquals("reward", targetless.sourceNode());
		assertEquals("complete", targetless.targetNode());
		// work item 清理两形等价（定义尾追加或 planner 提交时追加）。
		// Both cleanup placements are equivalent (definition tail or planner at commit time).
		List<QuestAction> expected = expectedActions(spec, rewardId);
		List<QuestAction> withoutCleanup = expected.stream()
			.filter(action -> !(action instanceof QuestAction.RemoveItem))
			.toList();
		assertTrue(targetless.actions().equals(expected)
			|| targetless.actions().equals(withoutCleanup),
			() -> "unexpected completion actions " + targetless.actions());
		// 通道分形 afterCommit：对话页确认回任务选择页；奖励窗自动确认以 CloseDialog 收窗
		// （遗留 targetless 契约形）。
		// Channel-shaped afterCommit: the talk-page confirm returns to the quest-selection page;
		// the reward-window auto-confirm closes the window (the legacy targetless contract shape).
		boolean windowChannel = dialogId == QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()
			|| dialogId >= QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id();
		List<AfterCommitAction> expectedAfterCommit = windowChannel
			? List.of(new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.CloseDialog())
			: List.of(new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
		assertEquals(expectedAfterCommit, targetless.afterCommit());
		if (windowChannel) {
			// 奖励窗自动确认按双协议注册：同一逻辑路由必须同时存在 QuestDialog 无主键形。
			// The reward-window auto-confirm registers under both protocols: the same logical route
			// must also exist in the npc-agnostic QuestDialog form.
			assertTrue(compiled.definition().transitions().stream()
				.filter(transition -> transition.event().equals(new QuestEvent.QuestDialog(dialogId)))
				.anyMatch(transition -> transition.conditions()
					.contains(new QuestCondition.AdvancedClassIs(playerClass))),
				"missing QuestDialog dual-protocol route for dialogId " + dialogId);
		}

		var source = compiled.definition().nodes().stream()
			.filter(node -> node.label().equals("reward")).findFirst().orElseThrow();
		var complete = compiled.definition().nodes().stream()
			.filter(node -> node.label().equals("complete")).findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, source.projection().status());
		assertEquals(Map.of("var0", 1), source.projection().variables());
		assertEquals(QuestStatus.COMPLETE, complete.projection().status());
		assertEquals(Map.of("var0", 0), complete.projection().variables());

		int packed = compiled.definition().progressLayout().pack(Map.of("var0", 1));
		QuestSnapshot snapshot = new QuestSnapshot(7, spec.questId(), QuestStatus.REWARD, packed,
			Map.of(spec.workItem(), 1)).withPlayerClass(playerClass);
		var plan = QuestMutationPlanner.plan(compiled, snapshot, event, targetless).orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(compiled.definition().progressLayout().pack(Map.of("var0", 0)),
			plan.nextPackedVariables());
		// work item 清理两形等价：定义携带或 planner 追加，去清理后必须逐项一致。
		// Both cleanup placements are equivalent: definition-carried or planner-appended — the
		// cleanup-stripped lists must match exactly.
		java.util.function.Function<List<QuestAction>, List<QuestAction>> stripCleanup = list ->
			list.stream().filter(action -> !(action instanceof QuestAction.RemoveItem remove
				&& remove.itemId() == spec.workItem() && remove.removeAll())).toList();
		assertEquals(stripCleanup.apply(targetless.actions()),
			stripCleanup.apply(plan.requiredActions()));
		assertEquals(targetless.afterCommit(), plan.afterCommit());
	}

	private static List<QuestAction> expectedActions(Spec spec, int rewardId) {
		// DD canonical 形：固定奖励（元数据序）在前、职业物品次之、CompleteQuest 收尾；
		// work item 的完成清理由 planner 追加（RemoveItem(ALL) 不进定义）。
		// DD canonical shape: fixed rewards (metadata order) first, then the class item, closed by
		// CompleteQuest; the planner appends the work-item completion cleanup (RemoveItem(ALL)
		// stays out of the definition).
		QuestAction item = new QuestAction.GrantReward("ITEM", rewardId, 1);
		QuestAction exp = new QuestAction.GrantReward("EXP", 0, spec.exp(), QuestRewardAmountMode.QUEST_BASE);
		QuestAction complete = new QuestAction.CompleteQuest(0);
		QuestAction removeItem = new QuestAction.RemoveItem(spec.workItem(), QuestAction.RemoveItem.ALL);
		// work item 清理两形等价（定义尾追加或 planner 提交时追加）。
		// Both cleanup placements are equivalent (definition tail or planner at commit time).
		return spec.itemFirst()
			? List.of(item, exp, complete, removeItem)
			: List.of(exp, item, complete, removeItem);
	}

	private static QuestTransition route(CompiledQuestDefinition compiled, QuestEvent event,
			PlayerClass playerClass, Integer priority) {
		return compiled.definition().transitions().stream()
			.filter(transition -> transition.event().equals(event))
			.filter(transition -> transition.sourceNode().equals("reward"))
			.filter(transition -> transition.conditions().contains(new QuestCondition.AdvancedClassIs(playerClass)))
			.filter(transition -> priority == null || priority.equals(transition.priority()))
			.findFirst().orElseThrow();
	}

	// 退役任务统一走生产视图（真端 overlay 合成；旧 XML 只在 git 历史里）。
	// Retired quests resolve through the production view (retail overlay; the old XML lives in
	// git history only).
	private static CompiledQuestDefinition load(int questId) throws Exception {
		return ProductionQuestDefinitions.definition(questId);
	}

	// DD canonical：职业路由优先级统一为职业序号（遗留分任务旗标随两通道归一而废弃）；itemFirst
	// 保留真端元数据序（该族部分行物品先于经验）。
	// DD canonical: class-route priorities are uniformly the class index (the per-quest legacy
	// flags are obsolete now that both channels share the shape); itemFirst keeps the retail
	// metadata order (some rows grant the item before EXP).
	private record Spec(int questId, int workItem, long exp, boolean itemFirst, List<Integer> rewardIds) {
	}
}
