package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aion 5.8 Iluma(210100000)/Norsvold(220110000) 击杀任务的"客户端变体族"覆盖门禁。
 * Coverage gate for the Aion 5.8 Iluma/Norsvold kill quests: every variant the Aion 5.8 client counts
 * for a hunting step must be registered by the quest, and each registered family must keep at least one
 * target that the production spawn data actually places in an active map.
 * <p>回归背景：15546《雷欧娜的委托》只登记了世界中不刷新的基础模板（240475/240483/240495/240497），
 * 而客户端对话与计数条目的目标是同族的 T_ 变体（241656/241664/241676/241678 等），
 * 因此 {@code QuestEngine.onKill} 拿不到该任务的 onKill owner，击杀不会下发到任务、进度不更新。
 * Regression background: quest 15546 registered only the base templates that are never spawned, so the
 * spawned {@code T_} variants of the same family never reached the quest owner and the counters stayed 0.
 * <p>契约快照 {@code iluma-norsvold-kill-target-contract.tsv} 由客户端 {@code quest_monster.csv}
 * 的怪物名单经 npc_template 名称解析后生成，见 {@code .agents/summary/quest-15546-kill-progress/}。
 */
class QuestIlumaNorsvoldKillTargetCoverageTest {
	private static final String CONTRACT_RESOURCE = "/quest/iluma-norsvold-kill-target-contract.tsv";
	/** 快照规模：低于该值说明基线被误删或生成脚本漏了任务。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 45;
	/** 15546/25546：四族各 4 杀的目标集合与实际刷新变体。 */
	private static final Set<Integer> LEONA_VARIANTS = Set.of(
		240475, 240476, 241656, 241657,
		240483, 240484, 241664, 241665,
		240495, 240496, 241676, 241677,
		240497, 240498, 241678, 241679);
	private static final Set<Integer> LEONA_SPAWNED = Set.of(241656, 241657, 241664, 241665, 241676, 241677, 241678, 241679);
	private static final Path STATIC_DATA = Path.of("src/main/resources/aion/data/static_data");
	private static final int FOUR_COUNTER_QUESTS_REQUIRED_KILLS = 16;
	/** 客户端计数上限（{@code Progress(SECTION_n<4)}）：满值后的击杀不得再命中任何路线。 */
	private static final int FOUR_COUNTER_QUEST_CEILING = 4;

	@Test
	void killTargetsMatchTheReviewedClientVariantContract() {
		Map<Integer, Set<Integer>> contract = contractSnapshot();
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"kill-target contract snapshot must keep its reviewed coverage");
		for (Map.Entry<Integer, Set<Integer>> entry : contract.entrySet()) {
			int questId = entry.getKey();
			Set<Integer> actual = killTargets(load(questId));
			// 真端优先 + 变体轴裁定后断言为超集：真端名解析的显示名族闭包（M2-c）会把世界中
			// 实刷的同名兄弟 id 一并并入，客户端名单是必须覆盖的下界而非精确相等。
			// After the retail-first adoption plus the variant-axis registry the assertion is a
			// superset: the retail names' display-family closure (M2-c) also pulls same-name live
			// siblings, so the client list is a lower bound rather than an exact set.
			assertTrue(actual.containsAll(entry.getValue()),
				() -> "quest " + questId + " must cover the reviewed Iluma/Norsvold variant set; missing="
					+ entry.getValue().stream().filter(target -> !actual.contains(target)).toList());
		}
	}

	@Test
	void everyReviewedQuestKeepsTargetsThatTheActiveMapSpawns() {
		Set<Integer> spawned = spawnedNpcIdsInActiveMaps();
		for (Map.Entry<Integer, Set<Integer>> entry : contractSnapshot().entrySet()) {
			Set<Integer> live = new LinkedHashSet<>(entry.getValue());
			live.retainAll(spawned);
			assertFalse(live.isEmpty(),
				() -> "quest " + entry.getKey() + " registers no target that the production spawn data places in an active map");
		}
	}

	@Test
	void spawnedVariantsOfLeonaAndArundFavorAreRoutedAndCounted() {
		Set<Integer> spawned = spawnedNpcIdsInActiveMaps();
		Set<Integer> leonaLive = new LinkedHashSet<>(LEONA_VARIANTS);
		leonaLive.retainAll(spawned);
		assertEquals(LEONA_SPAWNED, leonaLive,
			"15546 must count exactly the four T_ families that Iluma spawns");
		for (int questId : List.of(15546, 25546)) {
			CompiledQuestDefinition compiled = load(questId);
			Set<Integer> live = new LinkedHashSet<>(contractSnapshot().get(questId));
			live.retainAll(spawned);
			assertFalse(live.isEmpty(), () -> "quest " + questId + " has no spawned target");
			for (int npcId : live) {
				assertTrue(routesKill(compiled, npcId),
					() -> "quest " + questId + " has no kill route for spawned variant " + npcId);
			}
			// 真端四段顺序链：四个计数槽 var0..var3（客户端 SECTION_1..4 一一对应），每段 4 变体；
			// 领奖节点投影 = 全链满态（4 段 × 4 杀 = 16）。
			// Retail four-stage chain: counter slots var0..var3, four variants each; the reward node
			// projects the full chain state (4 stages x 4 kills = 16).
			Map<String, Set<Integer>> stageTargets = chainStageTargets(compiled);
			assertEquals(Set.of("var0", "var1", "var2", "var3"), stageTargets.keySet(),
				() -> "quest " + questId + " must keep four independent client counters");
			for (Map.Entry<String, Set<Integer>> stage : stageTargets.entrySet()) {
				assertEquals(4, stage.getValue().size(),
					() -> "quest " + questId + " stage " + stage.getKey() + " must keep four variants");
			}
			int rewardTotal = rewardProjection(compiled.definition()).variables().values().stream()
				.mapToInt(Integer::intValue).sum();
			assertEquals(FOUR_COUNTER_QUESTS_REQUIRED_KILLS, rewardTotal,
				() -> "quest " + questId + " must complete after four kills per family");
		}
	}

	/** 任务 IR 中所有击杀路线登记的 NPC：KillNpc 与 KillNpcSet 一并展开。 */
	private static Set<Integer> killTargets(CompiledQuestDefinition compiled) {
		Set<Integer> targets = new LinkedHashSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
				targets.add(npcId);
			} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
				targets.addAll(npcIds);
			}
		}
		return targets;
	}

	/**
	 * 至少存在一个可提交状态能匹配该 NPC 击杀事实：零态（旧网格 / 链首段）+ 顺序链全部前缀态
	 * （每槽依次为活跃段、计数 0..required-1）。
	 * Whether any committable state matches the kill fact: the zero state (legacy grid / chain
	 * head) plus every chained prefix state (each slot active in turn, counts 0..required-1).
	 */
	private static boolean routesKill(CompiledQuestDefinition compiled, int npcId) {
		if (matchesAnyKillRoute(compiled, zeroCounters(compiled), npcId)) {
			return true;
		}
		for (Map<String, Integer> state : chainStates(compiled)) {
			if (matchesAnyKillRoute(compiled, state, npcId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 计数器已饱和（等于客户端上限）时的额外击杀不得命中任何路线，否则会提交一笔状态未变化的空事务
	 * 并下发一次任务状态更新，客户端把这次重复通知显示成"任务更新"。
	 * An extra kill of a saturated counter must not match any route: a matched-but-identical plan still commits and
	 * pushes a quest-state update, which the client renders as an unsolicited "quest updated" notice.
	 */
	@Test
	void saturatedCountersRejectExtraKillsWithoutMatchingAnyRoute() {
		for (int questId : List.of(15546, 25546)) {
			CompiledQuestDefinition compiled = load(questId);
			for (String field : List.of("var0", "var1", "var2", "var3")) {
				Map<String, Integer> variables = zeroCounters(compiled);
				variables.put(field, FOUR_COUNTER_QUEST_CEILING);
				Set<Integer> targets = counterKillTargets(compiled, field);
				assertEquals(4, targets.size(),
					() -> "quest " + questId + " must keep four variants for " + field);
				for (int npcId : targets) {
					assertFalse(matchesAnyKillRoute(compiled, variables, npcId),
						() -> "quest " + questId + " must not match an extra kill of saturated " + field
							+ " via npc " + npcId);
				}
			}
		}
	}

	/** 所有计数器归零的可提交状态。/ Committable state with every counter at zero. */
	private static Map<String, Integer> zeroCounters(CompiledQuestDefinition compiled) {
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : compiled.definition().progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		return variables;
	}

	/**
	 * 某计数槽的全部目标 NPC：旧并行网格取"该字段的自环计数路线"，顺序链取"该槽为首个未满段的
	 * 链上击杀边"（{@code var0..}槽号即链形槽位）。
	 * All target NPCs of one counter slot: legacy parallel grids use that field's self-loop counting
	 * routes, sequential chains use the kill edges hanging off states where the slot is the first
	 * unfinished one.
	 */
	private static Set<Integer> counterKillTargets(CompiledQuestDefinition compiled, String field) {
		Set<Integer> targets = new LinkedHashSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (!"started".equals(transition.sourceNode()) || !"started".equals(transition.targetNode())) {
				continue;
			}
			boolean touchesField = transition.actions().stream().anyMatch(action ->
				(action instanceof QuestAction.SetVariable set && set.field().equals(field))
					|| (action instanceof QuestAction.IncrementVariable increment && increment.field().equals(field)));
			if (!touchesField) {
				continue;
			}
			if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
				targets.add(npcId);
			} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
				targets.addAll(npcIds);
			}
		}
		if (!targets.isEmpty()) {
			return targets;
		}
		return chainStageTargets(compiled).getOrDefault(field, Set.of());
	}

	/**
	 * 顺序链形状：计数槽字段 → 该段目标集（由链上前缀态的击杀边归组）。无链形返回空映射。
	 * The sequential-chain shape: counter field → that stage's targets, grouped from the chained
	 * prefix states' kill edges. Empty for non-chain shapes.
	 */
	private static Map<String, Set<Integer>> chainStageTargets(CompiledQuestDefinition compiled) {
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> required = rewardProjection(definition).variables();
		Map<String, Set<Integer>> byField = new LinkedHashMap<>();
		for (int slot = 0; slot < definition.progressLayout().fields().size(); slot++) {
			BitField field = definition.progressLayout().fields().get(slot);
			int ceiling = required.getOrDefault(field.name(), 0);
			Set<Integer> targets = new LinkedHashSet<>();
			for (int count = 0; count < ceiling; count++) {
				String source = chainLabel(definition, slot, count);
				for (QuestTransition transition : definition.transitions()) {
					if (source.equals(transition.sourceNode())
						&& transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
						targets.add(npcId);
					}
				}
			}
			byField.put(field.name(), targets);
		}
		return byField;
	}

	/** 槽位 slot 计数为 count、前序段全满、后序段为 0 的链态标签（编译器 {@code label()} 同构）。 */
	private static String chainLabel(QuestDefinition definition, int slot, int count) {
		Map<String, Integer> required = rewardProjection(definition).variables();
		StringBuilder label = new StringBuilder();
		for (int index = 0; index < definition.progressLayout().fields().size(); index++) {
			int value;
			if (index < slot) {
				value = required.get("var" + index);
			} else if (index == slot) {
				value = count;
			} else {
				value = 0;
			}
			label.append((char) ('a' + index)).append(value);
		}
		return label.toString();
	}

	/** 顺序链全部前缀态（每槽依次为活跃段）。 / Every chained prefix state, each slot active in turn. */
	private static List<Map<String, Integer>> chainStates(CompiledQuestDefinition compiled) {
		List<Map<String, Integer>> states = new ArrayList<>();
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> required = rewardProjection(definition).variables();
		for (int slot = 0; slot < definition.progressLayout().fields().size(); slot++) {
			int ceiling = required.get("var" + slot);
			for (int count = 0; count < ceiling; count++) {
				Map<String, Integer> state = new LinkedHashMap<>();
				for (int index = 0; index < definition.progressLayout().fields().size(); index++) {
					state.put("var" + index, index < slot ? required.get("var" + index)
						: (index == slot ? count : 0));
				}
				states.add(state);
			}
		}
		return states;
	}

	/** 领奖节点投影（链满态）。 / The reward node projection (the full chain state). */
	private static NodeProjection rewardProjection(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow().projection();
	}

	/** 给定快照下是否有任意击杀转换能匹配该 NPC 击杀事实。 */
	private static boolean matchesAnyKillRoute(CompiledQuestDefinition compiled, Map<String, Integer> variables,
			int npcId) {
		QuestDefinition definition = compiled.definition();
		QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), QuestStatus.START,
			definition.progressLayout().pack(variables), Map.of());
		QuestEvent event = new QuestEvent.KillNpc(npcId);
		List<QuestTransition> ordered = new ArrayList<>(definition.transitions());
		ordered.sort(Comparator.comparing(transition -> transition.priority() == null
			? Integer.MAX_VALUE : transition.priority()));
		return ordered.stream()
			.anyMatch(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent());
	}

	/** 生产刷怪数据中所有落在活跃地图上的 NPC 模板 ID。 */
	private static Set<Integer> spawnedNpcIdsInActiveMaps() {
		Set<Integer> activeMaps = activeMapIds();
		Set<Integer> spawned = new LinkedHashSet<>();
		try (Stream<Path> files = Files.walk(STATIC_DATA.resolve("spawns"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
				String text = Files.readString(file, StandardCharsets.UTF_8);
				Matcher map = Pattern.compile("spawn_map map_id=\"(\\d+)\"").matcher(text);
				if (!map.find() || !activeMaps.contains(Integer.parseInt(map.group(1)))) {
					continue;
				}
				Matcher npc = Pattern.compile("npc_id=\"(\\d+)\"").matcher(text);
				while (npc.find()) {
					spawned.add(Integer.parseInt(npc.group(1)));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return spawned;
	}

	/** world_maps.xml 中未被注释的可用地图；注释掉的 600200000(Lakrum) 等不参与判定。 */
	private static Set<Integer> activeMapIds() {
		try {
			String raw = Files.readString(STATIC_DATA.resolve("world_maps.xml"), StandardCharsets.UTF_8);
			Matcher map = Pattern.compile("<map id=\"(\\d+)\"").matcher(raw.replaceAll("(?s)<!--.*?-->", ""));
			Set<Integer> ids = new LinkedHashSet<>();
			while (map.find()) {
				ids.add(Integer.parseInt(map.group(1)));
			}
			assertFalse(ids.isEmpty(), "world_maps.xml must declare active maps");
			return ids;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Map<Integer, Set<Integer>> contractSnapshot() {
		try (InputStream input = Objects.requireNonNull(
				QuestIlumaNorsvoldKillTargetCoverageTest.class.getResourceAsStream(CONTRACT_RESOURCE), CONTRACT_RESOURCE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			Map<Integer, Set<Integer>> rows = new LinkedHashMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("quest_id")) {
					continue;
				}
				String[] columns = trimmed.split("\t");
				if (columns.length != 2) {
					throw new AssertionError("malformed contract row: " + trimmed);
				}
				Set<Integer> targets = Arrays.stream(columns[1].trim().split(" "))
					.filter(token -> !token.isEmpty())
					.map(Integer::parseInt)
					.collect(Collectors.toCollection(LinkedHashSet::new));
				rows.put(Integer.parseInt(columns[0].trim()), targets);
			}
			return rows;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static CompiledQuestDefinition load(int questId) {
		// Iluma/Norsvold 行已由真端表驱动（退役），改从生产视图取定义；对拍口径不变。
		// The Iluma/Norsvold rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(questId);
	}
}
