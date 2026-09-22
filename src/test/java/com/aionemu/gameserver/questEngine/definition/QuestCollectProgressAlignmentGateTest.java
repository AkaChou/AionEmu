package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全服任务收集进度 (collect_progress) 与交付对白契约对齐门禁测试。
 * Global quest collect_progress and handover dialog contract alignment gate test.
 *
 * <p>锁定同类任务族群：
 * 1. 10503 & 10530 & 20530：客户端 collect_progress 声明的步骤必须与服务端的 CHECK_USER_HAS_QUEST_ITEM 步骤严格对齐，
 *    严禁在拾取物/祭坛交互时抢跑推进 var0，避免玩家持物前往 NPC 时因步骤脱节导致对话失效（下发通用第 10 页）。
 *    同时，进入新步骤时必须清零前置击杀计数器 (var1=0)，严禁残留高位脏位污染整型步数。
 * 2. 10504：收集步不可因前置击杀推进而关闭采集入口或因 collecting-step 导致死锁，且 s1->s2 必须清零 var1。
 * 3. 1373：使用道具后进入 var0=1，与客户端 collect_progress=1 保持对齐，严禁跳步。
 * 4. 复合型多阶段任务进入收集步时，严禁前置击杀局部计数器残留污染打包整型步数。</p>
 */
class QuestCollectProgressAlignmentGateTest {

	@Test
	void quest10503Alignment() throws Exception {
		QuestDefinition def = load(10503);
		// 客户端 collect_progress=2: s2 必须处理 CHECK_USER_HAS_QUEST_ITEM
		boolean s2Check = def.transitions().stream()
			.anyMatch(t -> "s2".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertTrue(s2Check, "10503: s2 must handle CHECK_USER_HAS_QUEST_ITEM");

		// 不存在 get-item 抢跑推进 var0=3
		boolean getDoc = def.transitions().stream()
			.anyMatch(t -> t.event() instanceof QuestEvent.GetItem gi && gi.itemId() == 182215603);
		assertFalse(getDoc, "10503: must not have get-item 182215603 advancing var0 prematurely");

		// s1 -> s2 杀怪转移必须清零 var1，确保步数纯净为 2
		QuestTransition killTrans = def.transitions().stream()
			.filter(t -> "s1".equals(t.sourceNode()) && "s2".equals(t.targetNode())
				&& (t.event() instanceof QuestEvent.KillNpc || t.event() instanceof QuestEvent.KillNpcSet))
			.findFirst().orElseThrow();
		assertTrue(killTrans.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv
			&& "var1".equals(sv.field()) && sv.value() == 0), "10503: s1->s2 must reset var1 to 0");
	}

	@Test
	void quest10530Alignment() throws Exception {
		QuestDefinition def = load(10530);
		// 客户端 collect_progress=7: s7 必须处理 CHECK_USER_HAS_QUEST_ITEM
		boolean s7Check = def.transitions().stream()
			.anyMatch(t -> "s7".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertTrue(s7Check, "10530: s7 must handle CHECK_USER_HAS_QUEST_ITEM");

		// 祭坛 703387 交互不能推进 var0=8
		boolean altarAdvances = def.transitions().stream()
			.anyMatch(t -> "s7".equals(t.sourceNode()) && "s8".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 703387);
		assertFalse(altarAdvances, "10530: altar 703387 must not advance to s8");
	}

	@Test
	void quest20530Alignment() throws Exception {
		QuestDefinition def = load(20530);
		// 客户端 collect_progress=7: s7 必须处理 CHECK_USER_HAS_QUEST_ITEM
		boolean s7Check = def.transitions().stream()
			.anyMatch(t -> "s7".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertTrue(s7Check, "20530: s7 must handle CHECK_USER_HAS_QUEST_ITEM");

		// 祭坛 703389 交互不能推进 var0=8
		boolean altarAdvances = def.transitions().stream()
			.anyMatch(t -> "s7".equals(t.sourceNode()) && "s8".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 703389);
		assertFalse(altarAdvances, "20530: altar 703389 must not advance to s8");
	}

	@Test
	void quest10504Alignment() throws Exception {
		QuestDefinition def = load(10504);
		// 石板 702671 掉落步数必须为 0
		QuestDrop drop = def.metadata().drops().stream()
			.filter(d -> d.npcId() == 702671 && d.itemId() == 182215607)
			.findFirst().orElseThrow();
		assertTrue(drop.collectingStep() == 0, "10504: collecting-step must be 0");

		// s3 阶段支持交互 702671
		boolean s3Use = def.transitions().stream()
			.anyMatch(t -> "s3".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 702671 && talk.dialogId() == QuestDialogAction.USE_OBJECT.id());
		assertTrue(s3Use, "10504: s3 must support USE_OBJECT on 702671");

		// s1 -> s2 必须清零 var1
		QuestTransition killTrans = def.transitions().stream()
			.filter(t -> "s1".equals(t.sourceNode()) && "s2".equals(t.targetNode())
				&& (t.event() instanceof QuestEvent.KillNpc || t.event() instanceof QuestEvent.KillNpcSet))
			.findFirst().orElseThrow();
		assertTrue(killTrans.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv
			&& "var1".equals(sv.field()) && sv.value() == 0), "10504: s1->s2 must reset var1 to 0");
	}

	@Test
	void quest1373Alignment() throws Exception {
		QuestDefinition def = load(1373);
		// 客户端 collect_progress=1: v1 必须处理 CHECK_USER_HAS_QUEST_ITEM
		boolean v1Check = def.transitions().stream()
			.anyMatch(t -> "v1".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertTrue(v1Check, "1373: v1 must handle CHECK_USER_HAS_QUEST_ITEM");
	}

	@Test
	void multiStageMissionsResetCounterVarsOnEnteringNonCounterStages() throws Exception {
		// 针对希哥尼亚/厄尔盖特/阿斯特拉复合型使命任务，锁定进入后续收集/对话阶段时计数变量清零契约
		int[] hybridMissions = {10503, 10504, 10506, 10507, 10527, 10528, 20504, 20527, 20528};
		for (int qid : hybridMissions) {
			QuestDefinition def = load(qid);
			// 遍历所有有 target 的转移，若条件使用了 var1 且 actions 改变了 var0 转移到非 reward/complete 节点，且目标节点不使用 var1
			// 则 actions 必须包含 set-variable var1=0
			for (QuestTransition tr : def.transitions()) {
				if (tr.targetNode() == null || "reward".equals(tr.targetNode()) || "complete".equals(tr.targetNode())) {
					continue;
				}
				boolean checksVar1 = tr.conditions().stream().anyMatch(c -> c instanceof QuestCondition.VariableAtLeast val && "var1".equals(val.field())
					|| c instanceof QuestCondition.VariableBelow vb && "var1".equals(vb.field())
					|| c instanceof QuestCondition.QuestVariableIs qv && "var1".equals(qv.field()));
				boolean setsVar0 = tr.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv && "var0".equals(sv.field()));
				if (checksVar1 && setsVar0) {
					// 目标节点如果不使用 var1，必须显式清零
					boolean targetUsesVar1 = def.transitions().stream()
						.filter(t -> tr.targetNode().equals(t.sourceNode()))
						.anyMatch(t -> t.conditions().stream().anyMatch(c -> c instanceof QuestCondition.VariableAtLeast val && "var1".equals(val.field())
							|| c instanceof QuestCondition.VariableBelow vb && "var1".equals(vb.field())
							|| c instanceof QuestCondition.QuestVariableIs qv && "var1".equals(qv.field())));
					if (!targetUsesVar1) {
						boolean resetsVar1 = tr.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv
							&& "var1".equals(sv.field()) && sv.value() == 0);
						assertTrue(resetsVar1, "Mission " + qid + " transition " + tr.sourceNode() + "->" + tr.targetNode()
							+ " must reset var1 to 0 to prevent packed step corruption");
					}
				}
			}
		}
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestCollectProgressAlignmentGateTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
