package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
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
 * TELEPORT/SPAWN/MESSAGE/GIVE/REMOVE/CUTSCENE 动作面 × 原版执行矩阵）与**切换批**语义。
 * <p>
 * 冻结点：① 生产口径 = 切换集 1467 全量接管（步 f 起 routed 1444 / frozen 23，interests 非空）；
 * ② 兴趣面按 payload 逐类建立（Hunt 组名 → 怪物 id、Talk/CollectItem → NPC、TalkFOBJ → NPC、
 * ItemPlay → 物品 id、EnterArea → 同名注册区、EnterWorld → worldId、PvP → 行步）+ 接取兴趣面
 * （Talk 对话 NPC〔含挑战哨兵 = reward_npc_name〕/ 物品使用 / 进世界 / 等级等值 / 进区别名）；③ 事件语义 =
 * 原版 handler 逐指令 + 接取面（Talk `FUN_180c47220` 词汇、ItemPlay/EnterWorld/EnterArea 双角色、
 * LevelUp/LevelUpLogIn 等级等值遍历）+ 附加动作面（执行器 case 1/2/3/4/5/7；**步 f 执行矩阵修正**：
 * 推进边对 Hunt/EnterArea/TalkFOBJ/**Talk** 执行——对话平面推进/完成汇入 `FUN_180c4d5b0(…,-1)` 在
 * C:2075066 直调 c8d0，门 = 完成步 kind==4；Talk 对话接取 1002/20000 收尾同走 -1 路径 ⇒ **接取行
 * 附加动作**（`*(entry+0x10)` = QuestProgressExtraInfo 对象；2026-10-06 修正步 f 的「步 0 动作」误读）；
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

	/** 切换集规模（原版活行 ∧ owner RETAIL_TABLE）。 / Switch-set size frozen by the P7 contract batch. */
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
			// 领奖口的元数据来源（原版驱动，与生产目录同一条装载路径；与领奖门禁同型初始化）。
			// The claim flow's metadata source (same loader as production; mirrors the claim gate).
			com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.overlay(
				com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog.fromEntries(List.of()));
		}
		runtime = DataDrivenNativeRuntime.create(table, switchSet, NativeNpcNameResolver.instance(),
			enterAreaPort, itemIndex, inventory, movies, teleports, spawns, says, timers,
			NativeReportRewardFlow.withSink(claims), ChainAcquireEdges.instance());
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
	 * ZONE_ABSENT 9 + ACTION_UNFACED 1〔20032，creation 2 落点别名原版内在缺失〕）；非路由行的
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

	/**
	 * ①c 交付对象 #2（reward_npc_name）必须同时进对话注册表：传送门类 AI（PortalDialogAI2 /
	 * Specialize01PortalAI2）的开门页判定只读该表，缺注册时回落传送门页 1011（2026-10-08 实机 Kk：
	 * 19638 REWARD 后右键洛塔斯 799022 只见「进入卡斯帕内部」、零 C->S，奖励窗不可达）；且交付 NPC
	 * 不得进 onQuestStart（附近任务提示候选集只属接取面）。
	 * <p>
	 * The delivery object #2 must also land in the dialog registry (portal AIs decide the open page
	 * from it) while keeping out of onQuestStart (that candidate face belongs to acquire npcs).
	 */
	@Test
	void reportTalkInterestsAlsoRegisterTheDeliveryDialogFace() {
		com.aionemu.gameserver.dataholders.NpcData previous = com.aionemu.gameserver.dataholders.DataManager.NPC_DATA;
		com.aionemu.gameserver.dataholders.DataManager.NPC_DATA = new com.aionemu.gameserver.dataholders.NpcData();
		try {
			QuestEngine engine = new QuestEngine();
			runtime.installInterest(engine);
			Map<Integer, List<Integer>> reportTalks = runtime.reportTalkInterests();
			assertFalse(reportTalks.isEmpty(), "交付对话兴趣面非空（口径失效）");
			for (Map.Entry<Integer, List<Integer>> entry : reportTalks.entrySet()) {
				List<Integer> onTalk = engine.getQuestNpc(entry.getKey()).getOnTalkEvent();
				for (int questId : entry.getValue()) {
					assertTrue(onTalk.contains(questId), () -> "交付 NPC " + entry.getKey()
						+ " 的任务 " + questId + " 未进 onTalkEvent（传送门开门页/领奖窗不可达）");
				}
			}
			// 实机类锚点：洛塔斯 799022 承载 19638/19639 的交付面；19638 的接取 NPC 是凯西内尔
			// 798926，因此不得借移交面进入本 NPC 的 onQuestStart。
			List<Integer> lothasReport = reportTalks.getOrDefault(799022, List.of());
			assertTrue(lothasReport.containsAll(List.of(19638, 19639)),
				() -> "洛塔斯 799022 的交付面必须覆盖 19638/19639：" + lothasReport);
			assertFalse(engine.getQuestNpc(799022).getOnQuestStart().contains(19638),
				"交付面不得写 onQuestStart（19638 接取 NPC 是 798926）");
			// 冻结行不得借道任何交付 NPC 的 onTalkEvent 注册。
			for (int frozenId : runtime.frozenQuestIds().keySet()) {
				for (int npcId : reportTalks.keySet()) {
					assertFalse(engine.getQuestNpc(npcId).getOnTalkEvent().contains(frozenId),
						() -> "冻结行 " + frozenId + " 不应出现在交付 NPC " + npcId + " 的 onTalkEvent");
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
							"CollectItem 步必须落对话兴趣（原版 kind 1 = NPC 对话平面）：" + questId + "#" + step.index());
					}
					case ITEM_PLAY -> {
						itemPlay++;
						assertTrue(runtime.itemPlayInterests().values().stream()
							.anyMatch(hits -> contains(hits, expected)),
							"ItemPlay 步必须落物品使用兴趣（原版 kind 3 = 事件 5 = User__UseItem）：" + questId + "#" + step.index());
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

	/** ④ Hunt：组计数按原版守卫自增；达标收口。 / Hunt group counters and closure. */
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
	 * ④b Hunt 距离门（偏差修复第八批落面）：原版处理函数前奏 case 0 = {@code 2500.0 < distSq → return}
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
		// 边界：恰好 2500（50m）= 通过（原版是严格大于才拒绝）；z 轴参与平方和。
		// Boundary: exactly 2500 (50 m) passes (retail rejects only strictly greater); z enters the sum.
		assertTrue(DataDrivenNativeRuntime.withinRetailKillDistance(50f, 0f, 0f), "50m 整等于界内");
		assertFalse(DataDrivenNativeRuntime.withinRetailKillDistance(30f, 40f, 0.1f),
			"三维平方和超界必须拒绝");
	}

	/** ⑤ EnterArea / EnterWorld：直接步进（原版写目标步）。 / Direct advance kinds. */
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
	 * ⑤a Talk 步 = 原版共享对话平面（`FUN_180c474b0`）：打开发阶段页 select(K+1) 不写状态，
	 * 顺序动作 `10000+K`（K == 当前步 + 1）步进 + 关窗零发页（`mgr+0x5d8`），乱序零写零回发。
	 * Talk steps follow the shared dialog plane: open serves the stage page, sequential actions
	 * advance and close the window (zero page).
	 */
	@Test
	void talkStepsFollowTheSharedRetailDialogPlane() {
		int questId = firstRouted(Kind.TALK, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.talkInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		// 行选：阶段页（select1 = 1011），步号不变。页动作携带任务上下文（原版客户端在进度页发 questId）。
		assertTrue(runtime.onDialog(player, npcId, 31, 1, questId), "对话打开必须被对话平面服务");
		NativeTalkFixture.assertOnlyDialogPage(player, DataDrivenNativeRuntime.stagePageForGate(stepIndex));
		assertEquals(stepIndex, DataDrivenProgress.step(state.getQuestVars().getQuestVars()), "打开不写步号");
		// 打开（-1，无任务上下文）不认领：零发页——归引擎开门平面落通用页 10 列表，进行中阶段页
		// 不得占用开门（2026-10-05 实机 834166：进行中收集行挡掉同 NPC 其余可接任务）。
		NativeTalkFixture.clearPackets(player);
		assertFalse(runtime.onDialog(player, npcId, -1, 1, questId), "打开(-1) 不得被阶段面认领");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "打开(-1) 零发页");
		// 乱序动作（原版 `code-9999 != 当前步 + 1`）：零写、零回发。
		NativeTalkFixture.clearPackets(player);
		assertFalse(runtime.onDialog(player, npcId, 10000 + stepIndex + 2, 1, questId), "乱序动作必须静默");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "乱序动作不发页");
		// 顺序动作（原版守卫：code-9999 == 当前步 + 1 ⇔ code == 10000 + 当前步）：步号 + 1。
		assertTrue(runtime.onDialog(player, npcId, 10000 + stepIndex, 1, questId), "顺序动作必须步进");
		assertEquals(stepIndex + 1, DataDrivenProgress.step(state.getQuestVars().getQuestVars()),
			"顺序动作写 = 步号 + 1");
		// 推进尾 = 关窗零发页（`0x5d8`）：下一步骤由其自身 NPC 窗口服务，不发回新步页。
		NativeTalkFixture.assertCloseDialog(player);
	}

	/**
	 * ⑤c 推进不受「hit 步」限制 + 尾 = 关窗零发页（2026-10-06 实机 13403 两轮修正）：原版守卫只有
	 * 顺序（`code-9999 == 当前步 + 1`，`FUN_180c474b0`），不含 NPC 门；但推进尾 = `mgr+0x5d8`
	 * 关窗、零发页（退役 SETPRO 普查 3479/3923 关窗尾、同平台 SimpleTalk/1131 验收形）——下一步骤
	 * 由其自身 NPC 窗口的打开/行选服务。旧实现的「同窗续链」（把新步页发回同一对话窗）会让玩家在
	 * 首个 NPC 处就地把整条链走完（Beris/Jenel/两台机器步形同虚设）。
	 * The advance is not gated on the hit's step (retail guard = code order only), but its tail
	 * closes the window with zero page (0x5d8): the next step is served at its own entity's window.
	 */
	@Test
	void dialogChainAdvanceIsServedAtANonCurrentStepNpc() {
		// 实机行 13403：第 0 步 = Talk Kinesos(203096)；构造「对话链已推进过第 0 步」的中途状态。
		// Live row 13403: the step-0 talk is Kinesos (203096); seed a mid-chain state past step 0.
		int questId = 13403;
		int firstStep = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.talkInterests(), questId, firstStep);
		assertEquals(203096, npcId, "夹具锚点：13403 第 0 步 Talk NPC = Kinesos");
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, firstStep + 2);
		// 乱序推进（原版守卫：code-9999 == 当前步 + 1）：零写、零回发。
		NativeTalkFixture.clearPackets(player);
		assertFalse(runtime.onDialog(player, npcId, 10000, 1, questId), "乱序推进必须静默");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "乱序推进不发页");
		// 当前步的「结束对话」按钮（dialogId = 10000 + 当前步）在该 NPC 的对话窗里必须推进。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 10000 + firstStep + 2, 1, questId),
			"对话链推进必须在非当前步的 NPC 窗被服务");
		assertTrue(DataDrivenProgress.step(state.getQuestVars().getQuestVars()) > firstStep + 2,
			"推进后步号前进（含级联）");
		NativeTalkFixture.assertCloseDialog(player);
	}

	/**
	 * ⑤b 报告动作 1009：步进 + 报告通道（末步 = 待领奖 + 奖励窗页 5；原版 `player+0x100` + `mgr+0x1b0`）。
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
			"末步 1009 = 原版 SetQuestSuccess ⇒ 待领奖");
		NativeTalkFixture.assertOnlyDialogPage(player, 5);
	}

	/**
	 * ⑤d 交付检查按钮 39（收集型行的「交出持有物品」；参考 `_80875FightAgainstMechanerk` 的
	 * `checkQuestItems(0,1,false,10000,10001)`）：未持满 → 客户端声明的失败页（80875=10001，
	 * 事件行命名通道）；持满 → 按门扣除（quest.xml collect_item1 = quest_80875a ×7）+ 报告收尾
	 * （REWARD + 奖励窗页 5）。实机 2026-10-05：点击 39 静默关窗（无应答）修复。
	 */
	@Test
	void checkButton39FollowsTheHandOverGate() {
		int questId = 80875;
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.talkInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		inventory.clear();

		// 未持满：39 → 客户端声明的失败页 10001（check_user_item_fail），状态仍 START、零扣物。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 39, 1, questId), "39 检查必须被服务");
		NativeTalkFixture.assertOnlyDialogPage(player, 10001);
		assertEquals(QuestStatus.START, state.getStatus(), "失败检查不推进");
		assertTrue(inventory.calls().isEmpty(), "失败检查不扣物品");

		// 持满 7×（物品 182216117 = quest_80875a）：39 → 扣门 + REWARD + 奖励窗页 5。
		int gateItem = 182216117;
		inventory.hold(gateItem, 7L);
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 39, 1, questId), "持满后 39 必须收尾");
		NativeTalkFixture.assertOnlyDialogPage(player, 5);
		assertEquals(QuestStatus.REWARD, state.getStatus(), "持满 39 = 报告收尾");
		assertEquals(List.of("remove:" + gateItem + ":7"), inventory.calls(), "按门扣除收集物");
	}

	/**
	 * ⑤d' 交付检查按钮的 20002 编码（{@code HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE}）：切换集
	 * 65 行的步页按钮（select1/2/3）用它。只匹配 39 时 20002 会落进「≥1000 原样回发」分支——动作 id
	 * 被当页下发 ⇒ 客户端 load fail（同 QE-148 的 SETPRO1=10000 形）。20002 必须与 39 同族：
	 * 未持满 → 客户端声明失败页（80875=10001）；持满 → 扣门 + REWARD + 奖励窗页 5。
	 * The 20002 encoding of the check button must follow the same gate as 39.
	 */
	@Test
	void checkButtonSimpleEncodingBehavesLikeThePlainForm() {
		int questId = 80875;
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int npcId = keyOf(runtime.talkInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		inventory.clear();

		// 未持满：20002 → 同 39，客户端声明的失败页 10001，状态仍 START、零扣物。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 20002, 1, questId), "20002 检查必须被服务");
		NativeTalkFixture.assertOnlyDialogPage(player, 10001);
		assertEquals(QuestStatus.START, state.getStatus(), "失败检查不推进");
		assertTrue(inventory.calls().isEmpty(), "失败检查不扣物品");

		// 持满 7×（物品 182216117 = quest_80875a）：20002 → 扣门 + REWARD + 奖励窗页 5。
		int gateItem = 182216117;
		inventory.hold(gateItem, 7L);
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 20002, 1, questId), "持满后 20002 必须收尾");
		NativeTalkFixture.assertOnlyDialogPage(player, 5);
		assertEquals(QuestStatus.REWARD, state.getStatus(), "持满 20002 = 报告收尾");
		assertEquals(List.of("remove:" + gateItem + ":7"), inventory.calls(), "按门扣除收集物");
	}

	/**
	 * ⑤c ItemPlay 步 = 物品使用事件（原版事件 5 在 `User__UseItem` 的 `0x268+5*0x10` walk 派发；
	 * `FUN_180c46e90`）：物品 id 匹配 + 组 1 计数 + 达标收口；未声明物品零动作。
	 * ItemPlay steps fire on the item-USE event (retail tag 5 from User__UseItem); undeclared
	 * items are zero actions.
	 */
	@Test
	void itemPlayStepsCountTheUsedItem() {
		int questId = firstRouted(Kind.ITEM_PLAY, 0);
		int stepIndex = table.find(questId).orElseThrow().steps().getFirst().index();
		int itemId = keyOf(runtime.itemPlayInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);
		assertFalse(runtime.onItemUsed(player, itemId + 1), "未声明物品零动作");
		assertTrue(runtime.onItemUsed(player, itemId), "命中物品必须推进组槽");
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
		assertFalse(again, "二值组槽已置 1 ⇒ 超杀零动作（原版 uVar15 == 0 守卫）");
	}

	/** ⑦ PvP：军衔区间 + 等级差闸门（原版 `FUN_180c46980`）。 / PvP gates. */
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
	 * ⑦b PvP 距离门：原版 Pvp 处理函数（`FUN_180c46980`，槽 +0x528）与 Hunt 共用同一距离前奏——
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
	 * ⑧b col9 EnterInstance 落点面（偏差修复第九批）：落点表 = 原版 world.xml location_alias_list
	 * 原坐标；10034（creation 13）/20034（creation 3）解冻路由，20032（creation 2）因别名原版
	 * 内在缺失维持 fail-closed 冻结。
	 * The Enter Instance landing face (batch 9): 10034/20034 unfrozen and routed; 20032 stays
	 * fail-closed frozen (its landing alias is intrinsically absent in the retail world file).
	 */
	@Test
	void instanceEntryFaceFollowsTheRetailLandingTable() {
		NativeInstanceEntryPort port = NativeInstanceEntryPort.instance();
		assertEquals(3, port.all().size(), "表 = DD 引用的三个 creation（2/3/13）");
		NativeInstanceEntryPort.EntryPoint up = port.entry(3).orElseThrow();
		assertTrue(up.resolved(), "creation 3 别名在原版 IDTemple_Up world.xml 有坐标");
		assertEquals(300150000, up.worldId(), "creation 3 worldId = IDTemple_Up（与 20034 载荷一致）");
		assertEquals(562.176941f, up.x(), 1e-3f, "落点 x = 原版 location_alias 原值");
		assertEquals(223.046707f, up.y(), 1e-3f, "落点 y = 原版 location_alias 原值");
		assertEquals(137.100006f, up.z(), 1e-3f, "落点 z = 原版 location_alias 原值");
		assertEquals(270, up.heading(), "dir 270 = 原版原值（度）");
		NativeInstanceEntryPort.EntryPoint low = port.entry(13).orElseThrow();
		assertTrue(low.resolved(), "creation 13 别名在原版 IDTemple_Low world.xml 有坐标");
		assertEquals(300160000, low.worldId(), "creation 13 worldId = IDTemple_Low（与 10034 载荷一致）");
		assertEquals(794.989990f, low.x(), 1e-3f, "落点 x = 原版 location_alias 原值");
		assertEquals(213, low.heading(), "dir 213 = 原版原值（度）");
		assertFalse(port.entry(2).orElseThrow().resolved(), "creation 2 = IDElim_Entrance_alias 原版内在缺失");
		// 路由面：解冻两行 + 冻结一行且原因闭合。 / Routing: two unfrozen, one frozen with the closed reason.
		assertTrue(runtime.routes(10034), "10034（creation 13）已随第九批落面解冻");
		assertTrue(runtime.routes(20034), "20034（creation 3）已随第九批落面解冻");
		assertFalse(runtime.routes(20032), "20032（creation 2）维持 fail-closed 冻结");
		assertEquals(FreezeReason.ACTION_UNFACED, runtime.frozenQuestIds().get(20032),
			"冻结原因 = 落点别名原版内在缺失");
	}

	/**
	 * ⑧c col9 进入面的执行通道（2026-10-08 实机 10034 空副本事故）：进入步推进必须落到
	 * `NativeTeleportPort.enterInstance`（复用已注册/下一可用实例 + 注册 + 带 instanceId 传送），
	 * 不得走裸 `teleport`（跨世界恒 instanceId=1 ⇒ 副本默认空实例、从不 spawn）。
	 * The case-9 entry channel (the 2026-10-08 10034 empty-instance incident): the advance must reach
	 * enterInstance and never the bare teleport, which lands in the never-spawned default instance 1
	 * when crossing worlds.
	 */
	@Test
	void instanceEntryAdvancesThroughTheNextAvailableInstancePort() {
		int questId = 10034;
		int enterStep = 3;
		Step step = table.find(questId).orElseThrow().steps().get(enterStep);
		assertEquals("talk", step.kind().tableName(), "进入步 kind = Talk（执行器门放行）");
		assertEquals("13,300160000,7", step.column(9).replace(" ", ""),
			"原版载荷 = creation 13 / world 300160000 / leaveProgress 7");
		int npcId = keyOf(runtime.talkInterests(), questId, enterStep);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, questId, QuestStatus.START, enterStep);
		inventory.clear();
		teleports.clear();
		assertTrue(runtime.onDialog(player, npcId, 10000 + enterStep, 1, questId), "进入步推进事件");
		assertEquals(enterStep + 1, stepOf(player, questId), "推进到副本内首步");
		assertTrue(teleports.calls().contains("enterInstance:300160000"),
			"进入副本必须走下一可用实例通道：" + teleports.calls());
		assertFalse(teleports.calls().contains("teleport:300160000"), "不得走裸传送（默认空实例）");
		assertTrue(inventory.calls().contains("remove:182215627:1"), "进入步消耗凭证");
	}

	/**
	 * ⑧d 不可达副本阶段恢复（Playbook `UNREACHABLE_INSTANCE_REENTRY_RECOVERY`，14047 同族）：进入步与
	 * leaveProgress 之间（10034: 4..6；20034: 3..4）的持久步号在"非副本世界"的进世界事件写回进入步并
	 * 补发该进入步移除的物品（仅 0 持有时）；副本世界内 / 区间外 / 非 START 零动作；重发幂等。
	 * The unreachable-instance-stage recovery: an enter-world outside the instance world writes steps
	 * inside (enterStep, leaveProgress) back to the enter step and restores that step's removed items
	 * (only when the player holds none); inside the instance world, outside the range, or non-START rows
	 * stay untouched; repeats are idempotent.
	 */
	@Test
	void unreachableInstanceStepsRecoverOnEnterWorld() {
		// 收集面：生产 = 10034/20034 两行（20032 冻结不路由），载荷三数逐项对拍。
		List<DataDrivenNativeRuntime.LeaveRollback> rollbacks = runtime.instanceLeaveRollbacks();
		assertEquals(2, rollbacks.size(), "生产 = 10034/20034 两行");
		DataDrivenNativeRuntime.LeaveRollback low = rollbacks.stream()
			.filter(rollback -> rollback.questId() == 10034).findFirst().orElseThrow();
		assertEquals(3, low.enterStep(), "10034 进入步 = Talk LF4_FOBJ_Q10023D 所在步");
		assertEquals(300160000, low.worldId(), "10034 副本世界 = 300160000");
		assertEquals(7, low.leaveProgress(), "10034 leaveProgress = 原版载荷第三数");
		assertEquals(List.of(new DataDrivenNativeRuntime.ItemGrant(182215627, 1)), low.restoreItems(),
			"10034 补发物 = 进入步移除的 Jagged Sword");
		DataDrivenNativeRuntime.LeaveRollback high = rollbacks.stream()
			.filter(rollback -> rollback.questId() == 20034).findFirst().orElseThrow();
		assertEquals(2, high.enterStep(), "20034 进入步 = Talk DF4_SecretEntrance_Q20023 所在步");
		assertEquals(300150000, high.worldId(), "20034 副本世界 = 300150000");
		assertEquals(5, high.leaveProgress(), "20034 leaveProgress = 原版载荷第三数");
		assertTrue(high.restoreItems().isEmpty(), "20034 进入步不消耗物品");

		// 步 4 在非副本世界 ⇒ 写回 3 + 补发凭证 + 写回随包下发 + 零步执行器渲染。
		// Step 4 outside the instance world: write back + restore + the client sync packet + zero renders.
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, 10034, QuestStatus.START, 4);
		inventory.clear();
		teleports.clear();
		spawns.clear();
		movies.clear();
		says.clear();
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onEnterWorld(player, 210050000), "进世界事件触发恢复");
		assertEquals(3, stepOf(player, 10034), "写回进入步");
		assertEquals(List.of("give:182215627:1"), inventory.calls(), "补发被进入步移除的凭证");
		assertEquals(List.of(new NativeTalkFixture.QuestActionView(10034, 2, QuestStatus.START.value(), 3)),
			NativeTalkFixture.questActionViews(player), "写回必须下发 update 包（步 3）");
		assertTrue(teleports.calls().isEmpty() && spawns.calls().isEmpty() && movies.calls().isEmpty()
			&& says.calls().isEmpty(), "纯进度写：零步执行器渲染");
		// 幂等：重发零动作、零重复补发（CM_LEVEL_READY 无 once-only）。 / Idempotent on repeats.
		assertFalse(runtime.onEnterWorld(player, 210050000), "已在进入步：零动作");
		assertEquals(List.of("give:182215627:1"), inventory.calls(), "不得重复补发");
		// 已持有（步 5）不补发，但仍写回。 / Holding one suppresses only the restore.
		Player holding = NativeTalkFixture.player();
		NativeTalkFixture.add(holding, 10034, QuestStatus.START, 5);
		inventory.clear();
		inventory.hold(182215627, 1);
		assertTrue(runtime.onEnterWorld(holding, 210050000), "步 5 同样恢复");
		assertEquals(3, stepOf(holding, 10034), "步 5 写回 3");
		assertTrue(inventory.calls().isEmpty(), "已持有凭证不补发");
		// 副本世界内不回滚。 / Inside the instance world the stage stays.
		Player inside = NativeTalkFixture.player();
		NativeTalkFixture.add(inside, 10034, QuestStatus.START, 5);
		inventory.clear();
		assertFalse(runtime.onEnterWorld(inside, 300160000), "副本世界内保持");
		assertEquals(5, stepOf(inside, 10034), "不得写回");
		// 区间外：进入步 3 与 leaveProgress 7 零动作。 / Outside the range: no action.
		Player atEntry = NativeTalkFixture.player();
		NativeTalkFixture.add(atEntry, 10034, QuestStatus.START, 3);
		inventory.clear();
		assertFalse(runtime.onEnterWorld(atEntry, 210050000), "进入步处零动作");
		assertTrue(inventory.calls().isEmpty(), "进入步处不补发");
		Player atGoal = NativeTalkFixture.player();
		NativeTalkFixture.add(atGoal, 10034, QuestStatus.START, 7);
		inventory.clear();
		assertFalse(runtime.onEnterWorld(atGoal, 210050000), "leaveProgress 处零动作");
		assertEquals(7, stepOf(atGoal, 10034), "leaveProgress 处不得写回");
		// 非 START 零动作。 / Non-START rows stay untouched.
		Player rewarded = NativeTalkFixture.player();
		NativeTalkFixture.add(rewarded, 10034, QuestStatus.REWARD, 4);
		inventory.clear();
		assertFalse(runtime.onEnterWorld(rewarded, 210050000), "REWARD 零动作");
		assertEquals(4, stepOf(rewarded, 10034), "REWARD 不得写回");
		// 20034 同型：步 3/4 → 2，无补发物品；步 5（= leaveProgress）越窗零动作（typed 的 s5 漂移不镜像）。
		// The 20034 shape: steps 3/4 → 2 with no restore; step 5 (= leaveProgress) stays untouched
		// (the typed s5 drift is not mirrored).
		Player asmodian = NativeTalkFixture.player();
		NativeTalkFixture.add(asmodian, 20034, QuestStatus.START, 3);
		inventory.clear();
		assertTrue(runtime.onEnterWorld(asmodian, 220080000), "20034 同型恢复");
		assertEquals(2, stepOf(asmodian, 20034), "20034 写回进入步 2");
		assertTrue(inventory.calls().isEmpty(), "20034 进入步无移除物品");
		Player asmodianDeep = NativeTalkFixture.player();
		NativeTalkFixture.add(asmodianDeep, 20034, QuestStatus.START, 4);
		inventory.clear();
		assertTrue(runtime.onEnterWorld(asmodianDeep, 220080000), "20034 步 4 同样恢复");
		assertEquals(2, stepOf(asmodianDeep, 20034), "20034 步 4 写回 2");
		Player asmodianAtGoal = NativeTalkFixture.player();
		NativeTalkFixture.add(asmodianAtGoal, 20034, QuestStatus.START, 5);
		inventory.clear();
		assertFalse(runtime.onEnterWorld(asmodianAtGoal, 220080000), "20034 leaveProgress 处零动作");
		assertEquals(5, stepOf(asmodianAtGoal, 20034), "20034 步 5 不得写回（typed s5 漂移不镜像）");
	}

	/**
	 * ⑧e 恢复面不得短路同事件的其余进世界面：10034@步 4 的回滚与一条 ENTER_WORLD 行的直接步进
	 * 必须在同一事件里同时发生（`onEnterWorld` 是"先恢复、再 acquire/dispatch、合并返回"）。
	 * The recovery face never starves the enter-world advance on the same event.
	 */
	@Test
	void recoveryDoesNotShortCircuitTheEnterWorldAdvance() {
		int enterWorldQuest = 0;
		int enterWorldStep = -1;
		int worldId = -1;
		for (int questId : new TreeSet<>(runtime.routedQuestIds())) {
			for (Step candidate : table.find(questId).orElseThrow().steps()) {
				if (candidate.kind() != Kind.ENTER_WORLD) {
					continue;
				}
				int candidateWorld = Integer.parseInt(candidate.payload().trim());
				if (candidateWorld != 300160000) {
					enterWorldQuest = questId;
					enterWorldStep = candidate.index();
					worldId = candidateWorld;
					break;
				}
			}
			if (enterWorldQuest != 0) {
				break;
			}
		}
		assertTrue(enterWorldQuest != 0, "夹具：存在非 300160000 的 ENTER_WORLD 行");
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, 10034, QuestStatus.START, 4);
		NativeTalkFixture.add(player, enterWorldQuest, QuestStatus.START, enterWorldStep);
		inventory.clear();
		assertTrue(runtime.onEnterWorld(player, worldId), "同事件双面命中");
		assertEquals(3, stepOf(player, 10034), "副本内失配阶段写回 3");
		assertEquals(enterWorldStep + 1, stepOf(player, enterWorldQuest), "ENTER_WORLD 行照常直接步进（恢复不得短路）");
	}

	/**
	 * ⑧f 空路由视图（`create` 的早退分支，端口全 null）：不得构造恢复面、进世界事件恒零动作、
	 * 零 NPE——生产启动的"切换集为空"路径。
	 * The empty-routing view (create's early-return branch): no recovery face, a no-op enter-world,
	 * zero NPE.
	 */
	@Test
	void emptyRoutingViewHasNoRecoveryFace() {
		DataDrivenNativeRuntime empty = DataDrivenNativeRuntime.create(table, Set.of(), null, null, null, null,
			null, null, null, null, null, null, null);
		assertTrue(empty.instanceLeaveRollbacks().isEmpty(), "空路由视图无恢复面");
		assertFalse(empty.routes(10034), "空路由视图不路由");
		assertFalse(empty.onEnterWorld(NativeTalkFixture.player(), 210050000), "空路由视图进世界恒零动作");
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
			assertFalse(runtime.chainAcquireSuccessors().contains(questId), "冻结行不得进链式接取面：" + questId);
		}
	}

	// ------------------------------------------------------------------ 链式接取面（acquire=none）

	/** 已注册链式后继冻结集（资源 34 行 − 6 XML_RETENTION − 1 冻结 = 27）。 / The frozen registered successor set. */
	private static final Set<Integer> CHAIN_ACQUIRE_SUCCESSORS = Set.of(
		10011, 10033, 10034, 10035, 10111, 10113, 10501, 10502, 10503, 10504, 10505, 10506, 10507, 10526,
		20011, 20033, 20034, 20111, 20113, 20501, 20502, 20503, 20504, 20505, 20506, 20507, 20526);

	/** 退役证据面 5 行的精确前序表（10032/20035 为 XML 行、20032 冻结 ⇒ 不在注册面）。 */
	private static final Map<Integer, List<Integer>> RETIRED_EVIDENCE_PREDECESSORS = Map.of(
		10033, List.of(10032),
		10034, List.of(10033),
		10035, List.of(10031, 10032, 10033, 10034),
		20033, List.of(20032),
		20034, List.of(20033));

	/** ⑫ 注册面：owned ∧ routed ∧ acquire=none ∧ 非冻结，冻结 27 行。 */
	@Test
	void chainAcquireRegistersOnlyOwnedRoutedNoneAcquiredUnfrozenSuccessors() {
		assertEquals(CHAIN_ACQUIRE_SUCCESSORS, runtime.chainAcquireSuccessors(), "链式接取面 = 证据集 ∩ 本车道");
		for (ChainAcquireEdges.Edge edge : runtime.chainAcquireEdges()) {
			int questId = edge.successor();
			assertTrue(runtime.owns(questId), "链式后继必须被本车道接管：" + questId);
			assertTrue(runtime.routes(questId), "链式后继必须可路由：" + questId);
			assertFalse(runtime.frozenQuestIds().containsKey(questId), "链式后继不得冻结：" + questId);
			assertEquals("none", table.find(questId).orElseThrow().acquireKind(),
				"链式后继接取类别 = none：" + questId);
			assertFalse(edge.predecessors().isEmpty(), "链式边必须声明前序：" + questId);
			assertFalse(edge.predecessors().contains(questId), "链式边不得自环：" + questId);
		}
	}

	/**
	 * ⑬ 证据对拍：原版 {@code finished_quest_cond} 桶逐行等于 quest.xml 列（独立重解析），
	 * 退役证据桶逐行等于冻结表（生成器输出的另 8 行中，注册面只剩 5 行）。
	 */
	@Test
	void chainAcquireEdgesMirrorTheRetailFinishedConditions() throws Exception {
		String questXml = new String(resource("/aion/data/static_data/quest/retail/quest.xml").readAllBytes(),
			StandardCharsets.UTF_8);
		int mirrored = 0;
		for (ChainAcquireEdges.Edge edge : runtime.chainAcquireEdges()) {
			String block = questBlock(questXml, edge.successor());
			assertNotNull(block, "原版 quest.xml 必须含链式后继行：" + edge.successor());
			List<Integer> finished = new ArrayList<>();
			Matcher matcher = Pattern.compile("<finished_quest_cond\\d+>(\\w+)</finished_quest_cond\\d+>").matcher(block);
			while (matcher.find()) {
				String token = matcher.group(1);
				assertTrue(token.matches("Q\\d+"), "链式行的 finished 条件词法 = Q<id>：" + token);
				finished.add(Integer.parseInt(token.substring(1)));
			}
			List<Integer> expected;
			if (finished.isEmpty()) {
				expected = RETIRED_EVIDENCE_PREDECESSORS.get(edge.successor());
				assertNotNull(expected, "无 finished 条件的链式后继必须落在退役证据冻结表：" + edge.successor());
			} else {
				expected = finished;
				assertEquals(ChainAcquireEdges.Source.RETAIL_FINISHED_COND, edge.source(),
					"有 finished 条件的链式行证据轴 = retail-finished-cond：" + edge.successor());
			}
			assertEquals(expected, edge.predecessors(), "链式前序逐行对拍：" + edge.successor());
			mirrored++;
		}
		assertEquals(27, mirrored, "注册面逐行对拍全覆盖");
	}

	/** 取出 quest.xml 中某 id 的整段行文本。 / The raw quest.xml row block for one id. */
	private static String questBlock(String questXml, int questId) {
		Matcher matcher = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL).matcher(questXml);
		while (matcher.find()) {
			String block = matcher.group(1);
			if (block.contains("<id>" + questId + "</id>")) {
				return block;
			}
		}
		return null;
	}

	/**
	 * ⑭ 冻结前置死边闭包：20032 冻结（ACTION_UNFACED）⇒ 直接后继 20033 不可达；20033 的自动发放面
	 * 只有本链（acquire=none）⇒ 下游 20034 同样不可达。解冻时本断言翻红，强制同步重生成资源与验收。
	 * <p>
	 * Dead-edge closure over the registered edges: the frozen 20032 rules out 20033 outright, and 20033
	 * has no grant face besides this chain, so 20034 is unreachable downstream as well. Unfreezing 20032
	 * turns this assertion red by design.
	 */
	@Test
	void chainAcquireFrozenPredecessorEdgesArePinnedAsDead() {
		Set<Integer> dead = new TreeSet<>();
		boolean grew = true;
		while (grew) {
			grew = false;
			for (ChainAcquireEdges.Edge edge : runtime.chainAcquireEdges()) {
				for (int predecessor : edge.predecessors()) {
					if (runtime.frozenQuestIds().containsKey(predecessor) || dead.contains(predecessor)) {
						grew |= dead.add(edge.successor());
					}
				}
			}
		}
		assertEquals(Set.of(20033, 20034), dead,
			"冻结前置的死边闭包 = 恰 20033/20034（20032 冻结 + 20033 仅链式面）");
		assertEquals(FreezeReason.ACTION_UNFACED, runtime.frozenQuestIds().get(20032), "20032 冻结原因不变");
	}

	/**
	 * ⑮ 发放行走（10033 链）：前置未完成 / unfinished 命中 / 重复触发 / 已完成态四类负例 + 正例的
	 * add 包断言。unfinished 方向 = 引用行"未"COMPLETE 才通过（10025 已 COMPLETE ⇒ 不发放）。
	 */
	@Test
	void chainGrantRequiresEveryPredecessorCompleteAndUnfinishedClear() {
		// 负例①：无 10032 完成态（完成通知自守卫之外的直接调用也不得发放）。
		Player fresh = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 52);
		assertFalse(runtime.onQuestCompleted(fresh, 10032), "无完成态：零动作");
		assertNull(fresh.getQuestStateList().getQuestState(10033), "前置未完成不得发放");

		// 正例：10032 完成 ⇒ 立即发放 + add 包（action 1）。
		Player eligible = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 52);
		NativeTalkFixture.completePrerequisites(eligible, 10032);
		NativeTalkFixture.clearPackets(eligible);
		assertTrue(runtime.onQuestCompleted(eligible, 10032), "前置完成 ⇒ 发放后继");
		QuestState granted = eligible.getQuestStateList().getQuestState(10033);
		assertNotNull(granted, "后继已建档");
		assertEquals(QuestStatus.START, granted.getStatus(), "后继状态 = START");
		assertEquals(List.of(new NativeTalkFixture.QuestActionView(10033, 1, QuestStatus.START.value(), 0)),
			NativeTalkFixture.questActionViews(eligible), "发放恰发一个 add 包（action 1 + questId + START + 步 0）");

		// 负例②：unfinished 条件命中（10025 已 COMPLETE）⇒ 不发放（原版行的未进行轴）。
		Player blocked = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 52);
		NativeTalkFixture.completePrerequisites(blocked, 10032, 10025);
		assertFalse(runtime.onQuestCompleted(blocked, 10032), "unfinished 命中不得发放");
		assertNull(blocked.getQuestStateList().getQuestState(10033), "unfinished 命中无建档");

		// 负例③：重复触发幂等（已 START ⇒ ALREADY_RUNNING 静默，不重复建档）。
		assertFalse(runtime.onQuestCompleted(eligible, 10032), "重复触发幂等");

		// 负例④：完成态不再发放（生产完成口径 = completeCount ≥ 1〔QuestService 逐次 +1〕⇒
		// max_repeat_count=1 预算耗尽 ⇒ REPEAT_LIMIT；裸 add 的 COMPLETE 计数 0 属"预算未用"口径，可重接）。
		Player done = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 52);
		NativeTalkFixture.completePrerequisites(done, 10032, 10033);
		assertFalse(runtime.onQuestCompleted(done, 10032), "已完成不得重发");
		assertEquals(QuestStatus.COMPLETE, done.getQuestStateList().getQuestState(10033).getStatus(), "完成态保持");
	}

	/**
	 * ⑯ 等级轴补发：完成时等级不达 ⇒ 不发；升级重走（{@code onLevelReached}）达标补发——
	 * 镜像退役 XML 的 {@code <level-up/> + <start-eligible/>} 第二事件面。
	 */
	@Test
	void chainGrantWaitsForTheLevelAxisAndFiresOnTheNextWalk() {
		Player low = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 51);
		NativeTalkFixture.completePrerequisites(low, 10032);
		assertFalse(runtime.onQuestCompleted(low, 10032), "51 级：等级轴不达不得发放");
		assertNull(low.getQuestStateList().getQuestState(10033), "51 级无建档");
		// 生产顺序 = 等级字段先落地，再通知重走（接取等级轴读模型等级，不读通知参数）。
		// Production order: the level field lands first, then the walk gets notified.
		NativeTalkFixture.levelUp(low, 52);
		assertTrue(runtime.onLevelReached(low, 52, false), "升级重走补发");
		QuestState granted = low.getQuestStateList().getQuestState(10033);
		assertNotNull(granted, "52 级补发建档");
		assertEquals(QuestStatus.START, granted.getStatus(), "补发状态 = START");
	}

	/**
	 * ⑰ 触发链锁定：native 完成通知（{@code setFinishingState → QuestEngine.onLvlUp}）经
	 * {@code onLevelReached} 重走到达链式面——生产单例上的依赖不许被静默删除。
	 */
	@Test
	void nativeCompletionReachesTheChainFaceThroughTheEngineLevelUpHook() {
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 52);
		NativeTalkFixture.completePrerequisites(player, 10032);
		new QuestEngine().onLvlUp(new QuestEnv(null, player, 10032, 0));
		QuestState granted = player.getQuestStateList().getQuestState(10033);
		assertNotNull(granted, "native 完成通知 ⇒ 链式补发（onLvlUp → onLevelReached 重走）");
		assertEquals(QuestStatus.START, granted.getStatus(), "补发状态 = START");
		assertTrue(DataDrivenNativeRuntime.instance().chainAcquireSuccessors().contains(10033),
			"生产单例注册面含 10033");
	}

	/** DD 表 ∩ drop 列的 XML_RETENTION 行（目录供源；native 不得注册——单一 owner）。 */
	private static final Set<Integer> XML_RETAINED_DROP_ROWS = Set.of(
		10032, 10112, 10527, 10530, 15606, 17540, 20031, 20112, 20527, 20530, 25050, 25082, 27540);

	/**
	 * ⑱ 原版掉落列注册（2026-10-08 实机 10034：击杀 216494 不掉 quest_10034a）：退役 XML 的
	 * {@code <drops>} 随 catalog 退场后，DD 车道必须与 Talk/Collect 同口径从 quest.xml
	 * {@code drop_*} 列接手。注册面 = routed ∧ 原版 drop 列（独立重解析对拍，零静默跳过）；
	 * 切换集 ∩ drop = 163，冻结 ∩ drop = {15602,15605,15608}（ZONE_ABSENT，不可达 ⇒ 惰性残差，
	 * 解冻自动接手），注册 = 160；XML_RETENTION 行不得由 native 供源（单一 owner）。
	 * <p>
	 * The DD lane serves the retail drop columns like the Talk/Collect families; the registered face
	 * must equal routed ∧ drop-column rows (independently re-parsed), with the three frozen rows pinned
	 * as the lazy residue and the XML-retained rows excluded.
	 */
	@Test
	void routedRowsCarryTheirRetailDropColumns() throws Exception {
		String questXml = new String(resource("/aion/data/static_data/quest/retail/quest.xml").readAllBytes(),
			StandardCharsets.UTF_8);
		Map<Integer, String> blocks = questRowBlocks(questXml);
		// ① 注册面 = routed ∧ 原版 drop 列（独立重解析；多一行/少一行即红，零静默跳过）。
		Set<Integer> expected = new TreeSet<>();
		for (int questId : runtime.routedQuestIds()) {
			String block = blocks.get(questId);
			assertNotNull(block, "routed 行必须在原版 quest.xml：" + questId);
			if (dropColumns(block)) {
				expected.add(questId);
			}
		}
		Set<Integer> actual = new TreeSet<>();
		runtime.dropInterests().values().forEach(drops -> drops.forEach(drop -> actual.add(drop.questId())));
		assertEquals(expected, actual, "注册面 = routed ∧ 原版 drop 列（零静默跳过）");

		// ② 计数钉：切换集 ∩ drop = 163；冻结 ∩ drop = 恰 3 行；注册 = 163 − 3 = 160。
		Set<Integer> switchDrop = new TreeSet<>();
		for (int questId : switchSet) {
			if (dropColumns(blocks.get(questId))) {
				switchDrop.add(questId);
			}
		}
		assertEquals(163, switchDrop.size(), "切换集 ∩ 原版 drop 列冻结 163 行");
		assertEquals(Set.of(15602, 15605, 15608),
			switchDrop.stream().filter(questId -> runtime.frozenQuestIds().containsKey(questId))
				.collect(java.util.stream.Collectors.toCollection(TreeSet::new)),
			"冻结 ∩ drop = 恰 {15602,15605,15608}（ZONE_ABSENT ⇒ 惰性残差）");
		assertEquals(160, actual.size(), "注册面 = 163 − 3 冻结残差");

		// ③ 负例：13 行 XML_RETENTION（目录供源）不得由 native 注册。
		for (int questId : XML_RETAINED_DROP_ROWS) {
			assertFalse(actual.contains(questId), "XML_RETENTION 行不得由 native 供源：" + questId);
		}

		// ④ collectingStep 范围钉（QE-025 死锁类）：∀ 注册条目 ∈ {0} ∪ [1, 步数)。
		runtime.dropInterests().values().forEach(drops -> drops.forEach(drop -> {
			int stepCount = table.find(drop.questId()).orElseThrow().steps().size();
			assertTrue(drop.collectingStep() == 0 || drop.collectingStep() < stepCount,
				"collectingStep 不得越界：quest " + drop.questId() + " step " + drop.collectingStep()
					+ " / 步数 " + stepCount);
		}));
	}

	/**
	 * ⑲ 10034 掉落形状冻结 + 聚合端到端：216494（{@code LF4_B6_FanaticAs_NamedQ_53_An}）→ quest_10034a
	 * （182215627 "Jagged Sword"），chance 100 / each-member / collectingStep 3（= 原版
	 * {@code collect_progress}，与 DD 步号 {@code var0==3} 直比——{@code QuestVars} 按 6-bit 拆槽，
	 * 步号即 {@code getQuestVarById(0)}）；{@code QuestEngine.questDrops} 聚合面必须含该条目
	 * （native 独立供源；实机 10034 的最小复现）。
	 * <p>
	 * The 10034 drop shape pin plus the end-to-end aggregation check through QuestEngine.questDrops.
	 */
	@Test
	void row10034ServesItsRetailDropThroughTheEngineQuery() throws Exception {
		Integer itemId = RetailItemNameIndex.loadItemTemplates().resolve("quest_10034a");
		assertEquals(Integer.valueOf(182215627), itemId, "quest_10034a = 182215627（Jagged Sword）");
		List<QuestCatalogDrop> drops = runtime.questDropsFor(216494);
		assertTrue(drops.stream().anyMatch(drop -> drop.questId() == 10034 && drop.itemId() == itemId
			&& drop.chance() == 100 && drop.collectingStep() == 3 && drop.dropEachGroupMember()),
			"216494 必须携带 10034 的原版击杀掉落（100% / each-member / step 3）");
		// 端到端：引擎聚合面（目录为空时由 native 源独立供源）。
		assertTrue(new QuestEngine().questDrops(216494).stream().anyMatch(drop -> drop.questId() == 10034),
			"QuestEngine.questDrops 必须聚合 DD 车道掉落（实机 10034 复现面）");
	}

	/**
	 * ⑳ 对象交互掉落（15011 生长任务：702730 = {@code LF5_FOBJ_Starflower_Coral_Q15011a}，
	 * {@code ai=quest_use_item}）：注册面必须含该对象（quest_15011a，100% / each-member /
	 * collectingStep 0 = 任意 START 步有效）——交互 AI 在 {@code QuestService.getQuestDrop} 为空时
	 * 静默返回，掉落列表路径缺它不可达。（实机「点击交互没反应」的**首因**是 can-act 资格门，
	 * 见 ㉑；本测试钉的是同链路的掉落供源面。）
	 * <p>
	 * The object-interaction drop case (quest 15011): the registered face must serve the object's
	 * drop list. (The live zero-response root cause was the can-act gate — see ㉑; this test pins the
	 * drop-supply face of the same interaction chain.)
	 */
	@Test
	void growthCollectRowsServeTheirObjectDrops() throws Exception {
		Integer itemId = RetailItemNameIndex.loadItemTemplates().resolve("quest_15011a");
		assertNotNull(itemId, "原版物品符号必须解析: quest_15011a");
		assertTrue(runtime.questDropsFor(702730).stream().anyMatch(drop -> drop.questId() == 15011
			&& drop.itemId() == itemId && drop.chance() == 100 && drop.collectingStep() == 0),
			"702730（Star Coral）必须携带 15011 的原版掉落（点击交互 → 掉落列表路径）");
	}

	/**
	 * ㉑ CollectItem 步的追加 FOBJ 列（采集物件使用面）：独立重解析原版 DD 表对拍 routed ∩ 列 1..4
	 * → 物件索引（逐条相等，零静默跳过），并钉死 15011/702730 的资格与认领端到端（含未接取/步不符/
	 * REWARD 负例）与 XML_RETENTION 物件缺席负例。2026-10-08 实机「右击 702730 零响应」的根因 =
	 * 列 1..4 从未被消费 ⇒ {@code onCanAct(ACTION_ITEM_USE)} 资格与交互认领双缺，交互 AI 在
	 * can-act 门早退（零包，进度条都不出现）。
	 * <p>
	 * The CollectItem appended-FOBJ object-use face: independent recomputation against the retail
	 * table, the 15011/702730 eligibility and claim end-to-end, and the negative cases. The live
	 * zero-response root cause (2026-10-08): the appended-FOBJ columns had no consumers, so both the
	 * ACTION_ITEM_USE eligibility and the interaction claim were missing.
	 */
	@Test
	void collectStepFobjColumnsServeTheObjectUseFace() {
		// ① 独立重算：解析原版表，收集 routed 行的 CollectItem 列 1..4（每条列值一个对象名）。
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		Map<Integer, Set<String>> expected = new TreeMap<>();
		Set<Integer> fobjRows = new TreeSet<>();
		for (int questId : table.questIds()) {
			Row row = table.find(questId).orElseThrow();
			boolean carriesFobj = row.steps().stream().anyMatch(step -> step.kind() == Kind.COLLECT_ITEM
				&& Stream.of(1, 2, 3, 4).anyMatch(column -> {
					String raw = step.column(column);
					return raw != null && !raw.isBlank();
				}));
			if (!carriesFobj) {
				continue;
			}
			fobjRows.add(questId);
			if (!runtime.routes(questId)) {
				continue;
			}
			for (Step step : row.steps()) {
				if (step.kind() != Kind.COLLECT_ITEM) {
					continue;
				}
				for (int column = 1; column <= 4; column++) {
					String raw = step.column(column);
					if (raw == null || raw.isBlank()) {
						continue;
					}
					List<Integer> npcIds = resolver.resolveMonsterIds(raw);
					assertFalse(npcIds.isEmpty(), "FOBJ 列名字必须可解析（否则运行期逐条静默跳过）: quest "
						+ questId + " col " + column + " = " + raw);
					for (int npcId : npcIds) {
						expected.computeIfAbsent(npcId, key -> new TreeSet<>())
							.add(questId + ":" + step.index() + ":" + column);
					}
				}
			}
		}
		Map<Integer, Set<String>> actual = new TreeMap<>();
		runtime.collectObjectInterests().forEach((npcId, hits) -> {
			Set<String> entries = new TreeSet<>();
			for (DataDrivenNativeRuntime.StepHit hit : hits) {
				entries.add(hit.questId() + ":" + hit.stepIndex() + ":" + hit.group());
			}
			actual.put(npcId, entries);
		});
		assertEquals(expected, actual, "routed ∩ CollectItem FOBJ 列 → 物件索引逐条对拍（零静默跳过）");
		// 82 = 表内全部携带 FOBJ 列的 CollectItem 步行（含非路由行；kind 词按加载器口径大小写不敏感，
		// 含表内 15 块 Collectitem 变体）。94 个物件名。
		assertEquals(82, fobjRows.size(), "原版表 CollectItem FOBJ 列行冻结（含非路由行 = 数据面钉子）");

		// ② 15011 实机件：702730 挂 15011 步 0 第 1 列。
		assertEquals(List.of(15011), runtime.collectObjectQuestIds(702730));
		assertTrue(runtime.isCollectObject(702730));

		// ③ 资格与认领端到端：START 步 0 ⇒ 可交互 + 引擎先手认领（-1 打开落 DD 面，携带 owner）。
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, 15011);
		assertTrue(runtime.allowsItemUse(player, 702730), "15011 START 步 0 ⇒ 物件可交互");
		QuestEnv env = NativeTalkFixture.dialog(player, 702730, 0, -1);
		assertTrue(new QuestEngine().onDialog(env), "物件打开（-1）必须被 DD 采集面先手认领");
		assertEquals(15011, env.getQuestId(), "认领必须携带 owner（组队每人一枚的掉落过滤）");

		// ④ 负例：未接取 / 步不符（var0=1）/ REWARD 态一律不可交互。未接取的打开仍走
		//    「认领 + 零副作用」吞掉（对采集族 onObjectUse 口径：采集物件绝不落通用页 10——
		//    客户端对物件开窗即 load fail，2026-10-04 真机 1103），且不得下发任何对话页。
		Player fresh = NativeTalkFixture.player();
		assertFalse(runtime.allowsItemUse(fresh, 702730), "未接取 ⇒ 物件不可交互");
		NativeTalkFixture.clearPackets(fresh);
		assertTrue(new QuestEngine().onDialog(NativeTalkFixture.dialog(fresh, 702730, 0, -1)),
			"无匹配任务 ⇒ 打开被认领并以零副作用吞掉（不落通用页）");
		assertTrue(NativeTalkFixture.dialogPages(fresh).isEmpty(), "认领零发包（物件不得开对话窗）");
		Player advanced = NativeTalkFixture.player();
		NativeTalkFixture.add(advanced, 15011, QuestStatus.START, 1);
		assertFalse(runtime.allowsItemUse(advanced, 702730), "步不符 ⇒ 物件不可交互");
		Player rewarded = NativeTalkFixture.player();
		NativeTalkFixture.add(rewarded, 15011, QuestStatus.REWARD, 1);
		assertFalse(runtime.allowsItemUse(rewarded, 702730), "REWARD ⇒ 物件不可交互");

		// ⑤ XML_RETENTION 负例：10112 的物件（列只出现在 XML 供源行）不得由 DD 供源。
		List<Integer> retained = resolver.resolveMonsterIds("Ab1_FOBJ_Supply_Box_Q10112a");
		assertFalse(retained.isEmpty(), "10112 物件名必须可解析（负例前置）");
		retained.forEach(npcId -> assertFalse(runtime.isCollectObject(npcId),
			"XML_RETENTION 物件不得由 DD 供源: " + npcId));
	}

	/** 原版行块是否声明掉落列（与 {@code NativeQuestXmlTable#hasRetailDropColumns} 同口径）。 */
	private static boolean dropColumns(String block) {
		return block != null && (block.contains("<drop_item_1>") || block.contains("<drop_monster_1>"));
	}

	/** 单遍解析原版 quest.xml 为 id → 行块（⑱ 全量对拍用；逐 id 重扫会 O(n²) 卡住）。 */
	private static Map<Integer, String> questRowBlocks(String questXml) {
		Map<Integer, String> blocks = new java.util.HashMap<>();
		Matcher matcher = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL).matcher(questXml);
		while (matcher.find()) {
			String block = matcher.group(1);
			Matcher id = Pattern.compile("<id>(\\d+)</id>").matcher(block);
			if (id.find()) {
				blocks.put(Integer.parseInt(id.group(1)), block);
			}
		}
		return blocks;
	}


	/**
	 * ⑨ 切换集分桶与逐类步数冻结（原版契约面）：`P7 步 1` 的逐类步数、以及本批
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
				+ "别名在原版 idelim/world.xml 本就缺失 = 内在缺失；10034/20034 已随第九批落面解冻〕）");
		// 步 e2 后唯一未解析面 = 切换集行引用的 LF6 原版缺席进区别名（原文大小写，§10.3-#23）；
		// 挑战哨兵 `_challengetask_` 已按 P0c-58 四源裁定落面（接取 NPC = reward_npc_name），
		// MESSAGE 字符串键经 retail-quest-string-ids.xml 全部解析。
		// 接取直方图（去重任务数，离线镜像权威值）：talk 1133（含哨兵 6）/ itemplay 13 /
		// enterworld 12 / leveluplogin 15 / enterarea 20（步 f kind-6 落面）/ none 264（其中 27 行由
		// 链式接取面发放，见 ⑫；余 237 行仓库内无链路证据，维持无接取面）。
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
			"未解析名字集 = 原版缺席进区别名（NativeEnterAreaPort.RETAIL_ABSENT_ALIASES 的切换集子集，§10.3-#23）");
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
	 * ⑩b 交付报告面（零步 Talk 行 = DD_TALK_SIMPLE；原版所有行恒注册交付对象 #2 {@code reward_npc_name}，
	 * 槽 +0x238）：START 31 → 报告推进 REWARD + 页 5；REWARD 31 → 页 5；{@code 23} = NOREWARD
	 * 无选择确认 → 结算 + 领奖收尾回选择对话页 10（原版 npc-complete finish=SELECTION_DIALOG；9/28
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
			"1009 报告确认 = 原版交付对象推进");
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
	 * ⑤e 中继末步 SET_SUCCEED(10255) 收口（2026-10-05 实机 19671「蕾娜的欢迎问候」：商人梅利娜 806699
	 * 对话收口）：原版 `0x280f` = SetProgress + 完成通道 `0x5d8`＝**关窗零发页**（退役 19671 尾 =
	 * `LEVEL_AND_VISIBILITY_REFRESH sync + close-dialog`；SETPRO 全量普查 3479/3923 关窗尾）。
	 * 旧实现的「完成页 1008」会让客户端误显「任务已完成」，任务实际停在待交付（回教官 806698 报告），
	 * 且 NPC 任务标记不刷新、玩家卡在「有标记无对话」。
	 * <p>
	 * The relay last-step SET_SUCCEED tail: advance to REWARD, refresh visibility and close the
	 * window (retail 0x280f / 0x5d8); no completion page is sent.
	 */
	@Test
	void relaySetSucceedClosesTheWindowAfterTheAdvance() {
		int questId = 19671;
		assertTrue(runtime.routedQuestIds().contains(questId), "19671 必须被切换批路由");
		int stepIndex = table.find(questId).orElseThrow().steps().getLast().index();
		int npcId = keyOf(runtime.talkInterests(), questId, stepIndex);
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, stepIndex);

		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 10255, 1, questId), "SET_SUCCEED 必须被进度面收口");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "收口 = 待交付（回教官报告）");
		assertEquals(stepIndex + 1, DataDrivenProgress.step(state.getQuestVars().getQuestVars()),
			"收口写步 = 步号 + 1");
		NativeTalkFixture.assertCloseDialog(player);
	}

	/**
	 * ⑤f 末步收口不受「hit 步」限制（2026-10-06 实机 13403 第三轮：末步按钮 SET_SUCCEED(10255) 从
	 * 非当前步的对话窗到达被守卫挡下 ⇒ 只关窗、任务不进 REWARD）：收口基准 = 当前步（非 hit 步），
	 * 尾 = 可见性刷新 + 关窗零发页（10528 同形：`LEVEL_AND_VISIBILITY_REFRESH → CloseDialog`）。
	 * The last-step completion is not gated on the hit's step (live 13403 round 3); it keys on the
	 * current step and closes with the visibility refresh.
	 */
	@Test
	void lastStepSetSucceedIsServedFromANonCurrentStepWindow() {
		int questId = 13403;
		int firstStep = table.find(questId).orElseThrow().steps().getFirst().index();
		int lastStep = table.find(questId).orElseThrow().steps().getLast().index();
		int npcId = keyOf(runtime.talkInterests(), questId, firstStep);
		assertEquals(203096, npcId, "夹具锚点：13403 第 0 步 Talk NPC = Kinesos");
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.START, lastStep);
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, npcId, 10255, 1, questId),
			"末步收口必须在非当前步的 NPC 窗被服务");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "13403 末步收口 = 待交付（Alaus）");
		NativeTalkFixture.assertCloseDialog(player);
	}

	/**
	 * ⑩c 中继步 Talk 链行的交付对象 #2（教官 806698 = `LC1_L_grow_npc_Rena_01`）：START 态不认领
	 * （进度面 owns——缺守卫会让「一键报告」跳过中继步直落 REWARD）；REWARD 态 31/1009 与打开（-1，
	 * 对齐 SimpleTalk 族交付 NPC 面）→ 奖励窗页 5，领奖确认（23）→ 结算 + npc-complete
	 * finish=SELECTION_DIALOG 回页 10（2026-10-05 实机 19671：收口后教官处无交付路由，任务卡在
	 * 「向成长支援教官报告」无法领奖；2026-10-06 实机 19683：REWARD 回教官打开落默认页 10、
	 * 无交付行可点——-1 认领即此缺口，QE-145 边界①）。
	 * <p>
	 * The relay Talk chain delivery object: START is not claimed by this face; REWARD opens the
	 * reward window on 31/1009 and on the open action (-1, matching the SimpleTalk delivery-NPC
	 * face) and settles through the claim flow.
	 */
	@Test
	void relayTalkChainRowsDeliverOnTheRewardInstructor() {
		int questId = 19671;
		Map<Integer, List<Integer>> reportInterests = runtime.reportTalkInterests();
		assertTrue(reportInterests.getOrDefault(806698, List.of()).containsAll(List.of(questId, 19683)),
			"教官 806698（静态数据 LC1_L_grow_npc_Rena_01）必须注册 19671/19683 的交付对象");
		int rewardNpc = 806698;

		// START（中继步未走）：教官处行选 31 与打开（-1）都不得跳步推进（进度面 owns）。
		Player started = NativeTalkFixture.player();
		NativeTalkFixture.add(started, questId, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(started);
		assertFalse(runtime.onDialog(started, rewardNpc, 31, 1, questId), "START 态交付面不得认领中继步行");
		assertFalse(runtime.onDialog(started, rewardNpc, -1, 1, questId), "START 态打开不得被交付面认领");
		assertTrue(NativeTalkFixture.dialogPages(started).isEmpty(), "START 态零发页");

		// REWARD（已收口）：31 → 奖励窗页 5；1009 → 页 5；打开（-1）→ 页 5（SimpleTalk 族同形）；
		// 23（NOREWARD）→ 结算 + 回页 10。
		Player rewarded = NativeTalkFixture.player();
		NativeTalkFixture.add(rewarded, questId, QuestStatus.REWARD, 1);
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, rewardNpc, 31, 1, questId), "REWARD 31 必须开奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(rewarded, 5);
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, rewardNpc, 1009, 1, questId), "REWARD 1009 必须重开奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(rewarded, 5);
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, rewardNpc, -1, 1, questId), "REWARD 打开（-1）必须开奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(rewarded, 5);

		claims.calls = 0;
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, rewardNpc, 23, 1, questId), "领奖确认必须结算");
		assertEquals(1, claims.calls, "领奖口必须被调用一次");
		assertEquals(List.of(10), NativeTalkFixture.dialogPages(rewarded),
			"结算后回选择对话页 10（npc-complete finish=SELECTION_DIALOG）");
	}

	/**
	 * ⑪ 无目标领奖（原版 QuestDialog 无主键协议；2026-10-07 实机 13830 任务窗「实时奖励」= 110 无响应）：
	 * 确认包不带 NPC 上下文（{@code visibleObject == null}，引擎以 npcId=0 进入本面），按 questId 结算 +
	 * 关窗；非 REWARD 态与非确认段动作零副作用。13830-13834/23830-23834 退役前的 XML 合同（Playbook
	 * 案例 8.3，commit 4a23cf0a）即此语义，native 重建时曾丢失。
	 * <p>
	 * The targetless reward claim: a quest-journal confirmation carries no NPC context and settles by
	 * quest id alone, with the close-dialog tail (the retired XML contract of Playbook case 8.3).
	 */
	@Test
	void targetlessRewardClaimSettlesByQuestIdAndClosesTheWindow() {
		int questId = 13830;
		assertTrue(runtime.routes(questId), "13830 必须由 DD 运行时路由（退役行基线）");
		// 职业奖励梯只认转职后职业（与结算段同一映射）⇒ 探针用高级职业。
		// The class ladder only admits advanced classes (the settlement's own mapping).
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 30);
		NativeTalkFixture.add(player, questId, QuestStatus.REWARD, 1);

		// 实时奖励槽 110（任务窗「实时奖励」按钮的原始动作）。
		NativeTalkFixture.clearPackets(player);
		claims.calls = 0;
		assertTrue(runtime.onDialog(player, 0, 110, 0, questId), "无目标实时奖励确认必须结算");
		assertEquals(1, claims.calls, "领奖口必须被调用一次");
		NativeTalkFixture.assertCloseDialog(player);

		// 普通奖励槽 8（任务书「在任务窗点击[领取奖励]%」）同形。
		NativeTalkFixture.clearPackets(player);
		claims.calls = 0;
		assertTrue(runtime.onDialog(player, 0, 8, 0, questId), "无目标普通奖励确认必须结算");
		assertEquals(1, claims.calls, "领奖口必须被调用一次");
		NativeTalkFixture.assertCloseDialog(player);

		// 状态门：START 态零认领、零发页。
		Player started = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 30);
		NativeTalkFixture.add(started, questId, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(started);
		claims.calls = 0;
		assertFalse(runtime.onDialog(started, 0, 110, 0, questId), "START 态不得认领无目标领奖");
		assertEquals(0, claims.calls);
		assertTrue(NativeTalkFixture.dialogPages(started).isEmpty(), "START 态零发页");

		// 动作门：非确认段动作零认领、零发页。
		NativeTalkFixture.clearPackets(player);
		claims.calls = 0;
		assertFalse(runtime.onDialog(player, 0, 31, 0, questId), "非确认段动作不得被无目标领奖认领");
		assertEquals(0, claims.calls);
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty());
	}

	/**
	 * ⑫ 非 Talk 接取行的交付面（原版所有行恒建对象 #2，槽 +0x238 = `FUN_180c473e0`，P7-STEP2E1）：
	 * 2026-10-07 实机 13830（LevelUpLogIn 行）任务书写明「在任务窗点击[领取奖励]% 或 和[奥尔佩]%对话」，
	 * 退役迁移曾把交付面收窄到 Talk 接取行 ⇒ 奥尔佩处无响应。行为分形：START 不认领（进度面 owns）；
	 * REWARD 打开（31/26/-1）→ 页 10002（客户端 select_success 声明，未声明 fail-closed）；
	 * 1009（「点头」= SELECT_QUEST_REWARD）→ 页 5；领奖确认（23）→ 结算 + 页 10。
	 * <p>
	 * The delivery face of non-Talk rows (the always-registered retail object #2): open -> page 10002,
	 * the ack (1009) -> the reward window, the confirm -> settlement plus page 10; START stays silent.
	 */
	@Test
	void nonTalkRowsServeTheDeliveryNpcWithTheRetailObjectTwoShape() {
		int questId = 13830;
		int orphe = 203711;
		assertTrue(runtime.reportTalkInterests().getOrDefault(orphe, List.of()).contains(questId),
			"13830 必须注册在奥尔佩（203711）的交付面");

		// START（说明书未使用）：不认领、不跳步、零发页。
		Player started = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 30);
		NativeTalkFixture.add(started, questId, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(started);
		assertFalse(runtime.onDialog(started, orphe, 31, 1, questId), "START 态交付面不得认领非 Talk 行");
		assertFalse(runtime.onDialog(started, orphe, -1, 1, questId), "START 态打开不得被交付面认领");
		assertTrue(NativeTalkFixture.dialogPages(started).isEmpty(), "START 态零发页");

		// REWARD：打开（31/-1）→ 页 10002；1009 → 页 5；23 → 结算 + 页 10。
		Player rewarded = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 30);
		NativeTalkFixture.add(rewarded, questId, QuestStatus.REWARD, 1);
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, orphe, 31, 1, questId), "REWARD 行选必须发进行中页");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(rewarded, 10002, questId);
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, orphe, -1, 1, questId), "REWARD 打开（-1）必须发进行中页");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(rewarded, 10002, questId);
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, orphe, 1009, 1, questId), "点头（1009）必须开奖励窗");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(rewarded, 5, questId);
		claims.calls = 0;
		NativeTalkFixture.clearPackets(rewarded);
		assertTrue(runtime.onDialog(rewarded, orphe, 23, 1, questId), "领奖确认必须结算");
		assertEquals(1, claims.calls, "领奖口必须被调用一次");
		assertEquals(List.of(10), NativeTalkFixture.dialogPages(rewarded),
			"结算后回选择对话页 10（npc-complete finish=SELECTION_DIALOG）");
	}

	/**
	 * ⑩ Talk 接取对话面（原版 `FUN_180c47220` 词汇，只服务无状态玩家）：行选 31/26 → 4762；1002 → 接取 +
	 * 1003；1003 → 1004；20000/20001 收尾 → 关窗页 0（旧 XML close-dialog）；1008/其余 ≥1000 原样回发；
	 * &lt;1000 零动作；1007 → 客户端契约问询窗（fail-closed）；已接取玩家不得再见接取入口页；
	 * 打开（-1）不认领——归引擎开门规则（进行中重放，否则通用页 10 列表，2026-10-05 实机 834166 修复）。
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
			"1002 = 原版 SetQuestAcquired");
		// 已接取玩家：接取面不再服务该任务（带任务上下文时不得再见 4762；同 NPC 其他任务仍可接）。
		NativeTalkFixture.clearPackets(player);
		runtime.onDialog(player, npcId, 31, 1, questId);
		assertFalse(NativeTalkFixture.dialogPages(player).contains(4762), "已接取玩家不得再见接取入口页");
		// 打开（-1，无任务上下文）不得被接取面认领：宿主开门平面 = 进行中/可交重放，否则通用页 10
		// 列表；原 -1 认领使 NPC 834166 打开直发 4762、任务列表不可见（实机 2026-10-05）。
		Player opener = NativeTalkFixture.player();
		assertFalse(runtime.onDialog(opener, npcId, -1, 1, 0), "打开(-1) 归引擎开门规则（页 10 列表），接取面不得认领");
		assertTrue(NativeTalkFixture.dialogPages(opener).isEmpty(), "打开(-1) 本面零发页");
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
		// ≥1000 动作：客户端契约声明过该页才原样回发；未声明不由接取面认领（零发页，留给
		// NPC 自身层/AI——事件应援按钮 SETPRO1=10000 的增益面即靠此不被劫走；逐轴见
		// acquireFaceEchoesOnlyClientDeclaredActionPages）。
		// The >=1000 echo holds for client-declared pages only; undeclared actions stay unclaimed.
		boolean page1012Declared = QuestDialogContract.loadDefault().hasButtonPage(questId, 1012);
		assertEquals(page1012Declared, runtime.onDialog(other, npcId, 1012, 1, 0), "≥1000 动作按客户端契约裁定");
		if (page1012Declared) {
			NativeTalkFixture.assertOnlyDialogPage(other, 1012);
		} else {
			assertTrue(NativeTalkFixture.dialogPages(other).isEmpty(), "未声明动作零发页（不认领）");
		}
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
	 * ⑨b 接取面 ≥1000 动作的契约纪律：只有客户端任务页声明过的页动作才可原样回发；未声明的动作 id
	 * 对未接取任务不是页——原版把 NPC 对话按钮（事件应援的 HACTION_SETPRO1=10000）交给 NPC 自身层
	 * （AI 脚本 `World_event_NPC_support_buffer_01` / `ayas_support` 的增益面），接取面**不认领**
	 * （零发页、零写），认领（回发页或关窗）都会劫走 AI 的 buff：实机 2026-10-06（NPC 833671/833672）
	 * 先为「页 10000 + questId」load fail，改为关窗后 buff 仍不触发（AI 的引擎前置守卫被挡下）。
	 * The acquire face echoes >=1000 actions only when the client contract declares the page;
	 * undeclared actions stay unclaimed for the NPC's own AI layer.
	 */
	@Test
	void acquireFaceEchoesOnlyClientDeclaredActionPages() {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		boolean undeclaredProbed = false;
		boolean declaredProbed = false;
		for (Map.Entry<Integer, List<Integer>> entry : runtime.acquireTalkInterests().entrySet()) {
			for (int candidate : entry.getValue()) {
				// 轴 1：未声明 —— 事件族（80787-80790）的 SETPRO1=10000 不在契约页集合里。
				if (!undeclaredProbed && !contract.hasButtonPage(candidate, 10000)) {
					Player player = NativeTalkFixture.player();
					assertFalse(runtime.onDialog(player, entry.getKey(), 10000, 1, candidate),
						"未声明动作不由接取面认领（零发页，留给 NPC 自身层/AI 的增益面）");
					assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "未声明动作零发页");
					assertNull(player.getQuestStateList().getQuestState(candidate),
						"未接取任务的 SETPRO1 零写（原版 SetQuestProgress 的 status∉{3,4} 过滤）");
					undeclaredProbed = true;
				}
				// 轴 2：已声明 —— 声明了 ≥1000 页动作的行仍原样回发（如 80875 族声明 10000/10001）；
				// 排除有专属分支的接取词汇（1002/1003/1004/1007/1008/20000/20001）。
				if (!declaredProbed) {
					int declared = contract.pagesForQuest(candidate).keySet().stream()
						.filter(pageId -> pageId >= 1000 && pageId != 1002 && pageId != 1003 && pageId != 1004
							&& pageId != 1007 && pageId != 1008 && pageId != 20000 && pageId != 20001)
						.findFirst().orElse(-1);
					if (declared > 0) {
						Player player = NativeTalkFixture.player();
						if (runtime.onDialog(player, entry.getKey(), declared, 1, candidate)) {
							NativeTalkFixture.assertOnlyDialogPage(player, declared);
							declaredProbed = true;
						}
					}
				}
				if (undeclaredProbed && declaredProbed) {
					break;
				}
			}
			if (undeclaredProbed && declaredProbed) {
				break;
			}
		}
		assertTrue(undeclaredProbed, "须有未声明 10000 的可接 Talk 行（80787-80790 事件族）");
		assertTrue(declaredProbed, "须有声明 ≥1000 页动作的可接 Talk 行（如 80875 族）");
	}

	/**
	 * ⑪ 双角色接取（原版 progress handler 的 state!=3 分支，接取即执行**接取行附加动作**：
	 * `*(entry+0x10)` = QuestProgressExtraInfo 对象；2026-10-06 修正步 f 的「步 0 动作」误读）：
	 * ItemPlay（事件 5）/ EnterWorld（事件 0x12）/ LevelUp·LevelUpLogIn（`ctx+8 == def+8` 等级等值，
	 * 登录遍历只服务 kind 10）。
	 * Dual-role acquires fire on their retail events and run the accept-row extra actions.
	 */
	@Test
	void dualRoleAcquiresFireOnTheirRetailEvents() {
		// ItemPlay 接取（kind 3，物品 id 键控；该批接取行 minlvl 65/66 ⇒ 探测玩家取两族 66 级）。
		int[] itemFixture = firstAcquirable(runtime.acquireItemInterests(),
			(player, key, candidate) -> {
				for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
					Player probe = NativeTalkFixture.player(race, PlayerClass.WARRIOR, 66);
					if (runtime.onItemUsed(probe, key)
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
		// 接取分支执行接取行附加动作（原版 `FUN_180c46bb0`：+0xd8 成功 → cd50 读 `*(entry+0x10)`）：
		// 10500 接取列空 ⇒ 接取零动作（Movie 32 属步 0 列，随步 0 完成执行，不在接取时）。
		int level10500 = Integer.parseInt(table.find(10500).orElseThrow().acquireParam().trim());
		Player levelUp10500 = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, level10500);
		movies.clear();
		assertTrue(runtime.onLevelReached(levelUp10500, level10500, true), "10500 等级等值接取");
		assertTrue(levelUp10500.getQuestStateList().getQuestState(10500) != null, "10500 接取成档");
		assertTrue(movies.calls().isEmpty(), "接取列空的等级行接取零动作（步 0 列不在此执行）：" + movies.calls());
		// 升级遍历服务 kind 8 与 10（切换集无 kind 8 行 ⇒ 登录/升级同集）。
		Player onLevelUp = player(21, Set.of());
		setLevel(onLevelUp, level);
		assertTrue(runtime.onLevelReached(onLevelUp, level, false), "升级遍历按等级等值接取");
		// 等级等值守卫（原版 `ctx+8 == def+8`，运行时以等级键索引承担）：差一级零接取。
		Player off = player(22, Set.of());
		setLevel(off, level + 1);
		assertFalse(runtime.onLevelReached(off, off.getLevel(), true), "等级等值守卫（登录遍历）");
		assertFalse(runtime.onLevelReached(off, off.getLevel(), false), "等级等值守卫（升级遍历）");
	}

	/**
	 * ⑪-b kind-6 进区接取面（步 f，原版区 handler `FUN_180c47bf0` 尾段独立接取侧名字哈希树）：
	 * 20 行 / 15 别名入面；无状态玩家进同名区 → `(+0xd8)` 接取 + 接取行动作（本族接取列空 ⇒ 零动作）；
	 * 已接取/其他区不触发；
	 * 原版无区定义的 `DF6_QuestArea_Q25674` 登记原文 = 永不命中死边（镜像原版）。
	 * The kind-6 zone acquire face (step f): entering the same-named zone acquires and runs step-0
	 * actions; the retail-absent alias stays a registered dead edge, mirroring retail.
	 */
	@Test
	void zoneAcquireFaceFiresOnTheSameNamedZone() {
		// 兴趣面形状：20 行 / 15 别名（原版可解析 14 + 原版缺席 1）。
		long questCount = runtime.acquireZoneInterests().values().stream().flatMap(List::stream).distinct().count();
		assertEquals(20, questCount, "kind-6 接取行数 = 离线镜像");
		assertEquals(15, runtime.acquireZoneInterests().size(), "kind-6 别名数 = 15（含原版缺席别名）");
		assertTrue(runtime.acquireZoneInterests().containsKey("DF6_QUESTAREA_Q25674"),
			"原版无区定义的别名仍登记原文（死边镜像）");
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
			"进区接取 = 原版 SetQuestAcquired");
		// 已接取玩家再进区：不重复接取（state 分支只服务无状态玩家）。
		assertFalse(runtime.onEnterZone(acquired, zoneKey.toLowerCase(java.util.Locale.ROOT)),
			"已接取玩家进区不再触发接取分支");
		// 无关区名零动作。
		Player stranger = NativeTalkFixture.player();
		assertFalse(runtime.onEnterZone(stranger, "NOT_A_REGISTERED_ZONE"), "未登记区名不触发任何面");
	}

	/**
	 * ⑩-b Talk 对话接取收尾 = `FUN_180c4d5b0(-1,-1)` ⇒ **接取行附加动作**（`*(entry+0x10)` =
	 * QuestProgressExtraInfo 对象；2026-10-06 反编译复读修正步 f 的「步 0 动作」误读——1817 类行的
	 * 接取发放全在接取列、步 0 列无动作，13403 反相）。找一个带接取动作列的 talk 接取行，
	 * 1002 接取即按**接取列**执行（与进度步 0 列无关）。
	 * The Talk accept tail executes the ACCEPT-row actions (the retail QuestProgressExtraInfo object),
	 * never progress step 0's columns.
	 */
	@Test
	void talkDialogAcceptRunsTheAcceptRowActions() {
		AcceptFixture fixture = firstAcquirableTalkRowWithAcceptActions();
		assertNotNull(fixture, "须有可接取且带接取动作列的 talk 接取行");
		int questId = fixture.questId();
		Player player = NativeTalkFixture.player(fixture.race(), PlayerClass.WARRIOR, fixture.level());
		inventory.clear();
		movies.clear();
		spawns.clear();
		says.clear();
		teleports.clear();
		assertTrue(runtime.onDialog(player, fixture.npcId(), 1002, 1, 0), "1002 接取");
		assertTrue(player.getQuestStateList().getQuestState(questId) != null, "接取成档");
		Row row = table.find(questId).orElseThrow();
		List<String> expected = new ArrayList<>();
		for (int column : new int[] {1, 2}) {
			String text = row.acceptColumns().get(column);
			if (text == null || text.isBlank()) {
				continue;
			}
			String[] tokens = text.trim().split("[,\\s]+");
			for (int index = 0; index < tokens.length; index += 2) {
				expected.add((column == 1 ? "give:" : "remove:") + itemIndex.resolve(tokens[index]) + ":"
					+ Integer.parseInt(tokens[index + 1]));
			}
		}
		assertFalse(expected.isEmpty(), "接取行必须携带发/扣物品列");
		assertEquals(expected, inventory.calls(), "接取列发/扣按列序执行");
	}

	/**
	 * 13403 双发放回归（2026-10-06 实机第四轮「一次发放了 2 个」）：20000 接取收尾执行**接取列**
	 * （13403 接取列空）⇒ 探测器零发放；步 0 完成（Kinesos 10000，完成步动作列）⇒ 发放一次。
	 * 修复前接取收尾误跑步 0 列 ⇒ 接取发一次、步 0 完成再发一次。
	 * The 13403 double-grant regression: accept grants nothing (empty accept columns); completing
	 * step 0 grants QUEST_13403A exactly once.
	 */
	@Test
	void acceptTailDoesNotGrantTheStepZeroItemAgain() {
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 66);
		inventory.clear();
		assertTrue(runtime.onDialog(player, 203098, 20000, 1, 13403), "20000 接取（Spatalos）");
		assertTrue(player.getQuestStateList().getQuestState(13403) != null, "接取成档");
		assertTrue(inventory.calls().isEmpty(), "接取收尾不得发放步 0 列物品（探测器）：" + inventory.calls());
		String[] stepZeroGive = table.find(13403).orElseThrow().steps().getFirst().column(1).trim()
			.split("[,\\s]+");
		int detectorId = itemIndex.resolve(stepZeroGive[0]);
		// 销毁停止判定面：探测器 → {13403}（发/扣/用物载荷反查）；未引用物品零命中。
		assertEquals(List.of(13403), runtime.questsReferencingItem(detectorId), "探测器引用面 = 13403");
		assertTrue(runtime.questsReferencingItem(0).isEmpty(), "未引用物品零命中");
		inventory.clear();
		assertTrue(runtime.onDialog(player, 203096, 10000, 1, 13403), "Kinesos 10000 推进步 0→1");
		assertEquals(List.of("give:" + detectorId + ":1"), inventory.calls(), "步 0 完成发放探测器恰好一次");
		// 引擎销毁判定面：进行中（START）才计入；完成后自动退出（原版 User_DeleteQuest 门）。
		QuestEngine engine = new QuestEngine();
		assertEquals(List.of(13403), engine.activeQuestsReferencingItem(player, detectorId), "进行中引用面");
		player.getQuestStateList().getQuestState(13403).setStatus(QuestStatus.COMPLETE);
		assertTrue(engine.activeQuestsReferencingItem(player, detectorId).isEmpty(), "完成后不再计入");
	}

	/** 接取探针命中组合（行 + 接取 NPC + 可接取玩家参数）。 / A probed accept fixture. */
	private record AcceptFixture(int questId, int npcId, Race race, int level) {
	}

	/**
	 * 首个「可接取 ∧ 带接取动作列（value1/2_acquire_ 发扣）」的 talk 接取行组合：逐候选行用
	 * 种族×等级探针实际接取，命中即返回该组合（测试再以同参数复现并断言接取列执行）。
	 */
	private AcceptFixture firstAcquirableTalkRowWithAcceptActions() {
		java.util.Set<Integer> candidates = new TreeSet<>();
		for (List<Integer> quests : runtime.acquireTalkInterests().values()) {
			candidates.addAll(quests);
		}
		for (int questId : candidates) {
			Row row = table.find(questId).orElseThrow();
			String col1 = row.acceptColumns().get(1);
			String col2 = row.acceptColumns().get(2);
			if ((col1 == null || col1.isBlank()) && (col2 == null || col2.isBlank())) {
				continue;
			}
			int npcId = -1;
			for (Map.Entry<Integer, List<Integer>> entry : runtime.acquireTalkInterests().entrySet()) {
				if (entry.getValue().contains(questId)) {
					npcId = entry.getKey();
					break;
				}
			}
			if (npcId <= 0) {
				continue;
			}
			for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
				for (int level : new int[] {20, 40, 55, 66}) {
					Player probe = NativeTalkFixture.player(race, PlayerClass.WARRIOR, level);
					if (runtime.onDialog(probe, npcId, 1002, 1, 0)
						&& probe.getQuestStateList().getQuestState(questId) != null) {
						return new AcceptFixture(questId, npcId, race, level);
					}
				}
			}
		}
		return null;
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
	 * ⑫ 发/扣物品动作面（原版执行器 case 1/2）× 执行矩阵（步 f 逐 handler 修正）：推进边对 Hunt/
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
		assertTrue(inventory.calls().isEmpty(), "CollectItem 推进边零动作（原版拾取分支无执行器）");
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
	 * ⑬ 过场/刷怪动作面（原版执行器 case 4/5，EnterArea 推进边）：`Cutscene|Cutscene2` 走 CUTSCENE
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

		@Override
		public void enterInstance(Player player, int worldId, float x, float y, float z, int headingDegrees) {
			calls.add("enterInstance:" + worldId);
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
	 * ①c 任务计时面（col10 落面，2026-10-02）：到期判定镜像原版 `FUN_180c46d80`——进行中 ∧
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

	/** 该行当前持久步号（raw vars 低 6 位）。 / The row's persistent step number (raw vars bits 0-5). */
	private static int stepOf(Player player, int questId) {
		return DataDrivenProgress
			.step(player.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars());
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
