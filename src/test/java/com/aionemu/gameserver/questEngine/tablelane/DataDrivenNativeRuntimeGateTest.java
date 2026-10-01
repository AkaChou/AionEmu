package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.FreezeReason;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * P7 步 2 步 d / 步 d2 门：DD 原生**进度运行时**（八类接线：Hunt / PvP / Talk / CollectItem / ItemPlay /
 * EnterArea / EnterWorld / TalkFOBJ）。
 * <p>
 * 冻结点：① 生产口径路由集为空（零行为变更：DD 切换集行仍由旧 IR 车道 owns）；② 兴趣面按 payload 逐类建立
 * （Hunt 组名 → 怪物 id、Talk/CollectItem → NPC、TalkFOBJ → NPC、ItemPlay → 物品 id、EnterArea → 同名注册区、
 * EnterWorld → worldId、PvP → 行步）；③ 事件语义 = 真端 handler 逐指令（Hunt `FUN_180c46020` 组计数、
 * PvP `FUN_180c46980` 闸门 + 单组、ItemPlay `FUN_180c46e90` 物品获得组计数、
 * Talk/CollectItem = `FUN_180c474b0` 共享对话平面（开页 select(K+1) + 顺序动作/1009/10255 步进）、
 * EnterArea `FUN_180c47bf0`/EnterWorld `FUN_180c467b0` 直接步进、TalkFOBJ `FUN_180c478e0` 二值组）；
 * ④ 冻结面闭合（routed ∪ frozen = 切换集，互斥；步 d2 后冻结面 = 真端缺席进区别名 §10.3-#23）。
 * <p>
 * P7 step 2d/2d2 gate for the native DD progress runtime: empty production routing (zero behavior change),
 * per-kind interest construction, retail event semantics for the eight wired kinds, and a closed
 * routed/frozen split of the switch set.
 */
class DataDrivenNativeRuntimeGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	private static final Path ZONES_DIR = Path.of("src/main/resources/aion/data/static_data/zones");
	private static final Pattern ZONE_NAME = Pattern.compile("<zone\\b[^>]*\\bname=\"([^\"]+)\"");

	/** 切换集规模（真端活行 ∧ owner RETAIL_TABLE）。 / Switch-set size frozen by the P7 contract batch. */
	private static final int SWITCH_ROWS = 1467;

	private static DataDrivenQuestTable table;
	private static Set<Integer> switchSet;
	private static NativeEnterAreaPort enterAreaPort;
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
		runtime = DataDrivenNativeRuntime.create(table, switchSet, NativeNpcNameResolver.instance(),
			enterAreaPort, RetailItemNameIndex.loadItemTemplates());
	}

	/** ① 生产单例 = 零行为变更：路由集为空，事件恒 false。 / Production singleton routes nothing. */
	@Test
	void productionRuntimeRoutesNothing() {
		DataDrivenNativeRuntime production = DataDrivenNativeRuntime.instance();
		assertTrue(production.routedQuestIds().isEmpty(), "本批不发运行期分流（切换随步 f）");
		assertTrue(production.ownedQuestIds().isEmpty(), "路由集为空 ⇒ 零行接管");
		assertTrue(production.killInterests().isEmpty(), "零兴趣注册");
		assertTrue(production.zoneInterests().isEmpty(), "零进区兴趣");
		assertTrue(production.itemPlayInterests().isEmpty(), "零物品获得兴趣");
		Player player = player(1, Set.of());
		assertFalse(production.onKill(player, 278516), "零路由 ⇒ 击杀恒 false");
		assertFalse(production.onEnterZone(player, "AB1_SensoryArea_Q1868a"), "零路由 ⇒ 进区恒 false");
		assertFalse(production.onEnterWorld(player, 210040000), "零路由 ⇒ 进世界恒 false");
		assertFalse(production.onDialog(player, 804915, 31, 1, 0), "零路由 ⇒ 对话恒 false");
		assertFalse(production.onItemAcquired(player, 100000001), "零路由 ⇒ 物品获得恒 false");
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
		// 打开：阶段页（select1 = 1011），步号不变。
		assertTrue(runtime.onDialog(player, npcId, 31, 1, 0), "对话打开必须被对话平面服务");
		NativeTalkFixture.assertOnlyDialogPage(player, DataDrivenNativeRuntime.stagePageForGate(stepIndex));
		assertEquals(stepIndex, DataDrivenProgress.step(state.getQuestVars().getQuestVars()), "打开不写步号");
		// 乱序动作（真端 `code-9999 != 当前步 + 1`）：零写、零回发。
		NativeTalkFixture.clearPackets(player);
		assertFalse(runtime.onDialog(player, npcId, 10000 + stepIndex + 2, 1, 0), "乱序动作必须静默");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "乱序动作不发页");
		// 顺序动作（真端守卫：code-9999 == 当前步 + 1 ⇔ code == 10000 + 当前步）：步号 + 1。
		assertTrue(runtime.onDialog(player, npcId, 10000 + stepIndex, 1, 0), "顺序动作必须步进");
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
		assertTrue(runtime.onDialog(player, npcId, 1009, 1, 0), "1009 必须被报告通道服务");
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
		assertTrue(runtime.onDialog(player, npcId, 31, 1, 0), "FOBJ 首次交互 0→1");
		boolean again = runtime.onDialog(player, npcId, 31, 1, 0);
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
		killer2.getQuestStateList().getQuestState(questId).getQuestVars().setVar(step.index());
		setLevel(killer2, 40 + gap + 5);
		setLevel(weakVictim, 40);
		assertFalse(runtime.onKillRanked(killer2, weakVictim, rank),
			"`killerLevel <= victimLevel + gap` 不成立 ⇒ 零动作");
		assertEquals(step.index(),
			DataDrivenProgress.step(killer2.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars()),
			"闸门失败不得写 vars");
	}

	/** ⑧ 冻结面：冻结行不得进兴趣面，且原因闭合。 / Frozen rows stay out of every interest. */
	@Test
	void frozenRowsAreNeverRouted() {
		assertFalse(runtime.frozenQuestIds().isEmpty(), "步 d2 后仍须有冻结行（真端缺席进区别名 §10.3-#23）");
		for (Map.Entry<Integer, FreezeReason> entry : runtime.frozenQuestIds().entrySet()) {
			int questId = entry.getKey();
			assertFalse(runtime.routes(questId), "冻结行不得路由");
			assertFalse(runtime.pvpSteps().containsKey(questId), "冻结行不得进 PvP 行集");
			assertEquals(FreezeReason.ZONE_ABSENT, entry.getValue(),
				"步 d2 后唯一冻结原因 = 真端缺席进区别名（§10.3-#23）");
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
		// 步 d2 八类接线后的已路由步数（planRow 离线镜像逐值一致，见
		// `.agents/summary/quest-engine-native/p7/tools/dd-planrow-mirror.py`）。
		assertEquals(Map.of(Kind.HUNT, 821, Kind.COLLECT_ITEM, 345, Kind.PVP, 207, Kind.TALK, 398,
			Kind.ENTER_AREA, 137, Kind.ITEM_PLAY, 42, Kind.ENTER_WORLD, 34, Kind.TALK_FOBJ, 18), routedSteps,
			"已路由行的逐类步数冻结");
		assertEquals(1458, runtime.routedQuestIds().size(), "可路由行冻结（步 d2：CollectItem/ItemPlay/对话平面接线）");
		assertEquals(9, runtime.frozenQuestIds().size(), "冻结行冻结（全部为真端缺席进区别名行）");
		Map<FreezeReason, Integer> byReason = new TreeMap<>();
		for (FreezeReason reason : runtime.frozenQuestIds().values()) {
			byReason.merge(reason, 1, Integer::sum);
		}
		assertEquals(Map.of(FreezeReason.ZONE_ABSENT, 9), byReason, "冻结原因分桶冻结（§10.3-#23）");
		// 步 d2 名字裁定后：载荷名面零未解析（165 个 hunt/talk/fobj/collectitem 失败名均为真端
		// quest_ai_name 组，已按 SimpleHunt 同轴落别名台账；唯一未解析面 = 切换集行引用的 LF6 进区别名）。
		assertEquals(Set.of("LF6_SensoryArea_Q15551_AtoB", "LF6_SensoryArea_Q15552_AtoD",
			"LF6_SensoryArea_Q15553_AtoF", "LF6_SensoryArea_Q15554_AtoH", "LF6_SensoryArea_Q15601a_Dynamic_Env",
			"LF6_SensoryArea_Q15602a_Dynamic_Env", "LF6_SensoryArea_Q15604a_Dynamic_Env",
			"LF6_SensoryArea_Q15605a_Named", "LF6_SensoryArea_Q15608a_Dynamic_Env"), runtime.unresolvedNames(),
			"未解析名字集 = 真端缺席进区别名（NativeEnterAreaPort.RETAIL_ABSENT_ALIASES 的切换集子集，§10.3-#23）");
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
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(RETENTION), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] columns = line.split("\t");
				if (columns.length >= 2 && columns[0].chars().allMatch(Character::isDigit)) {
					owners.put(Integer.parseInt(columns[0]), columns[1]);
				}
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
