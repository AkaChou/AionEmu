package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.w3c.dom.Element;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.FreezeReason;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * P7 步 2 步 d→步 f 门：DD 原生**进度运行时**（八类接线 + 五类接取面〔含步 f kind-6 进区〕+
 * TELEPORT/SPAWN/MESSAGE/GIVE/REMOVE/CUTSCENE 动作面 × 真端执行矩阵）与**切换批**语义。
 * <p>
 * 冻结点：① 生产口径 = 切换集 1467 全量接管（步 f 起 routed 1444 / frozen 23，interests 非空）；
 * ② 兴趣面按 payload 逐类建立（Hunt 组名 → 怪物 id、Talk/CollectItem → NPC、TalkFOBJ → NPC、
 * ItemPlay → 物品 id、EnterArea → 同名注册区、EnterWorld → worldId、PvP → 行步）+ 接取兴趣面
 * （Talk 对话 NPC〔含挑战哨兵 = reward_npc_name〕/ 物品获得 / 进世界 / 等级等值 / 进区别名）；③ 事件语义 =
 * 真端 handler 逐指令 + 接取面（Talk `FUN_180c47220` 词汇、ItemPlay/EnterWorld/EnterArea 双角色、
 * LevelUp/LevelUpLogIn 等级等值遍历）+ 附加动作面（执行器 case 1/2/3/4/5/7；**步 f 执行矩阵修正**：
 * 推进边对 Hunt/EnterArea/TalkFOBJ/**Talk** 执行——对话平面推进/完成汇入 `FUN_180c4d5b0(…,-1)` 在
 * C:2075066 直调 c8d0，门 = 完成步 kind==4；Talk 对话接取 1002/20000 收尾同走 -1 路径 ⇒ 步 0 动作；
 * CollectItem 拾取分支无执行器 ⇒ 排除）；④ 冻结面闭合（routed ∪ frozen = 切换集，互斥；ZONE_ABSENT 9
 * §10.3-#23 + ACTION_UNFACED 14〔col9/col10/c8d0 步 col6〕，证据
 * `.agents/summary/quest-engine-native/p7/tools/dd-planrow-e2-mirror.py`）。
 * <p>
 * P7 step 2d..f gate for the native DD progress runtime: the production singleton owns the whole
 * switch set (1444 routed / 23 frozen since step f), per-kind interest construction including the five
 * acquire faces, retail event semantics with the acquire vocabulary and the faced extra actions under
 * the corrected execution matrix, and a closed routed/frozen split of the switch set.
 */
class DataDrivenNativeRuntimeGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.xml";
	private static final Path ZONES_DIR = Path.of("src/main/resources/aion/data/static_data/zones");
	private static final Pattern ZONE_NAME = Pattern.compile("<zone\\b[^>]*\\bname=\"([^\"]+)\"");

	/** 切换集规模（真端活行 ∧ owner RETAIL_TABLE）。 / Switch-set size frozen by the P7 contract batch. */
	private static final int SWITCH_ROWS = 1467;

	private static DataDrivenQuestTable table;
	private static Set<Integer> switchSet;
	private static NativeEnterAreaPort enterAreaPort;
	private static RetailItemNameIndex itemIndex;
	private static NativeTalkFixture.RecordingInventory inventory;
	private static RecordingMovies movies;
	private static RecordingTeleports teleports;
	private static RecordingSpawns spawns;
	private static RecordingSays says;
	private static RecordingTimers timers;
	private static RecordingClaims claims;
	private static DataDrivenNativeRuntime runtime;

	@BeforeAll
	static void loadFixtures() throws Exception {
		table = DataDrivenQuestTable.load(resource(DD_TABLE));
		Map<Integer, String> owners = retentionOwners();
		switchSet = new TreeSet<>();
		for (int questId : table.questIds()) {
			if ("RETAIL_TABLE".equals(owners.get(questId))) {
				switchSet.add(questId);
			}
		}
		Set<String> zoneNames = new TreeSet<>();
		for (Path path : zoneFiles()) {
			Matcher matcher = ZONE_NAME.matcher(Files.readString(path, StandardCharsets.UTF_8));
			while (matcher.find()) {
				zoneNames.add(matcher.group(1));
			}
		}
		enterAreaPort = NativeEnterAreaPort.create(table, switchSet, zoneNames);
		itemIndex = RetailItemNameIndex.loadItemTemplates();
		inventory = new NativeTalkFixture.RecordingInventory();
		movies = new RecordingMovies();
		teleports = new RecordingTeleports();
		spawns = new RecordingSpawns();
		says = new RecordingSays();
		timers = new RecordingTimers();
		claims = new RecordingClaims();
		if (com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.current().isEmpty()) {
			// 领奖口的元数据来源（真端驱动，与生产目录同一条装载路径；与领奖门禁同型初始化）。
			// The claim flow's metadata source (same loader as production; mirrors the claim gate).
			com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.overlay(
				com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog.fromEntries(List.of()));
		}
		runtime = DataDrivenNativeRuntime.create(table, switchSet, NativeNpcNameResolver.instance(),
			enterAreaPort, itemIndex, inventory, movies, teleports, spawns, says, timers,
			NativeReportRewardFlow.withSink(claims));
	}

	/** 记录式领奖结算体（交付报告面接线断言用）。 / A recording claim sink for the delivery face. */
	private static final class RecordingClaims implements NativeReportRewardFlow.CompletionSink {
		private int calls;

		@Override
		public boolean complete(QuestEnv env, int rewardTier, QuestTemplate template) {
			calls++;
			return true;
		}
	}

	/**
	 * ① 生产单例 = 切换批语义（步 f 起）：切换集 1467 全量接管（routed 1457 / frozen 10——
	 * 2026-10-02 第三批 col10 Timer 落面解冻 9 行；2026-10-03 第七批 col6 Delay 空桩落面
	 * 解冻 2 行；2026-10-03 第九批 col9 EnterInstance 落面解冻 2 行〔10034/20034〕；残留 =
	 * ZONE_ABSENT 9 + ACTION_UNFACED 1〔20032，creation 2 落点别名真端内在缺失〕）；非路由行的
	 * 事件仍恒 false（冻结行不接事件面）。
	 * The production singleton owns the switch set since step f; non-routed rows still answer false.
	 */
	@Test
	void productionRuntimeRoutesTheSwitchSet() {
		DataDrivenNativeRuntime production = DataDrivenNativeRuntime.instance();
		assertEquals(1467, production.ownedQuestIds().size(), "生产接管 = 切换集全量");
		assertEquals(1457, production.routedQuestIds().size(), "可路由 1457（col9 EnterInstance 落面后）");
		assertEquals(10, production.frozenQuestIds().size(),
			"显式冻结 10（ZONE_ABSENT 9 + 20032；镜像同值）");
		assertFalse(production.killInterests().isEmpty(), "击杀兴趣面已注册");
		assertFalse(production.zoneInterests().isEmpty(), "进区兴趣面已注册");
		assertFalse(production.acquireTalkInterests().isEmpty(), "接取对话兴趣面已注册");
		assertFalse(production.acquireZoneInterests().isEmpty(), "接取进区兴趣面已注册（步 f kind-6）");
		// 冻结行（如 10033）不得出现在任何接取面。
		for (int frozenId : production.frozenQuestIds().keySet()) {
			assertFalse(production.routedQuestIds().contains(frozenId), "冻结行不路由: " + frozenId);
		}
		// 非切换集行的事件恒 false。
		Player player = player(1, Set.of());
		assertFalse(production.onKill(player, 1), "非切换集击杀恒 false");
		assertFalse(production.onEnterWorld(player, 210040000), "无接取面/无进度行的进世界恒 false");
	}

	/**
	 * ①b 接取 NPC 必须同时进 onQuestStart 注册面：附近任务提示轴（SM_NEARBY_QUESTS）的候选集 =
	 * {@code WorldMapInstance} 从 NPC 的 onQuestStart 并集枚举，DD talk 接取任务缺该注册就永远
	 * 进不了候选集（§10.3-#18 的 DD 侧残余）。对话接取流不经该注册，注册只喂提示面。
	 * ①b Acquire NPCs must also land in the onQuestStart registry: the nearby-quests hint axis
	 * (SM_NEARBY_QUESTS) enumerates candidates from the per-NPC onQuestStart union in
	 * WorldMapInstance — without it DD talk-acquire quests never reach the candidate set (the
	 * DD-side residual of §10.3-#18). The dialog acquire flow does not read this registry.
	 */
	@Test
	void acquireTalkInterestsAlsoRegisterTheNearbyQuestCandidateFace() {
		// installInterest 的击杀注册会查 NPC 模板（registerCanAct 告警面）；测试环境喂空 NpcData，
		// 与 QuestEngineEscortAndProximityRegistrationTest 同模式。
		// The kill-side registration consults NPC templates (registerCanAct warn face); feed an
		// empty NpcData like QuestEngineEscortAndProximityRegistrationTest does.
		com.aionemu.gameserver.dataholders.NpcData previous = com.aionemu.gameserver.dataholders.DataManager.NPC_DATA;
		com.aionemu.gameserver.dataholders.DataManager.NPC_DATA = new com.aionemu.gameserver.dataholders.NpcData();
		try {
			QuestEngine engine = new QuestEngine();
			runtime.installInterest(engine);
			var acquireTalks = runtime.acquireTalkInterests();
			assertFalse(acquireTalks.isEmpty(), "接取对话兴趣面非空（口径失效）");
			for (Map.Entry<Integer, List<Integer>> entry : acquireTalks.entrySet()) {
				List<Integer> onStart = engine.getQuestNpc(entry.getKey()).getOnQuestStart();
				for (int questId : entry.getValue()) {
					assertTrue(onStart.contains(questId), () -> "接取 NPC " + entry.getKey()
						+ " 的任务 " + questId + " 未进 onQuestStart（附近任务提示候选面缺失）");
				}
			}
			// 冻结行不得借道任何接取 NPC 的 onQuestStart 注册进入候选集。
			for (int frozenId : runtime.frozenQuestIds().keySet()) {
				for (int npcId : acquireTalks.keySet()) {
					assertFalse(engine.getQuestNpc(npcId).getOnQuestStart().contains(frozenId),
						() -> "冻结行 " + frozenId + " 不应出现在接取 NPC " + npcId + " 的 onQuestStart");
				}
			}
		} finally {
			com.aionemu.gameserver.dataholders.DataManager.NPC_DATA = previous;
		}
	}

	/** ② 逐行裁定闭合：切换集 = 可路由 ∪ 冻结（互斥，无第三桶）。 / Closed routed/frozen split. */
	@Test
	void routedAndFrozenPartitionTheSwitchSet() {
		assertEquals(SWITCH_ROWS, switchSet.size(), "切换集规模冻结");
		Set<Integer> owned = new TreeSet<>(runtime.ownedQuestIds());
		Set<Integer> routed = new TreeSet<>(runtime.routedQuestIds());
		Set<Integer> frozen = new TreeSet<>(runtime.frozenQuestIds().keySet());
		assertEquals(switchSet, owned, "DD 表 ∩ 路由集 = 切换集");
		Set<Integer> union = new TreeSet<>(routed);
		union.addAll(frozen);
		assertEquals(owned, union, "routed ∪ frozen = 切换集");
		assertTrue(java.util.Collections.disjoint(routed, frozen), "两桶互斥");
	}

	/** ③ 兴趣面逐元素复算：每个已路由步在对应兴趣面上恰好有键。 / Interest closure per kind. */
	@Test
	void everyRoutedStepYieldsItsInterestKey() {
		int hunt = 0;
		int talk = 0;
		int fobj = 0;
		int zone = 0;
		int world = 0;
		int pvp = 0;
		int itemPlay = 0;
		int collectItem = 0;
		for (int questId : runtime.routedQuestIds()) {
			Row row = table.find(questId).orElseThrow();
			for (Step step : row.steps()) {
				DataDrivenNativeRuntime.StepHit expected = new DataDrivenNativeRuntime.StepHit(questId,
					step.index(), 0);
				switch (step.kind()) {
					case HUNT -> {
						hunt++;
						assertFalse(runtime.killInterests().isEmpty(), "Hunt 步必须有击杀兴趣");
					}
					case TALK -> {
						talk++;
						assertTrue(runtime.talkInterests().values().stream().anyMatch(hits -> contains(hits, expected)),
							"Talk 步必须落对话兴趣：" + questId + "#" + step.index());
					}
					case COLLECT_ITEM -> {
						collectItem++;
						assertTrue(runtime.talkInterests().values().stream().anyMatch(hits -> contains(hits, expected)),
							"CollectItem 步必须落对话兴趣（真端 kind 1 = NPC 对话平面）：" + questId + "#" + step.index());
					}
					case ITEM_PLAY -> {
						itemPlay++;
						assertTrue(runtime.itemPlayInterests().values().stream()
							.anyMatch(hits -> contains(hits, expected)),
							"ItemPlay 步必须落物品获得兴趣（真端 kind 3 = 事件 5）：" + questId + "#" + step.index());
					}
					case TALK_FOBJ -> {
						fobj++;
						assertTrue(runtime.fobjInterests().values().stream().anyMatch(hits -> contains(hits, expected)),
							"TalkFOBJ 步必须落 FOBJ 兴趣：" + questId + "#" + step.index());
					}
					case ENTER_AREA -> {
						zone++;
						assertTrue(runtime.zoneInterests().values().stream().anyMatch(hits -> contains(hits, expected)),
							"EnterArea 步必须落进区兴趣：" + questId + "#" + step.index());
					}
					case ENTER_WORLD -> {
						world++;
						assertTrue(runtime.worldInterests().values().stream().anyMatch(hits -> contains(hits, expected)),
							"EnterWorld 步必须落进世界兴趣：" + questId + "#" + step.index());
					}
					case PVP -> {
						pvp++;
						List<Integer> steps = runtime.pvpSteps().get(questId);
						assertTrue(steps != null && steps.contains(step.index()),
							"PvP 步必须落 PvP 行集：" + questId + "#" + step.index());
					}
				}
			}
		}
		assertTrue(hunt > 0 && talk > 0 && fobj > 0 && zone > 0 && world > 0 && pvp > 0 && itemPlay > 0
			&& collectItem > 0, "八类均须有已路由步");
	}

	private static boolean contains(List<DataDrivenNativeRuntime.StepHit> hits,
			DataDrivenNativeRuntime.StepHit expected) {
		for (DataDrivenNativeRuntime.StepHit hit : hits) {
			if (hit.questId() == expected.questId() && hit.stepIndex() == expected.stepIndex()) {
				return true;
			}
		}
		return false;
	}

	/** ④ Hunt：组计数按真端守卫自增；达标收口。 / Hunt group counters and closure. */
	@Test
	void huntKillAdvancesGroupCounter() {
		int questId = firstRouted(Kind.HUNT, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.killInterests(), questId, stepIndex);
		assertTrue(npcId > 0, "Hunt 兴趣键必须是怪物 id");
		Player player = player(10, Set.of(questId));
		QuestState state = player.getQuestStateList().getQuestState(questId);
		state.getQuestVars().setVar(stepIndex);
		assertTrue(runtime.onKill(player, npcId), "命中当前步组槽必须推进");
		assertTrue(state.getQuestVars().getQuestVars() >= stepIndex, "组槽自增不得回退步号");
	}

	/**
	 * ④b Hunt 距离门（偏差修复第八批落面）：真端处理函数前奏 case 0 = {@code 2500.0 < distSq → return}
	 * （def+0x70 恒 0 ⇒ 恒 2500 = 50m 平方；param_6 = 成员↔死亡对象平方欧氏距离，生产链 =
	 * `AllianceBattleGroup::GetValidMember` → NS_VALID_MEMBER_LIST 0xff78 → NPCSvr64
	 * `PacketValidMemberList` → `FUN_180c46020` 第 6 参）。
	 * Hunt distance gate (deviation-fix batch 8): retail handler-preamble case 0 with the squared
	 * 50 m bound; the argument is the squared member-to-dead-object distance.
	 */
	@Test
	void killProgressMirrorsTheRetailDistanceGate() {
		int questId = firstRouted(Kind.HUNT, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.killInterests(), questId, stepIndex);
		// 界内（49m）：进度照常。 / Inside (49 m): progress as usual.
		Player near = player(30, Set.of(questId));
		place(near, 0f, 0f, 0f);
		near.getQuestStateList().getQuestState(questId).getQuestVars().setVar(stepIndex);
		assertTrue(runtime.onKillAt(near, npcId, 49f, 0f, 0f), "50m 内击杀必须推进");
		// 界外（51m）：零动作零写。 / Outside (51 m): no progress, no write.
		Player far = player(31, Set.of(questId));
		place(far, 0f, 0f, 0f);
		far.getQuestStateList().getQuestState(questId).getQuestVars().setVar(stepIndex);
		assertFalse(runtime.onKillAt(far, npcId, 51f, 0f, 0f), "50m 外距离门必须拒绝进度");
		assertEquals(stepIndex, DataDrivenProgress
			.step(far.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars()),
			"距离门外不得写 vars");
		// 边界：恰好 2500（50m）= 通过（真端是严格大于才拒绝）；z 轴参与平方和。
		// Boundary: exactly 2500 (50 m) passes (retail rejects only strictly greater); z enters the sum.
		assertTrue(DataDrivenNativeRuntime.withinRetailKillDistance(50f, 0f, 0f), "50m 整等于界内");
		assertFalse(DataDrivenNativeRuntime.withinRetailKillDistance(30f, 40f, 0.1f),
			"三维平方和超界必须拒绝");
	}

	/** ⑤ EnterArea / EnterWorld：直接步进（真端写目标步）。 / Direct advance kinds. */
	@Test
	void directAdvanceKindsMoveTheStepNumber() {
		assertDirectAdvance(Kind.ENTER_AREA, (player, questId, stepIndex, step) -> {
			String zone = enterAreaPort.zoneName(questId, stepIndex).orElseThrow();
			return runtime.onEnterZone(player, zone);
		});
		assertDirectAdvance(Kind.ENTER_WORLD, (player, questId, stepIndex, step) ->
			runtime.onEnterWorld(player, Integer.parseInt(step.payload().trim())));
	}

	private void assertDirectAdvance(Kind kind, StepEvent event) {
		int questId = firstRouted(kind, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		Player player = player(11, Set.of(questId));
		QuestState state = player.getQuestStateList().getQuestState(questId);
		state.getQuestVars().setVar(stepIndex);
		Step step = table.find(questId).orElseThrow().steps().getFirst();
		assertTrue(event.fire(player, questId, stepIndex, step), kind + " 命中必须直接步进");
		assertEquals(stepIndex + 1, state.getQuestVars().getQuestVars(),
			kind + " 直接步进写 = 步号 + 1");
	}

	/**
	 * ⑤a Talk 步 = 真端共享对话平面（`FUN_180c474b0`）：打开发阶段页 select(K+1) 不写状态，
	 * 顺序动作 `10000+K`（K == 当前步 + 1）步进，乱序零写零回发。
	 * Talk steps follow the shared dialog plane: open serves the stage page, sequential actions advance.
	 */
	@Test
	void talkStepsFollowTheSharedRetailDialogPlane() {
		int questId = firstRouted(Kind.TALK, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.talkInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		// 打开：阶段页（select1 = 1011），步号不变。页动作携带任务上下文（真端客户端在进度页发 questId）。
		assertTrue(runtime.onDialog(player, npcId, 31, 1, questId), "对话打开必须被对话平面服务");
		NativeTalkFixture.assertOnlyDialogPage(player, DataDrivenNativeRuntime.stagePageForGate(stepIndex));
		assertEquals(stepIndex, DataDrivenProgress.step(state.getQuestVars().getQuestVars()), "打开不写步号");
		// 乱序动作（真端 `code-9999 != 当前步 + 1`）：零写、零回发。
		NativeTalkFixture.clearPackets(player);
		assertFalse(runtime.onDialog(player, npcId, 10000 + stepIndex + 2, 1, questId), "乱序动作必须静默");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "乱序动作不发页");
		// 顺序动作（真端守卫：code-9999 == 当前步 + 1 ⇔ code == 10000 + 当前步）：步号 + 1。
		assertTrue(runtime.onDialog(player, npcId, 10000 + stepIndex, 1, questId), "顺序动作必须步进");
		assertEquals(stepIndex + 1, DataDrivenProgress.step(state.getQuestVars().getQuestVars()),
			"顺序动作写 = 步号 + 1");
	}

	/**
	 * ⑤b 报告动作 1009：步进 + 报告通道（末步 = 待领奖 + 奖励窗页 5；真端 `player+0x100` + `mgr+0x1b0`）。
	 * The report action advances and opens the reward window on the final step.
	 */
	@Test
	void reportActionAdvancesAndOpensTheRewardWindowOnTheLastStep() {
		int questId = firstRoutedTalkOnLastStep();
		Row row = table.find(questId).orElseThrow();
		int stepIndex = row.steps().getLast().index();
		int npcId = keyOf(runtime.talkInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		assertTrue(runtime.onDialog(player, npcId, 1009, 1, questId), "1009 必须被报告通道服务");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus(),
			"末步 1009 = 真端 SetQuestSuccess ⇒ 待领奖");
		NativeTalkFixture.assertOnlyDialogPage(player, 5);
	}

	/**
	 * ⑤c ItemPlay 步 = 物品获得事件（真端事件 5 / `FUN_180c46e90`）：物品 id 匹配 + 组 1 计数 +
	 * 达标收口；未声明物品零动作。
	 * ItemPlay steps count the acquired item id; undeclared items are zero actions.
	 */
	@Test
	void itemPlayStepsCountTheAcquiredItem() {
		int questId = firstRouted(Kind.ITEM_PLAY, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int itemId = keyOf(runtime.itemPlayInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		assertFalse(runtime.onItemAcquired(player, itemId + 1), "未声明物品零动作");
		assertTrue(runtime.onItemAcquired(player, itemId), "命中物品必须推进组槽");
		assertTrue(DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1) >= 1
			|| state.getStatus() == QuestStatus.REWARD, "组 1 计数或收口");
	}

	/** ⑥ TalkFOBJ：组计数二值（0→1），超杀零动作。 / TalkFOBJ binary groups. */
	@Test
	void talkFobjGroupsAreBinary() {
		int questId = firstRouted(Kind.TALK_FOBJ, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.fobjInterests(), questId, stepIndex);
		Player player = player(12, Set.of(questId));
		QuestState state = player.getQuestStateList().getQuestState(questId);
		state.getQuestVars().setVar(stepIndex);
		assertTrue(runtime.onDialog(player, npcId, 31, 1, questId), "FOBJ 首次交互 0→1");
		boolean again = runtime.onDialog(player, npcId, 31, 1, questId);
		assertFalse(again, "二值组槽已置 1 ⇒ 超杀零动作（真端 uVar15 == 0 守卫）");
	}

	/** ⑦ PvP：军衔区间 + 等级差闸门（真端 `FUN_180c46980`）。 / PvP gates. */
	@Test
	void pvpGatesFollowTheRetailComparisons() {
		int questId = firstRoutedPvpWithGate();
		Row row = table.find(questId).orElseThrow();
		Step step = row.steps().getFirst();
		Player killer = player(13, Set.of(questId));
		Player victim = player(14, Set.of());
		// 距离门夹具：同点 ⇒ 0 距离过门（本测试只裁军衔/等级差轴）。 / Same point ⇒ the distance gate passes.
		place(killer, 0f, 0f, 0f);
		place(victim, 0f, 0f, 0f);
		QuestState state = killer.getQuestStateList().getQuestState(questId);
		state.getQuestVars().setVar(step.index());
		// 闸门内：受害者等级 + levelGap >= 击杀者等级，军衔落在区间内。
		int minRank = optionalInt(step.column(1));
		int maxRank = optionalInt(step.column(2));
		int gap = optionalInt(step.column(3));
		AbyssRankEnum rank = rankWithin(minRank, maxRank);
		setLevel(victim, 60);
		setLevel(killer, 60);
		assertTrue(runtime.onKillRanked(killer, victim, rank), "闸门内必须推进");
		// 闸门内计数 +1（单组槽 bit6 起）。
		assertTrue(DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1) >= 1
			|| state.getStatus() == QuestStatus.REWARD, "单组计数或收口");
		// 闸门失败：等级差不足（`killerLevel <= victimLevel + gap` 不成立）。
		Player killer2 = player(15, Set.of(questId));
		Player weakVictim = player(16, Set.of());
		place(killer2, 0f, 0f, 0f);
		place(weakVictim, 0f, 0f, 0f);
		killer2.getQuestStateList().getQuestState(questId).getQuestVars().setVar(step.index());
		setLevel(killer2, 40 + gap + 5);
		setLevel(weakVictim, 40);
		assertFalse(runtime.onKillRanked(killer2, weakVictim, rank),
			"`killerLevel <= victimLevel + gap` 不成立 ⇒ 零动作");
		assertEquals(step.index(),
			DataDrivenProgress.step(killer2.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars()),
			"闸门失败不得写 vars");
	}

	/**
	 * ⑦b PvP 距离门：真端 Pvp 处理函数（`FUN_180c46980`，槽 +0x528）与 Hunt 共用同一距离前奏——
	 * 成员↔死亡玩家平方距离 > 2500（50m）⇒ 零动作；距离门先于军衔/等级差逐任务闸门。
	 * PvP distance gate: the retail PvP handler shares the Hunt preamble — beyond the squared 50 m
	 * bound the progress is a no-op, ahead of the per-quest rank/level gates.
	 */
	@Test
	void pvpKillProgressMirrorsTheRetailDistanceGate() {
		int questId = firstRoutedPvpWithGate();
		Row row = table.find(questId).orElseThrow();
		Step step = row.steps().getFirst();
		Player killer = player(32, Set.of(questId));
		Player victim = player(33, Set.of());
		place(killer, 0f, 0f, 0f);
		place(victim, 51f, 0f, 0f);
		killer.getQuestStateList().getQuestState(questId).getQuestVars().setVar(step.index());
		AbyssRankEnum rank = rankWithin(optionalInt(step.column(1)), optionalInt(step.column(2)));
		setLevel(victim, 60);
		setLevel(killer, 60);
		assertFalse(runtime.onKillRanked(killer, victim, rank), "50m 外 PvP 进度必须零动作");
		assertEquals(step.index(), DataDrivenProgress
			.step(killer.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars()),
			"距离门外不得写 vars");
		// 界内回到既有闸门链（同图同闸门参数 ⇒ 推进）。 / Inside, the existing gate chain resumes.
		place(victim, 49f, 0f, 0f);
		assertTrue(runtime.onKillRanked(killer, victim, rank), "50m 内必须推进");
	}

	/**
	 * ⑧b col9 EnterInstance 落点面（偏差修复第九批）：落点表 = 真端 world.xml location_alias_list
	 * 原坐标；10034（creation 13）/20034（creation 3）解冻路由，20032（creation 2）因别名真端
	 * 内在缺失维持 fail-closed 冻结。
	 * The Enter Instance landing face (batch 9): 10034/20034 unfrozen and routed; 20032 stays
	 * fail-closed frozen (its landing alias is intrinsically absent in the retail world file).
	 */
	@Test
	void instanceEntryFaceFollowsTheRetailLandingTable() {
		NativeInstanceEntryPort port = NativeInstanceEntryPort.instance();
		assertEquals(3, port.all().size(), "表 = DD 引用的三个 creation（2/3/13）");
		NativeInstanceEntryPort.EntryPoint up = port.entry(3).orElseThrow();
		assertTrue(up.resolved(), "creation 3 别名在真端 IDTemple_Up world.xml 有坐标");
		assertEquals(300150000, up.worldId(), "creation 3 worldId = IDTemple_Up（与 20034 载荷一致）");
		assertEquals(562.176941f, up.x(), 1e-3f, "落点 x = 真端 location_alias 原值");
		assertEquals(223.046707f, up.y(), 1e-3f, "落点 y = 真端 location_alias 原值");
		assertEquals(137.100006f, up.z(), 1e-3f, "落点 z = 真端 location_alias 原值");
		assertEquals(270, up.heading(), "dir 270 = 真端原值（度）");
		NativeInstanceEntryPort.EntryPoint low = port.entry(13).orElseThrow();
		assertTrue(low.resolved(), "creation 13 别名在真端 IDTemple_Low world.xml 有坐标");
		assertEquals(300160000, low.worldId(), "creation 13 worldId = IDTemple_Low（与 10034 载荷一致）");
		assertEquals(794.989990f, low.x(), 1e-3f, "落点 x = 真端 location_alias 原值");
		assertEquals(213, low.heading(), "dir 213 = 真端原值（度）");
		assertFalse(port.entry(2).orElseThrow().resolved(), "creation 2 = IDElim_Entrance_alias 真端内在缺失");
		// 路由面：解冻两行 + 冻结一行且原因闭合。 / Routing: two unfrozen, one frozen with the closed reason.
		assertTrue(runtime.routes(10034), "10034（creation 13）已随第九批落面解冻");
		assertTrue(runtime.routes(20034), "20034（creation 3）已随第九批落面解冻");
		assertFalse(runtime.routes(20032), "20032（creation 2）维持 fail-closed 冻结");
		assertEquals(FreezeReason.ACTION_UNFACED, runtime.frozenQuestIds().get(20032),
			"冻结原因 = 落点别名真端内在缺失");
	}

	/** ⑧ 冻结面：冻结行不得进任何兴趣面，原因闭合（e2 后 = ZONE_ABSENT + ACTION_UNFACED）。 */
	@Test
	void frozenRowsAreNeverRouted() {
		assertFalse(runtime.frozenQuestIds().isEmpty(), "步 e2 后仍须有冻结行");
		for (Map.Entry<Integer, FreezeReason> entry : runtime.frozenQuestIds().entrySet()) {
			int questId = entry.getKey();
			assertFalse(runtime.routes(questId), "冻结行不得路由");
			assertFalse(runtime.pvpSteps().containsKey(questId), "冻结行不得进 PvP 行集");
			assertFalse(runtime.acquireTalkInterests().values().stream().anyMatch(q -> q.contains(questId)),
				"冻结行不得进接取对话兴趣：" + questId);
			assertFalse(runtime.acquireItemInterests().values().stream().anyMatch(q -> q.contains(questId)),
				"冻结行不得进接取物品兴趣：" + questId);
			assertFalse(runtime.acquireWorldInterests().values().stream().anyMatch(q -> q.contains(questId)),
				"冻结行不得进接取进世界兴趣：" + questId);
			assertFalse(runtime.acquireLevelInterests().values().stream().anyMatch(q -> q.contains(questId)),
				"冻结行不得进接取等级兴趣：" + questId);
		}
	}


	/**
	 * ⑨ 切换集分桶与逐类步数冻结（真端契约面）：`P7 步 1` 的逐类步数、以及本批
	 * 「可路由 ∪ 冻结」的精确分桶。 / Frozen switch-set split and per-kind step counts.
	 */
	@Test
	void switchSetSplitAndPerKindCountsAreFrozen() {
		Map<Kind, Integer> switchSteps = new TreeMap<>();
		Map<Kind, Integer> routedSteps = new TreeMap<>();
		for (int questId : switchSet) {
			Row row = table.find(questId).orElseThrow();
			for (Step step : row.steps()) {
				switchSteps.merge(step.kind(), 1, Integer::sum);
				if (runtime.routes(questId)) {
					routedSteps.merge(step.kind(), 1, Integer::sum);
				}
			}
		}
		// P7 步 1 冻结的逐类步数（本批独立复算必须逐值一致）。
		assertEquals(Map.of(Kind.HUNT, 827, Kind.COLLECT_ITEM, 348, Kind.PVP, 207, Kind.TALK, 403,
			Kind.ENTER_AREA, 153, Kind.ITEM_PLAY, 42, Kind.ENTER_WORLD, 34, Kind.TALK_FOBJ, 19), switchSteps,
			"切换集逐类步数 = P7 步 1 契约");
		// 已路由步数；2026-10-03 偏差修复第九批 col9 EnterInstance 落面后随 10034/20034 解冻重冻
		// （离线镜像逐值一致，见 `.agents/summary/quest-engine-native/p7/tools/dd-planrow-e2-mirror.py`）。
		assertEquals(Map.of(Kind.HUNT, 820, Kind.COLLECT_ITEM, 345, Kind.PVP, 207, Kind.TALK, 395,
			Kind.ENTER_AREA, 137, Kind.ITEM_PLAY, 40, Kind.ENTER_WORLD, 32, Kind.TALK_FOBJ, 18), routedSteps,
			"已路由行的逐类步数冻结（col9 EnterInstance 落面后）");
		assertEquals(1457, runtime.routedQuestIds().size(), "可路由行冻结（EnterInstance 落面：1457 = 1467 − 10）");
		assertEquals(10, runtime.frozenQuestIds().size(), "冻结行冻结（两桶，见离线镜像）");
		Map<FreezeReason, Integer> byReason = new TreeMap<>();
		for (FreezeReason reason : runtime.frozenQuestIds().values()) {
			byReason.merge(reason, 1, Integer::sum);
		}
		assertEquals(Map.of(FreezeReason.ZONE_ABSENT, 9, FreezeReason.ACTION_UNFACED, 1), byReason,
			"冻结原因分桶冻结（ZONE_ABSENT 9 §10.3-#23 + ACTION_UNFACED 1 = 20032〔creation 2 的落点"
				+ "别名在真端 idelim/world.xml 本就缺失 = 内在缺失；10034/20034 已随第九批落面解冻〕）");
		// 步 e2 后唯一未解析面 = 切换集行引用的 LF6 真端缺席进区别名（原文大小写，§10.3-#23）；
		// 挑战哨兵 `_challengetask_` 已按 P0c-58 四源裁定落面（接取 NPC = reward_npc_name），
		// MESSAGE 字符串键经 retail-quest-string-ids.xml 全部解析。
		// 接取直方图（去重任务数，离线镜像权威值）：talk 1133（含哨兵 6）/ itemplay 13 /
		// enterworld 12 / leveluplogin 15 / enterarea 20（步 f kind-6 落面）/ none 264。
		assertEquals(1133, runtime.acquireTalkInterests().values().stream().flatMap(List::stream).distinct().count(),
			"接取 talk 去重任务数（EnterInstance 落面后不变）");
		assertEquals(13, runtime.acquireItemInterests().values().stream().flatMap(List::stream).distinct().count(),
			"接取 itemplay 去重任务数");
		assertEquals(12, runtime.acquireWorldInterests().values().stream().flatMap(List::stream).distinct().count(),
			"接取 enterworld 去重任务数");
		assertEquals(15, runtime.acquireLevelInterests().values().stream().flatMap(List::stream).distinct().count(),
			"接取 leveluplogin 去重任务数");
		assertEquals(20, runtime.acquireZoneInterests().values().stream().flatMap(List::stream).distinct().count(),
			"接取 enterarea 去重任务数（步 f kind-6）");
		assertEquals(Set.of("LF6_SensoryArea_Q15551_AtoB", "LF6_SensoryArea_Q15552_AtoD",
			"LF6_SensoryArea_Q15553_AtoF", "LF6_SensoryArea_Q15554_AtoH", "LF6_SensoryArea_Q15601a_Dynamic_Env",
			"LF6_SensoryArea_Q15602a_Dynamic_Env", "LF6_SensoryArea_Q15604a_Dynamic_Env",
			"LF6_SensoryArea_Q15605a_Named", "LF6_SensoryArea_Q15608a_Dynamic_Env"), runtime.unresolvedNames(),
			"未解析名字集 = 真端缺席进区别名（NativeEnterAreaPort.RETAIL_ABSENT_ALIASES 的切换集子集，§10.3-#23）");
	}

	/**
	 * ⑩a 接取入口的资格预检（P7-REPORT：清单与接取面同用 {@code CanAcquireQuest}，「不会出现能接但
	 * 不在清单」）。80789 前置 {@code Q80787}：未完成时 31 不得进接取面（2026-10-03 真机 80790 案例：
	 * 进了 4762 点 20000 却被静默拒），完成后 31 正常发接取入口页。
	 * <p>
	 * The acquire-entry eligibility precheck shares {@code CanAcquireQuest} with the nearby list:
	 * a player missing quest 80789's prerequisite stays out of the acquire face entirely.
	 */
	@Test
	void acquireEntryPageIsEligibilityGatedLikeTheNearbyList() {
		int npcId = 833671;
		Player without = NativeTalkFixture.player();
		assertFalse(runtime.onDialog(without, npcId, 31, 1, 80789), "前置 Q80787 未完成不得进接取面");
		assertTrue(NativeTalkFixture.dialogPages(without).isEmpty(), "资格不满足不下发任何页");

		Player with = NativeTalkFixture.player();
		NativeTalkFixture.completePrerequisites(with, 80787);
		assertTrue(runtime.onDialog(with, npcId, 31, 1, 80789), "前置完成后 31 发接取入口页");
		NativeTalkFixture.assertOnlyDialogPage(with, 4762);
	}

	/**
	 * ⑩b 交付报告面（零步 Talk 行 = DD_TALK_SIMPLE；真端所有行恒注册交付对象 #2 {@code reward_npc_name}，
	 * 槽 +0x238）：START 31 → 报告推进 REWARD + 页 5；REWARD 31 → 页 5；{@code 23} = NOREWARD
	 * 无选择确认 → 结算 + 领奖收尾回选择对话页 10（真端 npc-complete finish=SELECTION_DIALOG；9/28
	 * 旧引擎基线「状态=5 → 页=10」）。2026-10-03 回归补面：P7 步 f 切换批（715a00136）删旧后
	 * 已接玩家在交付 NPC 上 31 零响应（quests.log 9/28 80790 基线：31 → REWARD+页5 → 23 → COMPLETE）。
	 * <p>
	 * The delivery-report face for zero-step Talk rows: 31 reports to REWARD + page 5 and the
	 * no-selection confirm settles through the claim flow.
	 */
	@Test
	void zeroStepTalkRowsReportAndClaimOnTheRewardNpc() {
		// 80787：value0_acquire_ == reward_npc_name（event_Mongsil_Newbie），无 progress 步。
		int npcId = 0;
		for (Map.Entry<Integer, List<Integer>> entry : runtime.acquireTalkInterests().entrySet()) {
			if (entry.getValue().contains(80787)) {
				npcId = entry.getKey();
				break;
			}
		}
		assertTrue(npcId > 0, "80787 的接取/交付 NPC 必须可解析");

		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, 80787);

		// 两步报告（裁定 a）：31 只发报告确认页（80787 契约声明 10002=select_success）不推进；
		// 1009（10002 型由客户端自动回发）才推进 REWARD + 页 5。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 31, 1, 80787), "START 31 必须发报告确认页");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(80787).getStatus(),
			"31 不推进状态（两步第一步）");
		assertEquals(List.of(10002), NativeTalkFixture.dialogPages(player), "报告确认页 = 契约声明 10002");

		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 1009, 1, 80787), "START 1009 必须报告推进 REWARD");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(80787).getStatus(),
			"1009 报告确认 = 真端交付对象推进");
		assertEquals(List.of(5), NativeTalkFixture.dialogPages(player), "推进后发奖励窗页 5");

		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 31, 1, 80787), "REWARD 31 必须再发奖励窗");
		assertEquals(List.of(5), NativeTalkFixture.dialogPages(player));

		claims.calls = 0;
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 23, 1, 80787), "23 = NOREWARD 完成确认必须结算");
		assertEquals(1, claims.calls, "领奖口必须被调用一次");
		assertEquals(List.of(10), NativeTalkFixture.dialogPages(player),
			"结算后回选择对话页 10（npc-complete finish=SELECTION_DIALOG）");
	}

	/**
	 * ⑩ Talk 接取对话面（真端 `FUN_180c47220` 词汇，只服务无状态玩家）：打开 → 4762；1002 → 接取 +
	 * 1003；1003 → 1004；20000/20001 收尾 → 关窗页 0（旧 XML close-dialog）；1008/其余 ≥1000 原样回发；
	 * &lt;1000 零动作；1007 → 客户端契约问询窗（fail-closed）；已接取玩家不得再见接取入口页。
	 * The Talk acquire dialog face (retail FUN_180c47220 vocabulary), served only to stateless players.
	 */
	@Test
	void acquireTalkDialogFaceFollowsTheRetailVocabulary() {
		int npcId = -1;
		int questId = -1;
		boolean found = false;
		outer:
		for (Map.Entry<Integer, List<Integer>> entry : runtime.acquireTalkInterests().entrySet()) {
			for (int candidate : entry.getValue()) {
				Player probe = NativeTalkFixture.player();
				// 31 入口带 CanAcquireQuest 同源预检（清单与接取面同一判定函数，P7-REPORT）：等级/前置/
				// 种族/职业轴不满足的行对本探针（20 级天族战士）不进接取面——跳过，找放行的行走词汇。
				// The 31 entry shares CanAcquireQuest with the nearby list; rows ineligible for this
				// 20-level Elyos probe skip the acquire face — probe on for an eligible row.
				if (!runtime.onDialog(probe, entry.getKey(), 31, 1, 0)) {
					continue;
				}
				NativeTalkFixture.assertOnlyDialogPage(probe, 4762);
				if (runtime.onDialog(probe, entry.getKey(), 1002, 1, 0)
					&& probe.getQuestStateList().getQuestState(candidate) != null) {
					npcId = entry.getKey();
					questId = candidate;
					found = true;
					break outer;
				}
			}
		}
		assertTrue(found, "须有可接取的 Talk 行（NativeQuestStartPort 条件面放行）");
		// 1002 → 接取 + 1003。
		Player player = NativeTalkFixture.player();
		assertTrue(runtime.onDialog(player, npcId, 1002, 1, 0), "探针已证明该任务可接取");
		NativeTalkFixture.assertOnlyDialogPage(player, 1003);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus(),
			"1002 = 真端 SetQuestAcquired");
		// 已接取玩家：接取面不再服务该任务（带任务上下文时不得再见 4762；同 NPC 其他任务仍可接）。
		NativeTalkFixture.clearPackets(player);
		runtime.onDialog(player, npcId, 31, 1, questId);
		assertFalse(NativeTalkFixture.dialogPages(player).contains(4762), "已接取玩家不得再见接取入口页");
		// 1003 → 1004；20001（QUEST_REFUSE_SIMPLE 拒绝）→ 关窗页 0（旧 XML close-dialog）；
		// 1008 回发；1012 回发；999 零动作。
		Player other = NativeTalkFixture.player();
		assertTrue(runtime.onDialog(other, npcId, 1003, 1, 0), "1003 必须发拒绝确认页");
		NativeTalkFixture.assertOnlyDialogPage(other, 1004);
		NativeTalkFixture.clearPackets(other);
		assertTrue(runtime.onDialog(other, npcId, 20001, 1, 0), "20001 = 拒绝收尾 = 关窗");
		NativeTalkFixture.assertOnlyDialogPage(other, 0);
		NativeTalkFixture.clearPackets(other);
		assertTrue(runtime.onDialog(other, npcId, 1008, 1, 0), "1008 原样回发");
		NativeTalkFixture.assertOnlyDialogPage(other, 1008);
		NativeTalkFixture.clearPackets(other);
		assertTrue(runtime.onDialog(other, npcId, 1012, 1, 0), "其余 ≥1000 动作原样回发");
		NativeTalkFixture.assertOnlyDialogPage(other, 1012);
		NativeTalkFixture.clearPackets(other);
		assertFalse(runtime.onDialog(other, npcId, 999, 1, 0), "<1000 非接取词汇零动作");
		assertTrue(NativeTalkFixture.dialogPages(other).isEmpty(), "<1000 不发页");
		// 1007 → 客户端契约问询窗（无契约页 = fail-closed 拒绝）。
		Player asker = NativeTalkFixture.player();
		int askPage = QuestDialogContract.loadDefault().askWindowPage(questId);
		assertEquals(askPage >= 0, runtime.onDialog(asker, npcId, 1007, 1, 0), "1007 按客户端契约裁定");
		if (askPage >= 0) {
			NativeTalkFixture.assertOnlyDialogPage(asker, askPage);
		}
	}

	/**
	 * ⑪ 双角色接取（真端 progress handler 的 state!=3 分支，接取即执行步 0 动作）：ItemPlay（事件 5）/
	 * EnterWorld（事件 0x12）/ LevelUp·LevelUpLogIn（`ctx+8 == def+8` 等级等值，登录遍历只服务 kind 10）。
	 * Dual-role acquires fire on their retail events (item acquire / enter world / exact level).
	 */
	@Test
	void dualRoleAcquiresFireOnTheirRetailEvents() {
		// ItemPlay 接取（kind 3，物品 id 键控；该批接取行 minlvl 65/66 ⇒ 探测玩家取两族 66 级）。
		int[] itemFixture = firstAcquirable(runtime.acquireItemInterests(),
			(player, key, candidate) -> {
				for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
					Player probe = NativeTalkFixture.player(race, PlayerClass.WARRIOR, 66);
					if (runtime.onItemAcquired(probe, key)
						&& probe.getQuestStateList().getQuestState(candidate) != null) {
						return true;
					}
				}
				return false;
			});
		assertTrue(itemFixture[0] > 0, "须有可接取的 ItemPlay 行");
		// EnterWorld 接取（kind 7，worldId 键控；该批接取行同为 minlvl 66 ⇒ 双族 66 级探测）。
		int[] worldFixture = firstAcquirable(runtime.acquireWorldInterests(),
			(player, key, candidate) -> {
				for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
					Player probe = NativeTalkFixture.player(race, PlayerClass.WARRIOR, 66);
					if (runtime.onEnterWorld(probe, key)
						&& probe.getQuestStateList().getQuestState(candidate) != null) {
						return true;
					}
				}
				return false;
			});
		assertTrue(worldFixture[0] > 0, "须有可接取的 EnterWorld 行");
		// 等级键集冻结（镜像：30/40/45/50/55/66，切换集内全部 kind 10 LevelUpLogIn；66 级行随
		// 步 e2 con_quest 非闸门解冻入面）。
		assertEquals(Set.of(30, 40, 45, 50, 55, 66), runtime.acquireLevelInterests().keySet(),
			"接取等级键集 = 离线镜像");
		int[] levelFixture = firstAcquirable(runtime.acquireLevelInterests(),
			(player, key, candidate) -> {
				setLevel(player, key);
				return runtime.onLevelReached(player, key, true)
					&& player.getQuestStateList().getQuestState(candidate) != null;
			});
		assertTrue(levelFixture[0] > 0, "须有可接取的 LevelUpLogIn 行");
		int level = levelFixture[0];
		// 接取分支执行步 0 动作（真端 `FUN_180c46bb0`：+0xd8 成功 → cd50）：10500 接取即播 Movie 32。
		int level10500 = Integer.parseInt(table.find(10500).orElseThrow().acquireParam().trim());
		Player levelUp10500 = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, level10500);
		movies.clear();
		assertTrue(runtime.onLevelReached(levelUp10500, level10500, true), "10500 等级等值接取");
		assertTrue(levelUp10500.getQuestStateList().getQuestState(10500) != null, "10500 接取成档");
		assertTrue(movies.calls().contains("movie:32"), "接取分支执行步 0 动作：" + movies.calls());
		// 升级遍历服务 kind 8 与 10（切换集无 kind 8 行 ⇒ 登录/升级同集）。
		Player onLevelUp = player(21, Set.of());
		setLevel(onLevelUp, level);
		assertTrue(runtime.onLevelReached(onLevelUp, level, false), "升级遍历按等级等值接取");
		// 等级等值守卫（真端 `ctx+8 == def+8`，运行时以等级键索引承担）：差一级零接取。
		Player off = player(22, Set.of());
		setLevel(off, level + 1);
		assertFalse(runtime.onLevelReached(off, off.getLevel(), true), "等级等值守卫（登录遍历）");
		assertFalse(runtime.onLevelReached(off, off.getLevel(), false), "等级等值守卫（升级遍历）");
	}

	/**
	 * ⑪-b kind-6 进区接取面（步 f，真端区 handler `FUN_180c47bf0` 尾段独立接取侧名字哈希树）：
	 * 20 行 / 15 别名入面；无状态玩家进同名区 → `(+0xd8)` 接取 + 步 0 动作；已接取/其他区不触发；
	 * 真端无区定义的 `DF6_QuestArea_Q25674` 登记原文 = 永不命中死边（镜像真端）。
	 * The kind-6 zone acquire face (step f): entering the same-named zone acquires and runs step-0
	 * actions; the retail-absent alias stays a registered dead edge, mirroring retail.
	 */
	@Test
	void zoneAcquireFaceFiresOnTheSameNamedZone() {
		// 兴趣面形状：20 行 / 15 别名（真端可解析 14 + 真端缺席 1）。
		long questCount = runtime.acquireZoneInterests().values().stream().flatMap(List::stream).distinct().count();
		assertEquals(20, questCount, "kind-6 接取行数 = 离线镜像");
		assertEquals(15, runtime.acquireZoneInterests().size(), "kind-6 别名数 = 15（含真端缺席别名）");
		assertTrue(runtime.acquireZoneInterests().containsKey("DF6_QUESTAREA_Q25674"),
			"真端无区定义的别名仍登记原文（死边镜像）");
		// 找一个可接取的进区行：无状态玩家进同名区 → 接取成档。
		String zoneKey = null;
		int questId = -1;
		Player acquired = null;
		outer:
		for (Map.Entry<String, List<Integer>> entry : runtime.acquireZoneInterests().entrySet()) {
			for (int candidate : entry.getValue()) {
				for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
					for (int level : new int[] {10, 20, 30, 40, 50, 60, 66}) {
						Player probe = NativeTalkFixture.player(race, PlayerClass.WARRIOR, level);
						if (runtime.onEnterZone(probe, entry.getKey().toLowerCase(java.util.Locale.ROOT))
							&& probe.getQuestStateList().getQuestState(candidate) != null) {
							zoneKey = entry.getKey();
							questId = candidate;
							acquired = probe;
							break outer;
						}
					}
				}
			}
		}
		assertNotNull(zoneKey, "须有可接取的进区行（NativeQuestStartPort 条件面放行）");
		assertEquals(QuestStatus.START, acquired.getQuestStateList().getQuestState(questId).getStatus(),
			"进区接取 = 真端 SetQuestAcquired");
		// 已接取玩家再进区：不重复接取（state 分支只服务无状态玩家）。
		assertFalse(runtime.onEnterZone(acquired, zoneKey.toLowerCase(java.util.Locale.ROOT)),
			"已接取玩家进区不再触发接取分支");
		// 无关区名零动作。
		Player stranger = NativeTalkFixture.player();
		assertFalse(runtime.onEnterZone(stranger, "NOT_A_REGISTERED_ZONE"), "未登记区名不触发任何面");
	}

	/**
	 * ⑩-b Talk 对话接取收尾 = `FUN_180c4d5b0(-1,-1)` ⇒ 步 0 动作（步 f 修正；e1 曾误判零动作）：
	 * 找一个步 0 带已落面动作的 talk 接取行，1002 接取即执行步 0 动作。
	 * The Talk dialog accept tail funnels into FUN_180c4d5b0(-1,-1) and runs step-0 actions.
	 */
	@Test
	void talkDialogAcceptRunsStepZeroActions() {
		int[] fixture = firstAcquireTalkStepWithFacedActions();
		assertTrue(fixture[0] > 0, "须有步 0 带已落面动作的 talk 接取行");
		int questId = fixture[0];
		Player player = NativeTalkFixture.player();
		inventory.clear();
		movies.clear();
		spawns.clear();
		says.clear();
		teleports.clear();
		int npcId = -1;
		for (Map.Entry<Integer, List<Integer>> entry : runtime.acquireTalkInterests().entrySet()) {
			if (entry.getValue().contains(questId)) {
				npcId = entry.getKey();
				break;
			}
		}
		assertTrue(npcId > 0, "接取行必须在 talk 接取兴趣面上");
		assertTrue(runtime.onDialog(player, npcId, 1002, 1, 0), "1002 接取");
		assertTrue(player.getQuestStateList().getQuestState(questId) != null, "接取成档");
		assertFalse(inventory.calls().isEmpty() && movies.calls().isEmpty() && spawns.calls().isEmpty()
			&& says.calls().isEmpty() && teleports.calls().isEmpty(), "接取收尾执行步 0 动作");
		Step first = table.find(questId).orElseThrow().steps().getFirst();
		List<String> expected = new ArrayList<>();
		for (int column : new int[] {1, 2}) {
			String text = first.column(column);
			if (text == null || text.isBlank()) {
				continue;
			}
			String[] tokens = text.trim().split("[,\\s]+");
			for (int index = 0; index < tokens.length; index += 2) {
				expected.add((column == 1 ? "give:" : "remove:") + itemIndex.resolve(tokens[index]) + ":"
					+ Integer.parseInt(tokens[index + 1]));
			}
		}
		assertEquals(expected, inventory.calls(), "步 0 发/扣按列序执行");
	}

	/** 首个「talk 接取 + 步 0 带发/扣物品列」的行（{questId}，无则 {-1}）。 */
	private int[] firstAcquireTalkStepWithFacedActions() {
		java.util.Set<Integer> candidates = new TreeSet<>();
		for (List<Integer> quests : runtime.acquireTalkInterests().values()) {
			candidates.addAll(quests);
		}
		for (int questId : candidates) {
			Row row = table.find(questId).orElseThrow();
			if (!row.steps().isEmpty()) {
				Step first = row.steps().getFirst();
				String col1 = first.column(1);
				if (col1 != null && !col1.isBlank()) {
					return new int[] {questId};
				}
			}
		}
		return new int[] {-1};
	}

	/** 接取面探测回调：key 触发 + candidate 任务成档才认。 / Probe: key fires and the quest commits. */
	private interface AcquireProbe {
		boolean fire(Player player, int key, int candidate);
	}

	/** 在接取兴趣面里找首个「key 触发 + 任务成档」的组合（fail-closed 探测）。 */
	private static int[] firstAcquirable(Map<Integer, List<Integer>> interests, AcquireProbe probe) {
		for (Map.Entry<Integer, List<Integer>> entry : interests.entrySet()) {
			for (int candidate : entry.getValue()) {
				Player player = NativeTalkFixture.player();
				if (probe.fire(player, entry.getKey(), candidate)) {
					return new int[] {entry.getKey(), candidate};
				}
			}
		}
		return new int[] {-1, -1};
	}

	/**
	 * ⑫ 发/扣物品动作面（真端执行器 case 1/2）× 执行矩阵（步 f 逐 handler 修正）：推进边对 Hunt/
	 * EnterArea/TalkFOBJ/**Talk** 调执行器（对话平面推进/完成汇入 `FUN_180c4d5b0(…,-1)` → C:2075066
	 * 直调 c8d0，门 = 完成步 kind==4）；CollectItem 推进零动作（拾取分支无执行器、门只放行 kind 4）。
	 * The give/remove action face follows the corrected execution matrix: Talk advances DO run the
	 * step's faced actions (the dialog plane funnels into FUN_180c4d5b0(-1) → c8d0, gated on step
	 * kind 4); CollectItem advances run nothing.
	 */
	@Test
	void facedGiveRemoveActionsFollowTheRetailExecutionMatrix() {
		// Talk 步推进 = 对话平面顺序页动作（10000+当前步）⇒ d5b0(-1) → c8d0(完成步动作) 按列序执行。
		int[] talkFixture = firstRoutedStepWithExtra(Kind.TALK, 1);
		assertAdvanceItemCalls(runtime.talkInterests(), talkFixture, 10000 + talkFixture[1]);
		// TalkFOBJ 步推进 = 交互事件（对话框 31）⇒ c8d0 执行器按列序执行。
		assertAdvanceItemCalls(runtime.fobjInterests(), firstRoutedStepWithExtra(Kind.TALK_FOBJ, 1), 31);
		// CollectItem 步推进 = 拾取分支无执行器 ⇒ 步列零执行（即使带动作列也不跑）。
		int[] collectStep = firstRoutedStepWithExtra(Kind.COLLECT_ITEM, 1);
		int collectNpc = keyOf(runtime.talkInterests(), collectStep[0], collectStep[1]);
		Player collectPlayer = NativeTalkFixture.player();
		NativeTalkFixture.add(collectPlayer, collectStep[0], QuestStatus.START, collectStep[1]);
		inventory.clear();
		movies.clear();
		spawns.clear();
		says.clear();
		teleports.clear();
		assertTrue(runtime.onDialog(collectPlayer, collectNpc, 10000 + collectStep[1], 1, collectStep[0]),
			"CollectItem 顺序动作步进");
		assertTrue(inventory.calls().isEmpty(), "CollectItem 推进边零动作（真端拾取分支无执行器）");
		assertTrue(movies.calls().isEmpty() && spawns.calls().isEmpty() && says.calls().isEmpty()
			&& teleports.calls().isEmpty(), "CollectItem 推进边零渲染");
	}

	/** 从表列原文复算期望调用并断言一次推进的物品通道轨迹（kind 4/9 = 完成步动作）。 */
	private void assertAdvanceItemCalls(Map<Integer, List<DataDrivenNativeRuntime.StepHit>> interests,
			int[] fixture, int dialogId) {
		assertAdvanceItemCalls(interests, fixture[0], fixture[1], dialogId);
	}

	private void assertAdvanceItemCalls(Map<Integer, List<DataDrivenNativeRuntime.StepHit>> interests, int questId,
			int stepIndex, int dialogId) {
		Step step = table.find(questId).orElseThrow().steps().get(stepIndex);
		List<String> expected = new ArrayList<>();
		for (int column : new int[] {1, 2}) {
			String text = step.column(column);
			if (text == null || text.isBlank()) {
				continue;
			}
			String[] tokens = text.trim().split("[,\\s]+");
			for (int index = 0; index < tokens.length; index += 2) {
				expected.add((column == 1 ? "give:" : "remove:") + itemIndex.resolve(tokens[index]) + ":"
					+ Integer.parseInt(tokens[index + 1]));
			}
		}
		assertFalse(expected.isEmpty(), "夹具步必须带发/扣物品列：" + questId + "#" + stepIndex);
		int npcId = keyOf(interests, questId, stepIndex);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		inventory.clear();
		assertTrue(runtime.onDialog(player, npcId, dialogId, 1, questId), "推进事件");
		assertEquals(expected, inventory.calls(), "推进分支按列序执行已落面发/扣动作");
	}

	/**
	 * ⑬ 过场/刷怪动作面（真端执行器 case 4/5，EnterArea 推进边）：`Cutscene|Cutscene2` 走 CUTSCENE
	 * 资源型、`Movie|Movie2` 走电影型（+0x1b8 独立槽）；Spawn Relative = 玩家随机偏移、Absolute = 精确坐标。
	 * Cutscene and spawn actions run on the EnterArea advance branch.
	 */
	@Test
	void cutsceneAndSpawnActionsRunOnTheEnterAreaAdvanceBranch() {
		int[] fixture = firstRoutedStepWithExtra(Kind.ENTER_AREA, 4);
		int questId = fixture[0];
		int stepIndex = fixture[1];
		Step step = table.find(questId).orElseThrow().steps().get(stepIndex);
		String[] tokens = step.column(4).trim().split("[,\\s]+");
		int movieId = Integer.parseInt(tokens[1]);
		String expectedMovie = (tokens[0].equalsIgnoreCase("Movie") || tokens[0].equalsIgnoreCase("Movie2")
			? "movie:" : "play:") + movieId;
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		String zone = enterAreaPort.zoneName(questId, stepIndex).orElseThrow();
		movies.clear();
		spawns.clear();
		assertTrue(runtime.onEnterZone(player, zone), "进区直接步进");
		assertTrue(movies.calls().contains(expectedMovie), "收口/步进分支播放已声明过场，实际：" + movies.calls());
		if (step.column(5) != null) {
			// Spawn 名单 = NPC 名（相对/绝对形），断言每只按声明 npcId/count 刷出。
			for (String group : step.column(5).trim().split(";")) {
				String[] parts = group.trim().split("[,\\s]+");
				if (parts.length < 3 || !(parts[0].equalsIgnoreCase("Relative")
					|| parts[0].equalsIgnoreCase("Absolute"))) {
					continue;
				}
				int npcId = NativeNpcNameResolver.instance().resolveMonsterIds(parts[1]).getFirst();
				boolean relative = parts[0].equalsIgnoreCase("Relative");
				int count = Integer.parseInt(parts[2]);
				int life = Integer.parseInt(parts[3]);
				assertTrue(spawns.calls().contains("spawn:" + npcId + ":" + count + ":" + (relative ? "rel" : "abs")
					+ ":" + life), "推进分支按声明刷怪：" + spawns.calls());
			}
		}
	}

	/** 记录式假过场端口。 / A recording fake movie port. */
	private static final class RecordingMovies implements NativeMoviePort {

		private final List<String> calls = new ArrayList<>();

		/** 清零调用记录。 / Clears the call log. */
		void clear() {
			calls.clear();
		}

		/** 调用记录（{@code play:id} / {@code movie:id}）。 / The call log. */
		List<String> calls() {
			return List.copyOf(calls);
		}

		@Override
		public void play(Player player, int movieId) {
			calls.add("play:" + movieId);
		}

		@Override
		public void playMovie(Player player, int movieId) {
			calls.add("movie:" + movieId);
		}
	}

	/** 记录式假传送端口。 / A recording fake teleport port. */
	private static final class RecordingTeleports implements NativeTeleportPort {

		private final List<String> calls = new ArrayList<>();

		/** 清零调用记录。 / Clears the call log. */
		void clear() {
			calls.clear();
		}

		/** 调用记录。 / The call log. */
		List<String> calls() {
			return List.copyOf(calls);
		}

		@Override
		public void teleport(Player player, int worldId, float x, float y, float z, int headingDegrees) {
			calls.add("teleport:" + worldId);
		}
	}

	/** 记录式假刷怪端口。 / A recording fake spawn port. */
	private static final class RecordingSpawns implements NativeSpawnPort {

		private final List<String> calls = new ArrayList<>();

		/** 清零调用记录。 / Clears the call log. */
		void clear() {
			calls.clear();
		}

		/** 调用记录（{@code spawn:npcId:count:rel|abs:life}）。 / The call log. */
		List<String> calls() {
			return List.copyOf(calls);
		}

		@Override
		public void spawn(Player player, int npcId, int count, boolean relative, float x, float y, float z,
				int headingDegrees, int lifeSeconds) {
			calls.add("spawn:" + npcId + ":" + count + ":" + (relative ? "rel" : "abs") + ":" + lifeSeconds);
		}
	}

	/** 记录式假播报端口。 / A recording fake say port. */
	private static final class RecordingSays implements NativeSayPort {

		private final List<String> calls = new ArrayList<>();

		/** 清零调用记录。 / Clears the call log. */
		void clear() {
			calls.clear();
		}

		/** 调用记录（{@code say:id}）。 / The call log. */
		List<String> calls() {
			return List.copyOf(calls);
		}

		@Override
		public void say(Player player, int stringId) {
			calls.add("say:" + stringId);
		}
	}

	/** 记录式假计时端口。 / A recording fake timer port. */
	/**
	 * ①c 任务计时面（col10 落面，2026-10-02）：到期判定镜像真端 `FUN_180c46d80`——进行中 ∧
	 * `0 < 当前步 < 目标步` 才动作（旗标 0 直写步号 / 旗标 1 弃任），范围外与非进行中零操作；
	 * col9（EnterInstance）维持冻结（立即执行面 EVIDENCE_MISSING）。
	 * ①c Quest-timer face: the expiry verdict mirrors FUN_180c46d80 — in-progress ∧
	 * `0 < cur < dest` only (flag 0 writes the step, flag 1 abandons); out-of-range and
	 * non-active states are no-ops; col9 stays frozen (entry face EVIDENCE_MISSING).
	 */
	@Test
	void questTimerExpiryMirrorsTheRetailRangeCheck() {
		// 13945 = 路由 Timer 行 `2700, 2, 0`（目标步 2，推进旗标）。
		// 13945 = a routed timer row `2700, 2, 0` (dest step 2, advance flag).
		assertTrue(runtime.routedQuestIds().contains(13945), "13945 应已解冻（col10 Timer 落面）");
		assertFalse(runtime.routedQuestIds().contains(20032), "20032 应维持冻结（col9 立即面 EVIDENCE_MISSING）");
		// 范围内：cur=1 → 直写到目标步 2。
		Player inRange = new ObjenesisStd().newInstance(Player.class);
		inRange.setQuestStateList(new QuestStateList());
		inRange.getQuestStateList().addQuest(13945, new QuestState(13945, QuestStatus.START, 1, 0, null, 0, null));
		runtime.onQuestTimerExpired(inRange, 13945, 2, false);
		assertEquals(2, inRange.getQuestStateList().getQuestState(13945).getQuestVars().getQuestVars(),
			"到期推进应直写步号到目标步（+0xf0 面）");
		// 已过目标步 / 尚在第 0 步：零操作。
		Player pastDest = new ObjenesisStd().newInstance(Player.class);
		pastDest.setQuestStateList(new QuestStateList());
		pastDest.getQuestStateList().addQuest(13945, new QuestState(13945, QuestStatus.START, 3, 0, null, 0, null));
		runtime.onQuestTimerExpired(pastDest, 13945, 2, false);
		assertEquals(3, pastDest.getQuestStateList().getQuestState(13945).getQuestVars().getQuestVars(),
			"cur >= 目标步 ⇒ 零操作");
		Player atZero = new ObjenesisStd().newInstance(Player.class);
		atZero.setQuestStateList(new QuestStateList());
		atZero.getQuestStateList().addQuest(13945, new QuestState(13945, QuestStatus.START, 0, 0, null, 0, null));
		runtime.onQuestTimerExpired(atZero, 13945, 2, false);
		assertEquals(0, atZero.getQuestStateList().getQuestState(13945).getQuestVars().getQuestVars(),
			"cur = 0 ⇒ 零操作");
		// 非进行中（REWARD）与未路由任务：零操作。
		Player rewarded = new ObjenesisStd().newInstance(Player.class);
		rewarded.setQuestStateList(new QuestStateList());
		rewarded.getQuestStateList().addQuest(13945, new QuestState(13945, QuestStatus.REWARD, 1, 0, null, 0, null));
		runtime.onQuestTimerExpired(rewarded, 13945, 2, false);
		assertEquals(QuestStatus.REWARD, rewarded.getQuestStateList().getQuestState(13945).getStatus(),
			"非进行中 ⇒ 零操作");
		Player unrouted = new ObjenesisStd().newInstance(Player.class);
		unrouted.setQuestStateList(new QuestStateList());
		unrouted.getQuestStateList().addQuest(20032, new QuestState(20032, QuestStatus.START, 1, 0, null, 0, null));
		runtime.onQuestTimerExpired(unrouted, 20032, 7, false);
		assertEquals(1, unrouted.getQuestStateList().getQuestState(20032).getQuestVars().getQuestVars(),
			"未路由（col9 冻结）行 ⇒ 零操作");
	}

	/** 记录式假计时端口。 / A recording fake timer port. */
	private static final class RecordingTimers implements NativeTimerPort {

		/** 武装记录（{@code questId:秒:目标步:旗标}）。 / Armed timers ({@code questId:sec:dest:flag}). */
		private final List<String> calls = new ArrayList<>();

		/** 清零调用记录。 / Clears the call log. */
		void clear() {
			calls.clear();
		}

		/** 武装记录。 / The arm log. */
		List<String> calls() {
			return List.copyOf(calls);
		}

		@Override
		public void schedule(Player player, int questId, int seconds, int destStep, boolean abandonOnExpiry) {
			calls.add(questId + ":" + seconds + ":" + destStep + ":" + (abandonOnExpiry ? 1 : 0));
		}
	}

	// ------------------------------------------------------------------ 辅助

	private interface StepEvent {
		boolean fire(Player player, int questId, int stepIndex, Step step);
	}

	private static int firstRouted(Kind kind, int stepOffset) {
		for (int questId : new TreeSet<>(runtime.routedQuestIds())) {
			List<Step> steps = table.find(questId).orElseThrow().steps();
			if (steps.size() > stepOffset && steps.get(stepOffset).kind() == kind) {
				return questId;
			}
		}
		throw new IllegalStateException("no routed " + kind + " fixture");
	}

	/** 首个「末步 = Talk 且已路由」的行（报告通道夹具）。 / First routed row whose last step is Talk. */
	private static int firstRoutedTalkOnLastStep() {
		for (int questId : new TreeSet<>(runtime.routedQuestIds())) {
			List<Step> steps = table.find(questId).orElseThrow().steps();
			if (!steps.isEmpty() && steps.getLast().kind() == Kind.TALK
				&& runtime.talkInterests().values().stream().anyMatch(hits -> hits.stream().anyMatch(
					hit -> hit.questId() == questId && hit.stepIndex() == steps.getLast().index()))) {
				return questId;
			}
		}
		throw new IllegalStateException("no routed talk-on-last-step fixture");
	}

	private static int firstRoutedPvpWithGate() {
		for (int questId : new TreeSet<>(runtime.routedQuestIds())) {
			List<Step> steps = table.find(questId).orElseThrow().steps();
			if (!steps.isEmpty() && steps.getFirst().kind() == Kind.PVP) {
				return questId;
			}
		}
		throw new IllegalStateException("no routed pvp fixture");
	}

	/** 首个「已路由 + 指定 kind + 指定附加动作列非空」的步（{questId, stepIndex}）。 */
	private static int[] firstRoutedStepWithExtra(Kind kind, int column) {
		for (int questId : new TreeSet<>(runtime.routedQuestIds())) {
			for (Step step : table.find(questId).orElseThrow().steps()) {
				if (step.kind() != kind) {
					continue;
				}
				String text = step.column(column);
				if (text != null && !text.isBlank()) {
					return new int[] {questId, step.index()};
				}
			}
		}
		throw new IllegalStateException("no routed " + kind + " step with extra column " + column);
	}

	private static int optionalInt(String column) {
		return column == null || column.isBlank() ? 0 : Integer.parseInt(column.trim());
	}

	private static AbyssRankEnum rankWithin(int minRank, int maxRank) {
		for (AbyssRankEnum rank : AbyssRankEnum.values()) {
			if ((minRank == 0 || rank.getId() >= minRank) && (maxRank == 0 || rank.getId() <= maxRank)) {
				return rank;
			}
		}
		throw new IllegalStateException("no rank within [" + minRank + "," + maxRank + "]");
	}

	private static int keyOf(Map<Integer, List<DataDrivenNativeRuntime.StepHit>> interests, int questId,
			int stepIndex) {
		for (Map.Entry<Integer, List<DataDrivenNativeRuntime.StepHit>> entry : interests.entrySet()) {
			for (DataDrivenNativeRuntime.StepHit hit : entry.getValue()) {
				if (hit.questId() == questId && hit.stepIndex() == stepIndex) {
					return entry.getKey();
				}
			}
		}
		throw new IllegalStateException("no interest key for " + questId + "#" + stepIndex);
	}

	private static void setLevel(Player player, int level) {
		try {
			// 单测环境没有玩家经验表（`PlayerCommonData.setLevel` 依赖数据装载），直接写字段。
			java.lang.reflect.Field field = PlayerCommonData.class.getDeclaredField("level");
			field.setAccessible(true);
			field.set(player.getCommonData(), level);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** 反射放置坐标（距离门测试面；生产经 WorldPosition 正常初始化）。 / Reflective placement for the gate tests. */
	private static void place(Player player, float x, float y, float z) {
		WorldPosition position = new WorldPosition(110010000);
		position.setXYZH(x, y, z, (byte) 0);
		player.setPosition(position);
	}

	private static Player player(int seed, Set<Integer> questIds) {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(10001 + seed);
		commonData.setRace(Race.ELYOS);
		commonData.setGender(Gender.MALE);
		try {
			java.lang.reflect.Field field = Player.class.getDeclaredField("playerCommonData");
			field.setAccessible(true);
			field.set(player, commonData);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		QuestStateList states = new QuestStateList();
		for (int questId : questIds) {
			states.addQuest(questId, new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null));
		}
		player.setQuestStateList(states);
		setLevel(player, 60);
		return player;
	}

	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new LinkedHashMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			String questId = RetailLedgerRows.cell(row, "quest_id");
			String owner = RetailLedgerRows.cell(row, "owner");
			if (questId != null && questId.chars().allMatch(Character::isDigit) && owner != null) {
				owners.put(Integer.parseInt(questId), owner);
			}
		}
		return owners;
	}

	private static List<Path> zoneFiles() throws Exception {
		try (Stream<Path> files = Files.list(ZONES_DIR)) {
			return files.filter(path -> path.getFileName().toString().startsWith("zones_"))
				.filter(path -> path.getFileName().toString().endsWith(".xml")).sorted().toList();
		}
	}

	private static InputStream resource(String path) {
		InputStream input = DataDrivenNativeRuntimeGateTest.class.getResourceAsStream(path);
		assertNotNull(input, "missing resource " + path);
		return input;
	}
}
