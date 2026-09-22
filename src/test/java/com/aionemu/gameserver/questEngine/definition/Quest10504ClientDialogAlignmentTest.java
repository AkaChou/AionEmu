package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 10504「没收石板 / Confiscate the Slate」步骤 1 杀怪计数清零、步骤 2/3 石板采集与步骤 3 交付的客户端对齐合同。
 * Locks the step 1 kill counter reset, step 2/3 slate collection, and step 3 handover client contract for quest 10504.
 * <p>Aion 5.8 客户端在 collect_progress=3 时以 progress==3 (var0==3) 要求持有破碎石板 (182215607) 交付给阿斯特拉佩 (804706)。
 * 步骤 1 杀怪推进到 s2 时，必须将 var1 计数清零，严禁残留 var1=4 污染打包整型步数。
 * 玩家在步骤 2 击杀精英怪 (236255) 会推进至 var0=3。
 * 石板 (702671) 的掉落步数必须放宽为 collecting-step=0，且 s2 与 s3 均必须允许 can-act 与 USE_OBJECT 交互，
 * 避免先杀怪进入 s3 后石板无法采集导致任务永久卡死。</p>
 */
class Quest10504ClientDialogAlignmentTest {
	private static final int ASTRAPE_NPC_ID = 804706;
	private static final int SLATE_OBJECT_ID = 702671;
	private static final int SLATE_ITEM_ID = 182215607;

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

		// 2. 石板 702671 掉落步数放宽为 0 (任意进行中步骤均可掉落)
		// Slate 702671 drop rule must allow collecting-step 0
		QuestDrop slateDrop = definition.metadata().drops().stream()
			.filter(d -> d.npcId() == SLATE_OBJECT_ID && d.itemId() == SLATE_ITEM_ID)
			.findFirst().orElseThrow();
		assertEquals(0, slateDrop.collectingStep());

		// 3. s2 与 s3 阶段均允许交互石板 USE_OBJECT
		// Both s2 and s3 must support USE_OBJECT interaction on slate 702671
		boolean s2Use = definition.transitions().stream()
			.anyMatch(t -> "s2".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == SLATE_OBJECT_ID && talk.dialogId() == QuestDialogAction.USE_OBJECT.id());
		boolean s3Use = definition.transitions().stream()
			.anyMatch(t -> "s3".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == SLATE_OBJECT_ID && talk.dialogId() == QuestDialogAction.USE_OBJECT.id());
		assertTrue(s2Use, "s2 must support USE_OBJECT on slate 702671");
		assertTrue(s3Use, "s3 must support USE_OBJECT on slate 702671");

		// 4. s2 与 s3 阶段均具备 can-act 许可
		// Both s2 and s3 must permit can-act for template 702671
		boolean s2CanAct = definition.transitions().stream()
			.anyMatch(t -> "s2".equals(t.sourceNode()) && t.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == SLATE_OBJECT_ID);
		boolean s3CanAct = definition.transitions().stream()
			.anyMatch(t -> "s3".equals(t.sourceNode()) && t.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == SLATE_OBJECT_ID);
		assertTrue(s2CanAct, "s2 must permit can-act for slate 702671");
		assertTrue(s3CanAct, "s3 must permit can-act for slate 702671");

		// 5. 步骤 3 交付成功: 在 s3 交付石板并推进至 reward
		// Step 3 handover success: delivers slate in s3 and advances to reward
		QuestTransition handOverSuccess = definition.transitions().stream()
			.filter(t -> "s3".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == ASTRAPE_NPC_ID
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
				&& Integer.valueOf(0).equals(t.priority()))
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 3),
			new QuestCondition.HasItem(SLATE_ITEM_ID, 1)), handOverSuccess.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(SLATE_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 4)), handOverSuccess.actions());
	}

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Quest10504ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/10504.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 10504.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
