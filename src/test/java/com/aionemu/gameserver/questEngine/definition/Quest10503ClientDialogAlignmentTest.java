package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 10503「提亚马特结界守护者 / Guard Down, Secrets Out」步骤 1 杀怪计数清零、步骤 2 收集交付与步骤 3 对话续接的客户端对齐合同。
 * Locks the step 1 kill counter reset, step 2 collection handover, and step 3 dialog continuation client contract for quest 10503.
 * <p>Aion 5.8 客户端在 collect_progress=2 时以 progress==2 (var0==2) 校验背包装有古老龙族文件 (182215603)，
 * 并在尤碧亚 (804705) 头顶显示任务标记，进入 select3(1693) -> check_user_has_quest_item(39)。
 * 步骤 1 (s1) 杀怪完成推进到 s2 时，必须显式将 var1 计数清零，严禁残留 var1=2 污染打包整型为 130（导致客户端无法对齐 step 2 对白）。
 * 采集行 s2 接受古书对象 (702670) 交互（真端形：TalkToNpc + CanAct 成对），行内关窗走 FINISH_DIALOG(1008)。
 * 交付成功后在事务内扣除 182215603 并推进 var0=3，展示 check_user_item_ok(10000)，随后通过 SELECT4(2034)
 * 衔接解读对话，最终以 SETPRO4(10003) 升行至 s4；解读好的龙族文件 (182215604) 是 quest.xml quest_work_item1，
 * 真端模型在接取事务内发放、由 s4 的 ItemPlay 演出消费（planner 完成/放弃回收）。</p>
 */
class Quest10503ClientDialogAlignmentTest {
	private static final int EUVIA_NPC_ID = 804705;
	private static final int ANCIENT_DOC_ITEM_ID = 182215603;
	private static final int TRANSLATED_DOC_ITEM_ID = 182215604;
	private static final int GATHER_OBJECT_ID = 702670;
	private static final int TIAMAT_GUARD_NPC_ID = 236252;

	@Test
	void step1KillStep2CollectionHandoverAndStep3DialogContinuationContract() throws Exception {
		QuestDefinition definition = definition();

		// 1. 步骤 1 杀怪完成转移至 s2 (priority 0): 必须同时设置 var0=2 并清零 var1=0，确保打包步数纯净为 2
		// Step 1 kill completion to s2: must set var0=2 and clear var1=0 so the packed step is purely 2
		QuestTransition killCompletion = definition.transitions().stream()
			.filter(t -> "s1".equals(t.sourceNode()) && "s2".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.KillNpc kill && kill.npcId() == TIAMAT_GUARD_NPC_ID
				&& Integer.valueOf(0).equals(t.priority()))
			.findFirst().orElseThrow();
		assertEquals(Map.of("var0", 1), nodeVariables(definition, "s1"),
			"the retail row guard lives in the s1 node projection");
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 2)), killCompletion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 2),
			new QuestAction.SetVariable("var1", 0)), killCompletion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			killCompletion.afterCommit());

		// 2. 采集行 s2 接受古书对象交互：真端把交互成对挂在掉落生效行（TalkToNpc(702670, dialogId 空)
		// + CanAct(702670, ACTION_ITEM_USE)，无 after-commit）；关窗由行内 FINISH_DIALOG(1008) 出口承担、
		// 步同步由交付推进边（PACKET_ONLY）承担——遗留形把两者都压在物件交互上。
		// The collect row s2 accepts the papyrus interaction: the retail chain hangs the pair
		// TalkToNpc(702670, null dialogId) + CanAct(702670, ACTION_ITEM_USE) on the drop's live row (no
		// after-commit); closing is the row's FINISH_DIALOG (1008) exit and the step sync belongs to the
		// hand-over advance (PACKET_ONLY) — the legacy shape folded both onto the object interaction.
		boolean gatherObject = definition.transitions().stream()
			.anyMatch(t -> "s2".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == GATHER_OBJECT_ID)
			&& definition.transitions().stream()
			.anyMatch(t -> "s2".equals(t.sourceNode()) && t.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == GATHER_OBJECT_ID);
		assertTrue(gatherObject, "the collect row must accept the papyrus interaction (702670)");
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			talk(definition, "s2", "s2", EUVIA_NPC_ID, QuestDialogAction.FINISH_DIALOG.id()).afterCommit());

		// 3. 不存在拾取道具时的抢跑 get-item 转移
		// No premature get-item transition must exist when looting the ancient document
		boolean hasGetItemTransition = definition.transitions().stream()
			.anyMatch(t -> t.event() instanceof QuestEvent.GetItem gi && gi.itemId() == ANCIENT_DOC_ITEM_ID);
		assertFalse(hasGetItemTransition, "quest 10503 must not prematurely advance var0 via get-item on looting");

		// 4. 步骤 2: 尤碧亚 QUEST_SELECT 必须在 s2 展示 SELECT3(1693)
		// Step 2: Euvia QUEST_SELECT in s2 must show SELECT3 (1693)
		QuestTransition select3 = talk(definition, "s2", "s2", EUVIA_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())),
			select3.afterCommit());

		// 5. 步骤 2 交付成功 (priority 0): 在 s2 校验 var0=2 与持有 182215603，扣除道具并推进 var0=3 进入 s3
		// Step 2 handover success: verifies var0=2 and has ancient doc, removes doc and sets var0=3 to s3
		QuestTransition handOverSuccess = transition(definition, "s2", "s3",
			new QuestEvent.TalkToNpc(EUVIA_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 0);
		assertEquals(Map.of("var0", 2), nodeVariables(definition, "s2"),
			"the retail row guard lives in the s2 node projection");
		assertEquals(List.of(new QuestCondition.HasItem(ANCIENT_DOC_ITEM_ID, 1, true)),
			handOverSuccess.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(ANCIENT_DOC_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 3)), handOverSuccess.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			handOverSuccess.afterCommit());

		// 6. 步骤 2 交付失败 (priority 1): 未持物时展示 CHECK_USER_ITEM_FAIL(10001)
		// Step 2 handover fail: shows CHECK_USER_ITEM_FAIL when items missing
		QuestTransition handOverFail = transition(definition, "s2", "s2",
			new QuestEvent.TalkToNpc(EUVIA_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 1);
		assertEquals(List.of(), handOverFail.conditions(), "the row guard is the s2 node projection");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			handOverFail.afterCommit());

		// 7. 步骤 3 解读对话: QUEST_SELECT -> SELECT4(2034)
		// Step 3 dialog continuation: QUEST_SELECT -> SELECT4 (2034)
		QuestTransition select4 = talk(definition, "s3", "s3", EUVIA_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			select4.afterCommit());

		// 8. 步骤 3 推进: SETPRO4(10003) 升行至 s4（PACKET_ONLY + 任务簿页）；凭证改为接取即发——
		// 真端模型：182215604 是 quest.xml quest_work_item1，接取边（LevelUp/ZoneMissionEnd 自动接取）
		// 同事务发放，s4 的 ItemPlay 演出消费它（planner 负责完成/放弃回收）。
		// Step 3 advance: SETPRO4 (10003) bumps the row to s4 (PACKET_ONLY plus the journal page); the
		// credential moves to accept time — the retail model grants 182215604 (quest.xml
		// quest_work_item1) in the acceptance transaction (level-up / zone-mission-end auto-accept) and
		// the s4 ItemPlay step consumes it (the planner reclaims it on completion or abandon).
		QuestTransition setpro4 = talk(definition, "s3", "s4", EUVIA_NPC_ID, QuestDialogAction.SETPRO4.id());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), setpro4.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			setpro4.afterCommit());
		assertTrue(definition.transitions().stream()
			.filter(t -> "unaccepted".equals(t.sourceNode()) && "started".equals(t.targetNode()))
			.flatMap(t -> t.actions().stream())
			.anyMatch(a -> a instanceof QuestAction.GiveItem give && give.itemId() == TRANSLATED_DOC_ITEM_ID),
			"the acceptance edges must grant the itemplay credential 182215604");
		QuestTransition translatedPlay = definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.ItemPlay play && play.itemId() == TRANSLATED_DOC_ITEM_ID)
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), translatedPlay.actions());

		// 9. 真端链不再需要遗留的两条行修复边（s3->s2 / s2->s2）：交付门只读 HasItem，击杀完成边进入 s2
		// 时已清零 var1，交付又在同一事务内扣物升行——行内不存在可达的待修复脏态；保留的只有 REWARD 态的
		// 通用修复边（var0=0 的老存档补投影）。
		// The retail chain no longer needs the legacy row-repair edges (s3 -> s2 / s2 -> s2): the
		// hand-over gate reads only HasItem, the kill completion already cleared var1 when entering s2,
		// and the hand-over removes the item and bumps the row in one transaction — no reachable dirty
		// row state is left. Only the generic REWARD-status repair edge (projection fix for var0=0
		// saves) stays.
		assertTrue(definition.transitions().stream().anyMatch(t -> t.sourceNode() == null
			&& "reward".equals(t.targetNode()) && t.event() instanceof QuestEvent.EnterWorld),
			"reward-status saves keep the generic enter-world repair edge");
	}

	/** 节点投影（引擎按它匹配源行——显式 var0 条件在真端形里由投影承担）。 / Node projection. */
	private static Map<String, Integer> nodeVariables(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow()
			.projection().variables();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& event.equals(candidate.event()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, int priority) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& event.equals(candidate.event()) && Integer.valueOf(priority).equals(candidate.priority()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition definition() throws Exception {
		// 10503 已由真端驱动退役（wave10 链式接取登记）：退役任务的 XML 只在 git 历史里，
		// 统一取生产视图（XML 目录 + 真端 overlay）——未退役任务与直接编译 XML 等价。
		// 10503 is retail-driven now (wave10 chain-acquire registry), so its XML lives only in git
		// history; the production view (XML directory plus retail overlay) is the single source.
		return ProductionQuestDefinitions.definition(10503).definition();
	}
}
