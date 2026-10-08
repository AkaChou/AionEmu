package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 任务对话/交互路由回归测试。
 * <p>
 * 遵循真端原则：1197(700004)、1198(700009)、1559(700513) 已按真端设计回归 NPC AI 层（QuestStartItemNpcAi2）
 * 独立发放道具，任务系统本身保持真端 SimpleUseItem 表驱动解耦，任务定义不再包含非标的 NPC USE_OBJECT 路由。
 * <p>
 * 1323（真端 SimpleTalk 行，acquired_npc = 交互物 LF2_Lost_JewelBox=730032）已退役：typed USE_OBJECT
 * 路由随 XML 退场，接取面改由 native 车道承载（31 → 客户端声明入口页）；1582 仍由 XML 拥有，typed
 * 路由断言照旧。
 * <p>
 * Quest 1323 is retired (SimpleTalk lane): its typed USE_OBJECT route retired with the XML and the accept
 * face now runs on the native lane; 1582 keeps its XML-owned typed route.
 */
class QuestStartItemDefinitionRegressionTest {
	private static final int RETIRED_QUEST = 1323;
	private static final int JEWEL_BOX_NPC = 730032;
	private static final int TALK_QUEST = 1582;
	private static final int TALK_NPC = 700196;

	@Test
	void startItemNpcsExposeTheDialogRoutesUsedAfterReading() throws Exception {
		// 1323（退役）：真端 acquired_npc_name 解析为交互物 NPC，任务行（31）下发客户端入口页。
		assertTrue(RetiredQuestIds.contains(RETIRED_QUEST));
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertTrue(handler.routes(RETIRED_QUEST), "SimpleTalk native 车道必须路由 1323");
		assertEquals(JEWEL_BOX_NPC, handler.acquireNpc(RETIRED_QUEST),
			"真端 acquired_npc_name = LF2_Lost_JewelBox");
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 22);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, JEWEL_BOX_NPC, RETIRED_QUEST, 31)),
			"交互物 NPC 的任务行打开面");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(RETIRED_QUEST));
		assertEquals(QuestDialogPage.SELECT1.id(), NativeTalkFixture.clientEntryPage(RETIRED_QUEST),
			"入口页 = 客户端声明的 select1(1011)");

		// 1582（XML 保有）：物件 owner 收口（2026-10-08，3036 实机验收同形）后，started 态 700196 只保留
		// USE_OBJECT(-1) 零回页推进；迁移期由 npc-report 展开出来的 31/报告页路由是「向物件发页」噪声，已移除。
		// 1582 (XML-owned): after the object-owner trim (2026-10-08, same shape as the live-accepted 3036)
		// the started-state route on 700196 is the zero-page USE_OBJECT(-1) advance; the migration's
		// npc-report 31/page route on the object is gone.
		CompiledQuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(TALK_QUEST);
		assertTrue(definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode()))
			.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == TALK_NPC && talk.dialogId() == QuestDialogAction.USE_OBJECT.id()
				&& transition.afterCommit().equals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)))),
			"quest " + TALK_QUEST + " NPC " + TALK_NPC);
	}
}
