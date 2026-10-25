package com.aionemu.gameserver.questEngine;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.showQuestDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
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
 * <p>
 * 交付按钮在更后面一页上的步不走兜底：那种步（入口页 → 续页 → 交付按钮）必须由玩家点完，
 * 替按等于跳过整条对话（实机 2026-10-22：点 Jucleas 无弹窗无影片，任务直接落到 Reward 30）。
 * <p>
 * A step whose delivery button sits on a later page never takes the fallback: such a step (entry page →
 * sub-page → delivery button) must be walked by the player, and pressing the button for them skips the
 * whole dialog (live 2026-10-22: no dialog window and no movie at Jucleas, the quest jumped straight to
 * Reward 30).
 * <p>
 * 交付兜底候选在同一 source 另有 {@code USE_OBJECT(-1)} 入口页时，开门先下发入口页，
 * 玩家在页面上点交付按钮（2620/2919）。 / When the delivery candidate carries a
 * {@code USE_OBJECT(-1)} entry page on the same source, the open shows that page first and
 * the player presses delivery there (2620/2919).
 */
class QuestDialogUnlistedRowEntryTest {

	private static final int RENA = 806_698;
	private static final int LISTED_ROW = 196_701;
	private static final int UNLISTED_ROW = 196_702;
	private static final int SIBLING_ROW = 196_703;

	private DataDrivenNativeRuntime originalRuntime;
	private NpcData originalNpcData;
	private ObjectProvider<QuestEngine> originalEngineProvider;

	@BeforeEach
	void setUp() throws ReflectiveOperationException {
		// 安装门禁读 NPC 模板（can-act 注册）。
		// The installation gate reads npc templates (can-act registration).
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
		// 生产定义的 can-act 安装会反向取引擎单例；测试必须先把 provider 指向被测引擎。
		// Installing a production can-act route resolves the engine singleton in reverse, so the
		// provider must point at the engine under test.
		originalEngineProvider = engineProvider();
		// 生产定义里的 ACTION_ITEM_USE 资格门需要模板 AI；这里只喂交互物模板。
		// The ACTION_ITEM_USE gate of the production definitions needs template AIs; feed the
		// interaction-object templates only.
		for (int npcId : new int[] { 700_323, 700_212, 731_710, 703_317, 731_716, 703_325 }) {
			NpcTemplate template = new NpcTemplate();
			template.setNpcId(npcId);
			template.setAi("quest_use_item");
			DataManager.NPC_DATA.getNpcData().put(npcId, template);
		}
		// 原版表只含 LISTED_ROW：其余合成行都算「客户端清单永远列不出」。
		// The retail table carries LISTED_ROW only, so every other synthetic row is unlistable client-side.
		originalRuntime = DataDrivenNativeRuntime.swapInstanceForTest(
			DataDrivenNativeRuntime.tableOnly(Set.of(LISTED_ROW)));
	}

	@AfterEach
	void tearDown() {
		QuestEngine.setInstanceProvider(originalEngineProvider);
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
	 * 交付按钮在更后面一页上的步（客户端页链）不得被开门替按：玩家还没看到那一页，替按等于跳过整条
	 * 对话与影片（实机 2026-10-22：点 Jucleas 无弹窗无影片，任务直接落到 Reward 30）。
	 * A step whose delivery button sits on a later page (a client page chain) must not be advanced by the
	 * open: the player has not seen that page yet, so pressing the button for them skips the whole dialog
	 * (live 2026-10-22: no dialog window and no movie at Jucleas, the quest jumped straight to Reward 30).
	 */
	@Test
	void aStepWithAClientPageChainIsNotAdvanced() {
		QuestEngine engine = engineWith(pageChainRow(UNLISTED_ROW));

		assertNull(router(engine).unlistedDeliveryTransition(playerAt(UNLISTED_ROW, 1), RENA),
			"交付按钮在续页上的步必须由玩家点完，兜底不得替按");
	}

	/**
	 * 生产目录里的「典礼」两族（1007 天族 Jucleas / 2009 魔族 Kalsten）：这一步是
	 * 「入口页 → 续页（影片）→ 交付按钮」，开门替按等于跳过整条对话——实机 2026-10-22：点 Jucleas
	 * 无弹窗无影片，任务从 s2 直接落到 Reward 30。
	 * The production "ceremony" pair (1007 Elyos at Jucleas / 2009 Asmodian at Kalsten): the step is
	 * entry page → sub-page (movie) → delivery button, so pressing the delivery button on the open skips
	 * the whole dialog (live 2026-10-22: no dialog window and no movie at Jucleas, the quest jumped from
	 * s2 straight to Reward 30).
	 */
	@Test
	void theProductionCeremonyStepsAreNotAdvancedByTheOpen() throws Exception {
		for (int[] ceremony : new int[][] { { 1007, 203752 }, { 2009, 204075 } }) {
			int questId = ceremony[0];
			int npcId = ceremony[1];
			QuestEngine engine = engineWith(productionDefinition(questId));

			assertNull(router(engine).unlistedDeliveryTransition(playerAt(questId, 2), npcId),
				"典礼任务的交付按钮在续页上，开门不得替按：" + questId);
		}
	}

	/**
	 * 2620 的 s1 同时有 -1 入口页（{@code DEFAULT_SUCCESS}）与 1009 交付路由：开门必须先发
	 * 入口页，不能替玩家直接按交付。 / Quest 2620's s1 carries both a -1 entry page
	 * ({@code DEFAULT_SUCCESS}) and the 1009 delivery route: the open must show the entry page first,
	 * never press delivery for the player.
	 */
	@Test
	void theProductionQuest2620OpenShowsTheEntryPageBeforeDelivery() throws Exception {
		int questId = 2620;
		int npcId = 204787;
		CompiledQuestDefinition definition = productionDefinition(questId);
		QuestEngine engine = engineWith(definition);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, 1 | (5 << 6) | (5 << 12));

		QuestDialogRouter.UnlistedDelivery openPage =
			router(engine).unlistedOpenPageTransition(player, npcId);

		assertNotNull(openPage, "2620 的 -1 入口页必须被开门认领");
		assertEquals(questId, openPage.questId());
		assertEquals(QuestDialogAction.USE_OBJECT.id(),
			((QuestEvent.TalkToNpc) openPage.transition().event()).dialogId(),
			"开门下发的是原版入口动作");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			openPage.transition().afterCommit(), "入口页必须是 DEFAULT_SUCCESS");
		assertRowEntryPage(definition, "s1", npcId, QuestDialogPage.DEFAULT_SUCCESS);

		QuestDialogRouter.UnlistedDelivery delivery =
			router(engine).unlistedDeliveryTransition(player, npcId);
		assertNotNull(delivery, "同一步仍保留 1009 交付兜底");
		assertEquals(QuestDialogAction.SELECT_QUEST_REWARD.id(),
			((QuestEvent.TalkToNpc) delivery.transition().event()).dialogId());
	}

	/**
	 * 2919 的 bookInserted 同形：-1 入口页是 {@code SELECT8}，1009 才是交付。
	 * Quest 2919's bookInserted has the same shape: the -1 entry page is {@code SELECT8}; only
	 * 1009 delivers.
	 */
	@Test
	void theProductionQuest2919OpenShowsTheEntryPageBeforeDelivery() throws Exception {
		int questId = 2919;
		int npcId = 204206;
		CompiledQuestDefinition definition = productionDefinition(questId);
		QuestEngine engine = engineWith(definition);
		Player player = playerAt(questId, 7);

		QuestDialogRouter.UnlistedDelivery openPage =
			router(engine).unlistedOpenPageTransition(player, npcId);

		assertNotNull(openPage, "2919 的 -1 入口页必须被开门认领");
		assertEquals(questId, openPage.questId());
		assertEquals(QuestDialogAction.USE_OBJECT.id(),
			((QuestEvent.TalkToNpc) openPage.transition().event()).dialogId());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT8.id())),
			openPage.transition().afterCommit(), "入口页必须是 SELECT8");
		assertRowEntryPage(definition, "bookInserted", npcId, QuestDialogPage.SELECT8);

		QuestDialogRouter.UnlistedDelivery delivery =
			router(engine).unlistedDeliveryTransition(player, npcId);
		assertNotNull(delivery, "同一步仍保留 1009 交付兜底");
		assertEquals(QuestDialogAction.SELECT_QUEST_REWARD.id(),
			((QuestEvent.TalkToNpc) delivery.transition().event()).dialogId());
	}

	/**
	 * 19678/19679 没有 -1 入口页：开门仍直接走 {@code SET_SUCCEED} 交付兜底。
	 * 19678/19679 carry no -1 entry page: the open still walks the {@code SET_SUCCEED} delivery
	 * fallback directly.
	 */
	@Test
	void singleButtonRowsKeepTheDeliveryFallbackWithoutAnEntryPage() throws Exception {
		for (int[] row : new int[][] { { 19678, 806698 }, { 19679, 806698 } }) {
			int questId = row[0];
			int npcId = row[1];
			QuestEngine engine = engineWith(productionDefinition(questId));
			Player player = playerAt(questId, 1);

			assertNull(router(engine).unlistedOpenPageTransition(player, npcId),
				"没有 -1 入口页的行不得走入口页门：" + questId);
			QuestDialogRouter.UnlistedDelivery delivery =
				router(engine).unlistedDeliveryTransition(player, npcId);
			assertNotNull(delivery, "单按钮行必须保留交付兜底：" + questId);
			assertEquals(QuestDialogAction.SET_SUCCEED.id(),
				((QuestEvent.TalkToNpc) delivery.transition().event()).dialogId());
		}
	}

	/**
	 * 10529/20529 的交付按钮在 {@code SELECT11_1} 续页之后：页链保护必须继续压住交付兜底，
	 * 入口页门也不得抢先接管。 / 10529/20529 put the delivery button after the {@code SELECT11_1}
	 * sub-page: the page-chain guard must keep suppressing the delivery fallback, and the entry-page
	 * gate must not claim the open either.
	 */
	@Test
	void pageChainRowsNeverTakeTheEntryPageShortcut() throws Exception {
		for (int[] row : new int[][] { { 10529, 806075 }, { 20529, 806079 } }) {
			int questId = row[0];
			int npcId = row[1];
			QuestEngine engine = engineWith(productionDefinition(questId));
			Player player = playerAt(questId, 10);

			assertNull(router(engine).unlistedDeliveryTransition(player, npcId),
				"交付按钮在续页上的行不得被替按：" + questId);
			assertNull(router(engine).unlistedOpenPageTransition(player, npcId),
				"页链行不得走入口页门：" + questId);
		}
	}

	/**
	 * 入口页路由存在但本次未受理（条件不满足等）时，必须直接放弃，不得退回到 1009
	 * 交付兜底——否则仍会替玩家跳过入口页。 / When the entry-page route exists but is not
	 * accepted this time (unmet conditions and the like), the open must give up instead of
	 * falling back to the 1009 delivery, which would still skip the entry page.
	 */
	@Test
	void anEntryPageThatFailsToOpenNeverFallsBackToDelivery() throws Exception {
		int questId = 2620;
		List<Integer> attempted = new ArrayList<>();
		QuestEngine engine = new QuestEngine() {
			@Override
			public boolean onDialog(QuestEnv env) {
				attempted.add(env.getDialogId());
				return env.getDialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id();
			}
		};
		installEngineProvider(engine);
		engine.installProductionDefinitions(new ImmutableQuestCatalog(List.of(productionDefinition(questId))));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, 1 | (5 << 6) | (5 << 12));
		Npc npc = NativeTalkFixture.npc(204787);

		assertFalse(router(engine).onDialog(new QuestEnv(npc, player, 0, QuestDialogAction.USE_OBJECT.id())),
			"入口页未受理时开门必须放弃");
		assertEquals(List.of(QuestDialogAction.USE_OBJECT.id()), attempted,
			"入口页未受理时不得再尝试 1009 交付");
	}

	/**
	 * 入口页只在交付候选自己的 source 上找：同一任务其它 source 的 `-1` 页路由不得被借来
	 * 开门（否则会把玩家送进另一阶段的对话页）。 / The entry page is looked up on the delivery
	 * candidate's own source only: a `-1` page route on another source of the same quest must
	 * never be borrowed for the open (that would drop the player into another stage's page).
	 */
	@Test
	void theEntryPageComesFromTheDeliveryCandidatesOwnSource() {
		int questId = UNLISTED_ROW;
		CompiledQuestDefinition twoStages = quest(questId)
			.metadata(QuestMetadata.minimal("unlisted-two-stages", 0, "SIGNIFICANT"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("s0", project(QuestStatus.START, vars("var0", 0)))
			.node("s1", project(QuestStatus.START, vars("var0", 1)))
			.node("s2", project(QuestStatus.START, vars("var0", 2)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 3)))
			.on(new QuestEvent.UseItem(166_030_005))
			.from("s0").goTo("s1")
			// s1 的 -1 页路由：当前阶段自己就有一个入口页。
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.USE_OBJECT.id()))
			.from("s1").goTo("s1")
			.afterCommit(showQuestDialog(QuestDialogPage.SELECT2))
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.SETPRO1.id()))
			.from("s1").goTo("s2")
			// s2 的 -1 页路由：另一个阶段，不能被 s2 的交付候选借来开 s1 的门。
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.USE_OBJECT.id()))
			.from("s2").goTo("s2")
			.afterCommit(showQuestDialog(QuestDialogPage.SELECT3))
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.SET_SUCCEED.id()))
			.from("s2").goTo("reward")
			.afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
			.compile();
		QuestEngine engine = engineWith(twoStages);

		QuestDialogRouter.UnlistedDelivery openPage =
			router(engine).unlistedOpenPageTransition(playerAt(questId, 2), RENA);

		assertNotNull(openPage, "s2 自己的 -1 页路由必须被认领");
		assertEquals(QuestDialogPage.SELECT3.id(),
			((AfterCommitAction.ShowQuestDialog) openPage.transition().afterCommit().getFirst()).dialogId(),
			"只允许取交付候选同 source 的入口页");
	}

	/**
	 * 同一 source 上「-1 页面路由推进到下一阶段」不是入口页：重放它等于替玩家推任务，必须交回
	 * 单按钮交付兜底。 / A -1 page route that advances to the next stage is not an entry page:
	 * replaying it would advance the quest for the player, so the single-button delivery fallback
	 * stays in charge.
	 */
	@Test
	void anEntryPageThatAdvancesTheStageIsNotReplayedByTheOpen() {
		int questId = UNLISTED_ROW;
		CompiledQuestDefinition advancingPage = quest(questId)
			.metadata(QuestMetadata.minimal("unlisted-advancing-page", 0, "SIGNIFICANT"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("s0", project(QuestStatus.START, vars("var0", 0)))
			.node("s1", project(QuestStatus.START, vars("var0", 1)))
			.node("s2", project(QuestStatus.START, vars("var0", 2)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 3)))
			.on(new QuestEvent.UseItem(166_030_005))
			.from("s0").goTo("s1")
			// 页面路由把 s1 推进到 s2：不是入口页，开门不得重放。
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.USE_OBJECT.id()))
			.from("s1").goTo("s2")
			.afterCommit(showQuestDialog(QuestDialogPage.SELECT2))
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.SET_SUCCEED.id()))
			.from("s1").goTo("reward")
			.afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
			.compile();
		QuestEngine engine = engineWith(advancingPage);
		Player player = playerAt(questId, 1);

		assertNull(router(engine).unlistedOpenPageTransition(player, RENA),
			"推进阶段的 -1 路由不得被当作入口页");
		QuestDialogRouter.UnlistedDelivery delivery =
			router(engine).unlistedDeliveryTransition(player, RENA);
		assertNotNull(delivery, "单按钮交付兜底必须保留");
		assertEquals(QuestDialogAction.SET_SUCCEED.id(),
			((QuestEvent.TalkToNpc) delivery.transition().event()).dialogId());
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

	/**
	 * 带客户端页链的行（1007 的 Jucleas 形状）：入口页 → 续页 → 交付按钮。
	 * A row with a client page chain (the shape of quest 1007 at Jucleas): entry page → sub-page →
	 * delivery button.
	 */
	private static CompiledQuestDefinition pageChainRow(int questId) {
		return quest(questId)
			.metadata(QuestMetadata.minimal("unlisted-chain-" + questId, 0, "SIGNIFICANT"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("s0", project(QuestStatus.START, vars("var0", 0)))
			.node("s1", project(QuestStatus.START, vars("var0", 1)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 2)))
			.on(new QuestEvent.UseItem(166_030_005))
			.from("s0").goTo("s1")
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.QUEST_SELECT.id()))
			.from("s1").goTo("s1")
			.afterCommit(showQuestDialog(QuestDialogPage.SELECT3))
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.SELECT3_1.id()))
			.from("s1").goTo("s1")
			.afterCommit(showQuestDialog(QuestDialogPage.SELECT3_1))
			.on(new QuestEvent.TalkToNpc(RENA, QuestDialogAction.SETPRO3.id()))
			.from("s1").goTo("reward")
			.afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
			.compile();
	}

	/**
	 * 生产目录里的任务定义（编译后的 IR）。
	 * The production quest definition (compiled IR).
	 */
	private static CompiledQuestDefinition productionDefinition(int questId) throws Exception {
		try (InputStream input = QuestDialogUnlistedRowEntryTest.class
				.getResourceAsStream("/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			assertNotNull(input, "生产任务定义缺失 / production definition missing: " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestEngine engineWith(CompiledQuestDefinition... definitions) {
		QuestEngine engine = new QuestEngine();
		installEngineProvider(engine);
		engine.installProductionDefinitions(new ImmutableQuestCatalog(List.of(definitions)));
		return engine;
	}

	/**
	 * 锁定报告行的任务行入口：同 source 上必须存在 {@code QUEST_SELECT(31)} 页面路由，
	 * 否则客户端点任务行会落回通用页 10（与 1929 的 equipped96 同形）。
	 * Locks the task-row entry of the report step: a {@code QUEST_SELECT(31)} page route must exist on the
	 * same source, or the client row click falls back to page 10 (the shape of 1929's equipped96).
	 */
	private static void assertRowEntryPage(CompiledQuestDefinition definition, String sourceNode,
			int npcId, QuestDialogPage page) {
		QuestTransition rowRoute = definition.transitionsFor("TALK_TO_NPC").stream()
			.filter(transition -> sourceNode.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(QuestDialogAction.QUEST_SELECT.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow(() -> new AssertionError(
				"缺少任务行入口 / missing task-row entry: " + definition.id() + " " + sourceNode));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), rowRoute.afterCommit(),
			"任务行入口必须下发同一入口页 / the row entry must show the same page");
	}

	/** 把全局引擎单例指向被测引擎；tearDown 恢复原 provider。
	 * Points the engine singleton at the engine under test; tearDown restores the original provider. */
	private static void installEngineProvider(QuestEngine engine) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(QuestDialogUnlistedRowEntryTest.class.getName() + ".engine", engine);
		QuestEngine.setInstanceProvider(beanFactory.getBeanProvider(QuestEngine.class));
	}

	/** 读取当前全局引擎 provider。 / Reads the current global engine provider. */
	@SuppressWarnings("unchecked")
	private static ObjectProvider<QuestEngine> engineProvider() throws ReflectiveOperationException {
		Field field = QuestEngine.class.getDeclaredField("instanceProvider");
		field.setAccessible(true);
		return (ObjectProvider<QuestEngine>) field.get(null);
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
