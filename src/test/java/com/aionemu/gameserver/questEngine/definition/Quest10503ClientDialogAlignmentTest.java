package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 锁定任务 10503「提亚马特结界守护者 / Guard Down, Secrets Out」步骤 1 杀怪计数清零、步骤 2 收集交付与步骤 3 对话续接的客户端对齐合同。
 * Locks the step 1 kill counter reset, step 2 collection handover, and step 3 dialog continuation client contract for quest 10503.
 * <p>Aion 5.8 客户端在 collect_progress=2 时以 progress==2 (var0==2) 校验背包装有古老龙族文件 (182215603)，
 * 并在尤碧亚 (804705) 头顶显示任务标记，进入 select3(1693) -> check_user_has_quest_item(39)。
 * 步骤 1 (s1) 杀怪完成推进到 s2 时，必须显式将 var1 计数清零，严禁残留 var1=2 污染打包整型为 130（导致客户端无法对齐 step 2 对白）。
 * 采集物 (702670) 交互完成后必须触发 PACKET_ONLY 状态同步与 close-dialog。
 * 交付成功后在事务内扣除 182215603 并推进 var0=3，展示 check_user_item_ok(10000)，随后通过 SELECT4(2034)
 * 衔接解读对话，最终以 SETPRO4(10003) 发放解读好的龙族文件 (182215604) 并推进 var0=4。
 * 同时支持 enter-world 将脏存档（处于 var0=3 且持有旧道具，或处于 s2 且残留 var1>=1）自愈修复为纯净的 s2(var0=2, var1=0)。</p>
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
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 1),
			new QuestCondition.VariableAtLeast("var1", 2)), killCompletion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 2),
			new QuestAction.SetVariable("var1", 0)), killCompletion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			killCompletion.afterCommit());

		// 2. 采集物 702670 交互完成: 触发 PACKET_ONLY 状态同步与 close-dialog
		// Gather object 702670 interaction: emits PACKET_ONLY sync and closes dialog
		QuestTransition gatherObj = talk(definition, "s2", "s2", GATHER_OBJECT_ID, QuestDialogAction.USE_OBJECT.id());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), gatherObj.afterCommit());

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
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 2),
			new QuestCondition.HasItem(ANCIENT_DOC_ITEM_ID, 1, true)), handOverSuccess.conditions());
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
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), handOverFail.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			handOverFail.afterCommit());

		// 7. 步骤 3 解读对话: QUEST_SELECT -> SELECT4(2034)
		// Step 3 dialog continuation: QUEST_SELECT -> SELECT4 (2034)
		QuestTransition select4 = talk(definition, "s3", "s3", EUVIA_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			select4.afterCommit());

		// 8. 步骤 3 领物推进: SETPRO4(10003) 发放 182215604 并推进 var0=4 至 s4
		// Step 3 reward: SETPRO4 gives translated doc 182215604 and sets var0=4 to s4
		QuestTransition setpro4 = talk(definition, "s3", "s4", EUVIA_NPC_ID, QuestDialogAction.SETPRO4.id());
		assertEquals(List.of(
			new QuestAction.GiveItem(TRANSLATED_DOC_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 4)), setpro4.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), setpro4.afterCommit());

		// 9. 旧存档自愈迁移 (s3 -> s2): enter-world 将 var0=3 且仍持有 182215603 的玩家修正回 s2(var0=2, var1=0)
		// Self-healing migration: enter-world rolls back dirty save (var0=3 with ancient doc) to s2 (var0=2, var1=0)
		QuestTransition enterWorldHealS3 = transition(definition, "s3", "s2", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 3),
			new QuestCondition.HasItem(ANCIENT_DOC_ITEM_ID, 1, true)), enterWorldHealS3.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 2),
			new QuestAction.SetVariable("var1", 0)), enterWorldHealS3.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			enterWorldHealS3.afterCommit());

		// 10. 存量 s2 脏计数自愈迁移 (s2 -> s2): enter-world 清理处于 var0=2 但残留 var1>=1 的脏计数
		// Self-healing migration: enter-world cleans residual var1>=1 while already in s2
		QuestTransition enterWorldHealS2 = transition(definition, "s2", "s2", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 2),
			new QuestCondition.VariableAtLeast("var1", 1)), enterWorldHealS2.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var1", 0)), enterWorldHealS2.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			enterWorldHealS2.afterCommit());
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
		try (InputStream input = Quest10503ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/10503.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 10503.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
