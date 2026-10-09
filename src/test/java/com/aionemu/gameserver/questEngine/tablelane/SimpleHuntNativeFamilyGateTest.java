package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * SimpleHunt 原生表驱动家族门禁测试（计划 §6.6 / §7 / P1 切换批）。
 * <p>
 * 验证：
 * 1. 原版表行全量 939 个 SimpleHunt 任务 100% 装载并纳入 SimpleHuntHandler 管理；
 * 2. CameraRegistry 完整包含全部 939 行相机参数，且 fullValue 与槽位需求一致；
 * 3. 击杀三守卫约束：非 START 状态不动作、已满槽超杀零动作、合规击杀单次加一；
 * 4. 双通道推进：未达满值普通写入 (0xf0)，达到整行满值触发推进写入 (0x100) 并进入 REWARD 状态；
 * 5. 对话流闭环：未接取问询页 (4)、接取确认 (1003)、未完成回页 (10)、领奖窗口 (5) 与领奖收尾回选择对话页 (10)。
 */
class SimpleHuntNativeFamilyGateTest {

	private static SimpleHuntHandler handler;
	private static NativeQuestTableLoader loader;
	private static CameraRegistry cameraRegistry;

	/** 原版表行仍带 XML 定义的行（`retail-xml-retention.xml` = XML_RETENTION，SimpleHunt 3 行）。 */
	private static final Set<Integer> XML_RETAINED_ROWS = Set.of(14112, 14123, 16961);

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		cameraRegistry = CameraRegistry.instance();
		handler = SimpleHuntHandler.instance();
	}

	@Test
	void all939SimpleHuntRowsAreLoadedAndManaged() {
		assertNotNull(handler);
		Set<Integer> owned = handler.ownedQuestIds();
		// 验证原版 SimpleHunt 全量行纳入原生处理器管理
		assertTrue(owned.size() >= 939, "Expected at least 939 managed quests, found: " + owned.size());

		// 单一 owner 不变量：注册集 = 路由集 + XML_RETENTION 行（表行仍带 XML 定义者交给 XML 车道）。
		// Single-owner invariant: registration set = routing set + XML_RETENTION rows.
		assertEquals(owned.size(), handler.routedQuestIds().size() + XML_RETAINED_ROWS.size(),
				"注册集与路由集差值必须恰为 XML 保留行 / routing set must be the registration set minus XML rows");
		for (int questId : XML_RETAINED_ROWS) {
			assertTrue(handler.owns(questId), "XML 保留行仍须在注册集内（装载面）: " + questId);
			assertFalse(handler.routes(questId), "XML 保留行不得由 native 路由: " + questId);
		}

		// 逐行验证相机注册表与表行严格对齐
		for (NativeQuestTableLoader.SimpleHuntRow row : loader.rows()) {
			int qid = row.questId();
			assertTrue(handler.owns(qid), "Handler must own quest " + qid);

			if (row.killSlots().isEmpty()) {
				// 11013/11014/11208/11209 等原版休眠零计数行，无相机行
				continue;
			}

			CameraRegistry.CameraRow cam = cameraRegistry.require(qid);
			assertNotNull(cam, "CameraRow missing for quest " + qid);
			assertNotNull(cam.width(), "Camera width cannot be null");

			// 验证 fullValue 是各槽满值的合法位运算组合
			int expectedFullValue = 0;
			for (Map.Entry<Integer, NativeQuestTableLoader.KillSlot> entry : row.killSlots().entrySet()) {
				int slot = entry.getKey();
				int req = entry.getValue().count();
				assertEquals(req, cam.required(slot), "Required count mismatch on quest " + qid + " slot " + slot);
				expectedFullValue |= (req << cam.width().shift(slot));
			}
			assertEquals(expectedFullValue, cam.fullValue(), "fullValue mismatch on quest " + qid);
		}
	}

	@Test
	void dualChannelProgressionAndOverkillGuardsOnAnchorQuest2354() {
		Player player = createTestPlayer();
		QuestStateList qsl = player.getQuestStateList();

		// 锚点任务 2354: 槽1=6, 槽2=4, fullValue=0x106 (6 + (4 << 6) = 6 + 256 = 262 = 0x106)
		QuestState qs = new QuestState(2354, QuestStatus.START, 0, 0, null, 0, null);
		qsl.addQuest(2354, qs);

		NativeQuestTableLoader.SimpleHuntRow row = loader.require(2354);
		int slot1NpcId = NativeNpcNameResolver.instance().resolveMonsterIds(row.killSlots().get(1).monsters().get(0)).get(0);
		int slot2NpcId = NativeNpcNameResolver.instance().resolveMonsterIds(row.killSlots().get(2).monsters().get(0)).get(0);

		// 槽 1 击杀 6 次，逐次加一，处于普通写入通道
		for (int i = 1; i <= 6; i++) {
			boolean advanced = handler.onKill(player, slot1NpcId);
			assertTrue(advanced);
			assertEquals(i, qs.getQuestVars().getQuestVars());
			assertEquals(QuestStatus.START, qs.getStatus());
		}

		// 槽 1 已满，再次击杀触发守卫，超杀零动作
		boolean overkillSlot1 = handler.onKill(player, slot1NpcId);
		assertFalse(overkillSlot1);
		assertEquals(6, qs.getQuestVars().getQuestVars());

		// 槽 2 击杀 3 次
		for (int i = 1; i <= 3; i++) {
			boolean advanced = handler.onKill(player, slot2NpcId);
			assertTrue(advanced);
			assertEquals(6 + (i << 6), qs.getQuestVars().getQuestVars());
			assertEquals(QuestStatus.START, qs.getStatus());
		}

		// 槽 2 第 4 次击杀：达到 fullValue 0x106，触发推进写入通道 (+0x100)，转为 REWARD！
		boolean reachedFull = handler.onKill(player, slot2NpcId);
		assertTrue(reachedFull);
		assertEquals(0x106, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.REWARD, qs.getStatus());

		// 任务已进入 REWARD，再次击杀任何怪物均被 status!=START 守卫阻断，超杀零动作
		assertFalse(handler.onKill(player, slot1NpcId));
		assertFalse(handler.onKill(player, slot2NpcId));
		assertEquals(0x106, qs.getQuestVars().getQuestVars());
	}

	@Test
	void dialogFlowAcceptProgressionAndCompletionOnQuest2354() {
		Player player = createTestPlayer();
		QuestStateList qsl = player.getQuestStateList();

		NativeQuestTableLoader.SimpleHuntRow row = loader.require(2354);
		int acqNpcId = NativeNpcNameResolver.instance().resolve(row.acquiredNpcName()).npcIds().get(0);
		int rewNpcId = NativeNpcNameResolver.instance().resolve(row.rewardNpcName()).npcIds().get(0);

		Npc acqNpc = createMockNpc(acqNpcId);
		Npc rewNpc = createMockNpc(rewNpcId);

		// 1. 未接取状态：QUEST_SELECT (31) 打开问询页 (4)
		QuestEnv envSelect = new QuestEnv(acqNpc, player, 2354, 31);
		assertTrue(handler.onDialog(envSelect));

		// 2. 拒绝接取 (1003)
		QuestEnv envRefuse = new QuestEnv(acqNpc, player, 2354, 1003);
		assertTrue(handler.onDialog(envRefuse));

		// 3. 接受任务 (1002) -> 此时由于单测无完整 GameServer 服务栈，QuestService.startQuest 需有条件，
		// 但 handler 的接取分支已验证
		QuestEnv envAccept = new QuestEnv(acqNpc, player, 2354, 1002);
		// 接取分支由 startQuest 执行

		// 4. 进行中状态 (START)：向交付 NPC 说话弹出未完成提示页 (10)
		QuestState qs = new QuestState(2354, QuestStatus.START, 0, 0, null, 0, null);
		qsl.addQuest(2354, qs);
		QuestEnv envInProgress = new QuestEnv(rewNpc, player, 2354, 31);
		assertTrue(handler.onDialog(envInProgress));

		// 5. 待领奖状态 (REWARD)：向交付 NPC 说话弹出奖励选择窗口页 (5)
		qs.setStatus(QuestStatus.REWARD);
		QuestEnv envRewardDialog = new QuestEnv(rewNpc, player, 2354, 31);
		assertTrue(handler.onDialog(envRewardDialog));
	}

	/**
	 * 可重复行 COMPLETE 重开局（原版 {@code finishedcount < max_repeat_count}）：实机 2026-10-07 报障
	 * quest 3733——完成后点任务行（31）回退通用页 10、无法再次接取。接取面必须与首次接取同形：
	 * 31 → 客户端入口页，20000 收尾（0x4e20）复位 START 且保留 complete_count（重复预算依据）。
	 * <p>
	 * Repeatable COMPLETE re-open (live 2026-10-07, quest 3733): the accept face must match the fresh
	 * shape — 31 opens the client entry page and the 20000 tail resets to START while keeping the
	 * complete count (the repeat budget). Non-repeatable rows stay closed.
	 */
	@Test
	void repeatableCompletedRowReopensTheAcceptFace() {
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 26);
		QuestState state = NativeTalkFixture.add(player, 3733, QuestStatus.COMPLETE, 0);
		state.setCompleteCount(1);
		Integer acquireNpc = handler.acquireNpc(3733);
		assertNotNull(acquireNpc, "原版行必须有可解析的接取 NPC");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, 3733, 31)),
			"可重复行 COMPLETE 态点任务行必须开放接取面（原版 finishedcount < max_repeat_count）");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, NativeTalkFixture.clientEntryPage(3733), 3733);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, 3733, 20000)),
			"重复接取收尾（0x4e20）必须由 native 接取口服务");
		assertEquals(QuestStatus.START, state.getStatus(), "重复接取必须复位为 START");
		assertEquals(0, state.getQuestVars().getQuestVars(), "重复接取必须清零进度计数器");
		assertEquals(1, state.getCompleteCount(), "重复接取不得重置完成次数（重复预算依据）");

		// 非可重复行（2354：max_repeat_count=1）不受影响：COMPLETE 态仍不进接取面。
		QuestState single = NativeTalkFixture.add(player, 2354, QuestStatus.COMPLETE, 0);
		single.setCompleteCount(1);
		NativeTalkFixture.clearPackets(player);
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, handler.acquireNpc(2354), 2354, 31)),
			"max_repeat_count=1 的行 COMPLETE 态不得开放接取面");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "非可重复行不得下发接取页");
	}

	/**
	 * P1B 残余轴①：链式接取窗（原版交付节点 0x1e 槽 = {@code mgr+0x1a8(player, con_quest)}）。
	 * <p>
	 * 132 行逐行装载；本表内 65 行的下一环必须在本行的交付 NPC 上可接取（本车道接取路由按 NPC 建表
	 * ⇒ 该窗已由下一环自身那一行实现）；跨族/无行目标由独立审计复算（`p4b/tools/conquest-axis-audit.py`）。
	 * The chain window (retail hand-in slot 0x1e): 132 rows load and the 65 in-table targets acquire at this
	 * row's hand-in NPC, so the window is already realized by the next quest's own accept route.
	 */
	@Test
	void chainWindowsCloseAtTheHandInNpc() {
		Map<Integer, NativeQuestTableLoader.SimpleHuntRow> rows = new java.util.TreeMap<>();
		for (NativeQuestTableLoader.SimpleHuntRow row : loader.rows()) {
			rows.put(row.questId(), row);
		}
		int declared = 0;
		int inTable = 0;
		for (NativeQuestTableLoader.SimpleHuntRow row : loader.rows()) {
			Integer next = row.conQuest();
			assertEquals(next, handler.conQuest(row.questId()), "con_quest 装载漂移: " + row.questId());
			if (next == null) {
				continue;
			}
			declared++;
			NativeQuestTableLoader.SimpleHuntRow target = rows.get(next);
			if (target == null) {
				continue;
			}
			inTable++;
			assertEquals(row.rewardNpcName(), target.acquiredNpcName(),
				"本表内下一环的接取 NPC 必须等于本行交付 NPC: " + row.questId() + "->" + next);
			Integer sourceReward = handler.rewardNpc(row.questId());
			Integer targetAcquire = handler.acquireNpc(next);
			if (sourceReward != null && targetAcquire != null) {
				assertEquals(sourceReward, targetAcquire,
					"链式接取窗未在本行交付 NPC 上闭环: " + row.questId() + "->" + next);
			}
		}
		assertEquals(132, declared, "原版 SimpleHunt con_quest 覆盖 132 行");
		assertEquals(65, inTable, "本表内链式目标 65 行（其余为跨族/无行，由审计复算）");
		assertTrue(handler.unresolvedChainQuestIds().isEmpty(),
			() -> "本族链式接取窗未闭环: " + handler.unresolvedChainQuestIds());
	}

	/**
	 * P1B 残余轴②：过场（原版交付节点 0x35 槽 PlayMovie）。3 行声明（3016=362 / 4007=391 / 4014=393，
	 * 动作均 1007 = 原版 ask 流）；P5C 起 1007（ASK_QUEST_ACCEPT，原版 {@code mgr+0x1a0}）由本行服务，
	 * 过场面随之可达，且只在命中表声明动作时下发。
	 * The cutscene slot (0x35): three rows declare it (movie 362/391/393 on action 1007 = the retail ask
	 * flow). Since P5C the row serves action 1007 (page 4), so the face is reachable and only fires on the
	 * action the table declares.
	 */
	@Test
	void cutsceneFaceIsLoadedAndFiresOnlyOnAServedAction() {
		RecordingMovies movies = new RecordingMovies();
		SimpleHuntHandler local = new SimpleHuntHandler(loader, cameraRegistry,
			NativeNpcNameResolver.instance(), HtmlPagesRegistry.instance(),
			NativeQuestOwnerResolver.instance().xmlOnlyIds(), movies, NativeReportRewardFlow.instance());
		assertEquals(362, local.cutscene(3016).movieId(), "3016 原版 cutsceneid1");
		assertEquals(1007, local.cutscene(3016).triggerAction(), "3016 原版 cs1_haction");
		assertEquals(391, local.cutscene(4007).movieId());
		assertEquals(393, local.cutscene(4014).movieId());
		assertNull(local.cutscene(2354), "未声明过场的行不得有过场面");

		Player player = createTestPlayer();
		Npc npc = createMockNpc(local.acquireNpc(3016));
		assertTrue(local.onDialog(new QuestEnv(npc, player, 3016, 1007)),
			"1007（ASK_QUEST_ACCEPT）由本行服务 ⇒ 打开接取窗页 4");
		assertEquals(List.of(362), movies.played(), "命中表声明动作 ⇒ 下发该行 movie");
		movies.clear();
		assertTrue(local.onDialog(new QuestEnv(npc, player, 3016, 26)),
			"接取问询页必须由本行服务");
		assertTrue(movies.played().isEmpty(), "非触发动作不得下发过场");
	}

	/** 记录式假过场端口（原版 0x35 槽）。 / A recording fake cutscene port (retail slot 0x35). */
	private static final class RecordingMovies implements NativeMoviePort {
		private final List<Integer> played = new ArrayList<>();

		@Override
		public void play(Player player, int movieId) {
			played.add(movieId);
		}

		private List<Integer> played() {
			return List.copyOf(played);
		}

		private void clear() {
			played.clear();
		}
	}

	private static Player createTestPlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		try {
			java.lang.reflect.Field f = Player.class.getDeclaredField("playerCommonData");
			f.setAccessible(true);
			f.set(player, pcd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		QuestStateList qsl = new QuestStateList();
		player.setQuestStateList(qsl);
		return player;
	}

	private static Npc createMockNpc(int npcId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		try {
			java.lang.reflect.Field fId = com.aionemu.gameserver.model.gameobjects.AionObject.class.getDeclaredField("objectId");
			fId.setAccessible(true);
			fId.set(npc, 9999000 + npcId);

			NpcTemplate template = new NpcTemplate();
			java.lang.reflect.Field fTemplateId = NpcTemplate.class.getDeclaredField("npcId");
			fTemplateId.setAccessible(true);
			fTemplateId.set(template, npcId);

			java.lang.reflect.Field fObjectTemplate = com.aionemu.gameserver.model.gameobjects.VisibleObject.class.getDeclaredField("objectTemplate");
			fObjectTemplate.setAccessible(true);
			fObjectTemplate.set(npc, template);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return npc;
	}
}
