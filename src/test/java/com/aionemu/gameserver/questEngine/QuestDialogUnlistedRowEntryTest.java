package com.aionemu.gameserver.questEngine;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;

/**
 * 未进客户端任务列表的行必须能到达可交付态：客户端 {@code data_driven_quest.xml} 是服务端原版表的子集，
 * 不在该表里的行永远渲染不进 NPC 的任务清单，玩家连可点的确认按钮都没有（{@code SET_SUCCEED} 只能由阶段页
 * 发出），于是交付条件满足后永远停在 START——头顶没有可交付标记、也开不出奖励窗
 * （2026-10-20 实机 806698：19678/19679 推了页 1011 但无标记）。兜底在开门时替玩家走完那条进入 REWARD 的
 * 转换；此后开门由 REWARD 的 preview 打开奖励窗。
 * <p>
 * Rows missing from the client quest list must still reach the deliverable state: the client
 * {@code data_driven_quest.xml} is a subset of the retail table, so such a row never renders in an npc's quest
 * list and the player has no confirm button either ({@code SET_SUCCEED} comes only from the stage page). Its
 * delivery condition being met therefore left it in START forever — no hand-in marker and no reward window
 * (live 2026-10-20, npc 806698: page 1011 was pushed with no marker). The fallback walks the REWARD transition
 * for the player on the npc open; the REWARD preview then opens the reward window.
 */
class QuestDialogUnlistedRowEntryTest {

	private static final int RENA = 806_698;
	private static final int LISTED_ROW = 196_701;
	private static final int UNLISTED_ROW = 196_702;
	private static final int SIBLING_ROW = 196_703;

	private DataDrivenNativeRuntime originalRuntime;
	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		// 安装门禁读 NPC 模板（can-act 注册）：测试环境喂空表即可。
		// The installation gate reads npc templates (can-act registration): an empty table is enough here.
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
		// 原版表只含 LISTED_ROW：其余合成行都算「客户端清单永远列不出」。
		// The retail table carries LISTED_ROW only, so every other synthetic row is unlistable client-side.
		originalRuntime = DataDrivenNativeRuntime.swapInstanceForTest(
			DataDrivenNativeRuntime.tableOnly(Set.of(LISTED_ROW)));
	}

	@AfterEach
	void tearDown() {
		DataDrivenNativeRuntime.swapInstanceForTest(originalRuntime);
		DataManager.NPC_DATA = originalNpcData;
	}

	/**
	 * 核心用例：不在原版表 ⇒ 客户端清单必然列不出，开门必须替玩家把它推进到交付态。
	 * The core case: absent from the retail table means the client list can never carry the row, so the open
	 * has to walk it into the deliverable state for the player.
	 */
	@Test
	void anUnlistedRowOnItsDeliveryStageIsAdvancedByTheOpen() {
		QuestEngine engine = engineWith(unlistedRow(UNLISTED_ROW));

		QuestDialogRouter.UnlistedDelivery delivery =
			router(engine).unlistedDeliveryTransition(playerAt(UNLISTED_ROW, 1), RENA);

		assertNotNull(delivery, "未列表行的交付转换必须被开门认领");
		assertEquals(UNLISTED_ROW, delivery.questId());
		assertEquals(QuestDialogAction.SET_SUCCEED.id(),
			((QuestEvent.TalkToNpc) delivery.transition().event()).dialogId(),
			"推进的必须是通向奖励窗的那条确认路由");
	}

	/**
	 * 反例：进了原版表的行由客户端清单负责，服务端不得抢开门（否则会在有列表的 NPC 上越过列表）。
	 * The counter-case: a row present in the retail table is served by the client list, so the server must
	 * not claim the open (that would bypass the list on npcs that do render one).
	 */
	@Test
	void aListedRowIsNeverAdvanced() {
		QuestEngine engine = engineWith(unlistedRow(LISTED_ROW));

		assertNull(router(engine).unlistedDeliveryTransition(playerAt(LISTED_ROW, 1), RENA),
			"在表行必须交回客户端任务列表");
	}

	/**
	 * 反例：玩家还停在上一阶段（交付条件未满足）时，兜底不得把任务提前推进到交付态。
	 * The counter-case: while the player still sits on an earlier stage the fallback must not advance the
	 * quest into the deliverable state.
	 */
	@Test
	void aRowBeforeItsDeliveryStageIsNotAdvanced() {
		QuestEngine engine = engineWith(unlistedRow(UNLISTED_ROW));

		assertNull(router(engine).unlistedDeliveryTransition(playerAt(UNLISTED_ROW, 0), RENA),
			"未到阶段的任务不得被提前推进");
	}

	/**
	 * 目标不是 REWARD 的推页路由不参与：兜底只负责「送进可交付态」，不替玩家翻页或结算。
	 * A route that does not target REWARD takes no part: the fallback only advances into the deliverable
	 * state, it never turns pages or settles the quest for the player.
	 */
	@Test
	void aPageOnlyRouteIsNotADeliveryTransition() {
		CompiledQuestDefinition pageOnly = quest(UNLISTED_ROW)
			.metadata(QuestMetadata.minimal("unlisted-page", 0, "SIGNIFICANT"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("s0", project(QuestStatus.START, vars("var0", 0)))
			.node("s1", project(QuestStatus.START, vars("var0", 1)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 2)))
			.on(new QuestEvent.UseItem(166_030_005))
			.from("s0").goTo("s1")
			// 推页自环（目标不是 REWARD）＋另一条无路由的 reward 节点，仅用于让目录可编译。
			// A page-only self-loop (target is not REWARD) plus an unrouted reward node so the catalog compiles.
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.QUEST_SELECT.id()))
			.from("s1").goTo("s1")
			.on(new QuestEvent.UseItem(166_030_006))
			.from("s1").goTo("reward")
			.afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
			.compile();
		QuestEngine engine = engineWith(pageOnly);

		assertNull(router(engine).unlistedDeliveryTransition(playerAt(UNLISTED_ROW, 1), RENA),
			"目标不是 REWARD 的路由不得参与交付推进");
	}

	/**
	 * 多条同形行同时停在交付阶段时只推进一条：取 id 最小者。
	 * With several same-shaped rows on their delivery stage exactly one advances: the lowest id wins.
	 */
	@Test
	void sameShapedSiblingsResolveToTheLowestId() {
		QuestEngine engine = engineWith(unlistedRow(SIBLING_ROW), unlistedRow(UNLISTED_ROW));
		Player player = playerAt(SIBLING_ROW, 1);
		NativeTalkFixture.add(player, UNLISTED_ROW, QuestStatus.START, 1 << 6);

		QuestDialogRouter.UnlistedDelivery delivery =
			router(engine).unlistedDeliveryTransition(player, RENA);

		assertNotNull(delivery, "同形行必须认领开门");
		assertEquals(SIBLING_ROW, delivery.questId(), "同形行只推进一条，取 id 最小者");
	}

	/**
	 * 合成的未列表行：s1 上一条 SET_SUCCEED → REWARD（与 19678/19679 的真实形状一致）。
	 * A synthetic unlisted row: SET_SUCCEED → REWARD on s1, the real shape of 19678/19679.
	 */
	private static CompiledQuestDefinition unlistedRow(int questId) {
		return quest(questId)
			.metadata(QuestMetadata.minimal("unlisted-" + questId, 0, "SIGNIFICANT"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("s0", project(QuestStatus.START, vars("var0", 0)))
			.node("s1", project(QuestStatus.START, vars("var0", 1)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 2)))
			// 编译器要求每个节点都从首个节点可达：真实行靠「用成长石」从 s0 走到 s1。
			// The compiler requires every node to be reachable from the first one; the real row gets
			// there by using the growth stone on s0.
			.on(new QuestEvent.UseItem(166_030_005))
			.from("s0").goTo("s1")
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.SET_SUCCEED.id()))
			.from("s1").goTo("reward")
			.afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
			.compile();
	}

	private static QuestEngine engineWith(CompiledQuestDefinition... definitions) {
		QuestEngine engine = new QuestEngine();
		engine.installProductionDefinitions(new ImmutableQuestCatalog(List.of(definitions)));
		return engine;
	}

	private static QuestDialogRouter router(QuestEngine engine) {
		return engine.dialogRouterForTest();
	}

	/** 玩家停在 s⟨var0⟩：`var0` 占 0..5 位，值 = {@code stage}。 / The player sitting on s⟨var0⟩. */
	private static Player playerAt(int questId, int stage) {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, stage);
		return player;
	}
}
