package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 10504「没收石板 / Confiscate the Slate」步骤 1 杀怪计数清零、步骤 2/3 石板采集与步骤 3 交付的客户端对齐合同。
 * Locks the step 1 kill counter reset, step 2/3 slate collection, and step 3 handover client contract for quest 10504.
 * <p>Aion 5.8 客户端在 collect_progress=3 时以 progress==3 (var0==3) 要求持有破碎石板 (182215607) 交付给阿斯特拉佩 (804706)。
 * 步骤 1 杀怪推进到 s2 时，必须将 var1 计数清零，严禁残留 var1=4 污染打包整型步数。
 * 玩家在步骤 2 击杀精英怪 (236255) 会推进至 var0=3。
 * 石板 (702671) 的掉落生效行与交互行同为真端声明的收集行 s3：掉落 gate 的 var0==collectingStep 与节点投影
 * s3={var0=3} 在此行同时成立，既不早关采集入口，也不错过收集行。</p>
 */
class Quest10504ClientDialogAlignmentTest {
	private static final int ASTRAPE_NPC_ID = 804706;
	private static final int SLATE_OBJECT_ID = 702671;
	private static final int SLATE_ITEM_ID = 182215607;
	private static final int WORK_CREDENTIAL_ID = 182215606;

	@Test
	void slateCollectionAndHandoverContract() throws Exception {
		QuestDefinition definition = definition();

		// 1. 步骤 1 杀怪完成转移至 s2 (priority 0): 必须同时设置 var0=2 并清零 var1=0
		// Step 1 kill completion to s2: must set var0=2 and clear var1=0
		QuestTransition killCompletion = definition.transitions().stream()
			.filter(t -> "s1".equals(t.sourceNode()) && "s2".equals(t.targetNode())
				&& (t.event() instanceof QuestEvent.KillNpc || t.event() instanceof QuestEvent.KillNpcSet)
				&& Integer.valueOf(0).equals(t.priority()))
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 2),
			new QuestAction.SetVariable("var1", 0)), killCompletion.actions());

		// 2. 石板 702671 的掉落生效行 = 真端声明的收集行：quest.xml collect_progress=3，与客户端任务书
		// 第 3 行（s3，石板交互行）同判据。遗留 XML 的 collecting-step=0（任意行）是旧形；生效行过早会
		// 在前置击杀推进 var0 后关闭采集入口。
		// The slate drop lives on the retail collect row: quest.xml declares collect_progress=3 and the
		// client's third row is the collect row (the slate interaction asserted below). The legacy XML's
		// collecting-step=0 was the "any row" shape; an earlier row would close the gate once the kill
		// step advances var0.
		QuestDrop slateDrop = definition.metadata().drops().stream()
			.filter(d -> d.npcId() == SLATE_OBJECT_ID && d.itemId() == SLATE_ITEM_ID)
			.findFirst().orElseThrow();
		assertEquals(3, slateDrop.collectingStep());

		// 3. 收集行 s3 允许与石板交互：真端把交互成对挂在掉落生效行上——TalkToNpc(702671, dialogId 空)
		// + CanAct(702671, ACTION_ITEM_USE)（掉落 gate 的 var0==collectingStep 恰在此行成立）；
		// s2 是击杀行，不再提供石板交互（遗留 XML 放宽到 s2）。
		// The collect row s3 accepts the slate interaction: the retail chain hangs the pair
		// TalkToNpc(702671, null dialogId) + CanAct(702671, ACTION_ITEM_USE) on the drop's live row
		// (where the gate's var0 == collectingStep holds); s2 is the kill row and no longer offers the
		// slate (the legacy XML allowed it there).
		boolean s3Talk = definition.transitions().stream()
			.anyMatch(t -> "s3".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == SLATE_OBJECT_ID);
		boolean s3CanAct = definition.transitions().stream()
			.anyMatch(t -> "s3".equals(t.sourceNode()) && t.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == SLATE_OBJECT_ID);
		assertTrue(s3Talk && s3CanAct, "s3 must accept the slate interaction (702671)");

		// 4. 步骤 3 交付成功: 在 s3 交付石板并推进至 reward。真端把行门写进节点投影（s3 = {var0=3}，
		// 引擎按节点投影匹配源行），条件下只剩持物检查；交付同时发放工作凭证 182215606
		// （quest.xml quest_work_item1，真端"接取/交付即发凭证、完成与放弃由 planner 回收"的模型）。
		// Step 3 handover: delivers the slate in s3 and lands on reward. The retail row guard lives in
		// the node projection (s3 = {var0=3}, the engine matches source rows through it), so the
		// condition keeps only the item check; the handover also grants the work credential 182215606
		// (quest.xml quest_work_item1, the retail credential model whose completion and abandon cleanup
		// is owned by the planner).
		QuestTransition handOverSuccess = definition.transitions().stream()
			.filter(t -> "s3".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == ASTRAPE_NPC_ID
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
				&& Integer.valueOf(0).equals(t.priority()))
			.findFirst().orElseThrow();
		assertEquals(Map.of("var0", 3), nodeVariables(definition, "s3"),
			"the retail row guard lives in the s3 node projection");
		assertEquals(List.of(new QuestCondition.HasItem(SLATE_ITEM_ID, 1)), handOverSuccess.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(SLATE_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 3),
			new QuestAction.GiveItem(WORK_CREDENTIAL_ID, 1)), handOverSuccess.actions());
	}

	/** 节点投影（引擎按它匹配源行——显式 var0 条件在真端形里由投影承担）。 / Node projection. */
	private static Map<String, Integer> nodeVariables(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow()
			.projection().variables();
	}

	private static QuestDefinition definition() throws Exception {
		// 10504 已由真端驱动退役（wave10 链式接取登记）：退役任务的 XML 只在 git 历史里，
		// 统一取生产视图（XML 目录 + 真端 overlay）——未退役任务与直接编译 XML 等价。
		// 10504 is retail-driven now (wave10 chain-acquire registry), so its XML lives only in git
		// history; the production view (XML directory plus retail overlay) is the single source.
		return ProductionQuestDefinitions.definition(10504).definition();
	}
}
